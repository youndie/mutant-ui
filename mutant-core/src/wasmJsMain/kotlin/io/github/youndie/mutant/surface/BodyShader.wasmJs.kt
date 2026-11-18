package io.github.youndie.mutant.surface

import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ShaderBrush
import androidx.compose.ui.graphics.asComposeShader
import io.github.youndie.mutant.gen.GenPath
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
import org.jetbrains.skia.Shader

internal actual val bodyShaderSupported: Boolean = true

private val effect by lazy { RuntimeEffect.makeForShader(BODY_SKSL) }

internal actual fun bodyShaderBrush(
    contour: GenPath,
    highlight: GenPath,
    box: Rect,
    slot: Slot,
    look: BodyLook,
    density: Float,
): Brush? {
    // Exact where a pixel can tell: half the outline, the pixel its coverage spreads over, and a grid cell's diagonal more,
    // so that bilinear reads inside that reach use exact nodes only.
    val band = look.outline / 2 + 1 / density + GRID_DP * 1.5f + .25f
    val c = distanceGrid(contour, box, band)
    val h = distanceGrid(highlight, box, band)
    val ox = slot.ox.toFloat()
    val oy = slot.oy.toFloat()
    val w = slot.w.toFloat()
    val hh = slot.h.toFloat()
    val r = slot.r.toFloat()
    val over = slot.over.toFloat()
    val b = RuntimeShaderBuilder(effect)
    b.child("cDist", texture(c.texels, c.nx, c.ny))
    b.child("hDist", texture(h.texels, h.nx, h.ny))
    b.uniform("gridOrigin", c.originX, c.originY)
    b.uniform("gridDims", c.nx.toFloat(), c.ny.toFloat())
    b.uniform("band", band)
    b.uniform("pxDp", 1f / density)
    b.uniform("slip", look.slip)
    b.uniform("outlineW", look.outline)
    b.color("fillC", look.fill)
    b.color("inkC", look.ink)
    b.color("hlC", look.highlight)
    b.color("shadowC", look.shadow ?: Color.Transparent)
    b.uniform("clipA", ox - 6, oy + hh + .5f, ox + w + 6, oy + hh + .5f + over + 30)
    b.uniform("clipB", ox - 4, oy - 6, ox - 4 + r + 13, oy - 6 + r + 10)
    return ShaderBrush(b.makeShader().asComposeShader())
}

/** RGBA bytes as a texture read texel by texel. */
private fun texture(
    bytes: ByteArray,
    w: Int,
    h: Int,
): Shader =
    Image
        .makeRaster(ImageInfo(w, h, ColorType.RGBA_8888, ColorAlphaType.OPAQUE, ColorSpace.sRGB), bytes, w * 4)
        .makeShader(FilterTileMode.CLAMP, FilterTileMode.CLAMP, FilterMipmap(FilterMode.NEAREST, MipmapMode.NONE))

/** A colour as the shader's premultiplied `half4`. */
private fun RuntimeShaderBuilder.color(
    name: String,
    c: Color,
) = uniform(name, c.red * c.alpha, c.green * c.alpha, c.blue * c.alpha, c.alpha)
