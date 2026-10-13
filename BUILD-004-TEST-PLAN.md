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
- Preserve randomised tile types, numbers and robber-on-desert at creation. Randomise all nine port trade types (one 2:1 per resource, four 3:1) for each NEW game while keeping coastal locations fixed.
- Copy the existing board ports unchanged during state updates. Added regression test covering map variety, resource counts, chits and port assignments.
- Correct two misspelled legacy UI group references.
- Critical: reconstructed the development-card and score markup after finding malformed game XML; verified structural XML tag balance and all old required game IDs.

## Acceptance checks on Samsung

1. Install **over** Build 003 using same permanent signing certificate and higher Android version code; no data cleared.
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
- Full persistent autosave/resume after Android terminates the app is still missing. The return-to-app fix preserves existing live games where possible, NOT finished match persistence.
- AI negotiation with CPU players not implemented.
- Complete CPU strength tuning and all Catan rule edge cases not independently device-tested.
- Build 004 source has not yet passed GitHub compilation or installed on a device.

## Prebuild source checks completed
- Game board and all five opening/remote backing XML layouts are structurally balanced.
- No original gameplay IDs removed other than obsolete `group_static_sidebar_menu`.
- All referenced legacy game groups and action-rail IDs resolve.
- Start-page controls that GameMainActivity expects still exist through its included layouts.
- Ordinary development commits do not run GitHub Actions.
