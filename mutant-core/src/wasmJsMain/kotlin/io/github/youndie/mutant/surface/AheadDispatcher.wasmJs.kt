package io.github.youndie.mutant.surface

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Runnable
import kotlin.coroutines.CoroutineContext

/**
 * The browser has one thread, and `Dispatchers.Default` runs up to sixteen of its queued coroutines in one task, so a
 * tick's builds for every body landed between two frames together (B-87). Here each build is a task of its own, at the
 * scheduler's `background` priority (`scheduler.postTask`), after the page's input, its frames and its other tasks;
 * where there is no scheduler, posted through a `MessageChannel`.
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

/** A `MessageChannel` whose every message runs [run]; the returned function posts one. */
@OptIn(kotlin.js.ExperimentalWasmJsInterop::class)
@JsFun(
    "(run) => { const c = new MessageChannel(); c.port1.onmessage = () => run(); return () => c.port2.postMessage(0); }",
)
private external fun channel(run: () -> Unit): JsAny

@OptIn(kotlin.js.ExperimentalWasmJsInterop::class)
@JsFun("(post) => post()")
private external fun send(post: JsAny)
