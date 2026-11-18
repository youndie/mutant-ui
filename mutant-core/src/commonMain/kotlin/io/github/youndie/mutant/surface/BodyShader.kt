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
 * The body's silhouette as a brush over scene coordinates (the generator's, in dp), from [field] at [density] device
 * pixels a dp: the field is handed to the shader as a texture, and a pixel near the edge marches the cells around it
 * as `march` does (B-130). Null where the platform has no shader.
 */
internal expect fun bodyShaderBrush(
    field: io.github.youndie.mutant.gen.DistanceField,
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
uniform float rOutline;
uniform half4 fillC;
uniform half4 inkC;
uniform half4 hlC;
uniform half4 shadowC;
uniform float4 clipA;
uniform float4 clipB;

half4 node(float2 ij) { return field.eval(clamp(ij, float2(0.0), dims - 1.0) + 0.5); }

float val(half4 t) {
    float hi = floor(float(t.r) * 255.0 + 0.5);
    float lo = floor(float(t.g) * 255.0 + 0.5);
    return (hi * 256.0 + lo) / 65535.0 * 64.0 - 32.0;
}

float texel(float2 ij) { return val(node(ij)); }

// The field at p, bilinear.
float bil(float2 g) {
    float2 i0 = floor(g);
    float2 f = g - i0;
    float a = texel(i0);
    float b = texel(i0 + float2(1.0, 0.0));
    float c = texel(i0 + float2(0.0, 1.0));
    float d = texel(i0 + float2(1.0, 1.0));
    return mix(mix(a, b, f.x), mix(c, d, f.x), f.y);
}

float segD(float2 p, float2 a, float2 b) {
    float2 pa = p - a;
    float2 ba = b - a;
    float h = clamp(dot(pa, ba) / max(dot(ba, ba), 1e-8), 0.0, 1.0);
    return length(pa - ba * h);
}

// The distance from p to the iso-line in cell ij, as `march` draws it there: the crossings on the cell's edges, joined
// by its case, a saddle split by the cell's mean. 1000 where the line does not cross the cell.
float cellD(float2 p, float2 ij, float iso) {
    float a = texel(ij) - iso;
    float b = texel(ij + float2(1.0, 0.0)) - iso;
    float c = texel(ij + float2(1.0, 1.0)) - iso;
    float d = texel(ij + float2(0.0, 1.0)) - iso;
    float cs = (a < 0.0 ? 8.0 : 0.0) + (b < 0.0 ? 4.0 : 0.0) + (c < 0.0 ? 2.0 : 0.0) + (d < 0.0 ? 1.0 : 0.0);
    if (cs == 0.0 || cs == 15.0) return 1000.0;
    float2 A = origin + ij * stepDp;
    float2 B = A + float2(stepDp, 0.0);
    float2 C = A + float2(stepDp, stepDp);
    float2 D = A + float2(0.0, stepDp);
    float2 top = mix(A, B, a / (a - b + 1e-12));
    float2 right = mix(B, C, b / (b - c + 1e-12));
    float2 bottom = mix(D, C, d / (d - c + 1e-12));
    float2 left = mix(A, D, a / (a - d + 1e-12));
    bool low = (a + b + c + d) * 0.25 < 0.0;
    if (cs == 1.0 || cs == 14.0) return segD(p, left, bottom);
    if (cs == 2.0 || cs == 13.0) return segD(p, bottom, right);
    if (cs == 3.0 || cs == 12.0) return segD(p, left, right);
    if (cs == 4.0 || cs == 11.0) return segD(p, top, right);
    if (cs == 6.0 || cs == 9.0) return segD(p, top, bottom);
    if (cs == 7.0 || cs == 8.0) return segD(p, left, top);
    if (cs == 5.0) {
        return low ? min(segD(p, left, top), segD(p, bottom, right)) : min(segD(p, top, right), segD(p, left, bottom));
    }
    return low ? min(segD(p, top, right), segD(p, left, bottom)) : min(segD(p, left, top), segD(p, bottom, right));
}

// The signed distance to the iso-line, negative inside, exact within reach r cells where the node is marked with bit.
float sdist(float2 p, float iso, float r, float bit) {
    float2 g = (p - origin) / stepDp;
    if (g.x < 0.0 || g.y < 0.0 || g.x > dims.x - 1.0 || g.y > dims.y - 1.0) return 1000.0;
    float v = bil(g) - iso;
    float2 c0 = floor(g);
    float flags = floor(float(node(c0).b) * 255.0 + 0.5);
    bool near = bit == 1.0 ? mod(flags, 2.0) >= 1.0 : flags >= 2.0;
    float best = 1000.0;
    if (near) {
        for (int dj = -2; dj <= 2; dj++) {
            for (int di = -2; di <= 2; di++) {
                if (abs(float(di)) <= r && abs(float(dj)) <= r) {
                    best = min(best, cellD(p, c0 + float2(float(di), float(dj)), iso));
                }
            }
        }
    }
    return v < 0.0 ? -best : best;
}

float cov(float d) { return clamp(0.5 - d / pxDp, 0.0, 1.0); }

bool inRect(float2 p, float4 r) { return p.x >= r.x && p.y >= r.y && p.x < r.z && p.y < r.w; }

half4 over(half4 src, half4 dst) { return src + dst * (1.0 - src.a); }

half4 main(float2 p) {
    half4 col = half4(0.0);
    float d0 = sdist(p, 0.0, rOutline, 1.0);
    if (shadowC.a > 0.0) col = over(shadowC * half(cov(sdist(p - 4.0, 0.0, 1.0, 1.0))), col);
    float dSlip = slip == 0.0 ? d0 : sdist(p - slip, 0.0, 1.0, 1.0);
    col = over(fillC * half(cov(dSlip)), col);
    if (inRect(p, clipA) || inRect(p, clipB)) {
        col = over(hlC * half(cov(sdist(p, ${HIGHLIGHT_ISO}, 1.0, 2.0))), col);
        col = over(fillC * half(cov(sdist(p - 1.5, ${HIGHLIGHT_ISO}, 1.0, 2.0))), col);
    }
    col = over(inkC * half(cov(abs(d0) - outlineW * 0.5)), col);
    return col;
}
"""
