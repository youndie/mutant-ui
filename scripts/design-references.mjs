#!/usr/bin/env node
// B-05: render the pixel references in headless Chrome (research D2).
//
//   node scripts/design-references.mjs [--check] [--chrome <binary>]
//   node scripts/design-references.mjs --manifest      no Chrome: the gate's check that nothing moved
//
// Two kinds of reference:
//
// - Probes: one body's outline as the vendored generator draws it at a fixed time — the fill clipped by
//   the contour (evenodd) and the ink stroke, round joins — on a blank page in Chrome, with no other
//   layer. Compose draws the port's outline the same way, and the difference is the floor of the body:
//   what two rasterisers make of one path (BodyFloorTest).
// - Page cells: a component as the design's page draws it, with the page's own runtime and engine. The
//   engine runs a live clock, so before the page starts it is frozen: every stage gets `data-mu-t`, which
//   the engine reads as "every slot still at this time" (its `still`/`tFix`, the Tape and the stage's
//   animations too), and keyframe loss is switched off. The engine is reached by wrapping
//   `window.MutantEngine` as the page assigns it; the vendored files are not touched.
//
// Fonts: the pages' Google Fonts stylesheet is answered, over the DevTools Fetch domain, with
// @font-face rules for the repository's own files (B-02), so a reference and a fixture draw the same
// faces. The page runtime (React, ReactDOM, Babel) comes from unpkg at the versions support.js pins.
// Device scale 1: 1 CSS px = 1 dp = 1 px. The PNGs go to mutant-core/src/desktopTest/snapshots/design.
// --check renders into memory and fails if any PNG differs. Node 24 has WebSocket, so the DevTools
// protocol needs no dependency.

import { spawn } from 'node:child_process';
import { createHash } from 'node:crypto';
import { existsSync, mkdirSync, mkdtempSync, readFileSync, rmSync, writeFileSync } from 'node:fs';
import { tmpdir } from 'node:os';
import { join, resolve } from 'node:path';
import { setTimeout as sleep } from 'node:timers/promises';
import { pathToFileURL } from 'node:url';
import { inflateSync } from 'node:zlib';

const ROOT = resolve(import.meta.dirname, '..');
const DESIGN = join(ROOT, 'reference/design-system');
const FONTS = join(ROOT, 'mutant-core/src/commonMain/composeResources/font');
const OUT = join(ROOT, 'mutant-core/src/desktopTest/snapshots/design');
const GENERATOR = join(DESIGN, 'mutant-generator.js');
const ENGINE = join(DESIGN, 'mutant-engine.js');
const MVP = 'mutant-ui Компоненты.dc.html';
const DEMO = 'mutant-ui Демо.dc.html';
const SPECS = 'mutant-ui Анимационные спеки.dc.html';
const ICONS = 'mutant-ui Иконки и заготовки.dc.html';
const V1 = 'mutant-ui Компоненты v1.dc.html';
const LATER = 'mutant-ui Компоненты позже.dc.html';
// B-14: the Icons page's icons (in its order), patterns and illustrations, named as the kit names them.
const ICON_NAMES = ['Menu', 'Back', 'Close', 'Search', 'Home', 'Add', 'Delete', 'Edit', 'Share', 'Refresh', 'Settings', 'Done', 'Error', 'Warning', 'Info', 'Schedule'];
const PATTERNS = [['Lines', 'lines'], ['Snow', 'snow'], ['Raster', 'raster'], ['Mosaic', 'mosaic']];
// Each illustration and its stage; its art is the box its parts stand in (`elementCell`).
const ILLUSTRATIONS = [['Cassette', 'il-cassette'], ['Television', 'il-tv'], ['Antenna', 'il-antenna'], ['Floppy', 'il-floppy'], ['Remote', 'il-remote'], ['TestCard', 'il-testcard']];
// The engine's own default still time (`tFix`), and the time every probe and fixture is drawn at.
const T = 5000;

