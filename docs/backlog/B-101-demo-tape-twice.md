---
id: B-101
title: "On the Demo page the phones carry the Tape twice"
status: open
priority: P2
size: S
stage: stage-6-later
---

# B-101 — On the Demo page the phones carry the Tape twice

The owner, 2026-11-16: «когда заходим в demo у нас глобальные помехи и помехи на каждом смартфоне — накладываются».

The sample's root draws `mutantTape()` over the whole page (`MutantUiSample.kt`), and each demo phone draws its own
(`DemoPage.kt`, `ScreenBox`). Over a phone the two noises sum.

- **The decision and its reason.** On the Demo page only the phones carry the Tape: a phone is a screen, and the Tape
  is a screen's. The Components page keeps the root's.
- AC:
  - over a phone on the Demo page exactly one Tape draws: a pixel test against the phone alone;
  - the Components page is unchanged.
- Anchors: `sample/src/commonMain/kotlin/io/github/youndie/mutant/sample/MutantUiSample.kt`
