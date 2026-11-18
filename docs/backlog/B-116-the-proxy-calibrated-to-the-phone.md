---
id: B-116
title: "The phone proxy calibrated to the phone, and the cuts measured again on it"
status: wip
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
