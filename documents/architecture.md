# 福大メシ・ルーレット - アーキテクチャ概要

## 目次

- [プロジェクト概要](#プロジェクト概要)
- [技術スタック](#技術スタック)
- [アーキテクチャパターン](#アーキテクチャパターン)
- [ディレクトリ構成](#ディレクトリ構成)
- [データの流れ](#データの流れ)
- [各レイヤーの責務](#各レイヤーの責務)

---

## プロジェクト概要

「福大メシ・ルーレット」は、福岡大学周辺の飲食店をランダムに選出するAndroidアプリです。ルーレット機能でランダムにお店を選んだり、一覧から探したり、詳細情報を確認することができます。

**パッケージ名**: `com.fukudai.meshiroulette`

## 技術スタック

| カテゴリ | 技術 | バージョン |
|---------|------|-----------|
| 言語 | Kotlin | 2.0.21 |
| UI | Jetpack Compose | BOM 2024.12.01 |
| DI（依存性注入） | Hilt | 2.56.1 |
| API通信 | Retrofit + OkHttp | 2.11.0 / 4.12.0 |
| JSONパース | Gson | 2.13.2 |
| 画像読み込み | Coil | 2.7.0 |
| 非同期処理 | Coroutines + Flow | 1.9.0 |
| ナビゲーション | Navigation Compose | 2.8.5 |
| コード生成 | KSP | 2.0.21-1.0.27 |

**動作要件**:
- minSdk: 24 (Android 7.0 Nougat)
- targetSdk: 34 (Android 14)
- compileSdk: 35

## アーキテクチャパターン

本プロジェクトは **Clean Architecture + MVVM** パターンを採用しています。

```
┌─────────────────────────────────────────────┐
│              Presentation Layer              │
│  (Screen / ViewModel / UiState)              │
│                                              │
│  ┌──────────┐ ┌──────────┐ ┌──────────────┐ │
│  │ Screen   │→│ViewModel │→│  UiState     │ │
│  │(Compose) │←│          │←│ (data class) │ │
│  └──────────┘ └────┬─────┘ └──────────────┘ │
├────────────────────┼────────────────────────┤
│              Domain Layer                    │
│  (UseCase / Repository Interface / Model)    │
│                                              │
│  ┌──────────┐ ┌──────────────────┐           │
│  │ UseCase  │→│ Repository(I/F)  │           │
│  └──────────┘ └──────────────────┘           │
├────────────────────┼────────────────────────┤
│               Data Layer                     │
│  (RepositoryImpl / ApiService / DTO)         │
│                                              │
│  ┌──────────────┐ ┌──────────┐ ┌─────────┐  │
│  │RepositoryImpl│→│ApiService│→│   DTO   │  │
│  └──────────────┘ └──────────┘ └─────────┘  │
├──────────────────────────────────────────────┤
│               DI Layer                       │
│  (NetworkModule / RepositoryModule)          │
└──────────────────────────────────────────────┘
```

## ディレクトリ構成

```
app/src/main/java/com/fukudai/meshiroulette/
├── MeshiRouletteApplication.kt  ← Hiltエントリーポイント
├── MainActivity.kt              ← アプリのエントリーActivity
│
├── data/                        ← データ層
│   ├── remote/
│   │   └── ApiService.kt       ← Retrofit APIインターフェース
│   ├── model/
│   │   ├── RestaurantDto.kt    ← APIレスポンスDTO + リクエストボディ
│   │   └── RestaurantMapper.kt ← DTO → ドメインモデル変換
│   └── repository/
│       └── RestaurantRepositoryImpl.kt ← Repository実装
│
├── domain/                      ← ドメイン層
│   ├── model/
│   │   └── Restaurant.kt       ← ドメインモデル + Enum定義
│   ├── repository/
│   │   └── RestaurantRepository.kt ← Repositoryインターフェース
│   └── usecase/
│       ├── GetRestaurantsUseCase.kt      ← 一覧取得
│       ├── GetRestaurantDetailUseCase.kt ← 詳細取得
│       └── SpinRouletteUseCase.kt        ← ルーレット実行
│
├── presentation/                ← プレゼンテーション層
│   ├── navigation/
│   │   └── NavGraph.kt         ← 画面遷移定義 + BottomNavigation
│   ├── components/
│   │   ├── RestaurantCard.kt    ← レストランカード（共通）
│   │   ├── ErrorContent.kt      ← エラー表示（共通）
│   │   ├── LoadingContent.kt    ← ローディング表示（共通）
│   │   └── FilterBottomSheet.kt ← フィルターシート（共通）
│   ├── roulette/
│   │   ├── RouletteScreen.kt    ← ルーレット画面
│   │   ├── RouletteViewModel.kt ← ルーレットVM
│   │   └── RouletteUiState.kt   ← ルーレットUI状態
│   ├── list/
│   │   ├── RestaurantListScreen.kt    ← 一覧画面
│   │   ├── RestaurantListViewModel.kt ← 一覧VM
│   │   └── RestaurantListUiState.kt   ← 一覧UI状態
│   ├── detail/
│   │   ├── RestaurantDetailScreen.kt    ← 詳細画面
│   │   ├── RestaurantDetailViewModel.kt ← 詳細VM
│   │   └── RestaurantDetailUiState.kt   ← 詳細UI状態
│   └── theme/
│       └── Theme.kt             ← Material3テーマ定義
│
├── di/                          ← 依存性注入設定
│   ├── NetworkModule.kt         ← ネットワーク関連のDI
│   └── RepositoryModule.kt     ← Repository関連のDI
│
└── util/                        ← ユーティリティ
    └── NetworkResult.kt         ← APIレスポンスラッパー
```

## データの流れ

### API → 画面表示の流れ

```
[バックエンドAPI]
      │
      ▼
[ApiService] ── Retrofitが HTTP通信を実行
      │
      ▼
[RestaurantDto] ── JSONがGsonでDTOにデシリアライズ
      │
      ▼
[RestaurantMapper] ── DTO → ドメインモデルに変換
      │
      ▼
[RestaurantRepositoryImpl] ── NetworkResultでラップしてFlowで返す
      │
      ▼
[UseCase] ── Repositoryを呼ぶだけ（ビジネスロジック層）
      │
      ▼
[ViewModel] ── UseCaseを実行し、結果をStateFlowで保持
      │
      ▼
[Screen (Compose)] ── StateFlowをcollectしてUIに反映
```

### ユーザー操作 → API呼び出しの流れ

```
[ユーザーがボタンをタップ]
      │
      ▼
[Screen] ── ViewModel のメソッドを呼び出し
      │
      ▼
[ViewModel] ── viewModelScope.launchでコルーチン起動
      │
      ▼
[UseCase] ── Repositoryメソッドを呼び出し
      │
      ▼
[RepositoryImpl] ── ApiServiceを通じてHTTPリクエスト送信
      │
      ▼
[ApiService] ── Retrofitがネットワーク通信を実行
```

## 各レイヤーの責務

### Data Layer（データ層）
- API通信の実行（Retrofit）
- JSONのパース（Gson + DTO）
- DTO → ドメインモデルの変換（Mapper）
- エラーハンドリングとNetworkResultへのラップ

### Domain Layer（ドメイン層）
- ビジネスロジックの定義（UseCase）
- ドメインモデルの定義（Restaurant, Genre, PriceRange）
- Repositoryインターフェースの定義（データ層への依存を逆転）

### Presentation Layer（プレゼンテーション層）
- UIの描画（Jetpack Compose）
- UI状態の管理（ViewModel + StateFlow + UiState）
- ユーザーイベントの処理
- 画面遷移（Navigation Compose）

### DI Layer（依存性注入層）
- ネットワーク関連のインスタンス管理（OkHttp, Retrofit, ApiService）
- Repositoryのバインド（インターフェース ↔ 実装の紐付け）

---

> 詳細は各レイヤーごとのドキュメントを参照してください:
> - [data-layer.md](./data-layer.md) - データ層の詳細解説
> - [domain-layer.md](./domain-layer.md) - ドメイン層の詳細解説
> - [presentation-layer.md](./presentation-layer.md) - プレゼンテーション層の詳細解説
> - [di-module.md](./di-module.md) - DI設定の詳細解説
> - [build-config.md](./build-config.md) - ビルド設定の詳細解説
