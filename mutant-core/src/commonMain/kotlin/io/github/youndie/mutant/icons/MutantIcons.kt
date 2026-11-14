package io.github.youndie.mutant.icons

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.addPathNodes
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import io.github.youndie.mutant.theme.MutantTheme

/**
 * The kit's sixteen icons (research §1.13): each the Material icon of its name redrawn on a 24 dp grid with a
 * 2 dp line, square caps and one accent of damage already in its path — a cut shifted by 1 dp or a dropped
 * segment. They are one colour, never animated and never generated: a seed or a tick does not change them.
 */
public object MutantIcons {
    public val Menu: ImageVector by lazy { icon("Menu", MutantIconPaths.MENU) }
    public val Back: ImageVector by lazy { icon("Back", MutantIconPaths.BACK) }
    public val Close: ImageVector by lazy { icon("Close", MutantIconPaths.CLOSE) }
    public val Search: ImageVector by lazy { icon("Search", MutantIconPaths.SEARCH) }
    public val Home: ImageVector by lazy { icon("Home", MutantIconPaths.HOME) }
    public val Add: ImageVector by lazy { icon("Add", MutantIconPaths.ADD) }
    public val Delete: ImageVector by lazy { icon("Delete", MutantIconPaths.DELETE) }
    public val Edit: ImageVector by lazy { icon("Edit", MutantIconPaths.EDIT) }
    public val Share: ImageVector by lazy { icon("Share", MutantIconPaths.SHARE) }
    public val Refresh: ImageVector by lazy { icon("Refresh", MutantIconPaths.REFRESH) }
    public val Settings: ImageVector by lazy { icon("Settings", MutantIconPaths.SETTINGS) }
    public val Done: ImageVector by lazy { icon("Done", MutantIconPaths.DONE) }
    public val Error: ImageVector by lazy { icon("Error", MutantIconPaths.ERROR) }
    public val Warning: ImageVector by lazy { icon("Warning", MutantIconPaths.WARNING) }
    public val Info: ImageVector by lazy { icon("Info", MutantIconPaths.INFO) }
    public val Schedule: ImageVector by lazy { icon("Schedule", MutantIconPaths.SCHEDULE) }

    /** Every icon in the design page's order, by name. */
    public val all: List<Pair<String, ImageVector>> by lazy {
        listOf(
            "Menu" to Menu,
            "Back" to Back,
            "Close" to Close,
            "Search" to Search,
            "Home" to Home,
            "Add" to Add,
            "Delete" to Delete,
            "Edit" to Edit,
            "Share" to Share,
            "Refresh" to Refresh,
            "Settings" to Settings,
            "Done" to Done,
            "Error" to Error,
            "Warning" to Warning,
            "Info" to Info,
            "Schedule" to Schedule,
        )
    }

    private fun icon(
        name: String,
        path: String,
    ): ImageVector =
        ImageVector
            .Builder("Mutant.$name", GRID.dp, GRID.dp, GRID, GRID)
            .addPath(
                pathData = addPathNodes(path),
                fill = null,
                stroke = SolidColor(Color.Black),
                strokeLineWidth = LINE,
                strokeLineCap = StrokeCap.Square,
            ).build()

    private const val GRID = 24f
    private const val LINE = 2f
}

/**
 * One of [MutantIcons] in one colour, [tint] — the theme's text by default, its accent for an active icon (the
 * page's rule 3). The design checks every icon at 16 dp; below that the accent is not promised to read.
 */
@Composable
public fun MutantIcon(
    icon: ImageVector,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    tint: Color = MutantTheme.colors.text,
    size: Dp = 24.dp,
) {
    Image(icon, contentDescription, modifier.size(size), colorFilter = ColorFilter.tint(tint))
}
