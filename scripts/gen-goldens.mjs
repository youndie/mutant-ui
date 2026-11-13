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
