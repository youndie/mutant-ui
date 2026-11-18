package io.github.youndie.mutant.surface

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Runnable
import kotlin.coroutines.CoroutineContext

/**
 * The browser has one thread, and `Dispatchers.Default` runs up to sixteen of its queued coroutines in one task, so a
 * tick's builds for every body landed between two frames together (B-87). Here each build is a task of its own, at the
 * scheduler's `user-visible` priority (`scheduler.postTask`), between frames with the page's other tasks; where there is
 * no scheduler, posted through a `MessageChannel`. At `background` they ran only on an idle page, and a page drawing every
 * frame on a busy thread never was: slowed as the phone runs, no body reached its tick image (B-122).
 */
internal actual val aheadDispatcher: CoroutineDispatcher = OneTaskEach

private object OneTaskEach : CoroutineDispatcher() {
    private val queue = ArrayDeque<Runnable>()

    @OptIn(kotlin.js.ExperimentalWasmJsInterop::class)
    private val post: JsAny = channel { queue.removeFirstOrNull()?.run() }

    @OptIn(kotlin.js.ExperimentalWasmJsInterop::class)
    override fun dispatch(
        context: CoroutineContext,
        block: Runnable,
    ) {
        queue.addLast(block)
        send(post)
    }
}

/** A function that posts one task running [run]: a background task of the scheduler, or a `MessageChannel`'s message. */
@OptIn(kotlin.js.ExperimentalWasmJsInterop::class)
@JsFun(
    "(run) => { const s = globalThis.scheduler; " +
        "if (s && s.postTask) return () => { s.postTask(() => run(), { priority: 'user-visible' }); }; " +
        "const c = new MessageChannel(); c.port1.onmessage = () => run(); return () => c.port2.postMessage(0); }",
)
private external fun channel(run: () -> Unit): JsAny

@OptIn(kotlin.js.ExperimentalWasmJsInterop::class)
@JsFun("(post) => post()")
private external fun send(post: JsAny)
