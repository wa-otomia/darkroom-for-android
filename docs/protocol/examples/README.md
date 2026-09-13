# Examples / 示例 / 例

[English](#english) · [简体中文](#简体中文) · [日本語](#日本語)

## English

Protocol commentary cites the official modules. Those files are the source.

| Path | Role |
|---|---|
| [`js/modules/`](js/README.md) | 17 official plugin modules |
| [`js/SOURCE_EXCERPTS.md`](js/SOURCE_EXCERPTS.md) | Numbered excerpts with original line numbers |
| [`js/verify_official.js`](js/verify_official.js) | 37 checks on original constructors |
| [`data/`](data/) | Vectors / constants / error policy regenerated from those functions |
| [`python/`](python/pro_protocol.py) | Offline byte helper from the analysis pack (no Bluetooth) |

```bash
node docs/protocol/examples/js/verify_official.js
python3 docs/protocol/examples/python/test_pro_protocol.py
```

The fixture key `30313233343536373839414243444546` is synthetic.

## 简体中文

协议正文按官方模块解读。那些 JS 才是出处。

| 路径 | 作用 |
|---|---|
| [`js/modules/`](js/README.md) | 17 个官方插件模块 |
| [`js/SOURCE_EXCERPTS.md`](js/SOURCE_EXCERPTS.md) | 带原行号的摘录 |
| [`js/verify_official.js`](js/verify_official.js) | 对原始构造器的 37 项检查 |
| [`data/`](data/) | 由这些函数再生的向量 / 常量 / 错误策略 |
| [`python/`](python/pro_protocol.py) | 分析包里的离线字节辅助（不连蓝牙） |

合成密钥 `3031…444546` 不得发往实机。

## 日本語

本文は公式モジュールを解説します。出典はそれらの JS です。

| パス | 役割 |
|---|---|
| [`js/modules/`](js/README.md) | 公式プラグインモジュール 17 本 |
| [`js/SOURCE_EXCERPTS.md`](js/SOURCE_EXCERPTS.md) | 元の行番号付き抜粋 |
| [`js/verify_official.js`](js/verify_official.js) | 元のコンストラクタ 37 項 |
| [`data/`](data/) | それらから再生成したベクタ / 定数 / エラー方針 |
| [`python/`](python/pro_protocol.py) | 解析パックのオフライン補助（Bluetooth なし） |

合成鍵 `3031…444546` を実機へ送らないでください。
