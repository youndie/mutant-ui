#!/usr/bin/env python3
"""B-06: read a field-bench log into the per-case table the item reports.

    python3 scripts/b06_summary.py logs/b-06/<log>     the table
    python3 scripts/b06_summary.py --control           the reader on a log whose answer is known

Per case and arm: the median of the round medians of rounds 2..5 (round 1 discarded); the ruler is
|warm - warm2| per case; the control's delta per counted round.
"""
import re
import statistics
import sys

LINE = re.compile(r"^round (\d+) (\S+) (.*)$")
CELL = re.compile(r"(\S+?)=(-?[\d.]+)")


def read(text):
    rows, control = {}, []
    for line in text.splitlines():
        m = LINE.match(line)
        if not m or int(m.group(1)) == 1:
            continue
        case, rest = m.group(2), m.group(3)
        if case == "checksum":
            continue
        cells = dict((k, float(v)) for k, v in CELL.findall(rest))
        if case == "control":
            control.append(cells["delta"])
            continue
        for k, v in cells.items():
            rows.setdefault(case, {}).setdefault(k, []).append(v)
    table = {c: {k: statistics.median(v) for k, v in arms.items()} for c, arms in rows.items()}
    return table, control


def control():
    text = "\n".join(
        [f"round {r} x warm=100.0 warm2={100 + r} cold=1000.0" for r in range(1, 6)]
        + [f"round {r} control card-a warm=1 warm+200us=201 delta=200.0" for r in range(1, 6)]
    )
    table, ctl = read(text)
    # Rounds 2..5 give warm2 102..105, median 103.5; round 1's 101 is discarded.
    assert table == {"x": {"warm": 100.0, "warm2": 103.5, "cold": 1000.0}}, table
    assert ctl == [200.0] * 4, ctl
    print("b06_summary: control passed")


def main():
    if "--control" in sys.argv:
        return control()
    table, ctl = read(open(sys.argv[1], encoding="utf-8").read())
    print(f"control delta (counted rounds): {', '.join(f'{d:.1f}' for d in ctl)} us")
    print(f"{'case':<12}{'instance':>10}{'warm':>9}{'warm2':>9}{'ruler':>8}{'hot':>9}{'cold':>9}   (us, median of rounds 2-5)")
    for case, a in table.items():
        print(f"{case:<12}{a['instance']:>10.1f}{a['warm']:>9.1f}{a['warm2']:>9.1f}{abs(a['warm'] - a['warm2']):>8.1f}{a['hot']:>9.1f}{a['cold']:>9.1f}")


if __name__ == "__main__":
    main()
