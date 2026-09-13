# Xiaomi ポータブルフォトプリンター Pro Bluetooth プロトコル

[English](PROTOCOL.md) · [简体中文](PROTOCOL.zh-CN.md) · **日本語**

Mi Home プラグイン `com.hannto.printer` 1.1.15（version_code 69）iOS `main.bundle` のモジュール解説です。行番号は [examples/js/modules](examples/js/README.md) の末尾 NUL 除去後の原文に対応します。長い抜粋は [SOURCE_EXCERPTS.md](examples/js/SOURCE_EXCERPTS.md)。

対象は `xiaomi.printer.ricott` / `ricotg`。ファームウェア仕様ではありません。権利は原作者にあります。Xiaomi / Hannto とは無関係です。

| 標識 | 意味 |
|---|---|
| **ソース確認** | モジュールに定義と呼び出しがある |
| **サンプルのみ** | 同梱デバッグ画面にのみ存在 |
| **実機未確認** | プラグイン源だけでは決まらない |

## 1. 機種 — `12179.js`

`xiaomi.printer.ricott` / `ricotg` / `ricotp` は Ricotta 設定を共有します。`isPhotoPrinter()` に ricott/ricotg が含まれるため、エラー画面は写真機の再開経路を使います。Mint や他機種の定数を Pro に自動適用しないでください。

```javascript
// 12179.js L19–L63
var modelMap = {
  'xiaomi.printer.ricott': _deviceConfig4.default,
  'xiaomi.printer.ricotg': _deviceConfig4.default,
  'xiaomi.printer.ricotp': _deviceConfig4.default
};
isPhotoPrinter: function () {
  return model == 'xiaomi.printer.mintg'
    || model == 'xiaomi.printer.ricott'
    || model == 'xiaomi.printer.ricotg'
    || model == 'xiaomi.printer.ricotp';
},
isRicottaType: function () {
  return model == 'xiaomi.printer.ricott'
    || model == 'xiaomi.printer.ricotg'
    || model == 'xiaomi.printer.ricotp';
}
```

## 2. 業務要求 — `12407.js`

JSON は暗号化前の業務本体です。`handleCommond` が AES してデータチャンネルのフレームに包みます。`id` は `getMsgSn()`。params の配列 / オブジェクトの形は保ちます。

```javascript
// 12407.js L201–L211  getMixStatus
{ method: 'mixed_status', id: msgSn, params: {} }

// L227–L230  getDeviceInfo
{ method: 'get_prop', id: msgSn, params: ["device_info"] }

// L267–L270  cancelJob
{ method: 'cancel_job', id: msgSn, params: [jobId] }

// L288–L291  resumePrinter
{ method: 'resume_printer', id: msgSn, params: {} }

// L692–L695  getJobInfo
{ method: 'job_info', id: msgSn, params: [jobId] }

// L712–L715  confirmJob（デバッグ画面のみ）
{ method: 'confirm_job', id: msgSn, params: [jobId] }
```

本番の作成は `printJob2` です。隣の `printJob` スタブ（`channel: 1`、`job_type: 1`）ではありません。

```javascript
// 12407.js L335–L358  printJob2
if (Platform.OS === "ios") channel = 64;
else channel = 576;   // JSON のプラットフォーム値。フレーム channel 3/4 ではない
{
  method: 'print_job', id: msgSn,
  params: { file_size: result.totalLength, copies, job_type: jobType, channel }
}
```

`job_info` / `cancel_job` は `[jobId]` であり `{job_id:…}` ではありません。`resume_printer` にジョブ ID は付きません。`cancelJob` は Promise resolve のあと `isCanceled = true`（L274）とし、業務 `error` は見ません。

## 3. 状態オブジェクト — `12311.js`

認証後にまず `getDeviceInfo` し、`result.result[0]` を読みます。その後 3000 ms ごとに `getMixStatus`。`sendFile` 中は状態照会を飛ばします。3 秒はクライアント方針です。

```javascript
// 12311.js L108–L114
setInterval(..., 3000);

// L591–L601  送信中は照会しない。result はオブジェクト
if (getIsSendingFile()) { /* skip */ }
else {
  getMixStatus().then(function (res) {
    var state = res.result;          // オブジェクトであり配列ではない
    state.isSuccess = true;          // クライアント側
    state.connectionState = ...;     // クライアント側
    handlePrinterState(state);
  });
}

// L728
this.deviceInfo = result.result[0];
```

`isSuccess` / `connectionState` をファームウェアが返す必要はありません。機器層の `category` / `error` とジョブ層の `job_state` は分けます。新規ジョブの入口：

```javascript
// 12311.js L694–L695
if (state.category === "idle" && !state.hasOwnProperty("job_id")) {
  this.updateQuene();
}
```

