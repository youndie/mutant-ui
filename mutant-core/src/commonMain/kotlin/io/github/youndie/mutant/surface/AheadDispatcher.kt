package io.github.youndie.mutant.surface

/**
 * Where a body's builds off the frame run (B-50, B-75, B-82): `Dispatchers.Default`, but in the browser, where it is the
 * one thread, each build is a task of its own, so a tick's builds do not all land between the same two frames (B-87).
 */
internal expect val aheadDispatcher: kotlinx.coroutines.CoroutineDispatcher
