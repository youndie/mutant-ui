---
id: research-architecture
title: mutant-ui — architecture research
type: research
status: active
date: 2026-11-13
---

# Research: the architecture of mutant-ui

mutant-ui is a Compose Multiplatform component kit in the look of a damaged copy: grime, glitch and
VHS. Every component is a copy of a clean original, spoiled its own way — flat acid slime with an
ink outline and drips that run down past the slot, compression mosaic along the edge, tape noise
over the screen — while text, icons, layout and touch targets stay clean. It is the fourth kit of
the line after oldge-ui, toon-ui and plakat-ui and keeps their contract: Material 3's inventory and
API expectations, redrawn, with no Material dependency. What sets it apart is that the shape of
every instance is **generated**: a pure function of the cassette seed, the instance key, the slot
and the time, so the same button is damaged the same way on every launch and every platform
(brief, «Что это и зачем»).

This document records **verified facts** (what was actually read or run in the vendored design and
in the artefacts it depends on), **decisions taken**, and **risks**. Anything unverified is marked
as a hypothesis and names the backlog item that settles it.

This is the entry point of the documentation. Nothing is built yet: every path into code below is
a path into the frozen design, `reference/design-system/`, or into this repository's one script.

---

## 1. Verified facts

### 1.1 What was imported

The owner exported the Claude Design project `3c6970f5-9ab1-4100-b418-41b7bacfcf78` as a zip on
2026-11-13 («Mutant-UI этап 1 — обзор экранов.zip»). It was unpacked unchanged into
`reference/design-system/`. DesignSync was not used: in this portfolio it runs only under the
owner's `/design-sync`, and the zip is the designer's own handoff set.

| Fact | Where verified |
|---|---|
| 17 files: the handoff README, the brief (revision 5), the generator, the browser engine, the golden vectors, the tokens, ten `.dc.html` pages and the pages' runtime | `reference/README.md`, `reference/design-system/README.md` («Состав пакета») |
| The zip stores its names in UTF-8 without the UTF-8 flag; `unzip` on macOS mangles them and stops with "write error (disk full?)". Python's `zipfile` with a cp437 → UTF-8 re-decode unpacks all 17 | the import run, 2026-11-13 |
| Every page loads `mutant-engine.js?v=50`; the tokens' `version` is «ред. 5 · этап 1 · по состоянию движка» | `reference/design-system/mutant-ui Компоненты.dc.html` (`<helmet>`), `reference/design-system/mutant-tokens.json` (`mutant.version`) |
| The pages need the network to render: `support.js` loads React 18.3.1, ReactDOM 18.3.1 and Babel standalone 7.29.0 from unpkg, and each page loads Handjet, Rubik and Yeseva One from Google Fonts | `reference/design-system/support.js`, the `<helmet>` of each page |
| Yeseva One is not a third face of the kit: it is one of four options of the pages' `headFont` editor control, whose default is Handjet | `reference/design-system/mutant-ui Компоненты.dc.html` (`static FONTS`, the `headFont` property) |

**Consequence 1.** Nothing the code needs is missing: there are no raster textures at all, the
icons are path strings on a page, and the illustrations are built from the same bodies as the
components (handoff README, «Ресурсы»). The reference renderer must still serve the fonts from the
repository rather than from Google, so that the faces are the files the Compose fixtures use
([B-05](../backlog/B-05-pixel-references-and-floor.md)).

### 1.2 What kind of design this is

`cmp-ui-library` splits designs into CSS design systems (pixels only) and executable designs
(numbers and pixels). This one is **executable**, like toon-ui's, and more so: the drawing of every
component is the output of a generator, and the pages only arrange slots.

| Fact | Where verified |
|---|---|
| A component on a page is a slot element with `data-mu="<type>"` and attributes (`data-key`, `data-fill`, `data-over`, …); the engine measures it, calls the generator and draws SVG layers behind the content | `reference/design-system/mutant-engine.js` (`itemHtml`, `drawItem`), the 47 `data-mu` slots in the markup of `reference/design-system/mutant-ui Компоненты.dc.html` |
| The generator is pure: no DOM, no clock, no `Math.random`; it ends with `window.MutantGenerator = MutantGenerator` and runs in a Node `vm` context whose only global is an empty `window` | `reference/design-system/mutant-generator.js`, `scripts/design-golden.mjs` |
| The engine has a clock of its own in ms, scaled by `speed` and stopped by `setPaused(true)`, which the comment names the reduce-motion mode; a slot with `data-mu-t` (or `still`) is drawn at a fixed time, 5000 ms by default | `reference/design-system/mutant-engine.js` (`fxTime`, `setPaused`, `still`/`tFix` in the slot reader) |
| The type profiles (`btn`, `fab`, `icon`, `chip`, `card`, `field`, `track`, `thumb`, `box`, `dot`, `bar`, `pill`, `blob`) are the generator's `static K` and the tokens' `profile`; the two themes are its `static TH` (`efir`, `kopiya`) | `reference/design-system/mutant-generator.js` (`static K`, `static TH`), `reference/design-system/mutant-tokens.json` (`mutant.profile`, `mutant.color`) |

| The engine's clock: a frame steps `min(1000, now − last)` ms, 16 on the first, times `speed`; the resting tick is `floor(t / step) · step` with `step = 1000 / fps`, the hot one 1.5 times as often; the boil phase is `floor(t / (hot ? 125 / 1.5 : 125) + 0.001) mod 4`, whose 0.001 the ticks lack, so at 83.333333 ms the hot phase is 1 while the hot tick is still 0. The kit's `MutantClock` reproduces all three at 546 times around every boundary and between them, on every target | `reference/design-system/mutant-engine.js` (`frame`, `drawItem`), `MutantClockTest` ([B-07](../backlog/B-07-kit-clock.md)) |

**Consequence 2.** Two parity regimes, as in toon-ui: the generator is checked by **numbers**
against goldens its own JS emits in Node; components are checked by **pixels** against references
rendered from the pages in headless Chrome at a fixed engine time (D2). The engine itself is not
ported (handoff README), so how it calls the generator — the layer order, the boil formula, the
tick — is read from it and held by the frame goldens.

### 1.3 The generator and its golden

`scripts/design-golden.mjs` runs the vendored generator untouched and compares it with every entry
of the vendored golden, drawing frames the way `drawItem` draws them.

| Fact | Where verified |
|---|---|
| 120 of 123 comparisons are byte-identical: FNV-1a for five strings, three mulberry32 streams, the lattice hash and value noise at five points, six instances (scale, bottom radius, wave, shape, every drip, number of flow lines and of mosaic blocks), nine frames (boil, primitive count, grid, contour, highlight), the flow lines of the six resting frames, and nine venom shapes | `node scripts/design-golden.mjs`, run 2026-11-13 |
| The three that differ are the flow lines of the three hot frames (heat 1, t = 9340). They match when `markPath` is given the resting boil 0.8; the engine passes boil' = boil·(1 + 2·heat) + 0.6·heat = 3.0, which the golden itself records as the frame's `boil` | the same run; a parameter search over t, boil phase and boil; `reference/design-system/mutant-engine.js` (`drawItem`: `markPath(g, t, bk, boE)`) |
| The golden's `noise[].h21` is the hash of the lattice node the point falls in, `h21(floor x, floor y)`, not of the point | the same run |
| The check can fail: a copy of the generator with `smin`'s 0.25 changed to 0.26 gives 15 unexpected differences, one with a flow-line width constant changed gives 6, and both exit 1 | a mutated copy in a scratch directory, 2026-11-13 |

**Consequence 3.** The vendored golden is a correct oracle except for the hot frames' flow lines,
and it is too small to hold a port: six instances, nine frames, no `field` without a mosaic, no
`dot`/`box`/`pill`/`bar`/`blob`, no pour, no error blocks. The repository writes its own goldens
from the vendored JS over many keys, sizes, profiles, times and heats, and keeps the vendored one
as an external check ([B-03](../backlog/B-03-generator-port.md), D3). For the hot flow lines the
port follows the engine, because the engine is what the pages draw (D4).

### 1.4 What a port has to get exactly right

Read in `reference/design-system/mutant-generator.js`; each line is a place where an obvious Kotlin
translation would draw a different shape.

| Fact | Where verified |
|---|---|
| The lattice hash is `fract(sin(127.1·x + 311.7·y) · 43758.5453)`: a platform's `sin` that differs in the last bit moves the hash by about 4·10⁴ ulp | `mutant-generator.js` (`h21`) |
| The field is stored in `Float32Array` and the march reads it back as float; computed in `Double` and never rounded to `Float`, the iso-contours move | `mutant-generator.js` (`field`: `new Float32Array(N)`; `march`) |
| Path coordinates are rounded with JS `Math.round(v · 10) / 10`, half up toward +∞; Kotlin's `kotlin.math.round` is half to even, and the golden's strings are compared exactly | `mutant-generator.js` (`pathBuf`, `pathOf`) |
| FNV-1a and mulberry32 rely on `Math.imul`, `>>>` and `\| 0`; strings are hashed by UTF-16 code unit (`charCodeAt`) | `mutant-generator.js` (`hash`, `rng`) |
| Each kind of randomness has its own stream `rng(seed + '/' + key, name)` (`grime`, `venom`, `err`, `mark:dead`, `edge`, …) and the order of calls inside a stream is part of the contract | `mutant-generator.js` (`genItem`, `venomShapes`, `errBlocks`); handoff README, «Детерминизм и сверка» |
| `field` caches the static half of the field per instance and grid size in `this._N` under the key `E.e + '\|' + nx + '\|' + ny`, and `march` reuses one buffer `this._mb`; both are host state, not part of the function | `mutant-generator.js` (`field`, `march`) |
| The grid step is 0.9 dp, or 1.5 dp when the slot is wider than 200 dp; the grid starts 7 dp left and 13 dp above the slot and reaches `over + 31` dp below its top edge plus height | `mutant-generator.js` (`field`) |