// The themes' colours a probe needs: `static TH` of the generator. Fill by profile, as `static K` names it.
const THEMES = {
  Efir: { bg: '#10142a', ink: '#05060c', green: '#b6ff1a', surf: '#232b4a', surf2: '#2f3960' },
  Kopiya: { bg: '#efe9d9', ink: '#14110f', green: '#b6ff1a', surf: '#e3dbc6', surf2: '#d6ccb2' },
};
// One probe per grid step and size class: a button (0.9 dp grid, drips), a card (1.5 dp grid, the widest
// body), a dot (a small round body with no drips). Slots as scripts/gen-goldens.mjs builds them.
const PROBES = [
  { name: 'Button', type: 'btn', key: 'btn/a', w: 200, h: 48, content: [52, 15, 96, 18] },
  { name: 'Card', type: 'card', key: 'card/a', w: 328, h: 80, content: [16, 14, 200, 36] },
  { name: 'Dot', type: 'dot', key: 'dot/a', w: 22, h: 22 },
];
// B-52: a label alone, in each face at sizes the type roles use (Handjet 18 at line-height 1 and Rubik 14 at
// 1.2 among them: there Blink's rounding of the face's ascent and descent moves the baseline a pixel), at a whole-pixel origin: what is left between
// Chrome's text and Compose's when nothing else differs — the text floor. Words from the design's pages.
const TEXT_PROBES = [
  ['Handjet', 400, 16, 1, 1, 'CH 03 · НАСТРОЙКА 1.44 МБ'],
  ['Handjet', 400, 18, 1, 1, 'CH 03 · НАСТРОЙКА 1.44 МБ'],
  ['Handjet', 400, 20, 1, 1, 'CH 03 · НАСТРОЙКА 1.44 МБ'],
  ['Handjet', 400, 24, 1, 0, 'Здесь пока пусто'],
  ['Rubik', 400, 13, 1.4, 0, 'Сторона A · 4 записи · без подписи'],
  ['Rubik', 400, 16, 1.4, 0, 'Без подписи №3 · Сторона A'],
  ['Rubik', 500, 14, 1.4, 0, 'Ничего не выбрано · Пульт'],
  // Two where Blink's rounding of the face's ascent and descent moves the baseline a pixel (B-52).
  ['Rubik', 400, 14, 1.2, 0, 'Ничего не выбрано · Пульт'],
].map(([family, weight, size, line, spacing, text]) => ({ family, weight, size, line, spacing, text }));
export const REFERENCES = [
  ...TEXT_PROBES.map((p) => ({ stem: `Text_${p.family}${p.weight}_${p.size}_Efir`, text: p })),
  ...PROBES.flatMap((p) => Object.keys(THEMES).map((theme) => ({ stem: `Body_${p.name}_${theme}`, probe: p, theme }))),
  // The Button stage's first cell: the resting button, its drips and the stage's background around it.
  { stem: 'Button_Rest_Efir', page: MVP, stage: 'button', type: 'btn', index: 0 },
  // B-10: resting bodies with every text on their stage made transparent, so what remains is the body's own
  // layers. Each reference records the slot as the engine built it, for the fixture to build the same.
  { stem: 'Button_RestBody_Efir', page: MVP, stage: 'button', type: 'btn', index: 0, hideText: true },
  { stem: 'Card_RestBody_Efir', page: MVP, stage: 'card', type: 'card', index: 0, hideText: true },
  { stem: 'Field_RestBody_Efir', page: MVP, stage: 'field', type: 'field', index: 0, hideText: true },
  // B-11: the states, each the slot carrying it on its stage, text transparent.
  ...[
    ['Button_PressedBody_Efir', 'button', '[data-mu="btn"][data-key="button"][data-heat]'],
    ['Button_LoadingBody_Efir', 'button', '[data-mu="btn"][data-key="button"][data-state="loading"]'],
    ['Button_SuccessBody_Efir', 'button', '[data-mu="btn"][data-key="button"][data-state="clean"]'],
    ['Button_DisabledBody_Efir', 'button', '[data-mu="btn"][data-key="button"][data-state="disabled"]'],
    ['Field_FocusedBody_Efir', 'field', '[data-mu="field"][data-brackets]'],
    ['Field_ErrorBody_Efir', 'field', '[data-mu="field"][data-state="error"]'],
    ['Field_DisabledBody_Efir', 'field', '[data-mu="field"][data-state="disabled"]'],
    ['Card_DisabledBody_Efir', 'card', '[data-mu="card"][data-state="disabled"]'],
  ].map(([stem, stage, sel]) => ({ stem, page: MVP, stage, sel, hideText: true })),
  // B-12: the motion spec page's frames, each a stage of its own frozen at its time with a fixed transition
  // progress (data-pour, data-slide, data-heat-k). Every slot of the stage is recorded, so a fixture draws
  // them all: the switch's track pours while its thumb slides.
  ...[['0', '0'], ['30', '0.3'], ['60', '0.6'], ['100', '1']].map(([n, p]) =>
    ({ stem: `Chip_Pour${n}Body_Efir`, page: SPECS, sel: `[data-key="spec-chip"][data-pour="${p}|surf2"]`, hideText: true })),
  ...[['0', '0'], ['33', '0.33'], ['66', '0.66'], ['100', '1']].map(([n, p]) =>
    ({ stem: `Switch_Slide${n}Body_Efir`, page: SPECS, sel: `[data-key="spec-switch"][data-pour="${p}|surf2"]`, hideText: true })),
  ...[['0', '0'], ['33', '0.33'], ['66', '0.66'], ['100', '1']].map(([n, p]) =>
    ({ stem: `Check_Pour${n}Body_Efir`, page: SPECS, sel: `[data-key="spec-check"][data-pour="${p}|surf2"]`, hideText: true })),
  // B-12: the venom's two layers as the engine's venom() builds them — ink with a doubled outline under the pill,
  // the fill over it, the fat shapes through the goo filter (blur 3.2, alpha · 18 − 8.2) — for the three cases of
  // the designer's golden, on a blank page.
  ...['menu', 'segmented', 'drawer'].flatMap((c) => [10, 30, 50, 70].map((n) => ({ stem: `Venom_${c[0].toUpperCase() + c.slice(1)}${n}_Efir`, venom: { case: c, p: n / 100 } }))),
  // B-13: the Tape over a card, the spec page's drift frames: at rest, the tracking band entering, mid-pass, gone.
  ...[3000, 7350, 8050, 8900].map((t) => ({ stem: `Tape_Drift${t}_Efir`, page: SPECS, sel: `[data-mu-stage][data-mu-t="${t}"] [data-key="spec-card"]`, hideText: true, tape: true })),
  // B-14: an element cropped as it stands (`el`), with what its fixture needs to stand it the same way.
  ...ICON_NAMES.flatMap((name, i) => [48, 16].map((size) => ({ stem: `Icon_${name}${size}_Efir`, page: ICONS, el: true, sel: `[data-screen-label="Иконки"] svg[width="${size}"]`, index: i }))),
  ...PATTERNS.flatMap(([name, key]) => [0, 1].map((q) => ({ stem: `Pattern_${name}${q ? 'Quiet' : ''}_Efir`, page: ICONS, el: true, sel: `[data-pat="${key}"][data-quiet="${q}"]`, index: 0, hide: `[data-pat] * { color: transparent !important; }` }))),
  // The art with what its drips may hang below it; the Body twin hides the text, the kept parts and the reels.
  ...ILLUSTRATIONS.flatMap(([name, stage]) => [false, true].map((body) => ({ stem: `Illustration_${name}${body ? 'Body' : ''}_Efir`, page: ICONS, el: true, stage, hideText: body }))),
  // B-15: the Button stage whole, label in, in both themes, each with a bare twin (text hidden) to tell the label
  // from the body; the outlined pair; the text button over its divider.
  ...['Efir', 'Kopiya'].flatMap((theme) => [
    ...['Rest', 'Pressed', 'Loading', 'Success', 'Disabled', 'OutlinedRest', 'OutlinedDisabled']
      .flatMap((s, index) => [false, true].map((bare) =>
        ({ stem: `MutantButton_${s}${bare ? 'Bare' : ''}_${theme}`, page: MVP, stage: 'button', type: 'btn', index, theme, below: 28, hideText: bare }))),
    { stem: `MutantButton_Text_${theme}`, page: MVP, el: true, sel: '[data-mu-stage="button"] div:has(> [data-mu-divider])', index: 0, theme },
  ]),
  // B-16: the IconButton · FAB stage, whole and bare (text and currentColor icons hidden), both themes; cut above
  // the captions, 30 px under the icon cells and 38 under the FABs.
  ...['Efir', 'Kopiya'].flatMap((theme) =>
    [['IconButton_Rest', 'icon', 0, 28], ['IconButton_Pressed', 'icon', 1, 28], ['IconButton_Disabled', 'icon', 2, 28],
      ['Fab_Rest', 'fab', 0, 36], ['Fab_Pressed', 'fab', 1, 36], ['Fab_Extended', 'fab', 2, 36]]
      .flatMap(([s, type, index, below]) => [false, true].map((bare) =>
        ({ stem: `Mutant${s}${bare ? 'Bare' : ''}_${theme}`, page: MVP, stage: 'iconfab', type, index, theme, below, hideText: bare })))),
  // B-17: the Chip stage, whole and bare, both themes; cut above the captions, 28 px under the slot.
  ...['Efir', 'Kopiya'].flatMap((theme) =>
    ['Unselected', 'Selected', 'Pressed', 'Disabled'].flatMap((s, index) => [false, true].map((bare) =>
      ({ stem: `MutantChip_${s}${bare ? 'Bare' : ''}_${theme}`, page: MVP, stage: 'chip', type: 'chip', index, theme, below: 26, hideText: bare })))),
  // B-18: the select stage's checkbox and radio cells (the switch is B-19's), whole and bare, both themes. A row
  // is 48 tall; the crop is the slot with the label beside it, 8 px under it.
  ...['Efir', 'Kopiya'].flatMap((theme) =>
    [['Checkbox_Unchecked', 'box', 0], ['Checkbox_Checked', 'box', 1], ['Checkbox_Disabled', 'box', 2],
      ['Radio_Unselected', 'dot', 0], ['Radio_Selected', 'dot', 1], ['Radio_Disabled', 'dot', 2]]
      .flatMap(([s, type, index]) => [false, true].map((bare) =>
        ({ stem: `Mutant${s}${bare ? 'Bare' : ''}_${theme}`, page: MVP, stage: 'select', type, index, theme, below: 8, wide: 140, hideText: bare })))),
  // B-19: the select stage's switch cells, whole and bare, both themes, reaching the label beside the track.
  ...['Efir', 'Kopiya'].flatMap((theme) =>
    ['Off', 'On', 'Disabled'].flatMap((s, index) => [false, true].map((bare) =>
      ({ stem: `MutantSwitch_${s}${bare ? 'Bare' : ''}_${theme}`, page: MVP, stage: 'select', type: 'track', index, theme, below: 8, wide: 140, hideText: bare })))),
  // B-20: the TextField stage, whole and bare, both themes; cut above the captions, 26 px under the field.
  ...['Efir', 'Kopiya'].flatMap((theme) =>
    ['Rest', 'Focused', 'Error', 'Disabled'].flatMap((s, index) => [false, true].map((bare) =>
      ({ stem: `MutantField_${s}${bare ? 'Bare' : ''}_${theme}`, page: MVP, stage: 'field', type: 'field', index, theme, below: 24, hideText: bare })))),
  // B-21: the Card stage's card at rest and disabled, whole and bare, both themes; cut above the captions.
  ...['Efir', 'Kopiya'].flatMap((theme) =>
    ['Rest', 'Disabled'].flatMap((s, index) => [false, true].map((bare) =>
      ({ stem: `MutantCard_${s}${bare ? 'Bare' : ''}_${theme}`, page: MVP, stage: 'card', type: 'card', index, theme, below: 34, hideText: bare })))),
  // B-22: the Card stage's two cocoons as they stand — the card's, and the list row's three — both themes.
  ...['Efir', 'Kopiya'].flatMap((theme) => [
    { stem: `MutantCocoon_Card_${theme}`, page: MVP, el: true, sel: '[data-mu-stage="card"] [data-mu-cocoon]', index: 0, theme },
    { stem: `MutantCocoon_ListRow_${theme}`, page: MVP, el: true, sel: '[data-mu-stage="card"] div:has(> [data-mu-cocoon] + div)', index: 0, theme },
  ]),
  // B-23: the nav stage's top app bar (its row and the divider under it) and tabs (the labels and the bar), cropped
  // as they stand, whole and with text and currentColor icons hidden, both themes.
  ...['Efir', 'Kopiya'].flatMap((theme) => [false, true].flatMap((bare) => {
    const hide = bare ? '[data-mu-stage="nav"], [data-mu-stage="nav"] * { color: transparent !important; }' : '';
    return [
      { stem: `MutantTopAppBar_Rest${bare ? 'Bare' : ''}_${theme}`, page: MVP, el: true, sel: '[data-mu-stage="nav"] div:has(> [data-mu-divider])', index: 0, theme, hide },
      { stem: `MutantTabs_Rest${bare ? 'Bare' : ''}_${theme}`, page: MVP, el: true, sel: '[data-mu-stage="nav"] div:has(> div > span + span + span)', index: 0, theme, hide },
    ];
  })),
  // B-30: the v1 page's two sliders, the one to drag and the disabled one, cropped as they stand (header, ticks,
  // channel, fill and the thumb with its drips), whole and with the text hidden, both themes; the thumb's slot is
  // recorded with the crop's place on its stage.
  ...['Efir', 'Kopiya'].flatMap((theme) => ['Rest', 'Disabled'].flatMap((s, index) => [false, true].map((bare) => ({
    stem: `MutantSlider_${s}${bare ? 'Bare' : ''}_${theme}`, page: V1, el: true, slots: true, index, theme,
    sel: '[data-mu-stage="v1-slider"] div:has(> div > [data-mu="fab"])',
    hide: bare ? '[data-mu-stage="v1-slider"], [data-mu-stage="v1-slider"] * { color: transparent !important; }' : '',
  })))),
  // B-31: the v1 page's segmented buttons, the one to pick in (its pill on the first segment) and the disabled one,
  // whole and with the text hidden, both themes; cut above the captions, 26 px under the body.
  ...['Efir', 'Kopiya'].flatMap((theme) => ['Rest', 'Disabled'].flatMap((s, index) => [false, true].map((bare) =>
    ({ stem: `MutantSegmented_${s}${bare ? 'Bare' : ''}_${theme}`, page: V1, stage: 'v1-seg', type: 'btn', index, theme, below: 26, hideText: bare })))),
  // B-32: the v1 page's two tooltip plates, the plain one and the one with two lines, whole and with the text hidden,
  // both themes; cut 16 px under the plate, above the two-line plate's FAB, whose outline rises to 6 px over its slot.
  ...['Efir', 'Kopiya'].flatMap((theme) => ['Plain', 'Rich'].flatMap((s, index) => [false, true].map((bare) =>
    ({ stem: `MutantTooltip_${s}${bare ? 'Bare' : ''}_${theme}`, page: V1, stage: 'v1-tip', type: 'blob', index, theme, below: 16, hideText: bare })))),
  // B-35: the v1 page's bottom sheet screen — the still card under the scrim, the tear line and the sheet with its
  // grabber, words and button — as it stands, whole and with every word hidden, both themes; and the two stages'
  // cells, whose records carry where the engine stood each body (the card on `v1-sheet-bg`, the sheet and its button
  // on `v1-sheet-win`).
  ...['Efir', 'Kopiya'].flatMap((theme) => [false, true].map((bare) =>
    ({ stem: `MutantSheet_Open${bare ? 'Bare' : ''}_${theme}`, page: V1, el: true, sel: '[data-mu-stage="v1-sheet-bg"]', index: 0, theme,
      hide: bare ? '[data-mu-stage="v1-sheet-bg"], [data-mu-stage="v1-sheet-bg"] * { color: transparent !important; }' : '' }))),
  ...['Efir', 'Kopiya'].flatMap((theme) => [
    { stem: `MutantSheet_Window_${theme}`, page: V1, stage: 'v1-sheet-win', type: 'blob', index: 0, theme, below: 0 },
    { stem: `MutantSheet_Card_${theme}`, page: V1, stage: 'v1-sheet-bg', type: 'card', index: 0, theme, below: 20 },
  ]),
  // B-36: the v1 page's three pull-to-refresh frames — pulling, refreshing, done — each its screen as it stands
  // (the header, the divider, the tongue or the pill, the list below), whole and with every word hidden, both themes,
  // with the bodies the engine built on it.
  ...['Efir', 'Kopiya'].flatMap((theme) => [['Pulling', 'a'], ['Refreshing', 'b'], ['Done', 'c']].flatMap(([s, k]) => [false, true].map((bare) => ({
    stem: `MutantPullToRefresh_${s}${bare ? 'Bare' : ''}_${theme}`, page: V1, el: true, slots: true, index: 0, theme,
    sel: `[data-mu-stage="v1-ptr-${k}"]`,
    hide: bare ? `[data-mu-stage="v1-ptr-${k}"], [data-mu-stage="v1-ptr-${k}"] * { color: transparent !important; }` : '',
  })))),
  // B-44: the later page's search bar as it stands — the focused field with its caret and clear icon, the two
  // suggestion chips, the result row with its tile, the divider and the cocoon row — whole and with every word and
  // currentColor icon hidden — the caret too, as the MVP field's bare twin — both themes, with the bodies the engine
  // built on it.
  ...['Efir', 'Kopiya'].flatMap((theme) => [false, true].map((bare) => ({
    stem: `MutantSearchBar_Focused${bare ? 'Bare' : ''}_${theme}`, page: LATER, el: true, slots: true, index: 0, theme,
    sel: '[data-mu-stage="l-search"] > div:has(> [data-mu="field"])',
    hide: bare ? '[data-mu-stage="l-search"], [data-mu-stage="l-search"] * { color: transparent !important; } [data-mu-stage="l-search"] [data-mu-anim="blink"] { visibility: hidden !important; }' : '',
  }))),
  // B-46: the later page's data table as it stands — the OSD header, the tracking dividers and four rows, the second
  // selected (its box filled and ticked) — whole and with every word and tick hidden, both themes, with the bodies
  // the engine built on it.
  ...['Efir', 'Kopiya'].flatMap((theme) => [false, true].map((bare) => ({
    stem: `MutantDataTable_Selected${bare ? 'Bare' : ''}_${theme}`, page: LATER, el: true, slots: true, index: 0, theme,
    sel: '[data-mu-stage="l-table"] > div:has(> [data-mu-divider])',
    hide: bare ? '[data-mu-stage="l-table"], [data-mu-stage="l-table"] * { color: transparent !important; }' : '',
  }))),
  // B-47: the later page's carousel as it stands — the view (a stage of its own, clipped) with the current card and
  // its calm neighbours, and the dots with the pill pointer — whole and with every word hidden, both themes, with the
  // bodies the engine built on both stages.
  ...['Efir', 'Kopiya'].flatMap((theme) => [false, true].map((bare) => ({
    stem: `MutantCarousel_Rest${bare ? 'Bare' : ''}_${theme}`, page: LATER, el: true, slots: true, index: 0, theme,
    sel: '[data-mu-stage="l-carousel"] > div:has(> [data-mu-stage="l-carousel-view"])',
    hide: bare ? '[data-mu-stage="l-carousel"], [data-mu-stage="l-carousel"] * { color: transparent !important; }' : '',
  }))),
  // B-33: the v1 page's menu as it stands — the anchor chip and the menu under it with its window on the first item —
  // whole and with every word and currentColor icon hidden, both themes, with the bodies the engine built on it.
  ...['Efir', 'Kopiya'].flatMap((theme) => [false, true].map((bare) => ({
    stem: `MutantMenu_Open${bare ? 'Bare' : ''}_${theme}`, page: V1, el: true, slots: true, index: 0, theme,
    sel: '[data-mu-stage="v1-menu"] div:has(> [data-mu="btn"][data-key="menu"])',
    hide: bare ? '[data-mu-stage="v1-menu"], [data-mu-stage="v1-menu"] * { color: transparent !important; }' : '',
  }))),
  // B-34: the v1 page's navigation drawer screen as it stands — the still cards under the scrim and the drawer over
  // them with its window on the third channel — whole and with every word hidden, both themes, with the bodies the
  // engine built on the screen's stage and on the drawer's (`v1-drawer-win`, nested).
  ...['Efir', 'Kopiya'].flatMap((theme) => [false, true].map((bare) => ({
    stem: `MutantDrawer_Open${bare ? 'Bare' : ''}_${theme}`, page: V1, el: true, slots: true, index: 0, theme,
    sel: '[data-mu-stage="v1-drawer-bg"]',
    hide: bare ? '[data-mu-stage="v1-drawer-bg"], [data-mu-stage="v1-drawer-bg"] * { color: transparent !important; }' : '',
  }))),
  // B-45: the later page's calendar (the bar with its month, weekdays and days, the 13th in its drop) and its time
  // picker (two fields and the colon), each as it stands, whole and with every word hidden, both themes, with the
  // bodies the engine built on the stage.
  ...['Efir', 'Kopiya'].flatMap((theme) => [false, true].flatMap((bare) => {
    const hide = bare ? '[data-mu-stage="l-date"], [data-mu-stage="l-date"] * { color: transparent !important; }' : '';
    return [
      { stem: `MutantDatePicker_Rest${bare ? 'Bare' : ''}_${theme}`, page: LATER, el: true, slots: true, index: 0, theme, hide,
        sel: '[data-mu-stage="l-date"] [data-mu="bar"][data-key="cal"]' },
      { stem: `MutantTimePicker_Hours${bare ? 'Bare' : ''}_${theme}`, page: LATER, el: true, slots: true, index: 0, theme, hide,
        sel: '[data-mu-stage="l-date"] div:has(> [data-mu="field"][data-key="tp-h"])' },
    ];
  })),
  // B-48: the specs page's keyframe loss, its four frames — before it, at 45 % and 95 % of the loss, and recovering
  // (`data-mu-mosh-p` 0.45, 0.95, 1.2) — each stage whole, with its bodies, its name and the loss's plan as the engine
  // holds it (`st.m`), so a fixture finds which loss of the page it was.
  ...[['Before', null], ['At45', '0.45'], ['At95', '0.95'], ['Recovering', '1.2']].map(([s, p]) => ({
    stem: `MutantKeyframeLoss_${s}_Efir`, page: SPECS, el: true, slots: true, index: 0, theme: 'Efir',
    sel: p === null ? '[data-mu-stage][data-mu-mosh]:not([data-mu-mosh-p])' : `[data-mu-stage][data-mu-mosh-p="${p}"]`,
  })),
  // B-24: the nav stage's navigation bar with its pill, whole and bare (text and currentColor icons hidden, the
  // badge's plate kept), both themes.
  ...['Efir', 'Kopiya'].flatMap((theme) => [false, true].map((bare) =>
    ({ stem: `MutantNavigationBar_Rest${bare ? 'Bare' : ''}_${theme}`, page: MVP, stage: 'nav', type: 'bar', index: 0, theme, below: 24, hideText: bare }))),
  // B-25: the list stage's two rows, each with its tile, words, trailing time or badge and the divider under it — the
  // tile the slot, the row reached by `wide` (328 less the tile's 40), down to the divider's bottom — whole and bare,
  // both themes.
  ...['Efir', 'Kopiya'].flatMap((theme) => [['Time', 0], ['Badge', 1]].flatMap(([s, index]) => [false, true].map((bare) =>
    ({ stem: `MutantListItem_${s}${bare ? 'Bare' : ''}_${theme}`, page: MVP, stage: 'list', type: 'icon', index, theme, wide: 288, below: 14, hideText: bare })))),
  // B-26: the list stage's snackbar, whole and bare, both themes; cut 8 px under it, above the progress row.
  ...['Efir', 'Kopiya'].flatMap((theme) => [false, true].map((bare) =>
    ({ stem: `MutantSnackbar_Rest${bare ? 'Bare' : ''}_${theme}`, page: MVP, stage: 'list', type: 'bar', index: 0, theme, below: 8, hideText: bare }))),
  // B-27: the list stage's progress rows as they stand — the determinate track with its value, the running band with
  // the reel — both themes; the rows are the stage column's third and fourth children.
  ...['Efir', 'Kopiya'].flatMap((theme) => [['Determinate', 3], ['Indeterminate', 4]].map(([s, n]) =>
    ({ stem: `MutantProgress_${s}_${theme}`, page: MVP, el: true, sel: `[data-mu-stage="list"] > div:nth-child(3) > div:nth-child(${n})`, index: 0, theme }))),
  // B-28: the dialog demo's frame — two still cards, the scrim, the pause band and the window — as it stands, whole and
  // with every word in it hidden, both themes.
  ...['Efir', 'Kopiya'].flatMap((theme) => [false, true].map((bare) =>
    ({ stem: `MutantDialog_Open${bare ? 'Bare' : ''}_${theme}`, page: MVP, el: true, sel: '[data-mu-stage="dialogdemo"] > div:nth-child(3)', index: 0, theme,
      hide: bare ? '[data-mu-stage="dialogdemo"], [data-mu-stage="dialogdemo"] * { color: transparent !important; }' : '' }))),
  // …and the two stages' cells, whose records carry where the engine stood each body (the window and its button on
  // `dialogwin`, the cards on `dialogdemo`) for the frame's fixture.
  ...['Efir', 'Kopiya'].flatMap((theme) => [
    { stem: `MutantDialog_Window_${theme}`, page: MVP, stage: 'dialogwin', type: 'bar', index: 0, theme, below: 24 },
    { stem: `MutantDialog_Card_${theme}`, page: MVP, stage: 'dialogdemo', type: 'card', index: 0, theme, below: 20 },
  ]),
  // B-29: the Components page's empty state — the cassette, its title and its line — as it stands, whole and with its
  // text, kept parts and reels hidden; and the stage's cell, whose record gives where the engine stood the cassette.
  ...['Efir', 'Kopiya'].flatMap((theme) => [
    ...[false, true].map((bare) => ({ stem: `MutantEmptyState_Cassette${bare ? 'Bare' : ''}_${theme}`, page: MVP, el: true, sel: '[data-mu-stage="illus"] > div:nth-child(3)', index: 0, theme,
      hide: bare ? '[data-mu-stage="illus"], [data-mu-stage="illus"] * { color: transparent !important; } [data-mu-stage="illus"] [data-keep], [data-mu-stage="illus"] [data-mu-anim] { visibility: hidden !important; }' : '' })),
    { stem: `MutantEmptyState_Stage_${theme}`, page: MVP, stage: 'illus', type: 'blob', index: 0, theme, below: 20 },
  ]),
  // B-37: the Demo page's three screens as they stand, both themes — for the eye: the sample rebuilds them from the
  // public API as it stands, whose components keep their own numbers where the page composes with its own (the owner's
  // answer of 2026-11-14).
  ...['Efir', 'Kopiya'].flatMap((theme) => [['Library', 'demoA'], ['Recording', 'demoB'], ['Settings', 'demoC']].map(([s, stage]) =>
    ({ stem: `Demo_${s}_${theme}`, page: DEMO, el: true, sel: `[data-mu-stage="${stage}"]`, index: 0, theme }))),
  // B-55: the OSD line under the cassette on the Demo page's recording screen (`46 МИН · SP · …`), the first span set in
  // the OSD role there (Handjet 20, tracking 1): what the public `osd` style is held to.
  ...['Efir', 'Kopiya'].map((theme) =>
    ({ stem: `Demo_OsdLine_${theme}`, page: DEMO, el: true, sel: '[data-mu-stage="demoB"] span[style*="letter-spacing"]', index: 0, theme })),
  { stem: 'Button_Boil50Body_Efir', page: SPECS, sel: '[data-key="spec-btn"][data-heat-k="0.5"]', index: 0, hideText: true },
];
// A probe's canvas: the slot at (20, 20) with room for the drips below and the field's margins.
export const probeSize = (p, over) => ({ w: p.w + 40, h: p.h + over + 52 });

