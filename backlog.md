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

## Open (21)

| Task | | Priority | Size | Blocked by |
|---|---|---|---|---|
| [B-151](docs/backlog/B-151-a-real-screen-under-a-finger.md) `[~]` | A real screen under a finger: tabs and scroll on the phone | P1 | L | - |
| [B-185](docs/backlog/B-185-the-dialog-opaque-and-tall.md) `[ ]` | The dialog rises opaque and keeps a margin when tall | P1 | M | B-179 |
| [B-197](docs/backlog/B-197-the-tape-over-every-layer.md) `[ ]` | The Tape over menus, tooltips and dialogs | P1 | L | - |
| [B-109](docs/backlog/B-109-bodies-as-vectors-in-the-browser.md) `[ ]` | Resting bodies as vectors in the browser | P2 | S | - |
| [B-179](docs/backlog/B-179-bodies-tune-in-as-they-appear.md) `[ ]` | Bodies tune in as they appear and step out as they leave | P2 | L | - |
| [B-180](docs/backlog/B-180-a-screen-tunes-in.md) `[ ]` | A new screen tunes in as the switch starts | P2 | M | B-179 |
| [B-181](docs/backlog/B-181-a-body-loses-its-signal-in-steps.md) `[ ]` | A body loses its signal in three steps and tunes in when enabled | P2 | M | B-179 |
| [B-182](docs/backlog/B-182-the-slider-moves-by-itself-on-a-slide.md) `[ ]` | The slider moves by itself on the slide curve | P2 | S | - |
| [B-183](docs/backlog/B-183-the-drawer-and-the-sheet-follow-a-finger.md) `[ ]` | The drawer and the sheet follow a finger and a mouse | P2 | L | - |
| [B-184](docs/backlog/B-184-the-sheet-scrolls-its-content.md) `[ ]` | The bottom sheet scrolls its content and hands a pull at the top to its drag | P2 | M | B-183 |
| [B-187](docs/backlog/B-187-tab-labels-and-a-narrow-top-bar.md) `[ ]` | Tab labels hand the accent over late; a narrow top bar drops its status first | P2 | S | - |
| [B-189](docs/backlog/B-189-a-press-held-inside-a-scroll.md) `[ ]` | A press inside a scroll heats after 100 ms | P2 | M | - |
| [B-192](docs/backlog/B-192-text-buttons-pressed.md) `[ ]` | The text button and the snackbar action show a press | P2 | S | - |
| [B-194](docs/backlog/B-194-the-cassette-label-translated.md) `[ ]` | The cassette label is a translated string that fits | P2 | S | - |
| [B-195](docs/backlog/B-195-a-play-icon.md) `[ ]` | A play icon | P2 | XS | - |
| [B-200](docs/backlog/B-200-the-tooltip-at-the-edges.md) `[ ]` | The tooltip stays in the window and wraps | P2 | M | - |
| [B-202](docs/backlog/B-202-the-data-table-at-a-large-font.md) `[ ]` | The data table at a large font and in a narrow slot | P2 | M | - |
| [B-205](docs/backlog/B-205-mirrored-for-right-to-left.md) `[ ]` | Every layout mirrored for right to left | P2 | L | - |
| [B-206](docs/backlog/B-206-default-keys-by-place.md) `[ ]` | A body's default key is its place in the composition | P2 | M | - |
| [B-132](docs/backlog/B-132-the-shader-rounds-its-contour.md) `[ ]` | The shader rounds its contour as the paths do | P3 | M | - |
| [B-139](docs/backlog/B-139-the-sample-under-the-status-bar-on-ios.md) `[ ]` | The sample under the status bar on iOS | P3 | S | - |

## Closed (188)

**Goldens and references regenerate deterministically; the port matches the goldens on every target; the floor and the frame cost have measured numbers**

