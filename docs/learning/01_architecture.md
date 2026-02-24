# 01 アーキテクチャ解説

> 生成日: 2026-02-24 | skill: code-learner

---

## 採用しているパターン: クリーンアーキテクチャ + MVVM

このアプリは **Clean Architecture** を採用し、コードを 3 つのレイヤーに分離しています。

```
┌──────────────────────────────────────────────────────┐
│              Presentation Layer (MVVM)               │
│   Screen (Composable) ←→ ViewModel ←→ UiState       │
└───────────────────────────┬──────────────────────────┘
                            │ UseCase を呼ぶ
┌───────────────────────────▼──────────────────────────┐
│                  Domain Layer                        │
│         UseCase / Repository Interface               │
│         Domain Model (Restaurant, Genre...)          │
└───────────────────────────┬──────────────────────────┘
                            │ Repository を実装
┌───────────────────────────▼──────────────────────────┐
│                   Data Layer                         │
│    RepositoryImpl / ApiService / DTO / Mapper        │
└──────────────────────────────────────────────────────┘
```

---

## 各レイヤーの責任

### Presentation Layer (`presentation/`)

**役割**: ユーザーに情報を見せ、操作を受け付ける

| コンポーネント | 役割 |
|-------------|------|
| `*Screen.kt` | Composable 関数。UI を描画し、イベントを ViewModel に委譲 |
| `*ViewModel.kt` | UI の状態を管理。UseCase を呼び出して結果を StateFlow に反映 |
| `*UiState.kt` | 画面の状態を表すデータクラス。ViewModel から Screen へ流れる |
| `components/` | 複数画面で再利用する Composable コンポーネント |
| `navigation/NavGraph.kt` | 画面遷移のルート定義 |

**コード例**: `RouletteViewModel.kt`

```kotlin
@HiltViewModel
class RouletteViewModel @Inject constructor(
    private val spinRouletteUseCase: SpinRouletteUseCase,
    private val getRestaurantsUseCase: GetRestaurantsUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(RouletteUiState())
    val uiState: StateFlow<RouletteUiState> = _uiState.asStateFlow()

    fun spinRoulette() {
        viewModelScope.launch {
            spinRouletteUseCase(...).collect { result ->
                when (result) {
                    is NetworkResult.Loading ->
                        _uiState.update { it.copy(isSpinning = true) }
                    is NetworkResult.Success ->
                        _uiState.update { it.copy(selectedRestaurant = result.data) }
                    is NetworkResult.Error ->
                        _uiState.update { it.copy(error = result.message) }
                }
            }
        }
    }
}
```

---

### Domain Layer (`domain/`)

**役割**: アプリのビジネスロジックを担う。外部依存ゼロの純粋な Kotlin コード

| コンポーネント | 役割 |
|-------------|------|
| `model/Restaurant.kt` | ドメインモデル。アプリ内で使うデータ構造 |
| `repository/RestaurantRepository.kt` | リポジトリの **インターフェース**（実装は Data 層） |
| `usecase/GetRestaurantsUseCase.kt` | レストラン一覧を取得するユースケース |
| `usecase/SpinRouletteUseCase.kt` | ルーレットを回すユースケース |
| `usecase/GetRestaurantDetailUseCase.kt` | 詳細情報を取得するユースケース |

**なぜ UseCase を使うか?**

- ViewModel が肥大化するのを防ぐ
- ロジックを再利用しやすくする (複数の ViewModel から同じ UseCase を使える)
- テストが書きやすくなる

**コード例**: `GetRestaurantsUseCase.kt`

```kotlin
class GetRestaurantsUseCase @Inject constructor(
    private val repository: RestaurantRepository  // ← インターフェースを注入
) {
    operator fun invoke(
        genres: List<Genre>? = null,
        priceRange: PriceRange? = null,
        isOpenNow: Boolean? = null
    ): Flow<NetworkResult<List<Restaurant>>> {
        return repository.getRestaurants(genres, priceRange, isOpenNow)
    }
}
```

