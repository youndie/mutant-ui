// B-03: the generator's goldens, emitted by the vendored mutant-generator.js itself (research D2, D3).
//
// Runs reference/design-system/mutant-generator.js untouched in Node and writes what it returns for a
// fixed set of instances and frames into Kotlin sources under mutant-core's commonTest, one file per
// instance, so the same goldens are checked on the JVM, on iOS and in the browser. Slots are built the
// way the engine's makeItem builds them; frames the way its drawItem draws them.
//
//   node scripts/gen-goldens.mjs           write the goldens
//   node scripts/gen-goldens.mjs --check   exit 1 if the files on disk are not what the generator emits
import fs from 'node:fs';
import path from 'node:path';
import vm from 'node:vm';
import { fileURLToPath } from 'node:url';

const root = path.resolve(path.dirname(fileURLToPath(import.meta.url)), '..');
const design = path.join(root, 'reference', 'design-system');
const out = path.join(root, 'mutant-core', 'src', 'commonTest', 'kotlin', 'io', 'github', 'youndie', 'mutant', 'gen', 'goldens');
const check = process.argv.includes('--check');

const ctx = { window: {}, Math, Float32Array, Int32Array, Uint8Array, NaN, Infinity, console };
vm.createContext(ctx);
vm.runInContext(fs.readFileSync(path.join(design, 'mutant-generator.js'), 'utf8'), ctx, { filename: 'mutant-generator.js' });
const G = ctx.window.MutantGenerator;
const gen = new G({ seed: 5, density: 1 });
const K = G.K;
const C = G.C;

// ---- numbers as Kotlin literals -----------------------------------------------------------------------
const num = (x) => {
  if (typeof x === 'boolean') return x ? '1.0' : '0.0';
  if (x === undefined || x === null) return '0.0';
  if (!Number.isFinite(x)) throw new Error(`not finite: ${x}`);
  if (Object.is(x, -0) || x === 0) return '0.0';
  const s = String(x);
  return Number.isInteger(x) && !s.includes('e') ? s + '.0' : s;
};
const arr = (xs) => {
  const lines = [];
  for (let i = 0; i < xs.length; i += 6) lines.push(xs.slice(i, i + 6).map(num).join(', '));
  return xs.length ? `doubleArrayOf(\n${lines.map((l) => '            ' + l + ',').join('\n')}\n        )` : 'doubleArrayOf()';
};
const str = (s) => {
  if (!/^[0-9MQZ, .\-]*$/.test(s)) throw new Error('unexpected path characters');
  // Kotlin string constants are limited in length on the JVM; split long paths into concatenated chunks.
  const parts = [];
  for (let i = 0; i < s.length; i += 4000) parts.push(`"${s.slice(i, i + 4000)}"`);
  return parts.length ? parts.join(' +\n            ') : '""';
};

// ---- slots, as the engine's makeItem builds them ------------------------------------------------------
function slot(type, w, h, { ox = 20, oy = 20, calm = 1, content = null } = {}) {
  const P = K[type];
  const tz = content ? [content[0] - 6, content[1] - 2, content[2] + 12, content[3] + 4] : null;
  return {
    ox, oy, w, h,
    r: Math.min(P.r, w / 2, h / 2),
    over: P.over * (calm < 1 ? 0.4 + 0.6 * calm : 1),
    cf: P.cf,
    ow: P.ow || 3,
    fill: C[P.fill],
    hl: '#ffffff',
    dead: P.dead || 0,
    marks: P.marks ?? 1,
    edge: P.edge ?? 1,
    wob: P.wob ?? 1,
    round: P.round || 0,
    calm,
    state: '',
    tz: tz || [-99, -99, 0, 0],
    cz: tz ? [tz[0] - 2, tz[1] - 2, tz[2] + 4, tz[3] + 4] : [0, 0, 0, 0],
  };
}

