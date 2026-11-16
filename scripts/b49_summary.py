#!/usr/bin/env python3
"""B-49: the verdicts of the phone's run, by the rules pre-registered in docs/backlog/B-49-frame-cost-on-a-pixel-6a.md.

    scripts/b49_summary.py <run dir> <M1 fieldBench log>

Reads <run dir>/metrics.csv, scenes.csv and instance.txt as scripts/device-bench.sh pulls them. Counts rounds 2 and
later, and each scene's frames from its second second on; drops a window's first frame.
"""
import csv
import re
import statistics
import sys
from collections import defaultdict
from pathlib import Path

MS = 1e6
FRAME = 16.7


def pct(xs, q):
    xs = sorted(xs)
    if not xs:
        return float("nan")
    return xs[min(len(xs) - 1, int(q * len(xs)))]


def screens(run: Path):
    rows = defaultdict(list)
    with open(run / "metrics.csv") as f:
        lines = [l for l in f if not l.startswith("#")]
    for r in csv.DictReader(lines):
        rnd = int(r["round"])
        since = int(r["since_ns"])
        if rnd < 2 or since < 1e9 or r["first"] == "1":
            continue
        rows[(r["scene"], rnd)].append(r)
    scenes = {}
    with open(run / "scenes.csv") as f:
        lines = [l for l in f if not l.startswith("#")]
    for r in csv.DictReader(lines):
        scenes[(r["scene"], int(r["round"]))] = r
    return rows, scenes


def summarise(rows, scenes):
    names = sorted({s for s, _ in rows})
    out = {}
    print(
        "scene        frames  fps   missed  total p50  total p95  work p50  work p95  gpu p95  >16.7   >33  skipped"
        "  frozen   amended"
    )
    for s in names:
        frames = [r for (n, _), rs in rows.items() if n == s for r in rs]
        total = [int(r["total_ns"]) / MS for r in frames]
        work = [(int(r["animation_ns"]) + int(r["layout_ns"]) + int(r["draw_ns"])) / MS for r in frames]
        gpu = [int(r["gpu_ns"]) / MS for r in frames]
        missed = sum(1 for r in frames if int(r["total_ns"]) > int(r["deadline_ns"])) / len(frames)
        gaps = []
        seconds = 0.0
        for (n, rnd), rs in rows.items():
            if n != s:
                continue
            v = sorted(int(r["vsync_ns"]) for r in rs)
            gaps += [(b - a) / MS for a, b in zip(v, v[1:])]
            seconds += (v[-1] - v[0]) / 1e9 if len(v) > 1 else 0
        fps = len(frames) / seconds if seconds else float("nan")
        over17 = sum(1 for g in gaps if g > FRAME) / max(1, len(gaps))
        over33 = sum(1 for g in gaps if g > 33.3) / max(1, len(gaps))
        p50, p95 = pct(total, .5), pct(total, .95)
        if missed < .01 and p95 <= FRAME:
            verdict = "GREEN"
        elif p50 > FRAME or missed > .05:
            verdict = "RED"
        else:
            verdict = "GREY"
        # The owner's late amendment (2026-11-16): vsyncs skipped and the main thread's work, not TOTAL.
        skipped = sum(1 for g in gaps if g > 25) / max(1, len(gaps))
        if skipped < .01 and pct(work, .95) <= FRAME:
            amended = "GREEN"
        elif skipped > .05:
            amended = "RED"
        else:
            amended = "GREY"
        if s in ("Blank", "TenCards8", "Spin5"):
            verdict = f"({verdict.lower()})"
            amended = f"({amended.lower()})"
        out[s] = dict(work=work, draw=[int(r["draw_ns"]) / MS for r in frames])
        print(
            f"{s:12} {len(frames):6} {fps:5.1f} {missed:7.1%} {p50:10.2f} {p95:10.2f} {pct(work, .5):9.2f} "
            f"{pct(work, .95):9.2f} {pct(gpu, .95):8.2f} {over17:6.1%} {over33:6.1%} {skipped:8.2%}  {verdict:8} {amended}"
        )
    print(
        "Times in ms; skipped: gaps between vsyncs over 25 ms. frozen: the pre-registered rule on TOTAL; amended: the "
        "owner's late amendment. Verdicts in brackets are not the brief's targets."
    )
    scrolled = [int(r["scrolled_px"]) for (n, _), r in scenes.items() if n == "List100"]
    print(f"List100 scrolled per scene, px: {', '.join(map(str, scrolled))}")
    thermals = sorted({(r["thermal_start"], r["thermal_end"]) for r in scenes.values()})
    print(f"thermal status (start, end) seen: {thermals}")
    if "Spin5" in out and "TenCards12" in out:
        print("control: Spin5 over TenCards12, draw p50 per counted round (pre-registered: 5 ± 1 ms)")
        for rnd in sorted({r for (_, r) in rows}):
            a = [int(r["draw_ns"]) / MS for r in rows.get(("Spin5", rnd), [])]
            b = [int(r["draw_ns"]) / MS for r in rows.get(("TenCards12", rnd), [])]
            if a and b:
                print(f"  round {rnd}: {statistics.median(a) - statistics.median(b):.2f}")
        d = statistics.median(out["Spin5"]["draw"]) - statistics.median(out["TenCards12"]["draw"])
        print(f"  pooled: {d:.2f} ms -> {'SEES IT' if abs(d - 5) <= 1 else 'BLIND: no screen verdict stands'}")


ROUND = re.compile(r"^round (\d+) (\S+) (.*)$")


def arms(path: Path):
    """Per case and arm, the median over rounds 2 and later of each round's median, µs."""
    cells = defaultdict(list)
    for line in open(path):
        m = ROUND.match(line.strip())
        if not m or int(m.group(1)) < 2 or m.group(2) in ("checksum", "control"):
            continue
        for kv in m.group(3).split():
            k, v = kv.split("=")
            # The phone prints in its own locale: a decimal comma.
            cells[(m.group(2), k)].append(float(v.replace(",", ".")))
    return {k: statistics.median(v) for k, v in cells.items()}


def instance(run: Path, m1: Path):
    phone, mac = arms(run / "instance.txt"), arms(m1)
    cases = sorted({c for c, _ in phone})
    print("case          instance µs  (M1)   factor   warm µs   (M1)   factor")
    fi, fw = [], []
    for c in cases:
        pi, pw = phone[(c, "instance")], phone[(c, "warm")]
        mi, mw = mac.get((c, "instance")), mac.get((c, "warm"))
        fi.append(pi / mi)
        fw.append(pw / mw)
        print(f"{c:12} {pi:11.1f} {mi:7.1f} {pi / mi:7.2f} {pw:9.1f} {mw:7.1f} {pw / mw:7.2f}")
    worst = max(phone[(c, "instance")] for c in cases)
    print(f"instance: worst case {worst:.1f} µs -> {'GREEN' if worst <= 1000 else 'RED'} (≤ 1 ms every case)")
    for name, f in (("instance", fi), ("warm frame", fw)):
        med = statistics.median(f)
        word = "confirms" if 3 <= med <= 5 else "refutes"
        print(f"factor, {name}: median {med:.2f} (range {min(f):.2f}–{max(f):.2f}) -> {word} B-06's 4")
    for line in open(run / "instance.txt"):
        if line.startswith("round") and "control" in line:
            print("phone " + line.strip())


if __name__ == "__main__":
    run, m1 = Path(sys.argv[1]), Path(sys.argv[2])
    summarise(*screens(run))
    print()
    instance(run, m1)
