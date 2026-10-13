# Build 005 — Continue Saved Game repair (13 October 2026)

## Scope
Fix the Build 004 issue where the Start screen offers **Continue Saved Game**, but tapping it fails to complete the return to the playable match.

### Source changes prepared
- Restore directly into CatanLocalGame from the saved CatanGameState; do not generate a second random map and immediately throw it away.
- Add explicit confirmation that the game's ready handshake finished; when it fails within 10 seconds, show a useful message and **retain the original save file**.
- Wrap Continue's restore path in guarded error reporting so an unexpected Java exception does not silently strand the player.
- Make the human setup End Turn button use persisted settlement and road flags (previously transient UI memory was lost when app reopened).
- Add one JVM regression test for direct restart preserving board, active CPU turn, settlement and resources.
- Keep the same permanently backed-up Build 004 signing key and package name; update should install OVER Build 004 without uninstalling or clearing data.

### Acceptance on Samsung
1. **Do not uninstall or clear data:** use Update to retain the Build 004 saved match.
2. Open app; Continue Saved Game must open the original island, with exactly the same player turn, scores and placements.
3. Press Home, reopen via recents; current game still shown.
4. Fully close and reopen; Continue restores the same board again.
5. Save during your first settlement/road setup, resume, and finish setup with End Turn without repeating a placement.
6. Resume when a computer is due to act. Confirm all three CPUs continue without duplicating roads or settlements.
7. Tap New Game with an existing save, cancel confirmation, and verify Continue still works.
8. If restoration cannot proceed, a visible message should appear instead of remaining on Loading forever; existing save should remain available.
9. Check normal new-game launch and random boards are unchanged.

### Status
- Source changes ready for review.
- No Build 005 GitHub Actions run triggered; **owner approval required**.
- JVM regression tests not yet executed for this version; physical device verification still required.
