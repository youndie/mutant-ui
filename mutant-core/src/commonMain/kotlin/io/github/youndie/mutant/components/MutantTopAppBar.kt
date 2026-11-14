package io.github.youndie.mutant.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.youndie.mutant.surface.drawTrackingDivider
import io.github.youndie.mutant.theme.LocalMutantCassette
import io.github.youndie.mutant.theme.LocalMutantFonts
import io.github.youndie.mutant.theme.LocalMutantStage
import io.github.youndie.mutant.theme.MutantTheme
import io.github.youndie.mutant.type.LocalMutantTextRaster
import io.github.youndie.mutant.type.MutantFaceMetrics
import io.github.youndie.mutant.type.cssLine

/**
 * The top app bar: no body, a 56 dp row — the [navigationIcon], the [title] in Handjet at 26 taking what is left,
 * an on-screen-display [status] in amber (`CH 03 · SP`), then the [actions] — 12 dp apart, over a 2 dp tracking
 * divider. It stands below [windowInsets], the status bar by default, and draws nothing under it; the screen's
 * ground shows there. The row has no side margins: the screen gives them, as the page's column does.
 */
@Composable
public fun MutantTopAppBar(
    title: String,
    modifier: Modifier = Modifier,
    navigationIcon: (@Composable () -> Unit)? = null,
    status: String? = null,
    windowInsets: WindowInsets = WindowInsets.statusBars,
    key: String = "0",
    actions: @Composable RowScope.() -> Unit = {},
) {
    val colors = MutantTheme.colors
    val fonts = LocalMutantFonts.current
    val raster = LocalMutantTextRaster.current
    val seed = "${LocalMutantCassette.current}/${LocalMutantStage.current}/div$key"
    Column(modifier.windowInsetsPadding(windowInsets)) {
        Row(
            Modifier.fillMaxWidth().height(BAR),
            horizontalArrangement = Arrangement.spacedBy(GAP),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            navigationIcon?.invoke()
            BasicText(
                title,
                Modifier.weight(1f),
                style =
                    TextStyle(
                        fontFamily = fonts?.display,
                        fontSize = TITLE_SIZE.sp,
                        color = colors.text,
                        platformStyle = raster,
                    ).cssLine(MutantFaceMetrics.Handjet),
                maxLines = 1,
                softWrap = false,
                overflow = TextOverflow.Ellipsis,
            )
            if (status != null) {
                BasicText(
                    status,
                    style =
                        TextStyle(
                            fontFamily = fonts?.display,
                            fontSize = STATUS_SIZE.sp,
                            letterSpacing = STATUS_TRACKING.sp,
                            color = colors.textOsd,
                            platformStyle = raster,
                        ).cssLine(MutantFaceMetrics.Handjet),
                    maxLines = 1,
                    softWrap = false,
                )
            }
            actions()
        }
        Canvas(Modifier.fillMaxWidth().height(DIVIDER)) {
            scale(density, Offset.Zero) { drawTrackingDivider(seed, size.width / density, DIVIDER.value, colors) }
        }
    }
}

/** The bar's row and the divider under it (the page's `height: 56px` and `height: 2px`). */
private val BAR = 56.dp
private val DIVIDER = 2.dp

/** Between the icon, the title, the status and the actions (the page's `gap: 12px`). */
private val GAP = 12.dp

/** The title's and the status's sizes, sp, and the status's tracking (`letter-spacing: 1px`). */
private const val TITLE_SIZE = 26
private const val STATUS_SIZE = 20
private const val STATUS_TRACKING = 1
