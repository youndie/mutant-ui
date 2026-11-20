package io.github.youndie.mutant.surface

import kotlinx.coroutines.Deferred

/**
 * B-149: the first builds of the bodies a frame shows, started together. A body with no instance yet asks at layout
 * ([ask]); the first body drawn in the frame starts every build asked for ([start]), by then every clean zone of the
 * frame registered, so the builds run side by side on the platform's threads while each body's draw waits for its own.
 * Read and written on the frame's thread alone.
 */
internal object FirstBuilds {
    private val asked = ArrayList<() -> Unit>()

    fun ask(start: () -> Unit) {
        asked += start
    }

    fun start() {
        if (asked.isEmpty()) return
        val now = asked.toList()
        asked.clear()
        for (s in now) s()
    }
}

/**
 * Whether a draw may wait for a build on another thread: true where the platform has threads to build on (desktop,
 * Android, iOS), false in the browser, whose one thread is the frame's own.
 */
internal expect val firstFrameWaits: Boolean

/** Waits on this thread for [this]; only where [firstFrameWaits]. */
internal expect fun <T> Deferred<T>.awaitHere(): T