const FACES = [
  ['Handjet', 'handjet_regular.ttf', 400],
  ['Handjet', 'handjet_semibold.ttf', 600],
  ['Rubik', 'rubik_regular.ttf', 400],
  ['Rubik', 'rubik_medium.ttf', 500],
];
const fontCss = FACES.map(([family, file, weight]) =>
  `@font-face{font-family:'${family}';font-style:normal;font-weight:${weight};font-display:block;` +
  `src:url(data:font/ttf;base64,${readFileSync(join(FONTS, file)).toString('base64')}) format('truetype')}`).join('\n');

// Runs before any page script: wraps the engine class the page will create, freezing it (see the head).
const FREEZE = `(() => {
  let E;
  const freeze = (root) => root.querySelectorAll('[data-mu-stage]').forEach((h) => { if (h.dataset.muT === undefined) h.dataset.muT = '${T}'; });
  Object.defineProperty(window, 'MutantEngine', { configurable: true, get() { return E; }, set(v) {
    E = class extends v {
      constructor(root, props) { super(root, Object.assign({}, props, { keyframeLoss: false })); (window.__muEngines = window.__muEngines || []).push(this); freeze(root); }
      setProps(p) { super.setProps(Object.assign({}, p, { keyframeLoss: false })); }
      start() { freeze(this.root); super.start(); }
    };
  } });
})();`;

