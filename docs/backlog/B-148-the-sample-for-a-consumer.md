---
id: B-148
title: "The sample for a consumer, light on a phone"
status: wip
priority: P1
size: M
stage: stage-6-later
blocked_by: []
---

# B-148 — The sample for a consumer, light on a phone

The owner, 2026-11-20, a new round: «Demo kit (названия категорий MVP, later) знать никчему потребителю»;
«Производительность (страничка components тяжёлая для мобилы и 3 экрана demo за раз не удобно)».

What the sample does today (`main` at `66d9c57`):
- the Components page groups its 38 sections by the design's stages, `SpecStage.Mvp`, `V1`, `Later` and `Base`, with
  titles «Компоненты», «Компоненты v1», «Компоненты позже» and a stage badge (`MVP`, `V1`, `LATER`) in every section's
  head (`ComponentsPage.kt`, `SampleStrings.kt`). How the designer's pages were cut is nothing to a consumer;
- the page is one `Column` of every section at once, every specimen live: all of them composed, laid out and drawn,
  the ones off the window too. B-138 and B-140 measured this page as the heaviest of the sample on a phone;
- the Demo page lays its three screens out side by side in a `FlowRow` (`DemoPage.kt`).

- **The decision.**
  - The sections are grouped by what a consumer looks for, as the README groups them: **Foundations** (theme, icons,
    patterns, the Tape, the keyframe loss), **Actions**, **Selection and input**, **Containment**, **Navigation**,
    **Communication**, **Overlays**. No design stage is named anywhere in the sample.
  - The page shows **one group at a time**: the index's chips choose it, and only its sections are composed. The
    first group, Foundations, is the default.
  - The Demo page shows **one screen at a time**, a row of chips choosing Library, Recording or Settings.
- **Rejected:** a lazy column of all 38 sections (the names stay the designer's, and a fast fling still composes most
  of them); keeping the stage as a small badge (it says nothing to a consumer).
- AC:
  - no `SpecStage` name, «MVP», «v1», «позже» or «later» left in what the sample shows, in either language; a test
    walks every group's chip and finds every one of the 38 sections in exactly one group;
  - the Components page composes only the chosen group's sections (a test counts them), the Demo page one screen;
  - the sample's existing tests green, with what they assert about the old grouping moved to the new one; the benches
    (`FrameBench`, `IosBench`, `frame-bench-browser.mjs`) still find their scenes, each scene's meaning written down
    where it changed;
  - the browser bench in the phone proxy (`PHONE=1`, the CPU slowed as B-116 calibrated it), `ComponentsRest`,
    before against after, with its log: the cost of the default view, not a phone's number (the phone is not
    available, the owner, 2026-11-20).
- Anchors: `sample/src/commonMain/kotlin/io/github/youndie/mutant/sample/ComponentsPage.kt`,
  `sample/src/commonMain/kotlin/io/github/youndie/mutant/sample/SpecCatalogue.kt`,
  `sample/src/commonMain/kotlin/io/github/youndie/mutant/sample/DemoPage.kt`

## Findings

### Iteration 1, 2026-11-20

**What changed**, all in `sample/` (`mutant-core` untouched, no golden moved; the sample has none of its own):
- `SpecCatalogue.kt`: `SpecStage` is gone. `SpecGroup` (`Foundations`, `Actions`, `Selection`, `Containment`,
  `Navigation`, `Communication`, `Overlays`) takes its place, and `SpecId`'s entries stand in the groups' order. It is
  public, as `SampleSettings` remembers the group.
- The section files are filed by group, their functions moved unchanged (the moved lines compared equal, imports
  aside): `ComponentsFoundations.kt`, `ComponentsActions.kt`, `ComponentsSelection.kt`, `ComponentsContainment.kt`,
  `ComponentsNavigation.kt`, `ComponentsCommunication.kt` and `ComponentsOverlays.kt` replace `ComponentsBase.kt`,
  `ComponentsMvp.kt`, `ComponentsV1.kt` and `ComponentsLater.kt`. Every specimen is as it was.
- `ComponentsPage.kt`: the index's chips choose the group (the chosen chip selected) instead of scrolling to it. The page
  composes the index, the group's title and line, and that group's sections alone. A section's head has lost its stage
  badge.
- `SampleStrings.kt`: the stage titles, lines and badges (`groupMvp`…`groupBase`, `stageMvp`…`stageBase`) are replaced by
  the seven groups' titles and one-line descriptions in both languages (Основа, Действия, Выбор и ввод, Контейнеры,
  Навигация, Сообщения, Оверлеи).
- `MutantUiSample.kt`: `SampleSettings` remembers `group` (default `Foundations`) and `demoScreen` (default `Library`).
- `DemoPage.kt`: one screen at a time under a row of chips (Library, Recording, Settings). The screen's frame, size,
  title and keyframe loss are as they were. `DemoScreen` is public, for the settings.
