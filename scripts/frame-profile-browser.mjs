#!/usr/bin/env node
// B-73: where a scene's main-thread time goes in the browser — a sampling profile of the production wasm build in
// headless Chrome (the CDP `Profiler`, every 100 µs), folded by function: self time, and total time (the function
// anywhere on the stack). Same stand as frame-bench-browser.mjs.
//
//   node scripts/frame-profile-browser.mjs <scene> <out.txt> [ms] [top]
//
// Needs `./gradlew :sample:wasmJsBrowserDistribution` first. Not part of `make check`.
import { spawn } from 'node:child_process';
import { createServer } from 'node:http';
import { existsSync, readFileSync, statSync, writeFileSync, mkdtempSync } from 'node:fs';
import { join, extname } from 'node:path';
import { tmpdir } from 'node:os';

const [scene, outFile, msArg, topArg] = process.argv.slice(2);
if (!scene || !outFile) throw new Error('usage: frame-profile-browser.mjs <scene> <out.txt> [ms] [top]');
const MS = +(msArg ?? 8000);
const TOP = +(topArg ?? 40);
// DIST: another copy of the build, as one whose Kotlin module wasm-opt rebuilt with `-g` to keep its names.
const DIST = process.env.DIST ?? 'sample/build/dist/wasmJs/productionExecutable';
const CHROME = '/Applications/Google Chrome.app/Contents/MacOS/Google Chrome';
const types = { '.html': 'text/html', '.js': 'text/javascript', '.wasm': 'application/wasm', '.ttf': 'font/ttf',
  '.png': 'image/png', '.json': 'application/json', '.map': 'application/json' };
const server = createServer((req, res) => {
  const path = decodeURIComponent(new URL(req.url, 'http://x').pathname);
  const file = join(DIST, path === '/' ? 'index.html' : path);
  if (!file.startsWith(DIST) || !existsSync(file) || statSync(file).isDirectory()) { res.writeHead(404); res.end(); return; }
  res.writeHead(200, { 'content-type': types[extname(file)] ?? 'application/octet-stream', 'cache-control': 'no-store' });
  res.end(readFileSync(file));
});
await new Promise(r => server.listen(0, '127.0.0.1', r));
const chrome = spawn(CHROME, ['--headless=new', '--window-size=1280,814', '--force-device-scale-factor=2',
  '--remote-debugging-port=9336', `--user-data-dir=${mkdtempSync(join(tmpdir(), 'b73p-'))}`, '--no-first-run',
  '--disable-background-timer-throttling', '--disable-renderer-backgrounding', 'about:blank'], { stdio: 'ignore' });
let up;
for (let i = 0; i < 50 && !up; i++) {
  await new Promise(r => setTimeout(r, 200));
  up = await fetch('http://127.0.0.1:9336/json/list').then(r => r.json()).catch(() => null);
}
const ws = new WebSocket(up.find(t => t.type === 'page').webSocketDebuggerUrl);
await new Promise(r => { ws.onopen = r; });
let seq = 0;
const pending = new Map();
ws.onmessage = e => { const m = JSON.parse(e.data); if (pending.has(m.id)) { pending.get(m.id)(m); pending.delete(m.id); } };
const send = (method, params = {}) => new Promise((res, rej) => {
  const id = ++seq;
  pending.set(id, m => (m.error ? rej(new Error(`${method}: ${m.error.message}`)) : res(m.result)));
  ws.send(JSON.stringify({ id, method, params }));
});
await send('Page.enable');
await send('Emulation.setDeviceMetricsOverride', { width: 1280, height: 814, deviceScaleFactor: 2, mobile: false });
await send('Page.navigate', { url: `http://127.0.0.1:${server.address().port}/?scene=${scene}` });
// WAIT_MS: how long the page runs before the profile starts (4 s by default).
await new Promise(r => setTimeout(r, +(process.env.WAIT_MS ?? 4000)));
await send('Profiler.enable');
await send('Profiler.setSamplingInterval', { interval: 100 });
await send('Profiler.start');
await new Promise(r => setTimeout(r, MS));
const { profile } = await send('Profiler.stop');

const byId = new Map(profile.nodes.map(n => [n.id, n]));
const parent = new Map();
for (const n of profile.nodes) for (const c of n.children ?? []) parent.set(c, n.id);
const name = n => n.callFrame.functionName || `(${n.callFrame.url.split('/').pop() || 'anonymous'})`;
const dt = profile.timeDeltas;
const self = new Map();
const total = new Map();
let all = 0;
let idle = 0;
profile.samples.forEach((id, i) => {
  const d = (dt[i + 1] ?? dt[i]) / 1000;
  all += d;
  const leaf = byId.get(id);
  if (name(leaf) === '(idle)') { idle += d; return; }
  self.set(name(leaf), (self.get(name(leaf)) ?? 0) + d);
  const seen = new Set();
  for (let n = leaf; n; n = byId.get(parent.get(n.id))) {
    const k = name(n);
    if (seen.has(k)) continue;
    seen.add(k);
    total.set(k, (total.get(k) ?? 0) + d);
  }
});
const busy = all - idle;
const line = ([k, v]) => `${v.toFixed(1).padStart(9)} ms ${(100 * v / busy).toFixed(1).padStart(5)} %  ${k}`;
const text = [
  `# B-73 profile of ${scene}, ${new Date().toISOString()}, ${MS} ms sampled every 100 µs`,
  `# sampled ${all.toFixed(0)} ms, idle ${idle.toFixed(0)} ms, busy ${busy.toFixed(0)} ms (${(100 * busy / all).toFixed(1)} %)`,
  '', `## self time, top ${TOP} (share of busy)`, ...[...self].sort((a, b) => b[1] - a[1]).slice(0, TOP).map(line),
  '', `## total time, top ${TOP} (share of busy)`, ...[...total].sort((a, b) => b[1] - a[1]).slice(0, TOP).map(line), '',
].join('\n');
writeFileSync(outFile, text);
console.log(text.split('\n').slice(0, 4).join('\n'));
ws.close();
chrome.kill();
server.close();
process.exit(0);
