# Build 032 — Code Quality & Performance (development candidate)

Date: 15 October 2026
Base: confirmed-good **Build 031**; master SHA `1c7daf9a8c0bb53c7c4caa78a4ccd36e93a322d0`.
Branch: `dev/build-032-code-quality`.
Status: **UNRELEASED until automated validation and Samsung landscape gameplay test**.

## Goals and completed source changes

1. **AI correctness:** Normal's Knight play does not enqueue End Turn before the mandatory robber process; wait for authoritative discard/move/steal updates instead. Easy, Hard and Expert strategies otherwise unchanged.
2. **Board performance:** Cache `HexagonGrid` tile objects between camera pans, zooms and purely cosmetic frames; invalidate the cache on board instance, robber location or tile selection changes. Cache tile polygon geometry, terrain/resource/robber drawables in `HexagonDrawable`; geometry and tile visuals intentionally unchanged. Remove a redundant `HexagonGrid.invalidate()` call from its render helper; the owning board view still schedules all redraws, including flash animations.
3. **Logging:** Do not stringify/log the full rules state or board on every received action/state broadcast. This is a performance and private-inventory/logging improvement. Remove a hot-path per-frame road drawing log. Game engine still creates independent player state copies; no schema or rules changes.
4. **Proved-unused cleanup:** Remove unfinished `animation/CatanLocalGame.java` placeholder (always returned false, never provided a real match), the unreachable Easy-CPU `tryMoveRobber` helper which incorrectly described desert as forbidden, and unused `checkSetupPhaseIntersection` helper in Normal/Hard/Expert CPU. Keep other legacy network/framework classes and audio until a separate dependency and licensing audit establishes they are safe to remove.
5. **Regression tests:** `CatanBuild032RegressionTest` tests Knight -> mandatory robber -> legally end turn, AI strategy invariants across four difficulty levels and Classic/Expanded/Grand, non-mutating trade evaluation, 360 deterministic Market Event round transitions and no consecutive repeats. `CatanBuild032TurnStressTest` uses authoritative GameActions across 2/4/5/7 players, completing opening placements and 30 normal rounds of rolls/robber/builds, checking resource invariants. These are **headless rules/decision tests**, not actual Android AI thread tournaments.

## Preserved contracts

- No change to `edu.up.cs.androidcatan` app ID, permanent signing key, AndroidManifest target, `CatanGameState` or `Board` serial UIDs, save paths or record format.
- No changes to the agreed player-trade consent protocol, opponent trade personalities, five optional Market Events or classic rules.
- Neither `master` nor `release/build-031-stable` is modified. User's installed Build 31 remains safe.
- Full end-to-end on-device CPU tournaments and measured FPS/GC profiling are still required before claiming the app or AI is fully tested. Do not call headless legal-action stress tests AI-vs-AI tournaments.

## Acceptance gates

- One GitHub Actions run executes Android XML validation, all JVM tests (including new headless tests), `assembleDebug`.
- If it passes, separately sign QA32 using the original PRIVATE permanent key, verify exact cert/package/version advancement. Never upload key/password to GitHub.
- On Samsung, test opening placement, road/settlement intersection tapping, map pan/zoom on all island sizes, seven and Knight robber phases, opponent trading, Market Events, Save & Main Menu, Continue, Completed Match/Career.
- **Do not merge to master** or delete/overwrite prior signed Build 31 until the user has tested the candidate and approved promotion.

## QA32 validation and signed install candidate — 15 October 2026

- Exact source snapshot: `785ecc0d916511673280b6fe8c0828e0301af0d8` on `apk-build/032-code-quality-qa`.
- [GitHub Actions run 38696346473](https://github.com/milncj1/android-catan/actions/runs/38696346473) **SUCCESS**, version/run number **32**: Android XML validation, `:app:testDebugUnitTest` (including `CatanBuild032RegressionTest` and `CatanBuild032TurnStressTest`), `:app:assembleDebug`, and unsigned artifact export all passed. Real Samsung testing and Android instrumented tests remain outstanding.
- Signed **only outside GitHub** using protected original PKCS#12 signing key; the key and password were not committed or uploaded to GitHub.
- Owner QA installer: `Catan_Build_032_Code_Quality_QA_Permanent_Signed.apk`, **27,318,205 bytes**, SHA256 `72e1f4f131f3c59c36ce3283243552ce2fd14796c7144d881ca606c2a6f71876`.
- Verified Java APK JAR signature, full ZIP CRC test and certificate SHA256 `0E:07:4B:64:AA:6E:AB:48:EC:BD:19:0C:2C:F6:90:C9:8F:E2:A4:EA:92:B9:F0:75:98:E6:53:62:FB:EF:59:97` (**same as Build 31**). Independently read signed binary AndroidManifest: package `edu.up.cs.androidcatan`, versionCode `32`, versionName `1.2-preview`.
- **Install over Build 31 only; never uninstall/clear data.** If Android refuses an update, stop and report the message rather than deleting the installed app. If owner still has unfinished match, optionally finish it or back it up first.
- Rollback: `master` and `release/build-031-stable` remain unchanged. Android generally rejects a lower versionCode over Build 32; a rollback APK would have to be rebuilt from the Build 31 source with a new higher version code and the original certificate, while preserving user data.
- **Pending owner test before stable promotion:** compare touch hitboxes, robber and Knight flow, larger islands pan/zoom smoothness, Continue Game, robber dialogs, AI trades and completed match record. No measured FPS improvement claimed yet.
