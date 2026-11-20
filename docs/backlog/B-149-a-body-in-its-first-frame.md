---
id: B-149
title: "A body drawn in its first frame"
status: done
priority: P1
size: L
stage: stage-6-later
blocked_by: []
---

# B-149 — A body drawn in its first frame

The owner, 2026-11-20, a new round: «И нужно уйти от пропуска рендера на первом кадре».

Since [B-50](B-50-first-frame-off-the-frame.md) a body's first instance is built off the frame: the frame it first
appears in draws its content and nothing of the body (`MutantSurface.kt`, `return@drawWithCache onDrawBehind {}`), and
the body appears from a later frame. In the browser "off the frame" is a task on the page's own thread
(`AheadDispatcher.wasmJs.kt`), so there the body comes as late as that queue lets it. With one group of the Components
page at a time (B-148) every switch shows a page of bare content first.

**What it costs today** ([log](../../logs/b-149/2026-11-20-appear-desktop.log), B-50's `AppearBench`, this M1, JDK 25,
`main` at `66d9c57`, ten 328 × 80 cards appearing in one frame, arms interleaved, rounds 2–6): built in the frame,
**138.4 ms** for that frame, 113.6 of it drawing; off the frame, 18.9 ms. B-50 read 85.8 and 12.8 on the same machine on
2026-11-14: the first build has grown with the performance loop (B-82's tick image, B-117's still layers), while warm
frames have not.

- **The decision.** To be decided from the numbers. A body is to be drawn in the frame it first appears in, on every
  platform, at a cost a frame can carry; a route is chosen from where the first build's time goes.
- **Pre-registered, the first measurement, before any change.** The in-frame arm of `AppearBench` sampled by a
  profiler on the JVM (JFR, or async sampling), ten cards: the share of frame N under `gen.instance`, the dead pixels,
  the first `gen.frame`, `pictureOf`, the still layers' image, the tick image and the vector draw. Each a share of frame
  N with its log.
- Routes known before it, none chosen:
  - the first build made cheaper where it is expensive, the generator's numbers unchanged (its goldens hold them);
  - the first build split: what the first frame must draw built in it, the rest from the next frame without a change
    a viewer can see;
  - on platforms with threads, every body of a frame built in parallel from its layout and joined at its draw.
- AC: the profile, with its log; a route measured by `AppearBench` (and the browser bench in the phone proxy, the phone
  being away) against `main`; every body drawn in its first frame, held by a test that fails on `main`; the goldens,
  parity and numeric goldens unchanged.
- Anchors: `mutant-core/src/commonMain/kotlin/io/github/youndie/mutant/surface/MutantSurface.kt`,
  `mutant-core/src/desktopTest/kotlin/io/github/youndie/mutant/bench/AppearBench.kt`

## Findings

### Iteration 1, 2026-11-20: where the first frame goes

- The first measurement above ran with the sample left running in the iOS simulator by B-138's run (about 60 % of a
  core, for 19 hours), found by B-148's run. Stopped, [again](../../logs/b-149/2026-11-20-appear-desktop-2.log): built in
  the frame 127.8 ms (100.1 drawing), off it 9.4. The cost is the first build's, not the load's.
- [The profile](../../logs/b-149/2026-11-20-appear-jfr-main-thread.txt) (`AppearBench` under JFR, a sample every
  1 ms, [its run](../../logs/b-149/2026-11-20-appear-jfr.log)): of the frame thread's samples under the kit, **87 % are
  the first `gen.frame`, 81 % its `distanceField`, 61 % `fieldStatic`**, the grid's part that is built once an
  instance; the generator's instance 4 %, the still layers' image 3 %, the vectors' draw under 1 %.
