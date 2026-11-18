package io.github.youndie.mutant.surface

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.drawscope.CanvasDrawScope
import androidx.compose.ui.graphics.toAwtImage
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.v2.runComposeUiTest
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import io.github.youndie.mutant.clock.MutantClock
import io.github.youndie.mutant.clock.ProvideMutantClock
import io.github.youndie.mutant.tokens.MutantBody
import java.awt.image.BufferedImage
import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * B-133: the keyframe loss draws the screen's bodies into a bitmap on the CPU, where a body's shader runs pixel by pixel
 * (29 s a loss in the browser, every 9 s). A body the shader draws gives the scene its paths instead: its snapshot is
 * the picture the paths draw, pixel for pixel, every decoration on.
 */
@OptIn(ExperimentalTestApi::class)
class MoshSnapshotOnGpuTest {
    private val d = 2.625f

    /** What the scene's snapshot of a card holds, drawn on a CPU bitmap as the loss draws it. */
    private fun snapshot(onGpu: Boolean): BufferedImage {
        val scene = MoshScene(1)
        var out: BufferedImage? = null
        runComposeUiTest {
            setContent {
                CompositionLocalProvider(
                    LocalDensity provides Density(d),
                    LocalMutantBodyOnGpu provides onGpu,
                    LocalMutantMoshScene provides scene,
                ) {
                    ProvideMutantClock(remember { MutantClock(startMs = 5000.0, frozen = true) }) {
                        Box(Modifier.size(376.dp, 160.dp).testTag("stage")) {
                            MutantSurface(
                                MutantBody.Card,
                                "b133/card",
                                Modifier.offset(24.dp, 16.dp).size(328.dp, 80.dp),
                            ) {}
                        }
                    }
                }
            }
            onNodeWithTag("stage").captureToImage()
            val body = scene.bodies.single()
            if (onGpu) {
                assertTrue(
                    body.snapshot != null,
                    "a body the shader draws gives the scene no snapshot of its own",
                )
            }
            val w = (376 * d).toInt()
            val h = (160 * d).toInt()
            val bitmap = ImageBitmap(w, h)
            CanvasDrawScope().draw(Density(d), LayoutDirection.Ltr, Canvas(bitmap), Size(w.toFloat(), h.toFloat())) {
                body.inSnapshot!!.invoke(this)
            }
            out = bitmap.toAwtImage()
        }
        return out!!
    }

    @Test
    fun `a body the shader draws gives the loss what its paths draw`() {
        val paths = snapshot(onGpu = false)
        val gpu = snapshot(onGpu = true)
        var ink = 0
        for (y in 0 until paths.height) for (x in 0 until paths.width) if (paths.getRGB(x, y) ushr 24 > 0) ink++
        assertTrue(ink > paths.width * paths.height / 10, "the paths' snapshot draws next to nothing: $ink pixels")
        var apart = 0
        for (y in 0 until paths.height) {
            for (x in 0 until paths.width) {
                val p = paths.getRGB(x, y)
                val q = gpu.getRGB(x, y)
                if (intArrayOf(24, 16, 8, 0).any { abs((p shr it and 255) - (q shr it and 255)) > 2 }) apart++
            }
        }
        assertEquals(0, apart, "pixels of the GPU body's snapshot apart from the paths' picture")
    }
}
