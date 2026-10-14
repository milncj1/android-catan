# CATAN — selectable island sizes and 1–6 computer opponents
Date: 14 October 2026
Status: requirements and architecture plan only. NOT implemented, compiled or installed.
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
