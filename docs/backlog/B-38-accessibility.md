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

## The band's borders, 2026-11-18

Since [B-136](B-136-the-tape-band-on-every-frame.md) the Tape's band moves on every frame and passes every row, so for a
few frames of each cycle one of its 1 dp borders crosses a disabled word's box. The measure's ground, the commonest
colour of the box, is then the border, and «Эфир»'s disabled button reads 3.89. The owner, 2026-11-18: «Край
отдельно» — those frames are measured and printed apart, without the floor; every other frame of the cycle is held at
4.3.

- `DisabledContrastTest` moved from the sample to `mutant-core`, where it drives the screen's clock and knows the
  band's place (`tapeBandTop`) on each frame, and it now measures every frame of a 7 s cycle instead of the 56 ticks.
- **The 4.38 above was itself read under a border.** With the band stepped again (`steppedMotion`) and the same split,
  that tick has the band's top border at 21 dp inside the button's word box (15–34); off the borders the stepped band's
  worst is 5.09.
- Now, per frame: off the borders «Эфир» 4.83 (the button, under the band's body) to 6.33, «Копия» 4.43 to 4.52
  (12.22 the button); under a border «Эфир» 3.89 (the button) and «Копия» 4.30.
- Mutants, each failing the test: the disabled text darker (`8A90A6` → `6E7488`, 3.91); the split dropped (its guard:
  no frame under a border); the bottom border not counted (3.89).
