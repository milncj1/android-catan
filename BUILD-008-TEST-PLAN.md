# Build 008 — Development card collection and clear two-round opening
Date: 13 October 2026
Status: **Compiled, unit-tested and permanently signed successfully; awaiting Samsung install and owner testing.**
Working installed baseline: **Build 007**, owner-confirmed 13 October. Master/default branch untouched.

## Features requested
The Development control was buried in an old dropdown. Replace it with an attractive, scrollable card collection that does not change classic CATAN gameplay. The owner additionally reports seeing only one opening settlement; clarify the classic two-pass, reverse-order setup and ensure the second placement remains possible.

## Implemented development UI
- New `CatanDevelopmentCardsUI.java` presents five coloured custom-vector illustrated card types: Knight, Victory Point, Year of Plenty, Monopoly, Road Building.
- Show descriptions, quantity owned, status (not owned / newly bought / ready to play / secret VP), and a playable-only action button.
- Buying is a distinct, prominent action with visible cost of exactly 1 wool, 1 grain and 1 ore. Disabled if unaffordable, during setup, before rolling, out of turn, during robber, while already drawing, or when the finite deck is depleted.
- Development cards can be inspected during any player’s turn; Buy and Play remain strictly restricted.
- New purchases are confirmed **only** by the authoritative private new game state, checked as a multiset difference to handle duplicate cards. A private illustrated reveal is shown. Buying never announces the card type to other players.
- Newly bought action cards cannot be played the same turn. One action card per turn; secret VP points remain automatic, not played as an ordinary card.
- Existing core engine and random 25-card deck unchanged; no change to serialised save objects or signing.
- Left Development button opens new screen; expanded hand summary is also tappable. Existing legacy group/code kept until new APK passes phone tests.

## Opening placement
- Classic snake order remains **Human, CPU1, CPU2, CPU3, CPU3, CPU2, CPU1, Human**.
- One free settlement AND adjacent road per placement, twice per player. Human presses **End Turn** after the first pair so the computers can complete their placements before the human's second pair.
- Map turn indicator now displays "Opening 1/8" through "Opening 8/8".
- Specific instructions show first/second settlement, adjoining road, computer waiting and **End Turn** after each pair.
- After a confirmed initial road the message explicitly tells the human that the second free placement happens after the CPUs.
- Previously existing `CatanTurnProgressionTest` already exercises all eight placements and human's two settlements. Enhanced to explicitly assert human has only one after first round and returns for the second.
- `CatanDevelopmentCardPurchaseTest` newly checks 25-card deck composition, forbidden setup/underfunded purchases, exact payment, one secret new card, same-turn card restriction and next-turn availability.

## Important phone acceptance checks (after a separately approved build)
1. Install only as **update** to Build 007 with the permanent existing signing key and versionCode > 10; never uninstall/clear data.
2. Continue the exact saved game. Hand, map, roads, scores, resource counts and match log must persist.
3. Tap Development even if not your turn: view illustrated catalogue; no illegal buy/play.
4. During opening setup: Human first settlement + attached road, tap End Turn; watch all CPU placements incl. CPU3 consecutive two turns, then Human second settlement + attached road.
5. Confirm each player has exactly two starting settlements and two roads, and the second settlement awards its adjacent starting resources only.
6. Development purchase requires 1 wool, 1 grain, 1 ore and rolled dice. No card purchase on setup, during robber, without funds or after deck is empty.
7. A successful buy consumes those three resource cards once and reveals exactly one random illustrated development card privately. CPU purchases remain undisclosed.
8. Collection shows duplicate counts, full descriptions and newly purchased action cards disabled until a later turn; VP is secret and already counted.
9. Later-turn Knight, Road Building, Monopoly and Year of Plenty actions correctly trigger existing dialogs/board actions, without allowing multiple action-card plays per turn.
10. Fully close, relaunch, Continue: bought cards, game state and VP remain intact.
11. Confirm the board zoom/pan and collapsible match HUD remain working; no obstruction or clipping in landscape.

## Approval discipline
No `apk-build/**` branch and no GitHub Actions build without an explicit owner request. Once requested: one workflow run, stop on failure instead of retrying automatically, sign privately with original PKCS12 keystore, check signature/version, share APK for Samsung acceptance. 

## Owner-approved Build 008 — 13 October 2026
- User explicitly authorised building Build 008; created exactly one snapshot branch `apk-build/008-development-cards` from dev commit `987aa72a54e87147107066e464f548515ffc5c36`.
- [GitHub Actions run 38461614030](https://github.com/milncj1/android-catan/actions/runs/38461614030), run number **11**: first run SUCCESS. Android XML checks, `:app:testDebugUnitTest`, `:app:assembleDebug` all passed; no reruns.
- APK locally signed using the SAME permanent PKCS#12 key/certificate as Build 007; `jarsigner -verify` succeeded; ZIP CRC verified.
- App ID `edu.up.cs.androidcatan`; versionCode **11** (prior Build 007: 10), versionName `1.2-preview`, per `app/build.gradle` reading GitHub Actions run number.
- Output: `Catan_Build_008_Permanent_Signed.apk`, 27,226,801 bytes, SHA-256 `fe37574aabacfb9fba4a5e34b7df126a80d7923af6597f45ca5ecf7a5cccde7e`.
- Sign key and plaintext password were materialised privately for signing and erased from local temporary signing workspace afterwards; retained in the secure user's Library.
- **Not yet device-installed/tested.** Build 007 remains the last confirmed-good installed version. No default-branch merge.
