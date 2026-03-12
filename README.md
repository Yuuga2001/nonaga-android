# Hexlide for Android

六角形タイルの上でコマを滑らせるターン制ボードゲーム「Hexlide」の Android アプリです。

19 枚の六角形タイルで構成されたボード上で、自分の 3 つのコマを隣同士に並べたら勝ち。
コマを滑らせるだけでなく、盤面そのものを動かすことで攻守が一手ごとに入れ替わる、シンプルで奥深い対戦ストラテジーです。

## ゲームルール

各ターンは 2 つのフェーズで進行します。

1. **コマを滑らせる** --- 自分のコマを 6 方向のいずれかにスライド。コマは障害物か盤面の端まで滑り続けます。
2. **タイルを動かす** --- 空いているタイルを別の位置へ移動し、盤面を変化させます（ボードが分裂しない配置のみ有効）。

**勝利条件**: 自分の 3 つのコマが互いに隣接する配置（隣接ペアが 2 つ以上）になれば勝利です。

## ゲームモード

| モード | 説明 |
|--------|------|
| AI 対戦 | AI と 1 人で対戦。いつでもどこでも腕を磨ける |
| ふたりで対戦 | 1 台の端末で 2 人対戦。交互にタップして対面プレイ |
| オンライン対戦 | ルームコードを共有してリアルタイム対戦（Android / iOS / Web クロスプラットフォーム） |

## スクリーンショット

<!-- TODO: スクリーンショットを追加 -->

## アーキテクチャ

### 技術スタック

| 項目 | 内容 |
|------|------|
| 言語 | Kotlin 2.1.10 |
| UI | Jetpack Compose + Material 3 |
| 設計パターン | MVVM + Hilt DI |
| ナビゲーション | Navigation Compose 2.8.9 |
| ネットワーク | Retrofit 2.11 + OkHttp 4.12 |
| シリアライズ | Kotlinx Serialization 1.8 |
| 非同期処理 | Kotlin Coroutines + StateFlow |
| ロギング | Timber 5.0 |
| テスト | JUnit 4 + MockK 1.13 + Turbine 1.2 |
| Min SDK | 29 (Android 10) |
| Target SDK | 35 (Android 15) |
| ビルド | Gradle (Version Catalog) |

### パッケージ構成

```
jp.riverapp.hexlide/
├── HexlideApplication.kt          # Hilt Application エントリポイント
├── MainActivity.kt                 # Activity (ディープリンク対応)
├── data/
│   ├── model/                      # データモデル
│   │   ├── GameSession.kt              # ゲームセッション
│   │   ├── Piece.kt                    # コマ
│   │   ├── Tile.kt                     # タイル
│   │   ├── PlayerColor.kt              # プレイヤー色
│   │   ├── GamePhase.kt                # ゲームフェーズ
│   │   ├── GameStatus.kt               # ゲーム状態
│   │   ├── GameMode.kt                 # ゲームモード
│   │   ├── MoveRequest.kt              # API リクエスト
│   │   ├── SelectedItem.kt             # 選択状態
│   │   └── ApiError.kt                 # API エラー
│   ├── remote/
│   │   └── HexlideApi.kt              # Retrofit API インターフェース
│   └── repository/
│       └── GameRepository.kt          # リポジトリ層
├── di/
│   ├── AppModule.kt                # Hilt アプリケーションモジュール
│   └── NetworkModule.kt            # Hilt ネットワークモジュール
├── domain/
│   ├── logic/
│   │   ├── AIEngine.kt                 # AI 評価関数
│   │   ├── GameLogic.kt                # ゲームロジック
│   │   ├── GameConstants.kt            # 初期配置・定数
│   │   └── HexMath.kt                  # 六角形座標系
│   └── service/
│       ├── GamePollingService.kt       # オンラインポーリング
│       └── PlayerIdentityService.kt    # プレイヤー ID 管理
├── presentation/
│   ├── component/
│   │   ├── HexBoard.kt                 # Canvas ベース六角形ボード
│   │   ├── GameStatusBar.kt            # ステータスバー
│   │   ├── VictoryOverlay.kt           # 勝利オーバーレイ
│   │   ├── ConfettiEffect.kt           # 紙吹雪エフェクト
│   │   ├── ModeSelectorSheet.kt        # モード選択シート
│   │   ├── RulesSection.kt             # ルール表示
│   │   └── ShuffleAnimation.kt         # シャッフルアニメーション
│   ├── localization/
│   │   ├── LocalizationManager.kt      # 言語管理
│   │   └── LocalizedStrings.kt         # 翻訳文字列（15 言語）
│   ├── navigation/
│   │   ├── HexlideNavHost.kt           # ナビゲーションホスト
│   │   └── Screen.kt                   # 画面定義
│   ├── screen/
│   │   ├── game/
│   │   │   └── GameScreen.kt           # ゲーム画面
│   │   ├── online/
│   │   │   ├── OnlineLobbyScreen.kt    # オンラインロビー
│   │   │   └── OnlineGameScreen.kt     # オンライン対戦画面
│   │   └── settings/
│   │       ├── SettingsScreen.kt       # 設定画面
│   │       └── InAppWebView.kt         # アプリ内ブラウザ
│   ├── theme/
│   │   ├── Color.kt                    # カラー定義（38 色）
│   │   └── Theme.kt                    # Material 3 テーマ
│   └── viewmodel/
│       ├── LocalGameViewModel.kt       # ローカルゲーム ViewModel
│       ├── OnlineGameViewModel.kt      # オンラインゲーム ViewModel
│       └── OnlineLobbyViewModel.kt     # ロビー ViewModel
└── util/
    └── Constants.kt                # API URL・タイミング定数
```

