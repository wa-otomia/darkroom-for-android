# Darkroom

**English** · [简体中文](README.zh-CN.md) · [日本語](README.ja.md)

Native Android darkroom. A camera drops JPGs into an on-device FTP inbox; browse, frame to 2:3,
optionally run an AI edit, print to the **Xiaomi Portable Photo Printer Pro** (KDRSHDY03HT) over
Bluetooth Classic SPP. No Mi Home cloud.

> Built on a community-reverse-engineered protocol. Unofficial, unaffiliated with Xiaomi/Hannto,
> no warranty, may break on any firmware update. Published as a study and research reference.

## Features

- **FTP inbox** — in-process server, default port 2121, JPG only, Wi-Fi interfaces only, optional auto-start and keep-alive.
- **Ingest** — FTP, system camera, or manual import.
- **Gallery** — Room catalog with edit lineage, per-photo versions, delete-with-undo.
- **Studio** — 2:3 framing with snap-drag and 45° rotation snap, framing saved per version, watermark, 2× export, share.
- **AI** — Grok (xAI) and OpenAI behind one interface, with prompt presets.
- **Watermark** — SNS handle and date, separate portrait/landscape positions, rendered at print and export only.
- **Print** — queue with per-job progress, copies, fill/fit, cancel, out-of-paper detection. Output 1040 × 1560.
- **Automation** — ingest → AI edit → watermark → print, each step toggleable.
- **Locales** — English, 简体中文, 日本語, 한국어.

API keys and the FTP password are kept in `EncryptedSharedPreferences`.

## Build

JDK 17 and the Android SDK. minSdk 29, target/compileSdk 37, application ID `io.github.wa_otomia.darkroom`.
Kotlin, Compose, Room, Hilt, OkHttp, Coil.

```bash
./gradlew :app:test :app:assembleDebug
```

`local.properties` is not committed — Android Studio writes it, or add `sdk.dir=/path/to/Android/sdk`.

## Release

`git tag v1.0.0 && git push origin v1.0.0` — CI attaches a release APK. Optional signing secrets: `SIGNING_STORE_BASE64`, `SIGNING_STORE_PASSWORD`, `SIGNING_KEY_ALIAS`, `SIGNING_KEY_PASSWORD`.

## Printer

Xiaomi / Mijia Portable Photo Printer Pro (KDRSHDY03HT), over Bluetooth Classic SPP/RFCOMM
channel 1: baseline JPEG 1040 × 1560 sent in 988-byte chunks, Diffie–Hellman handshake then
AES-128-ECB. No Mi Home account or device token. Pair in Android's Bluetooth settings, then bind
in the app's Settings. Only the Pro is supported.

## Credits

Protocol reference: **[tuat-yate/xiaomi-photo-printer](https://github.com/tuat-yate/xiaomi-photo-printer)**
(MIT) — a Python client that reverse-engineered the official plugin and verified the handshake and
print frames on real hardware. The Kotlin implementation here was written against it.
Full notices in [THIRD-PARTY-NOTICES.md](THIRD-PARTY-NOTICES.md).

## License

[GPL-3.0](LICENSE) © 2026 Otomiya.
