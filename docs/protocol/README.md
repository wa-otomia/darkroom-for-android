# Pro protocol / Pro 协议 / Pro プロトコル

[English](PROTOCOL.md) · [简体中文](PROTOCOL.zh-CN.md) · [日本語](PROTOCOL.ja.md)

Commentary on the official plugin modules for the **Xiaomi Portable Photo Printer Pro** (`xiaomi.printer.ricott` / `ricotg`). Source: Mi Home `com.hannto.printer` 1.1.15 (69). Not a firmware spec. Unaffiliated with Xiaomi / Hannto.

对官方插件模块的解读。对象：小米口袋照片打印机 Pro。来源：米家 `com.hannto.printer` 1.1.15（69）。不是固件规范。与小米 / 汉图无关。

公式プラグインモジュールの解説。対象：Xiaomi ポータブルフォトプリンター Pro。出典：Mi Home `com.hannto.printer` 1.1.15（69）。ファームウェア仕様ではありません。Xiaomi / Hannto とは無関係です。

| | EN | 中文 | 日本語 |
|---|---|---|---|
| Commentary | [PROTOCOL.md](PROTOCOL.md) | [PROTOCOL.zh-CN.md](PROTOCOL.zh-CN.md) | [PROTOCOL.ja.md](PROTOCOL.ja.md) |
| Modules | [examples/js](examples/js/README.md) | 同上 | 同上 |
| Excerpts | [SOURCE_EXCERPTS.md](examples/js/SOURCE_EXCERPTS.md) | | |

```
docs/protocol/
  PROTOCOL.md / PROTOCOL.zh-CN.md / PROTOCOL.ja.md
  examples/js/modules/     17 official modules
  examples/js/SOURCE_EXCERPTS.md
  examples/python/         offline byte helper from the analysis pack
  examples/data/           golden vectors / constants / error policy
```

```bash
node docs/protocol/examples/js/verify_official.js
```

The synthetic AES key in the golden vectors must not be sent to a printer. The full plugin binary is not included.
