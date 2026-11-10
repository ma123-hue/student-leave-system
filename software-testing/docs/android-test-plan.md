# Android WebView 专项测试计划

## 测试范围

- APK 覆盖安装、升级安装、启动、后台切换和卸载后重装。
- Android WebView 中的学生登录、强制改密、管理端登录与路由保护。
- 不同 Android API、屏幕尺寸、横竖屏与 WebView 内核版本兼容性。
- Wi-Fi 正常、断网、恢复网络及前后端服务不可用场景。
- 通过 `adb logcat`、`dumpsys activity`、`dumpsys connectivity` 和 `bugreport` 定位页面加载与网络问题。

## 核心用例

| 编号 | 场景 | 预期结果 |
| --- | --- | --- |
| AND-INST-001 | 首次安装 Debug APK | 安装成功，桌面展示应用入口 |
| AND-INST-002 | `adb install -r` 覆盖安装 | 升级成功，应用数据保留 |
| AND-COMP-001 | 360×640 小屏竖屏 | 表单可操作，无横向滚动 |
| AND-COMP-002 | 1080×1920 竖屏 | 页面布局、字体和弹窗正常 |
| AND-COMP-003 | 横竖屏切换 | 页面不崩溃，当前路由保留 |
| AND-NET-001 | 正常网络启动 | H5 首页加载成功 |
| AND-NET-002 | 关闭 Wi-Fi/数据后重启 | 应用不崩溃，显示无网络/加载失败 |
| AND-NET-003 | 恢复网络并刷新 | 页面可恢复加载 |
| AND-LOG-001 | 采集 WebView 加载日志 | 包含 PAGE_START/PAGE_FINISH 且无 AndroidRuntime 崩溃 |
| AND-LOG-002 | 断网日志 | 包含 NETWORK_LOST/WEB_ERROR，可追踪失败 URL |

## 准出标准

- 安装、启动、登录关键路径通过，无 `FATAL EXCEPTION` 和 ANR。
- 断网与网络恢复过程不崩溃，失败日志包含 URL 和错误码。
- 小屏和主流竖屏尺寸下主要控件可见、可点击。