const centred = (w, h, cw, ch) => [(w - cw) / 2, (h - ch) / 2, cw, ch];
const A = [0, 0, 0]; // rest, rest, full heat
const CASES = [
  ['btn', 'a', slot('btn', 200, 48, { content: centred(200, 48, 96, 18) }), A],
  ['btn', 'b', slot('btn', 120, 40, { ox: 12.5, oy: 33.25, calm: 0.3 }), null],
  ['btn', 'still', slot('btn', 200, 48, { content: centred(200, 48, 96, 18) }), A, true],
  ['fab', 'a', slot('fab', 56, 56, { content: centred(56, 56, 24, 24) }), A],
  ['fab', 'b', slot('fab', 136, 56, { ox: 12.5, oy: 33.25, calm: 1.7, content: [16, 16, 104, 24] }), null],
  ['icon', 'a', slot('icon', 48, 48, { content: centred(48, 48, 24, 24) }), A],
  ['icon', 'b', slot('icon', 40, 40, { ox: 12.5, oy: 33.25, calm: 0.3 }), null],
  ['chip', 'a', slot('chip', 84, 32, { content: centred(84, 32, 52, 16) }), A],
  ['chip', 'b', slot('chip', 120, 32, { ox: 12.5, oy: 33.25, calm: 1.7 }), null],
  ['card', 'a', slot('card', 328, 80, { content: [16, 14, 200, 36] }), A],
  ['card', 'b', slot('card', 220, 120, { ox: 12.5, oy: 33.25, calm: 0.3, content: [16, 16, 120, 40] }), null],
  ['field', 'a', slot('field', 260, 52, { content: [14, 17, 180, 18] }), A],
  ['field', 'b', slot('field', 320, 52, { ox: 12.5, oy: 33.25 }), null],
  ['track', 'a', slot('track', 52, 28), A],
  ['track', 'b', slot('track', 64, 28, { ox: 12.5, oy: 33.25, calm: 1.7 }), null],
  ['thumb', 'a', slot('thumb', 22, 22), A],
  ['thumb', 'b', slot('thumb', 28, 28, { ox: 12.5, oy: 33.25, calm: 0.3 }), null],
  ['box', 'a', slot('box', 22, 22), A],
  ['box', 'b', slot('box', 30, 30, { ox: 12.5, oy: 33.25, calm: 1.7 }), null],
  ['dot', 'a', slot('dot', 22, 22), A],
  ['dot', 'b', slot('dot', 30, 30, { ox: 12.5, oy: 33.25, calm: 1.7 }), null],
  ['bar', 'a', slot('bar', 328, 56, { content: [16, 19, 220, 18] }), A],
  ['bar', 'b', slot('bar', 204, 36, { ox: 12.5, oy: 33.25, calm: 0.3 }), null],
  ['pill', 'a', slot('pill', 56, 30, { content: centred(56, 30, 32, 14) }), A],
  ['pill', 'b', slot('pill', 90, 32, { ox: 12.5, oy: 33.25 }), null],
  ['blob', 'a', slot('blob', 160, 120), A],
  ['blob', 'b', slot('blob', 240, 60, { ox: 12.5, oy: 33.25 }), null],
];
// Rest, rest, hot: t and heat. "a" instances at heat 1, "b" instances at heat 0.5.
const FRAMES_A = [[5000, 0], [7375, 0], [9340, 1]];
const FRAMES_B = [[5000, 0], [12100, 0.5], [15990, 0]];

