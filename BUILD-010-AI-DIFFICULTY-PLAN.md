# Build 010 — Four genuinely different computer-player difficulties

Date: 14 October 2026
Development branch: `dev/android-modernisation`
Status: **Initial code committed; static review successful. No Build 010 APK, GitHub Actions run or owner acceptance.**
Known phone baseline: Build 007 confirmed; Builds 008 and 009 compiled/delivered but phone acceptance not recorded.
Do not touch `master`. Do not create an `apk-build/**` snapshot or trigger CI without owner's explicit approval.
Never change permanent signing identity, package or serialVersionUID of existing saves.

## Requirements
One default-level selector on New Game, **Easy, Normal, Hard, Expert**.
Default to Normal, which is the existing strategic computer player. Extra players may still be set individually via the existing player rows.
All levels have exactly the same luck, starting resources, supply rules, robber restrictions, move validation and dice outcomes. The difficulty changes strategy, not rule legality.
Applies to every board size (Classic 19, Expanded 30, Grand 37) and 1–6 computer opponents.

## Compatibility-critical mapping
Historical saved snapshots include player type index `1` = `Dumb Computer` and `2` = `Smart Computer`. Keep these IDs AND labels unchanged for existing saved configurations.
Player type `3` = Hard, `4` = Expert. Human stays at `0`. The save format and player types array persist the choices.
Use `CatanAiDifficulty.fromPlayerType()` for descriptive difficulty mapping.

## Initial source implementation
- `CatanAiDifficulty.java`: four levels with persistent type identifiers and planning flags.
- `MainActivity.java`: two new AI factories, plus a match-wide new-game difficulty spinner; default Normal.
- `GameMainActivity.java`: validates types 1–4, updates opponent rows to selected level.
- `game_config_main.xml`: difficulty selector and explanatory subtitle under board and opponent controls.
- `CatanSaveStore.java`: accepts stored AI type IDs 1–4 while preserving existing index meanings and saves.
- `CatanSmartComputerPlayer.java`: Normal uses existing heuristics, Hard/Expert call enhanced strategists. Hard/Expert can take multiple legal building actions before ending their turn rather than always ending after the first accepted build. For Monopoly, advanced AI chooses the resource with greatest estimated opponent production from **visible tile numbers/settlements only**, not their private hands.
- `CatanStrategy.java`: Hard evaluates resource-balance gaps, harbour value and reachable settlement sites; Expert also values contested junctions and looks another road segment ahead. All moves remain validated by the authoritative game engine.
- `CatanAiDifficultyTest.java`: legacy IDs, no resource alterations from scoring, finite scores on every board size.
- Static review of 19 source checks PASSED. These checks are not a substitute for compilation or Samsung testing.

## Acceptance before an APK is approved
1. A saved four-player Build 007/008/009 match opens unchanged, preserves original AI types, resources, board and turn.
2. Starting a new game shows Easy, Normal, Hard, Expert with Normal preselected; changing the level updates each CPU, without changing the selected island/opponent count.
3. Test Classic 19 + three Easy opponents: straightforward and occasionally random.
4. Test Expanded 30 + four Normal opponents: original strategic decisions and comparable to previous behaviour.
5. Test Grand 37 + six Hard opponents: each CPU can legally place two starting settlements, pursue sensible sites and finish turns without loops.
6. Test Grand 37 + six Expert opponents: legal moves and better strategic expansion/blocking, including resource variety and sensible cities/development cards.
7. If configured individually, a mix of difficulty types survives save / close / continue correctly.
8. No difficulty gets extra resources, changed dice, a scoring bonus or secret hand knowledge.
9. Run JVM unit tests for four difficulty settings on all island sizes, and catch CPU illegal-action loops. Confirm one complete match can reach legal victory.
10. Preserve signing and use a higher versionCode to install in place; request ONE owner-approved APK build only after source is ready.

## Next refinements before final quality rating
- Source-static tests passed; compile/Gradle tests still unrun for Build 010.
- Verify mixed-difficulty play and game saving on actual device, not just compile checks.
- Trading among computers/with human is still reserved for Build 011; do not claim Hard/Expert can negotiate yet.
- Difficulty strength needs practical balancing over matches, not a guarantee Expert will always win.

## Further development review — 14 October 2026
- Added `CatanAiPlanner.java` to guide Hard/Expert settlements, roads, robber targets, bank trades and Monopoly choices using public information, not opponents' secret resource holdings.
- Connected advanced CPU players to multi-action turn planning and limited road construction to 15 pieces, including free-road action handling.
- Corrected the original 2018 network-player fallback: the final player type was treated as a remote participant. With Expert as the last type, this wrongly disabled its player-name field. All local AI name fields now remain editable and nonblank names are validated.
- Removed an accidental duplicate difficulty spinner from the XML; exactly one selection control and help label remain.
- Additional 11 static source compatibility checks passed: one spinner, four levels, historic type indices, save validation, CPU modes and strategic choices.
- **No Android compile/test build triggered for Build 010.** On-device strength and turn reliability still unverified.

## Easy and Normal reliability refinement — 14 October 2026
After owner requested continued Build 010 development and asked specifically what happens to Easy/Normal:
- **Easy:** Replaced the original duplicated random build branches with one legal-move selector. It now chooses randomly among affordable cities, settlements, and valid connected roads; uses no non-existent road endpoints; occasionally passes by design rather than stalling on rejected moves.
- Easy avoids the desert and occupied robber tile when moving the robber; can buy random development cards, play valid action cards (Knight, Monopoly, Year of Plenty, Road Building), construct both granted free roads, and occasionally make a legal 4:1 bank trade if no immediate build is possible. No extra resources, dice adjustment or private hand peeking.
- **Normal:** Retained the pre-existing strategic settlement/road heuristics and its single planned action per turn; improved bank trades by choosing a missing resource for a particular affordable build, avoiding same-resource/empty-bank trades. Its existing playable development cards are now considered BEFORE another new card purchase.
- **Hard/Expert:** Continue to use their stronger public-information decision evaluator, multi-build turn logic, and the shared bank/robber validation from the engine. Both must be tested for illegal action loops before owner acceptance.
- Additional pure JUnit regression cases now cover legal advanced-road selection on all 3 islands, useful 4:1 exchanges and valid robber movement avoiding desert/current tile.
- **11 static source checks passed. This is not a compile/test or phone acceptance result.** No `apk-build/**` branch or CI run triggered for Build 010.
