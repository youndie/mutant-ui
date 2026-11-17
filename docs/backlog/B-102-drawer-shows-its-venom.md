---
id: B-102
title: "The drawer closes before its venom run is seen"
status: wip
priority: P2
size: M
stage: stage-6-later
---

# B-102 — The drawer closes before its venom run is seen

The owner, 2026-11-16: «переключение элемента в drawer сразу закрывает его, и мы не видим анимацию — предлагаю
концептуально пересмотреть появление и пропадание drawer и убирать его чуть с задержкой, чтобы мы видели venom
animation».

A consumer closes the drawer in `onSelect`, as the demo does (B-96). The drawer starts its exit on the same frame,
and the venom run from the old row to the new one is never seen.

- **The decision and its reason.** To be taken after a frame strip of the drawer's entrance, a pick and its exit.
  - The direction the owner gave: the kit holds the drawer open after a pick until the venom run has landed on the new
    row, then plays its exit. `visible = false` during a run is honoured when the run ends, and a scrim click or
    Escape closes at once.
  - The entrance and the exit themselves are reviewed in the same strip.
  - Every number the design does not state is listed here (D15).
- AC:
  - after a pick that closes the drawer, the frames show the run reaching the new row before the drawer moves;
  - a dismissal by the scrim or Escape starts the exit on its frame;
  - under reduce motion the drawer goes at once.
- Anchors: `mutant-core/src/commonMain/kotlin/io/github/youndie/mutant/components/MutantNavigationDrawer.kt`
