# 04 開発スタートガイド

> 生成日: 2026-02-24 | skill: code-learner

---

## セットアップ手順

### 必要なツール

- **Android Studio** (最新の Ladybug 以降推奨)
- **JDK 17 以上**
- **Android SDK** (API Level 34 / 35)

### ビルド手順

```bash
# 1. リポジトリをクローン
git clone <repo-url>
cd androidapp

# 2. Android Studio で開く
# File > Open > androidapp ディレクトリを選択

# 3. Gradle Sync
# Android Studio が自動的に依存関係をダウンロードする

# 4. エミュレータまたは実機で実行
# Run > Run 'app' (Shift+F10)
```

### API エンドポイントの確認

`app/build.gradle.kts` に定義されています:

```kotlin
buildConfigField("String", "BASE_URL",
    "\"https://fukudaifood-130668695114.asia-northeast1.run.app/\"")
```

認証不要で API にアクセスできます。

---

## 新しい画面を追加する手順

例として「お気に入り画面 (FavoriteScreen)」を追加する手順を示します。

### Step 1: ドメインモデルの確認・追加

既存の `Restaurant` モデルをそのまま使う場合は不要。

### Step 2: UseCase を作成 (必要であれば)

```kotlin
// domain/usecase/GetFavoriteRestaurantsUseCase.kt
class GetFavoriteRestaurantsUseCase @Inject constructor(
    private val repository: RestaurantRepository
) {
    operator fun invoke(): Flow<NetworkResult<List<Restaurant>>> {
        return repository.getFavorites()
    }
}
```

### Step 3: UiState を作成

```kotlin
// presentation/favorite/FavoriteUiState.kt
data class FavoriteUiState(
    val isLoading: Boolean = false,
    val restaurants: List<Restaurant> = emptyList(),
    val error: String? = null
)
```

### Step 4: ViewModel を作成

```kotlin
// presentation/favorite/FavoriteViewModel.kt
@HiltViewModel
class FavoriteViewModel @Inject constructor(
    private val getFavoriteRestaurantsUseCase: GetFavoriteRestaurantsUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(FavoriteUiState())
    val uiState: StateFlow<FavoriteUiState> = _uiState.asStateFlow()

    init {
        loadFavorites()
    }

    private fun loadFavorites() {
        viewModelScope.launch {
            getFavoriteRestaurantsUseCase().collect { result ->
                when (result) {
                    is NetworkResult.Loading ->
                        _uiState.update { it.copy(isLoading = true) }
                    is NetworkResult.Success ->
                        _uiState.update { it.copy(isLoading = false, restaurants = result.data) }
                    is NetworkResult.Error ->
                        _uiState.update { it.copy(isLoading = false, error = result.message) }
                }
            }
        }
    }
}
```

### Step 5: Screen Composable を作成

```kotlin
// presentation/favorite/FavoriteScreen.kt
@Composable
fun FavoriteScreen(
    onNavigateToDetail: (String) -> Unit,
    viewModel: FavoriteViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    when {
        uiState.isLoading -> LoadingContent()
        uiState.error != null -> ErrorContent(
            message = uiState.error!!,
            onRetry = { viewModel.loadFavorites() }
        )
        else -> LazyColumn {
            items(uiState.restaurants) { restaurant ->
                RestaurantCard(
                    restaurant = restaurant,
                    onClick = { onNavigateToDetail(restaurant.id) }
                )
            }
        }
    }
}
```

### Step 6: NavGraph にルートを追加

```kotlin
// presentation/navigation/NavGraph.kt

// 1. Screen に追加
sealed class Screen(val route: String) {
    // ...既存...
    data object Favorite : Screen("favorite")  // ← 追加
}

// 2. BottomNavItem に追加 (タブに表示する場合)
sealed class BottomNavItem(val route: String, val title: String, val icon: ImageVector) {
    // ...既存...
    data object Favorite : BottomNavItem("favorite", "お気に入り", Icons.Default.Favorite)
}

// 3. NavHost に composable を追加
composable(Screen.Favorite.route) {
    FavoriteScreen(onNavigateToDetail = { id ->
        navController.navigate(Screen.Detail.createRoute(id))
    })
}
```

---

## 新しい API エンドポイントを追加する手順

例として `/v1/restaurants/{id}/favorite` (POST) を追加する場合。

### Step 1: ApiService に追加

