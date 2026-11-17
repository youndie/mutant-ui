#!/usr/bin/env node
// B-73: what a frame of the sample costs in the browser, per scene — the production wasm build in headless Chrome,
// driven over CDP, as B-72 measured the JVM. Per frame: the interval between two frames' starts (`requestAnimationFrame`'s
// timestamp) and the main thread's frame span, *work*: from the frame's start to the first task after its rendering
// update (a message posted from the frame's callback). Long animation frames (≥ 50 ms) are counted from the page's own
// observer. Scenes rotate each round; round 1 and each scene's first second are not counted. The control is the resting
// page with 10 ms of busy-waiting inside every frame, and the recorder must read it.
//
//   node scripts/frame-bench-browser.mjs <out-dir> [rounds] [scene-ms] [Scene,Scene]
//
// B-107: DIST_B=<dir> measures a second build beside the first, interleaved: each scene also runs as `<scene>~b` from
// it, in the same rotation, so a drift of the machine (a fanless Mac heating up) falls on both builds alike.
// B-107: PHONE=1 stands in for a phone: a 390 × 844 viewport at 3x, emulated as mobile with touch, and the main thread
// slowed CPU=<rate> times (4 by default) by Chrome's own throttling. It is a proxy, not a phone: the GPU is the Mac's.
//
// B-107: DEVICE=1 measures in Chrome on a phone attached over adb instead: the build is served through `adb reverse`,
// the page opened as a new tab of the phone's Chrome through its DevTools socket, nothing emulated or throttled. The phone
// keeps its screen on while plugged in, and Chrome must be open on it. The wheel scene has no meaning there.
//
// Needs `./gradlew :sample:wasmJsBrowserDistribution` first and Google Chrome. Not part of `make check`.
import { spawn, execSync } from 'node:child_process';
import { createServer } from 'node:http';
import { createHash } from 'node:crypto';
import { mkdirSync, readFileSync, readdirSync, writeFileSync, existsSync, statSync, mkdtempSync } from 'node:fs';
import { join, extname } from 'node:path';
import { tmpdir } from 'node:os';

const [out, roundsArg, sceneMsArg, scenesArg] = process.argv.slice(2);
if (!out) throw new Error('usage: frame-bench-browser.mjs <out-dir> [rounds] [scene-ms] [Scene,Scene]');
const ROUNDS = +(roundsArg ?? 6);
const SCENE_MS = +(sceneMsArg ?? 10_000);
const SETTLE_MS = 1_000;
const CONTROL_MS = 10;
const WHEEL_EVERY_MS = 32;
const WHEEL_FLIP_MS = 2_500;
const DEVICE = process.env.DEVICE === '1';
const PHONE = process.env.PHONE === '1' && !DEVICE;
const CPU = PHONE ? +(process.env.CPU ?? 4) : 1;
const WIDTH = PHONE ? 390 : 1280;
const HEIGHT = PHONE ? 844 : 814;
const SCALE = PHONE ? 3 : 2;
const ALL = DEVICE ? ['ComponentsRest', 'ComponentsClean', 'DemoPage', 'DemoPageClean', 'DemoLibrary', 'DemoRecording',
  'DemoSettings', 'ComponentsReduce', 'Control', 'ControlRest'] : ['ComponentsRest', 'ComponentsWheel', 'DemoPage', 'DemoLibrary', 'DemoRecording', 'DemoSettings',
  'ComponentsClean', 'ComponentsReduce', 'Control', 'ControlRest', 'DemoPageClean'];
// The control is the idle page (reduce motion stops the clock) with the busy-wait; ControlRest is the resting page with
// it, reported beside, as the first smoke run found the resting page saturated (B-73's amendment).
const HOST = { Control: 'ComponentsReduce', ControlRest: 'ComponentsRest' };
const BASE_SCENES = scenesArg ? scenesArg.split(',') : ALL;
const DIST_B = process.env.DIST_B;
const SCENES = DIST_B ? BASE_SCENES.flatMap(s => [s, `${s}~b`]) : BASE_SCENES;
// DIST: another copy of the build to measure, as one built at another commit.
const DIST = process.env.DIST ?? 'sample/build/dist/wasmJs/productionExecutable';
const CHROME = '/Applications/Google Chrome.app/Contents/MacOS/Google Chrome';

if (!existsSync(join(DIST, 'index.html'))) throw new Error(`no build at ${DIST}: run :sample:wasmJsBrowserDistribution`);
mkdirSync(out, { recursive: true });

// The build, served as the site serves it.
const types = { '.html': 'text/html', '.js': 'text/javascript', '.mjs': 'text/javascript', '.wasm': 'application/wasm',
  '.css': 'text/css', '.ttf': 'font/ttf', '.png': 'image/png', '.json': 'application/json', '.map': 'application/json' };
