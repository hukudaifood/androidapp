# 00 プロジェクト全体図

> 生成日: 2026-02-24 | skill: code-learner

---

## 何を作っているか

**福大メシ・ルーレット (Meshi Roulette)** は、福岡大学周辺の飲食店をランダムに選んでくれる Android アプリです。

- **ルーレット機能**: ジャンル・価格帯・営業状況でフィルタリングして店をランダム選択
- **一覧機能**: 周辺飲食店の全リスト表示
- **詳細機能**: 店舗詳細情報と Google Maps 連携

---

## 技術スタック

| カテゴリ | ライブラリ | バージョン |
|---------|-----------|-----------|
| UI | Jetpack Compose (BOM) | 2024.12.01 |
| 状態管理 | StateFlow + ViewModel | Kotlin 2.0.21 |
| DI | Hilt | 2.56.1 |
| ネットワーク | Retrofit2 + OkHttp3 | 2.11.0 / 4.12.0 |
| JSON | Gson | 2.13.2 |
| 画像 | Coil | 2.7.0 |
| 非同期 | Coroutines + Flow | 1.9.0 |
| ナビゲーション | Navigation Compose | 2.8.5 |

**API サーバー**: `https://fukudaifood-130668695114.asia-northeast1.run.app/`
**最小 SDK**: 24 (Android 7.0)
**ターゲット SDK**: 34

---

## ディレクトリ構造

```
androidapp/
├── app/
│   └── src/main/
│       ├── AndroidManifest.xml          # パーミッション・Activity登録
│       └── java/com/fukudai/meshiroulette/
│           │
│           ├── MeshiRouletteApplication.kt  # Hilt Application エントリポイント
│           ├── MainActivity.kt              # UI エントリポイント
│           │
│           ├── data/                        # 【データ層】APIと話す
│           │   ├── model/
│           │   │   ├── RestaurantDto.kt     # API レスポンス用データクラス
│           │   │   └── RestaurantMapper.kt  # DTO → Domain モデル変換
│           │   ├── remote/
│           │   │   └── ApiService.kt        # Retrofit API 定義
│           │   └── repository/
│           │       └── RestaurantRepositoryImpl.kt  # リポジトリ実装
│           │
│           ├── domain/                      # 【ドメイン層】ビジネスロジック
│           │   ├── model/
│           │   │   └── Restaurant.kt        # ドメインモデル + Genre/PriceRange Enum
│           │   ├── repository/
│           │   │   └── RestaurantRepository.kt  # リポジトリ インターフェース
│           │   └── usecase/
│           │       ├── GetRestaurantsUseCase.kt      # 一覧取得
│           │       ├── SpinRouletteUseCase.kt         # ルーレット実行
│           │       └── GetRestaurantDetailUseCase.kt  # 詳細取得
│           │
│           ├── presentation/               # 【プレゼンテーション層】UI
│           │   ├── components/             # 再利用可能な UI コンポーネント
│           │   │   ├── RestaurantCard.kt
│           │   │   ├── FilterBottomSheet.kt
│           │   │   ├── LoadingContent.kt
│           │   │   └── ErrorContent.kt
│           │   ├── roulette/               # ルーレット画面 (ホーム)
│           │   │   ├── RouletteScreen.kt
│           │   │   ├── RouletteViewModel.kt
│           │   │   └── RouletteUiState.kt
│           │   ├── list/                   # 一覧画面
│           │   │   ├── RestaurantListScreen.kt
│           │   │   ├── RestaurantListViewModel.kt
│           │   │   └── RestaurantListUiState.kt
│           │   ├── detail/                 # 詳細画面
│           │   │   ├── RestaurantDetailScreen.kt
│           │   │   ├── RestaurantDetailViewModel.kt
│           │   │   └── RestaurantDetailUiState.kt
│           │   ├── navigation/
│           │   │   └── NavGraph.kt         # 画面遷移の定義
│           │   └── theme/
│           │       └── Theme.kt            # Material3 テーマ定義
│           │
│           ├── di/                         # 【DI 設定】Hilt モジュール
│           │   ├── NetworkModule.kt        # Retrofit, OkHttp 提供
│           │   └── RepositoryModule.kt     # Repository バインディング
│           │
│           └── util/
│               └── NetworkResult.kt        # 通信結果の型ラッパー
└── docs/
    └── learning/                           # ← 今いる場所
```

---

## アプリの画面構成

```
┌─────────────────────────────────────────┐
│  底部ナビゲーションバー                  │
│  [ルーレット🎰]    [一覧📋]              │
└─────────────────────────────────────────┘

画面 1: ルーレット (起動時の初期画面)
┌─────────────────────────────────────────┐
│  フィルター設定 (BottomSheet)            │
│  [ジャンル選択] [価格帯] [営業中のみ]   │
│                                         │
│  ┌─────────────────────────────────┐   │
│  │  🎲 ルーレット結果カード         │   │
│  │     (アニメーション付き)         │   │
│  └─────────────────────────────────┘   │
│                                         │
│  [ルーレットを回す] ボタン              │
└─────────────────────────────────────────┘
             ↓ タップ
画面 3: 詳細 (RestaurantDetailScreen)
┌─────────────────────────────────────────┐
│  [← 戻る]                              │
│  店舗画像                              │
│  店名 / ジャンル / 価格帯              │
│  住所                                  │
│  営業時間                              │
│  [地図で見る → Google Maps]            │
└─────────────────────────────────────────┘

画面 2: 一覧 (RestaurantListScreen)
┌─────────────────────────────────────────┐
│  [ジャンルチップ] [営業中のみ]          │
│  ┌───────────────────────────────────┐ │
│  │ 🍱 RestaurantCard                 │ │
│  └───────────────────────────────────┘ │
│  ┌───────────────────────────────────┐ │
│  │ 🍜 RestaurantCard                 │ │
│  └───────────────────────────────────┘ │
│  ...                                   │
└─────────────────────────────────────────┘
```

---

## データフロー図 (全体)

```
ユーザー操作
    │
    ▼
Composable (Screen)        ← UI 表示・イベント受信
    │ collectAsStateWithLifecycle
    ▼
ViewModel                  ← 状態管理・ビジネスロジック呼び出し
    │ MutableStateFlow
    │ viewModelScope.launch
    ▼
UseCase                    ← 単一責務のユースケース
    │ Flow<NetworkResult<T>>
    ▼
Repository (Interface)     ← 抽象インターフェース
    │
    ▼
RepositoryImpl             ← 実際の通信・データ変換
    │ apiService.call()
    │ dto.toDomain()
    ▼
ApiService (Retrofit)      ← HTTP 通信定義
    │ suspend fun
    ▼
REST API サーバー
(Google Cloud Run)
```
