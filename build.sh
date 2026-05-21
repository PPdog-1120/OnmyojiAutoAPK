#!/bin/bash
# OnmyojiAuto APK 构建脚本
# 用法: ./build.sh [--debug|--release]

set -e

echo "========================================"
echo "OnmyojiAuto APK 构建"
echo "========================================"

# 检查 Android SDK
if [ -z "$ANDROID_HOME" ] && [ -z "$ANDROID_SDK_ROOT" ]; then
    echo "❌ 未设置 ANDROID_HOME 或 ANDROID_SDK_ROOT"
    echo ""
    echo "请安装 Android Studio 或 Android SDK Command Line Tools"
    echo "然后设置环境变量:"
    echo "  export ANDROID_HOME=\$HOME/Android/Sdk"
    echo "  export PATH=\$PATH:\$ANDROID_HOME/platform-tools"
    exit 1
fi

SDK="${ANDROID_HOME:-$ANDROID_SDK_ROOT}"
echo "Android SDK: $SDK"

# 检查 Java
if ! command -v java &> /dev/null; then
    echo "❌ 未安装 Java"
    echo "请安装 JDK 17: https://adoptium.net/"
    exit 1
fi

echo "Java: $(java -version 2>&1 | head -1)"

# 接受 SDK 许可
mkdir -p "$SDK/licenses"
echo "24333f8a63b6825ea9c5514f83c2829b004d1fee" > "$SDK/licenses/android-sdk-license"
echo "84831b9409646a918e30573bab4c9c91346d8abd" >> "$SDK/licenses/android-sdk-license"

# Gradle wrapper
if [ ! -f "gradlew" ]; then
    echo "生成 Gradle Wrapper..."
    # 如果系统有 gradle
    if command -v gradle &> /dev/null; then
        gradle wrapper --gradle-version 8.5
    else
        echo "请安装 Gradle 或使用 Android Studio 打开项目"
        exit 1
    fi
fi

chmod +x gradlew

# 构建
MODE="${1:---debug}"
echo ""
echo "构建模式: $MODE"
echo "========================================"

case "$MODE" in
    --debug)
        ./gradlew assembleDebug
        APK="app/build/outputs/apk/debug/app-debug.apk"
        ;;
    --release)
        ./gradlew assembleRelease
        APK="app/build/outputs/apk/release/app-release-unsigned.apk"
        ;;
    *)
        echo "用法: ./build.sh [--debug|--release]"
        exit 1
        ;;
esac

if [ -f "$APK" ]; then
    SIZE=$(du -h "$APK" | cut -f1)
    echo ""
    echo "✅ 构建成功！"
    echo "📦 APK: $APK ($SIZE)"
    echo ""
    echo "安装到设备:"
    echo "  adb install $APK"
else
    echo "❌ 构建失败，请检查错误日志"
    exit 1
fi
