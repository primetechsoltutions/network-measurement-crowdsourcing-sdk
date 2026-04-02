package com.ptsl.network_sdk.utils

import android.content.Context
import androidx.startup.Initializer

public class SdkInitializer : Initializer<Unit> {
    override fun create(context: Context) {
        NetworkSdk.init(context)
    }

    override fun dependencies(): List<Class<out Initializer<*>>> {
        return emptyList()
    }
}
