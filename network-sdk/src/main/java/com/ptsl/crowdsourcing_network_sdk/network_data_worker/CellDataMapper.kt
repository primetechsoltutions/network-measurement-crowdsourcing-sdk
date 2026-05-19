package com.ptsl.crowdsourcing_network_sdk.network_data_worker

import android.content.Context
import android.os.Build
import com.ptsl.crowdsourcing_network_sdk.data_model.BandwidthTestResult
import com.ptsl.crowdsourcing_network_sdk.data_model.entity.NetworkDataEntity
import com.ptsl.crowdsourcing_network_sdk.utils.CommonUtils
import com.ptsl.crowdsourcing_network_sdk.utils.isUserOnCall
import com.ptsl.crowdsourcing_network_sdk.utils.toIntSafe
import com.ptsl.crowdsourcing_network_sdk.utils.calculateRXQUAL
import cz.mroczis.netmonster.core.model.cell.*

/**
 * Responsible for mapping NetMonster [ICell] data to [NetworkDataEntity].
 * Encapsulates the complex mapping logic for different cell types.
 *
 * Design pattern: Mapper / Strategy
 */
internal class CellDataMapper {

    fun mapToEntity(
        cell: ICell,
        locationPair: Pair<Double, Double>,
        speedResult: BandwidthTestResult,
        hasMobileInternet: Boolean,
        usedSimSlot: Int,
        rtt: Double,
        latency: Double,
        context: Context
    ): NetworkDataEntity {
        val mcc = cell.network?.mcc
        val mnc = cell.network?.mnc

        val entity = NetworkDataEntity().apply {
            this.time = CommonUtils.getCurrentDateTime()
            this.date = CommonUtils.getCurrentDate()
            this.mcc = mcc?.let { toIntSafe(it).toString() } ?: "0"
            this.mnc = mnc?.let { toIntSafe(it).toString() } ?: "0"
            this.lattitude = locationPair.first
            this.longitude = locationPair.second
            this.dlspeed = speedResult.downloadSpeedKbps
            this.ulspeed = speedResult.uploadSpeedKbps
            this.deviceModel = Build.MODEL
            this.data = if (CommonUtils.isMobileNetworkConnected(context)) {
                "Mobile"
            } else if (CommonUtils.isWifiNetworkConnected(context)) {
                "Wifi"
            } else {
                "NA"
            }
            this.isDataCaptureOffline = false
            this.isUserDeviceOnCall = isUserOnCall(context = context)
            this.deviceManufacture = Build.MANUFACTURER
            this.deviceOsVersion = Build.VERSION.SDK_INT.toString()
            this.usedSimSlot = usedSimSlot
            this.rtt = rtt
            this.latency = latency
            this.totalUploadVolume = speedResult.totalUploadMB
            this.totalDownloadVolume = speedResult.totalDownloadMB
        }

        mapCellSpecificFields(cell, entity)

        
        return entity
    }

    private fun mapCellSpecificFields(cell: ICell, entity: NetworkDataEntity) {
        entity.band = cell.band?.name ?: ""
        
        when (cell) {
            is CellCdma -> {
                entity.type = "CDMA"
                entity.snr = toIntSafe(cell.signal.evdoSnr)
                entity.rssi = toIntSafe(cell.signal.cdmaRssi)
            }
            is CellGsm -> {
                entity.type = "2G"
                entity.lac = toIntSafe(cell.lac)
                entity.cid = toIntSafe(cell.cid)
                entity.arfcn = toIntSafe(cell.band?.arfcn)
                entity.ta = toIntSafe(cell.signal.timingAdvance)
                entity.rxlev = toIntSafe(cell.signal.rssi)
                entity.bitRateError = toIntSafe(cell.signal.bitErrorRate)
                entity.rxQual = calculateRXQUAL(entity.bitRateError ?: 0)
                entity.rssi = toIntSafe(cell.signal.rssi)
            }
            is CellWcdma -> {
                entity.type = "3G"
                entity.lac = toIntSafe(cell.lac)
                entity.cid = toIntSafe(cell.cid)
                entity.psc = toIntSafe(cell.psc)
                entity.arfcn = toIntSafe(cell.band?.downlinkUarfcn)
                entity.rscp = toIntSafe(cell.signal.rscp)
                entity.ecNo = toIntSafe(cell.signal.ecno)
                entity.rssi = toIntSafe(cell.signal.rssi)
            }
            is CellLte -> {
                entity.type = "4G"
                entity.tac = toIntSafe(cell.tac)
                entity.cid = toIntSafe(cell.cid)
                entity.enb = toIntSafe(cell.enb)
                entity.pci = toIntSafe(cell.pci)
                entity.ta = toIntSafe(cell.signal.timingAdvance)
                entity.bw = toIntSafe(cell.bandwidth)
                entity.arfcn = toIntSafe(cell.band?.downlinkEarfcn)
                entity.rsrp = toIntSafe(cell.signal.rsrp)
                entity.rsrq = toIntSafe(cell.signal.rsrq)
                entity.snr = toIntSafe(cell.signal.snr)
                entity.cqi = toIntSafe(cell.signal.cqi)
                entity.rssi = toIntSafe(cell.signal.rssi)
            }
            is CellNr -> {
                entity.type = "5G"
                entity.tac = toIntSafe(cell.tac)
                entity.pci = toIntSafe(cell.pci)
                entity.rsrp = toIntSafe(cell.signal.ssRsrp)
                entity.rsrq = toIntSafe(cell.signal.ssRsrq)
                entity.snr = toIntSafe(cell.signal.ssSinr)
            }
            is CellTdscdma -> {
                entity.type = "3G"
                entity.lac = toIntSafe(cell.lac)
                entity.cid = toIntSafe(cell.cid)
                entity.rssi = toIntSafe(cell.signal.rssi)
            }
        }
    }
}
