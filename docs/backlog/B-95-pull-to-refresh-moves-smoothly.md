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

- **The decision and its reason.** To be decided when taken, after a frame strip of each phase. The likely shape: the gap, the content's offset and the hand-overs move per frame on closed-form eased curves (cmp-ui-component §2), with no jump between phases. The slime keeps its own tick (D6). Each number the design does not state is listed in the item (D15).
- AC:
  - Over every millisecond of a pull, a short release, a refresh and its end, the content's offset steps by no more than a frame's worth of its curve. A continuity test holds it.
  - Under reduce motion every move is at once.
  - Frame strips before and after are attached to the PR.
- Anchors: `mutant-core/src/commonMain/kotlin/io/github/youndie/mutant/components/MutantPullToRefresh.kt`
