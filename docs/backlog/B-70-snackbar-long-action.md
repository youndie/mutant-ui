---
id: B-70
title: "Snackbar: a long action pushes the message out"
status: done
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

## Findings (2026-11-15)

**Fixed.** The snackbar's action is measured before the message, which takes the rest by weight, and is now capped at half the bar (`ACTION_SHARE`). A longer action ends in an ellipsis. A short action (`ОТМЕНА`) takes its own width as before, and its target keeps the bar's height and 48 dp at least. Under an unbounded width the action is not capped.

**Tests.**
- `LongContentSweepTest.F21` passes without its `@Ignore`: next to a 300-character action the message keeps over 40 dp, and the action ends inside the bar.
- The snackbar's parity, its behaviour tests and the QA sweeps are unchanged.

**Mutant.** The action allowed the whole bar is killed by F21.
