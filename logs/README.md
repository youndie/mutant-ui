# logs — raw runs

Every measurement's raw output, one directory per backlog item (`b-NN/`), one file per run named
`<date>-<what>.log`, each with a header naming the host, the JVM and the commit of the harness. These
files are the evidence; a number in a document points at the run it came from. A void run stays here,
named as void in its item, and no number of it is reported.

| Directory | Item | Reader |
|---|---|---|
| `b-06/` | [B-06](../docs/backlog/B-06-frame-cost.md) — the field's cost on the desktop JVM | `scripts/b06_summary.py` |