```kotlin
// data/remote/ApiService.kt
interface ApiService {
    // ...既存...

    @POST("v1/restaurants/{id}/favorite")
    suspend fun addFavorite(
        @Path("id") restaurantId: String
    ): Response<Unit>
}
```

### Step 2: Repository インターフェースに追加

```kotlin
// domain/repository/RestaurantRepository.kt
interface RestaurantRepository {
    // ...既存...
    fun addFavorite(restaurantId: String): Flow<NetworkResult<Unit>>
}
```

### Step 3: RepositoryImpl に実装を追加

```kotlin
// data/repository/RestaurantRepositoryImpl.kt
override fun addFavorite(restaurantId: String): Flow<NetworkResult<Unit>> = flow {
    emit(NetworkResult.Loading)
    try {
        val response = apiService.addFavorite(restaurantId)
        if (response.isSuccessful) {
            emit(NetworkResult.Success(Unit))
        } else {
            emit(NetworkResult.Error("お気に入り追加に失敗しました", response.code()))
        }
    } catch (e: Exception) {
        emit(NetworkResult.Error(e.message ?: "不明なエラー"))
    }
}
```

---

## よくあるタスクのパターン

### フィルター状態を追加する

1. `*UiState.kt` に新しいフィールドを追加
2. `*ViewModel.kt` に `update()` を呼ぶメソッドを追加
3. `FilterBottomSheet.kt` に UI を追加
4. UseCase の呼び出しに引数を追加

### エラー表示をカスタマイズする

`presentation/components/ErrorContent.kt` を修正するか、画面ごとに独自のエラー表示を実装します。

### ログを確認する

OkHttp のログインターセプターが有効なため、Logcat で HTTP 通信の詳細を確認できます。

```
Tag: OkHttp
--> POST https://fukudaifood.../v1/roulette
{"genres":["ラーメン"],"price_range":1}
<-- 200 OK
{"id":"xxx","name":"博多ラーメン"}
```

---

## デバッグのヒント

### StateFlow の状態を確認する

```kotlin
// ViewModel 内で Log を使う
_uiState.update { state ->
    Log.d("RouletteVM", "State updated: $state")
    state.copy(isSpinning = true)
}
```

### Compose Preview を活用する

```kotlin
@Preview(showBackground = true)
@Composable
fun RouletteScreenPreview() {
    MeshiRouletteTheme {
        // ViewModel 不要のプレビュー用関数を作る
        RestaurantCard(
            restaurant = Restaurant(
                id = "1",
                name = "プレビュー食堂",
                genre = Genre.TEISHOKU,
                priceRange = PriceRange.CHEAP,
                address = "福岡市西区",
                // ...
            ),
            onClick = {}
        )
    }
}
```

### よくあるエラーと対処法

| エラー | 原因 | 対処 |
|-------|------|------|
| `NullPointerException` in ViewModel | `savedStateHandle["key"]` が null | `checkNotNull()` でキー名を確認 |
| `IllegalStateException: Hilt...` | `@AndroidEntryPoint` 忘れ | Activity/Fragment に `@AndroidEntryPoint` を付加 |
| `NetworkOnMainThreadException` | suspend 関数をメインスレッドで直接呼んだ | `viewModelScope.launch` 内で呼ぶ |
| 画像が表示されない | imageUrl が null または無効 | `placeholder` / `error` を Coil に設定 |
| ナビゲーションで戻れない | `popBackStack()` 忘れ | `onNavigateBack = { navController.popBackStack() }` |

---

## ファイル追加チェックリスト

新機能を追加するときの確認リスト:

- [ ] `domain/model/` - 必要なドメインモデルがあるか
- [ ] `domain/repository/` - インターフェースにメソッドを追加したか
- [ ] `domain/usecase/` - UseCase を作成したか (`@Inject constructor`)
- [ ] `data/remote/ApiService.kt` - API エンドポイントを追加したか
- [ ] `data/repository/RepositoryImpl.kt` - 実装を追加したか
- [ ] `presentation/xxx/XxxUiState.kt` - UiState を作成したか
- [ ] `presentation/xxx/XxxViewModel.kt` - `@HiltViewModel` + `@Inject constructor`
- [ ] `presentation/xxx/XxxScreen.kt` - Composable を作成したか
- [ ] `presentation/navigation/NavGraph.kt` - ルートと `composable()` を追加したか
