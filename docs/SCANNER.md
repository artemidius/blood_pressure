# Monitor recognition

The camera uses [7-segment-digits-Scanner](https://github.com/artemidius/7-segment-digits-Scanner)
at the pinned JitPack release `v0.2.0-alpha.1` (commit
`af2a1512bc9d3645bd0b7f7f637584d895105d26`). This release combines a seven-segment
LiteRT detector with bundled ML Kit Latin OCR for printed labels. Its AAR requires
compile SDK 37. The app's target SDK remains 36 and minimum device API remains 29.

## Model setup

The library AAR does not contain detector weights. Prepare these before building:

```sh
python3 scripts/prepare_scanner_model.py /path/to/seven-digits-export
```

The export must contain `detector.tflite`, `detector.json`, and `MODEL_LICENSE`.
The script verifies the model against the metadata's SHA-256 and copies all three
into `app/src/main/assets/seven-digits/`. Gradle also checks these assets and the
checksum before building. The model binary is Git-ignored but packaged in the APK;
it is not downloaded at runtime. The metadata and license are retained in source.

The current local build uses the prepared SMART-BP export from the scanner project:

- Source: Nikookar et al., [BP Image Digitize v1.2.0](https://zenodo.org/records/21480466),
  upstream commit `c71450adf67247e41674394e8f26ae873e040345`.
- Source weights SHA-256: `7b01cce5eaa6d08f9150fa4769bf11368f7bb6717b3983c1e904b801ac21bc7d`.
- LiteRT export SHA-256: `a795188f8dce78f2a1409e0dfca9a845ac1f4afedb9f192ad222550f650420d6`.
- Engine license: Apache-2.0. Model license: **AGPL-3.0**, separately retained as
  `MODEL_LICENSE`. The engine's license does not relicense the weights. Distribution
  of a model-bearing APK must account for the model's terms and upstream licensing
  notes; see the [upstream model documentation](https://github.com/artemidius/7-segment-digits-Scanner/blob/v0.2.0-alpha.1/docs/MODELS.md).

To produce an export from scratch, follow that upstream model documentation. A
different compatible detector needs its own provenance and device validation.

## Photo recognition flow

"Scan monitor" opens a choice of Camera or Gallery. Camera uses the system camera
with `TakePicture` and a FileProvider URI scoped to `cache/scan-photos`. Gallery uses
AndroidX `PickVisualMedia` in image-only mode, with the platform's picker fallback.
Neither broad gallery access nor an in-app camera permission is requested.
See [Android's photo picker documentation](https://developer.android.com/training/data-storage/shared/photo-picker).

After an image is returned, a progress indicator replaces the source choices while
one recognition call runs. Success updates the existing input wheels, returns to
the reading screen, and shows a short toast. It does not save the reading or display
a confirmation dialog. An unreadable image or absence of a plausible numeric pair shows an error
with Camera and Gallery available for another attempt. Cancelling either picker
returns to the choices without an error.

`ScanImageRepositoryImpl` decodes content URIs off the UI thread. BitmapFactory
power-of-two subsampling bounds the initial allocation without shrinking below the
target resolution. After EXIF rotation/mirroring, a filtered resize limits the longest
side to **1280 pixels**, preserving the aspect ratio. Smaller images keep their
existing dimensions and are not resampled or upscaled. For example, 2040x1530 becomes
1280x960, while 800x600 stays 800x600. This is a pixel-dimension limit rather than a
compressed-file byte limit: decoded pixels determine OCR work and bitmap memory.
`PhotoDecoder.kt` is adapted from the Apache-2.0 test app; its license is retained in assets.
Software bitmaps are passed to the scanner and recycled after
recognition, including cancellation. Private camera files are removed on completion
or cancellation; gallery originals are never deleted. Abrupt process termination
can leave cache files; a restored interrupted scan removes its pending capture.

`TextRecognizerImpl` lazily creates the library recognizer. It first calls
`recognizeVisual` on a filtered preview with maximum side 1280 (twice the detector's
640px input). At this size the library avoids its nine extra zoom passes while
retaining backlight-neutral OCR. The original phone JPEG reproduced a scale-sensitive
failure: at 1530x2040 the library returned `18 / 08 / 70`; the same image scaled to
960x1280 returned `118 / 78 / 70`. The permissive extractor had selected `70/18`
from the damaged output. Resizing re-reads the pixels; no missing digit is inferred.

The same shared size limiter also protects direct bitmap callers of
`TextRecognizerImpl`. There is one recognition pass and no larger-image retry, so
all inference respects the limit. Both coordinates and text go into
`PressureReadingExtractor`; the editable best guess remains available when evidence
is weak. Original gallery and camera-source files are not resized in place.

The ViewModel retains an in-progress scan across configuration changes and saves the
pending capture URI while the camera app is open. After process death during
recognition, the screen offers another image instead of restoring a stuck spinner.
Leaving the scan screen cancels work and discards late results. A result is consumed
once, so recomposition cannot repeatedly update the input or display the toast.

Images are processed on-device and are not uploaded by the app. Temporary capture
storage and ML Kit diagnostic behavior are disclosed in the privacy page.

## Spatial assignment

The extractor converts original-image boxes into upright coordinates, retaining row
order. It ranks ordered pairs using digit height, alignment, spacing, prominence,
nearby labels/units, surrounding numbers, and character confidence. Integers with
leading zeros are accepted; signs, decimal values, clock strings and arbitrary text
are not converted into pressure readings. Missing digits are never invented.

Per the user's preference, **a plausible best guess is preferable to a rejection**.
The broad scan acceptance bounds are SYS 30–300 and DIA 10–250 mmHg, with SYS > DIA.
These are configurable software bounds, not healthy/normal ranges or a clinical
definition of viability. Manual entry retains its existing technical validation.

There is no minimum evidence score, confidence margin, mandatory label, pulse field,
complete layout, or geometric cutoff. Conflicting labels, unusual order, intervening
numbers, large gaps and overlapping fields lower the ranking instead of preventing
an otherwise plausible pair from being shown. The highest-ranked pair is returned
even if its score is low or tied; row and left-to-right order make ties deterministic.
Scores and margins remain diagnostic values, not calibrated probabilities.

Pressure labels and layout still help distinguish pulse and clock values when there
are alternatives. If OCR only supplies plausible DIA/PULSE numbers, those can now
fill the pressure inputs as a best guess, as explicitly requested. The user can edit
the numbers and must still save the reading manually. An error remains appropriate
when no pair satisfies the numeric bounds or image decoding/recognition fails.
Pair searches are limited to the 80 largest numeric fields on cluttered images.

`PressureScanner` debug logs include dimensions, field counts, outcome, evidence score
and margin, or an exception class. Failed matches additionally log each field's row,
box, character score and character shape (digits become D, letters become L).
Production diagnostics do not log image contents, URIs, recognized text, pressure
values, or exception messages that may contain those details.

This is not a universal monitor classifier. Missing OCR digits cannot be recovered
from layout; unlabelled non-monitor numbers can resemble a monitor. Broad accuracy
requires a representative held-out set split by monitor model and original photo,
tracking correct assignments, wrong assignments and rejections separately.

## Verification

`SevenSegmentScannerTest` invokes the packaged library and model on two full monitor
photos, smaller images, and rotated images, asserting `126/81` despite pulse and
clock fields. It also exercises content-URI decoding of a captured JPEG with EXIF
rotation and checks temporary-file deletion and decoder sampling boundaries. Tests
also remove printed labels from real OCR results and verify pressure selection using
the remaining numeric boxes. `PressureReadingExtractorTest` covers missing labels,
clocks, unrelated text, pulse confusion, competing groups, incomplete readings,
horizontal layouts, relaxed best-guess acceptance, numeric bounds and coordinate
transformations. The 118/78/70 regressions include the test-app output and the exact OCR boxes and
confidence scores obtained from the subsequently supplied monitor photograph.
`ScannerInputRegressionTest` can exercise a supplied image through native recognition
and the complete image repository on a device. The photo is staged in app cache,
not committed to source or bundled in an APK. Enable it explicitly with instrumentation
arguments `scannerRegression=true`, `expectedSys=118`, and `expectedDia=78` after
placing the input at `cache/scanner-regression.jpg`. This asserts production recognition
and the content-URI repository path. `ScanViewModelTest` covers progress,
success consumption, retries, picker cancellation, unreadable images, pending camera
restoration, process interruption, and late results after cancellation. Compose
tests verify source choices, progress/error states, and direct return to input wheels.

The photos are from Chakib Belgaid's
[bp-tracker](https://github.com/chakib-belgaid/bp-tracker), commit
`ba4701e3e72767d096b48fb1b3277664a58b0138`; their MIT license is included beside
the Android test assets. They are development fixtures, not an accuracy benchmark.
Real-camera focus, glare, motion, and other monitor models require device testing.

Validated on 2026-11-15 after the strict size limit: debug build, lint and 64 JVM
tests passed. Eight scanner instrumentation tests passed on the API 34 emulator,
covering smaller images, the 1280 boundary, larger portrait/landscape/square images,
EXIF rotation, and the original phone JPEG still returning 118/78 through production
recognition and the image repository.

Previously, after the preview fix: debug build, lint and 64 JVM tests
passed. All seven scanner instrumentation tests passed on the physical Samsung
phone, including the exact original JPEG selected in the failing scan (4080x3060
with EXIF rotation, decoded to 1530x2040). Both direct production recognition and
the image repository returned 118/78; the recognition pass took about 3 seconds.
The earlier emulator run of 15 Android tests used the smaller shared JPEG, which
did not reproduce the original-resolution failure. A prior manual system-picker check showed
progress and updated the draft to 126/81. A system-camera capture of the emulator
scene showed progress, then the unrecognized-image error with both retry choices;
the temporary photo was removed. No test reading was saved.

Install tests without removing an existing guest session:

```sh
./gradlew testDebugUnitTest assembleDebug assembleDebugAndroidTest
adb install -r app/build/outputs/apk/debug/app-debug.apk
adb install -r app/build/outputs/apk/androidTest/debug/app-debug-androidTest.apk
adb shell am instrument -w -e class com.artemidius.bloodpressure.SevenSegmentScannerTest,com.artemidius.bloodpressure.ReadingScreensTest com.artemidius.bloodpressure.test/androidx.test.runner.AndroidJUnitRunner
```
