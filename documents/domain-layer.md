# ドメイン層 (Domain Layer) 詳細解説

ドメイン層はアプリのビジネスロジックとデータモデルを定義するレイヤーです。他のどのレイヤーにも依存しません（最も内側の層）。

---

## 1. Restaurant（ドメインモデル）

**ファイル**: `domain/model/Restaurant.kt`

### Restaurant data class

```kotlin
data class Restaurant(
    val id: String,
    val name: String,
    val genre: Genre,
    val priceRange: PriceRange,
    val address: String,
    val latitude: Double?,
    val longitude: Double?,
    val imageUrl: String?,
    val googleMapsUrl: String?,
    val openingHours: String?,
    val closingHours: String?
)
```

#### 解説

アプリ内で使用するレストランの**ドメインモデル**です。DTOとの違いは以下の通り:

| 比較点 | RestaurantDto (Data層) | Restaurant (Domain層) |
|--------|----------------------|---------------------|
| `id` | `String?` (Nullable) | `String` (Non-Null) |
| `name` | `String?` (Nullable) | `String` (Non-Null) |
| `genre` | `String?` (文字列) | `Genre` (Enum型) |
| `priceRange` | `Int?` (数値) | `PriceRange` (Enum型) |
| `address` | `String?` (Nullable) | `String` (Non-Null) |

**ポイント**: ドメインモデルでは型安全性を高めています。Enum型を使うことで無効な値を排除し、Non-Null型を使うことでnullチェックの必要をなくしています。

---

### Genre（ジャンル列挙型）

```kotlin
enum class Genre(val displayName: String, val apiValue: String) {
    ALL("すべて", ""),
    TEISHOKU("定食", "定食"),
    CHINESE("中華", "中華"),
    RAMEN("ラーメン", "ラーメン"),
    CURRY("カレー", "カレー"),
    UDON("うどん", "うどん"),
    GYUDON("牛丼", "牛丼"),
    TONKATSU("とんかつ", "とんかつ"),
    YAKINIKU("焼肉", "焼肉"),
    IZAKAYA("居酒屋", "居酒屋"),
    CAFE("カフェ", "カフェ"),
    OTHER("その他", "other");

    companion object {
        fun fromApiValue(value: String?): Genre {
            if (value.isNullOrEmpty()) return OTHER
            return entries.find { it.apiValue == value } ?: OTHER
        }
    }
}
```

#### 解説

レストランのジャンルを定義するEnum classです。

#### プロパティ

| プロパティ | 型 | 用途 |
|-----------|------|------|
| `displayName` | `String` | UI表示用の日本語名（例: "中華"） |
| `apiValue` | `String` | API通信用の値（例: "中華"） |

#### 各ジャンルの一覧

| Enum定数 | UI表示 | API値 |
|---------|--------|-------|
| `ALL` | すべて | `""` (空文字) |
| `TEISHOKU` | 定食 | `"定食"` |
| `CHINESE` | 中華 | `"中華"` |
| `RAMEN` | ラーメン | `"ラーメン"` |
| `CURRY` | カレー | `"カレー"` |
| `UDON` | うどん | `"うどん"` |
| `GYUDON` | 牛丼 | `"牛丼"` |
| `TONKATSU` | とんかつ | `"とんかつ"` |
| `YAKINIKU` | 焼肉 | `"焼肉"` |
| `IZAKAYA` | 居酒屋 | `"居酒屋"` |
| `CAFE` | カフェ | `"カフェ"` |
| `OTHER` | その他 | `"other"` |

#### fromApiValue（ファクトリメソッド）

```kotlin
companion object {
    fun fromApiValue(value: String?): Genre {
        if (value.isNullOrEmpty()) return OTHER  // nullまたは空文字 → OTHER
        return entries.find { it.apiValue == value } ?: OTHER  // 一致するものがなければ → OTHER
    }
}
```

`companion object`内に定義することで、`Genre.fromApiValue("中華")`のようにクラスメソッドとして呼び出せます。

- `entries` — Kotlin 1.9+のEnum全エントリ一覧（旧`values()`に代わるもの）
- `.find { it.apiValue == value }` — 一致するEnumを検索
- `?: OTHER` — 見つからなければデフォルトの`OTHER`を返す

---

### PriceRange（価格帯列挙型）

```kotlin
enum class PriceRange(val displayName: String, val apiValue: Int) {
    ALL("すべて", 0),
    CHEAP("〜500円", 1),
    MEDIUM("500〜1000円", 2),
    EXPENSIVE("1000円〜", 3);

    companion object {
        fun fromApiValue(value: Int?): PriceRange {
            if (value == null) return MEDIUM
            return entries.find { it.apiValue == value } ?: MEDIUM
        }
    }
}
```

#### 解説

価格帯を表すEnum classです。`Genre`と同じパターンですが、`apiValue`が`Int`型です。

| Enum定数 | UI表示 | API値 |
|---------|--------|-------|
| `ALL` | すべて | `0` |
| `CHEAP` | 〜500円 | `1` |
| `MEDIUM` | 500〜1000円 | `2` |
| `EXPENSIVE` | 1000円〜 | `3` |

`fromApiValue`でnullの場合は`MEDIUM`をデフォルトとして返します。

---

## 2. RestaurantRepository（インターフェース）

**ファイル**: `domain/repository/RestaurantRepository.kt`

