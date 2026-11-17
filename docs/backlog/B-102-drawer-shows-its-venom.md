---
id: B-102
title: "The drawer closes before its venom run is seen"
status: done
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

## Findings, 2026-11-17

- **Seen before.** In a frame strip of a pick that closes the drawer, every 32 ms, the drawer slid out in 200 ms and
  was gone by 224 ms. The venom run lasts 720 ms (`MutantMotion.VENOM_MS`) and had only begun.
- **Decision.**
  - A pick that closes the drawer holds it on screen until the run has landed on the new row and lingered 160 ms,
    then the drawer plays its 200 ms exit: 1.08 s from the pick to gone.
  - While it stands, the drawer takes no input: the scrim, Escape and back are already closed.
  - A dismissal with no pick (the scrim, Escape, back) goes on its frame. Under reduce motion everything is at once.
  - A pick counts only on a drawer that was on screen at the last composition, so a selection changed elsewhere while
    the drawer is gone does not bring it back.
- **The entrance and the exit, reviewed.** The entrance slides in over 330 ms, eased out by the square, with the
  scrim coming in. The exit sinks in 200 ms. Since B-100 both move on every frame, and the strip shows no cut. They
  stay as they are.
- **Numbers the design does not state (D15).** The linger, 160 ms.
- **Held by** `DrawerPickHoldTest`:
  - the drawer stands 680 ms after a pick and is gone a second later;
  - the scrim dismisses in 300 ms;
  - under reduce motion a pick closes it at once;
  - a selection changed while it is gone leaves it gone.

  The demo's navigation test now waits the whole close.
- **Mutants**, each failing its case:
  - no hold;
  - a pick counted on a hidden drawer;
  - a hold under reduce motion.

