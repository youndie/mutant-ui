#!/usr/bin/env node
// B-151: what a finger costs on the Demo's Library screen, in Chrome on the phone — the production wasm build served
// through `adb reverse` and opened as B-73's bench opens it (`DEVICE=1` there), the screen as `?scene=DemoTouch`
// composes it: DemoLibrary's phone, its keyframe loss included, which also says where it stands (`window.__phone`).
//
// Two gestures, each in its own load:
//   tabs   — a tap on the navigation bar every TAP_EVERY ms, round the four tabs: On air, Queue, Find, Settings.
//   scroll — on the On air tab, a swipe up the content then one down, one every SWIPE_EVERY ms, each followed by its
//            fling.
// Per gesture, from the page's own clock: the tap's `pointerup` (or the swipe's `pointerdown`) and every frame after it
// (B-73's recorder: rAF start, the first task after the rendering update). Read:
//   latency — from the tap's pointerup to the end of the first frame that starts after it;
//   window  — the frame intervals from the gesture to WINDOW ms after it ends: the longest, and how many exceed 25 ms
//             (a frame missed at 60 Hz).
//
//   DIST=<dir> [DIST_B=<dir>,…] node scripts/demo-touch-bench.mjs <out-dir> [rounds] [load-ms] [tabs,scroll,…]
//
// `blank` is the floor: B-110's ground and Tape with no body (`?scene=Blank`), tapped where the bar would be, so the
// phone's own frame rate under the same touches is read beside the screen's. Round 1 is not counted; loads rotate.
// PROFILE=1 also samples each load with the CDP profiler (every 200 µs) and keeps it as <out>/<load>-r<round>.cpuprofile.
// Needs the phone over adb with Chrome open on it, and `./gradlew :sample:wasmJsBrowserDistribution`.
import { execSync } from 'node:child_process';
import { createServer } from 'node:http';
import { createHash } from 'node:crypto';
import { mkdirSync, readFileSync, readdirSync, writeFileSync, existsSync, statSync } from 'node:fs';
import { join, extname } from 'node:path';

const [out, roundsArg, loadMsArg, kindsArg] = process.argv.slice(2);
if (!out) throw new Error('usage: demo-touch-bench.mjs <out-dir> [rounds] [load-ms] [tabs,scroll,blank]');
const ROUNDS = +(roundsArg ?? 4);
const LOAD_MS = +(loadMsArg ?? 30_000);
const SETTLE_MS = 3_000;
const TAP_EVERY = 1_500;
const SWIPE_EVERY = 1_500;
const WINDOW = 1_000;
const MISSED = 25;
const KINDS = (kindsArg ?? 'tabs,scroll,blank').split(',');
const DIST = process.env.DIST ?? 'sample/build/dist/wasmJs/productionExecutable';
const OTHERS = (process.env.DIST_B ? process.env.DIST_B.split(',') : []).map((dir, i) => ({ tag: `~${String.fromCharCode(98 + i)}`, dir }));
const ARMS = [{ tag: '', dir: DIST }, ...OTHERS];
for (const a of ARMS) if (!existsSync(join(a.dir, 'index.html'))) throw new Error(`no build at ${a.dir}`);
mkdirSync(out, { recursive: true });

const ADB = process.env.ADB ?? `${process.env.HOME}/Library/Android/sdk/platform-tools/adb`;
const sh = cmd => { try { return execSync(cmd, { encoding: 'utf8' }).trim(); } catch { return '?'; } };
const types = { '.html': 'text/html', '.js': 'text/javascript', '.mjs': 'text/javascript', '.wasm': 'application/wasm',
  '.css': 'text/css', '.ttf': 'font/ttf', '.png': 'image/png', '.json': 'application/json', '.map': 'application/json' };
let nextPort = 8765;
const serve = async dist => {
  const server = createServer((req, res) => {
    const path = decodeURIComponent(new URL(req.url, 'http://x').pathname);
    const file = join(dist, path === '/' ? 'index.html' : path);
    if (!file.startsWith(dist) || !existsSync(file) || statSync(file).isDirectory()) { res.writeHead(404); res.end(); return; }
    res.writeHead(200, { 'content-type': types[extname(file)] ?? 'application/octet-stream', 'cache-control': 'no-store' });
    res.end(readFileSync(file));
  });
  await new Promise(r => server.listen(nextPort++, '127.0.0.1', r));
  sh(`${ADB} reverse tcp:${server.address().port} tcp:${server.address().port}`);
  return `http://127.0.0.1:${server.address().port}`;
};
for (const a of ARMS) a.origin = await serve(a.dir);

