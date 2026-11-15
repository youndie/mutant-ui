---
id: B-70
title: "Snackbar: a long action pushes the message out"
status: wip
priority: P3
size: S
stage: stage-5-product
blocked_by: [B-40]
---

# B-70 — Snackbar: a long action pushes the message out

Found in [B-40](B-40-qa-pass-and-real-window.md) (F21). The action is measured first with `softWrap = false` and no ellipsis; a 300-character action takes the bar and the message drops to 0 × 0. An 18-character action still leaves the message room.

- **The decision.** The action ellipsizes within a share of the bar; the message keeps the rest. The rejected alternative is a length limit in the API.
- AC: `LongContentSweepTest.F21` passes without its `@Ignore`.
- Anchors: `mutant-core/src/commonMain/kotlin/io/github/youndie/mutant/components/MutantSnackbar.kt`, `mutant-core/src/desktopTest/kotlin/io/github/youndie/mutant/qa/LongContentSweepTest.kt`
