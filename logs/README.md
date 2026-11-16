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
| `b-77/` | [B-77](../docs/backlog/B-77-the-tape-on-the-render-thread.md) — the Tape's parts on the render thread (`tape`, `tape-after`), `FrameBench` before and after, the browser after | the benches' own tables at the end of each log |
| `b-80/` | [B-80](../docs/backlog/B-80-contour-clip-on-the-render-thread.md) — B-78's bench re-run after B-82: vectors, images, without the contour clip, and the clip's share of the rasterising | the bench's own table at the end of each log |
| `b-86/` | [B-86](../docs/backlog/B-86-tall-body-rasterised-whole.md) — B-69's tall body bench in two pairs: `main`'s code at `e643d81` (`before`) and `ba3e291` (`before2`) and the branch at `f647f32` (`after`, `after2`) | the bench's own table at the end of each log |
| `b-49/` | [B-49](../docs/backlog/B-49-frame-cost-on-a-pixel-6a.md) — the Pixel 6a's screens (`metrics.csv` a frame, `scenes.csv` a scene) and B-06's instance arms (`instance.txt`), with the M1's `fieldBench` at the same commit | `scripts/b49_summary.py <run dir> <M1 log>` |
