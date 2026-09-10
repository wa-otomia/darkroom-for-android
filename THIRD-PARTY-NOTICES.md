# Third-party notices

Darkroom is licensed under [GPL-3.0](LICENSE). It builds on the work below.

## Protocol reference

### tuat-yate/xiaomi-photo-printer

<https://github.com/tuat-yate/xiaomi-photo-printer>

A Python client for the Xiaomi Portable Photo Printer Pro that reverse-engineered the official
React Native plugin and verified the Diffie–Hellman handshake, the `0x7E` frame format and the
chunked JPEG upload on real hardware. The printer code in
`app/src/main/java/io/github/wa_otomia/darkroom/data/printer/` and the frame and crypto helpers in
`app/src/main/java/io/github/wa_otomia/darkroom/core/` were written against it. MIT text reproduced as
that license requires:

```
MIT License

Copyright (c) 2026 the xiaomi-photo-printer contributors

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all
copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
SOFTWARE.
```

## Bundled dependencies

All Apache License 2.0. The in-app About screen lists the same set; keep it, this file and
`gradle/libs.versions.toml` in sync when a dependency changes.

| Component | License |
|---|---|
| Kotlin Standard Library — JetBrains | Apache-2.0 |
| kotlinx.coroutines — JetBrains | Apache-2.0 |
| kotlinx.serialization — JetBrains | Apache-2.0 |
| Jetpack Compose (ui, foundation, animation) — Google | Apache-2.0 |
| Material Components for Compose (material3, material-icons) — Google | Apache-2.0 |
| AndroidX Core, Activity, Lifecycle — Google | Apache-2.0 |
| AndroidX Navigation — Google | Apache-2.0 |
| AndroidX Room — Google | Apache-2.0 |
| AndroidX DataStore — Google | Apache-2.0 |
| AndroidX Security Crypto — Google | Apache-2.0 |
| AndroidX ExifInterface — Google | Apache-2.0 |
| Dagger and Hilt — Google | Apache-2.0 |
| OkHttp — Square | Apache-2.0 |
| Okio — Square | Apache-2.0 |
| Coil — Coil Contributors | Apache-2.0 |
