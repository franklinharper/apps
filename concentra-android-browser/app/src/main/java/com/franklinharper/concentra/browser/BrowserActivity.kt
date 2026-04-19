package com.franklinharper.concentra.browser

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import com.franklinharper.concentra.browser.ui.BrowserRoute
import com.franklinharper.concentra.browser.web.PasskeyBridge

class BrowserActivity : ComponentActivity() {
    private var passkeyBridge: PasskeyBridge? = null
    private var currentFidoRequestCode: Int = -1

    private val fidoLauncher = registerForActivityResult(
        ActivityResultContracts.StartIntentSenderForResult()
    ) { result ->
        passkeyBridge?.onActivityResult(currentFidoRequestCode, result.resultCode, result.data)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            BrowserApp(
                activity = this,
                onBridgeCreated = { bridge ->
                    passkeyBridge = bridge
                    bridge.launchIntentCallback = { pendingIntent, requestCode ->
                        currentFidoRequestCode = requestCode
                        fidoLauncher.launch(
                            IntentSenderRequest.Builder(pendingIntent.intentSender).build()
                        )
                    }
                },
            )
        }
    }
}

@Composable
private fun BrowserApp(activity: BrowserActivity, onBridgeCreated: (PasskeyBridge) -> Unit) {
    val container = remember(activity) { BrowserAppContainer(activity) }

    MaterialTheme {
        Surface(modifier = Modifier.fillMaxSize()) {
            BrowserRoute(container = container, onBridgeCreated = onBridgeCreated)
        }
    }
}
