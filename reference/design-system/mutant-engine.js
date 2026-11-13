/*
 * mutant-ui · хост движка для браузера. Наследует MutantGenerator (mutant-generator.js) и сам его подгружает.
 *
 * Здесь всё, что не переносится в Kotlin как есть: измерение вёрстки, сборка SVG, события, планирование кадров,
 * Лента, потеря кадра, темы и токены. Разметка: data-mu-stage — сцена, data-mu="тип" — компонент,
 * data-key — ключ экземпляра, data-state, data-press, data-mu-toggle, data-mu-cocoon, data-mu-divider, data-mu-tape.
 *
 * Время: this.time — часы движка в мс (стоят на паузе, масштабируются speed). Кадры покоя, нагрев при нажатии
 * и переходы считаются только от них. performance.now используется лишь для планирования и бюджета кадра.
 */
(() => {
const SRC = document.currentScript && document.currentScript.src;
function define() {
class MutantEngine extends window.MutantGenerator {
  /** root — корневой узел страницы; props — seed, fps, boil, rough, density, speed, keyframeLoss, theme. */
  constructor(root, props) {
    super(props);
    this.root = root;
    this.props = Object.assign({ seed: 5, fps: 8, boil: .8, rough: .35, density: 1, speed: 1, keyframeLoss: true }, props || {});
    Object.assign(MutantEngine.C, MutantEngine.TH[this.props.theme] || MutantEngine.TH.efir);
    this._N = {};
    this.items = [];
    this.stages = [];
    this.time = 0;
    this.rr = 0;
    this.paused = false;
    this.uid = MutantEngine.n = (MutantEngine.n || 0) + 1;
    this.iu = 0;
  }

  /** Имя цвета темы или #rrggbb → #rrggbb. */
  col(v, d) { return !v ? d : v[0] === '#' ? v : MutantEngine.C[v] || d; }

  /** Положение и размер узла относительно сцены, в CSS px (= dp). */
  box(n, host) {
    let x = 0, y = 0, e = n;
    while (e && e !== host) {
      x += e.offsetLeft;
      y += e.offsetTop;
      e = e.offsetParent;
    }
    return e === host ? { x, y, w: n.offsetWidth, h: n.offsetHeight } : null;
  }

  /** Чистая зона: общий прямоугольник текста и иконок внутри узла плюс поля 6 dp по горизонтали и 2 dp по вертикали. В Compose это приходит из измерения текста. */
  textZone(n, b) {
    const nr = n.getBoundingClientRect(), sc = nr.width / b.w || 1, rg = document.createRange();
    let xa = 1e9, ya = 1e9, xb = -1e9, yb = -1e9;
    const add = r => {
      if (r.width < 1 || r.height < 1) return;
      xa = Math.min(xa, r.left);
      ya = Math.min(ya, r.top);
      xb = Math.max(xb, r.right);
      yb = Math.max(yb, r.bottom);
    };
    const tw = document.createTreeWalker(n, NodeFilter.SHOW_TEXT);
    let tn;
    while ((tn = tw.nextNode())) {
      if (!tn.nodeValue.trim()) continue;
      rg.selectNodeContents(tn);
      add(rg.getBoundingClientRect());
    }
    n.querySelectorAll('svg,[data-mu],[data-keep]').forEach(c => add(c.getBoundingClientRect()));
    return xb < xa ? null : [(xa - nr.left) / sc - 6, (ya - nr.top) / sc - 2, (xb - xa) / sc + 12, (yb - ya) / sc + 4];
  }

  /** Читает data-атрибуты узла и собирает описание экземпляра el: слот, профиль типа, состояние, чистая зона. */
  makeItem(st, n, ord) {
    const b = this.box(n, st.host);
    if (!b || b.w < 6 || b.h < 6) return null;
    const C = MutantEngine.C,
      d = n.dataset,
      P = MutantEngine.K[d.mu] || MutantEngine.K.btn,
      num = (v, def) => v === undefined || v === '' ? def : +v;
    const fill = this.col(d.fill, C[P.fill]),
      state = d.state || '',
      calm = num(d.calm, 1),
      over0 = num(d.over, P.over),
      tz = this.textZone(n, b);
    const el = {
      ox: b.x,
      oy: b.y,
      w: b.w,
      h: b.h,
      r: Math.min(num(d.r, P.r), b.w / 2, b.h / 2),
      over: over0 * (calm < 1 ? .4 + .6 * calm : 1),
      cf: num(d.cf, P.cf),
      ow: num(d.ow, P.ow || 3),
      fill,
      hl: this.lum(fill) > .45 ? '#ffffff' : C.hlDark,
      dead: num(d.dead, P.dead || 0),
      marks: num(d.marks, P.marks ?? 1),
      edge: num(d.edge, P.edge ?? 1),
      wob: num(d.wob, P.wob ?? 1),
      round: P.round || 0,
      calm,
      state,
      stroke: this.col(d.stroke, C.ink),
      shadow: d.shadow ? this.col(d.shadow, C.blue) : null,
      slip: num(d.slip, C.print && !P.round && state !== 'clean' ? 1.5 : 0),
      brackets: d.brackets !== undefined,
      still: d.still !== undefined || st.host.dataset.muT !== undefined,
      tFix: +(d.t ?? st.host.dataset.muT ?? 5000),
      hkFix: d.heatK !== undefined ? +d.heatK : null,
      pourFix: d.pour ? { p: +d.pour.split('|')[0], from: this.col(d.pour.split('|')[1], C.surf2) } : null,
      slideFix: d.slide ? { p: +d.slide.split('|')[0], dx: +d.slide.split('|')[1] } : null,
      tz: tz || [-99, -99, 0, 0],
      cz: tz ? [tz[0] - 2, tz[1] - 2, tz[2] + 4, tz[3] + 4] : [0, 0, 0, 0]
    };
    const it = {
      st,
      n,
      el,
      uid: ++this.iu,
      id: st.pre + 'e' + ord,
      key: d.key || (st.key + '/' + d.mu + '#' + ord),
      heat: n._muPulse ? (n._muPulse = 0, .8) : 0,
      pressed: false,
      forced: d.heat !== undefined,
      stat: state === 'disabled' || state === 'clean'
    };
    if (d.press !== undefined && !n._mu) {
      n._mu = 1;
      const on = () => {
        const t = n._muIt;
        if (t) t.pressed = true;
      },
        off = () => {
        const t = n._muIt;
        if (t) t.pressed = false;
      };
      n.addEventListener('pointerdown', on);
      n.addEventListener('pointerup', off);
      n.addEventListener('pointerleave', off);
      n.addEventListener('pointercancel', off);
    }
    const fxs = n._muFx;
    n._muFx = null;
    if (fxs && !it.stat) {
      const pour = fxs.from && fxs.from !== fill ? fxs.from : null;
      if (pour || fxs.dx || fxs.dy) it.fx = {
        t0: this.time,
        dur: fxs.venom ? 720 : fxs.dx || fxs.dy ? 330 : Math.max(300, Math.min(620, 260 + 5 * b.h)),
        pour,
        dx: fxs.dx || 0,
        dy: fxs.dy || 0,
        venom: !!fxs.venom,
        px: fxs.px || 0,
        py: fxs.py || 0,
        ph: (this.hash(it.key) % 628) / 100
      };
    }
    n._muIt = it;
    return it;
  }

  /** Статичные состояния без слизи: disabled (обесцвечено, строки, снег, сдвинутая полоса) и clean (чистый кадр). */
  staticHtml(it, st) {
    const C = MutantEngine.C,
      ink = C.ink,
      g = it.g,
      el = g.el,
      { ox, oy, w, h, cz } = el,
      r = el.r,
      ow = el.ow,
      id = it.id,
      box = `x="${ox}" y="${oy}" width="${w}" height="${h}"`,
      body = `<rect ${box} rx="${r}"`;
    if (el.state === 'clean') return {
      defs: '',
      art: `<g id="${id}">${body} fill="${el.fill}" stroke="${el.stroke}" stroke-width="${ow}"/><path d="M${ox + 5},${oy + r + 4}V${oy + r}A${Math.max(1, r - 5)},${Math.max(1, r - 5)} 0 0 1 ${ox + r},${oy + 5}H${ox + r + Math.min(18, w * .2)}" fill="none" stroke="${el.hl}" stroke-width="2.2" stroke-linecap="round"/></g>`
    };
    const R = this.rng(g.sd, 'vhs'),
      fill = this.desat(el.fill, (C.print ? this.sat(el.fill) > 90 : el.hl === '#ffffff') ? C.disK : 1),
      hh = Math.max(4, Math.round(h * .14)),
      by = oy + (cz[2] > 0 ? Math.round(Math.min(h - hh, cz[1] + cz[3] + R() * Math.max(0, h - cz[1] - cz[3] - hh))) : Math.round(R() * (h - hh))),
      dx = (4 + 3 * R()).toFixed(1);
    const inCz = (x, y, l) => cz[2] > 0 && x < cz[0] + cz[2] && x + l > cz[0] && y < cz[1] + cz[3] && y + 1 > cz[1];
    let ln = '';
    for (let i = 0; i < Math.max(2, Math.round(w / 55)); i++) {
      const x = R() * w, y = Math.round(R() * h) + .5, l = Math.min(w * .5, 8 + 32 * R());
      if (!inCz(x, y, l)) ln += `<line x1="${(ox + x).toFixed(1)}" x2="${(ox + x + l).toFixed(1)}" y1="${oy + y}" y2="${oy + y}" stroke="${C.drop}" stroke-width="1" opacity=".8"/>`;
    }
    for (let y = h - Math.min(5, h * .2); y < h; y++) for (let i = 0; i < Math.max(1, Math.round(w / 65)); i++) ln += `<rect x="${(ox + R() * w).toFixed(1)}" y="${(oy + y).toFixed(1)}" width="${(3 + 18 * R()).toFixed(1)}" height="1" fill="${C.drop}" opacity="${(.3 + .4 * R()).toFixed(2)}"/>`;
    const fx = `<rect ${box} fill="url(#${st.pre}vl)"/>`;
    return {
      defs: `<clipPath id="${id}b">${body}/></clipPath><clipPath id="${id}v"><rect x="${ox - 20}" y="${by}" width="${w + 40}" height="${hh}"/></clipPath>`,
      art: `<g id="${id}">${body} fill="${fill}"/><g clip-path="url(#${id}b)">${el.edge && w >= 40 ? this.edgeMos(g) : ''}${fx}<rect ${box} fill="url(#${st.pre}vs)"/>${ln}</g>${body} fill="none" stroke="${ink}" stroke-width="${ow}"/><g clip-path="url(#${id}v)"><g transform="translate(${dx} 0)">${body} fill="${fill}"/>${fx}<rect x="${ox}" y="${by}" width="${w}" height="${hh}" fill="${C.drop}" opacity=".22"/>${body} fill="none" stroke="${ink}" stroke-width="${ow}"/></g></g></g>`
    };
  }

  /** SVG-каркас живого экземпляра: слои заливки, мозаики, линий, блика и обводки. Пути в него пишет drawItem. */
  itemHtml(it, st) {
    if (it.stat) return this.staticHtml(it, st);
    const C = MutantEngine.C, ink = C.ink, g = it.g, el = g.el, id = it.id, ow = el.ow, r = el.r;
    const deco = (el.edge && el.w >= 40 ? this.edgeMos(g) : '') + (el.dead ? this.deadPx(g) : '') + (el.state === 'error' ? this.errBlocks(g) : '');
    const bb = `x="${el.ox - 12}" y="${el.oy - 15}" width="${el.w + 24}" height="${el.h + el.over + 45}"`,
      bx = `x="${el.ox - 10}" width="${el.w + 20}" fill="#fff"`;
    let s = `<g id="${id}">`;
    if (el.shadow) s += `<path data-d fill="${el.shadow}" fill-rule="evenodd" transform="translate(4 4)"/>`;
    if (el.state === 'error') s += `<path data-d fill="none" stroke="${C.red}" stroke-width="${ow}" transform="translate(2.5 0)"/>`;
    s += `<g clip-path="url(#${id}b)"${el.slip ? ` transform="translate(${el.slip} ${el.slip})"` : ''}><rect data-f ${bb} fill="${el.fill}"/>${(it.fx && it.fx.pour) || el.pourFix ? `<path data-pour fill="${el.pourFix ? el.pourFix.from : it.fx.pour}"/>` : ''}<rect ${bb} fill="url(#${st.pre}sp)"/>${deco}<path data-m fill="${ink}"/>${el.state === 'loading' ? `<rect data-band ${bx} height="6" opacity=".5"/><rect data-band ${bx} height="3" opacity=".35"/>` : ''}</g>`;
    s += `<g data-hg clip-path="url(#${id}h)"><path data-a fill="${el.hl}" fill-rule="evenodd"/><path data-a data-f fill="${el.fill}" fill-rule="evenodd" transform="translate(1.5 1.5)"/></g><path data-d data-o fill="none" stroke="${el.stroke}" stroke-width="${ow}" stroke-linejoin="round"/>`;
    if (el.brackets) {
      const x0 = el.ox - 7, y0 = el.oy - 7, x1 = el.ox + el.w + 7, y1 = el.oy + el.h + 7, q = 10;
      s += `<path d="M${x0},${y0 + q}V${y0}H${x0 + q}M${x1 - q},${y0}H${x1}V${y0 + q}M${x1},${y1 - q}V${y1}H${x1 - q}M${x0 + q},${y1}H${x0}V${y1 - q}" fill="none" stroke="${C.focus}" stroke-width="2"/>`;
    }
    return {
      defs: `<clipPath id="${id}h"><rect x="${el.ox - 6}" y="${el.oy + el.h + .5}" width="${el.w + 12}" height="${el.over + 30}"/><rect x="${el.ox - 4}" y="${el.oy - 6}" width="${r + 13}" height="${r + 10}"/></clipPath><clipPath id="${id}b"><path id="${id}p" clip-rule="evenodd"/></clipPath>`,
      art: s + '</g>'
    };
  }

  /** Пересобирает сцену: находит узлы data-mu, создаёт экземпляры, разделители, Коконы, Ленту и слой потери кадра. */
  buildStage(st) {
    const C = MutantEngine.C, host = st.host, P = this.props;
    if (!host.offsetWidth || !host.offsetHeight) return;
    st.built = true;
    st.W = host.offsetWidth;
    st.H = host.offsetHeight;
    st.svg.setAttribute('width', st.W);
    st.svg.setAttribute('height', st.H);
    st.svg.setAttribute('viewBox', `0 0 ${st.W} ${st.H}`);
    this.items = this.items.filter(it => it.st !== st);
    st.items = [];
    [...host.querySelectorAll('[data-mu]')].forEach((n, i) => {
      if (n.closest('[data-mu-stage]') !== host) return;
      const it = this.makeItem(st, n, i);
      if (it) {
        st.items.push(it);
        this.items.push(it);
      }
    });
    const R = this.rng(P.seed + '/' + st.key, 'speck');
    let sp = '';
    for (let i = 0; i < 8; i++) sp += `<rect x="${(R() * 40).toFixed(1)}" y="${(R() * 40).toFixed(1)}" width="${(.6 + R() * .8).toFixed(2)}" height="${(.6 + R() * .8).toFixed(2)}" fill="${C.ink}"/>`;
    let sn = '';
    for (let i = 0; i < 60; i++) {
      const c = R();
      sn += `<rect x="${Math.floor(R() * 32)}" y="${Math.floor(R() * 32)}" width="1" height="1" fill="${c < .4 ? '#000' : C.drop}" opacity="${(.2 + .4 * c).toFixed(2)}"/>`;
    }
    let defs = `<pattern id="${st.pre}vs" width="32" height="32" patternUnits="userSpaceOnUse">${sn}</pattern><pattern id="${st.pre}sp" width="40" height="40" patternUnits="userSpaceOnUse"><g opacity="${(.35 * Math.min(1, P.rough * 2)).toFixed(2)}">${sp}</g></pattern>${C.print ? `<pattern id="${st.pre}vl" width="3" height="3" patternUnits="userSpaceOnUse"><circle cx="1.5" cy="1.5" r=".75" fill="${C.ink}" opacity=".4"/></pattern>` : `<pattern id="${st.pre}vl" width="4" height="2" patternUnits="userSpaceOnUse"><rect y="1" width="4" height="1" fill="${C.ink}" opacity=".42"/></pattern>`}`,
      art = '';
    st.items.forEach(it => {
      this.genItem(it);
      const o = this.itemHtml(it, st);
      defs += o.defs;
      art += o.art;
    });
    [...host.querySelectorAll('[data-mu-divider]')].forEach((n, i) => {
      const b = this.box(n, host);
      if (!b) return;
      const Q = this.rng(P.seed + '/' + st.key + '/div' + i, 'd'), y = b.y + Math.round(b.h / 2) - 1;
      let x = 0;
      while (x < b.w) {
        const len = Math.min(b.w - x, 6 + Q() * 46), comet = Q() < .3 && len > 20;
        art += `<rect x="${(b.x + x).toFixed(1)}" y="${y}" width="${len.toFixed(1)}" height="2" fill="${C.blue}"/>`;
        if (comet) for (let q = 0; q < 4; q++) art += `<rect x="${(b.x + x + len - 4 - q * 6).toFixed(1)}" y="${y}" width="${q ? 6 : 4}" height="2" fill="${C.comet}" opacity="${[1, .55, .3, .14][q]}"/>`;
        x += len + 2 + Q() * Q() * 18;
      }
    });
    st.coc = [];
    [...host.querySelectorAll('[data-mu-cocoon]')].forEach((n, i) => {
      const b = this.box(n, host);
      if (!b) return;
      const Q = this.rng(P.seed + '/' + st.key + '/coc' + i, 'c'),
        pal = [C.coc1, C.surf, C.surf2, C.coc2, C.surf, C.coc1],
        id = st.pre + 'c' + i;
      let s = '';
      for (let y = 0; y < b.h; y += 16) for (let x = 0; x < b.w; x += 16) {
        const edge = x < 16 || y < 16 || x + 32 > b.w || y + 32 > b.h;
        if (edge) {
          for (let q = 0; q < 4; q++) if (Q() < .8) s += `<rect x="${b.x + x + (q & 1) * 8}" y="${b.y + y + (q >> 1) * 8}" width="8" height="8" fill="${pal[Math.floor(Q() * 6)]}"/>`;
        } else if (Q() < .94) s += `<rect x="${b.x + x}" y="${b.y + y}" width="16" height="16" fill="${pal[Math.floor(Q() * 6)]}"/>`;
      }
      defs += `<clipPath id="${id}"><rect x="${b.x}" y="${b.y}" width="${b.w}" height="${b.h}" rx="${Math.min(10, b.h / 2)}"/></clipPath>`;
      art += `<g id="${id}g" clip-path="url(#${id})">${s}</g>`;
      st.coc.push(id + 'g');
    });
    st.defs.innerHTML = defs;
    st.art.innerHTML = art;
    st.items.forEach(it => {
      if (it.stat) return;
      const G = st.art.querySelector('#' + it.id);
      if (!G) return;
      it.H = {
        D: [st.defs.querySelector('#' + it.id + 'p'), ...G.querySelectorAll('[data-d]')],
        O: G.querySelector('[data-o]'),
        A: [...G.querySelectorAll('[data-a]')],
        F: [...G.querySelectorAll('[data-f]')],
        M: G.querySelector('[data-m]'),
        B: [...G.querySelectorAll('[data-band]')],
        PX: [...G.querySelectorAll('[data-px]')],
        P: G.querySelector('[data-pour]'),
        HG: G.querySelector('[data-hg]')
      };
      it.G = G;
    });
    st.coc = st.coc.map(id => [...st.art.querySelectorAll('#' + id + ' rect')]);
    st.anims = [...host.querySelectorAll('[data-mu-anim]')];
    const tp = host.querySelector('[data-mu-tape]');
    if (tp) {
      tp.innerHTML = this.tapeHtml(st);
      st.tp = {
        pat: tp.querySelector('pattern'),
        band: tp.querySelector('[data-tb]'),
        nz: tp.querySelector('[data-tn]')
      };
    }
    if (host.dataset.muMosh !== undefined && !st.mosh) {
      const cv = document.createElement('canvas');
      cv.style.cssText = 'position:absolute;left:0;top:0';
      st.lay.appendChild(cv);
      st.mosh = cv;
    }
    if (st.mosh) {
      st.mosh.width = st.W * 2;
      st.mosh.height = st.H * 2;
      st.mosh.style.width = st.W + 'px';
      st.mosh.style.height = st.H + 'px';
      st.m = null;
      st.moshFix = 0;
    }
  }

  /** Рисует один кадр экземпляра: поле → контур, блик, линии потока; переходы pour и slide; кипение и бегущая мозаика при нажатии. */
  drawItem(it, ts, tsH) {
    const C = MutantEngine.C,
      g = it.g,
      el = g.el,
      H = it.H,
      hk = el.hkFix != null ? el.hkFix : it.heat,
      hot = hk > 0 || it.pressed,
      t = el.still ? el.tFix : hot ? tsH : ts,
      P = this.props;
    const bk = Math.floor(t / (hot ? 125 / 1.5 : 125) + .001) % 4,
      bo0 = P.boil * el.calm,
      boE = bo0 * (1 + 2 * hk) + .6 * hk,
      E = { g, e: it.uid, P: this.prims(g, t, hot) };
    if (hk > 0) E.P.push(...this.bubbles(g, t, hk));
    const fd = this.field(E, t, P.rough * el.calm, bk, boE, hk, bo0), D = this.pathBuf(this.march(fd, 0), .9, 5);
    H.D.forEach(p => p.setAttribute('d', D));
    H.O.setAttribute('stroke-width', (el.ow * (1 + .12 * boE * this.vn(bk * 3.3 + it.uid * 7.1, 2.5))).toFixed(2));
    if (g.ML.length && H.M) H.M.setAttribute('d', this.markPath(g, t, bk, boE));
    it.dc = (it.dc || 0) + 1;
    if (hk > 0 || it.hc) {
      it.hc = hk > 0;
      const c = hk > 0 ? this.mix(el.fill, C.amber, Math.min(1, hk * 1.15), hk > .5 && Math.floor(t / 333) % 2 ? .2 : 0) : el.fill;
      H.F.forEach(p => p.setAttribute('fill', c));
    }
    if (el.pourFix && H.P) {
      H.P.setAttribute('d', el.pourFix.p >= 1 ? '' : this.pourPath(g, el.pourFix.p));
      if (H.HG) H.HG.style.display = el.pourFix.p >= 1 ? '' : 'none';
    }
    if (el.slideFix) {
      const p = el.slideFix.p,
        e = 1 - Math.pow(1 - p, 2),
        q = Math.sin(Math.PI * p),
        cx = el.ox + el.w / 2,
        cy = el.oy + el.h / 2;
      it.G.setAttribute('transform', `translate(${(el.slideFix.dx * (1 - e)).toFixed(1)} 0) translate(${cx} ${cy}) scale(${(1 + .4 * q).toFixed(2)} ${(1 - .18 * q).toFixed(2)}) translate(${-cx} ${-cy})`);
    }
    if (it.fx) {
      const fx = it.fx, p = Math.min(1, Math.floor((this.fxTime() - fx.t0) / (fx.venom ? 41.67 : 83.33)) * (fx.venom ? 41.67 : 83.33) / fx.dur);
      if (fx.pour && H.P) {
        H.P.setAttribute('d', p >= 1 ? '' : this.pourPath(g, p));
        if (H.HG) H.HG.style.display = p >= 1 ? '' : 'none';
      }
      if (fx.venom) this.venom(it, fx, p);
      else if (fx.dx || fx.dy) {
        const e = 1 - Math.pow(1 - p, 2), q = Math.sin(Math.PI * p), cx = el.ox + el.w / 2, cy = el.oy + el.h / 2;
        if (p >= 1) it.G.removeAttribute('transform');
        else it.G.setAttribute('transform', `translate(${(fx.dx * (1 - e)).toFixed(1)} ${((fx.dy || 0) * (1 - e)).toFixed(1)}) translate(${cx} ${cy}) scale(${(1 + .4 * q).toFixed(2)} ${(1 - .18 * q).toFixed(2)}) translate(${-cx} ${-cy})`);
      }
      if (p >= 1) it.fx = null;
    }
    if (H.PX.length && (hk > 0 || it.pxh)) {
      if (hk > 0) {
        it.pxh = 1;
        const Q = this.rng(g.sd, 'px' + Math.round(t / 83.33)), top = g.ms === 'top';
        H.PX.forEach(p => {
          if (!p._o) p._o = [p.getAttribute('fill'), p.getAttribute('opacity')];
          const c = Q(),
            v = Q(),
            d = Q(),
            n = Q(),
            bs = +p.getAttribute('width'),
            sh = d < .5 * hk ? (n < .5 ? -1 : 1) * bs * (n < .25 || n > .75 ? 2 : 1) : 0;
          p.setAttribute('fill', c < .38 ? '#000' : c < .78 ? '#fff' : c < .9 ? C.mag : C.cyan);
          p.setAttribute('opacity', v < .22 * hk ? 0 : (.3 + .5 * v).toFixed(2));
          p.setAttribute('transform', sh ? (top ? `translate(${sh} 0)` : `translate(0 ${sh})`) : '');
        });
      } else {
        it.pxh = 0;
        H.PX.forEach(p => {
          if (p._o) {
            p.setAttribute('fill', p._o[0]);
            p.setAttribute('opacity', p._o[1]);
            p.setAttribute('transform', '');
          }
        });
      }
    }
    if (!it.a0 || hot || el.hl === '#ffffff' || it.dc % 2) {
      it.a0 = 1;
      const A = this.pathBuf(this.march(fd, -3.4, Math.ceil((el.r + 19) / fd.s), Math.floor((el.h + 7) / fd.s)), 1.1);
      H.A.forEach(p => p.setAttribute('d', A));
    }
    if (H.B.length) {
      const ph = (t % 900) / 900;
      H.B.forEach((b, i) => b.setAttribute('y', (el.oy + el.h + 4 - ((ph + i * .45) % 1) * (el.h + 12)).toFixed(1)));
    }
  }

  /** Лента: строки развёртки, зерно, полоса трекинга, шум головки. */
  tapeHtml(st) {
    const R = this.rng('tape', st.key), W = st.W, H = st.H;
    let tl = '';
    for (let i = 0; i < 44; i++) {
      const x = Math.round(R() * 90), y = Math.round(R() * 180), v = R();
      tl += `<rect x="${x}" y="${y}" width="${v > .8 ? 3 : 1}" height="1" fill="${MutantEngine.C.print ? '#6b5f48' : '#fff'}" opacity="${(.1 + .06 * v).toFixed(3)}"/>`;
    }
    return `<div style="position:absolute;inset:0;background:repeating-linear-gradient(to bottom, rgba(0,0,0,${MutantEngine.C.print ? 0 : .075}) 0 1px, transparent 1px 3px)"></div><div data-tb style="position:absolute;left:0;right:0;top:0;height:22px;display:none;${MutantEngine.C.print ? 'background:rgba(20,17,15,.05);border-top:1px solid rgba(20,17,15,.28);border-bottom:1px solid rgba(20,17,15,.1)' : 'background:rgba(233,242,228,.07);border-top:1px solid rgba(233,242,228,.3);border-bottom:1px solid rgba(5,6,12,.5)'}"></div><svg width="${W}" height="${H}" style="position:absolute;left:0;top:0"><defs><pattern id="${st.pre}tg" width="90" height="180" patternUnits="userSpaceOnUse">${tl}</pattern></defs><rect width="${W}" height="${H}" fill="url(#${st.pre}tg)"/><g data-tn></g></svg>`;
  }

  /** Шаг сцены раз в тик 125 мс: зерно и полоса Ленты, катушки, курсор, перемотка, мерцание Кокона. */
  stageStep(st, fi, t) {
    const ft = st.host.dataset.muT;
    if (ft !== undefined) {
      t = +ft;
      fi = Math.floor(t / 125);
    }
    if (st.fi === fi) return;
    st.fi = fi;
    if (st.host.offsetWidth !== st.W || st.host.offsetHeight !== st.H) {
      st.built = false;
      return;
    }
    const T = st.tp, W = st.W, H = st.H;
    if (T && T.pat) {
      T.pat.setAttribute('patternTransform', `translate(${(fi * 37) % 90} ${(fi * 71) % 180})`);
      const ph = (t % 7000) / 7000,
        R = this.rng('hd', st.key + fi),
        nz = (y, hh) => `<rect x="${(R() * W).toFixed(1)}" y="${y + Math.round(R() * hh)}" width="${(6 + R() * 40).toFixed(1)}" height="1" fill="${MutantEngine.C.print ? '#6b5f48' : '#fff'}" opacity="${((MutantEngine.C.print ? .1 : .3) + (MutantEngine.C.print ? .1 : .4) * R()).toFixed(2)}"/>`;
      let s = '';
      if (ph < .3) {
        const by = Math.round(H - (ph / .3) * (H + 30));
        T.band.style.display = 'block';
        T.band.style.top = by + 'px';
        for (let i = 0; i < 14; i++) s += nz(by, 22);
      } else T.band.style.display = 'none';
      for (let i = 0; i < Math.round(W / 10); i++) s += nz(H - 12, 11);
      T.nz.innerHTML = s;
    }
    st.anims.forEach(n => {
      const k = n.dataset.muAnim;
      if (k === 'reel') n.style.transform = `rotate(${(fi * 30) % 360}deg)`;
      else if (k === 'blink') n.style.opacity = fi % 8 < 5 ? 1 : 0;
      else if (k === 'count') {
        const sec = 2537 - (fi * 61) % 2537;
        n.textContent = '◄◄ ' + Math.floor(sec / 3600) + ':' + String(Math.floor(sec / 60) % 60).padStart(2, '0') + ':' + String(sec % 60).padStart(2, '0');
      }
      else if (k === 'rewind') {
        const pw = n.parentNode.offsetWidth, w = n.offsetWidth;
        n.style.transform = `translateX(${((fi * 12) % (pw + w)) - w}px)`;
      }
    });
    const pal = [MutantEngine.C.coc1, MutantEngine.C.surf, MutantEngine.C.surf2, MutantEngine.C.coc2];
    st.coc.forEach((rs, i) => {
      if (!rs.length) return;
      const R = this.rng('coc', st.key + i + '/' + fi);
      for (let q = 0; q < Math.max(2, rs.length * .06); q++) rs[Math.floor(R() * rs.length)].setAttribute('fill', pal[Math.floor(R() * 4)]);
    });
  }

  /** Потеря ключевого кадра: снимает сцену в растр и раскладывает полосу на блоки 16 и 8 dp с векторами движения. */
  moshStart(st) {
    st = st || this.stages.filter(s => s.mosh && s.vis && s.built)[(this.moshN || 0) % Math.max(1, this.stages.filter(s => s.mosh && s.vis && s.built).length)];
    if (!st || !st.mosh || st.m) return;
    const W = st.W, H = st.H, cv = st.mosh, xml = new XMLSerializer().serializeToString(st.svg), img = new Image();
    st.m = { loading: 1 };
    img.onerror = () => { st.m = null; };
    img.onload = () => {
      const c = cv.getContext('2d'),
        R = this.rng(this.props.seed + '/' + (this.moshN = (this.moshN || 0) + 1), 'mosh'),
        nr = Math.floor(H / 16);
      const rows = Math.min(nr, 9 + Math.floor(R() * 10)),
        y0 = Math.floor(R() * (nr - rows + 1)) * 16,
        runs = [],
        tints = ['#ff2e93', '#b6ff1a', '#19e3ff'];
      const gvx = Math.round((R() - .5) * 8), gvy = 3 + Math.floor(R() * 4);
      for (let r = 0; r < rows; r++) for (let x = 0; x < W;) {
        const bw = Math.min(W - x, 16 * (1 + Math.floor(R() * 4))),
          edge = r === 0 || r === rows - 1,
          hs = R() < .3 ? [0, 8] : [0];
        hs.forEach(o => runs.push({
          x,
          y: y0 + r * 16 + o,
          w: bw,
          h: hs.length > 1 ? 8 : 16,
          vx: gvx + Math.round((R() - .5) * 6),
          vy: Math.max(1, gvy + Math.round((R() - .5) * 5)),
          st: R() * .6,
          gone: edge && R() < .5,
          tint: R() < .1 ? tints[Math.floor(R() * 3)] : null
        }));
        x += bw;
      }
      c.setTransform(1, 0, 0, 1, 0, 0);
      c.globalAlpha = 1;
      c.globalCompositeOperation = 'source-over';
      c.clearRect(0, 0, W * 2, H * 2);
      c.fillStyle = getComputedStyle(st.host).backgroundColor || MutantEngine.C.bg;
      c.fillRect(0, y0 * 2, W * 2, rows * 32);
      c.drawImage(img, 0, 0, W * 2, H * 2);
      c.clearRect(0, 0, W * 2, y0 * 2);
      c.clearRect(0, (y0 + rows * 16) * 2, W * 2, H * 2);
      runs.forEach(b => { if (b.gone) c.clearRect(b.x * 2, b.y * 2, b.w * 2, b.h * 2); });
      const rf = st.ref || (st.ref = document.createElement('canvas'));
      rf.width = W * 2;
      rf.height = H * 2;
      rf.getContext('2d').drawImage(cv, 0, 0);
      st.m = { t0: this.time, y0, rows, runs, dur: 900 + R() * 500, n: -1 };
      const fp = st.host.dataset.muMoshP;
      if (fp !== undefined) {
        const m = st.m, steps = Math.floor(+fp * m.dur / 125);
        for (let k = 0; k <= steps && st.m === m; k++) {
          m.t0 = this.time - k * 125;
          this.moshStep(st);
        }
        if (st.m === m) m.frozen = true;
      }
    };
    img.src = 'data:image/svg+xml;charset=utf-8,' + encodeURIComponent(xml);
  }

  /** Шаг потери кадра раз в 125 мс: блоки тянут картинку, затем полоса восстанавливается сверху вниз. */
  moshStep(st) {
    const m = st.m;
    if (!m || m.loading || m.frozen) return;
    const cv = st.mosh,
      c = cv.getContext('2d'),
      el = this.time - m.t0,
      n = Math.floor(el / 125),
      W2 = st.W * 2,
      H2 = st.H * 2;
    if (n === m.n) return;
    m.n = n;
    if (el < m.dur) {
      const u = el / m.dur;
      m.runs.forEach(b => {
        if (b.gone || u < b.st) return;
        c.drawImage(cv, b.x * 2, b.y * 2, b.w * 2, b.h * 2, (b.x + b.vx) * 2, (b.y + b.vy) * 2, b.w * 2, b.h * 2);
        if (b.tint && !b.td) {
          b.td = 1;
          c.globalAlpha = .3;
          c.globalCompositeOperation = 'screen';
          c.fillStyle = b.tint;
          c.fillRect(b.x * 2, b.y * 2, b.w * 2, b.h * 2);
          c.globalAlpha = 1;
          c.globalCompositeOperation = 'source-over';
        }
      });
      st.items.forEach(it => {
        const q = it.el;
        if (q.tz[2] <= 0) return;
        const X = (q.ox + q.tz[0] - 3) * 2,
          Y = (q.oy + q.tz[1] - 3) * 2,
          Wd = (q.tz[2] + 6) * 2,
          Hd = (q.tz[3] + 6) * 2;
        if (Y + Hd <= m.y0 * 2 || Y >= (m.y0 + m.rows * 16) * 2) return;
        c.clearRect(X, Y, Wd, Hd);
        c.drawImage(st.ref, X, Y, Wd, Hd, X, Y, Wd, Hd);
      });
    } else {
      const k = Math.ceil(m.rows * Math.min(1, (el - m.dur + 1) / 375));
      c.clearRect(0, 0, W2, (m.y0 + k * 16) * 2);
      if (k >= m.rows) {
        c.clearRect(0, 0, W2, H2);
        st.m = null;
      }
    }
  }

  /** Запускает оплывание индикатора (галочка, точка): on — натекает сверху, иначе стекает вниз. */
  melt(x, on) {
    this.fxDom = (this.fxDom || []).filter(f => f.el !== x);
    x.style.display = '';
    if (this.paused) {
      x.style.display = on ? '' : 'none';
      return;
    }
    this.fxDom.push({ el: x, on, t0: this.time });
  }

  /** Шаг оплывания индикаторов, 12 кадров в секунду по часам движка. */
  meltStep() {
    const now = this.fxTime();
    this.fxDom = this.fxDom.filter(f => {
      const p = Math.min(1, Math.floor((now - f.t0) / 83.33) * 83.33 / 330), q = f.on ? 1 - p : p, s = f.el.style;
      if (p >= 1) {
        s.transform = '';
        s.clipPath = '';
        s.transformOrigin = '';
        if (!f.on) s.display = 'none';
        return false;
      }
      s.transformOrigin = '50% 0';
      s.transform = `translateY(${((f.on ? -q : q) * 7).toFixed(1)}px) scaleY(${(1 + q * .9).toFixed(2)})`;
      s.clipPath = f.on ? `inset(0 0 ${(q * 100).toFixed(0)}% 0)` : `inset(${(q * 85).toFixed(0)}% 0 0 0)`;
      return true;
    });
  }

  /** Переход «веном» для плашки выбора: из старой плашки к новому месту выстреливают острые изогнутые щупальца, цепляются, по ним перетекает масса — старая плашка убывает, новая растёт из точек захвата; в конце щупальца втягиваются. p = 0…1. */
  venom(it, fx, p) {
    const el = it.el, ink = MutantEngine.C.ink, f = v => v.toFixed(1), cx = el.ox + el.w / 2, cy = el.oy + el.h / 2;
    let V = it.V;
    if (p >= 1) {
      if (V) V.remove();
      if (it.V2) it.V2.remove();
      it.V = it.V2 = null;
      it.G.removeAttribute('transform');
      it.G.style.opacity = '';
      return;
    }
    // Два слоя: чёрный силуэт под плашкой и заливка поверх неё, чтобы подтёки сливались с новой плашкой без обводки на стыке.
    if (!V) {
      V = it.V = document.createElementNS('http://www.w3.org/2000/svg', 'g');
      it.G.parentNode.insertBefore(V, it.G);
      it.V2 = document.createElementNS('http://www.w3.org/2000/svg', 'g');
      it.G.parentNode.insertBefore(it.V2, it.G.nextSibling);
    }
    // Геометрия перехода — в чистом генераторе (venomShapes); здесь только слои SVG, фильтр и показ самой плашки.
    const { SF, S, full, vert } = this.venomShapes(el, it.key, fx, p);
    // Сглаживание стыков: размытие и порог по альфе («метаболы») скругляют все вогнутые углы там, где жирное щупальце
    // входит в массы. В Compose это RenderEffect: blur + ColorMatrix по альфе, либо шейдер.
    const goo = it.id + 'goo', X0 = Math.min(el.ox, el.ox + fx.dx) - 30, Y0 = Math.min(el.oy, el.oy + fx.dy) - 30, X1 = Math.max(el.ox, el.ox + fx.dx) + el.w + 30, Y1 = Math.max(el.oy, el.oy + fx.dy) + el.h + 30;
    const flt = `<filter id="${goo}" filterUnits="userSpaceOnUse" x="${f(X0)}" y="${f(Y0)}" width="${f(X1 - X0)}" height="${f(Y1 - Y0)}" color-interpolation-filters="sRGB"><feGaussianBlur stdDeviation="3.2"/><feColorMatrix values="1 0 0 0 0 0 1 0 0 0 0 0 1 0 0 0 0 0 18 -8.2"/></filter>`;
    V.innerHTML = flt + `<g fill="${ink}" stroke="${ink}" stroke-width="${el.ow * 2}" stroke-linejoin="round"><g filter="url(#${goo})">${SF}</g>${S}</g>`;
    it.V2.innerHTML = `<g fill="${el.fill}"><g filter="url(#${goo})">${SF}</g>${S}</g>`;
    // Новая плашка растёт одновременно: от ближнего края, куда пришёл жирный канал, в ширину и в высоту.
    // Саму плашку показываем, когда слизь заняла всё место; до этого её рисует слой перетекания.
    it.G.style.opacity = full ? '' : 0;
    // Пружина: заполнившись, новая плашка проседает вдоль оси движения и возвращается с затуханием.
    const sv = (p - .8) / .2, amp = full ? .09 * Math.sin(sv * Math.PI * 2.5) * (1 - sv) : 0, mxp = f(cx), myp = f(cy);
    if (Math.abs(amp) < .004) it.G.removeAttribute('transform');
    else it.G.setAttribute('transform', `translate(${mxp} ${myp}) scale(${(vert ? 1 + amp * .5 : 1 - amp).toFixed(3)} ${(vert ? 1 - amp : 1 + amp * .5).toFixed(3)}) translate(${-mxp} ${-myp})`);
  }

  /** Группа «один из нескольких» (Segmented button, Menu, Drawer): чистая плашка data-pick-pill перетекает к выбранному узлу data-pick. */
  bindPicks() {
    this.root.querySelectorAll('[data-mu-pick]').forEach(box => {
      if (box._muP) return;
      box._muP = 1;
      const pill = box.querySelector('[data-pick-pill]'), opts = [...box.querySelectorAll('[data-pick]')];
      opts.forEach(o => {
        o.style.cursor = 'pointer';
        o.addEventListener('click', ev => {
          if (!pill || o.dataset.on === '1') return;
          const ox = pill.offsetLeft, oy = pill.offsetTop;
          opts.forEach(x => {
            x.dataset.on = x === o ? '1' : '0';
          });
          setTimeout(() => {
            opts.forEach(x => {
              x.style.color = x.dataset.on === '1' ? '#05060c' : '';
            });
          }, this.paused ? 0 : 340);
          pill.style.left = o.offsetLeft + 'px';
          pill.style.top = o.offsetTop + 'px';
          pill.style.width = o.offsetWidth + 'px';
          pill.style.height = o.offsetHeight + 'px';
          if (!this.paused) {
            // Точка клика относительно центра выбранного пункта, dp: туда пойдёт основная масса.
            const rc = o.getBoundingClientRect(), zs = rc.width / o.offsetWidth || 1, pt = ev && (ev.clientX || ev.clientY);
            pill._muFx = { dx: ox - o.offsetLeft, dy: oy - o.offsetTop, venom: 1, px: pt ? (ev.clientX - rc.left) / zs - o.offsetWidth / 2 : 0, py: pt ? (ev.clientY - rc.top) / zs - o.offsetHeight / 2 : 0 };
          }
          const st = this.stages.find(s => s.host === box.closest('[data-mu-stage]'));
          if (st) {
            st.built = false;
            st.sync = true;
          }
        });
      });
    });
  }

  /** Перетаскивание по горизонтали (Slider): узел data-mu-drag ездит внутри родителя, тело кипит, пока его держат. Событие mu-drag несёт долю 0…1. */
  bindDrags() {
    this.root.querySelectorAll('[data-mu-drag]').forEach(n => {
      if (n._muD) return;
      n._muD = 1;
      n.style.cursor = 'grab';
      n.style.touchAction = 'none';
      let x0 = 0, l0 = 0, sc = 1;
      const move = e => {
        const max = n.parentNode.clientWidth - n.offsetWidth, l = Math.max(0, Math.min(max, l0 + (e.clientX - x0) / sc)), it = n._muIt;
        n.style.left = l + 'px';
        if (it && it.G) it.G.setAttribute('transform', `translate(${(l - it.dragL).toFixed(1)} 0)`);
        n.dispatchEvent(new CustomEvent('mu-drag', { bubbles: true, detail: max ? l / max : 0 }));
      };
      const up = () => {
        window.removeEventListener('pointermove', move);
        window.removeEventListener('pointerup', up);
        if (n._muIt) n._muIt.pressed = false;
        n._muPulse = 1;
        const st = this.stages.find(s => s.host === n.closest('[data-mu-stage]'));
        if (st) {
          st.built = false;
          st.sync = true;
        }
      };
      n.addEventListener('pointerdown', e => {
        const it = n._muIt;
        x0 = e.clientX;
        l0 = n.offsetLeft;
        sc = n.getBoundingClientRect().width / n.offsetWidth || 1;
        if (it) {
          it.pressed = true;
          it.dragL = l0;
        }
        window.addEventListener('pointermove', move);
        window.addEventListener('pointerup', up);
        e.preventDefault();
      });
    });
  }

  /** Подключает переключение по клику к узлам data-mu-toggle. */
  bindToggles() {
    this.root.querySelectorAll('[data-mu-toggle]').forEach(n => {
      if (n._muT) return;
      n._muT = 1;
      n.style.cursor = 'pointer';
      n.style.userSelect = 'none';
      n.addEventListener('click', () => this.toggle(n));
    });
  }

  /** Переключает чип, чекбокс, радио или переключатель и запускает переходы pour, melt и slide. На паузе состояние меняется сразу. */
  toggle(n, force) {
    const kind = n.dataset.muToggle, c = n.matches('[data-mu]') ? n : n.querySelector('[data-mu]');
    if (!c) return;
    const on = force !== undefined ? force : n.dataset.on !== '1';
    if (kind === 'radio') {
      if (!on && force === undefined) return;
      if (on) this.root.querySelectorAll('[data-mu-toggle="radio"]').forEach(o => {
        if (o !== n && o.dataset.group === n.dataset.group && o.dataset.on === '1') this.toggle(o, false);
      });
    }
    n.dataset.on = on ? '1' : '0';
    const prev = c._muIt && !c._muIt.stat ? c._muIt.el.fill : null;
    const set = (k, v) => v === null ? c.removeAttribute(k) : c.setAttribute(k, v);
    if (kind === 'chip') {
      set('data-fill', on ? 'green' : null);
      set('data-slip', on ? null : '2');
      clearTimeout(c._muTc);
      c._muTc = setTimeout(() => { c.style.color = on ? '#05060c' : 'var(--mu-text)'; }, this.paused ? 0 : 200);
    } else if (kind === 'check') set('data-fill', on ? 'green' : null);
    else if (kind === 'switch') {
      set('data-fill', on ? null : 'surf2');
      set('data-over', on ? null : '6');
      const th = c.querySelector('[data-mu="thumb"]');
      if (th) {
        th.style.left = on ? '27px' : '3px';
        if (!this.paused) th._muFx = { dx: on ? -24 : 24 };
      }
    }
    c.querySelectorAll('[data-on-only]').forEach(x => this.melt(x, on));
    if (!this.paused) c._muFx = { from: prev };
    const st = this.stages.find(s => s.host === c.closest('[data-mu-stage]'));
    if (st) {
      st.built = false;
      st.sync = true;
    }
  }

  /** Находит сцены data-mu-stage и создаёт в каждой SVG-слой под содержимым. */
  mount() {
    this.items = [];
    this._N = {};
    this.stages = [...this.root.querySelectorAll('[data-mu-stage]')].map((host, si) => {
      if (getComputedStyle(host).position === 'static') host.style.position = 'relative';
      host.style.isolation = 'isolate';
      let lay = host.querySelector(':scope > [data-mu-layer]');
      if (!lay) {
        lay = document.createElement('div');
        lay.setAttribute('data-mu-layer', '');
        host.insertBefore(lay, host.firstChild);
      }
      lay.style.cssText = 'position:absolute;left:0;top:0;width:0;height:0;z-index:-1;pointer-events:none';
      lay.innerHTML = '<svg xmlns="http://www.w3.org/2000/svg" width="10" height="10" style="position:absolute;left:0;top:0;overflow:visible"><defs></defs><g></g></svg>';
      return {
        host,
        si,
        key: host.dataset.muStage || 's' + si,
        pre: 'm' + this.uid + 's' + si,
        lay,
        svg: lay.firstChild,
        defs: lay.querySelector('defs'),
        art: lay.querySelector('g'),
        items: [],
        anims: [],
        coc: [],
        vis: true,
        built: false,
        mosh: null
      };
    });
  }

  /** Часы переходов: время движка, а на паузе — «бесконечность», чтобы переход завершился сразу. */
  fxTime() { return this.paused ? 1e12 : this.time; }

  /** Один проход: двигает часы движка, считает нагрев по времени события, перерисовывает видимые экземпляры в пределах бюджета кадра. performance.now здесь только для планирования, на картинку не влияет. */
  frame() {
    const P = this.props, now = performance.now();
    if (now < (this.nextAt || 0)) return;
    const dt = this.last ? Math.min(1000, now - this.last) : 16;
    this.last = now;
    if (!this.paused) this.time += dt * (+P.speed || 1);
    const t = this.time,
      fps = +P.fps || 30,
      step = 1000 / fps,
      ts = Math.floor(t / step) * step,
      tsH = Math.floor(t / (step / 1.5)) * (step / 1.5),
      fi = Math.floor(t / 125),
      t0 = performance.now();
    const pend = this.stages.find(s => !s.built && s.vis) || this.stages.find(s => !s.built);
    if (pend) {
      this.buildStage(pend);
      if (pend.sync && pend.built) {
        pend.sync = false;
        pend.items.forEach(it => {
          if (it.H) {
            it.lk = it.el.still ? 'still' : Math.round(ts);
            this.drawItem(it, ts, tsH);
          }
        });
      }
    }
    this.items.forEach(it => {
      if (!it.H) return;
      const on = it.pressed || it.forced;
      if (it.hT === undefined) {
        it.hOn = false;
        it.h0 = it.heat;
        it.hT = t;
      }
      if (on !== it.hOn) {
        it.hOn = on;
        it.h0 = it.heat;
        it.hT = t;
      }
      if (on || it.heat > 0) it.heat = on ? Math.min(1, it.h0 + (t - it.hT) / 320) : Math.max(0, it.h0 - (t - it.hT) / 480);
    });
    for (const it of this.items) if (it.H && it.st.vis && !it.forced && (it.heat > 0 || it.pressed || it.fx)) {
      const k = it.fx ? 'x' + Math.floor((this.fxTime() - it.fx.t0) / (it.fx.venom ? 41.67 : 83.33)) : 'h' + Math.round(tsH) + ':' + it.heat.toFixed(2);
      if (it.lk !== k) {
        it.lk = k;
        this.drawItem(it, ts, tsH);
      }
    }
    const n = this.items.length;
    let i = n ? this.rr % n : 0;
    for (let c = 0; c < n; c++, i = (i + 1) % n) {
      const it = this.items[i];
      if (!it.H || !it.st.vis || (!it.forced && (it.heat > 0 || it.pressed || it.fx))) continue;
      const k = it.forced ? 'f' + Math.round(tsH) : it.el.still ? 'still' : Math.round(ts);
      if (it.lk === k) continue;
      it.lk = k;
      this.drawItem(it, ts, tsH);
      if (performance.now() - t0 > 10) {
        i = (i + 1) % n;
        break;
      }
    }
    this.rr = i;
    if (this.fxDom && this.fxDom.length) this.meltStep();
    this.stages.forEach(st => {
      if (!st.built || !st.vis) return;
      this.stageStep(st, fi, t);
      if (st.mosh) this.moshStep(st);
    });
    this.stages.forEach(st => {
      if (st.built && st.vis && st.mosh && !st.moshFix && st.host.dataset.muMoshP !== undefined && st.items.every(it => !it.H || it.lk !== undefined)) {
        st.moshFix = 1;
        this.moshStart(st);
      }
    });
    if (P.keyframeLoss && !this.paused) {
      const win = Math.floor(t / 9000);
      if (win !== this._mw && t - win * 9000 > 3500) {
        this._mw = win;
        this.moshStart();
      }
    }
    this.nextAt = performance.now() + (performance.now() - now) * .8;
  }

  /** Запускает цикл кадров и слежение за видимостью сцен. */
  start() {
    this.mount();
    this.bindToggles();
    this.bindPicks();
    this.bindDrags();
    if (window.IntersectionObserver) {
      this.io = new IntersectionObserver(es => es.forEach(e => {
        const st = this.stages.find(s => s.host === e.target);
        if (st) st.vis = e.isIntersecting;
      }), { rootMargin: '150px' });
      this.stages.forEach(s => this.io.observe(s.host));
    }
    const tick = () => {
      try { this.frame(); } catch (e) {
        if (!this._err) {
          this._err = e;
          console.error(e);
        }
      }
    };
    const loop = () => {
      this.raf = requestAnimationFrame(loop);
      tick();
    };
    this.raf = requestAnimationFrame(loop);
    this.iv = setInterval(() => { if (performance.now() - (this.last || 0) > 120) tick(); }, 60);
  }

  /** Останавливает цикл. */
  stop() {
    cancelAnimationFrame(this.raf);
    clearInterval(this.iv);
    if (this.io) this.io.disconnect();
  }

  /** Пауза — режим reduce motion: часы движка стоят. */
  setPaused(p) { this.paused = !!p; }

  /** Меняет параметры; при смене seed, плотности, шума или темы сцены пересобираются. */
  setProps(p) {
    const o = this.props,
      n = Object.assign({}, o, p),
      re = n.seed !== o.seed || n.density !== o.density || n.rough !== o.rough || n.theme !== o.theme;
    this.props = n;
    if (n.theme !== o.theme) Object.assign(MutantEngine.C, MutantEngine.TH[n.theme] || MutantEngine.TH.efir);
    if (re) {
      this._N = {};
      this.stages.forEach(s => { s.built = false; });
    } else if (n.boil !== o.boil) this.items.forEach(it => { it.lk = undefined; });
  }
}
MutantEngine.UI = {
  efir: {
    page: '#0a0c18',
    text: '#e9f2e4',
    text2: '#aab4cf',
    dis: '#8a90a6',
    acc: '#b6ff1a',
    osd: '#ffb000',
    err: '#ff6a61',
    onblue: '#e9f2e4',
    onblueAcc: '#ffe08a'
  },
  kopiya: {
    page: '#cfc8b6',
    text: '#14110f',
    text2: '#5b5446',
    dis: '#6f6858',
    acc: '#2b3fbf',
    osd: '#a8380b',
    err: '#b3261e',
    onblue: '#f6f2e6',
    onblueAcc: '#ffd84a'
  }
};
MutantEngine.tokens = function () {
  const T = MutantEngine.TH, U = MutantEngine.UI, K = MutantEngine.K;
  const L = h => {
    const n = parseInt(h.slice(1), 16),
      f = v => {
      v /= 255;
      return v <= .03928 ? v / 12.92 : Math.pow((v + .055) / 1.055, 2.4);
    };
    return .2126 * f(n >> 16 & 255) + .7152 * f(n >> 8 & 255) + .0722 * f(n & 255);
  };
  const cr = (a, b) => {
    const x = L(a), y = L(b);
    return Math.round((Math.max(x, y) + .05) / (Math.min(x, y) + .05) * 10) / 10;
  };
  const color = {}, contrast = {}, afterTape = {}, noiseRow = {};
  // Сплошные слои Ленты: [цвет, непрозрачность]. Строка развёртки и полоса трекинга накрывают текст и фон под ним одинаково.
  // noise — штрих шума головки высотой 1 dp на один тик; зерно (точки 1 dp, меньше 1 % площади) в расчёт не входит.
  const TAPE = {
    efir: { layers: [['#000000', .075], ['#e9f2e4', .07]], noise: ['#ffffff', .7] },
    kopiya: { layers: [['#14110f', .05]], noise: ['#6b5f48', .2] }
  };
  const rgb = h => { const n = parseInt(h.slice(1), 16); return [n >> 16 & 255, n >> 8 & 255, n & 255]; };
  const hex = v => '#' + v.map(x => Math.round(x).toString(16).padStart(2, '0')).join('');
  const lay = (v, l) => { const t = rgb(l[0]); return v.map((x, i) => x + (t[i] - x) * l[1]); };
  // Худший тик: каждый слой либо накрывает текст вместе с фоном, либо нет; берём минимум контраста по всем сочетаниям.
  const worst = (fg, bg, layers) => {
    let m = 99;
    for (let mask = 0; mask < Math.pow(2, layers.length); mask++) {
      let f = rgb(fg), b = rgb(bg);
      layers.forEach((l, i) => { if (mask >> i & 1) { f = lay(f, l); b = lay(b, l); } });
      m = Math.min(m, cr(hex(f), hex(b)));
    }
    return m;
  };
  Object.keys(T).forEach(th => {
    const c = T[th], u = U[th];
    color[th] = {
      primary: c.green,
      'on-primary': '#05060c',
      magenta: c.mag,
      cyan: c.cyan,
      'no-signal': c.blue,
      amber: c.amber,
      phosphor: c.white,
      danger: c.red,
      background: c.bg,
      surface: c.surf,
      'surface-variant': c.surf2,
      ink: c.ink,
      page: u.page,
      text: u.text,
      'text-secondary': u.text2,
      'text-disabled': u.dis,
      'text-accent': u.acc,
      'text-osd': u.osd,
      'text-error': u.err,
      'on-no-signal': u.onblue,
      'on-no-signal-accent': u.onblueAcc,
      focus: c.focus,
      misregister: c.mis,
      'shadow-deep': c.deep,
      'highlight-on-dark': c.hlDark,
      dropout: c.drop
    };
    const pairs = {
      'text / background': [u.text, c.bg],
      'text / surface': [u.text, c.surf],
      'text / surface-variant': [u.text, c.surf2],
      'text-secondary / background': [u.text2, c.bg],
      'text-secondary / surface': [u.text2, c.surf],
      'text-accent / background': [u.acc, c.bg],
      'text-osd / background': [u.osd, c.bg],
      'text-osd / surface': [u.osd, c.surf],
      'text-error / background': [u.err, c.bg],
      'on-primary / primary': ['#05060c', c.green],
      'on-primary / amber': ['#05060c', c.amber],
      'on-no-signal / no-signal': [u.onblue, c.blue],
      'on-no-signal-accent / no-signal': [u.onblueAcc, c.blue],
      'text-disabled / background': [u.dis, c.bg]
    };
    contrast[th] = {};
    afterTape[th] = {};
    Object.keys(pairs).forEach(k => {
      contrast[th][k] = cr(pairs[k][0], pairs[k][1]);
      afterTape[th][k] = worst(pairs[k][0], pairs[k][1], TAPE[th].layers);
    });
    noiseRow[th] = cr(u.text, hex(lay(rgb(c.bg), TAPE[th].noise)));
  });
  const profile = {};
  Object.keys(K).forEach(k => {
    const p = K[k];
    profile[k] = {
      radius: p.r >= 99 ? 'full' : p.r,
      overhang: p.over,
      'drip-share': p.cf,
      fill: p.fill,
      outline: p.ow || 3,
      marks: p.marks ?? 1,
      mosaic: p.edge ?? 1,
      wobble: p.wob ?? 1,
      'dead-pixels': p.dead || 0
    };
  });
  const G = (level, range, unit, def, engine, status) => ({ level, range, unit, default: def, engine, status });
  return {
    mutant: {
      version: 'ред. 5 · этап 1 · по состоянию движка',
      color,
      contrast,
      'contrast-after-tape': afterTape,
      tape: { efir: { scanline: 'чёрный 7.5 %, 1 dp из 3', band: 'фосфорный белый 7 %, высота 22 dp', grain: 'белый до 16 %, 1 dp', 'noise-row': 'белый до 70 %, 1 dp на один тик; контраст text / background под штрихом ' + noiseRow.efir + ':1' }, kopiya: { scanline: 'нет', band: 'тушь 5 %, высота 22 dp', grain: '#6b5f48 до 20 %, 1 dp', 'noise-row': '#6b5f48 до 20 %, 1 dp на один тик; контраст text / background под штрихом ' + noiseRow.kopiya + ':1' } },
      flash: {
        rule: 'WCAG 2.3.1: не больше трёх вспышек в секунду; вспышка — пара противоположных изменений относительной яркости на 0.1 и больше',
        'boil-over': { 'flashes-per-second': 1.5, 'luminance-delta': Math.round(Math.abs(L('#ffbf33') - L('#ffb000')) * 1000) / 1000, note: 'заливка при полном нагреве чередует янтарь #ffb000 и осветлённый #ffbf33 раз в 333 мс' },
        'running-mosaic': { 'changes-per-second': 12, area: 'до трёх рядов блоков 4 и 8 dp вдоль одной стороны нажатого тела' },
        cocoon: { 'changes-per-second': 8, area: 'около 6 % блоков Кокона за тик' },
        'keyframe-loss': { 'changes-per-second': 8, area: 'полоса 9–18 рядов по 16 dp во всю ширину экрана', note: 'замер на двух демо-экранах, 4 прогона, фаза сбоя без восстановления: за шаг яркость меняется на 0.1 и больше не более чем на 2.9 % полосы (до 3032 dp²); чаще трёх раз в секунду мигает не больше 42 dp² при пороге площади 21 824 px²' },
        red: 'красный не мигает: ошибка и точка радио статичны, в бегущей мозаике красного нет'
      },
      type: {
        display: {
          family: 'Handjet',
          license: 'OFL',
          use: 'заголовки экранов и диалогов',
          sizes: [22, 24, 26, 28]
        },
        text: {
          family: 'Rubik',
          license: 'OFL',
          use: 'основной текст и подписи',
          sizes: [11, 13, 14, 15, 16],
          weights: [400, 500]
        },
        osd: {
          family: 'Handjet',
          license: 'OFL',
          use: 'экранные надписи, счётчики, бейджи',
          sizes: [14, 16, 18, 20],
          'letter-spacing': 1
        }
      },
      grid: {
        unit: 4,
        'touch-target': 48,
        'screen-margin': 16,
        'mosaic-blocks': [4, 8, 16],
        'clean-zone-padding': { horizontal: 6, vertical: 2, 'mosaic-extra': 2 },
        'overhang-max': 24,
        'overhang-list': 8,
        'field-step': { small: .9, large: 1.5 }
      },
      genome: {
        generation: G('кассета', [0, 4], 'поколение', null, 'нет; износ экземпляра задаёт calm 0.2–1.7', 'не реализован'),
        tracking: G('кассета', [0, 1], '', 1, 'полоса 22 dp, проход 2100 мс раз в 7000 мс', 'фиксирован'),
        tick: G('кассета', [8, 24], 'кадров/с', 8, 'fps; нажатый элемент идёт в 1.5 раза чаще', 'реализован'),
        boil: G('кассета', [0, 1.5], '', .8, 'boil: покадровое дрожание контура и линий', 'реализован, в брифе гена нет'),
        melt: G('компонент', [0, 1], '', 1, 'overhang × drip-share из профиля типа', 'реализован через профиль'),
        blot: G('компонент', [0.6, 1.6], '', 1, 'density: линии потока внутри тела', 'реализован как линии потока, не пятна'),
        slip: G('компонент', [0, 2], 'dp', 0, 'сдвиг заливки от контура: чип 2, тема «Копия» 1.5', 'только сдвиг заливки; срезов нет'),
        split: G('компонент', [0, 3], 'dp', 0, 'нет', 'не используется: нажатие заменено кипением'),
        crush: G('компонент', [0, 1], '', 1, 'мозаика края 4 и 8 dp в 3 ряда, контур под ней ломается по блокам; Кокон 16 и 8 dp', 'включён или выключен по профилю'),
        noise: G('компонент', [0, 1], '', .35, 'rough: рваность кромки и крап', 'реализован'),
        ghost: G('компонент', [0, 1], '', 0, 'нет; эхо бегунка переключателя нарисовано отдельно', 'не реализован'),
        outline: G('компонент', [2, 3], 'dp', 3, 'outline из профиля, ±12 % × boil по кадрам', 'реализован'),
        hueDrift: G('компонент', [-8, 8], '°', 0, 'нет', 'не реализован')
      },
      profile,
      motion: {
        tick: { ms: 125, note: '8 кадров/с; в брифе 83 мс' },
        'tick-hot': { ms: 83, note: 'нажатый элемент и переходы' },
        'boil-over-in': {
          ms: 320,
          event: 'press',
          note: 'нагрев: дрожание ×3, пузыри сверху, цвет в янтарь, мозаика бегает, капли стоят'
        },
        'boil-over-out': { ms: 480, event: 'release' },
        creep: { ms: [8000, 14000], note: 'полный цикл подтёка' },
        hang: { ms: [2200, 4400], note: 'капля висит' },
        pinch: { ms: 1000, curve: 'ease-in²' },
        drop: { gravity: 0.00055, unit: 'dp/мс²', note: 'капля гаснет через 10–18 dp' },
        recoil: { ms: 1000, curve: 'затухающая пружина 160 мс' },
        flow: { ms: [5000, 9500], note: 'цикл линии потока' },
        wave: { ms: [6000, 10000], note: 'период волны верхней кромки' },
        tracking: { ms: 2100, every: 7000 },
        seek: { ms: 900, event: 'loading', note: 'цикл полос перемотки' },
        pour: { ms: [300, 620], event: 'toggle', note: 'новый цвет стекает сверху; 260 + 5 × высота' },
        'melt-in': { ms: 330, event: 'toggle on', note: 'индикатор натекает сверху' },
        'melt-out': { ms: 330, event: 'toggle off', note: 'индикатор оплывает вниз' },
        slide: { ms: 330, event: 'switch', curve: 'ease-out²', note: 'бегунок растягивается на 40 %' },
        'keyframe-loss': { ms: [900, 1400], recover: 375, every: 9000, note: 'полоса 9–18 рядов блоков 16 и 8 dp' },
        clean: { ms: null, note: 'успех показан статичным чистым кадром; возврата через 600 мс нет' }
      }
    }
  };
};
// Компоненты кита: из каких тел собран каждый и чем отличается от профиля типа. Попадает в токены как mutant/component.
MutantEngine.COMPONENTS = {
  "Button": {
    "tier": "MVP",
    "body": [
      "btn"
    ],
    "variants": "filled; outlined: fill=bg, stroke=acc, shadow=mis, over=14; text: без тела, подпись над разделителем",
    "states": "rest, pressed (boil), loading (seek), success (clean), disabled"
  },
  "IconButton": {
    "tier": "MVP",
    "body": [
      "icon"
    ],
    "variants": "48×48",
    "states": "rest, pressed, disabled"
  },
  "FAB": {
    "tier": "MVP",
    "body": [
      "fab"
    ],
    "variants": "56×56; extended 136×56",
    "states": "rest, pressed"
  },
  "Chip": {
    "tier": "MVP",
    "body": [
      "chip"
    ],
    "variants": "unselected: slip=2; selected: fill=acc",
    "states": "rest, pressed, selected (pour), disabled"
  },
  "Checkbox": {
    "tier": "MVP",
    "body": [
      "box"
    ],
    "variants": "22×22; checked: fill=acc",
    "states": "unchecked, checked (pour + melt), disabled"
  },
  "RadioButton": {
    "tier": "MVP",
    "body": [
      "dot"
    ],
    "variants": "22×22",
    "states": "unselected, selected (melt), disabled"
  },
  "Switch": {
    "tier": "MVP",
    "body": [
      "track",
      "thumb"
    ],
    "variants": "52×28, бегунок 22; off: fill=surf2, over=6",
    "states": "off, on (pour + slide), disabled"
  },
  "TextField": {
    "tier": "MVP",
    "body": [
      "field"
    ],
    "variants": "высота 52",
    "states": "rest, focused (calm=.3 + скобки), error (corrupt), disabled"
  },
  "Card": {
    "tier": "MVP",
    "body": [
      "card"
    ],
    "variants": "битые пиксели в углу",
    "states": "rest, disabled"
  },
  "Skeleton (Кокон)": {
    "tier": "MVP",
    "body": [],
    "variants": "мозаика 16 и 8 dp по форме будущего компонента",
    "states": "loading"
  },
  "TopAppBar": {
    "tier": "MVP",
    "body": [],
    "variants": "высота 56, разделитель трекинга снизу",
    "states": "rest"
  },
  "Tabs": {
    "tier": "MVP",
    "body": [],
    "variants": "индикатор 4 dp с эхом",
    "states": "rest"
  },
  "NavigationBar": {
    "tier": "MVP",
    "body": [
      "bar",
      "pill"
    ],
    "variants": "высота 62–68; активный пункт — pill 56×30",
    "states": "rest"
  },
  "ListItem": {
    "tier": "MVP",
    "body": [
      "icon"
    ],
    "variants": "высота 64, плитка 40 (over=6)",
    "states": "rest"
  },
  "Divider": {
    "tier": "MVP",
    "body": [],
    "variants": "штрихи трекинга 2 dp",
    "states": "rest"
  },
  "Badge": {
    "tier": "MVP",
    "body": [],
    "variants": "чистая плашка osd",
    "states": "rest"
  },
  "Snackbar": {
    "tier": "MVP",
    "body": [
      "bar"
    ],
    "variants": "fill=blue, shadow, r=6, высота 48",
    "states": "rest"
  },
  "LinearProgress": {
    "tier": "MVP",
    "body": [],
    "variants": "сегменты 6+2 dp; неопределённый — бегущая полоса",
    "states": "determinate, indeterminate"
  },
  "CircularProgress": {
    "tier": "MVP",
    "body": [],
    "variants": "катушка 36 dp, шаг 30° на тик",
    "states": "indeterminate"
  },
  "Dialog": {
    "tier": "MVP",
    "body": [
      "bar",
      "btn"
    ],
    "variants": "окно — чистый кадр (clean), r=12; фон на паузе с полосой",
    "states": "open"
  },
  "EmptyState": {
    "tier": "MVP",
    "body": [
      "blob",
      "bar",
      "pill",
      "dot",
      "btn"
    ],
    "variants": "6 иллюстраций из тел",
    "states": "rest"
  },
  "Slider": {
    "tier": "v1",
    "body": [
      "fab"
    ],
    "variants": "канал 14 dp, заливка 8 dp, ручка 34 (fill=osd, r=17, over=12) со значением внутри",
    "states": "rest, dragging (boil), disabled"
  },
  "SegmentedButton": {
    "tier": "v1",
    "body": [
      "btn",
      "pill"
    ],
    "variants": "тело fill=surf2, over=14; окно выбора pill calm=.2, r=9",
    "states": "rest, selection (venom), disabled"
  },
  "Tooltip": {
    "tier": "v1",
    "body": [
      "blob"
    ],
    "variants": "fill=blue или osd, r=6, over=20, cf=0; высота 30 или 44",
    "states": "shown"
  },
  "Menu": {
    "tier": "v1",
    "body": [
      "btn",
      "pill"
    ],
    "variants": "тело fill=surf, r=8, over=18; пункт 36; окно выбора pill r=6",
    "states": "open, selection (venom)"
  },
  "NavigationDrawer": {
    "tier": "v1",
    "body": [
      "bar",
      "pill"
    ],
    "variants": "тело fill=surf, r=0, slip=3, shadow; пункт 42; окно выбора pill r=8",
    "states": "open, selection (venom)"
  },
  "BottomSheet": {
    "tier": "v1",
    "body": [
      "blob",
      "btn"
    ],
    "variants": "fill=surf, r=22, over=0, wob=1.6; над листом разделитель",
    "states": "open"
  },
  "PullToRefresh": {
    "tier": "v1",
    "body": [
      "btn",
      "pill"
    ],
    "variants": "язык: r=5–7, over=14–24, cf=0–1; готово — pill clean",
    "states": "pulling, refreshing (boil), done (clean)"
  },
  "SearchBar": {
    "tier": "later",
    "body": [
      "field",
      "chip",
      "icon"
    ],
    "variants": "TextField в фокусе + чипы подсказок + строки результатов",
    "states": "focused"
  },
  "DatePicker": {
    "tier": "later",
    "body": [
      "bar",
      "pill"
    ],
    "variants": "тело fill=surf, r=12; ячейка 36; выбранный день pill r=18",
    "states": "rest"
  },
  "TimePicker": {
    "tier": "later",
    "body": [
      "field"
    ],
    "variants": "два тела 76×72, over=14; активное fill=acc",
    "states": "rest"
  },
  "DataTable": {
    "tier": "later",
    "body": [
      "box"
    ],
    "variants": "шапка osd 36, строка 48, разделители трекинга",
    "states": "rest, row selected"
  },
  "Carousel": {
    "tier": "later",
    "body": [
      "card",
      "dot",
      "pill"
    ],
    "variants": "карточка 220×120; боковые calm=.5; указатель pill 26×10 среди точек 10",
    "states": "rest"
  }
};
const baseTokens = MutantEngine.tokens;
MutantEngine.tokens = () => {
  const t = baseTokens();
  t.mutant.component = MutantEngine.COMPONENTS;
  t.mutant.motion.venom = { ms: 720, event: 'selection', curve: '24 кадра/с; щупальца 0–0.25, перетекание 0.2–0.8, втягивание до 0.96, пружина 0.8–1', note: 'переход плашки выбора в Segmented button, Menu, NavigationDrawer; в брифе нет' };
  return t;
};
window.MutantEngine = MutantEngine;
}
(function boot(again) {
  if (window.MutantEngine) return;
  if (window.MutantGenerator) return define();
  let tag = document.querySelector('script[data-mu-gen]');
  if (!tag) {
    tag = document.createElement('script');
    tag.setAttribute('data-mu-gen', '');
    tag.src = SRC ? SRC.replace(/mutant-engine\.js.*$/, 'mutant-generator.js?v=9') : './mutant-generator.js?v=9';
    document.head.appendChild(tag);
  }
  // Генератор мог ещё не загрузиться: ждём его событие load; таймер — запасной путь (в фоновой вкладке таймеры могут не сработать).
  if (!again) tag.addEventListener('load', () => boot(true));
  setTimeout(() => boot(true), 30);
})();
})();
