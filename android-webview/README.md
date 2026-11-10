# 学生请销假系统 Android WebView 测试壳

该工程将 Vue H5 系统加载到原生 Android WebView 中，用于执行安装、移动端兼容性、网络切换和日志分析测试。

## 运行方式

1. 启动 MySQL 和 Spring Boot 后端（`8080`）。
2. 启动 Vue/Vite 前端（`5173`）：`npm run dev`。
3. 构建 APK：`gradlew.bat assembleDebug`。
4. 安装：`adb install -r app/build/outputs/apk/debug/app-debug.apk`。

模拟器中 `10.0.2.2` 指向宿主机，默认页面地址为 `http://10.0.2.2:5173`。

## 日志标记

`adb logcat -s StudentLeaveQA:* chromium:* AndroidRuntime:E`

关键标记：`PAGE_START`、`PAGE_FINISH`、`WEB_ERROR`、`HTTP_ERROR`、`WEB_CONSOLE`、`NETWORK_LOST`、`NETWORK_AVAILABLE`。