| *Settled by [B-03](../backlog/B-03-generator-port.md):* the Kotlin port in `mutant-core/src/commonMain/kotlin/io/github/youndie/mutant/gen/` reproduces all 243 golden paths of 27 instances (every profile, both grid steps, `calm` 0.3–1.7, heat 0.5 and 1) **byte for byte** on the JVM, the iOS simulator and wasm in the browser, and every instance number to 1e-9 | `GeneratorGoldenTest` on each target, `GeneratorExactnessTest`, 2026-11-14 |
| *Settled by [B-04](../backlog/B-04-transition-geometry.md):* `venomShapes`, `pourPath` and `errBlocks` ported the same way reproduce the three venom cases of the designer's golden at every 24th of their run, five pour fronts at every 8th and five sets of error blocks — exactly on the JVM, within 0.1 dp on the iOS simulator and wasm | `TransitionGoldenTest` on each target, `GeneratorExactnessTest`, 2026-11-14 |
| `venomShapes` and `pourPath` print part of their numbers with `toFixed(1)` and part unrounded (`pourPath`'s bottom and left), unlike `pathBuf`'s `Math.round(v · 10) / 10`; the port returns shapes and numbers, and the goldens are compared as numbers, not as SVG text | `reference/design-system/mutant-generator.js` (`venomShapes`, `pourPath`) |
| Replacing the V8 `Math.hypot` by the JVM's, or shifting the field by 1e-6, changes no output string: those last-bit details fall below the 0.1 dp the paths are written in. Replacing JS rounding by `kotlin.math.round` changes 162 of the 243 paths | mutants against `GeneratorExactnessTest`, 2026-11-14 |

**Consequence 4.** The port keeps `FloatArray` for the field, rounds half up by
`floor(v · 10 + 0.5) / 10` where it must reproduce a string, and keeps every stream and the order of
its calls. Exactness to four decimals is expected for hash, rng, noise and instances; paths are held
to ±0.1 dp per coordinate, the handoff's own tolerance, because `sin`, `exp` and `pow` may differ in
the last bit between V8, the JVM, Kotlin/Native's libm and the browser under wasm (D3, H1).

### 1.5 Tokens and contrast

| Fact | Where verified |
|---|---|
| Two themes of 26 colours each, «Эфир» (dark) and «Копия» (light); type roles `display`, `text`, `osd`; grid; 13 genes with level, range, default and implementation status; 13 type profiles; 20 motion events; 33 components with tier, bodies, variants and states | `reference/design-system/mutant-tokens.json` (`mutant.*`) |
| All 28 flat contrast pairs recompute from the colours to within 0.05 of the claimed value; the lowest are «Копия» `text-disabled / background` 4.56 and `text-osd / surface` 4.70 | a WCAG 2.x relative-luminance computation over `mutant.color` and `mutant.contrast`, 2026-11-13 |
| After the Tape («Лента») one pair falls under 4.5: «Копия» `text-disabled / background` 4.4. Under the one-tick white noise stroke «Эфир» text falls to 1.7:1, which the owner accepted | `mutant-tokens.json` (`mutant.contrast-after-tape`, `mutant.tape.efir.noise-row`); handoff README, «Контраст текста» |
| The flash budget is measured by the designer: boil-over 1.5 changes per second at a luminance delta of 0.064; keyframe loss changes ≥ 0.1 on at most 2.9 % of its band | `mutant-tokens.json` (`mutant.flash`) |

| *Read by [B-08](../backlog/B-08-token-generator.md):* every colour the engine's `static TH` and the tokens both name agrees. The engine draws with paints the tokens do not carry: the divider's comet, the cocoon's two paints (in «Копия» `#cfc5aa` and `#bfb394`, which appear nowhere in the tokens), the disabled tint and darkening, and the print flag | `scripts/tokens.mjs` (`TH_TO_TOKEN`, `ENGINE_ONLY`) |
| The profiles agree with `static K` but for one field: the tokens give the pill radius "full", the engine gives it `r: 99` with no `round` flag, so its corner radii are randomised and clamped to half its side — the same on every size the kit uses, and the engine's is generated | `scripts/tokens.mjs` (`KNOWN`), `MutantTokensTest` |
| `motion.tick-hot` says 83 ms; the engine's hot tick is `125 / 1.5` = 83⅓ ms. The token is generated as written; the clock computes the engine's | `reference/design-system/mutant-tokens.json`, `MutantClock` |
| The Tape's parameters are prose in the tokens («чёрный 7.5 %, 1 dp из 3»), not numbers; [B-13](../backlog/B-13-tape.md) reads them from the engine's `tapeHtml` | `reference/design-system/mutant-tokens.json` (`tape`) |

**Consequence 5.** The brief asks for 4.5:1 *after the Tape at the worst tick*. WCAG 1.4.3 exempts
inactive components, so disabled text at 4.4 is within WCAG and outside the brief; the
accessibility item measures it on the rendered kit and puts the choice to the owner
([B-38](../backlog/B-38-accessibility.md)).

### 1.6 Fonts

| Fact | Where verified |
|---|---|
| Two families: Handjet (OFL) for display and on-screen text, sizes 14–28, letter-spacing 1 for `osd`; Rubik (OFL) for text, 11–16 sp, weights 400 and 500 | `mutant-tokens.json` (`mutant.type`) |
| The pages ask Google Fonts for Handjet 400 and 600 and Rubik 400, 500 and 700 | the `<helmet>` of each page |
| ~~*Hypothesis H2:* both are variable files on Google Fonts with Cyrillic~~ *Settled by [B-02](../backlog/B-02-fonts.md):* both carry the whole Russian and Latin alphabets. Handjet is variable on `wght` 100–900 (default 400), `ELGR` 1–2 (default 1) and `ELSH` 0–16 (default 2), 1339 code points; Rubik on `wght` 300–900 **with 300 as its default**, 885 code points | `google/fonts@2fe1e8c6b137fb13041a7cfca6fdcd8a910ea4dc!/ofl/handjet/Handjet[ELGR,ELSH,wght].ttf`, `google/fonts@2fe1e8c6b137fb13041a7cfca6fdcd8a910ea4dc!/ofl/rubik/Rubik[wght].ttf` (`fvar`, `cmap`), read by `scripts/fonts.py --report` |
| Of the 167 code points the ten pages render, every instance lacks only ▶ ❚ (the pages' own pause button) and ◄ ► (the DatePicker's month arrows) | `MutantFontCoverageTest` |
| Handjet's advances are whole multiples of a 480-unit module (8160 units per em = 17 modules) and identical at 400 and 600; Rubik widens with weight (`Н` 714 at 400, 729 at 500) | `MutantFontMetricsTest` |
| The pages set Handjet at 500 in four places but load only 400 and 600 from Google, so Chrome draws those at 400 | the `<helmet>` of each page; a count of `font-weight` per family over the pages |

**Consequence 6.** At `minSdk 24` a variable font's `wght` axis is ignored and its default instance
draws — for Rubik that is 300, so every label would turn Light on old phones. The bundled files are
static instances cut from the variable ones, as plakat-ui did with Golos Text (D9). A component that
asks Handjet for 500 gets 400, in Chrome and in Compose alike, so the references stay comparable.

### 1.7 What the design draws, and what it does not

| Fact | Where verified |
|---|---|
| 21 MVP components with all their states on one page; 7 v1 and 5 "later" components on two more | `reference/design-system/mutant-ui Компоненты.dc.html`, `… Компоненты v1.dc.html`, `… Компоненты позже.dc.html`; handoff README, «Компоненты» |
| The v1 and "later" sets are mostly static samples: the tooltip does not appear on hover, the sheet and drawer do not slide, pull-to-refresh is three frames, the pickers and the carousel are not interactive | handoff README, «Что не сделано и риски» |
| The genes `generation`, `ghost` and `hueDrift` are not implemented and `split` is unused; the brief's default "generation 2" therefore has no engine behind it | `mutant-tokens.json` (`mutant.genome.*.status`) |
| The brief's «Чистый сигнал» mode (generation 0, tracking 0, no Tape, no idle glitches) is mandatory; the design shows it only as a switch label on the settings demo | `reference/design-system/brief.md` («Доступность», «Обязательные ограничения»), `reference/design-system/mutant-ui Демо.dc.html` |
| The handoff lists departures from the brief that the owner accepted: tick 8 fps instead of 12, cool-down 480 ms instead of 240, a resting boil of the outline, and the pour, melt and venom transitions the brief does not have | handoff README, «Что не сделано и риски» |
| Reduce motion: the brief says "pause, transitions fade 100 ms"; the handoff says "the clock stops, damage stays, toggles switch instantly without transitions", and the engine's paused mode sets every transition delay to 0 | `reference/design-system/brief.md` («Два жёстких ограничения»), handoff README («Время, состояния, взаимодействие»), `mutant-engine.js` (`this.paused ? 0 : …`) |
| An instance is rebuilt when its slot changes size: drips are laid out again and the shape may jump | handoff README, «Что не сделано и риски»; `mutant-generator.js` (`genItem` depends on `w`, `h`) |

### 1.8 Pixel references and the floor

Settled by [B-05](../backlog/B-05-pixel-references-and-floor.md); `scripts/design-references.mjs` renders, `make references` re-renders.

| Fact | Where verified |
|---|---|
| The pages render in headless Chrome 157 at device scale 1 with the repository's fonts answered in place of Google's; the runtime comes from unpkg at the versions `support.js` pins | `scripts/design-references.mjs`, `mutant-core/src/desktopTest/snapshots/design/manifest.json` |
| The engine is frozen without touching it: `window.MutantEngine` is wrapped as the page assigns it, every stage gets `data-mu-t = 5000` before `start()` (the engine's own "every slot still" switch, which the Tape and the stage animations read too) and keyframe loss is forced off. A render waits until every stage is built and every visible slot drawn, rebuilds once after the fonts load — forgetting each stage's tick, or the frozen stage never steps its Tape again (§1.12) — and a second render is byte-identical | `scripts/design-references.mjs` (`FREEZE`, `pageCells`), two runs on 2026-11-14 |
| The engine draws only stages within 150 px of the viewport (`IntersectionObserver`); a page is rendered with the viewport as tall as the page | `reference/design-system/mutant-engine.js` (`start`), the same script |
| The outline's width trembles per frame by `vn(boilPhase · 3.3 + uid · 7.1, 2.5)`, where `uid` is the engine's running count of slots on the page: a host artefact with no counterpart in a kit, which [B-10](../backlog/B-10-mutant-surface.md) replaces by a seed of the instance's own | `reference/design-system/mutant-engine.js` (`drawItem`, `it.uid`) |
| A page switches to «Копия» only through its Tweaks control (the page component's `theme` prop, which also rewrites its CSS variables); the probes draw both themes, page cells are «Эфир» until a component item needs the other | `reference/design-system/mutant-ui Компоненты.dc.html` (`static FONTS`, the theme handling) |
| **The floor of the body**: Chrome drawing the generator's contour at t = 5000 against Compose drawing the port's — fill (even-odd) and ink stroke with round joins, nothing else — differs on 0.000–0.017 % of pixels (button 0.010 / 0.017, card and dot 0.000 in «Эфир» / «Копия»), a pixel differing when a channel is more than 16 apart | `BodyFloorTest`, 2026-11-14 |
| The floor separates: the button's outline 20 % thicker reads 2.644 %, another instance's outline 12.322 % | `BodyFloorTest` (`the floor separates…`) |

**Consequence 7.** Both browsers' renderers are Skia and the numbers are the same, so the body
itself costs nothing in parity: a body that reads above 0.02 % has moved, and what a component reads
beyond the floor belongs to its other layers and its text. A wrong instance passes viddik's 5 % default
by a wide margin only because it is so far off; a 20 % thicker outline (2.6 %) would pass it, so body
parity is read against 0.02 %, not 5 %.

### 1.9 The cost of the field on the desktop JVM

Settled by [B-06](../backlog/B-06-frame-cost.md), pre-registered in the item before its first number;
raw runs in `logs/b-06/`.

| Fact | Where verified |
|---|---|
| On this M1 (JDK 25), generating an instance costs at most 107.7 µs over all 27 golden cases: **green** against 0.25 ms | `logs/b-06/2026-11-14-desktop-2.log`, `scripts/b06_summary.py` |
| A warm frame of a 328 × 80 card costs 532.5 µs (ruler 10.3 µs): **grey** between 0.4 and 1.6 ms; the blob 858.9 µs, a 22 dp dot 46.8 µs | the same |
| The first frame of an instance costs about ten times a warm one (card 5.8 ms, blob 10.2 ms): it builds the static layers | the same |
| *B-50:* built off the frame, ten 328 × 80 cards appearing together cost the main thread 12.8 ms in their frame against 85.8 ms built in it, and 1.5 ms in the next, against a warm frame's 13.6 (the test runtime's frames, render included) | `logs/b-50/2026-11-14-desktop.log`, `AppearBench` |
| A first run was void: its control read 136–207 µs for 200, because two arms timed in separate blocks saw the machine drift between them; arms are now interleaved per call and the control reads 197.7–199.7 µs | `logs/b-06/2026-11-14-desktop.log`, amendment 1 in the item |

**Consequence 8.** The CPU field survives the screen: ten cards on one tick fit a desktop frame
three times over, so the components are built on it. Whether they fit a phone is grey and is the
first thing measured after the first body ([B-49](../backlog/B-49-frame-cost-on-a-pixel-6a.md)). The
first frame is the expensive one, so [B-10](../backlog/B-10-mutant-surface.md) must not build ten
instances' static layers inside one frame.

### 1.10 The body as the kit draws it

Settled by [B-10](../backlog/B-10-mutant-surface.md).

| Fact | Where verified |
|---|---|
| The renderer layers a body as `itemHtml` does — shadow, the outline-clipped group shifted by `slip` (fill, the stage's speckle, edge mosaic, dead pixels, flow lines), the highlight in two clip rectangles, the outline — and against the Components page's resting button, card and field (text made transparent, the slot rebuilt from what the engine recorded) differs on 0.008 %, 0.000 % and 0.000 % of pixels | `BodyParityTest`, `mutant-core/build/parity/*_DIFF.png`, 2026-11-14 |
| The engine samples the edge noise and the boil's noise at the slot's **stage** coordinates, so on a page a body moved within its stage would change its edge; its speckle tile is anchored to the stage | `reference/design-system/mutant-generator.js` (`field`: `vn(X · .8, Y · .8)` at scene X, Y), `mutant-engine.js` (`buildStage`, pattern `sp`) |
| Growing a field from 260 to 268 dp over 200 keys keeps every drip (3162 of 3162) and 80 % of the mosaic (7810 of 9762 blocks) and moves every flow line (0 of 1460 kept): their spacing is `w / n` | a Node run of the vendored generator, 2026-11-14 |

| *Settled by [B-11](../backlog/B-11-surface-states.md):* the eight states the Components page draws — the button pressed at heat 1, loading, success and disabled; the field focused, in error and disabled; the card disabled — differ from the kit's on at most 0.011 % of pixels, against the body's 0.02 % | `BodyParityTest`, 2026-11-14 |
| The engine redraws a pressed body on every frame while its heat changes (its key holds the heat to two decimals), but draws it at the hot tick's time; the kit redraws it on the hot tick, 12 times a second, as the handoff states the rule («пока нагрев > 0: тик в 1.5 раза чаще») | `reference/design-system/mutant-engine.js` (`frame`: the `'h' + … + heat.toFixed(2)` key), `MutantSurface` |
| Under the engine's pause a press does not boil: heat is a function of the engine's time, which stands. The kit leaves it since [B-190](../backlog/B-190-a-press-under-the-pause.md) (D24): a press under the pause is a still frame at heat 1 | `reference/design-system/mutant-engine.js` (`frame`), `SurfaceStatesTest`, `PressUnderThePauseTest` |

**Consequence 9.** The kit's body is the page's, layer for layer. Where the page relies on its host —
stage coordinates for the noise, the slot count for the tremble — the kit uses the body's own (D16).
A size change is visible in the flow lines, and the owner accepted it (D17).

### 1.11 Transitions

Settled by [B-12](../backlog/B-12-transitions.md).

| Fact | Where verified |
|---|---|
| The engine steps every transition at 83.33 ms from the moment of its event and evaluates it on every frame (`meltStep`, `drawItem`'s `fx`), so its first step shows exactly 83 ms in; a kit evaluating only on its global hot tick showed it up to a tick late. The clock exposes its frame as state, and a running transition reads it | `reference/design-system/mutant-engine.js` (`meltStep`, `drawItem`), `SurfaceTransitionsTest` |
| Under the engine's pause a transition lands at once (`fxTime()` is 10¹²); with the kit's clock standing, a transition would never move, so the theme's pause lands it at once instead | `reference/design-system/mutant-engine.js` (`fxTime`), `MutantSurface`, `mutantMelt` |
| An SVG pattern clips its content to the tile: the speckle square that starts near a tile's edge is cut there; drawn uncut it showed in the next tile on bright fills (8 pixels on a poured chip) | `BodyDrawing.kt` (`drawSpeckle`), `BodyParityTest` |
| The spec page's pour (chip, check), slide (switch's thumb over its pouring track) and boil (heat 0.5) frames differ from the kit's on at most 3 pixels each; a cell's floor is stated in pixels, 5 — the outline probes' worst — since on a 76 × 60 cell one pixel is already 0.022 % | `BodyParityTest`, 2026-11-14 |
| The venom's soft joins drawn by a graphics layer (blur 3.2 dp, alpha `18a − 8.2`) differ from the engine's SVG filter in Chrome on 0.24–0.88 % of pixels, all on the masses' outer edge, half lighter, half darker: the two blurs differ slightly and the threshold turns that into an edge a fraction of a pixel off. Without the goo it is 11.6 %, with the threshold at −6.2 it is 2.9 %, with a blur of σ 2.4 it is 0.95 % — the venom's floor, 0.9 %, sees a blur change of a quarter only just | `VenomParityTest`, 2026-11-14 |

### 1.12 The Tape

Settled by [B-13](../backlog/B-13-tape.md).

| Fact | Where verified |
|---|---|
| The engine's Tape is four layers over the stage, in this order: scanlines (black 7.5 % one dp in three, none in «Копия»); the band, 22 dp of colour between two 1 dp borders (24 in all, content-box), shown for the first 30 % of each 7 s cycle at `round(H − (phase / 0.3) · (H + 30))`; a grain tile of 44 strokes in 90 × 180 seeded `rng('tape', stage)`, translated by (37, 71) per tick; the head's noise, seeded `rng('hd', stage + tick)`, 14 strokes in the band and `round(W / 10)` along the bottom 12 dp. It steps on a fixed 125 ms tick, whatever the genome's rate | `reference/design-system/mutant-engine.js` (`tapeHtml`, `stageStep`) |
| `stageStep` returns when the stage's tick has not changed, and a rebuild rewrites the Tape bare (no band, no noise, the grain untranslated). Under a frozen time the tick never changes, so a frozen stage rebuilt — after the fonts load, or resized — keeps the bare Tape for good; the reference script forgets the tick when it rebuilds | `reference/design-system/mutant-engine.js` (`stageStep`, `buildStage`), `scripts/design-references.mjs` |
| The kit's Tape over the spec page's card at 3000, 7350, 8050 and 8900 ms — grain only; the band low; the band halfway; the band leaving past the top — differs from the engine's on 0 pixels in each | `BodyParityTest` (`the Tape over a card…`), 2026-11-14 |
| Under the band's body the kit keeps every pair of `contrast-after-tape` on the same side of 4.5:1 as the design. Its figures read up to 0.17 from the kit's: they sit up to 0.12 from the exact composite of the engine's own layers, and 8-bit pixels move it up to 0.08 more (Эфир `text / surface-variant` declared 7.3, composite 7.42, drawn 7.46) | `TapeContrastTest`, 2026-11-14 |
| The design's figures stop at the band's body. Its 1 dp top edge, 30 % phosphor, takes «Эфир» `text / background` to 2.50:1 and `text-disabled / background` to 1.74:1 on one row for 2.1 s of every 7 s; «Копия» `text-disabled / background` reads 3.10:1 there. The head's noise for one tick takes «Эфир» pairs to 1.34–1.69:1 (the tokens' 1.7 for `text`) | `TapeContrastTest` (printed), 2026-11-14 |

**Consequence 12.** The Tape costs nothing in parity, and the design's own contrast claim holds on
the kit. What the claim leaves out — the band's edge row and the noise strokes, both a single dp —
falls far under 4.5:1 in «Эфир»; whether a 1 dp line crossing a glyph counts as "after the Tape at
the worst tick" is the brief's question, and goes to [B-38](../backlog/B-38-accessibility.md) with
these numbers.


### 1.13 Icons, patterns and illustrations

Settled by [B-14](../backlog/B-14-icons-illustrations-patterns.md); references from the Icons page (`Icon_*`, `Pattern_*`, `Illustration_*`).

| Fact | Where verified |
|---|---|
| The sixteen icons are the page's `static ICONS`: one path each on a 24 grid, stroke 2, square caps, the accent of damage already in the path. Generated as path data, checked in `make check`, and built as `ImageVector`s | `reference/design-system/mutant-ui Иконки и заготовки.dc.html`, `scripts/icons.mjs` |
| The icon grid's columns are `1fr`, so cells stand at fractions of a pixel, and Chrome snaps an `<svg>` to whole pixels: drawn at the fraction instead, every icon reads worse (Menu at 48 dp: 216 px against 0) | `IconParityTest`, a scratch run on 2026-11-14 |
| 25 of 32 icon cells (16 icons at 48 and 16 dp) match to the pixel. The rest, 5–58 px, is two causes: Compose turns an SVG arc into cubics where Chrome's Skia draws conics (with Skia's own arcs Settings reads 0 and Search 3), and the two renderers anti-alias the same strokes apart — Schedule's circle is Error's byte for byte, and only Schedule differs | `IconParityTest` (`RESIDUAL`), 2026-11-14 |
| The four patterns and their quiet variants, drawn from the page's `patterns()` (snow and mosaic from `rng('pattern', name)`), match the page's swatches to the pixel once the swatch's own frame (`box-shadow: 0 0 0 2px #05060c`, filling its rounded corners) is drawn by the fixture. On «Эфир»'s dark ground the lines' 0.38 and 0.30 are 3 levels apart, under the 4 the frame's edge already moves, so those alphas are held to the page's source, not its pixels | `PatternParityTest`, `PatternSourceTest`, 2026-11-14 |
| An illustration's parts are absolutely placed bodies with per-slot overrides of their type (`data-r`, `data-over`, `data-cf`, `data-ow`, `data-wob`, `data-marks`, `data-edge`). A body's clean zone counts every nested body, kept part and text inside it, laid out even where hidden — the test card's mosaic depends on its label | `mutant-engine.js` (`makeItem`, `textZone`), `IllustrationParityTest` |
| The engine draws a stage's bodies in an `<svg>` at the stage's corner, and Chrome snaps it by a rule the DOM does not show: of the page's three columns, at fractions 0.33, 0.98 and 0.66, the last lands a pixel left of the art's rounded box. The fixture places the whole picture within a pixel of its box where it matches best | `IllustrationParityTest` (`placed`) |
| With each body generated at the engine's stage origin and drawn at its outline width (D16), the bodies of the six illustrations differ from the page's on 1–16 pixels: 1 for three of them, the rest single pixels where one body's outline lies over another's fill (the label on the cassette, the arms across the mast) — a material the floor's single-body probes never had | `IllustrationParityTest` (`RESIDUAL`), 2026-11-14 |
| The labels in Handjet read 2 px lower than Chrome's and heavier; neither `LineHeightStyle(Center, Trim.None)` nor an unspecified line height moved them. *Settled by [B-52](../backlog/B-52-display-text-against-chrome.md)* (§1.14): labels, kept parts and reels now stand within a pixel of the page's | `IllustrationParityTest`, 2026-11-14 |

**Consequence 13.** Icons, patterns and illustrations are held to their references: patterns exactly,
icons and bodies to residuals measured once and listed with their causes, never raised; their text as
§1.14 holds text.

### 1.14 Text against Chrome

Settled by [B-52](../backlog/B-52-display-text-against-chrome.md); probes `Text_<face><weight>_<size>_Efir`, a label alone at a whole pixel.

| Fact | Where verified |
|---|---|
| A line height equal to the font size is ignored by Compose, which lays out the face's own line — Handjet's is 22.35 px at 20 — with the baseline at the ascent. Blink rounds the ascent and descent (18 + 5 = 23 at 20 px) and floors the half-leading (−2), so its baseline is at 16 where Compose's was at 18: the labels' 2 px. `cssLine` lays a style on Blink's line box from the faces' metrics, nudging such a line height by 0.01 % so Compose keeps it | `MutantLine.kt` (`cssLine`), `TextFloorTest`; catalogue cmp-ui-parity «Text» |
| Handjet: ascender 7200, descender −1920 of 8160; Rubik: 935, −250 of 1000; `hhea` and `OS/2` typo agree and `USE_TYPO_METRICS` is set, so Blink has one answer on every platform | `MutantFaceMetricsTest`, the bundled files |
| On «Эфир»'s dark ground the two renderers' glyph masks differ on nearly every edge pixel: a half-covered stroke reads 186 in Compose and 102 in Chrome (largest difference 64–65 in every probe). Compose's API sets no text gamma or contrast. **The text floor** is therefore 65–87 % of a label's ink pixels differing by more than 16 — 74–80 % Handjet, 65–87 % Rubik — with Chrome's raster (no hinting, fractional glyph x) set in fixtures, 69–89 % without | `TextFloorTest` (printed), 2026-11-14 |
| What a text defect moves, the floor does not: the ink's extent. Counting pixels more than 64 levels off the ground, on all eight probes the top ink row is Chrome's exactly and the bottom row and the sides within a pixel (descender edges, whole-pixel advances). Before `cssLine`, Handjet 20 read two rows low | `TextFloorTest` (`its top row exactly`) |
| The top row is exact on macOS 27 (this Mac, where the references are rendered). On macOS 26.7.2 (the hosted `macos-latest`) CoreText puts Handjet's ink a row higher with the line box unchanged — `Text_Handjet400_18` top 3 against 4, the Demo OSD line 4 against 5, the «Копия» dialog title 76 against 77 — Rubik's nowhere. The three checks allow that row, upward only, on macOS 26 alone (`HANDJET_ROWS_ABOVE`); the exact form runs here before every merge (B-74) | `HandjetRowsAboveTest`, CI on PR #8, 2026-11-14 |
| On macOS 26 CoreText draws Handjet's strokes heavier, whatever the kit's raster settings (antialiased or LCD, hinting none, slight or normal, subpixel positioning on or off): of `−4`'s 52–60 glyph pixels at 18 sp, 43–49 reach the full ink there against 0–3 on macOS 27, on every ground, enabled or disabled (B-85, `logs/b-85/`). It is the host's rasteriser, not the kit, and likely the same cause as the row above (B-74). Only the slider's disabled value notices, because its ink on the page never reaches the full colour; that value alone is allowed there, its sides within 2 px and its colour not compared (`BOLDER_DISABLED_VALUE`, owner's decision, B-30) | `SliderParityTest`, CI on PR #18 and PR #29, 2026-11-15 |

