// B-134: counts skia_memSetByte calls a second on a build whose sample.js was patched to count them (wrap the glue's
// `skia_memSetByte` export in a counter on globalThis.__mb). Usage: node scripts/count-skia-byte-copies.mjs <url>

import { spawn } from 'node:child_process'; import { mkdtempSync } from 'node:fs'; import { tmpdir } from 'node:os'; import { join } from 'node:path';
const port = 9347;
const chrome = spawn('/Applications/Google Chrome.app/Contents/MacOS/Google Chrome', ['--headless=new', '--window-size=390,844', `--remote-debugging-port=${port}`, `--user-data-dir=${mkdtempSync(join(tmpdir(), 'ct-'))}`, '--no-first-run', 'about:blank'], { stdio: 'ignore' });
let list; for (let i = 0; i < 50 && !list; i++) { await new Promise(r => setTimeout(r, 200)); list = await fetch(`http://127.0.0.1:${port}/json/list`).then(r => r.json()).catch(() => null); }
const ws = new WebSocket(list.find(t => t.type === 'page').webSocketDebuggerUrl); await new Promise(r => (ws.onopen = r));
let id = 0; const pend = new Map();
const send = (m, p = {}) => new Promise(res => { pend.set(++id, res); ws.send(JSON.stringify({ id, method: m, params: p })); });
ws.onmessage = e => { const m = JSON.parse(e.data); if (m.id && pend.has(m.id)) { pend.get(m.id)(m.result); pend.delete(m.id); } };
const ev = async x => (await send('Runtime.evaluate', { expression: x, returnByValue: true })).result.value;
await send('Emulation.setDeviceMetricsOverride', { width: 390, height: 844, deviceScaleFactor: 3, mobile: true });
await send('Page.navigate', { url: process.argv[2] });
await new Promise(r => setTimeout(r, 15000));
const a = await ev('globalThis.__mb||0'); await new Promise(r => setTimeout(r, 5000)); const b = await ev('globalThis.__mb||0');
console.log(`memSetByte calls: ${((b - a) / 5).toFixed(0)} a second`);
chrome.kill(); process.exit(0);
