---
id: B-146
title: "The repository put in order, and a README"
status: done
priority: P2
size: S
stage: stage-6-later
blocked_by: []
---

# B-146 — The repository put in order, and a README

The owner, 2026-11-20: «приводи репозиторий в порядок, оформи readme». The README is 52 lines on how to depend on the
kit; it shows none of it and says nothing of the theme's switches, the sample, the targets' state or how the kit is
held to its design. 33 local and about 70 remote branches are left from merged pull requests.

- **The decision.** The README is the kit's front page: what it looks like (the demo's own goldens, so it can never
  show a picture the code does not draw), what is in it, how to use it and switch it, where it runs and how well, how
  it is kept honest, how to build it, where the rest is. It points to the documents rather than retelling them.
- AC:
  - the README rewritten, every claim in it checked against the code at its commit, every link resolving;
  - branches whose pull request merged deleted, local and remote; open ones and `fix/b-145-…` (parked) kept;
  - `make check` green.
- Anchors: `README.md`

## Findings

### 2026-11-20

- **The README** is rewritten. It shows the demo's two screens through the kit's own goldens
  (`Demo_Library_Efir.png`, `Demo_Recording_Kopiya.png`). It lists the public components by the design's groups and
  the theme's six switches, with their defaults and meanings taken from `MutantTheme`'s KDoc. Every platform is given
  with how to run it and its measured state, each from the item that measured it. It also says how the kit is held to
  its design, the two gates, and where things are. Every local link resolves. Three claims were corrected against the
  code before the commit: what `cleanSignal` leaves (the bodies' shapes and the clock), `mosaicPastContour` (an
  experiment, not the design's) and the sample's browser task (`wasmJsBrowserDistribution`).
- **Branches**, on the owner's word («Удалить локальные и удалённые»):
  - 33 local branches deleted. Ten had a merged pull request. Twenty-three, B-110 to B-135, had none of their own, and
    each tip was checked to be an ancestor of `feat/b-110-b-135-the-performance-loop`, whose pull request merged.
  - On GitHub only `fix/b-71-tests-never-from-cache` (merged) was left; the other merged branches had already been
    deleted at their merge, and their stale tracking refs went with `git fetch --prune`.
  - Kept: `main`, `renovate/sborka` (open), `fix/b-145-…` (parked, B-145).
- `make check` green.
