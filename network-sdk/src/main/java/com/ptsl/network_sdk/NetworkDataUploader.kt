package com.ptsl.network_sdk

import android.content.Context
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.google.gson.Gson
import com.ptsl.network_sdk.data_model.NetworkDataResponse
import com.ptsl.network_sdk.data_model.UploadStatus
import com.ptsl.network_sdk.data_model.entity.AuthEntity
import com.ptsl.network_sdk.network_data_worker.NetworkDataWorker
import com.ptsl.network_sdk.permission.PermissionHandler
import com.ptsl.network_sdk.utils.SdkContainer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.lang.ref.WeakReference

/**
 * Main entry point for the Network Measurement SDK. Handles initialization, permission requests,
 * and enqueueing measurement tasks.
 */
class NetworkDataUploader {
    private var activityRef: WeakReference<AppCompatActivity>? = null
    private var lifecycleOwnerRef: WeakReference<LifecycleOwner>? = null
    private lateinit var permissionHandler: PermissionHandler
    private lateinit var context: Context
    private lateinit var applicationName: String

    private val TAG = "NetworkDataUploader"

    private object InputKeys {
        const val MSISDN = "msisdn"
        const val INTEGRATED_APP_VERSION = "integratedAppVersion"
        const val SDK_INITIATE_TIMESTAMP = "sdkInitiateTimeStamp"
        const val INTEGRATED_APP_EVENT_NAME = "integratedAppEventName"
        const val SDK_VERSION = "sdkVersion"
        const val USER_LATITUDE = "userLatitude"
        const val USER_LONGITUDE = "userLongitude"
    }

    fun init(activity: AppCompatActivity, applicationName: String) {
        setup(activity, activity, PermissionHandler(activity), applicationName)
    }

    fun init(fragment: Fragment, applicationName: String) {
        val activity = fragment.activity as? AppCompatActivity ?: return
        setup(activity, fragment, PermissionHandler(fragment), applicationName)
    }

