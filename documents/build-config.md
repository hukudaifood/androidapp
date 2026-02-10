# ビルド設定 (Build Configuration) 詳細解説

プロジェクトのビルド設定ファイルの詳細を解説します。

---

## 1. プロジェクト構成

```
androidapp/                    ← ルートプロジェクト
├── build.gradle.kts           ← ルートビルドスクリプト
├── settings.gradle.kts        ← プロジェクト設定
├── gradle.properties          ← Gradle設定プロパティ
├── gradle/
│   ├── libs.versions.toml     ← 依存関係のバージョンカタログ
│   └── wrapper/
│       └── gradle-wrapper.properties  ← Gradleラッパー設定
└── app/                       ← アプリモジュール
    ├── build.gradle.kts       ← アプリビルドスクリプト
    └── proguard-rules.pro     ← コード難読化ルール
```

---

## 2. ルート build.gradle.kts

**ファイル**: `build.gradle.kts`（ルート）

```kotlin
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.ksp) apply false
    alias(libs.plugins.hilt) apply false
}
```

### 解説

ルートの`build.gradle.kts`はサブモジュール全体で使用するプラグインを宣言します。

- **`alias(libs.plugins.xxx)`**: `libs.versions.toml`で定義されたプラグインを参照
- **`apply false`**: ルートプロジェクトには適用せず、サブモジュール（`app/`）でのみ適用可能にする

| プラグイン | 用途 |
|-----------|------|
| `android.application` | Androidアプリケーションのビルド |
| `kotlin.android` | KotlinのAndroidサポート |
| `ksp` | Kotlin Symbol Processing（コード生成） |
| `hilt` | Hilt（依存性注入）のコード生成 |

---

## 3. アプリ build.gradle.kts

**ファイル**: `app/build.gradle.kts`

### プラグイン設定

```kotlin
plugins {
    alias(libs.plugins.android.application)  // Androidアプリプラグイン
    alias(libs.plugins.kotlin.android)       // Kotlin Androidプラグイン
    alias(libs.plugins.kotlin.compose)       // Kotlin Compose コンパイラプラグイン
    alias(libs.plugins.ksp)                  // KSP（アノテーション処理）
    alias(libs.plugins.hilt)                 // Hilt（DI）
}
```

ここでは`apply false`がないため、実際にプラグインが適用されます。

### Android設定ブロック

```kotlin
android {
    namespace = "com.fukudai.meshiroulette"  // パッケージ名（R classの生成先）
    compileSdk = 35                           // コンパイルに使用するAPI レベル
```

| 設定 | 値 | 説明 |
|------|------|------|
| `namespace` | `com.fukudai.meshiroulette` | アプリのパッケージ名。Rクラスの生成先 |
| `compileSdk` | `35` | Android 15 (Vanilla Ice Cream) のAPIでコンパイル |

### defaultConfig

```kotlin
defaultConfig {
    applicationId = "com.fukudai.meshiroulette"  // Google Playでの一意識別子
    minSdk = 24         // 対応する最低Androidバージョン (Android 7.0)
    targetSdk = 34      // 動作確認済みのターゲットバージョン (Android 14)
    versionCode = 1     // 内部バージョン番号（整数、アップデート判定に使用）
    versionName = "1.0" // 表示用バージョン名

    testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

    buildConfigField("String", "BASE_URL",
        "\"https://fukudaifood-130668695114.asia-northeast1.run.app/\"")
}
```

#### buildConfigField

```kotlin
buildConfigField("String", "BASE_URL", "\"https://...\"")
```

ビルド時に`BuildConfig.java`に定数を自動生成します。コード内で`BuildConfig.BASE_URL`として参照できます。

**なぜハードコードしないのか？**:
- ビルドバリアント（debug/release）ごとに異なるURLを設定できる
- 環境変数やCI/CDで動的に変更できる
- APIキーなどの機密情報も同様に管理できる

#### SDKバージョンの意味

