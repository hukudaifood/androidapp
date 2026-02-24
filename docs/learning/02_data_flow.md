# 02 データフロー解説

> 生成日: 2026-02-24 | skill: code-learner

---

## ルーレット実行のデータフロー

ユーザーが「ルーレットを回す」をタップしてから UI が更新されるまでの完全な流れです。

```
[ユーザー] ボタンタップ
    │
    ▼
RouletteScreen.kt
    Button(onClick = { viewModel.spinRoulette() })
    │
    ▼
RouletteViewModel.kt
    fun spinRoulette() {
        viewModelScope.launch {
            spinRouletteUseCase(genres, priceRange, isOpenNow).collect { result -> ... }
        }
    }
    │
    ▼
SpinRouletteUseCase.kt
    operator fun invoke(...): Flow<NetworkResult<Restaurant>> {
        return repository.spinRoulette(...)
    }
    │
    ▼
RestaurantRepositoryImpl.kt
    override fun spinRoulette(...): Flow<NetworkResult<Restaurant>> = flow {
        emit(NetworkResult.Loading)          ← Loading 状態を先に流す
        val response = apiService.spinRoulette(request)
        if (response.isSuccessful) {
            emit(NetworkResult.Success(response.body()!!.toDomain()))
        } else {
            emit(NetworkResult.Error("エラー", response.code()))
        }
    }
    │
    ▼
ApiService.kt (Retrofit)
    @POST("v1/roulette")
    suspend fun spinRoulette(@Body request: RouletteRequest): Response<RestaurantDto>
    │
    ▼ HTTP POST /v1/roulette
REST API サーバー (Google Cloud Run)
    │
    ▼ JSON レスポンス { "id": "...", "name": "..." }
    │
    ▼ (逆順に戻る)
RestaurantRepositoryImpl.kt
    response.body().toDomain()  ← DTO → ドメインモデルに変換
    emit(NetworkResult.Success(restaurant))
    │
    ▼
RouletteViewModel.kt (collect で受け取る)
    is NetworkResult.Loading →
        _uiState.update { it.copy(isSpinning = true, error = null) }
    is NetworkResult.Success →
        _uiState.update { it.copy(isSpinning = false, selectedRestaurant = result.data) }
    is NetworkResult.Error →
        _uiState.update { it.copy(isSpinning = false, error = result.message) }
    │
    ▼ StateFlow が更新される
RouletteScreen.kt
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    ← 再コンポジション (UI 再描画) が自動的に発火
    │
    ▼
スロットマシン演出 → 結果の店舗カード表示
```

---

## 状態管理の仕組み

### UiState とは

各画面は自分専用の `UiState` データクラスを持ちます。

```kotlin
// RouletteUiState.kt
data class RouletteUiState(
    val isSpinning: Boolean = false,           // ← ルーレット中か
    val selectedRestaurant: Restaurant? = null, // ← 選ばれた店舗
    val selectedGenres: Set<Genre> = emptySet(), // ← フィルター状態
    val selectedPriceRange: PriceRange = PriceRange.ALL,
    val isOpenNowOnly: Boolean = false,
    val showFilterSheet: Boolean = false,       // ← BottomSheet 表示状態
    val error: String? = null,                  // ← エラーメッセージ
    val candidateRestaurants: List<Restaurant> = emptyList() // ← 候補一覧
)
```

`isLoading` が存在しない点に注目。このアプリでは `isSpinning` がローディング状態を表します。

### StateFlow の流れ

```
MutableStateFlow<RouletteUiState>  ← ViewModel が書き込む
         │
         │ asStateFlow()
         ▼
StateFlow<RouletteUiState>         ← 外部 (Screen) は読み取り専用
         │
         │ collectAsStateWithLifecycle()
         ▼
State<RouletteUiState>             ← Compose が監視
         │
         │ 値が変わると自動的に再コンポジション
         ▼
UI 更新
```

### update 関数のパターン

```kotlin
// NG: 直接代入はできない (StateFlow は immutable)
_uiState.value = _uiState.value.copy(isSpinning = true)

// OK: update を使って atomically に更新
_uiState.update { it.copy(isSpinning = true) }
// it は現在の UiState
// copy() で必要なフィールドだけ変更した新しいオブジェクトを返す
```

---

## API コール・データ変換のパターン

### NetworkResult 型

```kotlin
sealed class NetworkResult<out T> {
    data class Success<T>(val data: T) : NetworkResult<T>()
    data class Error(val message: String, val code: Int? = null) : NetworkResult<Nothing>()
    data object Loading : NetworkResult<Nothing>()
}
```

**なぜ sealed class か?**
- `when (result)` で分岐したとき、`else` が不要になる
- コンパイラが全ケースの処理を強制してくれる → 処理漏れがなくなる

### DTO → ドメインモデル変換

