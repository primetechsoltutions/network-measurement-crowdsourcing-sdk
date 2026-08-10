# Network Measurement SDK Summary

This document summarizes the Network Measurement Crowdsourcing SDK based on the current `network-sdk` source code.

## Platform / Language

The SDK is built as a native Android library module named `network-sdk`. It is written in Kotlin and uses Gradle Kotlin DSL for build configuration. The module targets Android API 36, supports devices from API 23, and compiles with Java/Kotlin JVM target 17.

The main SDK package is:

```text
com.ptsl.crowdsourcing_network_sdk
```

The SDK depends on Android platform services and common Android libraries. WorkManager runs the measurement job in the background, Room stores local retry data, Retrofit and OkHttp handle backend communication, Gson handles JSON serialization, Google Play Services Location provides location access, and NetMonster reads mobile radio/cell information.

The publish configuration defines these artifacts:

```text
Release: com.github.primetechsoltutions:crowdsourcing-network-measurement-sdk
Debug:   com.github.primetechsoltutions:crowdsourcing-network-measurement-sdk-debug
```

## SDK Process

The public entry point is `NetworkCrowdSourcingDataUploader`. The host app first initializes the SDK from an Activity or Fragment, then starts a crowdsourcing upload request.

Initialization prepares the SDK runtime. It stores lifecycle-safe weak references to the host screen, registers the permission handler, initializes the Room database, creates Retrofit services, creates local and remote repositories, and prepares the download/upload measurement helper.

```kotlin
val uploader = NetworkCrowdSourcingDataUploader()

uploader.init(
    activity = this,
    applicationName = "Host_App_Name"
)
```

After initialization, the host app starts a measurement request:

```kotlin
uploader.startCrowdSourcingUploading(
    msisdn = "019XXXXXXXX",
    integratedAppVersion = "1.0.0",
    sdkInitiateTimeStamp = "2026-08-09T10:30:00",
    integratedAppEventName = "Home_Screen_Load",
    userLatitude = 23.8103,
    userLongitude = 90.4125
) { success, status ->
    // SDK enqueue result
}
```

The SDK checks initialization, requests permissions when needed, saves the current SDK/auth state, creates a one-time WorkManager job, and immediately returns a callback indicating whether the task was enqueued. The callback does not mean the backend upload has already completed; it means the background worker has been scheduled.

The actual measurement runs inside `NetworkDataWorker`. The worker reads the input values, loads auth data from Room, collects GPS location when available, checks active connectivity, calculates RTT and latency, reads primary cell data using NetMonster, performs download/upload speed tests for the active mobile data cell, maps all values into `NetworkDataEntity`, merges fresh data with previously cached offline rows, and posts the final request to the backend.

If upload succeeds, cached measurement rows are cleared. If upload fails, fresh rows are marked as offline and saved locally for the next retry. If permissions, GPS, or cell data are unavailable, the SDK still creates fallback data with connection type, RTT, latency, and device information.

## Calculation

The SDK calculates three main measurement groups: RTT/latency, download/upload speed, and radio signal details.

RTT and latency are calculated in `calculateRttAndLatency`. The SDK sends HTTPS requests to the configured test URL currently used in the worker:

```text
https://crsrcgz.banglalink.net
```

OkHttp event timings are used to measure network timing. RTT is taken from TCP connect timing when available:

```text
RTT = connectEnd - connectStart
```

If connect timing is unavailable, RTT falls back to half of the request-to-response-header time:

```text
RTT = (responseHeadersStart - requestHeadersEnd) / 2
```

Latency is measured as time to first response header:

```text
Latency = responseHeadersStart - requestHeadersEnd
```

If the server returns an `X-Server-Processing-Time` header, that value is subtracted from raw latency so the SDK reports network latency instead of server processing time:

```text
Adjusted latency = raw latency - server processing time
```

The SDK takes three samples by default and uses the median of successful samples. If mobile internet is not available, RTT and latency return `0.0`.

