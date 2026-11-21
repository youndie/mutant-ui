package io.github.youndie.mutant.surface

import kotlinx.coroutines.Deferred

// The browser's one thread is the frame's: a build waited for here could never run.
internal actual val firstFrameWaits: Boolean = false

internal actual fun <T> Deferred<T>.awaitHere(): T = error("no waiting on the browser's one thread")
