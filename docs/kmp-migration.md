# KMP 移行計画

SvelteKit + Capacitor の Web 版は `legacy/svelte` ブランチで凍結した。
`master` の本線は Kotlin Multiplatform に切り替える。

## いまの状態

```text
master
├── kmp/                  ← 本線
│   ├── shared/           ← 共有コア（モデル・検証・計算・DB・API）
│   ├── composeApp/       ← Compose Multiplatform の画面（Android / Wasm / iOS）
│   ├── syllabusTool/     ← TSV → CBOR の変換
│   └── data/syllabus/    ← 公開シラバスの TSV（一次ソース）
├── docs/                 ← 解析メモと本計画
├── src/, android/, ios/  ← legacy/svelte の内容（参照用に残置。段階的に削除する）
└── tools/                ← 公開データ生成（KMP でも再利用）
```

`legacy/svelte` には 2026-11-02 時点の Svelte 版（データ検証の修正込み）が入っている。
`git checkout legacy/svelte` でいつでも戻せる。

## ツールチェーン

実測で動いた組み合わせ。

| 要素 | バージョン |
| --- | --- |
| Gradle | 8.14.3（`/data/android-caches/.gradle` にキャッシュ済み） |
| Kotlin | 2.4.21 |
| AGP | 8.13.2 |
| compileSdk / minSdk | 36 / 26 |
| JDK | 21 |
| Node | 22（Wasm テスト用） |
| Compose Multiplatform | 1.11.1（UI 追加時に使用） |
| kotlinx-serialization | 1.9.0 |
| SQLDelight | 2.4.1 |

```bash
cd kmp
./gradlew :shared:jvmTest          # 62 tests（JVM 固有の DB / API テストを含む）
./gradlew :shared:wasmJsNodeTest   # 40 tests（共通コード + Wasm のインメモリ API）
./gradlew :shared:assembleDebug    # Android AAR
./gradlew :composeApp:assembleDebug            # Android APK
./gradlew :composeApp:wasmJsBrowserDistribution # Wasm の配布物
./gradlew :syllabusTool:run                     # TSV から CBOR を生成
```

iOS ターゲット（`iosArm64` / `iosSimulatorArm64` / `iosX64`）は宣言してあるが、
Kotlin/Native の Apple ターゲットは macOS でしかコンパイルできない。
この Linux 機では Android と Wasm を検証し、iOS は LAN の Intel Mac（Temurin 21）で Kotlin コンパイルまで確認した。リンクと実機実行には Xcode が必要。

## ターゲット構成

| ターゲット | 状態 | 備考 |
| --- | --- | --- |
| Android | ビルド可 | 共有コアの AAR と `composeApp` の APK を生成済み |
| Web (Wasm) | ビルド・テスト可 | `composeApp` の配布物を生成済み。Chrome で描画確認 |
| iOS | Kotlin コンパイル可 | 共有コアは `iosX64` / `iosArm64` / `iosSimulatorArm64`、Compose アプリは `iosArm64` / `iosSimulatorArm64` を Mac で確認。Intel シミュレータは Compose 1.11 が未対応 |
| JVM | テスト用 | ロジックの高速な検証に使う |

UI は Compose Multiplatform で Android / iOS / Wasm を共通化する方針。
Wasm は DOM ではなく Canvas に描画するため、SEO や既存の PWA 資産は失われる。
Web 版に検索流入が必要なら、後から SSR 用の薄いシェルを別途用意する。

## 共有コアの設計

`kmp/shared/src/commonMain/kotlin/org/openyaske/core/`

| パッケージ | 内容 |
| --- | --- |
| `model` | Course / CourseSession / Term / SchoolEvent / CourseTask / CourseGrade / Todo / CalendarEvent / AttendanceRecord / SyllabusEntry / UserData |
| `logic` | AttendanceCalculator / GpaCalculator / TimetableValidator / SessionGenerator / AgendaProjector |
| `validation` | UserDataCodec（JSON 入出力 + 正規化）、SyllabusTsvParser、IsoDate |
| `data` | DatabaseRepository（モデル ↔ SQLite の変換）、プラットフォーム別ドライバ |
| `api` | CoreApi（画面向けの状態・射影・変更 API）、AppSettings、SyllabusCatalog |

APK の欠陥をそのまま持ち込まないための決定。

