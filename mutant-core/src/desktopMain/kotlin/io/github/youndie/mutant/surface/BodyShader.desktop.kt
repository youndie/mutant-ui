package io.github.youndie.mutant.surface

import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ShaderBrush
import androidx.compose.ui.graphics.asComposeShader
import androidx.compose.ui.graphics.asSkiaBitmap
import io.github.youndie.mutant.gen.DistanceField
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

internal actual val bodyShaderSupported: Boolean = true

internal actual val bodyOnGpuByDefault: Boolean = false

private val effect by lazy { RuntimeEffect.makeForShader(BODY_SKSL) }

private val nearest = FilterMipmap(FilterMode.NEAREST, MipmapMode.NONE)

internal actual fun bodyShaderBrush(
    field: DistanceField,
    slot: Slot,
    look: BodyLook,
    density: Float,
    still: StillImage?,
    flow: GenPath,
): Brush? {
    // The flow lines' coverage reaches half a pixel past their edge, and a little.
    val flows = if (flow.isEmpty) null else flowBins(flow, .5f / density + .25f) ?: return null
    val nx = field.nx
    val ny = field.ny
    val image =
        Image.makeRaster(
            ImageInfo(nx, ny, ColorType.RGB_565, ColorAlphaType.OPAQUE, ColorSpace.sRGB),
            fieldTexels(field),
            nx * 2,
        )
    val texture = image.makeShader(FilterTileMode.CLAMP, FilterTileMode.CLAMP, nearest)
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
    if (still != null) {
        b.child(
            "still",
            still.image
                .asSkiaBitmap()
                .let {
                    Image.makeFromBitmap(it)
                }.makeShader(FilterTileMode.DECAL, FilterTileMode.DECAL, nearest),
        )
        b.uniform("hasStill", 1f)
        b.uniform("stillGrid", still.gridX, still.gridY)
        b.uniform("stillAt", still.at.x.toFloat(), still.at.y.toFloat())
    } else {
        b.child("still", texture)
        b.uniform("hasStill", 0f)
        b.uniform("stillGrid", 0f, 0f)
        b.uniform("stillAt", 0f, 0f)
    }
    b.uniform("dens", density)
    if (flows != null) {
        b.child("flowBins", raster(flows.bins, flows.binsW, flows.binsH))
        b.child("flow", words565(flows.entries))
        b.child("flowSegs", words565(flows.segs))
        b.uniform("hasFlow", 1f)
        b.uniform("flowOrigin", flows.originX, flows.originY)
        b.uniform("flowDims", flows.binsW.toFloat(), flows.binsH.toFloat())
    } else {
        b.child("flowBins", texture)
        b.child("flow", texture)
        b.child("flowSegs", texture)
        b.uniform("hasFlow", 0f)
        b.uniform("flowOrigin", 0f, 0f)
        b.uniform("flowDims", 0f, 0f)
    }
    return ShaderBrush(b.makeShader().asComposeShader())
}

/** 16-bit words as `RGB_565` texels, read texel by texel. */
private fun words565(words: Words) =
    Image
        .makeRaster(
            ImageInfo(words.w, words.h, ColorType.RGB_565, ColorAlphaType.OPAQUE, ColorSpace.sRGB),
            words.bytes,
            words.w * 2,
        ).makeShader(FilterTileMode.CLAMP, FilterTileMode.CLAMP, nearest)

/** Opaque RGBA bytes as a texture read texel by texel. */
private fun raster(
    bytes: ByteArray,
    w: Int,
    h: Int,
) = Image
    .makeRaster(ImageInfo(w, h, ColorType.RGBA_8888, ColorAlphaType.OPAQUE, ColorSpace.sRGB), bytes, w * 4)
    .makeShader(FilterTileMode.CLAMP, FilterTileMode.CLAMP, nearest)

/** A colour as the shader's premultiplied `half4`. */
private fun RuntimeShaderBuilder.color(
    name: String,
    c: Color,
) = uniform(name, c.red * c.alpha, c.green * c.alpha, c.blue * c.alpha, c.alpha)