- Why `fieldStatic` costs: a body wider than 200 dp has a grid of 1.5 dp, so a 328 × 80 card is about 230 × 100 cells.
  Each cell asks `valueNoise` four times (two terms at 0.8 and 2.1 for the edge noise, two at 0.03 × 0.05 for the warp),
  and each `valueNoise` hashes four lattice corners with a `sin` (`latticeHash`); the warp's pass takes two more `sin`s,
  one depending on the cell's row only, one on its column only, and `topWave` on its column only. About 18 `sin`s a
  cell, four million for ten cards.
- Most of them repeat a value already computed: a lattice corner is shared by every cell around it (the warp's lattice
  is 33 × 20 dp, so a corner serves about 200 cells; the 0.8 term's about 1.4), and a row's or a column's term is the same
  for the whole row or column.

**Pre-registered before the change.** `fieldStatic` computes each lattice corner's hash once per call and each row's
and column's term once, with the same expressions, so every value is the same to the bit. Held if:
- the numeric goldens (`gen/goldens`), the parity tests and the screenshot goldens pass unchanged;
- `AppearBench`'s in-frame frame N falls by at least a quarter against `main`, in two runs of each, alternating
  (`main`, this, `main`, this), on this M1, same host, the median of rounds 2–6 each.
It is not the item's answer: what remains after it decides the route.

### Iteration 2, 2026-11-20: the lattice hashed once — not held

The change (`39868b6`): `fieldStatic` hashes each lattice corner once per call (`LatticeRows`) and computes each row's
and column's term once. `LatticeRowsTest` holds it to `valueNoise` bit for bit over the field's own steps and over 40
random grids; a mutant shifting the lattice rows wrongly fails both its tests. The numeric goldens and the parity
tests: 56 green, unchanged.

[`AppearBench`, alternating](../../logs/b-149/2026-11-20-appear-ab.log), this M1, the host loaded at 8–12 by another
session's headless Chrome (not this item's, left running): in-frame frame N, `main` 127.8 and 120.5 ms, this 155.5 and
97.8. **Not held**: the first run is slower, not a quarter faster. Same host, two runs each, alternating: yes.

