---
id: B-59
title: "Dialog: its actions stay reachable under long text and long labels"
status: open
priority: P2
size: M
stage: stage-5-product
blocked_by: [B-40]
---

# B-59 — Dialog: its actions stay reachable under long text and long labels

Found in [B-40](B-40-qa-pass-and-real-window.md).

| Finding | What happens |
|---|---|
| F06 | Title and text taller than the screen: the actions' row gets the remaining height, 0, and both actions are 0 × 0, unreachable by mouse and keys. Same in a 120 × 120 dp window |
| F07 | A long first action label takes the row, and the primary action after it gets 0 width |

- **The decision.** The text scrolls inside the sheet when it does not fit, and is clipped only then (a clip moves glyph edges, so parity stays as it is); the actions are measured first and keep their size. In the actions' row each label ellipsizes within a share of the row, so the primary action always keeps its label's minimum. The rejected alternative is capping the text's length in the API.
- AC: `LongContentSweepTest.F06` and `.F07` pass without their `@Ignore`; the dialog's parity cells and goldens unchanged.
- Anchors: `mutant-core/src/commonMain/kotlin/io/github/youndie/mutant/components/MutantDialog.kt`, `mutant-core/src/desktopTest/kotlin/io/github/youndie/mutant/qa/LongContentSweepTest.kt`
