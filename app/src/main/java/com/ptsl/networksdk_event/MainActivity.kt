package com.ptsl.networksdk_event

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import com.ptsl.network_sdk.NetworkDataUploader
import com.ptsl.network_sdk.UploadType
import com.ptsl.networksdk_event.ui.HomeFragment
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
            val currentDate = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault())
                .format(System.currentTimeMillis())
            networkDataUploader.startUploading(
                "MYBL-1000111",
                "1.1.0-demo",
                currentDate,
                "MainActivity",
                uploadType = UploadType.FTPNetworkDataCapture
            ) { success, status ->
                if (success){
                    Log.d("UploadStatus", "SDK Success for MainActivity. Error: ${status.message}")
                }else{
                    Log.e("UploadStatus", "SDK failed for MainActivity. Error: ${status.message}")
                }

            }
        }

        findViewById<View>(R.id.btn_navigate_basic).setOnClickListener {
            val intent = Intent(this, BasicActivity::class.java)
            startActivity(intent)
        }
    }


}
