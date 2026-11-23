---
id: B-175
title: "A palette switch pours nothing: every body takes the new palette at once"
status: done
priority: P2
size: S
stage: stage-6-later
blocked_by: []
---

# B-175 — A palette switch pours nothing: every body takes the new palette at once

[B-168](B-168-look-and-motion-pass.md)'s pass found one defect four times; the foundations report
(`logs/b-168/foundations.md`, «shared-code classes» 4) located the cause.

| Face | Report | Seen on `main` (density 2) |
|---|---|---|
| The sample header's chips | F-chip-02 ([report](../../logs/b-168/chip.md)) | the chosen «Copy» chip shows «Эфир»'s navy under «Копия»'s dark word at 1.81:1 for 3 frames; «RU», which the switch does not touch, pours navy → cream |
| A card | F-card-cocoon-02 | the title reads 1.46:1 for 5 frames, the subtitle 1.85:1 for 9; switched back mid-pour it flicks; after a switch back an invisible navy-to-navy pour runs on, and a press then boils only the poured part for ~130 ms |
| A card under a dialog | F-dialog-09 | the held screen never runs its pour: the card keeps «Эфир»'s fill under «Копия»'s dark words for the dialog's whole life |
| The cassette | F-empty-state-04 | its window pours from the old ground while its reels take the new colour at once: the reels vanish for ~144 ms (1.03:1 into «Копия», 1.06:1 back) |

**Why**: `BodyMoves.follow` started a pour whenever the body's resolved fill changed, and `MutantSurface` feeds it
`fill ?: colors.fillOf(profile.fill)`, so «Эфир» ↔ «Копия» is a change of fill for every body on the screen. The engine
pours only on a toggle (`toggle` sets `_muFx = { from: prev }`); a change of theme redraws its stages in the new
colours at once.

## The decision

