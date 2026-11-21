@file:OptIn(kotlin.js.ExperimentalWasmJsInterop::class)

package io.github.youndie.mutant.surface

import org.jetbrains.skia.Bitmap
import org.jetbrains.skia.Image
import org.jetbrains.skia.ImageInfo
import kotlin.wasm.unsafe.UnsafeWasmMemoryApi
import kotlin.wasm.unsafe.withScopedMemoryAllocator

/**
 * `Image.makeRaster(info, bytes, rowBytes)` without its copy: on wasm Skiko moves a `ByteArray` into its memory one
 * byte per call across the JavaScript boundary, which was most of a body's brush in the browser, built at every tick of
 * every body (B-157). The bytes go into this module's linear memory, then into a fresh bitmap's pixels in Skiko's
 * memory in one block; the bitmap is immutable, so the image shares its pixels.
 */
@OptIn(UnsafeWasmMemoryApi::class)
internal fun rasterImage(
    info: ImageInfo,
    bytes: ByteArray,
    rowBytes: Int,
): Image {
    val bitmap = Bitmap()
    bitmap.allocPixels(info, rowBytes)
    val size = rowBytes * info.height
    if (size > 0) {
        val pixmap = checkNotNull(bitmap.peekPixels()) { "a bitmap just allocated has its pixels" }
        val target = pixmap.addr
        check(pixmap.rowBytes == rowBytes) { "rows as given" }
        pixmap.close()
        withScopedMemoryAllocator { allocator ->
            val source = allocator.allocate(size)
            for (i in 0 until size) (source + i).storeByte(bytes[i])
            copyIntoSkiko(loadedWasm, source.address.toInt(), target, size)
        }
        bitmap.notifyPixelsChanged()
    }
    bitmap.setImmutable()
    return Image.makeFromBitmap(bitmap)
}

@JsFun(
    "(skiko, source, target, size) => new Uint8Array(skiko._.memory.buffer, target, size)" +
        ".set(new Uint8Array(wasmExports.memory.buffer, source, size))",
)
internal external fun copyIntoSkiko(
    skiko: JsAny,
    source: Int,
    target: Int,
    size: Int,
)
