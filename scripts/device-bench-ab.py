#!/usr/bin/env python3
"""B-197: bench-android's scenes on the attached phone, one scene a launch, so that two builds alternate within a round.

    scripts/device-bench-ab.py run <out dir> [--rounds 6] [--seconds 10] [--hint-manager false] \
        --launch a=<package>:<Scene> --launch b=<package>:<Scene> ...
    scripts/device-bench-ab.py read <out dir> [--seconds 10]

`run` launches each `--launch` once a round, the order rotated by one each round (round r starts at the r-th). A launch
force-stops both bench packages, starts the one named with that scene for `seconds`, reads `dumpsys gfxinfo <package>
framestats` once a second while it runs (every view root's last 120 frames, the popup's included), and pulls the
activity's `metrics.csv` (FrameMetrics) and the host's logcat. The battery's temperature and the big cores' (cpu6)
current and maximum frequency are printed before the run and after every launch; a phone over 38 °C at the start is
left to cool first, logged. `--hint-manager false` sets `debug.hwui.use_hint_manager` for the run (B-49's control reads
low with HWUI's ADPF hints on, field-ui B-57) and restores the value it found.

`read` prints, per launch, as B-197's pre-registration and its amendment read them:
- the activity window's main-thread work (FrameMetrics animation + layout/measure + draw) p50, and its missed vsyncs
  (gaps between consecutive VSYNC_TIMESTAMPs over 25 ms), after the scene's first second;
- the reader's control: the activity's frames in `gfxinfo` against FrameMetrics, matched by vsync;
- the popup's root (any root that is not the activity's): its frames a second and its draw (SyncQueued - DrawStart);
- the main thread's work for a frame: the latest SyncQueued among the roots drawn at that vsync minus their shared
  AnimationStart; and beside it the literal sum, the activity's work plus the popup's SyncQueued - AnimationStart.

Every adb call has its own time limit: adb waits for ever at "waiting for device" when the phone drops.
"""
import argparse
import csv
import glob
import os
import re
import subprocess
import sys
import time

ADB = os.environ.get("ADB", os.path.expanduser("~/Library/Android/sdk/platform-tools/adb"))
ACT = "io.github.youndie.mutant.bench.android.BenchActivity"
PACKAGES = ["io.github.youndie.mutant.bench", "io.github.youndie.mutant.bench.main"]
GAP_NS = 25_000_000
SETTLE_NS = 1_000_000_000


def adb(*args, timeout=30):
    try:
        return subprocess.run([ADB, *args], capture_output=True, text=True, timeout=timeout).stdout
    except subprocess.TimeoutExpired:
        return None


def phone():
    t = adb("shell", "dumpsys battery | grep temperature")
    f = adb("shell", "cat /sys/devices/system/cpu/cpu6/cpufreq/scaling_cur_freq "
                     "/sys/devices/system/cpu/cpu6/cpufreq/scaling_max_freq")
    temp = int(re.sub(r"\D", "", t)) / 10 if t and re.search(r"\d", t) else float("nan")
    freq = "/".join(str(round(int(v) / 1000)) for v in f.split()) if f else "?"
    return temp, freq


