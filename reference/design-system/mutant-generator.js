/*
 * mutant-ui · генератор слизи. Эталон для переноса на Kotlin.
 *
 * Чистый код: без DOM, без часов, без Math.random. Один и тот же вход даёт один и тот же SVG-путь.
 * Хост (mutant-engine.js) измеряет слоты и текст, ведёт часы и собирает SVG; сюда приходят только числа.
 *
 * Конвейер одного кадра:
 *   1. genItem(it)                — один раз на экземпляр: форма, подтёки, мозаика, линии потока → it.g
 *   2. prims(g, t)                — примитивы подтёков и капель на момент t (+ bubbles при нажатии)
 *   3. field(E, t, …)             — поле знаковых расстояний на сетке
 *   4. march(fd, 0) → pathBuf     — контур тела; march(fd, -3.4) → pathBuf — линия блика
 *   5. markPath(g, t, …)          — линии потока внутри тела
 *   6. venomShapes(el, key, fx, p) — переход плашки выбора: щупальца, убывающая и растущая массы (отдельно от кадра покоя)
 *
 * Единицы: координаты и размеры в dp, время в мс. Кадр покоя — функция (seed кассеты, ключ, t).
 * Seed экземпляра — строка seed + '/' + key; каждый вид случайности берёт свой поток rng(seed, имя).
 *
 * Словарь структур:
 *   el  — слот: ox, oy (угол в сцене), w, h, r (радиус), over (предел свеса), cf (доля подтёков с каплей),
 *         ow (обводка), fill, hl (цвет блика), calm (износ 0.2…1.7), wob (сила оплыва), edge, marks, dead (флаги приёмов),
 *         tz — чистая зона [x, y, w, h] относительно слота, cz — она же с запасом 2 dp
 *   g   — экземпляр: k (масштаб от размера 0.5…1), rb (нижний радиус),
 *         wave [амплитуда, длина волны, фаза 1, фаза 2, угловая скорость] — волна верхней кромки,
 *         sh [выпуклость слева, справа, наклон верха, сила шумового оплыва, горб верха, число горбов, фаза горбов,
 *             сдвиг шума, радиусы: верх-лево, верх-право, низ-право, низ-лево],
 *         D — подтёки {x, b (полуширина), Lmax, L0 (длины), P (период), off (фаза 0…1), u, drip (капает)},
 *         ML — линии потока, mz — блоки мозаики [x, y, размер], ms — сторона мозаики, dz — зона битых пикселей [cx, cy, r], sd — seed
 *   примитив — [ax, ay, bx, by, ra, rb, k]: неравная капсула и радиус её слияния с телом
 *   поле — {F (Float32Array, < 0 внутри), nx, ny (узлы), x0, y0 (угол сетки), s (шаг)}
 *
 * Что ещё не соответствует брифу: при смене размера экземпляр пересобирается (подтёки расставляются с шагом в dp),
 * генов generation, ghost, hueDrift и split нет.
 */
