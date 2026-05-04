package com.ptsl.crowdsourcing_network_sdk

import android.content.Context
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.lifecycle.LifecycleOwner
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.google.gson.Gson
import com.ptsl.crowdsourcing_network_sdk.data_model.NetworkDataResponse
import com.ptsl.crowdsourcing_network_sdk.data_model.NetworkDataCrowdSourcingStatus
import com.ptsl.crowdsourcing_network_sdk.data_model.entity.AuthEntity
import com.ptsl.crowdsourcing_network_sdk.network_data_worker.NetworkDataWorker
import com.ptsl.crowdsourcing_network_sdk.network_data_worker.WorkerInputKeys
import com.ptsl.crowdsourcing_network_sdk.utils.CheckPermissionHandler

import com.ptsl.crowdsourcing_network_sdk.utils.SdkContainer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.lang.ref.WeakReference

/**
 * Main entry point for the Network Measurement SDK. Handles initialization, permission requests,
 * and enqueueing measurement tasks.
 */
class NetworkCrowdSourcingDataUploader {
    private var activityRef: WeakReference<AppCompatActivity>? = null
    private var lifecycleOwnerRef: WeakReference<LifecycleOwner>? = null
    private lateinit var checkPermissionHandler: CheckPermissionHandler
    private lateinit var context: Context
    private lateinit var applicationName: String

    private val TAG = "NetworkDataUploader"
    private val gson = Gson()


    fun init(activity: AppCompatActivity, applicationName: String) {
        setup(activity, activity, CheckPermissionHandler(activity), applicationName)
    }

    fun init(fragment: Fragment, applicationName: String) {
        val activity = fragment.activity as? AppCompatActivity ?: return
        setup(activity, fragment, CheckPermissionHandler(fragment), applicationName)
    }

    private fun setup(
        activity: AppCompatActivity,
        owner: LifecycleOwner,
        permissionHandler: CheckPermissionHandler,
        appName: String
    ) {
        this.activityRef = WeakReference(activity)
        this.lifecycleOwnerRef = WeakReference(owner)
        this.checkPermissionHandler = permissionHandler
        this.context = activity.applicationContext
        this.applicationName = appName
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
    fun startCrowdSourcingUploading(
        msisdn: String,
        integratedAppVersion: String,
        sdkInitiateTimeStamp: String,
        integratedAppEventName: String,
        userLatitude: Double = 0.0,
        userLongitude: Double = 0.0,
        callback: (Boolean, NetworkDataCrowdSourcingStatus) -> Unit
    ) {
        if (!this::checkPermissionHandler.isInitialized || !SdkContainer.isInitialized()) {
            Log.e(TAG, "SDK not initialized. Call init() first.")
            callback(
                false, NetworkDataCrowdSourcingStatus(
                    isSdkInit = false, response = gson.toJson(
                        NetworkDataResponse(
                            status = "Failed", statusCode = 400, message = "SDK not initialized"
                        )
                    )
                )
            )

            return
        }

        try {
            requestPermission { isGranted ->
                SdkContainer.coroutineScope?.launch {
                    val auth = createAuthEntity()
                    SdkContainer.localCacheRepository?.saveAuth(auth)


                    enqueueNetworkDataWork(
                        auth,
                        msisdn,
                        integratedAppVersion,
                        sdkInitiateTimeStamp,
                        integratedAppEventName,
                        userLatitude,
                        userLongitude
                    )

                    dispatchCallback(
                        callback, true, buildCrowdSourcingStatus(
                            NetworkDataResponse(
                                statusCode = 200,
                                message = "SDK Task enqueued"
                            )
                        )
                    )

                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error starting upload process ${e.message}")
            dispatchCallback(
                callback, false, buildCrowdSourcingStatus(
                    NetworkDataResponse(
                        status = "Failed",
                        statusCode = 400,
                        message = "Error starting uploading process"
                    )
                )
            )

        }
    }


    private fun dispatchCallback(
        callback: (Boolean, NetworkDataCrowdSourcingStatus) -> Unit, success: Boolean, status: NetworkDataCrowdSourcingStatus
    ) {
        SdkContainer.coroutineScope?.launch {
            withContext(Dispatchers.Main) {
                if (!isLifecycleOwnerValid()) return@withContext
                callback(success, status)
            }
        }
    }

    private fun isLifecycleOwnerValid(): Boolean {
        val activity = activityRef?.get()
        if (activity == null || activity.isFinishing || activity.isDestroyed) {
            Log.w(TAG, "Host Activity is no longer valid. Skipping callback.")
            return false
        }

        val owner = lifecycleOwnerRef?.get()
        if (owner == null) {
            Log.w(TAG, "LifecycleOwner reference lost. Skipping callback.")
            return false
        }

        if (owner is Fragment) {
            if (!owner.isAdded || owner.isDetached || owner.viewLifecycleOwnerLiveData.value == null) {
                Log.w(
                    TAG,
                    "Host Fragment is no longer valid (detached or removed). Skipping callback."
                )
                return false
            }
        }
        return true
    }

    private fun createAuthEntity() = AuthEntity(
        sdkVersion = BuildConfig.SdkVersion,
        isSdkInitialized = this::checkPermissionHandler.isInitialized,
        isLocationEnabled = checkPermissionHandler.isLocationPermissionGranted() && checkPermissionHandler.isGpsEnabled(),
        isPhoneStateEnabled = checkPermissionHandler.isPhoneStatePermissionGranted(),
        hostAppName = applicationName
    )

    private fun buildCrowdSourcingStatus(
        networkDataResponse: NetworkDataResponse = NetworkDataResponse(
            status = "Failed", statusCode = 400, message = ""
        )
    ): NetworkDataCrowdSourcingStatus {
        return NetworkDataCrowdSourcingStatus(
            isSdkInit = this::checkPermissionHandler.isInitialized,
            isLocationEnabled = checkPermissionHandler.isLocationPermissionGranted(),
            isPhoneStateGranted = checkPermissionHandler.isPhoneStatePermissionGranted(),
            response = gson.toJson(networkDataResponse)
        )
    }


    /** Internal helper to request necessary permissions. */
    private fun requestPermission(callback: (Boolean) -> Unit) {
        if (this::checkPermissionHandler.isInitialized) {
            if (checkPermissionHandler.isPermissionGranted()) {
                callback(true)
            } else {
                checkPermissionHandler.requestPermission(callback = callback)
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
                WorkerInputKeys.MSISDN to msisdn,
                WorkerInputKeys.INTEGRATED_APP_VERSION to integratedAppVersion,
                WorkerInputKeys.SDK_INITIATE_TIMESTAMP to sdkInitiateTimeStamp,
                WorkerInputKeys.INTEGRATED_APP_EVENT_NAME to integratedAppEventName,
                WorkerInputKeys.SDK_VERSION to authEntity.sdkVersion,
                WorkerInputKeys.USER_LATITUDE to userLatitude,
                WorkerInputKeys.USER_LONGITUDE to userLongitude
            )


            val workRequest =
                OneTimeWorkRequestBuilder<NetworkDataWorker>().setInputData(inputData).build()

            WorkManager.getInstance(context).enqueue(workRequest)
            return workRequest.id
        } catch (e: Exception) {
            Log.e(TAG, "Failed to enqueue work: ${e.message}")
            return java.util.UUID(0, 0)
        }
    }
}

