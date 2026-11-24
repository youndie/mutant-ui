package io.github.youndie.mutant.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.selection.toggleable
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.InputMode
import androidx.compose.ui.platform.LocalInputModeManager
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import io.github.youndie.mutant.surface.BodyState
import io.github.youndie.mutant.surface.MutantSurface
import io.github.youndie.mutant.surface.SlideRequest
import io.github.youndie.mutant.surface.mutantCleanZone
import io.github.youndie.mutant.surface.mutantMelt
import io.github.youndie.mutant.surface.with
import io.github.youndie.mutant.theme.MutantTheme
import io.github.youndie.mutant.tokens.MutantBody

/**
 * A switch (bodies `track` 52 × 28 and `thumb` 22) in a 48 dp row with its [label]: the row is the touch target. Off,
 * the track is the second surface's colour with a short drip; on, the acid fill pours in, the thumb slides right with
 * a stretch and a ring melts in where it stood — the design's echo, drawn as its own part (the `ghost` gene is not
 * implemented). Disabled, both bodies lose their signal and the label greys. Right to left the row is mirrored: the
 * label first, the thumb on at the left and sliding left (B-205); the bodies are drawn as they are left to right.
 */
@Composable
public fun MutantSwitch(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    label: String? = null,
    enabled: Boolean = true,
    key: String = label ?: "switch",
    interactionSource: MutableInteractionSource? = null,
) {
    SwitchBody(checked, onCheckedChange, modifier, label, enabled, key, "$key/thumb", interactionSource)
}

/** [MutantSwitch]; [thumbKey] is the thumb's own key (the page's `thumb`, `thumb2`), [hidden] a bare twin's. */
@Composable
internal fun SwitchBody(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier,
    label: String?,
    enabled: Boolean,
    key: String,
    thumbKey: String,
    interactionSource: MutableInteractionSource?,
    hidden: Boolean = false,
) {
    val colors = MutantTheme.colors
    val source = interactionSource ?: remember { MutableInteractionSource() }
    val focused by source.collectIsFocusedAsState()
    val keyboard = LocalInputModeManager.current.inputMode == InputMode.Keyboard
    val state = if (enabled) BodyState.Rest else BodyState.Disabled
    // Right to left the thumb stands on at the left and slides that way: its run is mirrored with it (B-205).
    val side = if (LocalLayoutDirection.current == LayoutDirection.Rtl) -1 else 1
    ControlRow(
        modifier.mutantHand(enabled).toggleable(
            checked,
            source,
            indication = null,
            enabled = enabled,
            role = Role.Switch,
            onValueChange = onCheckedChange,
        ),
        label,
        enabled,
        hidden,
    ) {
        MutantSurface(
            MutantBody.Track,
            key,
            Modifier.size(TRACK_W, TRACK_H),
            fill = if (checked) null else colors.surfaceVariant,
            state = state,
            focused = focused && keyboard,
            interactionSource = source,
            profile = if (checked) MutantBody.Track.profile else MutantBody.Track.profile.with(overhang = OFF_OVERHANG),
        ) {
            Box(Modifier.size(TRACK_W, TRACK_H)) {
                // The thumb, sliding from where it was: the request's id is the state, its offset the way it came.
                MutantSurface(
                    MutantBody.Thumb,
                    thumbKey,
                    Modifier
                        .align(Alignment.TopStart)
                        .wrapContentSize(Alignment.TopStart, unbounded = true)
                        .offset(if (checked) ON_X else INSET, INSET)
                        .size(THUMB)
                        .mutantCleanZone(),
                    state = state,
                    slide = SlideRequest(checked, side * (if (checked) -TRAVEL else TRAVEL)),
                ) {}
                // The echo: a ring where the thumb stood, melting in when it leaves (the page's on-only `<i>`). It is page
                // content, drawn over every body, the thumb's included.
                if (enabled) {
                    Canvas(
                        Modifier
                            .align(Alignment.TopStart)
                            .wrapContentSize(Alignment.TopStart, unbounded = true)
                            .offset(GHOST_X, INSET)
                            .size(THUMB)
                            .mutantMelt(checked),
                    ) {
                        val ring = 2.dp.toPx()
                        drawCircle(
                            colors.onPrimary,
                            radius = (size.minDimension - ring) / 2,
                            center = Offset(size.width / 2, size.height / 2),
                            alpha = GHOST_ALPHA,
                            style = Stroke(ring),
                        )
                    }
                }
            }
        }
    }
}

/** The track (the page's `width: 52px; height: 28px`). */
private val TRACK_W = 52.dp
private val TRACK_H = 28.dp

/** The thumb, 22 dp, 3 dp in; on, 27 dp in; its echo at 19. */
private val THUMB = 22.dp
private val INSET = 3.dp
private val ON_X = 27.dp
private val GHOST_X = 19.dp

/** The thumb's travel, dp: the slide's starting offset (the engine's `dx: on ? -24 : 24`). */
private const val TRAVEL = 24.0

/** The off track's drip (the page's `data-over="6"`). */
private const val OFF_OVERHANG = 6.0

/** The echo's ring: the page's `opacity: .35`. */
private const val GHOST_ALPHA = .35f
