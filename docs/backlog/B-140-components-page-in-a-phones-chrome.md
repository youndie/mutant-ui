---
id: B-140
title: "The resting Components page in a phone's Chrome falls to 16 frames a second"
status: wip
priority: P2
size: M
stage: stage-6-later
blocked_by: []
---

# B-140 — The resting Components page in a phone's Chrome falls to 16 frames a second

Found by [B-138](B-138-a-round-on-every-platform.md) on the Pixel 6a, Chrome 157, 2026-11-19, the kit at `102a2dd`: the
resting Components page at 16.4 frames a second pooled, work p50 69.8 ms. The owner, the same day: «Разобрать
Components в Chrome».

What B-138's own frames already show (`logs/b-138/2026-11-19-chrome-pixel/frames.csv`, read per second of each load):
- the work does not rise with the battery's temperature: by round it is 17, 71, 92, 42, 100, 60 ms at p50, at 35.0,
  37.4, 37.9, 38.2, 38.4, 38.4 °C;
- inside one load it holds a level for seconds, then steps up and seldom comes back: levels near 15, 32, 60 and
  90–100 ms; round 1's load held 15–17 ms for seven seconds and stepped to 127 at its eighth;
- the same page with the clock stopped (`ComponentsReduce`) holds 6–7 ms in every round; the clean signal held 6 ms in
  round 1 and 10–100 afterwards.