const DEBUG = 9336;
sh(`${ADB} shell am start -a android.intent.action.VIEW -d about:blank -n com.android.chrome/com.google.android.apps.chrome.Main`);
sh(`${ADB} forward tcp:${DEBUG} localabstract:chrome_devtools_remote`);
let version;
for (let i = 0; i < 50 && !version; i++) {
  await new Promise(r => setTimeout(r, 200));
  version = await fetch(`http://127.0.0.1:${DEBUG}/json/version`, { signal: AbortSignal.timeout(2_000) }).then(r => r.json()).catch(() => null);
}
if (!version) throw new Error('no DevTools on the phone: is Chrome open on it?');
// B-140: a bench tab left by a killed run keeps drawing and heats the phone; this bench's and B-73's are closed.
for (const t of await fetch(`http://127.0.0.1:${DEBUG}/json/list`).then(r => r.json())) {
  if (t.type === 'page' && /^http:\/\/127\.0\.0\.1:876\d\/\?scene=/.test(t.url)) {
    console.log(`# closed a leftover bench tab: ${t.url}`);
    await fetch(`http://127.0.0.1:${DEBUG}/json/close/${t.id}`).catch(() => null);
  }
}
let page;
for (let i = 0; i < 50 && !page; i++) {
  if (i > 0) await new Promise(r => setTimeout(r, 200));
  page = (await fetch(`http://127.0.0.1:${DEBUG}/json/list`).then(r => r.json())).find(t => t.type === 'page' && t.url === 'about:blank');
}
if (!page) throw new Error('no blank tab to measure in');
const ws = new WebSocket(page.webSocketDebuggerUrl);
await new Promise(r => { ws.onopen = r; });
let seq = 0;
const pending = new Map();
const waiters = [];
ws.onmessage = e => {
  const m = JSON.parse(e.data);
  if (m.id && pending.has(m.id)) { pending.get(m.id)(m); pending.delete(m.id); return; }
  for (const w of waiters.splice(0)) if (!w(m)) waiters.push(w);
};
const send = (method, params = {}) => new Promise((res, rej) => {
  const id = ++seq;
  pending.set(id, m => (m.error ? rej(new Error(`${method}: ${m.error.message}`)) : res(m.result)));
  ws.send(JSON.stringify({ id, method, params }));
});
const event = (name, ms = 30_000) => new Promise((res, rej) => {
  const t = setTimeout(() => rej(new Error(`no ${name}`)), ms);
  waiters.push(m => { if (m.method !== name) return false; clearTimeout(t); res(m.params); return true; });
});
const evaluate = async expr => (await send('Runtime.evaluate', { expression: expr, returnByValue: true, awaitPromise: true })).result.value;
const sleep = ms => new Promise(r => setTimeout(r, ms));

await send('Page.enable');
await send('Runtime.enable');
// B-73's recorder, plus the gestures' own times as the page saw them.
await send('Page.addScriptToEvaluateOnNewDocument', { source: `(() => {
  const frames = []; const marks = [];
  window.__bench = { frames, marks, recording: false };
  const mc = new MessageChannel();
  let open = null;
  mc.port1.onmessage = () => { if (open) { open.end = performance.now(); if (window.__bench.recording) frames.push(open); open = null; } };
  function tick(ts) {
    if (open && window.__bench.recording) frames.push(open);
    open = { ts, end: -1 };
    mc.port2.postMessage(0);
    requestAnimationFrame(tick);
  }
  requestAnimationFrame(tick);
  for (const type of ['pointerdown', 'pointerup']) addEventListener(type, e => { if (window.__bench.recording) marks.push([type, e.timeStamp]); }, true);
})();` });

const digestOf = dist => readdirSync(dist).filter(f => f.endsWith('.wasm')).sort()
  .map(f => `${f} ${createHash('sha256').update(readFileSync(join(dist, f))).digest('hex').slice(0, 16)}`).join(', ');