- [B-01](docs/backlog/B-01-repository-skeleton.md) `[x]` - Skeleton: mutant-core on desktop, Android, iOS and wasm, the sample kit, both gates
- [B-02](docs/backlog/B-02-fonts.md) `[x]` - Fonts: Handjet and Rubik as static OFL instances, coverage and metrics pinned
- [B-03](docs/backlog/B-03-generator-port.md) `[x]` - Generator: a 1:1 common-code port of the resting frame, held to Node goldens on every target
- [B-04](docs/backlog/B-04-transition-geometry.md) `[x]` - Transition geometry: venom, pour and error blocks ported and held to goldens
- [B-05](docs/backlog/B-05-pixel-references-and-floor.md) `[x]` - Pixel references rendered here from the pages at a fixed engine time, and the parity floor
- [B-06](docs/backlog/B-06-frame-cost.md) `[x]` - The cost of the field on the desktop JVM: an instance and a frame per profile

**Tokens generated and checked; a body in every state and transition matches its references; the Tape matches the demo**

- [B-07](docs/backlog/B-07-kit-clock.md) `[x]` - The kit clock: one per screen, in ms, ticks of 125 and 83 ms, pausable
- [B-08](docs/backlog/B-08-token-generator.md) `[x]` - Tokens generated from mutant-tokens.json, read a second time in a test
- [B-09](docs/backlog/B-09-theme.md) `[x]` - MutantTheme: «Эфир» and «Копия», the cassette seed, reduce motion as pause, «Чистый сигнал»
- [B-10](docs/backlog/B-10-mutant-surface.md) `[x]` - MutantSurface: the body behind the content, the layers, the clean zone, drips outside the bounds
- [B-11](docs/backlog/B-11-surface-states.md) `[x]` - Body states: boil-over, loading, success, disabled, error, focus
- [B-12](docs/backlog/B-12-transitions.md) `[x]` - Transitions: pour, melt, slide, and the venom with its soft joins
- [B-13](docs/backlog/B-13-tape.md) `[x]` - The Tape («Лента»): one overlay over the screen
- [B-14](docs/backlog/B-14-icons-illustrations-patterns.md) `[x]` - Icons, the six illustrations and the four patterns
- [B-49](docs/backlog/B-49-frame-cost-on-a-pixel-6a.md) `[x]` - The cost of a drawn body on a Pixel 6a: ten cards with the Tape, a list, an instance
- [B-50](docs/backlog/B-50-first-frame-off-the-frame.md) `[x]` - Build a body's instance and first frame off the frame
- [B-51](docs/backlog/B-51-size-change-moves-the-flow-lines.md) `[x]` - A size change moves every flow line: accept, or hold them?
- [B-52](docs/backlog/B-52-display-text-against-chrome.md) `[x]` - Display text against Chrome: Handjet labels 2 px low, and a floor for text

**Each MVP component matches its page's states in both themes and its motion at the engine's timestamps**

- [B-15](docs/backlog/B-15-button.md) `[x]` - Button: filled, outlined, text; pressed, loading, success, disabled
- [B-16](docs/backlog/B-16-icon-button-and-fab.md) `[x]` - IconButton and FAB: 48 and 56 dp bodies, the extended FAB
- [B-17](docs/backlog/B-17-chip.md) `[x]` - Chip: unselected with slip, selected by pour
- [B-18](docs/backlog/B-18-checkbox-and-radio-button.md) `[x]` - Checkbox and RadioButton: 22 dp bodies, pour and melt
- [B-19](docs/backlog/B-19-switch.md) `[x]` - Switch: track and thumb, pour and slide
- [B-20](docs/backlog/B-20-text-field.md) `[x]` - TextField: 52 dp field, focus brackets, error corruption
- [B-21](docs/backlog/B-21-card.md) `[x]` - Card: dead pixels in the corner
- [B-22](docs/backlog/B-22-skeleton-cocoon.md) `[x]` - Skeleton («Кокон»): mosaic of 16 and 8 dp in the shape of the coming component
- [B-23](docs/backlog/B-23-top-app-bar-and-tabs.md) `[x]` - TopAppBar and Tabs: the 56 dp bar with a tracking divider, the 4 dp indicator with an echo
- [B-24](docs/backlog/B-24-navigation-bar.md) `[x]` - NavigationBar: bar body, the active item a 56 × 30 pill
- [B-25](docs/backlog/B-25-list-item-divider-badge.md) `[x]` - ListItem, Divider and Badge
- [B-26](docs/backlog/B-26-snackbar.md) `[x]` - Snackbar: a blue bar with a shadow
- [B-27](docs/backlog/B-27-progress.md) `[x]` - LinearProgress and CircularProgress
- [B-28](docs/backlog/B-28-dialog.md) `[x]` - Dialog: a clean window over a paused background
- [B-29](docs/backlog/B-29-empty-state.md) `[x]` - EmptyState: the six illustrations with a title
- [B-53](docs/backlog/B-53-text-button-divider-at-density.md) `[x]` - Text button: its tracking divider is drawn unscaled at a density above 1
- [B-54](docs/backlog/B-54-melt-stalls-short-of-its-end.md) `[x]` - Melt: a mark can stall at 76 % when a frame falls in its last 3 ms

