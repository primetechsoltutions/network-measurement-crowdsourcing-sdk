package com.ptsl.network_sdk.utils

import android.content.Context

object NetworkSdk {
    private var isInitialized = false

    fun init(context: Context) {
        if (isInitialized) return
        SdkContainer.init(context.applicationContext)
        SdkManager.init(context.applicationContext)
        isInitialized = true
    }
}