| APK の欠陥 | KMP での扱い |
| --- | --- |
| 休講が出席テーブルと休講テーブルに分裂 | `AttendanceStatus.CANCELLED` の 1 系統に統一 |
| 出席記録に一意制約なし | `courseId + date` を論理キーとして正規化時に統合 |
| 日付を epoch millis で保存 | `YYYY-MM-DD` 文字列。タイムゾーン非依存 |
| 外部キーなし | SQLDelight のスキーマで外部キーと CASCADE を定義済み |
| 学年の主キーが instanceId | `AcademicYear.idOf(year, semester)` で導出 |
| 時限の範囲チェックなし | `Course.occupiedSlots()` と `TimetableValidator` で保存前に検出 |
| 単位数が nullable / 3 値固定 | `Int`（0〜30 にクランプ） |
| サブタスクが JSON 文字列 | `List<SubTask>` |
| 遅刻の重みが実装ごとに違う | `AttendancePolicy.lateWeight` で明示。既定 0.5（Svelte 版に合わせる） |
| スキーマバージョンなし | `UserData.schemaVersion` |
| シラバスの「月1, 月2」を 1 コマに潰す | `SyllabusTsvParser` がコマごとに 1 件作る（null コマも保持） |
| 通知 ID が String.hashCode() 由来 | Android 実装時に安定キー（cellId / todoId）をそのまま使う |

`UserDataCodec.decode` は 1 レコードずつデコードし、壊れた行だけを落として件数を返す。
1 行の型崩れでバックアップ全体が読めなくなる事態を避ける。

## オフライン方針

OpenYASKE はネットワークを前提にしない。時間割・TODO・カレンダー・出席・GPA・PDF・シラバス検索は
すべて端末内で完結させ、公開データはビルド時に同梱する。天気・交通・オンライン同期は
`offline` と `full` の flavor に分け、`offline` では `INTERNET` を宣言しない。
転送は QR を第一候補にし、サイズの実測と形式案は `docs/offline-and-qr-transfer.md` にある。

## 開発時のビルド

Gradle の設定キャッシュとビルドキャッシュを有効にしている。実測値は次のとおり。

| タスク | 時間 |
| --- | --- |
| `:shared:jvmTest`（変更なし） | 約 1 秒 |
| `:shared:jvmTest --rerun-tasks` | 約 34 秒 |
| `:composeApp:assembleDebug`（変更なし） | 約 8 秒 |
| `:composeApp:compileKotlinWasmJs`（変更なし） | 約 2 秒 |
| `:composeApp:wasmJsBrowserDistribution`（変更あり） | 約 78 秒 |

Wasm の配布物は webpack と Skiko の Wasm を含むため、これだけ時間がかかる。
開発中は `jvmTest` と `compileKotlinWasmJs` / `compileDebugKotlinAndroid` を使い、
APK と配布物は区切りでだけ作る。4 コアの環境では、JVM・Wasm テスト・APK・配布物を
1 コマンドで同時に回すと数分かかるのが普通で、停止ではない。

## 移行フェーズ

1. 共有コア（完了）
   モデル、検証、出席・GPA・時間割の計算、JSON コーデック、シラバス TSV パーサ、
   JVM/Wasm/Android のテスト。
2. Compose アプリケーション（着手）
   `kmp/composeApp` にホーム・ToDo・時間割・カレンダー・さがすの 5 画面と設定、科目詳細を作った。
   画面構成と M3E の適用方針は `docs/ui-structure.md` にある。
   テーマは Svelte 版の ThemePack をそのまま持ち込まず、Material3 のカラースキームへ写像する。
3. 永続化
   SQLDelight は導入済み（スキーマ・リポジトリ・JVM/Android/iOS ドライバ）。設定の永続化は未接続。
   RemoteStorage はプロトコル実装が重いので、まずローカル + エクスポート JSON で成立させ、
   同期は WebDAV か自前 API を比較してから決める。
4. iOS
   macOS ランナーで `iosSimulatorArm64` をビルドし、Xcode プロジェクトを接続する。
5. Web
   Wasm ビルドを配信。シラバスは CBOR を同梱し、検索は `SyllabusCatalog` が行う。
   データ形式と更新手順は `docs/syllabus-data.md` にある。
6. 仕上げ
   ウィジェット（Glance）、通知、PDF、シラバス検索を個別に移植する。
   UI の品質は `unslop` スキルで監査する（これはリンタではなくデザインレビュー手順）。

## 未決事項

- Compose Multiplatform の Wasm は描画品質と初回ロードがまだ重い。Web を Wasm にするか、
  Kotlin/JS + Svelte のままにするかは、2 の画面移植後に実測して決める。
- 同期のバックエンド。RemoteStorage 継続か、WebDAV か、Firebase か。
- iOS の配布。Mac と Apple Developer アカウントが前提。
