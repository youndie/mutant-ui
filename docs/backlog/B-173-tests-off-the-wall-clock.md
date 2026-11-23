---
id: B-173
title: "Two tests hang on whether a background build finished in wall time"
status: done
priority: P1
size: S
stage: stage-6-later
blocked_by: []
---

# B-173 — Two tests hang on whether a background build finished in wall time

Two tests failed now and then on the CI Mac (`macos-latest`, `./gradlew check --no-daemon`), and once on this Mac under
load. Each passed alone.

- `DemoTapeOnceTest` ([B-101](B-101-demo-tape-twice.md)): on CI on 2026-11-17 (three times on `main`), 19.11, 20.11 and
  22.11, with «instant 1: 425 px unlike the phone alone» and «instant 2: 405 px»; on this Mac on 23.11 with «instant 1:
  6498 px». The bound is 0.2 % of the 360 × 455 capture, 327.6 px.
- `TallBodyBandTest`, «a tall body rasterises the band around the window, not its whole height»
  ([B-86](B-86-tall-body-rasterised-whole.md)): on CI on 23.11 with «images rasterised: 1», two asked.

## Cause

A resting body's next tick is built ahead on `Dispatchers.Default`: its frame and, for a body drawn as an image, the
image ([B-75](B-75-tick-frames-off-the-main-thread.md), B-82, B-86). At the tick, `FrameAhead.take` hands the build over
only if it is done. A late build is not waited for: the tick builds its frame in the frame and draws vectors. A viewer
cannot tell, but the vectors and the image differ at every edge. The desktop's first frame waits for its build
([B-149](B-149-a-body-in-its-first-frame.md)); the browser never builds in the frame
([B-153](B-153-a-first-picture-between-frames.md), [B-155](B-155-no-tick-built-in-a-frame.md),
[B-161](B-161-builds-in-steps.md)). The desktop's later ticks do neither: they race the frame.

A test clock advances in no time. So whether a tick drew its image or its vectors came down to how fast the machine's
background threads were in wall time:

- `DemoTapeOnceTest` compares two compositions at the same instant, the phone on the page and the phone alone. When one
  composition's tick builds were late, its bodies drew vectors where the other's drew images.
- `TallBodyBandTest` slept 6 ms a frame for 40 frames and counted the images rasterised. On this Mac's own run the count
  is exactly the two asked: the body's cache settles at its fourth tick, and the fifth tick takes the one image. The
  second image is still being rasterised when the test reads the count. On a slower runner that second image was not
  done yet.

## Reproduced

A scratch delay before every tick build in `FrameAhead.prepare` (`delay(200)`, not committed), on this Mac:

| Run | Result |
|---|---|
| Demo, as on `main`, no delay | 1–3 px an instant, one instant 146 (the band's edge, B-86) |
| Demo, the page's tick builds late, the phone alone's not | 6 834–7 113 px at all 28 instants; body frames: page 0 images / 1 785 vectors, alone 806 / 66 |
| Demo, the phone alone's late, the page's not | the same 28 of 28, 6 834–7 113 px |
| Demo, both late | 1 instant, 6 961 px |
| `DemoTapeOnceTest` as on `main`, every build late | fails, at instants 1 to 21 or more, 5 039–7 092 px |
| `TallBodyBandTest` as on `main`, no delay | 2 rasterised; 5 body draws: 1 image, 4 vectors |
| `TallBodyBandTest` as on `main`, every build late | «images rasterised: 1». Its third test, «scrolled past its band…», fails too, with «ticks drawn from images: 1» |

The 6 498 px on this Mac is one composition's ticks all late. The 405–425 px on CI is a body or two late.

## Decision

The tests wait for the build instead of racing it. A test seam, `LocalMutantTickWaits` (`surface/TestSeams.kt`, `false`
in the kit), makes a resting tick wait in `FrameAhead.take` for the frame prepared for that very instance and time. This
is the wait a first frame already makes on the desktop. The seam is read only where the frame can wait (`waits`). The
kit never provides it, so no product path changes.

- `DemoTapeOnceTest` provides the seam in both compositions, so each tick draws its image built ahead. The sample cannot
  name an internal of `mutant-core`, so it reaches the seam's JVM getter (`TestSeamsKt.getLocalMutantTickWaits`) by
  reflection.
- `TallBodyBandTest`'s card provides it, its 6 ms sleeps are gone, and the rasterising test goes on to the next tick
  drawn. That tick waits for the image the last one asked for. The third test of the class sleeps no more either, and is
  held by the same seam.

No bound moved: `MAX_SHARE` stays at 0.2 %, `EDGE_SHARE` at 0.05 %, `bytes.size >= 2` and two thirds of the whole image's
bytes as they were.

**Rejected**:

- retrying the test, or raising its tolerance: a retry hides a test that tells nothing on a busy machine, and a tolerance
  that lets 7 000 px through lets a second Tape through too. The rules do not allow either.
- sleeping longer: still wall time, and only less likely to fail.
- drawing both compositions as vectors (`LocalMutantBodyAsImage` off): `TallBodyBandTest` is about the image, and the
  Demo phones would no longer be drawn as the kit draws them.
- the seam made public, or the sample's test made a friend of `mutant-core` by its compiler's paths: the first puts a
  test switch in the consumer's API, and the second leans on where Gradle puts a project's classes.

## AC

- Each test holds what it held, whatever the wall time: it passes under the repro that made it fail, and fails under the
  mutant it was written to catch.
- No bound raised, no retry. The kit draws as before: the seam defaults to `false`, and every other test, golden and
  parity is unchanged.
- Anchors: `mutant-core/src/commonMain/kotlin/io/github/youndie/mutant/surface/TestSeams.kt`,
  `mutant-core/src/commonMain/kotlin/io/github/youndie/mutant/surface/BodyAhead.kt`,
  `sample/src/desktopTest/kotlin/io/github/youndie/mutant/sample/DemoTapeOnceTest.kt`,
  `mutant-core/src/desktopTest/kotlin/io/github/youndie/mutant/surface/TallBodyBandTest.kt`

## Findings, 2026-11-23

With the seam on and the same delay, the Demo comparison is the same picture as without it. Every arm — page late, alone
late, both late — draws 1 674 images / 111 vectors on the page and 806 / 66 alone. Every arm has the same 28 counts as
the run with no delay: 1–3 px, one instant 146. Both tests pass under every build late.