Download and upload speed are calculated in `DownloadUploadHelper`. The SDK downloads a bandwidth payload from the server, measures elapsed time, and calculates speed from bytes transferred:

```text
Download speed Kbps = (downloaded bytes * 8 / 1000) / elapsed seconds
```

For upload, the SDK reuses the downloaded payload model, serializes it as JSON, posts it back to the server, and calculates upload speed with the same formula:

```text
Upload speed Kbps = (uploaded bytes * 8 / 1000) / elapsed seconds
```

For 4G cells, the SDK performs two download and two upload attempts. Other network types use one attempt. Final speed values are rounded to two decimal places.

Radio calculation and mapping are handled by `CellDataMapper`. NetMonster cell objects are converted into SDK fields based on radio type. For LTE, the SDK captures TAC, CID, eNB, PCI, timing advance, bandwidth, EARFCN, RSRP, RSRQ, SNR, CQI, and RSSI. For GSM, it captures LAC, CID, ARFCN, timing advance, RXLEV, BER, RXQUAL, and RSSI. For WCDMA, it captures LAC, CID, PSC, UARFCN, RSCP, EC/NO, and RSSI. For NR/5G, it captures TAC, PCI, SS-RSRP, SS-RSRQ, and SS-SINR.

The final measurement also includes device model, manufacturer, Android version, SIM count, connection type, user call state, transferred volume, and location.

## APIs Request / Response

The Retrofit base URL comes from Gradle property `BASE_URL`. The API token comes from Gradle property `TOKEN` and is serialized as `apiKey` inside `AuthEntity`.

The SDK exposes one public start method:

```kotlin
startCrowdSourcingUploading(
    msisdn: String,
    integratedAppVersion: String,
    sdkInitiateTimeStamp: String,
    integratedAppEventName: String,
    userLatitude: Double = 0.0,
    userLongitude: Double = 0.0,
    callback: (Boolean, NetworkDataCrowdSourcingStatus) -> Unit
)
```

A successful enqueue callback looks like this:

```json
{
  "success": true,
  "status": {
    "isSdkInit": true,
    "isLocationEnabled": true,
    "isPhoneStateGranted": true,
    "response": "{\"status\":\"Success\",\"statusCode\":200,\"message\":\"SDK Task enqueued\"}"
  }
}
```

If the SDK was not initialized, the callback is:

```json
{
  "success": false,
  "status": {
    "isSdkInit": false,
    "response": "{\"status\":\"Failed\",\"statusCode\":400,\"message\":\"SDK not initialized\"}"
  }
}
```

The main backend upload endpoint is:

```text
POST v903/UnifiedNetworkSDK/save-network-event-sdk-data
```

Request body:

```json
{
  "networkUserModel": {
    "apiKey": "TOKEN",
    "userType": 0,
    "sdkVersion": "1.0.0",
    "isSdkInitialized": true,
    "isLocationEnabled": true,
    "isPhoneStateEnabled": true,
    "hostAppName": "Host_App_Name"
  },
  "networkMeasurementRequestModel": [
    {
      "msisdn": "019XXXXXXXX",
      "sdkInitiateTimeStamp": "2026-08-09T10:30:00",
      "integratedAppVersion": "1.0.0",
      "integratedAppEventName": "Home_Screen_Load",
      "userLatitude": 23.8103,
      "userLongitude": 90.4125,
      "time": "10:30:00",
      "date": "09-08-2026",
      "mcc": "470",
      "mnc": "03",
      "type": "4G",
      "cid": 123456,
      "enb": 123,
      "pci": 10,
      "tac": 1001,
      "rsrp": -85,
      "rsrq": -12,
      "snr": 15,
      "cqi": 9,
      "rssi": -65,
      "longitude": 90.4125,
      "latitude": 23.8103,
      "ulspeed": 1500.25,
      "dlspeed": 8000.5,
      "data": "Mobile",
      "isDataCaptureOffline": false,
      "isUserDeviceOnCall": false,
      "deviceModel": "SM-G998B",
      "deviceManufacture": "Samsung",
      "deviceOsVersion": "36",
      "usedSimSlot": 2,
      "rtt": 35.0,
      "latency": 70.0,
      "totalDownloadVolume": 0.1,
      "totalUploadVolume": 0.1
    }
  ]
}
```