Wall time on a loaded host says little, so [the frame thread's CPU time](../../logs/b-149/2026-11-20-first-build-cpu.log)
of ten cards' first build alone (`gen.instance` and the first `gen.frame`, a scratch test, not kept), alternating three
times: `main` 115.2, 125.3, 111.2 ms; this 111.0, 88.5, 106.8. Lower in each pair, by 4 to 29 %, with rounds inside one
run moving 75–135 ms. The JVM's `sin` is cheap: the hashes were not what the first frame spends. In the browser, whose
`sin` is Kotlin's own (`fdlibm`, `__ieee754_rem_pio2` in every profile since B-73), it may be; not measured, the phone
being away and the proxy's host loaded.

**What it means for the item.** Ten cards' first builds cost about 100 ms of CPU on this M1 with or without the
change, about 10 ms a body, in `fieldStatic`'s grid of 1.5 dp: about 23 000 cells for a 328 × 80 card, each with its
distance to the body's rounded shape and two noise terms. That grid is the engine's own (research D3); a coarser one
would be a departure that moves every body. No change inside this design draws ten bodies exactly in a frame of 16 ms.

## The question

How should a body's first frame look? Choices, for the owner:
1. **Exact, in its frame.** The body is built where it first draws, as before B-50: never missing, but the frame in
   which bodies appear takes about 10 ms a body on this M1, several times that on a phone: a group of the Components
   page switching shows a stall of a few hundred ms there.
2. **A stand-in for that frame.** The frame a body appears in draws its slot's plain rounded shape in the body's fill
   and outline; the real body replaces it from the next frame the build is ready. Never bare and never a stall, but a
   visible change from the plain shape to the slime.
3. **Exact, built in parallel** on platforms with threads (desktop, Android, iOS): every body of the frame starts its
   build at layout, and the draw waits for it. Never missing; the stall shrinks by the cores (about 20–30 ms for ten
   cards on this M1). In the browser there is one thread, so there it is choice 1 or 2.
And separately: keep the exact hashing change (no loss, a small gain on the JVM, perhaps more in the browser,
unmeasured) or take it out as not held.

## The decision

The owner, 2026-11-20: «Parallel + stand-in» (choice 3, with choice 2 in the browser). The exact hashing change
(`39868b6`) stays: it is the same to the bit, held by `LatticeRowsTest`, and costs nothing (not held as a speed-up).

- **Where the platform has threads** (desktop, Android, iOS): a body whose first instance is not built yet asks for its
  build at layout; the first body drawn in a frame starts every build asked for, each on `Dispatchers.Default`, by then
  every clean zone of the frame being registered; each body's draw then waits for its own. A body with no build asked
  for (or a signature that changed between layout and draw) builds in its draw, as before B-50. Every body is drawn
  exactly in its first frame.
- **In the browser** (one thread): the first frame draws a stand-in, the slot's rounded shape in the body's fill with
  its outline, and the body from the frame its build is taken in, as B-50 does.
- **Pre-registered, before the change:**
  - a test, failing on `main`: under a running clock, on the desktop, the body is drawn in its first frame, built once,
    and every frame from the first is the picture a body built in its frame draws;
  - a test of the stand-in, the platform's waiting turned off by a seam: the first frame draws the stand-in and builds
    nothing in the frame, the body is drawn from the next frame;
  - the screenshot goldens, the parity tests and the numeric goldens unchanged;
  - `AppearBench`, the off-the-frame arm now the parallel build: its frame N under half of the in-frame arm's in every
    counted round, on this M1, the host's load logged;
  - iOS, Android and wasm compile.

### Iteration 3, 2026-11-20: the first frame drawn

`4d3d1fa`. `FirstBuilds` takes the frame's first builds: a body with no instance asks at layout (its surface's
`onGloballyPositioned`, a running clock), the first body drawn in the frame starts every build asked for on
`Dispatchers.Default` (`HeldInstance.startFirst`), each body's draw waits for its own (`takeFirst`, `awaitHere`:
`runBlocking` on the desktop, Android and iOS). A body with nothing started for it (a standing clock, whose first build
also makes B-83's image; a slot changed since layout) builds in its draw. In the browser (`firstFrameWaits = false`) the
first frame draws `drawStandIn` and builds as B-50 did. The slot and its signature come from one function, `slotOf`,
at layout and at draw.

- `FirstFrameOffTheFrameTest`, rewritten to the new contract, five tests green:
  - under a running clock the body is drawn in its first frame, built once, no stand-in;
  - every frame, the first included, is what a body built in its frame draws (before: every frame but the first);
  - with the seam `LocalMutantFirstFrameWaits` off, as in the browser: the first frame draws the stand-in and builds
    nothing in the frame, the body is taken in the next, one stand-in in all;
  - unchanged: a frozen clock draws in its first frame; a later rebuild is drawn in its own frame.
  A mutant turning the desktop's waiting off fails exactly the first two. On `main` the first fails (nothing built in
  the first frame) and the second at frame 0.
- `make check` green; `./gradlew check` green, 479 tests, 0 failed, every screenshot golden, parity test and numeric
  golden unchanged; iOS (arm64, simulator), Android and the wasm sample compile.
- [`AppearBench`](../../logs/b-149/2026-11-20-appear-parallel.log), this M1, the host loaded 8–14 by another session:
  frame N, the parallel arm against the in-frame arm, per counted round 59.3/86.0, 50.0/80.9, 43.2/99.6, 40.9/72.8,
  40.2/74.2 ms (69, 62, 43, 56, 54 %); medians 43.2 and 80.9. **The bar written, under half in every round, is not
  held**: met in one round of five. The first frame of ten appearing cards costs about 43 ms where B-50's frame
  without bodies cost about 15. Not measured: the browser's stand-in on a screen (the phone away; held on the desktop
  by the seam alone), and Android and iOS, compiled only.

**Merged on the owner's word**, 2026-11-20: «Merge it», the bar on `AppearBench` not held as written above.
