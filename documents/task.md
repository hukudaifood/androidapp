福大メシ・ルーレットのAndroidアプリをKotlin + Jetpack Composeで開発してください。

【プロジェクト概要】
- minSdk: 24 (Android 7.0)
- targetSdk: 34
- アーキテクチャ: MVVM + Clean Architecture
- 依存性注入: Hilt
- 非同期処理: Coroutines + Flow
- API通信: Retrofit + OkHttp

【バックエンドAPI】
- ベースURL: [あなたのCloud Run URL]
- エンドポイント:
  * GET /v1/restaurants (一覧取得、genre/priceRangeでフィルタ可)
  * GET /v1/restaurants/:id (詳細取得)
  * POST /v1/roulette (ランダム選出、リクエストボディでフィルタ指定)

【実装してほしい機能】
1. データ層 (data/)
   - ApiService（Retrofit interface）
   - Restaurant, RouletteRequest/Response のdata class
   - RestaurantRepositoryImpl（RemoteDataSourceを使用）
   - NetworkResult sealed class（Success/Error/Loading）

2. ドメイン層 (domain/)
   - RestaurantRepository interface
   - GetRestaurantsUseCase, GetRestaurantDetailUseCase, SpinRouletteUseCase

3. プレゼンテーション層 (presentation/)
   - RouletteViewModel（StateFlow使用）
   - RestaurantListViewModel
   - RestaurantDetailViewModel
   - 各UIState data class

4. UI (Jetpack Compose)
   - RouletteScreen: ルーレットボタン、アニメーション、結果表示
   - RestaurantListScreen: LazyColumn、フィルタチップ
   - RestaurantDetailScreen: 詳細情報、地図起動ボタン
   - FilterBottomSheet: ジャンル・価格帯選択

5. DI設定
   - NetworkModule（Retrofit, OkHttp設定）
   - RepositoryModule
   - UseCaseModule

6. その他
   - Coilによる画像読み込み
   - Google Mapsインテント連携
   - エラー・ローディング状態のUI

【ディレクトリ構成】
app/src/main/java/com/fukudai/meshiroulette/
├── data/
│   ├── remote/
│   ├── model/
│   └── repository/
├── domain/
│   ├── model/
│   ├── repository/
│   └── usecase/
├── presentation/
│   ├── roulette/
│   ├── list/
│   └── detail/
├── di/
└── util/

段階的に実装し、各ステップで動作確認できるようにしてください。
まずはRetrofit設定とデータモデルから始めてください。
コード規約に則って行ってください