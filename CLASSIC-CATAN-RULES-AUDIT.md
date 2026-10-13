# Android Catan — classic base-game rules audit
Date: 13 October 2026
Status: Source audit and unbuilt development changes. Build 005 remains the last Samsung-confirmed installation.

## Reference
Official CATAN base-game rules and FAQ:
- https://www.catan.com/understand-catan/game-rules
- https://www.catan.com/faq/basegame

## Existing rules checked in source
- Four-player 19-hex island, correct terrain and number distributions, nine ports and anti-adjacent 6/8 numbers.
- Two starting settlements/roads per player; order 0,1,2,3,3,2,1,0. Second settlement grants adjacent resources, excluding desert.
- Settlement distance rule, road continuity (opponent settlement blocks through travel), city upgrade only from owned settlement.
- Standard construction costs and maximum 15 roads, five settlements and four cities.
- Rolling first in a normal turn; no resource production on 7 or where robber sits; one resource per settlement, two per city.
- Seven: discard half, rounded down, for players holding more than seven, then robber move and eligible victim selection.
- Development deck 14 Knights, five Victory Points, two each of Year of Plenty, Monopoly, Road Building; one action card per turn and no same-turn play.
- Largest Army at three Knights and Longest Road at five segments; awards count two points; ten points needed on the active player's turn.
- 4:1 bank, 3:1 general port and 2:1 specific port trades.

## Newly checked/corrected in development source
- Resource bank is derived as 19 minus cards in four player hands; avoids serialisation change and retains old save compatibility.
- Production shortages now pay nobody when multiple players claim an insufficient stock; if just one player claims it, they receive remaining stock.
- Bank/port purchases and Year of Plenty cannot create unlimited resources.
- Building while robber unresolved is rejected; robber discard actor must own discarded cards.
- Human GUI and CPU action paths were reviewed for turn progression.
- New regression tests: CatanClassicRulesTest, CatanResourceBankRulesTest, CatanTurnProgressionTest.

## Outstanding limitations — not yet certified complete
1. Human-to-CPU negotiation and player-to-player resource trading is missing; bank/port trades are not a substitute for classic domestic trades.
2. CPU turn progression and complete games have not yet been run on an actual Samsung for all phases. Check all three CPUs, normal turns, 7, robber, development cards and victory.
3. Automated tests added after Build 005 have NOT been executed through GitHub Actions. Owner approval required before triggering a new APK build.
4. Older saved games that acquired more than 19 of a single resource while the bank was effectively unlimited remain loadable; no data is deleted or corrected automatically.
5. Assess resource-scarcity behaviour and CPU trade decisions together when building human/CPU trade negotiation.

## Release discipline
- Development branch only: dev/android-modernisation.
- Preserve Build 005 as installed baseline; do not uninstall, clear data or change saved schema.
- Do not create apk-build/** or trigger GitHub Actions without explicit user approval.
- Run existing plus new regression tests on next approved build; if compilation/tests fail, do not automatically rerun.

## Priority
A. Run unbuilt rules regression tests in next owner-approved build.
B. Check one full match, from opening through score/victory on Samsung, including resume.
C. Implement fair and understandable CPU-to-human player trading, with safeguards against infinite offers / deadlocks.
D. Tune CPU difficulty and strategic variety only after rules and stability are confirmed.
