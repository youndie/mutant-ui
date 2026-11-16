---
id: B-98
title: "The keyframe loss crashes the window when a popup is open over it"
status: done
priority: P0
size: S
stage: stage-6-later
---

# B-98 — The keyframe loss crashes the window when a popup is open over it

Found on 2026-11-16, right after [B-96](B-96-a-richer-demo.md) merged. The desktop sample died on the AWT thread with
`IllegalArgumentException: layouts are not part of the same hierarchy`, thrown at `MoshScene.cutIn`
(`MutantKeyframeLoss.kt`).

B-96 put the first popups (the FAB's and the menu's tooltips, the dropdown, the sort menu) inside the demo phone the
sample gives the keyframe loss. A `Popup` is a layout root of its own, but it reads `LocalMutantMoshScene` through the
composition like anything under the loss. So its bodies registered with the scene, and the scene asked
`localPositionOf` across the two roots as a loss ran.

- **The decision and its reason.** The scene draws, cuts and clears only bodies in its own layout hierarchy. A body in
  another root is skipped by `offsetOf`, so it draws as it is, uncut. The loss covers the screen, not what floats over
  it.
  - Rejected: a `null` scene provided inside every kit popup. That fixes the kit's popups and leaves a consumer's own
    `Popup` with a body to crash the same way.
- AC:
  - a menu open over a screen under a running loss draws on every frame of the loss, and nothing throws;
  - the test fails with the fix reverted.
- Anchors: `mutant-core/src/commonMain/kotlin/io/github/youndie/mutant/surface/MutantKeyframeLoss.kt`

## Findings

- **Held by** `KeyframeLossPopupTest`, which draws every root over the first loss of a screen with a menu open. It is
  red without the guard, with the same exception the window threw.
