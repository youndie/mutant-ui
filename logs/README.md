# logs — raw runs

Every measurement's raw output, one directory per backlog item (`b-NN/`), one file per run named
`<date>-<what>.log`, each with a header naming the host, the JVM and the commit of the harness. These
files are the evidence; a number in a document points at the run it came from. A void run stays here,
named as void in its item, and no number of it is reported.

| Directory | Item | Reader |
|---|---|---|
| `b-06/` | [B-06](../docs/backlog/B-06-frame-cost.md) — the field's cost on the desktop JVM | `scripts/b06_summary.py` |
| `b-84/` | [B-84](../docs/backlog/B-84-drip-webs-flip-every-tick.md) — steady webs against the engine's frame, the `steady` arm of the field bench | by hand: medians of rounds 2–5, `steady` against `warm` |
| `b-85/` | [B-85](../docs/backlog/B-85-aliased-text-on-macos-26.md) — the slider's value under six raster settings over four grounds, on the macOS 26 runner and on this Mac, with crops | by hand |
| `b-69/` | [B-69](../docs/backlog/B-69-tall-body-tick-cost.md) — a tall body's cost per tick in a real window | the bench's own table at the end of each log |
| `b-73/` | [B-73](../docs/backlog/B-73-frame-cost-in-the-browser.md) — the sample's frame in headless Chrome per scene, the JVM's beside it at the same commit, and profiles of the red scenes | the bench's own pooled table at the end of each log; profiles by hand |
| `b-87/` | [B-87](../docs/backlog/B-87-ticks-on-the-one-thread.md) — B-73's browser and JVM benches after the fix: `-final` at `085a110`, the others intermediate (`2271b6d`, `6e3faa5`) | the bench's own pooled table at the end of each log |
| `b-88/` | [B-88](../docs/backlog/B-88-keyframe-loss-copies-the-whole-picture.md) — a step of the keyframe loss on the JVM (`-before`, `-after`), B-73's browser bench, the Library screen with and without its loss (`-control`), and the discarded first try (`-pixels`) | the benches' own tables at the end of each log |