- **A fill that changes because the palette changed lands at once; only a change of the body's own fill pours.**
  `BodyMoves.follow` takes the palette the fill was resolved in (`LocalMutantColors`, a `MutantColors`) and tells a
  switch by its identity: a composition that brings another palette than the last one repaints the body and starts no
  pour. A pour still running at the switch (a toggle's) lands too: what it pours over — its base and B-171's bands — is
  the old palette's, and drawn on it would be a colour the new palette does not have.
- A toggle in the same composition as the switch lands with it: on the sample's header choosing «Copy» is both, and
  the chosen chip stands lime from the switch's frame, as the engine draws a theme switch.
- The palette is told by identity, not by `equals`: `MutantColors` has no `equals` of its own and its constructor is
  internal, so the kit's palettes are its two objects, and a consumer cannot make a third that equals one.
- Under reduce motion everything lands at once, as before (D7).
- **Rejected:**
  - comparing the fill's role (a token) rather than its colour: a component asks for its fill as a `Color`
    (`fill = if (selected) colors.primary else null` in the chip, and so in every component that passes one); a role would have to be carried through
    every one of them and the surface's signature, to tell what the palette's identity tells already;
  - comparing `colors.print`: a palette of the same kind would pass unseen, and nothing more is learnt from a flag than
    from the object;
  - pouring from the old fill mapped into the new palette (or a cross-fade): the engine draws neither, and the pour
    would still run for 300–620 ms on every body of the screen, the cost this item removes.

## Cost (B-151's phone budget)

- Per frame at rest: one reference comparison per body in composition (`palette !== this.palette`), nothing at draw.
- **A switch saves a phone the pours**: before, every body of the screen was drawn by paths with a pour front for
  300–620 ms (and a pouring body is never drawn by the shader in the browser, B-110); now each draws its new fill from
  its next frame, at rest, as a body that never poured.
- The body shader (`BodyShader.kt`) reads none of this and is untouched.

## Acceptance

- After a palette switch, in both directions, every body shows the new palette's fill from its first frame and no
  pour starts, the frames read from what is drawn at density 2 with the test's clock advanced by hand:
  - `HeaderPaletteSwitchTest` (sample): the chosen chip's word reads ≥ 4.5:1 on its fill on every frame, and «RU»
    shows no pixel of the old palette's fill;
  - `PaletteSwitchPoursNothingTest`: a card's title and subtitle ≥ 4.5:1 and at most 100 px of the old palette's
    fill in the card (a resting card in «Эфир» shows 36 near «Копия»'s cream, a pour from it some 75 000), alone and
    under a dialog's held clock; the cassette's reels ≥ 4.5:1 on their window.
- A toggle still pours as before (the same test: a chip selected pours lime over frames), and a switch mid-pour lands
  it (no pixel of the old fill from the switch's frame on).
- Every golden, parity cell and behaviour test unchanged (none draws a palette switch).
- Anchors: `mutant-core/src/commonMain/kotlin/io/github/youndie/mutant/surface/BodyMoves.kt`,
  `mutant-core/src/commonMain/kotlin/io/github/youndie/mutant/surface/MutantSurface.kt`,
  `mutant-core/src/desktopTest/kotlin/io/github/youndie/mutant/surface/PaletteSwitchPoursNothingTest.kt`,
  `sample/src/desktopTest/kotlin/io/github/youndie/mutant/sample/HeaderPaletteSwitchTest.kt`

## Findings, 2026-11-23

Frames before and after, the header's three chips, a card, a card switched back mid-pour, a card under a dialog and the
cassette, each switched to «Копия» and back (contact sheets and per-frame readings): `/private/tmp/b175/{before,after}/`.
Before, the window of the cassette pours cream from the top past its dark reels and the card under the dialog stands
navy under dark words for as long as the dialog shows; after, every frame from the switch's is the new palette at rest.

Read by the kept tests (the "before" column is the fix's wiring reverted, `main`'s behaviour):

| Face | Before | After |
|---|---|---|
| Header, «Copy» chosen | 1.81:1 in frames 1–3; «RU» shows 3 874 px of «Эфир»'s fill, gone by frame 11 | 16.68:1 from frame 1; 0 px |
| Header, back to «Эфир» | «RU» shows 3 787 px of «Копия»'s fill for 11 frames | «On air» 9.75:1 from frame 1; 0 px |
| Card | title 1.46:1 (5 frames), subtitle 1.85:1 (9), 75 621 px of the old fill in frame 1; back, 74 602 px | 13.62 / 5.43:1 («Копия»), 12.06 / 6.69:1 («Эфир») on every frame; ≤ 36 px |
| Card switched back mid-pour (336 ms) | a second pour runs over the first: the title flicks to 1.46:1 in one frame 192 ms later | lands both times: 13.62 / 5.43:1, then 12.06 / 6.69:1 from the switch's frame |
| Card under a dialog | 1.46 / 1.85:1 and 75 817 px of the old fill on every frame, held | as the card alone |
| Cassette | reels 1.03:1 for 9 frames (144 ms); back, 1.06:1 for 9 | 15.52:1 / 15.82:1 on every frame |
| A chip's toggle, palette switched at frame 8 | the pour runs on from «Эфир»'s fill: 5 535 px of it at the switch | lime whole at the switch, 0 px of the old fill |

The toggle alone pours as before: the frame after it shows under a fifth of the lime, and the lime grows frame by frame.

**Mutants**, each one line after the commit, each killed by its aimed test (the `<failure>` read from the JUnit XML):

- the surface not passing its palette (`MutantSurface`, `follow(…, instant)`): every face fails — the header's
  «Copy» 1.81:1 and 3 874 px in «RU», the card 1.46:1 and 75 621 px, under the dialog 75 817 px on all 40 frames, the
  reels 1.03:1, the toggle's old fill 5 535 px at the switch;
- the running pour not landed (`pourUnder = null` dropped): the toggle switched mid-pour keeps 5 535 px of «Эфир»'s
  fill in the switch's frame; the other faces pass, as nothing else pours at a switch;
- every change of fill taken for a palette switch (`repainted = this.palette != null`): the toggle no longer pours —
  its first frame shows 12 177 lime px of 11 971.

A first version also set `pourSince` to the far past; with `pourUnder` gone nothing pours either way, so that line could
not be killed and was dropped.

**Gate**: `:mutant-core:desktopTest` 512 tests and `:sample:desktopTest` 23, no failure (read from the JUnit XML), every
golden and parity cell among them unchanged and no golden file touched; ktlint and the ABI dump unchanged; the iOS
simulator and wasm builds compile.