`job_info` は `result[0]` を取り、`finished` / `canceled` / `aborted` で分岐します（L911–L930）。`initlization` はソースの綴りです（`12173.js` L1234）。

## 4. フレーム — `12368.js` / `12359.js`

接続：`12407.js` L1202、SPP UUID `00001101-0000-1000-8000-00805f9b34fb`。

`HTPackage.build`（`12368.js` L180–L214）は `22 + body` バイトを書きます。多バイトは `Uint32Array` / `Uint16Array`（この実行系ではリトルエンディアン）。

| オフセット | フィールド | ソース |
|---|---|---|
| 0 | `FRAME_HEAD` `0x7E` | `frame[0]` |
| 1 | version `0x64` | `this.version` |
| 2 | reserve | `this.reserve` |
| 3 | channelId | 暗号 JSON=3、ファイル=4、認証=255 |
| 4 | interactive | 要求 6、応答 7；認証 16–19 |
| 5 | encoding | JSON=3、ファイル HEX=2 |
| 6–13 | arcMsgSn、msgSn | `getView6to13` |
| 14–19 | 総数、番号、msgAttribute | `getView14to19` |
| 20 | body | 業務コマンドは暗号文 |
| 末尾-2 | チェックサム | `sum - 126`（`0x7E`） |
| 末尾 | `FRAME_TAIL` `0x7E` | |

`encryptType === '101'`（AES-ECB）なら `msgAttribute += 5120`。`msgPackageTotal > 1 || isStream` ならさらに 8192。パーサ `12359.js` L65 は `bodyLength > 992` を拒否します。受信側でチェックサム比較は見当たりません。

body 内の `0x7E` で切らないでください。`getFrameEndIndex` は属性の長さを使います。

## 5. AES — `12365.js`

```javascript
// 12365.js L12–L21  ゼロパディング。16 整列済みなら fillLength = 0
var fillLength = data.length % 16 === 0 ? 0 : 16 - data.length % 16;
// L23–L38  remove=true のときだけ末尾 0x00 を除く（JSON）。ファイル断片は残す
```

## 6. ハンドシェイク — `12407.js`

チャンネル 255。`sayHello`（L900–L920）の body は平文 `'hello'` で、属性は ECB のままです。`handleInfo`（L974–L1042）は server-hello がちょうど 36 バイトであることを要求します：`G(4) | P(16) | RA(16)`。ASCII 十六進の整数として読みます。

```javascript
// 12407.js L1004–L1032
rb = G^b mod P;   // 16 バイト未満：arrayPacking16（左詰め）
K  = RA^b mod P;  // 16 バイト未満：arrayPacking16AtEnd（右詰め）
mesbody = RB(16) | AES-ECB(K, P)(16);
```

`handleConfirm`（L938–L945）：末尾ゼロを除いた文字列が `ok` なら `AUTH`。認証フレームは通常の業務 JSON 復号に乗せません。

## 7. 電池とセンサ

本番画面は `battery` 列挙を使い、百分率ではありません。`12863.js` L826 が `getNewBattery(battery-temp, battery, sensor)` を呼び、ナビバーが帯域でアイコンを選びます（`12419.js` L221+）。

| `battery`（`12173.js`） | 意味 |
|---|---|
| 0–5 | 非充電：危機 / 低 / 中 / 高 / 満 / 電源オフ帯 |
| 16–21 | 同じ帯、充電グループ |
| `isCharging()` | 16–21 が真（`12311.js` L1612） |

`battery-level` はデバッグ画面 `13166.js` L199–L224 の 2 サンプルにだけ出ます（4→94、20→96）。**サンプルのみ**であり、捕捉ではありません。Mint デバッグの `battery_pct` と入れ替えないでください。

```javascript
// 12311.js L1621–L1628
isUSBConnected(sensor)    { return (sensor & 8) != 0; }  // ≠ 充電中
isPaperTrayColsed(sensor) { return (sensor & 4) != 0; }  // ≠ 用紙あり
```

充電一時停止：`温度異常 && USB && !isCharging`（L1648）。`battery-temp === 2` は NORMAL であり ℃ ではありません。bit 0/1 を用紙 / リボンありとは定義していません。

## 8. 機器異常 — `12332.js` / `13484.js`

```javascript
// 12332.js L911–L913
if (printerStatus.category == "error") {
  getErrorInfo(printerStatus.error, printerStatus.sub_category);
}
```

`getErrorInfo`（L248+）がこの版 Pro 画面の正本です。`isResume` が真なのは **-7103、-7110、-7111、-7112、-7114、-7201、-7208**。中国語標題は `13484.js` L934+。