const ADB = process.env.ADB ?? `${process.env.HOME}/Library/Android/sdk/platform-tools/adb`;
const sh = cmd => { try { return execSync(cmd, { encoding: 'utf8' }).trim(); } catch { return '?'; } };
let nextPort = 8761;
const serve = async dist => {
  const server = createServer((req, res) => {
    const path = decodeURIComponent(new URL(req.url, 'http://x').pathname);
    const file = join(dist, path === '/' ? 'index.html' : path);
    if (!file.startsWith(dist) || !existsSync(file) || statSync(file).isDirectory()) { res.writeHead(404); res.end(); return; }
    res.writeHead(200, { 'content-type': types[extname(file)] ?? 'application/octet-stream', 'cache-control': 'no-store' });
    res.end(readFileSync(file));
  });
  await new Promise(r => server.listen(DEVICE ? nextPort++ : 0, '127.0.0.1', r));
  if (DEVICE) sh(`${ADB} reverse tcp:${server.address().port} tcp:${server.address().port}`);
  return `http://127.0.0.1:${server.address().port}`;
};
const origin = await serve(DIST);
const originB = DIST_B ? await serve(DIST_B) : null;

const DEBUG = DEVICE ? 9336 : 9335;
if (DEVICE) sh(`${ADB} forward tcp:${DEBUG} localabstract:chrome_devtools_remote`);
const profile = DEVICE ? null : mkdtempSync(join(tmpdir(), 'b73-'));
const chrome = DEVICE ? null : spawn(CHROME, ['--headless=new', `--window-size=${WIDTH},${HEIGHT}`, `--force-device-scale-factor=${SCALE}`,
  '--remote-debugging-port=9335', `--user-data-dir=${profile}`, '--no-first-run', '--no-default-browser-check',
  '--disable-background-timer-throttling', '--disable-renderer-backgrounding', '--disable-backgrounding-occluded-windows',
  'about:blank'], { stdio: 'ignore' });
let version;
for (let i = 0; i < 50 && !version; i++) {
  await new Promise(r => setTimeout(r, 200));
  version = await fetch(`http://127.0.0.1:${DEBUG}/json/version`).then(r => r.json()).catch(() => null);
}
const page = DEVICE
  ? await fetch(`http://127.0.0.1:${DEBUG}/json/new?about:blank`, { method: 'PUT' }).then(r => r.json())
  : (await fetch(`http://127.0.0.1:${DEBUG}/json/list`).then(r => r.json())).find(t => t.type === 'page');
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

await send('Page.enable');
await send('Runtime.enable');
if (!DEVICE) await send('Emulation.setDeviceMetricsOverride', { width: WIDTH, height: HEIGHT, deviceScaleFactor: SCALE, mobile: PHONE });
if (PHONE) {
  await send('Emulation.setTouchEmulationEnabled', { enabled: true, maxTouchPoints: 5 });
  await send('Emulation.setCPUThrottlingRate', { rate: CPU });
}
// The recorder, in the page before the sample's own script.
await send('Page.addScriptToEvaluateOnNewDocument', { source: `(() => {
  const control = new URLSearchParams(location.search).get('control') === '1';
  const frames = []; const long = [];
  window.__bench = { frames, long, recording: false };
  const mc = new MessageChannel();
  let open = null;
  mc.port1.onmessage = () => { if (open) { open.end = performance.now(); if (window.__bench.recording) frames.push(open); open = null; } };
  function tick(ts) {
    if (open && window.__bench.recording) frames.push(open);
    if (control) { const until = performance.now() + ${CONTROL_MS}; while (performance.now() < until) {} }
    open = { ts, end: -1 };
    mc.port2.postMessage(0);
    requestAnimationFrame(tick);
  }
  requestAnimationFrame(tick);
  new PerformanceObserver(l => { if (window.__bench.recording) for (const e of l.getEntries()) long.push([e.startTime, e.duration, e.blockingDuration]); })
    .observe({ type: 'long-animation-frame' });
})();` });

// The GPU is the browser target's to report, not the page's.
const gpu = await new Promise((res, rej) => {
  if (DEVICE) { res(null); return; }
  const b = new WebSocket(version.webSocketDebuggerUrl);
  b.onopen = () => b.send(JSON.stringify({ id: 1, method: 'SystemInfo.getInfo' }));
  b.onmessage = e => { const m = JSON.parse(e.data); b.close(); m.error ? rej(new Error(m.error.message)) : res(m.result.gpu); };
});
const digestOf = dist => readdirSync(dist).filter(f => f.endsWith('.wasm')).sort()
  .map(f => `${f} ${createHash('sha256').update(readFileSync(join(dist, f))).digest('hex').slice(0, 16)}`).join(', ');
