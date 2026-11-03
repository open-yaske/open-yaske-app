# 画面向けコア API

画面は `CoreApi` だけを参照する。SQLDelight や DB の行型は画面に出さない。

```kotlin
val api = CoreApi(
    repository = DatabaseRepository(openDatabase(AndroidDatabaseDriverFactory(context))),
    syllabus = bundledSyllabus
)

api.state     // StateFlow<UserData>
api.settings  // StateFlow<AppSettings>
api.snapshot  // UserData（現在値）
```

## 画面用の射影

| API | 返すもの | 用途 |
| --- | --- | --- |
| `timetable(termId)` | `TimetableScreenState` | 時間割のセル、時間割外科目、競合、単位数 |
| `agenda(start, end)` | `List<AgendaEntry>` | 学校アジェンダ（授業回・学事・課題） |
| `attendance(courseId)` | `AttendanceScreenState` | 出席率、記録、授業回 |
| `courseDetail(courseId)` | `CourseDetailView` | 科目詳細（授業回、課題、出席、成績） |
| `gpa(scenarioId)` | `GpaScreenState` | 科目別の成績行、GPA、what-if |
| `searchSyllabus(query, year, limit)` | `List<SyllabusSearchHit>` | シラバス検索 |

`timetable` の `TimetableScreenState` は、設定の表示曜日と最大時限で絞ったセルを返す。
週次パターンを持たない科目は `unscheduled` に入る。重複は `issues` に `TimetableIssue` として出る。

`agenda` は個人の予定を含めない。授業回、学事、科目課題だけを日付順に返す。

## 変更 API

| API | 内容 |
| --- | --- |
| `saveTerm(term)` | 学期を保存。ID が空なら年と学期から作る |
| `saveCourse(course)` | 科目を保存。ID・作成時刻・更新時刻を補完 |
| `deleteCourse(id)` | 科目を論理削除 |
| `saveSession(session)` | 授業回を保存。時限または開始時刻が必要 |
| `cancelSession(id)` | 休講にする |
| `addMakeupSession(...)` | 補講を追加。元の回を `replacesSessionId` で指定できる |
| `setAttendance(courseId, status, sessionId?, date?)` | 出席を記録。授業回または日付で特定 |
| `removeAttendance(...)` | 出席を論理削除 |
| `saveTask(task)` | 科目課題を保存 |
| `setTaskStatus(id, status)` | 課題の状態を変更 |
| `deleteTask(id)` | 課題を論理削除 |
| `saveSchoolEvent(event)` | 学事・休講・試験などを保存 |
| `saveGrade(grade)` | 科目の成績を保存 |
| `deleteGrade(courseId)` | 成績を削除 |
| `saveScenario(name, overrides)` | GPA の what-if を作成 |
| `deleteScenario(id)` | シナリオを削除 |
| `updateSettings { ... }` | 設定を更新して保存 |
| `setSyllabus(entries)` | 検索対象のシラバスを差し替え |
| `exportJson()` | ユーザーデータを JSON で出力 |
| `importJson(raw)` | JSON を検証して復元 |

変更系は保存後に `state` を読み直す。画面は Flow を購読していれば再描画できる。

## 入力の検証

`CoreApi` は画面から来た値をそのまま保存しない。

- 科目名、課題名、学事名は空を拒否
- 曜日と時限は両方指定または両方なし
- 時限は 1..7、コマ数は時限を超えない
- 日付は `YYYY-MM-DD`、時刻は `HH:MM`
- 成績が `GRADED` なら評価が必須
- 休講の授業回に出席を付けない
- 科目・学期・授業回の参照が切れていないか確認

違反は `CoreException` になる。

## 設定

`AppSettings` は 1 つの型に集約する。

- テーマ、ロケール
- 週の開始、表示曜日、最大時限
- 時限の開始時刻
- 出席ポリシー（遅刻の重み、欠席上限、危険ライン）
- 通知（有効、何分前、授業中抑制、科目別）

`updateSettings` は値を正規化して JSON で保存する。画面ごとに別の設定を持たない。

## バックアップ

`exportJson()` は `UserDataCodec` の JSON を返す。`importJson()` は
1 レコードずつ検証し、壊れた行を落として件数を返す。復元は子テーブルから順に消して入れ直す。

## スレッド

DB 呼び出しは同期的だが、大量の読み込みや復元は画面スレッドの外で呼ぶ。
`state` と `settings` は `StateFlow` なので、購読側で更新を受け取れる。

## プラットフォーム

| ターゲット | 保存層 | 検証 |
| --- | --- | --- |
| Android | `AndroidSqliteDriver` | コンパイル（AAR） |
| iOS | `NativeSqliteDriver` | `iosX64` / `iosArm64` / `iosSimulatorArm64` の Kotlin コンパイル |
| JVM | `JdbcSqliteDriver`（インメモリ / ファイル） | 60 テスト |
| Wasm | `InMemoryRepository` + localStorage | コンパイルと 38 テスト |

Wasm は `createWasmCoreApi()` を使う。SQLDelight の非同期 web worker driver は使わず、
ユーザーデータの JSON を localStorage に保存する。数百 KB のデータなので容量内に収まる。
SQLite による保存が必要になったら、`CoreRepository` の実装を差し替える。

iOS の Kotlin コンパイルは Mac（Intel / macOS 15.7.9、Temurin 21）で確認済み。
フレームワークのリンクには Xcode の iOS SDK が必要で、Command Line Tools だけでは
`xcrun` が失敗する。Xcode を入れるまでリンクと実機実行はできない。
