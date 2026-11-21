package io.github.youndie.mutant.illustration

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import io.github.youndie.mutant.clock.readMutantClock
import io.github.youndie.mutant.components.MutantText
import io.github.youndie.mutant.surface.MutantSurface
import io.github.youndie.mutant.surface.mutantCleanZone
import io.github.youndie.mutant.surface.with
import io.github.youndie.mutant.theme.LocalMutantColors
import io.github.youndie.mutant.theme.LocalMutantFonts
import io.github.youndie.mutant.theme.LocalMutantStage
import io.github.youndie.mutant.tokens.MutantBody
import io.github.youndie.mutant.tokens.MutantColors
import io.github.youndie.mutant.tokens.MutantProfile
import io.github.youndie.mutant.type.LocalMutantTextRaster
import io.github.youndie.mutant.type.MutantFaceMetrics
import io.github.youndie.mutant.type.cssLine
import kotlin.math.ceil

/** The six empty-state illustrations of the Icons page, each built from the kit's own bodies. */
public enum class MutantIllustrationKind(
    internal val width: Int,
    internal val height: Int,
    internal val stage: String,
) {
    /** A cassette with no label: «Здесь пока пусто». */
    Cassette(216, 132, "il-cassette"),

    /** A CRT set showing no signal: «Нет сигнала». */
    Television(220, 176, "il-tv"),

    /** An antenna that caught nothing: «Ничего не поймали». */
    Antenna(200, 190, "il-antenna"),

    /** A floppy disk: «Ничего не сохранено». */
    Floppy(156, 160, "il-floppy"),

    /** A remote control: «Ничего не выбрано». */
    Remote(92, 176, "il-remote"),

    /** A test card of colour bars: «Идёт настройка». */
    TestCard(236, 156, "il-testcard"),
}

/**
 * One of the six illustrations at the design's size ([MutantIllustrationKind]): bodies from the generator with
 * fixed keys, so the same cassette draws the same picture everywhere, on the screen's clock. Its drips hang
 * below its bounds and take no space. The title and the line under it are the caller's (an empty state).
 */
@Composable
public fun MutantIllustration(
    kind: MutantIllustrationKind,
    modifier: Modifier = Modifier,
) {
    IllustrationArt(kind, modifier, IllustrationParts.All)
}

/**
 * Which parts besides the bodies are drawn: [text] the labels, [kept] what the page marks `data-keep` (and the
 * reels it animates). A parity fixture leaves both out to compare the bodies with a reference whose text and
 * kept parts the script hid. A part not drawn is still laid out and still marks its surface's clean zone, as
 * the page's hidden text and kept parts still count in the engine's `textZone`.
 */
internal class IllustrationParts(
    val text: Boolean,
    val kept: Boolean,
) {
    companion object {
        val All = IllustrationParts(text = true, kept = true)
        val Bodies = IllustrationParts(text = false, kept = false)
    }
}

/** The art of [kind]; [stage] seeds its bodies, its own in the kit, another page's in a fixture of that page. */
@Composable
internal fun IllustrationArt(
    kind: MutantIllustrationKind,
    modifier: Modifier,
    parts: IllustrationParts,
    stage: String = kind.stage,
) {
    CompositionLocalProvider(LocalMutantStage provides stage) {
        Box(modifier.size(kind.width.dp, kind.height.dp)) {
            val art = Art(LocalMutantColors.current, parts)
            when (kind) {
                MutantIllustrationKind.Cassette -> art.cassette()
                MutantIllustrationKind.Television -> art.television()
                MutantIllustrationKind.Antenna -> art.antenna()
                MutantIllustrationKind.Floppy -> art.floppy()
                MutantIllustrationKind.Remote -> art.remote()
                MutantIllustrationKind.TestCard -> art.testCard()
            }
        }
    }
}

