---
id: B-202
title: "The data table at a large font and in a narrow slot"
status: wip
priority: P2
size: M
stage: stage-6-later
blocked_by: []
---

# B-202 — The data table at a large font and in a narrow slot

The data table's fixed columns keep their dp at font scale 2, and the grid has no gap («таблица02:10»); narrower than its fixed columns, it drops every name with no ellipsis (F-data-table-04, F-data-table-05). [B-168](B-168-look-and-motion-pass.md)'s designer's answers, answer 34 ([решение]).

- **The decision and its reason.** Numeric columns size to their content, the first column stretches and ellipsises, and when the table still does not fit it scrolls sideways with its header moving with the rows.

- AC:
  - at font scale 2 no two cells' ink touch, and the first column's names end in an ellipsis
  - a table narrower than its numeric columns scrolls sideways, the header in register with the rows
  - at the design's size and font scale 1 its parity cells and goldens are unchanged
  - each new test fails on the code before and kills a mutant after the commit; goldens and parity cells at rest unchanged, or the moved ones listed with this item and looked at
- Anchors: `mutant-core/src/commonMain/kotlin/io/github/youndie/mutant/components/MutantDataTable.kt`