- **The decision.** To be decided from the numbers; this item first finds the mechanism.
- **Pre-registered on 2026-11-19, before the first number of this item.**
  - **Code.** The kit and the sample at `102a2dd` (`main` at `2d36670` builds the same wasm: `2be7f9bb49abc303`,
    `089052ba37e2a6e8`).
  - **Q1, heat or the page.** `ComponentsRest` alone in the phone's Chrome, 6 × 10 s, starting cool (battery under
    28 °C), on mains, the big cores' current and maximum frequency read after every scene beside the temperature; then
    one load held 60 s. Heat says: the work follows the maximum frequency and the temperature. The page says: levels
    and steps at a steady frequency, a cool phone included.
  - **Q2, which work.** A sampled profile (CDP, every 100 µs, 8 s) of the resting Components page on the phone, with the
    Kotlin module rebuilt by the same `wasm-opt` passes plus `-g` (B-73's way), once in a fast load and once in a slow
    one. Measured: the share of busy time with `gen.frame` under the bodies' draw (a tick built in the frame) and under
    the build ahead (B-75's `FrameAhead`, run by `Dispatchers.Default`, which in the browser is the page's own queue).
  - **The prediction**, written before it is tested: a cascade of missed builds ahead. A resting tick taken from its
    build ahead costs the frame little; one not ready is built in the frame, which makes the frame late, which leaves
    the next build ahead not ready, while the builds ahead still run on the same thread. That gives two steady modes and
    steps between them. It is refuted if the slow profile shows no more `gen.frame` under the draw than the fast one.
  - **No fix before the mechanism.** A route is chosen from Q1 and Q2 and measured by B-73's bench on the phone, against
    this item's numbers, with its own control.
- AC: raw logs under `logs/b-140/`; Q1 and Q2 answered, each with its log; the mechanism named only where one variable
  moved its signature.
- Anchors: `mutant-core/src/commonMain/kotlin/io/github/youndie/mutant/surface/MutantSurface.kt`,
  `scripts/frame-bench-browser.mjs`, `scripts/frame-profile-browser.mjs`

## Findings

### Iteration 1, 2026-11-19: a leaked bench tab

Waiting for the phone to cool after the priority A/B (`logs/b-140/2026-11-19-priority-warm.log`), it rose to 37.4 °C with
the big cores' maximum at 1106 MHz and nothing measuring. `top` on the phone: one Chrome renderer at 110 % of a core,
112 min of CPU. The tab list (read only) held `http://127.0.0.1:8761/?scene=DemoPageClean&load=4`: this bench's own
address, round 1's fourth scene of the phone's list, left by a killed run; its server was gone, the page loaded and
drawing. It was closed (only it); the renderer left `top` within 5 s.

So every phone run of B-138 and of this item so far may have run beside it: 127.0.0.1 is one site whatever the port,
so it most likely shared the measured tab's renderer, and in the front between runs it heated the phone. Whether it
moved the numbers is not known; the cool `ComponentsRest` run is repeated without it as the control. Both scripts now
close any page with their own address (`http://127.0.0.1:876N/?scene=`) before choosing their tab and log it.

**Pre-registered on 2026-11-19, after the leak was found and before any run without it.** The same day's earlier
numbers carry the leak, so they are not the item's baseline until this is answered.
- **Q0, the leak.** On mains, starting cool (battery ≤ 28.5 °C), `ComponentsRest` alone, 6 × 10 s, the build of
  `logs/b-140/2026-11-19-rest-cool.log` (`2be7f9bb49abc303`), with no bench tab but the measured one (the scripts'
  log line says what they closed). Then, as soon as it ends, the same run with a leak made on purpose: a second tab
  opened by intent on `DemoPageClean` from that build, served on a port outside the scripts' own range (8780), left in
  the background, as the found one was.
- **The prediction**: the leak is the mechanism of the day's slowdown. Clean, the work comes back near B-135's
  shader arm on the same phone the day before (`logs/b-135/2026-11-18-pixel6a.log`, `~b`: 53.5 f/s, work p50 5.9 ms)
  and well under the leaked cool run (13.5 ms); with the leak back, it rises again. Refuted if the clean run's work
  p50 is within 2 ms of 13.5, or the leak arm's within 2 ms of the clean one's. Heat runs the same way between the
  two arms (the second is warmer), so only a leak arm *slower* than the clean one by more than the clean run's own
  round-to-round spread names the leak.
- If the leak is the mechanism, Q1 and Q2 are re-measured without it before anything else is said of the page.
- **Amended before the run, 2026-11-19**: the owner asked to measure at once («меряй щас»); the clean arm starts at
  29.5 °C with the big cores' maximum back at 2802 MHz, a degree over the 28.5 °C written above. Heat runs against the
  prediction (a warmer phone is slower), so a clean run faster than the leaked cool run is not explained by it.

### Iteration 2, 2026-11-19: Q0 answered, and the build

[Clean](../../logs/b-140/2026-11-19-q0-clean.log), 29.3–29.6 °C: `ComponentsRest` 51.7 f/s, work p50 12.7 ms (rounds
11.9–15.1). [Leak made on purpose](../../logs/b-140/2026-11-19-q0-leak.log), right after, 30.2–32.3 °C: 44.5 f/s, work
p50 16.3 ms (16.1–17.0), busy 99.8 % every round. Same host, same build, two runs a minute apart: yes.
- **The prediction is refuted**: the clean run is 0.8 ms under the leaked cool run (13.5), inside the 2 ms written. The
  leak is not why the day's page costs twice the day before's.
- The leak arm is 3.6 ms slower, more than the clean run's spread (3.2), so a background bench tab does cost the
  measured one; it ran a degree or two warmer, so the size is not the leak's alone.
- Chrome (157.0.8103.78), the phone and its build are the same as B-135's run the day before; the kit's build is not.
  Between B-135's (`66cc1f0`) and this one (`102a2dd`): [B-136](B-136-the-tape-band-on-every-frame.md) (the Tape's band
  and head noise on every frame; its cost on the phone "not measured here"), the edge mosaic behind a switch, and the
  shader by default (B-135's own `~b` arm).

**Pre-registered before the run**: `ComponentsRest`, today's build (`2be7f9bb49abc303`) against B-135's (`c64ceef…`,
`31029a6aaddbdee5`) as `~b`, interleaved, 6 × 10 s. If the build is the cause, `~b` reads near B-135's 5.9 ms and is
under today's by more than 2 ms in every counted round; if it is the phone's state, the two stand within 2 ms.

[The build A/B](../../logs/b-140/2026-11-19-build-ab.log), interleaved, 33.2–35.2 °C, same host and run: today's build
36.7 f/s, work p50 20.9 ms, busy 99.8 % in every round; B-135's 51.6 f/s, 6.1 ms, busy 75–83 %. Paired, `~b` minus
today's, work p50 per counted round −12.0, −18.9, −10.6, −16.0, −17.2 ms (mean −14.9); busy −16.9 to −24.9 points.
- **The build is the cause**, by the rule written: more than 2 ms in every counted round. The phone's state is not.
- B-135's build holds 5.5–6.7 ms while the phone warms from 33 to 35 °C; today's climbs from 15 to 25 ms with it. A
  page with room on the thread does not feel the frequency; one at 99.8 % does — which is what Q1 read as "heat".

**Pre-registered before the run**: one variable, B-136. Arm `~b` is `main` (`2d36670`) with `MutantTape.kt` as it
stood before B-136 (`7e590c0^`; the file is unchanged since, the theme's part of B-136 is KDoc), interleaved with
today's build, `ComponentsRest`, 6 × 10 s. B-136 is the cause if `~b` is under today's by more than 2 ms in every
counted round and within 2 ms of B-135's build (6.1 ms in the A/B above); a part of it if only the first holds.
