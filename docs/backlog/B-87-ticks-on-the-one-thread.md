---
id: B-87
title: "In the browser the bodies' ticks have one thread, and the Components page needs more than it"
status: wip
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
