# Dark appearance

Dark mode uses a fixed charcoal/blue Material color scheme, with teal for the
second chart series. Surface levels distinguish cards, dialogs and selected
controls. Text, outlines and chart colors use semantic palette roles. Light mode
retains its existing wallpaper-derived colors on Android 12+ and its fallback
palette on older devices.

The original swipe number wheel is retained. Its neighboring values fade to 65%
opacity on dark surfaces instead of 30%; light-mode fading is unchanged.
App window resources and Firebase UI's theme use DayNight rather than fixed
light/dark themes. Window backgrounds have matching night resources.

Measurement date/time selection uses Material 3 dialogs, including clock and
keyboard time entry. Dates from the calendar API are UTC-midnight calendar values;
`MeasurementTime.kt` converts them to the user's local date/time. A draft is only
updated after the final Set action. Cancelling either step does not update it.

Verified on 2026-11-15: build, lint, 68 JVM tests and 13 Compose instrumentation
tests passed. Tests include text/control contrast, time-zone round trips, dialog
confirmation/cancellation, swipe pickers and inset handling. Visual checks used
the API 34 emulator: dark home, history, charts, scanner, date/time dialogs, plus
light home and keyboard time entry. Cloud readings were not saved or deleted.
The external Google sign-in flow was not exercised in this visual pass.
