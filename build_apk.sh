#!/bin/bash
set -e

echo "=== 构建 APK 安装包 ==="
echo ""

# 检查 Android SDK
if [ -z "$ANDROID_SDK_ROOT" ] && [ -z "$ANDROID_HOME" ]; then
    echo "⚠️ 警告: 未找到 Android SDK"
    echo "请设置 ANDROID_SDK_ROOT 或 ANDROID_HOME 环境变量"
    echo ""
fi

# 创建必要的目录
mkdir -p app/src/main/assets
mkdir -p app/src/main/res/mipmap-hdpi
mkdir -p app/src/main/res/mipmap-mdpi
mkdir -p app/src/main/res/mipmap-xhdpi
mkdir -p app/src/main/res/mipmap-xxhdpi
mkdir -p app/src/main/res/mipmap-xxxhdpi
mkdir -p app/src/main/res/values

# 创建 strings.xml
cat > app/src/main/res/values/strings.xml << 'XMLEOF'
<resources>
    <string name="app_name">商品扫描器</string>
</resources>
XMLEOF

# 创建 colors.xml
cat > app/src/main/res/values/colors.xml << 'XMLEOF'
<?xml version="1.0" encoding="utf-8"?>
<resources>
    <color name="purple_200">#FFBB86FC</color>
    <color name="purple_500">#FF6200EE</color>
    <color name="purple_700">#FF3700B3</color>
    <color name="teal_200">#FF03DAC5</color>
    <color name="teal_700">#FF018786</color>
    <color name="black">#FF000000</color>
    <color name="white">#FFFFFFFF</color>
</resources>
XMLEOF

# 创建 themes.xml
cat > app/src/main/res/values/themes.xml << 'XMLEOF'
<resources xmlns:tools="http://schemas.android.com/tools">
    <style name="Theme.ProductScanner" parent="android:Theme.Material.Light.NoActionBar" />
</resources>
XMLEOF

# 创建简单的 ic_launcher.xml (使用系统默认图标)
cat > app/src/main/res/mipmap-anydpi-v26/ic_launcher.xml << 'XMLEOF'
<?xml version="1.0" encoding="utf-8"?>
<monochrome android:drawable="@mipmap/ic_launcher_foreground" />
XMLEOF

echo "✅ 资源文件创建完成"
echo ""
echo "⚠️ 注意: 由于缺少完整的 Android SDK 环境，无法直接构建 APK"
echo ""
echo "📋 请使用以下方法安装:"
echo ""
echo "方法1: Android Studio 构建"
echo "  1. 打开 Android Studio"
echo "  2. File → Open → 选择 android_product_scanner 目录"
echo "  3. Build → Build Bundle(s) / APK(s) → Build APK(s)"
echo "  4. APK 将生成在 app/build/outputs/apk/debug/"
echo ""
echo "方法2: 命令行构建 (需要安装 Android SDK)"
echo "  ./gradlew assembleDebug"
echo ""
echo "方法3: 在线构建服务"
echo "  使用 GitHub Actions 或 Bitrise 等 CI/CD 服务"
echo ""
