package org.traccar.client.trailblazer.util

import androidx.compose.runtime.mutableStateListOf
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * In-memory activity log surfaced by the Logs tab.
 *
 * Backed by a snapshot state list so Compose observes appends directly — entries added from the
 * tracking service while the user is looking at the Logs tab show up without a manual refresh.
 */
object Logger {

    private const val LIMIT = 200

    data class LogEntry(val title: String, val description: String, val timestamp: String)

    private val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())

    private val entries = mutableStateListOf<LogEntry>()

    fun addLog(title: String, description: String) {
        val timestamp = dateFormat.format(Date())
        synchronized(entries) {
            entries.add(0, LogEntry(title, description, timestamp))
            while (entries.size > LIMIT) {
                entries.removeAt(entries.lastIndex)
            }
        }
    }

    fun getLogs(): List<LogEntry> = entries
}