(() => {
class MutantGenerator {
  static TH = {
    efir: {
      ink: '#05060c',
      green: '#b6ff1a',
      mag: '#ff2e93',
      cyan: '#19e3ff',
      red: '#ff3b30',
      white: '#e9f2e4',
      bg: '#10142a',
      surf: '#232b4a',
      surf2: '#2f3960',
      blue: '#3a47a0',
      amber: '#ffb000',
      acc: '#b6ff1a',
      mis: '#3a47a0',
      deep: '#1a2040',
      focus: '#ffb000',
      comet: '#e9f2e4',
      coc1: '#1a2040',
      coc2: '#3a47a0',
      hlDark: '#6b78b6',
      drop: '#e9f2e4',
      disK: .55,
      disT: [58, 71, 160],
      print: 0
    },
    kopiya: {
      ink: '#14110f',
      green: '#b6ff1a',
      mag: '#ff2e93',
      cyan: '#19e3ff',
      red: '#e0301e',
      white: '#fbf8ee',
      bg: '#efe9d9',
      surf: '#e3dbc6',
      surf2: '#d6ccb2',
      blue: '#2b3fbf',
      amber: '#ffb000',
      acc: '#2b3fbf',
      mis: '#ff5a4a',
      deep: '#14110f',
      focus: '#2b3fbf',
      comet: '#14110f',
      coc1: '#cfc5aa',
      coc2: '#bfb394',
      hlDark: '#8f9be0',
      drop: '#efe9d9',
      disK: 1.02,
      disT: [214, 206, 188],
      print: 1
    }
  };

  static C = { ...this.TH.efir };

  static K = {
    btn: { r: 12, over: 24, cf: .55, fill: 'green' },
    fab: { r: 16, over: 24, cf: 1, fill: 'green' },
    icon: { r: 14, over: 10, cf: .4, fill: 'surf2', ow: 2.5, marks: 0 },
    chip: { r: 16, over: 16, cf: .45, fill: 'surf2', ow: 2.5 },
    card: { r: 10, over: 24, cf: .4, fill: 'surf', dead: 1 },
    field: { r: 6, over: 16, cf: .4, fill: 'surf' },
    track: { r: 14, over: 12, cf: .5, fill: 'green', ow: 2.5 },
    thumb: { r: 11, over: 8, cf: .4, fill: 'white', ow: 2, marks: 0, edge: 0 },
    box: { r: 5, over: 0, cf: 0, fill: 'surf2', ow: 2, marks: 0, edge: 0, wob: .45 },
    dot: { r: 99, over: 0, cf: 0, fill: 'surf2', ow: 2, marks: 0, edge: 0, wob: .35, round: 1 },
    bar: { r: 8, over: 0, cf: 0, fill: 'surf', wob: .6 },
    pill: { r: 99, over: 0, cf: 0, fill: 'green', ow: 2.5, marks: 0, edge: 0, wob: .6 },
    blob: { r: 14, over: 28, cf: .7, fill: 'mag' }
  };

  /** root — корневой узел страницы; props — seed, fps, boil, rough, density, speed, keyframeLoss, theme. */
  constructor(props) {
    this.props = Object.assign({ seed: 5, density: 1 }, props || {});
    this._N = {};
  }

  /** FNV-1a, 32 бита: строка → беззнаковое целое. Основа всех seed. */
  hash(s) {
    let h = 0x811c9dc5;
    for (let i = 0; i < s.length; i++) {
      h ^= s.charCodeAt(i);
      h = Math.imul(h, 0x01000193);
    }
    return h >>> 0;
  }

  /** Поток случайных чисел mulberry32 на пару (seed, имя потока). Потоки независимы: лишний вызов в одном не сдвигает остальные. */
  rng(seed, stream) {
    let a = this.hash(seed + '/' + stream);
    return () => {
      a |= 0;
      a = a + 0x6D2B79F5 | 0;
      let t = Math.imul(a ^ a >>> 15, 1 | a);
      t = t + Math.imul(t ^ t >>> 7, 61 | t) ^ t;
      return ((t ^ t >>> 14) >>> 0) / 4294967296;
    };
  }

  /** smoothstep(a, b, x) с зажимом в 0…1. */
  ss(a, b, x) {
    const v = Math.max(0, Math.min(1, (x - a) / (b - a)));
    return v * v * (3 - 2 * v);
  }

  /** Гладкий минимум двух расстояний с радиусом слияния k. Так подтёки, шейки и пузыри сливаются с телом без шва. */
  smin(a, b, k) {
    const h = Math.max(k - Math.abs(a - b), 0) / k;
    return Math.min(a, b) - h * h * k * .25;
  }

  /** Хэш узла решётки → 0…1. Основа value-шума. */
  h21(x, y) {
    const s = Math.sin(x * 127.1 + y * 311.7) * 43758.5453;
    return s - Math.floor(s);
  }

  /** Value-шум с шагом решётки 1, результат −1…1. */
  vn(x, y) {
    const ix = Math.floor(x),
      iy = Math.floor(y),
      fx = x - ix,
      fy = y - iy,
      ux = fx * fx * (3 - 2 * fx),
      uy = fy * fy * (3 - 2 * fy),
      a = this.h21(ix, iy),
      b = this.h21(ix + 1, iy),
      c = this.h21(ix, iy + 1),
      d = this.h21(ix + 1, iy + 1);
    return (a + (b - a) * ux + (c - a) * uy + (a - b - c + d) * ux * uy) * 2 - 1;
  }

  /** Знаковое расстояние до «неравной капсулы»: отрезок (ax, ay) → (bx, by) с радиусом ra в начале и rb в конце. Из этого примитива собраны подтёки, шейки, капли и пузыри. */
  ucap(px, py, ax, ay, bx, by, ra, rb) {
    px -= ax;
    py -= ay;
    const Bx = bx - ax, By = by - ay, h = Bx * Bx + By * By;
    if (h < 1e-4) return Math.hypot(px, py) - ra;
    const qx = Math.abs((px * By - py * Bx) / h),
      qy = (px * Bx + py * By) / h,
      b = ra - rb,
      cx = Math.sqrt(Math.max(h - b * b, 1e-4)),
      k = cx * qy - b * qx,
      n = qx * qx + qy * qy;
    if (k < 0) return Math.sqrt(h * n) - ra;
    if (k > cx) return Math.sqrt(h * (n + 1 - 2 * qy)) - rb;
    return cx * qx + b * qy - ra;
  }

  /** Смещение верхней кромки в точке x на момент t, dp: две бегущие синусоиды из g.wave. */
  ty(g, x, t) {
    const [A, lam, p0, p1, ws] = g.wave, T = 6.2832;
    return -A * (.2 + .6 * Math.sin(T * x / lam + p0 + t * ws) + .25 * Math.sin(T * x / (lam * .53) + p1 - t * ws * .7));
  }

  /** Знаковое расстояние до тела экземпляра без кэша. Нужно при расстановке штрихов: скруглённый прямоугольник слота, искажённый формой g.sh и волной сверху. n1, n2 — готовые значения шума, если они уже посчитаны. */
  bodyD(px, py, g, tyx, n1, n2) {
    const { w, h } = g.el,
      S = g.sh,
      u = Math.max(0, Math.min(1, px / w)),
      v = Math.max(0, Math.min(1, py / h)),
      m = Math.max(0, Math.min(1, 1 - py / (h * .5)));
    let x = px + (px < w / 2 ? S[0] : -S[1]) * Math.sin(Math.PI * v),
      y = py - (tyx + S[2] * (u - .5) * 2 + S[4] * Math.sin(Math.PI * u * S[5] + S[6])) * m;
    x += S[3] * (n1 === undefined ? this.vn(px * .03 + S[7], py * .05) : n1);
    y += S[3] * .7 * (n2 === undefined ? this.vn(px * .03 + S[7] + 31, py * .05 + 9) : n2);
    const cx = x - w / 2, cy = y - h / 2, rr = cy > 0 ? (cx > 0 ? S[10] : S[11]) : (cx > 0 ? S[9] : S[8]);
    const qx = Math.abs(cx) - w / 2 + rr, qq = Math.abs(cy) - h / 2 + rr;
    return Math.min(Math.max(qx, qq), 0) + Math.hypot(Math.max(qx, 0), Math.max(qq, 0)) - rr;
  }

  /** Знаковое расстояние до скруглённого прямоугольника слота с четырьмя разными радиусами g.sh[8…11]. Координаты x, y уже искажены. */
  rrD(g, x, y) {
    const { w, h } = g.el,
      S = g.sh,
      cx = x - w / 2,
      cy = y - h / 2,
      rr = cy > 0 ? (cx > 0 ? S[10] : S[11]) : (cx > 0 ? S[9] : S[8]),
      qx = Math.abs(cx) - w / 2 + rr,
      qq = Math.abs(cy) - h / 2 + rr;
    return Math.min(Math.max(qx, qq), 0) + Math.hypot(Math.max(qx, 0), Math.max(qq, 0)) - rr;
  }

  /** Штрих тушью переменной толщины: ось C (точки) и полуширины W → замкнутый SVG-путь. */
  taper(C, W) {
    const n = C.length, Lp = [], Rp = [];
    for (let i = 0; i < n; i++) {
      const a = C[Math.max(0, i - 1)],
        b = C[Math.min(n - 1, i + 1)],
        dx = b[0] - a[0],
        dy = b[1] - a[1],
        l = Math.hypot(dx, dy) || 1,
        nx = -dy / l,
        ny = dx / l,
        hw = W[i] / 2;
      Lp.push([C[i][0] + nx * hw, C[i][1] + ny * hw]);
      Rp.push([C[i][0] - nx * hw, C[i][1] - ny * hw]);
    }
    return this.pathOf([Lp.concat(Rp.reverse())], 0);
  }

  /** Вспомогательная для marks: заготовка линии потока типа «крюк». */
  mkHook(x, y, L, dx, a, hk, dir, fl) {
    const C = [], n = Math.max(3, Math.round(L / 1.1));
    if (fl) C.push([x - dir * fl, y - fl * .45], [x - dir * fl * .35, y - fl * .08]);
    for (let i = 0; i <= n; i++) {
      const u = i / n;
      C.push([x + dx * u * u * (3 - 2 * u), y + L * u]);
    }
    const bx = x + dx, by = y + L;
    let ib = 0;
    for (let i = 1; i <= 8; i++) {
      const q = Math.PI * i / 8;
      C.push([bx + dir * a * (1 - Math.cos(q)), by + a * 1.15 * Math.sin(q)]);
      if (i === 4) ib = C.length - 1;
    }
    const m = Math.max(2, Math.round(hk / 1.1));
    for (let i = 1; i <= m; i++) {
      const u = i / m;
      C.push([bx + dir * a * (2 + .45 * u * u), by - hk * u]);
    }
    return { C, ib };
  }

  /** Вспомогательная для marks: заготовка линии потока типа «дуга». */
  mkArc(x, y, wd, dp) {
    const C = [], n = Math.max(6, Math.round(wd / 1.1));
    for (let i = 0; i <= n; i++) {
      const u = i / n;
      C.push([x - wd / 2 + wd * u, y + dp * Math.sin(Math.PI * u) - dp * .55 * Math.pow(Math.abs(2 * u - 1), 4)]);
    }
    return { C, ib: n >> 1 };
  }

  /** Геометрия одной линии потока на момент t: ось и толщины, вход для taper. */
  markAt(m, u) {
    const q = .35 + .65 * this.ss(0, .75, u), y = m.y + m.sl * u;
    return m.k === 'a' ? this.mkArc(m.x, y, m.wd, m.dp * q) : this.mkHook(m.x, y, m.L * q, m.dx * q, m.a, m.hk * (.4 + .6 * (q - .35) / .65), m.dir, m.fl);
  }

  /** SVG-путь всех линий потока на момент t. Каждая линия растёт, сползает вниз и гаснет по своему циклу. bk — фаза покадрового дрожания 0…3, bo — его сила. */
  markPath(g, t, bk, bo) {
    const { ox, oy } = g.el, wm = 2.1 * Math.max(.72, g.k);
    let out = '';
    g.ML.forEach(m => {
      const u = (t / m.P + m.off) % 1, e = this.ss(0, .08, u) * (1 - this.ss(.86, 1, u));
      if (e < .08) return;
      const { C, ib } = this.markAt(m, u), cum = [0];
      for (let i = 1; i < C.length; i++) cum.push(cum[i - 1] + Math.hypot(C[i][0] - C[i - 1][0], C[i][1] - C[i - 1][1]));
      const tot = cum[C.length - 1], wk = wm * e * (1 + .14 * bo * this.vn(m.sd + bk * 3.7, 5));
      const W = C.map((p, i) => wk * (.26 + .74 * this.ss(0, 3.2, Math.min(cum[i], tot - cum[i])) + .22 * Math.exp(-Math.pow((cum[i] - cum[ib]) / 2.4, 2))));
      out += this.taper(C.map(([px, py], i) => [px + ox + bo * .55 * this.vn(i * .3 + m.sd + bk * 7.3, bk * 3.1), py + oy + bo * .4 * this.vn(i * .3 + m.sd + 40 + bk * 5.9, 1.7)]), W);
    });
    return out;
  }

  /** Расставляет линии потока внутри тела (g.ML): вне чистой зоны, мозаики и зоны битых пикселей, без наложений. Один раз на экземпляр. */
  marks(g, R, inb, dens) {
    const el = g.el, { w, h } = el, tz = el.tz, sc = Math.max(.72, g.k);
    const dz = g.dz,
      hitT = (x, y, m) => (x > tz[0] - m && x < tz[0] + tz[2] + m && y > tz[1] - m && y < tz[1] + tz[3] + m) || (!!dz && Math.hypot(x - dz[0], y - dz[1]) < dz[2] + m) || (!!g.mz && g.mz.some(b => x > b[0] - m && x < b[0] + b[2] + m && y > b[1] - m && y < b[1] + b[2] + m));
    const boxes = [], ML = [];
    const place = m => {
      const { C } = this.markAt(m, 1);
      if (!inb(m.x, m.y, 2.4) || C.some(([px, py]) => hitT(px, py, 2.6) || !inb(px, py, 2.4))) return false;
      let xa = 1e9, xb = -1e9, ya = 1e9, yb = -1e9;
      C.forEach(p => {
        xa = Math.min(xa, p[0]);
        xb = Math.max(xb, p[0]);
        ya = Math.min(ya, p[1]);
        yb = Math.max(yb, p[1]);
      });
      ya = Math.min(ya, m.y - 1);
      if (boxes.some(b => xa < b[1] + 3.5 && xb > b[0] - 3.5 && ya < b[3] + 3 && yb > b[2] - 3)) return false;
      boxes.push([xa, xb, ya, yb]);
      m.P = 5000 + 4500 * R();
      m.off = R();
      m.sd = R() * 90;
      ML.push(m);
      return true;
    };
    const n = Math.max(2, Math.round(w / (30 * sc) * dens)), sw = w / n;
    for (let i = 0; i < n; i++) {
      const t0 = R();
      for (let tr = 0; tr < 24; tr++) {
        const t = t0 + tr * .035, x = (i + .15 + .7 * R()) * sw, dir = R() < .5 ? -1 : 1;
        let m;
        if (t < .4) {
          const a = (2.6 + 1.2 * R()) * sc,
            sl = 4 * sc,
            L = Math.min(h - 14 - sl, (13 + 12 * R()) * sc),
            dx = (R() - .5) * 14 * sc;
          m = {
            k: 'j',
            x: x - (dx + dir * a * 2) / 2,
            y: 3 + R() * Math.max(1, h - L - a - 8 - sl),
            L,
            dx,
            a,
            hk: (3 + 4 * R()) * sc,
            dir,
            fl: R() < .5 ? 2 * sc : 0,
            sl
          };
        } else if (t < .75) {
          const a = (2.2 + 1.2 * R()) * sc, sl = 3 * sc;
          m = {
            k: 'j',
            x: x - dir * a,
            y: 4 + R() * Math.max(1, h - 14 - sl),
            L: (3 + 5 * R()) * sc,
            dx: 0,
            a,
            hk: (3 + 4.5 * R()) * sc,
            dir,
            fl: 1.8 * sc,
            sl
          };
        } else {
          const sl = 2 * sc;
          m = {
            k: 'a',
            x,
            y: 4 + R() * Math.max(1, h - 9 - sl),
            wd: (7 + 7 * R()) * sc,
            dp: (1.6 + 1.4 * R()) * sc,
            sl
          };
        }
        if (place(m)) break;
      }
    }
    g.ML = ML;
    return '';
  }

  /** Состояние одного подтёка на момент t: длина, радиус кончика, толщина шейки и положение оторвавшейся капли. Фазы цикла: рост → капля висит → шейка рвётся → падение и отскок. */
  dyn(d, t, h, k) {
    const b = d.b, S = 2.2 + .9 * b, T = 6.283;
    if (!d.drip) {
      const ph = (t / (d.P * 1.7) + d.off) % 1;
      return { tip: { L: d.Lmax * (.88 + .12 * (.5 + .5 * Math.sin(T * ph))), r2: b * 1.08, nr: .9 } };
    }
    const P = d.P,
      Hd = 2200 + 2200 * d.u,
      Pn = 1000,
      Rc = 1000,
      Gt = P - Hd - Pn - Rc,
      tau = (t + d.off * P) % P,
      R2 = b * 1.45,
      Lh = d.Lmax + S * .25,
      yc0 = h + Lh - R2;
    if (tau < Gt) {
      const u = tau / Gt, e = u * u * (.35 + .65 * u);
      return {
        tip: {
          L: d.L0 + (d.Lmax - d.L0) * e,
          r2: b * (1 + .45 * this.ss(.3, 1, u)),
          nr: .9 - .14 * this.ss(.4, 1, u)
        }
      };
    }
    if (tau < Gt + Hd) {
      const v = (tau - Gt) / Hd, sw = Math.sin(v * T * 1.5) * .6 * (1 - v);
      return { tip: { L: d.Lmax + S * .25 * v + sw, r2: R2, nr: .76 - .08 * v } };
    }
    if (tau < Gt + Hd + Pn) {
      const v = (tau - Gt - Hd) / Pn, e = v * v;
      return {
        tip: { L: Lh - R2 * 1.2 * e, r2: R2 + (b * .55 - R2) * e, nr: .68 - .12 * e },
        drop: { y: yc0 + S * 1.1 * e, r: R2 * (1 - .08 * e) }
      };
    }
    const w = tau - Gt - Hd - Pn,
      dist = .5 * .00055 * w * w,
      sc = Math.max(0, 1 - Math.max(0, dist - 10 * k) / (8 * k)),
      Ls = Lh - R2 * 1.2,
      q = 1 - Math.exp(-w / 90);
    return {
      tip: {
        L: d.L0 + (Ls - d.L0) * Math.exp(-w / 160) * Math.cos(w / 90),
        r2: b * (.55 + .45 * q),
        nr: .56 + .34 * q
      },
      drop: sc > .05 ? { y: yc0 + S * 1.1 + dist, r: R2 * .92 * sc } : null
    };
  }

  /** Цвет для disabled: яркость исходного цвета × dark с подмесом оттенка темы (disT). */
  desat(hex, dark) {
    const n = parseInt(hex.slice(1), 16),
      l = (.3 * (n >> 16 & 255) + .59 * (n >> 8 & 255) + .11 * (n & 255)) * dark,
      m = t => Math.round(l * .85 + t * .15).toString(16).padStart(2, '0');
    return '#' + m(MutantGenerator.C.disT[0]) + m(MutantGenerator.C.disT[1]) + m(MutantGenerator.C.disT[2]);
  }

  /** Мозаика края: до трёх рядов блоков 4 и 8 dp вдоль одной стороны тела. Возвращает SVG-прямоугольники и записывает блоки в g.mz: под ними контур квантуется (см. field). */
  edgeMos(g) {
    const el = g.el, { w, h, cz, ox, oy } = el, r = Math.min(el.r, h / 2), R = this.rng(g.sd, 'edge');
    let s = '';
    const mz = g.mz = [];
    const hit = (x, y, a) => cz[2] > 0 && x < cz[0] + cz[2] && x + a > cz[0] && y < cz[1] + cz[3] && y + a > cz[1];
    const sv = R(),
      side = h - 2 * r >= 20 && sv > .6 ? (sv > .8 ? 'left' : 'right') : 'top',
      top = (g.ms = side) === 'top',
      len = (top ? w : h) - 2 * r,
      segL = Math.max(12, len * (top ? .4 + .25 * R() : .9)),
      s0 = r + R() * Math.max(0, len - segL);
    for (let p = 0; p < segL;) {
      const bs = Math.abs(p / segL - .5) < .22 ? 8 : 4, a = Math.round((s0 + p) / 4) * 4;
      for (let j = 0; j < 3; j++) {
        const v = R(), c = R(), x = top ? a : side === 'left' ? j * bs : w - (j + 1) * bs, y = top ? j * bs : a;
        if (v < [.95, .7, .35][j] && !hit(x, y, bs)) {
          mz.push([x, y, bs]);
          if (!j) for (let q = 0; q < bs; q += 4) mz.push(top ? [a + q, -4, 4] : side === 'left' ? [-4, a + q, 4] : [w, a + q, 4]);
          s += `<rect data-px x="${ox + x}" y="${oy + y}" width="${bs}" height="${bs}" fill="${c < .45 ? '#000' : '#fff'}" opacity="${c < .25 ? .5 : c < .45 ? .28 : c < .75 ? .38 : .62}"/>`;
        }
      }
      p += bs;
    }
    return s;
  }

  /** Битые пиксели в «грязном» углу карточки: блоки 8, 4 и 2 dp, плотность падает с расстоянием от угла. */
  deadPx(g) {
    const C = MutantGenerator.C,
      el = g.el,
      { w, h, cz, ox, oy } = el,
      R = this.rng(g.sd, 'mark:dead'),
      x0 = cz[0] + cz[2] + 2;
    let s = '';
    const rect = (x, y, q, f) => { s += `<rect x="${ox + x}" y="${oy + y}" width="${q}" height="${q}" fill="${f}"/>`; };
    const bright = [C.green, C.mag, C.cyan, '#fff'],
      top = R() < .5,
      cy0 = top ? 0 : h,
      pal = [C.coc1, C.surf2, C.coc2, C.ink];
    const pick = () => {
      const v = R(), u = R();
      return v < .06 ? C.green : v < .1 ? C.mag : pal[Math.floor(u * 4)];
    };
    const g8 = Math.ceil(x0 / 8) * 8, g4 = Math.ceil(x0 / 4) * 4;
    for (let y = 0; y < h; y += 8) for (let x = g8; x < w; x += 8) {
      const d = Math.hypot(x + 4 - w, y + 4 - cy0), v = R(), c = pick();
      if (d < 34 && v < .85) rect(x, y, 8, c);
    }
    for (let y = 0; y < h; y += 4) for (let x = g4; x < w; x += 4) {
      const d = Math.hypot(x + 2 - w, y + 2 - cy0), v = R(), c = pick();
      if (d >= 34 && d < 58 && v < .42) rect(x, y, 4, c);
    }
    for (let n = 0; n < 18; n++) {
      const a = R(),
        b = R(),
        c = R(),
        x = Math.floor(x0 + a * (w - x0)),
        y = Math.floor(b * h),
        d = Math.hypot(x - w, y - cy0);
      if (d >= 52 && d < 92) rect(x, y, 2, bright[Math.floor(c * 4)]);
    }
    const ly = oy + Math.round(top ? 22 + R() * 26 : h - 22 - R() * 26) + .5, lx = ox + x0 + R() * 16;
    return s + `<line x1="${lx.toFixed(1)}" x2="${(lx + 20 + R() * 30).toFixed(1)}" y1="${ly}" y2="${ly}" stroke="#fff" stroke-width="1" opacity=".85"/>`;
  }

  /** Смесь цветов a → b на долю k с осветлением lift. Цвет кипения при нажатии. */
  mix(a, b, k, lift) {
    const pa = parseInt(a.slice(1), 16),
      pb = parseInt(b.slice(1), 16),
      ch = sh => {
      const x = (pa >> sh & 255) + ((pb >> sh & 255) - (pa >> sh & 255)) * k;
      return Math.round(x + (255 - x) * lift).toString(16).padStart(2, '0');
    };
    return '#' + ch(16) + ch(8) + ch(0);
  }

  /** Пузыри кипения на верхней кромке на момент t при нагреве k = 0…1. Возвращает круглые примитивы. */
  bubbles(g, t, k) {
    const el = g.el, P = [], n = Math.max(3, Math.round(el.w / 20));
    for (let i = 0; i < n; i++) {
      const R = this.rng(g.sd, 'bub' + i),
        per = 360 + 440 * R(),
        off = R(),
        cyc = Math.floor(t / per + off),
        ph = (t / per + off) % 1,
        Q = this.rng(g.sd, 'bb' + i + '/' + cyc);
      const x = el.r * .6 + Q() * (el.w - el.r * 1.2),
        rm = (2.4 + 3.8 * Q()) * Math.max(.7, g.k) * k,
        r = ph < .8 ? rm * (.35 + .65 * ph / .8) : 0;
      if (r > .9) P.push([el.ox + x, el.oy + 1.5 - r * .55, el.ox + x, el.oy + 1.5 - r * .55, r, r, 2.6]);
    }
    return P;
  }

  /** Главный шаг. Поле знаковых расстояний экземпляра на сетке с шагом s (отрицательное — внутри): тело + примитивы E.P через гладкое объединение + шум кромки (rough) + покадровое дрожание (bk, bo; hk — нагрев, bo0 — сила в покое). Под блоками мозаики значение берётся из центра блока. Статичные слои кэшируются в this._N. */
  field(E, t, rough, bk, bo, hk, bo0) {
    const g = E.g,
      el = g.el,
      s = el.w > 200 ? 1.5 : .9,
      x0 = el.ox - 7,
      y0 = el.oy - 13,
      nx = Math.ceil((el.w + 14) / s) + 1,
      ny = Math.ceil((el.h + el.over + 31) / s) + 1,
      N = nx * ny,
      nk = E.e + '|' + nx + '|' + ny;
    if (!this._N[nk]) {
      const A = new Float32Array(N), B = new Float32Array(N).fill(NaN);
      for (let j = 0; j < ny; j++) for (let i = 0; i < nx; i++) {
        const X = x0 + i * s, Y = y0 + j * s, ly = Y - el.oy;
        A[j * nx + i] = .55 * this.vn(X * .8, Y * .8) + .3 * this.vn(X * 2.1 + 17, Y * 2.1 + 17);
        if (ly >= el.h * .5) B[j * nx + i] = this.bodyD(X - el.ox, ly, g, 0);
      }
      this._N[nk] = A;
      this._N[nk + 'B'] = B;
      const V1 = new Float32Array(N), V2 = new Float32Array(N);
      for (let j = 0; j < ny; j++) for (let i = 0; i < nx; i++) {
        const lx = x0 + i * s - el.ox, ly = y0 + j * s - el.oy;
        V1[j * nx + i] = this.vn(lx * .03 + g.sh[7], ly * .05);
        V2[j * nx + i] = this.vn(lx * .03 + g.sh[7] + 31, ly * .05 + 9);
      }
      const XW = new Float32Array(N),
        YW = new Float32Array(N),
        B0 = new Float32Array(N),
        S = g.sh,
        w = el.w,
        h = el.h;
      for (let j = 0; j < ny; j++) for (let i = 0; i < nx; i++) {
        const idx = j * nx + i,
          px = x0 + i * s - el.ox,
          py = y0 + j * s - el.oy,
          u = Math.max(0, Math.min(1, px / w)),
          v = Math.max(0, Math.min(1, py / h)),
          m = Math.max(0, Math.min(1, 1 - py / (h * .5)));
        XW[idx] = px + (px < w / 2 ? S[0] : -S[1]) * Math.sin(Math.PI * v) + S[3] * V1[idx];
        YW[idx] = py - (S[2] * (u - .5) * 2 + S[4] * Math.sin(Math.PI * u * S[5] + S[6])) * m + S[3] * .7 * V2[idx];
        B0[idx] = this.rrD(g, XW[idx], YW[idx] - this.ty(g, Math.max(0, Math.min(w, px)), 0) * m);
      }
      this._N[nk + 'X'] = XW;
      this._N[nk + 'Y'] = YW;
      this._N[nk + '0'] = B0;
      if (g.mz && g.mz.length) {
        const pr = [], cl = (v, n) => Math.max(0, Math.min(n - 1, v));
        g.mz.forEach(([bx, by, bs]) => {
          const ci = cl(Math.round((el.ox + bx + bs / 2 - x0) / s), nx),
            cj = cl(Math.round((el.oy + by + bs / 2 - y0) / s), ny);
          for (let j = Math.max(0, Math.ceil((el.oy + by - y0) / s)); j < ny && y0 + j * s < el.oy + by + bs; j++) for (let i = Math.max(0, Math.ceil((el.ox + bx - x0) / s)); i < nx && x0 + i * s < el.ox + bx + bs; i++) pr.push(j * nx + i, cj * nx + ci);
        });
        this._N[nk + 'Q'] = new Int32Array(pr);
        this._N[nk + 'QT'] = new Float32Array(pr.length / 2);
      }
    }
    const kk = nk + 'K' + bk;
    if (bo && !this._N[kk]) {
      const K = new Float32Array(N);
      for (let j = 0; j < ny; j++) for (let i = 0; i < nx; i++) K[j * nx + i] = this.vn((x0 + i * s) * .16 + bk * 41.3, (y0 + j * s) * .16 + bk * 17.9);
      this._N[kk] = K;
    }
    const BK = bo ? this._N[kk] : null;
    const NA = this._N[nk],
      BB = this._N[nk + 'B'],
      XW = this._N[nk + 'X'],
      YW = this._N[nk + 'Y'],
      B0 = this._N[nk + '0'],
      hh2 = el.h * .5,
      F = this._N[nk + 'F'] || (this._N[nk + 'F'] = new Float32Array(N)),
      cols = this._N[nk + 'C'] || (this._N[nk + 'C'] = Array.from({ length: nx }, () => []));
    for (let i = 0; i < nx; i++) cols[i].length = 0;
    E.P.forEach(p => {
      const rr = Math.max(p[4], p[5]) + p[6] + 1,
        a = Math.max(0, Math.floor((p[0] - rr - x0) / s)),
        b = Math.min(nx - 1, Math.ceil((p[0] + rr - x0) / s));
      for (let i = a; i <= b; i++) cols[i].push(p);
    });
    for (let i = 0; i < nx; i++) {
      const X = x0 + i * s,
        lx = X - el.ox,
        tyx = hk ? this.ty(g, Math.max(0, Math.min(el.w, lx)), t * (1 + 2 * hk)) * (1 + 1.5 * hk) : this.ty(g, Math.max(0, Math.min(el.w, lx)), t),
        C = cols[i];
      let cm = -1e9;
      for (let c = 0; c < C.length; c++) {
        const p = C[c];
        cm = Math.max(cm, Math.max(p[1], p[3]) + Math.max(p[4], p[5]) + p[6] + 14);
      }
      for (let j = 0; j < ny; j++) {
        const Y = y0 + j * s, idx = j * nx + i, bc = BB[idx], b0 = B0[idx];
        if (b0 < -12.5 || (b0 > 14 && Y > cm)) {
          F[idx] = b0;
          continue;
        }
        let d = bc === bc ? bc : this.rrD(g, XW[idx], YW[idx] - tyx * Math.max(0, Math.min(1, 1 - (Y - el.oy) / hh2)));
        for (let c = 0; c < C.length; c++) {
          const p = C[c], rr = Math.max(p[4], p[5]) + p[6] + 1;
          if (Y < Math.min(p[1], p[3]) - rr || Y > Math.max(p[1], p[3]) + rr) continue;
          d = this.smin(d, this.ucap(X, Y, p[0], p[1], p[2], p[3], p[4], p[5]), p[6]);
        }
        F[idx] = d + rough * NA[idx] + (BK ? (hk ? bo0 + (bo - bo0) * Math.max(0, Math.min(1, 1.7 - 2 * (Y - el.oy) / el.h)) : bo) * BK[idx] : 0);
      }
    }
    const Q = this._N[nk + 'Q'];
    if (Q) {
      const T = this._N[nk + 'QT'];
      for (let q = 0; q < T.length; q++) T[q] = F[Q[2 * q + 1]];
      for (let q = 0; q < T.length; q++) F[Q[2 * q]] = T[q];
    }
    return { F, nx, ny, x0, y0, s };
  }

  /** Marching squares: изолиния поля на уровне iso → замкнутые ломаные в буферах. Строки сетки jA…jB можно пропустить: блик нужен только у верха и под нижним краем. */
  march(fd, iso, jA, jB) {
    const { F, nx, ny, x0, y0, s } = fd, N = nx * ny;
    jA = jA || 0;
    jB = jB || 0;
    let M = this._mb;
    if (!M || M.E.length < N * 2) {
      const c = Math.max(N * 2, 8192);
      M = this._mb = {
        E: new Int32Array(c),
        PX: new Float32Array(c),
        PY: new Float32Array(c),
        NB: new Int32Array(c * 2),
        OX: new Float32Array(c),
        OY: new Float32Array(c),
        TX: new Float32Array(c),
        TY: new Float32Array(c),
        seen: new Uint8Array(c),
        LS: new Int32Array(4096),
        nl: 0
      };
    }
    const { E, PX, PY, NB, OX, OY, LS, seen } = M;
    E.fill(0, 0, N * 2);
    let np = 0;
    const ep = (k, a, b, ax, ay, bx, by) => {
      let id = E[k];
      if (id) return id - 1;
      const t = (a - iso) / (a - b);
      id = np++;
      PX[id] = x0 + (ax + (bx - ax) * t) * s;
      PY[id] = y0 + (ay + (by - ay) * t) * s;
      NB[2 * id] = -1;
      NB[2 * id + 1] = -1;
      E[k] = id + 1;
      return id;
    };
    const link = (p, q) => {
      if (NB[2 * p] < 0) NB[2 * p] = q;
      else NB[2 * p + 1] = q;
      if (NB[2 * q] < 0) NB[2 * q] = p;
      else NB[2 * q + 1] = p;
    };
    const eT = (o, i, j, a, b) => ep(o * 2, a, b, i, j, i + 1, j),
      eR = (o, i, j, b, c) => ep((o + 1) * 2 + 1, b, c, i + 1, j, i + 1, j + 1),
      eB = (o, i, j, d, c) => ep((o + nx) * 2, d, c, i, j + 1, i + 1, j + 1),
      eL = (o, i, j, a, d) => ep(o * 2 + 1, a, d, i, j, i, j + 1);
    for (let j = 0; j < ny - 1; j++) {
      if (j >= jA && j < jB) continue;
      for (let i = 0, o = j * nx; i < nx - 1; i++, o++) {
        const a = F[o],
          b = F[o + 1],
          c = F[o + nx + 1],
          d = F[o + nx],
          sa = a < iso,
          sb = b < iso,
          sc = c < iso,
          sd = d < iso;
        if (sa === sb && sb === sc && sc === sd) continue;
        const cs = (sa ? 8 : 0) | (sb ? 4 : 0) | (sc ? 2 : 0) | (sd ? 1 : 0);
        switch (cs) {
          case 1: case 14: link(eL(o, i, j, a, d), eB(o, i, j, d, c));
          break;
          case 2: case 13: link(eB(o, i, j, d, c), eR(o, i, j, b, c));
          break;
          case 3: case 12: link(eL(o, i, j, a, d), eR(o, i, j, b, c));
          break;
          case 4: case 11: link(eT(o, i, j, a, b), eR(o, i, j, b, c));
          break;
          case 6: case 9: link(eT(o, i, j, a, b), eB(o, i, j, d, c));
          break;
          case 7: case 8: link(eL(o, i, j, a, d), eT(o, i, j, a, b));
          break;
          case 5: if ((a + b + c + d) / 4 < iso) {
            link(eL(o, i, j, a, d), eT(o, i, j, a, b));
            link(eB(o, i, j, d, c), eR(o, i, j, b, c));
          } else {
            link(eT(o, i, j, a, b), eR(o, i, j, b, c));
            link(eL(o, i, j, a, d), eB(o, i, j, d, c));
          }
          break;
          case 10: if ((a + b + c + d) / 4 < iso) {
            link(eT(o, i, j, a, b), eR(o, i, j, b, c));
            link(eL(o, i, j, a, d), eB(o, i, j, d, c));
          } else {
            link(eL(o, i, j, a, d), eT(o, i, j, a, b));
            link(eB(o, i, j, d, c), eR(o, i, j, b, c));
          }
          break;
        }
      }
    }
    seen.fill(0, 0, np);
    let nl = 0, no = 0;
    for (let pass = 0; pass < 2; pass++) for (let k = 0; k < np; k++) {
      if (seen[k] || (pass === 0 && NB[2 * k + 1] >= 0)) continue;
      const st = no;
      let prev = -1, cur = k;
      while (cur >= 0 && !seen[cur]) {
        seen[cur] = 1;
        OX[no] = PX[cur];
        OY[no] = PY[cur];
        no++;
        const n0 = NB[2 * cur], n1 = NB[2 * cur + 1], nxt = n0 !== prev ? n0 : n1;
        prev = cur;
        cur = nxt;
      }
      if (no - st > 2 && nl < LS.length - 1) LS[nl++] = st;
      else no = st;
    }
    LS[nl] = no;
    M.nl = nl;
    M.fd = fd;
    M.iso = iso;
    return M;
  }

  /** Ломаные из march → сглаженный SVG-путь (квадратичные кривые через середины отрезков). md — минимальный шаг между точками; fh — убрать внутренние просветы уже этой ширины. */
  pathBuf(M, md, fh) {
    const { OX, OY, TX, TY, LS, nl } = M, r = v => Math.round(v * 10) / 10;
    let d = '', sg = 0;
    if (fh) {
      const BM = M.BM || (M.BM = new Float32Array(4096)), fd = M.fd;
      for (let l = 0; l < nl; l++) {
        let sx = 0, sy = 0, xa = 1e9, xb = -1e9, ya = 1e9, yb = -1e9;
        const a = LS[l], b = LS[l + 1];
        for (let i = a; i < b; i++) {
          sx += OX[i];
          sy += OY[i];
          if (OX[i] < xa) xa = OX[i];
          if (OX[i] > xb) xb = OX[i];
          if (OY[i] < ya) ya = OY[i];
          if (OY[i] > yb) yb = OY[i];
        }
        const ci = Math.max(0, Math.min(fd.nx - 1, Math.round((sx / (b - a) - fd.x0) / fd.s))),
          cj = Math.max(0, Math.min(fd.ny - 1, Math.round((sy / (b - a) - fd.y0) / fd.s)));
        BM[l] = fd.F[cj * fd.nx + ci] > M.iso ? Math.min(xb - xa, yb - ya) : 1e9;
      }
      sg = 1;
    }
    for (let l = 0; l < nl; l++) {
      if (sg && M.BM[l] < fh) continue;
      let n = 0, lx = 0, ly = 0;
      for (let i = LS[l], b = LS[l + 1]; i < b; i++) {
        const x = OX[i], y = OY[i];
        if (!n || Math.abs(x - lx) + Math.abs(y - ly) > md) {
          TX[n] = x;
          TY[n] = y;
          n++;
          lx = x;
          ly = y;
        }
      }
      if (n < 3) continue;
      d += 'M' + r((TX[n - 1] + TX[0]) / 2) + ',' + r((TY[n - 1] + TY[0]) / 2);
      for (let i = 0; i < n; i++) {
        const k = i + 1 === n ? 0 : i + 1;
        d += 'Q' + r(TX[i]) + ',' + r(TY[i]) + ' ' + r((TX[i] + TX[k]) / 2) + ',' + r((TY[i] + TY[k]) / 2);
      }
      d += 'Z';
    }
    return d;
  }

  /** То же сглаживание для ломаных, заданных массивами точек [x, y]. */
  pathOf(loops, md) {
    const f = v => Math.round(v * 10) / 10;
    let d = '';
    loops.forEach(L => {
      const P = [];
      let lp = null;
      L.forEach(p => {
        if (!lp || Math.abs(p[0] - lp[0]) + Math.abs(p[1] - lp[1]) > md) {
          P.push(p);
          lp = p;
        }
      });
      if (P.length < 3) return;
      const n = P.length, mid = (a, b) => [(a[0] + b[0]) / 2, (a[1] + b[1]) / 2], m0 = mid(P[n - 1], P[0]);
      d += `M${f(m0[0])},${f(m0[1])}`;
      for (let i = 0; i < n; i++) {
        const a = P[i], m = mid(a, P[(i + 1) % n]);
        d += `Q${f(a[0])},${f(a[1])} ${f(m[0])},${f(m[1])}`;
      }
      d += 'Z';
    });
    return d;
  }

  /** Яркость цвета #rrggbb, 0…1. */
  lum(hex) {
    const n = parseInt(hex.slice(1), 16);
    return (.3 * (n >> 16 & 255) + .59 * (n >> 8 & 255) + .11 * (n & 255)) / 255;
  }

  /** Насыщенность цвета #rrggbb как разброс каналов, 0…255. */
  sat(hex) {
    const n = parseInt(hex.slice(1), 16), r = n >> 16 & 255, g = n >> 8 & 255, b = n & 255;
    return Math.max(r, g, b) - Math.min(r, g, b);
  }

  /** Создаёт экземпляр it.g по ключу it.key и seed кассеты: форма тела, волна кромки, подтёки, мозаика, линии потока. Результат не зависит от времени и состояния. */
  genItem(it) {
    const el = it.el,
      sd = this.props.seed + '/' + it.key,
      R = this.rng(sd, 'grime'),
      T = 6.283,
      { w, h, r, over, cf } = el,
      k = Math.max(.5, Math.min(1, h / 48, w / 110)),
      rb = over > 0 ? Math.min(r, Math.max(4, r * .45)) : r;
    const wq = k * el.wob * (.3 + .7 * el.calm);
    const wave = [Math.min(2.6, .8 + h * .035) * el.wob * el.calm, 24 + 20 * R(), R() * T, R() * T, T / (6000 + 4000 * R())],
      lw = 34 + 40 * R(),
      lp = R() * T,
      D = [];
    const cm = Math.min(w, h) / 2,
      rc = f => el.round ? f : Math.min(cm, f * (.5 + 1.3 * R())),
      sg = () => R() < .5 ? -1 : 1;
    const sh = [(1.5 + 3.5 * R()) * wq, (1.5 + 3.5 * R()) * wq, sg() * (1.5 + 2.5 * R()) * wq, 2.6 * wq, sg() * (1.5 + 2.5 * R()) * wq, 1 + Math.floor(R() * 3), R() * T, R() * 50, 0, 0, 0, 0];
    sh[8] = rc(r);
    sh[9] = rc(r);
    sh[10] = rc(rb);
    sh[11] = rc(rb);
    if (over > 0) {
      let x = rb + 1.5 + R() * 3;
      for (let i = 0; i < 80; i++) {
        const q = R(), s = R(), u = R(), gp = R(), p = R(), o = R(), a = R(), ty = R(), thin = ty < .25;
        const b = Math.max(1.9, (thin ? 2 + q : 3.4 + 4.6 * q * q) * k), cx = x + b * 1.45;
        if (cx + b * 1.45 > w - rb - 1) break;
        const mod = .5 + .5 * Math.sin(T * cx / lw + lp), drip = a < cf;
        let L = thin ? over * (.55 + .45 * s) : over * (.2 + .8 * Math.pow(mod, .7) * (.3 + .7 * s));
        L = Math.max(b * 2.2 + 1.5, Math.min(drip ? over * .74 : over, L));
        D.push({
          x: cx,
          b,
          Lmax: L,
          L0: Math.max(b * 1.8 + 1, L * (.5 + .2 * u)),
          P: 8000 + 6000 * p,
          off: o,
          u,
          drip
        });
        x = cx + b * 1.45 + (.8 + 9 * gp * gp * gp) * k;
      }
    }
    const g = { el, k, rb, wave, sh, D, I: [], sd, ML: [] },
      inb = (px, py, m) => this.bodyD(px, py, g, this.ty(g, Math.max(0, Math.min(w, px)), 0)) < -m;
    if (el.dead) g.dz = [w, this.rng(sd, 'mark:dead')() < .5 ? 0 : h, 62];
    if (el.edge && w >= 40 && !it.stat) this.edgeMos(g);
    if (el.marks && !it.stat) this.marks(g, R, inb, this.props.density * el.calm);
    it.g = g;
  }

  /** Примитивы подтёков и капель экземпляра на момент t. noDrop — режим нажатия: капля не отрывается, подтёк ждёт на пороге отрыва. */
  prims(g, t, noDrop) {
    const el = g.el, ox = el.ox, oy = el.oy, h = el.h, k = g.k, P = [];
    g.D.forEach(d => {
      let tt = t, hold = false;
      if (noDrop && d.drip) {
        const lim = d.P - 2000 - 2200 - 2200 * d.u + 2200 + 2200 * d.u - 1, tau = (t + d.off * d.P) % d.P;
        if (tau > lim && tau < lim + 1001) tt = t - (tau - lim);
        hold = true;
      }
      const s = this.dyn(d, tt, h, k),
        x = d.x + ox,
        tp = s.tip,
        kd = 4 * k,
        y1 = h - d.b,
        y2 = h + tp.L - tp.r2,
        nr = d.b * tp.nr;
      if (y2 - y1 <= Math.abs(tp.r2 - d.b) + .1) {
        const rr = Math.max(d.b, tp.r2);
        P.push([x, oy + y1, x, oy + y1, rr, rr, kd]);
      } else {
        const yn = Math.min(y2 - tp.r2 * 1.1, y2 - Math.abs(tp.r2 - nr) - .1);
        if (yn - y1 > Math.abs(d.b - nr) + .1) P.push([x, oy + y1, x, oy + yn, d.b, nr, kd], [x, oy + yn, x, oy + y2, nr, tp.r2, kd]);
        else P.push([x, oy + y1, x, oy + y2, d.b, tp.r2, kd]);
      }
      if (s.drop && !hold) P.push([x, oy + s.drop.y, x, oy + s.drop.y, s.drop.r, s.drop.r, 2.4 * k]);
    });
    return P;
  }

  /** Блоки порчи данных по периметру тела для состояния ошибки. */
  errBlocks(g) {
    const C = MutantGenerator.C, el = g.el, { w, h, ox, oy } = el, R = this.rng(g.sd, 'err');
    let s = '';
    for (let i = 0; i < 14; i++) {
      const bs = R() < .4 ? 8 : 4,
        side = Math.floor(R() * 4),
        u = R(),
        x = side < 2 ? Math.round(u * (w - bs) / 4) * 4 : side === 2 ? 0 : w - bs,
        y = side === 0 ? 0 : side === 1 ? h - bs : Math.round(u * (h - bs) / 4) * 4,
        c = R();
      s += `<rect x="${ox + x}" y="${oy + y}" width="${bs}" height="${bs}" fill="${c < .45 ? C.red : c < .75 ? C.ink : C.bg}"/>`;
    }
    return s;
  }

  /** Фронт стекающей заливки при смене цвета, p = 0…1. Подтёки фронта бегут впереди основной линии. */
  pourPath(g, p) {
    const el = g.el,
      { ox, oy, w, h } = el,
      R = this.rng(g.sd, 'pour'),
      n = Math.max(2, Math.round(w / 13)),
      F = [],
      f = v => Math.round(v * 10) / 10;
    for (let i = 0; i < n; i++) F.push([R() * w, 1.6 + 3.2 * R(), (5 + 16 * R()) * Math.max(.5, Math.min(1, h / 40)), .6 + .8 * R()]);
    const bot = oy + h + el.over + 34, base = oy - 10 + p * (bot - oy + 12), st = w > 120 ? 2 : 1;
    let d = '';
    for (let x = -12; x <= w + 12; x += st) {
      let y = base + 1.6 * Math.sin(x * .21 + p * 9);
      for (let i = 0; i < n; i++) {
        const q = F[i], u = (x - q[0]) / q[1];
        if (u > -3 && u < 3) y += q[2] * Math.min(1, p * 2.4 * q[3]) * Math.exp(-u * u);
      }
      d += (d ? 'L' : 'M') + f(ox + x) + ',' + f(Math.min(bot, y));
    }
    return d + `L${f(ox + w + 12)},${bot}L${ox - 12},${bot}Z`;
  }

  /** Геометрия перехода «веном» плашки выбора (Segmented button, Menu, NavigationDrawer) на момент p = 0…1. el — слот новой плашки; fx = { dx, dy — откуда пришла плашка (старое положение минус новое), px, py — точка клика от центра нового слота, ph — фаза изгиба }. Возвращает SVG-фигуры: SF — жирное щупальце и обе массы (их стыки сглаживаются размытием с порогом), S — тонкие щупальца и шипы; full — новая плашка заполнена, vert — движение по вертикали. */
  venomShapes(el, key, fx, p) {
    const f = v => v.toFixed(1), cx = el.ox + el.w / 2, cy = el.oy + el.h / 2;
    // Ось движения a и обращённый к цели край старой плашки E. P(s, c): s — вдоль оси от края, c — поперёк.
    const cl = v => Math.max(0, Math.min(1, v)), eo = v => 1 - Math.pow(1 - cl(v), 2), ei = v => cl(v) * cl(v);
    const mx = -fx.dx, my = -fx.dy, vert = Math.abs(my) >= Math.abs(mx), sg = (vert ? my : mx) < 0 ? -1 : 1;
    const ax = vert ? 0 : sg, ay = vert ? sg : 0, ha = (vert ? el.h : el.w) / 2, hp = (vert ? el.w : el.h) / 2;
    const D = Math.abs(vert ? my : mx) || 1, off = vert ? mx : my, ex = cx + fx.dx + ax * ha, ey = cy + fx.dy + ay * ha;
    const P = (s, c) => [ex + ax * s + Math.abs(ay) * c, ey + ay * s + Math.abs(ax) * c];
    const R = this.rng(this.props.seed + '/' + key, 'venom'), gap = Math.max(0, D - 2 * ha), lerp = (a, b2, t) => a + (b2 - a) * t;
    // pd — момент, когда старая плашка убыла до тонкой полоски: с него она не рисуется, а щупальца отпускают её край.
    const kd = 1 - Math.max(5, ha * (vert ? .24 : .5)) / (2 * ha);
    let pd = .8;
    for (let lo = .15, i = 0; i < 12; i++) {
      const mid = (lo + pd) / 2;
      if (this.ss(.15, .8, mid) < kd) lo = mid;
      else pd = mid;
    }
    const k = this.ss(.15, .8, p), full = p >= .8, n = vert ? Math.max(3, Math.min(6, Math.round(hp / 18))) : 3, lead = Math.floor(R() * n), rr = Math.min(el.r, hp);
    // Старая плашка убывает к краю и сужается к его середине; корни щупалец съезжаются вместе с ней.
    const hs = 1 - (vert ? .8 : .45) * k * k;
    // SF — жирное щупальце и обе массы (их стыки сглаживает фильтр), S — тонкие щупальца и шипы (остаются резкими).
    let S = '', SF = '';
    // Щупальца симбиота: выстреливают из старой плашки острыми изогнутыми хлыстами, цепляются за новое место,
    // по ним перетекает масса, в конце они отпускают старый край и втягиваются. Одно жирное, остальные тонкие.
    // Куда кликнули: туда идёт основная масса (жирное щупальце) и оттуда растёт новая плашка.
    const km = vert ? Math.min(hp * .6, 30) : 8, kc = Math.max(-hp + km, Math.min(hp - km, (vert ? fx.px : fx.py) || 0)), ks = Math.max(gap + ha * .5, Math.min(gap + 1.5 * ha, gap + ha + sg * ((vert ? fx.py : fx.px) || 0)));
    // Щупальца: одно жирное к точке клика, пара прямых вдоль оси, остальные изогнутые; плюс три коротких шипа, которые не достают до цели.
    for (let i = 0; i < n + 3; i++) {
      const jit = R(), q = R(), r1 = R(), r2 = R(), r3 = R(), r4 = R(), fat = i === lead, spike = i >= n;
      const straight = vert && !fat && !spike && (i === (lead + 1) % n || i === (lead + 2) % n);
      const b = fat ? Math.min(vert ? 11 + 5 * q : hp * .45, ha * .8) : spike ? 1.6 + 1.2 * q : 1.8 + 2.2 * q * q;
      const slot = spike ? -1 + 2 * jit : -1 + 2 * (i + .5 + (jit - .5) * .7) / n, cr0 = slot * Math.max(0, hp - rr - b), cr = fat ? lerp(cr0, kc, .6) : cr0;
      const ct = fat ? kc : straight || spike ? cr * .92 : Math.max(-hp + 8, Math.min(hp - 8, lerp(cr, kc, .45) + (r1 - .5) * hp * .7));
      const st = fat ? ks : gap + ha * (.7 + .6 * r2), dl = fat ? 0 : .03 + .14 * r3, pa = spike ? 9 : dl + (fat ? .2 : .16 + .1 * r4);
      if (p <= dl) continue;
      const sw = straight ? 0 : (r1 < .5 ? -1 : 1) * (spike ? 3 : 5 + 10 * r4) * (vert ? 1 : .5), ph = r2 * 6.28, on = p >= pa;
      // Шип резко выстреливает на треть–половину пути и втягивается.
      const hu = spike ? (.3 + .3 * r4) * eo((p - dl) / .12) * (1 - ei((p - dl - .16) / .22)) : on ? 1 : eo((p - dl) / (pa - dl)), tu = spike ? 0 : cl((p - pd) / ((fat ? .96 : Math.min(.96, pd + .12)) - pd));
      // Масса новой плашки там, где вцепилось жирное щупальце: разрастается из точки захвата до размера плашки.
      let mg = null;
      if (fat && on && !full) {
        const e = this.ss(0, 1, (p - pa) / (.8 - pa)), r0 = Math.min(b * 1.7 + 1, ha, hp);
        const ah = lerp(Math.min(ha, b * .9), ha, e), ch = lerp(Math.min(hp, b * 1.3), hp, e);
        mg = { e, r0, ah, ch, sc: Math.max(gap + ah, Math.min(gap + 2 * ha - ah, lerp(st, gap + ha, e))), cc: Math.max(-hp + ch, Math.min(hp - ch, lerp(ct, 0, this.ss(.35, 1, e)))) };
      }
      if (hu - tu > .04) {
        // Перемычка между двумя массами: пока летит — острый хлыст; вцепившись, тонкое щупальце расширяется к обоим концам,
        // жирное у источника убывает вместе со старой плашкой (множитель 1 − k), а у цели остаётся толстым.
        const th = on ? 1 + (fat ? .3 : .9) * Math.sin(Math.PI * cl((p - pa) / Math.max(.05, pd - pa))) : 1, C = [], W = [], N = fat ? 32 : 8;
        // Галтель: жирное щупальце входит в обе массы по дуге радиуса Rf, без угла на стыке.
        const nEdge = mg ? mg.sc - mg.ah : st, Rf = fat && on ? Math.min(26, ha * 1.3, nEdge * .6) : 0, arc = (d, r) => r - Math.sqrt(Math.max(0, r * r - Math.pow(r - Math.max(0, Math.min(r, d)), 2)));
        // У источника дуга и спрятанный корень не больше остатка старой плашки, иначе корень вылезает с её обратной стороны.
        const extO = 2 * ha * (1 - k), RfO = Math.min(Rf, extO * .8);
        let cA = 0, cZ = 0;
        // Хвост жирного щупальца после отрыва — полукруг радиусом в половину его толщины (ширина штриха W — полная, не полуширина).
        const rT = fat && tu > 0 ? b * th * (.94 - .35 * tu) / 2 : 0, tq = Math.min(hu, tu + rT / (st + 3));
        for (let j = 0; j <= N; j++) {
          const v = j / N, u = tq + (hu - tq) * v, sj = lerp(-3, st, u), cj = lerp(cr * hs, ct, fat ? this.ss(0, 1, u) : u) + sw * Math.pow(Math.sin(Math.PI * u), 2) * (1 + .5 * Math.sin(2 * Math.PI * u + ph));
          let wj = b * th * (!on ? (.1 + 1.9 * Math.pow(1 - v, 2)) : tu > 0 ? (fat ? .75 - .35 * tu + .19 * (1 - v) : .4 + 1.2 * (1 - tu) * (1 - v)) + 1.6 * Math.pow(v, 1.5) : (fat ? .75 + 1.6 * (1 - k) * Math.pow(1 - v, 1.5) + 1.6 * Math.pow(v, 1.5) : .4 + 1.8 * Math.pow(Math.abs(2 * v - 1), 3)));
          // Не толще, чем расстояние до дальнего края нового места, и не шире места плашки: наружу ничего не торчит.
          wj = Math.min(wj, Math.max(.6, gap + 2 * ha - 1 - sj));
          if (fat) wj = Math.min(wj, Math.max(b * .5, 2 * (hp - Math.abs(cj) - 1)));
          if (Rf > 1.5) {
            if (mg) wj = Math.min(wj + 2 * arc(nEdge - sj, Rf), Math.max(wj, 2 * (mg.ch - Math.abs(cj - mg.cc) - .5)));
            if (!tu && p < pd) wj = Math.min(wj + 2 * arc(sj, RfO), Math.max(wj, 2 * (hp * hs - Math.abs(cj) - .5)));
            // Ограничение по дальнему краю — только внутри новой массы: на подходе к ней оно срезало бы всю дугу.
            if (sj > nEdge) wj = Math.min(wj, Math.max(.6, gap + 2 * ha - 1 - sj));
          }
          if (!j) cA = cj;
          cZ = cj;
          C.push(P(sj, cj));
          W.push(wj);
        }
        // Штрих на концах сходится в ноль. Прячем эти концы внутри масс: корень — в старой плашке, голову жирного — в новой,
        // иначе щупальце выглядит оторванным.
        if (!tu) {
          C.unshift(P(-Math.max(1, Math.min(12, extO - 1)), cA));
          W.unshift(W[0]);
        }
        if (fat && on) {
          C.push(P(Math.min(gap + 2 * ha - 3, Math.max(st + Math.min(8, ha * .5), nEdge + 4)), cZ));
          W.push(W[W.length - 1]);
        }
        if (fat) SF += `<path d="${this.taper(C, W)}"/>`;
        else S += `<path d="${this.taper(C, W)}"/>`;
        if (rT > 1) SF += `<circle cx="${f(C[0][0])}" cy="${f(C[0][1])}" r="${f(Math.min(rT, W[1] / 2))}"/>`;
      }
      if (mg) {
        const A = P(mg.sc - mg.ah, mg.cc - mg.ch), Bq = P(mg.sc + mg.ah, mg.cc + mg.ch), w0 = Math.abs(A[0] - Bq[0]), h0 = Math.abs(A[1] - Bq[1]);
        SF += `<rect x="${f(Math.min(A[0], Bq[0]))}" y="${f(Math.min(A[1], Bq[1]))}" width="${f(w0)}" height="${f(h0)}" rx="${f(Math.min(lerp(Math.min(mg.ah, mg.ch), Math.min(el.r, ha, hp), this.ss(.65, 1, mg.e)), w0 / 2, h0 / 2))}"/>`;
      }
    }
    // Старая плашка убывает к краю, из которого выходят щупальца.
    const ext = 2 * ha * (1 - k);
    if (p < pd) {
      const A = P(-ext, -hp * hs), Bq = P(0, hp * hs), w0 = Math.abs(A[0] - Bq[0]), h0 = Math.abs(A[1] - Bq[1]);
      SF += `<rect x="${f(Math.min(A[0], Bq[0]))}" y="${f(Math.min(A[1], Bq[1]))}" width="${f(w0)}" height="${f(h0)}" rx="${f(Math.min(el.r, w0 / 2, h0 / 2))}"/>`;
    }
    return { SF, S, full, vert };
  }
}
window.MutantGenerator = MutantGenerator;
})();
