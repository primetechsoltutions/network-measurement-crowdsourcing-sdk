package com.ptsl.networksdk_event

import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import com.github.mikephil.charting.charts.HorizontalBarChart
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.BarData
import com.github.mikephil.charting.data.BarDataSet
import com.github.mikephil.charting.data.BarEntry
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter
import com.github.mikephil.charting.utils.ColorTemplate
import com.ptsl.network_sdk.NetworkDataUploader
import com.ptsl.network_sdk.UploadType
import java.text.SimpleDateFormat
import java.util.*
import org.json.JSONObject

class MainActivity : AppCompatActivity() {
    var networkDataUploader = NetworkDataUploader()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        networkDataUploader.init(this, "MyBL")

//        val currentDate =
//            SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault())
//                .format(System.currentTimeMillis())
//
//        uploadData("MYBL-1000111", currentDate, "onCreate-1", UploadType.NetworkDataCapture)
//        uploadData("MYBL-1000111", currentDate, "onCreate-2", UploadType.NetworkDataCapture)
//

        findViewById<Button>(R.id.event_1).setOnClickListener {
            val currentDate =
                    SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault())
                            .format(System.currentTimeMillis())

            uploadData("MYBL-1000111", currentDate, "Button-1", UploadType.NetworkDataCapture)
        }

        findViewById<Button>(R.id.event_2).setOnClickListener {
            val currentDate =
                    SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault())
                            .format(System.currentTimeMillis())
            uploadData("MYBL-1023", currentDate, "Button-2", UploadType.FTPNetworkDataCapture)
        }
    }

    private fun uploadData(
            msisdn: String,
            currentDate: String,
            eventName: String,
            uploadType: UploadType
    ) {
        val loadingOverlay = findViewById<android.view.View>(R.id.loadingOverlay)
        val resultTextView = findViewById<android.widget.TextView>(R.id.resultTextView)

        loadingOverlay.visibility = android.view.View.VISIBLE
        if (uploadType == UploadType.FTPNetworkDataCapture) {
            resultTextView.text = "Starting FTP data captureing. Please wait...\n"
        }

        networkDataUploader.startUploading(
                msisdn,
                "10.0.0",
                currentDate,
                eventName,
                uploadType = uploadType
        ) { success, status ->
            runOnUiThread {
                val loadingOverlay = findViewById<android.view.View>(R.id.loadingOverlay)
                loadingOverlay.visibility = android.view.View.GONE
                if (success) {
                    Log.i(
                            "UploadStatus",
                            "SDK finished successfully for $eventName. Data: ${status.message}"
                    )
                    resultTextView.text = "Success for $eventName:\n${status.message}"
                    displayChart(status.message ?: "")
                } else {
                    Log.e("UploadStatus", "SDK failed for $eventName. Error: ${status.message}")
                    resultTextView.text = "Failed for $eventName:\n${status.message}"
                }
            }
        }
    }

    private fun displayChart(jsonData: String) {
        try {
            val jsonObject = JSONObject(jsonData)
            val testResult = jsonObject.optString("testResult", "N/A")
            val assessmentStatusTextView = findViewById<android.widget.TextView>(R.id.assessmentStatusTextView)
            val assessmentResultCard = findViewById<android.view.View>(R.id.assessmentResultCard)

            assessmentResultCard.visibility = android.view.View.VISIBLE
            assessmentStatusTextView.text = testResult
            if (testResult.equals("Pass", ignoreCase = true)) {
                assessmentStatusTextView.setTextColor(android.graphics.Color.parseColor("#2E7D32")) // Green
            } else if (testResult.equals("Failed", ignoreCase = true)) {
                assessmentStatusTextView.setTextColor(android.graphics.Color.parseColor("#C62828")) // Red
            } else {
                assessmentStatusTextView.setTextColor(android.graphics.Color.DKGRAY)
            }

            val data = jsonObject.optJSONObject("data") ?: return

            setupSignalChart(data.optJSONObject("networkData"))
            setupSpeedChart(data.optJSONObject("speedPair"))
            setupCellChart(data.optJSONObject("cellInfo"))
            updateUserInfo(data.optJSONObject("userInfo"))
        } catch (e: Exception) {
            Log.e("ChartError", "Error parsing JSON for multi-charts: ${e.message}")
        }
    }

    private fun setupSignalChart(networkData: JSONObject?) {
        val chart = findViewById<HorizontalBarChart>(R.id.signalChart)
        val entries = ArrayList<BarEntry>()
        val labels = ArrayList<String>()
        var index = 0f

        networkData?.let {
            entries.add(BarEntry(index++, it.optDouble("RSRP", 0.0).toFloat()))
            labels.add("RSRP")
            entries.add(BarEntry(index++, it.optDouble("SNR", 0.0).toFloat()))
            labels.add("SNR")
            entries.add(BarEntry(index++, it.optDouble("RSRQ", 0.0).toFloat()))
            labels.add("RSRQ")
        }
        setupHorizontalBarChart(
                chart,
                entries,
                labels,
                "Signal Metrics",
                ColorTemplate.MATERIAL_COLORS
        )
    }

    private fun setupSpeedChart(speedPair: JSONObject?) {
        val chart = findViewById<LineChart>(R.id.speedChart)
        val entriesDL = ArrayList<Entry>()
        val entriesUL = ArrayList<Entry>()

        speedPair?.let {
            // Line chart usually represents data over "time" or "points"
            // Since we have only one measurement point, we can show it as a single point or a line
            // from 0
            entriesDL.add(Entry(0f, 0f))
            entriesDL.add(Entry(1f, (it.optDouble("dlSpeedKbps", 0.0) / 1024).toFloat()))

            entriesUL.add(Entry(0f, 0f))
            entriesUL.add(Entry(1f, (it.optDouble("ulSpeedKbps", 0.0) / 1024).toFloat()))
        }

        val dataSetDL = LineDataSet(entriesDL, "DL_Mbps")
        dataSetDL.color = Color.parseColor("#1E88E5")
        dataSetDL.setCircleColor(Color.parseColor("#1E88E5"))
        dataSetDL.lineWidth = 3f
        dataSetDL.setDrawFilled(true)
        dataSetDL.fillColor = Color.parseColor("#1E88E5")
        dataSetDL.fillAlpha = 50
        dataSetDL.valueTextSize = 10f

        val dataSetUL = LineDataSet(entriesUL, "UL_Mbps")
        dataSetUL.color = Color.parseColor("#E53935")
        dataSetUL.setCircleColor(Color.parseColor("#E53935"))
        dataSetUL.lineWidth = 3f
        dataSetUL.setDrawFilled(true)
        dataSetUL.fillColor = Color.parseColor("#E53935")
        dataSetUL.fillAlpha = 50
        dataSetUL.valueTextSize = 10f

        val lineData = LineData(dataSetDL, dataSetUL)
        chart.data = lineData

        chart.xAxis.apply {
            position = XAxis.XAxisPosition.BOTTOM
            granularity = 1f
            setDrawGridLines(false)
            setDrawLabels(false) // Hide 0, 1 indices
        }

        chart.axisLeft.setDrawGridLines(true)
        chart.axisLeft.gridColor = Color.LTGRAY
        chart.setBackgroundColor(Color.parseColor("#F5F5F5"))
        chart.setDrawGridBackground(true)
        chart.setGridBackgroundColor(Color.parseColor("#F0F4F8"))

        // Remove axisMinimum = 0f to support negative values
        chart.axisLeft.resetAxisMinimum()

        chart.axisRight.isEnabled = false
        chart.description.isEnabled = false
        chart.legend.isEnabled = true

        chart.animateX(800)
        chart.invalidate()
    }

    private fun setupCellChart(cellInfo: JSONObject?) {
        val chart = findViewById<HorizontalBarChart>(R.id.cellChart)
        val entries = ArrayList<BarEntry>()
        val labels = ArrayList<String>()
        var index = 0f

        cellInfo?.let {
            entries.add(BarEntry(index++, it.optDouble("nbhDlThroughputMbps", 0.0).toFloat()))
            labels.add("DL_Throughput")
            entries.add(BarEntry(index++, it.optDouble("nbhTrafficGB", 0.0).toFloat()))
            labels.add("Traffic_GB")
        }
        setupHorizontalBarChart(
                chart,
                entries,
                labels,
                "Cell Metrics",
                ColorTemplate.COLORFUL_COLORS
        )
    }

    private fun setupHorizontalBarChart(
            chart: HorizontalBarChart,
            entries: ArrayList<BarEntry>,
            labels: ArrayList<String>,
            label: String,
            colors: IntArray
    ) {
        chart.setBackgroundColor(Color.parseColor("#FBFBFB"))
        chart.setDrawGridBackground(true)
        chart.setGridBackgroundColor(Color.parseColor("#F2F2F2"))

        val dataSet = BarDataSet(entries, label)
        dataSet.colors = colors.toList()
        dataSet.valueTextSize = 10f
        dataSet.valueTextColor = Color.DKGRAY

        val barData = BarData(dataSet)
        barData.barWidth = 0.6f
        chart.data = barData

        chart.xAxis.apply {
            position = XAxis.XAxisPosition.BOTTOM
            valueFormatter = IndexAxisValueFormatter(labels)
            granularity = 1f
            isGranularityEnabled = true
            labelCount = labels.size
            setDrawGridLines(false)
            setDrawAxisLine(true)
            textColor = Color.DKGRAY
        }

        chart.axisLeft.apply {
            setDrawGridLines(true)
            gridColor = Color.LTGRAY
            // Remove axisMinimum = 0f to support negative values (e.g. RSRP)
            resetAxisMinimum()
            textColor = Color.DKGRAY
            setDrawZeroLine(true) // Draw line at 0 for context
            zeroLineColor = Color.GRAY
            zeroLineWidth = 1f
        }

        chart.axisRight.isEnabled = false
        chart.description.isEnabled = false
        chart.legend.isEnabled = true
        chart.legend.textColor = Color.DKGRAY
        chart.setFitBars(true)
        chart.setExtraOffsets(40f, 0f, 10f, 0f) // Increase left offset for labels

        chart.animateY(800)
        chart.invalidate()
    }

    private fun updateUserInfo(userInfo: JSONObject?) {
        val textView = findViewById<android.widget.TextView>(R.id.userInfoTextView)
        userInfo?.let {
            val info = StringBuilder()
            info.append("MSISDN: ${it.optString("msisdn", "N/A")}\n")
            info.append(
                    "Device: ${it.optString("deviceManufacture", "")} ${it.optString("deviceModel", "N/A")}\n"
            )
            info.append("OS Version: ${it.optString("deviceOsVersion", "N/A")}\n")
            info.append(
                    "Location: Lat ${it.optDouble("latitude", 0.0)}, Lon ${it.optDouble("longitude", 0.0)}"
            )
            textView.text = info.toString()
        }
    }
}