// Runs before any page script on a «Копия» render: the page takes its theme from its `data-props` default, which
// the runtime reads once the document is parsed. The vendored file stays as it is; this tab's copy says «Копия».
const KOPIYA = `(() => {
  const patch = (el) => { const p = JSON.parse(el.getAttribute('data-props')); p.theme.default = 'Копия'; el.setAttribute('data-props', JSON.stringify(p)); };
  new MutationObserver((ms, o) => { const el = document.querySelector('script[data-dc-script]'); if (el) { patch(el); o.disconnect(); } })
    .observe(document, { childList: true, subtree: true });
})();`;

class Cdp {
  constructor(ws) {
    this.ws = ws; this.id = 0; this.pending = new Map(); this.listeners = [];
    ws.addEventListener('message', (event) => {
      const msg = JSON.parse(event.data);
      if (msg.id && this.pending.has(msg.id)) {
        const { ok, fail } = this.pending.get(msg.id); this.pending.delete(msg.id);
        msg.error ? fail(new Error(`${msg.error.message} ${msg.error.data ?? ''}`)) : ok(msg.result);
      } else for (const l of this.listeners) l(msg);
    });
  }
  send(method, params = {}, sessionId) {
    const id = ++this.id;
    this.ws.send(JSON.stringify({ id, method, params, sessionId }));
    return new Promise((ok, fail) => this.pending.set(id, { ok, fail }));
  }
  once(method, sessionId) {
    return new Promise((ok) => {
      const l = (msg) => { if (msg.method === method && msg.sessionId === sessionId) { this.listeners.splice(this.listeners.indexOf(l), 1); ok(msg.params); } };
      this.listeners.push(l);
    });
  }
}

