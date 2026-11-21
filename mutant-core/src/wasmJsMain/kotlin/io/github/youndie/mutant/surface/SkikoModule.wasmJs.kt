@file:JsModule("./skiko.mjs")
@file:OptIn(kotlin.js.ExperimentalWasmJsInterop::class)

package io.github.youndie.mutant.surface

/**
 * Skiko's emscripten module as `skiko.mjs` exports it: `_` holds its wasm exports, its linear `memory` among them, where
 * a Skia bitmap's pixels live. Read only by [rasterImage], to copy a texture's bytes in one block (B-157).
 */
internal external val loadedWasm: JsAny