// ---- frames, as the engine's drawItem draws them ------------------------------------------------------
let uid = 0;
function frame(g, t, heat) {
  const el = g.el, hot = heat > 0;
  const bk = Math.floor(t / (hot ? 125 / 1.5 : 125) + 0.001) % 4;
  const bo0 = 0.8 * el.calm, boE = bo0 * (1 + 2 * heat) + 0.6 * heat;
  const E = { g, e: 'golden-' + uid++, P: gen.prims(g, t, hot) };
  if (heat > 0) E.P.push(...gen.bubbles(g, t, heat));
  const fd = gen.field(E, t, 0.35 * el.calm, bk, boE, heat, bo0);
  return {
    t, heat, bk, boil: boE,
    prims: E.P.flat(),
    grid: [fd.nx, fd.ny, fd.x0, fd.y0, fd.s],
    contour: gen.pathBuf(gen.march(fd, 0), 0.9, 5),
    highlight: gen.pathBuf(gen.march(fd, -3.4, Math.ceil((el.r + 19) / fd.s), Math.floor((el.h + 7) / fd.s)), 1.1),
    marks: g.ML.length ? gen.markPath(g, t, bk, boE) : '',
  };
}

const ROLES = { [C.green]: 0, [C.mag]: 1, [C.cyan]: 2, '#fff': 3, [C.coc1]: 4, [C.surf2]: 5, [C.coc2]: 6, [C.ink]: 7 };
function deadPixels(g) {
  const s = gen.deadPx(g);
  const px = [...s.matchAll(/<rect x="([^"]+)" y="([^"]+)" width="([^"]+)" height="[^"]+" fill="([^"]+)"\/>/g)].flatMap((m) => {
    const role = ROLES[m[4]];
    if (role === undefined) throw new Error('dead pixel colour ' + m[4]);
    return [+m[1], +m[2], +m[3], role];
  });
  const line = s.match(/<line x1="([^"]+)" x2="([^"]+)" y1="([^"]+)"/);
  return { px, line: [+line[1], +line[2], +line[3]] };
}

const camel = (s) => s.replace(/[-_](.)/g, (_, c) => c.toUpperCase());
const files = new Map();
const names = [];
for (const [type, variant, el, frames, still] of CASES) {
  const name = `${type}-${variant}`;
  const key = `${type}/${variant}`;
  const it = { el, key, stat: !!still };
  gen.genItem(it);
  const g = it.g;
  const fr = (frames ? FRAMES_A : FRAMES_B).map(([t, heat]) => frame(g, t, heat));
  const dead = el.dead ? deadPixels(g) : { px: [], line: [] };
  const ident = camel(`golden-${type}-${variant}`);
  names.push(ident);
  const flow = g.ML.flatMap((m) => [m.k === 'a' ? 1 : 0, m.x, m.y, m.L, m.dx, m.a, m.hk, m.dir, m.fl, m.sl, m.wd, m.dp, m.P, m.off, m.sd]);
  const tiles = [];
  if (g.mz) {
    // The drawn tiles are re-derived from the SVG edgeMos returns.
    const svg = gen.edgeMos({ ...g, mz: undefined });
    for (const m of svg.matchAll(/x="([^"]+)" y="([^"]+)" width="([^"]+)" height="[^"]+" fill="([^"]+)" opacity="([^"]+)"/g)) {
      tiles.push(+m[1], +m[2], +m[3], m[4] === '#000' ? 1 : 0, +m[5]);
    }
  }
  const side = g.ms === undefined ? -1 : { top: 0, left: 1, right: 2 }[g.ms];
  const kt = `// Generated by scripts/gen-goldens.mjs from reference/design-system/mutant-generator.js. Do not edit.
// Not formatted: make check holds every byte of it against the generator instead (research D3).
@file:Suppress("ktlint")

package io.github.youndie.mutant.gen.goldens

internal val ${ident}: GoldenInstance =
    GoldenInstance(
        name = "${name}",
        key = "${key}",
        still = ${!!still},
        slot = ${arr([el.ox, el.oy, el.w, el.h, el.r, el.over, el.cf, el.wob, el.round, el.calm, el.dead, el.marks, el.edge, ...el.tz, ...el.cz])},
        k = ${num(g.k)},
        bottomRadius = ${num(g.rb)},
        wave = ${arr(g.wave)},
        shape = ${arr(g.sh)},
        drips = ${arr(g.D.flatMap((d) => [d.x, d.b, d.Lmax, d.L0, d.P, d.off, d.u, d.drip]))},
        flowLines = ${arr(flow)},
        mosaic = ${arr((g.mz || []).flat())},
        mosaicTiles = ${arr(tiles)},
        mosaicSide = ${side},
        deadZone = ${arr(g.dz || [])},
        deadPixels = ${arr(dead.px)},
        deadLine = ${arr(dead.line)},
        frames =
            listOf(
${fr.map((f) => `                GoldenFrame(
                    t = ${num(f.t)},
                    heat = ${num(f.heat)},
                    boilPhase = ${f.bk},
                    boil = ${num(f.boil)},
                    primitives = ${arr(f.prims).replace(/\n {8}/g, '\n                ')},
                    grid = ${arr(f.grid).replace(/\n {8}/g, '\n                ')},
                    contour =
                        ${str(f.contour).replace(/\n {12}/g, '\n                        ')},
                    highlight =
                        ${str(f.highlight).replace(/\n {12}/g, '\n                        ')},
                    flowLines =
                        ${str(f.marks).replace(/\n {12}/g, '\n                        ')},
                ),`).join('\n')}
            ),
    )
`;
  files.set(`Golden${ident.slice(6)}.kt`, kt);
}