async function launch(chrome) {
  const profile = mkdtempSync(join(tmpdir(), 'mutant-refs-'));
  const proc = spawn(chrome, ['--headless=new', '--remote-debugging-port=0', `--user-data-dir=${profile}`, '--allow-file-access-from-files',
    '--force-device-scale-factor=1', '--hide-scrollbars', '--disable-gpu', '--no-first-run', '--no-default-browser-check', 'about:blank'], { stdio: 'ignore' });
  const portFile = join(profile, 'DevToolsActivePort');
  for (let i = 0; i < 100 && !existsSync(portFile); i++) await sleep(100);
  const [port, path] = readFileSync(portFile, 'utf8').trim().split('\n');
  const ws = new WebSocket(`ws://127.0.0.1:${port}${path}`);
  await new Promise((ok, fail) => { ws.onopen = ok; ws.onerror = fail; });
  const version = (await fetch(`http://127.0.0.1:${port}/json/version`).then((r) => r.json())).Browser;
  const exited = new Promise((ok) => proc.once('exit', ok));
  return { cdp: new Cdp(ws), version, close: async () => { ws.close(); proc.kill(); await exited; rmSync(profile, { recursive: true, force: true, maxRetries: 5 }); } };
}

async function tab(cdp, width, url, freeze, theme = 'Efir') {
  const { targetId } = await cdp.send('Target.createTarget', { url: 'about:blank' });
  const { sessionId } = await cdp.send('Target.attachToTarget', { targetId, flatten: true });
  const errors = [];
  const listener = (msg) => {
    if (msg.sessionId !== sessionId) return;
    if (msg.method === 'Runtime.exceptionThrown') errors.push(msg.params.exceptionDetails.exception?.description ?? msg.params.exceptionDetails.text);
    if (msg.method === 'Fetch.requestPaused') {
      cdp.send('Fetch.fulfillRequest', { requestId: msg.params.requestId, responseCode: 200, responseHeaders: [{ name: 'Content-Type', value: 'text/css' }],
        body: Buffer.from(fontCss).toString('base64') }, sessionId);
    }
  };
  cdp.listeners.push(listener);
  await cdp.send('Page.enable', {}, sessionId);
  await cdp.send('Runtime.enable', {}, sessionId);
  await cdp.send('Fetch.enable', { patterns: [{ urlPattern: '*fonts.googleapis.com*' }] }, sessionId);
  await cdp.send('Emulation.setDeviceMetricsOverride', { width, height: 900, deviceScaleFactor: 1, mobile: false }, sessionId);
  if (freeze) await cdp.send('Page.addScriptToEvaluateOnNewDocument', { source: FREEZE }, sessionId);
  if (theme === 'Kopiya') await cdp.send('Page.addScriptToEvaluateOnNewDocument', { source: KOPIYA }, sessionId);
  const evaluate = async (expression) => {
    const r = await cdp.send('Runtime.evaluate', { expression, awaitPromise: true, returnByValue: true }, sessionId);
    if (r.exceptionDetails) throw new Error(r.exceptionDetails.exception?.description ?? r.exceptionDetails.text);
    return r.result.value;
  };
  if (url) {
    const loaded = cdp.once('Page.loadEventFired', sessionId);
    await cdp.send('Page.navigate', { url }, sessionId);
    await loaded;
  }
  return {
    sessionId, evaluate, errors,
    resize: (height) => cdp.send('Emulation.setDeviceMetricsOverride', { width, height, deviceScaleFactor: 1, mobile: false }, sessionId),
    shot: async (r) => Buffer.from((await cdp.send('Page.captureScreenshot', { format: 'png', clip: { x: r.x, y: r.y, width: r.w, height: r.h, scale: 1 } }, sessionId)).data, 'base64'),
    close: async () => { cdp.listeners.splice(cdp.listeners.indexOf(listener), 1); await cdp.send('Target.closeTarget', { targetId }); },
  };
}

const settle = 'new Promise(r => requestAnimationFrame(() => requestAnimationFrame(() => setTimeout(() => r(true), 100))))';

// ---- probes -------------------------------------------------------------------------------------------
async function probes(cdp) {
  const t = await tab(cdp, 800, null, false);
  await t.evaluate(`window.window = window; ${readFileSync(GENERATOR, 'utf8')}; true`);
  const out = new Map();
  for (const ref of REFERENCES.filter((r) => r.probe)) {
    const p = ref.probe, c = THEMES[ref.theme];
    const svg = await t.evaluate(`(() => {
      const G = window.MutantGenerator, gen = new G({ seed: 5, density: 1 }), P = G.K[${JSON.stringify(p.type)}];
      const w = ${p.w}, h = ${p.h}, content = ${JSON.stringify(p.content ?? null)};
      const tz = content ? [content[0] - 6, content[1] - 2, content[2] + 12, content[3] + 4] : null;
      const el = { ox: 20, oy: 20, w, h, r: Math.min(P.r, w / 2, h / 2), over: P.over, cf: P.cf, ow: P.ow || 3, dead: P.dead || 0,
        marks: P.marks ?? 1, edge: P.edge ?? 1, wob: P.wob ?? 1, round: P.round || 0, calm: 1, state: '',
        tz: tz || [-99, -99, 0, 0], cz: tz ? [tz[0] - 2, tz[1] - 2, tz[2] + 4, tz[3] + 4] : [0, 0, 0, 0] };
      const it = { el, key: ${JSON.stringify(p.key)} };
      gen.genItem(it);
      const bk = Math.floor(${T} / 125 + .001) % 4;
      const E = { g: it.g, e: 'probe', P: gen.prims(it.g, ${T}, false) };
      const fd = gen.field(E, ${T}, .35, bk, .8, 0, .8);
      return { d: gen.pathBuf(gen.march(fd, 0), .9, 5), ow: el.ow, over: el.over };
    })()`);
    const size = probeSize(p, svg.over);
    const fill = c[{ btn: 'green', card: 'surf', dot: 'surf2' }[p.type]];
    await t.resize(size.h);
    await t.evaluate(`(() => { document.documentElement.style.margin = document.body.style.margin = '0';
      document.body.innerHTML = '<svg xmlns="http://www.w3.org/2000/svg" width="${size.w}" height="${size.h}" style="display:block">' +
        '<rect width="${size.w}" height="${size.h}" fill="${c.bg}"/>' +
        '<path d="${svg.d}" fill="${fill}" fill-rule="evenodd"/>' +
        '<path d="${svg.d}" fill="none" stroke="${c.ink}" stroke-width="${svg.ow}" stroke-linejoin="round"/></svg>'; return true; })()`);
    await t.evaluate(settle);
    out.set(ref.stem, { png: await t.shot({ x: 0, y: 0, w: size.w, h: size.h }), rect: { x: 0, y: 0, ...size } });
  }
  await t.close();
  return out;
}

