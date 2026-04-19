package com.franklinharper.concentra.browser.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp

@Composable
fun HotspotOverlay(modifier: Modifier = Modifier) {
    Box(
        modifier =
            modifier
                .size(64.dp)
                .background(
                    color = Color.Black.copy(alpha = 0.2f),
                    shape = RoundedCornerShape(20.dp),
                )
                .testTag(BrowserScreenTags.HotspotOverlay),
    )
}
