package com.ptsl.network_sdk.utils

import android.content.Context

object NetworkSdk {
    fun init(context: Context) {
        SdkContainer.init(context.applicationContext)
        SdkManager.init(context.applicationContext)
    }
}