package com.ptsl.networksdk_event

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import com.ptsl.network_sdk.NetworkDataUploader
import java.text.SimpleDateFormat
import java.util.Locale

class MainActivity : AppCompatActivity() {

    // Store the uploader here so fragments can access it
    val networkDataUploader = NetworkDataUploader()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val currentDate =
            SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault())
                .format(System.currentTimeMillis())

        networkDataUploader.init(this, "MyBL")

        networkDataUploader.startUploading(
            "MYBL-1000111",
            "1.1.0-demo",
            currentDate,
            "MainActivity Capture FWA1",
        ) { success, status ->
            // Print the full response to Logcat for debugging
            Log.d("SDK_RESONSE", "Status: $success, Message: ${status.response}")
        }

        findViewById<View>(R.id.btn_call_sdk3).setOnClickListener {
            startMeasurement("MainActivity Standard Capture")
        }
        findViewById<View>(R.id.btn_call_sdk4).setOnClickListener {
            startMeasurement("MainActivity Standard Capture 1")
            startMeasurement("MainActivity Standard Capture 2")
            startMeasurement("MainActivity Standard Capture 2")
        }

        findViewById<View>(R.id.btn_open_viewpager).setOnClickListener {
            val intent = Intent(this, ViewPagerActivity::class.java)
            startActivity(intent)
        }

        findViewById<View>(R.id.btn_navigate_basic).setOnClickListener {
            val intent = Intent(this, BasicActivity::class.java)
            startActivity(intent)
        }
    }

    private fun startMeasurement(eventName: String) {
        val currentDate =
            SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault())
                .format(System.currentTimeMillis())

        networkDataUploader.startUploading(
            "MYBL-1000111",
            "1.1.0-demo",
            currentDate,
            eventName,
        ) { success, status ->
            // Print the full response to Logcat for debugging
            Log.d("SDK_RESONSE", "Status: $success, Message: ${status.response}")
        }
    }
}