// ---- text probes --------------------------------------------------------------------------------------
// One label on «Эфир»'s ground in its text colour, 10 px in and 4 px down, with the repository's faces and the
// page cells' antialiased smoothing.
async function textProbes(cdp) {
  const t = await tab(cdp, 800, null, false);
  const out = new Map();
  for (const ref of REFERENCES.filter((r) => r.text)) {
    const p = ref.text, w = 360, h = Math.ceil(p.size * p.line) + 8;
    await t.evaluate(`(() => { document.documentElement.style.margin = document.body.style.margin = '0';
      document.head.innerHTML = '<style>' + ${JSON.stringify(fontCss)} + '*{-webkit-font-smoothing:antialiased}</style>';
      document.body.innerHTML = '<div style="position:relative;width:${w}px;height:${h}px;background:#10142a">' +
        '<span style="position:absolute;left:10px;top:4px;white-space:nowrap;font-family:${p.family};font-weight:${p.weight};font-size:${p.size}px;' +
        'line-height:${p.line};letter-spacing:${p.spacing}px;color:#e9f2e4">${p.text}</span></div>'; return true; })()`);
    await t.evaluate('document.fonts.ready.then(() => true)');
    await t.evaluate(settle);
    out.set(ref.stem, { png: await t.shot({ x: 0, y: 0, w, h }), rect: { x: 0, y: 0, w, h, ...p, left: 10, top: 4, ground: '#10142a', ink: '#e9f2e4' } });
  }
  await t.close();
  return out;
}

// ---- venom probes --------------------------------------------------------------------------------------
async function venoms(cdp) {
  const t = await tab(cdp, 800, null, false);
  await t.evaluate(`window.window = window; ${readFileSync(GENERATOR, 'utf8')}; true`);
  const cases = JSON.parse(readFileSync(join(DESIGN, 'mutant-golden.json'), 'utf8')).venom;
  const out = new Map();
  for (const ref of REFERENCES.filter((r) => r.venom)) {
    const v = cases.find((c) => c.case === ref.venom.case);
    const r = await t.evaluate(`(() => {
      const gen = new window.MutantGenerator({ seed: 5 }), el = ${JSON.stringify(v.el)}, fx = ${JSON.stringify(v.fx)};
      const { SF, S } = gen.venomShapes(el, ${JSON.stringify(v.key)}, fx, ${ref.venom.p});
      const f = (x) => x.toFixed(1), X0 = Math.min(el.ox, el.ox + fx.dx) - 30, Y0 = Math.min(el.oy, el.oy + fx.dy) - 30,
        X1 = Math.max(el.ox, el.ox + fx.dx) + el.w + 30, Y1 = Math.max(el.oy, el.oy + fx.dy) + el.h + 30;
      const flt = '<filter id="goo" filterUnits="userSpaceOnUse" x="' + f(X0) + '" y="' + f(Y0) + '" width="' + f(X1 - X0) + '" height="' + f(Y1 - Y0) +
        '" color-interpolation-filters="sRGB"><feGaussianBlur stdDeviation="3.2"/><feColorMatrix values="1 0 0 0 0 0 1 0 0 0 0 0 1 0 0 0 0 0 18 -8.2"/></filter>';
      const W = Math.ceil(X1) + 10, H = Math.ceil(Y1) + 10;
      document.documentElement.style.margin = document.body.style.margin = '0';
      document.body.innerHTML = '<svg xmlns="http://www.w3.org/2000/svg" width="' + W + '" height="' + H + '" style="display:block"><defs>' + flt + '</defs>' +
        '<rect width="' + W + '" height="' + H + '" fill="${THEMES.Efir.bg}"/>' +
        '<g fill="${THEMES.Efir.ink}" stroke="${THEMES.Efir.ink}" stroke-width="' + el.ow * 2 + '" stroke-linejoin="round"><g filter="url(#goo)">' + SF + '</g>' + S + '</g>' +
        '<g fill="' + el.fill + '"><g filter="url(#goo)">' + SF + '</g>' + S + '</g></svg>';
      return { w: W, h: H };
    })()`);
    await t.resize(r.h);
    await t.evaluate(settle);
    out.set(ref.stem, { png: await t.shot({ x: 0, y: 0, w: r.w, h: r.h }), rect: { x: 0, y: 0, w: r.w, h: r.h, venom: { ...ref.venom, el: v.el, key: v.key, fx: v.fx } } });
  }
  await t.close();
  return out;
}

