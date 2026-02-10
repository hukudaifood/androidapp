# データ層 (Data Layer) 詳細解説

データ層はAPIとの通信、JSONのパース、データ変換を担当するレイヤーです。

---

## 1. ApiService（API通信インターフェース）

**ファイル**: `data/remote/ApiService.kt`

```kotlin
interface ApiService {

    @GET("v1/restaurants")
    suspend fun getRestaurants(
        @Query("genre") genre: String? = null,
        @Query("price_range") priceRange: Int? = null
    ): Response<List<RestaurantDto>>

    @GET("v1/restaurants/{id}")
    suspend fun getRestaurantDetail(
        @Path("id") id: String
    ): Response<RestaurantDto>

    @POST("v1/roulette")
    suspend fun spinRoulette(
        @Body request: RouletteRequest
    ): Response<RestaurantDto>
}
```

### 解説

RetrofitのAPIインターフェースです。`interface`として定義し、Retrofitが実行時に自動で実装を生成します。

#### 各エンドポイントの詳細

| メソッド | エンドポイント | 説明 | パラメータ |
|---------|-------------|------|-----------|
| `getRestaurants` | `GET /v1/restaurants` | レストラン一覧を取得 | `genre`(クエリ), `priceRange`(クエリ) |
| `getRestaurantDetail` | `GET /v1/restaurants/{id}` | 特定レストランの詳細を取得 | `id`(パス) |
| `spinRoulette` | `POST /v1/roulette` | ルーレットでランダム選出 | `RouletteRequest`(ボディ) |

#### アノテーションの意味

- **`@GET("v1/restaurants")`**: HTTPの`GET`メソッドで`BASE_URL + v1/restaurants`にリクエストを送る
- **`@Query("genre")`**: URLのクエリパラメータとして追加される（例: `?genre=中華`）
- **`@Path("id")`**: URLのパス変数`{id}`を実際の値に置換する
- **`@POST("v1/roulette")`**: HTTPの`POST`メソッドでリクエストを送る
- **`@Body`**: オブジェクトをJSONに変換してリクエストボディとして送信する
- **`suspend fun`**: Kotlinコルーチンの一時停止関数。非同期処理をコルーチンスコープ内で実行する
- **`Response<T>`**: RetrofitのResponseラッパー。HTTPステータスコードやヘッダーにもアクセスできる

#### パラメータがnullの場合

`@Query`のパラメータが`null`の場合、Retrofitは自動的にそのクエリパラメータをURLに含めません。つまり、フィルターなしで全件取得したい場合は`null`を渡せばOKです。

---

## 2. RestaurantDto（データ転送オブジェクト）

**ファイル**: `data/model/RestaurantDto.kt`

```kotlin
data class RestaurantDto(
    @SerializedName("id")
    val id: String?,
    @SerializedName("name")
    val name: String?,
    @SerializedName("genre")
    val genre: String?,
    @SerializedName("price_range")
    val priceRange: Int?,
    @SerializedName("address")
    val address: String?,
    @SerializedName("latitude")
    val latitude: Double?,
    @SerializedName("longitude")
    val longitude: Double?,
    @SerializedName("image_url")
    val imageUrl: String?,
    @SerializedName("google_maps_url")
    val googleMapsUrl: String?,
    @SerializedName("opening_hours")
    val openingHours: String?,
    @SerializedName("closing_hours")
    val closingHours: String?
)
```

### 解説

APIから返ってくるJSONレスポンスをKotlinオブジェクトにマッピングするためのクラスです。

#### なぜDTOを使うのか？

1. **APIとドメインモデルの分離**: APIのJSON構造が変わってもドメインモデルへの影響を最小限にできる
2. **Null安全**: APIから`null`が返る可能性があるため、全フィールドをNullableにしている
3. **命名規則の吸収**: APIは`snake_case`（例: `price_range`）、Kotlinは`camelCase`（例: `priceRange`）を使用。`@SerializedName`で変換する

#### @SerializedName アノテーション

GsonライブラリのアノテーションでJSONのキー名とKotlinのプロパティ名のマッピングを指定します。

