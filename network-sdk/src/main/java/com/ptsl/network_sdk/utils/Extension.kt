package com.ptsl.network_sdk.utils

import android.os.Build
import android.util.Log
import com.ptsl.network_sdk.data_model.entity.NetworkDataEntity
import com.ptsl.network_sdk.dl_ul_test.DownloadUploadHelper
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
import com.ptsl.network_sdk.data_model.entity.FTPNetworkDataEntity

suspend fun ICell.prepareDate(
    locationPair: Pair<Double, Double>,
    downloader: DownloadUploadHelper,
    hasMobileInternet: Boolean=false,
    activeNetworkMnc : String = "-1",
    usedSimSlot: Int = 0,
    rtt:Double=0.0,
    latency:Double=0.0,
    context: Context
): NetworkDataEntity {
    val mcc = this.network?.mcc
    val mnc = this.network?.mnc
    Log.e("MNC", "MNC : ${mnc}")
    return when (this) {
        is CellCdma -> {
            val speedPair = downloader.getBandWidthSpeed(networkType = "2G",hasMobileInternet= hasMobileInternet, currentMnc = mnc, activeNetworkMnc = activeNetworkMnc)
            return NetworkDataEntity().also {
                it.time = CommonUtils.getCurrentDateTime()
                it.date = CommonUtils.getCurrentDate()
                it.mcc = "${removeNullFromString("${mcc}")}"
                it.mnc = "${removeNullFromString("${mnc}")}"
                it.type = "CDMA"
                it.band = "${removeNullFromString(this.band?.name ?: "")}"
                it.snr = removeNullFromString("${this.signal.evdoSnr}")
                it.rssi = removeNullFromString("${this.signal.cdmaRssi}")
                it.lattitude = locationPair.first
                it.longitude = locationPair.second
                it.dlspeed = speedPair.downloadSpeedKbps
                it.ulspeed = speedPair.uploadSpeedKbps
                it.deviceModel = "${Build.MODEL}"
                it.data = if (hasMobileInternet) "Mobile" else "Wifi"
                it.isDataCaptureOffline = false
                it.isUserDeviceOnCall = isUserOnCall(context = context)
                it.deviceManufacture ="${Build.MANUFACTURER}"
                it.deviceOsVersion ="${Build.VERSION.SDK_INT}"
                it.usedSimSlot= usedSimSlot
                it.rtt = rtt
                it.latency =latency
                it.totalUploadVolume=speedPair.totalUploadMB
                it.totalDownloadVolume=speedPair.totalDownloadMB
            }
        }

        is CellGsm -> {
            val speedPair = downloader.getBandWidthSpeed(networkType = "2G",hasMobileInternet= hasMobileInternet, currentMnc = mnc, activeNetworkMnc = activeNetworkMnc)
            return NetworkDataEntity().also {
                it.time = CommonUtils.getCurrentDateTime()
                it.date = CommonUtils.getCurrentDate()
                it.mcc = "${removeNullFromString("${mcc}")}"
                it.mnc = "${removeNullFromString("${mnc}")}"
                it.lac = removeNullFromString("${this.lac}")
                it.type = "2G"
                it.cid = removeNullFromString("${this.cid}")
                it.arfcn = removeNullFromString("${this.band?.arfcn}")
                it.ta = removeNullFromString("${this.signal.timingAdvance}")
                it.band = "${removeNullFromString(this.band?.name ?: "")}"
                it.rxlev = removeNullFromString("${this.signal.rssi}")
                it.rxQual = removeNullFromString("${this.signal.bitErrorRate?.let {value -> calculateRXQUAL(
                    value
                )}}")
                it.bitRateError = removeNullFromString("${this.signal.bitErrorRate}")
                it.rssi = removeNullFromString("${this.signal.rssi}")
                it.lattitude = locationPair.first
                it.longitude = locationPair.second
                it.dlspeed = speedPair.downloadSpeedKbps
                it.ulspeed = speedPair.uploadSpeedKbps
                it.deviceModel = "${Build.MODEL}"
                it.data = if (hasMobileInternet) "Mobile" else "Wifi"
                it.isDataCaptureOffline = false
                it.isUserDeviceOnCall = isUserOnCall(context = context)
                it.deviceManufacture ="${Build.MANUFACTURER}"
                it.deviceOsVersion ="${Build.VERSION.SDK_INT}"
                it.usedSimSlot= usedSimSlot
                it.rtt = rtt
                it.latency =latency
                it.totalUploadVolume=speedPair.totalUploadMB
                it.totalDownloadVolume=speedPair.totalDownloadMB

            }
        }

        is CellWcdma -> {
            val speedPair = downloader.getBandWidthSpeed(networkType = "3G",hasMobileInternet= hasMobileInternet, currentMnc = mnc, activeNetworkMnc = activeNetworkMnc)
            return NetworkDataEntity().also {
                it.time = CommonUtils.getCurrentDateTime()
                it.date = CommonUtils.getCurrentDate()
                it.mcc = "${removeNullFromString("${mcc}")}"
                it.mnc = "${removeNullFromString("${mnc}")}"
                it.lac = removeNullFromString("${this.lac}")
                it.type = "3G"
                it.cid = removeNullFromString("${this.cid}")
                it.psc = removeNullFromString("${this.psc}")
                it.arfcn = removeNullFromString("${this.band?.downlinkUarfcn ?: ""}")
                it.band = "${removeNullFromString(this.band?.name ?: "")}"
                it.rscp = removeNullFromString("${this.signal.rscp}")
                it.ecNo = removeNullFromString("${this.signal.ecno}")
                it.rssi = removeNullFromString("${this.signal.rssi}")
                it.lattitude = locationPair.first
                it.longitude = locationPair.second
                it.dlspeed = speedPair.downloadSpeedKbps
                it.ulspeed = speedPair.uploadSpeedKbps
                it.deviceModel = "${Build.MODEL}"
                it.data = if (hasMobileInternet)"Mobile" else "Wifi"
                it.isDataCaptureOffline = false
                it.isUserDeviceOnCall = isUserOnCall(context = context)
                it.deviceManufacture ="${Build.MANUFACTURER}"
                it.deviceOsVersion ="${Build.VERSION.SDK_INT}"
                it.usedSimSlot= usedSimSlot
                it.rtt = rtt
                it.latency =latency
                it.totalUploadVolume=speedPair.totalUploadMB
                it.totalDownloadVolume=speedPair.totalDownloadMB
            }
        }

        is CellLte -> {
            val speedPair = downloader.getBandWidthSpeed(networkType = "4G",hasMobileInternet= hasMobileInternet, currentMnc = mnc, activeNetworkMnc = activeNetworkMnc, retryCountDownload = 2, retryCountUpload = 2)
            return NetworkDataEntity().also {
                it.time = CommonUtils.getCurrentDateTime()
                it.date = CommonUtils.getCurrentDate()
                it.mcc = "${removeNullFromString("${mcc}")}"
                it.mnc = "${removeNullFromString("${mnc}")}"
                it.tac = removeNullFromString("${this.tac}")
                it.type = "4G"
                it.cid = removeNullFromString("${this.cid}")
                it.enb = removeNullFromString("${this.enb}")
                it.pci = removeNullFromString("${this.pci}")
                it.ta = removeNullFromString("${this.signal.timingAdvance}")
                it.bw = removeNullFromString("${this.bandwidth}")
                it.arfcn = removeNullFromString("${this.band?.downlinkEarfcn}")
                it.band = "${removeNullFromString(this.band?.name ?: "")}"
                it.rsrp = removeNullFromString("${this.signal.rsrp}")
                it.rsrq = removeNullFromString("${this.signal.rsrq}")
                it.snr = removeNullFromString("${this.signal.snr}")
                it.cqi = removeNullFromString("${this.signal.cqi}")
                it.rssi = removeNullFromString("${this.signal.rssi}")
                it.lattitude = locationPair.first
                it.longitude = locationPair.second
                it.dlspeed = speedPair.downloadSpeedKbps
                it.ulspeed = speedPair.uploadSpeedKbps
                it.deviceModel = "${Build.MODEL}"
                it.data = if (hasMobileInternet)"Mobile" else "Wifi"
                it.isDataCaptureOffline = false
                it.isUserDeviceOnCall = isUserOnCall(context = context)
                it.deviceManufacture ="${Build.MANUFACTURER}"
                it.deviceOsVersion ="${Build.VERSION.SDK_INT}"
                it.usedSimSlot= usedSimSlot
                it.rtt = rtt
                it.latency =latency
                it.totalUploadVolume=speedPair.totalUploadMB
                it.totalDownloadVolume=speedPair.totalDownloadMB
            }
        }

        is CellNr -> {
            val speedPair = downloader.getBandWidthSpeed(networkType = "4G",hasMobileInternet= hasMobileInternet, currentMnc = mnc, activeNetworkMnc = activeNetworkMnc, retryCountDownload = 2, retryCountUpload = 2)
            return NetworkDataEntity().also {
                it.time = CommonUtils.getCurrentDateTime()
                it.date = CommonUtils.getCurrentDate()
                it.mcc = "${removeNullFromString("${mcc}")}"
                it.mnc = "${removeNullFromString("${mnc}")}"
                it.tac = removeNullFromString("${this.tac}")
                it.type = "5G"
                it.pci = removeNullFromString("${this.pci}")
                it.band = "${removeNullFromString(this.band?.name ?: "")}"
                it.rsrp = removeNullFromString("${this.signal.ssRsrp}")
                it.rsrq = removeNullFromString("${this.signal.ssRsrq}")
                it.snr = removeNullFromString("${this.signal.ssSinr}")
                it.lattitude = locationPair.first
                it.longitude = locationPair.second
                it.dlspeed = speedPair.downloadSpeedKbps
                it.ulspeed = speedPair.uploadSpeedKbps
                it.deviceModel = "${Build.MODEL}"
                it.data = if (hasMobileInternet)"Mobile" else "Wifi"
                it.isDataCaptureOffline = false
                it.isUserDeviceOnCall = isUserOnCall(context = context)
                it.deviceManufacture ="${Build.MANUFACTURER}"
                it.deviceOsVersion ="${Build.VERSION.SDK_INT}"
                it.usedSimSlot= usedSimSlot
                it.rtt = rtt
                it.latency =latency
                it.totalUploadVolume=speedPair.totalUploadMB
                it.totalDownloadVolume=speedPair.totalDownloadMB
            }
        }

        is CellTdscdma -> {
            val speedPair = downloader.getBandWidthSpeed(networkType = "3G",hasMobileInternet= hasMobileInternet, currentMnc = mnc, activeNetworkMnc = activeNetworkMnc)
            return NetworkDataEntity().also {
                it.time = CommonUtils.getCurrentDateTime()
                it.date = CommonUtils.getCurrentDate()
                it.mcc = "${removeNullFromString("${mcc}")}"
                it.mnc = "${removeNullFromString("${mnc}")}"
                it.lac = removeNullFromString("${this.cid}")
                it.type = "3G"
                it.cid = removeNullFromString("${this.cid}")
                it.band = "${removeNullFromString(this.band?.name ?: "")}"
                it.rssi = removeNullFromString("${this.signal.rssi}")
                it.lattitude = locationPair.first
                it.longitude = locationPair.second
                it.dlspeed = speedPair.downloadSpeedKbps
                it.ulspeed = speedPair.uploadSpeedKbps
                it.deviceModel = "${Build.MODEL}"
                it.data = if (hasMobileInternet)"Mobile" else "Wifi"
                it.isDataCaptureOffline = false
                it.isUserDeviceOnCall = isUserOnCall(context = context)
                it.deviceManufacture ="${Build.MANUFACTURER}"
                it.deviceOsVersion ="${Build.VERSION.SDK_INT}"
                it.usedSimSlot= usedSimSlot
                it.rtt = rtt
                it.latency =latency
                it.totalUploadVolume=speedPair.totalUploadMB
                it.totalDownloadVolume=speedPair.totalDownloadMB
            }
        }

        else -> NetworkDataEntity()
    }
}

