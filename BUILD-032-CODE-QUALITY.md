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
