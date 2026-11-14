# Backlog: mutant-ui

> Role of this document: the product backlog. **One file per item in
> [`docs/backlog/`](docs/backlog/)**, named `B-NN-<slug>.md`. What lives here is the index (generated)
> and everything that is not an item: the goal, the stages, and the order of work.
>
> New item: copy [`docs/templates/backlog-item.md`](docs/templates/backlog-item.md), take the next
> free `B-NN`, and run `make fix` after editing.

## Goal

Ship the mutant-ui design — the damaged copy: generated slime bodies with drips, mosaic and flow
lines under clean content, the Tape over the screen, two themes, 21 MVP components with their
states and transitions, then the v1 set — as a Compose Multiplatform library that **provably**
matches the design. The generator is held to goldens its own JS emits in Node, on every target;
components are held to references the repository renders from the pages in Chrome (research D2).

The design is frozen in [`reference/design-system/`](reference/design-system/) at the snapshot
[`reference/README.md`](reference/README.md) names. It is evidence, not a document this repository
maintains.

## Order of work, and why it is this order

1. **What can invalidate the architecture comes first** (stage 0): the skeleton, the fonts, the
   generator port with its oracle, the transition geometry, the pixel references with the parity
   floor, and the cost of the field. The field is recomputed on the CPU every tick for every
   visible instance; if that does not fit a phone, the body renderer is built differently, so it is
   measured before the components are built on it (research D13).
2. **The materials** (stage 1): the kit clock, generated tokens, the theme with reduce motion and
   «Чистый сигнал», the body renderer and its states, the transitions, the Tape, icons and
   illustrations. Every component reads these.
3. **The MVP components** (stage 2), in the groups of the Components page.
4. **The demo screens and accessibility** (stage 3): the three screens rebuilt from the
   components, contrast after the Tape, the flash rule, greyscale, semantics.
5. **The v1 set** (stage 4): slider, segmented button, tooltip, menu, drawer, sheet,
   pull-to-refresh — their appearance and gestures designed here, as the owner decided
   ([B-43](docs/backlog/B-43-undesigned-behaviour.md)).
6. **The library as a product** (stage 5): licence, a QA pass and a real window, publishing,
   re-importing the design.
7. **Later** (stage 6): the components the brief puts after publication, and keyframe loss.

The owner answered on 2026-11-14: the licence is MIT ([B-39](docs/backlog/B-39-licence.md)); what the
design does not draw is designed in this repository, as in plakat-ui
([B-43](docs/backlog/B-43-undesigned-behaviour.md)). The loop works the backlog locally, merges its
own branches and commits without asking (`CLAUDE.md`, «Workflow»).

## Stages

| Stage | What closes it |
|---|---|
| `stage-0-risks` | Goldens and references regenerate deterministically; the port matches the goldens on every target; the floor and the frame cost have measured numbers |
| `stage-1-materials` | Tokens generated and checked; a body in every state and transition matches its references; the Tape matches the demo |
| `stage-2-components` | Each MVP component matches its page's states in both themes and its motion at the engine's timestamps |
| `stage-3-screens` | The three demo screens within the floor in both themes; contrast, flashes and greyscale measured on the kit |
| `stage-4-v1` | Each v1 component matches its page and behaves as the owner's answer to B-43 says |
| `stage-5-product` | The owner's questions answered; the QA pass triaged; the repository public and publishing |
| `stage-6-later` | Built only when the owner asks |

<!-- BEGIN INDEX -->

## Open (41)

