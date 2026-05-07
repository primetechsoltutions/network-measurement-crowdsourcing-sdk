package com.ptsl.network_sdk.bandwidth

class Stopwatch {
    private var startTime: Long = 0
    private var endTime: Long = 0

    fun start() {
        startTime = System.currentTimeMillis()
    }

    fun stop() {
        endTime = System.currentTimeMillis()
    }

    fun reset() {
        startTime = 0
        endTime = 0
    }

    fun elapsedSeconds(): Double {
        return (endTime - startTime) / 1000.0
    }
}