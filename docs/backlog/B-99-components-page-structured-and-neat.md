---
id: B-99
title: "The Components page is to be structured and neat"
status: done
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

## Findings, 2026-11-16

- **Before.** One column of 24 sections, each a title over whatever its component needed, with its own widths and
  spacing. Several components were grouped under one title. No section named its functions or stage. States had no
  captions, and the Russian page said «Disabled».
- **What the page is now** (`ComponentsPage.kt`, the catalogue `SpecCatalogue.kt`).
  - An index of the groups: Components (21), v1 (7), later (5), Foundations (5). Each chip scrolls its group's title to
    the window's top.
  - One section per component, in the design's groups: the handoff's «Компоненты» table, a section per row. The
    Foundations group adds the theme, icons, patterns, the Tape and the keyframe loss.
  - Every section has the same head: name, stage badge, purpose, and the public functions it shows. Beside it, on a page
    900 dp or wider, labelled rows: VARIANTS, STATES, TRY IT. Each specimen has its caption under it.
  - The states are the design's table's. A pressed or focused specimen is held through its interaction source, so the
    pressed boil runs.
  - An overlay (dialog, drawer, sheet, pull to refresh, keyframe loss) stands in a stage with a clock of its own (B-90).
    The stage also keeps the scroll request of its overlay's first-frame focus to itself, so the page opens at its top.
  - Tooltip and menu show no static state. Their plates are popups, and an always-open popup over a scrolling page
    would take presses (plakat-ui B-72). They have a live specimen with a hint.
- **Numbers the design does not state (D15).**
  - Section: padding 28 dp, 24 dp between sections.
  - Head and rows: the head 260 dp wide, 40 dp from the rows, 28 dp between rows.
  - Specimens: 32 dp between them; the caption 26 dp under, clear of the drips.
  - Widths: field 280, card 290, bar 360, stage 360, table 520, icon cell 72 dp.
  - Type: names in Handjet 28, group titles in Handjet 40.
- **Contrast.** The group line read 4.49:1 on the page's ground in «Копия» in the secondary ink, and now uses the
  text's own ink.
- **Held by** `ComponentsPageTest`:
  - every section's head and labelled rows;
  - each section's state captions against the design's table;
  - every public function of the kit named by a section, and every name a section shows existing in the kit;
  - the page opening at its top;
  - the index scrolling a group to the top;
  - the page's own words at 4.5:1 in both palettes. This one is checked by the role's tokens, not by pixels; the
    specimens' words are held by the kit's own tests.
- **Mutants.** Each of four fails its test:
  - the stage's scroll guard removed;
  - the index's plain `bringIntoView`;
  - the pressed button removed;
  - the functions removed from the head.

  The contrast test reads the tokens a role uses: putting the group line back in the secondary ink would not fail
  it. Code reading holds that choice.
- **The owner looks at it in the window** after the merge; a remark from that is a new item.