- Found on the way: the Settings screen's dialog takes focus as it opens, and the focus asked the page to scroll to it
  (265 px in the test's window). The chips scrolled out of view, so a click on the next chip missed. The phone now stands
  in the Components page's `StayPut` (made `internal`), and the page stays where it is.

**The groups as implemented** (`SpecId` by group; 38 sections, each in exactly one group, none left over or added):

| Group | Sections |
|---|---|
| Foundations | Theme, Icons, Patterns, Tape, KeyframeLoss |
| Actions | Button, IconButton, Fab, Segmented |
| Selection and input | Checkbox, Radio, Switch, Slider, Chip, TextField, Search, DatePicker, TimePicker |
| Containment | Card, ListItem, Divider, Carousel, Table |
| Navigation | TopAppBar, Tabs, NavigationBar, Drawer, Menu |
| Communication | Badge, LinearProgress, CircularProgress, Cocoon, Snackbar, Tooltip, EmptyState, Pull |
| Overlays | Dialog, Sheet |

**Tests** (`sample/src/desktopTest/`, 22 tests, all green):
- `ComponentsPageTest`:
  - new, `the index chooses a group, and the page composes that group's sections alone`: the page opens on
    Foundations. Each group's chip, clicked in turn, is selected and the others are not, the settings hold the group, and
    the sections composed are exactly the table's row, in its order. Across the walk each of the 38 is found in exactly
    one group. It replaces `the index scrolls to a group`;
  - new, `no design stage is named on either page, in either language`: every text of every group and every demo
    screen, in English and in Russian, against `MVP`, `v1`, `later` and `позже`;
  - `every section has its head…` and `each section shows the states…` walk the groups through the settings; the head
    no longer wants a stage badge;
  - `the page opens at its top…` opens each group on its own: the overlays shown from the first frame are in
    Navigation, Communication and Overlays, none of them the default.
- `DemoPageTest`, new: the Demo page opens on Library alone; each chip chooses its screen, shows selected and leaves one
  screen composed, the page's scroll at 0.
- `SampleAppTest`: walks every group and every demo screen in each theme and language.
- `DemoClocksTest` (B-90): the Library and Settings screens stand side by side in the test, as the page no longer shows
  two at once. `DemoTapeOnceTest` (B-101) opens the Demo page on Recording through the settings.
- Mutants, checked on the branch: the page composing every section, the Demo page composing all three screens, and a
  head showing «MVP» each failed its new test (the index, the Demo page and the stage test). With the code restored, all
  were green again.

**The benches' scenes.** No scene was added or renamed. `FrameBench`, `IosBench`, `BenchScene.kt` and
`frame-bench-browser.mjs` reach the sample through `SampleSettings`' defaults, so:
- `ComponentsRest`, `ComponentsWheel`, `ComponentsClean`, `ComponentsReduce`, `ControlRest` and the `Control` of
  `FrameBench` and of the browser bench now show the Components page with the default group, Foundations, alone (five
  sections), not the whole page;
- `DemoPage`, `DemoPageClean`, `DemoPageTicked` and `DemoPageLoad` now show the Demo page with Library alone, not three
  phones;
- `DemoLibrary`, `DemoRecording`, `DemoSettings`, `DemoRow`, `DemoRowNoClip`, `TenCards8`, `Blank`, `BlankLoad` and
  `IosBench`'s `Control` are unchanged: they compose the screens or cards themselves.

A number from before this item for a scene of the first two lists does not compare with one taken after it.

**The phone proxy, before against after.** [The run](../../logs/b-148/2026-11-20-proxy-components.log): B-73's browser
bench in the phone proxy (`PHONE=1 CPU=2`: 390 × 844 at 3x, mobile, touch, the main thread slowed twice), headless
Chrome 157 on the M1 MacBook Air, 6 rounds of 10 s interleaved, round 1 not counted. `main`'s code (`66d9c57`, built
at `2d0b4f4`, which adds only this item's document) is `a`, this branch's (`6156260`) is `~b`. It is a proxy on this
Mac, not a phone: the GPU is the Mac's, and the phone was not available.

| Scene | Arm | f/s | work p50, ms | work p95, ms |
|---|---|---|---|---|
| `ComponentsRest` | `main`: every section | 18.3 | 26.8 | 145.7 |
| `ComponentsRest` | this branch: Foundations alone | 34.1 | 8.8 | 129.0 |
| `DemoPage` | `main`: three phones | 29.5 | 9.9 | 111.3 |
| `DemoPage` | this branch: Library alone | 22.4 | 9.2 | 139.1 |

Same host, same run: yes.

- **The host was not quiet**, and the run says so: a load average of 10 to 12 on 8 cores through it, from another
  repository's Gradle tests and a `MutantSample` app in the iOS simulator at about 60 % of a core (neither this item's;
  left running). Rounds swing by tens of milliseconds of work in either arm (`ComponentsRest` on `main`, round 2:
  8.4 f/s, 127 ms; `DemoPage` on the branch, rounds 3 and 6: 7.3 and 11.9 f/s), and every pooled verdict is red.
- `ComponentsRest`: the branch did less work than `main` in each of the five counted rounds (p50 by −120.6, −17.1,
  −12.2, −10.6 and −19.2 ms) and drew more frames in each (+39.3, +8.8, +6.6, +9.0, +15.3 f/s). Read as the default
  view costing about a third of the whole page's work at the median, under that load.
- `DemoPage`: no difference that the run can tell. Rounds go either way (p50 −2.8, +123.5, −5.2, −0.9, +84.8 ms); the two
  large ones are the branch's rounds that fell to 7 and 12 f/s. On a 390 dp wide window `main` stacked the three phones,
  so only the Library phone was on screen there too; the two others were composed and laid out, off the window.

**Gates** at the branch's code: `./gradlew check` green (`mutant-core` 452 tests, `sample` 22, none skipped or failed,
the iOS sample compiled by `:sample:compileKotlinIosSimulatorArm64`), `:sample:wasmJsBrowserDistribution` built (the
run's `~b`), `make check` green.

**Left out:**
- No phone: the proxy above is all this item measured. A run on the Pixel 6a's Chrome would say what the default view
  costs there (`DEVICE=1`, the same two scenes).
- The README's table of frame rates still cites B-138's and B-141's numbers, taken on the whole page; they describe the
  page as it was until a run on the new one replaces them.
- A second proxy run on a quiet host, if `DemoPage`'s answer is wanted: this one could not tell the two arms apart.