suspend fun ICell.prepareFTPData(
    locationPair: Pair<Double, Double>,
    downloader: DownloadUploadHelper,
    hasMobileInternet: Boolean = false,
    activeNetworkMnc: String = "-1",
): FTPNetworkDataEntity {
    val mcc = this.network?.mcc
    val mnc = this.network?.mnc
    val deviceManufacture = Build.MANUFACTURER
    val deviceModel = Build.MODEL
    val deviceOsVersion = Build.VERSION.SDK_INT.toString()
    val type = when (this) {
        is CellGsm -> "2G"
        is CellWcdma, is CellTdscdma -> "3G"
        is CellLte -> "4G"
        is CellNr -> "5G"
        else -> "Unknown"
    }

    val speedPair = downloader.getBandWidthSpeed(
        networkType = type,
        hasMobileInternet = hasMobileInternet,
        currentMnc = mnc,
        activeNetworkMnc = activeNetworkMnc,
        retryCountDownload = 2,
        retryCountUpload = 2
    )

    return FTPNetworkDataEntity().also {
        it.date = CommonUtils.getCurrentDate()
        it.mcc = removeNullFromString("$mcc").toString()
        it.mnc = removeNullFromString("$mnc").toString()
        it.technologyType = type
        it.band = removeNullFromString(this.band?.name ?: "").toString()
        it.latitude = locationPair.first
        it.longitude = locationPair.second
        it.dlSpeed = speedPair.downloadSpeedKbps
        it.ulSpeed = speedPair.uploadSpeedKbps
        it.deviceManufacture = deviceManufacture
        it.deviceModel = deviceModel
        it.deviceOsVersion = deviceOsVersion
        it.internetConnectivityType = if (hasMobileInternet) "Mobile" else "Wifi"
        it.totalUploadVolume = speedPair.totalUploadMB
        it.totalDownloadVolume = speedPair.totalDownloadMB

        when (this) {
            is CellGsm -> {
                it.cid = removeNullFromString("${this.cid}")
            }
            is CellWcdma -> {
                it.cid = removeNullFromString("${this.cid}")
            }
            is CellLte -> {
                it.cid = removeNullFromString("${this.cid}")
                it.enb = removeNullFromString("${this.enb}")
                it.tac = removeNullFromString("${this.tac}")
                it.rsrp = removeNullFromString("${this.signal.rsrp}")
                it.rsrq = removeNullFromString("${this.signal.rsrq}")
                it.snr = removeNullFromString("${this.signal.snr}")
            }
            is CellNr -> {
                it.tac = removeNullFromString("${this.tac}")
                it.rsrp = removeNullFromString("${this.signal.ssRsrp}")
                it.rsrq = removeNullFromString("${this.signal.ssRsrq}")
                it.snr = removeNullFromString("${this.signal.ssSinr}")
            }
        }
    }
}