// ---- transitions (B-04): venom, pour, error blocks ------------------------------------------------------
const items = new Map();
for (const [type, variant, el, , still] of CASES) {
  const it = { el, key: `${type}/${variant}`, stat: !!still };
  gen.genItem(it);
  items.set(`${type}-${variant}`, it.g);
}
const vendored = JSON.parse(fs.readFileSync(path.join(design, 'mutant-golden.json'), 'utf8')).venom;
const VENOM = [];
for (const v of vendored) if (!VENOM.some((c) => c.case === v.case)) VENOM.push(v);
const venomParts = (svg) => [...svg.matchAll(/<(path|circle|rect) ([^>]*)\/>/g)].map((m) => {
  const a = Object.fromEntries([...m[2].matchAll(/([a-z]+)="([^"]*)"/g)].map((x) => [x[1], x[2]]));
  if (m[1] === 'path') return `GoldenVenomPart(path = ${str(a.d).replace(/\n {12}/g, '\n                            ')})`;
  if (m[1] === 'circle') return `GoldenVenomPart(circle = ${arr([+a.cx, +a.cy, +a.r]).replace(/\n {8}/g, '\n                            ')})`;
  return `GoldenVenomPart(rect = ${arr([+a.x, +a.y, +a.width, +a.height, +a.rx]).replace(/\n {8}/g, '\n                            ')})`;
});
const venomCases = VENOM.map((v) => {
  const frames = [];
  for (let i = 0; i <= 24; i++) {
    const p = i / 24;
    const r = gen.venomShapes(JSON.parse(JSON.stringify(v.el)), v.key, v.fx, p);
    frames.push(`                GoldenVenomFrame(
                    p = ${num(p)},
                    full = ${r.full},
                    vertical = ${r.vert},
                    fat =
                        listOf(
${venomParts(r.SF).map((x) => '                            ' + x + ',').join('\n')}
                        ),
                    thin =
                        listOf(
${venomParts(r.S).map((x) => '                            ' + x + ',').join('\n')}
                        ),
                ),`);
  }
  const el = v.el;
  return `private fun venom${VENOM.indexOf(v)}(): GoldenVenom =
        GoldenVenom(
            name = "${v.case}",
            key = "${v.key}",
            slot = ${arr([el.ox, el.oy, el.w, el.h, el.r]).replace(/\n {8}/g, '\n            ')},
            from = ${arr([v.fx.dx, v.fx.dy, v.fx.px || 0, v.fx.py || 0]).replace(/\n {8}/g, '\n            ')},
            frames =
                listOf(
${frames.join('\n')}
                ),
        )
`;
});
const POUR = ['chip-a', 'box-a', 'track-a', 'btn-a', 'card-b'];
const pourCases = POUR.map((name) => {
  const g = items.get(name);
  const fr = [];
  for (let i = 0; i <= 8; i++) {
    const p = i / 8;
    const d = gen.pourPath(g, p);
    const nums = [...d.matchAll(/-?\d+(?:\.\d+)?(?:e-?\d+)?/g)].map((m) => +m[0]);
    fr.push(`                GoldenPour(p = ${num(p)}, numbers = ${arr(nums).replace(/\n {8}/g, '\n                ')}),`);
  }
  return `private fun pour${POUR.indexOf(name)}(): List<GoldenPour> =
            listOf(
${fr.join('\n')}
            )
`;
});
const ERR = ['btn-a', 'chip-a', 'field-a', 'field-b', 'card-a'];
const ROLE_ERR = { [C.red]: 0, [C.ink]: 1, [C.bg]: 2 };
const errCases = ERR.map((name) => {
  const svg = gen.errBlocks(items.get(name));
  const nums = [...svg.matchAll(/<rect x="([^"]+)" y="([^"]+)" width="([^"]+)" height="[^"]+" fill="([^"]+)"\/>/g)].flatMap((m) => {
    const role = ROLE_ERR[m[4]];
    if (role === undefined) throw new Error('error block colour ' + m[4]);
    return [+m[1], +m[2], +m[3], role];
  });
  return `        "${name}" to ${arr(nums).replace(/\n {8}/g, '\n        ')},`;
});
files.set('GoldenTransitions.kt', `// Generated by scripts/gen-goldens.mjs from reference/design-system/mutant-generator.js. Do not edit.
// Not formatted: make check holds every byte of it against the generator instead (research D3).
@file:Suppress("ktlint")

package io.github.youndie.mutant.gen.goldens

/** venomShapes for the three cases of the designer's golden, at p = 0, 1/24, …, 1. */
internal val goldenVenom: List<GoldenVenom>
    get() = listOf(${VENOM.map((_, i) => `venom${i}()`).join(', ')})

${venomCases.join('\n')}
/** pourPath's numbers in the order the SVG writes them, per instance, at p = 0, 1/8, …, 1. */
internal val goldenPour: List<Pair<String, List<GoldenPour>>>
    get() = listOf(${POUR.map((n, i) => `"${n}" to pour${i}()`).join(', ')})

${pourCases.join('\n')}
/** errBlocks: x, y, size and colour role (0 danger, 1 ink, 2 background) per block, per instance. */
internal val goldenErrorBlocks: List<Pair<String, DoubleArray>> =
    listOf(
${errCases.join('\n')}
    )
`);

