# 暗房 Darkroom

[English](README.md) · **简体中文** · [日本語](README.ja.md)

原生 Android 暗房。相机把 JPG 传进机内 FTP 收件匣，浏览、裁成 2:3、按需 AI 修图，
再经蓝牙 Classic SPP 打印到**米家便携照片打印机 Pro**（KDRSHDY03HT）。不经过米家云。

> 基于社区逆向得到的协议。非官方，与小米 / 汉图无关，不提供任何担保，固件更新后可能失效。
> 作为学习与研究的参考发布。

## 功能

- **FTP 收件匣** —— 进程内服务器，默认端口 2121，只收 JPG，仅绑定 Wi-Fi 网卡，可选自启与保活。
- **入匣** —— FTP、系统相机，或手动导入。
- **相册** —— Room 图库，记录修图谱系与每张照片的版本，删除带撤销。
- **工作室** —— 2:3 构图，吸附拖动与 45° 旋转吸附，构图按版本保存，水印，2 倍导出，分享。
- **AI** —— Grok（xAI）与 OpenAI 收敛到同一接口，带提示词预设。
- **水印** —— SNS 账号与日期，竖版横版分别记位置，只在打印和导出时渲染。
- **打印** —— 打印队列，逐任务进度、份数、填满/留白、取消、缺纸检测。输出 1040 × 1560。
- **自动化** —— 入匣 → AI 修图 → 水印 → 打印，每步可单独开关。
- **多语言** —— English、简体中文、日本語、한국어。

API key 与 FTP 密码存在 `EncryptedSharedPreferences`。

## 构建

需要 JDK 17 和 Android SDK。minSdk 29，target/compileSdk 37，应用 ID `io.github.wa_otomia.darkroom`。
技术栈 Kotlin、Compose、Room、Hilt、OkHttp、Coil。

```bash
./gradlew :app:test :app:assembleDebug
```

`local.properties` 不入库 —— Android Studio 会自动生成，或自己写 `sdk.dir=/path/to/Android/sdk`。

## 发布

`git tag v1.0.0 && git push origin v1.0.0`，CI 会把 release APK 挂到 GitHub Release。可选签名 secrets：`SIGNING_STORE_BASE64`、`SIGNING_STORE_PASSWORD`、`SIGNING_KEY_ALIAS`、`SIGNING_KEY_PASSWORD`。

## 打印机

小米 / 米家便携照片打印机 Pro（KDRSHDY03HT），走蓝牙 Classic SPP / RFCOMM 通道 1：
baseline JPEG 1040 × 1560，按 988 字节分片，Diffie–Hellman 握手后 AES-128-ECB。
不需要米家账号和设备 token。先在系统蓝牙设置里配对，再到应用设置页绑定。仅支持 Pro。

## 致谢

协议参考：**[tuat-yate/xiaomi-photo-printer](https://github.com/tuat-yate/xiaomi-photo-printer)**（MIT）
—— 一个 Python 客户端，逆向了官方插件并在真机上验证了握手与打印帧。本项目的 Kotlin 实现参照它写成。
完整声明见 [THIRD-PARTY-NOTICES.md](THIRD-PARTY-NOTICES.md)。

## 许可证

[GPL-3.0](LICENSE) © 2026 Otomiya。
