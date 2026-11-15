# README screenshots

Captured on 15 November 2026 from the running debug app on a Pixel 3a API 34
emulator. These are real UI captures, not mockups. `ReviewActivity` supplies
synthetic readings (124/79, 119/76, and 127/82); no real account or health history
appears in the images. The preview has a simplified app bar and does not access
Firebase or Health Connect.

| File | Preview screen | Theme |
| --- | --- | --- |
| `home-light.png` | `home` | Light, non-dynamic fallback colors |
| `home-dark.png` | `home` | Dark |
| `history-dark.png` | `history` | Dark |
| `trends-dark.png` | `graph` | Dark |
| `scan-dark.png` | `scan` | Dark |
| `date-dark.png` | `home`, then open the Measured button | Dark |

The source captures are 1080×2220. The README PNGs are scaled proportionally to
540×1110 for a lighter README. No content, readings, or UI elements were retouched.
The calendar screenshot demonstrates date selection; it is not a saved measurement.

## Reproduce

Install a debug build with the scanner model prepared as described in the main
README. Use a development emulator; `-S` stops the app before launching a fresh
preview so its screen/theme extras take effect. It does not clear app data.

```sh
adb shell am start -S -W \
  -n com.artemidius.bloodpressure/.ReviewActivity \
  --es screen home --ez dark true
```

Wait for the screen to settle, then capture it:

```sh
adb exec-out screencap -p > /tmp/home-dark.png
# macOS; an equivalent image resize tool can be used on other platforms.
sips --resampleWidth 540 /tmp/home-dark.png \
  --out docs/screenshots/home-dark.png
```

Repeat with the screen/theme combinations in the table. For `date-dark.png`, open
**Measured** in the dark home preview before capturing, then cancel the dialog.
There is no need to save a reading or connect an account.

`home.png` and `trends-dark-large.png` are older polish-review captures retained for
that review's history. They are not used in the current project README.
