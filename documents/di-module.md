# DI設定 (Dependency Injection) 詳細解説

依存性注入（DI）は、クラスが必要とする依存オブジェクトを外部から注入する設計パターンです。本プロジェクトでは **Hilt** を使用しています。

---

## Hiltの基本概念

### なぜDIを使うのか？

```kotlin
// ❌ DIなし（クラスが自分で依存を生成する）
class RestaurantRepositoryImpl {
    private val apiService = Retrofit.Builder()
        .baseUrl("...")
        .build()
        .create(ApiService::class.java)  // 毎回インスタンスを作ってしまう
}

// ✅ DIあり（外部からインスタンスを注入する）
class RestaurantRepositoryImpl @Inject constructor(
    private val apiService: ApiService  // Hiltが自動で注入してくれる
)
```

**メリット**:
- **シングルトン管理**: 同じインスタンスをアプリ全体で共有できる
- **テスタビリティ**: テスト時にモックに差し替えられる
- **疎結合**: クラスが具体的な生成方法を知らなくていい

### Hiltの動作の仕組み

```
1. @HiltAndroidApp (Application) → DIコンテナの起点
2. @Module + @InstallIn → 依存の提供方法を定義
3. @Inject constructor → 依存の注入先を指定
4. @AndroidEntryPoint (Activity) → Activity でのDI有効化
5. @HiltViewModel → ViewModel でのDI有効化
```

---

## 1. NetworkModule（ネットワーク設定）

**ファイル**: `di/NetworkModule.kt`

```kotlin
@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    @Provides
    @Singleton
    fun provideLoggingInterceptor(): HttpLoggingInterceptor { ... }

    @Provides
    @Singleton
    fun provideOkHttpClient(loggingInterceptor: HttpLoggingInterceptor): OkHttpClient { ... }

    @Provides
    @Singleton
    fun provideRetrofit(okHttpClient: OkHttpClient): Retrofit { ... }

    @Provides
    @Singleton
    fun provideApiService(retrofit: Retrofit): ApiService { ... }
}
```

### アノテーションの意味

| アノテーション | 説明 |
|-------------|------|
| `@Module` | Hiltにこのクラスが依存の提供方法を定義するモジュールであることを伝える |
| `@InstallIn(SingletonComponent::class)` | アプリ全体（シングルトンスコープ）で有効にする |
| `@Provides` | このメソッドが特定の型のインスタンスを提供することを宣言 |
| `@Singleton` | アプリのライフサイクル内で1つだけインスタンスを生成（共有） |

### 依存関係の連鎖

```
provideLoggingInterceptor()
    │ HttpLoggingInterceptor を返す
    ▼
provideOkHttpClient(loggingInterceptor)
    │ OkHttpClient を返す（loggingInterceptorを組み込む）
    ▼
provideRetrofit(okHttpClient)
    │ Retrofit を返す（okHttpClientを使用）
    ▼
provideApiService(retrofit)
    │ ApiService を返す（retrofitから生成）
    ▼
RestaurantRepositoryImpl に注入される
```

Hiltは引数の型を見て自動的に依存関係を解決します。

### 各メソッドの詳細

#### provideLoggingInterceptor

```kotlin
@Provides
@Singleton
fun provideLoggingInterceptor(): HttpLoggingInterceptor {
    return HttpLoggingInterceptor().apply {
        level = if (BuildConfig.DEBUG) {
            HttpLoggingInterceptor.Level.BODY    // デバッグ時: リクエスト/レスポンスの全内容をログ出力
        } else {
            HttpLoggingInterceptor.Level.NONE    // リリース時: ログ出力なし
        }
    }
}
```

HTTPリクエスト/レスポンスのログ出力設定です。

**ログレベル**:
| レベル | 出力内容 |
|-------|---------|
| `NONE` | ログなし |
| `BASIC` | リクエストURL、メソッド、レスポンスコードのみ |
| `HEADERS` | BASIC + ヘッダー情報 |
| `BODY` | HEADERS + リクエスト/レスポンスのボディ全体 |

`BuildConfig.DEBUG`はデバッグビルドの場合に`true`になるフラグです。

#### provideOkHttpClient

```kotlin
@Provides
@Singleton
fun provideOkHttpClient(
    loggingInterceptor: HttpLoggingInterceptor  // ← Hiltが自動で前のメソッドの結果を注入
): OkHttpClient {
    return OkHttpClient.Builder()
        .addInterceptor(loggingInterceptor)       // ログインターセプタを追加
        .connectTimeout(30, TimeUnit.SECONDS)     // 接続タイムアウト: 30秒
        .readTimeout(30, TimeUnit.SECONDS)        // 読み取りタイムアウト: 30秒
        .writeTimeout(30, TimeUnit.SECONDS)       // 書き込みタイムアウト: 30秒
        .build()
}
```

OkHttp（HTTP通信ライブラリ）のクライアント設定です。

