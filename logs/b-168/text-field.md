# B-168 — text field: `MutantTextField`

Group 5 of [B-168](../../docs/backlog/B-168-look-and-motion-pass.md), on `fix/b-168-text-field` from `22a6d74`,
2026-11-22. Run on this Mac (M-series) on the desktop JVM, in `runComposeUiTest` with the clock driven by hand.
The search bar shares `TextFieldBody` but is group 6's and was not looked at here.

## What was looked at

Two throwaway tests, deleted before committing. The frames stay in `/private/tmp/b168-text-field/` (not in the
repository).

- **The components as the sample composes them** (`core/`), each in eight looks: «Эфир», «Копия», reduce motion,
  «Чистый сигнал», `steppedMotion` and density 2. The last two looks are the browser's way of drawing in both palettes
  (`LocalMutantFirstFrameWaits` false, `LocalMutantPoolBodies` true, `LocalMutantBodyOnGpu` true,
  `LocalMutantBodyAsImage` false, as `FillChangeOnGpuTest`). A sheet every 48 ms, and a per-frame pixel diff
  (`<name>.diff.txt`).
  - `states-`: the section's four cells (rest with prefix and placeholder, held focus, error, disabled) for 2 s.
  - `typing-`: a mouse click on the rest field, then «Night to Tue» typed a letter every 64 ms. Then three Backspaces,
    then focus cleared.
  - `long-`: a value longer than the field (280 dp), focused. Then a letter typed, Home, Right ×6, End, Left ×10, and
    focus cleared.
  - `flags-`: error on and off, disabled and enabled, focus with error, a focused error turned disabled and back.
  - `placeholder-`: an empty field focused, a letter typed and deleted, and a placeholder too long for its field.
  - Dense sheets (every frame) around focus in and out: `typing-*-dense-*.png`.
- **The sample** (`sample/`):
  - **The page's section**: `MutantUiSample(SampleSettings(group = Selection))` scrolled to the TextField section, in
    «Эфир», «Копия», reduce motion, «Чистый сигнал», `steppedMotion` and Russian.
    - 1.2 s at rest, then a mouse click into the rest cell and «Night» typed.
    - Then Tab, then a click at the end of the focused cell and a long text typed there.
  - **The demo's three uses**, in both palettes (`demo-*`):
    - the library's search field typed into until the list is empty, and cleared;
    - the settings' name field typed into and left by Tab;
    - the recording's rename sheet opened, its field clicked and a long title typed.

The mouse is used for every press, so no focus is left by a test's touch.

## Findings

### F-text-field-01 — past the end of a long value the block covers its last letters, and inside it the bar stands at the edge — local

- **What.** A value longer than the field is scrolled by `BasicTextField` itself, where the field cannot read it.
  - The caret was placed in the text's unscrolled coordinates and then clamped to the field's edge.
  - Past the end the 9 × 20 block covered the last letter, or two («…to the en█»).
  - Inside a scrolled value (Left ×10 from the end) the bar stood at the field's right edge after «end!», ten letters
    away from the insertion point.
- **Frames.**
  - `core/long-efir-0.png` (336–1104 ms), `core/long-efir-1.png` (from 2368 ms), `core/long-d2-1.png`: the block over
    the last letter.
  - `core/long-efir-2.png` (2902–3780 ms): the bar after Left ×10.
  - `sample/section-efir-17.png`, `sample/section-ru-17.png`: the section's focused cell with a long text typed.
- **The test, before the fix.**
  - «the gap between the last letter's ink (214) and the block (217)»: the page's gap is 8 dp.
  - «the bar 224 stands ten letters before the end, 217»: the bar stood past where the block had been.
- **The design and the items.** B-20's AC: «the caret stays inside the field». B-58: the caret marks the insertion
  point, the block 8 dp past the end. cmp-ui-component §9 names this face (plakat-ui B-59).

### F-text-field-02 — on the frame a letter is typed, the block stands at the previous text's end — local

- **What.** The caret read the text's layout from state that `onTextLayout` wrote during the measure. It recomposed a
  frame later, so on the first frame of every letter the block stood where the last text ended, against or over the new
  letter («Nig█»). It moved past it on the next frame.
