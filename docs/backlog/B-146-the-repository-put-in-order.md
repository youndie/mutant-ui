---
id: B-146
title: "The repository put in order, and a README"
status: wip
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
