# 商品扫描器

基于 YOLOv8 的 Android 商品识别应用

## 📱 功能特性

- 📸 实时相机预览和拍照
- 🔍 商品识别（5种商品）
- 📊 识别结果展示（商品名称+置信度）
- 🔢 商品计数

## 📦 支持商品

1. 东鹏特饮 250ml
2. 可口可乐 500ml
3. 康师傅冰红茶 500ml
4. 康师傅红烧牛肉面大食桶 115g
5. 泉阳泉野生蓝莓果汁饮料

## 🚀 快速开始

### 下载 APK

从 [GitHub Releases](../../releases) 下载最新 APK

### 安装

```bash
adb install app-debug.apk
```

或者手动复制到手机安装

## 🏗️ 开发

### 环境要求

- Android Studio Arctic Fox 或更高版本
- Android SDK 21+
- Kotlin 1.5+

### 构建

```bash
./gradlew assembleDebug
```

APK 将生成在 `app/build/outputs/apk/debug/`

## 📊 性能指标

- **模型大小**: ~5MB (INT8量化)
- **推理时间**: ~200ms (中端手机)
- **准确率**: 60-80% (基于20张训练数据)

## ⚠️ 已知限制

1. 需要更好的光照条件
2. 商品不能严重遮挡
3. 需要距离商品 30-50cm 拍摄

## 📄 许可证

MIT License
