---
id: B-154
title: "The keyframe loss's snapshot draws only the bodies its band can show"
status: done
priority: P1
size: S
stage: stage-6-later
blocked_by: [B-151]
---

# B-154 — The keyframe loss's snapshot draws only the bodies its band can show

From [B-151](B-151-a-real-screen-under-a-finger.md)'s baseline on the phone: a scroll of the Library screen is smooth
between the keyframe losses and stops at each, a single frame of 750–1160 ms once in 9 s, the loss's start. There the
scene paints its snapshot (`MoshScene.start`): every body of the screen drawn into the band's strips, each by paths on a
bitmap (B-133: a shader drawn there runs pixel by pixel), so each builds its frame by paths first. The strips cover the
band alone, and what the bodies draw above and below it is cleared; a body that does not reach the band draws nothing
the band keeps, and was built and drawn for nothing.

- **The decision**: the snapshot draws a body only if its box, with the reach its drips and shadow may ink (64 dp, as
  B-88's holes), meets the band. Rejected: a snapshot read back from the screen, which holds the words too (the band is
  the bodies alone) and, in a layer, is a picture being recorded with no pixels to read.
- AC:
  - the keyframe loss's goldens, parity and behaviour tests unchanged: nothing the band keeps is drawn differently;
  - measured by B-151's protocol against `main` on the phone.
- Anchors: `mutant-core/src/commonMain/kotlin/io/github/youndie/mutant/surface/MutantKeyframeLoss.kt`

## Findings

### 2026-11-21

- On the phone, [arms 1](../../logs/b-151/2026-11-21-arms-1.log) (a comparison, no floor, throttled): scroll max 797 against the arm before's 1479 ms.
- The snapshot drew each body once a strip, 9 to 19 strips, and a body drawn by paths built its frame each time: the frame
  is built once for all strips now (`byPathsPicture`, lazily). On the phone, [arms 2](../../logs/b-151/2026-11-21-arms-2.log) (valid: the floor held), against the
  arm with B-155 and B-156: scroll longest p95 200 against 449 ms, max 216 against 466.