const digest = digestOf(DIST) + (DIST_B ? `; ~b: ${digestOf(DIST_B)}` : '');
const viewport = DEVICE ? await evaluate('`${innerWidth}x${innerHeight} at ${devicePixelRatio}x`') : null;
const adbProp = k => sh(`${ADB} shell getprop ${k}`);
const battery = DEVICE ? sh(`${ADB} shell dumpsys battery | grep -E 'level|AC powered|temperature' | tr -s ' ' | tr '\\n' ';'`) : '';
const header = [
  `# B-73 browser frame bench, ${new Date().toISOString()}`,
  ...(DEVICE ? [
    `# device: ${adbProp('ro.product.model')} (${adbProp('ro.product.device')}), Android ${adbProp('ro.build.version.release')}, build ${adbProp('ro.build.id')}`,
    `# device power: ${battery}`,
    `# browser: ${version.Browser}, on the device, viewport ${viewport}, nothing emulated or throttled`,
  ] : []),
  `# driver host: ${sh('sysctl -n hw.model')}, ${sh('sysctl -n hw.ncpu')} cores, ${sh('sysctl -n hw.memsize') / 2 ** 30} GB, macOS ${sh('sw_vers -productVersion')}`,
  ...(DEVICE ? [] : [`# power: ${sh("pmset -g batt | tail -1 | sed 's/\\t/ /g'")}`,
  `# browser: ${version.Browser}, headless=new, ${WIDTH}x${HEIGHT} at ${SCALE}x${PHONE ? `, phone proxy: mobile, touch, CPU slowed ${CPU}x` : ''}`,
  `# gpu: ${gpu.devices.map(d => d.deviceString).join('; ')}; webgl ${gpu.featureStatus.webgl}, gpu_compositing ${gpu.featureStatus.gpu_compositing}`]),
  `# commit: ${sh('git rev-parse --short HEAD')}${sh('git status --porcelain -- sample mutant-core') ? ' (dirty)' : ''}`,
  `# build: ${digest}`,
  `# rounds ${ROUNDS} (round 1 not counted), ${SCENE_MS} ms a scene, first ${SETTLE_MS} ms not counted; scenes ${SCENES.join(',')}`,
];
console.log(header.join('\n'));
const csv = ['round,scene,ts,interval,work'];
const raw = {}; // scene -> counted rounds' intervals and work, unrounded
const results = {};

const q = (a, p) => { if (!a.length) return NaN; const s = [...a].sort((x, y) => x - y); return s[Math.min(s.length - 1, Math.floor(p * s.length))]; };
let load = 0;
for (let round = 1; round <= ROUNDS; round++) {
  const order = SCENES.map((_, i) => SCENES[(i + round - 1) % SCENES.length]);
  for (const scene of order) {
    const b = scene.endsWith('~b');
    const name = b ? scene.slice(0, -2) : scene;
    const url = `${b ? originB : origin}/?scene=${HOST[name] ?? name}${HOST[name] ? '&control=1' : ''}&load=${++load}`;
    const loaded = event('Page.loadEventFired');
    await send('Page.navigate', { url });
    await loaded;
    // The sample is up once its canvas has drawn: wait for the wasm to start, then settle.
    for (let i = 0; i < 100 && !(await evaluate('!!document.querySelector("canvas")')); i++) await new Promise(r => setTimeout(r, 100));
    await new Promise(r => setTimeout(r, SETTLE_MS));
    await evaluate('window.__bench.recording = true; 0');
    const until = Date.now() + SCENE_MS;
    let down = true;
    let flipAt = Date.now() + WHEEL_FLIP_MS;
    let wheels = 0;
    while (Date.now() < until) {
      if (scene === 'ComponentsWheel') {
        if (Date.now() > flipAt) { down = !down; flipAt += WHEEL_FLIP_MS; }
        await send('Input.dispatchMouseEvent', { type: 'mouseWheel', x: WIDTH / 2, y: HEIGHT / 2, deltaX: 0, deltaY: down ? 100 : -100 });
        wheels++;
        await new Promise(r => setTimeout(r, WHEEL_EVERY_MS));
      } else {
        await new Promise(r => setTimeout(r, 100));
      }
    }
    const got = await evaluate('window.__bench.recording = false; JSON.stringify({ f: window.__bench.frames, l: window.__bench.long, s: window.__scrolled ?? -1 })');
    const { f, l, s } = JSON.parse(got);
    const intervals = [];
    const work = [];
    for (let i = 1; i < f.length; i++) {
      const iv = f[i].ts - f[i - 1].ts;
      const w = f[i].end < 0 ? NaN : f[i].end - f[i].ts;
      intervals.push(iv);
      if (!Number.isNaN(w)) work.push(w);
      csv.push(`${round},${scene},${f[i].ts.toFixed(2)},${iv.toFixed(2)},${Number.isNaN(w) ? '' : w.toFixed(2)}`);
    }
    if (round > 1) {
      const into = (raw[scene] ??= { iv: [], w: [], frames: 0 });
      into.iv.push(...intervals);
      into.w.push(...work);
      into.frames += f.length;
    }
    const r = {
      frames: f.length, fps: f.length / (SCENE_MS / 1000), ivP95: q(intervals, .95),
      over17: intervals.filter(x => x > 16.7).length / Math.max(1, intervals.length),
      over33: intervals.filter(x => x > 33).length / Math.max(1, intervals.length),
      workP50: q(work, .5), workP95: q(work, .95), loaf: l.length, loafMs: l.reduce((a, e) => a + e[1], 0), wheels, scrolled: s,
    };
    (results[scene] ??= []).push({ round, ...r });
    console.log(`round ${round} ${scene.padEnd(17)} ${r.fps.toFixed(1).padStart(5)} f/s  interval p95 ${r.ivP95.toFixed(1).padStart(6)}  >16.7 ${(100 * r.over17).toFixed(2).padStart(6)} %  >33 ${(100 * r.over33).toFixed(2).padStart(6)} %  work p50 ${r.workP50.toFixed(2).padStart(6)} p95 ${r.workP95.toFixed(2).padStart(6)}  LoAF ${r.loaf} (${r.loafMs.toFixed(0)} ms)${scene === 'ComponentsWheel' ? `  wheel ${wheels} events, scrolled ${s} px` : ''}`);
  }
}
writeFileSync(join(out, 'frames.csv'), csv.join('\n') + '\n');

