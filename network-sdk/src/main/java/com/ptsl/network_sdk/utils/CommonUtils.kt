package com.ptsl.network_sdk.utils


import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.roundToInt

object CommonUtils {

    private const val DATE_TIME_FORMAT = "yyyy-MM-dd'T'HH:mm:ss"
    private const val DATE_ONLY_FORMAT = "yyyy-MM-dd"

    /** Returns current date-time in ISO-like format */
    fun getCurrentDateTime(): String {
        return SimpleDateFormat(DATE_TIME_FORMAT, Locale.US)
            .format(Date())
    }

    /** Returns only date (yyyy-MM-dd) */
    fun getCurrentDate(): String {
        return SimpleDateFormat(DATE_ONLY_FORMAT, Locale.US)
            .format(Date())
    }

    /** Round double to exactly 2 decimal places */
    fun round2(value: Double): Double {
        return (value * 100).roundToInt() / 100.0
    }

    /** Check if GPS is enabled */
    fun isGpsEnabled(context: android.content.Context): Boolean {
        val locationManager = context.getSystemService(android.content.Context.LOCATION_SERVICE) as android.location.LocationManager
        return locationManager.isProviderEnabled(android.location.LocationManager.GPS_PROVIDER)
    }
}
