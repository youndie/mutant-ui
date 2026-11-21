@file:OptIn(kotlin.js.ExperimentalWasmJsInterop::class)

package io.github.youndie.mutant.gen

/** The browser's `Math.sin` and `Math.cos`, called from the module (B-152). */
@JsFun("Math.sin")
internal actual external fun jsSin(x: Double): Double

@JsFun("Math.cos")
internal actual external fun jsCos(x: Double): Double
