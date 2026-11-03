# コア設計と戻る挙動のレビュー

## 結論

コアを共通にする方針は正しい。UI は薄くするべきだが、UI から業務ルールを完全に消すのではなく、
「表示状態・入力・ナビゲーション」だけを持たせ、業務ルールは共通コアに置く。

いまの `CoreApi` は 1 クラスに寄りすぎている。機能追加のたびに巨大化し、
画面が `UserData` 全体を購読するため、機能同士がデータ経由で密結合して見える。
方向は合っているが、このまま大きくしていくのは危険。

## いまの結合

```text
Compose 画面
   ↓ 参照
CoreApi（状態・投影・コマンド・設定・バックアップ・検索）
   ↓
UserData（全機能の集合）
   ↓
CoreRepository → SQLite / メモリ
```

共有しているもの:

- Course → CourseSession → AttendanceRecord
- Course → CourseTask、CourseGrade
- CourseSession → SchoolEvent
- Term → Course、Session

この結合は業務上必要で、壊すべきではない。問題は「共有モデル」ではなく、
`CoreApi` と `UserData` が全機能の入口になっていること。

## 問題点

1. `CoreApi` がファサード兼サービス兼ユースケースになっている
2. 画面が `state: StateFlow<UserData>` 全体を購読する
3. 1 回の更新で `repository.loadAll()` を呼び、全機能のスナップショットを作り直す
4. 画面ごとの投影を Compose の `remember` で計算しており、UI 側にロジックが漏れやすい
5. 設定・シラバス・バックアップまで同じクラスにあり、責務が広い
6. 機能を追加すると `UserData`、`CoreApi`、画面の 3 か所を同時に変える必要がある

## 目標とする形

```text
Compose 画面
   ↓ 画面状態とコマンドだけ
ScreenStateHolder（Timetable / Tasks / Attendance / Grades ...）
   ↓
CoreServices（機能ごとのユースケース）
   ↓
CoreRepository（同一トランザクション）
   ↓
SQLite / メモリ
```

### 共有するもの

- モデル、ID、検証、日付計算
- 授業回の生成、出席率、GPA、時間割の競合
- 科目・授業回・課題・成績の関係
- JSON コーデック、バックアップ
- シラバス検索索引

### UI に残すもの

- 画面の一時状態（選択中の学期、ダイアログ、スクロール位置）
- 入力の途中状態
- ナビゲーションと戻る
- 表示フォーマット（日付、時刻、単位）
- プラットフォーム固有の通知・ウィジェット・カメラ・共有

### 分割案

| サービス | 責務 |
| --- | --- |
| `CourseService` | 学期、科目、時間割外、集中、連携 |
| `ScheduleService` | 授業回の生成、休講、補講、アジェンダ |
| `AttendanceService` | 出席記録、出席率、危険判定 |
| `TaskService` | 科目課題、期限、状態、繰り返し |
| `GradeService` | 成績、GPA、シナリオ |
| `SettingsService` | 表示・通知・同期設定 |
| `SyllabusService` | 検索、索引、年度絞り込み |
| `BackupService` | エクスポート、インポート、正規化 |

`CoreApi` はこれらを束ねるだけの薄いファサードにし、画面は必要なら
`TimetableStateHolder` のような画面単位の状態ホルダーを挟む。

### 移行順

1. `CoreApi` のメソッドを機能ごとのサービスへ移す
2. `CoreApi` は互換ファサードとして残す
3. 画面を画面状態ホルダー経由に変える
4. `state: StateFlow<UserData>` の全購読を、機能ごとの Flow / セレクタへ置き換える
5. 更新時の全体再読み込みを、変更されたテーブルだけの再読込に変える

## 戻る挙動

本家は iOS 前提のため、Android のシステム Back と Web のブラウザ Back に対する
明示的な設計が見えない。OpenYASKE ではプラットフォームごとの標準に合わせる。

### 共通ルール

- シートやダイアログが開いていれば、まずそれを閉じる
- 科目詳細・設定・GPA は 1 段階の詳細画面として戻る
- 下部ナビのタブはルート。戻るとホームへ
- ホームで戻る操作をしたら、Android はアプリ終了、Web はブラウザに任せる
- iOS は画面左上の戻るボタンとシートのスワイプ

