package com.jamiecarl.droneside.data.live

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream
import java.io.Writer
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.zip.GZIPOutputStream

/**
 * Records a live session for debugging, in the same layout as tools/capture-live.sh so either kind of capture
 * replays the same way:
 *
 * - `stream.log`: every SSE frame as `<epoch ms> event: <type>` / `<epoch ms> data: <json>` / `<epoch ms> `, plus
 *   `<epoch ms> # <note>` lines (connects, drops, alerts sent).
 * - `snapshots/<epoch ms>.html.gz`: the dashboard page, at most every [SNAPSHOT_EVERY_MS] unless the current
 *   race changed (then straight away, as those are the interesting moments).
 *
 * Writes happen in order on a background thread; [close] flushes them.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class LiveRecorder(
    val dir: File,
    private val now: () -> Long = System::currentTimeMillis,
    private val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.IO.limitedParallelism(1))
) {
    private val snapshots = File(dir, "snapshots").apply { mkdirs() }
    private val log: Writer = File(dir, "stream.log").bufferedWriter()
    private var lastSnapshotAt = 0L
    private var lastSignature: Any? = null

    fun frame(type: String, data: String) {
        val at = now()
        write("$at event: $type\n$at data: $data\n$at \n")
    }

    fun note(text: String) {
        write("${now()} # $text\n")
    }

    fun snapshot(html: String, parsed: LiveSnapshot?) {
        val at = now()
        val signature = parsed?.current?.let { it.id to it.state }
        if (at - lastSnapshotAt < SNAPSHOT_EVERY_MS && signature == lastSignature) return
        lastSnapshotAt = at
        lastSignature = signature
        scope.launch {
            GZIPOutputStream(FileOutputStream(File(snapshots, "$at.html.gz"))).bufferedWriter().use { it.write(html) }
        }
    }

    fun close() {
        scope.launch {
            log.close()
        }.invokeOnCompletion { scope.cancel() }
    }

    private fun write(text: String) {
        scope.launch {
            log.write(text)
            log.flush()
        }
    }

    companion object {
        const val SNAPSHOT_EVERY_MS = 15_000L

        /** A fresh folder under [root] named for the club and start time, e.g. `cmrc-20261106-1802`. */
        fun start(root: File, clubUrl: String, now: Long = System.currentTimeMillis()): LiveRecorder {
            val slug = clubUrl.substringAfter("://").removePrefix("www.").substringBefore('.')
            val stamp = SimpleDateFormat("yyyyMMdd-HHmmss", Locale.US).format(Date(now))
            return LiveRecorder(File(root, "$slug-$stamp").apply { mkdirs() }).also {
                it.note("recording $clubUrl")
            }
        }
    }
}