```
JSON: { "price_range": 2 }
      ↓ @SerializedName("price_range")
Kotlin: val priceRange: Int?
```

#### フィールド一覧

| フィールド | JSON名 | 型 | 説明 |
|-----------|--------|------|-----|
| `id` | `id` | `String?` | レストランの一意識別子 |
| `name` | `name` | `String?` | レストラン名 |
| `genre` | `genre` | `String?` | ジャンル（例: "中華", "ラーメン"） |
| `priceRange` | `price_range` | `Int?` | 価格帯（1=〜500円, 2=500〜1000円, 3=1000円〜） |
| `address` | `address` | `String?` | 住所 |
| `latitude` | `latitude` | `Double?` | 緯度 |
| `longitude` | `longitude` | `Double?` | 経度 |
| `imageUrl` | `image_url` | `String?` | 画像URL |
| `googleMapsUrl` | `google_maps_url` | `String?` | GoogleマップURL |
| `openingHours` | `opening_hours` | `String?` | 開店時間 |
| `closingHours` | `closing_hours` | `String?` | 閉店時間 |

### RouletteRequest（リクエストボディ）

```kotlin
data class RouletteRequest(
    @SerializedName("genre")
    val genre: String? = null,
    @SerializedName("price_range")
    val priceRange: Int? = null
)
```

ルーレットAPI（`POST /v1/roulette`）に送信するリクエストボディです。ジャンルと価格帯でフィルタリングを指定できます。デフォルトは`null`（フィルタなし）です。

---

## 3. RestaurantMapper（データ変換）

**ファイル**: `data/model/RestaurantMapper.kt`

```kotlin
fun RestaurantDto.toDomain(): Restaurant {
    return Restaurant(
        id = id.orEmpty(),
        name = name.orEmpty(),
        genre = Genre.fromApiValue(genre),
        priceRange = PriceRange.fromApiValue(priceRange),
        address = address.orEmpty(),
        latitude = latitude,
        longitude = longitude,
        imageUrl = imageUrl,
        googleMapsUrl = googleMapsUrl,
        openingHours = openingHours,
        closingHours = closingHours
    )
}

fun List<RestaurantDto?>.toDomain(): List<Restaurant> {
    return mapNotNull { it?.toDomain() }
}
```

### 解説

DTOからドメインモデルへの変換を行う**拡張関数**です。

#### ポイント

1. **`RestaurantDto.toDomain()`**: `RestaurantDto`の拡張関数として定義。`dto.toDomain()`の形式で呼べる
2. **`orEmpty()`**: `null`の場合に空文字列`""`を返す。NullableなStringをNon-Nullに変換する安全な方法
3. **`Genre.fromApiValue(genre)`**: APIの文字列値をEnum型に変換（後述のドメイン層で定義）
4. **`PriceRange.fromApiValue(priceRange)`**: APIのInt値をEnum型に変換
5. **`mapNotNull { it?.toDomain() }`**: リスト内の`null`要素を除外しつつ変換。APIから`null`が含まれるリストが返ってきても安全に処理できる

---

## 4. RestaurantRepositoryImpl（Repository実装）

**ファイル**: `data/repository/RestaurantRepositoryImpl.kt`

```kotlin
class RestaurantRepositoryImpl @Inject constructor(
    private val apiService: ApiService
) : RestaurantRepository {

    override fun getRestaurants(
        genre: Genre?,
        priceRange: PriceRange?
    ): Flow<NetworkResult<List<Restaurant>>> = flow {
        emit(NetworkResult.Loading)
        try {
            val response = apiService.getRestaurants(
                genre = genre?.takeIf { it != Genre.ALL }?.apiValue,
                priceRange = priceRange?.takeIf { it != PriceRange.ALL }?.apiValue
            )
            if (response.isSuccessful) {
                response.body()?.let { body ->
                    emit(NetworkResult.Success(body.toDomain()))
                } ?: emit(NetworkResult.Error("レスポンスが空です"))
            } else {
                emit(NetworkResult.Error("エラーが発生しました", response.code()))
            }
        } catch (e: Exception) {
            emit(NetworkResult.Error(e.message ?: "不明なエラーが発生しました"))
        }
    }
    // ... 他のメソッドも同じパターン
}
```