const adbProp = k => sh(`${ADB} shell getprop ${k}`);
const power = () => sh(`${ADB} shell dumpsys battery | grep -E 'level|AC powered|temperature' | tr -s ' ' | tr '\\n' ';'`);
const freq = () => sh(`${ADB} shell 'cat /sys/devices/system/cpu/cpu6/cpufreq/scaling_cur_freq /sys/devices/system/cpu/cpu6/cpufreq/scaling_max_freq'`).split(/\s+/).map(v => Math.round(+v / 1000)).join('/');
console.log([
  `# B-151 Demo touch bench, ${new Date().toISOString()}`,
  `# device: ${adbProp('ro.product.model')} (${adbProp('ro.product.device')}), Android ${adbProp('ro.build.version.release')}, build ${adbProp('ro.build.id')}`,
  `# device power: ${power()} big ${freq()} MHz`,
  `# browser: ${version.Browser}, on the device, nothing emulated or throttled`,
  `# commit: ${sh('git rev-parse --short HEAD')}${sh('git status --porcelain -- sample mutant-core') ? ' (dirty)' : ''}`,
  `# build: ${ARMS.map(a => `${a.tag || 'a'}: ${digestOf(a.dir)}`).join('; ')}`,
  `# rounds ${ROUNDS} (round 1 not counted), ${LOAD_MS} ms a load, ${SETTLE_MS} ms settle; loads ${KINDS.join(',')}; tap every ${TAP_EVERY} ms, swipe every ${SWIPE_EVERY} ms, window ${WINDOW} ms, missed > ${MISSED} ms`,
].join('\n'));

const touch = (type, x, y) => send('Input.dispatchTouchEvent', { type, touchPoints: type === 'touchEnd' ? [] : [{ x, y, id: 1 }] });
const tap = async (x, y) => { await touch('touchStart', x, y); await sleep(40); await touch('touchEnd', x, y); };
// A swipe of 300 px over ~160 ms in 10 moves, then let go mid-motion, so the content flings.
const swipe = async (x, y0, dy) => {
  await touch('touchStart', x, y0);
  for (let i = 1; i <= 10; i++) { await sleep(16); await touch('touchMove', x, y0 + dy * i / 10); }
  await touch('touchEnd', x, y0 + dy);
};