    private fun setup(
        activity: AppCompatActivity,
        owner: LifecycleOwner,
        permissionHandler: PermissionHandler,
        appName: String
    ) {
        this.activityRef = WeakReference(activity)
        this.lifecycleOwnerRef = WeakReference(owner)
        this.permissionHandler = permissionHandler
        this.context = activity.applicationContext
        this.applicationName = appName
        
        owner.lifecycle.addObserver(LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_DESTROY) {
                this.activityRef?.clear()
                this.activityRef = null
                this.lifecycleOwnerRef?.clear()
                this.lifecycleOwnerRef = null
            }
        })

        SdkContainer.init(this.context)
        Log.i(TAG, "SDK Initialized for $appName via ${owner::class.java.simpleName}")
    }

    /**
     * Starts the data collection and upload process.
     * @param msisdn User mobile number
     * @param integratedAppVersion Version of the host app
     * @param sdkInitiateTimeStamp Format: yyyy-MM-dd'T'HH:mm:ss
     * @param integratedAppEventName Unique name for the event
     * @param callback Result callback returning success status and details
     */
    fun startUploading(
        msisdn: String,
        integratedAppVersion: String,
        sdkInitiateTimeStamp: String,
        integratedAppEventName: String,
        userLatitude: Double = 0.0,
        userLongitude: Double = 0.0,
        callback: (Boolean, UploadStatus) -> Unit
    ) {
        if (!ensureSdkReady(callback)) return

        try {
            requestPermission { _ ->
                val scope = getSdkScopeOrFail(callback) ?: return@requestPermission
                scope.launch {
                    val auth = createAuthEntity()
                    SdkContainer.dao?.insertAuthData(auth)

                    enqueueNetworkDataWork(
                        auth,
                        msisdn,
                        integratedAppVersion,
                        sdkInitiateTimeStamp,
                        integratedAppEventName,
                        userLatitude,
                        userLongitude
                    )

                    successCallback(callback, "SDK Task enqueued")
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error starting upload process ${e.message}")
            failCallback(callback, "Error starting upload process")
        }
    }

    private fun ensureSdkReady(callback: (Boolean, UploadStatus) -> Unit): Boolean {
        if (!isSdkReady()) {
            Log.e(TAG, "SDK not initialized. Call init() first.")
            failCallback(callback, "SDK not initialized")
            return false
        }
        return true
    }

    private fun getSdkScopeOrFail(callback: (Boolean, UploadStatus) -> Unit): CoroutineScope? {
        val scope = SdkContainer.coroutineScope
        if (scope == null) {
            failCallback(callback, "SDK not initialized")
            return null
        }
        return scope
    }

    private fun successCallback(callback: (Boolean, UploadStatus) -> Unit, message: String) {
        dispatchCallback(
            callback,
            true,
            createSuccessStatus(NetworkDataResponse(message = message))
        )
    }

    private fun failCallback(
        callback: (Boolean, UploadStatus) -> Unit,
        message: String,
        statusCode: Int = 400
    ) {
        dispatchCallback(
            callback,
            false,
            createFailureStatus(message, statusCode)
        )
    }

    private fun isSdkReady(): Boolean {
        return this::permissionHandler.isInitialized && SdkContainer.isInitialized()
    }

    private fun dispatchCallback(
        callback: (Boolean, UploadStatus) -> Unit, success: Boolean, status: UploadStatus
    ) {
        SdkContainer.coroutineScope?.launch {
            withContext(Dispatchers.Main) {
                if (!isLifecycleOwnerValid()) return@withContext
                callback(success, status)
            }
        }
    }

    private fun isLifecycleOwnerValid(): Boolean {
        if (activityRef?.get() == null || lifecycleOwnerRef?.get() == null) {
            Log.w(TAG, "Host component is no longer valid or destroyed. Skipping callback.")
            return false
        }
        return true
    }

    private fun createAuthEntity() = AuthEntity(
        sdkVersion = BuildConfig.SdkVersion,
        isSdkInitialized = this::permissionHandler.isInitialized,
        isLocationEnabled = permissionHandler.isLocationPermissionGranted() && permissionHandler.isGpsEnabled(),
        isPhoneStateEnabled = permissionHandler.isPhoneStatePermissionGranted(),
        hostAppName = applicationName
    )

    private fun createSuccessStatus(
        networkDataResponse: NetworkDataResponse = NetworkDataResponse(
            status = "Failed", statusCode = 400, message = ""
        )
    ): UploadStatus {
        return UploadStatus(
            isSdkInit = this::permissionHandler.isInitialized,
            isLocationEnabled = permissionHandler.isLocationPermissionGranted(),
            isPhoneStateGranted = permissionHandler.isPhoneStatePermissionGranted(),
            response = Gson().toJson(networkDataResponse)
        )
    }

    private fun createFailureStatus(message: String, statusCode: Int = 400): UploadStatus {
        return UploadStatus(
            isSdkInit = this::permissionHandler.isInitialized,
            response = Gson().toJson(
                NetworkDataResponse(
                    status = "Failed",
                    statusCode = statusCode,
                    message = message
                )
            )
        )
    }

    /** Internal helper to request necessary permissions. */
    private fun requestPermission(callback: (Boolean) -> Unit) {
        if (this::permissionHandler.isInitialized) {
            if (permissionHandler.isPermissionGranted()) {
                callback(true)
            } else {
                permissionHandler.requestPermission(callback = callback)
            }
        } else {
            callback(false)
        }
    }

    private fun enqueueNetworkDataWork(
        authEntity: AuthEntity,
        msisdn: String,
        integratedAppVersion: String,
        sdkInitiateTimeStamp: String,
        integratedAppEventName: String,
        userLatitude: Double,
        userLongitude: Double,
    ): java.util.UUID {

        try {
            val inputData = workDataOf(
                InputKeys.MSISDN to msisdn,
                InputKeys.INTEGRATED_APP_VERSION to integratedAppVersion,
                InputKeys.SDK_INITIATE_TIMESTAMP to sdkInitiateTimeStamp,
                InputKeys.INTEGRATED_APP_EVENT_NAME to integratedAppEventName,
                InputKeys.SDK_VERSION to authEntity.sdkVersion,
                InputKeys.USER_LATITUDE to userLatitude,
                InputKeys.USER_LONGITUDE to userLongitude
            )

            val workRequest =
                OneTimeWorkRequestBuilder<NetworkDataWorker>().setInputData(inputData).build()

            WorkManager.getInstance(context).enqueue(workRequest)
            Log.d(TAG, "Enqueued NetworkDataCapture with ID: ${workRequest.id}")
            return workRequest.id
        } catch (e: Exception) {
            Log.e(TAG, "Failed to enqueue work: ${e.message}")
            return java.util.UUID(0, 0)
        }
    }
}