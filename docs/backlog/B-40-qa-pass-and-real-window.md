---
id: B-40
title: "A QA pass over the finished MVP and a real window driven by AWT events"
status: wip
priority: P1
size: M
stage: stage-5-product
blocked_by: [B-37]
---

# B-40 — A QA pass over the finished MVP and a real window driven by AWT events

Per-component proof holds what the design draws, not what a consumer does to it: in plakat-ui one adversarial pass over a finished kit found 40 defects, and a real window found one more.

- **The decision and its reason.** An agent told to break, not to confirm, over the MVP on the desktop test runtime: constraints, long content, computed values, font scale 2, keyboard focus, hidden input, overlays and focus return, the mouse. Findings numbered and triaged P0–P3 into items; the sweeps stay as tests. Then a throwaway driver posts AWT mouse, wheel and key events to the real window over the sample.

- AC: a report with numbered findings and repros; items for every finding the owner keeps; the sweeps in the test suite; hover, Tab order, the wheel and resize checked in a real window.
- Anchors: `docs/research/research-architecture.md`
