package io.github.youndie.mutant.surface

import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Brush
import io.github.youndie.mutant.gen.DistanceField
import io.github.youndie.mutant.gen.Slot

/**
 * B-110, a spike: whether a body is drawn by a GPU shader from its field instead of by paths. The shader draws the
 * silhouette only — the misregistered shadow, the fill shifted by the slip, the highlight and its crescent inside
 * their clips, the outline — so the decorations (speckle, mosaic, dead pixels, flow lines) are not drawn with it.
 * Off by default; a measurement turns it on.
 */
internal val LocalMutantBodyOnGpu = staticCompositionLocalOf { BODY_ON_GPU }

/** The spike's default, for a build that measures the shader in the browser. */
internal const val BODY_ON_GPU: Boolean = false

/** Whether this platform can draw a body by shader (B-110): Skia's runtime effects on desktop and in the browser. */
internal expect val bodyShaderSupported: Boolean

/**
 * The body's silhouette as a brush over scene coordinates (the generator's, in dp), from [field] at [density] device
 * pixels a dp: the field is handed to the shader as a texture, read bilinearly and thresholded per pixel. Null where
 * the platform has no shader.
 */
internal expect fun bodyShaderBrush(
    field: DistanceField,
    slot: Slot,
    look: BodyLook,
    density: Float,
): Brush?

/** The SkSL of [bodyShaderBrush], shared by the platforms that run it. */
internal const val BODY_SKSL: String = """
uniform shader field;
uniform float2 origin;
uniform float stepDp;
uniform float2 dims;
uniform float pxDp;
uniform float slip;
uniform float outlineW;
uniform half4 fillC;
uniform half4 inkC;
uniform half4 hlC;
uniform half4 shadowC;
uniform float4 clipA;
uniform float4 clipB;

float texel(float2 ij) {
    ij = clamp(ij, float2(0.0), dims - 1.0);
    half4 c = field.eval(ij + 0.5);
    float hi = floor(float(c.r) * 255.0 + 0.5);
    float lo = floor(float(c.g) * 255.0 + 0.5);
    return (hi * 256.0 + lo) / 65535.0 * 64.0 - 32.0;
}

// The field at p, as a distance: the bilinear value over the length of its gradient in the cell, where that is under 1.
// A distance field's gradient is about 1 and this changes little; inside a mosaic block, whose cells hold one value,
// there is none, so no outline fills the block. A steeper gradient (a block's border, a seam of the field) is left as
// it is, or a seam far outside the body would read as an edge.
float F(float2 p) {
    float2 g = (p - origin) / stepDp;
    if (g.x < 0.0 || g.y < 0.0 || g.x > dims.x - 1.0 || g.y > dims.y - 1.0) return 32.0;
    float2 i0 = floor(g);
    float2 f = g - i0;
    float a = texel(i0);
    float b = texel(i0 + float2(1.0, 0.0));
    float c = texel(i0 + float2(0.0, 1.0));
    float d = texel(i0 + float2(1.0, 1.0));
    float v = mix(mix(a, b, f.x), mix(c, d, f.x), f.y);
    float2 grad = float2(mix(b - a, d - c, f.y), mix(c - a, d - b, f.x)) / stepDp;
    return v / clamp(length(grad), 0.05, 1.0);
}

float cov(float dist) { return clamp(0.5 - dist / pxDp, 0.0, 1.0); }

bool inRect(float2 p, float4 r) { return p.x >= r.x && p.y >= r.y && p.x < r.z && p.y < r.w; }

half4 over(half4 src, half4 dst) { return src + dst * (1.0 - src.a); }

half4 main(float2 p) {
    half4 col = half4(0.0);
    float d0 = F(p);
    if (shadowC.a > 0.0) col = over(shadowC * half(cov(F(p - 4.0))), col);
    col = over(fillC * half(cov(F(p - slip))), col);
    if (inRect(p, clipA) || inRect(p, clipB)) {
        col = over(hlC * half(cov(d0 + 3.4)), col);
        col = over(fillC * half(cov(F(p - 1.5) + 3.4)), col);
    }
    col = over(inkC * half(cov(abs(d0) - outlineW * 0.5)), col);
    return col;
}
"""
