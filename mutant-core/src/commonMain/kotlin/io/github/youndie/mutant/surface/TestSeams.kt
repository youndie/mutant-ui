package io.github.youndie.mutant.surface

import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size

// The switches, probes and parity overrides of the body and the Tape: for tests, benches and parity fixtures only,
// never for a consumer. The kit never provides one; each default is what the kit draws with. Each is still read by the
// test or bench its KDoc names (B-147); one nothing reads any more is removed, not left here.

// Parity fixtures (`ComponentParity`, `ButtonParityTest`, `IllustrationParityTest`): a body where the engine has it.

/**
 * A parity fixture's way to draw an outline at the width the engine drew it: the engine seeds the outline's
 * tremble with its running slot count, which a kit has no counterpart for (research §1.10, D16). Null in the kit.
 */
internal val LocalMutantOutlineOverride = staticCompositionLocalOf<((String) -> Float?)?> { null }

/**
 * A parity fixture's way to stand a body where the engine stood it: the engine generates a slot in its stage's
 * coordinates, so its noise depends on where the slot is, where the kit's is its own (D16). The body is generated
 * at that origin and drawn back at its place; the speckle is anchored there too. Null in the kit.
 */
internal val LocalMutantSlotOrigin = staticCompositionLocalOf<((String) -> Offset?)?> { null }

/**
 * A parity fixture's way to give a body the engine's text zone (`textZone`): Chrome measures a label's box at a
 * fraction (83.29 dp) where Compose rounds it up (84), and a flow line starting between the two is kept by one and
 * dropped by the other. Null in the kit, which measures its own content.
 */
internal val LocalMutantTextZone = staticCompositionLocalOf<((String) -> DoubleArray?)?> { null }

/**
 * A parity fixture's way to generate a body at the size the engine generated it: Chrome lays a label-sized slot out
 * at a fraction (103.4 dp) the engine records whole (103), where Compose rounds the label up (104). The body is
 * generated at that size and drawn in its own box. Null in the kit.
 */
internal val LocalMutantSlotSize = staticCompositionLocalOf<((String) -> Size?)?> { null }

// Switches: a test or a bench turns one of the performance loop's paths off to compare with it, or leaves a part out.

/**
 * Whether a running clock builds a body's first instance off the frame (B-50). Off only for a test or a bench that
 * compares with building it in the frame, as before (`FirstFrameOffTheFrameTest`, `AppearBench`).
 */
internal val LocalMutantBuildOffFrame = staticCompositionLocalOf { true }

/**
 * B-149: whether a body's first frame waits for its build where the platform could; null follows the platform
 * ([firstFrameWaits]). `false` only for a test of the browser's stand-in on the desktop (`FirstFrameOffTheFrameTest`).
 */
internal val LocalMutantFirstFrameWaits = staticCompositionLocalOf<Boolean?> { null }

/**
 * B-173: whether a resting tick waits for the frame built ahead for it (B-75), as a first frame waits for its build
 * (B-149), where the kit draws the tick's vectors when that thread is late: a test's pictures then do not depend on how
 * fast a background thread ran. On only where the frame can wait, for `DemoTapeOnceTest` and `TallBodyBandTest`.
 */
internal val LocalMutantTickWaits = staticCompositionLocalOf { false }

/**
 * B-156: whether a surface leaving the composition gives its body to [BodyPool] and a later one takes it back; null
 * follows the platform: the browser, where a body cannot be built on another thread. `true` for its tests on the desktop.
 */
internal val LocalMutantPoolBodies = staticCompositionLocalOf<Boolean?> { null }

/**
 * Whether a resting body under a running clock is rasterised once per tick, ahead with its frame, and drawn as that
 * image until the next tick (B-82). Off only for a test or a bench comparing it with drawing the vectors
 * (`BodyAsImageTest`, `BodyRenderBench`).
 */
internal val LocalMutantBodyAsImage = staticCompositionLocalOf { true }

/**
 * B-117: whether a body's still layers are drawn once per instance. Off only for a test comparing it with drawing them
 * each tick (`StillLayersOnceTest`).
 */
internal val LocalMutantStillLayersOnce = staticCompositionLocalOf { true }

/**
 * The parts of a body, each of which a measurement can leave out of the draw (B-78). The kit draws them all;
 * [LocalMutantSkipBodyParts] is empty but for a bench or a test (`BodyRenderBench`, `BodyShaderParityTest`).
 */
internal enum class BodyPart { Shadow, Fill, Speckle, Mosaic, DeadPixels, FlowLines, Highlight, Outline, ContourClip }

/** What a measurement leaves out of every body's draw (B-78); empty in the kit. */
internal val LocalMutantSkipBodyParts = staticCompositionLocalOf<Set<BodyPart>> { emptySet() }

/** A part of the Tape, as B-77's bench leaves one out to read what it costs. */
internal enum class TapePart { Lines, Band, Grain, Noise }

/** The parts of the Tape left out: for B-77's bench only (`TapeRenderBench`), empty in the kit. */
internal val LocalMutantTapeSkip = staticCompositionLocalOf<Set<TapePart>> { emptySet() }

// Probes: told what the body or the Tape did, for a test or a bench to read. Null in the kit.

/** B-149: told each time a body draws its stand-in, its build not ready: `FirstFrameOffTheFrameTest` reads it. */
internal val LocalMutantStandInProbe = staticCompositionLocalOf<(() -> Unit)?> { null }

/**
 * Told, for every body frame a surface draws, whether it was built ahead off the frame (B-75), whether it is drawn from
 * an image rasterised ahead (B-82), and whether its webs were steadied (B-84): the surface's tests and the tall-body and
 * Android benches read it.
 */
internal val LocalMutantFrameProbe =
    staticCompositionLocalOf<
        (
            (
                fromAhead: Boolean,
                asImage: Boolean,
                steadyWebs: Boolean,
            ) -> Unit
        )?,
    > { null }

/** Told the time and the bytes of every body rasterised (B-82): the benches and `BodyAsImageTest` read it. */
internal val LocalMutantRasterProbe = staticCompositionLocalOf<((nanos: Long, bytes: Long) -> Unit)?> { null }

/** Called on every draw of the Tape itself: for its tests only (B-141), to tell its draws from its holder's. */
internal val LocalMutantTapeProbe = staticCompositionLocalOf<(() -> Unit)?> { null }
