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

const ROOT = resolve(import.meta.dirname, '..');
const DESIGN = join(ROOT, 'reference/design-system');
const FONTS = join(ROOT, 'mutant-core/src/commonMain/composeResources/font');
const OUT = join(ROOT, 'mutant-core/src/desktopTest/snapshots/design');
const GENERATOR = join(DESIGN, 'mutant-generator.js');
const ENGINE = join(DESIGN, 'mutant-engine.js');
const MVP = 'mutant-ui Компоненты.dc.html';
const SPECS = 'mutant-ui Анимационные спеки.dc.html';
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
export const REFERENCES = [
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

async function tab(cdp, width, url, freeze) {
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
async function pageCells(cdp, page, refs) {
  const t = await tab(cdp, 1720, pathToFileURL(join(DESIGN, page)).href, true);
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
      let x0 = Math.round(el.ox - 12), y0 = Math.round(el.oy - 15), x1 = x0 + Math.round(el.w + 24), y1 = y0 + Math.round(el.h + el.over + 45);
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
  for (const page of [...new Set(REFERENCES.filter((r) => r.page).map((r) => r.page))]) {
    for (const [k, v] of await pageCells(cdp, page, REFERENCES.filter((r) => r.page === page))) shots.set(k, v);
  }
} finally { await close(); }

const stale = [];
if (!check) mkdirSync(OUT, { recursive: true });
for (const ref of REFERENCES) {
  const target = join(OUT, `${ref.stem}.png`), { png } = shots.get(ref.stem);
  if (check) { if (!existsSync(target) || !readFileSync(target).equals(png)) stale.push(ref.stem); } else writeFileSync(target, png);
}
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
