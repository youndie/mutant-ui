---
id: B-30
title: "Slider: a 14 dp channel, an 8 dp fill and a 34 dp handle with the value inside"
status: question
priority: P2
size: M
stage: stage-4-v1
epic: feature-components
blocked_by: [B-10, B-11, B-43]
---

# B-30 — Slider: a 14 dp channel, an 8 dp fill and a 34 dp handle with the value inside

v1. Handle body `fab` (fill = OSD, r 17, `over` 12) with the value inside; channel 14 dp, fill 8 dp. States: rest, dragging (boil), disabled. Dragging works with a mouse as well as touch.

- AC: every state the page draws, in both themes, within the parity floor of [B-05](B-05-pixel-references-and-floor.md); the body's frames at the engine's timestamps equal the generator goldens; reduce motion is the pause (D7); the touch target is at least 48 dp by the slot; a mutant in its geometry or motion is caught; a constraint sweep (no width, long text, font scale 2) neither throws nor clips the content.
- Anchors: `reference/design-system/mutant-ui Компоненты v1.dc.html`

## Findings (2026-11-15)

**Built.** `MutantSlider(value: Int, onValueChange, …, valueRange = -10..10)`: the page's header (label and range in OSD type), five ticks, the channel with its inset ring, the fill, and the handle, an amber `fab` body of 34 dp with the value inside, signed as the page signs it (`+3`, `−4`). The value is a whole number: the page writes one into the handle, and a fraction would not fit in a drop of 34 dp.

**The page, as data.** The handoff says the handle's fill is OSD; the page draws `data-fill="amber"`, and the engine draws the page. The page's disabled handle sets only `data-r="17"`, so it keeps the fab's drips of 24, its marks and its mosaic, and the kit follows it.

**Designed here (D15).** The design draws a handle that follows the pointer from where it was taken; everything else is chosen here:
- a press anywhere on the 60 dp row takes the handle there by its middle, so the row is the touch target;
- the handle follows the pointer while held and settles on the value's place when let go, at most half a step away;
- the arrows step the value one at a time, Home and End go to the ends; a reader gets the range, the value and `setProgress`;
- with no width (a scrolling row) the slider takes the page's 300 dp; a range of one value, or a reversed one, draws the handle at the start and takes no input;
- a value that does not fit with 4 dp of room on each side widens the handle (font scale 2); at font scale 1 every value of −10…+10 fits and the handle is the page's 34 dp.

**Parity** (`SliderParityTest`, references `MutantSlider_*`, the first of the v1 page). Without its words, each slider is held whole to the body's floor of 5 px: 0, 3, 1 px, and 16 in the disabled one in «Копия», where the print theme's dots are clipped at the round handle's edge (single pixels up to 50 levels, as the disabled icon button's), kept as a measured residual. The words are held by where their ink lies, every side within a pixel and the top row exactly, and by their colour: every word lies exactly, colours within 5.

Two causes were found and removed on the way:
- **Ticks.** Chrome lays the five ticks out with `space-between` at 82.5 and 215.5 dp and paints each box snapped to a whole pixel. The kit snaps them the same way.
- **Words at fractions.** Compose places a text's box at a whole pixel and rounds its width up. Chrome's glyphs start at a fraction, both when centred in the handle and when pushed to the end of the header. Before the fix, the range lay a pixel left, and the stem of «4» lay whole in one column where the page splits it across two (the page's darkest pixel was 23 levels lighter). The slider draws each line shifted by what the rounding took.

**Tests.** `SliderBehaviourTest`:
- a mouse drag by where the handle was taken;
- a finger drag, stopping at the end;
- a press on the channel;
- the arrows, Home and End;
- the reader's range, value and `setProgress`;
- disabled takes no input;
- reduce motion is the pause;
- the handle boils while held: 11 or more changes a second at the hot tick, against 9 or fewer at rest;
- keyboard focus shows;
- no width, and a range of one value;
- nothing is cut at font scale 2.

The slider is also in the QA subjects (density 2, font scale 2) and in the keyboard sweep, where it does not activate on Enter.

**Mutants.** Killed, each by its aimed test:
- ticks unsnapped;
- the words not shifted;
- the press taking the handle by its edge;
- an arrow stepping by 2;
- no press while held;
- the handle not widening;
- half the page's width with none given.

Survived as equivalent: the fill's end moved by 2 dp. It always ends under the handle, which covers it from 0.5 to 33.5 dp across at the fill's rows.

**Found on the way.** Compose's `TextLayoutResult.didOverflowWidth` compares the box with the paragraph, which is as wide as the constraint, so a short line in a wide box reads as overflowing. The slider's test compares the text's own width with its box instead.

## Question (2026-11-15): the disabled value on the macOS 26 runner

PR #18 is green on this Mac (macOS 27) and red on the runner (`macos-latest`, macOS 26), on `SliderParityTest`'s two disabled cells only. The runner's images (now uploaded with the test reports) show why. There the fixture draws the disabled handle's value `−4` **without antialiasing**: every pixel of the glyphs is either the ink itself (`#060606`-ish) or the body behind it, with nothing in between. On this Mac the same frame is smoothed and lies on the page's pixels. The words in the header (B-74's one row higher there, allowed) and `+3` on the amber handle stay smooth on the runner too.

Not established: whether the trigger is the glyphs (`−4` against `+3`) or the disabled body under them. Nor whether the kit outside a fixture (platform text settings, not the fixture's `ChromeText`) does the same on macOS 26. If it does, a consumer on macOS 26 sees jagged digits there.

Choices, for the owner:
1. **Find the trigger on the runner first.** A scratch fixture pushed through CI draws `−4` and `+3` over each body, with the fixture's settings and the platform's. About two CI runs. If the kit itself draws jagged text on macOS 26, that is a defect of its own.
2. **Allow it on macOS 26 for the disabled value only**, as B-74 allows its row, and file the investigation as an item. The value's place and colour stay exact on this Mac.
3. **Leave the merge** until the runner matches this Mac (a newer runner image).
