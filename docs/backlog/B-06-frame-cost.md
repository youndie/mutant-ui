---
id: B-06
title: "The cost of the field on the desktop JVM: an instance and a frame per profile"
status: done
priority: P0
size: M
stage: stage-0-risks
blocked_by: [B-03]
---

# B-06 — The cost of the field on the desktop JVM: an instance and a frame per profile

The handoff names this the main risk: the signed-distance field is recomputed on the CPU each tick for every visible instance, and the speed on a device was never measured. The brief's target is 60 fps on a Pixel 6a with ten cards and the Tape, a 100-item list scrolling, and an instance generated in ≤ 1 ms (research D13, H3).

- **The decision and its reason.** Measure where the architecture can still change: `genItem` and one frame per profile on the desktop JVM right after [B-03](B-03-generator-port.md); then the first drawn body ([B-10](B-10-mutant-surface.md)) on a Pixel 6a, ten cards, `FrameMetrics` (animation + layout + draw), with a control that busy-waits a known time inside the same nesting to prove the recorder sees it.
- Verdict against the brief's green and red; on red, the brief's retreat order (static Tape tile, no grain, idle ticks only for six visible components, mosaic only on state change) and the handoff's two outs (a coarser grid, a shader field), each measured.
- Rejected: measuring last (plakat-ui's order); there the architecture did not depend on the answer.

- AC: a verdict per target in the research with raw numbers and the control's reading; the instance budget (≤ 1 ms) and the frame budget each green, red or not measured.
- Anchors: `reference/design-system/mutant-generator.js`, `reference/design-system/brief.md`, `reference/design-system/mutant-engine.js`

## Pre-registration (2026-11-14, before the first number)

The device half moves to [B-49](B-49-frame-cost-on-a-pixel-6a.md): on a phone there is nothing to draw
until the first body exists (B-10), and no device is attached to this Mac today. This item measures the
desktop JVM, which screens the architecture: a frame too slow here is red everywhere.

- **Subject.** The port at this branch's base: `MutantGenerator.instance` (`genItem`) and `frame`
  (primitives, field, two marches, the outline, the highlight and the flow lines), for every case of
  `scripts/gen-goldens.mjs` (27 instances, 13 profiles), at rest (t = 5000) and hot (heat 1). A frame is
  measured with the instance's field cache warm, as a running kit has it, and separately cold (first
  frame of an instance).
- **Unit.** µs per call, the median of a round of ≥ 200 calls; the round's spread beside it. Not
  fps: a frame's share of the 16.7 ms budget is computed from it.
- **Host.** This Mac (MacBookAir10,1, Apple M1, 8 cores, macOS 27.0.1), the Gradle toolchain's JDK 25,
  one JVM, nothing else running that the run starts. `gen=local` does not apply: there is no generator.
- **Protocol.** Warm-up of 3 s per case before counting; cases interleaved; 5 rounds, round 1
  discarded; every run's output into `logs/b-06/` with a header (host, JDK, commit).
- **Ruler.** The same measurement twice in one run (arms A and A'); a case's difference between them is
  its noise, and a later difference smaller than twice it is *below resolution*.
- **Positive control.** An arm that runs the card's warm frame plus a busy-wait of exactly 200 µs; it must
  read the card's frame + 200 µs ± 20 %, or the run is void.
- **Load delivered.** Every call's output is folded into a checksum printed with the round (the contour's
  point count and the field's sum), so nothing is optimised away.
- **Thresholds**, for the desktop, sized from the brief's phone targets with a factor of 4 between this
  M1 and a Pixel 6a (an assumption B-49 checks):
  - instance (`genItem`): **green** ≤ 0.25 ms for every case, **red** > 1 ms for any (the brief's phone
    budget, already missed on the desktop);
  - warm frame of a 328 × 80 card: **green** ≤ 0.4 ms (ten cards on one tick ≤ 4 ms here, ≤ 16 ms on the
    phone), **red** > 1.6 ms (ten cards ≥ 16 ms here: the frame is missed before the phone);
  - anything between is **grey**, decided by B-49.
- **Kill.** Red on the card frame stops the components built on the CPU field: the retreat order of the
  brief and the handoff's two outs (a coarser grid, a shader field) become the next items before B-10.

## Amendment 1 (2026-11-14, after a void run, before a valid one)

The first run (`logs/b-06/2026-11-14-desktop.log`, harness `e052507`) is **void** by the rule above: the
control read +136, +157, +207 and +197 µs in the counted rounds against 200 ± 20 %. Its cause is in the
same log: the warm frame and the control were timed as separate blocks of 200 calls, and the card's warm
frame itself moved between blocks (523–672 µs), so the control measured the drift. The harness now calls
every arm once per step, in turn, for all arms and the control alike; nothing else changes — not the
thresholds, the rounds, the calls or the control's 200 µs. The void run stays in `logs/` and none of its
numbers is reported as a result.

## Result (2026-11-14)

Run `logs/b-06/2026-11-14-desktop-2.log` (harness `8e52390`, this Mac, JDK 25.0.4.1, one JVM), read by
`scripts/b06_summary.py` into `logs/b-06/2026-11-14-desktop-2.summary.txt`. Medians of rounds 2–5, µs.

- **Control: valid.** The card's warm frame plus 200 µs read +199.7, +197.7, +199.0, +199.7 µs.
- **Ruler.** |warm − warm2| is 0.2–17.3 µs per case, under 2.1 % of the frame; the card's 10.3 µs.
- **Load delivered.** The checksum moves every round; every call's output is folded into it.

| Question | Verdict | Number |
|---|---|---|
| Instance (`genItem`) ≤ 0.25 ms for every case | **GREEN** | worst 107.7 µs (`fab-b`, 136 × 56); a 328 × 80 card 53.3 µs |
| Warm frame of a 328 × 80 card ≤ 0.4 ms green, > 1.6 ms red | **GREY** | 532.5 µs (ruler 10.3): ten cards on one tick 5.3 ms here; at the assumed factor of 4, about 21 ms on a Pixel 6a — decided by [B-49](B-49-frame-cost-on-a-pixel-6a.md) |

Same host, same run: yes, for every number above.

Beyond the thresholds, measured and not judged:

- **The first frame of an instance is ten times its warm frame**: 5.8 ms for the card, 10.2 ms for the
  160 × 120 blob, 7.6 ms for the 200 × 48 button; it builds the field's static layers (`FieldStatic`).
  Ten new cards appearing together cost about 58 ms of CPU here. [B-10](B-10-mutant-surface.md) has to
  build them off the frame, or spread them.
- **Hot frames cost 10–25 % more than warm ones** (card 614 µs against 533): the bubbles, the boil's
  noise layer per phase and the hot tick.
- **The widest frames are the 1.5 dp grid's** (card, blob, field), but the price follows the field's
  node count and the drips more than the width: the 160 × 120 blob (0.9 dp grid, 28 dp of drips) is the
  most expensive warm frame, 858.9 µs.
- A disabled body (`btn-still`) still costs a full frame here because the harness draws it; the engine
  draws disabled and clean slots without the field at all.

What it does not show: the phone (B-49); the cost of turning paths into Compose `Path`s and drawing
them; the garbage a frame allocates — about 0.9 MB for the card by the sizes of the field and the march's
buffers, an estimate, not measured — which a long run and the device will show; several instances computed on one tick in parallel.

The kill criterion did not fire (no red); the components may be built on the CPU field, with the grey
carried into B-49.