**The three demo screens within the floor in both themes; contrast, flashes and greyscale measured on the kit**

- [B-37](docs/backlog/B-37-demo-screens-and-sample.md) `[x]` - The three demo screens and the sample kit on desktop and the web
- [B-38](docs/backlog/B-38-accessibility.md) `[x]` - Accessibility: contrast after the Tape, flashes, greyscale, semantics
- [B-55](docs/backlog/B-55-public-text-styles.md) `[x]` - Public text styles: a consumer cannot set its own words in Handjet or Rubik

**Each v1 component matches its page and behaves as the owner's answer to B-43 says**

- [B-30](docs/backlog/B-30-slider.md) `[x]` - Slider: a 14 dp channel, an 8 dp fill and a 34 dp handle with the value inside
- [B-31](docs/backlog/B-31-segmented-button.md) `[x]` - SegmentedButton: the selection moves by venom
- [B-32](docs/backlog/B-32-tooltip.md) `[x]` - Tooltip: a blob plate of 30 or 44 dp
- [B-33](docs/backlog/B-33-menu.md) `[x]` - Menu: 36 dp items, the selection by venom
- [B-34](docs/backlog/B-34-navigation-drawer.md) `[x]` - NavigationDrawer: 42 dp items, slip 3, a shadow, the selection by venom
- [B-35](docs/backlog/B-35-bottom-sheet.md) `[x]` - BottomSheet: a blob of r 22 with a divider above
- [B-36](docs/backlog/B-36-pull-to-refresh.md) `[x]` - PullToRefresh: the tongue, refreshing by boil, done by a clean pill

**The owner's questions answered; the QA pass triaged; the repository public and publishing**

