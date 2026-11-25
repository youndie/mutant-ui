package io.github.youndie.mutant.surface

import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.ui.Modifier

/**
 * A screen's content slot, keyed by the [screen] it shows (the designer's answer 2, B-180): when [screen] changes, the
 * old content leaves the composition at once, so its last frame is the one before the switch, and [content] is composed
 * anew and tunes in as [MutantAppear] does — a 16 dp mosaic over each body, an 8 dp mosaic, then the finished screen with
 * its words and icons, 320 ms in three steps. Nothing slides and no frame shows the two screens together. The steps
 * count from the composition that sees the new [screen], which is the one where a [io.github.youndie.mutant.components.MutantNavigationBar]'s
 * pill, a [io.github.youndie.mutant.components.MutantTabs]' bar or a [io.github.youndie.mutant.components.MutantChip]'s pour
 * starts for the same pick: the new screen tunes in on the frame what chose it starts to move. Under reduce motion, and
 * on a clock that stands, the new screen shows finished at once.
 *
 * Composed for the first time, it tunes in too, as any body appearing does: a tab composed only while it is shown tunes
 * in on every visit, with no key of its own to change.
 *
 * [screen] is any value with an `equals`: a destination, a tab's index, a filter. The bodies inside it that take no key
 * of their own are seeded by their place, which counts [screen]'s `hashCode` (B-206): a string or a number gives them
 * the same shapes on every visit and every run, a value hashed by identity new ones on every run. Keep the control that picks the screen
 * outside: everything inside tunes in, a navigation bar included. The content's state is the screen's own and goes with
 * it; keep what must survive the visit above, or in a `SaveableStateHolder`. [MutantAppear]s inside it tune in with it,
 * their bodies drawn by the screen's mosaic.
 *
 * The content stands in a box that passes its minimum size on, as [androidx.compose.foundation.layout.Box] with
 * `propagateMinConstraints`.
 */
@Composable
public fun MutantScreen(
    screen: Any?,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    key(screen) { MutantAppear(visible = true, modifier = modifier, content = content) }
}