- **Frames.** `core/typing-efir-0.png` at 1164 ms («Nig█»), as in every look.
- **The test, before the fix.** The gap between the new letter's ink and the block, on each letter's first frame:
  `[0, 3, 9, 3, 0, 9, 0, 2, 1, 1, 1, 1, 1]` px, against the page's 8 dp (B-20).

### F-text-field-03 — over an empty field's placeholder, the block covers its first letter — local

- **What.** Focused and empty, the block stood at the line's start, 0 dp gap (B-20), over the placeholder's first
  letter («█ind a recording»).
- **Frames.**
  - `core/typing-efir-0.png` and `core/typing-kopiya-0.png`, 588–636 ms;
  - `core/placeholder-efir-0.png`, 336–624 ms;
  - `sample/section-efir-5.png`, the rest cell after a click.
- **The test, before the fix.** «the bar: [65 … 73]»: 9 columns, the block.
- **The rule.** B-58 decided that where a block would cover the next letter, the caret is a 2 dp bar standing just
  before the insertion point, and may stand in the padding or the gap before the text. The placeholder's first letter
  is such a letter. The fix applies B-58's rule; it decides no new number.

### F-text-field-04 — the caret's blink is not restarted by focus or typing — reported (a look the design does not settle)

- **What.** The blink runs on the screen's scene tick, five ticks lit in eight (B-20, the engine's `fi % 8 < 5`).
  - Focus can arrive in its dark part. In `core/typing-efir-0.png` the field is clicked at 480 ms and the caret shows at
    588–636, then is dark from 684 to about 1050 ms.
  - While letters are typed, the caret blinks off between them. In `core/typing-efir-1.png` the caret is dark from
    1692 to 1980 ms while «Tue» is typed.
- **The design.** The engine has no typing, and its page's caret blinks on the page's clock. B-20 decided the blink's
  shape, not when it restarts.
- **For the owner.** Should the blink restart lit on focus and on each edit, as most editors do? The phase would then
  come from the clock's time at the last edit, not the scene tick alone.

### F-text-field-05 — the bar inside the text hides the right stem of letters that reach their advance — reported (B-58's numbers)

- **What.** The 2 dp bar stands just before the insertion point, against the letter already typed (B-58). After a
  letter whose ink reaches its advance (Rubik's «h», «n», «m», «l»), it covers that stem: «Nigh|t» reads «Nig|t».
- **Frames.** `after/sample/demo-settings-efir-0.png`, 588–828 ms: the settings' name field clicked after «Nigh».
  It is unchanged by the fixes, and the before-sheet was overwritten. Enlarged: `/private/tmp/b168-text-field/zoom1.png`.
- **For the owner.** A 1 dp bar, or a bar centred on the insertion point, would each cover less. B-58's test strings
  («abc») never met it.

### F-text-field-06 — in the browser's way of drawing, focus in and out keep the previous body for a frame — reported (`surface/`)

- **What.** In the browser's way (`LocalMutantBodyOnGpu`, the pool, no first-frame wait) the body lags focus by a frame:
  - On focus the brackets and the caret come at once, over the resting body (its full drips and mosaic). The calm body
    comes one frame later.
  - On focus out the brackets go at once, and the calm body stays one more frame.
  On the desktop's way both change in the same frame.
- **Frames.** `core/typing-gpuefir-dense-540.png` (556 ms: brackets over the resting body; 572: calm) and
  `core/typing-gpuefir-dense-3040.png` (3058: no brackets, calm body; 3074: rest). The desktop's way:
  `core/typing-efir-dense-540.png`, `-3040.png`.
- **Numbers.** Per-frame diff in `core/typing-gpuefir.diff.txt`: 3389 px at 556 and 3120 at 572 ms. The desktop's way:
  3831 at 556, and an ordinary tick after it.
- **Reach.** A focused body keeps its paths (D23), so the switch from the shader to the paths is a frame late. This is
  B-166's class, a stale look for a frame. Its face here is focus, not a fill or a press (F-icon-fab-06 is the desktop's
  press).

