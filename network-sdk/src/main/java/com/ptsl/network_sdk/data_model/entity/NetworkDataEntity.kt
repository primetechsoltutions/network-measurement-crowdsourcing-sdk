package com.ptsl.network_sdk.data_model.entity

import android.os.Parcelable
import androidx.annotation.Keep
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.google.gson.annotations.SerializedName
import kotlinx.parcelize.Parcelize

@Parcelize
@Entity(
    indices = [Index(
        value = ["time", "mnc", "type", "cid", "lac", "arfcn", "tac", "rssi"],
        unique = true
    )]
)
@Keep
data class NetworkDataEntity(
    @PrimaryKey(autoGenerate = true) var id: Int = 0,

    @SerializedName("msisdn") var msisdn: String = "",
    @SerializedName("sdkInitiateTimeStamp") var sdkInitiateTimeStamp: String = "",
    @SerializedName("integratedAppVersion") var integratedAppVersion: String = "",
    @SerializedName("integratedAppEventName") var integratedAppEventName: String = "Event",
    @SerializedName("userLatitude") var userLatitude: Double = 0.0,
    @SerializedName("userLongitude") var userLongitude: Double = 0.0,


    @SerializedName("time") var time: String = "00:00:00",
    @SerializedName("date") var date: String = "00-00-2000",
    @SerializedName("mcc") var mcc: String = "000",
    @SerializedName("mnc") var mnc: String = "00",
    @SerializedName("lac") var lac: Int? = null,
    @SerializedName("tac") var tac: Int? = null,
    @SerializedName("type") var type: String? = null,
    @SerializedName("cid") var cid: Int? = null,
    @SerializedName("enb") var enb: Int? = null,
    @SerializedName("psc") var psc: Int? = null,
    @SerializedName("pci") var pci: Int? = null,
    @SerializedName("ta") var ta: Int? = null,
    @SerializedName("bw") var bw: Int? = null,
    @SerializedName("arfcn") var arfcn: Int? = null,
    @SerializedName("band") var band: String? = null,
    @SerializedName("rxlev") var rxlev: Int? = null,
    @SerializedName("rxQual") var rxQual: Int? = null,
    @SerializedName("rscp") var rscp: Int? = null,
    @SerializedName("ecNo") var ecNo: Int? = null,
    @SerializedName("rsrp") var rsrp: Int? = null,
    @SerializedName("rsrq") var rsrq: Int? = null,
    @SerializedName("snr") var snr: Int? = null,
    @SerializedName("cqi") var cqi: Int? = null,
    @SerializedName("rssi") var rssi: Int? = null,
    @SerializedName("longitude") var longitude: Double? = null,
    @SerializedName("latitude") var lattitude: Double? = null,
    @SerializedName("ulspeed") var ulspeed: Double? = null,
    @SerializedName("dlspeed") var dlspeed: Double? = null,
    @SerializedName("data") var data: String? = null,
    @SerializedName("bitRateError") var bitRateError: Int? = null,
    @SerializedName("inDoor") var inDoor: Boolean = false,
    @SerializedName("outDoor") var outDoor: Boolean = false,
    @SerializedName("deviceModel") var deviceModel: String? = null,
    @SerializedName("superOfficeTicket") var superOfficeTicket: Int? = null,
    @SerializedName("isDataCaptureOffline") var isDataCaptureOffline: Boolean = false,
    @SerializedName("isUserDeviceOnCall") var isUserDeviceOnCall: Boolean = false,
    @SerializedName("deviceManufacture") var deviceManufacture: String? = null,
    @SerializedName("deviceOsVersion") var deviceOsVersion: String? = null,
    @SerializedName("usedSimSlot") var usedSimSlot: Int? = null,
    @SerializedName("rtt") var rtt: Double? = null,
    @SerializedName("latency") var latency: Double? = null,
    @SerializedName("totalDownloadVolume") var totalDownloadVolume: Double? = null,
    @SerializedName("totalUploadVolume") var totalUploadVolume: Double? = null
) : Parcelable