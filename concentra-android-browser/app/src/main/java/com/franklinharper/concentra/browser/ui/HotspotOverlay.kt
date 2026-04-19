package com.franklinharper.concentra.browser.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.input.pointer.PointerInputScope
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.gestures.awaitFirstDown
import kotlin.math.abs

@Composable
fun HotspotOverlay(
    onSwipeUp: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        contentAlignment = Alignment.Center,
        modifier =
            modifier
                .size(64.dp)
                .pointerInput(onSwipeUp) {
                    detectVerticalSwipeUp(onSwipeUp)
                }
                .testTag(BrowserScreenTags.HotspotOverlay),
    ) {
        Box(
            modifier =
                Modifier
                    .size(28.dp)
                    .background(
                        color = Color.Black.copy(alpha = 0.2f),
                        shape = RoundedCornerShape(10.dp),
                    ),
        )
    }
}

private suspend fun PointerInputScope.detectVerticalSwipeUp(
    onSwipeUp: () -> Unit,
) {
    awaitPointerEventScope {
        while (true) {
            val down = awaitFirstDown(requireUnconsumed = false)
            var totalDragY = 0f
            var totalDragX = 0f
            var pointer = down.id

            while (true) {
                val event = awaitPointerEvent()
                val change = event.changes.firstOrNull { it.id == pointer } ?: break
                if (!change.pressed) {
                    break
                }

                val delta = change.positionChange()
                totalDragX += delta.x
                totalDragY += delta.y
                if (totalDragY <= -48f && abs(totalDragY) > abs(totalDragX)) {
                    change.consume()
                    onSwipeUp()
                    break
                }
            }
        }
    }
}
