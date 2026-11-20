---
id: B-148
title: "The sample for a consumer, light on a phone"
status: wip
priority: P1
size: M
stage: stage-6-later
blocked_by: []
---

# B-148 — The sample for a consumer, light on a phone

The owner, 2026-11-20, a new round: «Demo kit (названия категорий MVP, later) знать никчему потребителю»;
«Производительность (страничка components тяжёлая для мобилы и 3 экрана demo за раз не удобно)».

What the sample does today (`main` at `66d9c57`):
- the Components page groups its 38 sections by the design's stages, `SpecStage.Mvp`, `V1`, `Later` and `Base`, with
  titles «Компоненты», «Компоненты v1», «Компоненты позже» and a stage badge (`MVP`, `V1`, `LATER`) in every section's
  head (`ComponentsPage.kt`, `SampleStrings.kt`). How the designer's pages were cut is nothing to a consumer;
- the page is one `Column` of every section at once, every specimen live: all of them composed, laid out and drawn,
  the ones off the window too. B-138 and B-140 measured this page as the heaviest of the sample on a phone;
- the Demo page lays its three screens out side by side in a `FlowRow` (`DemoPage.kt`).

- **The decision.**
  - The sections are grouped by what a consumer looks for, as the README groups them: **Foundations** (theme, icons,
    patterns, the Tape, the keyframe loss), **Actions**, **Selection and input**, **Containment**, **Navigation**,
    **Communication**, **Overlays**. No design stage is named anywhere in the sample.
  - The page shows **one group at a time**: the index's chips choose it, and only its sections are composed. The
    first group, Foundations, is the default.
  - The Demo page shows **one screen at a time**, a row of chips choosing Library, Recording or Settings.
- **Rejected:** a lazy column of all 38 sections (the names stay the designer's, and a fast fling still composes most
  of them); keeping the stage as a small badge (it says nothing to a consumer).
- AC:
  - no `SpecStage` name, «MVP», «v1», «позже» or «later» left in what the sample shows, in either language; a test
    walks every group's chip and finds every one of the 38 sections in exactly one group;
  - the Components page composes only the chosen group's sections (a test counts them), the Demo page one screen;
  - the sample's existing tests green, with what they assert about the old grouping moved to the new one; the benches
    (`FrameBench`, `IosBench`, `frame-bench-browser.mjs`) still find their scenes, each scene's meaning written down
    where it changed;
  - the browser bench in the phone proxy (`PHONE=1`, the CPU slowed as B-116 calibrated it), `ComponentsRest`,
    before against after, with its log: the cost of the default view, not a phone's number (the phone is not
    available, the owner, 2026-11-20).
- Anchors: `sample/src/commonMain/kotlin/io/github/youndie/mutant/sample/ComponentsPage.kt`,
  `sample/src/commonMain/kotlin/io/github/youndie/mutant/sample/SpecCatalogue.kt`,
  `sample/src/commonMain/kotlin/io/github/youndie/mutant/sample/DemoPage.kt`
