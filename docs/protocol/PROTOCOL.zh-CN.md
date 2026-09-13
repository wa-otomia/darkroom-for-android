# 小米口袋照片打印机 Pro 蓝牙协议

[English](PROTOCOL.md) · **简体中文** · [日本語](PROTOCOL.ja.md)

对米家插件 `com.hannto.printer` 1.1.15（version_code 69）iOS `main.bundle` 的模块解读。行号对应 [examples/js/modules](examples/js/README.md) 中去掉末尾 NUL 后的原文。带行号的长摘录见 [SOURCE_EXCERPTS.md](examples/js/SOURCE_EXCERPTS.md)。

对象：`xiaomi.printer.ricott` / `ricotg`。不是固件规范。版权仍归原作者。与小米 / 汉图无关。

| 标记 | 含义 |
|---|---|
| **源码确认** | 模块中有定义与调用链 |
| **样例支持** | 仅见于随包调试页 |
| **实机待验** | 插件源码无法单独决定 |

## 1. 型号 — `12179.js`

`xiaomi.printer.ricott` / `ricotg` / `ricotp` 共用 Ricotta 配置。`isPhotoPrinter()` 含 ricott/ricotg，因此错误页会走照片机恢复。本说明不把 Mint 或其他机型的常量自动套到 Pro。

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

## 2. 业务请求 — `12407.js`

JSON 是加密前的业务体。`handleCommond` 将其 AES 后装进数据通道帧。`id` 来自 `getMsgSn()`。params 的数组 / 对象形状必须保持。

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

// L712–L715  confirmJob（仅调试页调用）
{ method: 'confirm_job', id: msgSn, params: [jobId] }
```

生产创建任务是 `printJob2`，不是旁边的 `printJob` 桩函数（后者 `channel: 1`、`job_type: 1`）。

```javascript
// 12407.js L335–L358  printJob2
if (Platform.OS === "ios") channel = 64;
else channel = 576;   // JSON 平台字段，不是帧头 channel 3/4
{
  method: 'print_job', id: msgSn,
  params: { file_size: result.totalLength, copies, job_type: jobType, channel }
}
```

`job_info` / `cancel_job` 是 `[jobId]`，不是 `{job_id:…}`。`resume_printer` 不带任务 ID。`cancelJob` 在 Promise resolve 后置 `isCanceled = true`（L274），不检查业务 `error`。

## 3. 状态对象 — `12311.js`

鉴权后先 `getDeviceInfo`，读 `result.result[0]`。之后每 3000 ms 调 `getMixStatus`。正在 `sendFile` 时跳过状态查询。3 秒是客户端策略。

```javascript
// 12311.js L108–L114
setInterval(..., 3000);

// L591–L601  传文件时不查状态；result 当对象用
if (getIsSendingFile()) { /* skip */ }
else {
  getMixStatus().then(function (res) {
    var state = res.result;          // 对象，不是数组
    state.isSuccess = true;          // 客户端字段
    state.connectionState = ...;     // 客户端字段
    handlePrinterState(state);
  });
}

