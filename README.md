# Darkroom

**English** · [简体中文](README.zh-CN.md) · [日本語](README.ja.md)

A native Android darkroom. A camera drops JPGs into an on-device FTP inbox; you browse,
frame to 2:3, optionally run an AI edit, then print over Bluetooth Classic SPP to the
**Xiaomi Portable Photo Printer Pro** (KDRSHDY03HT). Nothing goes through the Mi Home cloud.

> ### ⚠️ Research and study use only
>
> This app drives the printer through a **community-reverse-engineered** protocol. It is not
> affiliated with, endorsed by, or supported by Xiaomi or Hannto, and it may stop working or
> misbehave after any firmware update. It is published as a study and research reference.
> **Please do not use it commercially.** See [Scope and disclaimer](#scope-and-disclaimer).

```
camera --FTP--> inbox --> gallery --> framing / AI / watermark --> RFCOMM ch.1 --> printer
```

## Features

- **Camera FTP inbox** — in-process FTP server, default port **2121** (binding 21 needs root on
  Android), JPG only, passive mode, bound to Wi-Fi only. Optional auto-start and a keep-alive
  foreground service.
- **Ingest** — FTP, the system camera, or manual import from the photo picker.
- **Gallery** — Room-backed catalog with edit lineage, per-photo versions, and delete-with-undo.
- **Studio** — 2:3 framing with snap-drag and 45° rotation snap, framing persisted per version,
  watermark overlay, export to MediaStore at 2×, and share.
- **AI editing** — Grok (xAI) and OpenAI providers behind one interface, with prompt presets
  (portrait / creative / custom). Generate a new version of a photo, or a standalone gallery entry.
- **Watermark** — SNS handle (Instagram / X / Facebook / Weibo) and date, positioned separately
  for portrait and landscape sheets, rendered only at print and export time.
- **Printing** — print queue with per-job progress, copies, fill/fit, cancel, and out-of-paper
  detection. Output is 1040 × 1560, 2:3, matching the printer's 313 dpi 2×3 in sheet.
- **Automation** — ingest → AI edit → watermark → print, with a switch for each step.
- **Localized** — English, 简体中文, 日本語, 한국어.

API keys and the FTP password are stored in `EncryptedSharedPreferences`, never in plain text.

## Build

Requires JDK 17 and the Android SDK (`compileSdk` 37).

```bash
./gradlew :app:test :app:assembleDebug
```

Open this directory in Android Studio, or set `ANDROID_HOME` and use Gradle directly.
`local.properties` is intentionally not committed — Android Studio writes it on first open,
or you can create it yourself with `sdk.dir=/path/to/Android/sdk`.

| | |
|---|---|
| minSdk / targetSdk / compileSdk | 29 / 37 / 37 |
| Application ID | `app.darkroom.android` |
| Stack | Kotlin, Jetpack Compose, Room, Hilt, OkHttp, Coil |

## Printer

| | |
|---|---|
| Device | Xiaomi / Mijia Portable Photo Printer Pro |
| Model | KDRSHDY03HT |
| Transport | Bluetooth Classic SPP / RFCOMM, channel 1 |
| Payload | Baseline JPEG, 1040 × 1560, sent in 988-byte chunks |
| Session | Diffie–Hellman handshake, then AES-128-ECB |

The print path uses no Mi Home account and no device token: the AES key is a per-session
Diffie–Hellman secret. Pair the printer in Android's Bluetooth settings first, then bind it
in the app's Settings screen.

Only the Pro has been tested. Other Xiaomi photo printers use different framing and are
not supported.

## Credits

The printer protocol work rests on
**[tuat-yate/xiaomi-photo-printer](https://github.com/tuat-yate/xiaomi-photo-printer)** (MIT),
a Python client that reverse-engineered the official React Native plugin and verified the
handshake and print frames against real hardware. The Kotlin implementation here was written
against that reference. Thank you to its authors and contributors.

Full third-party notices, including the MIT text that must travel with that attribution, are in
[THIRD-PARTY-NOTICES.md](THIRD-PARTY-NOTICES.md).

## Scope and disclaimer

- **Study and research only.** This project exists to document and exercise an undocumented
  protocol. The author asks that it not be used commercially.
- **Not an official product.** No affiliation with Xiaomi, Mijia, or Hannto. All trademarks
  belong to their respective owners. Model names appear only to identify compatible hardware.
- **Reverse-engineered behaviour.** The protocol was recovered from observation, not from
  documentation. Parts of it remain unverified. A firmware update can break it at any time.
- **No warranty.** Provided as-is. You are responsible for anything that happens to your
  printer, your paper, or your data, and for complying with the laws that apply where you are.

A note on the license, so this is not misleading: **GPL-3.0 explicitly grants the right to use
the software commercially**, and an added "no commercial use" term would contradict it and make
the license invalid. The paragraphs above are therefore a statement of intent and a request, not
an extra restriction on top of the GPL. If you need a legally binding non-commercial term, this
is the wrong license and you should ask the author.

## License

Copyright (C) 2026 Otomiya

This program is free software: you can redistribute it and/or modify it under the terms of the
GNU General Public License as published by the Free Software Foundation, either version 3 of the
License, or (at your option) any later version.

This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
See the [GNU General Public License](LICENSE) for more details.
