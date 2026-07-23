package org.traccar.client.trailblazer.ui.compose

/**
 * Everything the Compose layer needs to render. The activity owns the tracking, SOS, camera
 * and upload logic and pushes changes in here; the UI stays a pure function of this state.
 */
data class TrailblazerUiState(
    val online: Boolean = false,
    val deviceId: String = "",
    val serverUrl: String = "",
    val locationAccuracy: String = "",
)

/** Callbacks back into the activity. */
data class TrailblazerActions(
    val onClockToggle: () -> Unit = {},
    val onSosTriggered: () -> Unit = {},
    val onCapturePhoto: () -> Unit = {},
    val onSaveDeviceId: (String) -> Unit = {},
    val onOpenAbout: () -> Unit = {},
)
