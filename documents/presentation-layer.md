# プレゼンテーション層 (Presentation Layer) 詳細解説

プレゼンテーション層はUIの描画、ユーザーインタラクション、画面状態の管理を担当するレイヤーです。

---

## 目次

1. [エントリーポイント（Application / Activity）](#1-エントリーポイント)
2. [テーマ設定](#2-テーマ設定)
3. [ナビゲーション](#3-ナビゲーション)
4. [共通コンポーネント](#4-共通コンポーネント)
5. [ルーレット画面](#5-ルーレット画面)
6. [レストラン一覧画面](#6-レストラン一覧画面)
7. [レストラン詳細画面](#7-レストラン詳細画面)

---

## 1. エントリーポイント

### MeshiRouletteApplication

**ファイル**: `MeshiRouletteApplication.kt`

```kotlin
@HiltAndroidApp
class MeshiRouletteApplication : Application()
```

#### 解説

- **`@HiltAndroidApp`**: Hiltの依存性注入を有効にするための必須アノテーション。アプリ全体のDIコンテナの起点となる
- **`Application`クラス**: Androidアプリのグローバルな状態を管理するクラス。アプリのプロセスが起動されたときに最初にインスタンス化される
- このクラスは`AndroidManifest.xml`の`android:name=".MeshiRouletteApplication"`で参照されている

### MainActivity

**ファイル**: `MainActivity.kt`

```kotlin
@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MeshiRouletteTheme {
                MainNavigation()
            }
        }
    }
}
```

#### 解説

| 要素 | 説明 |
|------|------|
| `@AndroidEntryPoint` | Hiltが依存性注入を行うActivity/Fragmentに必須のアノテーション |
| `ComponentActivity` | Jetpack Composeを使用するための基本Activity |
| `enableEdgeToEdge()` | ステータスバーやナビゲーションバーの裏側までUIを描画する（Android 15+のEdge-to-Edge対応） |
| `setContent { }` | Jetpack ComposeのUIをセットする。XMLレイアウトの代わりにComposeを使用 |
| `MeshiRouletteTheme { }` | アプリ全体にMaterial3テーマを適用 |
| `MainNavigation()` | ナビゲーション（画面遷移）の起点 |

---

## 2. テーマ設定

**ファイル**: `presentation/theme/Theme.kt`

```kotlin
@Composable
fun MeshiRouletteTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        content = content
    )
}
```

#### 解説

Material3のテーマを定義するComposable関数です。

#### カラースキーム選択ロジック

```
Android 12 (API 31) 以上 かつ dynamicColor=true
    → Dynamic Color（端末の壁紙に合わせた色）を使用
それ以外でダークモード
    → DarkColorScheme（カスタム定義のダークカラー）
それ以外
    → LightColorScheme（カスタム定義のライトカラー）
```

#### Dynamic Color とは

Android 12以降で利用可能な機能で、ユーザーの壁紙からカラーパレットを自動生成します。`dynamicLightColorScheme(context)` / `dynamicDarkColorScheme(context)` で取得できます。

#### LightColorScheme / DarkColorScheme

Material3 のカラーシステムに基づいた色定義です。主要な色:

| カラーロール | ライト | ダーク | 用途 |
|------------|-------|--------|------|
| `primary` | `#6750A4` (紫) | `#D0BCFF` (薄紫) | ボタン、アクセント |
| `secondary` | `#625B71` | `#CCC2DC` | フィルターチップ等 |
| `background` | `#FFFBFE` (白) | `#1C1B1F` (黒) | 背景色 |
| `error` | `#B3261E` (赤) | `#F2B8B5` (薄赤) | エラー表示 |

---

## 3. ナビゲーション

**ファイル**: `presentation/navigation/NavGraph.kt`

### Screen（画面ルート定義）

```kotlin
sealed class Screen(val route: String) {
    data object Roulette : Screen("roulette")
    data object List : Screen("list")
    data object Detail : Screen("detail/{restaurantId}") {
        fun createRoute(restaurantId: String) = "detail/$restaurantId"
    }
}
```

#### 解説

Sealed classで画面のルート（URL的な識別子）を型安全に定義しています。

| 画面 | ルート文字列 | パラメータ |
|------|------------|-----------|
| ルーレット | `"roulette"` | なし |
| 一覧 | `"list"` | なし |
| 詳細 | `"detail/{restaurantId}"` | `restaurantId` (パス変数) |

`Detail.createRoute("abc123")` → `"detail/abc123"` のように動的にルートを生成します。

### BottomNavItem（ボトムナビゲーション項目）

```kotlin
sealed class BottomNavItem(
    val route: String,
    val title: String,
    val icon: ImageVector
) {
    data object Roulette : BottomNavItem(
        route = Screen.Roulette.route,
        title = "ルーレット",
        icon = Icons.Default.Casino
    )
    data object List : BottomNavItem(
        route = Screen.List.route,
        title = "一覧",
        icon = Icons.Default.List
    )
}
```

下部ナビゲーションバーに表示する2つのタブを定義:
- **ルーレット**: カジノアイコン
- **一覧**: リストアイコン

### MainNavigation（メインナビゲーション）

```kotlin
@Composable
fun MainNavigation() {
    val navController = rememberNavController()
    val bottomNavItems = listOf(BottomNavItem.Roulette, BottomNavItem.List)

    Scaffold(
        bottomBar = { /* ボトムナビゲーションバー */ }
    ) { paddingValues ->
        NavHost(
            navController = navController,
            startDestination = Screen.Roulette.route,
            modifier = Modifier.padding(paddingValues)
        ) {
            composable(Screen.Roulette.route) { RouletteScreen(...) }
            composable(Screen.List.route) { RestaurantListScreen(...) }
            composable(Screen.Detail.route, arguments = ...) { RestaurantDetailScreen(...) }
        }
    }
}
```

#### 解説（行ごとに分解）

**`rememberNavController()`**: ナビゲーションの状態を保持するコントローラーを生成。`remember`により再コンポジション時に状態を保持する。

**`Scaffold`**: Material3の基本レイアウト構造。`topBar`, `bottomBar`, `floatingActionButton`などを配置できる。`paddingValues`でコンテンツがバーと重ならないようにする。

**ボトムバーの表示制御**:
```kotlin
val showBottomBar = bottomNavItems.any { item ->
    currentDestination?.hierarchy?.any { it.route == item.route } == true
}
```
現在の画面がボトムナビの項目（ルーレット or 一覧）の場合のみボトムバーを表示。詳細画面では非表示になる。

**`NavHost`**: ナビゲーションのコンテナ。`startDestination`で初期画面をルーレット画面に設定。

**ナビゲーションのオプション**:
```kotlin
navController.navigate(item.route) {
    popUpTo(navController.graph.findStartDestination().id) {
        saveState = true         // 離れる画面の状態を保存
    }
    launchSingleTop = true       // 同じ画面の多重スタックを防止
    restoreState = true          // 以前の状態を復元
}
```

**画面遷移の引数**:
```kotlin
composable(
    route = Screen.Detail.route,  // "detail/{restaurantId}"
    arguments = listOf(
        navArgument("restaurantId") { type = NavType.StringType }
    )
)
```
`{restaurantId}`の部分を文字列型のパラメータとして受け取る設定。ViewModelが`SavedStateHandle`から自動で取得する。

---

## 4. 共通コンポーネント

### RestaurantCard（レストランカード）

**ファイル**: `presentation/components/RestaurantCard.kt`

```kotlin
@Composable
fun RestaurantCard(
    restaurant: Restaurant,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) { ... }
```

#### 解説

レストラン一覧で使われる各アイテムのカードUIです。

**レイアウト構造**:
```
┌──────────────────────────────────────┐
│ ┌──────┐  レストラン名               │
│ │ 画像  │  ジャンル | 価格帯          │
│ │80x80 │  住所（1行、省略あり）       │
│ └──────┘                             │
└──────────────────────────────────────┘
```

- **`Card`**: Material3のカードコンポーネント。影（elevation: 2dp）付き
- **`AsyncImage`**: Coilライブラリの非同期画像読み込みComposable。URLから画像を自動取得して表示
  - `ContentScale.Crop`: 画像をコンテナに合わせてトリミング
  - `Modifier.clip(MaterialTheme.shapes.small)`: 角丸でクリップ
- **`TextOverflow.Ellipsis`**: テキストが長い場合に`...`で省略

### ErrorContent（エラー表示）

**ファイル**: `presentation/components/ErrorContent.kt`

```kotlin
@Composable
fun ErrorContent(
    message: String,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier
) { ... }
```

#### 解説

エラー発生時に表示するUIです。

**レイアウト構造**:
```
         エラーが発生しました
           [エラーメッセージ]
             [再試行]
```

- 画面中央にテキストとボタンを縦に配置
- `onRetry`コールバックでリトライ処理をトリガー

### LoadingContent（ローディング表示）

**ファイル**: `presentation/components/LoadingContent.kt`

```kotlin
@Composable
fun LoadingContent(
    modifier: Modifier = Modifier
) { ... }
```

#### 解説

データ読み込み中に表示するUIです。

- `Box`で画面全体を覆い、中央に`CircularProgressIndicator`（くるくる回るインジケーター）を表示

### FilterBottomSheet（フィルターシート）

**ファイル**: `presentation/components/FilterBottomSheet.kt`

```kotlin
@Composable
fun FilterBottomSheet(
    selectedGenre: Genre,
    selectedPriceRange: PriceRange,
    onGenreSelected: (Genre) -> Unit,
    onPriceRangeSelected: (PriceRange) -> Unit,
    onDismiss: () -> Unit
) { ... }
```

#### 解説

ジャンルと価格帯を選択するボトムシートUIです。

**レイアウト構造**:
```
┌──────────────────────────────────┐
│ ジャンル                          │
│ [すべて] [定食] [中華] [ラーメン]  │
│ [カレー] [うどん] [牛丼] ...      │
│                                   │
│ 価格帯                            │
│ [すべて] [〜500円] [500〜1000円]  │
│ [1000円〜]                        │
└──────────────────────────────────┘
```

- **`ModalBottomSheet`**: Material3のモーダルボトムシート。画面下からスライドして表示
- **`rememberModalBottomSheetState()`**: ボトムシートの展開/折りたたみ状態を管理
- **`FlowRow`**: 子要素を自動折り返しで横に並べるレイアウト。チップが画面幅を超えると次の行に折り返す
- **`FilterChip`**: Material3のフィルターチップ。選択状態（`selected`）によって見た目が変わる
- **`Genre.entries`** / **`PriceRange.entries`**: Enum の全エントリを取得して`forEach`でチップを生成

---

## 5. ルーレット画面

### RouletteUiState

**ファイル**: `presentation/roulette/RouletteUiState.kt`

```kotlin
data class RouletteUiState(
    val isSpinning: Boolean = false,
    val selectedRestaurant: Restaurant? = null,
    val selectedGenre: Genre = Genre.ALL,
    val selectedPriceRange: PriceRange = PriceRange.ALL,
    val showFilterSheet: Boolean = false,
    val error: String? = null
)
```

#### 解説

ルーレット画面の全UI状態を一つのdata classにまとめたものです。

| フィールド | 型 | デフォルト値 | 説明 |
|-----------|------|------------|------|
| `isSpinning` | `Boolean` | `false` | ルーレット回転中かどうか |
| `selectedRestaurant` | `Restaurant?` | `null` | 選ばれたレストラン（なければnull） |
| `selectedGenre` | `Genre` | `Genre.ALL` | 選択中のジャンルフィルター |
| `selectedPriceRange` | `PriceRange` | `PriceRange.ALL` | 選択中の価格帯フィルター |
| `showFilterSheet` | `Boolean` | `false` | フィルターシートの表示/非表示 |
| `error` | `String?` | `null` | エラーメッセージ（なければnull） |

### RouletteViewModel

**ファイル**: `presentation/roulette/RouletteViewModel.kt`

```kotlin
@HiltViewModel
class RouletteViewModel @Inject constructor(
    private val spinRouletteUseCase: SpinRouletteUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(RouletteUiState())
    val uiState: StateFlow<RouletteUiState> = _uiState.asStateFlow()

    fun spinRoulette() { ... }
    fun setGenre(genre: Genre) { ... }
    fun setPriceRange(priceRange: PriceRange) { ... }
    fun showFilterSheet() { ... }
    fun hideFilterSheet() { ... }
    fun clearResult() { ... }
    fun clearError() { ... }
}
```

#### 解説

##### クラス定義

- **`@HiltViewModel`**: HiltにこのViewModelを管理させるアノテーション。`hiltViewModel()`で取得可能になる
- **`@Inject constructor`**: Hiltがコンストラクタ引数（`SpinRouletteUseCase`）を自動で注入する
- **`: ViewModel()`**: AndroidのViewModelを継承。画面回転時などでも状態が保持される

##### StateFlow（状態管理）

```kotlin
private val _uiState = MutableStateFlow(RouletteUiState())  // 内部用（変更可能）
val uiState: StateFlow<RouletteUiState> = _uiState.asStateFlow()  // 外部用（読み取り専用）
```

- **`MutableStateFlow`**: 値の変更が可能なStateFlow。ViewModel内部でのみ変更する
- **`asStateFlow()`**: 読み取り専用の`StateFlow`に変換。Screenからは読むことしかできない
- このパターンを**バッキングプロパティ**と呼ぶ。`_`プレフィックス付きがprivate、なしがpublic

##### spinRoulette（ルーレット実行）

```kotlin
fun spinRoulette() {
    viewModelScope.launch {
        spinRouletteUseCase(
            genre = _uiState.value.selectedGenre,
            priceRange = _uiState.value.selectedPriceRange
        ).collect { result ->
            when (result) {
                is NetworkResult.Loading -> {
                    _uiState.update { it.copy(isSpinning = true, error = null) }
                }
                is NetworkResult.Success -> {
                    _uiState.update {
                        it.copy(isSpinning = false, selectedRestaurant = result.data, error = null)
                    }
                }
                is NetworkResult.Error -> {
                    _uiState.update { it.copy(isSpinning = false, error = result.message) }
                }
            }
        }
    }
}
```

処理の流れ:

1. **`viewModelScope.launch`**: ViewModelのライフサイクルに紐づいたコルーチンスコープでコルーチンを起動
2. **`spinRouletteUseCase(...)`**: UseCaseを関数として呼び出し（`operator fun invoke`）
3. **`.collect { }`**: Flowから値を順次受け取る
4. **`_uiState.update { it.copy(...) }`**: 現在のUiStateの一部だけを変更した新しいインスタンスを生成して状態を更新

##### 状態更新のパターン

`_uiState.update { it.copy(...) }` は以下のように動作する:
- `update`はアトミック（スレッドセーフ）な更新
- `it`は現在の`RouletteUiState`
- `copy(...)`はdata classのコピー関数。指定したフィールドだけ変更した新しいインスタンスを返す

### RouletteScreen

**ファイル**: `presentation/roulette/RouletteScreen.kt`

```kotlin
@Composable
fun RouletteScreen(
    onNavigateToDetail: (String) -> Unit,
    viewModel: RouletteViewModel = hiltViewModel()
) { ... }
```

#### 解説

##### パラメータ

| パラメータ | 型 | 説明 |
|-----------|------|------|
| `onNavigateToDetail` | `(String) -> Unit` | 詳細画面への遷移コールバック（レストランIDを渡す） |
| `viewModel` | `RouletteViewModel` | Hiltが自動注入するViewModel |

##### 状態の取得

```kotlin
val uiState by viewModel.uiState.collectAsStateWithLifecycle()
```

- **`collectAsStateWithLifecycle()`**: StateFlowをCompose Stateに変換。ライフサイクルに対応しており、画面がバックグラウンドに行ったら収集を停止する
- **`by`**: Kotlinの委譲プロパティ。`uiState.value`ではなく`uiState`で直接アクセスできる

##### エラー表示（Snackbar）

```kotlin
LaunchedEffect(uiState.error) {
    uiState.error?.let { error ->
        snackbarHostState.showSnackbar(error)
        viewModel.clearError()
    }
}
```

- **`LaunchedEffect(key)`**: `key`が変わるたびに内部のコルーチンを再実行するCompose副作用
- エラーが発生するとSnackbar（画面下部の一時メッセージ）を表示し、表示後にエラーをクリア

##### RouletteAnimation（ルーレットアニメーション）

```kotlin
@Composable
private fun RouletteAnimation(isSpinning: Boolean) {
    val rotation = remember { Animatable(0f) }

    LaunchedEffect(isSpinning) {
        if (isSpinning) {
            rotation.animateTo(
                targetValue = rotation.value + 1080f,  // 3回転 (360° × 3)
                animationSpec = tween(durationMillis = 2000, easing = LinearEasing)
            )
        }
    }

    Box(
        modifier = Modifier.size(200.dp).rotate(rotation.value),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = Icons.Default.Restaurant,
            contentDescription = "Roulette",
            modifier = Modifier.size(120.dp),
            tint = MaterialTheme.colorScheme.primary
        )
    }
}
```

- **`Animatable(0f)`**: アニメーション可能な浮動小数点値。初期値0度
- **`animateTo`**: 指定値までアニメーション。`1080f` = 360° × 3 で3回転
- **`tween(2000, LinearEasing)`**: 2秒間の等速アニメーション
- **`Modifier.rotate(rotation.value)`**: 現在の回転角度をUIに適用
- レストランアイコン（120dp）を200dpのBoxの中央に配置

##### ResultCard（結果カード）

```kotlin
@Composable
private fun ResultCard(
    restaurant: Restaurant,
    onDetailClick: () -> Unit
) { ... }
```

ルーレットの結果を表示するカード。「今日のおすすめ」のラベル、レストラン名、ジャンル・価格帯、「詳細を見る」ボタンを表示する。

##### FilterChipDisplay（フィルター表示チップ）

```kotlin
@Composable
private fun FilterChipDisplay(label: String) { ... }
```

現在適用中のフィルターをテキストチップで表示。「すべて」の場合は非表示。

---

## 6. レストラン一覧画面

### RestaurantListUiState

**ファイル**: `presentation/list/RestaurantListUiState.kt`

```kotlin
data class RestaurantListUiState(
    val isLoading: Boolean = false,
    val restaurants: List<Restaurant> = emptyList(),
    val selectedGenre: Genre = Genre.ALL,
    val selectedPriceRange: PriceRange = PriceRange.ALL,
    val showFilterSheet: Boolean = false,
    val error: String? = null
)
```

| フィールド | 型 | 説明 |
|-----------|------|------|
| `isLoading` | `Boolean` | データ読み込み中か |
| `restaurants` | `List<Restaurant>` | レストラン一覧データ |
| `selectedGenre` | `Genre` | 選択中のジャンル |
| `selectedPriceRange` | `PriceRange` | 選択中の価格帯 |
| `showFilterSheet` | `Boolean` | フィルターシートの表示状態 |
| `error` | `String?` | エラーメッセージ |

### RestaurantListViewModel

**ファイル**: `presentation/list/RestaurantListViewModel.kt`

```kotlin
@HiltViewModel
class RestaurantListViewModel @Inject constructor(
    private val getRestaurantsUseCase: GetRestaurantsUseCase
) : ViewModel() {

    init {
        loadRestaurants()  // ViewModel生成時に自動で一覧を取得
    }

    fun loadRestaurants() { ... }

    fun setGenre(genre: Genre) {
        _uiState.update { it.copy(selectedGenre = genre) }
        loadRestaurants()  // フィルター変更時に再取得
    }

    fun setPriceRange(priceRange: PriceRange) {
        _uiState.update { it.copy(selectedPriceRange = priceRange) }
        loadRestaurants()  // フィルター変更時に再取得
    }
}
```

#### ルーレットVMとの違い

| ポイント | RouletteViewModel | RestaurantListViewModel |
|---------|------------------|------------------------|
| 初期ロード | なし（ユーザーがボタンを押すまで待つ） | `init`ブロックで自動取得 |
| フィルター変更時 | 状態更新のみ | 状態更新 + 再取得（`loadRestaurants()`） |
| UseCase | `SpinRouletteUseCase` | `GetRestaurantsUseCase` |

### RestaurantListScreen

**ファイル**: `presentation/list/RestaurantListScreen.kt`

```kotlin
@Composable
fun RestaurantListScreen(
    onNavigateToDetail: (String) -> Unit,
    viewModel: RestaurantListViewModel = hiltViewModel()
) { ... }
```

#### 画面の状態遷移

```kotlin
when {
    uiState.isLoading -> {
        LoadingContent(...)           // ① ローディング中
    }
    uiState.error != null && uiState.restaurants.isEmpty() -> {
        ErrorContent(...)             // ② エラー（データなし）
    }
    else -> {
        LazyColumn(...) { ... }       // ③ 一覧表示
    }
}
```

1. **ローディング中**: `CircularProgressIndicator`を表示
2. **エラー（データなし）**: エラーメッセージと再試行ボタンを表示
3. **一覧表示**: `LazyColumn`でスクロール可能なリストを表示

#### LazyColumn（リスト表示）

```kotlin
LazyColumn(
    contentPadding = PaddingValues(16.dp),
    verticalArrangement = Arrangement.spacedBy(12.dp)
) {
    items(
        items = uiState.restaurants,
        key = { it.id }
    ) { restaurant ->
        RestaurantCard(
            restaurant = restaurant,
            onClick = { onNavigateToDetail(restaurant.id) }
        )
    }
}
```

- **`LazyColumn`**: RecyclerViewのCompose版。画面に表示される分だけ描画する（遅延ロード）
- **`contentPadding`**: リスト全体の外側余白
- **`verticalArrangement = Arrangement.spacedBy(12.dp)`**: アイテム間の間隔
- **`key = { it.id }`**: 各アイテムを一意に識別するキー。リスト更新時の再利用を最適化

---

## 7. レストラン詳細画面

### RestaurantDetailUiState

**ファイル**: `presentation/detail/RestaurantDetailUiState.kt`

```kotlin
data class RestaurantDetailUiState(
    val isLoading: Boolean = false,
    val restaurant: Restaurant? = null,
    val error: String? = null
)
```

他の画面と比べてシンプルな構成。フィルター機能がないため。

### RestaurantDetailViewModel

**ファイル**: `presentation/detail/RestaurantDetailViewModel.kt`

```kotlin
@HiltViewModel
class RestaurantDetailViewModel @Inject constructor(
    private val getRestaurantDetailUseCase: GetRestaurantDetailUseCase,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val restaurantId: String = checkNotNull(savedStateHandle["restaurantId"])

    init {
        loadRestaurantDetail()
    }
}
```

#### SavedStateHandle

```kotlin
savedStateHandle: SavedStateHandle
```

**`SavedStateHandle`**: Navigationのルートパラメータを取得するためのマップ的なオブジェクト。Hiltが自動的に注入する。

```kotlin
private val restaurantId: String = checkNotNull(savedStateHandle["restaurantId"])
```

- ナビゲーションで渡された`restaurantId`パラメータを取得
- `checkNotNull`: nullの場合は`IllegalStateException`をスロー（必須パラメータのため）
- ルート定義: `"detail/{restaurantId}"` の `{restaurantId}` が自動的にSavedStateHandleに格納される

### RestaurantDetailScreen

**ファイル**: `presentation/detail/RestaurantDetailScreen.kt`

#### 画面構成

```
┌────────────────────────────────┐
│ ← レストラン名        (TopAppBar) │
├────────────────────────────────┤
│ ┌────────────────────────────┐ │
│ │       レストラン画像        │ │
│ │       (200dp高さ)          │ │
│ └────────────────────────────┘ │
│                                │
│ レストラン名                    │
│ ジャンル | 価格帯              │
│                                │
│ 📍 住所                        │
│ 🕐 営業時間 〜 閉店時間        │
│                                │
│ [  🗺️ 地図で見る  ]            │
└────────────────────────────────┘
```

#### 地図連携（Googleマップインテント）

```kotlin
onOpenMap = { lat, lng, name ->
    val uri = Uri.parse("geo:$lat,$lng?q=$lat,$lng($name)")
    val intent = Intent(Intent.ACTION_VIEW, uri)
    if (intent.resolveActivity(context.packageManager) != null) {
        context.startActivity(intent)
    }
}
```

1. **`geo:` URIスキーム**: 地図アプリを起動するためのURI形式
   - `geo:33.5,130.4?q=33.5,130.4(レストラン名)` のような形式
2. **`Intent.ACTION_VIEW`**: URIを表示するインテント
3. **`resolveActivity`**: 対応するアプリがインストールされているか確認
4. **`startActivity`**: Googleマップ等の地図アプリを起動

#### TopAppBar（ヘッダー）

```kotlin
TopAppBar(
    title = { Text(uiState.restaurant?.name ?: "詳細") },
    navigationIcon = {
        IconButton(onClick = onNavigateBack) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "戻る")
        }
    }
)
```

- タイトルはレストラン名（取得前は「詳細」を表示）
- 左上に戻るボタン（`ArrowBack`アイコン）を配置
- `AutoMirrored`: RTL（右から左）言語でアイコンが自動反転する

#### 営業時間表示ロジック

```kotlin
val timeText = buildString {
    append(restaurant.openingHours ?: "")
    if (!restaurant.openingHours.isNullOrEmpty() && !restaurant.closingHours.isNullOrEmpty()) {
        append(" 〜 ")
    }
    append(restaurant.closingHours ?: "")
}
```

- 開店時間と閉店時間の間に「〜」を挿入
- どちらかがnull/空の場合は「〜」を表示しない
- `buildString`: `StringBuilder`のラッパー関数。文字列を効率的に構築