- [B-39](docs/backlog/B-39-licence.md) `[x]` - Licence
- [B-40](docs/backlog/B-40-qa-pass-and-real-window.md) `[x]` - A QA pass over the finished MVP and a real window driven by AWT events
- [B-41](docs/backlog/B-41-public-repository-and-publishing.md) `[x]` - Public repository, publishing every main to Reposilite, the site on Pages
- [B-42](docs/backlog/B-42-re-import-the-design.md) `[-]` - Re-import the design when the owner starts it
- [B-43](docs/backlog/B-43-undesigned-behaviour.md) `[x]` - What the design does not draw: appearance, gestures, «Чистый сигнал», hover
- [B-56](docs/backlog/B-56-navigation-bar-unbounded-width.md) `[x]` - Navigation bar: throws in a horizontally scrolling row
- [B-57](docs/backlog/B-57-visible-keyboard-focus-everywhere.md) `[x]` - Keyboard focus: every Tab stop visible, and the dialog's Tab cycle
- [B-58](docs/backlog/B-58-text-field-target-and-caret.md) `[x]` - Text field: the whole body takes the pointer, and the caret marks the insertion point
- [B-59](docs/backlog/B-59-dialog-actions-always-reachable.md) `[x]` - Dialog: its actions stay reachable under long text and long labels
- [B-60](docs/backlog/B-60-tabs-that-do-not-fit.md) `[x]` - Tabs: tabs past the row's width are invisible yet focusable
- [B-61](docs/backlog/B-61-text-under-font-scale.md) `[x]` - Font scale: fixed heights cut text, and illustration labels leave the art
- [B-62](docs/backlog/B-62-focus-through-loading.md) `[x]` - Button: keyboard focus survives Loading and Success
- [B-63](docs/backlog/B-63-hover-and-hand-cursor.md) `[x]` - Desktop: the hand cursor and a hover look
- [B-64](docs/backlog/B-64-weighted-parts-in-unbounded-width.md) `[x]` - Unbounded width: weighted parts collapse to nothing
- [B-65](docs/backlog/B-65-tabs-and-navigation-in-rtl.md) `[x]` - Tabs and the navigation bar mirror in a right-to-left layout
- [B-66](docs/backlog/B-66-navigation-pill-in-narrow-cells.md) `[x]` - Navigation bar: the pill overruns narrow cells
- [B-67](docs/backlog/B-67-text-field-error-description.md) `[x]` - Text field: the error has no words for a screen reader
- [B-68](docs/backlog/B-68-fab-icon-name-match.md) `[x]` - FAB: a consumer's icon is swapped for the kit's on a name match
- [B-69](docs/backlog/B-69-tall-body-tick-cost.md) `[x]` - Measure what a tall body costs per tick
- [B-70](docs/backlog/B-70-snackbar-long-action.md) `[x]` - Snackbar: a long action pushes the message out
- [B-71](docs/backlog/B-71-text-ink-varies-across-ci-macs.md) `[x]` - The code gate replayed failed tests from the build cache as a pass
- [B-72](docs/backlog/B-72-frame-cost-on-the-jvm.md) `[x]` - The sample's frame cost on the JVM, in a real window, and where it goes
- [B-73](docs/backlog/B-73-frame-cost-in-the-browser.md) `[x]` - The sample's frame cost in the browser (wasm)
- [B-74](docs/backlog/B-74-handjet-a-row-higher-on-macos-26.md) `[x]` - Handjet's ink a row higher on the CI's macOS 26 than on macOS 27 and in Chrome
- [B-75](docs/backlog/B-75-tick-frames-off-the-main-thread.md) `[x]` - The generator's tick frame runs on the main thread, 60–80 ms per tick on the Components page
- [B-76](docs/backlog/B-76-render-thread-replay-on-the-demo-page.md) `[x]` - Skia's replay on the render thread bounds the Demo page
- [B-77](docs/backlog/B-77-the-tape-on-the-render-thread.md) `[x]` - The Tape costs 3.2 ms a frame on the render thread of the Demo page
- [B-78](docs/backlog/B-78-the-bodies-on-the-render-thread.md) `[x]` - The bodies' drawing is most of the render thread's frame on the Demo page
- [B-79](docs/backlog/B-79-outline-stroke-on-the-render-thread.md) `[-]` - The outline's round-joined stroke is the dearest part of a body on the render thread
- [B-80](docs/backlog/B-80-contour-clip-on-the-render-thread.md) `[-]` - Clipping a body's layers to its contour costs the render thread 2.7 ms a frame
- [B-81](docs/backlog/B-81-speckle-on-the-render-thread.md) `[-]` - The speckle's squares cost the render thread 1.3 ms a frame
- [B-82](docs/backlog/B-82-rasterise-a-body-once-per-tick.md) `[x]` - Rasterise a body once per tick, and draw the image between ticks
- [B-83](docs/backlog/B-83-a-held-clock-draws-vectors.md) `[x]` - Bodies under a held or paused clock keep drawing vectors on the render thread
- [B-84](docs/backlog/B-84-drip-webs-flip-every-tick.md) `[x]` - Neighbouring drips' webs zip open and shut every tick at rest
- [B-85](docs/backlog/B-85-aliased-text-on-macos-26.md) `[x]` - On macOS 26 the slider's disabled value is drawn without antialiasing
- [B-87](docs/backlog/B-87-ticks-on-the-one-thread.md) `[x]` - In the browser the bodies' ticks have one thread, and the Components page needs more than it
- [B-88](docs/backlog/B-88-keyframe-loss-copies-the-whole-picture.md) `[x]` - The keyframe loss copies its whole picture for every block it drags
- [B-89](docs/backlog/B-89-twelve-ticks-built-in-the-frame-on-a-phone.md) `[x]` - At 12 ticks a second a third of a phone's ticks are built in the frame

