#!/usr/bin/env python3
"""B-02: fetch the kit's two families from google/fonts at one commit and cut their static instances.

    python3 -m venv .venv && .venv/bin/pip install fonttools==4.68.0
    .venv/bin/python scripts/fonts.py           # fetch, cut, write
    .venv/bin/python scripts/fonts.py --report  # print what the variable files carry, write nothing

Both families are published only as variable fonts: Handjet with `ELGR`, `ELSH` and `wght`, Rubik
with `wght`. A `wght` setting needs Android API 26; under it the file's default instance draws, so a
weight the kit asks for would silently fall back at minSdk 24 (research §1.6, D9). The weights the
kit uses (mutant-tokens.json `type`, and the pages) are therefore cut here with fontTools'
instancer, every other axis pinned at its default. The OFL permits modified versions, and neither
family declares a Reserved Font Name, so the instances keep the family name; each OFL text ships
beside its fonts.

Every fetched file is checked against `scripts/fonts.lock`, so a changed upstream file stops the
script instead of quietly changing the kit. The instances keep the upstream `head.modified`, so a
second run is byte-identical.
"""
import hashlib
import io
import pathlib
import sys
import urllib.request

from fontTools.ttLib import TTFont
from fontTools.varLib import instancer

# The google/fonts commit plakat-ui's fonts were read at; both families are present there.
COMMIT = "2fe1e8c6b137fb13041a7cfca6fdcd8a910ea4dc"
BASE = f"https://raw.githubusercontent.com/google/fonts/{COMMIT}/ofl"
HANDJET = "handjet/Handjet%5BELGR%2CELSH%2Cwght%5D.ttf"
HANDJET_OFL = "handjet/OFL.txt"
RUBIK = "rubik/Rubik%5Bwght%5D.ttf"
RUBIK_OFL = "rubik/OFL.txt"
ROOT = pathlib.Path(__file__).resolve().parent.parent
RES = ROOT / "mutant-core/src/commonMain/composeResources"
FONTS = RES / "font"
LICENCES = RES / "files/licences"
LOCK = ROOT / "scripts/fonts.lock"
# Handjet: display and on-screen text, 400 and 600 as the pages load them (a page's 500 draws as 400
# in Chrome, which has no 500 to pick). Rubik: text, 400 and 500 (tokens `type.text.weights`).
STATICS = [
    (HANDJET, 400, "handjet_regular"),
    (HANDJET, 600, "handjet_semibold"),
    (RUBIK, 400, "rubik_regular"),
    (RUBIK, 500, "rubik_medium"),
]


def fetch(path: str) -> bytes:
    with urllib.request.urlopen(f"{BASE}/{path}") as r:
        return r.read()


def axes(font: TTFont) -> str:
    return ", ".join(f"{a.axisTag} {a.minValue:g}..{a.maxValue:g} (default {a.defaultValue:g})" for a in font["fvar"].axes)


def main() -> None:
    report = "--report" in sys.argv
    lock = {}
    if LOCK.exists():
        for line in LOCK.read_text().splitlines():
            digest, path = line.split("  ", 1)
            lock[path] = digest
    data = {}
    for path in (HANDJET, HANDJET_OFL, RUBIK, RUBIK_OFL):
        body = fetch(path)
        digest = hashlib.sha256(body).hexdigest()
        if path in lock and lock[path] != digest:
            sys.exit(f"{path}: upstream changed ({digest} != {lock[path]}); review it, then delete scripts/fonts.lock")
        lock[path] = digest
        data[path] = body

    if report:
        for path in (HANDJET, RUBIK):
            font = TTFont(io.BytesIO(data[path]))
            print(f"{path}: {len(data[path])} bytes; axes {axes(font)}; {len(font.getBestCmap())} code points")
        return

    LOCK.write_text("".join(f"{d}  {p}\n" for p, d in sorted(lock.items())))
    FONTS.mkdir(parents=True, exist_ok=True)
    LICENCES.mkdir(parents=True, exist_ok=True)
    (LICENCES / "handjet-OFL.txt").write_bytes(data[HANDJET_OFL])
    (LICENCES / "rubik-OFL.txt").write_bytes(data[RUBIK_OFL])

    for path, weight, name in STATICS:
        variable = TTFont(io.BytesIO(data[path]))
        modified = variable["head"].modified
        # Every axis but wght pinned at its default, so the instance is fully static.
        location = {a.axisTag: a.defaultValue for a in variable["fvar"].axes}
        location["wght"] = weight
        static = instancer.instantiateVariableFont(variable, location, updateFontNames=True)
        static["OS/2"].usWeightClass = weight
        static["head"].modified = modified
        static.recalcTimestamp = False
        out = io.BytesIO()
        static.save(out, reorderTables=True)
        (FONTS / f"{name}.ttf").write_bytes(out.getvalue())
        print(f"{name}.ttf  {location}  {len(out.getvalue())} bytes")


if __name__ == "__main__":
    main()
