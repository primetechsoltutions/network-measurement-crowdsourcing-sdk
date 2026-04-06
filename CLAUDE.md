# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

---

## Project Overview

This is a Network Measurement SDK for Android (`network-sdk/` module) that collects network metrics, signal strength, location data, and performs bandwidth tests. The `app/` module is a dummy/demo application for testing and **should not be modified** for production use. Host applications will integrate directly with the `network-sdk/` module.

### Module Structure

- **`network-sdk/`** - The actual SDK library (all production code lives here)
- **`app/`** - Demo/test application only (do NOT modify for client use)

---

## Build Commands

### Build the SDK
```bash
./gradlew :network-sdk:assembleDebug
./gradlew :network-sdk:assembleRelease
```

### Build the demo app
```bash
./gradlew :app:assembleDebug
```

### Run tests
```bash
# All tests
./gradlew test

# SDK tests only
./gradlew :network-sdk:test

# Connected Android tests
./gradlew connectedAndroidTest
```

### Lint
```bash
./gradlew lint
```

### Clean build
```bash
./gradlew clean
```

---

## Architecture Overview

### SDK Initialization Flow

```
Host App
    ↓
NetworkDataUploader.init(activity, appName)
    ↓
NetworkSdk.init(context)
    ↓
SdkContainer.init(context) → Room DB, DAO, CoroutineScope, ApiService
    ↓
SdkManager.init(context) → WorkManager with custom WorkerFactory
```

**Critical:** `SdkContainer` holds `lateinit` properties that are NOT safe to access before initialization. Always check initialization status before use.

### Data Flow

1. **Standard Network Capture** (`UploadType.NetworkDataCapture`)
   - `NetworkDataWorker` runs in background
   - Collects signal metrics, RTT, latency via NetMonster
   - Uploads to backend, caches locally on failure

2. **FTP Network Capture** (`UploadType.FTPNetworkDataCapture`)
   - `FTPNetworkDataWorker` runs with 60s timeout
   - Pre-flight checks (4G, Banglalink SIM, mobile data only)
   - Download/upload speed test
   - Returns qualitative assessment to host app

### Key Components

| Component | Purpose | Important Notes |
|-----------|---------|-----------------|
| `NetworkSdk` | Singleton entry point | Must call `init()` before any SDK operations |
| `SdkContainer` | Holds DB, DAO, API, CoroutineScope | All properties are `lateinit` - crash if accessed before init |
| `SdkWorkerFactory` | Creates workers with SDK dependencies | Checks `SdkContainer.isInitialized()` before creating workers |
| `CheckPermissionHandler` | Handles location/phone state permissions | Uses `WeakReference<AppCompatActivity` |
| `NetworkDataUploader` | Public API for host apps | `init()` then `startUploading()` |

---

## Known Issues & Safety Patterns

### Unsafe Operations to Avoid

1. **`getSystemService()` unsafe casts** - Many places use `as LocationManager` or `as ConnectivityManager` which throws `ClassCastException`. Use `as?` safe cast instead.

2. **`toInt()` on strings** - `DownloadUploadHelper.kt:31` uses `currentMnc?.toInt()` which throws `NumberFormatException` on non-numeric strings. Use `toIntOrNull()`.

3. **`SimpleDateFormat.parse()`** - Returns `Date?` nullable. Code in `Extension.kt:31` assigns to non-nullable `Date` type. Add null check.

4. **Uninitialized `lateinit` access** - `SdkContainer` properties crash if accessed before `init()` completes. Always add `isInitialized()` check.

### Data Storage

- **Database:** Room database at `network_db` - uses `fallbackToDestructiveMigration()` (deletes all data on schema change)
- **WorkManager:** InputData contains PII (MSISDN, location) stored in plaintext
- **No encryption:** Database is not encrypted (consider SQLCipher for production)

### Security

- **API Token:** Currently in `gradle.properties` - should be moved to environment variables
- **PII Logging:** MSISDN and location data may be logged - implement secure logging wrapper
- **HTTP Headers:** Auth token sent in request body (not standard) - consider moving to headers

---

## WorkManager Integration

Workers are created via `SdkWorkerFactory` which injects SDK dependencies:

```kotlin
// SdkWorkerFactory.kt
NetworkDataWorker(
    appContext, workerParams,
    SdkContainer.apiService,      // Lateinit - crash if not initialized
    SdkContainer.downloadUploadHelper,
    SdkContainer.dao
)
```

**Edge case:** Workers can be scheduled by system before SDK initializes (after app update, reboot). Factory checks `isInitialized()` and returns `null` if not ready.

---

## Permission Handling

The SDK requires:
- `ACCESS_FINE_LOCATION` / `ACCESS_COARSE_LOCATION`
- `READ_PHONE_STATE`

`CheckPermissionHandler` manages permission requests and GPS enablement prompts. GPS prompt logic differs by capture type:
- **Standard:** Once per day
- **FTP:** Every time (forced prompt)

---

## Network Layer

- **Retrofit** + **OkHttp** for API calls
- **Gson** for JSON serialization
- **Base URL:** Configured via `BuildConfig.BASE_URL` from `gradle.properties`
- **Timeout:** 60 seconds for connect/read/write

### API Endpoints

- `POST /v903/UnifiedNetworkSDK/save-network-event-sdk-data` - Standard network data
- `POST /v903/blWifiDeviceNetworkAssessments/save-network-assessment-data` - FTP assessment
- `POST /v903/UnifiedNetworkSDK/save-network-sdk-logs` - Event logs
- `GET /NetworkMesurment/GetBandwidthFile` - Bandwidth test file
- `POST /NetworkMesurment/SaveBandwidthFile` - Upload test results

---

## Dependencies (from libs.versions.toml)

- **NetMonster** (`app.netmonster:core`) - Cell information
- **Room** 2.8.4 - Local database
- **WorkManager** 2.11.1 - Background work
- **Retrofit** 3.0.0 + **OkHttp** 5.1.0 - Networking
- **Play Services Location** 21.3.0 - Location services
- **Coroutines** 1.10.2 - Async operations

---

## Testing Reports

Several analysis reports exist in the repository root:
- `CRASH_ANALYSIS_REPORT.md` - Exception handling issues
- `NETWORK_SDK_FUNCTION_CRASH_ANALYSIS.md` - Function return type analysis
- `SECURITY_ANALYSIS_REPORT.md` - Security vulnerabilities
- `HOST_APP_INTEGRATION_GUIDE.md` - Integration guidance for host apps

These reports document known issues that need addressing before production use.

---

## Publishing

The SDK is published to JitPack via Maven publishing. Version is controlled by `SdkVersion` in `gradle.properties`.

---

## Important: Focus Changes on `network-sdk/` Only

When making changes, focus exclusively on the `network-sdk/` module. The `app/` module is for demonstration/testing only and will not be used by client applications. All fixes, features, and improvements must be implemented in the SDK module itself.
