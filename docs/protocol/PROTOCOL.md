# Xiaomi Portable Photo Printer Pro Bluetooth Protocol

**English** · [简体中文](PROTOCOL.zh-CN.md) · [日本語](PROTOCOL.ja.md)

Commentary on Mi Home plugin `com.hannto.printer` 1.1.15 (version code 69), iOS `main.bundle`. Line numbers match the NUL-stripped text in [examples/js/modules](examples/js/README.md). Longer numbered excerpts: [SOURCE_EXCERPTS.md](examples/js/SOURCE_EXCERPTS.md).

Targets `xiaomi.printer.ricott` / `ricotg`. Not a firmware spec. Ownership stays with the original authors. Unaffiliated with Xiaomi / Hannto.

| Tag | Meaning |
|---|---|
| **Source-confirmed** | Defined and used in the module call chain |
| **Sample-only** | Debug page only |
| **Hardware-pending** | Plugin source cannot decide alone |

## 1. Models — `12179.js`

`xiaomi.printer.ricott` / `ricotg` / `ricotp` share the Ricotta config. `isPhotoPrinter()` includes ricott/ricotg, so the error page uses photo-printer resume. Do not apply Mint or other-model constants to Pro by default.

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

## 2. Business requests — `12407.js`

JSON is the pre-encryption payload. `handleCommond` AES-encrypts it and wraps a data-channel frame. `id` comes from `getMsgSn()`. Keep params as an array or an object.

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

// L712–L715  confirmJob (debug page only)
{ method: 'confirm_job', id: msgSn, params: [jobId] }
```

Production create is `printJob2`, not the `printJob` stub (`channel: 1`, `job_type: 1`).

```javascript
// 12407.js L335–L358  printJob2
if (Platform.OS === "ios") channel = 64;
else channel = 576;   // JSON platform field, not frame channel 3/4
{
  method: 'print_job', id: msgSn,
  params: { file_size: result.totalLength, copies, job_type: jobType, channel }
}
```

`job_info` / `cancel_job` take `[jobId]`, not `{job_id:…}`. `resume_printer` has no job id. `cancelJob` sets `isCanceled = true` on Promise resolve (L274) without checking a business `error`.

## 3. Status object — `12311.js`

After auth, `getDeviceInfo` reads `result.result[0]`. Then `getMixStatus` every 3000 ms. Status polls are skipped during `sendFile`. Three seconds is a client policy.

```javascript
// 12311.js L108–L114
setInterval(..., 3000);

// L591–L601  skip while sending; result is an object
if (getIsSendingFile()) { /* skip */ }
else {
  getMixStatus().then(function (res) {
    var state = res.result;          // object, not array
    state.isSuccess = true;          // client field
    state.connectionState = ...;     // client field
    handlePrinterState(state);
  });
}

