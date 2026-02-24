# 03 重要な実装パターン・概念

> 生成日: 2026-02-24 | skill: code-learner

---

## 1. Flow + Coroutines による非同期処理

このアプリの通信処理はすべて Kotlin **Flow** と **Coroutines** で実装されています。

### 基本パターン

```kotlin
// Repository での Flow 生成
fun getRestaurants(...): Flow<NetworkResult<List<Restaurant>>> = flow {
    emit(NetworkResult.Loading)     // ① まず Loading を流す
    try {
        val response = apiService.getRestaurants(...)  // ② suspend 関数を呼ぶ
        if (response.isSuccessful) {
            emit(NetworkResult.Success(response.body()!!.toDomain()))  // ③ 成功
        } else {
            emit(NetworkResult.Error("エラー", response.code()))        // ④ HTTP エラー
        }
    } catch (e: Exception) {
        emit(NetworkResult.Error(e.message ?: "不明なエラー"))           // ⑤ 例外
    }
}

// ViewModel での収集
viewModelScope.launch {
    getRestaurantsUseCase(...).collect { result ->  // Flow を collect する
        when (result) {
            is NetworkResult.Loading -> { /* ... */ }
            is NetworkResult.Success -> { /* result.data で結果を取得 */ }
            is NetworkResult.Error -> { /* result.message でエラー内容 */ }
        }
    }
}
```

**ポイント**:
- `flow { }` ブロックの中で `emit()` するたびに、`collect { }` に値が届く
- `viewModelScope.launch` で起動するため、ViewModel が破棄されると自動的にキャンセル
- `suspend fun` は一時停止できる関数。スレッドをブロックしない

---

## 2. Hilt による依存性注入 (DI)

DI は「クラスが必要とするオブジェクトを外から渡す」仕組みです。

### 注入の流れ

```kotlin
// ① @Provides でオブジェクトの作り方を定義
@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {
    @Provides
    @Singleton
    fun provideApiService(retrofit: Retrofit): ApiService {
        return retrofit.create(ApiService::class.java)
    }
}

// ② @Binds でインターフェースと実装を紐付け
@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {
    @Binds
    abstract fun bindRestaurantRepository(
        impl: RestaurantRepositoryImpl  // 実装クラス
    ): RestaurantRepository             // インターフェース
}

// ③ @Inject constructor で自動注入を有効化
class GetRestaurantsUseCase @Inject constructor(
    private val repository: RestaurantRepository  // Hilt が自動的に渡してくれる
)

// ④ ViewModel は @HiltViewModel + @Inject constructor
@HiltViewModel
class RouletteViewModel @Inject constructor(
    private val spinRouletteUseCase: SpinRouletteUseCase,
    private val getRestaurantsUseCase: GetRestaurantsUseCase
) : ViewModel()

// ⑤ Composable では hiltViewModel() で取得
@Composable
fun RouletteScreen(viewModel: RouletteViewModel = hiltViewModel())
```

**なぜ DI を使うか?**
- テスト時に `RestaurantRepositoryImpl` の代わりにモックを渡せる
- クラス間の依存関係が明確になる
- オブジェクトのライフサイクル管理が自動化される

---

## 3. Navigation Compose による画面遷移

### ルート定義

```kotlin
// presentation/navigation/NavGraph.kt

// 画面のルート (URL のようなもの)
sealed class Screen(val route: String) {
    data object Roulette : Screen("roulette")
    data object List : Screen("list")
    data object Detail : Screen("detail/{restaurantId}") {
        // 引数入りのルートを生成するヘルパー
        fun createRoute(restaurantId: String) = "detail/$restaurantId"
    }
}
```

### NavHost の定義

```kotlin
NavHost(
    navController = navController,
    startDestination = Screen.Roulette.route  // 起動時の初期画面
) {
    composable(Screen.Roulette.route) {
        RouletteScreen(
            onNavigateToDetail = { id ->
                navController.navigate(Screen.Detail.createRoute(id))
            }
        )
    }
    composable(
        route = Screen.Detail.route,
        arguments = listOf(navArgument("restaurantId") { type = NavType.StringType })
    ) {
        RestaurantDetailScreen(
            onNavigateBack = { navController.popBackStack() }
        )
    }
}
```

### 詳細画面での引数の受け取り

```kotlin
// 詳細 ViewModel が SavedStateHandle で自動的に受け取る
@HiltViewModel
class RestaurantDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle  // NavGraph の arguments が入る
) : ViewModel() {
    private val restaurantId: String = checkNotNull(savedStateHandle["restaurantId"])
}
```

### ボトムナビゲーションの遷移

```kotlin
NavigationBarItem(
    onClick = {
        navController.navigate(item.route) {
            // 戻るスタックがルートより深くなった場合、ルートまで pop する
            popUpTo(navController.graph.findStartDestination().id) {
                saveState = true   // 状態を保存
            }
            launchSingleTop = true  // 同じ画面を重複して積まない
            restoreState = true     // 以前の状態を復元
        }
    }
)
```

---

## 4. Material Design 3 + Jetpack Compose の UI パターン

### Scaffold によるレイアウト

