# Build 006 — playable match information and classic rules
Date: 13 October 2026
Status: Development changes prepared, **NOT built or Samsung-tested**.
Confirmed installed baseline remains Build 005, whose full close/reopen Continue Game was verified by the owner.

## Why this matters
The user cannot enjoy a match without knowing which dice were rolled, what resources
are in their hand, what development cards are owned/played, and what the CPUs are doing.

## Changes to verify
- Always-visible match panel beneath the turn indicator, independent of the left action rail.
- Last dice result shows the two dice and their total, even when a CPU rolls.
- Human hand explicitly lists all five resource types and counts and its development cards by type.
- Latest public activity visible on board: settlement, road, city, Knight, Monopoly,
  Year of Plenty, Road Building, robber, bank/port trades and player turn progression.
- Dice notifications disclose each player's public resource production from that roll,
  but NOT the contents of CPU resource hands or the type of development card they purchase.
- "View rolls & actions" shows most recent messages first and opponent public *card counts*.
- Private local history contains up to 60 events and survives normal app termination/resume;
  starting a new match resets it.
- No new fields added to serialized CatanGameState or other saved-game classes.
- Classic rules changes and regression tests prepared under CLASSIC-CATAN-RULES-AUDIT.md.

## Phone acceptance
1. **Install only as an update** to Build 005, using the same permanent signing key.
2. Tap Continue Saved Game: original island, pieces and current player must reappear.
3. Collapse the left controls: the new dice, hand, cards and activity information remains visible.
4. Complete opening placements: CPU settlements/roads appear in the history.
5. Roll dice yourself: both values and total are readable, your hand updates,
   and resource payments to each player appear in history.
6. Allow a CPU to roll: dice and production details update even though it is not your turn.
7. Build and trade: your resources update and the accepted action appears in the diary.
8. Buy a development card: your own card type shows in your private hand;
   the visible public event does not reveal opponent purchase types.
9. Play a development card or resolve a robber: the match log identifies the action.
10. Close completely, restart, Continue: hand/dice and existing match log remain.
11. Tap View rolls & actions: newest first, scrollable, no other player's hidden hand disclosed.
12. Check for clipping or an obstructed island on the Samsung S24+ in landscape.

## Owner approval discipline
- NO new apk-build branch, workflow run, APK compilation or signature until explicitly approved.
- On approval: one GitHub Actions run with JVM tests and XML checks; do not automatically retry failures.
- Independently sign with Build 004/005 permanent certificate, confirm versionCode > 8,
  install over Build 005 without uninstalling, clearing data or losing the saved match.

## Outstanding after this release
- CPU domestic trading/negotiations (a proper part of classic Catan).
- Rigorous full-match and AI difficulty testing after this visibility release.
