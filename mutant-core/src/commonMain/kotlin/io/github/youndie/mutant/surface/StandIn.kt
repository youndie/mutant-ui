package io.github.youndie.mutant.surface

import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import io.github.youndie.mutant.gen.ErrorBlockColor
import io.github.youndie.mutant.gen.MutantGenerator
import io.github.youndie.mutant.gen.Slot
import io.github.youndie.mutant.gen.errorBlocks
import io.github.youndie.mutant.tokens.MutantColors

/**
 * What a body's state is drawn by in its stand-in (B-177): everything of it that needs no generated instance — the slot,
 * the instance's seed (`"$cassette/$key"`, as `MutantGenerator.instance` makes it), the theme and the clock's time.
 */
internal class StandInState(
    val state: BodyState,
    val focused: Boolean,
    /** The body's heat now, a press's or a fixture's: the fill boils as the body's would. */
    val heat: Double,
    /** The time the body would be drawn at: the boil's flicker and the loading bands read it. */
    val t: Double,
    val seed: String,
    val gen: MutantGenerator,
    val colors: MutantColors,
    val patterns: () -> StagePatterns,
)

/**
 * B-149: what a body draws in the frame its first build is not ready in, where the frame cannot wait for it (the
 * browser): the slot's rounded shape in the body's fill, its outline inside it, so a body is never bare. The body
 * replaces it from the frame its build is taken in.
 *
 * B-177: the stand-in draws the body's state, as far as the state needs no instance. Disabled and Success are drawn on
 * the slot's rounded rectangle anyway, so they are drawn as the body draws them (Disabled but its edge mosaic, the one
 * part generated with the instance). A live body is the plate in its fill boiled by its heat, slipped from its outline as
 * the body's fill is, over its misregistered shadow, in its stroke's ink, with its state's marks on the plate: the
 * error's offset outline and blocks, the loading bands, the focus brackets.
 */
internal fun DrawScope.drawStandIn(
    slot: Slot,
    look: BodyLook,
    density: Float,
    st: StandInState,
) {
    val ox = slot.ox.toFloat()
    val oy = slot.oy.toFloat()
    val w = slot.w.toFloat()
    val h = slot.h.toFloat()
    val r = slot.r.toFloat()
    val o = look.outline
    val colors = st.colors
    scale(density, Offset.Zero) {
        translate(-ox, -oy) {
            when (st.state) {
                BodyState.Disabled -> {
                    drawDisabledBody(
                        slot,
                        st.seed,
                        emptyList(),
                        look.fill,
                        look.highlight,
                        colors,
                        o,
                        st.patterns(),
                        Offset.Zero,
                    )
                }

                BodyState.Success -> {
                    drawCleanBody(slot, look.fill, look.highlight, look.ink, o)
                    if (st.focused) drawBrackets(slot, colors.focus)
                }

                else -> {
                    val plate = Path().apply { addRoundRect(RoundRect(ox, oy, ox + w, oy + h, CornerRadius(r))) }
                    val inset = Offset(ox + o / 2, oy + o / 2)
                    val insetSize = Size(w - o, h - o)
                    val insetR = CornerRadius((r - o / 2).coerceAtLeast(0f))
                    look.shadow?.let { translate(4f, 4f) { drawPath(plate, it) } }
                    if (st.state == BodyState.Error) {
                        translate(2.5f, 0f) {
                            drawRoundRect(colors.danger, inset, insetSize, insetR, style = Stroke(look.baseOutline))
                        }
                    }
                    translate(look.slip, look.slip) {
                        clipPath(plate) {
                            drawPath(plate, boilFill(look.fill, colors, st.heat, st.t))
                            if (st.state == BodyState.Error) {
                                for (b in st.gen.errorBlocks(slot, st.seed)) {
                                    val c =
                                        when (b.color) {
                                            ErrorBlockColor.Danger -> colors.danger
                                            ErrorBlockColor.Ink -> colors.ink
                                            ErrorBlockColor.Background -> colors.background
                                        }
                                    val side = b.size.toFloat()
                                    drawRect(c, Offset(b.x.toFloat(), b.y.toFloat()), Size(side, side))
                                }
                            }
                            if (st.state == BodyState.Loading) {
                                for ((i, b) in loadingBands(slot, st.t).withIndex()) {
                                    drawRect(Color.White, b.topLeft, b.size, alpha = if (i == 0) .5f else .35f)
                                }
                            }
                        }
                    }
                    drawRoundRect(look.ink, inset, insetSize, insetR, style = Stroke(o))
                    if (st.focused) drawBrackets(slot, colors.focus)
                }
            }
        }
    }
}
