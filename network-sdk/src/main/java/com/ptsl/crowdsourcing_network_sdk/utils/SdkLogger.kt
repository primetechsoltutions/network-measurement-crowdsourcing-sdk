package com.ptsl.crowdsourcing_network_sdk.utils

import android.util.Log
import com.ptsl.crowdsourcing_network_sdk.BuildConfig

object SdkLogger {
    fun d(tag: String, msg: String) {
        if (BuildConfig.DEBUG) {
            Log.d(tag, msg)
        }
    }

    fun d(tag: String, msg: String, tr: Throwable) {
        if (BuildConfig.DEBUG) {
            Log.d(tag, msg, tr)
        }
    }

    fun i(tag: String, msg: String) {
        if (BuildConfig.DEBUG) {
            Log.i(tag, msg)
        }
    }

    fun i(tag: String, msg: String, tr: Throwable) {
        if (BuildConfig.DEBUG) {
            Log.i(tag, msg, tr)
        }
    }

    fun w(tag: String, msg: String) {
        if (BuildConfig.DEBUG) {
            Log.w(tag, msg)
        }
    }

    fun w(tag: String, msg: String, tr: Throwable) {
        if (BuildConfig.DEBUG) {
            Log.w(tag, msg, tr)
        }
    }

    fun e(tag: String, msg: String) {
        if (BuildConfig.DEBUG) {
            Log.e(tag, msg)
        }
    }

    fun e(tag: String, msg: String, tr: Throwable) {
        if (BuildConfig.DEBUG) {
            Log.e(tag, msg, tr)
        }
    }
}