// ---- the kit clock (B-07): the engine's own expressions, read out of mutant-engine.js ------------------
const engineSrc = fs.readFileSync(path.join(design, 'mutant-engine.js'), 'utf8');
const grab = (re, what) => { const m = engineSrc.match(re); if (!m) throw new Error(`mutant-engine.js: ${what} not found`); return m[1]; };
const bkExpr = grab(/const bk = (Math\.floor\(t \/ \(hot \? 125 \/ 1\.5 : 125\) \+ \.001\) % 4),/, 'the boil phase');
const tsExpr = grab(/ts = (Math\.floor\(t \/ step\) \* step),/, 'the resting tick');
const tsHExpr = grab(/tsH = (Math\.floor\(t \/ \(step \/ 1\.5\)\) \* \(step \/ 1\.5\)),/, 'the hot tick');
const dtExpr = grab(/const dt = (this\.last \? Math\.min\(1000, now - this\.last\) : 16);/, 'the frame step');
const bkOf = new Function('t', 'hot', `return ${bkExpr};`);
const ticksOf = new Function('t', 'step', `const ts = ${tsExpr}, tsH = ${tsHExpr}; return [ts, tsH];`);
const dtOf = new Function('now', 'last', `return (function () { return ${dtExpr}; }).call({ last });`);
const clockTimes = [];
for (let k = 0; k <= 48; k++) for (const d of [-0.2, -0.13, -0.124, -0.1, 0, 0.1]) {
  clockTimes.push(k * 125 + d, k * (125 / 1.5) + d);
}
for (let i = 0; i < 64; i++) clockTimes.push(Math.round(gen.rng('clock', 'times')() * 1e9) / 1e3 + i * 1234.567);
const clockRows = [...new Set(clockTimes.filter((t) => t >= 0).map((t) => +t.toFixed(6)))].sort((a, b) => a - b).flatMap((t) => {
  const [ts, tsH] = ticksOf(t, 1000 / 8);
  const [ts24] = ticksOf(t, 1000 / 24);
  return [t, bkOf(t, false), bkOf(t, true), ts, tsH, ts24];
});
const dtRows = [[1000, 0], [1016.7, 1000], [5000, 1000], [1000.5, 1000]].flatMap(([now, last]) => [now, last, dtOf(now, last)]);
files.set('GoldenClock.kt', `// Generated by scripts/gen-goldens.mjs from reference/design-system/mutant-engine.js. Do not edit.
// Not formatted: make check holds every byte of it against the engine instead (research D3).
@file:Suppress("ktlint")

package io.github.youndie.mutant.gen.goldens

/**
 * The engine's clock expressions (drawItem's boil phase, frame's resting and hot ticks), read out of
 * mutant-engine.js and evaluated in Node: per row t, the boil phase at rest and hot, the resting and the hot
 * tick at 8 fps, the resting tick at 24 fps.
 */
internal val goldenClockTicks: DoubleArray =
    ${arr(clockRows).replace(/\n {8}/g, '\n    ')}

/** frame's step: per row now, last (0 for none) and dt. */
internal val goldenClockSteps: DoubleArray =
    ${arr(dtRows).replace(/\n {8}/g, '\n    ')}
`);

