package com.ptsl.network_sdk.utils

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import kotlin.system.measureTimeMillis


//
//data class NetworkMetrics(
//    val rtt: Double,
//    val latency: Double
//)
//
//suspend fun calculateRttAndLatency(
//    testUrl: String = "https://salesforceapptest.banglalink.net",
//    hasMobileInternet: Boolean=false,
//): NetworkMetrics = withContext(Dispatchers.IO) {
//if(hasMobileInternet) {
//    val client = OkHttpClient.Builder()
//        .retryOnConnectionFailure(true)
//        .build()
//
//    var latency = 0L
//    var totalRtt = 0L
//    val attempts = 3
//
//    repeat(attempts) { index ->
//        val request = Request.Builder()
//            .url(testUrl)
//            .head()
//            .build()
//
//        val elapsed = measureTimeMillis {
//            client.newCall(request).execute().use { response ->
//                if (index == 0) {
//                    latency = response.receivedResponseAtMillis -
//                            response.sentRequestAtMillis
//                }
//            }
//        }
//        totalRtt += elapsed
//
//    }
//
//    NetworkMetrics(
//        rtt = totalRtt.toDouble() / attempts,
//        latency = latency.toDouble()
//    )
//}else{
//    NetworkMetrics(
//        rtt = 0.0,
//        latency = 0.0
//    )
//    }
//}



import okhttp3.*
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import java.net.InetSocketAddress
import java.net.Proxy
import java.net.Socket
import java.util.concurrent.TimeUnit
//
//data class NetworkMetrics(
//    val rtt: Double,      // TCP handshake time in ms
//    val latency: Double   // Downstream latency (TTFB minus upstream processing) in ms
//)
//
//suspend fun calculateRttAndLatency(
//    testUrl: String = "https://salesforceapptest.banglalink.net",
//    hasMobileInternet: Boolean,
//    samples: Int = 3
//): NetworkMetrics = withContext(Dispatchers.IO) {
//
//    if (!hasMobileInternet) return@withContext NetworkMetrics(0.0, 0.0)
//
//    val rttSamples = mutableListOf<Long>()
//    val latencySamples = mutableListOf<Long>()
//
//    repeat(samples) {
//        var connectStartNs = 0L
//        var connectEndNs = 0L
//        var requestEndNs = 0L
//        var responseStartNs = 0L
//        var serverProcessingMs = 0L  // Upstream server processing time
//
//        val client = OkHttpClient.Builder()
//            .connectionPool(ConnectionPool(0, 1, TimeUnit.SECONDS))
//            .retryOnConnectionFailure(false)
//            .eventListener(object : EventListener() {
//                override fun connectStart(
//                    call: Call,
//                    inetSocketAddress: java.net.InetSocketAddress,
//                    proxy: java.net.Proxy
//                ) {
//                    connectStartNs = System.nanoTime()
//                }
//
//                override fun connectEnd(
//                    call: Call,
//                    inetSocketAddress: java.net.InetSocketAddress,
//                    proxy: java.net.Proxy,
//                    protocol: Protocol?
//                ) {
//                    connectEndNs = System.nanoTime()
//                }
//
//                override fun requestHeadersEnd(call: Call, request: Request) {
//                    requestEndNs = System.nanoTime()
//                }
//
//                override fun responseHeadersStart(call: Call) {
//                    responseStartNs = System.nanoTime()
//                    // Try reading server processing header if present
//                    call.request().tag()?.let { tag ->
//                        // Optional: simulate reading server header
//                        // Example: "X-Server-Processing-Time"
//                    }
//                }
//            })
//            .build()
//
//        try {
//            val request = Request.Builder()
//                .url(testUrl)
//                .header("Connection", "close")
//                .get()
//                .build()
//
//            client.newCall(request).execute().use { response ->
//                // Optional: extract server processing time if header exists
//                response.header("X-Server-Processing-Time")?.let { header ->
//                    serverProcessingMs = header.toLongOrNull() ?: 0L
//                }
//            }
//
//            // ---------- RTT ----------
//            val rttMs = when {
//                connectStartNs > 0 && connectEndNs > connectStartNs ->
//                    (connectEndNs - connectStartNs) / 1_000_000
//                requestEndNs > 0 && responseStartNs > requestEndNs ->
//                    ((responseStartNs - requestEndNs) / 2) / 1_000_000
//                else -> 0
//            }
//
//            // ---------- Downstream Latency ----------
//            val rawLatencyMs = if (requestEndNs > 0 && responseStartNs > requestEndNs)
//                (responseStartNs - requestEndNs) / 1_000_000
//            else 0
//
//            val latencyMs = (rawLatencyMs - serverProcessingMs).coerceAtLeast(0L)
//
//            if (rttMs > 0) rttSamples.add(rttMs)
//            latencySamples.add(latencyMs)
//
//        } catch (e: Exception) {
//            // Ignore failed requests
//        }
//    }
//
//    fun median(values: List<Long>): Double {
//        if (values.isEmpty()) return 0.0
//        val sorted = values.sorted()
//        val mid = sorted.size / 2
//        return if (sorted.size % 2 == 0)
//            (sorted[mid - 1] + sorted[mid]) / 2.0
//        else sorted[mid].toDouble()
//    }
//
//    NetworkMetrics(
//        rtt = median(rttSamples),
//        latency = median(latencySamples)
//    )
//}
//
//


