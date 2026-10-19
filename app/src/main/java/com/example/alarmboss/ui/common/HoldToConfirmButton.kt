package com.example.alarmboss.ui.common

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch

/**
 * A button that only fires after being held down for [holdMillis], with a fill that shows how far
 * along the hold is. Releasing early cancels and resets it. Used for the emergency exits: they must
 * exist (an alarm that can never be silenced is unsafe) but should cost enough effort that nobody
 * does it half-asleep or on impulse, which a plain tap would not.
 */
@Composable
fun HoldToConfirmButton(
    label: String,
    holdMillis: Long,
    onConfirmed: () -> Unit,
    modifier: Modifier = Modifier
) {
    val progress = remember { Animatable(0f) }
    val scope = rememberCoroutineScope()
    val latestOnConfirmed = rememberUpdatedState(onConfirmed)
    val shape = RoundedCornerShape(24.dp)
    val fill = MaterialTheme.colorScheme.error.copy(alpha = 0.35f)

    Box(
        modifier
            .clip(shape)
            .background(MaterialTheme.colorScheme.errorContainer)
            // Drawn behind the label rather than as a child that fills the height: a child that fills
            // the height makes a wrap-content button stretch to the whole screen.
            .drawBehind { drawRect(fill, size = Size(size.width * progress.value, size.height)) }
            .semantics { role = Role.Button; contentDescription = label }
            .pointerInput(holdMillis) {
                awaitEachGesture {
                    awaitFirstDown()
                    val hold = scope.launch {
                        progress.animateTo(1f, tween(holdMillis.toInt(), easing = LinearEasing))
                        latestOnConfirmed.value()
                    }
                    waitForUpOrCancellation()
                    hold.cancel()
                    scope.launch { progress.snapTo(0f) }
                }
            },
        contentAlignment = Alignment.Center
    ) {
        Text(
            label,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onErrorContainer,
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp).heightIn(min = 24.dp)
        )
    }
}
