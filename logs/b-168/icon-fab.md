# B-168 — icon-fab: MutantIconButton, MutantFab, MutantExtendedFab

Group 2 of [B-168](../../docs/backlog/B-168-look-and-motion-pass.md), 2026-11-22, on the desktop test runtime of this Mac.
The components' own items: [B-16](../../docs/backlog/B-16-icon-button-and-fab.md) (sizes, the extended FAB's 24 dp
padding, the FAB's 2.5 dp icon line, no disabled FAB), [B-68](../../docs/backlog/B-68-fab-icon-name-match.md) (the
stroked icon by identity), [B-91](../../docs/backlog/B-91-pressed-labels-on-amber.md) (the icon's ink follows the fill).
The design: the «IconButton · FAB» stage of `mutant-ui Компоненты.dc.html` (`iconbtn` rest / `data-heat` / disabled,
`fab` rest / `data-heat`, `fab-x` 136 × 56), the handoff's rows `IconButton` (`icon`, 48 × 48) and `FAB` (`fab`, 56 × 56,
extended 136 × 56), the profiles `icon` and `fab`, and `drawItem`'s boil (heat up in 320 ms, down in 480, the fill
`mix(fill, amber, min(1, 1.15·heat))`, lifted 0.2 on odd 333 ms periods past half heat).

## What was looked at

Frames in `/private/tmp/b168-icon-fab/` (scratch tests, deleted). Sheets are one row a frame (16 ms) or every second
frame (32 ms), a column per specimen; `*-diff.txt` counts the pixels more than 16 levels apart between consecutive frames.

| Sheet | What it composes | States and transitions |
|---|---|---|
| `appear-{efir,kopiya}-p*.png` | `MutantUiSample(group = Actions)`, the IconButton and FAB sections: rest, pressed, disabled; FAB, extended, rest, pressed | the group switched away and back, 40 frames |
| `rest-{efir,kopiya}-p*.png` | the same | two seconds at rest, every frame diffed |
| `press-{efir,kopiya,reduce,clean,stepped,density2}-p*.png` | the same | a finger down on the rest icon button, the FAB and the extended FAB for 576 ms, up, 800 ms; under reduce motion, «Чистый сигнал», `steppedMotion`, density 2 |
| `hover-*`, `focus-*`, `cancel-efir-*` | the same | a mouse over and off; keyboard focus on each; a press dragged off and lifted; four quick taps |
| `fab-density2-p*.png` | the FAB section at density 2 | a press and release |
| `demo-lib-fab-*`, `demo-rec-share-*` | the Demo page's Library FAB (in its tooltip box, opening the sheet) and the Recording screen's share button | press, release, the sheet rising over the FAB; a mouse hover, press and release |
| `seams-{press,pool-efir,pool-kopiya,enabled,appear-d2}-p*.png` | the three, keyed as the sample keys them, in the browser's way of drawing (`LocalMutantFirstFrameWaits` false, `LocalMutantPoolBodies` true, `LocalMutantBodyOnGpu` true, `LocalMutantBodyAsImage` false) | a press and release; pressed to amber, removed, composed back (the pool); enabled → disabled → enabled; the first frames at density 2 |
| `misc-toggle-hover-kbd-p*.png` | an icon button whose icon and description toggle; a disabled one under the mouse; Enter held on a focused one | |
| `squeezed-before.png`, `squeezed-before.txt` | each after a long title in a 200 dp row, the extended FAB in 70 dp, the icon button in 30 dp | at rest |