### 六角形座標系

Axial 座標 (q, r) を採用しています。ボードは中心 `(0,0)` から半径 2 の六角形グリッド（19 タイル）です。

```
     (-1,-1) (0,-2) (1,-2)
   (-2,0) (-1,0) (0,-1) (1,-1) (2,-2)
     (-2,1) (-1,1) (0,0) (1,0) (2,-1)
   (-2,2) (-1,2) (0,1) (1,1) (2,0)
     (0,2)  (1,1)  (1,1)
```

6 方向の隣接ベクトル:

| 方向 | (dq, dr) |
|------|----------|
| 東 | (+1, 0) |
| 北東 | (+1, -1) |
| 北西 | (0, -1) |
| 西 | (-1, 0) |
| 南西 | (-1, +1) |
| 南東 | (0, +1) |

### AI エンジン

`AIEngine` はヒューリスティック評価関数ベースの AI です。

**コマ移動の評価**:
- 勝利判定（隣接ペア >= 2 で即座に選択）
- コマ間の最小距離の最小化
- 重心からのコンパクト性
- 中央寄りバイアス
- ランダムノイズによるプレイの多様性

**タイル移動の評価**:
- 敵の次ターン勝利を防ぐ防御評価
- 自コマの隣接ペア数の最大化
- 敵コマの隣接ペア数の抑制

AI の思考は 500ms の遅延を挟み、自然な対戦感を演出します。

## Web 連携

### システム全体像

```
┌──────────────────┐        HTTPS         ┌──────────────────────┐
│  Android アプリ    │ <------------------> │  hexlide.riverapp.jp │
│  (このリポジトリ)   │   REST API (JSON)    │  バックエンドサーバー     │
└──────────────────┘                      └──────────────────────┘
       |                                          |
       |  WebView (アプリ内ブラウザ)                  |
       |-- /app/how-to-play  遊び方                |
       |-- /app/privacy      プライバシーポリシー      |
       +-- /app              Web サイト             |
                                                  |
                                ┌-----------------┘
                                |
                         ┌──────┴──────┐
                         │  Web 版      │
                         │ hexlide.     │
                         │ riverapp.jp  │
                         └─────────────┘
```

Android アプリ・iOS アプリ・Web 版は同一のバックエンド API を共有しています。
オンライン対戦では Android <-> iOS <-> Web 間のクロスプラットフォーム対戦が可能です。

### API エンドポイント

ベース URL: `https://hexlide.riverapp.jp`

| メソッド | パス | 説明 |
|---------|------|------|
| POST | `/api/game` | ゲーム作成（ルームコード発行） |
| GET | `/api/game/{gameId}` | ゲーム状態取得 |
| POST | `/api/game/{gameId}/join` | ゲーム参加 |
| POST | `/api/game/{gameId}/move` | コマ・タイル移動 |
| DELETE | `/api/game/{gameId}` | ゲーム放棄 |
| POST | `/api/game/{gameId}/rematch` | リマッチ要求 |
| GET | `/api/game/room/{roomCode}` | ルームコードからゲーム検索 |

### アプリ内 Web ページ

| ページ | URL | 用途 |
|--------|-----|------|
| 遊び方 | `/app/how-to-play` | ルール説明（日本語/英語） |
| プライバシーポリシー | `/app/privacy` | 個人情報保護方針 |
| Web サイト | `/app` | Hexlide 公式ページ |
| お問い合わせ | Google Forms | ユーザーからの問い合わせ |

### オンライン対戦の仕組み

**通信方式**: HTTP ポーリング（1 秒間隔）