### F-text-field-07 — the first letter typed and the last deleted move the mosaic — reported (a look the design does not settle)

- **What.** An empty field's clean zone is the placeholder's box. A field with a value marks its whole line clean
  (`MutantTextField.kt`, «only the value marks the clean zone»). The mosaic right of the placeholder therefore goes
  with the first letter and comes back when the last is deleted.
- **Frames.** `core/placeholder-efir-0.png`, 768 → 816 ms: the blocks at the top right go when «N» is typed.
- **The design.** The engine's text zone is the union of the texts' own boxes (`textZone`), and it never types. Which
  zone a typed value should mark is the kit's to decide (D15). Following the text's own box would move the mosaic on
  every letter, and the body would be built anew at each.

### F-text-field-08 — focus in and out are a cut — reported (a look the design does not draw)

- **What.** Focus calms the body to 0.3, which shortens the drips (`over` × 0.58). On focus the body is built anew in
  one frame, and on focus out it is built back.
- **Frames.** `core/typing-efir-dense-540.png`, `core/typing-efir-2.png` at 3090 ms: the drips lengthen in one frame.
- **The design.** The engine's focus is a static attribute (`data-calm`, `data-brackets`) and draws no transition. The
  same class as the enabled ↔ disabled cut already reported (F-icon-fab-04).

### F-text-field-09 — the sample's focused specimen stays focused beside a really focused field — reported (the sample)

- **What.** The section's «focused» cell holds its focus look through `held(focus = true)`. A click into the rest cell
  therefore shows two fields with brackets and a caret, and typing goes into only one of them.
- **Frames.** `sample/section-efir-5.png`.
- **Note.** Clicking the specimen itself really focuses it, as it should. This is how the page shows states, and it is
  listed only for the owner's review of the sample.

## Looked at and found right

- **Error.** The red offset outline and the blocks come and go with the flag in one frame, with no flash. They are
  static under the clock, focused or not (`core/flags-*`).
- **Disabled.** No signal, both texts grey, no caret, and focus dropped. Back enabled, the body is the error body or the
  rest body as the flags say.
- **The error's description** is semantics only (B-67). The page's «нет сигнала на канале 99» is a stage caption, and
  the demo's settings screen draws its own line.
- **A long placeholder** ends in an ellipsis inside the field. A long value unfocused shows its end, as `BasicTextField`
  does.
- **Reduce motion.** The caret stands lit and nothing else moves. Typing and focus change only what they should
  (`core/*-reduce.diff.txt`: a single frame per event).
- **`steppedMotion` and «Чистый сигнал»** change nothing on a field. The field has no movement and no Tape of its own;
  the sample's section shows only the Tape going.
- **Density 2.** The block is 18 × 40 px, the brackets and drips scale, and there is no unit error (`core/*-d2-*`).
- **The demo's fields.** The library's search filters to the empty state and back, the settings' field takes a
  letter, and the rename sheet's field takes a long title. Each shows the faces above and nothing else.

## Fixes

After-frames are in `/private/tmp/b168-text-field/after/` (`core/` in all eight looks, the section and the settings
demo in `sample/`). The before-frames are where the findings name them.

### F-text-field-01 and F-text-field-02 — `220c848`

- **The change.** `MutantTextField.kt` (`TextFieldBody`, `valueLinePolicy`). The value's line is a `Layout` of its own:
  - it measures `BasicTextField` as wide as its text (never narrower than the line), so the field's own scroll never
    moves;
  - it scrolls the text itself, as little as keeps the whole block, or the bar's insertion point, in view;
  - it places the caret in the same measure, from the layout `onTextLayout` has just handed over.
  The text is clipped to the line, by its layer, only while it overflows. A press past the line's ends does not reach
  the hidden text. Without overflow it is placed as before, with no clip and no layer, so no parity cell moves.
- **What it chose.** The block's room past the end is kept focused or not. A long value unfocused ends 17 dp before the
  line's edge, and focus does not move the text.
