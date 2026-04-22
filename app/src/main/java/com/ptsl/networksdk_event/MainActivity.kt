package com.ptsl.networksdk_event

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import com.ptsl.network_sdk.NetworkDataUploader
import com.ptsl.network_sdk.UploadType
import java.text.SimpleDateFormat
import java.util.Locale

class MainActivity : AppCompatActivity() {

    // Store the uploader here so fragments can access it
    val networkDataUploader = NetworkDataUploader()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        networkDataUploader.init(this, "MyBL")
        findViewById<View>(R.id.btn_call_sdk).setOnClickListener {
            startMeasurement(UploadType.FTPNetworkDataCapture, "MainActivity Capture FWA1")
        }

        findViewById<View>(R.id.btn_call_sdk2).setOnClickListener {
            startMeasurement(UploadType.FTPNetworkDataCapture, "MainActivity Capture FWA2")
        }

        findViewById<View>(R.id.btn_call_sdk3).setOnClickListener {
            startMeasurement(UploadType.NetworkDataCapture, "MainActivity Standard Capture")
        }
        findViewById<View>(R.id.btn_call_sdk4).setOnClickListener {
            startMeasurement(UploadType.NetworkDataCapture, "MainActivity Standard Capture 1")
            startMeasurement(UploadType.NetworkDataCapture, "MainActivity Standard Capture 2")
            startMeasurement(UploadType.NetworkDataCapture, "MainActivity Standard Capture 2")
        }

        findViewById<View>(R.id.btn_navigate_basic).setOnClickListener {
            val intent = Intent(this, BasicActivity::class.java)
            startActivity(intent)
        }
    }

    private fun startMeasurement(uploadType: UploadType, eventName: String) {
        val currentDate =
                SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault())
                        .format(System.currentTimeMillis())

        networkDataUploader.startUploading(
                "MYBL-1000111",
                "1.1.0-demo",
                currentDate,
                eventName,
                uploadType = uploadType
        ) { success, status ->
            // Print the full response to Logcat for debugging
            Log.d("SDK_RESONSE", "Status: $success, Message: ${status.response}")
        }
    }
}
