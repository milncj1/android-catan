# Catan Android — Build 014: Gameplay Experience
**Status:** DESIGN FROZEN — owner approved, 14 October 2026. PREPARATION ONLY; implementation has not begun.

## Purpose
Make the existing offline Android Catan game feel like a polished, lively board game without modifying classic Catan rules, weakening computer decision-making, cluttering landscape gameplay, or jeopardising saved games. Build 014 follows the trading/market work in 011, the ongoing 012 work, and the owner-reported completed 013 work.

## Repo, branch and integration contract
- Repository: milncj1/android-catan.
- Dedicated preparation branch: dev/build-014-gameplay-experience.
- Initial base at branch creation: dev/build-011-trading @ dfbe91a766983a1aa4780e80a9899d5b08b0e56d (Build 011 QA3 source is at e21983a4097eac7b8cec3c16fc4d3c22522e19a8; later 011 branch commit is a documentation update).
- As of 14 October, no distinct Build 012 or 013 branch or commits were identifiable in the GitHub repository. **Do not assume their code has been integrated.** Obtain exact final source refs for both builds before the integration stage and review diffs, preserving both.
- Never push into, reset, or merge into master unless the owner specifically approves it. Do not modify the live 012 work or completed 013 work.
- Development branch creation and documentation do not trigger APK CI. Never create an apk-build/* snapshot until its run is justified/owner approved.
- Never change application ID edu.up.cs.androidcatan, signing identity, or the installed user's app data. Install updates over prior APKs, not via uninstall. Version code must exceed any version installed at release time; QA3 code was 21.
- Use ChatGPT/GitHub, avoid unnecessary Codex and GitHub Actions runs. Discuss scope changes with owner first.

## Frozen feature scope

### A. Polished and lively presentation
- Tasteful, brief animations: rolling dice and reveal of the authoritative result; resource-producing hex/resource collection feedback; road/settlement placement; city upgrade; robber movement; Longest Road/Largest Army milestones; final victory celebration.
- Effects must never determine or change rolls, resource allocations, actions, trading outcomes or scores; render state that the game engine has already accepted.
- Nonblocking where possible; tap to skip an individual animation; Reduced Animations toggle for minimalist presentation.
- Normal pacing should target approximately 0.7 seconds for dice feedback and 0.3–0.5 seconds for typical construction; use these as design targets, not delays in the engine.
- Preserve landscape usability: no permanent gameplay overlays, covered controls, tiny modal buttons or mandatory wait for decorative sequences.

### B. Adjustable pace
- Relaxed / Normal (default) / Fast; configurable during a match and remembered in Settings.
- AI strategies, dice probabilities and game rules are identical at every speed. Change only artificial pauses, presentation durations and notification timing.
- Even Fast mode retains brief signals for significant events; queues must coalesce/skip stale presentation when multiple computers act quickly.

### C. Clear gameplay feedback
- Short, nonblocking contextual notices for useful activity (e.g. new city, resources gained, Longest Road/Largest Army ownership, surprise market events, end of events, 'Your Turn').
- Retain the detailed existing match History as the permanent activity record. Avoid repeated dialogs and avoid flooding when several CPUs act sequentially.
- All notices use publicly permitted information; never disclose opponents' hidden development cards or exact private resource inventories.

### D. Audio, user choice and defaults
- **ALL AUDIO OFF BY DEFAULT**, both sound effects and any existing background music, including on upgrade when a new preference has never been set. No unwanted startup sound.
- Optional independently enabled sound effects (dice, construction, trade confirmation, major event, victory) and music; obey system audio and silent settings.
- Existing music features must be preserved but made subject to the default-off controls. Keep all audio assets available offline.
- Animations ON by default; Normal pace default; user-selected settings persisted locally.

### E. Accurate current-match statistics (accessible during play)
- Record authoritative dice distribution (2–12; number of sevens), resource production/acquisition by type, successful player-to-player deals, bank/harbour transactions, roads, settlements, cities, development-card activity, completed rounds, gameplay duration, milestones, victory-point progression and relevant match setup (island, opponent count, difficulty).
- Distinguish player-to-player exchanges from bank/harbour exchanges. Don't double-count cards moving among players as newly produced resources.
- Capture only accepted, authoritative game actions; preserve event ordering and avoid double counts after Save/Continue, UI recreation or repeated callbacks.
- Statistics remain readable from an unobtrusive Statistics entry in the landscape match controls; no permanent stats sidebar.
- Mid-game view uses only public rival facts and the human's own private facts. Score progress for rivals must not expose hidden VP cards. Final display may reveal truthful end-game standings once the match has ended.
- Build 014 begins tracking new statistics on imported earlier saves without inventing historic events.

### F. Match completion and career
- Automatically show a scrollable landscape Match Summary after finishing, with overview/standings, dice & resource charts, trades & building, and victory-point progress.
- End-of-match record has a stable unique ID, result, participant configuration, scores, date, duration, rounds and underlying statistical data. Completion must be idempotent (exactly one career result per completed match).
- A 'My Catan Career' main-menu view offers completed-match history (newest first), individual match details and lifetime wins, losses, win rate, best streak, best/fastest results and total trades. Filters: difficulty, island size, opponent count, dates where practical.
- Only **completed** games count toward career win/loss. Loss to a CPU counts as human loss; abandoned unfinished game does not. Deleting a completed test game is permitted with confirmation and recalculates derived totals; no arbitrary cap on match count.
- Record current match statistics incrementally, including active gameplay time that pauses while the app is not being played. CPU turns while the app is active count as active playtime.
- Preserve accurate save/continue state for unfinished matches and unchanged classic Catan gameplay.

### G. Career backup and restore
- Manual offline export/import through Android's standard file picker, without account, subscriptions or network requirement. Backup includes completed match records, full historical statistics, performance configurations and progression; personal aggregates may be recomputed.
- Backup of career history is **separate** from Continue Game; an import must never reset or replace the current unfinished saved match.
- **Smart Merge ONLY.** Before import, verify schema/version/integrity and preview total matches, new matches and duplicates. Import new unique IDs; skip byte-identical existing IDs; report ID conflicts with different payload and do not overwrite silently.
- Imports are transactional/atomic: corrupted or incompatible data cannot partially replace existing records. Do not offer Replace All. Recalculate lifetime metrics from deduplicated completed records after success.
- Store locally. No silent cloud sync. Backups can be saved by the user into their own chosen device location.

## Layout contract
- Keep board largest and readable in Samsung Galaxy S24+ landscape.
- Existing game sidebar and History stay familiar. Access Statistics and Game Speed without a permanent overlay.
- Main menu gains 'My Catan Career'. End-of-match automatically opens summary with scrollable tabs/sections.
- Existing trade dialog (Players / Bank & Harbours), market indicators, market toggle, smart AI settings, and all gameplay actions are preserved.

## Storage and compatibility contract
- Reuse existing atomic, offline match-save principles; do not break legacy serialVersionUIDs or existing Continue Game data.
- New statistic tracking can use independent versioned local storage keyed to the live match and a unique match ID; lifecycle should be reconciled with accepted engine actions and atomic match saves.
- Ensure no loss of finished statistics if the app closes immediately after victory and no duplication if a victory callback is delivered twice.
- Backups must be schema-versioned and reject malicious, oversized or unexpected data safely.

## Engineering work packages — after preparation
1. Establish final integration base by inspecting completed Build 013 and final Build 012 source. Resolve conflicts without discarding established 011 features.
2. Build authoritative, idempotent statistics capture and its persistence; add game-continuation tests, privacy tests and legacy-save coverage.
3. Implement in-game stats, completed-match summary, career/history views and personal records; verify all three island sizes and 2–7 total-player configurations.
4. Implement gameplay speed, animation/notification pipeline and optional audio controls, default silent, without blocking game engine.
5. Implement versioned offline export and Smart Merge import with conflict, corruption and duplication tests.
6. Run Android XML and JVM tests, conduct focused end-to-end checks and obtain owner's approval for one candidate APK snapshot. Permanently sign outside GitHub with original owner key and verify install/update identity. Test on Samsung before declaring Build 014 complete.

## Acceptance gate
- Builds 012 and 013 preserved and their actual source integrated/reconciled before shipment.
- Existing Build 011 trading, proactive CPU offers, events, settlements, victory rules and saves behave as before.
- Dice/resource/trade/construction stats reflect accepted engine actions across Save/Continue.
- Mid-game stats do not leak private opposing hands or private VP.
- End-game result and history saved once; career totals and filters reliable.
- Import duplicate handling and conflicting IDs behave exactly as Smart Merge specifies; unfinished match unaffected.
- Speed modes change only presentation; changing modes mid-match is safe.
- No unexpected sound on first launch; existing audio can be enabled intentionally.
- Touch targets, dialogs, notifications and animations fit Samsung Galaxy S24+ landscape.
- Build test passes and owner confirms actual device behaviour before stable release.

## Change control
Any new rules, substantial extra features, analytics expansion, achievements, networking, cloud sync, or a full visual redesign require explicit owner discussion/approval. This file freezes the approved scope; implementation/acceptance outcomes will be recorded separately.
