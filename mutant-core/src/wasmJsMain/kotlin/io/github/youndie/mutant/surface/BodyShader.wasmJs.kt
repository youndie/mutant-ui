package io.github.youndie.mutant.surface

import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ShaderBrush
import androidx.compose.ui.graphics.asComposeShader
import io.github.youndie.mutant.gen.DistanceField
import io.github.youndie.mutant.gen.Slot
import org.jetbrains.skia.ColorAlphaType
import org.jetbrains.skia.ColorSpace
import org.jetbrains.skia.ColorType
import org.jetbrains.skia.FilterMipmap
import org.jetbrains.skia.FilterMode
import org.jetbrains.skia.FilterTileMode
import org.jetbrains.skia.Image
import org.jetbrains.skia.ImageInfo
import org.jetbrains.skia.MipmapMode
import org.jetbrains.skia.RuntimeEffect
import org.jetbrains.skia.RuntimeShaderBuilder
import kotlin.math.roundToInt

internal actual val bodyShaderSupported: Boolean = true

private val effect by lazy { RuntimeEffect.makeForShader(BODY_SKSL) }

internal actual fun bodyShaderBrush(
    field: DistanceField,
    slot: Slot,
    look: BodyLook,
    density: Float,
): Brush? {
    val nx = field.nx
    val ny = field.ny
    val bytes = ByteArray(nx * ny * 4)
    for (k in 0 until nx * ny) {
        val v = ((field.values[k].coerceIn(-32f, 32f) + 32f) / 64f * 65535f).roundToInt()
        bytes[4 * k] = (v shr 8).toByte()
        bytes[4 * k + 1] = (v and 255).toByte()
        bytes[4 * k + 3] = 255.toByte()
    }
    val image =
        Image.makeRaster(ImageInfo(nx, ny, ColorType.RGBA_8888, ColorAlphaType.OPAQUE, ColorSpace.sRGB), bytes, nx * 4)
    val texture =
        image.makeShader(FilterTileMode.CLAMP, FilterTileMode.CLAMP, FilterMipmap(FilterMode.NEAREST, MipmapMode.NONE))
    val ox = slot.ox.toFloat()
    val oy = slot.oy.toFloat()
    val w = slot.w.toFloat()
    val h = slot.h.toFloat()
    val r = slot.r.toFloat()
    val over = slot.over.toFloat()
    val b = RuntimeShaderBuilder(effect)
    b.child("field", texture)
    b.uniform("origin", (field.x0 - field.pad * field.step).toFloat(), (field.y0 - field.pad * field.step).toFloat())
    b.uniform("stepDp", field.step.toFloat())
    b.uniform("dims", nx.toFloat(), ny.toFloat())
    b.uniform("pxDp", 1f / density)
    b.uniform("slip", look.slip)
    b.uniform("outlineW", look.outline)
    b.color("fillC", look.fill)
    b.color("inkC", look.ink)
    b.color("hlC", look.highlight)
    b.color("shadowC", look.shadow ?: Color.Transparent)
    b.uniform("clipA", ox - 6, oy + h + .5f, ox + w + 6, oy + h + .5f + over + 30)
    b.uniform("clipB", ox - 4, oy - 6, ox - 4 + r + 13, oy - 6 + r + 10)
    return ShaderBrush(b.makeShader().asComposeShader())
}

/** A colour as the shader's premultiplied `half4`. */
private fun RuntimeShaderBuilder.color(
    name: String,
    c: Color,
) = uniform(name, c.red * c.alpha, c.green * c.alpha, c.blue * c.alpha, c.alpha)
