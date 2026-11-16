---
id: B-87
title: "In the browser the bodies' ticks have one thread, and the Components page needs more than it"
status: done
priority: P1
size: M
stage: stage-5-product
blocked_by: [B-73]
---

# B-87 — In the browser the bodies' ticks have one thread

Found by [B-73](B-73-frame-cost-in-the-browser.md) on 2026-11-16, in headless Chrome 157 on an Apple M1, on the production wasm build. The Components page at rest holds 24 frames a second, with work p50 60.6 ms and 53 % of intervals over 33 ms. On the JVM it holds 60. With the clean signal it is the same, so the Tape is not the cost. The Library screen alone holds 48, one frame in six over 33 ms.

The profile ([logs](../../logs/b-73/2026-11-16-profile-ComponentsRest-named.txt)) puts the resting Components page's main thread at 100 % busy:
- `gen.frame` is on the stack 50.5 % of the time;
- the builds ahead (B-75's `FrameAhead`, B-82's rasterising) take 28.3 %, run by `Dispatchers.Default`, which in the browser is the page's own message queue;
- the bodies' draw takes 45.6 %, building in the frame the ticks whose build ahead had not run by then.

On the JVM the same work runs on other threads, and the page's tick cost 60–80 ms before B-75 moved it there ([B-72](B-72-frame-cost-on-the-jvm.md)). The browser has one thread for it, and every tick of every visible body.

- **The decision and its reason.** To be decided when the item is taken, measured on B-73's bench (`scripts/frame-bench-browser.mjs`) and profile (`scripts/frame-profile-browser.mjs`). The candidates:
  - **Build only what is on screen.** Check first whether a body scrolled out of the viewport still builds its ticks: a `verticalScroll` column lays out and draws all of its children, and the Components page is many screens tall.
  - **Spread a tick's builds over the frames between ticks**, so no frame waits for all of them. The Library screen's frames late past 33 ms are that burst.
  - **Cut the generator's cost per tick on every target**: the field and the march, and the boxed array accessors the profile shows around them in wasm.
  - Rejected for now: a Web Worker. Kotlin/wasm's GC objects cannot be shared with a worker, so the body's instance and cache would have to be rebuilt there. That is the biggest change, and the last to try.
- AC:
  - On B-73's bench, the Components page at rest, on the wheel and with the clean signal are no longer red: work p50 under 16.7 ms, intervals over 33 ms under 5 %.
  - The Library screen alone has intervals over 33 ms under 5 %.
  - The JVM's numbers on `FrameBench` are no worse, and parity and goldens are unchanged.
- Anchors: `mutant-core/src/commonMain/kotlin/io/github/youndie/mutant/surface/MutantSurface.kt`, `scripts/frame-bench-browser.mjs`, `scripts/frame-profile-browser.mjs`

## Results, 2026-11-16

Three changes, measured on B-73's stand and benches. The final runs are at `085a110`: [browser](../../logs/b-87/2026-11-16-browser-final.log), [JVM](../../logs/b-87/2026-11-16-desktop-final.log). The runs without `-final` are intermediate, at `2271b6d` and `6e3faa5`. `main`'s own code was run again on the JVM the same night, for the JVM's comparison.

1. **A body off the screen builds no tick.** A `verticalScroll` column lays out and draws every child, and every body of the Components page built and rasterised its tick: a body 3 000 dp off the screen drew 31 frames and 28 rasters in 4 s, as one on it did. Now a body works out where it stands from its layout (`standing`): it checks whether its box, 64 dp wider for its drips, meets each box it is laid out in up to the window.
   - A body off the window draws nothing.
   - One that misses only a box inside the window draws its last picture, since that box may not clip it.
   - Its layout callback tells it when it shows again. That holds for a box that grows and for a layer that slides it in, as the sheet, the drawer and the snackbar do: Compose calls `onGloballyPositioned` for a `translationY`, read in a test.
   - A first version looked again once a second as well. No case needed it, and a mutant without it survived every test, so it went.
   - A body hidden from its first frame first kept that frame's vectors as its last picture. The column records every child each frame, so the JVM's main thread did 0.5 ms more a frame, until a body off the window drew nothing.
