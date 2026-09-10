# Darkroom（暗室）

[English](README.md) · [简体中文](README.zh-CN.md) · **日本語**

Android ネイティブの暗室アプリ。カメラが端末内の FTP 受信箱へ JPG を送り、閲覧して 2:3 にトリミングし、
必要なら AI 加工をかけ、Bluetooth Classic SPP で **Xiaomi ポータブルフォトプリンター Pro**
（KDRSHDY03HT）に印刷します。Mi Home クラウドは経由しません。

> 有志によるリバースエンジニアリングで判明したプロトコルに基づきます。非公式、Xiaomi / Hannto とは無関係、
> 無保証、ファームウェア更新で動かなくなる可能性があります。学習・研究の参考として公開しています。

## 機能

- **FTP 受信箱** —— プロセス内サーバー。既定ポート 2121、JPG のみ、Wi-Fi インターフェースのみ、自動起動と常駐は任意。
- **取り込み** —— FTP、標準カメラ、手動インポート。
- **ギャラリー** —— Room カタログ。編集の系譜と写真ごとのバージョンを保持し、削除は取り消せます。
- **スタジオ** —— スナップドラッグと 45° 回転スナップによる 2:3 構図（バージョンごとに保存）、透かし、2 倍書き出し、共有。
- **AI** —— Grok（xAI）と OpenAI を共通インターフェースにまとめ、プロンプトのプリセット付き。
- **透かし** —— SNS アカウントと日付。縦横で位置を別々に保持し、印刷と書き出し時のみ焼き込み。
- **印刷** —— キュー、ジョブごとの進捗、部数、フィル / フィット、キャンセル、用紙切れ検出。出力は 1040 × 1560。
- **自動化** —— 取り込み → AI 加工 → 透かし → 印刷。各ステップを個別に切り替え可能。
- **多言語** —— English、简体中文、日本語、한국어。

API キーと FTP パスワードは `EncryptedSharedPreferences` に保存されます。

## ビルド

JDK 17 と Android SDK が必要です。minSdk 29、target/compileSdk 37、アプリケーション ID `io.github.wa_otomia.darkroom`。
構成は Kotlin、Compose、Room、Hilt、OkHttp、Coil。

```bash
./gradlew :app:test :app:assembleDebug
```

`local.properties` はコミットしていません。Android Studio が生成するか、`sdk.dir=/path/to/Android/sdk` を自分で記述してください。

## リリース

`git tag v1.0.0 && git push origin v1.0.0` で CI が release APK を GitHub Release に添付します。任意の署名 secrets：`SIGNING_STORE_BASE64`、`SIGNING_STORE_PASSWORD`、`SIGNING_KEY_ALIAS`、`SIGNING_KEY_PASSWORD`。

## プリンター

Xiaomi / Mijia ポータブルフォトプリンター Pro（KDRSHDY03HT）。Bluetooth Classic SPP / RFCOMM
チャンネル 1 経由で、ベースライン JPEG 1040 × 1560 を 988 バイト単位で送信、Diffie–Hellman
ハンドシェイク後に AES-128-ECB。Mi Home アカウントもデバイストークンも不要です。
Android の Bluetooth 設定でペアリングしてから、アプリの設定画面でバインドしてください。対応は Pro のみ。

## クレジット

プロトコルの参照元：**[tuat-yate/xiaomi-photo-printer](https://github.com/tuat-yate/xiaomi-photo-printer)**（MIT）
—— 公式プラグインをリバースエンジニアリングし、ハンドシェイクと印刷フレームを実機で検証した Python クライアント。
本プロジェクトの Kotlin 実装はこれを参照して書かれました。
詳細な告知は [THIRD-PARTY-NOTICES.md](THIRD-PARTY-NOTICES.md) にあります。

## ライセンス

[GPL-3.0](LICENSE) © 2026 Otomiya
