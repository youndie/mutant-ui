---
id: B-213
title: "The Demo's phone alone in the browser, for a page to embed"
status: wip
priority: P2
size: S
stage: stage-6-later
blocked_by: []
---

# B-213 — The Demo's phone alone in the browser, for a page to embed

The owner, in chat on 2026-11-25: «Добавляй на kotlin.website кит». The site's `/ui` section shows each kit's live demo as the kit's own Pages in an iframe at a phone's size, 390 × 844, as it does for toon-ui and plakat-ui (their `?scene=app`, toon-ui#27 and plakat-ui#18). mutant-ui's browser sample opens either the whole sheet, its header and pages around 360 × 720 phones, or one of B-73's bench scenes by `?scene=<name>`; neither fills a phone.

- **The decision and its reason.** As the sister kits do: `?scene=app` opens the Demo's phone screens alone, the screen filling the viewport under one small row of chips that picks the screen (Library, Recording, Settings under a dialog) and the palette («Эфир», «Копия»). Nothing else of the sample's sheet is shown. The screen is the Demo page's phone as it is (the app, its clock, its Tape, the Library's keyframe loss), only without the 360 × 720 frame, the ring and the rounded corners: the embedding page draws the phone around it. Reduce motion is off whatever the browser says, as the sample's header defaults it, so the Tape and the bodies run in the embed.
- **Rejected:** the sample's header switches in the embed (reduce motion, «Чистый сигнал», steps, the language), which are the sheet's, not the app's; the 360 × 720 phone centred in a 390 × 844 viewport, which leaves a band of page round it inside the site's own phone.

- AC:
  - the scene shows each Demo screen and each palette
  - the screen fills a 390 × 844 viewport
  - the Tape and the bodies run
  - reduce motion is off
  - a test holds what can be held on desktop: the scene is a `@Composable` shared through commonMain, and a sample test renders it at 390 × 844 in both palettes
  - the bench scenes are unchanged
- Anchors: `sample/src/wasmJsMain/kotlin/io/github/youndie/mutant/sample/Main.kt`, `sample/src/commonMain/kotlin/io/github/youndie/mutant/sample/DemoPage.kt`