// ---- keyframe loss (B-48): the engine's moshStart, run in Node on a stub canvas ------------------------
// The engine draws the band from a raster of the scene; its numbers come first, from rng(seed + '/' + n, 'mosh'),
// and st.m keeps them. The stubs give moshStart a scene of the size asked and an image that loads at once.
const engineCtx = { window: { MutantGenerator: G }, document: { currentScript: null }, Math, console, setTimeout: () => 0 };
vm.createContext(engineCtx);
vm.runInContext(engineSrc, engineCtx, { filename: 'mutant-engine.js' });
const E = engineCtx.window.MutantEngine;
const ctx2d = new Proxy({}, { get: (t, k) => (k in t ? t[k] : () => {}), set: (t, k, v) => { t[k] = v; return true; } });
engineCtx.XMLSerializer = class { serializeToString() { return '<svg/>'; } };
engineCtx.Image = class { set src(_) { this.onload(); } };
engineCtx.getComputedStyle = () => ({ backgroundColor: '#000' });
engineCtx.document.createElement = () => ({ getContext: () => ctx2d });
const MOSH = [[1, 250, 212], [2, 250, 212], [3, 250, 212], [4, 250, 212], [1, 360, 800], [7, 412, 915], [1, 300, 120]];
const moshCases = MOSH.map(([n, W, H]) => {
  const self = Object.create(E.prototype);
  self.props = { seed: 5 };
  self.moshN = n - 1;
  self.time = 0;
  self.stages = [];
  const st = { W, H, mosh: { getContext: () => ctx2d }, host: { dataset: {} }, svg: {}, items: [] };
  E.prototype.moshStart.call(self, st);
  const m = st.m;
  const runs = m.runs.flatMap((b) => [b.x, b.y, b.w, b.h, b.vx, b.vy, b.st, b.gone ? 1 : 0, b.tint ? ['#ff2e93', '#b6ff1a', '#19e3ff'].indexOf(b.tint) : -1]);
  return `        GoldenMosh(${n}, ${W}, ${H}, ${m.y0}, ${m.rows}, ${num(m.dur)}, ${arr(runs).replace(/\n {8}/g, '\n            ')}),`;
});
files.set('GoldenMosh.kt', `// Generated by scripts/gen-goldens.mjs from reference/design-system/mutant-engine.js. Do not edit.
// Not formatted: make check holds every byte of it against the engine instead (research D3).
@file:Suppress("ktlint")

package io.github.youndie.mutant.gen.goldens

/** A keyframe loss as moshStart plans it: the nth loss of seed 5 on a scene w × h; per block x, y, w, h, vx, vy, start, gone, tint. */
internal class GoldenMosh(val n: Int, val w: Int, val h: Int, val y0: Int, val rows: Int, val durationMs: Double, val runs: DoubleArray)

internal val goldenMosh: List<GoldenMosh> =
    listOf(
${moshCases.join('\n')}
    )
`);

