# Network Measurement SDK - Integration README

## 🚀 Quick Start

### 1. Implementation
Add the SDK dependency to your `build.gradle`:
```gradle
dependencies {
    implementation("com.github.primetechsoltutions:network-measurement-unified-sdk:X.X.X")
}
```

### 2. Initialization
Initialize the SDK in your `Activity` or `Fragment`:
```kotlin
val uploader = NetworkDataUploader(this) // Pass Activity context
uploader.initialize(
    msisdn = "01XXXXXXXXX",
    hostAppName = "Toffee",
    integratedAppVersion = "2.4.5"
)
```

### 3. Usage
- **Background Capture**: `uploader.initiateNetworkCapture(eventName = "Home_Load")`
- **FTP Diagnostic**: `uploader.initiateFTPNetworkDataCapture(eventName = "Live_Stream_Check")`

## 📄 Documentation
For detailed response formats and configuration, please refer to:
[SDK Integration Guide](sdk_integration_guide.md)

## ⚠️ Requirements
- Active Network (Mobile Data preferred for diagnostics)
- Battery Not Low
- Location Permissions (ACCESS_FINE_LOCATION)
