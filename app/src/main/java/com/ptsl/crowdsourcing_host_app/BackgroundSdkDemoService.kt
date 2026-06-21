package com.ptsl.crowdsourcing_host_app

import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.IBinder
import android.util.Log
import com.ptsl.crowdsourcing_network_sdk.NetworkCrowdSourcingDataUploader
import java.text.SimpleDateFormat
import java.util.Locale

/**
 * Demonstrates SDK initialization from a non-UI host layer.
 *
 * The SDK will only validate permissions in this flow. It will not request runtime permissions or
 * show GPS resolution UI, so the host app must make sure permissions are already granted.
 */
class BackgroundSdkDemoService : Service() {

    private val sdk = NetworkCrowdSourcingDataUploader()

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        runBackgroundSdkCapture(startId)
        return START_NOT_STICKY
    }

    private fun runBackgroundSdkCapture(startId: Int) {
        try {
            val initStatus = sdk.init(
                context = applicationContext,
                applicationName = "CrowdDemoApp"
            )

            Log.d(TAG, "Background SDK init status: ${initStatus.response}")
            if (initStatus.isSdkInit != true) {
                stopSelf(startId)
                return
            }

            val timestamp = SimpleDateFormat(
                "yyyy-MM-dd'T'HH:mm:ss",
                Locale.getDefault()
            ).format(System.currentTimeMillis())

            sdk.startCrowdSourcingUploading(
                msisdn = "8801900000000",
                integratedAppVersion = "1.1.0",
                sdkInitiateTimeStamp = timestamp,
                integratedAppEventName = "Background_Service_Capture"
            ) { success, status ->
                Log.d(TAG, "Background capture requested: success=$success, status=${status.response}")
                stopSelf(startId)
            }
        } catch (exception: Exception) {
            Log.e(TAG, "Background SDK demo failed: ${exception.message}", exception)
            stopSelf(startId)
        }
    }

    companion object {
        private const val TAG = "BackgroundSdkDemoService"

        fun createIntent(context: Context): Intent {
            return Intent(context, BackgroundSdkDemoService::class.java)
        }
    }
}
