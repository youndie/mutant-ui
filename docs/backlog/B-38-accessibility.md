---
id: B-38
title: "Accessibility: contrast after the Tape, flashes, greyscale, semantics"
status: done
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

## Result (2026-11-14)

- Measured on the kit over the sample's demo screens at every scene tick of a Tape cycle, both themes; the table is research §1.16. Every active word reads 4.70:1 or more after the Tape at its worst tick (the brief asks 4.5); no tick changes more than 7.3 % of a screen (the kit's rule: a quarter, once a second); the settings form's red holds 559–653 px tick by tick, moving with its bodies, never on and off.
- **The owner's answer on the disabled pair, 2026-11-14: WCAG's exemption for inactive components.** Measured 4.38 («Эфир»'s disabled button) to 4.52 («Копия»'s disabled labels); the tokens stay; the words are held from falling under 4.3.
- Greyscale: every state pair of the button, checkbox, radio, switch, chip and field stands at least 86 pixels apart in grey, but a loading button against one at rest (8 and 30 px): its seek bands are a light acid on the acid body. Listed with that cause.
- Semantics: each component's role and state are its Material counterpart's (the chip a selectable with the checkbox's role, as Material's FilterChip); a body adds no node.
- Noted, the design's own: «Копия»'s outlined button casts a misregistered shadow in a saturated red that comes and goes (2–157 px, under 0.1 % of the screen); the Tape's grain strokes are 1 dp thin and up to 3 dp long.
- Tests (the sample): `AccessibilityTest`, `DisabledContrastTest`, `StatesAndSemanticsTest` (2). Mutants, each failing its test: «Эфир»'s secondary text darker, «Копия»'s lighter, the disabled text darker, the switch's role a checkbox's, the progress without its value.
- Not covered: a device's recording through the flash rule (the Pixel 6a, B-49), and screen readers on a device.
