// Opens each build in a new phone tab (an intent), scrolls it by synthesized touch flings, and reports frames and gaps.
import { execSync } from 'node:child_process';
const ADB = `${process.env.HOME}/Library/Android/sdk/platform-tools/adb`;
const sh = c => execSync(c, { encoding: 'utf8' }).trim();
sh(`${ADB} forward tcp:9336 localabstract:chrome_devtools_remote`);
const temp = () => +sh(`${ADB} shell dumpsys battery | grep temperature`).replace(/\D/g, '') / 10;
for (const url of process.argv.slice(2)) {
  sh(`${ADB} shell am start -a android.intent.action.VIEW -d "'${url}'" -n com.android.chrome/com.google.android.apps.chrome.Main`);
  await new Promise(r => setTimeout(r, 3000));
  const page = (await fetch('http://127.0.0.1:9336/json/list').then(r => r.json())).find(t => t.type === 'page' && t.url === url);
  const ws = new WebSocket(page.webSocketDebuggerUrl);
  await new Promise(r => (ws.onopen = r));
  let id = 0; const pend = new Map();
  const send = (method, params = {}) => new Promise(res => { pend.set(++id, res); ws.send(JSON.stringify({ id, method, params })); });
  ws.onmessage = e => { const m = JSON.parse(e.data); if (m.id && pend.has(m.id)) { pend.get(m.id)(m); pend.delete(m.id); } };
  const ev = async x => (await send('Runtime.evaluate', { expression: x, returnByValue: true, awaitPromise: true })).result?.result?.value;
  await new Promise(r => setTimeout(r, 15000)); // load, fonts, first loss
  await ev(`window.__iv = []; (() => { let last = 0; const f = t => { if (last) window.__iv.push(t - last); last = t; requestAnimationFrame(f); }; requestAnimationFrame(f); })(); 1`);
  const t0 = temp();
  for (let i = 0; i < 16; i++) {
    const dir = i % 8 < 4 ? -1 : 1; // four flings down, four back up
    await send('Input.synthesizeScrollGesture', { x: 200, y: 400, yDistance: dir * 1200, speed: 2400, gestureSourceType: 'touch' });
    await new Promise(r => setTimeout(r, 1200));
  }
  const iv = await ev('window.__iv');
  const total = iv.reduce((a, b) => a + b, 0);
  const sorted = [...iv].sort((a, b) => a - b);
  const q = p => sorted[Math.floor(p * (sorted.length - 1))];
  console.log(`${url}  ${(1000 * iv.length / total).toFixed(1)} f/s over ${(total / 1000).toFixed(1)} s, interval p50 ${q(.5).toFixed(1)} p95 ${q(.95).toFixed(1)} max ${sorted.at(-1).toFixed(0)} ms, >33 ${(100 * iv.filter(x => x > 33.4).length / iv.length).toFixed(1)} %, >500 ${iv.filter(x => x > 500).length}, ${t0}-${temp()} °C`);
  await send('Page.navigate', { url: 'about:blank' });
  ws.close();
}
sh(`${ADB} forward --remove tcp:9336`);
