---
id: B-54
title: "Melt: a mark can stall at 76 % when a frame falls in its last 3 ms"
status: open
priority: P1
size: S
stage: stage-2-components
epic: feature-components
blocked_by: [B-18]
---

# B-54 — Melt: a mark can stall at 76 % when a frame falls in its last 3 ms

Found in B-26, where the snackbar had the same defect and its test caught it. `Modifier.mutantMelt` asks for frames while `elapsed < MELT_IN_MS` (330 ms), but its progress is stepped at 83.33 ms and reaches 1 only at 333.3 ms. A composition at an elapsed time in [330, 333.3) computes 250 / 330 = 0.76 and asks for no further frame, so the checkbox's mark and the radio's dot stand three quarters melted until something else recomposes them. With frames 16 ms apart, about one melt in five meets that window. The snackbar now asks while its progress is under 1 (`fix(snackbar)` in B-26), as the tabs' bar and the surface's pour and slide already do.

- AC: the melt asks for frames while its stepped progress is under 1; a test on a hand-driven clock that lands a frame at 331 ms of a melt and then advances fails without the fix and sees the mark land.
- Anchors: `mutant-core/src/commonMain/kotlin/io/github/youndie/mutant/surface/BodyTransitions.kt`