**Consequence 14.** Kit text is laid out with `cssLine` and held by where its ink lies, not by how
many pixels differ: the top row exactly, the rest within a pixel. A parity fixture rasterises text as
Chrome does through `LocalMutantTextRaster`; the kit never overrides the platform's settings.

### 1.15 The Button, the first component

Settled by [B-15](../backlog/B-15-button.md); references `MutantButton_<cell>[Bare]_<theme>` from the Components page's Button stage.

| Fact | Where verified |
|---|---|
| The engine strokes a body's outline with its own `stroke` but draws its flow lines and speckle in the theme's ink (`C.ink`) whatever the outline; the kit drew them in the outline's colour, which the outlined button (stroke = accent) showed as 227 px | `mutant-engine.js` (`itemHtml`), `BodyDrawing.kt`, `ButtonParityTest` |
| The page takes its theme from its `data-props` default when the runtime starts; the reference script patches that attribute in its own headless tab before the runtime reads it, so «Копия» cells come from the same vendored page | `scripts/design-references.mjs` (`KOPIYA`) |
| The page gives its five filled buttons one key (`button`): a fixture looking a slot up by key finds another cell's. Each cell is drawn from its own recorded slot | `ButtonParityTest` |
| Bodies outside the label's clean zone: 0–3 px in «Эфир» and the still cells of «Копия». «Копия»'s moving bodies read 15–18 px: its fill slips 1.5 dp off the outline, and Chrome draws the speckle as an SVG pattern rasterised once and resampled under that fractional translation, where the kit draws each square exactly (moving the kit's speckle by the slip either way doubles the count). The outlined rest cells read 6 and 11: the outline over its own misregistered shadow, as an illustration's parts over each other (§1.13) | `ButtonParityTest` (`RESIDUAL`), 2026-11-14 |
| Labels: what each renderer's label adds over its own bare twin (the page's cell with text hidden, the kit's with a transparent label) starts on the page's top row in every cell, and where it covers a pixel whole its colour is the page's. A disabled label is dark ink over the no-signal body's grey stripes, adding at most about 100 levels, and a glyph's faint top rows (25–60) fall either side of text's 64: there the threshold is 50 | `ButtonParityTest` |
| Chrome does not always render the pages byte-identically: under a load average of 22–38 a third of the cells moved between two renders; on a quiet machine the Button stage's disabled and outlined cells still alternate between two renders, 25 pixels apart, 2 of them by more than 16. `make references` now passes a cell within the body's floor of its committed render and names it | `scripts/design-references.mjs` (`apart`), 2026-11-14 |
| The theme's clock asks for frames for ever, so a Compose UI test with a kit body on screen never goes idle under the test clock's auto-advance: such a test drives the clock by hand (`mainClock.autoAdvance = false`) | `ButtonBehaviourTest` |

| An icon in a component is drawn to the pixel, as the icons were (§1.13): inside an icon button's or a FAB's clean zone 0–3 pixels differ, so icon-only content is held like a body, to the floor, where text is held by its place and colour (B-16) | `IconButtonParityTest`, 2026-11-14 |

| A label-sized slot is laid out by Chrome at a fraction the engine records whole (the chip's 103.4 dp as 103), and its text zone at a fraction (83.29) where Compose's label rounds up (84): a body generated at the kit's size is another shape, and a flow line starting between the two edges is kept on the page and dropped by the kit. Fixtures give the body the engine's size and text zone (`LocalMutantSlotSize`, `LocalMutantTextZone`), null in the kit (B-17) | `ChipParityTest`, 2026-11-14 |

| A cell's bare twin hides what the page draws in `currentColor` (text, a tick's SVG) or marks `data-keep` (the radio's dot), and keeps what it paints as a background (the checkbox's burnt blocks, the radio's echo): a kit's twin must hide exactly the same, or a mark it keeps differs from the page's twin (B-18). The select stage's layer lands a pixel below its recorded slots, as an illustration column's did (§1.13): component fixtures place each cell within a pixel where its twin matches best | `CheckboxParityTest`, 2026-11-14 |

| The engine records "no text" in a slot as the zone `[-99, -99, 0, 0]` and keeps no clean zone at all; a body given that sentinel as a zone draws its no-signal band elsewhere. Page content (an `<i>`, a switch's echo) is drawn over every body of the stage, a nested body's included (B-19) | `SwitchParityTest`, 2026-11-14 |

| The engine's cocoon is not a function of time: each scene tick repaints a few of its blocks over what earlier ticks left, and a stage rebuilt at a new size lays them out again without re-applying the tick it stands at (`st.fi` outlives the rebuild). The page frozen at `data-mu-t` 5000 draws the layout plus tick 40's repaints, which the kit reproduces exactly; a running kit applies each tick it sees, as the engine does, so two clocks that skip different ticks diverge (B-22) | `CocoonParityTest`, `CocoonBehaviourTest`, 2026-11-14 |

| Compose lays Rubik 14/500 out in whole-pixel advances (the tabs' labels 65, 60 and 44 wide) where Chrome's are fractional, so labels in a row drift apart by up to 2 px by the third; the page's tab bar is a literal 62 dp under a label the kit lays out 65 wide — the kit follows its label, the fixture gives the page's width (B-23) | `BarTabsParityTest` (`SIDE_RESIDUAL`, `LocalTabBarWidth`), 2026-11-14 |
| A fixture's child taller or wider than the cell it is drawn in is centred on it by Compose (the tabs' 48 dp target in the page's 46 dp cell stood a pixel high): such a child is laid out unbounded, `wrapContentSize(TopStart, unbounded = true)` (B-23) | `BarTabsParityTest`, 2026-11-14 |

| The engine's text zone is the union of the slot's text runs (a `Range` over each text node: the glyphs' box, not the line's), its `svg` icons, and every `[data-mu]` and `[data-keep]` inside it — a nested body such as the navigation bar's pill counts — plus 6 dp across and 2 down. A kit that marks the boxes around them (an item's column) measures another zone: the navigation bar's stood 2 dp lower and 3 wider until the icons, labels, badge text and pill were marked one by one. A fixture that hands the engine's zone to the kit (`LocalMutantTextZone`) cannot see this, so a component's own zone is read through its probe against the engine's record (B-24) | `NavigationBarParityTest`, 2026-11-14 |
| What a bare twin keeps inside the clean zone (a nested pill, a badge's plate and ring) is read by neither the body's count (outside the zone) nor the content's (what text adds over the twin): the twins are compared with each other inside the zone (B-24) | `NavigationBarParityTest`, 2026-11-14 |

| A body's `overhang` bounds its hanging drips, not the drops that tear off them and fall: under a list tile with `over` 6 drops reach 9–11 dp over a 30 s run. The list tile draws the same at 6 and at 8 (frozen and over 30 s); 10 moves its frozen frame (B-25) | `ListItemParityTest`, 2026-11-14 |
| `captureToImage` keeps what a node draws past its bounds (a layer is not clipped to its node): a badge's ring, drawn 1.5 dp outside its plate, is in the plate's capture, and a colour read over the whole capture reads the ring (B-25) | `ListItemBehaviourTest`, 2026-11-14 |

| A play stepped at 83.33 ms lands a step after its duration when the duration is not a multiple of the step (200 ms at 250, 330 at 333.3): a composition that asks for frames while the time is under the duration, not while the progress is under 1, stops short and stands there — the snackbar at 83 % for ever (found by its test), the melt at 76 % when a frame falls in its last 3 ms ([B-54](../backlog/B-54-melt-stalls-short-of-its-end.md)) (B-26) | `SnackbarBehaviourTest`, 2026-11-14 |

| The theme's clock starts at 0 and, under reduce motion, stands there; what the engine moves by the scene's tick from off-stage (the rewind band, `(fi · 12) % (pw + w) − w`) stands off-stage too, so a paused indeterminate progress showed an empty track. Such a picture is paused at the page's still frame (tick 40, the engine's 5 s) instead of the clock's tick (B-27). Tick 40 left the track empty too wherever `w + 96` nearly divides 480 (29 of 177 widths); since [B-191](../backlog/B-191-progress-under-the-pause-and-the-steps.md), on the designer's answer 13, the paused band stands centred on the track, its solid half from the middle on | `ProgressBehaviourTest`, 2026-11-14 |

| A dialog pauses the screen (the brief's `pause`): the screen's clock takes a hold (`MutantClock.holds`) while anything of it shows, so every body behind it stands, and the window runs on a clock of its own, frozen with a frozen screen — one clock per screen (D6) bent for an overlay, not broken (B-28) | `DialogBehaviourTest`, 2026-11-14 |
| A fractional `line-height` (14 × 1.45 = 20.3) lays two lines 40.6 tall in Chrome and 40 in Compose; what follows stands at 112.6, and the engine paints a body there at its slot's rounded 113 while the DOM's words stay at the fraction a row above. A kit that lays body and words together can match one: the dialog's text box takes its lines' CSS height rounded (the body exact) and its actions' words are listed a row low (B-28) | `DialogParityTest`, 2026-11-14 |

| A test over "every node that …" passes over none as readily as over all: an empty state given no width lays its words a letter a line, thousands of dp tall, and stacked above the measured one it pushed that out of the test window, so the sweep's checks ran over nothing and a mutant survived them. Such a check asserts how many it measured (B-29) | `EmptyStateBehaviourTest`, 2026-11-14 |

| The Demo page composes its three screens with spacing and sizes of its own that the components page does not draw (a 62 dp navigation bar against 68, a tab row of 32 and 4 against 36 and 6, a bar title at 22 against 26, radio labels 10 dp off against 12, a switch's label before it, a progress value read as a time). The owner decided on 2026-11-14: the screens are rebuilt from the public API as it stands, each component held to its own cell; the screens' references are for the eye, with no floor gate (B-37) | `DemoScreensTest`, `sample/build/demo/` |

**Consequence 15.** A component is held to its page as the button is: drawn through its public
function, each body from its own recorded slot, bodies outside the clean zone to the floor or a
listed residual, labels by what they add over the bare twin.

### 1.16 Accessibility, measured on the kit (B-38)

Measured on 2026-11-14 over the sample's three demo screens (B-37), 360 × 720, both themes, Russian, at every scene tick
of a Tape cycle (56 ticks of 125 ms), on desktop. A word's contrast is its strongest pixel against the commonest pixel
of its box, after the Tape, at the worst tick; a flash is a pair of ticks whose luminance changes by 0.1 or more, its
darker side under 0.8, counted as a share of the screen.

| Measure | «Эфир» | «Копия» | Rule | Held by |
|---|---|---|---|---|
| Active words' contrast, worst tick: library / recording / settings form / dialog | 5.81 / 6.29 / 6.48 / 7.44 | 4.70 / 5.36 / 4.70 / 5.43 | ≥ 4.5 (brief) | `AccessibilityTest` |
| Disabled words' contrast, worst frame off the band's borders (B-136; under a border, recorded apart: down to 3.89 «Эфир», 4.30 «Копия») | 4.83 (button) – 6.33 | 4.43 – 4.52 (12.22 the button) | exempt (WCAG 1.4.3, the owner's answer), held ≥ 4.3 off the borders | `DisabledContrastTest` |
| Largest change between ticks, share of a screen | 1.8 / 2.1 / 1.1 / 0.2 % | 7.3 / 4.3 / 4.1 / 0.3 % | ≤ 25 %, once a second (the kit's rule) | `AccessibilityTest` |
| Saturated red, tick by tick | the settings form's 559–653 px: the error's offset outline and the radio's dot move with their bodies, never on and off | 448–520 px likewise; the recording screen's outlined button casts a misregistered shadow in red that comes and goes, 2–157 px, under 0.1 % of the screen — the design's print effect, drawn as the engine draws it | the red never flashes | `AccessibilityTest` (the form's area within a quarter of its peak) |
| States in grey (pixels more than 16 levels apart) | every pair ≥ 99 px but a loading button against one at rest, 8 px | every pair ≥ 86 px but loading against rest, 30 px | distinguishable in grey (brief) | `StatesAndSemanticsTest` (the loading pair listed) |
| Semantics | the button, icon button, chip (a selectable checkbox, as Material's FilterChip), checkbox, radio, switch, field (editable text), tabs, progress (its value or indeterminate), card: their Material counterparts' roles and states; a body adds no node | — | Material's (brief) | `StatesAndSemanticsTest` |

A loading button's seek bands are a light acid over the acid body, under 16 grey levels from it, running or paused: in
grey a loading button reads as one at rest, and differs by its motion and its reader's progress. The Tape's grain is
1 dp strokes 1 or 3 dp long and the head's noise 1 dp lines 6–46 dp long — thinner than the brief's 1.5 dp, longer in
one direction; the design draws them so.


### 1.17 What a consumer does to the kit (B-40)

On 2026-11-14 an adversarial pass over every public component on the desktop test runtime, then a real window
driven by posted AWT events, found 22 defects: one throw (the navigation bar in unbounded width), five wrong for
every user (no visible focus on the text button, a Success button and the dialog's two hidden Tab stops; the text
field's narrow click target and its caret standing after the text), eight for a class of input (long dialog text and
labels, overflowing tabs, font scale, focus through Loading, right-to-left text, no hover), eight small. Every one
is an item, B-56…B-70, with its finding's table in [B-40](../backlog/B-40-qa-pass-and-real-window.md); the sweeps
stay as tests in `mutant-core/src/desktopTest/kotlin/io/github/youndie/mutant/qa/`, each defect an ignored test
named by its number. The real window confirmed the arrow cursor, the clean click, the Tab order, the wheel at 30 px
a notch, resize and the scrim taking a press; it does not see the operating system's own delivery of input.

---

### 1.18 The frame's cost on the JVM (B-72)

| Fact | Where verified |
|---|---|
| In a real window on this Mac, the sample is red on the Components page (27 % of intervals over 33 ms) and on the Demo page, grey on the Recording and Settings screens alone. A control adding 10 ms inside the frame reads 10.04 ms | `FrameBench`, `logs/b-72/`, 2026-11-14 |
| The tick frame costs 60–80 ms on the main thread, the frames between ticks 0.1 ms: each body rebuilds its distance field and outline on every tick, in the draw (`gen.frame` 77 % of the event thread's samples) | JFR, `logs/b-72/…-profile-components-rest.txt` (B-75) |
| A resting tick's frame is a function of its time alone. Built a tick ahead off the main thread, it took the Components page from 31.5 to 59.6 frames a second, and intervals over 33 ms from 26.9 % to 0.47 %. The field cache is shared only once a frame would read it and never write it (`FieldCache.settledFor`) | `TickFramesAheadTest`, `logs/b-75/` (B-75) |
| Skiko replays the frame's picture on a thread of its own. On the Demo page that thread is the bound (`drawPicture` 898 of 1 050 native samples), and a recorder timing the main thread alone does not see it | JFR, `logs/b-72/…-profile-demo-page.txt` (B-76) |
| On the Demo page Skia's render thread is the bound, at 87–92 % busy and 19.8 ms of CPU a frame. The Tape is 3.2 ms of that, the screens' rounded clip 0.6 ms, the screens' content 15–16 ms. A fixed load of paths reads the same on a blank page and on the Demo page (3.1–4.5 ms) | `FrameBench` render sampler, `logs/b-76/` (B-76, B-77, B-78) |
| Of a body's drawing on the render thread (11.07 ms a frame for the Library screen's bodies ×3), leaving out the outline's stroke saves 4.03 ms, the contour clip 2.07, the speckle 1.32, the highlight 0.61, everything else under 0.3. A load of paths adds 3.7 ms on a blank page but 1.5 over the bodies: on this thread costs do not add, and a reading is what a removal saves in its scene | `bodyRenderBench`, `logs/b-78/` (B-78; items B-79, B-80, B-81) |
| *Refuted* (B-79): the outline's cost on the render thread is not the stroking. Filling the stroke's outline, built once per tick, cost 11.09–11.10 ms a frame against 11.16 stroked, while leaving the outline out saved 4 ms. The render thread pays for rasterising the contour every frame, and a body's picture changes only at a tick (B-82) | `bodyRenderBench`, `logs/b-79/` |
| A resting body under a running clock is drawn from an image rasterised once per tick, off the main thread, with its frame (B-82). The render thread's CPU for the Library screen's bodies ×3 fell from 20.97 to 1.67 ms a frame (60 fps from 41), and the rasterising moved about 0.6 s of CPU a second to the background threads. The image differs from the vectors only at anti-aliased edges: more than 16 levels on 0.013–0.03 % of pixels. A held or paused clock still draws vectors (B-83) | `bodyRenderBench`, `BodyAsImageTest`, `logs/b-82/` |
| A body first built under a clock that stands (held by a dialog, paused) is rasterised with that build and draws its image from its first frame, so nothing switches under the stopped clock (B-83). The sample's Demo page, whose dialog holds all three screens' clock, fell from 14.56–18.42 to 4.77–5.31 ms of render CPU a frame, 59–60 fps from 43–55; the blank page's stand check did not move | `FrameBench`, `StandingClockImageTest`, `logs/b-83/` |
| On battery the Mac's render cost moved between runs of identical code (the Demo page at 19.45 and 11.27 ms a frame), and the vectors cost twice their mains-power figure. Measurements of the render thread are compared within one run, scenes interleaved | `logs/b-82/2026-11-15-demo-*.log` |
| In the browser the bodies' builds off the frame have one thread: `Dispatchers.Default` is the page's own queue, and it runs up to sixteen coroutines a task (B-73). A body off the screen builds no tick (B-87): one whose box, 64 dp wider, misses the window draws nothing, and one that misses only a box inside it draws its last picture. Its layout callback tells it when it shows again, a layer's slide included. In the browser each build ahead is a task of its own at the scheduler's `background` priority. The Components page went from 24 to 60 frames a second there, with no long animation frame left; on the JVM its work p95 went from 6.1 to 5.3 ms | `OffScreenTicksTest`, `scripts/frame-bench-browser.mjs`, `logs/b-73/`, `logs/b-87/` |
| The Tape's cost on the render thread was its grain: 2.20 of four Tapes' 3.33 ms a frame. Neither a clip a tile, nor a path a stroke, nor an image a tile helped (B-77). At a whole density the grain is one fill repeating its tile, rasterised once, to the byte the strokes' own: four Tapes 3.71 → 1.54 ms, the Demo page's render 4.10 → 2.54 ms. At a fractional density the strokes stayed, and a Pixel 6a (2.625) drew about 1 600 a frame, 13 % of its main thread in Chrome; since B-107 the fill repeats the fewest tiles that are whole pixels (4 × 2 there), and the scanlines are one fill too, to the byte the rects' own. On that phone, warm and charging, the Tape went from about 8 ms a frame to about 2. Cool, the page ran at 46 frames a second; ten cards ran at 22 in Chrome against 59 natively, as Android takes the bodies' builds and raster off the main thread and the browser has one: in Chrome Skia's own CPU work is 65 % of that thread, its WebGL calls 3 % (B-108) | `tapeRenderBench`, `TapeGrainTileTest`, `TapeScanlineFillTest`, `logs/b-77/`, `logs/b-107/` |
| In Chrome on a Pixel 6a ten cards cost about 48 ms a frame, and the bodies' decorations about 23 of it: the speckle 12.5, the mosaic, dead pixels and flow lines 10.5 together, the clip to the contour about 16 with every decoration kept. Read in frames a second the parts looked as if they did not add; in ms a frame they do (B-111). Without the four the page reached 60. The clip and the speckle are the dearest, as on the desktop (B-78): hundreds of small draws a card a tick under an anti-aliased clip. Drawing the still ones once into an image bought nothing on the phone proxy, though the image was used: it covers every pixel of the body's box, and the squares a few (B-112, reverted). There the speckle saves 12.7 ms, the flow lines 7.8, the dead pixels 4.2 | `logs/b-111/` |
| On the Mac the phone proxy slowed ×2 runs ten cards as the Pixel 6a does (17–18 frames a second, work p50 about 40 ms); slowed ×4, as B-107 set it, it was four times slower and past an edge: between ×2 and ×2.5 the cost triples. At the phone's load no body reaches its tick image (B-82): the builds ahead at `background` priority never run, and at `user-visible` they run, miss their tick and are thrown away (B-121, B-122). So every body is drawn as vectors in the frame, and what speeds that path counts. Compose on Skiko draws an `ImageBitmap` by `Image.makeFromBitmap` on every call, copying a bitmap not marked immutable. The speckle, mosaic and dead pixels drawn once into an image on the surface's grid (B-117) and that image marked immutable (B-119) took the kit from 16.3 to 23.3 frames a second, all decorations kept; a frozen clock draws as before, so no reference moved. The outline's stroke is now the dearest part, about 12 ms a frame; as a filled path it cost 22 ms more (B-120, B-121). A body's image at a fractional density stood up to half a pixel off its vectors until it was rasterised on the surface's grid (B-115) | `logs/b-116/`…`logs/b-123/`, `StillLayersOnceTest`, `BodyAsImageTest` |
| A body's image drawn ahead covers the window and half a window more on each side, cut on the whole image's pixel grid (B-86). Scrolled past that band between two ticks, the body draws its vectors until the next tick's image covers it. The 360 × 3 000 dp card went from 86–93 to 28–30 ms a tick on the M1, its rasterising from 700 to 190 ms a second; a body inside the band rasterises as before. A cut image differs from the whole one on 0.002–0.019 % of pixels by more than 16 levels, because Skia rasterises an edge a little apart on a smaller canvas | `tallBodyBench`, `TallBodyBandTest`, `logs/b-86/` |
| On the owner's Pixel 6a (Android 17, 60 Hz, a release host), B-49 read the brief's three targets. A list of 100 rows with drawn icon tiles scrolls at 60 frames a second with no vsync skipped. Ten cards under the Tape at 12 ticks a second skip 1.51 % of vsyncs, because a third of their ticks are built in the frame (B-89); at the kit's own 8 ticks they skip none. The worst instance takes 283 µs. The phone is 2.68 times the M1 on a warm frame, not B-06's assumed 4, and 3.53 times on an instance. `FrameMetrics.TOTAL` includes Android's buffer pipeline (an empty screen reads 28 ms at p50), so a screen is judged by the vsyncs it skips and its main thread's work, by the owner's late amendment | `bench-android`, `scripts/device-bench.sh`, `scripts/b49_summary.py`, `logs/b-49/` |
| A frame built ahead is found by its tick's time, by equality, so the next tick's time is the clock's own product `(restTick + 1) * stepMs` (B-89). `restTimeMs() + stepMs` misses it in the last bit on a third of the ticks at 12 or 24 ticks a second, and those ticks were rebuilt on the main thread: on the Pixel 6a ten cards at 12 ticks a second went from 1.70 % of vsyncs skipped and a main-thread p95 of 26.9 ms to 0 % and 3.1 ms | `MutantClockTest`, `TickFramesAheadTest`, `logs/b-89/` |

## 2. Decisions

### D1. `Mutant` in every identifier, package `io.github.youndie.mutant`, one module `mutant-core`, no Material

Bare `Button` or `Card` collide with foundation and Material in every consumer; the line's kits all
prefix. Only `foundation`: the design follows Material's inventory, never its rendering.

**Deviation from the handoff.** The handoff proposes two modules, a Compose-free `mutant-core` with
the generator and a `mutant-compose` with the components. One module ships instead, with the
generator in `io.github.youndie.mutant.gen` importing nothing from Compose — held by a test that
scans the package's imports. The reason: a second module is a second set of six coordinates
(desktop, Android, iosArm64, iosSimulatorArm64, wasm-js, the root) for a consumer nobody has named.
The package boundary keeps the split a move rather than a rewrite if one appears.

### D2. Numbers for the generator, pixels for the components

The generator's goldens are emitted by the vendored JS in Node and compared on every target
([B-03](../backlog/B-03-generator-port.md), [B-04](../backlog/B-04-transition-geometry.md)).
Components are compared with references the repository renders from the pages in headless Chrome,
at a fixed engine time, with the fonts served from the repository, and the parity floor is measured
before the first component ([B-05](../backlog/B-05-pixel-references-and-floor.md)). Rejected:
pixels only (a wrong drip period would pass every still frame), and numbers only (the layers, the
clean zone and the content are not in the generator).

### D3. The generator is ported 1:1 to common code, structure kept, names spelled out

`hash`, `rng`, `h21`, `vn`, `ucap`, `smin`, `bodyD`, `genItem` with `edgeMos`, `marks` and
`deadPx`, `prims`, `dyn`, `bubbles`, `field`, `march`, `pathBuf`, `markPath`, `errBlocks`,
`pourPath`, `venomShapes` keep their structure and their call order, so a golden that differs
points at one function. Short names get full ones (`g` → instance, `el` → slot, `sh` → shape). The
output is path nodes, not SVG strings; the test serialises them the way `pathBuf` does to compare.
The field stays `FloatArray`; rounding where a string is reproduced is half up (§1.4). The
per-instance caches and the march buffer belong to the instance's drawing state, not to a global.

The goldens are Kotlin sources in `mutant-core/src/commonTest/kotlin/io/github/youndie/mutant/gen/goldens/`,
one per instance, written by `scripts/gen-goldens.mjs` from the vendored generator and checked by
`make check` with `--check`; they carry `@file:Suppress("ktlint")`, because every byte of them is held
to the generator instead, and a formatter that rewraps them would make that check fail on no change.

### D4. Where the golden and the engine disagree, the engine wins

The hot frames' flow lines are drawn with boil' because `drawItem` draws them so and every page
shows that. The three entries stay listed in `scripts/design-golden.mjs` with their cause; the
designer is told, and nothing in `reference/` is edited.

### D5. A body is drawn behind its content, outside its bounds, and never affects layout

The layers are the handoff's, bottom to top: misregistered shadow, the red offset outline of an
error, the clipped group (fill, speckle tile, edge mosaic, dead pixels, corruption blocks, flow
lines, loading bands, shifted by `slip`), the highlight, the outline whose width trembles ±12 %·boil.
Drips hang up to `over` dp below the slot; they are drawn without clipping and take no space. The
clean zone is the content's own bounds plus 6 dp horizontally and 2 dp vertically, measured from
the content's layout, not guessed ([B-10](../backlog/B-10-mutant-surface.md)). Paths are rebuilt
once per tick and remembered by tick number.

### D6. One clock per screen, in milliseconds, quantised to the tick

A kit clock, provided once per screen, drives every body: rest at 125 ms ticks (boil phase
`floor(t / 125) mod 4`), a pressed body at 83 ms; pausable and scaled like the engine's. No
component reads system time ([B-07](../backlog/B-07-kit-clock.md)). Only visible instances
animate.

### D7. Reduce motion is the handoff's pause, a property of the theme

The clock stops, damage stays, toggles switch instantly (§1.7). **Deviation from the brief**, which
asked for 100 ms fades: the handoff is the later word and the engine implements it. The theme
takes `reduceMotion: Boolean?`, whose default reads the platform, so a fixture can force either.
«Чистый сигнал» is a separate theme switch that turns off the Tape and idle glitches and does not
stop the clock ([B-09](../backlog/B-09-theme.md)).

### D8. Tokens are generated from `mutant-tokens.json`

A Node script reads the tokens file and writes one committed Kotlin file; the gate runs it with
`--check`, and a test reads the JSON a second time independently. No literal colour, size or
duration in a component; the generator's own constants are copied as named constants and held by
the frame goldens ([B-08](../backlog/B-08-token-generator.md)).

### D9. Fonts: Handjet and Rubik as static instances

Cut from the variable files at the weights the tokens and pages use — Handjet 400 and 600 with
`ELGR` 1 and `ELSH` 2, Rubik 400 and 500 — by `scripts/fonts.py`, which pins the upstream files by
SHA-256 in `scripts/fonts.lock`; each OFL text ships beside them. A coverage test holds every
instance to every character the pages render ([B-02](../backlog/B-02-fonts.md)). Rubik 700, which the
pages load, is used by no element and is not bundled. The four files are 870 KB together, Handjet's
two being 222 KB each for 1339 code points; subsetting is allowed by the OFL but not done until a
consumer asks.

### D10. The Tape is one layer over the screen, not a layer per component

Scanlines, grain, the tracking band every 7 s and the head noise are drawn once, over the screen
root ([B-13](../backlog/B-13-tape.md)). Keyframe loss, which the handoff calls optional, waits
([B-48](../backlog/B-48-keyframe-loss.md)).

### D11. Documentation, code and commits in English; the brief stays in Russian

Like the other three kits. The design's demo strings are copied verbatim into fixtures. The brief
requires the demo screens to be checked in both Cyrillic and Latin, so the sample switches
language. It opens in English, the owner decided on 2026-11-14 when the sample was built
([B-37](../backlog/B-37-demo-screens-and-sample.md)); Russian is one switch away.

### D12. Built and verified on the Mac; the repository stays out of mutagen sync

Goldens and references are a claim about the rasteriser that recorded them, and the line's code
gate runs on `macos-latest`. The project is not in `mutagen sync list`.

### D13. The cost of the field is measured before the components are built on it

The handoff names it the main risk: the field is recomputed on the CPU every tick for every visible
instance. The cost of `genItem` and of one frame is measured on the desktop JVM as soon as the port
exists, and on a Pixel 6a as soon as one body is drawn, against the brief's target (60 fps, ten
cards with the Tape, a 100-item list, an instance in ≤ 1 ms) and its declared retreat order
([B-06](../backlog/B-06-frame-cost.md); the phone half is [B-49](../backlog/B-49-frame-cost-on-a-pixel-6a.md)). Rejected: measuring last, as plakat-ui did — there the
architecture did not depend on the answer; here it does.

### D14. MIT licence

The owner chose MIT on 2026-11-14, like toon-ui and plakat-ui ([B-39](../backlog/B-39-licence.md)).
The bundled fonts keep their OFL, each with its licence file beside it.

### D15. What the design does not draw is designed here *(as plakat-ui's D13)*

The owner decided on 2026-11-14: "как в plakat-ui" ([B-43](../backlog/B-43-undesigned-behaviour.md)).
The appearance and gestures of the v1 set (tooltip on hover and long press, sheet and drawer
sliding and dragging, pull-to-refresh as a gesture, menu opening), the snackbar's show and hide,
hover on desktop, and what «Чистый сигнал» changes are built from what the design does say: the
brief's event table (`osd-in`/`osd-out`, `pause`/`play`, `switch`, `hiccup`, …) and its eight motion
rules, the Principles page, the tokens, and the nearest drawn component that shares parts. Such a
behaviour has no Chrome reference; its goldens are recorded when it is built and revised in the
owner's review round over the finished sample, and its item lists every number it chose that the
design does not state. Everything the design does draw stays held to the design. The loop does not
wait for review of what is designed here.

Numbers chosen for the button under this decision (B-15): the label 24 dp from the body's sides (the page draws fixed widths); a text button's divider as wide as its label (the page draws 76 dp under a 69 dp label in a fixed cell); a disabled text button greys its label; keyboard focus draws the field's brackets and calm (`focused`). Given no width, the filled button is as wide as its widest state in every state: the check Success adds (24 dp and its 8 dp gap) keeps its room at rest, pressed, loading and disabled, so no state change moves the width or regenerates the body under D17; a new label is a new measurement, and a caller's width wins ([B-203](../backlog/B-203-a-button-as-wide-as-its-widest-state.md), the designer's answer 35).

### D16. A body's origin is its own

The kit generates every body with its slot at (0, 0) and anchors its speckle to the body, so a body that
moves — scrolled, animated, re-laid out — keeps its edge, its noise and its speckle. On the page the
same body moved within its stage would re-noise its edge and slide under the speckle; no page moves one,
so nothing the design draws is lost. The outline's tremble is seeded by the instance's hash instead of
the engine's running slot count. Parity fixtures pass the page's stage coordinates and its drawn outline
width to compare like with like ([B-10](../backlog/B-10-mutant-surface.md)); a fixture drawing a whole
composition does it through `MutantSurface` itself, with two internal locals the kit leaves null
(`LocalMutantSlotOrigin`, `LocalMutantOutlineOverride`, [B-14](../backlog/B-14-icons-illustrations-patterns.md)).

### D17. A size change regenerates the body, flow lines included

The owner's choice of 2026-11-14 ([B-51](../backlog/B-51-size-change-moves-the-flow-lines.md)). A body follows its slot's size, as the generator does. A change of size is a discrete event (a new label, a new value), and the body is generated anew at the new size. The drips and most of the mosaic stay; every flow line moves, because the lines are spaced `w / n` (§1.10). Rejected:

- holding the flow lines on a width rounded up to 32 dp;
- generating the whole body on a 16 dp bucket and stretching it.

Both are departures from the generator, for a jump the engine itself makes.

### D18. A running body's webs between drips are steadied

The owner's remark of 2026-11-15 ([B-84](../backlog/B-84-drip-webs-flip-every-tick.md)): two drips close together stick and come apart at every step, as the engine draws them too. Between two drips the smooth union leaves a flat saddle, and the boil's 0.8 dp tremble there moved the web's edge several dp at every boil phase. On a running clock a point keeps `(1 − h)²` of the tremble, h the blend weight of the strongest merge it lies in. Between drips standing still, the web jumps 3 dp or more on 444 of 61 963 ticks, against 6 450 as the engine draws it and 246 with no boil at all. A resting frame costs 3.6 % more. A frozen clock draws the engine's field, so no parity cell or golden moves. Rejected:

- damping by the field's slope: the saddle is not flat enough; 4 623 jumps at a threshold that keeps the edge;
- spacing the drips apart, which changes every body's shape and the generator's goldens;
- a plain `min` between drips, which cuts the webs to creases;
- smoothing the field between ticks, where the design steps on purpose.


### D19. Three owner-named exceptions to the 48 dp touch target

The owner's choice of 2026-11-15 ([B-33](../backlog/B-33-menu.md), [B-34](../backlog/B-34-navigation-drawer.md), [B-45](../backlog/B-45-date-and-time-pickers.md)). The kit's rule is a touch target of at least 48 dp by the slot. Three places in the design draw shorter targets, and the kit keeps the design's drawing there:
- the menu's 36 dp rows;
- the navigation drawer's 42 dp rows;
- the calendar's 36 × 36 day cells.

In each, a row or cell is the whole target. The menu's rows and the calendar's cells tile their column or grid with no gap, so every press inside lands on one; the drawer's rows stand 4 dp apart, as the page's do (B-34). An accessibility test pins each exception, and a sweep fails on a target under 48 dp that is not on this list.

Rejected:
- 48 dp rows with the 36 or 42 dp drawing inside: it departs from the page's spacing;
- 48 dp cells for the calendar: its seven columns would make a 336 dp row, wider than its 284 dp body.

### D20. The code is primary from 2026-11-16 *(as toon-ui's D13)*

The owner, on 2026-11-16: changes to the design system are to be made here, not through Claude Design and an archive carried across. Until then the vendored export was the ruler (D3, D4), and a newer one was to be re-imported whole ([B-42](../backlog/B-42-re-import-the-design.md)). From now on the design is where the kit started, and the kit moves from here.

- **Nothing is re-imported.** B-42 is dropped. A later export from the designer is read as a list of proposals, each taken here as an item, never as a snapshot to regenerate everything from.
- **`reference/design-system/` stays as it is, unedited**, as the record of where each token, shape and component came from.
- **The checks against it stay**, as a regression net for everything that has not left it: the generator's goldens (`gen-goldens.mjs`), the vendored golden (`design-golden.mjs`), the tokens (`tokens.mjs`), the icons (`icons.mjs`), parity against the Chrome references and the library's goldens.
- **A departure is recorded, not loosened.** A change the owner asks for is made in the code, in an item. Where it moves what a check holds against the design, the check's own list of known entries names it, with the item, as `design-golden.mjs`'s `KNOWN` already does. A cell it moves past the parity line goes into a departures registry with an `asDesigned` switch (cmp-ui-parity §7); one under it is a dated note with its numbers. The moved goldens are re-recorded and looked at.
- **The departures registry**: the cells a departure moves past the parity line, each held as designed by its switch in the parity test, and by its own tests as the kit draws it.

  | Item | Cells | The kit draws | Switch |
  |---|---|---|---|
  | [B-186](../backlog/B-186-the-caret-and-the-fields-mosaic.md) | `MutantField_Focused_*`, `MutantSearchBar_Focused_*` | the block caret 2 dp past the text, not the page's 8 dp gap (the designer's answer 17) | `LocalMutantTextFieldAsDesigned` |
  | [B-204](../backlog/B-204-the-navigation-badge-grows-right.md) | `MutantNavigationBar_Rest_*` | the badge's plate from the 22 dp icon's right edge, growing right, not its right edge 6 dp in from the 56 dp icon box's (the designer's answer 36): the page's «3» 5 dp further right | `LocalMutantNavigationBarAsDesigned` |

  B-186 also moves what no cell sees, as the cells take the engine's clean zone: a field's own zone is its input area, not its texts (answer 18), so an empty field's body loses the marks right of its placeholder.
  [B-199](../backlog/B-199-no-mosaic-in-the-clean-zone-when-pressed.md) moves no cell either, 2026-11-23: a pressed body's running mosaic draws no tile that meets the clean zone (answer 25), where the engine shifts it in over the label. No parity cell and none of the generator goldens' 27 hot frames has such a tile; `runningMosaic(asDesigned = true)` draws the engine's, and `GeneratorGoldenTest`'s `B199_ZONE_FRAMES` lists the hot golden frames it moves (none).
- **Rejected**: keeping the vendored files as an editable source of our own, edited here and regenerated from. The generator would then live twice, as JavaScript and as its Kotlin port, and every change would be made in both and proved equal. The Kotlin is what ships.

---

### D21. A body's field grid grows past the engine's where its contour reaches the edge

Found while building the demo, 2026-11-16 ([B-97](../backlog/B-97-a-risen-sheet-tears-a-wedge.md)). The engine samples a body's field on a grid 7 dp past the slot's sides, 13 dp above it and 18 dp below its drips' reach. A tall, wobbly body bulges further: the bottom sheet, a blob 376 × 271 dp at wobble 1.6. Its contour was cut at the grid's edge, and the march left open chains whose joins crossed the body (stitches of 195 dp against a normal 2.4). Under the even-odd fill the crossing showed as a wedge-shaped hole, the screen under the sheet showing through.

The engine draws the same hole: the port is 1:1 (D3), and the fault was there from the sheet's first commit.

- **The fix.** A body found at the edge gets 8 dp more grid on every side, then twice that, up to 32 dp.
  - The cells are added by index, so the engine's cells keep their positions to the bit.
  - A body that never reaches the edge draws exactly as the engine does, at the engine's cost.
  - The growth happens only on the frame's thread. A frame built ahead off it (B-75) never grows the grid, and a cut one is not taken.
- **The goldens it moves.** Of the vendored goldens, three frames of `fab-b` (5 000, 12 100 and 15 990 ms) reach the edge. Their contours are now the port's own, listed as `B97_CUT_FRAMES` in the generator's golden test, which computes the list and fails on any change to it.
- **Rejected:**
  - a wider grid for every body, at up to 40 % more field cells for a small button;
  - a margin estimated from the shape's amplitudes, which padded nearly every golden body that never reaches the edge;
  - a flat clip at the grid's edge, which closes the contour but cuts the body's true shape.

### D22. Movement runs on every frame; the boil keeps its tick

The owner's remark of 2026-11-16 ([B-100](../backlog/B-100-motion-on-every-frame.md)): the boil and the drips are bound to the tick, but movement need not be — «флаг конфигурации и по умолчанию перемещение не ограничивать».

The design steps every transition at the hot tick (83.33 ms, 12 a second: pour, slide, melt, the rise of a dialog, a sheet, a drawer and a snackbar, the tabs' bar, the calendar's drop) and the venom run at 24 a second. `MutantTheme(steppedMotion = false)` is now the default: these advance on every frame of the screen's clock. What a body looks like stays on its tick (D6): its boil, drips, mosaic and the Tape change only there, and the movement carries it between.

- **The design's steps.** `steppedMotion = true` brings them back, and the sample's header switches it. Over the first 300 ms of each of eleven kinds, 14 to 19 of 19 frames change on every frame, and 3 to 7 under the steps (`MotionEveryFrameTest`).
- **What it moves.** No golden or parity cell: they draw at rest or at frozen times. One behaviour test held the tabs' bar to a step, and now holds it to the frame.
- **The Tape's band and head noise** ([B-136](../backlog/B-136-the-tape-band-on-every-frame.md), 2026-11-18). On the owner's word they run on every frame too: the band rises and the noise is drawn anew each frame, so the Tape draws on every frame while the clock runs, over its content held in a layer of its own. Its grain keeps the tick; under `steppedMotion` the band and the noise step with it. Since [B-141](../backlog/B-141-the-tape-draws-in-a-layer-of-its-own.md) the Tape draws in a layer of its own as well: drawn in its holder's layer, a Tape inside a page's column recorded the whole column on every frame (B-140: 20.9 ms of work on the Pixel 6a in Chrome, 7–8 ms with the layer).
- **The progress's band and reel** ([B-191](../backlog/B-191-progress-under-the-pause-and-the-steps.md), 2026-11-23). On the designer's answer 14 they are movement too: by default the band runs 96 dp a second and the reel turns 240° a second on every frame; under `steppedMotion` they step at the 125 ms scene tick, 12 dp and 30° a tick, as the engine's `stageStep` draws them. Frozen (`data-mu-t`) they stand at the scene's tick, so no parity cell moves.
- **Rejected:**
  - the design's steps as the default with a switch off, which keeps the motion the owner found limited;
  - per-frame movement of the bodies' shapes too, which would rebuild every body's field on every frame against D6.

### D23. In the browser, a resting body is drawn by a shader

The owner's decision of 2026-11-18 ([B-126](../backlog/B-126-bodies-on-the-gpu.md)). In Chrome on a hot Pixel 6a Skia's CPU work was two thirds of the one thread; a runtime shader reading the body's field ([B-130](../backlog/B-130-the-shader-marches-its-cells.md)) and every decoration ([B-131](../backlog/B-131-decorations-in-the-shader.md)) runs ten cards at 60.8 frames a second against 26.9 by paths. It is the default on wasm only (`bodyOnGpuByDefault`); the desktop, Android and iOS draw by paths.

- **What it moves.** In the browser, the drips a shade more angular: four «Копия» cases of `BodyShaderParityTest` at 0.6 % against the paths, accepted ([B-132](../backlog/B-132-the-shader-rounds-its-contour.md) is the way back). No golden or parity cell on the Mac.
- **What keeps its paths.** A pressed or hot body, a pour, a slide, an error, focus, the mosaic past the contour, and the keyframe loss's snapshot ([B-133](../backlog/B-133-the-keyframe-loss-freezes-a-shader-page.md)).
- **Rejected:** the shader on the desktop too; a public switch; leaving it off.

### D24. The designer's answers to the look pass: steps, tokens, a finished frame

The owner brought the designer's answers to [B-168](../backlog/B-168-look-and-motion-pass.md)'s 40 questions on 2026-11-23; they are kept verbatim at the end of that item, and each answer that asks for work is an item, B-179…B-207. Three rules run through them, and an item built under D15 from now on follows them:

- **No smooth opacity in the kit.** A body appears, leaves and changes in steps at ticks, never by an alpha ramp. Appearing is `tune-in`: 320 ms in three steps, a 16 dp mosaic, an 8 dp mosaic, the finished body, its words and icons only on the third step; leaving is two steps over 160 ms, the 8 dp mosaic, then nothing ([B-179](../backlog/B-179-bodies-tune-in-as-they-appear.md)). The brief's event table drew `tune-in` as 16 → 8 → 4 → clean and a screen's `switch` as a 200 ms tear; the answers replace both. One exception the answers name: the dialog's scrim keeps its 200 ms fade (B-28, answer 7). A drag follows the finger unquantised (answer 5).
- **Durations come from the tokens**: `tune-in` 320 ms in three steps, `slide` 330 ms, heat in 320 ms, cooling 480 ms, the rest tick 125 ms. A new number an answer names (a 100 ms press hold in a scroll, a 280 ms accent hand-over, a 300 ms loss of signal) is a named constant beside them, with its item.
- **Under reduce motion every state is shown at once as its finished frame**, with no transition. This widens D7: the pause still stops the clock and keeps the damage, and a state that used to show nothing under it now shows its finished picture — a press is a still frame at heat 1 ([B-190](../backlog/B-190-a-press-under-the-pause.md)), the indeterminate band stands centred ([B-191](../backlog/B-191-progress-under-the-pause-and-the-steps.md)), a hover shows its tremble still, at the resting tick's time ([B-193](../backlog/B-193-hover-keeps-the-fill.md)).

How a body tunes in and leaves ([B-179](../backlog/B-179-bodies-tune-in-as-they-appear.md), answer 1), built once in the surface for B-180's screens, B-181's switching on and B-185's dialog:

- **Who keeps the leaving content composed: a host, `MutantAppear(visible)`.** Its content stays composed for the 160 ms after `visible` turns false, deaf to pointer, keys and readers, then leaves. Every `MutantSurface` inside registers its slot with the host, which draws each slot's mosaic while it tunes and the content itself (words, icons, bodies) only on the third step; under the mosaic the content is drawn into an empty clip, so its bodies are built by then. A component that keeps its own visibility gives its body a `MutantTune` instead (the tooltip's plate in its popup, the snackbar through `MutantSurface(tune = …)`). Composed visible, the host tunes in; a component the consumer takes straight out of the composition cannot step out, and the KDoc says so.
- **Rejected:** each component tuning in on its own first composition. A lazy list's cards would tune in again every time they scroll into view, a screen's every body would tune in on every visit with no way out, and leaving would still need something to hold the content composed.
- **The mosaic** (designed here, D15): blocks of 16 or 8 dp laid from the slot's origin, clipped to its corner; each block in the body's fill (half of them) or one of the cocoon's two paints (a quarter each), from the stream `rng(<cassette>/<key>, tune<side>)`, fixed for the step. So a cocoon handing over to a card goes from its own paints to the card's fill.
- **The steps are counted on the screen's clock from the composition that saw the change**: 0–106.7 ms the 16 dp mosaic, to 213.3 ms the 8 dp one, then the body; leaving, 0–80 ms the 8 dp mosaic, to 160 ms nothing. A held, paused or frozen clock shows the end at once, as reduce motion does. A turn back half-way goes on from the picture shown.

A hover's look (answer 16, [B-193](../backlog/B-193-hover-keeps-the-fill.md)) replaces B-63's simmer: the fill stays as it is, and only the outline trembles harder, as heat 0.25 draws it, with no amber, no bubbles, no held drops and no running mosaic. A body's heat is two: a press's, which the fill, the bubbles, the held drops and the running mosaic follow, and the tremble's, the higher of the press's and the hover's.

A heat impulse (answers 4 and 39) is `HeatMachine.impulse`: the press's heat stands at the impulse's value in the event's frame, where it stood lower, and cools from there at the release's rate; under the pause it does nothing, the body as it was being the finished frame. A surface takes a machine its owner drives through `MutantSurface(heatMachine = …)`; the menu's window was the first ([B-201](../backlog/B-201-the-menu-at-the-edges-and-its-chosen-row.md)).

Where an answer counts the steps of a movement (the slider's settle «330 мс … 4 шага», answer 4), those are the steps `steppedMotion = true` draws; by default the movement runs on every frame, as D22 says for every other transition. That is this repository's reading of the answer, not the designer's word, and its items say so.

The keyframe loss and a screen change (answer 24, [B-198](../backlog/B-198-the-keyframe-loss-ends-on-a-screen-change.md)): the kit learns of a screen change from a key the consumer passes, `MutantKeyframeLoss(screen = …)`, until a screen host (B-180) passes its own; on a change the loss ends at once and none starts for 1 s. A scroll is not a screen change. The tracking dividers are bodies of the loss's scene.

Deferred by the designer: the list's scroll shear (answer 6) and typed digits in the pickers (answer 21). Answer 25 says the designer's next package will clip the mosaic to the clean zone as well; under D20 that package is a list of proposals, and [B-199](../backlog/B-199-no-mosaic-in-the-clean-zone-when-pressed.md) makes the change here first.

## 3. Risks and open questions

| # | Risk or hypothesis | Mitigation, and the item that settles it |
|---|---|---|
| H1 | **The port does not match on every target.** `sin` in the lattice hash, `exp`/`pow` in the drips, `Float32` storage (§1.4). | *Closed by B-03:* every golden path is byte-identical on the JVM, the iOS simulator and wasm (§1.4); the common test still allows the handoff's 0.1 dp, and a JVM-only test holds exactness. |
| H2 | **The fonts lack a script or a weight** (§1.6). | *Closed by B-02:* both carry Cyrillic and Latin; four static instances are bundled. |
| H3 | **The field is too slow on a phone** (D13). | *Closed by B-49 and B-89 on the owner's Pixel 6a:* the instance's worst case is 283 µs; ten cards under the Tape at 12 ticks a second and a list of 100 scrolling skip no vsync, with a main-thread p95 of 3.1 and 11.8 ms. The phone is 2.7 times the M1 on a warm frame. |
| H4 | **The venom's soft joins need a blur and an alpha threshold**, which Compose exposes as `RenderEffect` on Skia and Android 12+ only. | *Desktop settled by B-12:* a graphics layer with a blur and a colour matrix reproduces the filter but for its edges (§1.11). Android below 12 draws the geometric shapes, as the handoff allows; not run on a device. |
| H5 | **The pages can be rendered at a fixed time** through `data-mu-t` and `setPaused`, so that a reference is reproducible. | *Closed by B-05:* `data-mu-t` on every stage, with keyframe loss off, renders byte-identically twice; the pause is not needed (§1.8). |
| R1 | **Shape jumps when a slot is resized** (§1.7). | *Measured by B-10:* the drips stay, the mosaic mostly stays, the flow lines all move (§1.10); the owner decides in [B-51](../backlog/B-51-size-change-moves-the-flow-lines.md). |
| R2 | **Disabled text under the Tape is 4.4:1 in «Копия»** (§1.5). | *Closed by B-38:* measured at every scene tick of a Tape cycle, the disabled words read 4.38 («Эфир»'s disabled button) to 4.52; the owner chose WCAG's exemption for inactive components on 2026-11-14 (1.4.3 asks no contrast of them), the tokens stay, and the words are held from falling under 4.3 (§1.16). |
| R3 | **What the design does not draw** — appearance and gestures of the v1 set, «Чистый сигнал», hover. | *Closed by B-43:* designed here (D15), each item listing the numbers it chose. |
| R4 | **Flashes on a real screen** were measured by the designer in a browser, not on a device. | *Closed by B-38 on the kit (desktop):* no scene tick changes more than 7.3 % of a demo screen against the rule's quarter; a device's recording waits for the Pixel 6a ([B-49](../backlog/B-49-frame-cost-on-a-pixel-6a.md)) (§1.16). |

---

## 4. What happens next

The backlog ([backlog.md](../../backlog.md)) starts with what can invalidate the architecture:
the skeleton, the fonts, the generator port and its oracle, the transition geometry, the pixel
references with their floor, and the cost of the field. The materials follow (clock, tokens, theme,
the body renderer and its states, transitions, the Tape, icons), then the MVP components in the
design's own groups, the demo screens and accessibility, the v1 set, and the library as a product.

## Anchors

| What | Where |
|---|---|
| The frozen design and what each file is for | `reference/README.md` |
| The generator, the oracle of the port | `reference/design-system/mutant-generator.js` |
| How the engine calls it: layers, boil, tick, pause | `reference/design-system/mutant-engine.js` |
| The designer's test vectors | `reference/design-system/mutant-golden.json` |
| Tokens of both themes, motion, profiles | `reference/design-system/mutant-tokens.json` |
| The handoff note and the brief | `reference/design-system/README.md`, `reference/design-system/brief.md` |
| The check that the generator still reproduces its golden | `scripts/design-golden.mjs` |
