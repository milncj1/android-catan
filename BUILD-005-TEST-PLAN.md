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

### 13 October follow-up source audit
The six original Build 005 commits have been retained. Additional pre-build
fixes were made after tracing the actual framework startup code:

- `LocalGame.sendAction` now queues every action on its game Looper instead of
  dispatching it immediately on whichever human/CPU thread called it. This
  removes a real concurrency risk in the name/ready handshake.
- The game and CPU Handler references are `volatile` for cross-thread visibility.
- Resume readiness now requires both the four-player handshake and the human
  player actually receiving and drawing the restored state; the existence of a
  `board` View alone no longer counts as success.
- The original resumed save is not overwritten by initial matchmaking or by
  CPU actions before the board is displayed. Confirmation is queued on the
  game thread, and only then is the current position autosaved. A premature
  game-over likewise cannot clear an unseen resumed save.
- The ten-second recovery message includes handshake counts for diagnosis.

**Source-only checks:** Verified action queue usage, handler visibility,
resume-save protection, actual GUI readiness, diagnostics, unchanged save
filename/format and unchanged serialized `CatanGameState` UID. Basic source
brace-balance checks passed for the eight inspected core Java files.

These are static reviews, not Java compilation or an Android runtime test.
The original cause of the Build 004 device failure cannot be proven without
a real Android run/log. **No GitHub Actions or Build 005 APK has run.**

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
- Source reviewed and additional startup / save-preservation fixes committed.
- No Build 005 GitHub Actions run triggered; **owner approval required**.
- JVM regression tests not yet executed for this version; physical device verification still required.