```
minSdk: 24 (Android 7.0 Nougat, 2016年)
    → このバージョン以上のデバイスでインストール可能
    → 日本のAndroid端末の99%以上をカバー

targetSdk: 34 (Android 14, 2023年)
    → このバージョンの動作規約に従う
    → 新しいセキュリティ制限やUI変更が適用される

compileSdk: 35 (Android 15, 2024年)
    → 最新のAPIをコンパイル時に使用可能
    → targetSdkより新しくてもOK（コンパイル時のみ）
```

### ビルドタイプ

```kotlin
buildTypes {
    release {
        isMinifyEnabled = false  // コード圧縮/難読化を無効
        proguardFiles(
            getDefaultProguardFile("proguard-android-optimize.txt"),
            "proguard-rules.pro"
        )
    }
}
```

- **`isMinifyEnabled = false`**: ProGuard/R8によるコード圧縮を無効化（開発段階のため）
- 本番リリース時は`true`にしてAPKサイズを削減し、コードを難読化する

### コンパイルオプション

```kotlin
compileOptions {
    sourceCompatibility = JavaVersion.VERSION_11  // Java 11構文を使用可能
    targetCompatibility = JavaVersion.VERSION_11  // Java 11バイトコードを生成
}
kotlinOptions {
    jvmTarget = "11"  // Kotlinが生成するJVMバイトコードのバージョン
}
```

### ビルド機能

```kotlin
buildFeatures {
    compose = true      // Jetpack Compose を有効化
    buildConfig = true  // BuildConfig クラスの生成を有効化
}
```

- `compose = true`: Composeコンパイラを有効にし、`@Composable`関数を使えるようにする
- `buildConfig = true`: `buildConfigField`で定義した値を含む`BuildConfig.java`を生成

### 依存関係（dependencies）

```kotlin
dependencies {
    // Core
    implementation(libs.androidx.core.ktx)     // Android KTX拡張
    implementation(libs.lifecycle.runtime.ktx) // ライフサイクル

    // Compose
    implementation(platform(libs.compose.bom))        // Compose BOM（バージョン一括管理）
    implementation(libs.compose.ui)                    // Compose UI基盤
    implementation(libs.compose.ui.graphics)           // グラフィックス
    implementation(libs.compose.ui.tooling.preview)    // プレビュー（Android Studio）
    implementation(libs.compose.material3)             // Material 3 コンポーネント
    implementation(libs.compose.material.icons.extended) // 拡張アイコンセット
    implementation(libs.activity.compose)               // Activity Compose統合
    implementation(libs.lifecycle.runtime.compose)       // ライフサイクル対応
    implementation(libs.lifecycle.viewmodel.compose)     // ViewModel Compose統合
    implementation(libs.navigation.compose)              // Navigation Compose
    debugImplementation(libs.compose.ui.tooling)         // デバッグ用ツール

    // Hilt
    implementation(libs.hilt.android)            // Hilt本体
    ksp(libs.hilt.compiler)                      // Hiltコード生成（KSP）
    implementation(libs.hilt.navigation.compose) // Hilt + Navigation統合

    // Retrofit & OkHttp
    implementation(libs.retrofit)                     // Retrofit本体
    implementation(libs.retrofit.converter.gson)      // Gson変換
    implementation(libs.okhttp)                       // OkHttp本体
    implementation(libs.okhttp.logging.interceptor)   // ログインターセプタ
    implementation(libs.gson)                         // Gson

    // Coil
    implementation(libs.coil.compose)  // 画像読み込み（Compose対応）

    // Coroutines
    implementation(libs.coroutines.core)     // コルーチン基盤
    implementation(libs.coroutines.android)  // Androidメインスレッド対応

    // Test
    testImplementation(libs.junit)                  // JUnit（ユニットテスト）
    androidTestImplementation(libs.androidx.junit)   // AndroidX JUnit
    androidTestImplementation(libs.androidx.espresso.core) // Espresso（UIテスト）
}
```

#### 依存関係のスコープ

| スコープ | 説明 |
|---------|------|
| `implementation` | コンパイル時・実行時に使用 |
| `ksp` | KSPアノテーション処理に使用（コード生成） |
| `debugImplementation` | デバッグビルドのみで使用 |
| `testImplementation` | ユニットテストのみで使用 |
| `androidTestImplementation` | Androidインストルメンテーションテストのみで使用 |

