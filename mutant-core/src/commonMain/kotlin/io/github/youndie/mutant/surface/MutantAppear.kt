package io.github.youndie.mutant.surface

import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.semantics.clearAndSetSemantics
import io.github.youndie.mutant.clock.MutantClock
import io.github.youndie.mutant.clock.readMutantClock
import io.github.youndie.mutant.theme.LocalMutantColors
import io.github.youndie.mutant.theme.LocalMutantReduceMotion

/**
 * Shows [content] tuning in as it appears and stepping out as it leaves (`tune-in`, the designer's answer 1): composed
 * with [visible] true, or when [visible] becomes true, every body in it shows a 16 dp mosaic over its slot, then an 8 dp
 * mosaic, then itself, over 320 ms in three steps, and the words and icons appear with the finished bodies on the third
 * step. When [visible] becomes false, the bodies show the 8 dp mosaic, then nothing, over 160 ms, and then [content]
 * leaves the composition; while it leaves it answers no pointer, no key and no reader. Turned back half-way, it goes on
 * from the picture it shows. The steps keep to the screen's clock whatever `steppedMotion` says, as they are a picture
 * sequence, not a movement; under reduce motion it shows the finished frame at once and leaves at once.
 *
 * This is how a [io.github.youndie.mutant.components.MutantCard], a
 * [io.github.youndie.mutant.components.MutantEmptyState] or a [io.github.youndie.mutant.components.MutantSearchResult]
 * appears, and how a [io.github.youndie.mutant.components.MutantCocoon] hands over to what it stood for:
 * `if (loading) MutantCocoon(…) else MutantAppear { MutantCard(…) }`. The cocoon goes in one frame, as an old screen
 * does (the designer's answer 2), and the card tunes in. A component the consumer takes straight out of the composition
 * cannot step out: only content kept here with [visible] false can. Content without a body shows nothing until the third
 * step. A lazy list's rows are better left out of it: each would tune in again whenever it scrolls into view.
 *
 * The content stands in a box that passes its minimum size on, as [Box] with `propagateMinConstraints`.
 */
@Composable
public fun MutantAppear(
    visible: Boolean = true,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    val clock = readMutantClock()
    val still = tuneStands(clock)
    val tune = remember { MutantTune(visible, clock.nowMs, play = true) }
    tune.follow(visible, clock.nowMs, still)
    if (!tune.composed(clock, still)) return
    MutantTuned(tune, modifier, leaving = !visible, content = content)
}

/** Whether a tune shows its end at once: under reduce motion, and on a clock that is frozen, paused or held. */
@Composable
internal fun tuneStands(clock: MutantClock): Boolean =
    LocalMutantReduceMotion.current || clock.frozen || clock.paused || clock.holds > 0

/**
 * Whether the body is still to be composed: false once it has left. Read from composition; while a run is under way it
 * subscribes to the clock's frames through a derived state, so the composition hears of the leaving's end alone.
 */
@Composable
internal fun MutantTune.composed(
    clock: MutantClock,
    still: Boolean,
): Boolean {
    val stillNow by rememberUpdatedState(still)
    val later =
        remember(this, clock) {
            derivedStateOf {
                clock.frame
                !gone(clock.nowMs, stillNow)
            }
        }
    if (running(clock.nowMs, still)) later.value
    return !gone(clock.nowMs, still)
}

/**
 * The tuning host: draws [content] as [tune] says at each frame — the bodies registered inside it as mosaics
 * ([drawTuneMosaics]), the content whole, or nothing ([mutantTuneDraw]). [leaving] takes its pointer, keys and
 * semantics away, unless [deafLeaving] is false: a component that answers for its own content while it leaves (the
 * dialog, whose actions go disabled as it closes) still passes [leaving], since its change is what redraws the host when
 * the tune turns. For [MutantAppear], and for a component that keeps its own visibility (the tooltip's popup, the
 * dialog's window).
 */
@Composable
internal fun MutantTuned(
    tune: MutantTune,
    modifier: Modifier = Modifier,
    leaving: Boolean = false,
    deafLeaving: Boolean = true,
    content: @Composable () -> Unit,
) {
    val clock = readMutantClock()
    val still = tuneStands(clock)
    val colors = LocalMutantColors.current
    val bodies = remember { MutantTuneBodies() }
    val deaf =
        if (leaving && deafLeaving) {
            Modifier
                .clearAndSetSemantics {}
                .onPreviewKeyEvent { true }
                .pointerInput(Unit) {
                    awaitPointerEventScope {
                        while (true) awaitPointerEvent(PointerEventPass.Initial).changes.forEach { it.consume() }
                    }
                }
        } else {
            Modifier
        }
    Box(
        modifier
            .then(deaf)
            .onGloballyPositioned { bodies.host = it }
            .mutantTuneDraw(tune, clock, still) { side -> drawTuneMosaics(bodies, side, colors) },
        propagateMinConstraints = true,
    ) {
        CompositionLocalProvider(LocalMutantTuneBodies provides bodies) { content() }
    }
}

/**
 * Draws what it modifies as [tune] says at each frame: whole, nothing, or [mosaics] with the blocks' side in dp. Under
 * the mosaics the content is still drawn, into an empty clip, so its bodies are built and ready for the third step. While
 * a run is under way it is drawn on every frame, so each step lands on its frame; done, it reads nothing of the clock.
 */
internal fun Modifier.mutantTuneDraw(
    tune: MutantTune,
    clock: MutantClock,
    still: Boolean,
    mosaics: DrawScope.(side: Int) -> Unit,
): Modifier =
    drawWithContent {
        val now = clock.nowMs
        if (tune.running(now, still)) clock.frame
        when (val look = tune.look(now, still)) {
            MutantTuneLook.Whole -> {
                drawContent()
            }

            MutantTuneLook.Nothing -> {}

            MutantTuneLook.Coarse, MutantTuneLook.Fine -> {
                clipRect(0f, 0f, 0f, 0f) { this@drawWithContent.drawContent() }
                mosaics(if (look == MutantTuneLook.Coarse) TUNE_COARSE_DP else TUNE_FINE_DP)
            }
        }
    }
