package org.traccar.client.trailblazer.ui.compose

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties

private val purposes = listOf(
    "Real-time tracking of delivery vehicles and sales personnel",
    "SOS emergency response functionality",
    "Route optimisation and performance analysis",
)

/**
 * Shown once, before the user can use the app. Not dismissible — consent to background location
 * collection is a precondition, and Play policy requires it to be explicit.
 */
@Composable
fun DisclaimerDialog(onConsent: () -> Unit) {
    AlertDialog(
        onDismissRequest = { /* consent is required */ },
        properties = DialogProperties(
            dismissOnBackPress = false,
            dismissOnClickOutside = false,
        ),
        title = { Text("Disclaimer") },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState())) {
                Text(
                    text = "This app collects and stores your precise location data even when the " +
                        "app is closed or not in use, to enable:",
                    style = MaterialTheme.typography.bodyMedium,
                )
                Spacer(Modifier.height(12.dp))
                purposes.forEach { purpose ->
                    Row(Modifier.padding(bottom = 6.dp)) {
                        Text(
                            text = "•  ",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.primary,
                        )
                        Text(purpose, style = MaterialTheme.typography.bodyMedium)
                    }
                }
                Spacer(Modifier.height(12.dp))
                Text(
                    text = "Your location is only tracked when tracking is manually enabled, and " +
                        "the tracking status is always clearly visible in the app.\n\n" +
                        "Location data is securely stored and used solely for business operations. " +
                        "It is never sold to third parties for marketing.\n\n" +
                        "You can disable tracking at any time through the app.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onConsent) {
                Text("I UNDERSTAND AND CONSENT")
            }
        },
    )
}
