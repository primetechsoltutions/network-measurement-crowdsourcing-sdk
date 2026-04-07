# Network Measurement SDK - AI Context & Architecture Blueprint

This document provides critical context for any AI agent or developer working on this repository. **Read this entirely before suggesting or making changes to the codebase.**

## 1. Project Overview
This repository contains the **Network Measurement Unified SDK**. It is a plug-and-play Android SDK designed to be integrated into host applications. Its primary goal is to collect detailed network diagnostics (Wi-Fi metrics, Cell Tower data, FTP Upload/Download speeds, Latency, RTT, and GPS location) autonomously in the background.

There are two primary upload flows:
1. **NetworkDataCapture:** Standard, passive background collection (GPS prompt requested max once a day).
2. **FTPNetworkDataCapture:** Diagnostic, user-initiated collection (Forces GPS/Permission prompts every time).

## 2. Core Architectural Principles
Because this is an SDK (and not a standalone App), it operates in a hostile environment where the Host App controls the UI lifecycle. **The SDK must remain entirely self-contained and invisible to the Host App.**

*   **WorkManager:** All network API calls, FTP diagnostic tasks, and large data-processing jobs **must** be executed via `WorkManager`. This guarantees the tasks complete even if the host app is killed or minimized.
*   **Coroutines:** Used heavily for background task orchestration (e.g., Room Database I/O). Never block the main thread.
*   **Headless Fragments:** We use an invisible Fragment (`PermissionFragment`) injected into the Host App's `FragmentManager` to handle permissions via `ActivityResultContracts`. This prevents us from having to force Host App developers to write `onActivityResult` overrides.

## 3. 🚨 Critical Rules for Code Modification
To prevent introducing crashes into Host Apps, you **must** follow these strict rules when modifying this codebase:

### A. Lifecycle Survival & Crash Prevention
*   **Never trust the Activity Reference:** The host app can destroy the activity at any millisecond. 
*   **Always use WeakReferences:** Store the host activity as `WeakReference<AppCompatActivity>`.
*   **Always check state before UI actions:** Before calling `runOnUiThread`, `postDelayed`, or interacting with the UI, you **must** verify the activity is valid and not dying:
    ```kotlin
    val activity = activityRef?.get()
    if (activity != null && !activity.isFinishing && !activity.isDestroyed) { 
        // safe to update UI / post delayed
    }
    ```
*   **Fragment Transactions:** When injecting the `PermissionFragment`, use `commitNowAllowingStateLoss()` to prevent asynchronous attachment crashes and `IllegalStateException` during state saving.
*   **Check Fragment state:** Before calling `launch()` on an `ActivityResultLauncher` inside the Fragment, you **must** check `if (isAdded)`.

### B. Graceful Degradation (Zero Host Crashes)
*   The SDK must **never** cause the Host App to crash.
*   Wrap all `WorkManager.enqueue()` calls, Room Database `dao.insert()` calls, and `window.decorView.postDelayed` calls in protective `try-catch` blocks.
*   If an exception occurs, swallow it, log a warning, and fire the `callback(false)` back to the host app so it doesn't hang.

### C. Security and Data Privacy
*   **PII & Logging:** MSISDN (Phone Numbers) and GPS coordinates are considered sensitive Personally Identifiable Information (PII). Never print these in production Logcat.
*   **No Body Logging:** Do not enable `HttpLoggingInterceptor.Level.BODY` in production builds.
*   **API Keys:** Tokens and Base URLs should never be committed into Git via `gradle.properties` or source code.

## 4. Known Edge Cases
*   **SystemJobService Cold Starts:** WorkManager can spin up your worker (`NetworkDataWorker`) before the host app calls `NetworkDataUploader.init()`. The `SdkWorkerFactory` catches the resulting `UninitializedPropertyAccessException` and safely suppresses the crash.
*   **NPE on window.decorView:** Using `activity.window.decorView.postDelayed()` for timeouts is great for avoiding memory leaks, but highly susceptible to NullPointerExceptions if the activity is destroyed between your null-check and the property access. Always wrap these in try-catch.

---
**Agent Instruction:** Do not override these architectural choices. If asked to refactor UI callbacks or lifecycle handlers, strictly maintain the `WeakReference` and `try-catch` safety constraints defined above.
