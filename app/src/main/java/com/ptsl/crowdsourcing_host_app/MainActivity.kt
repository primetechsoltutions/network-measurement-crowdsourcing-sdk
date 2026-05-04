package com.ptsl.crowdsourcing_host_app

import android.os.Bundle
import android.util.Log
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.compose.rememberNavController
import com.ptsl.crowdsourcing_network_sdk.NetworkCrowdSourcingDataUploader
import com.ptsl.crowdsourcing_host_app.ui.AppNavigation
import com.ptsl.crowdsourcing_host_app.ui.theme.FWASDKTheme
import java.text.SimpleDateFormat
import java.util.*
import kotlinx.coroutines.*

class MainActivity : AppCompatActivity() {

    private val sdk = NetworkCrowdSourcingDataUploader()
    private val TAG = "MainActivity"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Initialize SDK with Activity context
        sdk.init(this, "MYBL")

        setContent {
            FWASDKTheme {
                val navController = rememberNavController()
                AppNavigation(
                    navController = navController,
                    onStartCrowdsourcing = { eventName, _ ->
                        triggerDataCapture(eventName)
                    },
                    onTripleCapture = {
                        triggerTripleCapture()
                    }
                )
            }
        }
    }

    private fun triggerDataCapture(eventName: String) {
        val timeStamp = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault())
            .format(System.currentTimeMillis())

        // Fire and forget - call SDK without waiting for response
        // This is a fire-and-forget operation that runs in the background
        sdk.startCrowdSourcingUploading(
            msisdn = "8801900000000",
            integratedAppVersion = "1.1.0",
            sdkInitiateTimeStamp = timeStamp,
            integratedAppEventName = eventName
        ) { success, status ->
            // Logging only - don't block UI
            Log.d(TAG, "Event: $eventName, Success: $success")
        }
    }

    private fun triggerTripleCapture() {
        // Launch coroutine to trigger 3 captures with delays
        CoroutineScope(Dispatchers.Main).launch {
            Log.d(TAG, "Starting triple capture sequence")
            
            // First capture
            triggerDataCapture("Triple_Capture_1")
            delay(1000) // 1 second delay
            
            // Second capture
            triggerDataCapture("Triple_Capture_2")
            delay(1000) // 1 second delay
            
            // Third capture
            triggerDataCapture("Triple_Capture_3")
            
            Log.d(TAG, "Triple capture sequence completed")
        }
    }
}
