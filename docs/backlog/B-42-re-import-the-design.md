---
id: B-42
title: "Re-import the design when the owner starts it"
status: open
priority: P3
size: S
stage: stage-5-product
---

# B-42 — Re-import the design when the owner starts it

`reference/design-system/` is the 2026-11-13 export of project `3c6970f5-9ab1-4100-b418-41b7bacfcf78` (research §1.1). The designer will be told of the hot flow-line discrepancy (D4) and may deliver more.

- **The decision and its reason.** A new version is a branch of its own: unpack, diff tokens, generator, golden and each page's slots with a script, put the diff in the commit body, regenerate tokens, goldens and references, update research §1.1. DesignSync only under the owner's `/design-sync`.

- AC: the new snapshot is in `reference/design-system/` with its version in `reference/README.md`; every generated file and reference is regenerated in the same branch; `scripts/design-golden.mjs` is updated with the new golden's known entries.
- Anchors: `reference/README.md`, `scripts/design-golden.mjs`
