# Polish and reliability pass — 15 November 2026

## Implemented

1. Confirmed saves, explicit pending/error feedback, stable IDs, duplicate-tap protection.
2. Null-safe one-shot CSV export on an IO dispatcher, with visible failure feedback.
3. Unified navigation with a conditional Back handler; removed the accumulating success route.
4. Account-scoped Firestore subscriptions with cancellation, cache/pending indicators, retry, and clear state on account change.
5. Health Connect availability/permission refresh, explicit preference, handled failures, matching measurement time, stable IDs for updates/deletes.
6. Single-photo camera/gallery scanning, bounded decoding, EXIF orientation, temporary capture cleanup, cancellation and reset between visits.
7. Spatial pressure/pulse ranking with permissive best-guess acceptance, progress feedback, retry on recognition failure, automatic draft update with toast, no reading logs.
8. Both graph series, numerical pressure scale, real time spacing, date filters, empty/single-point handling, dark-theme colors, textual equivalents.
9. Original custom swipeable number pickers, labels, units, accessibility range controls, consistent validation, scrollable layouts and rotation support.
10. Newest-first history with complete dates, editable measurement time, edit/delete confirmation, record IDs preserved.
11. Correct CSV MIME type, header/units, complete UTC timestamps.
12. Direct Scan/History/Trends actions, working Health Connect settings, inline save feedback.
13. Actual data-use information, Google guest linking, explicit account/cloud deletion controls, Firebase Analytics removed, backups excluded.
14. Unit/UI/security tests, consolidated theme, unused screens/resources/dependencies removed, themed launcher icon, setup documentation.

## Live Firebase remediation

A read-only review found the existing Firestore rules allowed unrestricted reads and writes. The only top-level collection was `blood_pressure`. Replacement rules were validated, deployed explicitly to `bloodpressure-515bd`, and read back to verify the published source. Existing readings were not migrated or deleted.

The initial MCP deployment helper reported success but the active project context was inconsistent afterward. Final deployment used the Firebase CLI with an explicit project ID and was independently verified. The shared MCP environment was restored afterward.

## Verification and limits

- JVM tests cover the main data and permission state transitions.
- Android UI tests use synthetic data on the emulator; they do not submit medical readings or delete real accounts.
- Security rules are tested against a local demo Firestore emulator.
- The app builds and lint is checked; dependency-upgrade advisories are not blanket-upgraded during this behavior change.
- Physical-camera recognition across actual monitor models, Health Connect installation/revocation on multiple Android versions, and Google linking/reauthentication still require device-level checks.
- The privacy page describes implemented behavior. It is not a completed store/legal submission; actual owner contact, distribution audience and store declarations must be supplied before publication.
- Guest-to-existing-account collisions preserve the guest session and show recovery guidance; automatic cross-account merging/import is not implemented.
- Offline cloud writes are Firestore-managed. Health Connect retries are explicit through Save, rather than a background reconciliation service.

## Device acceptance checklist

- Enter a reading, go offline, retry, reconnect, and verify exactly one cloud document.
- Cancel export; export again and verify headers/year/timezone.
- Capture/select/cancel an image; rotate while the picker is open and while recognizing; check a real monitor photo updates the draft; retry an unreadable image.
- Enable Health Connect, revoke permission externally, return, disable copying without another prompt, and retry a failed copy.
- Edit a linked record and verify Health Connect updates rather than duplicates.
- Rotate and increase font size; inspect input, settings, history, graph and permission/privacy screens in light/dark mode.
- Link a disposable guest account, cancel sign-in, test an existing-account collision, then exercise explicit deletion with disposable data only.

## Verification results

- 64 JVM tests passed (including startup defaults, cache-to-server initialization, draft preservation, account switching, photo recognition state/cancellation, and 35 spatial extraction regressions).
- 15 Android instrumentation tests passed on the API 34 emulator, including actual detector inference, EXIF decoding, scan progress/retry and direct input updates.
- 5 Firestore rules tests passed against `demo-blood-pressure`, including after the test SDK/gRPC update.
- Debug assembly and lint passed; dependency/version advisories remain.
- `npm audit` for the isolated security-test tooling reported zero vulnerabilities.
- Input and a dark-mode chart at 1.6× font scale were visually inspected using synthetic readings; screenshots are in `docs/screenshots/`.
- During the initial polish pass, Gradle instrumentation removed the target installation; the debug APK was reinstalled afterward. Subsequent tests use manual APK installation and instrumentation to preserve the installation. A previous guest session on that emulator may need recovery/linking through an existing account. No cloud readings were deleted by these tests.

## SDK references

- [ML Kit data disclosures](https://developers.google.com/ml-kit/android-data-disclosure): diagnostics are disclosed separately from the removed Firebase Analytics integration.
- [Health Connect metadata](https://developer.android.com/health-and-fitness/health-connect/metadata): stable client IDs support updating linked records.
- [Firebase anonymous authentication](https://firebase.google.com/docs/auth/android/anonymous-auth): guest account upgrade/recovery context.

## Picker restoration

Restored the original Compose wheel implementation, animation, faded neighbors and dividers, with the original side-by-side layout and 120/80 starting values. Retained the updated surrounding UI, validation, units, accessibility semantics and save-in-progress lock. The picker range remains 1–300 so scanned or historical readings are never silently clamped to the old narrower ranges.

## Startup reading values

On app startup, initialize the wheels from the latest saved measurement for the current account; use 120/80 for an empty history. An empty local cache waits for the server. Existing cached readings can initialize the wheels offline. Once the user edits/scans/saves, later history updates cannot overwrite the draft. Only the pressure values are reused: measurement time and document identity remain those of a new reading.
