package org.traccar.client.trailblazer.ui.compose

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ListAlt
import androidx.compose.material.icons.outlined.MyLocation
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import kotlinx.coroutines.launch
import org.traccar.client.trailblazer.util.Logger

private enum class TrailblazerTab(val label: String, val icon: ImageVector) {
    Track("Track", Icons.Outlined.MyLocation),
    Logs("Logs", Icons.AutoMirrored.Outlined.ListAlt),
    Settings("Settings", Icons.Outlined.Settings),
}

/**
 * Root of the Compose UI. [Scaffold] applies the system bar insets, which is what keeps the app
 * usable under the edge-to-edge enforcement that Android 16 makes mandatory at targetSdk 36.
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
    val pagerState = rememberPagerState(pageCount = { tabs.size })
    val scope = rememberCoroutineScope()

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Trailblazer") },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface,
                ),
            )
        },
    ) { innerPadding ->
        Box(
            Modifier
                .fillMaxSize()
                .padding(innerPadding),
        ) {
            androidx.compose.foundation.layout.Column(Modifier.fillMaxSize()) {
                PrimaryTabRow(selectedTabIndex = pagerState.currentPage) {
                    tabs.forEachIndexed { index, tab ->
                        Tab(
                            selected = pagerState.currentPage == index,
                            onClick = { scope.launch { pagerState.animateScrollToPage(index) } },
                            text = { Text(tab.label) },
                            icon = { Icon(tab.icon, contentDescription = null) },
                        )
                    }
                }

                HorizontalPager(
                    state = pagerState,
                    modifier = Modifier.fillMaxSize(),
                ) { page ->
                    when (tabs[page]) {
                        TrailblazerTab.Track -> TrackScreen(state = state, actions = actions)
                        TrailblazerTab.Logs -> LogsScreen(logs = logs)
                        TrailblazerTab.Settings -> SettingsScreen(state = state, actions = actions)
                    }
                }
            }
        }
    }
}
