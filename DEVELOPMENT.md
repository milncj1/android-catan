# Personal Android Catan — development

## Initial goal
Preserve classic Catan gameplay and develop an offline single-player Android app against three computer players. This is an experimental fork, not yet a verified, feature-complete game.

## Build discipline
- Work on `dev/android-modernisation`, leaving `master` untouched.
- No automated builds for normal development commits.
- Ask the owner before **every** APK build.
- After the owner approves, create a fresh `apk-build/NNN-description` branch **from the agreed development commit**. Creating that branch starts one GitHub Actions APK build.
- Keep each build branch immutable; use a new name for each approved build.
- The workflow also supports `workflow_dispatch` as a fallback, but the owner does not need to operate Actions.
- Test compilation, game rules, phone compatibility, AI, and save behaviour before promoting a release.

## Compatibility approach
Start with Android Gradle Plugin 7.4.2 / Gradle 7.6.4 / Java 17 / compile SDK 33. Preserve the original Android Support Library source for the first compatibility build. This deliberately does not claim modern Android target-SDK compliance or complete gameplay.

## Known open items
- Confirm the first Android APK builds and launches on device.
- Audit full classic rules (including Longest Road).
- Original project supports bank/port trading but not full player-to-player trade negotiation.
- Review and improve offline computer opponents.
- Implement persistent saves and stable per-device APK signing before relying on update-in-place installations.
- Attribute original MIT-licensed authors and avoid distributing third-party protected game artwork without permission.
