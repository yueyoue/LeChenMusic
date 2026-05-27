# 乐宸音乐 - Windows 编译教程（新手详细版）

## 📋 目录
1. [需要安装的软件](#1-需要安装的软件)
2. [安装 JDK 17](#2-安装-jdk-17)
3. [安装 Android Studio](#3-安装-android-studio)
4. [打开项目](#4-打开项目)
5. [配置 SDK](#5-配置-sdk)
6. [编译 APK](#6-编译-apk)
7. [签名说明](#7-签名说明)
8. [常见问题](#8-常见问题)

---

## 1. 需要安装的软件

| 软件 | 用途 | 下载地址 |
|------|------|----------|
| **JDK 17** | Java 开发环境 | https://adoptium.net/temurin/releases/?version=17 (选 Windows x64 `.msi` 安装包) |
| **Android Studio** | 安卓开发 IDE | https://developer.android.com/studio (点 Download) |

> 💡 只需要装这两个软件就够了！Gradle（构建工具）会自动下载，不需要手动装。

---

## 2. 安装 JDK 17

### 2.1 下载
1. 打开 https://adoptium.net/temurin/releases/?version=17
2. 操作系统选 **Windows**，架构选 **x64**
3. 下载 `.msi` 安装包（文件名类似 `OpenJDK17U-jdk_x64_windows_hotspot_17.x.x_x.msi`）

### 2.2 安装
1. 双击 `.msi` 文件
2. 一路点 **Next**，**勾选 "Add to PATH"**（很重要！）
3. 安装路径保持默认即可（`C:\Program Files\Eclipse Adoptium\jdk-17...`）
4. 点 **Install** 完成安装

### 2.3 验证安装
1. 按 `Win + R`，输入 `cmd`，回车
2. 输入以下命令：
```
java -version
```
3. 应该显示类似 `openjdk version "17.x.x"` 的信息

---

## 3. 安装 Android Studio

### 3.1 下载
1. 打开 https://developer.android.com/studio
2. 点 **Download Android Studio** 按钮
3. 下载完成后得到一个 `.exe` 文件

### 3.2 安装
1. 双击 `.exe` 安装文件
2. 一路点 **Next**
3. **勾选 "Android Virtual Device"**（可选，模拟器用的，真机调试可以不装）
4. 安装路径保持默认即可
5. 点 **Install**，等待安装完成
6. 安装完成后会自动启动 Android Studio

### 3.3 首次启动设置
1. 启动后选择 **Standard** 安装类型
2. 选择 **Light** 或 **Darcula** 主题（随意）
3. 点 **Next** → **Finish**
4. 等待 Android Studio 下载必要的组件（SDK、构建工具等）
5. 这个过程需要 **10-30 分钟**，取决于网速
6. 下载完成后点 **Finish**

---

## 4. 打开项目

### 4.1 解压项目文件
1. 将下载的 `LeChenMusic.tar.gz` 解压到任意目录
   - 推荐解压到 `D:\Projects\LeChenMusic`（路径中**不要有中文和空格**！）
   - 可以用 7-Zip（https://www.7-zip.org/）解压 `.tar.gz` 文件
   - 解压后应该看到 `D:\Projects\LeChenMusic\app\` 等文件夹

### 4.2 在 Android Studio 中打开
1. 启动 Android Studio
2. 点 **Open**（不要点 New Project）
3. 浏览到你解压的 `LeChenMusic` 文件夹，选中它，点 **OK**
4. 等待项目加载（右下角会显示进度条）
5. **第一次打开会很慢**（5-15分钟），因为要下载 Gradle 和各种依赖

### 4.3 可能遇到的问题
如果右下角报错说 "Could not resolve..." 或 "Failed to download..."：
- 检查网络连接
- 如果在国内，可能需要配置代理或者使用国内镜像
- 详见 [常见问题](#8-常见问题)

---

## 5. 配置 SDK

### 5.1 检查 SDK
1. 在 Android Studio 菜单栏，点 **File** → **Settings**（Mac 是 Android Studio → Preferences）
2. 左侧找到 **Languages & Frameworks** → **Android SDK**
3. 确认 **Android SDK Location** 路径已设置（通常自动检测到）
4. 切换到 **SDK Platforms** 选项卡
5. **勾选** Android 14 (API 34)
6. 切换到 **SDK Tools** 选项卡
7. 确认以下已勾选：
   - ✅ Android SDK Build-Tools
   - ✅ Android SDK Platform-Tools
   - ✅ Android Emulator（可选）
8. 点 **Apply** → 等待下载完成 → **OK**

---

## 6. 编译 APK

### 6.1 编译 Debug 版本（测试用，不需要签名）

1. 在 Android Studio 菜单栏，点 **Build** → **Build Bundle(s) / APK(s)** → **Build APK(s)**
2. 等待编译完成（首次编译需要 5-15 分钟）
3. 编译完成后，右下角会弹出通知，点 **locate** 可以找到 APK 文件
4. APK 文件位置：`你的项目目录\app\build\outputs\apk\debug\app-debug.apk`
5. 把这个 APK 传到手机上安装即可！

### 6.2 编译 Release 版本（正式发布用，需要签名）

Release 版本需要签名才能安装到手机上。请看下面的签名说明。

---

## 7. 签名说明

### 什么是签名？
安卓要求所有 APK 必须经过**数字签名**才能安装。就像给文件盖个章，证明它是你发布的。
- **Debug 签名**：Android Studio 自动帮你签的，用于开发测试，**不需要你操作**
- **Release 签名**：发布正式版时需要自己创建签名文件

### 7.1 创建签名文件（KeyStore）

#### 方法一：用 Android Studio 图形界面（推荐新手）

1. 在 Android Studio 菜单栏，点 **Build** → **Generate Signed Bundle / APK**
2. 选择 **APK**，点 **Next**
3. 点 **Create new...** 按钮
4. 填写以下信息：
   - **Key store path**: 点右边的文件夹图标，选一个保存位置，文件名输入 `lechen.jks`
   - **Password**: 输入密码（记住这个密码！比如 `lechen123`）
   - **Confirm**: 再输入一次密码
   - **Alias**: 输入 `lechen`（密钥别名）
   - **Password**: 输入密码（可以和上面一样）
   - **Confirm**: 再输入一次
   - **Validity**: 保持默认 25 年
   - **First and Last Name**: 输入你的名字（随意）
   - **Organizational Unit**: 随意
   - **Organization**: 随意
   - **City or Locality**: 随意
   - **State or Province**: 随意
   - **Country Code (XX)**: CN
5. 点 **OK**
6. 回到上一个页面，确认 Key store path 已经填好
7. 勾选 **Remember passwords**
8. 点 **Next**
9. 选择 **release**，点 **Create**
10. 等待编译完成

#### 方法二：用命令行

1. 按 `Win + R`，输入 `cmd`，回车
2. 输入以下命令（请先确认 Java 已正确安装）：
```
keytool -genkey -v -keystore D:\lechen.jks -keyalg RSA -keysize 2048 -validity 10000 -alias lechen
```
3. 按提示输入密码、名字等信息
4. 完成后会在 `D:\` 下生成 `lechen.jks` 文件

### 7.2 签名后的 APK 位置
签名后的 APK 在：`你的项目目录\app\release\app-release.apk`

### 7.3 ⚠️ 重要提示
- **签名文件（.jks）一定要保管好！** 丢失后无法更新已发布的 APP
- **密码一定要记住！** 忘记密码就无法使用这个签名文件
- 不要把签名文件上传到公开的地方（GitHub 等）

---

## 8. 常见问题

### Q1: Gradle 下载很慢 / 超时
**解决方案：使用国内镜像**

在项目根目录找到 `settings.gradle.kts` 文件，把里面的 `google()` 和 `mavenCentral()` 改成：

```kotlin
pluginManagement {
    repositories {
        maven { url = uri("https://maven.aliyun.com/repository/google") }
        maven { url = uri("https://maven.aliyun.com/repository/central") }
        maven { url = uri("https://maven.aliyun.com/repository/gradle-plugin") }
    }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        maven { url = uri("https://maven.aliyun.com/repository/google") }
        maven { url = uri("https://maven.aliyun.com/repository/central") }
    }
}
```

### Q2: "SDK location not found"
**解决方案：**
1. 在项目根目录创建一个 `local.properties` 文件
2. 写入：`sdk.dir=C\:\\Users\\你的用户名\\AppData\\Local\\Android\\Sdk`
3. 注意路径中的反斜杠要转义（用 `\\`）

### Q3: "Could not find method compile()"
**解决方案：**
确保你使用的是项目自带的 Gradle 版本，不要手动升级。

### Q4: 编译报错 "Unresolved reference"
**解决方案：**
1. 点菜单 **File** → **Sync Project with Gradle Files**
2. 等待同步完成
3. 再次尝试编译

### Q5: 安装 APK 到手机提示 "安装失败"
**解决方案：**
1. 确认手机已开启 **开发者选项**
2. 在手机 设置 → 开发者选项 中，开启 **USB 调试**
3. 确认手机上没有同名的旧 APP（有的话先卸载）
4. 如果是 Release 版本，确认已正确签名

### Q6: APP 打开闪退
**解决方案：**
1. 确认手机 Android 版本 ≥ 8.0（API 26）
2. 检查是否有网络权限（首次打开需要联网连接服务器）

---

## 📱 安装到手机

### 方法一：USB 安装
1. 手机用数据线连接电脑
2. 手机上允许 USB 调试
3. 在 Android Studio 点顶部绿色三角 ▶ 按钮运行
4. 选择你的手机设备
5. APP 会自动安装并打开

### 方法二：传文件安装
1. 找到编译好的 APK 文件
2. 通过微信/QQ/邮件/数据线传到手机
3. 在手机上打开 APK 文件安装
4. 如果提示 "不允许安装未知来源应用"，去手机设置里允许一下

---

## 📁 项目文件结构说明

```
LeChenMusic/
├── app/                          # 主模块
│   ├── src/main/
│   │   ├── java/com/lechenmusic/ # Kotlin 源代码
│   │   │   ├── data/             # 数据层（API、模型、仓库）
│   │   │   ├── player/           # 播放器服务
│   │   │   └── ui/               # 界面（主题、页面、组件）
│   │   ├── res/                  # 资源文件（图标、布局等）
│   │   └── AndroidManifest.xml   # 应用配置
│   ├── build.gradle.kts          # 模块构建配置
│   └── proguard-rules.pro        # 混淆规则
├── gradle/                       # Gradle 包装器
├── build.gradle.kts              # 项目构建配置
├── settings.gradle.kts           # 项目设置
├── gradle.properties             # Gradle 属性
├── gradlew                       # Linux/Mac 构建脚本
└── gradlew.bat                   # Windows 构建脚本
```

---

## 💡 小贴士

1. **首次编译最慢**，因为要下载所有依赖。之后编译就快了（增量编译通常几十秒）
2. **保持网络畅通**，编译过程中可能需要下载额外的组件
3. **不要中断 Gradle Sync**，否则可能导致项目状态异常
4. 如果遇到问题，先试 **File → Invalidate Caches / Restart**
5. Debug 版本可以直接安装测试，不需要签名

---

**祝你编译顺利！🎵 乐宸音乐**
