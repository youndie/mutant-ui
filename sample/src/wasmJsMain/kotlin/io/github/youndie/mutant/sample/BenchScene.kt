package io.github.youndie.mutant.sample

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import io.github.youndie.mutant.components.MutantCard
import io.github.youndie.mutant.components.MutantCardSubtitle
import io.github.youndie.mutant.components.MutantCardTitle
import io.github.youndie.mutant.surface.mutantTape
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

        // B-107: the Demo page with the Tape off on every phone.
        "DemoPageClean" -> {
            { Recorded(SampleSettings(page = SamplePage.Demo, cleanSignal = true)) }
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

        // B-108: bench-android's TenCards8, composed alike, so the browser and Android run the same work on one phone.
        "TenCards8" -> {
            { TenCards8() }
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

/**
 * B-108: bench-android's `TenCards8` as its host composes it: ten cards under the Tape on the theme's clock (8 resting
 * ticks a second), with a layer that reads the frame time in its draw, so every frame is drawn as on Android.
 */
@Composable
private fun TenCards8() {
    var frame by remember { mutableLongStateOf(0L) }
    LaunchedEffect(Unit) { while (true) withFrameNanos { frame = it } }
    val read = { frame }
    MutantTheme(reduceMotion = false) {
        Box(Modifier.fillMaxSize().background(MutantTheme.colors.background).mutantTape()) {
            Column(
                Modifier.fillMaxSize().padding(horizontal = 16.dp, vertical = 24.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                repeat(10) { i ->
                    MutantCard(Modifier.size(328.dp, 80.dp), key = "card/b49/$i") {
                        MutantCardTitle("Кассета ${(i + 1).toString().padStart(2, '0')}")
                        MutantCardSubtitle("Запись ${12 + i}:0$i")
                    }
                }
            }
            Box(Modifier.fillMaxSize().graphicsLayer { }) {
                Box(Modifier.fillMaxSize().graphicsLayer { }.drawBehind { read() })
            }
        }
    }
}
