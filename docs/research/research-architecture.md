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

**Consequence 5.** The brief asks for 4.5:1 *after the Tape at the worst tick*. WCAG 1.4.3 exempts
inactive components, so disabled text at 4.4 is within WCAG and outside the brief; the
accessibility item measures it on the rendered kit and puts the choice to the owner
([B-38](../backlog/B-38-accessibility.md)).

### 1.6 Fonts

| Fact | Where verified |
|---|---|
| Two families: Handjet (OFL) for display and on-screen text, sizes 14–28, letter-spacing 1 for `osd`; Rubik (OFL) for text, 11–16 sp, weights 400 and 500 | `mutant-tokens.json` (`mutant.type`) |
| The pages ask Google Fonts for Handjet 400 and 600 and Rubik 400, 500 and 700 | the `<helmet>` of each page |
| *Hypothesis H2:* both are variable files on Google Fonts with Cyrillic. The handoff says "оба с кириллицей"; nobody here has read a cmap | handoff README, «Ресурсы»; settled by [B-02](../backlog/B-02-fonts.md) |

**Consequence 6.** At `minSdk 24` a variable font's `wght` axis is ignored and its default instance
draws, so the bundled files are static instances cut from the variable ones, as plakat-ui did with
Golos Text (D9).

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

---

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

Cut from the variable files at the weights the tokens and pages use, each licence shipped beside
them; a coverage test holds every family to every string the fixtures draw
([B-02](../backlog/B-02-fonts.md)).

### D10. The Tape is one layer over the screen, not a layer per component

Scanlines, grain, the tracking band every 7 s and the head noise are drawn once, over the screen
root ([B-13](../backlog/B-13-tape.md)). Keyframe loss, which the handoff calls optional, waits
([B-48](../backlog/B-48-keyframe-loss.md)).

### D11. Documentation, code and commits in English; the brief stays in Russian

Like the other three kits. The design's demo strings are copied verbatim into fixtures. The brief
requires the demo screens to be checked in both Cyrillic and Latin, so the sample switches
language; which one it opens in is the owner's call, asked when the sample is built
([B-37](../backlog/B-37-demo-screens-and-sample.md)).

### D12. Built and verified on the Mac; the repository stays out of mutagen sync

Goldens and references are a claim about the rasteriser that recorded them, and the line's code
gate runs on `macos-latest`. The project is not in `mutagen sync list`.

### D13. The cost of the field is measured before the components are built on it

The handoff names it the main risk: the field is recomputed on the CPU every tick for every visible
instance. The cost of `genItem` and of one frame is measured on the desktop JVM as soon as the port
exists, and on a Pixel 6a as soon as one body is drawn, against the brief's target (60 fps, ten
cards with the Tape, a 100-item list, an instance in ≤ 1 ms) and its declared retreat order
([B-06](../backlog/B-06-frame-cost.md)). Rejected: measuring last, as plakat-ui did — there the
architecture did not depend on the answer; here it does.

---

## 3. Risks and open questions

| # | Risk or hypothesis | Mitigation, and the item that settles it |
|---|---|---|
| H1 | **The port does not match on every target.** `sin` in the lattice hash, `exp`/`pow` in the drips, `Float32` storage (§1.4). | Goldens tested on JVM, Native and wasm; exact where the handoff asks for exact, ±0.1 dp on paths; a mutant in each of hash, rng, noise, smin and the march must be caught ([B-03](../backlog/B-03-generator-port.md)). |
| H2 | **The fonts lack a script or a weight** (§1.6). | Read the cmap of the delivered files before choosing instances ([B-02](../backlog/B-02-fonts.md)). |
| H3 | **The field is too slow on a phone** (D13). | Measured with a control that proves the harness can see a known cost; then the brief's retreat order and the handoff's two outs, a coarser grid or a shader field ([B-06](../backlog/B-06-frame-cost.md)). |
| H4 | **The venom's soft joins need a blur and an alpha threshold**, which Compose exposes as `RenderEffect` on Skia and Android 12+ only. | The handoff allows the geometric fallback on older Android; whether a common-code blur plus colour-matrix layer reproduces `a' = 18·a − 8.2` is checked against the venom references ([B-12](../backlog/B-12-transitions.md)). |
| H5 | **The pages can be rendered at a fixed time** through `data-mu-t` and `setPaused`, so that a reference is reproducible. | [B-05](../backlog/B-05-pixel-references-and-floor.md) renders each page twice and compares the two. |
| R1 | **Shape jumps when a slot is resized** (§1.7): a text field growing with its content rebuilds its drips. | Measured on the field and the card; if it is visible, the owner chooses between a size bucket and the jump ([B-10](../backlog/B-10-mutant-surface.md)). |
| R2 | **Disabled text under the Tape is 4.4:1 in «Копия»** (§1.5). | Measured on the kit at the worst tick; the owner chooses WCAG's exemption or a darker token ([B-38](../backlog/B-38-accessibility.md)). |
| R3 | **What the design does not draw** — appearance and gestures of the v1 set, «Чистый сигнал», hover. | Put to the owner before the first such component ([B-43](../backlog/B-43-undesigned-behaviour.md)). |
| R4 | **Flashes on a real screen** were measured by the designer in a browser, not on a device. | A recording of the demo screens through the flash rule, in [B-38](../backlog/B-38-accessibility.md). |

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
