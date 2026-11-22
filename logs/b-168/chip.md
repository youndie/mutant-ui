# B-168 — chip: `MutantChip`

Group 3 of [B-168](../../docs/backlog/B-168-look-and-motion-pass.md), on `fix/b-168-chip` from `7a8e691`, 2026-11-22, on
this Mac (the desktop JVM, `runComposeUiTest` with the clock driven by hand, a few ms of real time between frames so
the bodies built ahead are ready).

## What was looked at

Two throwaway tests, deleted before committing; the frames stay in `/private/tmp/b168-chip/` (not in the repository).
Every sheet is a strip of frames 48 ms apart over the first second, then 192 ms apart to 2 s. Each `.txt` beside it has
one line per 16 ms frame with:

- the pixels changed since the last frame (`diff`);
- the fill 6 dp left of each chip's label (`ground`);
- the label's best contrast against it (`c`).

- **The sample as it composes chips** (`MutantUiSample(SampleSettings(…))`, the page scrolled to them), each tap a press
  held 64 ms and released:
  - the Chip section's «Try it» row, «Noise» chosen over «Recordings» (`try-<look>`), in these looks:
    - «Эфир» and «Копия»;
    - reduce motion, «Чистый сигнал», `steppedMotion`;
    - the mouse;
    - density 2 in both palettes;
    - the browser's way of drawing in both palettes (`LocalMutantFirstFrameWaits` false, `LocalMutantPoolBodies` true,
      `LocalMutantBodyOnGpu` true, as `FillChangeOnGpuTest`);
  - the Components page's index, «Selection and input · 9» chosen over «Foundations · 5» (`index-<look>`), in both
    palettes and in the browser's way;
  - the header's theme chips, «Copy» chosen over «On air» (`header-<look>`, `theme-efir-<chip>`), with close-ups of
    «RU» and «EN», which the switch does not touch;
  - B-166's case: the index chips leave with the page (Components → Demo), the group is changed, and they come back
    from the pool with the other chip chosen (`pool-<look>`, the fill read 8 dp in from each chip's left, every frame
    for 24 frames);
  - the Demo page's screen chips and the Library screen's filter chips (`demoidx-`, `demolib-`), in «Эфир», «Копия» at
    density 2, and the browser's way;
  - a press held 600 ms, slid 120 dp off and let go, by touch and by the mouse (`press-<look>`);
  - the Chip section appearing as the Selection group is chosen (`appear-<look>`).
- **The chip alone** (`core-`), at density 2:
  - enabled ↔ disabled, selected or not, running and under reduce motion;
  - select and unselect under a running clock, with the label's ink read every frame as the token it is nearest;
  - a selection reversed 64–320 ms into its pour (`core-int-<print>-<ms>`), the lime pixels counted every frame;
  - a long label in 160 dp and a one-letter label (`core-long.png`).

A touch injected by the test leaves the window in keyboard input mode, so a chip a touch focused draws its focus
brackets for the rest of the test (`try-efir.png`). The mouse switches the window to touch mode, as a real window does:
no brackets in `try-mouse.png`. The brackets are the harness's, not a finding (the buttons' report says the same).

## Findings

### F-chip-01 — the chip's KDoc says its label turns 200 ms after the change — local

- **What.** `MutantChip`'s KDoc says the label "turns to the ink 200 ms after the change, as the engine's toggle does".
  `ChipLabelFollowsFillTest`'s KDoc says the same. Since B-94 the label is not on a timer. It picks between its two
  colours by the fill under its middle (`inkOnBody`), so it turns when the pour's front passes there.
- **Frames.** Measured on a 32 dp chip under a running clock, in `core-select-run.txt` and `core-unselect-run.txt`: the
  ink turns on the 8th frame after the change (128 ms), selecting and unselecting alike. In the sample it turns on the
  7th frame after the release (`try-efir.txt`, `t=192`). Under reduce motion it turns in the change's frame
  (`core-select-reduce.txt`).
- **The item that decided it.** B-94: "the 200 ms lag is gone", a departure from the engine's `_muTc` (D20). The
  comment inside `ChipBody` already says so; the public KDoc a consumer reads does not.

### F-chip-02 — a change of palette pours every chip from the old palette's fill — reported (`surface/BodyMoves.kt`)

- **What.** A body starts a pour whenever its resolved fill colour changes, and a change of palette changes it. On
  the sample's header, choosing «Копия» pours every chip and every other body on the page from «Эфир»'s colours to
  «Копия»'s over the pour's 420 ms:
  - the chosen «Copy» chip shows «Эфир»'s navy under «Копия»'s dark ink, 1.81:1 in the release's frame and the two
    after it (`header-efir.txt`, the "to" chip at `t=80`–`112`), then lime pours over the navy;
  - «RU», which the switch does not touch, pours from navy to cream, its dark word on navy in the strip's frames at
    32–96 ms (`theme-efir-RU.png`);
  - in the browser's way the same (`header-efir-web.png`).
- **The design.** The engine pours only on a toggle (`toggle` sets `_muFx = { from: prev }`). A change of theme
  rebuilds the stages and draws the new colours at once. Under reduce motion the kit lands at once too
  (`header-reduce.png`).
