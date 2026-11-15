---
id: B-73
title: "The sample's frame cost in the browser (wasm)"
status: wip
priority: P3
size: M
stage: stage-5-product
blocked_by: [B-72, B-75, B-76, B-78, B-82, B-83, B-30, B-31, B-32, B-33, B-34, B-35, B-36, B-44, B-45, B-46, B-47]
---

# B-73 — The sample's frame cost in the browser (wasm)

**Moved to the end by the owner on 2026-11-15**: the JVM's frame cost is acceptable after B-75 and B-82, and the kit's components come first. Blocked by the components still to build, so it is picked after them.

The owner, 2026-11-14: the published sample stutters in the browser to the eye. Measured after the JVM ([B-72](B-72-frame-cost-on-the-jvm.md)), so what is the kit's own cost is found and fixed there first, and what remains here is the browser's.

- **The decision and its reason.** The same scenes as B-72, in the production wasm build, in Chrome driven over CDP (headless, as the references are: a hidden pane draws no frames). Per frame, the interval between `requestAnimationFrame` callbacks and the long tasks the page reports; the Performance trace for any scene not green. The same positive control (a 10 ms busy-wait in a frame). Thresholds written in this item before its first number, against B-72's numbers for the same scene.
- Rejected: measuring the browser first, where a slow frame could be the kit, Skia's wasm build or the browser, and the cheapest of the three to profile is not there.

- AC: raw logs under `logs/b-73/`, the control's reading, a verdict per scene, its ratio to the JVM's; for each scene not green, the trace and an item per cause.
- Anchors: `.github/workflows/pages.yaml`, `sample/build.gradle.kts`