const q = (a, p) => { if (!a.length) return NaN; const s = [...a].sort((x, y) => x - y); return s[Math.min(s.length - 1, Math.floor(p * s.length))]; };
const csv = ['round,load,kind,gesture,mark,latency,longest,missed,intervals'];
const frameCsv = ['round,load,ts,interval,work'];
const pooled = {};
let load = 0;
const LOADS = ARMS.flatMap(a => KINDS.map(k => ({ arm: a, kind: k })));
for (let round = 1; round <= ROUNDS; round++) {
  const order = LOADS.map((_, i) => LOADS[(i + round - 1) % LOADS.length]);
  for (const { arm, kind } of order) {
    const name = `${kind}${arm.tag}`;
    const scene = kind === 'blank' ? 'Blank' : 'DemoTouch';
    const loaded = event('Page.loadEventFired');
    await send('Page.navigate', { url: `${arm.origin}/?scene=${scene}&load=${++load}` });
    await loaded;
    for (let i = 0; i < 100 && !(await evaluate('!!document.querySelector("canvas")')); i++) await sleep(100);
    await sleep(SETTLE_MS);
    // The blank scene has no phone; it is tapped where the screen's bar stands, from the screen's own report taken once.
    const phone = kind === 'blank' ? (globalThis.lastPhone ?? [24, 70, 360, 720]) : await evaluate('window.__phone ?? null');
    if (!phone) throw new Error('the screen did not say where its phone stands');
    if (kind !== 'blank') globalThis.lastPhone = phone;
    const [L, T, W, H] = phone;
    // The bar: 68 tall, 8 in from the phone's edges, four items across.
    const barY = T + H - 8 - 34;
    const itemX = i => L + 8 + (W - 16) * (i + .5) / 4;
    const PROFILE = process.env.PROFILE === '1';
    if (PROFILE) {
      await send('Profiler.enable');
      await send('Profiler.setSamplingInterval', { interval: 200 });
      await send('Profiler.start');
    }
    await evaluate('window.__bench.recording = true; 0');
    // With a profile, a marker the profile can be lined up by: a function of this name busy for 20 ms from the page
    // time it returns, so a sample on its stack is at that time give or take the sampling interval.
    const mark = PROFILE ? await evaluate('(function __b151mark() { const t = performance.now(); while (performance.now() - t < 20) {} return t; })()') : null;
    const until = Date.now() + LOAD_MS;
    const gestures = [];
    let n = 0;
    while (Date.now() < until) {
      const at = Date.now();
      if (kind === 'scroll') {
        await swipe(L + W / 2, n % 2 === 0 ? T + H * .7 : T + H * .3, n % 2 === 0 ? -300 : 300);
        gestures.push('swipe');
      } else {
        await tap(itemX((n + 1) % 4), barY);
        gestures.push(`tab${(n + 1) % 4}`);
      }
      n++;
      await sleep(Math.max(0, (kind === 'scroll' ? SWIPE_EVERY : TAP_EVERY) - (Date.now() - at)));
    }
    await sleep(WINDOW);
    if (PROFILE) writeFileSync(join(out, `${name}-r${round}.cpuprofile`), JSON.stringify({ ...(await send('Profiler.stop')).profile, b151mark: mark }));
    const { f, m } = JSON.parse(await evaluate('window.__bench.recording = false; JSON.stringify({ f: window.__bench.frames, m: window.__bench.marks })'));
    for (let i = 1; i < f.length; i++) frameCsv.push(`${round},${name},${f[i].ts.toFixed(2)},${(f[i].ts - f[i - 1].ts).toFixed(2)},${f[i].end < 0 ? '' : (f[i].end - f[i].ts).toFixed(2)}`);
    // A gesture's marks: a tap's pointerup; a swipe's pointerdown and its pointerup.
    const downs = m.filter(x => x[0] === 'pointerdown').map(x => x[1]);
    const ups = m.filter(x => x[0] === 'pointerup').map(x => x[1]);
    const rows = [];
    for (let g = 0; g < Math.min(ups.length, gestures.length); g++) {
      const start = kind === 'scroll' ? downs[g] : ups[g];
      const first = f.find(x => x.ts > ups[g] && x.end > 0);
      const latency = first ? first.end - ups[g] : NaN;
      const iv = [];
      for (let i = 1; i < f.length; i++) if (f[i].ts > start && f[i].ts <= ups[g] + WINDOW) iv.push(f[i].ts - f[i - 1].ts);
      const longest = Math.max(...iv);
      const missed = iv.filter(x => x > MISSED).length;
      rows.push({ latency, longest, missed, n: iv.length });
      csv.push(`${round},${name},${kind},${gestures[g]},${start.toFixed(2)},${latency.toFixed(2)},${longest.toFixed(2)},${missed},${iv.length}`);
    }
    if (round > 1) (pooled[name] ??= []).push(...rows);
    const lat = rows.map(r => r.latency).filter(x => !Number.isNaN(x));
    const iv = rows.reduce((a, r) => a + r.n, 0), miss = rows.reduce((a, r) => a + r.missed, 0);
    console.log(`round ${round} ${name.padEnd(9)} ${String(rows.length).padStart(3)} gestures (${ups.length} pointerups)  latency p50 ${q(lat, .5).toFixed(0).padStart(4)} max ${Math.max(...lat).toFixed(0).padStart(4)} ms  longest p50 ${q(rows.map(r => r.longest), .5).toFixed(0).padStart(4)} max ${Math.max(...rows.map(r => r.longest)).toFixed(0).padStart(4)} ms  missed ${miss}/${iv} (${(100 * miss / Math.max(1, iv)).toFixed(1)} %)  ${f.length} frames  ${power().match(/temperature: (\d+)/)?.[1] / 10} °C big ${freq()} MHz`);
  }
}
writeFileSync(join(out, 'gestures.csv'), csv.join('\n') + '\n');
writeFileSync(join(out, 'frames.csv'), frameCsv.join('\n') + '\n');
console.log('\n# counted rounds 2..' + ROUNDS + ', pooled');
for (const [name, rows] of Object.entries(pooled)) {
  const lat = rows.map(r => r.latency).filter(x => !Number.isNaN(x));
  const iv = rows.reduce((a, r) => a + r.n, 0), miss = rows.reduce((a, r) => a + r.missed, 0);
  const longest = rows.map(r => r.longest);
  const p95 = q(longest, .95), max = Math.max(...longest), missed = miss / Math.max(1, iv);
  // B-151's verdict: the longest interval at most 33.4 ms in 95 % of the gestures and at most 50 ms in all of them, and
  // at most 2 % of the intervals missed. The floor (blank) must hold it for the run to be valid.
  const failed = [p95 > 33.4 && 'p95', max > 50 && 'max', missed > .02 && 'missed'].filter(Boolean);
  console.log(`${name.padEnd(9)} ${String(rows.length).padStart(3)} gestures  latency p50 ${q(lat, .5).toFixed(0).padStart(4)} p95 ${q(lat, .95).toFixed(0).padStart(4)} max ${Math.max(...lat).toFixed(0).padStart(4)} ms  longest p50 ${q(longest, .5).toFixed(0).padStart(4)} p95 ${p95.toFixed(0).padStart(4)} max ${max.toFixed(0).padStart(4)} ms  over 50 ms in ${rows.filter(r => r.longest > 50).length}  missed ${(100 * missed).toFixed(1)} %  ${failed.length ? `NOT HELD (${failed.join(', ')})` : 'HELD'}`);
}
ws.close();
await fetch(`http://127.0.0.1:${DEBUG}/json/close/${page.id}`).catch(() => null);
sh(`${ADB} forward --remove tcp:${DEBUG}`);
sh(`${ADB} reverse --remove-all`);
process.exit(0);
