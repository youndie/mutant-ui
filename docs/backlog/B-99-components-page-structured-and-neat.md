---
id: B-99
title: "The Components page is to be structured and neat"
status: wip
priority: P2
size: L
stage: stage-6-later
---

# B-99 — The Components page is to be structured and neat

The owner, 2026-11-16, on [B-96](B-96-a-richer-demo.md): «когда я говорил про демо побогаче я имел в виду компоненты;
надо либо переработать либо доработать компоненты, чтобы было структурно и опрятно». Then: «у нас кит состоит из
компонентов и демо — вот улучшать надо Components».

The sample has two pages, Components and Demo. B-96 made the Demo rich. This item is the Components page
(`ComponentsPage.kt`). It is now a single column of 23 sections, each a title over whatever the component needed,
with spacing and widths of its own per section.

- **The decision and its reason.** Designed here (D15), in the brief's world.
  - A fixed structure per section: the component's name and a one-line purpose; then its variants side by side;
    then its states (rest, pressed, focused, disabled, error where it has one) in a labelled row; then a live
    example to play with.
  - The same grid, widths and spacing in every section; sections grouped as the design's pages group them (MVP, v1,
    later), with an index to jump to a group.
  - Nothing the kit offers is left out: every public component and every state it draws.
  - The owner looks at it in the window; a remark from that is a new item.
- AC:
  - every section follows the one structure, and a test holds it (name, variants, states, example present per
    section);
  - every public component and each of its states appears (the coverage test of B-96, extended to states);
  - both themes and both languages draw; the accessibility test over the page passes.
- Anchors: `sample/src/commonMain/kotlin/io/github/youndie/mutant/sample/ComponentsPage.kt`
