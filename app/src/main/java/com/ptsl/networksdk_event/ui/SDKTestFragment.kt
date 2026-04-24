package com.ptsl.networksdk_event.ui

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.ptsl.network_sdk.NetworkDataUploader
import com.ptsl.network_sdk.UploadType
import com.ptsl.networksdk_event.R
import java.text.SimpleDateFormat
import java.util.Locale

abstract class BaseSDKTestFragment : Fragment() {

    abstract val fragmentName: String
    private lateinit var tvTitle: TextView
    private lateinit var tvStatus: TextView
    private val networkDataUploader = NetworkDataUploader()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_sdk_test, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        tvTitle = view.findViewById(R.id.tv_fragment_title)
        tvStatus = view.findViewById(R.id.tv_sdk_status)

        tvTitle.text = fragmentName

        // Initialize and call SDK
        val activity = activity as? AppCompatActivity
        if (activity != null) {
            networkDataUploader.init(this, "MyBL")
            startMeasurement()
        }
    }

    private fun startMeasurement() {
        val currentDate = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault())
            .format(System.currentTimeMillis())

        tvStatus.text = "SDK Call Started..."

        networkDataUploader.startUploading(
            "MYBL-1000111",
            "1.1.0-demo",
            currentDate,
            "$fragmentName Capture",
            uploadType = UploadType.FTPNetworkDataCapture
        ) { success, status ->
            activity?.runOnUiThread {
                Log.d("SDK_RESPONSE", "$fragmentName Status: $success, Message: ${status.response}")
                tvStatus.text = "Status: ${if (success) "Success" else "Failed"}\nMessage: ${status.response}"
            }
        }
    }
}

class FragmentOne : BaseSDKTestFragment() { override val fragmentName = "Fragment 1" }
class FragmentTwo : BaseSDKTestFragment() { override val fragmentName = "Fragment 2" }
class FragmentThree : BaseSDKTestFragment() { override val fragmentName = "Fragment 3" }
class FragmentFour : BaseSDKTestFragment() { override val fragmentName = "Fragment 4" }
class FragmentFive : BaseSDKTestFragment() { override val fragmentName = "Fragment 5" }