2. **In the browser each build ahead is a task of its own.** `Dispatchers.Default` runs up to sixteen queued coroutines in one task on wasm, so a tick's builds for every visible body landed between the same two frames.
3. **At the scheduler's `background` priority** (`scheduler.postTask`), so the builds give way to the page's frames. Before it, the clean-signal Components page dropped a frame on half its ticks: 6.7 % of intervals over 33 ms. The commit that first claimed this priority (`56fe78d`) did not carry it: ktlint had wrapped the `@JsFun`, and the edit missed it. A change to a `@JsFun` alone also leaves the production build stale: `compileProductionExecutableKotlinWasmJsOptimize` stays up to date, and the bundle keeps the old JS. `docs/services/mutant-core.md` says how to rebuild.

**The browser**, against B-73 (same stand, five counted rounds; the control read 9.8–9.9 ms against 10):

| Scene | frames/s | > 33 ms | work p50 / p95 | long animation frames a round | Verdict |
|---|---|---|---|---|---|
| Components, at rest | 24.3 → 60.5 | 53.4 → 0.00 % | 60.6 / 117.1 → 5.0 / 16.4 | 59–60 → 0 | RED → GREY |
| Components, wheel | 26.0 → 59.5 | 35.6 → 1.04 % | 12.5 / 30.7 → 7.7 / 16.5 | 79–81 → 1–3 | RED → GREY |
| Components, «Чистый сигнал» | 24.8 → 60.5 | 57.7 → 0.00 % | 62.3 / 112.8 → 3.9 / 13.6 | — → 0 | RED → GREY |
| Demo, Library alone | 48.2 → 57.1 | 16.4 → 3.23 % | 3.4 / 46.2 → 3.5 / 26.8 | 70–76 → 10–12 | RED → GREY |
| Demo page, Recording, Settings, reduce motion | 60 | 0–0.93 → 0 % | — | — | GREY |

The wheel now scrolls 24 634–25 327 px a round, against 19 164–20 154: the page takes its notches sooner.

**The JVM**, `main`'s code against B-87's, run one after the other on the same host:

| Scene | work p95, `main` → B-87 | intervals over 33 ms |
|---|---|---|
| Components, at rest | 6.13 → 5.33 ms | 0.67 → 0.00 % |
| Components, wheel | 6.14 → 4.88 ms | 0.64 → 0.00 % |
| Components, «Чистый сигнал» | 5.81 → 6.04 ms | 0.97 → 0.00 % |

The clean signal's 0.23 ms is inside the spread of `main`'s own two runs (6.01 in B-73's, 5.81 here). The Library screen alone, all of whose bodies show, read work p99 17.4 ms against 14.8–15.9 in the earlier runs. Its long frames are the keyframe loss's (below), which these changes do not touch.

**AC.**
- The Components page at rest, on the wheel and with the clean signal are no longer red. Work p50 is 5.0, 7.7 and 3.9 ms, and intervals over 33 ms are 0, 1.04 and 0 %.
- The Library screen alone has 3.23 % of its intervals over 33 ms, under 5 %.
- On the JVM, the numbers are within the runs' spread or better.
- Parity and goldens are unchanged: `./gradlew check` is green.

**What remains: the keyframe loss.** The Library screen's 10–12 long animation frames a round stand where B-48's loss runs, 3.5 s into each 9 s window. Each step of it copies the whole picture once per block. → [B-88](B-88-keyframe-loss-copies-the-whole-picture.md)

**The recorder's *work* overstates a frame when the page queues tasks.** It is read up to the first task after the frame. At `d864e3c`, the builds queued at normal priority came first, and the resting page read work p50 17.8 ms at 60 frames a second with no long animation frame. At background priority they come after it. The verdict is B-73's, as registered.

**Mutants** (`MutantSurface.kt`), each killed by `OffScreenTicksTest`:

| Mutant | |
|---|---|
| every body counts as on the screen | killed |
| no reach past the box (64 → 0 dp) | killed |
| a change of standing does not invalidate the drawing (`flips`) | killed |
| a body off the window counts as only off a box | killed |
| a body off the screen builds its tick anyway | killed |

The background priority and the task per build are held by the browser's bench only: the JVM's tests run neither.