def run(a):
    os.makedirs(a.out, exist_ok=True)
    launches = []
    for spec in a.launch:
        arm, rest = spec.split("=", 1)
        pkg, scene = rest.split(":", 1)
        launches.append((arm, pkg, scene))
    model = (adb("shell", "getprop ro.product.model") or "?").strip()
    build = (adb("shell", "getprop ro.build.id") or "?").strip()
    print(f"# B-197 one-scene-a-launch bench, {time.strftime('%Y-%m-%dT%H:%M:%S%z')}")
    print(f"# device: {model}, build {build}; rounds {a.rounds} (round 1 not counted), {a.seconds} s a launch, "
          f"first second not counted; launches {', '.join(f'{x}:{s}' for x, _, s in launches)}")
    for arm, pkg, _ in sorted(set((x, p, "") for x, p, _ in launches)):
        ver = adb("shell", f"dumpsys package {pkg} | grep -E 'lastUpdateTime' | head -1") or "?"
        print(f"# arm {arm}: {pkg}, {ver.strip()}; label {a.label.get(arm, '?') if a.label else '?'}")
    hint_before = None
    if a.hint_manager is not None:
        hint_before = (adb("shell", "getprop debug.hwui.use_hint_manager") or "").strip()
        adb("shell", f"setprop debug.hwui.use_hint_manager {a.hint_manager}")
        now = (adb("shell", "getprop debug.hwui.use_hint_manager") or "").strip()
        print(f"# debug.hwui.use_hint_manager: was '{hint_before}', set to '{now}' for the run")
    temp, freq = phone()
    print(f"# phone before: {temp:.1f} °C, big {freq} MHz")
    waited = 0
    while temp > 38.0 and waited < 900:
        time.sleep(60)
        waited += 60
        temp, freq = phone()
        print(f"# cooling: {waited} s, {temp:.1f} °C, big {freq} MHz")
    sys.stdout.flush()
    try:
        for r in range(1, a.rounds + 1):
            order = [launches[(i + r - 1) % len(launches)] for i in range(len(launches))]
            for arm, pkg, scene in order:
                d = os.path.join(a.out, f"r{r}-{arm}-{scene}")
                os.makedirs(d, exist_ok=True)
                label = (a.label or {}).get(arm, arm)
                adb("logcat", "-c")
                adb("shell", "input keyevent KEYCODE_WAKEUP")
                for p in PACKAGES:
                    adb("shell", f"am force-stop {p}")
                adb("shell", f"am start -n {pkg}/{ACT} --es mode bench --es commit {label} --ei rounds 1 "
                             f"--ei seconds {a.seconds} --ez instance false --es scenes {scene} --ei round {r}")
                deadline = time.time() + a.seconds + 60
                n = 0
                done = False
                while time.time() < deadline:
                    time.sleep(1)
                    out = adb("shell", f"dumpsys gfxinfo {pkg} framestats", timeout=15)
                    if out:
                        n += 1
                        with open(os.path.join(d, f"gfx-{n:03d}.txt"), "w") as f:
                            f.write(out)
                    log = adb("logcat", "-d", "-s", "MutantBench:I", timeout=15) or ""
                    if "DONE" in log:
                        done = True
                        break
                with open(os.path.join(d, "logcat.txt"), "w") as f:
                    f.write(adb("logcat", "-d", "-s", "MutantBench:I", timeout=15) or "")
                files = f"/sdcard/Android/data/{pkg}/files"
                for name in ("metrics.csv", "scenes.csv"):
                    adb("pull", f"{files}/{name}", os.path.join(d, name), timeout=60)
                temp, freq = phone()
                print(f"round {r} {arm}:{scene:<14} {'done' if done else 'NO DONE'}  {n} gfxinfo dumps  "
                      f"{temp:.1f} °C  big {freq} MHz")
                sys.stdout.flush()
    finally:
        for p in PACKAGES:
            adb("shell", f"am force-stop {p}")
        if a.hint_manager is not None:
            adb("shell", f"setprop debug.hwui.use_hint_manager {hint_before or 'true'}")
            now = (adb("shell", "getprop debug.hwui.use_hint_manager") or "").strip()
            print(f"# debug.hwui.use_hint_manager restored to '{now}'")
        temp, freq = phone()
        print(f"# phone after: {temp:.1f} °C, big {freq} MHz")


def q(xs, p):
    if not xs:
        return float("nan")
    s = sorted(xs)
    return s[min(len(s) - 1, int(p * len(s)))]


def roots(path):
    """Each view root's framestats rows in one dump, by the root's name."""
    txt = open(path).read()
    parts = re.split(r"\n\t(.+?) \(visibility=\d+\)\n", txt)
    out = {}
    for i in range(1, len(parts), 2):
        m = re.search(r"---PROFILEDATA---\n(.*?)\n---PROFILEDATA---", parts[i + 1], re.S)
        if not m:
            continue
        lines = m.group(1).strip().split("\n")
        hdr = lines[0].rstrip(",").split(",")
        out[parts[i]] = [dict(zip(hdr, map(int, line.rstrip(",").split(",")))) for line in lines[1:] if line]
    return out


