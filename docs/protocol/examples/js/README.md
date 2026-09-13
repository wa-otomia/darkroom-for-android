# Official plugin modules / 官方插件模块 / 公式プラグインモジュール

[English](#english) · [简体中文](#简体中文) · [日本語](#日本語)

Selected modules from Mi Home `com.hannto.printer` 1.1.15 (69), `Plugin_1065830/ios/main.bundle`. Ownership remains with the original authors. Verification only. The full plugin binary is not included.

选自米家插件 `com.hannto.printer` 1.1.15（69）的 `main.bundle`。版权仍归原作者。仅供核验。完整插件二进制不收录。

Mi Home プラグイン `com.hannto.printer` 1.1.15（69）の `main.bundle` から抽出したモジュール。権利は原作者にあります。照合用です。プラグイン一式は含みません。

Bundle SHA-256: `95b61a95ec6e63c5330ba36a897cb41c3377a51483f3b5360e92586a3ebc2f8c`

Readable copies drop only a trailing NUL. Hashes and bundle offsets: [`module_index.json`](module_index.json). Numbered excerpts: [`SOURCE_EXCERPTS.md`](SOURCE_EXCERPTS.md).

| Module | Role / 作用 / 役割 |
|---|---|
| [`12173.js`](modules/12173.js) | Error / battery / job constants |
| [`12179.js`](modules/12179.js) | Model map, `isPhotoPrinter` |
| [`12308.js`](modules/12308.js) | Embedded AES |
| [`12311.js`](modules/12311.js) | Status poll, cancel, sensors |
| [`12332.js`](modules/12332.js) | Pro error UI, `isResume` |
| [`12359.js`](modules/12359.js) | Frame parse, SPP |
| [`12365.js`](modules/12365.js) | AES-ECB zero padding |
| [`12368.js`](modules/12368.js) | Frame builder |
| [`12371.js`](modules/12371.js) | Wire helpers |
| [`12407.js`](modules/12407.js) | RPC: status, print, cancel, resume |
| [`12419.js`](modules/12419.js) | Battery enum display |
| [`12863.js`](modules/12863.js) | Ricotta home battery |
| [`13145.js`](modules/13145.js) | Handled-button → `resume_printer` |
| [`13154.js`](modules/13154.js) | Resume call site |
| [`13166.js`](modules/13166.js) | `battery-level` samples |
| [`13208.js`](modules/13208.js) | Print-page cancel |
| [`13484.js`](modules/13484.js) | Chinese error strings |

```bash
node docs/protocol/examples/js/verify_official.js
```

Runs reviewed pure fragments only. Writes regenerated JSON under `generated/` and does not overwrite [`../data/`](../data/). Not a general JS sandbox.