```kotlin
// data/model/RestaurantMapper.kt
fun RestaurantDto.toDomain(): Restaurant {
    return Restaurant(
        id = id.orEmpty(),                         // null → ""
        name = name.orEmpty(),
        genre = Genre.fromApiValue(genre),          // "定食" → Genre.TEISHOKU
        priceRange = PriceRange.fromApiValue(priceRange), // 1 → PriceRange.CHEAP
        address = address.orEmpty(),
        latitude = latitude,
        longitude = longitude,
        imageUrl = imageUrl,
        googleMapsUrl = googleMapsUrl,
        openingHours = openingHours,
        closingHours = closingHours
    )
}

// List の場合は拡張関数で一括変換
fun List<RestaurantDto>.toDomain(): List<Restaurant> = map { it.toDomain() }
```

---

## シーケンス図

### レストラン一覧取得

```
Screen          ViewModel         UseCase         Repository       ApiService
  │                │                  │                │               │
  │ LaunchedEffect │                  │                │               │
  │──loadRestaurants()──────────────>│                │               │
  │                │──invoke()──────>│                │               │
  │                │                 │──getRestaurants()─────────────>│
  │                │                 │                │──GET /v1/restaurants──> API
  │                │                 │                │<── Response ──────────
  │                │                 │<──Flow(Loading)─│               │
  │<──uiState(isLoading=true)────────│                │               │
  │ [ローディング表示]               │                │               │
  │                │<──Flow(Success)──────────────────│               │
  │<──uiState(restaurants=[...])─────│                │               │
  │ [一覧表示]                       │                │               │
```

### ルーレット → 詳細画面

```
Screen          ViewModel         NavController
  │                │                  │
  │──spinRoulette()──────────────────>│ (API 通信)
  │                │                  │
  │<──uiState(selectedRestaurant=R)───│
  │ [スロット演出]                    │
  │                                   │
  │──onNavigateToDetail(R.id)─────────────────────────>│
  │                                   navController.navigate("detail/xxx")
  │                                   DetailScreen が表示される
```

---

## フィルタリングのデータフロー

フィルターは ViewModel の UiState に保存され、ルーレット・一覧の両方で使用されます。

```
FilterBottomSheet
  │ onGenreToggled(Genre.RAMEN)
  ▼
RouletteViewModel.toggleGenre(Genre.RAMEN)
  _uiState.update { state ->
      val newGenres = if (genre in state.selectedGenres)
          state.selectedGenres - genre
      else
          state.selectedGenres + genre
      state.copy(selectedGenres = newGenres)
  }
  ▼
RouletteScreen が再描画 (チップの選択状態が変わる)
  ▼
次回 spinRoulette() 時に selectedGenres を UseCase に渡す
  ▼
ApiService: GET /v1/restaurants?genre=ラーメン
```

---

## Composable での状態観察パターン

```kotlin
@Composable
fun RouletteScreen(
    onNavigateToDetail: (String) -> Unit,
    viewModel: RouletteViewModel = hiltViewModel()
) {
    // StateFlow を Compose の State に変換
    // ライフサイクルを考慮して、バックグラウンド時は収集を停止する
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    // uiState の値に応じて表示を切り替える
    when {
        uiState.isSpinning -> {
            // ルーレットアニメーション表示
        }
        uiState.error != null -> {
            ErrorContent(
                message = uiState.error!!,
                onRetry = { viewModel.spinRoulette() }
            )
        }
        uiState.selectedRestaurant != null -> {
            // 結果カード表示
            RestaurantCard(
                restaurant = uiState.selectedRestaurant!!,
                onClick = { onNavigateToDetail(uiState.selectedRestaurant!!.id) }
            )
        }
    }
}
```

---

## スロットマシンアニメーションの仕組み

APIの結果を待ちながら、UIは候補店をパラパラ表示するアニメーションを行います。

```kotlin
// RouletteScreen.kt
var displayedRestaurant by remember { mutableStateOf<Restaurant?>(null) }
var localSpinCount by remember { mutableIntStateOf(0) }

// spinRoulette() を呼ぶと localSpinCount が増える
// それを LaunchedEffect のキーにしてアニメーションを開始
LaunchedEffect(localSpinCount) {
    val candidates = uiState.candidateRestaurants
    var index = 0
    val endTime = System.currentTimeMillis() + 1500L

    // フェーズ 1: 1.5秒間、50ms ごとに候補店を切り替え
    while (System.currentTimeMillis() < endTime) {
        if (candidates.isNotEmpty()) {
            displayedRestaurant = candidates[index % candidates.size]
            index++
        }
        delay(50)
    }

    // フェーズ 2: API の結果が来るまで待機
    viewModel.uiState.first { !it.isSpinning }

    // フェーズ 3: 最終結果を表示
    displayedRestaurant = viewModel.uiState.value.selectedRestaurant
}
```
