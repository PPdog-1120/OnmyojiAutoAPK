# OnmyojiAuto - 阴阳师自动化 APK

> 从 [OnmyojiAutoScripts](https://gitee.com/PPdog-1120/OnmyojiAutoScripts) 提取核心逻辑，重构为原生 Android 应用。

## 功能

- **探索** — 自动选章节、UP怪优先、Boss/小怪战斗循环、单人/队长/队员模式
- **个人突破** — 勋章优先级选择、退四打九、失败刷新/重试策略、呱太检测

## 技术架构

```
┌─────────────────────────────────────────┐
│              Jetpack Compose UI         │
│     (任务选择 / 配置 / 日志 / 控制)      │
├─────────────────────────────────────────┤
│            TaskManager (调度器)          │
│     ExplorationTask │ RealmRaidTask     │
├─────────────────────────────────────────┤
│     ImageMatcher (OpenCV 模板匹配)       │
│     DeviceController (手势分发)          │
├─────────────────────────────────────────┤
│  AccessibilityService │ MediaProjection │
│     (无障碍服务)       (屏幕截图)        │
└─────────────────────────────────────────┘
```

## 环境要求

| 组件 | 版本 |
|------|------|
| Android Studio | Hedgehog+ |
| JDK | 17+ |
| Android SDK | 34 |
| Gradle | 8.5 |
| 设备 | Android 8.0+ (API 26+) |

## 构建步骤

### 方式一：Android Studio（推荐）

1. 用 Android Studio 打开项目目录
2. 等待 Gradle Sync 完成
3. 点击 Build → Build APK
4. APK 输出在 `app/build/outputs/apk/`

### 方式二：命令行

```bash
# 确保已设置环境变量
export ANDROID_HOME=$HOME/Android/Sdk

# Debug 版本
./build.sh --debug

# Release 版本
./build.sh --release

# 或直接用 gradle
./gradlew assembleDebug
```

## 使用说明

### 1. 安装 APK

```bash
adb install app/build/outputs/apk/debug/app-debug.apk
```

### 2. 授权权限

打开应用后需要授权两个权限：

- **无障碍服务** — 用于自动化点击/滑动
  - 设置 → 无障碍 → 阴阳师自动 → 开启
- **屏幕截图** — 用于屏幕画面识别
  - 点击"授权"按钮 → 允许屏幕录制

### 3. 配置任务

**探索配置：**
- 章节：选择要探索的章节（默认第二十八章）
- 模式：单人/队长/队员
- 战斗次数：最大战斗次数
- 时间限制：最大运行时间
- UP类型：优先打哪种UP怪

**个人突破配置：**
- 最大挑战次数
- 勋章优先级：如 `5>4>3>2>1>0`
- 退四打九：对第一个目标先进出4次再打9次
- 失败策略：退出/继续/刷新

### 4. 开始运行

1. 打开阴阳师游戏
2. 切换到目标界面（探索/突破页面）
3. 回到本应用，点击"开始"
4. 观察日志输出

## 项目结构

```
app/src/main/
├── java/com/onmyoji/auto/
│   ├── engine/               # 核心引擎
│   │   ├── ImageMatcher.kt   # OpenCV 图像匹配
│   │   ├── DeviceController.kt # 手势/点击控制
│   │   ├── RuleImage.kt      # 匹配规则定义
│   │   ├── BaseTask.kt       # 任务基类
│   │   ├── ExplorationTask.kt # 探索任务
│   │   ├── RealmRaidTask.kt  # 个人突破任务
│   │   └── TaskManager.kt    # 任务调度
│   ├── service/              # 系统服务
│   │   ├── AutomationService.kt  # 无障碍服务
│   │   └── ScreenCaptureService.kt # 截图服务
│   ├── ui/                   # 界面
│   │   └── MainActivity.kt   # Compose 主界面
│   └── model/                # 数据模型
│       └── TaskConfig.kt     # 配置类
├── assets/                   # 匹配图片资源
│   ├── exploration/          # 探索截图 (47个)
│   └── realm_raid/           # 突破截图 (40个)
└── res/                      # Android 资源
```

## 与原项目对比

| 维度 | OAS (Python) | OnmyojiAuto (APK) |
|------|-------------|-------------------|
| 运行环境 | PC + ADB | 手机本地 |
| 控制方式 | ADB 命令 | 无障碍服务手势 |
| 截图方式 | ADB screencap | MediaProjection |
| 图像匹配 | OpenCV Python | OpenCV Android |
| 依赖 | Python 3.10+ | Android 8.0+ |
| 安装方式 | pip/EXE | APK |

## 常见问题

### Q: 无障碍服务无法开启？

部分手机需要额外权限：
- 小米：设置 → 更多设置 → 无障碍 → 已下载的服务
- 华为：设置 → 无障碍 → 已安装的服务
- OPPO/vivo：设置 → 其他设置 → 无障碍

### Q: 截图服务被杀？

在手机设置中将本应用加入电池白名单/自启动白名单。

### Q: 图像匹配不准？

1. 确保游戏分辨率为 1280×720
2. 确保游戏为横屏模式
3. 如使用模拟器，关闭 GPU 渲染

## 免责声明

本软件开源、免费，仅供学习交流使用。使用本软件产生的所有问题与本项目无关。

## 许可证

GPL-3.0
