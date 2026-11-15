# Third-party notices

Our Kotlin library, demo app, scripts, and documentation are Apache-2.0 licensed.
That license does not replace licenses on models, dependencies, or third-party images.

## Runtime dependencies

- Google ML Kit Text Recognition 16.0.1 and its bundled Latin OCR model —
  [Google ML Kit Terms & Privacy](https://developers.google.com/ml-kit/terms).
  This dependency is not covered by our Apache-2.0 license. Image/text processing
  stays on-device; the SDK may transmit usage/performance metrics.
- Google LiteRT 2.3.2 (current source; published 0.2 uses 1.4.2) — Apache-2.0: https://github.com/google-ai-edge/LiteRT
- Kotlin and kotlinx.coroutines — Apache-2.0: https://github.com/Kotlin/kotlinx.coroutines
- AndroidX Core and ExifInterface (demo app) — Apache-2.0: https://android.googlesource.com/platform/frameworks/support/

These are dependency references in the library publication; no model weights are
included in our AAR itself. ML Kit supplies OCR weights transitively to the final app.
Consumers distributing an application must retain applicable
notices for the dependencies they package.

## Optional SMART-BP research model

Source: Nikookar et al., **BP Image Digitize**, v1.2.0, archived commit
`c71450adf67247e41674394e8f26ae873e040345`.

- Release and complete upstream archive: https://zenodo.org/records/21480466
- Upstream repository: https://github.com/cliffordlab/BP_image_digitize
- License: AGPL-3.0, retained as `models/smart-bp/MODEL_LICENSE` by the opt-in fetcher.
- Upstream README and commercial licensing notes are retained alongside the weights.

The conversion preserves the upstream model's terms. Model weights are not included
in our Git history, library release bundles, or release APKs. The demo APK built
locally after model setup does contain those weights. Our Apache-2.0 license is
not permission to redistribute that combined APK under Apache-2.0 alone.
See [model provenance and setup](docs/MODELS.md).

## Example photos and screenshot

The two device-photo test fixtures and the source photo visible in
`docs/images/gallery.png` are from Chakib Belgaid's **bp-tracker**, commit
`ba4701e3e72767d096b48fb1b3277664a58b0138`:
https://github.com/chakib-belgaid/bp-tracker

Their MIT license is retained at [third_party/bp-tracker/LICENSE](third_party/bp-tracker/LICENSE).
The fixture downloader verifies pinned SHA-256 hashes and copies the license into
the test assets. The screenshot was captured from our demo on an Android emulator;
its recognition output is an example, not an accuracy benchmark.
[Capture provenance and reproduction](docs/images/README.md) include the exact raw output.