// ---- the base vectors -------------------------------------------------------------------------------
const HASH = ['', 'a', '5/button', '5/card/01', 'mutant-ui', 'ЭФИР/кнопка', 'emoji 😀/grime'];
const STREAMS = [['5/btn/a', 'grime'], ['5/btn/a', 'edge'], ['5/card/a', 'mark:dead'], ['5/fab/b', 'bb3/41']];
const POINTS = [[0, 0], [0.5, 0.5], [3.2, 7.9], [-4.1, 12.6], [100.3, 55.7], [-0.25, -17.75], [1234.5, -987.25], [41.3, 17.9]];
const base = `// Generated by scripts/gen-goldens.mjs from reference/design-system/mutant-generator.js. Do not edit.
// Not formatted: make check holds every byte of it against the generator instead (research D3).
@file:Suppress("ktlint")

package io.github.youndie.mutant.gen.goldens

/** FNV-1a of each string. */
internal val goldenHashes: List<Pair<String, Long>> =
    listOf(
${HASH.map((s) => `        ${JSON.stringify(s)} to ${gen.hash(s)}L,`).join('\n')}
    )

/** The first twelve numbers of each mulberry32 stream, seed to stream. */
internal val goldenStreams: List<Triple<String, String, DoubleArray>> =
    listOf(
${STREAMS.map(([s, n]) => { const R = gen.rng(s, n); return `        Triple(${JSON.stringify(s)}, ${JSON.stringify(n)}, ${arr(Array.from({ length: 12 }, () => R())).replace(/\n {8}/g, '\n            ')}),`; }).join('\n')}
    )

/** x, y, h21(x, y), vn(x, y) at each point. */
internal val goldenNoise: DoubleArray =
    ${arr(POINTS.flatMap(([x, y]) => [x, y, gen.h21(x, y), gen.vn(x, y)])).replace(/\n {8}/g, '\n    ')}

internal val genGoldens: List<GoldenInstance> =
    listOf(
${names.map((n) => `        ${n},`).join('\n')}
    )
`;
files.set('GenGoldens.kt', base);

// ---- write or check -----------------------------------------------------------------------------------
let stale = 0;
const existing = fs.existsSync(out) ? fs.readdirSync(out).filter((f) => f.startsWith('Gen') || f.startsWith('Golden')).filter((f) => f !== 'GoldenModel.kt') : [];
for (const f of existing) if (!files.has(f)) { stale++; if (check) console.log(`stale: ${f} is not emitted any more`); else fs.unlinkSync(path.join(out, f)); }
fs.mkdirSync(out, { recursive: true });
for (const [f, text] of files) {
  const p = path.join(out, f);
  const now = fs.existsSync(p) ? fs.readFileSync(p, 'utf8') : null;
  if (now === text) continue;
  stale++;
  if (check) console.log(`stale: ${f} differs from what mutant-generator.js emits`);
  else fs.writeFileSync(p, text);
}
const bytes = [...files.values()].reduce((a, t) => a + t.length, 0);
console.log(`gen-goldens: ${files.size} files, ${CASES.length} instances, ${bytes} bytes${check ? `, ${stale} stale` : `, ${stale} written`}`);
process.exit(check && stale ? 1 : 0);
