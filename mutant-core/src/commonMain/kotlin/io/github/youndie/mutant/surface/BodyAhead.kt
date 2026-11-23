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

    /** What the instance held was built for, null while none is (B-156). */
    val signatureNow: List<Any?>? get() = if (held == null) null else signature

    /** The instance held, null while none is (B-162). */
    val current: Built? get() = held

    /** Holds [built], made earlier for [signature] (B-162). */
    fun adopt(
        signature: List<Any?>,
        built: Built,
    ) {
        this.signature = signature
        pending = null
        held = built
    }

    // B-149: the first build started for the frame, and what it was started for.
    private var first: Deferred<Built>? = null
    private var firstFor: List<Any?>? = null

    /** Whether a first build was started and not taken yet. */
    val starting: Boolean get() = first != null

    /** Starts [build] for [signature] on a background thread, to be taken by [takeFirst] in this frame's draw. */
    fun startFirst(
        signature: List<Any?>,
        scope: CoroutineScope,
        build: () -> Built,
    ) {
        if (held != null || first != null) return
        firstFor = signature
        first = scope.async(aheadDispatcher) { build() }
    }

    /**
     * The first build started for [signature], waited for here; null if none was, or it was started for another
     * signature (the slot changed between layout and draw), which is then dropped.
     */
    fun takeFirst(signature: List<Any?>): Built? {
        val f = first ?: return null
        val forNow = firstFor == signature
        first = null
        firstFor = null
        if (!forNow) {
            f.cancel()
            return null
        }
        val built = f.awaitHere()
        this.signature = signature
        pending = null
        held = built
        return built
    }

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
     * Starts [build] on a background thread for [signature], once, and takes its result in the first frame after it is
     * done (B-153: never built in a frame for being late, as the browser's one thread runs such a build between frames
     * only when the frames leave it time). What the build made beside the instance goes to [taken] with it.
     */
    @OptIn(ExperimentalCoroutinesApi::class)
    fun <X> buildOffFrame(
        signature: List<Any?>,
        scope: CoroutineScope,
        taken: (Built, X) -> Unit,
        build: suspend () -> Pair<Built, X>,
    ) {
        if (signature == pending) return
        pending = signature
        val off: Deferred<Pair<Built, X>> = scope.async(aheadDispatcher) { build() }
        // Undispatched: the wait for the next frame is registered in this one, so the result is taken in the next.
        scope.launch(start = CoroutineStart.UNDISPATCHED) {
            withFrameNanos { }
            if (pending != signature) return@launch off.cancel()
            // Done by now, it is taken in this frame; else when it is, and its arrival asks for the next.
            val made = off.await()
            if (pending != signature) return@launch
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
 *
 * In the browser, whose one thread is the frame's too, a frame is never built in a frame for being late (B-155):
 * [take] with `orEarlier` hands over the latest frame done for the instance, a tick or more behind the clock, while the
 * next is still waiting for the thread; the body's boil then steps when the thread has time, not on every tick.
 */
internal class FrameAhead {
    private var instance: Instance? = null
    private var at = Double.NaN
    private var job: Deferred<Ahead>? = null

    // B-155: the latest frame handed over or done, and its instance and time.
    private var last: Ahead? = null
    private var lastFor: Instance? = null
    private var lastAt = Double.NaN

    /**
     * The frame built ahead for [g] at [t], once it is done; with [wait] (a test's [LocalMutantTickWaits], B-173), a frame
     * prepared for that very instance and time is waited for here instead of being missed while its thread is late.
     */
    @OptIn(ExperimentalCoroutinesApi::class)
    fun take(
        g: Instance,
        t: Double,
        orEarlier: Boolean = false,
        wait: Boolean = false,
    ): Ahead? {
        val j = job
        if (wait && j != null && !j.isCompleted && instance === g && at == t) runCatching { j.awaitHere() }
        val done = j != null && j.isCompleted && j.getCompletionExceptionOrNull() == null
        if (done && instance === g && (at == t || (orEarlier && at <= t))) {
            // A frame cut at its grid's edge is built again in the frame, where the grid grows (B-97).
            val a = j.getCompleted().takeIf { !it.frame.field.cut } ?: return null
            last = a
            lastFor = g
            lastAt = at
            return a
        }
        return last.takeIf { orEarlier && lastFor === g && lastAt <= t }
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
        build: suspend (band: Rect?) -> Ahead,
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

    /** The latest frame done for [g] and its time, if any (B-162). */
    @OptIn(ExperimentalCoroutinesApi::class)
    fun latest(g: Instance): Pair<Ahead, Double>? {
        val j = job
        if (instance === g && j != null && j.isCompleted && j.getCompletionExceptionOrNull() == null) {
            return j.getCompleted() to at
        }
        val l = last ?: return null
        return if (lastFor === g) l to lastAt else null
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
        last = null
        lastFor = null
    }
}

/**
 * What one surface keeps of its body across ticks: its instance, its frame ahead and its still layers. When the
 * surface leaves the composition (a tab left, a list item scrolled out and disposed) they go to [BodyPool], and a
 * surface composed later for the very same signature takes them back in its first draw: a screen visited again draws
 * its bodies at once, from their last frame, with nothing to build (B-156).
 */
internal class BodyKeep : androidx.compose.runtime.RememberObserver {
    var held = HeldInstance()
        private set
    var ahead = FrameAhead()
        private set
    var stillLayers = StillLayers()
        private set

    /** Whether this surface's body goes to the pool when it leaves (set by the surface at each composition). */
    var pooled = false

    // B-162: the instances this surface held before, by signature, each with its latest frame done: at most RECENT.
    private val recent = LinkedHashMap<List<Any?>, Pair<Built, Pair<Ahead, Double>?>>()

    /**
     * The instance for [signature]: the one held, or one this surface held before for it (B-162) — a navigation bar's
     * clean zone, which its pill widens at either end, takes three signatures as the pill moves — with its latest frame
     * done; else [build]'s.
     */
    fun of(
        signature: List<Any?>,
        build: () -> Built,
    ): Built {
        val h = held.current
        val was = held.signatureNow
        if (h != null && signature == was) return h
        if (h != null && was != null) {
            recent.remove(was)
            recent[was] = h to ahead.latest(h.first)
            while (recent.size > RECENT) recent.remove(recent.keys.first())
        }
        val back = recent.remove(signature) ?: return held.of(signature, build)
        held.adopt(signature, back.first)
        back.second?.let { (a, at) -> ahead.preset(back.first.first, at, a) }
        return back.first
    }

    /** Whether this surface held an instance for [signature] before, and keeps it (B-162). */
    fun hasRecent(signature: List<Any?>): Boolean = recent.containsKey(signature)

    /** Takes back what a surface of [signature] left in the pool; false if there is none, or this one has its own. */
    fun restore(signature: List<Any?>): Boolean {
        if (!pooled || !held.isEmpty || held.starting) return false
        val k = BodyPool.take(signature) ?: return false
        held = k.held
        ahead = k.ahead
        stillLayers = k.stillLayers
        return true
    }

    override fun onRemembered() {}

    override fun onForgotten() {
        if (pooled) held.signatureNow?.let { BodyPool.give(it, this) }
    }

    override fun onAbandoned() {}
}

/** B-162: the instances a surface keeps besides the one it holds. */
private const val RECENT = 3

/**
 * Bodies whose surfaces left the composition, by signature, the most recently left last, at most [MAX] of them: a
 * body is some 0.3 to 1 MB of field and frame, so the pool holds about four screens of them (B-156).
 */
internal object BodyPool {
    private const val MAX = 48
    private val kept = LinkedHashMap<List<Any?>, BodyKeep>()

    /** Whether a body of [cassette] and [key] is kept, of whatever size (a signature's first two entries). */
    fun holds(
        cassette: String,
        key: String,
    ): Boolean = kept.keys.any { it[0] == cassette && it[1] == key }

    fun take(signature: List<Any?>): BodyKeep? = kept.remove(signature)

    fun give(
        signature: List<Any?>,
        keep: BodyKeep,
    ) {
        kept.remove(signature)
        kept[signature] = keep
        while (kept.size > MAX) kept.remove(kept.keys.first())
    }

    /** For tests: the pool emptied, as at a fresh start. */
    fun clear() = kept.clear()
}

/**
 * The last frame built in a frame, for its instance, time, heat (the tremble's and the press's, B-193) and
 * whether it has paths (B-160). A body redrawn on every
 * frame — sliding home, pouring a new fill, boiling — asked the generator for the same tick's frame each time; between
 * two ticks the frame is the same, and is taken from here.
 */
internal class FrameMemo {
    private var instance: Instance? = null
    private var at = Double.NaN
    private var heat = Double.NaN
    private var pressHeat = Double.NaN
    private var pressed = false
    private var paths = false
    private var frame: BodyFrame? = null

    fun of(
        g: Instance,
        t: Double,
        k: Double,
        pressK: Double,
        pressed: Boolean,
        paths: Boolean,
        build: () -> BodyFrame,
    ): BodyFrame {
        val f = frame
        if (f != null && instance === g && at == t && heat == k && pressHeat == pressK && this.pressed == pressed &&
            this.paths == paths
        ) {
            return f
        }
        return build().also {
            frame = it
            instance = g
            at = t
            heat = k
            pressHeat = pressK
            this.pressed = pressed
            this.paths = paths
        }
    }
}

/**
 * Runs [step] (a count of columns in, whether done out) until it is done, letting the thread go to the page's frames
 * every [BUILD_STEP_MS] (B-161): each resumption is a task of its own on the browser's [aheadDispatcher].
 */
internal suspend inline fun inSteps(step: (Int) -> Boolean) {
    var since =
        kotlin.time.TimeSource.Monotonic
            .markNow()
    while (!step(COLUMNS_A_STEP)) {
        if (since.elapsedNow().inWholeMicroseconds >= BUILD_STEP_MS * 1000) {
            kotlinx.coroutines.yield()
            since =
                kotlin.time.TimeSource.Monotonic
                    .markNow()
        }
    }
}

/** B-161: a build's columns between two looks at the time, and the time it holds the thread before letting it go, ms. */
internal const val COLUMNS_A_STEP = 8
internal const val BUILD_STEP_MS = 4