| Task | | Priority | Size | Blocked by |
|---|---|---|---|---|
| [B-10](docs/backlog/B-10-mutant-surface.md) `[ ]` | MutantSurface: the body behind the content, the layers, the clean zone, drips outside the bounds | P0 | L | B-03, B-05, B-07, B-08 |
| [B-07](docs/backlog/B-07-kit-clock.md) `[~]` | The kit clock: one per screen, in ms, ticks of 125 and 83 ms, pausable | P1 | S | B-01 |
| [B-08](docs/backlog/B-08-token-generator.md) `[ ]` | Tokens generated from mutant-tokens.json, read a second time in a test | P1 | S | B-01 |
| [B-09](docs/backlog/B-09-theme.md) `[ ]` | MutantTheme: «Эфир» and «Копия», the cassette seed, reduce motion as pause, «Чистый сигнал» | P1 | S | B-07, B-08 |
| [B-11](docs/backlog/B-11-surface-states.md) `[ ]` | Body states: boil-over, loading, success, disabled, error, focus | P1 | M | B-10, B-04 |
| [B-12](docs/backlog/B-12-transitions.md) `[ ]` | Transitions: pour, melt, slide, and the venom with its soft joins | P1 | M | B-04, B-11 |
| [B-13](docs/backlog/B-13-tape.md) `[ ]` | The Tape («Лента»): one overlay over the screen | P1 | M | B-07, B-09 |
| [B-14](docs/backlog/B-14-icons-illustrations-patterns.md) `[ ]` | Icons, the six illustrations and the four patterns | P1 | S | B-10 |
| [B-15](docs/backlog/B-15-button.md) `[ ]` | Button: filled, outlined, text; pressed, loading, success, disabled | P1 | M | B-10, B-11 |
| [B-16](docs/backlog/B-16-icon-button-and-fab.md) `[ ]` | IconButton and FAB: 48 and 56 dp bodies, the extended FAB | P1 | M | B-10, B-11 |
| [B-17](docs/backlog/B-17-chip.md) `[ ]` | Chip: unselected with slip, selected by pour | P1 | M | B-10, B-11, B-12 |
| [B-18](docs/backlog/B-18-checkbox-and-radio-button.md) `[ ]` | Checkbox and RadioButton: 22 dp bodies, pour and melt | P1 | M | B-10, B-11, B-12 |
| [B-19](docs/backlog/B-19-switch.md) `[ ]` | Switch: track and thumb, pour and slide | P1 | M | B-10, B-11, B-12 |
| [B-20](docs/backlog/B-20-text-field.md) `[ ]` | TextField: 52 dp field, focus brackets, error corruption | P1 | M | B-10, B-11 |
| [B-21](docs/backlog/B-21-card.md) `[ ]` | Card: dead pixels in the corner | P1 | M | B-10, B-11 |
| [B-22](docs/backlog/B-22-skeleton-cocoon.md) `[ ]` | Skeleton («Кокон»): mosaic of 16 and 8 dp in the shape of the coming component | P1 | M | B-10, B-07 |
| [B-23](docs/backlog/B-23-top-app-bar-and-tabs.md) `[ ]` | TopAppBar and Tabs: the 56 dp bar with a tracking divider, the 4 dp indicator with an echo | P1 | M | B-10, B-07 |
| [B-24](docs/backlog/B-24-navigation-bar.md) `[ ]` | NavigationBar: bar body, the active item a 56 × 30 pill | P1 | M | B-10, B-11 |
| [B-25](docs/backlog/B-25-list-item-divider-badge.md) `[ ]` | ListItem, Divider and Badge | P1 | M | B-10 |
| [B-26](docs/backlog/B-26-snackbar.md) `[ ]` | Snackbar: a blue bar with a shadow | P1 | M | B-10, B-11 |
| [B-27](docs/backlog/B-27-progress.md) `[ ]` | LinearProgress and CircularProgress | P1 | M | B-07, B-08 |
| [B-28](docs/backlog/B-28-dialog.md) `[ ]` | Dialog: a clean window over a paused background | P1 | M | B-10, B-11 |
| [B-29](docs/backlog/B-29-empty-state.md) `[ ]` | EmptyState: the six illustrations with a title | P1 | M | B-14 |
| [B-37](docs/backlog/B-37-demo-screens-and-sample.md) `[ ]` | The three demo screens and the sample kit on desktop and the web | P1 | M | B-15, B-16, B-17, B-18, B-19, B-20, B-21, B-22, B-23, B-24, B-25, B-26, B-27, B-28, B-29 |
| [B-38](docs/backlog/B-38-accessibility.md) `[ ]` | Accessibility: contrast after the Tape, flashes, greyscale, semantics | P1 | M | B-13, B-37 |
| [B-40](docs/backlog/B-40-qa-pass-and-real-window.md) `[ ]` | A QA pass over the finished MVP and a real window driven by AWT events | P1 | M | B-37 |
| [B-41](docs/backlog/B-41-public-repository-and-publishing.md) `[ ]` | Public repository, publishing every main to Reposilite, the site on Pages | P1 | M | B-39, B-40 |
| [B-49](docs/backlog/B-49-frame-cost-on-a-pixel-6a.md) `[ ]` | The cost of a drawn body on a Pixel 6a: ten cards with the Tape, a list, an instance | P1 | M | B-06, B-10 |
| [B-30](docs/backlog/B-30-slider.md) `[ ]` | Slider: a 14 dp channel, an 8 dp fill and a 34 dp handle with the value inside | P2 | M | B-10, B-11, B-43 |
| [B-31](docs/backlog/B-31-segmented-button.md) `[ ]` | SegmentedButton: the selection moves by venom | P2 | M | B-10, B-12 |
| [B-32](docs/backlog/B-32-tooltip.md) `[ ]` | Tooltip: a blob plate of 30 or 44 dp | P2 | M | B-10, B-43 |
| [B-33](docs/backlog/B-33-menu.md) `[ ]` | Menu: 36 dp items, the selection by venom | P2 | M | B-10, B-12, B-43 |
| [B-34](docs/backlog/B-34-navigation-drawer.md) `[ ]` | NavigationDrawer: 42 dp items, slip 3, a shadow, the selection by venom | P2 | M | B-10, B-12, B-43 |
| [B-35](docs/backlog/B-35-bottom-sheet.md) `[ ]` | BottomSheet: a blob of r 22 with a divider above | P2 | M | B-10, B-43 |
| [B-36](docs/backlog/B-36-pull-to-refresh.md) `[ ]` | PullToRefresh: the tongue, refreshing by boil, done by a clean pill | P2 | M | B-10, B-11, B-43 |
| [B-42](docs/backlog/B-42-re-import-the-design.md) `[ ]` | Re-import the design when the owner starts it | P3 | S | - |
| [B-44](docs/backlog/B-44-search-bar.md) `[ ]` | SearchBar: a focused field with suggestion chips and result rows | P3 | M | B-20, B-17 |
| [B-45](docs/backlog/B-45-date-and-time-pickers.md) `[ ]` | DatePicker and TimePicker | P3 | M | B-10, B-43 |
| [B-46](docs/backlog/B-46-data-table.md) `[ ]` | DataTable: OSD header, 48 dp rows, tracking dividers, row selection | P3 | M | B-18, B-25 |
| [B-47](docs/backlog/B-47-carousel.md) `[ ]` | Carousel: 220 × 120 cards, calm side cards, a pill pointer among dots | P3 | M | B-21, B-43 |
| [B-48](docs/backlog/B-48-keyframe-loss.md) `[ ]` | Keyframe loss: a band of the screen breaks into 16 and 8 dp blocks every 9 s | P3 | M | B-13, B-06 |