// ---- page cells ---------------------------------------------------------------------------------------
async function pageCells(cdp, page, refs, theme = 'Efir') {
  const t = await tab(cdp, 1720, pathToFileURL(join(DESIGN, page)).href, true, theme);
  const ready = `(() => { const es = window.__muEngines || []; return !document.querySelector('x-dc') && es.length > 0 &&
    es.every(e => e.stages.length && e.stages.every(s => s.built) && e.items.every(it => !it.H || it.lk !== undefined)); })()`;
  // The engine draws only stages in view: the template first, then the viewport as tall as the page.
  for (let i = 0; i < 300 && !(await t.evaluate("!document.querySelector('x-dc')")); i++) await sleep(100);
  await t.resize(await t.evaluate('document.documentElement.scrollHeight'));
  for (let i = 0; i < 300 && !(await t.evaluate(ready)); i++) await sleep(100);
  if (!(await t.evaluate(ready))) throw new Error(`${page}: the engine never finished drawing`);
  await t.evaluate('document.fonts.ready.then(() => true)');
  // The page measured its slots before the faces loaded: rebuild every stage once they are in. A rebuild
  // rewrites the Tape (`tapeHtml`: no band, no noise, the grain untranslated), and `stageStep` steps a stage
  // only when its tick changes, which under a frozen time it never does: forget the tick, or the frozen
  // stage keeps the bare Tape.
  await t.evaluate(`(() => { for (const e of window.__muEngines) e.stages.forEach(s => { s.built = false; s.fi = undefined; }); return true; })()`);
  for (let i = 0; i < 300 && !(await t.evaluate(ready)); i++) await sleep(100);
  const height = await t.evaluate('document.documentElement.scrollHeight');
  await t.resize(height);
  await t.evaluate(`(() => { const s = document.createElement('style'); s.textContent = '*{-webkit-font-smoothing:antialiased}'; document.head.appendChild(s); return true; })()`);
  for (let i = 0; i < 300 && !(await t.evaluate(ready)); i++) await sleep(100);
  await t.evaluate(settle);
  if (t.errors.length) throw new Error(`${page}: ${t.errors.join('; ')}`);
  const out = new Map();
  for (const ref of refs) {
    if (ref.el) {
      out.set(ref.stem, await elementCell(t, ref));
      continue;
    }
    // Text goes transparent; content that paints a box of its own (a field's block caret, kept in the clean
    // zone by data-keep) is hidden without leaving the layout, so the zone the engine measured stays.
    // The stage the slot stands on: named by the reference, or found from its selector.
    const stageKey = ref.stage ?? (await t.evaluate(`(() => { const n = document.querySelector(${JSON.stringify(ref.sel ?? '')}); const h = n && n.closest('[data-mu-stage]'); return h ? h.dataset.muStage : null; })()`));
    if (!stageKey) throw new Error(`${ref.stem}: no stage found`);
    const hide = ref.hideText ? `[data-mu-stage="${stageKey}"], [data-mu-stage="${stageKey}"] * { color: transparent !important; } [data-mu-stage="${stageKey}"] [data-keep], [data-mu-stage="${stageKey}"] [data-mu-anim] { visibility: hidden !important; }` : '';
    await t.evaluate(`(() => { const s = document.getElementById('mutant-hide') || document.head.appendChild(Object.assign(document.createElement('style'), { id: 'mutant-hide' }));
      s.textContent = ${JSON.stringify(hide)}; return true; })()`);
    if (page === SPECS) await t.evaluate(`(() => { for (const h of document.querySelectorAll('[data-mu-stage]')) h.style.transform = 'none'; return true; })()`);
    await t.evaluate(settle);
    const r = await t.evaluate(`(() => {
      const scope = ${JSON.stringify(ref.stage ?? null)} ? document.querySelector('[data-mu-stage="${ref.stage}"]') : document;
      const n = scope && (${JSON.stringify(ref.sel ?? null)} ? scope.querySelector(${JSON.stringify(ref.sel ?? '')}) : scope.querySelectorAll('[data-mu="${ref.type}"]')[${ref.index}]);
      if (!n) return { error: 'no slot ${ref.sel ?? ref.type} on ${ref.stage ?? 'the page'}' };
      const host = n.closest('[data-mu-stage]');
      const e = window.__muEngines.find(x => x.items.some(it => it.n === n)), it = e.items.find(it => it.n === n), el = it.el;
      const st = e.stages.find(s => s.host === host);
      const hr = host.getBoundingClientRect();
      // The slot with what the body may draw around it: the field's margins and the drips' overhang — cut to the
      // stage where the stage clips its content (the spec page's frames do).
      const cs = getComputedStyle(host), clips = cs.overflow === 'hidden';
      // A whole cell (label in) stops above the page's caption, 34 px under the slot: \`below\` px under it; \`wide\`
      // reaches a label beside the slot.
      let x0 = Math.round(el.ox - 12), y0 = Math.round(el.oy - 15), x1 = x0 + Math.round(el.w + 24) + ${ref.wide ?? 0}, y1 = ${JSON.stringify(ref.below ?? null)} !== null ? Math.round(el.oy + el.h + ${ref.below ?? 0}) : y0 + Math.round(el.h + el.over + 45);
      if (clips) { x0 = Math.max(0, x0); y0 = Math.max(0, y0); x1 = Math.min(host.offsetWidth, x1); y1 = Math.min(host.offsetHeight, y1); }
      return { x: Math.round(hr.left + scrollX) + x0, y: Math.round(hr.top + scrollY) + y0, w: x1 - x0, h: y1 - y0,
        stageClip: clips ? [host.offsetWidth, host.offsetHeight, parseFloat(cs.borderTopLeftRadius) || 0] : null,
        tape: (() => { const tp = host.querySelector('[data-mu-tape]'); return tp ? { x: tp.offsetLeft, y: tp.offsetTop, w: st.W, h: st.H, t: +(host.dataset.muT ?? 0) } : null; })(),
        slot: { key: it.key, stage: st.key, origin: [x0, y0], ox: el.ox, oy: el.oy, w: el.w, h: el.h, r: el.r, over: el.over, cf: el.cf, ow: el.ow,
          wob: el.wob, round: el.round, calm: el.calm, dead: el.dead, marks: el.marks, edge: el.edge, tz: el.tz, cz: el.cz,
          fill: el.fill, hl: el.hl, slip: el.slip, shadow: el.shadow, stroke: el.stroke, state: el.state,
          drawnOutline: it.H ? +it.H.O.getAttribute('stroke-width') : el.ow, still: !!it.stat, heat: it.heat, brackets: !!el.brackets,
          background: getComputedStyle(host).backgroundColor },
        stageSlots: st.items.map((x) => ({ key: x.key, ox: x.el.ox, oy: x.el.oy, w: x.el.w, h: x.el.h, r: x.el.r, over: x.el.over, cf: x.el.cf, ow: x.el.ow,
          wob: x.el.wob, round: x.el.round, calm: x.el.calm, dead: x.el.dead, marks: x.el.marks, edge: x.el.edge, tz: x.el.tz, cz: x.el.cz,
          fill: x.el.fill, hl: x.el.hl, slip: x.el.slip, shadow: x.el.shadow, stroke: x.el.stroke, state: x.el.state, still: !!x.stat, t: x.el.tFix,
          heat: x.el.hkFix ?? x.heat, pour: x.el.pourFix, slide: x.el.slideFix, brackets: !!x.el.brackets, drawnOutline: x.H ? +x.H.O.getAttribute('stroke-width') : x.el.ow })) };
    })()`);
    if (r.error) throw new Error(`${ref.stem}: ${r.error}`);
    out.set(ref.stem, { png: await t.shot(r), rect: r });
  }
  await t.close();
  return out;
}

// An element of a page cropped as it stands. An icon or a pattern swatch is cut to its own box; an illustration to
// its art (`[data-mu-stage] > div > div`, the box its parts stand in) with room for the drips below, its stage
// slots recorded as for a body. Recorded: the crop, the art's place in it, the ground behind it, the colour the
// element inherits and its corner radius.
async function elementCell(t, ref) {
  const scope = ref.stage ? `[data-mu-stage="${ref.stage}"]` : null;
  const hide = ref.hideText
    ? `${scope}, ${scope} * { color: transparent !important; } ${scope} [data-keep], ${scope} [data-mu-anim] { visibility: hidden !important; }`
    : (ref.hide ?? '');
  await t.evaluate(`(() => { const s = document.getElementById('mutant-hide') || document.head.appendChild(Object.assign(document.createElement('style'), { id: 'mutant-hide' }));
    s.textContent = ${JSON.stringify(hide)}; return true; })()`);
  await t.evaluate(settle);
  const r = await t.evaluate(`(() => {
    const host = ${JSON.stringify(scope)} ? document.querySelector(${JSON.stringify(scope)}) : null;
    const n = host ? host.querySelector(':scope > div:not([data-mu-layer]) > div') : document.querySelectorAll(${JSON.stringify(ref.sel ?? '')})[${ref.index ?? 0}];
    if (!n) return { error: 'no element' };
    const b = n.getBoundingClientRect();
    let g = n.parentElement; while (g && getComputedStyle(g).backgroundColor === 'rgba(0, 0, 0, 0)') g = g.parentElement;
    const pad = host ? [24, 12, 24, 36] : [0, 0, 0, 0];
    const x = Math.round(b.left + scrollX) - pad[0], y = Math.round(b.top + scrollY) - pad[1];
    const out = { x, y, w: Math.round(b.width) + pad[0] + pad[2], h: Math.round(b.height) + pad[1] + pad[3], art: [pad[0], pad[1], Math.round(b.width), Math.round(b.height)],
      background: getComputedStyle(g).backgroundColor, color: getComputedStyle(n).color, radius: parseFloat(getComputedStyle(n).borderTopLeftRadius) || 0,
      // A grid of 1fr columns puts an element at a fraction of a pixel; Chrome draws it there, so the fixture does.
      subpixel: [+(b.left + scrollX - Math.round(b.left + scrollX)).toFixed(4), +(b.top + scrollY - Math.round(b.top + scrollY)).toFixed(4)] };
    if (host) {
      const e = window.__muEngines.find(x => x.stages.some(s => s.host === host)), st = e.stages.find(s => s.host === host);
      const hr = host.getBoundingClientRect();
      out.artInStage = [Math.round(b.left - hr.left), Math.round(b.top - hr.top)];
      out.stageSlots = st.items.map((s) => ({ key: s.key, ox: s.el.ox, oy: s.el.oy, w: s.el.w, h: s.el.h, r: s.el.r, over: s.el.over, cf: s.el.cf, ow: s.el.ow,
        wob: s.el.wob, round: s.el.round, calm: s.el.calm, marks: s.el.marks, edge: s.el.edge, tz: s.el.tz, cz: s.el.cz, fill: s.el.fill, hl: s.el.hl,
        drawnOutline: s.H ? +s.H.O.getAttribute('stroke-width') : s.el.ow }));
    }
    // B-30: the bodies inside the element, as a component's cell records them, and the crop's place on their stage.
    if (${JSON.stringify(!!ref.slots)}) {
      const sh = n.closest('[data-mu-stage]'), e = window.__muEngines.find(x => x.stages.some(s => s.host === sh)), st = e.stages.find(s => s.host === sh);
      const hr = sh.getBoundingClientRect();
      out.origin = [Math.round(b.left - hr.left), Math.round(b.top - hr.top)];
      // B-48: the stage's name (it seeds the bodies) and its keyframe loss, when one stands frozen on it.
      out.stage = sh.dataset.muStage;
      if (st.m && !st.m.loading) out.mosh = { y0: st.m.y0, rows: st.m.rows, dur: st.m.dur };
      // B-47: and the bodies of the stages nested in the element (a carousel's clipped view), each slot in its own
      // stage's frame, as the kit seeds that view's bodies on a stage of their own.
      const nested = window.__muEngines.flatMap((x) => x.stages).filter((o) => o !== st && n.contains(o.host));
      out.stageSlots = [st, ...nested].flatMap((o) => o.items).filter((s) => n.contains(s.n)).map((s) => ({ key: s.key, ox: s.el.ox, oy: s.el.oy, w: s.el.w, h: s.el.h, r: s.el.r, over: s.el.over,
        cf: s.el.cf, ow: s.el.ow, wob: s.el.wob, round: s.el.round, calm: s.el.calm, dead: s.el.dead, marks: s.el.marks, edge: s.el.edge, tz: s.el.tz, cz: s.el.cz,
        fill: s.el.fill, hl: s.el.hl, state: s.el.state, still: !!s.stat, heat: s.el.hkFix ?? s.heat, drawnOutline: s.H ? +s.H.O.getAttribute('stroke-width') : s.el.ow }));
    }
    return out;
  })()`);
  if (r.error) throw new Error(`${ref.stem}: ${r.error}`);
  return { png: await t.shot(r), rect: r };
}

