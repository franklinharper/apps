package com.franklinharper.whatsapp.settings

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.lifecycle.lifecycleScope
import com.franklinharper.whatsapp.settings.intent.SettingsIntentBuilder
import com.franklinharper.whatsapp.settings.widget.StatusWidget
import kotlinx.coroutines.launch

class WidgetTrampolineActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        startActivity(SettingsIntentBuilder().build(this))
    }

    override fun onResume() {
        super.onResume()
        lifecycleScope.launch {
            StatusWidget().updateAll(this@WidgetTrampolineActivity)
        }
        finish()
    }
}
