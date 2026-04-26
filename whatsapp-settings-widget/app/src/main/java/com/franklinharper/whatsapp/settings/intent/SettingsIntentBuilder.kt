package com.franklinharper.whatsapp.settings.intent

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings

class SettingsIntentBuilder {

    fun build(context: Context): Intent {
        val intent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            Intent(Settings.ACTION_APP_BATTERY_SETTINGS).apply {
                putExtra("package", "com.whatsapp")
            }
        } else {
            Intent(
                Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                Uri.fromParts("package", "com.whatsapp", null)
            )
        }
        return intent.apply { addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) }
    }
}
