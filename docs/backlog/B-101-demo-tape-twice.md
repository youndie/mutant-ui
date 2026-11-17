---
id: B-101
title: "On the Demo page the phones carry the Tape twice"
status: done
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

## Findings, 2026-11-17

- **Measured before.** The Recording phone on the Demo page was compared with the same phone drawn on its own, at
  every other scene tick of a whole Tape cycle (28 instants over 7 s). On the page, 2 025 287 pixels differed:
  70 000 to 76 000 an instant, 28 % of the phone.
- **The fix.** The sample's root draws its Tape only on the Components page.
- **After.** No instant differs in more than 0.2 % of the phone. What is left are a body's edge pixels where its
  image is cut to the band of the window it stands in, and that band moves with where the phone stands (B-86). The
  phone alone stands on the page's ground, so the rounded corners match.
- **A test that could not see it.** Cutting the phone out of the whole window and comparing it with the phone's own
  capture found nothing either way. In the desktop test a node's capture is the scene cropped to it, parent's Tape
  included, so the comparison has to be between two compositions.
- **Held by** `DemoTapeOnceTest`. Putting the page's Tape back on the Demo page fails it, at 70 000 to 76 000 pixels
  an instant.