**ドメインモデル**: `Restaurant.kt`

```kotlin
data class Restaurant(
    val id: String,
    val name: String,
    val genre: Genre,           // ← Enum を使い型安全に
    val priceRange: PriceRange, // ← Enum を使い型安全に
    val address: String,
    val latitude: Double?,
    val longitude: Double?,
    val imageUrl: String?,
    val googleMapsUrl: String?,
    val openingHours: String?,
    val closingHours: String?
)

// API の文字列値を型安全な Enum に変換
enum class Genre(val displayName: String, val apiValue: String) {
    ALL("すべて", ""),
    TEISHOKU("定食", "定食"),
    RAMEN("ラーメン", "ラーメン"),
    // ...
    companion object {
        fun fromApiValue(value: String?): Genre {
            return entries.find { it.apiValue == value } ?: OTHER
        }
    }
}
```

---

### Data Layer (`data/`)

**役割**: 外部との通信（API）を担う。Domain 層のインターフェースを実装する

| コンポーネント | 役割 |
|-------------|------|
| `remote/ApiService.kt` | Retrofit で定義した API エンドポイント |
| `model/RestaurantDto.kt` | API の JSON レスポンスに対応するデータクラス |
| `model/RestaurantMapper.kt` | DTO → ドメインモデルへの変換関数 |
| `repository/RestaurantRepositoryImpl.kt` | Repository インターフェースの実装 |

**コード例**: `ApiService.kt`

```kotlin
interface ApiService {
    @GET("v1/restaurants")
    suspend fun getRestaurants(
        @Query("genre") genres: List<String>? = null,
        @Query("price_range") priceRange: Int? = null,
        @Query("is_open_now") isOpenNow: Boolean? = null
    ): Response<List<RestaurantDto>>

    @GET("v1/restaurants/{id}")
    suspend fun getRestaurantDetail(@Path("id") id: String): Response<RestaurantDto>

    @POST("v1/roulette")
    suspend fun spinRoulette(@Body request: RouletteRequest): Response<RestaurantDto>
}
```

**DTO vs ドメインモデルの違い**

```
RestaurantDto (API から来る生のJSON)
  ├── genre: String?        "定食" (nullable)
  └── price_range: Int?     1 (Int)

         ↓ toDomain() で変換

Restaurant (アプリ内で使うドメインモデル)
  ├── genre: Genre          Genre.TEISHOKU (型安全な Enum)
  └── priceRange: PriceRange PriceRange.CHEAP (型安全な Enum)
```

---

## レイヤー間の依存関係

```
Presentation → Domain ← Data
```

- **Presentation** は **Domain** の UseCase・モデルだけを知っている
- **Data** は **Domain** のインターフェースを実装する
- **Presentation** と **Data** は互いを知らない

これにより、例えば「APIをFirebaseに変更する」場合、`data/` 層だけ変更すればよい。

---

## DI (依存性注入) の役割

Hilt が各レイヤーを自動的に繋ぎ合わせます。

```
NetworkModule
  └── ApiService ────────────────┐
                                  ▼
RepositoryModule                 RestaurantRepositoryImpl
  └── RestaurantRepository (I/F) ┘
         ↓ inject
GetRestaurantsUseCase
SpinRouletteUseCase
         ↓ inject
RouletteViewModel
         ↓ hiltViewModel()
RouletteScreen
```

開発者は `@Inject constructor` と `@HiltViewModel` を書くだけで、Hilt が全てのオブジェクト生成と受け渡しを自動化します。

---

## なぜこの設計か

| 設計の理由 | 効果 |
|----------|------|
| レイヤー分離 | API 変更の影響が `data/` 層内に留まる |
| Repository パターン | テスト時にモックに差し替えられる |
| UseCase パターン | ビジネスロジックを ViewModel から独立させてテスト容易 |
| StateFlow + UiState | UI 状態が Single Source of Truth になる |
| Hilt DI | 手動でオブジェクト生成しなくてよい |
