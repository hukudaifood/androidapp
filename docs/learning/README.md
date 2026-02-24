# 📚 コードベース学習ガイド - 福大メシ Android アプリ

このドキュメントは [skill: code-learner] によって自動生成されました。

生成日時: 2026-02-24

---

## 読む順番

| # | ファイル | 内容 | 読む目的 |
|---|---------|------|---------|
| 1 | [00_overview.md](00_overview.md) | プロジェクト全体図・技術スタック・ディレクトリ構造 | まず全体を把握する |
| 2 | [01_architecture.md](01_architecture.md) | Clean Architecture + MVVM の設計・各レイヤーの役割 | 設計思想を理解する |
| 3 | [02_data_flow.md](02_data_flow.md) | データの流れ・状態管理・シーケンス図 | コードを追えるようになる |
| 4 | [03_key_concepts.md](03_key_concepts.md) | Flow・Hilt・Navigation 等の重要パターン | 実装パターンを習得する |
| 5 | [04_getting_started.md](04_getting_started.md) | セットアップ・新機能追加手順・デバッグ | 実際に手を動かす |

---

## アプリ概要 (TL;DR)

**福大メシ・ルーレット** は福岡大学周辺の飲食店をランダム選択するアプリ。

```
技術スタック:
  UI:      Jetpack Compose + Material3
  状態:    StateFlow + ViewModel (MVVM)
  DI:      Hilt
  通信:    Retrofit2 + OkHttp3
  画像:    Coil
  非同期:  Coroutines + Flow
  遷移:    Navigation Compose

アーキテクチャ:
  Clean Architecture (Data / Domain / Presentation)

画面:
  ルーレット → 一覧 → 詳細
```

---

## クイックリファレンス

### 新しい画面を追加する
→ [04_getting_started.md #新しい画面を追加する手順](04_getting_started.md)

### API 通信の仕組みを理解する
→ [02_data_flow.md](02_data_flow.md)

### Hilt (DI) の仕組みを理解する
→ [03_key_concepts.md #2-hilt-による依存性注入-di](03_key_concepts.md)

### 状態管理 (StateFlow) を理解する
→ [02_data_flow.md #状態管理の仕組み](02_data_flow.md)

### よくある疑問 (FAQ)
→ [03_key_concepts.md #8-初学者がつまずきやすいポイント](03_key_concepts.md)
