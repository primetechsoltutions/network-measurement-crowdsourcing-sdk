package com.ptsl.network_sdk.utils


import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.roundToInt

object CommonUtils {

    private const val DATE_TIME_FORMAT = "yyyy-MM-dd'T'HH:mm:ss"

    /** Returns current date-time in ISO-like format */
    fun getCurrentDateTime(): String {
        return SimpleDateFormat(DATE_TIME_FORMAT, Locale.US)
            .format(Date())
    }

    /** Returns only date-time (kept for backward compatibility if needed) */
    fun getCurrentDate(): String {
        return getCurrentDateTime()
    }

    /** Round double to exactly 2 decimal places */
    fun round2(value: Double): Double {
        return (value * 100).roundToInt() / 100.0
    }
}
