# 暗房 Android

独立原生暗房：相机 FTP 入匣 → 相册 → 2:3 裁切 / Grok → 米家便携照片打印机（Bluetooth Classic SPP）。

```bash
./gradlew :app:test :app:assembleDebug
```

- minSdk 29 / targetSdk 37 / compileSdk 37
- FTP 默认端口 **2121**（非 root 无法绑 21）
- 首次启动自动生成 `camera` 密码
- xAI key 与 FTP 密码存在 EncryptedSharedPreferences

用 Android Studio 打开本目录，或设置 `ANDROID_HOME` 后跑 Gradle。
