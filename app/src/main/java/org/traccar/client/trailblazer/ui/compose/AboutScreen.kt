package org.traccar.client.trailblazer.ui.compose

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

private val bullets = listOf(
    "Real-Time Tracking: Monitor the locations of delivery vehicles and sales personnel to optimise logistics and enhance responsiveness.",
    "SOS Emergency Response: Provide rapid assistance by utilising accurate location data during critical situations.",
    "Route Optimisation and Performance Analysis: Analyse location data to improve routes, reduce operational costs, and boost workforce efficiency.",
    "Parcel Delivery Management: Enable users to take and upload pictures of parcels to our secure servers, ensuring accurate tracking, proof of delivery, and streamlined package management.",
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AboutScreen(
    onBack: () -> Unit,
    onOpenPrivacyPolicy: () -> Unit,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("About Us") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
            )
        },
    ) { innerPadding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 16.dp),
        ) {
            Body(
                "Welcome to Trailblazer, a mobile tracking app designed to streamline business " +
                    "operations through secure, location-based solutions and advanced delivery " +
                    "management tools. Our mission is to empower businesses with real-time insights, " +
                    "efficient logistics, and robust privacy controls to drive success."
            )

            SectionHeading("What We Do")
            Body(
                "Trailblazer leverages precise location data, device IDs, and user-uploaded images " +
                    "to deliver powerful features tailored for business needs, including:"
            )
            Spacer(Modifier.height(8.dp))
            bullets.forEach { Bullet(it) }

            SectionHeading("Our Commitment to Your Privacy")
            Body(
                "At Trailblazer, your trust is paramount. We collect precise location data and " +
                    "device IDs, even when the app is closed or not in use, to support the features " +
                    "listed above. Location tracking is only active when manually enabled, and the " +
                    "tracking status is always clearly visible within the app. Additionally, any " +
                    "pictures you take and upload for parcel delivery management are securely stored " +
                    "and used solely for operational purposes, such as verifying deliveries."
            )
            Spacer(Modifier.height(12.dp))
            Body(
                "Your data — whether location, device IDs, or images — is never sold to third parties " +
                    "for marketing or any unauthorised use. You retain full control and can disable " +
                    "tracking or manage your data preferences at any time through the app."
            )

            Spacer(Modifier.height(8.dp))
            TextButton(onClick = onOpenPrivacyPolicy) {
                Text("Read our Privacy Policy")
            }
        }
    }
}

@Composable
private fun SectionHeading(text: String) {
    Spacer(Modifier.height(24.dp))
    Text(
        text = text,
        style = MaterialTheme.typography.titleLarge,
        color = MaterialTheme.colorScheme.primary,
    )
    Spacer(Modifier.height(8.dp))
}

@Composable
private fun Body(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

@Composable
private fun Bullet(text: String) {
    Row(Modifier.padding(bottom = 10.dp)) {
        Text(
            text = "•  ",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.primary,
        )
        Body(text)
    }
}
