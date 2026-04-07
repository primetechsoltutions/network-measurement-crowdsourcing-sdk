package com.ptsl.network_sdk.utils

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.LocationManager
import android.os.Bundle
import android.util.Log
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.google.android.gms.common.api.ResolvableApiException
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.LocationSettingsRequest
import com.google.android.gms.location.Priority
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

import java.lang.ref.WeakReference

/**
 * Handles permission requests and GPS enablement prompts for the Network SDK.
 * Implements case-by-case logic for Standard (once-per-day) vs FTP (every-time) GPS prompts.
 */
class CheckPermissionHandler(activity: AppCompatActivity) {
    private val activityRef = WeakReference(activity)
    private val activity: AppCompatActivity? get() = activityRef.get()

    private val pendingCallbacks = mutableListOf<(Boolean) -> Unit>()
    private var isRequestInProgress = false

    fun isPermissionGranted(): Boolean {
        return isAllPermissionsGrantedExcludingGps() && isGpsEnabled()
    }

    fun isAllPermissionsGrantedExcludingGps(): Boolean {
        return isPhoneStatePermissionGranted() && isLocationPermissionGranted()
    }

    /**
     * requests necessary permissions and GPS enablement.
     * @param ignoreGpsLimit If true (FTP), GPS prompt shows every call. 
     *                       If false (Standard), GPS prompt shows once-per-day.
     */
    fun requestPermission(ignoreGpsLimit: Boolean = false, callback: (Boolean) -> Unit) {
        if (isPermissionGranted()) {
            callback(true)
            return
        }

        synchronized(pendingCallbacks) {
            pendingCallbacks.add(callback)
            if (isRequestInProgress) return
            isRequestInProgress = true
        }

        val currentActivity = activity ?: run {
            notifyCallbacksAndReset()
            return
        }

        // 20-second safety reset for state management
        try {
            currentActivity.window.decorView.postDelayed({
                if (isRequestInProgress) {
                    Log.w("CheckPermissionHandler", "Permission request timed out. Resetting state.")
                    notifyCallbacksAndReset()
                }
            }, 20000)
        } catch (e: Exception) {
            Log.w("CheckPermissionHandler", "Could not post timeout (activity may be destroyed): ${e.message}")
        }

        // GPS Prompt Logic
        if (isAllPermissionsGrantedExcludingGps() && !isGpsEnabled()) {
            if (ignoreGpsLimit) {
                // Case-2: FTP capture - forced prompt every time
                showGpsEnablePrompt(isForced = true)
            } else if (shouldShowGpsPrompt()) {
                // Case-1: Standard capture - once a day
                showGpsEnablePrompt(isForced = false)
            } else {
                notifyCallbacksAndReset()
            }
            return
        }

        startPermissionFlow(ignoreGpsLimit)
    }

    private fun startPermissionFlow(ignoreGpsLimit: Boolean) {
        val permissions = arrayOf(
            Manifest.permission.READ_PHONE_STATE,
            Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.ACCESS_FINE_LOCATION
        )

        val fragment = getPermissionFragment() ?: run {
            Log.e("CheckPermissionHandler", "Cannot start permission flow: Activity/Fragment state invalid.")
            notifyCallbacksAndReset()
            return
        }
        val success = fragment.requestPermissions(permissions) { _ ->
            val allGranted = isAllPermissionsGrantedExcludingGps()
            if (allGranted && !isGpsEnabled()) {
                if (ignoreGpsLimit) {
                    showGpsEnablePrompt(isForced = true)
                } else if (shouldShowGpsPrompt()) {
                    showGpsEnablePrompt(isForced = false)
                } else {
                    notifyCallbacksAndReset()
                }
            } else if (!allGranted) {
                notifyCallbacksAndReset()
            } else {
                notifyCallbacksAndReset()
            }
        }
        
        if (!success) {
            Log.e("CheckPermissionHandler", "Failed to launch requestPermissions. Resetting.")
            notifyCallbacksAndReset()
        }
    }

