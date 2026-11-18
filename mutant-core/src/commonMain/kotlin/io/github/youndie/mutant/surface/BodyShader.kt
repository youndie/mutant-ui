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
uniform shader cBins;
uniform shader cSegs;
uniform shader hBins;
uniform shader hSegs;
uniform float2 binOrigin;
uniform float2 binDims;
uniform float pxDp;
uniform float slip;
uniform float outlineW;
uniform half4 fillC;
uniform half4 inkC;
uniform half4 hlC;
uniform half4 shadowC;
uniform float4 clipA;
uniform float4 clipB;

float u16(half hi, half lo) { return floor(float(hi) * 255.0 + 0.5) * 256.0 + floor(float(lo) * 255.0 + 0.5); }

float2 contourAt(float2 p) {
    float2 b = floor((p - binOrigin) / ${BIN_DP});
    if (b.x < 0.0 || b.y < 0.0 || b.x >= binDims.x || b.y >= binDims.y) return float2(1000.0, 0.0);
    half4 h = cBins.eval(b + 0.5);
    float first = u16(h.r, h.g);
    float cb = floor(float(h.b) * 255.0 + 0.5);
    float inside = cb >= 128.0 ? 1.0 : 0.0;
    float count = cb - inside * 128.0;
    float2 c = binOrigin + (b + 0.5) * ${BIN_DP};
    float best = 1000.0;
    for (int k = 0; k < ${MAX_BIN_ENTRIES}; k++) {
        if (float(k) >= count) break;
        float t = (first + float(k)) * 3.0;
        float2 at = float2(mod(t, ${SEGS_W}.0), floor(t / ${SEGS_W}.0)) + 0.5;
        half4 e0 = cSegs.eval(at);
        half4 e1 = cSegs.eval(at + float2(1.0, 0.0));
        half4 e2 = cSegs.eval(at + float2(2.0, 0.0));
        float2 a = binOrigin + float2(u16(e0.r, e0.g), u16(e0.b, e1.r)) / ${SEG_SCALE};
        float2 z = binOrigin + float2(u16(e1.g, e1.b), u16(e2.r, e2.g)) / ${SEG_SCALE};
        float2 pa = p - a;
        float2 ba = z - a;
        float hh = clamp(dot(pa, ba) / max(dot(ba, ba), 1e-8), 0.0, 1.0);
        best = min(best, length(pa - ba * hh));
        // Does the line from the bin's centre to p cross this segment? Each crossing flips inside.
        float2 r = p - c;
        float den = r.x * ba.y - r.y * ba.x;
        if (abs(den) > 1e-9) {
            float2 ac = a - c;
            float tc = (ac.x * ba.y - ac.y * ba.x) / den;
            float ts = (ac.x * r.y - ac.y * r.x) / den;
            if (tc >= 0.0 && tc < 1.0 && ts >= 0.0 && ts < 1.0) inside = 1.0 - inside;
        }
    }
    return float2(best, inside);
}

float2 highlightAt(float2 p) {
    float2 b = floor((p - binOrigin) / ${BIN_DP});
    if (b.x < 0.0 || b.y < 0.0 || b.x >= binDims.x || b.y >= binDims.y) return float2(1000.0, 0.0);
    half4 h = hBins.eval(b + 0.5);
    float first = u16(h.r, h.g);
    float cb = floor(float(h.b) * 255.0 + 0.5);
    float inside = cb >= 128.0 ? 1.0 : 0.0;
    float count = cb - inside * 128.0;
    float2 c = binOrigin + (b + 0.5) * ${BIN_DP};
    float best = 1000.0;
    for (int k = 0; k < ${MAX_BIN_ENTRIES}; k++) {
        if (float(k) >= count) break;
        float t = (first + float(k)) * 3.0;
        float2 at = float2(mod(t, ${SEGS_W}.0), floor(t / ${SEGS_W}.0)) + 0.5;
        half4 e0 = hSegs.eval(at);
        half4 e1 = hSegs.eval(at + float2(1.0, 0.0));
        half4 e2 = hSegs.eval(at + float2(2.0, 0.0));
        float2 a = binOrigin + float2(u16(e0.r, e0.g), u16(e0.b, e1.r)) / ${SEG_SCALE};
        float2 z = binOrigin + float2(u16(e1.g, e1.b), u16(e2.r, e2.g)) / ${SEG_SCALE};
        float2 pa = p - a;
        float2 ba = z - a;
        float hh = clamp(dot(pa, ba) / max(dot(ba, ba), 1e-8), 0.0, 1.0);
        best = min(best, length(pa - ba * hh));
        // Does the line from the bin's centre to p cross this segment? Each crossing flips inside.
        float2 r = p - c;
        float den = r.x * ba.y - r.y * ba.x;
        if (abs(den) > 1e-9) {
            float2 ac = a - c;
            float tc = (ac.x * ba.y - ac.y * ba.x) / den;
            float ts = (ac.x * r.y - ac.y * r.x) / den;
            if (tc >= 0.0 && tc < 1.0 && ts >= 0.0 && ts < 1.0) inside = 1.0 - inside;
        }
    }
    return float2(best, inside);
}

// Signed distance, negative inside.
float sd(float2 q) { return q.y > 0.5 ? -q.x : q.x; }

float cov(float d) { return clamp(0.5 - d / pxDp, 0.0, 1.0); }

bool inRect(float2 p, float4 r) { return p.x >= r.x && p.y >= r.y && p.x < r.z && p.y < r.w; }

half4 over(half4 src, half4 dst) { return src + dst * (1.0 - src.a); }

half4 main(float2 p) {
    half4 col = half4(0.0);
    float2 at0 = contourAt(p);
    if (shadowC.a > 0.0) col = over(shadowC * half(cov(sd(contourAt(p - 4.0)))), col);
    float2 atSlip = slip == 0.0 ? at0 : contourAt(p - slip);
    col = over(fillC * half(cov(sd(atSlip))), col);
    if (inRect(p, clipA) || inRect(p, clipB)) {
        col = over(hlC * half(cov(sd(highlightAt(p)))), col);
        col = over(fillC * half(cov(sd(highlightAt(p - 1.5)))), col);
    }
    col = over(inkC * half(cov(at0.x - outlineW * 0.5)), col);
    return col;
}
"""
