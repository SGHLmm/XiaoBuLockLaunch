# XiaoBuLockLaunch

小布助手 13.4.3 的应用名称到包名映射 LSPosed 模块。

## 已核验接口

- 目标进程：`com.heytap.speechassist`
- 启动入口：`OpenAppOperation.tryLaunchApp(LaunchApp)`
- 指令对象：`com.heytap.speech.engine.protocol.directive.app.LaunchApp`
- 可用方法：`getAppName()`、`getPackageName()`、`setPackageName(String)`

模块在原有启动指令处理前替换目标包名，然后交回小布助手原始启动流程；不会调用锁屏解锁方法，也不会伪造 Keyguard 状态。

## 构建和安装

GitHub Actions 会在推送到 main 或手动运行时构建 Debug APK，并上传 `XiaoBuLockLaunch-debug` artifact。

1. 下载 Actions 中的 APK artifact 并安装。
2. 在 LSPosed 中启用模块，作用域只勾选小布助手（`com.heytap.speechassist`）。
3. 强制停止并重新打开小布助手。

## 自定义映射

在模块应用中每行填写一条：

```
我的工具箱=com.example.toolbox
家庭相册=com.example.gallery
```

左侧必须与小布助手传给启动指令的应用名称相符，右侧填写目标应用包名。模块只在小布助手已经产生 LaunchApp 指令时生效，不会扩展语音识别或自然语言理解，也不能保证任意自定义短语都会被识别为启动应用。

## 锁屏限制

模块不解锁设备、不绕过 PIN/密码/生物识别，也不伪造系统锁屏状态。Android/ColorOS 可能阻止锁屏时的后台 Activity 启动或界面显示，必须在目标设备上实测，不能保证所有应用都能在锁屏状态显示。