// L728
this.deviceInfo = result.result[0];
```

Firmware is not required to return `isSuccess` / `connectionState`. Device-layer `category` / `error` is not job-layer `job_state`. New-job gate:

```javascript
// 12311.js L694–L695
if (state.category === "idle" && !state.hasOwnProperty("job_id")) {
  this.updateQuene();
}
```

`job_info` uses `result[0]` and branches on `finished` / `canceled` / `aborted` (L911–L930). `initlization` is the source spelling (`12173.js` L1234).

## 4. Frame — `12368.js` / `12359.js`

Connect: `12407.js` L1202, SPP UUID `00001101-0000-1000-8000-00805f9b34fb`.

`HTPackage.build` (`12368.js` L180–L214) writes `22 + body` bytes. Multi-byte fields use `Uint32Array` / `Uint16Array` (little-endian on this platform).

| Off | Field | Source |
|---|---|---|
| 0 | `FRAME_HEAD` `0x7E` | `frame[0]` |
| 1 | version `0x64` | `this.version` |
| 2 | reserve | `this.reserve` |
| 3 | channelId | encrypted JSON=3, file=4, auth=255 |
| 4 | interactive | request 6, response 7; auth 16–19 |
| 5 | encoding | JSON=3, file HEX=2 |
| 6–13 | arcMsgSn, msgSn | `getView6to13` |
| 14–19 | totals, index, msgAttribute | `getView14to19` |
| 20 | body | ciphertext for business commands |
| -2 | checksum | `sum - 126` (`0x7E`) |
| -1 | `FRAME_TAIL` `0x7E` | |

`encryptType === '101'` (AES-ECB) adds 5120. `msgPackageTotal > 1 || isStream` adds 8192. Parser `12359.js` L65 rejects `bodyLength > 992`. The receiver was not seen comparing the checksum.

Do not split on `0x7E` inside the body: `getFrameEndIndex` uses the length in the attribute.

## 5. AES — `12365.js`

```javascript
// 12365.js L12–L21  zero padding; fillLength = 0 if already 16-aligned
var fillLength = data.length % 16 === 0 ? 0 : 16 - data.length % 16;
// L23–L38  strip trailing 0x00 only when remove=true (JSON); keep zeros in file chunks
```

## 6. Handshake — `12407.js`

Channel 255. `sayHello` (L900–L920) sends plaintext `'hello'` with the ECB attribute still set. `handleInfo` (L974–L1042) requires a 36-byte server-hello: `G(4) | P(16) | RA(16)`, read as ASCII hex integers.

```javascript
// 12407.js L1004–L1032
rb = G^b mod P;   // short: arrayPacking16 (left pad)
K  = RA^b mod P;  // short: arrayPacking16AtEnd (right pad)
mesbody = RB(16) | AES-ECB(K, P)(16);
```

`handleConfirm` (L938–L945): after trimming tail zeros, the string `ok` enters `AUTH`. Auth frames are not decrypted as business JSON.

## 7. Battery and sensors

The production page uses the `battery` enum, not a percentage. `12863.js` L826 calls `getNewBattery(battery-temp, battery, sensor)`; the nav bar picks an icon by band (`12419.js` L221+).

| `battery` (`12173.js`) | Meaning |
|---|---|
| 0–5 | not charging: critical / low / mid / high / full / power-off |
| 16–21 | same bands, charging group |
| `isCharging()` | true for 16–21 (`12311.js` L1612) |

`battery-level` appears only in debug samples at `13166.js` L199–L224 (4→94, 20→96). **Sample-only**, not a capture. Mint-debug `battery_pct` is not interchangeable.

```javascript
// 12311.js L1621–L1628
isUSBConnected(sensor)    { return (sensor & 8) != 0; }  // ≠ charging
isPaperTrayColsed(sensor) { return (sensor & 4) != 0; }  // ≠ paper present
```

Charge-pause: `temp error && USB && !isCharging` (L1648). `battery-temp === 2` is NORMAL, not °C. Bits 0/1 are not defined as paper/ribbon present.

## 8. Device faults — `12332.js` / `13484.js`

```javascript
// 12332.js L911–L913
if (printerStatus.category == "error") {
  getErrorInfo(printerStatus.error, printerStatus.sub_category);
}
```

`getErrorInfo` (L248+) is this Pro page’s authoritative switch. `isResume` is true for **-7103, -7110, -7111, -7112, -7114, -7201, -7208**. Chinese titles: `13484.js` L934+.

| error | Title (13484) | isResume |
|---|---|---|
| -7001 | Cover open | no |
| -7103 | Paper not detected | yes |
| -7104 / -7105 | Jam / eject fault | no |
| -7110 / -7111 / -7112 / -7114 | Not taken / mis-set / removed / load fault | yes |
| -7201 | Ribbon exhausted | yes |
| -7204 / -7205 | Ribbon jam / ribbon fault | no |
| -7208 | Ribbon fault (install or reseat) | yes |
| -7308 / -7309 | Too hot / too cold | no |
| -7310 / -7311 | Low battery / about to power off | no |

Constant `-7101` is named PAPER_EMPTY; this page handles **-7103**. `-7203` is NO_RIBBON and has **no case** here; install/reseat copy is on **-7208**. In `12173.js`, Mint device codes collide with Ricotta job codes (e.g. -8001). Keep the namespace.

## 9. “Handled” — `13145.js`

```javascript
// 13145.js L303–L304
if (this.isPhotoPrinter()) {
  HTClassicBluetooth.getInstance().resumePrinter();  // → resume_printer {}
}
```

Success only means the command returned, then `goBack()`. It is not a job-id resume API. Official copy for -7104 / -7105 / -7204 / -7205 asks for a reboot; the source does not loop resume.

`confirmJob` is debug-only. `sendFile` ends with `onTransferSuccess(jobId)` (`12407.js` L600); the manager switches to PRINTING and does not send `confirm_job`.

## 10. Cancel — `13208.js` → `12311.js` → `12407.js`

Print page `cancelJob()` → `cancelCurrentJob()` → `cancelJob(currentJob.print_job_id)`. The object field is `print_job_id`; the wire value is the number in the array. No current job → `reject("no job")`. Not-yet-created local work uses `cancelLocalJob()` with no Bluetooth.

`sendFile` checks `isCanceled` (L502). Resolve sets the flag without a business-error check.

## 11. File upload — `12407.js`

`bodyMessageLength = 988` (L71). Plaintext: `int2Bytes(jobId) + chunk`, then ECB, channel 4, `ENCODING_HEX`, index from 1. Android coalesces up to `MAX_PACKAGE_NUM_PER_TIME = 3` frames; iOS writes each frame (L556). Completion still comes from `job_info` / `job_state`, not from `onTransferSuccess`.

## 12. Request queue — `12407.js` `processQueue`

One in-flight request (L807–L816). Encrypt JSON, `build(false)` (body already ciphertext). The listener drops after the first frame. IDs are compared only when both sides have a truthy `id` (L851–L858); otherwise it still `resolve(jsonData)`. Resolve is not business success, and `error` objects are not turned into exceptions. `event.rpt_err` with `params.code == -8201` is treated as a link fault in `getDeviceInfo` (`12311.js` L719–L725).

Timeout means no acknowledgement. After timeout the queue continues; the source does not automatically replay `print_job` / `resume_printer` / `cancel_job`.

## 13. Module index

| Module | Read for |
|---|---|
| [12179.js](examples/js/modules/12179.js) | Models, `isPhotoPrinter` |
| [12407.js](examples/js/modules/12407.js) | RPC, handshake, upload, SPP, queue |
| [12311.js](examples/js/modules/12311.js) | Poll, status object, cancel, sensors, terminals |
| [12332.js](examples/js/modules/12332.js) / [13484.js](examples/js/modules/13484.js) | Error page, `isResume`, Chinese copy |
| [12368.js](examples/js/modules/12368.js) / [12365.js](examples/js/modules/12365.js) / [12359.js](examples/js/modules/12359.js) | Frame, AES, unpack |
| [12863.js](examples/js/modules/12863.js) / [12419.js](examples/js/modules/12419.js) / [13166.js](examples/js/modules/13166.js) | Production battery UI, percent samples |
| [13145.js](examples/js/modules/13145.js) / [13208.js](examples/js/modules/13208.js) | Handled button, print-page cancel |
| [12173.js](examples/js/modules/12173.js) | Constants and colliding namespaces |

```bash
node docs/protocol/examples/js/verify_official.js   # 37 original-function checks
```

The full plugin binary and any account / token are not included. Mechanical resume/cancel, percentage accuracy, and which of `-7203` / `-7205` / `-7208` means “no ribbon” remain hardware-pending.
