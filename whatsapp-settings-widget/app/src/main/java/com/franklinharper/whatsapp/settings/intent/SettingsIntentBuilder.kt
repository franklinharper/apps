package com.franklinharper.whatsapp.settings.intent

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings

enum class IntentPath {
    BatterySettings,
    AppDetailsSettings,
}

class SettingsIntentBuilder(
    private val sdkInt: Int = Build.VERSION.SDK_INT,
) {

    fun selectPath(): IntentPath = if (sdkInt >= Build.VERSION_CODES.TIRAMISU) {
        IntentPath.BatterySettings
    } else {
        IntentPath.AppDetailsSettings
    }

    fun build(context: Context): Intent {
        val intent = when (selectPath()) {
            IntentPath.BatterySettings -> {
                Intent("android.settings.APP_BATTERY_SETTINGS").apply {
                    putExtra("package", "com.whatsapp")
                }
            }
            IntentPath.AppDetailsSettings -> {
                Intent(
                    Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                    Uri.fromParts("package", "com.whatsapp", null)
                )
            }
        }
        return intent.apply { addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) }
    }
}
