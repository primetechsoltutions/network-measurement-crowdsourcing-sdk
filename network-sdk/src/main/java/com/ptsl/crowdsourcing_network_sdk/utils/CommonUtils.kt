package com.ptsl.crowdsourcing_network_sdk.utils


import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build
import android.telephony.SubscriptionManager
import android.telephony.TelephonyManager
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
    fun isGpsEnabled(context: Context): Boolean {
        val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as? android.location.LocationManager
        return locationManager?.isProviderEnabled(android.location.LocationManager.GPS_PROVIDER)?:false
    }
/** Returns number of SIM cards */
 fun getSimCount(context: Context): Int {
        return try {
            val sm = SubscriptionManager.from(context)
            sm.activeSubscriptionInfoList?.size ?: 0
        } catch (e: Exception) {
            0
        }
    }

    fun isMobileNetworkConnected(context: Context): Boolean {
        return try {
            val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
            val network = cm?.activeNetwork ?: return false
            val caps = cm.getNetworkCapabilities(network) ?: return false
            caps.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)
        } catch (_: Exception) {
            false
        }
    }

    fun isWifiNetworkConnected(context: Context): Boolean {
        return try {
            val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
            val network = cm?.activeNetwork ?: return false
            val caps = cm.getNetworkCapabilities(network) ?: return false
            caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)
        } catch (_: Exception) {
            false
        }
    }

    fun getActiveNetworkMNC(context: Context): String {
        var mnc = "-1"
        try {
            val tm = context.getSystemService(Context.TELEPHONY_SERVICE) as? TelephonyManager
            if (tm != null) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    val dataSubId = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                        SubscriptionManager.getActiveDataSubscriptionId()
                    } else {
                        SubscriptionManager.getDefaultDataSubscriptionId()
                    }
                    if (dataSubId != SubscriptionManager.INVALID_SUBSCRIPTION_ID) {
                        val subManager = tm.createForSubscriptionId(dataSubId)
                        val operator = subManager.networkOperator
                        if (!operator.isNullOrEmpty() && operator.length >= 3) {
                            mnc = operator.substring(3)
                        }
                    }
                }

                // Fallback if mnc is still invalid/not set
                if (mnc == "-1" || mnc.isEmpty()) {
                    val operator = tm.networkOperator
                    if (!operator.isNullOrEmpty() && operator.length >= 3) {
                        mnc = operator.substring(3)
                    }
                }
            }
        } catch (_: Exception) { }

        val cleanMnc = mnc.trim()
        return if (cleanMnc == "-1") {
            "-1"
        } else if (cleanMnc.length == 1) {
            "0$cleanMnc"
        } else {
            cleanMnc
        }
    }

}