/** The page's slots, in its own numbers: each body at its box with the data attributes it overrides. */
private class Art(
    val c: MutantColors,
    val parts: IllustrationParts,
) {
    @Composable
    fun body(
        type: MutantBody,
        key: String,
        x: Int,
        y: Int,
        w: Int,
        h: Int,
        fill: Color? = null,
        profile: MutantProfile = type.profile,
        content: (@Composable BoxScope.() -> Unit)? = null,
    ) {
        MutantSurface(
            type,
            key,
            // A surface passes its size down to its content; a part stands at its own box, as `position: absolute`.
            Modifier
                .wrapContentSize(Alignment.TopStart, unbounded = true)
                .offset(x.dp, y.dp)
                .size(w.dp, h.dp)
                .mutantCleanZone(),
            fill = fill,
            profile = profile,
            // Not an empty literal: one that captures nothing becomes a public `ComposableSingletons` in the ABI.
            content = { content?.invoke(this) },
        )
    }

    @Composable
    fun BoxScope.label(
        text: String,
        size: Int,
        color: Color,
        spacing: Double,
        lineHeight: Double = 1.0,
        alignment: Alignment = Alignment.Center,
        x: Int = 0,
        y: Int = 0,
    ) {
        // The words are part of the picture: drawn at the art's scale, in dp, whatever the user's font scale (B-61).
        val d = androidx.compose.ui.platform.LocalDensity.current
        androidx.compose.runtime.CompositionLocalProvider(
            androidx.compose.ui.platform.LocalDensity provides
                androidx.compose.ui.unit
                    .Density(d.density, 1f),
        ) { wordsInDp(text, size, color, spacing, lineHeight, alignment, x, y) }
    }

    @Composable
    private fun wordsInDp(
        text: String,
        size: Int,
        color: Color,
        spacing: Double,
        lineHeight: Double,
        alignment: Alignment,
        x: Int,
        y: Int,
    ) {
        MutantText(
            text,
            // Wrapped to the text, as the engine's `textZone` measures the text's own box, not the label's.
            Modifier
                .wrapContentSize(alignment, unbounded = true)
                .offset(x.dp, y.dp)
                .mutantCleanZone(),
            style =
                TextStyle(
                    fontFamily = LocalMutantFonts.current?.display,
                    fontSize = size.sp,
                    letterSpacing = (spacing / size).em,
                    color = if (parts.text) color else Color.Transparent,
                    textAlign = TextAlign.Center,
                    platformStyle = LocalMutantTextRaster.current,
                ).cssLine(MutantFaceMetrics.Handjet, lineHeight.toFloat()),
        )
    }

    /** A plain box of the page's ink, drawn over the bodies. */
    @Composable
    fun ink(
        x: Double,
        y: Double,
        w: Double,
        h: Double,
        alpha: Float = 1f,
        kept: Boolean = false,
    ) {
        val shown = !kept || parts.kept
        // Chrome snaps a box's edges to whole pixels; the label's 70 % line, 64.4 dp, is painted 65 there.
        val m =
            Modifier
                .wrapContentSize(Alignment.TopStart, unbounded = true)
                .offset(x.dp, y.dp)
                .size(ceil(w).dp, ceil(h).dp)
        Canvas(if (kept) m.mutantCleanZone() else m) { if (shown) drawRect(INK, alpha = alpha) }
    }

    @Composable
    fun cassette() {
        val blob = MutantBody.Blob.profile.with(marks = false)
        body(MutantBody.Blob, "cassette", 0, 0, 216, 132, profile = blob) {
            body(MutantBody.Bar, "cassette-label", 18, 14, 180, 38, c.phosphor, plain(MutantBody.Bar, r = 4.0)) {
                label("БЕЗ ПОДПИСИ", 20, INK, 2.0)
            }
            body(MutantBody.Pill, "cassette-win", 44, 66, 128, 40, c.background) {
                reel(8, 7)
                reel(94, 7)
            }
        }
    }

    /** A take-up reel: 26 dp in the text's colour with three holes, turning 30° a scene tick. */
    @Composable
    fun reel(
        x: Int,
        y: Int,
    ) {
        val clock = readMutantClock()
        Canvas(
            Modifier
                .wrapContentSize(
                    Alignment.TopStart,
                    unbounded = true,
                ).offset(x.dp, y.dp)
                .size(26.dp)
                .mutantCleanZone(),
        ) {
            if (!parts.kept) return@Canvas
            val turn = (clock.sceneTick * REEL_STEP % 360).toFloat()
            rotate(turn, Offset(13f, 13f)) {
                drawCircle(c.text, 13f, Offset(13f, 13f))
                for ((hx, hy) in REEL_HOLES) drawCircle(c.background, 3f, Offset(hx + 3f, hy + 3f))
            }
        }
    }

    @Composable
    fun television() {
        body(MutantBody.Blob, "tv", 0, 34, 220, 136, c.noSignal, MutantBody.Blob.profile.with(marks = false)) {
            body(MutantBody.Bar, "tv-screen", 14, 14, 138, 106, c.background, plain(MutantBody.Bar, r = 18.0)) {
                label("НЕТ\nСИГНАЛА", 20, c.textOsd, 1.0, lineHeight = 1.1)
            }
            body(MutantBody.Dot, "tv-k1", 170, 16, 30, 30, c.phosphor)
            body(MutantBody.Dot, "tv-k2", 174, 56, 22, 22, c.amber)
            ink(168.0, 92.0, 34.0, 3.0, kept = true)
            ink(168.0, 100.0, 34.0, 3.0, kept = true)
            ink(168.0, 108.0, 24.0, 3.0, kept = true)
        }
        // The rods stand over the set's body, as the page's DOM draws over the stage's layer of bodies.
        Canvas(Modifier.wrapContentSize(Alignment.TopStart, unbounded = true).size(220.dp, 176.dp)) {
            rotate(-30f, Offset(97.5f, 38f)) { drawRect(INK, Offset(96f, 2f), Size(3f, 36f)) }
            rotate(34f, Offset(119.5f, 38f)) { drawRect(INK, Offset(118f, 2f), Size(3f, 36f)) }
        }
    }

    @Composable
    fun antenna() {
        body(MutantBody.Bar, "ant-mast", 95, 4, 10, 150, c.phosphor, plain(MutantBody.Bar, r = 4.0, wobble = .3))
        for ((key, x, y, w, over) in ANTENNA) {
            body(
                MutantBody.Btn,
                key,
                x,
                y,
                w,
                11,
                c.amber,
                plain(MutantBody.Btn, r = 5.0, over = over.toDouble(), drips = .8),
            )
        }
        body(MutantBody.Blob, "ant-base", 58, 148, 84, 24, c.surfaceVariant, plain(MutantBody.Blob, over = 14.0))
    }

    @Composable
    fun floppy() {
        body(
            MutantBody.Blob,
            "floppy",
            0,
            0,
            156,
            160,
            c.cyan,
            MutantBody.Blob.profile.with(radius = 8.0, marks = false),
        ) {
            body(
                MutantBody.Bar,
                "floppy-shutter",
                36,
                8,
                84,
                50,
                c.phosphor,
                plain(MutantBody.Bar, r = 3.0, wobble = .4),
            ) {
                ink(54.0, 8.0, 16.0, 34.0, kept = true)
            }
            body(
                MutantBody.Bar,
                "floppy-label",
                20,
                84,
                116,
                62,
                c.phosphor,
                plain(MutantBody.Bar, r = 4.0, wobble = .4),
            ) {
                // A column of 18 + 7 + 2 + 7 + 2 dp centred in 62, 12 dp in from each side.
                label("1.44 МБ", 18, INK, 0.0, alignment = Alignment.TopStart, x = 12, y = 13)
                ink(12.0, 38.0, 92.0, 2.0, alpha = .6f)
                ink(12.0, 47.0, 92.0 * .7, 2.0, alpha = .6f)
            }
        }
    }

    @Composable
    fun remote() {
        val shell = MutantBody.Blob.profile.with(radius = 22.0, overhang = 22.0, marks = false)
        body(MutantBody.Blob, "remote", 0, 0, 92, 176, c.surfaceVariant, shell) {
            body(MutantBody.Dot, "rm-p", 36, 14, 20, 20, c.danger)
            for (i in 0 until 9) {
                // The keypad's middle key is amber, the other eight phosphor.
                val key = if (i == MIDDLE_KEY) c.amber else c.phosphor
                body(MutantBody.Dot, "rm-${i + 1}", 14 + 24 * (i % 3), 48 + 26 * (i / 3), 16, 16, key)
            }
            body(MutantBody.Pill, "rm-ok", 18, 132, 56, 20)
        }
    }

    @Composable
    fun testCard() {
        body(
            MutantBody.Bar,
            "tc",
            0,
            0,
            236,
            156,
            c.surface,
            MutantBody.Bar.profile.with(radius = 10.0, marks = false),
        ) {
            val bars = listOf(c.phosphor, c.amber, c.cyan, c.primary, c.magenta, c.danger, c.noSignal)
            val profile = plain(MutantBody.Btn, r = 3.0, over = 22.0, drips = .8, outline = 2.5)
            bars.forEachIndexed { i, fill -> body(MutantBody.Btn, "tc-$i", 14 + 30 * i, 12, 26, 80, fill, profile) }
            label(
                "CH 03 · НАСТРОЙКА",
                20,
                c.textOsd,
                1.0,
                alignment = Alignment.TopCenter,
                y = 126,
            )
        }
    }
}