**Built only when the owner asks**

- [B-100](docs/backlog/B-100-motion-on-every-frame.md) `[x]` - Movement runs on every frame by default; the boil keeps its tick
- [B-101](docs/backlog/B-101-demo-tape-twice.md) `[x]` - On the Demo page the phones carry the Tape twice
- [B-102](docs/backlog/B-102-drawer-shows-its-venom.md) `[x]` - The drawer closes before its venom run is seen
- [B-103](docs/backlog/B-103-drawer-words-follow-the-pick.md) `[x]` - The drawer's words stay on the row picked before
- [B-104](docs/backlog/B-104-carousel-words-on-a-pressed-card.md) `[x]` - A pressed carousel card keeps its light words on amber
- [B-105](docs/backlog/B-105-tongue-becomes-the-pill.md) `[x]` - Refreshing ends with a cut from the tongue to the pill
- [B-106](docs/backlog/B-106-stepped-motion-named-plainly.md) `[x]` - The header's «Шаги дизайна» switch says nothing of what it does
- [B-107](docs/backlog/B-107-wasm-on-a-phone.md) `[x]` - The sample stutters in a phone's browser
- [B-108](docs/backlog/B-108-a-phones-budget-for-the-boil.md) `[x]` - What the browser costs against Android, on one phone
- [B-110](docs/backlog/B-110-a-body-as-a-shader.md) `[x]` - A body drawn by a GPU shader: a spike
- [B-111](docs/backlog/B-111-what-the-decorations-cost-on-a-phone.md) `[x]` - What a body's decorations cost on a phone
- [B-112](docs/backlog/B-112-static-decorations-drawn-once.md) `[-]` - A body's still decorations drawn once, and painted without the clip
- [B-113](docs/backlog/B-113-an-image-not-copied-each-frame.md) `[-]` - A body's image not copied each frame it is drawn
- [B-114](docs/backlog/B-114-a-tick-image-built-in-the-frame.md) `[-]` - A resting body's tick image built in the frame when the one ahead is late
- [B-115](docs/backlog/B-115-a-body-image-on-the-surface-grid.md) `[x]` - A body's image rasterised on the surface's pixel grid
- [B-116](docs/backlog/B-116-the-proxy-calibrated-to-the-phone.md) `[x]` - The phone proxy calibrated to the phone, and the cuts measured again on it
- [B-117](docs/backlog/B-117-still-layers-once-on-the-surface-grid.md) `[x]` - A body's still layers drawn once, on the surface's grid
- [B-118](docs/backlog/B-118-what-the-decorations-still-cost.md) `[x]` - What the decorations still cost, over B-117
- [B-119](docs/backlog/B-119-the-still-image-marked-finished.md) `[x]` - The still layers' image marked finished
- [B-120](docs/backlog/B-120-what-a-body-costs-over-b-119.md) `[x]` - What a body's parts cost, over B-119
- [B-121](docs/backlog/B-121-the-outline-stroked-once-a-tick.md) `[-]` - A body's outline stroked once a tick
- [B-122](docs/backlog/B-122-builds-ahead-at-user-visible-priority.md) `[-]` - A body's builds ahead posted at user-visible priority in the browser
- [B-123](docs/backlog/B-123-each-body-in-its-own-layer.md) `[-]` - Each body drawn in a layer of its own
- [B-124](docs/backlog/B-124-the-loop-on-the-phone.md) `[x]` - The performance loop's result on the phone
- [B-125](docs/backlog/B-125-what-the-decorations-cost-on-the-phone-now.md) `[x]` - What the decorations cost on the phone, over the loop's build
- [B-126](docs/backlog/B-126-bodies-on-the-gpu.md) `[x]` - Bodies on the GPU, with every decoration
- [B-127](docs/backlog/B-127-speckle-and-fill-as-one-tile.md) `[-]` - A body's fill and speckle as one tile the GPU repeats
- [B-128](docs/backlog/B-128-the-shader-edge-from-the-contour.md) `[x]` - The body shader's edge from the contour itself
- [B-129](docs/backlog/B-129-the-contour-distance-once-a-tick.md) `[-]` - The contour's distance computed once a tick, read by the shader
- [B-130](docs/backlog/B-130-the-shader-marches-its-cells.md) `[x]` - The body shader marches its own cells
- [B-131](docs/backlog/B-131-decorations-in-the-shader.md) `[x]` - A body's decorations drawn by its shader
- [B-133](docs/backlog/B-133-the-keyframe-loss-freezes-a-shader-page.md) `[x]` - The keyframe loss freezes a page the shader draws
- [B-134](docs/backlog/B-134-the-shader-hands-over-half-the-bytes.md) `[x]` - The body shader hands Skia half the bytes
- [B-135](docs/backlog/B-135-a-body-ticks-in-its-own-layer.md) `[x]` - A body ticks in its own layer
- [B-136](docs/backlog/B-136-the-tape-band-on-every-frame.md) `[x]` - The Tape's band and head noise move on every frame
- [B-137](docs/backlog/B-137-mosaic-past-the-contour.md) `[x]` - The edge mosaic's blocks past the body's contour, as an experiment
- [B-138](docs/backlog/B-138-a-round-on-every-platform.md) `[x]` - A round of frame measurements on every platform
- [B-140](docs/backlog/B-140-components-page-in-a-phones-chrome.md) `[x]` - The resting Components page in a phone's Chrome falls to 16 frames a second
- [B-141](docs/backlog/B-141-the-tape-draws-in-a-layer-of-its-own.md) `[x]` - The Tape draws in a layer of its own
- [B-142](docs/backlog/B-142-a-stall-every-nine-seconds-on-the-components-page.md) `[x]` - A stall every nine seconds on the resting Components page
- [B-143](docs/backlog/B-143-the-keyframe-loss-reads-its-blocks-without-copying-strips.md) `[x]` - The keyframe loss reads its blocks without copying whole strips
- [B-144](docs/backlog/B-144-the-keyframe-losss-draw-calls-per-step.md) `[x]` - The keyframe loss's draw calls per step
- [B-145](docs/backlog/B-145-the-keyframe-losss-steps-off-the-slow-path.md) `[x]` - The keyframe loss's steps off the slow path
- [B-146](docs/backlog/B-146-the-repository-put-in-order.md) `[x]` - The repository put in order, and a README
- [B-147](docs/backlog/B-147-the-code-put-in-order.md) `[x]` - The code put in order
- [B-148](docs/backlog/B-148-the-sample-for-a-consumer.md) `[x]` - The sample for a consumer, light on a phone
- [B-149](docs/backlog/B-149-a-body-in-its-first-frame.md) `[x]` - A body drawn in its first frame
- [B-150](docs/backlog/B-150-the-date-picker-keeps-its-width.md) `[x]` - The date picker keeps its width
- [B-152](docs/backlog/B-152-the-browsers-own-sine.md) `[x]` - The generator's sines are the browser's own
- [B-153](docs/backlog/B-153-a-first-picture-between-frames.md) `[x]` - In the browser, a body's first picture is built between frames, whole
- [B-154](docs/backlog/B-154-the-loss-snapshots-the-band-only.md) `[x]` - The keyframe loss's snapshot draws only the bodies its band can show
- [B-155](docs/backlog/B-155-no-tick-built-in-a-frame.md) `[x]` - In the browser, no resting tick is built in a frame
- [B-156](docs/backlog/B-156-a-pool-of-bodies.md) `[x]` - A body left behind is kept, and a screen visited again draws it at once
- [B-157](docs/backlog/B-157-textures-in-one-block.md) `[x]` - In the browser, a body's textures reach Skia in one block
- [B-158](docs/backlog/B-158-the-loss-band-in-memory.md) `[x]` - In the browser, the keyframe loss's band lives in this module's memory
- [B-159](docs/backlog/B-159-the-loss-snapshot-painted-ahead.md) `[x]` - The keyframe loss's snapshot is painted ahead, a body a task
- [B-160](docs/backlog/B-160-a-frame-kept-between-ticks.md) `[x]` - A frame built in a frame is kept until the next tick
- [B-161](docs/backlog/B-161-builds-in-steps.md) `[x]` - In the browser, a body's build lets the page's frames through every few milliseconds
- [B-162](docs/backlog/B-162-a-surface-keeps-its-recent-instances.md) `[x]` - A surface keeps its recent instances, and in the browser builds a new one between frames
- [B-163](docs/backlog/B-163-the-loss-starts-within-a-frame.md) `[x]` - The keyframe loss starts within a frame, and a path-drawn body builds ahead
- [B-164](docs/backlog/B-164-the-kits-text-layouts-kept.md) `[x]` - The kit's text layouts are kept between compositions
- [B-165](docs/backlog/B-165-glyphs-warmed-ahead.md) `[-]` - In the browser, a screen's first frames warm the kit's glyphs
- [B-166](docs/backlog/B-166-a-chip-flicks-back-to-its-old-fill.md) `[x]` - A chosen chip flicks back to its old fill
- [B-167](docs/backlog/B-167-outlines-lost-on-the-phone.md) `[x]` - On the phone, the bodies the shader draws have no outline
- [B-168](docs/backlog/B-168-look-and-motion-pass.md) `[x]` - A look-and-motion pass, one agent per component group, on the desktop JVM
- [B-169](docs/backlog/B-169-text-redraws-on-a-colour-change.md) `[x]` - A word whose colour alone changes is redrawn in the next frame
- [B-170](docs/backlog/B-170-illustrations-drawn-in-dp.md) `[x]` - The illustrations draw in dp at any density and keep their size in a narrow slot
- [B-171](docs/backlog/B-171-transitions-reversed-from-where-they-stand.md) `[x]` - A transition changed mid-way goes on from where the body stands
- [B-172](docs/backlog/B-172-the-venom-picked-mid-run.md) `[x]` - A pick during a venom run goes on from where the slime stands
- [B-173](docs/backlog/B-173-tests-off-the-wall-clock.md) `[x]` - Two tests hang on whether a background build finished in wall time
- [B-174](docs/backlog/B-174-the-sample-after-the-look-pass.md) `[x]` - The sample's own defects that the look-and-motion pass reported
- [B-175](docs/backlog/B-175-a-palette-switch-pours-nothing.md) `[x]` - A palette switch pours nothing: every body takes the new palette at once
- [B-176](docs/backlog/B-176-the-pause-lifted-replays-nothing.md) `[x]` - A transition the pause landed stays landed when the pause lifts
- [B-177](docs/backlog/B-177-the-stand-in-keeps-the-state.md) `[x]` - The browser's stand-in draws the body's state and stroke
- [B-178](docs/backlog/B-178-the-calendar-short-of-height.md) `[x]` - The calendar given less room than it wants keeps its size
- [B-186](docs/backlog/B-186-the-caret-and-the-fields-mosaic.md) `[x]` - The caret restarts, sits beside the letters, and the mosaic ignores the text
- [B-188](docs/backlog/B-188-the-carousel-glides-on-tab-and-fits-a-narrow-shelf.md) `[x]` - The carousel glides on Tab and fits a narrow shelf
- [B-190](docs/backlog/B-190-a-press-under-the-pause.md) `[x]` - A press under the pause shows a still frame at heat 1
- [B-191](docs/backlog/B-191-progress-under-the-pause-and-the-steps.md) `[x]` - The progress band under the pause and under the steps
- [B-193](docs/backlog/B-193-hover-keeps-the-fill.md) `[x]` - Hover keeps the fill and only stirs the outline
- [B-196](docs/backlog/B-196-the-pickers-today-disabled-days-six-rows.md) `[x]` - The pickers: today, disabled days, six rows, the clock's accent
- [B-198](docs/backlog/B-198-the-keyframe-loss-ends-on-a-screen-change.md) `[x]` - The keyframe loss ends on a screen change and takes the dividers
- [B-199](docs/backlog/B-199-no-mosaic-in-the-clean-zone-when-pressed.md) `[x]` - No mosaic block in the clean zone, pressed or at rest
- [B-201](docs/backlog/B-201-the-menu-at-the-edges-and-its-chosen-row.md) `[x]` - The menu opens upward near the foot, scrolls when tall, and answers its chosen row
- [B-203](docs/backlog/B-203-a-button-as-wide-as-its-widest-state.md) `[x]` - A button as wide as its widest state
- [B-204](docs/backlog/B-204-the-navigation-badge-grows-right.md) `[x]` - The navigation badge grows right and stops at 99+
- [B-207](docs/backlog/B-207-focus-after-the-snackbar-and-in-the-drawer.md) `[x]` - Focus returns after the snackbar and cycles in the drawer
- [B-208](docs/backlog/B-208-the-paused-press-shader-test-in-seconds.md) `[x]` - The paused press's shader test runs in seconds
- [B-209](docs/backlog/B-209-the-accessibility-test-in-seconds.md) `[x]` - The sample's accessibility test runs in seconds
- [B-44](docs/backlog/B-44-search-bar.md) `[x]` - SearchBar: a focused field with suggestion chips and result rows
- [B-45](docs/backlog/B-45-date-and-time-pickers.md) `[x]` - DatePicker and TimePicker
- [B-46](docs/backlog/B-46-data-table.md) `[x]` - DataTable: OSD header, 48 dp rows, tracking dividers, row selection
- [B-47](docs/backlog/B-47-carousel.md) `[x]` - Carousel: 220 × 120 cards, calm side cards, a pill pointer among dots
- [B-48](docs/backlog/B-48-keyframe-loss.md) `[x]` - Keyframe loss: a band of the screen breaks into 16 and 8 dp blocks every 9 s
- [B-86](docs/backlog/B-86-tall-body-rasterised-whole.md) `[x]` - A tall body rasterises its whole height every tick
- [B-90](docs/backlog/B-90-a-dialog-stops-the-whole-demo.md) `[x]` - A dialog open in the demo stops every screen and the Tape
- [B-91](docs/backlog/B-91-pressed-labels-on-amber.md) `[x]` - A pressed body's light label stands on amber, unreadable
- [B-92](docs/backlog/B-92-the-dropdown-stays-open.md) `[x]` - The dropdown closes itself after a pick
- [B-93](docs/backlog/B-93-the-calendar-slime-moves-to-the-new-day.md) `[x]` - The calendar's selection jumps to the new day
- [B-94](docs/backlog/B-94-the-navigation-bar-label-on-its-pill.md) `[x]` - A chip's label turned before its fill: light on lime
- [B-95](docs/backlog/B-95-pull-to-refresh-moves-smoothly.md) `[x]` - Pull-to-refresh moves in steps and cuts between its phases
- [B-96](docs/backlog/B-96-a-richer-demo.md) `[x]` - The demo is thin and plain
- [B-97](docs/backlog/B-97-a-risen-sheet-tears-a-wedge.md) `[x]` - A risen bottom sheet tears a wedge out of its body
- [B-98](docs/backlog/B-98-the-loss-crashes-on-a-popup.md) `[x]` - The keyframe loss crashes the window when a popup is open over it
- [B-99](docs/backlog/B-99-components-page-structured-and-neat.md) `[x]` - The Components page is to be structured and neat

<!-- END INDEX -->