### Android

- `BackHandler` をルートに置き、戻れる状態のときだけ有効にする
- `android:enableOnBackInvokedCallback="true"` で predictive back を有効にする
- タブのルートでは戻るを消費せず、システムに返す

### Web

- 画面に対応するパスを持つ
  - `/`, `/todo`, `/timetable`, `/calendar`, `/search`
  - `/settings`, `/gpa`, `/course/<id>`
- 進むときは `history.pushState`、戻るときは `replaceState` で URL を同期する
- 初期表示はパスから画面を復元する（ディープリンク）
- ブラウザの戻るは共通 `BackHandler` で受ける
- シートやダイアログは履歴を増やさず、閉じる操作として扱う

### iOS

- タブのルートには戻る概念を置かない
- 詳細画面は左上の戻る、シートはスワイプで閉じる
- システムの戻るボタンは無いため、Android の BackHandler は使わない

## 棚卸し（2026-11-03）

「神クラス」になっていないかを、行数と責務で見直した。

| 対象 | 行数 | 判定 | 対応 |
| --- | --- | --- | --- |
| `CoreApi` | 259（+ 機能サービス 8 本） | 分割済み。公開 API は互換のまま | `services/` へ委譲 |
| `CalendarScreen` | 467 | 画面 1 つとしては大きい | 月グリッドを `ui/calendar/MonthGrid.kt` に分離 |
| `TimetableScreen` | 376 | 同上 | グリッドとセルを `ui/timetable/TimetableGrid.kt` に分離 |
| `HomeScreen` | 422 | セクションが多い | リンクは `CampusLinks.kt` に分離済み |
| `UiStrings` | 601 → 21 + 147×4 | データの塊 | 言語ごとのファイルに分割 |

### コアの分割

`CoreApi` は入口だけを持ち、実装は `core/api/services/` に置く。

| サービス | 仕事 |
| --- | --- |
| `CourseService` | 学期、科目、授業回、補講 |
| `AttendanceService` | 出席の記録と集計 |
| `TaskService` | 課題、学事予定 |
| `GradeService` | 成績、GPA、シナリオ |
| `ScheduleService` | 時間割、アジェンダ、科目詳細の組み立て |
| `SyllabusService` | 同梱シラバスの索引と検索 |
| `SettingsService` | 設定の保存と正規化 |
| `BackupService` | JSON の書き出しと読み込み |

`CoreStore` はリポジトリ・時計・ID・スナップショット・変更通知だけを持ち、業務ルールは持たない。

### ファサードを残すか

結論は「薄いファサードとして残す」。理由は 3 つ。

- 画面から見た入口が 1 つだと、依存の注入と状態の購読が 1 行で済む
- 変更通知・時計・ID の扱いを 1 か所に閉じ込められる
- 画面をまたぐ処理（保存 → 再読込 → 再描画）を各サービスに散らさずに済む

ただし、ファサードに機能を足し続けると元に戻る。次の約束を置く。

- `CoreApi` は 300 行を上限にする。超えたらサービスへ逃がす
- 新しい画面は `api.tasks` のような**サービスを直接**受け取る。ファサード経由の新規メソッドは足さない
- 既存の委譲メソッドは、その画面を触ったときにサービス直参照へ置き換える

いまは 8 つのサービスを `CoreApi` のプロパティとして公開しているので、
`CoreApi` を丸ごと捨てる必要はない。捨てたくなったときに、画面側の引数だけ差し替えれば済む形にしてある。
| `GpaCalculator` / `GradeCatalog` | 小 | 純粋な計算 | そのまま |
| `SyllabusCatalog` | 約 90 | 索引と検索だけ | そのまま |

今回、`CoreApi.gpa()` から集計と行の組み立てを `logic/GpaProjector.kt` に移した。
`CoreApi` に残っているのは、保存・検証・状態の再読み込みという入口の仕事だけに寄せていく方針。

## 実装状況

- Compose Multiplatform の `ui-backhandler` を導入
- Android の predictive back を manifest で有効化
- Web のパス読み書き（`initialWebPath` / `pushWebPath` / `replaceWebPath`）を追加
- 設定・GPA・科目詳細を戻る対象にした
- `CoreApi` のサービス分割は未実施。次のリファクタリングで行う
