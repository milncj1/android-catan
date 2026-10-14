# Catan Android — Build 014: Gameplay Experience
**Status:** DESIGN FROZEN — source implementation and automated validation completed, 14 October 2026. FINAL INTEGRATION AND PHONE ACCEPTANCE PENDING.

## Purpose
Make the existing offline Android Catan game feel like a polished, lively board game without modifying classic Catan rules, weakening computer decision-making, cluttering landscape gameplay, or jeopardising saved games. Build 014 follows the trading/market work in 011 and the ongoing 012 work. Build 013 was deliberately skipped by owner instruction because relevant AI improvements had already been implemented in earlier builds; no separate Build 013 deliverable is required.

## Repo, branch and integration contract
- Repository: milncj1/android-catan.
- Dedicated preparation branch: dev/build-014-gameplay-experience.
- Initial base at branch creation: dev/build-011-trading @ dfbe91a766983a1aa4780e80a9899d5b08b0e56d (Build 011 QA3 source is at e21983a4097eac7b8cec3c16fc4d3c22522e19a8; later 011 branch commit is a documentation update).
- As of 14 October, no distinct Build 012 branch or commits were identifiable in the GitHub repository. **Do not assume its code has been integrated.** Obtain the exact final source ref for Build 012 before release integration and review diffs, preserving its changes. Build 013 was intentionally skipped, not lost.
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
1. Establish the final integration base by inspecting final Build 012 source. Resolve conflicts without discarding established 011 features. No separate Build 013 integration is needed.
2. Build authoritative, idempotent statistics capture and its persistence; add game-continuation tests, privacy tests and legacy-save coverage.
3. Implement in-game stats, completed-match summary, career/history views and personal records; verify all three island sizes and 2–7 total-player configurations.
4. Implement gameplay speed, animation/notification pipeline and optional audio controls, default silent, without blocking game engine.
5. Implement versioned offline export and Smart Merge import with conflict, corruption and duplication tests.
6. Run Android XML and JVM tests, conduct focused end-to-end checks and obtain owner's approval for one candidate APK snapshot. Permanently sign outside GitHub with original owner key and verify install/update identity. Test on Samsung before declaring Build 014 complete.

## Acceptance gate
- Build 012 preserved and its final source integrated/reconciled before shipment. Build 013 intentionally skipped; no separate 013 source is expected.
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

## Build 014 source implementation checkpoint — 14 October 2026

**Implemented (on isolated dev/build-014-gameplay-experience branch):**
- CatanMatchStats authoritative accepted-move ledger for dice, resources, player and bank trades, construction, development purchases and public point history; optional nullable field in CatanGameState preserving the original serial UID and Continue Game schema. Partial histories indicated for resumed pre-014 saves.
- Active gameplay duration pauses when Android backgrounds the match; CPU turns while active are counted.
- CatanCareerRecord, CatanCareerCodec, CatanCareerStore: local atomic completed match archives, duplicate-safe gameover handling, safe versioned CRC-checked backup format, explicit preview, Smart Merge only, conflict rejection and non-destructive import.
- CatanCareerUi: in-game statistics; end-match result, dice and resources charts; lifetime records, streaks, win rates, average/fastest times, best trade/score and filtering by opponent configuration and island; individual deletion.
- CatanExperienceSettings: Relaxed / Normal / Fast presentation-only pause scaling, animations switch and independently toggled audio (all music and sound effects OFF by default); preserves AI strategy.
- CatanExperienceFeedback: nonblocking short event/turn/resource messages, transient landscape banner, decorative dice faces settled on authoritative engine result.
- Programmatic setup and in-game entry points, native Android export/import picker, and JUnit regression tests for ledger copies, older saves, backup integrity and merge.
- **No changes to master or Build 012. Build 013 was intentionally skipped and has no separate implementation. No original signing credentials were uploaded.**