def read_launch(d, seconds):
    fm = []
    with open(os.path.join(d, "metrics.csv")) as f:
        rows = [line for line in f if not line.startswith("#")]
    for row in csv.DictReader(rows):
        fm.append(row)
    if not fm:
        return None
    since = int(fm[0]["vsync_ns"]) - int(fm[0]["since_ns"])
    lo, hi = since + SETTLE_NS, since + seconds * 1_000_000_000
    frames = []
    for row in fm:
        v = int(row["vsync_ns"])
        if lo <= v <= hi and row["first"] == "0":
            work = int(row["animation_ns"]) + int(row["layout_ns"]) + int(row["draw_ns"])
            frames.append((v, work))
    frames.sort()
    missed = sum(1 for i in range(1, len(frames)) if frames[i][0] - frames[i - 1][0] > GAP_NS)
    act, pop = {}, {}
    for g in sorted(glob.glob(os.path.join(d, "gfx-*.txt"))):
        for name, rs in roots(g).items():
            into = act if "BenchActivity" in name else pop
            for r in rs:
                into[r["Vsync"]] = r
    matched = [(v, w, act[v]) for v, w in frames if v in act]
    agree = sum(1 for _, w, r in matched if abs((r["SyncQueued"] - r["AnimationStart"]) - w) <= 100_000)
    pop_in = [r for v, r in pop.items() if lo <= v <= hi]
    main, literal = [], []
    for v, w in frames:
        p = pop.get(v)
        if p:
            main.append(max(w, p["SyncQueued"] - p["AnimationStart"]))
            literal.append(w + p["SyncQueued"] - p["AnimationStart"])
        else:
            main.append(w)
            literal.append(w)
    return {
        "frames": len(frames),
        "fps": len(frames) / (seconds - 1),
        "work": q([w for _, w in frames], 0.5) / 1e6,
        "work95": q([w for _, w in frames], 0.95) / 1e6,
        "missed": missed,
        "matched": len(matched),
        "agree": agree,
        "pop_fps": len(pop_in) / (seconds - 1),
        "pop_draw": q([r["SyncQueued"] - r["DrawStart"] for r in pop_in], 0.5) / 1e6,
        "main": q(main, 0.5) / 1e6,
        "main95": q(main, 0.95) / 1e6,
        "literal": q(literal, 0.5) / 1e6,
    }


def read(a):
    res = {}
    for d in sorted(glob.glob(os.path.join(a.out, "r*-*-*"))):
        m = re.match(r"r(\d+)-(\w+)-(\w+)$", os.path.basename(d))
        if not m or not os.path.exists(os.path.join(d, "metrics.csv")):
            print(f"# {os.path.basename(d)}: no metrics.csv")
            continue
        r = read_launch(d, a.seconds)
        if r is None:
            print(f"# {os.path.basename(d)}: empty metrics.csv")
            continue
        key = (int(m.group(1)), m.group(2), m.group(3))
        res[key] = r
        print(f"round {key[0]} {key[1]}:{key[2]:<14} {r['fps']:5.1f} f/s  work p50 {r['work']:5.2f} p95 "
              f"{r['work95']:5.2f}  missed {r['missed']:3d}  gfx matched {r['matched']}/{r['frames']} agree "
              f"{r['agree']}  popup {r['pop_fps']:5.1f} f/s draw p50 {r['pop_draw']:5.2f}  main-thread p50 "
              f"{r['main']:5.2f} p95 {r['main95']:5.2f} (literal sum {r['literal']:5.2f})")
    return res


if __name__ == "__main__":
    ap = argparse.ArgumentParser()
    ap.add_argument("mode", choices=["run", "read"])
    ap.add_argument("out")
    ap.add_argument("--rounds", type=int, default=6)
    ap.add_argument("--seconds", type=int, default=10)
    ap.add_argument("--hint-manager", choices=["true", "false"])
    ap.add_argument("--launch", action="append", default=[])
    ap.add_argument("--label", action="append", default=[], help="arm=commit, the label each arm's logs carry")
    a = ap.parse_args()
    a.label = dict(x.split("=", 1) for x in a.label)
    run(a) if a.mode == "run" else read(a)
