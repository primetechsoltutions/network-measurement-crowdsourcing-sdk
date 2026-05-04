package com.ptsl.crowdsourcing_network_sdk.utils

/**
 * Centralizes SharedPreferences file and key names used by the SDK.
 * Eliminates magic strings in [CheckPermissionHandler].
 */
internal object SdkPrefsKeys {
    const val PREFS_NAME                = "network_sdk_prefs"
    const val LAST_GPS_PROMPT_TIMESTAMP = "last_gps_prompt_timestamp"
}
