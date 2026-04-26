package com.franklinharper.whatsapp.settings.intent

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings

class SettingsIntentBuilder {

    fun build(context: Context): Intent {
        return Intent(
            Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
            Uri.fromParts("package", "com.whatsapp", null)
        ).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
    }
}