Held as the design draws it, and looked at: the icon button's surf2 body boils to amber under a press and its icon
turns from the text's colour to the ink as the fill passes the midpoint (both ways, no frame where neither reads: the
icon on the icon body reads at least 3.9:1 at every heat in «Эфир», 11:1 in «Копия», computed from the tokens); the FAB
and extended FAB boil green → amber with the ink icon and label throughout; the 333 ms lift past half heat (the
engine's); the hover simmer at 0.3 (B-63); keyboard focus calms the body and draws the four brackets; Enter boils the
body as a press does; a disabled icon button takes no hover; reduce motion stands every body; «Чистый сигнал» removes the
Tape only; `steppedMotion` moves nothing of this group's (its heat already steps at the hot tick); density 2 draws every
part in proportion; a group opened again draws its bodies from its first frame; in the browser's way a body back from
the pool after a press draws its rest fill from its first frame (no stale amber, B-166's class), and no frame is blank.
The rest diffs change on the body's 8 ticks a second and the Tape's band only: no frame flicks back.

A touch press in the test window focuses the control and shows the brackets (`press-*`, the extended FAB): the test
window's input mode is Keyboard. On a device a touch switches the mode to Touch, and on the desktop a mouse click does not
move focus (B-32); the `hover-*` and `demo-rec-share-*` sheets, by mouse, show no brackets. Not a finding.

## Findings

### F-icon-fab-01 — squeezed by its row, the icon button and the FAB vanish and stay focusable (local)

After a title that takes the whole row, `MutantIconButton` and `MutantFab` measure **0 × 48** and **0 × 56**: nothing is
drawn, and the node still takes focus and a reader's click (`squeezed-before.png`, `squeezed-before.txt`). In 30 dp the
icon button is a 30 × 48 sliver; in 70 dp the extended FAB loses its label and its body is squashed to the box. A user
sees the action gone. The kit called the same shape a defect before: a primary squeezed to 0 (B-40 F07, B-59), tabs at
0 × 0 still focusable (F08, B-60); and B-16's AC asks that no constraint clip the content. The design draws the body at
its size and never squeezed (48 × 48, 56 × 56, 56 tall).

Cause: `ActionBody` sizes the square bodies with `Modifier.size(side)` and the extended FAB with `defaultMinSize` and
`height`, all of which yield to a smaller incoming maximum. **Local**: `components/MutantIconButton.kt`.

### F-icon-fab-02 — the sample's «pressed» cells are not pressed under reduce motion, and ramp up on every visit (reported: the sample)

The States rows' pressed icon button and pressed FAB (`ComponentsActions.kt`, `held(true)`) are a held press, which
boils by the heat machine on the screen's clock. Under the header's reduce motion the clock stands and a press does not
boil (B-11, as the engine's pause), so both cells draw the rest body beside their «pressed» caption
(`press-reduce-p*.png`, columns 2 and 7). Without it, every time the group is opened they draw at rest for a frame and
then take the press's 320 ms to reach amber (`appear-efir-p0.png`, 1744–1888). The page's own pressed cells are a fixed
heat (`data-heat`), pressed from their first frame and under the pause. A specimen of a state should show the state;
the fixed heat exists in the kit only as a fixture's internal parameter, so the sample cannot ask for it. (Also there:
the pressed icon button's description is the OSD word «REFRESHING».) Shared with the Buttons row's pressed cell.

### F-icon-fab-03 — a default key follows the description, so a toggled icon button jumps to another body (reported: a look the design does not settle)

`key` defaults to `contentDescription ?: icon.name` (the extended FAB's to its text). A consumer's play / pause button
that swaps its icon and description gets a new instance on every toggle: drips, highlight and outline jump to another
shape in one frame (`misc-toggle-hover-kbd-p0.png`, 1040 → 1056). Every labelled component of the kit defaults its key
to its label (Button, Chip, Checkbox, Switch, Slider, Tooltip), and the sample sets explicit keys where its labels
change (`buttonB`). Whether a component's body should keep its shape across a change of its own words is the owner's to
settle; a KDoc line («give a stable `key` when the description changes») is the least the kit owes.

### F-icon-fab-04 — enabling or disabling cuts in one frame (reported: shared, `surface/`, a look the design does not settle)

`enabled = false` swaps the running body for the still «no signal» frame between two frames, and back
(`seams-enabled-p0.png` 1008 → 1024, `seams-enabled-p1.png` 1200 → 1216). The design draws the disabled state as a still
frame and no transition into or out of it; the brief's event table has none either. It is the surface's `BodyState`,
shared by every component with a disabled state.

### F-icon-fab-05 — under reduce motion a press shows nothing, and its release twitches the FABs' drips (reported: shared, `surface/`, as the engine)

With the clock standing, a press neither boils nor recolours (B-11 decided it from the engine's pause), so a press
gives no feedback but the focus brackets of a keyboard. On release the FAB and the extended FAB redraw from the rest
tick's frame instead of the hot tick's, and their drips change in one frame (`press-reduce-diff.txt`: 94 and 124 px at
2320 ms, `press-reduce-p1.png`). The engine does the same (`drawItem`: a pressed body draws at `tsH`, a resting one at
`ts`). Listed for the owner beside F-04: reduce motion leaves a press without a visible answer.

## Fixes

Filled in as each is made: F-icon-fab-01.
