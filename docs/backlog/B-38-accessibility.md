---
id: B-38
title: "Accessibility: contrast after the Tape, flashes, greyscale, semantics"
status: open
priority: P1
size: M
stage: stage-3-screens
blocked_by: [B-13, B-37]
---

# B-38 — Accessibility: contrast after the Tape, flashes, greyscale, semantics

The brief asks for 4.5:1 for glyphs after the Tape at the worst tick, its own flash rule (luminance change over a quarter of the screen at most once a second, red never flashes, grain no larger than 1.5 dp), states distinguishable in greyscale and Material's semantics with no damage in them. «Копия» disabled text is 4.4:1 under the Tape (research §1.5, R2, R4).

- **The decision and its reason.** Measure on the kit, not on the tokens: a contrast sweep over the demo screens at every tick of a Tape cycle; a recording of the demo screens run through the flash rule; a greyscale render of every state; a semantics test that no body adds a node. The 4.4 pair goes to the owner with WCAG's exemption for inactive components as one option.

- Input from [B-13](B-13-tape.md) (research §1.12): under the band's body the kit keeps the design's figures; the band's 1 dp edge and the head's noise, which the figures leave out, read 1.3–2.6:1 in «Эфир».

- AC: a table of measured contrasts, flashes and greyscale differences in the research; every component's semantics equal its Material counterpart's; the owner's answer on the disabled pair recorded.
- Anchors: `reference/design-system/mutant-tokens.json`, `reference/design-system/brief.md`, `reference/design-system/mutant-ui Демо.dc.html`
