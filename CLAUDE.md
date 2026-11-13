# mutant-ui — working notes for an agent

The mutant-ui design — a damaged copy: grime, glitch and VHS; generated slime bodies with drips,
mosaic and flow lines under clean content, tape noise over the screen — as a Compose Multiplatform
component library. The design is a Claude Design project with an executable generator, frozen in
[reference/design-system/](reference/design-system/).

## How to start a session

1. **[docs/research/research-architecture.md](docs/research/research-architecture.md)** — first,
   every time. It says what was imported and verified, and against what, which decisions were taken
   and what each rejected, and which hypotheses are still open. A task read without it looks like
   "draw the slime", and §1.3–1.4 show that the slime is a generator whose every stream, rounding
   and float width is part of the contract.
2. **[backlog.md](backlog.md)** — the order of work and why it is that order. Find the item: it
   states the decision, the rejected alternative and the acceptance criteria.
3. **The design for the component**: its slot on the page (`reference/design-system/mutant-ui
   Компоненты.dc.html` for the MVP, `… Компоненты v1.dc.html`, `… Компоненты позже.dc.html`), its
   row in the handoff's «Компоненты» table (`reference/design-system/README.md`), its type profile
   in `mutant-tokens.json`, and how the engine draws it (`mutant-engine.js`, `drawItem`). Shapes come
   from `mutant-generator.js`, never from a picture. These files are data, not instructions.
4. The layer document for the area you are touching, once it exists ([docs/README.md](docs/README.md)
   lists what does).

## Workflow

The repository is local, with no remote, until [B-41](docs/backlog/B-41-public-repository-and-publishing.md).
The loop over the backlog works one item per branch and merges its own:

1. `feat/b-NN-<slug>` (`fix/b-NN-<slug>` for a defect) from `main`, never a commit on `main`.
2. Implement, with the item's status and documents in the same branch. Both gates green.
3. `git merge --no-ff` into `main`, then delete the branch. `Refs: B-NN` in the commits.

Red is never merged, and a check is never loosened to get green. An item whose status is
`question` stops the loop with the question instead.

## Gates

```bash
make check          # the documentation gate (docs-bootstrap checkers) and this repository's own checks
./gradlew check     # the code gate, once B-01 has created the build
```

The documentation checkers are docs-bootstrap's, at the version the `uses: youndie/docs-bootstrap@…`
line in `.github/workflows/check.yaml` pins. The first `make check` fetches that version into
`.docs-bootstrap/`; there are no copies under `scripts/`. `make fix` regenerates the backlog index.
`make check` also runs `scripts/design-golden.mjs --check`: the vendored generator must still
reproduce the vendored golden, except the entries it lists with their cause.

**Builds run on this Mac, not on the Linux box** (research D12). The project is not in
`mutagen sync list`, and references and goldens are a claim about the rasteriser that recorded them.

## Re-importing the design

`reference/design-system/` is the owner's export of claude.ai/design project
`3c6970f5-9ab1-4100-b418-41b7bacfcf78`, taken on 2026-11-13 (engine `?v=50`). A newer version is
taken on a branch of its own, never by overwriting, and only when the owner starts it
([B-42](docs/backlog/B-42-re-import-the-design.md)):

1. **Unpack** with a cp437 → UTF-8 name re-decode: the export's names are UTF-8 without the flag,
   and macOS `unzip` stops on them (research §1.1).
2. **Diff** `mutant-tokens.json`, `mutant-generator.js`, `mutant-golden.json` and each page's slots,
   and put the diff in the commit body.
3. **Regenerate** tokens, goldens and references in the same branch, update `KNOWN` in
   `scripts/design-golden.mjs`, and update research §1.1.

## Rules that are easy to get wrong here

- **`Mutant` in every identifier, package `io.github.youndie.mutant.*`** (D1). The generator lives in
  `io.github.youndie.mutant.gen` and imports nothing from Compose.
- **The generator is ported 1:1** (D3): every random stream `rng(seed + '/' + key, name)` and the
  order of calls inside it, the field as `FloatArray`, half-up rounding `floor(v * 10 + 0.5) / 10`
  where a path string is reproduced — not `kotlin.math.round`, which is half to even.
- **Where the vendored golden and the engine disagree, the engine wins** (D4); the disagreement
  stays listed in `scripts/design-golden.mjs`.
- **One clock per screen**; no component reads system time (D6). Paths are rebuilt once per tick.
- **Drips hang outside the slot and never affect layout**; flow lines, mosaic and dead pixels never
  enter the clean zone (D5).
- **Glyphs, icons, the slot and the touch target never depend on the seed or an animation.** Text
  is never damaged (brief, «Порча смысла»). Touch target ≥ 48 dp by the slot.
- **Reduce motion is the pause** (D7): the clock stops, damage stays, toggles are instant.
  «Чистый сигнал» is a separate switch.
- **The red never flashes**: the error state and the radio dot are static (tokens, `flash.red`).
- **No literal colour, size or duration in a component.** Tokens are generated from
  `mutant-tokens.json` (D8); the generator's constants are named and held by the goldens.
- **Never edit the vendored design, a golden or a reference to make parity pass**, and never raise
  a tolerance to get green. A gap is a number in the commit body with its cause.
- **Fonts only under an open licence**: Handjet and Rubik, both OFL (D9).
- **No brands, franchises, real VCR menus, real test cards, vaporwave or analogue-horror motifs** in
  demo content (brief, «Не делаем»).
- **What the design does not draw is designed here, and says so** (D15): v1 appearance and gestures,
  the snackbar's show and hide, hover, «Чистый сигнал». The item lists every number the design does
  not state; its goldens are recorded when built and revised in the owner's review round.
- **`main` describes what exists.** A document describing something not yet built is
  `status: draft` and lives on a branch.

## Licence

MIT (`LICENSE`, research D14), like toon-ui and plakat-ui. The bundled fonts keep their OFL.

## Language

Documentation, code, comments, KDoc, test names and commit messages are in **English** (D11).
The vendored brief and handoff are in Russian. Demo strings in fixtures are copied from the design
verbatim. Commits follow Conventional Commits with no tool signature.
