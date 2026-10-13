# Build 003 — Stability and rule correction

Status: **SOURCE PREPARED; BUILD NOT APPROVED OR RUN**.
Branch: `dev/android-modernisation`, create immutable `apk-build/003-stability-rules` only after owner approval.

## Critical corrections included

- Removed legacy startup IP query after Internet permission was removed.
- Defaulted to all Smart CPU opponents; improved opening placement and avoided malformed roads.
- Opening turns must include a settlement plus its connected road.
- Correctly enforce legal road/city/settlement positions and piece limits; opponent settlements block extension.
- Corrected Longest Road calculation (branches, loops, ties, and blocked intersections).
- Fixed development deck to only 25 cards. Prevent dev cards being used on the turn bought, but allow action cards before dice; limit ordinary action cards to one per turn.
- Road Building grants two cost-free road placements, Year of Plenty selects two resources, and secret Victory Point cards count once.
- Prevented robber tile from producing resources; fixed discard validation, optional empty destinations and eligible victims; stolen cards drawn uniformly from actual hand.
- Corrected 2:1/3:1 port ownership checks and valid bank trade checks, and successful custom-port return value.
- Added JVM regression tests for the rules above, together with six Longest Road edge tests.
- Updated the GitHub build workflow to combine JUnit checks and one APK in one approved run.

## On-device acceptance

1. **Launch** on Samsung without crash. Do not change source/version again until install tested.
2. App installs directly over the permanently signed Build 002, with same package and signing key.
3. Start four-player offline game: Human + three Smart CPUs.
4. Complete all eight setup placements, each settlement+road, no repeat or freeze.
5. Full island and nine ports visible; taps target correct intersections.
6. Roll, collect resources, build roads/settlements/cities, use development cards, bank/port trade, robber 7 and Knight.
7. Check no production from robber tile and fair steals; robber may move to desert or empty tile.
8. Complete a game; winner correctly awarded at 10 VP.
9. Confirm app works entirely offline and returns normally after screen lock/rotation.

## Known incomplete features — do not imply classic rules are 100% complete

- Player-to-player CPU trade negotiation and counteroffers **not implemented**.
- Bank finite 19-card resource supply and resource scarcity rules **not implemented**.
- Full offline game-state autosave/resume **not implemented**.
- Some CPU strategies still heuristic and trade decision-making is limited.
- Full integration, simulation and device testing has not yet passed.
- The visual design has compiled on GitHub but not run successfully on the phone yet.

## Build discipline

No normal GitHub commits trigger Actions. Once the owner approves Build 003, create one new `apk-build/003-*` branch. The workflow runs unit tests + compiles the APK in a single run, then deliver an APK **signed outside GitHub with the same permanent signing key used for Build 002**. APK versionCode must increase. Maintain backups of the key and passphrase separately from the repository.
