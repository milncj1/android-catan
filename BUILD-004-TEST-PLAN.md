## Release audit — 13 October 2026

**BUILD BLOCKED — missing original permanent signing key.**
The Android signing certificate used to sign Build 002 and Build 003 remains in those APKs, but its PRIVATE key and passphrase are not available in the present project container; no matching backup was found by filename in the connected Dropbox. No code change or newly generated key can update the currently installed Android app unless signed with the same private key. Do not trigger an Actions build expecting a direct update until the old key is recovered, or the owner explicitly accepts one final reinstall with a new securely backed-up key. Never add signing keys or passwords to the public repository.

Pre-build review:
- All 76 main Java classes scanned for structural issues and all 22 layout XML files structurally checked.
- Main Android resources, string links, layout references, Gradle automatic versioning and owner-approval-only Actions triggers inspected.
- Rebuilt independent road matrices in Board copies; previous shallow-copied rows could taint autosaved states.
- Smart and Easy CPU opening decisions now use saved setup settlement/road flags rather than transient CPU fields, so they can resume halfway through setup.
- Easy CPU robber/discard loops now bounded and based on state; robber discard state copy corrected.
- Guard invalid player indices before robber discard arrays are indexed.
- Two additional JVM regression tests for copied roads and mid-CPU opening saves.
- **All source-level checks are provisional. Build 004 has not been compiled, unit-tested or run on Samsung.**
- Classic CPU negotiating trades and other engine features remain out-of-scope.


# Build 004 — UI fit, return-to-app and random islands

**Source ready for one owner-approved GitHub Actions build. NOT yet compiled or device-tested.**
Do not trigger Actions until owner approves. Use a new immutable `apk-build/004-*` branch.

## Changes staged after Build 003

- Fix the 'No GUI player' return-to-app screen by preserving the existing board on compatible display configuration changes. Explicitly explain when Android killed an unfinished game (persistent save/resume is not yet implemented).
- Narrow and optionally collapse the gameplay control rail; draw sea across the full board surface and use a clearer four-player scoreboard and turn indicator.
- Replace legacy fixed 870x550dp support/help panels with six updated native, scrollable help topics.
- Replace the old fixed-position bank/port trading overlay with a screen-sized scrollable trade dialog.
- Resize remaining development card, robber and resource-picker overlays within available screen; preserve tappable elements.
- Completely redesign the opening screen into an island-themed landscape welcome page with scrollable four-player settings, persistent Start New Game and Save Setup actions, and hidden unused network tab.
- Prevent stale saved multiplayer settings or an all-CPU setup from opening the 'No GUI' page: enforce one human plus three CPU opponents and always start local/offline.
- **Autosave every successful game move**, including each setup placement, dice roll, robber step, build and legal trade; first snapshot created after all four players are ready.
- **Continue saved game** on the opening screen: restore the board, chits, ports, resources, buildings, all player names/types, current turn/phase, private development cards and robber status. Recreate fresh CPU and human player objects and let the game thread resume from the saved turn.
- Atomic private Android file with schema validation and interrupted-write recovery; bad saves are retained without destructive cleanup.
- Confirm before starting a new game over an unfinished one; discard the save automatically once a player wins.
- Four new JVM serialization tests covering resumed turn, game board/ports, resource inventories, configuration and robber discard phase.
- Preserve randomised tile types, numbers and robber-on-desert at creation. Randomise all nine port trade types (one 2:1 per resource, four 3:1) for each NEW game while keeping coastal locations fixed.
- Copy the existing board ports unchanged during state updates. Added regression test covering map variety, resource counts, chits and port assignments.
- Correct two misspelled legacy UI group references.
- Critical: reconstructed the development-card and score markup after finding malformed game XML; verified structural XML tag balance and all old required game IDs.

## Acceptance checks on Samsung

1. Install **over** Build 003 using same permanent signing certificate and higher Android version code; no data cleared.
1a. Begin a match; place your opening settlement and road. Send the app to Home, return to it, and ensure nothing moved or disappeared.
1b. Force-close the app, relaunch, tap **Continue saved game**, and verify the board and current turn match the saved position.
1c. Progress to CPU turns and reload while one of the CPUs is due to act. Ensure all three CPU players reconnect without repeating opening placements.
1d. Repeat save/restore mid-game after a city, road, bank trade and during the robber/discard phase.
1e. Press New Game with an existing save: cancelling must keep Continue Game intact; confirming must generate a fresh island and replace the old snapshot.
2. Opening page fits in landscape, four slots and type selectors visible, main Start button always reachable; test keyboard on player name.
3. Start game with Human + 3 Smart CPU players; ensure no 'No GUI player' page appears.
4. Board is fully visible; score overlay reads correctly; collapse and expand rail and confirm taps place buildings accurately.
5. Open all six help topics and scroll to bottom; all headings and Close buttons accessible.
6. Test bank and port trade dialogs; all resource choices and Trade/Cancel visible and responsive.
7. Check development-card menus and robber/discard menus in a narrow landscape viewport; no clipped action buttons.
8. Return to the app from Home/recent apps and screen lock; game remains visible while process survives.
9. Start several new games; verify different terrain/numbers/port mixes, robber starts in the desert and one game's layout never changes mid-match.
10. Complete an opening round and several CPU turns to confirm scoring, ports and controls still work.

## Known incomplete work
- Autosave/resume is implemented in source but not yet tested on a physical phone. We still need to verify actual process-death restore and all three CPU turn transitions.
- AI negotiation with CPU players not implemented.
- Complete CPU strength tuning and all Catan rule edge cases not independently device-tested.
- Build 004 source has not yet passed GitHub compilation or installed on a device. All save tests are written, not executed.

## Prebuild source checks completed
- Game board and all five opening/remote backing XML layouts are structurally balanced.
- No original gameplay IDs removed other than obsolete `group_static_sidebar_menu`.
- All referenced legacy game groups and action-rail IDs resolve.
- Start-page controls that GameMainActivity expects still exist through its included layouts.
- Ordinary development commits do not run GitHub Actions.