suspend fun ResponseBody?.getTotalBytes(): Int {
    var size = 0
    val inputStream: InputStream? = this?.byteStream()
    val byteArrayOutputStream = ByteArrayOutputStream()
    val buffer = ByteArray(1024)
    var length: Int
    var totalBytesRead = 0
    try {
        while (inputStream?.read(buffer).also { length = it ?: -1 } != -1) {
            coroutineContext.ensureActive() // Check for cancellation
            byteArrayOutputStream.write(buffer, 0, length)
            totalBytesRead += length
        }
        val byteArray = byteArrayOutputStream.toByteArray()
        size = byteArray.size
    } catch (e: Exception) {
        if (e is kotlinx.coroutines.CancellationException) throw e
    } finally {
        inputStream?.close()
        byteArrayOutputStream.close()
    }
    return size
}

private fun removeNullFromString(str: String): Int {
    return try {
        str.replace("null", "").toDouble().toInt()
    } catch (e: Exception) {
        0
    }
}

 private fun calculateRXQUAL(ber: Int): Int {
     return if (ber in 0..7) ber else 0
 }

fun isUserOnCall(context: Context): Boolean {
    return try {
        val telephonyManager =
            context.getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager

        telephonyManager.callState == TelephonyManager.CALL_STATE_RINGING ||
                telephonyManager.callState == TelephonyManager.CALL_STATE_OFFHOOK

    } catch (_: Exception) {
        false
    }
}




