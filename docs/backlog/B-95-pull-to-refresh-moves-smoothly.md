---
id: B-95
title: "Pull-to-refresh moves in steps and cuts between its phases"
status: open
priority: P2
size: M
stage: stage-6-later
---

# B-95 — Pull-to-refresh moves in steps and cuts between its phases

The owner, 2026-11-16: «pulltorefresh надо отладить — чтобы все анимации были плавные».

What the code does now (`MutantPullToRefresh.kt`):
- A short release returns over 200 ms stepped at 12 a second: three jumps with no easing.
- A release past the line jumps the gap from where the finger left it to 74 dp.
- The end swaps the tongue (gap 74) for the done pill (gap 52) at once, a 22 dp jump. The pill then stands a second on a wall-clock `delay`.
- The tongue appears at 16 dp already 26 × 4 dp, and it regenerates its body at every whole dp of the pull.

- **The decision and its reason.** Taken after a frame strip of each phase (Findings below). Outside a drag the gap glides, every frame of the screen clock, from where it stands to the phase's gap along a closed-form eased-out cubic, and a change of phase mid-way takes it up from where it is: no jump between phases. The slime keeps its own tick (D6): the tongue's body is rebuilt once per hot tick and scaled to the gap's exact size between. Rejected: keeping the kit's 12-a-second `stepped()` transitions here, which is what the owner saw as steps. Each number the design does not state is listed below (D15).
- AC:
  - Over every millisecond of a pull, a short release, a refresh and its end, the content's offset steps by no more than a frame's worth of its curve. A continuity test holds it.
  - Under reduce motion every move is at once.
  - Frame strips before and after are attached to the PR.
- Anchors: `mutant-core/src/commonMain/kotlin/io/github/youndie/mutant/components/MutantPullToRefresh.kt`

## Findings (2026-11-16)

**What the frames showed.** A throwaway contact-sheet test drove the real gesture (a touch drag, density 2) and captured every frame of each phase, logging the list's top:
- **Let go short** (gap 25.5 dp): the list stood 83 ms, then fell 10.5 dp, stood again, 10.5 dp, then 4.5 dp — three jumps, linear, at 12 a second.
- **Let go past the line** (gap 95.5 dp): 95.5 → 74 in one frame.
- **Refreshing ends.** One frame with the list at the very top (gap 0) and nothing above it, then the pill at 52. The pill's word drew one frame before its body.
- **After the pill's second.** The pill went, the gap stood empty a frame, then the tongue grew back in it, while the gap fell 52 → 30.5 → 8.5 → 0 in 83 ms steps.
- **The pull.** The tongue first showed at a 24 dp gap as a 38 × 6 dp lump with its drips: from a 16 dp gap it was already 26 × 4 dp.

**The cause.**
- `RefreshPhase` stepped the way back by `stepped()`, the kit's 12-a-second transition steps, with no easing.
- Every phase other than the way back stood at its gap at once (`refreshing -> 74`, `doneShown -> 52`).
- The pill was marked by a `LaunchedEffect`, a frame after the composition that saw refreshing end. That composition fell through to the way back, which had already ended, so the gap was 0. The pill's second was a wall-clock `delay`.
- The tongue's size was `gap / 40` of the short tongue from 16 dp, and was rounded to whole dp, each a new body.

**The fix** (`MutantPullToRefresh.kt`).
- **`PullGlide`.** Outside a drag the gap moves from where it stands to the phase's gap (0; the long tongue's 74; the pill's 52) along `1 − (1 − p)³` over 300 ms of the screen clock. A new phase mid-way starts from the gap at that moment. It asks for a frame only while it moves. A screen composed already refreshing stands open from its first frame.
- **Grabbing it mid-way.** A finger or mouse picks the gap up from where it stands, not from 0.
- **The pill.** It is marked by composition as it sees refreshing end, so no frame falls through. Its second is counted on the screen clock, and waited out by a `delay` only under reduce motion, where that clock stands.
- **The way back after the pill.** The pill rides up with the content. It keeps its place over the content's top and is clipped at the tear line. The clip is applied only while it rides, never in the standing pill's frame. No tongue grows back in the closing gap.
- **The tongue.** It grows out of nothing from a 16 dp gap to the short tongue at 40: the short tongue's body is scaled down whole, about its top. From 40 to 74 its body is built at whole dp once per hot tick, and scaled to the gap's exact size between ticks. With no running clock (reduce motion, a frozen clock) it is built at every whole dp, as before. At 40 and 74 dp the scale is 1 and no layer is added, so the page's frames draw as before. The 4 dp least height is gone.
- **Reduce motion.** Every move is at once: let go short, let go past the line, done, back.

After the fix the strips show the list's top move every frame and land at rest. Let go short: 27.5, 23.5, 20, 17, 14.5 … 2.5, 2. Release past the line: 97.5, 94, 91.5 … 76. Done: 76, 72.5, 69.5 … 54. Back: 54, 46, 39, 33 … 2.

The pill's word still draws one frame before its body when it first appears. A new body's first frame is built off the frame by design (B-75, B-82), for every body of the kit; it is not a positional jump, and it is left as it is here.

**Numbers designed here (D15).**
- 300 ms, eased out (`1 − (1 − p)³`), for every move between the phases' gaps; it was 200 ms, stepped, for the way back only.
- The tongue: nothing at 16 dp, the whole short tongue at 40.
- The pill rides up with the content after its second, clipped at the tear line.
- The pill's second stays 1000 ms, now on the screen clock.
- Unchanged: half the distance dragged, up to 120 dp.

**Public API.** Unchanged.

**Tests.** `PullToRefreshMotionTest`:
- **Let go short.** Over every frame of the way back the list's top steps no more than the curve's steepest frame (`3 · distance / 300 ms · 16 ms`) plus a pixel of rounding, passes through a frame between, and lands at 0.
- **Past the line, done, back.** The same per frame through the release to 74, the end to 52, the pill's second (1000 ± 32 ms) and the way back to 0.
- **The pill rides.** It is still shown on the way back, and gone once back.
- **Reduce motion.** Every move is at once.
- **The glide over every millisecond.** No step over the curve's slope, across a change of target half way.
- **The easing.** Seven eighths of the way at half time; the last frame moves under 0.05 dp.
- **The tongue's size.** 0 × 0 at 16 dp, and no step of its width or height over 0.01 dp anywhere to 120.

`PullToRefreshBehaviourTest` waits out the 300 ms glide where it read the phases' gaps; its assertions are unchanged.

**Mutants**, each killed by its aimed test, read from the JUnit XML:
- the glide stepped at 83 ms;
- the glide linear;
- a retarget from the old target instead of from where it stands;
- reduce motion not instant;
- the release jumping to 74;
- the end jumping to 52;
- the tongue popping in at `gap / 40`;
- the pill giving way at once after its second;
- the pill standing half a second.
