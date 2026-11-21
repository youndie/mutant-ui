package io.github.youndie.mutant.surface

import kotlinx.coroutines.Deferred
import kotlinx.coroutines.runBlocking

internal actual val firstFrameWaits: Boolean = true

internal actual fun <T> Deferred<T>.awaitHere(): T = runBlocking { await() }
