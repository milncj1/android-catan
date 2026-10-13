# Visual/CPU milestone: Build 002 test plan

Current state: **source changes only; not compiled or tested on a phone.**
No further GitHub Actions build may run without the owner's approval.

## Included in this batch
- Fit the original fixed-coordinate island inside the available board area, including port margins.
- Convert taps back into original board coordinates, so building and robber placement use the same logic.
- Paint ocean, sand rim and illustrated land hexes with Kenney CC0 details and readable number tokens.
- Give roads stronger visual contrast and restyle existing (unchanged) sidebar actions.
- Improve the existing Smart CPU's settlement, city and road placement using legal-move scoring; fix least-held-resource selection.
- Remove internet permissions for offline single-player use.

## Mandatory on-device checks before this may be called stable
1. Full map and **all nine ports** visible without clipping in landscape.
2. First settlement and road placed exactly where tapped, including corners and edges of the island.
3. Starting CPU players each place two settlements and roads; no looping or freezes.
4. Roll production, 6/8 tiles, robber movement, building controls and resource totals still work.
5. Later city upgrades and road extensions use actual legal moves; no illegal moves or indefinite CPU turns.
6. Robber and cards continue functioning.
7. Check all four scores, active-player information, disabled action buttons and trade/menu dialogs.
8. Complete one full game without a crash.

## Known unfinished work
- Actual player-to-CPU negotiations are still missing in the original implementation; only bank/port trading exists.
- The improved Smart CPU is heuristic, **not** the external MCTS or neural-network engine yet.
- Proper three-tier difficulty, automatic saves, better buildings/ports and full Catan rules validation remain.
- APKs from hosted debug builds may use changing signing keys. Do not assume install-over-old-version succeeds or that saves are preserved. Establish stable release signing before persistent user data is relied upon.
- Build 002 source has *not* been compiled or test-played yet.

## Licensing
- Original source code retains its MIT copyright requirements.
- Terrain decorations are Kenney CC0 with details in KENNEY-ASSETS.md.
- This is a personal noncommercial adaptation. No CATAN trademark or official art rights are implied.