// ---- main ---------------------------------------------------------------------------------------------
const argv = process.argv.slice(2);
const check = argv.includes('--check');
const sha = (file) => createHash('sha256').update(readFileSync(file)).digest('hex');
const inputs = () => ({
  pages: Object.fromEntries([...new Set(REFERENCES.filter((r) => r.page).map((r) => r.page))].map((p) => [p, sha(join(DESIGN, p))])),
  scripts: { 'mutant-generator.js': sha(GENERATOR), 'mutant-engine.js': sha(ENGINE), 'support.js': sha(join(DESIGN, 'support.js')) },
  fonts: Object.fromEntries(FACES.map(([, file]) => [file, sha(join(FONTS, file))])),
});

if (argv.includes('--manifest')) {
  const m = JSON.parse(readFileSync(join(OUT, 'manifest.json'), 'utf8'));
  const now = inputs();
  const moved = [
    ...['pages', 'scripts', 'fonts'].flatMap((k) => Object.keys({ ...now[k], ...m[k] }).filter((f) => now[k][f] !== m[k][f]).map((f) => `${f} changed`)),
    ...REFERENCES.filter((r) => !m.references.some((x) => x.stem === r.stem)).map((r) => `${r.stem} not rendered`),
    ...m.references.filter((r) => !existsSync(join(OUT, `${r.stem}.png`))).map((r) => `${r.stem}.png missing`),
  ];
  if (moved.length) { console.error(`design references are stale: ${moved.join(', ')}; run make references`); process.exit(1); }
  console.log(`design-references: ${m.references.length} references match the vendored pages, scripts and fonts`);
  process.exit(0);
}

const chrome = argv.includes('--chrome') ? argv[argv.indexOf('--chrome') + 1] : '/Applications/Google Chrome.app/Contents/MacOS/Google Chrome';
const { cdp, version, close } = await launch(chrome);
const shots = new Map();
try {
  for (const [k, v] of await probes(cdp)) shots.set(k, v);
  for (const [k, v] of await venoms(cdp)) shots.set(k, v);
  for (const [k, v] of await textProbes(cdp)) shots.set(k, v);
  for (const page of [...new Set(REFERENCES.filter((r) => r.page).map((r) => r.page))]) {
    for (const theme of ['Efir', 'Kopiya']) {
      const refs = REFERENCES.filter((r) => r.page === page && (r.theme ?? 'Efir') === theme);
      if (refs.length) for (const [k, v] of await pageCells(cdp, page, refs, theme)) shots.set(k, v);
    }
  }
} finally { await close(); }

// Chrome's screenshots, decoded (8-bit RGB or RGBA, non-interlaced, as CDP writes them), to measure two renders apart.
function pixels(png) {
  const w = png.readUInt32BE(16), h = png.readUInt32BE(20), type = png[25], bpp = type === 6 ? 4 : 3;
  const chunks = [];
  for (let o = 8; o < png.length;) {
    const len = png.readUInt32BE(o), kind = png.toString('ascii', o + 4, o + 8);
    if (kind === 'IDAT') chunks.push(png.subarray(o + 8, o + 8 + len));
    o += 12 + len;
  }
  const raw = inflateSync(Buffer.concat(chunks)), stride = w * bpp, out = Buffer.alloc(h * stride);
  for (let y = 0; y < h; y++) {
    const f = raw[y * (stride + 1)], src = raw.subarray(y * (stride + 1) + 1, (y + 1) * (stride + 1)), row = y * stride;
    for (let x = 0; x < stride; x++) {
      const a = x >= bpp ? out[row + x - bpp] : 0, b = y ? out[row - stride + x] : 0, c = x >= bpp && y ? out[row - stride + x - bpp] : 0;
      const p = a + b - c, pa = Math.abs(p - a), pb = Math.abs(p - b), pc = Math.abs(p - c);
      const pred = [0, a, b, (a + b) >> 1, pa <= pb && pa <= pc ? a : pb <= pc ? b : c][f];
      out[row + x] = (src[x] + pred) & 255;
    }
  }
  return { w, h, bpp, data: out };
}

// Two renders of one stem apart: pixels whose channels differ by more than 16 (parity's threshold).
function apart(a, b) {
  const p = pixels(a), q = pixels(b);
  if (p.w !== q.w || p.h !== q.h) return Infinity;
  let n = 0;
  for (let i = 0; i < p.w * p.h; i++) {
    let m = 0;
    for (let c = 0; c < 3; c++) m = Math.max(m, Math.abs(p.data[i * p.bpp + c] - q.data[i * q.bpp + c]));
    if (m > 16) n++;
  }
  return n;
}

// A render that is not byte-identical passes only when it stays within the body's floor (research §1.8) of the
// committed one: Chrome rasterises some pages' patterns a little apart from run to run (B-15: the Button stage's
// disabled and outlined cells, up to 25 pixels changed, 2 of them by more than 16). The stems that did are printed.
const FLOOR = 5;
const stale = [];
const unsteady = [];
if (!check) mkdirSync(OUT, { recursive: true });
for (const ref of REFERENCES) {
  const target = join(OUT, `${ref.stem}.png`), { png } = shots.get(ref.stem);
  if (check) {
    if (!existsSync(target)) { stale.push(ref.stem); continue; }
    const committed = readFileSync(target);
    if (committed.equals(png)) continue;
    const n = apart(committed, png);
    if (n > FLOOR) stale.push(`${ref.stem} (${n} px)`); else unsteady.push(`${ref.stem} (${n} px)`);
  } else if (existsSync(target) && !readFileSync(target).equals(png) && apart(readFileSync(target), png) <= FLOOR) {
    // The other of Chrome's renders of an unchanged cell: keep the committed one, so tests stay on what they measured.
    unsteady.push(`${ref.stem} (kept)`);
  } else writeFileSync(target, png);
}
if (unsteady.length) console.log(`design-references: not byte-identical, within the floor: ${unsteady.join(', ')}`);
const manifest = {
  browser: version, scale: 1, time: T, ...inputs(),
  references: REFERENCES.map((r) => ({ stem: r.stem, page: r.page, stage: r.stage, type: r.type, index: r.index, sel: r.sel, hideText: r.hideText, probe: r.probe?.name, theme: r.theme, ...shots.get(r.stem).rect })),
};
if (check) {
  if (stale.length) { console.error(`design references differ from a fresh render in ${version}: ${stale.join(', ')}`); process.exit(1); }
  console.log(`design-references: ${REFERENCES.length} references match a fresh render in ${version}`);
} else {
  writeFileSync(join(OUT, 'manifest.json'), JSON.stringify(manifest, null, 2) + '\n');
  console.log(`design-references: ${REFERENCES.length} references written with ${version}`);
}
