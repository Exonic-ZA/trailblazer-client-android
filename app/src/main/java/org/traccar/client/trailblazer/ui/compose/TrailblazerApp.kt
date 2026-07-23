package org.traccar.client.trailblazer.ui.compose

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ListAlt
import androidx.compose.material.icons.automirrored.outlined.ListAlt
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.MyLocation
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import org.traccar.client.trailblazer.util.Logger

private enum class TrailblazerTab(
    val label: String,
    val selectedIcon: ImageVector,
    val icon: ImageVector,
) {
    Track("Track", Icons.Filled.MyLocation, Icons.Outlined.MyLocation),
    Logs("Logs", Icons.AutoMirrored.Filled.ListAlt, Icons.AutoMirrored.Outlined.ListAlt),
    Settings("Settings", Icons.Filled.Settings, Icons.Outlined.Settings),
}

/**
 * Root of the Compose UI.
 *
 * [Scaffold] applies the system bar insets to both the app bar and the navigation bar, which is
 * what keeps the app usable under the edge-to-edge enforcement Android 16 makes mandatory at
 * targetSdk 36.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TrailblazerApp(
    state: TrailblazerUiState,
    actions: TrailblazerActions,
    logs: List<Logger.LogEntry>,
    showDisclaimer: Boolean,
    onConsent: () -> Unit,
) {
    if (showDisclaimer) {
        DisclaimerDialog(onConsent = onConsent)
    }

    val tabs = TrailblazerTab.entries
    var selected by rememberSaveable { mutableStateOf(TrailblazerTab.Track) }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(selected.label) },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface,
                ),
            )
        },
        bottomBar = {
            NavigationBar {
                tabs.forEach { tab ->
                    val isSelected = tab == selected
                    NavigationBarItem(
                        selected = isSelected,
                        onClick = { selected = tab },
                        icon = {
                            Icon(
                                imageVector = if (isSelected) tab.selectedIcon else tab.icon,
                                contentDescription = null,
                            )
                        },
                        label = { Text(tab.label) },
                    )
                }
            }
        },
    ) { innerPadding ->
        AnimatedContent(
            targetState = selected,
            transitionSpec = { fadeIn() togetherWith fadeOut() using SizeTransform(clip = false) },
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            label = "tabContent",
        ) { tab ->
            when (tab) {
                TrailblazerTab.Track -> TrackScreen(state = state, actions = actions)
                TrailblazerTab.Logs -> LogsScreen(logs = logs)
                TrailblazerTab.Settings -> SettingsScreen(state = state, actions = actions)
            }
        }
    }
}
