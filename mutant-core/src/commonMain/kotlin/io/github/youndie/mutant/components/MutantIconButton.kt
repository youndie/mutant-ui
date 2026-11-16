package io.github.youndie.mutant.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.InputMode
import androidx.compose.ui.platform.LocalInputModeManager
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import io.github.youndie.mutant.icons.MutantIcon
import io.github.youndie.mutant.icons.MutantIcons
import io.github.youndie.mutant.surface.BodyState
import io.github.youndie.mutant.surface.MutantSurface
import io.github.youndie.mutant.surface.inkOnBody
import io.github.youndie.mutant.surface.mutantCleanZone
import io.github.youndie.mutant.theme.MutantTheme
import io.github.youndie.mutant.tokens.MutantBody

/**
 * An icon action on a small slime body (body `icon`, 48 × 48, the touch target). The icon is the text's colour at
 * rest, the dark ink while pressed, when the body boils to amber, and grey when disabled, as the page draws them.
 */
@Composable
public fun MutantIconButton(
    icon: ImageVector,
    contentDescription: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    key: String = contentDescription ?: icon.name,
    interactionSource: MutableInteractionSource? = null,
) {
    ActionBody(ActionKind.Icon, icon, contentDescription, null, onClick, modifier, enabled, key, interactionSource)
}

/**
 * The floating action: the most damaged body of the kit (`fab`, 56 × 56), an acid plate on which every drip lets
 * a drop go. Its icon is drawn with the heavier 2.5 dp line the design gives it, when it is one of [MutantIcons].
 */
@Composable
public fun MutantFab(
    icon: ImageVector,
    contentDescription: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    key: String = contentDescription ?: icon.name,
    interactionSource: MutableInteractionSource? = null,
) {
    ActionBody(ActionKind.Fab, icon, contentDescription, null, onClick, modifier, true, key, interactionSource)
}

/** The FAB with a label after its icon (the page's 136 × 56 «Запись»); 56 dp tall, as wide as its content. */
@Composable
public fun MutantExtendedFab(
    text: String,
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    key: String = text,
    interactionSource: MutableInteractionSource? = null,
) {
    ActionBody(ActionKind.Fab, icon, null, text, onClick, modifier, true, key, interactionSource)
}

internal enum class ActionKind(
    val body: MutantBody,
    val side: Dp,
) {
    Icon(MutantBody.Icon, 48.dp),
    Fab(MutantBody.Fab, 56.dp),
}

/**
 * The icon button and the FAB. [heat] holds a fixture's boil (the page's `data-heat`); [hidden] draws the content
 * transparent for a fixture's bare twin. Both are null and false in the kit.
 */
@Composable
internal fun ActionBody(
    kind: ActionKind,
    icon: ImageVector,
    contentDescription: String?,
    text: String?,
    onClick: () -> Unit,
    modifier: Modifier,
    enabled: Boolean,
    key: String,
    interactionSource: MutableInteractionSource?,
    heat: Double? = null,
    hidden: Boolean = false,
) {
    val colors = MutantTheme.colors
    val source = interactionSource ?: remember { MutableInteractionSource() }
    val focused by source.collectIsFocusedAsState()
    val keyboard = LocalInputModeManager.current.inputMode == InputMode.Keyboard
    val tint =
        when {
            hidden -> Color.Transparent
            !enabled -> colors.textDisabled
            kind == ActionKind.Fab -> colors.onPrimary
            else -> colors.text
        }
    val drawn = remember(icon, kind) { if (kind == ActionKind.Fab) MutantIcons.stroked(icon, FAB_LINE) else icon }
    val sized =
        if (text == null) {
            Modifier.size(kind.side)
        } else {
            Modifier.defaultMinSize(minWidth = kind.side).height(kind.side)
        }
    MutantSurface(
        kind.body,
        key,
        modifier
            .then(sized)
            .mutantHand(enabled)
            .clickable(source, indication = null, enabled = enabled, role = Role.Button, onClick = onClick),
        state = if (enabled) BodyState.Rest else BodyState.Disabled,
        focused = focused && keyboard,
        interactionSource = source,
        heat = heat,
    ) {
        Row(
            if (text == null) Modifier else Modifier.padding(horizontal = EXTENDED_PADDING),
            horizontalArrangement = Arrangement.spacedBy(ICON_GAP, Alignment.CenterHorizontally),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            MutantIcon(drawn, contentDescription, Modifier.mutantCleanZone(), tint = inkOnBody(tint))
            if (text != null) ButtonLabel(text, tint, Modifier.weight(1f, fill = false))
        }
    }
}

/** The FAB's icon line (the page's `stroke-width="2.5"`). */
private const val FAB_LINE = 2.5f

/** Between the extended FAB's icon and label (its `gap: 8px`). */
private val ICON_GAP = 8.dp

/** The extended FAB's content from its sides (the page draws a fixed 136 dp; research D15). */
private val EXTENDED_PADDING = 24.dp
