// Does the vendored generator reproduce the vendored golden? (research §1.3)
//
// Runs reference/design-system/mutant-generator.js untouched in Node, with no DOM and no clock,
// and compares every entry of reference/design-system/mutant-golden.json with what the generator
// emits today: hash, rng, noise, items, frames (contour, highlight, flow lines) and venom.
// Frames are drawn the way the engine draws them (mutant-engine.js, drawItem): boil phase from the
// golden, boil' = boil * (1 + 2 * heat) + 0.6 * heat, bubbles while heat > 0.
//
// KNOWN lists the entries that differ, each with its cause. The check fails when anything else
// differs, and also when a known entry starts to match: a list of exceptions that no longer holds
// is a list nobody reads.
//
//   node scripts/design-golden.mjs           print every comparison
//   node scripts/design-golden.mjs --check   exit 1 on an unexpected result
import fs from 'node:fs';
import path from 'node:path';
import vm from 'node:vm';
import { fileURLToPath } from 'node:url';

const root = path.resolve(path.dirname(fileURLToPath(import.meta.url)), '..');
const dir = path.join(root, 'reference', 'design-system');
const check = process.argv.includes('--check');

// The golden's flow lines of the three hot frames were written with the resting boil (0.8);
// the engine draws them with boil' (3.0 at heat 1). Every other path of those frames matches.
// Which one is intended is a question for the design (research §1.3, B-03).
const KNOWN = new Map([
  ['frame btn 200×48 @9340 marks', 'golden written with markPath(..., bo = 0.8); the engine passes boil\' = 3'],
  ['frame fab 56×56 @9340 marks', 'golden written with markPath(..., bo = 0.8); the engine passes boil\' = 3'],
  ['frame card 328×80 @9340 marks', 'golden written with markPath(..., bo = 0.8); the engine passes boil\' = 3'],
]);

const ctx = { window: {}, Math, Float32Array, Int32Array, NaN, Infinity, console };
vm.createContext(ctx);
vm.runInContext(fs.readFileSync(path.join(dir, 'mutant-generator.js'), 'utf8'), ctx, { filename: 'mutant-generator.js' });
const G = ctx.window.MutantGenerator;
const gold = JSON.parse(fs.readFileSync(path.join(dir, 'mutant-golden.json'), 'utf8'));
const gen = new G({ seed: 5 });
const r4 = (v) => Math.round(v * 1e4) / 1e4;

const results = [];
const cmp = (name, got, want) => results.push({ name, same: JSON.stringify(got) === JSON.stringify(want) });

for (const [s, v] of Object.entries(gold.hash)) cmp(`hash "${s}"`, gen.hash(s), v);
for (const [k, v] of Object.entries(gold.rng)) {
  const [seed, stream] = k.split(' | ');
  const R = gen.rng(seed, stream);
  cmp(`rng ${k}`, v.map(() => r4(R())), v);
}
for (const n of gold.noise) {
  // The golden's h21 is the hash of the lattice node the point falls in, not of the point.
  cmp(`noise h21(floor ${n.x}, floor ${n.y})`, r4(gen.h21(Math.floor(n.x), Math.floor(n.y))), n.h21);
  cmp(`noise vn(${n.x}, ${n.y})`, r4(gen.vn(n.x, n.y)), n.vn);
}

const items = {};
for (const [name, it] of Object.entries(gold.items)) {
  const o = { el: JSON.parse(JSON.stringify(it.el)), key: it.key };
  gen.genItem(o);
  items[name] = o;
  const g = o.g;
  cmp(`item ${name} k`, r4(g.k), it.k);
  cmp(`item ${name} rb`, r4(g.rb), it.rb);
  cmp(`item ${name} wave`, g.wave.map(r4), it.wave);
  cmp(`item ${name} sh`, g.sh.map(r4), it.sh);
  cmp(`item ${name} flow lines`, g.ML.length, it.marks);
  cmp(`item ${name} mosaic blocks`, (g.mz || []).length, it.mosaicBlocks);
  cmp(`item ${name} drips`, g.D.map((d) => ({ x: r4(d.x), b: r4(d.b), Lmax: r4(d.Lmax), L0: r4(d.L0), P: r4(d.P), off: r4(d.off), u: r4(d.u), drip: d.drip })), it.drips);
}

let uid = 0;
for (const f of gold.frames) {
  const g = items[f.item].g;
  const el = g.el;
  const heat = f.heat;
  const boil0 = 0.8;
  const boil = boil0 * (1 + 2 * heat) + 0.6 * heat;
  const E = { g, e: `frame-${uid++}`, P: gen.prims(g, f.t, heat > 0) };
  if (heat > 0) E.P.push(...gen.bubbles(g, f.t, heat));
  const at = `frame ${f.item} @${f.t}`;
  cmp(`${at} boil`, r4(boil), f.boil);
  cmp(`${at} primitives`, E.P.length, f.primitives);
  const fd = gen.field(E, f.t, f.rough, f.boilPhase, boil, heat, boil0);
  cmp(`${at} grid`, { nx: fd.nx, ny: fd.ny, x0: fd.x0, y0: fd.y0, step: fd.s }, f.grid);
  cmp(`${at} contour`, gen.pathBuf(gen.march(fd, 0), 0.9, 5), f.contour);
  cmp(`${at} highlight`, gen.pathBuf(gen.march(fd, -3.4, Math.ceil((el.r + 19) / fd.s), Math.floor((el.h + 7) / fd.s)), 1.1), f.highlight);
  if (f.marks !== undefined) cmp(`${at} marks`, gen.markPath(g, f.t, f.boilPhase, boil), f.marks);
}

for (const v of gold.venom) {
  const r = gen.venomShapes(JSON.parse(JSON.stringify(v.el)), v.key, v.fx, v.p);
  cmp(`venom ${v.case} @${v.p}`, { SF: r.SF, S: r.S, full: r.full, vert: r.vert }, { SF: v.SF, S: v.S, full: v.full, vert: v.vert });
}

let bad = 0;
for (const r of results) {
  const known = KNOWN.get(r.name);
  const ok = known ? !r.same : r.same;
  if (!ok) bad++;
  if (!check || !ok) {
    const verdict = r.same ? (known ? 'MATCHES, but is listed as known' : 'same') : known ? `differs (known: ${known})` : 'DIFFERS';
    console.log(`${ok ? '  ' : '✗ '}${r.name}: ${verdict}`);
  }
}
for (const name of KNOWN.keys()) {
  if (!results.some((r) => r.name === name)) {
    bad++;
    console.log(`✗ ${name}: listed as known, but no such comparison ran`);
  }
}
console.log(`design-golden: ${results.length} comparisons, ${results.filter((r) => !r.same).length} differ, ${KNOWN.size} known, ${bad} unexpected`);
process.exit(check && bad ? 1 : 0);