- **The tests.** `TextFieldCaretPlacementTest`, read from pixels at density 1:
  - «past the end of a value longer than the field, the block stands clear of the last letter»: the gap from the last
    letter's ink to the block is at least 6 px, the block sits inside the padding, and no ink spills into the padding
    before the line;
  - «inside a value longer than the field, the bar stands at the insertion point»: after Left ×10 the bar is 2 px wide,
    at least 40 px before the end, with a letter's ink on both sides;
  - «on the frame a letter is typed, the block stands past it»: the gap on each letter's first frame, at least 6 px.
  Before the change: the gap was 2 px, the bar stood past the block, and the gaps were `[0, 3, 9, 3, 0, 9, 0, 2, 1, 1,
  1, 1, 1]`.
- **After.** `after/core/long-*.png`: the block 8 dp past «end!», and the bar walks back through «the end» under Left.
  `after/core/typing-*.png` at 1164 ms: «Nig █». `after/sample/section-efir-17.png`: the section's focused cell.
- **Unchanged.** `TextFieldParityTest`, `SearchBarParityTest`, `OverlayAndMouseTest` (F04, F05, F12 and the
  left-to-right case) and `KeyboardFocusSweepTest` pass as they were. Per-frame diffs of the eight looks keep their
  peaks, within a few per cent.

### F-text-field-03 — `5de114c`

- **The change.** Over a placeholder, the caret at the line's start is the bar (B-58's rule), standing in the gap or
  the padding before the placeholder.
- **The test.** «over a placeholder the caret is the bar, before its first letter». The caret is 2 px wide and stands
  before the placeholder's first ink, and that ink's column is the same as unfocused. Before the change: «the bar:
  [65 … 73]».
- **After.** `after/core/placeholder-*.png` and `after/core/typing-*.png`, 336–624 ms: «|Find a recording».

### Mutants

Each was run after its commit against `TextFieldCaretPlacementTest`, read from the JUnit XML, and reverted with
`git checkout`.

| Mutant | Killed by |
|---|---|
| the line never scrolls (`scroll = 0`) | «the gap … (216) and the block (217)»; «the bar 224 stands ten letters before the end, 217» |
| the caret placed from the layout of the measure before | «the gap before the block, each first frame: [0, 3, 9, 3, 0, 9, 0, 2, 1, 1, 1, 1, 1]» |
| only the insertion point kept in view, not the block's room | «the gap … (215) and the block (217)» |
| the overflowing text not clipped (`clip = false`) | «the value's ink in the padding before its line: [2, 5, 6, 7, 8, 9, 10, 11]» |
| the overflowing text placed unscrolled and unclipped | «the gap … (216) and the block (217)» |
| the block over a placeholder (`block = at == n`) | «the bar: [65 … 73] expected: <2> but was: <9>» |
| the placeholder flag inverted | «the bar: [65 … 73] expected: <2> but was: <9>» |

### Reach

`TextFieldBody` is shared with `MutantSearchBar` (group 6), so the search bar's line scrolls and places its caret the
same way. Its parity and behaviour tests pass unchanged, and its look was not examined here.

## Gate

`./gradlew :mutant-core:desktopTest :sample:desktopTest :mutant-core:ktlintCheck :sample:ktlintCheck --continue` at
`5de114c`, with the scratch tests deleted. It exited 0.

- The JUnit XML of both modules: 450 tests, 0 failures, 0 errors, 0 skipped.
- Both ktlint checks are clean.
- The public API is unchanged (the new pieces are private), so there was no `updateKotlinAbi`.

## Not verified

- **The browser itself, Android and iOS.** The browser was looked at only through the JVM's seams. A real IME
  (composition, Chinese or Japanese input) was not typed, and neither was the on-screen keyboard.
- **The phone's GPU** (B-167): the body shader was not looked at.
- **A mouse drag that selects past the line's edge.** `BasicTextField` no longer scrolls itself, so a selection
  dragged beyond the line does not scroll it. This was not tried.
- **A long right-to-left value, scrolled.** It is covered only by reasoning: F12's short cases pass.
- **The search bar's look** (group 6), which shares the line.
- **Density 2 in the sample's section.** It was looked at in the component stage only.
