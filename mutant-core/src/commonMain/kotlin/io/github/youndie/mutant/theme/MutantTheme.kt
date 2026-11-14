package io.github.youndie.mutant.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import io.github.youndie.mutant.clock.MutantClock
import io.github.youndie.mutant.clock.ProvideMutantClock
import io.github.youndie.mutant.tokens.MutantColors
import io.github.youndie.mutant.tokens.MutantPalettes
import io.github.youndie.mutant.type.MutantFonts
import io.github.youndie.mutant.type.MutantTextStyles
import io.github.youndie.mutant.type.mutantFonts

/** The platform's "reduce motion" setting: the animator scale 0 on Android, the accessibility switch on iOS, the media query in a browser, off on desktop. */
@Composable
internal expect fun platformReduceMotion(): Boolean

/** The screen's colours (research D7). */
internal val LocalMutantColors = staticCompositionLocalOf { MutantPalettes.Efir }

/** The cassette seed every instance key is prefixed with (the engine's `props.seed`). */
internal val LocalMutantCassette = staticCompositionLocalOf { DEFAULT_CASSETTE }

/** The stage the speckle is seeded by (the engine's `data-mu-stage`). */
internal val LocalMutantStage = staticCompositionLocalOf { "screen" }

/** Whether motion is paused: the clock stands, damage stays, toggles land at once (D7). */
internal val LocalMutantReduceMotion = staticCompositionLocalOf { false }

/** «Чистый сигнал»: no Tape and no idle glitches; the clock runs (D7). */
internal val LocalMutantCleanSignal = staticCompositionLocalOf { false }

/** The two bundled families (B-02). */
internal val LocalMutantFonts = staticCompositionLocalOf<MutantFonts?> { null }

/** The engine's default cassette seed. */
internal const val DEFAULT_CASSETTE: String = "5"

/**
 * The mutant-ui theme (research D7): the palette — [MutantPalettes.Efir], dark, or [MutantPalettes.Kopiya],
 * light — and the [cassette] every component's damage is seeded by: the same cassette and key draw the same
 * copy on every launch and platform.
 *
 * [reduceMotion] is the kit's pause, not a fade: the clock stands still, the damage stays as it is, and
 * toggles switch at once. `null` follows the platform's setting. [cleanSignal] is the brief's «Чистый
 * сигнал» for long sessions and those sensitive to flicker: no Tape and no idle glitches over the screen,
 * while bodies keep their shapes and the clock keeps running; offer it to the user as a switch.
 */
@Composable
public fun MutantTheme(
    colors: MutantColors = MutantPalettes.Efir,
    cassette: String = DEFAULT_CASSETTE,
    reduceMotion: Boolean? = null,
    cleanSignal: Boolean = false,
    content: @Composable () -> Unit,
) {
    val paused = reduceMotion ?: platformReduceMotion()
    val clock = remember { MutantClock().apply { this.paused = paused } }
    SideEffect { clock.paused = paused }
    CompositionLocalProvider(
        LocalMutantColors provides colors,
        LocalMutantCassette provides cassette,
        LocalMutantReduceMotion provides paused,
        LocalMutantCleanSignal provides cleanSignal,
        LocalMutantFonts provides mutantFonts(),
    ) {
        ProvideMutantClock(clock, content)
    }
}

/** The current theme's values, as a component reads them. */
public object MutantTheme {
    /** The palette in effect. */
    public val colors: MutantColors
        @Composable @ReadOnlyComposable
        get() = LocalMutantColors.current

    /** The cassette seed in effect. */
    public val cassette: String
        @Composable @ReadOnlyComposable
        get() = LocalMutantCassette.current

    /** Whether motion is paused. */
    public val reduceMotion: Boolean
        @Composable @ReadOnlyComposable
        get() = LocalMutantReduceMotion.current

    /** Whether «Чистый сигнал» is on. */
    public val cleanSignal: Boolean
        @Composable @ReadOnlyComposable
        get() = LocalMutantCleanSignal.current

    /** The design's type roles as text styles in the bundled faces: display, OSD and text ([MutantTextStyles]). */
    public val textStyles: MutantTextStyles
        @Composable @ReadOnlyComposable
        get() = MutantTextStyles(LocalMutantFonts.current, LocalMutantColors.current)
}