| error | 標題（13484） | isResume |
|---|---|---|
| -7001 | トレイカバー未閉 | なし |
| -7103 | 用紙未検出 | あり |
| -7104 / -7105 | 内部紙詰まり / 取り出し異常 | なし |
| -7110 / -7111 / -7112 / -7114 | 未取り出し / セット誤り / 除去 / 取り出し異常 | あり |
| -7201 | リボン消耗 | あり |
| -7204 / -7205 | リボン詰まり / リボン異常 | なし |
| -7208 | リボン異常（装着または再装着） | あり |
| -7308 / -7309 | 過熱 / 過冷 | なし |
| -7310 / -7311 | 残量低下 / まもなく電源オフ | なし |

`-7101` の定数名は PAPER_EMPTY ですが、この画面が扱うのは **-7103** です。`-7203` は NO_RIBBON で **専用 case がありません**。装着 / 再装着の文面は **-7208** です。`12173.js` では Mint 機器コードと Ricotta ジョブコードが衝突します（例：-8001）。名前空間を分けてください。

## 9. 処理済み — `13145.js`

```javascript
// 13145.js L303–L304
if (this.isPhotoPrinter()) {
  HTClassicBluetooth.getInstance().resumePrinter();  // → resume_printer {}
}
```

成功はコマンドが返ったことだけを意味し、その後 `goBack()` します。`job_id` 付きの継続 API ではありません。-7104 / -7105 / -7204 / -7205 の公式文面は再起動であり、ソースは再開をループしません。

`confirmJob` はデバッグのみ。`sendFile` の終わりは `onTransferSuccess(jobId)`（`12407.js` L600）で、マネージャは PRINTING に変え、`confirm_job` は送りません。

## 10. 取消 — `13208.js` → `12311.js` → `12407.js`

印刷画面 `cancelJob()` → `cancelCurrentJob()` → `cancelJob(currentJob.print_job_id)`。オブジェクトの欄は `print_job_id`、線上は配列内の数値だけです。現行ジョブがなければ `reject("no job")`。未作成のローカル作業は `cancelLocalJob()` で、Bluetooth は出しません。

`sendFile` は `isCanceled` を見ます（L502）。resolve でフラグを立て、業務エラーは確認しません。

## 11. ファイル送信 — `12407.js`

`bodyMessageLength = 988`（L71）。平文は `int2Bytes(jobId) + chunk`、ECB のあとチャンネル 4、`ENCODING_HEX`、番号は 1 から。Android は最大 `MAX_PACKAGE_NUM_PER_TIME = 3` フレームをまとめ、iOS は毎フレーム書きます（L556）。完了判定は `onTransferSuccess` ではなく `job_info` の `job_state` です。

## 12. 要求キュー — `12407.js` `processQueue`

同時に処理するのは 1 件です（L807–L816）。JSON を暗号化し、`build(false)`（body はすでに暗号文）。リスナーは最初の 1 フレームで外れます。双方に truthy な `id` があるときだけ比較し（L851–L858）、そうでなければそのまま `resolve(jsonData)` します。resolve は業務成功ではなく、`error` オブジェクトを例外にもしません。`event.rpt_err` かつ `params.code == -8201` は `getDeviceInfo` で接続異常です（`12311.js` L719–L725）。

タイムアウトは未確認です。タイムアウト後もキューは進み、ソースは `print_job` / `resume_printer` / `cancel_job` を自動再送しません。

## 13. モジュール索引

| モジュール | 読む内容 |
|---|---|
| [12179.js](examples/js/modules/12179.js) | 機種、`isPhotoPrinter` |
| [12407.js](examples/js/modules/12407.js) | RPC、ハンドシェイク、送信、SPP、キュー |
| [12311.js](examples/js/modules/12311.js) | ポーリング、状態、取消、センサ、終態 |
| [12332.js](examples/js/modules/12332.js) / [13484.js](examples/js/modules/13484.js) | エラー画面、`isResume`、中国語 |
| [12368.js](examples/js/modules/12368.js) / [12365.js](examples/js/modules/12365.js) / [12359.js](examples/js/modules/12359.js) | フレーム、AES、分解 |
| [12863.js](examples/js/modules/12863.js) / [12419.js](examples/js/modules/12419.js) / [13166.js](examples/js/modules/13166.js) | 本番電池 UI、百分率サンプル |
| [13145.js](examples/js/modules/13145.js) / [13208.js](examples/js/modules/13208.js) | 処理済み、印刷画面の取消 |
| [12173.js](examples/js/modules/12173.js) | 定数と衝突する名前空間 |

```bash
node docs/protocol/examples/js/verify_official.js   # 元関数 37 項
```

プラグイン一式とアカウント / トークンは収録しません。再開・取消の機械的結果、百分率の精度、および「リボン未装着」が `-7203` / `-7205` / `-7208` のどれかは実機未確認です。
