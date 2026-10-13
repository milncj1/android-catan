# Build 006 — playable match information and classic rules
Date: 13 October 2026
Status: **Build compiled, JVM tests passed and permanent-signed APK verified. Samsung acceptance outstanding.**
Confirmed installed baseline remains Build 005, whose full close/reopen Continue Game was verified by the owner.

## Why this matters
The user cannot enjoy a match without knowing which dice were rolled, what resources
are in their hand, what development cards are owned/played, and what the CPUs are doing.

## Changes to verify
- Each non-desert terrain hex has a much larger, labelled (BRICK, GRAIN,
  LUMBER, ORE, WOOL) resource icon on a contrasting badge, within the
  hexagon; number tokens remain separate and legible.
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
12. On the Samsung S24+ in landscape, confirm that each enlarged terrain symbol
   and label is recognisable, and that number tokens, robber, roads and
   settlements are still legible without overlap or clipping.

## Owner approval discipline
- NO new apk-build branch, workflow run, APK compilation or signature until explicitly approved.
- On approval: one GitHub Actions run with JVM tests and XML checks; do not automatically retry failures.
- Independently sign with Build 004/005 permanent certificate, confirm versionCode > 8,
  install over Build 005 without uninstalling, clearing data or losing the saved match.

## Outstanding after this release
- CPU domestic trading/negotiations (a proper part of classic Catan).
- Rigorous full-match and AI difficulty testing after this visibility release.

## Approved Build 006 result — 13 October 2026
- Source snapshot: `apk-build/006-gameplay-visibility` at `468d3efd1871398532cda10c5cfbfc9cb491b4e8`.
- One approved GitHub Actions run: https://github.com/milncj1/android-catan/actions/runs/38411569063 — **success**, first attempt.
- XML validation, JVM unit tests and APK compilation passed.
- Signed privately using the permanent Build 004/005 PKCS12 certificate; verified same public SHA-256 fingerprint.
- Verified APK archive integrity and Android package `edu.up.cs.androidcatan`; Android versionCode **9** vs **8** for Build 005.
- Signed APK SHA-256: `a77416fc5ffc6da52a2adc937e4896b7d176adbc357df4ccb2bdf8e6fe21c5b5`.
- User must **install over Build 005**, not uninstall or clear data. Full game/device UI, retained saves and close/reopen behavior still need physical Samsung testing.
- Source changes made to `dev/android-modernisation` after the immutable build snapshot are not included in this APK.

