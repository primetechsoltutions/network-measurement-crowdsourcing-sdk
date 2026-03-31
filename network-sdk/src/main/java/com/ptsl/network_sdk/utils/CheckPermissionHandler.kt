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

/**
 * Handles permission requests and GPS enablement prompts for the Network SDK.
 * Implements case-by-case logic for Standard (once-per-day) vs FTP (every-time) GPS prompts.
 */
class CheckPermissionHandler(private val activity: AppCompatActivity) {

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

        // 20-second safety reset for state management
        activity.window.decorView.postDelayed({
            if (isRequestInProgress) {
                Log.w("CheckPermissionHandler", "Permission request timed out. Resetting state.")
                notifyCallbacksAndReset()
            }
        }, 20000)

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

        val fragment = getPermissionFragment()
        fragment.requestPermissions(permissions) { _ ->
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
                if (ignoreGpsLimit) {
                    showManualPermissionSettingsPrompt()
                } else {
                    notifyCallbacksAndReset()
                }
            } else {
                notifyCallbacksAndReset()
            }
        }
    }

    private fun showManualPermissionSettingsPrompt() {
        activity.runOnUiThread {
            android.app.AlertDialog.Builder(activity)
                .setTitle("Permissions Required")
                .setMessage("Please enable Location and Phone State permissions in App Settings to proceed with this diagnostic measurement.")
                .setPositiveButton("Settings") { _, _ ->
                    val intent = android.content.Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                    val uri = android.net.Uri.fromParts("package", activity.packageName, null)
                    intent.data = uri
                    getPermissionFragment().startSystemSettings(intent) {
                        if (isAllPermissionsGrantedExcludingGps() && !isGpsEnabled()) {
                            showGpsEnablePrompt(isForced = true)
                        } else {
                            notifyCallbacksAndReset()
                        }
                    }
                }
                .setNegativeButton("Cancel") { _, _ ->
                    notifyCallbacksAndReset()
                }
                .setCancelable(false)
                .show()
        }
    }

    private fun showGpsEnablePrompt(isForced: Boolean) {
        activity.runOnUiThread {
            if (!isForced) {
                markGpsPromptShown()
            }

            val locationRequest = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, System.currentTimeMillis() % 10000).build()
            val builder = LocationSettingsRequest.Builder()
                .addLocationRequest(locationRequest)
                .setAlwaysShow(true)
            
            LocationServices.getSettingsClient(activity)
                .checkLocationSettings(builder.build())
                .addOnCompleteListener { task ->
                    if (!task.isSuccessful) {
                        val exception = task.exception
                        if (exception is ResolvableApiException) {
                            Log.d("CheckPermissionHandler", "Resolution required for GPS. Status: ${exception.statusCode}")
                            try {
                                val intentSenderRequest = IntentSenderRequest.Builder(exception.resolution.intentSender).build()
                                getPermissionFragment().resolveGps(intentSenderRequest) {
                                    notifyCallbacksAndReset()
                                }
                                return@addOnCompleteListener
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

    private fun getPermissionFragment(): PermissionFragment {
        val fragmentManager = activity.supportFragmentManager
        var fragment = fragmentManager.findFragmentByTag("permission_fragment") as? PermissionFragment
        if (fragment == null) {
            fragment = PermissionFragment()
            fragmentManager.beginTransaction().add(fragment, "permission_fragment").commitNow()
        }
        return fragment
    }

    fun isGpsEnabled(): Boolean {
        val locationManager = activity.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)
    }

    private fun shouldShowGpsPrompt(): Boolean {
        val prefs = activity.getSharedPreferences("network_sdk_prefs", Context.MODE_PRIVATE)
        val lastPrompt = prefs.getLong("last_gps_prompt_timestamp", 0)
        val currentDate = SimpleDateFormat("yyyyMMdd", Locale.US).format(Date())
        val lastDate = SimpleDateFormat("yyyyMMdd", Locale.US).format(Date(lastPrompt))
        return currentDate != lastDate
    }

    private fun markGpsPromptShown() {
        val prefs = activity.getSharedPreferences("network_sdk_prefs", Context.MODE_PRIVATE)
        prefs.edit().putLong("last_gps_prompt_timestamp", System.currentTimeMillis()).apply()
    }

    fun isLocationPermissionGranted(): Boolean {
        return ContextCompat.checkSelfPermission(activity, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(activity, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
    }

    fun isPhoneStatePermissionGranted(): Boolean {
        return ContextCompat.checkSelfPermission(activity, Manifest.permission.READ_PHONE_STATE) == PackageManager.PERMISSION_GRANTED
    }

    class PermissionFragment : Fragment() {
        private var permissionCallback: ((Map<String, Boolean>) -> Unit)? = null
        private var gpsCallback: (() -> Unit)? = null
        private var settingsCallback: (() -> Unit)? = null
        
        private val permissionLauncher = registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
            permissionCallback?.invoke(it)
        }

        private val gpsResolutionLauncher = registerForActivityResult(ActivityResultContracts.StartIntentSenderForResult()) {
            gpsCallback?.invoke()
        }

        private val settingsLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            settingsCallback?.invoke()
        }

        fun requestPermissions(permissions: Array<String>, callback: (Map<String, Boolean>) -> Unit) {
            this.permissionCallback = callback
            permissionLauncher.launch(permissions)
        }

        fun resolveGps(intentSenderRequest: IntentSenderRequest, callback: () -> Unit) {
            this.gpsCallback = callback
            gpsResolutionLauncher.launch(intentSenderRequest)
        }

        fun startSystemSettings(intent: android.content.Intent, callback: () -> Unit) {
            this.settingsCallback = callback
            settingsLauncher.launch(intent)
        }

        override fun onCreate(savedInstanceState: Bundle?) {
            super.onCreate(savedInstanceState)
            retainInstance = true
        }
    }
}
