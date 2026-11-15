# Edge-to-edge layout

App-owned activities call `configureEdgeToEdge()`. Navigation-bar contrast scrims
are disabled because a themed surface fills the window behind both gesture and
three-button navigation. `BloodPressureTheme` also keeps status/navigation icon
appearance synchronized with its light or dark colors, including the scan,
unauthorized and loading screens.

`ScreenScaffold` supplies `WindowInsets.safeDrawing` and applies and consumes its
content padding once. A top app bar owns its top inset; child layouts receive the
remaining safe content area. The drawer handles its own vertical and start insets.
Main and privacy activities use `adjustResize` for keyboard inset delivery.
The privacy page uses the same scaffold around its Android `TextView`.

This follows the Android guidance for [Compose window insets](https://developer.android.com/develop/ui/compose/system/insets-ui)
and [Material 3 inset consumption](https://developer.android.com/develop/ui/compose/system/material-insets).
Do not add unconsumed system-bar padding to individual reading screens.

## Verification (2026-11-15)

- Debug build, lint and 64 JVM tests passed.
- Eleven Compose instrumentation tests passed on the API 34 emulator. Three inset
  tests cover asymmetric cutout/navigation padding, top-bar ownership without
  double padding, and simulated keyboard insets growing and shrinking without
  adding the navigation-bar inset twice. Eight existing reading/scan UI tests passed.
- Visual checks on the same emulator covered light/dark scan screens, gesture and
  three-button navigation, a tall simulated cutout, landscape reading controls,
  scrolling, the privacy page and the main navigation drawer.
- The debug-only `ReviewActivity` now uses the same edge-to-edge setup and scaffold;
  it supports `screen=scan` and `screen=loading` without accessing cloud data.

The keyboard test injects inset sizes; it is not an end-to-end keyboard interaction
test. Android 15/16 and a physical device were not available for this layout pass.
External camera, photo picker, Google sign-in and Health Connect windows are owned
by their respective applications/libraries and are not styled by this scaffold.