```kotlin
Scaffold(
    topBar = { /* TopAppBar */ },
    bottomBar = { /* NavigationBar */ },
    floatingActionButton = { /* FAB */ }
) { innerPadding ->
    // コンテンツエリア。Scaffold が bottom bar 分の padding を自動計算
    Content(modifier = Modifier.padding(innerPadding))
}
```

### ModalBottomSheet (フィルター)

```kotlin
if (uiState.showFilterSheet) {
    ModalBottomSheet(
        onDismissRequest = { viewModel.hideFilterSheet() },
        sheetState = rememberModalBottomSheetState()
    ) {
        FilterBottomSheet(
            selectedGenres = uiState.selectedGenres,
            onGenreToggled = { viewModel.toggleGenre(it) },
            // ...
        )
    }
}
```

### Coil による画像ロード

```kotlin
AsyncImage(
    model = restaurant.imageUrl,     // URL を渡すだけで非同期ロード
    contentDescription = restaurant.name,
    modifier = Modifier
        .size(80.dp)
        .clip(MaterialTheme.shapes.small),
    contentScale = ContentScale.Crop,
    placeholder = painterResource(R.drawable.placeholder),  // ロード中の画像
    error = painterResource(R.drawable.error_image)         // エラー時の画像
)
```

---

## 5. DTO ↔ ドメインモデルの変換パターン

API が返す `RestaurantDto` はそのままアプリ内で使わず、ドメインモデル `Restaurant` に変換します。

```
RestaurantDto                  Restaurant
─────────────────             ──────────────────
id: String?         →         id: String          (null → "")
genre: String?      →         genre: Genre         ("定食" → Genre.TEISHOKU)
price_range: Int?   →         priceRange: PriceRange (1 → PriceRange.CHEAP)
```

**なぜ変換するか?**
- DTO の null 許容型をドメインモデルで non-null にすることで、UI での null チェックが不要
- API の数値 (`1`, `2`, `3`) を意味のある Enum に変換することで型安全になる
- API の仕様が変わっても、`toDomain()` だけ修正すれば済む

---

## 6. enableEdgeToEdge とシステムバー対応

```kotlin
// MainActivity.kt
override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()  // ← コンテンツをシステムバー(ステータスバー/ナビゲーションバー)の下まで広げる
    setContent {
        MeshiRouletteTheme {
            MainNavigation()
        }
    }
}
```

`Scaffold` が `innerPadding` を提供するため、コンテンツがシステムバーと被らない。

---

## 7. 命名規則

| 対象 | 規則 | 例 |
|-----|------|---|
| Composable | PascalCase + `Screen`/`Content`/`Card` suffix | `RouletteScreen`, `RestaurantCard` |
| ViewModel | PascalCase + `ViewModel` suffix | `RouletteViewModel` |
| UiState | PascalCase + `UiState` suffix | `RouletteUiState` |
| UseCase | PascalCase + `UseCase` suffix | `SpinRouletteUseCase` |
| Repository | PascalCase + `Repository` suffix | `RestaurantRepository` |
| RepositoryImpl | PascalCase + `RepositoryImpl` suffix | `RestaurantRepositoryImpl` |
| DTO | PascalCase + `Dto` suffix | `RestaurantDto` |
| DI Module | PascalCase + `Module` suffix | `NetworkModule` |
| private StateFlow | `_` prefix | `_uiState` |
| public StateFlow | no prefix | `uiState` |

---

## 8. 初学者がつまずきやすいポイント

### Q: `_uiState` と `uiState` の 2 つある理由は?

```kotlin
// ViewModel 内部: MutableStateFlow で書き込み可能
private val _uiState = MutableStateFlow(RouletteUiState())

// 外部公開: StateFlow (読み取り専用) にキャスト
val uiState: StateFlow<RouletteUiState> = _uiState.asStateFlow()
```

Screen が `_uiState` に直接書き込むことを防ぐため。

---

### Q: `collectAsStateWithLifecycle()` vs `collectAsState()` の違いは?

`collectAsStateWithLifecycle()` はライフサイクルを考慮し、アプリがバックグラウンドに行ったときに収集を停止します。バッテリー節約のために `collectAsStateWithLifecycle()` を使います。

---

### Q: `hiltViewModel()` はいつ ViewModel を作るか?

Navigation 画面の Composable が初めて表示されたときに作成され、その画面が NavBackStack から取り除かれるまで保持されます。画面の再構成 (例: 画面回転) では破棄されません。

---

### Q: `viewModelScope.launch` の中で UI を更新できる理由は?

`StateFlow.update()` はスレッドセーフです。IO スレッドで通信してメインスレッドで UI 更新...という手間なく、`_uiState.update { }` を呼ぶだけで Compose が自動的にメインスレッドで再コンポジションします。

---

### Q: `LaunchedEffect` と `viewModelScope.launch` の使い分けは?

| | LaunchedEffect | viewModelScope.launch |
|--|----------------|----------------------|
| 場所 | Composable 内 | ViewModel 内 |
| ライフサイクル | Composable が画面から消えると停止 | ViewModel が破棄されると停止 |
| 用途 | アニメーション, 画面初回ロード | API 通信, 状態更新 |

ルーレットのスロットアニメーションは `LaunchedEffect` を使い、API 通信は `viewModelScope.launch` を使っています。