```kotlin
interface RestaurantRepository {

    fun getRestaurants(
        genre: Genre? = null,
        priceRange: PriceRange? = null
    ): Flow<NetworkResult<List<Restaurant>>>

    fun getRestaurantDetail(id: String): Flow<NetworkResult<Restaurant>>

    fun spinRoulette(
        genre: Genre? = null,
        priceRange: PriceRange? = null
    ): Flow<NetworkResult<Restaurant>>
}
```

### 解説

Repository（データの取得・保存を抽象化する層）の**インターフェース**です。

#### なぜインターフェースを定義するのか？

**依存性の逆転原則 (DIP: Dependency Inversion Principle)** を実現するためです。

```
【従来の依存関係】
ViewModel → RepositoryImpl → ApiService
  ↑ 上位が下位に直接依存している

【Clean Architectureの依存関係】
ViewModel → UseCase → Repository(I/F) ← RepositoryImpl → ApiService
  ↑ 上位はインターフェースに依存し、実装を知らない
```

これにより:
- **テストが容易**: Repository のモックを作ってViewModelを単体テストできる
- **実装の交換が容易**: APIからローカルDBに切り替える場合もインターフェースはそのまま
- **レイヤー間の結合度が低い**: ドメイン層はデータ層を知らない

#### 戻り値の型: `Flow<NetworkResult<T>>`

- **`Flow`**: 非同期のデータストリーム。時間の経過とともに複数の値を送出できる
  - 今回のケースでは `Loading → Success` または `Loading → Error` の2回emitされる
- **`NetworkResult<T>`**: 通信の状態（Loading / Success / Error）をラップするsealed class
- **`T`**: `List<Restaurant>` または `Restaurant`

---

## 3. UseCase（ユースケース）

ユースケースはドメイン層のビジネスロジックを1つの操作にカプセル化するクラスです。

### GetRestaurantsUseCase

**ファイル**: `domain/usecase/GetRestaurantsUseCase.kt`

```kotlin
class GetRestaurantsUseCase @Inject constructor(
    private val repository: RestaurantRepository
) {
    operator fun invoke(
        genre: Genre? = null,
        priceRange: PriceRange? = null
    ): Flow<NetworkResult<List<Restaurant>>> {
        return repository.getRestaurants(genre, priceRange)
    }
}
```

### GetRestaurantDetailUseCase

**ファイル**: `domain/usecase/GetRestaurantDetailUseCase.kt`

```kotlin
class GetRestaurantDetailUseCase @Inject constructor(
    private val repository: RestaurantRepository
) {
    operator fun invoke(id: String): Flow<NetworkResult<Restaurant>> {
        return repository.getRestaurantDetail(id)
    }
}
```

### SpinRouletteUseCase

**ファイル**: `domain/usecase/SpinRouletteUseCase.kt`

```kotlin
class SpinRouletteUseCase @Inject constructor(
    private val repository: RestaurantRepository
) {
    operator fun invoke(
        genre: Genre? = null,
        priceRange: PriceRange? = null
    ): Flow<NetworkResult<Restaurant>> {
        return repository.spinRoulette(genre, priceRange)
    }
}
```

### 解説

#### 共通パターン

3つのUseCaseは全て同じ設計パターンに従っています:

```kotlin
class XxxUseCase @Inject constructor(   // ① Hiltによるコンストラクタインジェクション
    private val repository: RestaurantRepository  // ② Repositoryインターフェースに依存
) {
    operator fun invoke(...): Flow<NetworkResult<T>> {  // ③ operator funで関数のように呼べる
        return repository.xxx(...)  // ④ Repositoryに処理を委譲
    }
}
```

#### `operator fun invoke` とは？

`operator fun invoke`を定義すると、インスタンスを関数のように呼び出せます:

```kotlin
// 通常の呼び方
val result = getRestaurantsUseCase.invoke(genre, priceRange)

// operator fun invokeを定義しているので、こう書ける
val result = getRestaurantsUseCase(genre, priceRange)
```

ViewModelでの使用例:
```kotlin
getRestaurantsUseCase(genre = Genre.RAMEN).collect { result ->
    // ...
}
```

#### `@Inject constructor` とは？

Hiltに「このクラスのインスタンスを作るときは、コンストラクタの引数を自動で注入してください」と指示するアノテーションです。UseCaseはModuleで`@Provides`を書かなくても、`@Inject constructor`だけでHiltが自動的にインスタンスを生成できます。

#### なぜUseCaseが必要なのか？

現時点ではUseCaseはRepositoryを呼ぶだけですが、将来的にビジネスロジックを追加する場所として機能します。例えば:

- データのキャッシュ判定ロジック
- 複数のRepositoryを組み合わせる処理
- データのバリデーション
- ソートやフィルタの追加ロジック

また、**ViewModelがRepositoryに直接依存しない**ようにすることで、レイヤー間の結合度を下げています。

#### UseCase一覧

| UseCase | 入力 | 出力 | 用途 |
|---------|------|------|------|
| `GetRestaurantsUseCase` | genre?, priceRange? | `Flow<NetworkResult<List<Restaurant>>>` | レストラン一覧取得 |
| `GetRestaurantDetailUseCase` | id | `Flow<NetworkResult<Restaurant>>` | レストラン詳細取得 |
| `SpinRouletteUseCase` | genre?, priceRange? | `Flow<NetworkResult<Restaurant>>` | ルーレット実行 |