#### BOM（Bill of Materials）

```kotlin
implementation(platform(libs.compose.bom))
```

BOMを使うと、Compose関連ライブラリのバージョンを一括管理できます。個別にバージョンを指定する必要がなくなり、互換性の問題を防げます。

---

## 4. バージョンカタログ (libs.versions.toml)

**ファイル**: `gradle/libs.versions.toml`

### 構成

```toml
[versions]      # バージョン番号の定義
[libraries]     # ライブラリ（依存関係）の定義
[plugins]       # プラグインの定義
```

### バージョン定義

```toml
[versions]
agp = "8.7.3"          # Android Gradle Plugin
kotlin = "2.0.21"      # Kotlin
ksp = "2.0.21-1.0.27"  # Kotlin Symbol Processing
composeBom = "2024.12.01"  # Compose BOM
hilt = "2.56.1"        # Hilt
retrofit = "2.11.0"    # Retrofit
okhttp = "4.12.0"      # OkHttp
gson = "2.13.2"        # Gson
coil = "2.7.0"         # Coil
coroutines = "1.9.0"   # Coroutines
```

### ライブラリ参照の流れ

```
libs.versions.toml          build.gradle.kts
─────────────────           ────────────────
[versions]
hilt = "2.56.1"     ←──── version.ref = "hilt"
                            │
[libraries]                 ↓
hilt-android = {            implementation(libs.hilt.android)
  group = "com.google.dagger",
  name = "hilt-android",
  version.ref = "hilt"
}
```

### プラグイン定義

```toml
[plugins]
android-application = { id = "com.android.application", version.ref = "agp" }
kotlin-android = { id = "org.jetbrains.kotlin.android", version.ref = "kotlin" }
kotlin-compose = { id = "org.jetbrains.kotlin.plugin.compose", version.ref = "kotlin" }
ksp = { id = "com.google.devtools.ksp", version.ref = "ksp" }
hilt = { id = "com.google.dagger.hilt.android", version.ref = "hilt" }
```

---

## 5. AndroidManifest.xml

**ファイル**: `app/src/main/AndroidManifest.xml`

```xml
<?xml version="1.0" encoding="utf-8"?>
<manifest xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:tools="http://schemas.android.com/tools">

    <uses-permission android:name="android.permission.INTERNET" />

    <application
        android:name=".MeshiRouletteApplication"
        android:allowBackup="true"
        android:dataExtractionRules="@xml/data_extraction_rules"
        android:fullBackupContent="@xml/backup_rules"
        android:icon="@mipmap/ic_launcher"
        android:label="@string/app_name"
        android:roundIcon="@mipmap/ic_launcher_round"
        android:supportsRtl="true"
        android:theme="@style/Theme.Androidapp"
        tools:targetApi="31">
        <activity
            android:name=".MainActivity"
            android:exported="true"
            android:theme="@style/Theme.Androidapp">
            <intent-filter>
                <action android:name="android.intent.action.MAIN" />
                <category android:name="android.intent.category.LAUNCHER" />
            </intent-filter>
        </activity>
    </application>
</manifest>
```

### 解説

| 属性 | 説明 |
|------|------|
| `uses-permission INTERNET` | インターネット接続の権限を要求（API通信に必須） |
| `android:name=".MeshiRouletteApplication"` | アプリケーションクラスを指定（Hiltの`@HiltAndroidApp`） |
| `android:allowBackup="true"` | アプリデータの自動バックアップを許可 |
| `android:icon` / `android:roundIcon` | アプリアイコン（通常/丸型） |
| `android:label="@string/app_name"` | アプリ名（`strings.xml`で定義） |
| `android:supportsRtl="true"` | RTL（右→左）レイアウトをサポート |
| `android:exported="true"` | MainActivityを外部から起動可能にする（ランチャーに必須） |
| `intent-filter MAIN + LAUNCHER` | ホーム画面のアプリ一覧に表示し、タップで起動する |

---

## 6. settings.gradle.kts

```kotlin
rootProject.name = "androidapp"
include(":app")
```

- **`rootProject.name`**: プロジェクトのルート名
- **`include(":app")`**: `app/`ディレクトリをサブモジュールとして含める