```
ホスト                      サーバー                      ゲスト
  |                           |                           |
  |-- POST /api/game -------->|                           |
  |<-- roomCode: 123456 ------|                           |
  |                           |                           |
  |    ルームコード共有         |                           |
  |-------------------------------------------------------->|
  |                           |                           |
  |                           |<-- GET /room/123456 ------|
  |                           |<-- POST /{id}/join -------|
  |                           |                           |
  |-- GET /{id} (1秒毎) ----->|<-- GET /{id} (1秒毎) -----|
  |<-- game state ------------|---- game state ---------->|
  |                           |                           |
  |-- POST /{id}/move ------->|                           |
  |   (楽観的 UI 更新)         |---- 次回ポーリングで反映 -->|
```

- **ルームコード**: 6 桁の数字。ホストが生成し、ゲストに共有
- **ディープリンク**: `https://hexlide.riverapp.jp/game/{gameId}` から直接参加可能
- **楽観的更新**: 移動後、サーバー応答を待たずに UI を即時更新
- **再接続**: 連続 5 回の通信失敗でエラー表示。アプリ復帰時に自動再開

## 対応言語

15 言語 + 端末設定自動検出に対応しています。

| 言語 | コード |
|------|--------|
| English | en |
| 日本語 | ja |
| 한국어 | ko |
| 简体中文 | zh-Hans |
| 繁體中文 | zh-Hant |
| Español | es |
| Français | fr |
| Deutsch | de |
| Português | pt |
| Italiano | it |
| Русский | ru |
| ไทย | th |
| Tiếng Việt | vi |
| Bahasa Indonesia | id |
| Türkçe | tr |

デフォルトは「端末設定に合わせる」で、未対応言語は英語にフォールバックします。

### 多言語の設計

- `LocalizationManager` が言語の選択・解決・永続化を一元管理
- `LocalizedStrings` に全翻訳を型安全なコード内定義として保持
- Android 標準の `strings.xml` ではなく、iOS 版と同一のコード内定義方式を採用
- 設定画面から手動切替が可能（即時反映、アプリ再起動不要）

## テスト

154 件のユニットテストで主要ロジックを網羅しています。

| テストファイル | 対象 | 件数 |
|--------------|------|------|
| GameLogicTests | ボード接続性、スライド移動、勝利判定、タイル移動 | 31 |
| LocalizationTests | 全 15 言語の文字列検証（リフレクションベース） | 25 |
| LocalGameViewModelTests | ローカルゲーム ViewModel | 17 |
| ModelTests | データモデル（シリアライズ・同値性） | 15 |
| HexMathTests | 座標変換、隣接判定、距離計算 | 14 |
| AIEngineTests | AI 評価関数 | 11 |
| OnlineLobbyViewModelTests | ルーム作成・参加 | 9 |
| GameConstantsTests | 初期配置の整合性 | 8 |
| OnlineGameViewModelTests | オンラインゲーム同期 | 8 |
| SerializationTests | JSON シリアライズ/デシリアライズ | 5 |
| ApiServiceTests | API リクエスト/レスポンス（MockWebServer） | 4 |
| SettingsScreenTests | 設定画面 | 4 |
| PlayerIdentityTests | プレイヤー ID 管理 | 3 |

### テストの実行

```bash
# 全ユニットテスト
./gradlew test

# デバッグビルド向けテストのみ
./gradlew testDebugUnitTest
```

## ビルド

### 必要環境

- Android Studio Ladybug 以降
- JDK 17
- Android SDK 35

### デバッグビルド

```bash
./gradlew assembleDebug
```

### リリースビルド

```bash
# AAB (Google Play 用)
./gradlew bundleRelease

# APK
./gradlew assembleRelease
```

> **注意**: リリースビルドには署名設定（keystore）が必要です。`signingConfigs` を `app/build.gradle.kts` に設定するか、コマンドラインで指定してください。

### リリースビルド設定

リリースビルドでは以下の最適化が有効です:

- **コード縮小** (`isMinifyEnabled = true`): 未使用コードの除去
- **リソース縮小** (`isShrinkResources = true`): 未使用リソースの除去
- **ProGuard/R8**: Kotlinx Serialization、Retrofit、OkHttp 向けのルールを設定済み

## CI/CD

GitHub Actions による自動パイプライン:

```yaml
# .github/workflows/ci.yml
# トリガー: main ブランチへの push / PR
# ジョブ:
#   1. test  - ユニットテスト実行
#   2. build - デバッグ APK ビルド (test 成功後)
```

- JDK 17 (Temurin)
- Gradle キャッシュ (`gradle/actions/setup-gradle@v4`)
- テスト成功後にビルドを実行

## プライバシー

- トラッキング: なし
- データ収集: なし
- 使用ストレージ: SharedPreferences（プレイヤー ID・言語設定の保存のみ）
- UUID ベースの匿名プレイヤー識別
- 広告なし

## 関連プロジェクト

- [Hexlide iOS](https://github.com/Yuuga2001/nonaga-ios)
- [Hexlide Web](https://hexlide.riverapp.jp)
