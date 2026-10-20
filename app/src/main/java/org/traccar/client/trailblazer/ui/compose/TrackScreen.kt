package org.traccar.client.trailblazer.ui.compose

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.outlined.Login
import androidx.compose.material.icons.outlined.Logout
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import org.traccar.client.trailblazer.ui.theme.StatusOffline
import org.traccar.client.trailblazer.ui.theme.StatusOnline

private const val SOS_HOLD_MILLIS = 2000L

@Composable
fun TrackScreen(
    state: TrailblazerUiState,
    actions: TrailblazerActions,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        ConnectionCard(online = state.online, deviceId = state.deviceId)

        Spacer(Modifier.height(32.dp))

        SosButton(onTriggered = actions.onSosTriggered)

        Spacer(Modifier.height(12.dp))
        Text(
            text = "Hold for 2 seconds to raise an alert",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )

        Spacer(Modifier.height(32.dp))

        Button(
            onClick = actions.onClockToggle,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            colors = if (state.online) {
                ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                    contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                )
            } else {
                ButtonDefaults.buttonColors()
            },
        ) {
            Icon(
                imageVector = if (state.online) Icons.Outlined.Logout else Icons.Outlined.Login,
                contentDescription = null,
            )
            Spacer(Modifier.size(10.dp))
            Text(
                text = if (state.online) "Clock out" else "Clock in",
                style = MaterialTheme.typography.labelLarge,
            )
        }

        Spacer(Modifier.height(12.dp))

        OutlinedButton(
            onClick = actions.onCapturePhoto,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
        ) {
            Icon(Icons.Filled.PhotoCamera, contentDescription = null)
            Spacer(Modifier.size(10.dp))
            Text("Capture photo", style = MaterialTheme.typography.labelLarge)
        }
    }
}

@Composable
private fun ConnectionCard(online: Boolean, deviceId: String) {
    val indicator by animateColorAsState(
        targetValue = if (online) StatusOnline else StatusOffline,
        label = "statusColour",
    )

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
        ),
    ) {
        Column(Modifier.padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier
                        .size(12.dp)
                        .background(indicator, CircleShape),
                )
                Spacer(Modifier.size(10.dp))
                Text(
                    text = if (online) "Connected" else "Disconnected",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            Spacer(Modifier.height(16.dp))

            Text(
                text = "Device",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = deviceId.ifBlank { "Not set" },
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
            )
        }
    }
}

/**
 * Press-and-hold SOS control. The ring fills over [SOS_HOLD_MILLIS] and only fires once it
 * completes, so a stray tap in a pocket cannot raise an alert. Releasing early cancels.
 */
@Composable
private fun SosButton(onTriggered: () -> Unit) {
    var pressed by remember { mutableStateOf(false) }
    var progress by remember { mutableStateOf(0f) }

    // Slow pulse while idle, to draw the eye without being frantic.
    val pulse = rememberInfiniteTransition(label = "sosPulse")
    val idleScale by pulse.animateFloat(
        initialValue = 1f,
        targetValue = 1.04f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "sosIdleScale",
    )
    val pressScale by animateFloatAsState(
        targetValue = if (pressed) 0.94f else idleScale,
        label = "sosPressScale",
    )

    LaunchedEffect(pressed) {
        if (!pressed) {
            progress = 0f
            return@LaunchedEffect
        }
        val step = 16L
        var elapsed = 0L
        while (elapsed < SOS_HOLD_MILLIS) {
            delay(step)
            elapsed += step
            progress = (elapsed.toFloat() / SOS_HOLD_MILLIS).coerceAtMost(1f)
        }
        pressed = false
        progress = 0f
        onTriggered()
    }

    Box(contentAlignment = Alignment.Center) {
        if (pressed) {
            CircularProgressIndicator(
                progress = { progress },
                modifier = Modifier.size(184.dp),
                strokeWidth = 6.dp,
                color = MaterialTheme.colorScheme.error,
                trackColor = Color.Transparent,
            )
        }

        Box(
            modifier = Modifier
                .size(160.dp)
                .scale(pressScale)
                .background(MaterialTheme.colorScheme.error, CircleShape)
                .semantics { contentDescription = "Hold to send an SOS alert" }
                .pointerInput(Unit) {
                    detectTapGestures(
                        onPress = {
                            pressed = true
                            tryAwaitRelease()
                            pressed = false
                        },
                    )
                },
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = "SOS",
                style = MaterialTheme.typography.displaySmall,
                color = MaterialTheme.colorScheme.onError,
                fontWeight = FontWeight.Bold,
            )
        }
    }
}
