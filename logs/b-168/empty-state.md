# B-168 — empty state: `MutantEmptyState` and its six illustrations

Group 14 of [B-168](../../docs/backlog/B-168-look-and-motion-pass.md), on `fix/b-168-empty-state` from `2afc3e9`,
2026-11-22, on this Mac (the desktop JVM, `runComposeUiTest` with the clock driven by hand, 3 ms of real time between
frames so the bodies built ahead are ready).

## What was looked at

Two throwaway tests, deleted before committing; the frames stay in `/private/tmp/b168-empty-state/` (not in the
repository). Each `.txt` beside a sheet has one line per 16 ms frame with the pixels changed since the last frame.

- **Each of the six illustrations alone** (`art-<kind>-<palette>-<density>-<run|reduce>`): «Эфир» and «Копия», density 1
  and 2, running (frames 96 ms apart over 1.7 s, every frame diffed over 2 s) and under reduce motion.
- **The empty state** (the cassette, the design's title and line, a `MutantButton` as the action, the Tape over it;
  `state-<case>-<palette>`): 328 dp (the page's), 160, 120, 600 and no width; a long title and a long line; font scale
  2, with and without the long words; density 2; no line; running, «Чистый сигнал», `steppedMotion`.
- **Appearing in place of content** and leaving again (`appear-<palette>-<desk|web>-<run|reduce>`): a card swapped for
  the empty state, back and again, on the desktop and in the browser's way (`LocalMutantFirstFrameWaits` false,
  `LocalMutantPoolBodies` true, `LocalMutantBodyOnGpu` true, `LocalMutantBodyAsImage` false).
- **A live change of palette** under a shown empty state, «Эфир» → «Копия» → «Эфир» (`palette-<desk|web>`).
- **The sample as it composes them**: `MutantUiSample(SampleSettings(group = Communication))` scrolled to the Empty
  state section through its `ScrollState` (`sample-<palette>-<density>-<look>`), running, reduce motion, «Чистый
  сигнал», `steppedMotion`, and density 2; the Demo page's Library screen with «zzz» typed in its search, which empties
  the list and shows the empty state in its place (`library-<palette>-<run|reduce>`).

Under reduce motion no frame changes in two seconds, in every case. Running, an illustration changes on the 16 ticks of
two seconds (125 ms) and on no frame between; the reels turn on the ticks.

## Findings

### F-empty-state-01 — given less than the art's width, the art runs off to the right — local

- **What.** The empty state's column narrower than its illustration (a 120 or 160 dp side panel, the cassette 216 dp)
  squeezed the art's box to the column, and the art's bodies, placed from the box's left (`position: absolute`), ran
  off to the right alone: the cassette stood 48 dp right of the words' middle at 120 dp, its right end past the
  column, while the title, line and button stayed centred.
- **Frames.** `before/state-w120-efir.png`, `before/state-w160-kopiya.png` (before), `state-w120-efir.png`,
  `state-w160-kopiya.png` (after: the art centred over the words, overflowing both sides alike).
- **The decision.** B-29's AC: "a constraint sweep … neither throws nor clips the content"; its sweep stood at 0 dp (one
  letter a line) and 240, never between the words and the art's width. The skill's rule for a form squeezed under its
  size: it keeps its size, centred, overflowing its slot.

### F-empty-state-02 — the cassette's reels and the set's rods are drawn in pixels, not dp — reported (`illustration/`), [B-170](../../docs/backlog/B-170-illustrations-drawn-in-dp.md)

- **What.** `Art.reel` draws its disc and holes and `Art.television` its two rods with pixel numbers in a `Canvas`
  (`drawCircle(c.text, 13f, Offset(13f, 13f))`, `drawRect(INK, Offset(96f, 2f), Size(3f, 36f))`). At density 2 each
  reel is half its size, 13 dp across in the top-left quarter of its 26 dp box; the rods are half as long and stand at
  half their place, floating over the set's left shoulder. On a phone at 2.625 a reel is 38 % of its size.
