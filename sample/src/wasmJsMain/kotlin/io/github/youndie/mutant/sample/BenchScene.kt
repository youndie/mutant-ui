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
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import io.github.youndie.mutant.components.MutantCard
import io.github.youndie.mutant.components.MutantCardSubtitle
import io.github.youndie.mutant.components.MutantCardTitle
import io.github.youndie.mutant.components.MutantMenu
import io.github.youndie.mutant.surface.mutantTape
import io.github.youndie.mutant.theme.MutantTheme
import kotlin.math.abs

/** The page's scroll travelled, for B-73's recorder, which cannot read the canvas: `window.__scrolled`. */
@OptIn(kotlin.js.ExperimentalWasmJsInterop::class)
@JsFun("(px) => { window.__scrolled = px; }")
private external fun reportScrolled(px: Int)

/**
 * B-151: where the Demo screen's phone stands, in CSS pixels, for the touch bench, which cannot read the canvas:
 * `window.__phone = [left, top, width, height]`.
 */
@OptIn(kotlin.js.ExperimentalWasmJsInterop::class)
@JsFun("(l, t, w, h) => { window.__phone = [l, t, w, h]; }")
private external fun reportPhone(
    left: Double,
    top: Double,
    width: Double,
    height: Double,
)

/**
 * B-73: the scenes of B-72's recorder, opened by `?scene=<name>` so the browser's recorder measures what the JVM's did.
 * Null for a name it does not know, and the sample opens as usual. B-148: the Components scenes show the page's default
 * group, Foundations, alone, and the Demo page scenes its default screen, Library, alone.
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

        // B-197: the resting page with a menu open over its Tape from the load on.
        "ComponentsMenu" -> {
            { WithMenu { Recorded(SampleSettings()) } }
        }

        // B-197: a page with a dialog open over a Tape on the clock the dialog holds: the Demo page's Settings phone, its
        // erase dialog open as the screen opens, the page scrolled 420 dp so the dialog stands in a phone's viewport. The
        // Components page's dialogs stand on stages whose clock its Tape does not read.
        "ComponentsDialog" -> {
            { Recorded(SampleSettings(page = SamplePage.Demo, demoScreen = DemoScreen.Settings), scrolledDp = 420) }
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

        // B-151: the Library screen as DemoLibrary opens it, telling the touch bench where its phone stands.
        "DemoTouch" -> {
            { Alone(DemoScreen.Library, report = true) }
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

        // B-110's ruler: the ground and the Tape with every frame drawn, and no body.
        "Blank" -> {
            { TenCards8(cards = false) }
        }

        else -> {
            null
        }
    }

@Composable
private fun Recorded(
    settings: SampleSettings,
    scrolledDp: Int = 0,
) {
    val density = LocalDensity.current
    val scroll = rememberScrollState(with(density) { scrolledDp.dp.roundToPx() })
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

/**
 * B-197: [page] with the sample's menu open over it, 24 dp from its start and 200 dp from its top, and left open. The menu
 * stands in an outer theme around the page, so it finds the page's Tape (an inner theme keeps the outer one's Tapes); the
 * outer theme's clock drives the menu alone.
 */
@Composable
private fun WithMenu(page: @Composable () -> Unit) {
    val strings = remember { SampleStrings(SampleLanguage.English) }
    MutantTheme(reduceMotion = false) {
        Box(Modifier.fillMaxSize()) {
            page()
            Box(Modifier.padding(start = 24.dp, top = 200.dp)) {
                MutantMenu(true, {}, strings.menuItems, 0, {}, key = "bench-menu")
            }
        }
    }
}

/** One Demo screen alone, as B-72's recorder composes it: no header, no page Tape. */
@Composable
private fun Alone(
    screen: DemoScreen,
    keyframeLoss: Boolean = true,
    report: Boolean = false,
) {
    val strings = remember { SampleStrings(SampleLanguage.English) }
    val density = LocalDensity.current.density
    MutantTheme {
        Box(Modifier.fillMaxSize().background(MutantTheme.colors.page).padding(24.dp)) {
            // The frame is the screen's title over its 360 × 720 phone: the phone is the frame's bottom 720 dp.
            val where =
                if (report) {
                    Modifier.onGloballyPositioned { c ->
                        val at = c.localToWindow(androidx.compose.ui.geometry.Offset.Zero)
                        val w = c.size.width / density.toDouble()
                        val h = 720.0
                        val top = at.y / density.toDouble() + c.size.height / density - h
                        reportPhone(at.x / density.toDouble(), top, w, h)
                    }
                } else {
                    Modifier
                }
            Box(where) { DemoScreenFrame(screen, strings, keyframeLoss = keyframeLoss) }
        }
    }
}

/**
 * B-108: bench-android's `TenCards8` as its host composes it: ten cards under the Tape on the theme's clock (8 resting
 * ticks a second), with a layer that reads the frame time in its draw, so every frame is drawn as on Android.
 */
@Composable
private fun TenCards8(cards: Boolean = true) {
    var frame by remember { mutableLongStateOf(0L) }
    LaunchedEffect(Unit) { while (true) withFrameNanos { frame = it } }
    val read = { frame }
    MutantTheme(reduceMotion = false) {
        Box(Modifier.fillMaxSize().background(MutantTheme.colors.background).mutantTape()) {
            Column(
                Modifier.fillMaxSize().padding(horizontal = 16.dp, vertical = 24.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                if (cards) {
                    repeat(10) { i ->
                        MutantCard(Modifier.size(328.dp, 80.dp), key = "card/b49/$i") {
                            MutantCardTitle("Кассета ${(i + 1).toString().padStart(2, '0')}")
                            MutantCardSubtitle("Запись ${12 + i}:0$i")
                        }
                    }
                }
            }
            Box(Modifier.fillMaxSize().graphicsLayer { }) {
                Box(Modifier.fillMaxSize().graphicsLayer { }.drawBehind { read() })
            }
        }
    }
}
