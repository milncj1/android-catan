# CATAN — selectable island sizes and 1–6 computer opponents
Date: 14 October 2026
Status: Architecture plan plus isolated Stage 1 topology generator and JUnit test sources committed. NOT integrated into gameplay, compiled or installed.
Last Samsung-confirmed-good baseline: Build 007; Build 008 compiled and delivered, not yet owner-accepted.
Development branch: dev/android-modernisation. Never touch master without approval.
Do not run a GitHub Actions build without explicit owner approval.

## Owner request
Add new-game settings for selectable board size AND number of computer opponents up to six.
Interpretation: **1–6 AI opponents plus the human = 2–7 total players**, with one human using the Samsung.
Keep private, ad-free, offline gameplay. Existing match save must remain intact.
Do not offer a setting in an APK unless it actually creates a fully playable map/game.

## Proposed clear options

| Board | Land hexagons | Grid shape (rows) | Rule status | Suggested total players |
|---|---:|---|---|---|
| Classic | 19 | 3-4-5-4-3 | Official 3-4-player board | 2–4 (2 is a house-rule duel) |
| Expanded | 30 | 3-4-5-6-5-4-3 | Official 5-6-player extension board | 4–6 (4 on enlarged board is a house-rule choice) |
| Grand | 37 | 4-5-6-7-6-5-4 | Custom board, NOT an official Catan format | 5–7 |

Opponent count: 1,2,3,4,5,6 AI players. Suggest automatic best-fit board: 1–3 AIs -> Classic; 4–5 AIs -> Expanded; 6 AIs -> Grand.
A user who wants a less crowded game may manually pick a bigger board even for fewer opponents. Do not silently switch their deliberate choice.
Keep the original default as Classic, 3 AIs so opening the app never changes an existing user's expectations.
The current 2025+ 5–6 player official rules include a paired-player system; review details before claiming exact compliance. If conventional sequential turns are used for expanded AI games, describe this clearly as a digital house-rule adaptation.
Grand board/resource bag/harbours, and seven total players are explicitly custom house rules; balance and test rather than claiming official support.

## Existing code audit — why a simple option switch cannot work
- `CatanGameState` has hard-coded four `Player` objects and fixed `setupPhaseTurnOrder={0,1,2,3,3,2,1,0}`; numerous production/resource/robber loops use `4`.
- `Board` has `boolean[19][19]` hex adjacency, `Building[54]`, `Road[54][54]`, manual 54-intersection graph and hex-to-junction map, fixed tile bag, fixed 19-tile board generator.
- `HexagonGrid` is fixed 3-4-5-4-3 with 54 manually positioned intersections. Screen scaling uses constant board bounds.
- `CatanSaveStore` requires precisely four players and names/types arrays of four. Preserve legacy `catan-current-match-v1.dat` and original serialVersionUIDs.
- `MainActivity` and `GameMainActivity` reject non-four-player configurations; initial UI scores/controls/CPU implementation assume four.
- Larger islands require correctly generated tile positions, vertex uniqueness, road edges and ports; changing the hex picture alone would allow incorrect roads/settlements.

## Implementation stages (each tested before build)
1. Introduce a pure-Java board-topology generator using integer axial hex coordinates. Produce matching vertices, edges, hex-to-vertex and vertex-to-hex maps, logical adjacency, balanced land bag and port placements for Classic/Expanded/Grand. Validate against original 19-hex 54-intersection behaviour before switching engine.
2. Generalise `Board` and graphics to topology size. Road/city/settlement costs, distance rule, coastal ports and touch snapping must work on all maps.
3. Generalise player engine (2–7), turn order (forward/backward setup), robber production/bank, achievements, CPUs, colours, scoreboard, history and victory. Preserve classic 5 settlements/4 cities/15 roads per player; scale other deck/bank contents appropriately.
4. Add new-game settings for Board and Computer Opponents with sensible recommendations and protection from unfairly cramped 7-player Classic layouts. **Do not expose nonfunctional settings**.
5. Save compatibility: read the existing 4-player save unchanged, and store chosen board/player settings in new-game snapshots without dropping legacy data. Continue must not recalculate or rerandomise a saved island.
6. Automated regression: exact hex/junction/road counts for 19/30/37; no graph self-edges; symmetric adjacency; 6/8 balancing; resource/bank/deck ratios; 2–7-player double opening placement; CPU legal moves; independent counters; saved 4-player regression; complete playable match to ten points.
7. Do not start an APK build until all implemented modes pass source reviews and unit tests; request explicit owner approval for one snapshot build with existing permanent signing key.

