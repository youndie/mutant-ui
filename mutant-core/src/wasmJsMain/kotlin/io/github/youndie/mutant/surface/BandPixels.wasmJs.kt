@file:OptIn(kotlin.js.ExperimentalWasmJsInterop::class)

package io.github.youndie.mutant.surface

import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asComposeImageBitmap
import androidx.compose.ui.graphics.asSkiaBitmap
import org.jetbrains.skia.Bitmap
import org.jetbrains.skia.ColorAlphaType
import org.jetbrains.skia.ColorType
import org.jetbrains.skia.ImageInfo
import kotlin.wasm.unsafe.UnsafeWasmMemoryApi
import kotlin.wasm.unsafe.withScopedMemoryAllocator

internal actual val lossBandInMemory: Boolean = true

/** Read from Skiko's memory in one block (B-158), where `readPixels` would come a byte a call. */
@OptIn(UnsafeWasmMemoryApi::class)
internal actual fun readImagePixels(
    image: ImageBitmap,
    into: IntArray,
) {
    val b = image.asSkiaBitmap()
    val pm = b.peekPixels()
    // Compose's bitmaps are Skia's N32: RGBA or BGRA by platform, premultiplied, rows packed.
    val type = pm?.info?.colorType
    val direct =
        pm != null && (type == ColorType.RGBA_8888 || type == ColorType.BGRA_8888) &&
            pm.info.colorAlphaType == ColorAlphaType.PREMUL && pm.rowBytes == b.width * 4
    if (pm == null || !direct) {
        pm?.close()
        val info = ImageInfo(b.width, b.height, ColorType.RGBA_8888, ColorAlphaType.PREMUL)
        val bytes = b.readPixels(info, b.width * 4, 0, 0) ?: return
        for (i in into.indices) {
            into[i] = (bytes[4 * i].toInt() and 255) or ((bytes[4 * i + 1].toInt() and 255) shl 8) or
                ((bytes[4 * i + 2].toInt() and 255) shl 16) or ((bytes[4 * i + 3].toInt() and 255) shl 24)
        }
        return
    }
    val source = pm.addr
    pm.close()
    val n = minOf(into.size, b.width * b.height)
    withScopedMemoryAllocator { allocator ->
        val target = allocator.allocate(n * 4)
        copyFromSkiko(loadedWasm, source, target.address.toInt(), n * 4)
        if (type == ColorType.BGRA_8888) {
            for (i in 0 until n) {
                val v = (target + 4 * i).loadInt()
                into[i] = (v and 0xFF00FF00.toInt()) or ((v and 0xFF) shl 16) or ((v ushr 16) and 0xFF)
            }
        } else {
            for (i in 0 until n) into[i] = (target + 4 * i).loadInt()
        }
    }
}

/** Into Skiko's memory in one block (B-157's way), the bitmap immutable so drawing it does not copy it. */
@OptIn(UnsafeWasmMemoryApi::class)
internal actual fun imageOfPixels(
    px: IntArray,
    width: Int,
    height: Int,
): ImageBitmap {
    val bitmap = Bitmap()
    bitmap.allocPixels(ImageInfo(width, height, ColorType.RGBA_8888, ColorAlphaType.PREMUL), width * 4)
    val n = width * height
    if (n > 0) {
        val pixmap = checkNotNull(bitmap.peekPixels()) { "a bitmap just allocated has its pixels" }
        val target = pixmap.addr
        pixmap.close()
        withScopedMemoryAllocator { allocator ->
            val source = allocator.allocate(n * 4)
            for (i in 0 until n) (source + 4 * i).storeInt(px[i])
            copyIntoSkiko(loadedWasm, source.address.toInt(), target, n * 4)
        }
        bitmap.notifyPixelsChanged()
    }
    bitmap.setImmutable()
    return bitmap.asComposeImageBitmap()
}

@JsFun(
    "(skiko, source, target, size) => new Uint8Array(wasmExports.memory.buffer, target, size)" +
        ".set(new Uint8Array(skiko._.memory.buffer, source, size))",
)
private external fun copyFromSkiko(
    skiko: JsAny,
    source: Int,
    target: Int,
    size: Int,
)