- **Frames.** `art-cassette-efir-2x-run.png`, `state-d2-efir.png` (the reels as two small dots at the window's left),
  `art-television-kopiya-2x-run.png`, `sample-kopiya-2x-run.png` (the sample's Television cell: the rods in the air
  above the set's top-left). At density 1 both are right (`art-cassette-efir-1x-run.png`).
- **Why no test saw it.** `IllustrationBehaviourTest` holds a pattern at density 2, not the reel or the rods; the
  parity runs at density 1.

### F-empty-state-03 — `MutantIllustration` squeezed under its width runs off to the right — reported (`illustration/`), [B-170](../../docs/backlog/B-170-illustrations-drawn-in-dp.md)

- **What.** F-empty-state-01's cause is `IllustrationArt`'s `Modifier.size(kind.width.dp, kind.height.dp)`, which a
  smaller constraint wins over. The empty state now gives the art its size itself; `MutantIllustration` used alone in a
  narrow slot still draws from the slot's left and past its right. A `requiredSize` there would serve both.
- **Frames.** The same picture as the empty state before its fix (`before/state-w120-efir.png`), the art drawn by the
  same function.

### F-empty-state-04 — a change of palette hides the cassette's reels for the pour — reported (face of F-chip-02)

- **What.** The cassette's window (`fill = c.background`) pours from the old palette's ground to the new one's, while
  its reels take the new `text` at once. «Эфир» → «Копия»: «Копия»'s dark reels (`#14110F`) on «Эфир»'s navy window
  (`#10142A`), 1.04:1, only the three cream holes showing, from the switch's frame to about 128 ms in; back, «Эфир»'s
  cream reels (`#E9F2E4`) on «Копия»'s cream (`#EFE9D9`), 1.06:1, for as long. The words above and below change in
  the switch's frame.
- **Frames.** `palette-desk.png` (`t=976`–`1104`, `1616`–`1744`), `palette-web.png`, `palette-desk.txt`.
- **Cause.** As F-chip-02: `BodyMoves.follow` pours on any change of the resolved fill, a palette switch included; the
  engine pours only on a toggle.

### F-empty-state-05 — the empty state takes the content's place in one frame — reported (face of F-search-05 / F-card-cocoon-03)

- **What.** On the Library screen, a search that matches nothing swaps the two cards and the cocoon for the empty
  state in one frame (125 898 of 259 200 pixels of the phone), and «Play all» with everything under it jumps up
  by the height difference in the same frame; clearing it swaps back the same way. Alone, card ↔ empty state is a
  one-frame cut both ways (51 266 px of 360 × 440). The design draws the empty state at rest only (README: `rest`);
  the brief's `appear → tune-in` is the card's; whether an empty state tunes in is the owner's.
- **Frames.** `library-efir-run.png`, `library-kopiya-reduce.png`, `appear-efir-desk-run.png` and their `.txt`.
- In the browser's way the first frame of an appearing empty state is B-149's stand-in (the cassette and the button as
  plain shapes, no drips, no reels), the body from the next frame (`appear-efir-web-run.png`, `t=976`), as B-149
  decided; back from the pool it draws whole in its first frame (`t=1936`).

## Not findings

- The cassette in the States row and the five others in the Variants row of the sample section are each drawn once
  (`EmptyStates` drops the cassette from the variants); its words, the button and the art look right in both palettes,
  under «Чистый сигнал» (no Tape) and `steppedMotion` (the Tape steps; the bodies change on their ticks as before).
- Font scale 2: the title and line grow and wrap centred; the art and its labels stay at their size (B-61).
- No width: the column is the art's or the line's width, whichever is wider.

## Fixes

- F-empty-state-01: `MutantEmptyState` gives its art `requiredSize` of the art's size, so a narrower column centres
  it; held by `EmptyStateBehaviourTest` («given less than its art's width, the art stands centred over the words»),
  which reads the drawn columns of the cassette's rows in a 120 dp empty state centred in 360.
