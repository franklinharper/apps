package com.franklinharper.concentra.browser.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp

@Composable
fun HotspotOverlay(
    modifier: Modifier = Modifier,
) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .size(64.dp)
            .testTag(BrowserScreenTags.HotspotOverlay),
    ) {
        Box(
            modifier = Modifier
                .size(28.dp)
                .background(
                    color = Color(0xFFFF4500).copy(alpha = 0.6f),
                    shape = RoundedCornerShape(10.dp),
                ),
        )
    }
}