/** A body with neither flow lines nor edge mosaic (`data-marks="0" data-edge="0"`) and the numbers given. */
private fun plain(
    type: MutantBody,
    r: Double = type.profile.radius,
    over: Double = type.profile.overhang,
    drips: Double = type.profile.dripShare,
    wobble: Double = type.profile.wobble,
    outline: Double = type.profile.outline,
): MutantProfile =
    type.profile.with(
        radius = r,
        overhang = over,
        dripShare = drips,
        outline = outline,
        wobble = wobble,
        marks = false,
        mosaic = false,
    )

/** The antenna's three arms: key, x, y, width, overhang. */
private data class Arm(
    val key: String,
    val x: Int,
    val y: Int,
    val w: Int,
    val over: Int,
)

private val ANTENNA = listOf(Arm("ant-a", 22, 18, 156, 16), Arm("ant-b", 44, 52, 112, 14), Arm("ant-c", 64, 86, 72, 12))

/** The reel's three holes, their top-left corners in the 26 dp reel. */
private val REEL_HOLES = listOf(10f to 2f, 3f to 15f, 17f to 15f)

/** The remote's amber key, of nine. */
private const val MIDDLE_KEY = 4

/** The page's `rotate((fi * 30) % 360)` for a reel. */
private const val REEL_STEP = 30L

/** The page's ink for labels and the parts drawn over bodies: `#05060c` in both themes. */
private val INK = Color(0xFF05060C)
