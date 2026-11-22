package io.github.youndie.mutant.surface

import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Brush
import io.github.youndie.mutant.gen.Slot

/**
 * B-110: whether a resting body is drawn by a GPU shader from its field instead of by paths, every decoration with it
 * (B-131). A pressed or hot body, a pour, a slide, an error, focus and the mosaic past the contour keep their paths.
 * The default is [bodyOnGpuByDefault]; a test or a measurement provides either.
 */
internal val LocalMutantBodyOnGpu = staticCompositionLocalOf { bodyOnGpuByDefault }

/**
 * B-126, the owner's decision of 2026-11-18: the shader by default in the browser only, where a hot phone runs ten cards
 * at 60.8 frames a second by it against 26.9 by paths. The desktop keeps paths, so its goldens and parity stand as
 * recorded; Android holds 59 by paths and has no shader (B-108).
 */
internal expect val bodyOnGpuByDefault: Boolean

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
    /** B-131: the still layers' image, on the surface's grid, or null where none is drawn. */
    still: StillImage? = null,
    /** B-131: the flow lines this tick. */
    flow: io.github.youndie.mutant.gen.GenPath = io.github.youndie.mutant.gen.GenPath.Empty,
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
uniform shader still;
uniform float hasStill;
uniform float2 stillGrid;
uniform float2 stillAt;
uniform float dens;
uniform shader flowBins;
uniform shader flow;
uniform shader flowSegs;
uniform float hasFlow;
uniform float2 flowOrigin;
uniform float2 flowDims;

half4 node(float2 ij) { return field.eval(clamp(ij, float2(0.0), dims - 1.0) + 0.5); }


// B-167: a node's value and its flags from its channels, never through the whole 16-bit word: a channel is a half, and a
// phone's GPU (the Pixel 6a's, in Chrome) works out an expression of halves at half precision, where a number past 2048
// loses its low bits — the word's flags among them, and with them every outline. The parts stay under 2048 here.
float c5(half v) { return floor(float(v) * 31.0 + 0.5); }
float c6(half v) { return floor(float(v) * 63.0 + 0.5); }

float val(half4 t) {
    float r = c5(t.r);
    float g = c6(t.g);
    float b = c5(t.b);
    return (r * 512.0 + g * 8.0 + floor(b / 4.0)) / ${FIELD_STEPS} * 64.0 - 32.0;
}

float flagsOf(half4 t) {
    float b = c5(t.b);
    return b - floor(b / 4.0) * 4.0;
}

// A 16-bit word from its 5-6-5 channels (the flow lines' tables), each channel a float before the word is summed.
float word(half4 t) {
    float r = c5(t.r);
    float g = c6(t.g);
    float b = c5(t.b);
    return r * 2048.0 + g * 32.0 + b;
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
    float flags = flagsOf(node(c0));
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

float c8(half v) { return floor(float(v) * 255.0 + 0.5); }

float u16(half hi, half lo) {
    float h = c8(hi);
    float l = c8(lo);
    return h * 256.0 + l;
}

// The signed distance to the flow lines, negative inside by the non-zero rule: the winding at the bin's centre, changed at
// every segment the line from the centre to p crosses, and the nearest of the bin's segments.
float flowSd(float2 p) {
    float2 b = floor((p - flowOrigin) / ${FLOW_BIN_DP});
    if (b.x < 0.0 || b.y < 0.0 || b.x >= flowDims.x || b.y >= flowDims.y) return 1000.0;
    half4 h = flowBins.eval(b + 0.5);
    float first = u16(h.r, h.g);
    float cb = c8(h.b);
    float wind = floor(cb / 32.0) - 4.0;
    float count = cb - floor(cb / 32.0) * 32.0;
    float2 c = flowOrigin + (b + 0.5) * ${FLOW_BIN_DP};
    float2 r = p - c;
    float best = 1000.0;
    for (int k = 0; k < ${MAX_FLOW_ENTRIES}; k++) {
        if (float(k) >= count) break;
        float e = first + float(k);
        float t = word(flow.eval(float2(mod(e, ${FLOW_W}.0), floor(e / ${FLOW_W}.0)) + 0.5)) * 4.0;
        float2 at = float2(mod(t, ${FLOW_W}.0), floor(t / ${FLOW_W}.0)) + 0.5;
        float2 a = flowOrigin + float2(word(flowSegs.eval(at)), word(flowSegs.eval(at + float2(1.0, 0.0)))) / ${FLOW_SCALE};
        float2 z = flowOrigin +
            float2(word(flowSegs.eval(at + float2(2.0, 0.0))), word(flowSegs.eval(at + float2(3.0, 0.0)))) / ${FLOW_SCALE};
        float2 ba = z - a;
        best = min(best, segD(p, a, z));
        float den = r.x * ba.y - r.y * ba.x;
        if (abs(den) > 1e-9) {
            float2 ac = a - c;
            float tc = (ac.x * ba.y - ac.y * ba.x) / den;
            float ts = (ac.x * r.y - ac.y * r.x) / den;
            if (tc >= 0.0 && tc < 1.0 && ts >= 0.0 && ts < 1.0) wind += ba.x * r.y - ba.y * r.x > 0.0 ? 1.0 : -1.0;
        }
    }
    return wind != 0.0 ? -best : best;
}

float cov(float d) { return clamp(0.5 - d / pxDp, 0.0, 1.0); }

bool inRect(float2 p, float4 r) { return p.x >= r.x && p.y >= r.y && p.x < r.z && p.y < r.w; }

half4 over(half4 src, half4 dst) { return src + dst * (1.0 - src.a); }

half4 main(float2 p) {
    half4 col = half4(0.0);
    float d0 = sdist(p, 0.0, rOutline, 1.0);
    if (shadowC.a > 0.0) col = over(shadowC * half(cov(sdist(p - 4.0, 0.0, 1.0, 1.0))), col);
    float dSlip = slip == 0.0 ? d0 : sdist(p - slip, 0.0, 1.0, 1.0);
    // The group the paths clip by the contour, shifted by the slip: the fill, the still layers, the flow lines.
    half clip = half(cov(dSlip));
    col = over(fillC * clip, col);
    if (clip > 0.0) {
        if (hasStill > 0.5) col = over(still.eval((p - stillGrid) * dens - stillAt) * clip, col);
        if (hasFlow > 0.5) col = over(inkC * half(cov(flowSd(p - slip))) * clip, col);
    }
    if (inRect(p, clipA) || inRect(p, clipB)) {
        col = over(hlC * half(cov(sdist(p, ${HIGHLIGHT_ISO}, 1.0, 2.0))), col);
        col = over(fillC * half(cov(sdist(p - 1.5, ${HIGHLIGHT_ISO}, 1.0, 2.0))), col);
    }
    col = over(inkC * half(cov(abs(d0) - outlineW * 0.5)), col);
    return col;
}
"""
