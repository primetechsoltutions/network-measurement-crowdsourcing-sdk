package com.ptsl.crowdsourcing_network_sdk

import android.content.Context
import com.ptsl.crowdsourcing_network_sdk.utils.SdkLogger as Log
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
import com.ptsl.crowdsourcing_network_sdk.utils.CommonUtils

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
    private enum class InitMode {
        UI,
        BACKGROUND
    }

    private var activityRef: WeakReference<AppCompatActivity>? = null
    private var lifecycleOwnerRef: WeakReference<LifecycleOwner>? = null
    private var initMode: InitMode? = null
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

    /**
     * Initializes the SDK without binding to an Activity or Fragment.
     *
     * This flow is intended for host service/background layers. It validates required runtime
     * permissions only and never attempts to request permissions or resolve GPS settings.
     */
    fun init(context: Context, applicationName: String): NetworkDataCrowdSourcingStatus {
        initMode = null
        val appContext = context.applicationContext
        val validation = CheckPermissionHandler.validateRequiredPermissions(appContext)
        if (!validation.isGranted) {
            Log.e(
                TAG,
                "SDK background initialization failed. Missing permissions: ${validation.missingPermissions}"
            )
            return buildInitializationStatus(
                appContext,
                isInitialized = false,
                NetworkDataResponse(
                    status = "Failed",
                    statusCode = 403,
                    message = "Missing required permissions: ${validation.missingPermissions.joinToString()}"
                )
            )
        }
        if (!CommonUtils.isGpsEnabled(appContext)) {
            Log.e(TAG, "SDK background initialization failed. Location services are disabled.")
            return buildInitializationStatus(
                appContext,
                isInitialized = false,
                NetworkDataResponse(
                    status = "Failed",
                    statusCode = 412,
                    message = "Location services are disabled"
                )
            )
        }

        this.activityRef = null
        this.lifecycleOwnerRef = null
        this.context = appContext
        this.applicationName = applicationName
        this.initMode = InitMode.BACKGROUND
        SdkContainer.init(appContext)

        if (!SdkContainer.isInitialized()) {
            initMode = null
            Log.e(TAG, "SDK background initialization failed. Container was not initialized.")
            return buildInitializationStatus(
                appContext,
                isInitialized = false,
                NetworkDataResponse(
                    status = "Failed",
                    statusCode = 500,
                    message = "SDK initialization failed"
                )
            )
        }

        Log.i(TAG, "SDK Initialized for $applicationName via background context")
        return buildInitializationStatus(
            appContext,
            isInitialized = true,
            NetworkDataResponse(
                status = "Success",
                statusCode = 200,
                message = "SDK initialized"
            )
        )
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
        this.initMode = InitMode.UI
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
        if (!isInitializedForStart()) {
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
                if (!isGranted) {
                    dispatchCallback(
                        callback,
                        false,
                        buildCrowdSourcingStatus(
                            NetworkDataResponse(
                                status = "Failed",
                                statusCode = 403,
                                message = "Required permissions are not available"
                            )
                        )
                    )
                    return@requestPermission
                }

                SdkContainer.coroutineScope?.launch {
                    val auth = createAuthEntity()
                    SdkContainer.dataFacade?.saveAuth(auth)


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
                                status = "Success",
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
        val scope = SdkContainer.coroutineScope
        if (scope == null) {
            callback(success, status)
            return
        }

        scope.launch {
            withContext(Dispatchers.Main) {
                if (!isLifecycleOwnerValid()) return@withContext
                callback(success, status)
            }
        }
    }

    private fun isLifecycleOwnerValid(): Boolean {
        if (initMode == InitMode.BACKGROUND) return true

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
        isSdkInitialized = isInitializedForStart(),
        isLocationEnabled = isLocationPermissionGranted() && isGpsEnabled(),
        isPhoneStateEnabled = isPhoneStatePermissionGranted(),
        hostAppName = applicationName
    )

    private fun buildCrowdSourcingStatus(
        networkDataResponse: NetworkDataResponse = NetworkDataResponse(
            status = "Failed", statusCode = 400, message = ""
        )
    ): NetworkDataCrowdSourcingStatus {
        return NetworkDataCrowdSourcingStatus(
            isSdkInit = isInitializedForStart(),
            isLocationEnabled = isLocationPermissionGranted(),
            isPhoneStateGranted = isPhoneStatePermissionGranted(),
            response = gson.toJson(networkDataResponse)
        )
    }


    /** Internal helper to request necessary permissions. */
    private fun requestPermission(callback: (Boolean) -> Unit) {
        when (initMode) {
            InitMode.BACKGROUND -> {
                callback(
                    CheckPermissionHandler.validateRequiredPermissions(context).isGranted &&
                            CommonUtils.isGpsEnabled(context)
                )
            }

            InitMode.UI -> {
                if (!this::checkPermissionHandler.isInitialized) {
                    callback(false)
                } else if (checkPermissionHandler.isPermissionGranted()) {
                    callback(true)
                } else {
                    checkPermissionHandler.requestPermission(callback = callback)
                }
            }

            null -> {
                callback(false)
            }
        }
    }

    private fun isInitializedForStart(): Boolean {
        return initMode != null && this::context.isInitialized && SdkContainer.isInitialized()
    }

    private fun buildInitializationStatus(
        context: Context,
        isInitialized: Boolean,
        networkDataResponse: NetworkDataResponse
    ): NetworkDataCrowdSourcingStatus {
        return NetworkDataCrowdSourcingStatus(
            isSdkInit = isInitialized,
            isLocationEnabled = isLocationPermissionGranted(context),
            isPhoneStateGranted = isPhoneStatePermissionGranted(context),
            response = gson.toJson(networkDataResponse)
        )
    }

    private fun isLocationPermissionGranted(): Boolean {
        return when {
            initMode == InitMode.UI && this::checkPermissionHandler.isInitialized ->
                checkPermissionHandler.isLocationPermissionGranted()

            this::context.isInitialized -> isLocationPermissionGranted(context)
            else -> false
        }
    }

    private fun isPhoneStatePermissionGranted(): Boolean {
        return when {
            initMode == InitMode.UI && this::checkPermissionHandler.isInitialized ->
                checkPermissionHandler.isPhoneStatePermissionGranted()

            this::context.isInitialized -> isPhoneStatePermissionGranted(context)
            else -> false
        }
    }

    private fun isGpsEnabled(): Boolean {
        return when {
            initMode == InitMode.UI && this::checkPermissionHandler.isInitialized ->
                checkPermissionHandler.isGpsEnabled()

            this::context.isInitialized -> CommonUtils.isGpsEnabled(context)

            else -> false
        }
    }

    private fun isLocationPermissionGranted(context: Context): Boolean {
        val validation = CheckPermissionHandler.validateRequiredPermissions(context)
        return validation.missingPermissions.none {
            it == android.Manifest.permission.ACCESS_FINE_LOCATION ||
                    it == android.Manifest.permission.ACCESS_COARSE_LOCATION
        }
    }

    private fun isPhoneStatePermissionGranted(context: Context): Boolean {
        val validation = CheckPermissionHandler.validateRequiredPermissions(context)
        return validation.missingPermissions.none {
            it == android.Manifest.permission.READ_PHONE_STATE
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
