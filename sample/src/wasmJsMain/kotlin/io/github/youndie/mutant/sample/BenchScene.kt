package io.github.youndie.mutant.sample

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import io.github.youndie.mutant.theme.MutantTheme
import kotlin.math.abs

/** The page's scroll travelled, for B-73's recorder, which cannot read the canvas: `window.__scrolled`. */
@OptIn(kotlin.js.ExperimentalWasmJsInterop::class)
@JsFun("(px) => { window.__scrolled = px; }")
private external fun reportScrolled(px: Int)

/**
 * B-73: the scenes of B-72's recorder, opened by `?scene=<name>` so the browser's recorder measures what the JVM's did.
 * Null for a name it does not know, and the sample opens as usual.
 */
internal fun benchScene(name: String?): (@Composable () -> Unit)? =
    when (name) {
        "ComponentsRest", "ComponentsWheel" -> {
            { Recorded(SampleSettings()) }
        }

        "ComponentsClean" -> {
            { Recorded(SampleSettings(cleanSignal = true)) }
        }

        "ComponentsReduce" -> {
            { Recorded(SampleSettings(reduceMotion = true)) }
        }

        "DemoPage" -> {
            { Recorded(SampleSettings(page = SamplePage.Demo)) }
        }

        "DemoLibrary" -> {
            { Alone(DemoScreen.Library) }
        }

        // B-88's control: the Library screen without its keyframe loss.
        "DemoLibraryNoLoss" -> {
            { Alone(DemoScreen.Library, keyframeLoss = false) }
        }

        "DemoRecording" -> {
            { Alone(DemoScreen.Recording) }
        }

        "DemoSettings" -> {
            { Alone(DemoScreen.Settings) }
        }

        else -> {
            null
        }
    }

@Composable
private fun Recorded(settings: SampleSettings) {
    val scroll = rememberScrollState()
    LaunchedEffect(scroll) {
        var last = scroll.value
        var travelled = 0
        snapshotFlow { scroll.value }.collect {
            travelled += abs(it - last)
            last = it
            reportScrolled(travelled)
        }
    }
    MutantUiSample(remember { settings }, scroll)
}

/** One Demo screen alone, as B-72's recorder composes it: no header, no page Tape. */
@Composable
private fun Alone(
    screen: DemoScreen,
    keyframeLoss: Boolean = true,
) {
    val strings = remember { SampleStrings(SampleLanguage.English) }
    MutantTheme {
        Box(Modifier.fillMaxSize().background(MutantTheme.colors.page).padding(24.dp)) {
            DemoScreenFrame(screen, strings, keyframeLoss = keyframeLoss)
        }
    }
}
