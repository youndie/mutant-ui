package io.github.youndie.mutant.surface

import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Brush
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
 * The body's silhouette as a brush over scene coordinates (the generator's, in dp), drawn from [contour] and [highlight],
 * the paths' own curves, binned over [box] (B-128), at [density] device pixels a dp. Null where the platform has no
 * shader.
 */
internal expect fun bodyShaderBrush(
    contour: io.github.youndie.mutant.gen.GenPath,
    highlight: io.github.youndie.mutant.gen.GenPath,
    box: androidx.compose.ui.geometry.Rect,
    slot: Slot,
    look: BodyLook,
    density: Float,
): Brush?

/** The SkSL of [bodyShaderBrush], shared by the platforms that run it. */
internal const val BODY_SKSL: String = """
uniform shader cDist;
uniform shader hDist;
uniform float2 gridOrigin;
uniform float2 gridDims;
uniform float band;
uniform float pxDp;
uniform float slip;
uniform float outlineW;
uniform half4 fillC;
uniform half4 inkC;
uniform half4 hlC;
uniform half4 shadowC;
uniform float4 clipA;
uniform float4 clipB;

// A texel's signed distance: 16 bits in red and green over -band..band.
float u16(half4 t) { return ((floor(float(t.r) * 255.0 + 0.5) * 256.0 + floor(float(t.g) * 255.0 + 0.5)) / 65535.0 * 2.0 - 1.0) * band; }

float contourAt(float2 p) {
    float2 g = (p - gridOrigin) / ${GRID_DP};
    if (g.x < 0.0 || g.y < 0.0 || g.x > gridDims.x - 1.0 || g.y > gridDims.y - 1.0) return band;
    float2 i0 = floor(g);
    float2 f = g - i0;
    float a = u16(cDist.eval(i0 + 0.5));
    float b = u16(cDist.eval(i0 + float2(1.5, 0.5)));
    float c = u16(cDist.eval(i0 + float2(0.5, 1.5)));
    float d = u16(cDist.eval(i0 + 1.5));
    return mix(mix(a, b, f.x), mix(c, d, f.x), f.y);
}

float highlightAt(float2 p) {
    float2 g = (p - gridOrigin) / ${GRID_DP};
    if (g.x < 0.0 || g.y < 0.0 || g.x > gridDims.x - 1.0 || g.y > gridDims.y - 1.0) return band;
    float2 i0 = floor(g);
    float2 f = g - i0;
    float a = u16(hDist.eval(i0 + 0.5));
    float b = u16(hDist.eval(i0 + float2(1.5, 0.5)));
    float c = u16(hDist.eval(i0 + float2(0.5, 1.5)));
    float d = u16(hDist.eval(i0 + 1.5));
    return mix(mix(a, b, f.x), mix(c, d, f.x), f.y);
}

float cov(float d) { return clamp(0.5 - d / pxDp, 0.0, 1.0); }

bool inRect(float2 p, float4 r) { return p.x >= r.x && p.y >= r.y && p.x < r.z && p.y < r.w; }

half4 over(half4 src, half4 dst) { return src + dst * (1.0 - src.a); }

half4 main(float2 p) {
    half4 col = half4(0.0);
    float d0 = contourAt(p);
    if (shadowC.a > 0.0) col = over(shadowC * half(cov(contourAt(p - 4.0))), col);
    float dSlip = slip == 0.0 ? d0 : contourAt(p - slip);
    col = over(fillC * half(cov(dSlip)), col);
    if (inRect(p, clipA) || inRect(p, clipB)) {
        col = over(hlC * half(cov(highlightAt(p))), col);
        col = over(fillC * half(cov(highlightAt(p - 1.5))), col);
    }
    col = over(inkC * half(cov(abs(d0) - outlineW * 0.5)), col);
    return col;
}
"""
