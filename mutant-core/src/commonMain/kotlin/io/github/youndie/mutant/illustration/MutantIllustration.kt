package io.github.youndie.mutant.illustration

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
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
 * below its bounds and take no space. The title and the line under it are the caller's (an empty state). Given less
 * than its size, it keeps its size, centred in what it is given, and overflows both sides alike. Right to left it is
 * drawn as it is left to right: a picture is not mirrored (B-205).
 *
 * [cassetteLabel] is the word on the [MutantIllustrationKind.Cassette]'s label, the design's «БЕЗ ПОДПИСИ» unless the
 * app translates it; the other kinds ignore it. It is one line, drawn at 20 dp whatever the font scale; a word wider
 * than the label's 164 dp at 20 is drawn at 16, and one wider still, or a blank one, leaves the label without text,
 * ruled with three lines (B-194).
 */
@Composable
public fun MutantIllustration(
    kind: MutantIllustrationKind,
    modifier: Modifier = Modifier,
    cassetteLabel: String = CASSETTE_LABEL,
) {
    IllustrationArt(kind, modifier, IllustrationParts.All, cassetteLabel = cassetteLabel)
}

/** The design's word on the cassette's label. */
internal const val CASSETTE_LABEL = "БЕЗ ПОДПИСИ"

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

/**
 * The art of [kind]; [stage] seeds its bodies, its own in the kit, another page's in a fixture of that page. Given less
 * than the art's size, the art keeps it, centred: squeezed by a plain `size`, its bodies, placed from its left, ran off
 * to the right alone (B-170).
 */
@Composable
internal fun IllustrationArt(
    kind: MutantIllustrationKind,
    modifier: Modifier,
    parts: IllustrationParts,
    stage: String = kind.stage,
    cassetteLabel: String = CASSETTE_LABEL,
) {
    CompositionLocalProvider(LocalMutantStage provides stage) {
        Box(modifier.requiredSize(kind.width.dp, kind.height.dp)) {
            // A picture is not mirrored right to left (B-205, the designer's answer 37): its parts stand at the page's
            // boxes from the left whatever the layout's direction.
            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                Box(Modifier.matchParentSize()) {
                    val art = Art(LocalMutantColors.current, parts)
                    when (kind) {
                        MutantIllustrationKind.Cassette -> art.cassette(cassetteLabel)
                        MutantIllustrationKind.Television -> art.television()
                        MutantIllustrationKind.Antenna -> art.antenna()
                        MutantIllustrationKind.Floppy -> art.floppy()
                        MutantIllustrationKind.Remote -> art.remote()
                        MutantIllustrationKind.TestCard -> art.testCard()
                    }
                }
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
    fun cassette(word: String) {
        val blob = MutantBody.Blob.profile.with(marks = false)
        body(MutantBody.Blob, "cassette", 0, 0, 216, 132, profile = blob) {
            body(MutantBody.Bar, "cassette-label", 18, 14, 180, 38, c.phosphor, plain(MutantBody.Bar, r = 4.0)) {
                val size = cassetteLabelSize(word)
                if (size != null) label(word, size, INK, CASSETTE_LABEL_SPACING) else cassetteRules()
            }
            body(MutantBody.Pill, "cassette-win", 44, 66, 128, 40, c.background) {
                reel(8, 7)
                reel(94, 7)
            }
        }
    }

    /**
     * The size [word] fits the cassette's label at (B-194, the designer's answer 19): the design's 20, or 16 for a word
     * wider than [CASSETTE_LABEL_ROOM] at 20; null for one wider still at 16, or a blank one. Measured in dp, as
     * [label] draws it: at the screen's density and no font scale.
     */
    @Composable
    private fun cassetteLabelSize(word: String): Int? {
        if (word.isBlank()) return null
        val measurer = rememberTextMeasurer()
        val d = LocalDensity.current
        val inDp = Density(d.density, 1f)
        val font = LocalMutantFonts.current?.display
        return listOf(CASSETTE_LABEL_SIZE, CASSETTE_LABEL_SMALL).firstOrNull { size ->
            val style =
                TextStyle(fontFamily = font, fontSize = size.sp, letterSpacing = (CASSETTE_LABEL_SPACING / size).em)
            val width = measurer.measure(word, style, density = inDp).size.width / d.density
            width <= CASSETTE_LABEL_ROOM
        }
    }

    /**
     * A label with no word: three rules of the page's ink across it, as a blank tape label is ruled for writing on
     * (D15: the answer names the three lines; their length, weight and pitch are designed here, B-194). They stand
     * where the word would, and like it they keep the clean zone and are left out where the text is.
     */
    @Composable
    private fun cassetteRules() {
        Canvas(
            // Centred as the word is: the label passes its size down as constraints, not as a box to align in.
            Modifier
                .wrapContentSize(Alignment.Center, unbounded = true)
                .size(CASSETTE_RULE_LENGTH.dp, (CASSETTE_RULE_PITCH * 2 + CASSETTE_RULE_WEIGHT).dp)
                .mutantCleanZone(),
        ) {
            if (!parts.text) return@Canvas
            illustrationInDp {
                repeat(3) { i ->
                    drawRect(
                        INK,
                        Offset(0f, i * CASSETTE_RULE_PITCH),
                        Size(CASSETTE_RULE_LENGTH, CASSETTE_RULE_WEIGHT),
                    )
                }
            }
        }
    }

    /** A take-up reel: 26 dp in the text's colour with three holes, turning 30° a scene tick, drawn in dp. */
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
            illustrationInDp {
                rotate(turn, Offset(13f, 13f)) {
                    drawCircle(c.text, 13f, Offset(13f, 13f))
                    for ((hx, hy) in REEL_HOLES) drawCircle(c.background, 3f, Offset(hx + 3f, hy + 3f))
                }
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
            illustrationInDp {
                rotate(-30f, Offset(97.5f, 38f)) { drawRect(INK, Offset(96f, 2f), Size(3f, 36f)) }
                rotate(34f, Offset(119.5f, 38f)) { drawRect(INK, Offset(118f, 2f), Size(3f, 36f)) }
            }
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

/**
 * Draws [block] in dp, the page's numbers as they stand, by one scale about the canvas's origin (B-170): drawn in
 * pixels, a reel was half its size at density 2 and the set's rods half their length, at half their place. The parts
 * drawn so are fills, with no stroke for the scale to thicken; at density 1 the scale is the identity.
 */
private inline fun DrawScope.illustrationInDp(block: DrawScope.() -> Unit) {
    scale(density, density, Offset.Zero) { block() }
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

/** The cassette label's word: 20 dp (`font-size: 20px`), tracked 2 dp (`letter-spacing: 2px`). */
private const val CASSETTE_LABEL_SIZE = 20
private const val CASSETTE_LABEL_SPACING = 2.0

/** The answer's smaller size, and the room a word has at either: the 180 dp label less 8 dp a side (B-194). */
private const val CASSETTE_LABEL_SMALL = 16
private const val CASSETTE_LABEL_ROOM = 164f

/** The three rules of a label with no word (D15, B-194): 148 dp long, 2 dp of ink, 8 dp from one to the next. */
private const val CASSETTE_RULE_LENGTH = 148f
private const val CASSETTE_RULE_WEIGHT = 2f
private const val CASSETTE_RULE_PITCH = 8f

/** The page's `rotate((fi * 30) % 360)` for a reel. */
private const val REEL_STEP = 30L

/** The page's ink for labels and the parts drawn over bodies: `#05060c` in both themes. */
private val INK = Color(0xFF05060C)
