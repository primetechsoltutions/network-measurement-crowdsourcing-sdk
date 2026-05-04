# Network Measurement SDK & Host App - Developer Test Cases

This document outlines the required developer test cases to verify the stability, reliability, and correct integration of the Network Measurement SDK within the host application.

| Category | Test Case ID | Description | Pre-conditions | Action | Expected Result |
| :--- | :--- | :--- | :--- | :--- | :--- |
| **Initialization** | **INIT-01** | Correct Initialization | App launched | Call `NetworkDataUploader().init()` inside `onCreate()` of Activity/Fragment. | SDK initializes successfully without crashing. |
| **Initialization** | **INIT-02** | Late Initialization (Error Handling) | App launched | Call `init()` inside a button click listener (after component reaches `STARTED` state). | App throws `IllegalStateException` or SDK handles it gracefully based on integration. |
| **Initialization** | **INIT-03** | Missing WorkManager Provider | `tools:node="remove"` added to manifest | Launch app and initialize SDK. | SDK gracefully falls back to default WorkManager initialization, preventing crashes. |
| **Initialization** | **INIT-04** | Custom WorkManager Config | Host app implements `Configuration.Provider` | Launch app and initialize SDK. | SDK utilizes the host app's custom WorkManager configuration without conflicts. |
| **Standard Capture** | **STD-01** | Standard Capture Success | SDK initialized | Trigger `startUploading` with `UploadType.NetworkDataCapture`. | Callback returns `{ "message": "SDK Task enqueued" }`. Background worker executes. |
| **Standard Capture** | **STD-02** | Capture Without Init | SDK **not** initialized | Trigger standard capture. | Callback returns status `Failed` and message `SDK not initialized`. |
| **Standard Capture** | **STD-03** | Network Disconnect | SDK initialized, Network OFF | Trigger standard capture. | Task is enqueued successfully. WorkManager delays execution until the network is restored. |
| **Standard Capture** | **STD-04** | App Killed During Capture | SDK initialized | Trigger standard capture, then swipe away app. | WorkManager preserves the job and executes it the next time network constraints are met. |
| **FTP Capture** | **FTP-01** | FTP Capture Success | SDK initialized, Network ON | Trigger `startUploading` with `UploadType.FTPNetworkDataCapture`. | Callback returns `Success` with test metrics within ~60 seconds. |
| **FTP Capture** | **FTP-02** | Capture Without Init | SDK **not** initialized | Trigger FTP capture. | Callback returns status `Failed` and message `SDK not initialized`. |
| **FTP Capture** | **FTP-03** | Network Timeout | SDK initialized, bad network | Trigger FTP capture. | Callback returns `Failed` after reaching the timeout limit (e.g., 60s) without crashing. |
| **FTP Capture** | **FTP-04** | Lifecycle Navigation | SDK initialized | Trigger FTP capture, then navigate away. | SDK safely detects detached state, cancels UI updates, and avoids `IllegalStateException`. |
| **FTP Capture** | **FTP-05** | Rapid Successive Triggers | SDK initialized | Tap FTP capture trigger 5 times rapidly. | SDK handles concurrent calls gracefully without crashing or OOM errors. |
| **Edge Cases** | **PERM-01** | Permission Granted | First launch | Trigger capture, click "Allow". | Test proceeds and captures location data successfully. |
| **Edge Cases** | **PERM-02** | Permission Denied | First launch | Trigger capture, click "Deny". | Test proceeds gracefully without location data or fails cleanly (no crash). |
| **Edge Cases** | **PERM-03** | "Don't Ask Again" | Permission permanently denied | Trigger capture. | Dialog hidden. SDK proceeds via handler gracefully. |
| **Edge Cases** | **EDGE-01** | Obfuscation (R8/ProGuard) | Release build (Minified) | Trigger FTP capture. | JSON response maintains correct keys (`cellName`, `RSRP`) and is not minified to `a`, `b`. |
| **Edge Cases** | **EDGE-02** | Invalid Payload Data | SDK initialized | Pass empty strings/nulls for required fields. | SDK handles validation safely and returns a clean `Failed` callback. |
| **UI/UX Tests** | **UI-01** | Loading State | SDK initialized | Trigger FTP capture. | Host app displays a non-blocking loading spinner while waiting for callback. |
| **UI/UX Tests** | **UI-02** | Success Display | FTP Capture triggered | Wait for `Success` callback. | Host app displays speeds and results visually based on `status.response`. |
| **UI/UX Tests** | **UI-03** | Error State | Network off | Trigger FTP capture. | Host app displays an appropriate error UI/Snackbar using the callback's `Failed` message. |
