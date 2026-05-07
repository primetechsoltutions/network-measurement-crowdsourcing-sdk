package com.ptsl.network_sdk.location

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import androidx.core.app.ActivityCompat
import com.ptsl.network_sdk.permission.PermissionHandler
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull

/**
 * Utility to fetch current location using Play Services.
 * Returns a fallback (0.0, 0.0) if permissions are missing or timeout occurs.
 */
object LocationHelper {
    private val ZERO_LOCATION = Pair(0.0, 0.0)

    suspend fun getCurrentLocation(context: Context): Pair<Double, Double> {
        val hasPermission = PermissionHandler.isLocationPermissionGranted(context)

        if (!hasPermission) return ZERO_LOCATION

        val client = LocationServices.getFusedLocationProviderClient(context)

        return try {
            // 1. Try to get a fresh location with a 10-second timeout
            val location = withTimeoutOrNull(10_000) {
                awaitCurrentLocation(client)
            }

            if (location != null) {
                Pair(location.latitude, location.longitude)
            } else {
                // 2. Fallback to last known location if fresh location fails or times out
                awaitLastLocation(client)
            }
        } catch (_: Exception) {
            ZERO_LOCATION
        }
    }

    private suspend fun awaitCurrentLocation(
        client: com.google.android.gms.location.FusedLocationProviderClient
    ): android.location.Location? {
        return suspendCancellableCoroutine { cont ->
            val cts = CancellationTokenSource()
            try {
                client.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, cts.token)
                    .addOnSuccessListener { loc -> cont.resume(loc) { _, _, _ -> } }
                    .addOnFailureListener { cont.resume(null) { _, _, _ -> } }
                cont.invokeOnCancellation { cts.cancel() }
            } catch (_: SecurityException) {
                cont.resume(null) { _, _, _ -> }
            }
        }
    }

    private suspend fun awaitLastLocation(
        client: com.google.android.gms.location.FusedLocationProviderClient
    ): Pair<Double, Double> {
        return suspendCancellableCoroutine { cont ->
            try {
                client.lastLocation
                    .addOnSuccessListener { lastLoc ->
                        if (lastLoc != null) {
                            cont.resume(Pair(lastLoc.latitude, lastLoc.longitude)) { _, _, _ -> }
                        } else {
                            cont.resume(ZERO_LOCATION) { _, _, _ -> }
                        }
                    }
                    .addOnFailureListener { cont.resume(ZERO_LOCATION) { _, _, _ -> } }
            } catch (_: SecurityException) {
                cont.resume(ZERO_LOCATION) { _, _, _ -> }
            }
        }
    }
}