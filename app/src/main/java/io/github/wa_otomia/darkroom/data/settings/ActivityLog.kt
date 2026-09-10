package io.github.wa_otomia.darkroom.data.settings

import io.github.wa_otomia.darkroom.core.ActivityEntry
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import java.time.Instant
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ActivityLog @Inject constructor() {
    private val _entries = MutableStateFlow<List<ActivityEntry>>(emptyList())
    val entries: StateFlow<List<ActivityEntry>> = _entries

    fun record(kind: String, message: String, status: String = "ok", error: String? = null) {
        val entry = ActivityEntry(
            id = UUID.randomUUID().toString(),
            at = Instant.now().toString(),
            kind = kind,
            message = message,
            status = status,
            error = error,
        )
        _entries.update { (listOf(entry) + it).take(200) }
    }

    /**
     * Drops every entry. The log lives only in memory, so there is nothing else to purge.
     *
     * [record] is called from the FTP service, print coroutines and the Grok client, but the write
     * below is a single atomic assignment: a concurrent [record] either lands before it and is
     * discarded, or loses its compare-and-set and retries against the emptied list.
     */
    fun clear() {
        _entries.value = emptyList()
    }
}