// L728
this.deviceInfo = result.result[0];
```

`isSuccess` / `connectionState` 由客户端写入，不要求固件返回。设备层 `category` / `error` 与任务层 `job_state` 分开。新任务入口：

```javascript
// 12311.js L694–L695
if (state.category === "idle" && !state.hasOwnProperty("job_id")) {
  this.updateQuene();
}
```

`job_info` 取 `result[0]`，并按 `job_state` 分支 `finished` / `canceled` / `aborted`（L911–L930）。`initlization` 是源码拼写（`12173.js` L1234）。

## 4. 帧 — `12368.js` / `12359.js`

连接：`12407.js` L1202，SPP UUID `00001101-0000-1000-8000-00805f9b34fb`。

`HTPackage.build`（`12368.js` L180–L214）写出 `22 + body` 字节。多字节字段用 `Uint32Array` / `Uint16Array`，即运行平台小端。

| 偏移 | 字段 | 源码 |
|---|---|---|
| 0 | `FRAME_HEAD` `0x7E` | `frame[0]` |
| 1 | version `0x64` | `this.version` |
| 2 | reserve | `this.reserve` |
| 3 | channelId | 加密 JSON=3，文件=4，鉴权=255 |
| 4 | interactive | 请求 6、响应 7；鉴权 16–19 |
| 5 | encoding | JSON=3，文件 HEX=2 |
| 6–13 | arcMsgSn、msgSn | `getView6to13` |
| 14–19 | 总分片、当前片、msgAttribute | `getView14to19` |
| 20 | body | 业务命令为密文 |
| 末尾-2 | 校验 | `sum - 126`（126 = `0x7E`） |
| 末尾 | `FRAME_TAIL` `0x7E` | |

`encryptType === '101'`（AES-ECB）时 `msgAttribute += 5120`。`msgPackageTotal > 1 || isStream` 时再加 8192。解析器 `12359.js` L65：`bodyLength > 992` 则拒绝。收帧函数未见比较校验和。

不要按 body 内的 `0x7E` 切帧：`getFrameEndIndex` 用属性里的长度。

## 5. AES — `12365.js`

```javascript
// 12365.js L12–L21  零填充；已对齐 16 则 fillLength = 0
var fillLength = data.length % 16 === 0 ? 0 : 16 - data.length % 16;
// L23–L38  remove=true 时去掉末尾 0x00（JSON）；文件分片必须 remove=false
```

## 6. 握手 — `12407.js`

通道 255。`sayHello`（L900–L920）body 为明文 `'hello'`，属性仍标 ECB。`handleInfo`（L974–L1042）要求 server-hello body 正好 36 字节：`G(4) | P(16) | RA(16)`，按 ASCII 十六进制读成大整数。

```javascript
// 12407.js L1004–L1032
rb = G^b mod P;   // 不足 16 字节：arrayPacking16（左补）
K  = RA^b mod P;  // 不足 16 字节：arrayPacking16AtEnd（右补）
mesbody = RB(16) | AES-ECB(K, P)(16);
```

`handleConfirm`（L938–L945）：去掉尾零后字符串等于 `ok` 即进入 `AUTH`。鉴权帧不走普通业务 JSON 解密。

## 7. 电量与传感器

生产页用 `battery` 枚举，不是百分比。`12863.js` L826 调用 `getNewBattery(battery-temp, battery, sensor)`，再交给导航栏按档位选图标（`12419.js` L221+）。

| `battery`（`12173.js`） | 含义 |
|---|---|
| 0–5 | 未充电：临界 / 低 / 中 / 高 / 满 / 关机档 |
| 16–21 | 同上，充电组 |
| `isCharging()` | 16–21 为真（`12311.js` L1612） |

`battery-level` 只出现在调试页 `13166.js` L199–L224 的两份样例（4→94、20→96）。**样例支持**，不是抓包。Mint 调试场景里的 `battery_pct` 不能和它互换。

```javascript
// 12311.js L1621–L1628
isUSBConnected(sensor)    { return (sensor & 8) != 0; }  // ≠ 正在充电
isPaperTrayColsed(sensor) { return (sensor & 4) != 0; }  // ≠ 有纸
```

充电暂停：`温度异常 && USB && !isCharging`（L1648）。`battery-temp === 2` 为 NORMAL，不是 ℃。源码未把 bit 0/1 定义为有纸 / 有色带。

## 8. 设备异常 — `12332.js` / `13484.js`

```javascript
// 12332.js L911–L913
if (printerStatus.category == "error") {
  getErrorInfo(printerStatus.error, printerStatus.sub_category);
}
```

`getErrorInfo`（L248+）是本版 Pro 页面的权威分支。`isResume` 为真：**-7103、-7110、-7111、-7112、-7114、-7201、-7208**。中文标题在 `13484.js` L934+。

| error | 标题（13484） | isResume |
|---|---|---|
| -7001 | 纸盒盖未关闭 | 否 |
| -7103 | 未检测到相纸 | 是 |
| -7104 / -7105 | 内部卡纸 / 取纸异常 | 否 |
| -7110 / -7111 / -7112 / -7114 | 未取走 / 放置有误 / 被移走 / 取纸异常 | 是 |
| -7201 | 色带用尽 | 是 |
| -7204 / -7205 | 色带卡住 / 色带异常 | 否 |
| -7208 | 色带异常（安装或重插） | 是 |
| -7308 / -7309 | 温度过高 / 过低 | 否 |
| -7310 / -7311 | 电量低 / 即将关机 | 否 |

`-7101` 常量名 PAPER_EMPTY，但本页处理的是 `-7103`。`-7203` 定义为 NO_RIBBON，**本函数无专用 case**；安装 / 重插文案在 `-7208`。`12173.js` 里 Mint 设备码与 Ricotta 任务码会撞号（如 -8001），必须带上下文解释。

## 9. 已处理 — `13145.js`

```javascript
// 13145.js L303–L304
if (this.isPhotoPrinter()) {
  HTClassicBluetooth.getInstance().resumePrinter();  // → resume_printer {}
}
```

按钮成功只表示命令返回，然后 `goBack()`。不是带 `job_id` 的续传 API。官方对 -7104 / -7105 / -7204 / -7205 要求重启，源码不会循环发恢复。

`confirmJob` 只在调试页出现；`sendFile` 结束后直接 `onTransferSuccess(jobId)`（`12407.js` L600），管理器改 PRINTING，不发 `confirm_job`。

## 10. 取消 — `13208.js` → `12311.js` → `12407.js`

打印页 `cancelJob()` → `cancelCurrentJob()` → `cancelJob(currentJob.print_job_id)`。对象字段叫 `print_job_id`，线上只有数组里的数字。无当前任务则 `reject("no job")`；本地未创建的任务走 `cancelLocalJob()`，不发蓝牙。

`sendFile` 循环检查 `isCanceled`（L502）。resolve 即置位，不核对业务错误。

## 11. 文件上传 — `12407.js`

`bodyMessageLength = 988`（L71）。明文：`int2Bytes(jobId) + chunk`，再 ECB，通道 4、`ENCODING_HEX`，分片号从 1。Android 最多聚 `MAX_PACKAGE_NUM_PER_TIME = 3` 帧再写；iOS 每帧即写（L556）。传完调用 `onTransferSuccess`，完成仍看 `job_info` 的 `job_state`。

## 12. 请求队列 — `12407.js` `processQueue`

一次只处理一个请求（L807–L816）。加密 JSON，`build(false)`（body 已是密文）。监听器收到第一帧即撤销。仅当请求与响应双方都有 truthy `id` 时才比较（L851–L858）；否则照样 `resolve(jsonData)`。`resolve` 不等于业务成功，也没有把 `error` 对象统一转成异常。`event.rpt_err` 且 `params.code == -8201` 在 `getDeviceInfo` 里当连接异常（`12311.js` L719–L725）。

超时只表示没有确认。源码在超时后继续队列，不会自动重放 `print_job` / `resume_printer` / `cancel_job`。

## 13. 模块索引

| 模块 | 解读要点 |
|---|---|
| [12179.js](examples/js/modules/12179.js) | 型号、`isPhotoPrinter` |
| [12407.js](examples/js/modules/12407.js) | RPC、握手、上传、SPP、队列 |
| [12311.js](examples/js/modules/12311.js) | 轮询、状态对象、取消、传感器、终态 |
| [12332.js](examples/js/modules/12332.js) / [13484.js](examples/js/modules/13484.js) | 错误页、`isResume`、中文 |
| [12368.js](examples/js/modules/12368.js) / [12365.js](examples/js/modules/12365.js) / [12359.js](examples/js/modules/12359.js) | 组帧、AES、拆帧 |
| [12863.js](examples/js/modules/12863.js) / [12419.js](examples/js/modules/12419.js) / [13166.js](examples/js/modules/13166.js) | 生产电量 UI、百分数样例 |
| [13145.js](examples/js/modules/13145.js) / [13208.js](examples/js/modules/13208.js) | 已处理、打印页取消 |
| [12173.js](examples/js/modules/12173.js) | 常量与冲突命名空间 |

```bash
node docs/protocol/examples/js/verify_official.js   # 37 项原始函数检查
```

完整插件二进制与账号 / token 不收录。恢复 / 取消的机械结果、百分数精度、以及未装色带对应 `-7203` / `-7205` / `-7208` 中的哪一个，均为实机待验。
