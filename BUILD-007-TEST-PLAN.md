# Build 007 — settlement placement accuracy and movable map
Date: 13 October 2026
Status: Source edits staged for review. **Not compiled or installed.** Build 006 remains the confirmed-good installed baseline.

## User-reported faults
- The settlement UI sometimes says "built" without any piece on the map or enough resources.
- Some valid-looking three-way junctions refuse settlements.
- Nearby tap targets sometimes select a different intersection from the intended one.
- Build 006 dice/resources popup occupies too much of the upper-right island.
- Need to pinch-zoom and drag the island while maintaining accurate selection.

## Development changes
- Tapping within 88 *board-coordinate* units snaps to the single nearest junction; selection and highlighted map junction are kept in sync.
- A strong cyan/white target halo makes the chosen intersection visible before any build.
- Build buttons precheck normal turn/robber state, minimum resources, settlement distance, own-road connection and five-settlement limit; explain why not allowed.
- Requests are *pending* until the authoritative game state confirms that the selected settlement/city/road actually exists. Only then show success and clear selection.
- Rejected/not-your-turn build requests show a warning and keep the attempted selection available for correction.
- Compact `Dice & hand` control opens/closes the information panel; it starts collapsed.
- `Fit map` returns the whole island to its original fit.
- One-finger drag pans the map; two-finger pinch zooms 1.0–3.25× around the fingers; pan is clamped to island edges.
- A gesture that pans or pinches cannot also select a junction. Touch coordinates are transformed back through the same zoom/pan used for drawing.
- Camera position and HUD visibility are UI-only and do not modify saved CatanGameState files.

## Added regression tests
- `CatanSettlementPlacementRegressionTest`: a three-way owned-road junction allows a settlement; lack of resources prevents a settlement and does not change VP or board; exact payment grants settlement and deducts resources; immediate neighbouring settlement still triggers distance rule.
- Existing tests for Build 006 must continue to pass.

## Next owner-approved build checklist
1. Trigger only **one** GitHub Actions job once the owner authorises a new APK.
2. Run XML validation, all JVM tests and compile; stop on failures, do not rerun automatically.
3. Sign with the **same permanent key** as Builds 004–006; versionCode strictly exceeds 9.
4. Install as an UPDATE over Build 006, never uninstall or clear data.
5. Resume the exact saved island and make sure progress and history remain intact.
6. Collapse/open the HUD; when collapsed the island should not be hidden behind the large panel.
7. Zoom into centre and edge hexes, pan to all edges, use Fit map, then zoom again.
8. Select a junction after panning/zooming: precisely the closest one must glow, and a pan must not select.
9. Three-way road junction: build a settlement if an owned road reaches it, adjacent vertices are empty and resources are available.
10. Without 1 each of brick/grain/lumber/wool, tapping Build Settlement must show a clear cost warning, never "Built".
11. With valid placement and resources, wait for success, see the piece on the same junction, count decreases correctly and one point is gained.
12. Invalid placement near other settlements, insufficient resources, already occupied and off-network locations must explain why.
13. Road and city confirmations should also happen only after visible authoritative board changes.
14. Confirm all CPU turns, robber actions and saved-game reopening continue to work.

## Do not include
- No unrelated gameplay redesign, CPU trading or signing-key changes in this maintenance patch.
- Do not merge into the default branch until the owner approves.
