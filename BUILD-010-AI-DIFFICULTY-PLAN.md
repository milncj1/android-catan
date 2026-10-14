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
