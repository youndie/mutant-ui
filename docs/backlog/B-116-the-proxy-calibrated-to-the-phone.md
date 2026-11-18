---
id: B-116
title: "The phone proxy calibrated to the phone, and the cuts measured again on it"
status: done
priority: P2
size: S
stage: stage-6-later
blocked_by: []
---

# B-116 — The phone proxy calibrated to the phone, and the cuts measured again on it

Found by [B-114](B-114-a-tick-image-built-in-the-frame.md): the proxy slowed four times (B-107's default) runs ten cards at
about 210 ms a frame, the Pixel 6a at about 48 (B-111), so B-112 to B-114 were measured in a state the phone is not in.

**The calibration** ([logs](../../logs/b-116/), the kit, three rounds of 8 s, the Mac on mains): ten cards ran at 60.4
frames a second slowed ×1, 32.9 at ×1.5, **17.1 at ×2 (work p50 39.6 ms)**, 8.1 at ×2.5 (135.0), 6.8 at ×3 (160.2). The
phone ran them at 21.5 and 18.3 frames a second, work p50 37.4 and 40.7 ms: ×2 is the phone. Between ×2 and ×2.5 the cost
jumps threefold: there the builds ahead stop finishing (B-113's control) and every body moves into the frame. **The phone
stands at that edge.**

- **Measured before it ran.** The proxy slowed ×2, in one rotation: the kit (B-111's build), B-113's freeze, B-112's still
  layers once (both paths), B-114's tick image in the frame (with B-113's freeze and B-115's grid), the kit without the four
  decorations; `TenCards8` and `Blank`, six rounds of ten seconds, round 1 not counted. A build **buys its cost** if every
  counted round is cheaper than the kit, in work p50 and in ms a frame, by more than twice the largest move of `Blank`'s
  work between the same builds. A build that buys it is taken up again as its own item; the rest stay reverted.

## Results, 2026-11-18

[The run](../../logs/b-116/2026-11-18-proxy-x2.log), the Mac on mains. `Blank`'s work moved by at most 1.2 ms between any
two builds, so the line is 2.4 ms. Per counted round, against the kit at 16.4 frames a second:

| Build | ms a frame saved | work p50 saved | Verdict |
|---|---|---|---|
| B-113, the image frozen | 0.4, 0.7, 0.4, −0.7, 0.0 | 62.3, −0.1, −0.6, 0.4, −5.6 | does not buy it |
| **B-112, the still layers once** | **6.4, 6.7, 6.1, 6.6, 6.0** | **69.8, 8.2, 6.4, 8.1, 8.1** | **buys it**: 18.3 frames a second |
| B-114, the tick image in the frame | −77.5 to −78.3 | −48.2 to −110.8 | dearer: it pushes the frame over the edge, 7.2 a second |
| the kit without the four decorations | 22.1–23.0 | 11.3–74.1 | 26.1 a second |

So on the calibrated proxy the still layers drawn once buy back about 6.4 of the 61 ms a frame and of the 23 the
decorations cost; on the proxy slowed four times, past the edge, they bought nothing. B-112 is taken up again as
[B-117](B-117-still-layers-once-on-the-surface-grid.md), with its four parity failures to answer. B-113 and B-114 stay
reverted.