    private fun showGpsEnablePrompt(isForced: Boolean) {
        val currentActivity = activity ?: return
        currentActivity.runOnUiThread {
            if (!isForced) {
                markGpsPromptShown()
            }

            val locationRequest = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, System.currentTimeMillis() % 10000).build()
            val builder = LocationSettingsRequest.Builder()
                .addLocationRequest(locationRequest)
                .setAlwaysShow(true)
            
            LocationServices.getSettingsClient(currentActivity)
                .checkLocationSettings(builder.build())
                .addOnCompleteListener { task ->
                    if (!task.isSuccessful) {
                        val exception = task.exception
                        if (exception is ResolvableApiException) {
                            Log.d("CheckPermissionHandler", "Resolution required for GPS. Status: ${exception.statusCode}")
                            try {
                                val intentSenderRequest = IntentSenderRequest.Builder(exception.resolution.intentSender).build()
                                val fragment = getPermissionFragment()
                                if (fragment != null) {
                                    fragment.resolveGps(intentSenderRequest) {
                                        notifyCallbacksAndReset()
                                    }
                                    return@addOnCompleteListener
                                } else {
                                    notifyCallbacksAndReset()
                                }
                            } catch (e: Exception) {
                                notifyCallbacksAndReset()
                            }
                        } else {
                            notifyCallbacksAndReset()
                        }
                    } else {
                        notifyCallbacksAndReset()
                    }
                }
        }
    }

    private fun notifyCallbacksAndReset() {
        val result = isPermissionGranted()
        synchronized(pendingCallbacks) {
            val callbacks = ArrayList(pendingCallbacks)
            pendingCallbacks.clear()
            isRequestInProgress = false
            callbacks.forEach { it(result) }
        }
    }

    private fun getPermissionFragment(): PermissionFragment? {
        val currentActivity = activity ?: return null
        if (currentActivity.isFinishing || currentActivity.isDestroyed) {
            Log.w("CheckPermissionHandler", "Activity is finishing or destroyed. Aborting.")
            return null
        }
        
        val fragmentManager = currentActivity.supportFragmentManager
        if (fragmentManager.isDestroyed || fragmentManager.isStateSaved) {
            Log.w("CheckPermissionHandler", "FragmentManager is destroyed or state is saved. Aborting.")
            return null
        }

        var fragment = fragmentManager.findFragmentByTag("permission_fragment") as? PermissionFragment
        if (fragment == null) {
            fragment = PermissionFragment()
            try {
                // CRITICAL: Must use commitNowAllowingStateLoss() to ensure the fragment 
                // is attached synchronously before we try to use its launchers.
                fragmentManager.beginTransaction()
                    .add(fragment, "permission_fragment")
                    .commitNowAllowingStateLoss()
            } catch (e: Exception) {
                Log.e("CheckPermissionHandler", "Failed to add permission fragment: ${e.message}")
                return null
            }
        }
        return fragment
    }

    fun isGpsEnabled(): Boolean {
        val currentActivity = activity ?: return false
        val locationManager = currentActivity.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)
    }

    private fun shouldShowGpsPrompt(): Boolean {
        val currentActivity = activity ?: return false
        val prefs = currentActivity.getSharedPreferences("network_sdk_prefs", Context.MODE_PRIVATE)
        val lastPrompt = prefs.getLong("last_gps_prompt_timestamp", 0)
        val currentDate = SimpleDateFormat("yyyyMMdd", Locale.US).format(Date())
        val lastDate = SimpleDateFormat("yyyyMMdd", Locale.US).format(Date(lastPrompt))
        return currentDate != lastDate
    }

    private fun markGpsPromptShown() {
        val currentActivity = activity ?: return
        val prefs = currentActivity.getSharedPreferences("network_sdk_prefs", Context.MODE_PRIVATE)
        prefs.edit().putLong("last_gps_prompt_timestamp", System.currentTimeMillis()).apply()
    }

    fun isLocationPermissionGranted(): Boolean {
        val currentActivity = activity ?: return false
        return ContextCompat.checkSelfPermission(currentActivity, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(currentActivity, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
    }

    fun isPhoneStatePermissionGranted(): Boolean {
        val currentActivity = activity ?: return false
        return ContextCompat.checkSelfPermission(currentActivity, Manifest.permission.READ_PHONE_STATE) == PackageManager.PERMISSION_GRANTED
    }

    class PermissionFragment : Fragment() {
        private var permissionCallback: ((Map<String, Boolean>) -> Unit)? = null
        private var gpsCallback: (() -> Unit)? = null
        
        private val permissionLauncher = registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
            permissionCallback?.invoke(it)
        }

        private val gpsResolutionLauncher = registerForActivityResult(ActivityResultContracts.StartIntentSenderForResult()) {
            gpsCallback?.invoke()
        }


        fun requestPermissions(permissions: Array<String>, callback: (Map<String, Boolean>) -> Unit): Boolean {
            if (!isAdded) {
                Log.e("PermissionFragment", "Fragment not attached. Cannot request permissions.")
                return false
            }
            return try {
                this.permissionCallback = callback
                permissionLauncher.launch(permissions)
                true
            } catch (e: Exception) {
                Log.e("PermissionFragment", "Error launching permissions: ${e.message}")
                false
            }
        }

        fun resolveGps(intentSenderRequest: IntentSenderRequest, callback: () -> Unit) {
            if (!isAdded) {
                Log.e("PermissionFragment", "Fragment not attached. Cannot resolve GPS.")
                callback()
                return
            }
            try {
                this.gpsCallback = callback
                gpsResolutionLauncher.launch(intentSenderRequest)
            } catch (e: Exception) {
                Log.e("PermissionFragment", "Error launching GPS resolution: ${e.message}")
                callback()
            }
        }

        override fun onCreate(savedInstanceState: Bundle?) {
            super.onCreate(savedInstanceState)
        }
    }
}