### 解説

`RestaurantRepository`インターフェースの具体的な実装クラスです。

#### クラスの構造

- **`@Inject constructor`**: Hiltがコンストラクタ引数（`ApiService`）を自動で注入する
- **`: RestaurantRepository`**: ドメイン層で定義されたインターフェースを実装
- **`override fun`**: インターフェースのメソッドをオーバーライド

#### Flow（Kotlin Flow）の使い方

```kotlin
fun getRestaurants(...): Flow<NetworkResult<List<Restaurant>>> = flow {
    emit(NetworkResult.Loading)     // ① まずLoading状態を送出
    try {
        val response = apiService.getRestaurants(...)  // ② API呼び出し
        if (response.isSuccessful) {
            emit(NetworkResult.Success(body.toDomain()))  // ③ 成功時
        } else {
            emit(NetworkResult.Error(...))  // ④ HTTPエラー時
        }
    } catch (e: Exception) {
        emit(NetworkResult.Error(...))  // ⑤ ネットワークエラー等
    }
}
```

`flow { }` ビルダーでFlowを作成し、`emit()`で値を順次送出します。ViewModelが`collect`することで値を受け取ります。

#### フィルター処理のロジック

```kotlin
genre = genre?.takeIf { it != Genre.ALL }?.apiValue
```

この1行を分解すると:
1. `genre?` — `null`なら以降は全てスキップ
2. `.takeIf { it != Genre.ALL }` — `Genre.ALL`（すべて）の場合は`null`を返す
3. `?.apiValue` — `null`でなければAPI用の値に変換

結果として:
- `Genre.ALL` → `null`（APIにパラメータを送らない = フィルタなし）
- `Genre.RAMEN` → `"ラーメン"`（APIにフィルタとして送る）

#### 3つのメソッドの比較

| メソッド | API | 入力 | 出力 |
|---------|-----|------|------|
| `getRestaurants` | `GET /v1/restaurants` | genre?, priceRange? | `Flow<NetworkResult<List<Restaurant>>>` |
| `getRestaurantDetail` | `GET /v1/restaurants/{id}` | id | `Flow<NetworkResult<Restaurant>>` |
| `spinRoulette` | `POST /v1/roulette` | genre?, priceRange? | `Flow<NetworkResult<Restaurant>>` |

---

## 5. NetworkResult（APIレスポンスラッパー）

**ファイル**: `util/NetworkResult.kt`

```kotlin
sealed class NetworkResult<out T> {
    data class Success<T>(val data: T) : NetworkResult<T>()
    data class Error(val message: String, val code: Int? = null) : NetworkResult<Nothing>()
    data object Loading : NetworkResult<Nothing>()
}
```

### 解説

API通信の3つの状態を型安全に表現する**Sealed Class**です。

#### なぜSealed Classを使うのか？

`when`式で全パターンを網羅的に処理でき、新しい状態を追加した場合にコンパイルエラーで漏れを検知できます。

```kotlin
when (result) {
    is NetworkResult.Loading -> { /* ローディング表示 */ }
    is NetworkResult.Success -> { /* データ表示 */ }
    is NetworkResult.Error -> { /* エラー表示 */ }
}
```

#### 各状態の詳細

| 状態 | クラス | 保持するデータ | 用途 |
|------|--------|--------------|------|
| 読み込み中 | `Loading` | なし | ローディングインジケーター表示 |
| 成功 | `Success<T>` | `data: T` | データの表示（ジェネリクスで型安全） |
| エラー | `Error` | `message: String`, `code: Int?` | エラーメッセージとHTTPステータスコード |

#### ジェネリクスの使い方

- `NetworkResult<List<Restaurant>>` — 成功時にレストランのリストを返す
- `NetworkResult<Restaurant>` — 成功時に1件のレストランを返す
- `<out T>` — 共変（covariant）。`NetworkResult<Restaurant>`は`NetworkResult<Any>`の型として扱える
- `NetworkResult<Nothing>` — `Error`と`Loading`はデータを持たないので`Nothing`型を使用