// The verdict, pre-registered in B-73, over the counted rounds pooled per scene.
console.log('\n# counted rounds 2..' + ROUNDS + ', pooled');
for (const scene of SCENES) {
  const { iv, w, frames } = raw[scene] ?? { iv: [], w: [], frames: 0 };
  const o17 = iv.filter(x => x > 16.7).length / iv.length;
  const o33 = iv.filter(x => x > 33).length / iv.length;
  const p50 = q(w, .5), p95 = q(w, .95);
  const verdict = p95 <= 8 && o17 < .01 ? 'GREEN' : p50 > 16.7 || o33 > .05 ? 'RED' : 'GREY';
  const fps = frames / ((ROUNDS - 1) * SCENE_MS / 1000);
  console.log(`${scene.padEnd(17)} ${fps.toFixed(1).padStart(5)} f/s  interval p95 ${q(iv, .95).toFixed(1).padStart(6)}  >16.7 ${(100 * o17).toFixed(2).padStart(6)} %  >33 ${(100 * o33).toFixed(2).padStart(6)} %  work p50 ${p50.toFixed(2).padStart(6)} p95 ${p95.toFixed(2).padStart(6)}  ${verdict}`);
}
for (const [ctl, base] of [['Control', 'ComponentsReduce'], ['ControlRest', 'ComponentsRest']]) {
  const c = (results[ctl] ?? []).filter(r => r.round > 1);
  const b = (results[base] ?? []).filter(r => r.round > 1);
  if (!c.length || !b.length) continue;
  const by = new Map(b.map(r => [r.round, r.workP50]));
  console.log(`# ${ctl}: work p50 over ${base}'s, per counted round: ` +
    c.map(r => (r.workP50 - by.get(r.round)).toFixed(2)).join(', ') + ` ms against ${CONTROL_MS} ms`);
}
// B-107: the second build against the first, per counted round: work p50 of `<scene>~b` minus `<scene>`.
if (DIST_B) {
  for (const scene of BASE_SCENES) {
    const a = new Map((results[scene] ?? []).filter(r => r.round > 1).map(r => [r.round, r.workP50]));
    const d = (results[`${scene}~b`] ?? []).filter(r => r.round > 1 && a.has(r.round)).map(r => r.workP50 - a.get(r.round));
    if (!d.length) continue;
    const mean = d.reduce((x, y) => x + y, 0) / d.length;
    console.log(`# ~b minus a, ${scene}: per counted round ${d.map(x => x.toFixed(2)).join(', ')} ms; mean ${mean.toFixed(2)} ms`);
  }
}
ws.close();
if (DEVICE) {
  await fetch(`http://127.0.0.1:${DEBUG}/json/close/${page.id}`).catch(() => null);
  sh(`${ADB} forward --remove tcp:${DEBUG}`);
  sh(`${ADB} reverse --remove-all`);
} else chrome.kill();
process.exit(0);