## Closed (8)

**Goldens and references regenerate deterministically; the port matches the goldens on every target; the floor and the frame cost have measured numbers**

- [B-01](docs/backlog/B-01-repository-skeleton.md) `[x]` - Skeleton: mutant-core on desktop, Android, iOS and wasm, the sample kit, both gates
- [B-02](docs/backlog/B-02-fonts.md) `[x]` - Fonts: Handjet and Rubik as static OFL instances, coverage and metrics pinned
- [B-03](docs/backlog/B-03-generator-port.md) `[x]` - Generator: a 1:1 common-code port of the resting frame, held to Node goldens on every target
- [B-04](docs/backlog/B-04-transition-geometry.md) `[x]` - Transition geometry: venom, pour and error blocks ported and held to goldens
- [B-05](docs/backlog/B-05-pixel-references-and-floor.md) `[x]` - Pixel references rendered here from the pages at a fixed engine time, and the parity floor
- [B-06](docs/backlog/B-06-frame-cost.md) `[x]` - The cost of the field on the desktop JVM: an instance and a frame per profile

**The owner's questions answered; the QA pass triaged; the repository public and publishing**

- [B-39](docs/backlog/B-39-licence.md) `[x]` - Licence
- [B-43](docs/backlog/B-43-undesigned-behaviour.md) `[x]` - What the design does not draw: appearance, gestures, «Чистый сигнал», hover

<!-- END INDEX -->