- **Cause.** `BodyMoves.follow` compares the resolved colours, not what the caller asked for (a fill or none). Not the
  chip's: every body with a fill of its theme does it. B-166 named "a change of theme in place" among its cases, for
  the brush built ahead; this is the pour itself.

### F-chip-03 — a selection reversed in its pour's first half drops the new fill in one frame — reported (`surface/BodyMoves.kt`, B-94's number)

- **What.** B-94 decided that a change back within a pour's first half (by time, `stepped(…) < .5`) pours from the
  pour's own old fill. The target is that same fill, so nothing pours: the new fill vanishes in one frame. The front is
  eased, so by half the time it covers most of the body. On a 32 dp chip in «Эфир», the lime pixels frame by frame
  after a select reversed at:
  - 112 ms: 5 000, then 0;
  - 176 ms: 9 776, then 0;
  - 208 ms: 11 587 (93 % of the poured chip's 12 506), then 0;
  - 240 ms and later: a pour back over 13 frames from 12 350 down (`core-interrupted.txt`).
  «Копия» is the same (11 806 → 0 at 208 ms). A double click on a filter chip does this, as does a quick change of
  mind in the «Try it» row.
- **Frames.** `core-int-false-176.png`, `core-int-false-208.png` against `core-int-false-240.png`.
- **The item that decided it.** B-94 ("a body never jumps to a colour it was not showing"). The new fill covers half
  the body at about 125 ms (6 431 px at 128 ms), not at half the pour's time. The threshold looks wrong; not re-decided
  here.

## Looked at and found right

- **B-166 holds**, on the desktop and in the browser's way:
  - in place, every chosen chip's fill changes once and never flicks back in the 2 s after a tap: the «Try it» row,
    the index, the header and the Demo page's two rows, in both palettes and at density 2 (one change per chip in
    every `.txt`);
  - back from the pool with the other chip chosen, each chip draws its new fill from its first frame back, all 24
    frames (`pool-efir-web.txt`, `pool-kopiya-web.txt`).
- **The pour and the label.** The acid pours in from the top over 420 ms and out the same way. The label turns as the
  front passes its middle, and no frame reads under 3:1 against the fill beside it: the least is 4.31:1, at the frame
  the front reaches the word (`try-efir.txt`, «Эфир» at density 1). While the front crosses the word, for one or two
  frames, half the word stands on the other fill (`zoom-192.png`, `zoom-d192.png`). That is B-94's decision: a word
  of one colour, turned at its middle.
- **Reduce motion** turns fill and label in the change's frame (`try-reduce.png`, `core-*-reduce.txt`).
  **`steppedMotion`** steps the pour at the hot tick (`try-stepped.png`). **«Чистый сигнал»** draws the same pour.
- **Density 2** draws the same strips as density 1 at twice the pixels (`try-efir-d2.png`, `try-kopiya-d2.png`).
- **The press.** It heats to amber over about 320 ms and cools over about 480 ms, as the engine's heat does. The word
  turns dark on amber and back (B-91): the least contrast is 3.86:1 by touch and 4.10:1 by the mouse, in «Эфир»
  (`press-touch.txt`, `press-mouse.txt`). A touch slid off scrolls the page and cancels the press. A mouse slid off
  leaves the chip unselected.
- **Disabled ↔ enabled** turns the body and the word in the change's frame, running or paused. The word's ink is
  exactly `textDisabled` or `text` from that frame (`core-disable-*.txt`, `core-enable-reduce.txt`). F-buttons-02 does
  not show on the chip: its body redraws under the word.
- **A long label** ends in an ellipsis inside 160 dp; a one-letter chip keeps its 48 dp (`core-long.png`).
- **Appearing**, in the browser's way too, every chip draws its body from the first frame. The pressed cell heats as
  the held press arrives a frame later (`appear-*.png`).

## Looks the design does not settle

- **Disabled ↔ enabled is a cut**: the slime body becomes the grey no-signal plate in one frame, and back. The design
  draws no transition for `data-state`, and B-17 settled none.
- **A dark mark under the highlight during a pour.** While a body pours, its highlight is hidden. A dark `ˇ` the
  highlight's fill-coloured stroke normally covers then shows near the left of the wide index chips, 10 dp in, for the
  pour's 420 ms (`mark-efir-d2-Founda.png`, `zoom-idx.png`). The engine hides the highlight the same way
  (`H.HG.style.display = 'none'` while `fx.pour` runs), so the design draws it too.

## Fixed here

- **F-chip-01** in `ad96bc5`: `MutantChip`'s KDoc and `ChipLabelFollowsFillTest`'s say the label turns as the pour's
  front passes under its middle, not on a timer. Documentation only: nothing drawn changes, so there are no new frames.
  The behaviour it states is held by the kept `ChipLabelFollowsFillTest` and `ChipBehaviourTest`. The mutant
  `val other = label` turns the word with the selection rather than with the fill. It fails both cases of
  `ChipLabelFollowsFillTest` at 1.75:1 on turn 1, frame 1. Killed.