Response wrapper:

```json
{
  "Data": {
    "status": "Success",
    "testResult": null,
    "statusCode": 200,
    "message": "Saved successfully"
  },
  "Status": 200,
  "Message": "Success"
}
```

SDK logs are uploaded to:

```text
POST v903/UnifiedNetworkSDK/save-network-sdk-logs
```

Request body:

```json
{
  "networkUserModel": {
    "apiKey": "TOKEN",
    "userType": 0,
    "sdkVersion": "1.0.0",
    "isSdkInitialized": true,
    "isLocationEnabled": true,
    "isPhoneStateEnabled": true,
    "hostAppName": "Host_App_Name"
  },
  "unifiedNetworkLogs": [
    {
      "log_source": "Network Measurement SDK",
      "event_type": "NETWORK_REQUEST_FAILED",
      "title": "Network request failed",
      "description": "Upload failed",
      "status_code": 500,
      "status": "Failed",
      "message": "Network error",
      "stack_trace": "...",
      "os": "Android",
      "device_model": "SM-G998B",
      "msisdn": "019XXXXXXXX",
      "sdkInitiateTimeStamp": "2026-08-09T10:30:00",
      "integratedAppVersion": "1.0.0",
      "integratedAppEventName": "Home_Screen_Load",
      "userLatitude": 23.8103,
      "userLongitude": 90.4125
    }
  ]
}
```

Bandwidth test APIs are:

```text
GET  NetworkMesurment/GetBandwithFile?Size={networkType}
POST NetworkMesurment/SaveBandwithFile
```

The download API returns a `BaseResponse<BandWidth>` payload:

```json
{
  "Data": {
    "UploadFile": "server bandwidth payload",
    "Type": "4G"
  },
  "Status": 200,
  "Message": "Success"
}
```

The upload API receives the same model serialized as JSON and returns HTTP success when the upload is accepted.

## Local DB

The SDK uses Room database `network_db`, version `11`. The database contains three entities: `AuthEntity`, `NetworkDataEntity`, and `EventLogModel`.

`AuthEntity` stores the latest SDK auth and host app state. It contains the API key, user type, SDK version, SDK initialization flag, location permission/GPS state, phone-state permission state, and host application name.

`NetworkDataEntity` stores captured network measurements and also acts as the offline retry cache. It contains user/session information, user-provided coordinates, measured GPS coordinates, radio identity fields, radio signal fields, speed results, RTT, latency, transferred volume, device details, SIM count, connection type, user call state, and the `isDataCaptureOffline` flag.

The table has a unique index across:

```text
time, mnc, type, cid, lac, arfcn, tac, rssi
```

When backend upload fails, the SDK marks fresh rows as offline and stores them in Room. On the next upload, cached rows are merged with new rows and submitted together. The cache is deleted only after the backend returns a successful response.

`EventLogModel` stores SDK error and event logs. If log upload fails, the new log is cached locally. On the next log upload, cached logs are merged with the new log and sent together. Cached logs are cleared only after successful log upload.

## Final Result

The final SDK result is a complete network measurement package sent to the backend. It contains SDK auth state, host app metadata, user/session metadata, location, active connection type, cell identity, signal quality, RTT, latency, download speed, upload speed, transferred volume, and device information.

From the host app side, the immediate result is the SDK callback. This callback confirms whether the SDK was initialized and whether the WorkManager measurement task was successfully enqueued. The real measurement and server synchronization happen in the background.

When everything succeeds, measurement data is uploaded and local cached rows are cleared. When backend upload fails, the SDK keeps the data and logs locally for retry. When full cell data cannot be collected, the SDK still produces fallback output with available connection, timing, and device information.
