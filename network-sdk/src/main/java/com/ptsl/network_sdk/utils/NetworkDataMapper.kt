package com.ptsl.network_sdk.utils

import android.os.Build
import android.util.Log
import com.ptsl.network_sdk.data_model.entity.NetworkDataEntity
import com.ptsl.network_sdk.bandwidth.DownloadUploadHelper
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

suspend fun ICell.prepareData(
    locationPair: Pair<Double, Double>,
    downloader: DownloadUploadHelper,
    hasMobileInternet: Boolean = false,
    activeNetworkMnc: String = "-1",
    usedSimSlot: Int = 0,
    rtt: Double = 0.0,
    latency: Double = 0.0,
    context: Context
): NetworkDataEntity {
    val mcc = this.network?.mcc
    val mnc = this.network?.mnc
    Log.d("MNC", "MNC : $mnc")

    val networkType = when (this) {
        is CellCdma, is CellGsm -> "2G"
        is CellWcdma, is CellTdscdma -> "3G"
        is CellLte, is CellNr -> "4G"
        else -> "Unknown"
    }
    
    val retryCount = if (networkType == "4G") 2 else 1

    val speedPair = downloader.getBandWidthSpeed(
        networkType = networkType,
        hasMobileInternet = hasMobileInternet,
        currentMnc = mnc,
        activeNetworkMnc = activeNetworkMnc,
        retryCountDownload = retryCount,
        retryCountUpload = retryCount
    )

    val entity = NetworkDataEntity().apply {
        this.time = CommonUtils.getCurrentDateTime()
        this.date = CommonUtils.getCurrentDate()
        this.mcc = mcc?.let { toIntSafe(it).toString() } ?: "0"
        this.mnc = mnc?.let { toIntSafe(it).toString() } ?: "0"
        this.lattitude = locationPair.first
        this.longitude = locationPair.second
        this.dlspeed = speedPair.downloadSpeedKbps
        this.ulspeed = speedPair.uploadSpeedKbps
        this.deviceModel = Build.MODEL
        this.data = if (hasMobileInternet) "Mobile" else "Wifi"
        this.isDataCaptureOffline = false
        this.isUserDeviceOnCall = isUserOnCall(context = context)
        this.deviceManufacture = Build.MANUFACTURER
        this.deviceOsVersion = Build.VERSION.SDK_INT.toString()
        this.usedSimSlot = usedSimSlot
        this.rtt = rtt
        this.latency = latency
        this.totalUploadVolume = speedPair.totalUploadMB
        this.totalDownloadVolume = speedPair.totalDownloadMB
        this.band = this@prepareData.band?.name ?: ""
    }

    when (this) {
        is CellCdma -> {
            entity.type = "CDMA"
            entity.snr = toIntSafe(this.signal.evdoSnr)
            entity.rssi = toIntSafe(this.signal.cdmaRssi)
        }
        is CellGsm -> {
            entity.type = "2G"
            entity.lac = toIntSafe(this.lac)
            entity.cid = toIntSafe(this.cid)
            entity.arfcn = toIntSafe(this.band?.arfcn)
            entity.ta = toIntSafe(this.signal.timingAdvance)
            entity.rxlev = toIntSafe(this.signal.rssi)
            entity.bitRateError = toIntSafe(this.signal.bitErrorRate)
            entity.rxQual = calculateRXQUAL(entity.bitRateError ?: 0)
            entity.rssi = toIntSafe(this.signal.rssi)
        }
        is CellWcdma -> {
            entity.type = "3G"
            entity.lac = toIntSafe(this.lac)
            entity.cid = toIntSafe(this.cid)
            entity.psc = toIntSafe(this.psc)
            entity.arfcn = toIntSafe(this.band?.downlinkUarfcn)
            entity.rscp = toIntSafe(this.signal.rscp)
            entity.ecNo = toIntSafe(this.signal.ecno)
            entity.rssi = toIntSafe(this.signal.rssi)
        }
        is CellLte -> {
            entity.type = "4G"
            entity.tac = toIntSafe(this.tac)
            entity.cid = toIntSafe(this.cid)
            entity.enb = toIntSafe(this.enb)
            entity.pci = toIntSafe(this.pci)
            entity.ta = toIntSafe(this.signal.timingAdvance)
            entity.bw = toIntSafe(this.bandwidth)
            entity.arfcn = toIntSafe(this.band?.downlinkEarfcn)
            entity.rsrp = toIntSafe(this.signal.rsrp)
            entity.rsrq = toIntSafe(this.signal.rsrq)
            entity.snr = toIntSafe(this.signal.snr)
            entity.cqi = toIntSafe(this.signal.cqi)
            entity.rssi = toIntSafe(this.signal.rssi)
        }
        is CellNr -> {
            entity.type = "5G"
            entity.tac = toIntSafe(this.tac)
            entity.pci = toIntSafe(this.pci)
            entity.rsrp = toIntSafe(this.signal.ssRsrp)
            entity.rsrq = toIntSafe(this.signal.ssRsrq)
            entity.snr = toIntSafe(this.signal.ssSinr)
        }
        is CellTdscdma -> {
            entity.type = "3G"
            entity.lac = toIntSafe(this.lac) // Actually maps to cid for CellTdscdma in the original code, but lac is safer if available. We keep the original logic for lac which mapped cid.
            entity.cid = toIntSafe(this.cid)
            entity.rssi = toIntSafe(this.signal.rssi)
        }
    }
    return entity
}


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

private fun toIntSafe(value: Any?): Int? {
    if (value == null) return null
    val str = value.toString()
    if (str.equals("null", ignoreCase = true) || str.isBlank()) return null
    return try {
        str.toDouble().toInt()
    } catch (e: Exception) {
        null
    }
}

private fun calculateRXQUAL(ber: Int): Int {
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





