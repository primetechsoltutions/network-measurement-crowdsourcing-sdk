package com.ptsl.crowdsourcing_network_sdk.utils

import android.os.Build
import android.util.Log
import com.ptsl.crowdsourcing_network_sdk.data_model.entity.NetworkDataEntity
import com.ptsl.crowdsourcing_network_sdk.dl_ul_test.DownloadUploadHelper
import cz.mroczis.netmonster.core.model.cell.CellCdma
import cz.mroczis.netmonster.core.model.cell.CellGsm
import cz.mroczis.netmonster.core.model.cell.CellLte
import cz.mroczis.netmonster.core.model.cell.CellNr
import cz.mroczis.netmonster.core.model.cell.CellTdscdma
import cz.mroczis.netmonster.core.model.cell.CellWcdma
import cz.mroczis.netmonster.core.model.cell.ICell
import okhttp3.ResponseBody
import java.io.ByteArrayOutputStream
import java.io.InputStream
import kotlinx.coroutines.ensureActive
import kotlin.coroutines.coroutineContext
import android.content.Context
import android.telephony.TelephonyManager



suspend fun ResponseBody?.getTotalBytes(): Int {
    var size = 0
    val inputStream: InputStream? = this?.byteStream()
    val byteArrayOutputStream = ByteArrayOutputStream()
    val buffer = ByteArray(1024)
    var length: Int
    try {
        while (inputStream?.read(buffer).also { length = it ?: -1 } != -1) {
            coroutineContext.ensureActive() // Check for cancellation
            byteArrayOutputStream.write(buffer, 0, length)
        }
        size = byteArrayOutputStream.size()
    } catch (e: Exception) {
        if (e is kotlinx.coroutines.CancellationException) throw e
    } finally {
        inputStream?.close()
        byteArrayOutputStream.close()
    }
    return size
}

internal fun toIntSafe(value: Any?): Int? {

    if (value == null) return null
    val str = value.toString()
    if (str.equals("null", ignoreCase = true) || str.isBlank()) return null
    return try {
        str.toDouble().toInt()
    } catch (e: Exception) {
        null
    }
}

internal fun calculateRXQUAL(ber: Int): Int {

    return if (ber in 0..7) ber else 0
}

fun isUserOnCall(context: Context): Boolean {
    return try {
        val telephonyManager =
            context.getSystemService(Context.TELEPHONY_SERVICE) as? TelephonyManager

        telephonyManager?.callState == TelephonyManager.CALL_STATE_RINGING ||
                telephonyManager?.callState == TelephonyManager.CALL_STATE_OFFHOOK

    } catch (_: Exception) {
        false
    }
}