## Verification sources
- CATAN official: https://www.catan.com/explore-catan/catan-basegame-expansions/play-catan-5-6-players
- CATAN official rulebooks: https://www.catan.com/understand-catan/game-rules
- CATAN edition compatibility noting paired-player mechanic: https://www.catan.com/compatibility

## Open design judgement
7 total players is beyond the official game. Start the custom Grand board at 37 hexes and tune resource/port/development-card counts and match pacing during tests. If an additional very spacious option becomes warranted, discuss separately rather than silently adding another gameplay mode.

## Initial Stage 1 implementation progress
- Added `app/src/main/java/edu/up/cs/androidcatan/catan/gamestate/CatanBoardTopology.java`: an independent, immutable integer-coordinate tile/vertex/edge graph for 19, 30 and 37 tiles. Does not mutate the current Board or legacy saved-match IDs.
- Added `app/src/test/java/edu/up/cs/androidcatan/catan/gamestate/CatanBoardTopologyTest.java` to verify no duplicate roads, symmetric intersections/hex neighbours, coastlines and exact graph counts.
- Expected counts checked independently: Classic 19 hex/54 vertices/72 edges/30 coast edges; Expanded 30/81/110/40; Grand 37/96/132/42.
- Eleven static source requirements checked successfully. **No Gradle/JUnit tests or APK builds have yet been executed** for this feature; this is foundation only and should not be described as playable.
- Still needed: tile resource distributions, port layout, rendering, real game rules, 2–7 player AI/scoreboard/turn setup, variable-count save compatibility, and new-game UI choices.

## Build 009 delivered for device test — 14 October 2026
- Owner explicitly authorised implementation, and one snapshot `apk-build/009-configurable-islands` was created at source commit `ac140440aa8ad8110776d3a62e3e978e78e1d52e`.
- [GitHub Actions run 38531305808](https://github.com/milncj1/android-catan/actions/runs/38531305808), build number 12: first run **SUCCESS**, XML checks, JUnit unit tests (including 2, 3, 4, 6 and 7-player snake setup) and APK compilation all passed. No reruns.
- `Catan_Build_009_Permanent_Signed.apk`: 27,237,971 bytes. SHA-256 `3520bb41d280513000edc375270ae5dd45717257dcfaabd0725ac09bb51c42db`.
- `jarsigner -verify` returned success, all APK zip CRC checks passed, and certificate SHA-256 matches permanently signed Build 008 exactly: `0E:07:4B:64:AA:6E:AB:48:EC:BD:19:0C:2C:F6:90:C9:8F:E2:A4:EA:92:B9:F0:75:98:E6:53:62:FB:EF:59:97`.
- Expected Android package `edu.up.cs.androidcatan`, `versionCode=12` derived from GitHub run number (Build 008 versionCode 11). Original signing keystore/password temporarily materialised privately and deleted from working copy after signing. Authoritative secure originals remain in user Library.
- Original save file `catan-current-match-v1.dat` and serialVersionUIDs preserved; snapshot validation now accepts 2–7 players and 19/30/37 tiles while retaining original classic save support.
- UI provides choice of 19/30/37 tiles and 1–6 computer opponents; run a new match only after confirming existing saved game loads, because starting a new match replaces the unfinished match by design.
- **Not yet installed or accepted on Samsung.** Build 007 remains confirmed-installed working baseline; Build 008 delivered but user has not explicitly confirmed it works. No merge into `master`.
