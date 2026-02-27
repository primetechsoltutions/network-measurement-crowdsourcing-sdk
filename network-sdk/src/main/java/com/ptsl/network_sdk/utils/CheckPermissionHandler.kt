package com.ptsl.network_sdk.utils

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.LocationManager
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
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

class CheckPermissionHandler(private val activity: AppCompatActivity) {

    private val pendingCallbacks = mutableListOf<(Boolean) -> Unit>()
    private var isRequestInProgress = false

    fun isPermissionGranted(): Boolean {
        val permissions = listOf(
            Manifest.permission.READ_PHONE_STATE,
            Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.ACCESS_FINE_LOCATION
        )

        val isAllPermissionsGranted = permissions.all { permission ->
            ContextCompat.checkSelfPermission(activity, permission) == PackageManager.PERMISSION_GRANTED
        }
        return isGpsEnabled() && isAllPermissionsGranted
    }

    fun requestPermission(callback: (Boolean) -> Unit) {
        if (isPermissionGranted()) {
            callback(true)
            return
        }

        synchronized(pendingCallbacks) {
            pendingCallbacks.add(callback)
            if (isRequestInProgress) return
            isRequestInProgress = true
        }

        startPermissionFlow()
    }

    private fun startPermissionFlow() {
        val permissions = arrayOf(
            Manifest.permission.READ_PHONE_STATE,
            Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.ACCESS_FINE_LOCATION
        )

        val fragmentManager = activity.supportFragmentManager
        var fragment = fragmentManager.findFragmentByTag("permission_fragment") as? PermissionFragment
        
        if (fragment == null) {
            fragment = PermissionFragment()
            fragmentManager.beginTransaction().add(fragment, "permission_fragment").commitNow()
        }

        fragment.requestPermissions(permissions) { results ->
            val allGranted = results.values.all { it }
            if (allGranted && !isGpsEnabled() && shouldShowGpsPrompt()) {
                showGpsEnablePrompt()
            } else {
                // No longer showing rationale or settings dialog per request
                notifyCallbacksAndReset()
            }
        }
    }

    /*
    private fun showRationaleOrSettings(deniedList: List<String>) {
        val showRationale = deniedList.any { activity.shouldShowRequestPermissionRationale(it) }
        
        val builder = AlertDialog.Builder(activity)
            .setCancelable(false)
            .setNegativeButton("Cancel") { _, _ -> notifyCallbacksAndReset() }

        if (showRationale) {
            builder.setTitle("Permissions Required")
                .setMessage("The core functionalities of the app rely on these permissions for optimal performance.")
                .setPositiveButton("OK") { _, _ -> startPermissionFlow() }
        } else {
            builder.setTitle("Permissions settings")
                .setMessage("Please allow necessary permissions in settings for the app to function correctly.")
                .setPositiveButton("Settings") { _, _ ->
                    val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                        data = Uri.fromParts("package", activity.packageName, null)
                    }
                    activity.startActivity(intent)
                    notifyCallbacksAndReset() // Cannot track when they return from settings easily
                }
        }
        builder.show()
    }
    */

    private fun showGpsEnablePrompt() {
        markGpsPromptShown()

        val locationRequest = LocationRequest.Builder(Priority.PRIORITY_BALANCED_POWER_ACCURACY, 1000).build()
        val builder = LocationSettingsRequest.Builder().addLocationRequest(locationRequest)
        
        LocationServices.getSettingsClient(activity)
            .checkLocationSettings(builder.build())
            .addOnCompleteListener { task ->
                if (!task.isSuccessful) {
                    val exception = task.exception
                    if (exception is ResolvableApiException) {
                        try {
                            exception.startResolutionForResult(activity, 1001)
                        } catch (e: Exception) {
                            Log.e("PermissionHandler", "Error starting GPS resolution", e)
                        }
                    }
                }
                notifyCallbacksAndReset()
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
        private var callback: ((Map<String, Boolean>) -> Unit)? = null
        
        private val launcher = registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
            callback?.invoke(it)
        }

        fun requestPermissions(permissions: Array<String>, callback: (Map<String, Boolean>) -> Unit) {
            this.callback = callback
            launcher.launch(permissions)
        }

        override fun onCreate(savedInstanceState: Bundle?) {
            super.onCreate(savedInstanceState)
            retainInstance = true
        }
    }
}

