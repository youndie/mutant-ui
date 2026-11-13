---
id: B-41
title: "Public repository, publishing every main to Reposilite, the site on Pages"
status: open
priority: P1
size: M
stage: stage-5-product
blocked_by: [B-39, B-40]
---

# B-41 — Public repository, publishing every main to Reposilite, the site on Pages

The line's kits publish every push to `main` as `<version>.<run>` through sborka's `publish-wip.yaml`, six coordinates per kit, and deploy the sample to Pages.

- **The decision and its reason.** The order plakat-ui followed: read the tree and the whole history for private names, paths, secrets and tool signatures; `publishToMavenLocal` gives exactly six coordinates; create the repository and push `main` without the publish workflow so both gates prove themselves first; then the token, Pages, and the publish workflow through a pull request.
- Only with the owner's go-ahead: it is the first public step.

- AC: the repository is public with both gates green on `main`; the first snapshot resolves on all six coordinates; the README's snippet compiles from a separate project; the site serves the sample.
- Anchors: `.github/workflows/check.yaml`