**Automated validation:** Two justified GitHub Actions snapshot builds:
- [Run 38592299235](https://github.com/milncj1/android-catan/actions/runs/38592299235), source SHA aeb15086e6a8feadbbce44a0be3c6937b1080cf1 — SUCCESS.
- [Run 38593222963](https://github.com/milncj1/android-catan/actions/runs/38593222963), source SHA 23cf211ecbea9184f46794c2fc6f4f3b7e6b4963 — SUCCESS, including Android XML, complete JVM test suite and :app:assembleDebug.
- CI output is a **debug/unsigned-for-delivery artifact**, not a permanently signed upgrade APK. Do not install it over an existing original-key-signed app.

**Release blockers and remaining manual QA:**
1. Build 012 remains ongoing; no distinct 012 code branch or commit was identifiable at this checkpoint. Obtain the actual final Build 012 implementation ref and incorporate it with conflict review before releasing 014. **Owner clarified Build 013 was intentionally skipped** because AI work was already handled previously. Do not search for or invent a separate 013 branch; no 013 integration gate. Build 014 was intentionally created from 011 to avoid interfering with 012.
2. Verify on Samsung in landscape: dialogs, imported backup, saved-game compatibility, statistics after pause/restart, dice/resource and computer trading behaviour, CPU speed modes, sound-off initial startup, market events, victory summary and prolonged matches.
3. More sophisticated on-board placement/achievement animations and differentiated offline sound effects require a final presentation pass; current implemented feedback is decorative dice, pulses, notices and optional system sound cues. Do not overclaim these as fully device-polished.
4. Produce a final APK with a versionCode higher than the owner's installed Builds 012/013, signed **outside public GitHub** with the original permanent signing certificate. Never recommend uninstall or clearing data. Obtain owner confirmation before release/master merge.

## Owner correction — 14 October 2026
- Build 013 was deliberately skipped; relevant AI work had already been undertaken previously. No standalone Build 013 source, APK or merge is required. Remove Build 013 as a release blocker.
- Build 012 remains ongoing and must be preserved and integrated when its finished source becomes available. Continue Build 014 on its isolated branch until that integration.

## Build 014 visual experience finish and validation — 14 October 2026

Owner asked to continue implementing 014; no additional change of specification.
- BoardSurfaceView: nonblocking, camera-aware cosmetic glow at actual confirmed settlement/city junctions, built road segments and robber destinations. Effects are UI-only, disabled when animations are off, and never affect engine results or board hit-testing.
- CatanHumanPlayer: compares previous and accepted copied game state before requesting animations; failed or unaccepted moves never trigger construction feedback.
- CatanCareerGraph and CatanCareerUi: native in-match and end-of-match public victory-point progression line charts; hidden VP development-card points stay out of the live graph.
- CatanExperienceSounds: small unique offline synthesized effects for dice, building, trade, market, achievements and victory. Background sound worker; **ALL audio remains OFF by default**. Honours phone silent/vibrate mode and volume zero; does not play unless explicitly enabled.
- CatanExperienceFeedback: reduces notification flooding during lengthy CPU sequences by leaving ordinary roads and routine development-card purchases in History; distinguishes noteworthy events, with banner replacement rather than stacking.
- CatanCareerCodecTest: added explicit checks for hidden rival VP and immutable completed ledger statistics.
- Final visual-polish validation [GitHub Actions run 38601596231](https://github.com/milncj1/android-catan/actions/runs/38601596231), snapshot apk-build/014-visual-polish-qa @ 5a076179e4176a07a4b540d3fbed1f5d68c3f0a7: **SUCCESS** (Android XML, JVM unit suite, assembleDebug). No signed delivery APK produced; source branch remains separate from owner-installed APK.
- Only remaining pre-release integration dependency is final Build 012 source. Build 013 was explicitly skipped by the owner. Avoid inventing a missing 013 branch or requiring it.
- Phone/owner acceptance remains mandatory, particularly landscape layout, end-game archival, import/export and sound/speed behaviour. Do not merge into master or release as production until 012 code is reconciled and tested.

## Build 014 extended career QA and safeguards — 14 October 2026

- Implemented date filters for personal career: All dates, Last 30 days, Last 90 days and This year. Existing board size, CPU difficulty and player-count filters remain in place.
- For very long matches, limit the visible round-by-round text history to the latest 40 entries while retaining *all* data in the stored record. Chart rendering samples long histories to bound rendering cost.
- When decoding offline career backups, validate internal IDs and reject contradictory same-ID records before showing the Smart Merge preview. Added JUnit regression for this case. Duplicate identical match records remain safe to skip during merge.
- Dice reveal can now be skipped with a tap on the dice display; authoritative engine results are unchanged.
- CI snapshot **apk-build/014-career-regression-qa** at `87ddae01c1cfe482bc8ae340ea565d9f9e6d3212`: [GitHub Actions run 38602833102](https://github.com/milncj1/android-catan/actions/runs/38602833102) **SUCCESS**: Android XML validation, JVM unit tests, and debug APK compilation.
- All changes stay on `dev/build-014-gameplay-experience` and its ephemeral QA snapshot branch. The repository's `master` branch, installed device app and ongoing Build 012 are untouched. Build 013 was intentionally skipped.
- **Not signed or installed.** A source compilation is not proof of correct landscape display, full-match stats, signed upgrade, or device behaviour. Final signed release remains gated on integrating completed Build 012 source, checking versionCode/signing identity, and human phone acceptance.

## Standalone Build 014 signed QA candidate — 14 October 2026

- Owner approved proceeding towards integration and installer. Checked all GitHub branches and pull requests: there is no identifiable completed Build 012 source or integration ref. Do NOT represent Build 012 as merged; do not overwrite its ongoing work. Build 013 remains deliberately skipped.
- Retrieved successful CI snapshot from run [38602833102](https://github.com/milncj1/android-catan/actions/runs/38602833102), build-number **25**, source commit `87ddae01c1cfe482bc8ae340ea565d9f9e6d3212`. No new Actions run or code compilation was needed.
- Signed downloaded app-debug.apk **locally, outside GitHub**, using the original permanent Catan PKCS#12 signing key from the owner's private Library. The key and password were **never uploaded to public GitHub**.
- Signed test APK: `Catan_Build_014_Standalone_QA_Permanent_Signed.apk`. SHA256 `561efc709e42ea18870a1f0b8effc36fa058824ea3a0c4e1d099b686f8d12fde`; size **27,300,016** bytes. JAR signature verification passed; ZIP CRC integrity passed.
- Certificate SHA256 unchanged from owner's existing Build 011 installation: `0E:07:4B:64:AA:6E:AB:48:EC:BD:19:0C:2C:F6:90:C9:8F:E2:A4:EA:92:B9:F0:75:98:E6:53:62:FB:EF:59:97`.
- Parsed actual AndroidManifest.xml from both APKs: applicationId `edu.up.cs.androidcatan`, Build 011 versionCode **21**, Build 014 test versionCode **25**, same versionName `1.2-preview`. This candidate is designed for an **in-place Android update over Build 011, without uninstalling/clearing data**. Android on-device acceptance still pending.
- This is a **standalone QA build of 014** based on 011, **NOT a final integrated 012+014 release**. The final release must incorporate finished 012 when its source is available, choose a higher versionCode than any installed candidate, pass tests and be signed with this original identity.
- Owner's in-progress saves and local history must be checked on device before stable approval. Never label the QA APK as full Build 012 integration.

## Build 014 QA26 — visible player-name repair, 14 October 2026

- **Reported on Samsung after installing QA25:** start screen displayed "Local player name cannot be empty" when starting a new match; there was no visible name entry, because the original GameMainActivity still scraped the legacy player-row UI hidden behind the redesigned tab/scroll layout.
- Fix: explicitly add visible **YOUR NAME** field to `game_config_main.xml`, default `You`, user-editable and remembered in local SharedPreferences. The name is authoritative for Start New Game / Save Setup.
- Make `GameMainActivity.scrapeData()` use the visible name field, normalize blank human names to `You`, and safely supply missing CPU names `Computer 1`, etc. The hidden framework rows are kept for gameplay and legacy setup compatibility; the saved-game Continue flow remains unmodified.
- Added a pure Java `CatanPlayerNames` helper with dedicated JUnit regression tests for entered, blank and missing names.
- Automated QA snapshot `apk-build/014-player-name-repair` SHA `764f8d60287d7346f72e88fb95bd779007a2508e`, [GitHub Actions run 38610233531](https://github.com/milncj1/android-catan/actions/runs/38610233531): **SUCCESS** (Android layout validation, unit tests, assembleDebug).
- Retrieved generated build-number 26 APK from GitHub Actions and locally signed it with the **original permanent signing key**, without uploading credentials to public GitHub.
- Delivered `Catan_Build_014_Name_Fix_Permanent_Signed.apk`, 27,300,949 bytes, SHA256 `d449952cfb1b4a6f267c1a30149ab60b61ca2d7d9ebb81bb9b24b0fcc26def05`.
- Signing certificate SHA256 verified unchanged: `0E:07:4B:64:AA:6E:AB:48:EC:BD:19:0C:2C:F6:90:C9:8F:E2:A4:EA:92:B9:F0:75:98:E6:53:62:FB:EF:59:97`. New APK is versionCode **26**, higher than previous installed QA25.
- Requires phone acceptance: visible field, Start New Game, Continue existing saved game, saved career data, landscape fit. **Install as update; never uninstall or clear data.** No Build 012 integration yet.

## Build 014 QA27 — name prominence + board-selection repair (14 October 2026)

**User phone report (after signed QA26):** New Game player-name entry was not obvious enough; after several rounds the game could no longer select a board intersection.

**Root cause found in source:** `CatanHumanPlayer` previously used a mutable `isMenuOpen` flag to reject every board tap. That flag could remain true after a robber/discard transition, an automatic no-card discard, or a trade menu closed by ending a turn. The previous 12-pixel drag threshold also misclassified small finger movements on high-density devices.

**QA27 fixes:**
- On the New Game card, place the existing editable player name inside a gold-outlined, numbered **"1. YOUR PLAYER NAME"** strip, with "Tap the box to edit →", bolder larger input, and select-all-on-focus. Existing name persistence and blank-name fallback retained.
- Replace sticky `isMenuOpen` with `CatanBoardTapGuard` using **current human turn + actual visibility** of discard, trade and resource-picker modal panels. Leave robber move/steal tile and junction selection enabled.
- Dismiss discard overlay whenever the authoritative game reports discard complete, or the robber phase has ended. Close robber/trade and related panels at turn end. Discard-confirm button now explicitly closes its panel instead of toggling and potentially reopening it in a race.
- Increase tap/drag separation threshold to `max(Android scaled touch slop, 12dp)`, so normal finger jitter does not accidentally pan rather than select.
- Add pure JVM regression cases covering after-seven automatic discard, trade/menu closure, CPU turns and allowing robber placement/steal board taps.
- **Verified** GitHub Actions [run 38612311374](https://github.com/milncj1/android-catan/actions/runs/38612311374) (source snapshot `97a7ed730632ee4d59f37393d74370b32275e9e1`): status **SUCCESS**, including Android XML validation, JVM unit tests and `assembleDebug`; build number/versionCode **27**.
- Retrieved the CI APK and signed **outside GitHub** with the owner's original private Catan permanent PKCS12 key. Output `Catan_Build_014_Intersection_Fix_Permanent_Signed.apk`, size **27,302,120 bytes**, SHA256 `c90be92abe6a6537736c00080b19f6caf8c5da65be52d196392555fff4e4eb2e`. JAR signature and ZIP CRC validated; matching original signing certificate SHA256 `0E:07:4B:64:AA:6E:AB:48:EC:BD:19:0C:2C:F6:90:C9:8F:E2:A4:EA:92:B9:F0:75:98:E6:53:62:FB:EF:59:97` confirmed. Temporary extracted signing secrets were deleted after signing.
- This APK is an in-place QA update (no uninstall / no clear data). **Device test still required**: enter/edit name, resume existing match, take several turns through seven/robber/trading, tap and select intersections, zoom/pan, verify no regression. No Build 012 merge performed.

## Build 014 QA28 — landscape New Game layout repair (14 October 2026)

**Owner device report after QA27:** The highlighted name row forced the bottom buttons off-screen; screenshot showed Continue saved game cut off and Start new game / Save setup not visible in Samsung Galaxy S24+ landscape.

**Root cause:** The redesigned game_config_main.xml kept name, board size, opponent count, difficulty and market events in a non-scrollable fixed-height vertical layout. A `ScrollView` holding only a *legacy hidden* TabHost consumed the remaining layout_weight space while the critical footer overflowed below the screen.

**Fix implemented:** 
- Move the **visible setup options** (numbered player name, island, opponents, difficulty, market events, hints) into a `ScrollView` with `layout_height="0dp"` and `layout_weight="1"`, so it shrinks and scrolls as necessary on landscape screens.
- Keep Continue saved game, Save setup and Start new game as **fixed siblings outside the scrolling section**, always remaining at the bottom of the New Game card rather than being clipped.
- Keep `game_config_tabhost` inflated for backward compatibility with GameMainActivity's player rows/config spinners, but set the include `visibility="gone"` so that hidden legacy UI cannot claim visible vertical space.
- Remove redundant subtitle and slightly compact the highlighted name control (50dp high, still bold and editable) to make more options visible at once.
- Add `CatanStartLayoutTest` structural JVM tests checking that the primary options are in the weighted scroll, that critical action buttons are outside it, and that the legacy TabHost remains mounted but hidden.
- Validation snapshot `apk-build/014-home-layout-fix-qa` source `d249f177fc6349d9624da729a0517d9cc0d45e32`: [GitHub Actions run 38613423805](https://github.com/milncj1/android-catan/actions/runs/38613423805) **SUCCESS** (layout validation, JVM tests, assembleDebug).
- Signed CI build **28** locally with original owner's protected permanent PKCS#12 key, without uploading it or passwords to public GitHub. Result: `Catan_Build_014_Home_Layout_Fix_Permanent_Signed.apk`, **27,302,156 bytes**, SHA256 `99947f323b1ebfae7d5c06d2a6af606a0cd3773672bad7ceaf3003ab9fbe8c5a`. ZIP integrity and JAR signature verified; original signing certificate fingerprint preserved. Verified binary APK manifest: applicationId `edu.up.cs.androidcatan`, versionCode **28**, versus QA27 versionCode **27**; both versionName `1.2-preview`.
- **Still requires owner screenshot / on-device verification.** Compile and structural layout tests do not prove final screen rendering. Install only as an update, do not uninstall/clear data. No Build 012 integration yet; Build 013 intentionally skipped.

## Build 014 QA29 — robber flow overhaul (14 October 2026)

**Owner request:** The entire robber sequence is cumbersome and needs a coherent UI. Implemented on the isolated Build 014 branch; Build 012 and master not altered.

**New user experience:**
1. **Discard (only when required):** Responsive, scrollable native Android dialog with resource names, current holdings, +/- choices, exact discard count and a disabled confirmation until the selection is legal. Holding 7 or fewer skips discard; Knight cards skip discard too. Other players can require the human to discard during their turns.
2. **Move:** Compact on-board status/action bar. Tap a hexagon (not a junction; proper polygon hit-testing even when zoomed), then press **Move robber here**. The current robber hex cannot be reused. Selections reset between robber turns to avoid stale destinations.
3. **Choose opponent:** A native dialog lists unique *eligible players* adjacent to the robber's destination, showing their resource card counts, instead of requiring the owner to identify and tap an opponent's settlement vertex. If there are no adjacent opponents, the engine automatically completes the phase with the existing legal no-target action; an eligible player with zero cards remains a legal target.
- Old 2018 fixed-image robber discard and confirm groups remain as hidden XML compatibility widgets, but are no longer displayed. The new UI uses the game's authoritative persisted robber flags, not a second transient state that could lose progress.
- Prevent repeated move/discard/steal requests while the previous one is pending; dismiss dialogs on confirmed stage changes and reset them after rejected moves.
- Added `CatanRobberSteps`, `CatanRobberUi` and focused `CatanRobberStepsTest` for >7 discard rules, exactly half, Knight skip, CPU-triggered player discards, move/steal transitions, save-state copy, and hex polygon tapping.
- CI snapshot branch `apk-build/014-robber-flow-qa` @ `d2b45c8488c78a3b665408c962b7908a5d474fab`, [GitHub Actions run 38618699307](https://github.com/milncj1/android-catan/actions/runs/38618699307): **SUCCESS** (Android XML validation, JVM unit tests, assembleDebug).
- Signed locally outside GitHub, original permanent PKCS#12 credentials kept private and working copies deleted. Delivered `Catan_Build_014_Robber_Redesign_Permanent_Signed.apk` (**Build/Version code 29**), 27,307,935 bytes, SHA256 `91cdafdc3d3a1dc585afbec005dc7419cedbad1cf167babdfae6747514a9da1d`. ZIP CRC and JAR signature verified. Certificate SHA256 matches previous signed Catan releases: `0E:07:4B:64:AA:6E:AB:48:EC:BD:19:0C:2C:F6:90:C9:8F:E2:A4:EA:92:B9:F0:75:98:E6:53:62:FB:EF:59:97`.
- APK may be installed in place over QA28; never uninstall or clear data. **Human Samsung landscape testing still required.** Specifically test human roll of seven with 8+ cards, seven with <=7, CPU-rolled seven requiring human discard, Knight card, tile selection after zoom/pan, choosing one of several legal opponents, no eligible opponent, interrupt/resume during discard and after moving, and normal board/intersection taps after the robber completes.
- Build 012 remains separate and ongoing; Build 013 was deliberately skipped. Do not call this an integrated Build 012+014 release.