**タイムアウト設定**:
- `connectTimeout`: サーバーへの接続確立のタイムアウト
- `readTimeout`: サーバーからのレスポンス読み取りのタイムアウト
- `writeTimeout`: サーバーへのリクエスト送信のタイムアウト

#### provideRetrofit

```kotlin
@Provides
@Singleton
fun provideRetrofit(okHttpClient: OkHttpClient): Retrofit {
    return Retrofit.Builder()
        .baseUrl(BuildConfig.BASE_URL)                      // ベースURL
        .client(okHttpClient)                                // OkHttpクライアントを使用
        .addConverterFactory(GsonConverterFactory.create())  // JSON変換にGsonを使用
        .build()
}
```

Retrofit（REST APIクライアントライブラリ）の設定です。

- **`BuildConfig.BASE_URL`**: `build.gradle.kts`で定義された`"https://fukudaifood-130668695114.asia-northeast1.run.app/"`
- **`GsonConverterFactory`**: JSON ↔ Kotlinオブジェクトの自動変換を担当

#### provideApiService

```kotlin
@Provides
@Singleton
fun provideApiService(retrofit: Retrofit): ApiService {
    return retrofit.create(ApiService::class.java)
}
```

Retrofitが`ApiService`インターフェースの実装を**動的に生成**します。Javaのダイナミックプロキシを使用して、インターフェースのメソッド定義からHTTPリクエストを自動構築します。

---

## 2. RepositoryModule（Repository設定）

**ファイル**: `di/RepositoryModule.kt`

```kotlin
@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindRestaurantRepository(
        impl: RestaurantRepositoryImpl
    ): RestaurantRepository
}
```

### 解説

#### `@Binds` vs `@Provides`

| 比較 | `@Provides` | `@Binds` |
|------|------------|---------|
| 使う場面 | インスタンスを生成して返す場合 | インターフェースと実装を紐付ける場合 |
| メソッド | `fun provide...(): 型 { return ... }` | `abstract fun bind...(impl): インターフェース` |
| クラス | `object`（具象クラス） | `abstract class`（抽象クラス） |
| パフォーマンス | やや重い | 軽い（実装の生成コードを省略できる） |

#### このモジュールが行っていること

```
「RestaurantRepository型が要求されたら、RestaurantRepositoryImplのインスタンスを渡してね」
```

という紐付けを宣言しています。

```kotlin
// UseCase内（ドメイン層）
class GetRestaurantsUseCase @Inject constructor(
    private val repository: RestaurantRepository  // ← インターフェース型で受け取る
)

// Hiltが自動的に以下を解決:
// RestaurantRepository → RestaurantRepositoryImpl のインスタンスを注入
```

これにより、UseCaseやViewModelは`RestaurantRepository`インターフェースだけに依存し、`RestaurantRepositoryImpl`の存在を知りません。

---

## 3. Hilt設定ファイル一覧

### AndroidManifest.xml

```xml
<application
    android:name=".MeshiRouletteApplication"
    ...>
```

`MeshiRouletteApplication`（`@HiltAndroidApp`付き）をアプリケーションクラスとして登録。

### UseCaseのDI

UseCaseには`@Module`が不要です。`@Inject constructor`だけでHiltが自動的にインスタンスを生成・注入します。

```kotlin
class GetRestaurantsUseCase @Inject constructor(
    private val repository: RestaurantRepository
)
```

Hiltは以下を自動解決:
1. `RestaurantRepository`型 → `RepositoryModule`の`@Binds`定義により`RestaurantRepositoryImpl`
2. `RestaurantRepositoryImpl`のコンストラクタ引数`ApiService` → `NetworkModule`の`@Provides`定義

### DIの全体像

```
@HiltAndroidApp (MeshiRouletteApplication)
    │
    ├── NetworkModule (@Module)
    │   ├── HttpLoggingInterceptor  (@Provides @Singleton)
    │   ├── OkHttpClient            (@Provides @Singleton)
    │   ├── Retrofit                (@Provides @Singleton)
    │   └── ApiService              (@Provides @Singleton)
    │
    ├── RepositoryModule (@Module)
    │   └── RestaurantRepository ← RestaurantRepositoryImpl  (@Binds @Singleton)
    │
    ├── 自動解決 (@Inject constructor)
    │   ├── RestaurantRepositoryImpl(apiService)
    │   ├── GetRestaurantsUseCase(repository)
    │   ├── GetRestaurantDetailUseCase(repository)
    │   └── SpinRouletteUseCase(repository)
    │
    └── @HiltViewModel (@AndroidEntryPoint → Activity → Screen)
        ├── RouletteViewModel(spinRouletteUseCase)
        ├── RestaurantListViewModel(getRestaurantsUseCase)
        └── RestaurantDetailViewModel(getRestaurantDetailUseCase, savedStateHandle)
```
