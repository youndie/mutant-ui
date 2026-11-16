---
id: B-100
title: "Movement runs on every frame by default; the boil keeps its tick"
status: wip
priority: P2
size: M
stage: stage-6-later
---

# B-100 — Movement runs on every frame by default; the boil keeps its tick

The owner, 2026-11-16: «"кипение" и капание жижи у нас ограничено fps, но перемещение, venom и т. д., возможно, не
должны быть ограничены (может сделать флаг конфигурации и по умолчанию перемещение не ограничивать)».

What steps now:
- transitions — pour, slide, the rise and fall of a dialog, a sheet, a drawer and a snackbar, the tabs' and the date
  picker's runs — through `stepped()`: 83.33 ms steps, 12 a second (`BodyTransitions.kt`);
- the venom run: 41.67 ms steps, 24 a second (`VenomRun.kt`).

The boil, the drips, the mosaic and the tape tick at 8 a second at rest and 12 hot (D6), and stay so.

- **The decision and its reason.** A theme flag. By default movement runs on every frame of the screen clock; set, it
  steps as the design draws it.
  - Movement means where a body is and how far a transition has gone.
  - What a body looks like stays on its tick: its shape is rebuilt once per tick (D6), and the movement carries it
    between rebuilds.
  - The decision is recorded as research D21, a departure of behaviour, not of cells: the spec frames hold the stepped
    flag.
- AC:
  - with the default, every listed transition advances on every frame (a frame-by-frame test per kind), and the boil
    still changes only on its tick;
  - with the flag set, the old steps hold, and the existing goldens and frame tests pass unchanged;
  - reduce motion still stops everything (D7).
- Anchors: `mutant-core/src/commonMain/kotlin/io/github/youndie/mutant/surface/BodyTransitions.kt`,
  `mutant-core/src/commonMain/kotlin/io/github/youndie/mutant/components/VenomRun.kt`
