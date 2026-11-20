package io.github.youndie.mutant.surface

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.geometry.Rect
import io.github.youndie.mutant.gen.BodyFrame
import io.github.youndie.mutant.gen.FieldCache
import io.github.youndie.mutant.gen.Instance
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.launch

private typealias Built =
    Triple<io.github.youndie.mutant.gen.Instance, FieldCache, io.github.youndie.mutant.gen.DeadPixels?>

/**
 * One surface's instance and field cache, kept across ticks and rebuilt only when what shapes the instance
 * changes (cassette, key, size, wear, profile, clean zone): a fresh cache makes a cold frame, ten times a warm
 * one (B-06).
 *
 * The first instance can be built off the frame ([buildOffFrame], B-50): on a background thread from the frame
 * that first knows the slot, and taken in the next frame — finished there, or built then if the thread is late,
 * so the body always draws from that next frame and the same picture either way. Later rebuilds (a new size, a
 * focus's wear) stay in their frame, so a resizing window never shows a body missing.
 */
internal class HeldInstance {
    private var signature: List<Any?>? = null
    private var held: Built? = null
    private var pending: List<Any?>? = null

    /** Bumped when an instance built off the frame is taken; a draw cache reads it to be invalidated. */
    var arrivals: Int by mutableIntStateOf(0)
        private set

    /** No instance held yet: the body has not drawn. */
    val isEmpty: Boolean get() = held == null

    fun of(
        signature: List<Any?>,
        build: () -> Built,
    ): Built {
        val h = held
        if (h != null && signature == this.signature) return h
        this.signature = signature
        pending = null
        return build().also { held = it }
    }

    /**
     * Starts [build] on a background thread for [signature], once, and takes its result in the next frame, built there
     * if the thread is late. What the build made beside the instance goes to [taken] with it, whichever built it.
     */
    @OptIn(ExperimentalCoroutinesApi::class)
    fun <X> buildOffFrame(
        signature: List<Any?>,
        scope: CoroutineScope,
        taken: (Built, X) -> Unit,
        build: () -> Pair<Built, X>,
    ) {
        if (signature == pending) return
        pending = signature
        val off: Deferred<Pair<Built, X>> = scope.async(aheadDispatcher) { build() }
        // Undispatched: the wait for the next frame is registered in this one, so the result is taken in the next.
        scope.launch(start = CoroutineStart.UNDISPATCHED) {
            withFrameNanos { }
            if (pending != signature) return@launch off.cancel()
            val done = if (off.isCompleted && off.getCompletionExceptionOrNull() == null) off.getCompleted() else null
            if (done == null) off.cancel()
            val made = done ?: build()
            if (of(signature) { made.first } === made.first) taken(made.first, made.second)
            arrivals++
        }
    }
}

/** A tick's frame built ahead, with its image when one was asked for and the inputs that image was drawn with. */
internal class Ahead(
    val frame: BodyFrame,
    val image: RasterisedBody?,
    val inputs: List<Any?>?,
    /** B-110: the shader's brush, built ahead with the frame, for a body the shader draws. */
    val shaded: androidx.compose.ui.graphics.Brush? = null,
)

/**
 * The next resting tick's frame of one body, built on a background thread (B-75): [prepare] starts it for an
 * instance and a time, once; [take] hands it over only for that very instance and time, and only once it is done.
 */
internal class FrameAhead {
    private var instance: Instance? = null
    private var at = Double.NaN
    private var job: Deferred<Ahead>? = null

    @OptIn(ExperimentalCoroutinesApi::class)
    fun take(
        g: Instance,
        t: Double,
    ): Ahead? {
        val j = job ?: return null
        if (instance !== g || at != t || !j.isCompleted || j.getCompletionExceptionOrNull() != null) return null
        // A frame cut at its grid's edge is built again in the frame, where the grid grows (B-97).
        return j.getCompleted().takeIf { !it.frame.field.cut }
    }

    private var band: Rect? = null

    /**
     * Builds the frame for [t] once, with an image of the body around [shown] (B-86); again for the same [t] only when
     * the window has moved out of the band the first build was given.
     */
    fun prepare(
        g: Instance,
        t: Double,
        shown: Rect?,
        scope: CoroutineScope,
        build: (band: Rect?) -> Ahead,
    ) {
        val b = band
        val inside =
            (shown == null && b == null) ||
                (
                    shown != null && b != null && shown.left >= b.left && shown.top >= b.top &&
                        shown.right <= b.right && shown.bottom <= b.bottom
                )
        if (instance === g && at == t && inside) return
        job?.cancel()
        instance = g
        at = t
        val next = shown?.let { bandAround(it) }
        band = next
        job = scope.async(aheadDispatcher) { build(next) }
    }

    /** Hands over a frame built with the body's first instance, for [t] (B-83). */
    fun preset(
        g: Instance,
        t: Double,
        built: Ahead,
    ) {
        job?.cancel()
        instance = g
        at = t
        band = null
        job = CompletableDeferred(built)
    }
}
