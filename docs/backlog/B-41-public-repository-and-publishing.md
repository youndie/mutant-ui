---
id: B-41
title: "Public repository, publishing every main to Reposilite, the site on Pages"
status: done
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

## Result (2026-11-14)

- **Before anything left the Mac**, the tree and all 220 commits were read for private project names (the owner's private repositories by name, and the portfolio's), absolute paths, addresses, tokens and tool signatures: none. Every commit is the owner's.
- **Six coordinates.** `mutant-core` applies sborka's publish plugin; `publishToMavenLocal` gave exactly `mutant-core`, `-desktop`, `-android`, `-iosarm64`, `-iossimulatorarm64` and `-wasm-js`, their POMs with no pin on the publishing host.
- **The repository** went public with `main` as it stood and only `check.yaml`. Its first `gradle` job failed three text checks by one row of ink on macOS 26.7.2, and the rerun on the same image passed: [B-71](B-71-text-ink-varies-across-ci-macs.md). The gate now prints its Mac; the green run on `main` after the merge ran on an `Apple M1 (Virtual)`, `VirtualMac2,1`, 7 GB.
- **The token** came from the infrastructure repository's `reposilite-token.yaml`, one route per coordinate, as plakat-ui's; only the secrets' names were ever read back. **Pages** were turned on for workflow deployments. `publish.yaml`, `pages.yaml` and `renovate.json` (sborka's presets) arrived through pull request #2, merged on a green head commit.
- **The first publish**, run 43608110258 over `5fd3e5b`: `0.1.0.1`, the latest version of all six coordinates in their `maven-metadata.xml`; sborka's consumer probe passed.
- **A consumer project** outside this build, with this repository only by the README's block: the README's snippet compiled for desktop, wasm, `iosArm64` and `iosSimulatorArm64`, and on desktop rendered the button's body and its label in the bundled face, on this Mac. Android was not compiled; nothing ran on a phone, a simulator or a browser from it.
- **The site**, <https://youndie.github.io/mutant-ui/>: the page, `sample.js` and both wasm files answer 200; looked at in a browser, the Components page draws, its bodies arriving on the frame after the page's first (B-50).
- The loop now works through pull requests (`CLAUDE.md`, Workflow).