data class NetworkMetrics(
    val rtt: Double,
    val latency: Double
)

suspend fun calculateRttAndLatency(
    testUrl: String,
    hasMobileInternet: Boolean,
    samples: Int = 3
): NetworkMetrics = withContext(Dispatchers.IO) {

    if (!hasMobileInternet) return@withContext NetworkMetrics(0.0, 0.0)

    val rttSamples = mutableListOf<Long>()
    val latencySamples = mutableListOf<Long>()

    val url = testUrl.toHttpUrlOrNull() ?: return@withContext NetworkMetrics(0.0, 0.0)
    val host = url.host
    val port = url.port
    val isIpAddress = host.matches(Regex("""\d{1,3}(\.\d{1,3}){3}"""))

    repeat(samples) {

        var connectStartNs = 0L
        var connectEndNs = 0L
        var requestEndNs = 0L
        var responseStartNs = 0L
        var serverProcessingMs = 0L

        val client = OkHttpClient.Builder()
            .connectionPool(ConnectionPool(0, 1, TimeUnit.SECONDS))
            .retryOnConnectionFailure(false)
            .eventListener(object : EventListener() {

                override fun connectStart(
                    call: Call,
                    inetSocketAddress: InetSocketAddress,
                    proxy: Proxy
                ) {
                    connectStartNs = System.nanoTime()
                }

                override fun connectEnd(
                    call: Call,
                    inetSocketAddress: InetSocketAddress,
                    proxy: Proxy,
                    protocol: Protocol?
                ) {
                    connectEndNs = System.nanoTime()
                }

                override fun requestHeadersEnd(call: Call, request: Request) {
                    requestEndNs = System.nanoTime()
                }

                override fun responseHeadersStart(call: Call) {
                    responseStartNs = System.nanoTime()
                }
            })
            .build()

        try {
            val request = Request.Builder()
                .url(testUrl)
                .header("Connection", "close")
                .header("Host", host) // important for IP-based HTTP
                .get()
                .build()

            client.newCall(request).execute().use { response ->
                response.header("X-Server-Processing-Time")?.let {
                    serverProcessingMs = it.toLongOrNull() ?: 0L
                }
            }

            // ---------- RTT ----------
            val rttMs = when {
                connectStartNs > 0 && connectEndNs > connectStartNs ->
                    (connectEndNs - connectStartNs) / 1_000_000

                requestEndNs > 0 && responseStartNs > requestEndNs ->
                    ((responseStartNs - requestEndNs) / 2) / 1_000_000

                else -> 0
            }

            // ---------- Latency (TTFB) ----------
            val rawLatencyMs =
                if (requestEndNs > 0 && responseStartNs > requestEndNs)
                    (responseStartNs - requestEndNs) / 1_000_000
                else 0

            val latencyMs = (rawLatencyMs - serverProcessingMs).coerceAtLeast(0)

            if (rttMs > 0) rttSamples.add(rttMs)
            if (latencyMs > 0) latencySamples.add(latencyMs)

        } catch (_: Exception) {
            // Ignore HTTP failures
        }

        // ---------- TCP fallback for IP ----------
        if (isIpAddress && rttSamples.isEmpty()) {
            try {
                val start = System.nanoTime()
                Socket().use { socket ->
                    socket.connect(InetSocketAddress(host, port), 3000)
                }
                val end = System.nanoTime()
                rttSamples.add((end - start) / 1_000_000)
            } catch (_: Exception) {
            }
        }
    }

    fun median(values: List<Long>): Double {
        if (values.isEmpty()) return 0.0
        val sorted = values.sorted()
        val mid = sorted.size / 2
        return if (sorted.size % 2 == 0)
            (sorted[mid - 1] + sorted[mid]) / 2.0
        else sorted[mid].toDouble()
    }

    NetworkMetrics(
        rtt = median(rttSamples),
        latency = median(latencySamples)
    )
}
