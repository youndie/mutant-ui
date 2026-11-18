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
    val image =
        Image.makeRaster(
            ImageInfo(nx, ny, ColorType.RGBA_8888, ColorAlphaType.OPAQUE, ColorSpace.sRGB),
            fieldTexels(field),
            nx * 4,
        )
    val texture =
        image.makeShader(FilterTileMode.CLAMP, FilterTileMode.CLAMP, FilterMipmap(FilterMode.NEAREST, MipmapMode.NONE))
    val ox = slot.ox.toFloat()
    val oy = slot.oy.toFloat()
    val w = slot.w.toFloat()
    val h = slot.h.toFloat()
    val r = slot.r.toFloat()
    val over = slot.over.toFloat()
    val step = field.step.toFloat()
    val b = RuntimeShaderBuilder(effect)
    b.child("field", texture)
    b.uniform("origin", (field.x0 - field.pad * field.step).toFloat(), (field.y0 - field.pad * field.step).toFloat())
    b.uniform("stepDp", step)
    b.uniform("dims", nx.toFloat(), ny.toFloat())
    b.uniform("pxDp", 1f / density)
    b.uniform("slip", look.slip)
    b.uniform("outlineW", look.outline)
    // The outline reaches half its width and half a pixel from the line: one cell around, or two where cells are small.
    b.uniform("rOutline", if (step >= look.outline / 2 + .5f / density) 1f else 2f)
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
