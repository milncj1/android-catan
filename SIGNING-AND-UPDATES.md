# Android Catan — permanent APK updates

The **owner-approved Build 004** starts a new, permanent signing identity.
It cannot be installed over Build 003 because Android prohibits updates
signed with a different certificate. Uninstall Build 003 **once**, then
install the Build 004 APK.

## Key management

**Never commit the private signing key or password to this public repository.**

The complete private key and password backups were saved on 13 October 2026
to the owner's private ChatGPT Library, under:

- `/App Production/Catan Signing/Catan_Permanent_Signing_Key_2026.p12`
- `/App Production/Catan Signing/Catan_Signing_Password_KEEP_PRIVATE.txt`

Do not put either file in a public GitHub issue, branch, Actions artifact,
pull request, or the APK itself. The keystore alias is `catan-permanent`.

Public certificate fingerprint (SHA-256, for checking delivered APKs):
`0E:07:4B:64:AA:6E:AB:48:EC:BD:19:0C:2C:F6:90:C9:8F:E2:A4:EA:92:B9:F0:75:98:E6:53:62:FB:EF:59:97`.
This fingerprint is not a password and is safe to publish.
User should also save backups in another safe, private location.

## Future update procedure

1. Commit normal source changes to `dev/android-modernisation`. No build.
2. Get explicit owner approval for a build.
3. Create one immutable `apk-build/NNN-*` branch, which triggers the single
   owner-approved workflow with local unit tests and `assembleDebug`.
4. Retrieve the resulting debug APK privately; **remove the original Android
   debug signing entries**.
5. Sign with the original `Catan_Permanent_Signing_Key_2026.p12` and
   alias `catan-permanent` using the **same** backup password.
6. Verify the signed APK and a **strictly increasing versionCode**.
7. Deliver to the owner; install **Update** directly over the last release.
8. Never delete or rotate signing identity without explaining the
   unavoidable reinstall, data loss and mitigation.

The app targets Android SDK 27 for compatibility with its original framework.
Its successfully installed earlier APKs used Android v1 (JAR) signatures;
signing may be upgraded to v2/v3 when appropriate if it keeps the same key.

Future work: modernise the Android target and support v2/v3 signing.
