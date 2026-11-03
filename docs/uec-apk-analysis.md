# UEC 向け非公式アプリ APK 解析メモ

2026-11-02 に REA（jadx-headless 0.7.1）で静的解析した結果。対象は SHA-256 `0eaaf5ffffdfa15d01820db5341ce96b0e3ff0dba2b7f5ab6190c8141c13efd5` の APK。

## 対象の正体

| 項目 | 値 |
| --- | --- |
| ファイル | `uec-universal.apk`（リポジトリ外に退避） |
| SHA-256 | `0eaaf5ffffdfa15d01820db5341ce96b0e3ff0dba2b7f5ab6190c8141c13efd5` |
| 種別 | 第三者が配布する非公式アプリ |
| バージョン | 1.0.0 (versionCode 4) |
| minSdk / targetSdk | 32 / 36 |
| 実装 | Kotlin ネイティブ（Capacitor ではない） |
| UI | Jetpack Compose Material3 + Glance ウィジェット |
| データ | Room（8 テーブル）+ Firebase Auth / Firestore |
| その他 | AdMob、Play Billing、カレンダー読み書き、通知 |

APK 内のクラスは 18,179。アプリ本体は `com.example.*` 配下にドメインモデルとウィジェットが残り、
画面とリポジトリ層は R8 で難読化されている（`ru`, `xd0`, `s43` など）。そのため UI の不具合は
ウィジェットのロジックと文字列リソースから確認できる範囲に限られる。

同梱の `assets/syllabus/syllabus_2020.tsv` 〜 `2026.tsv` は UEC シラバスの TSV。1 行目は `//昼間` のコメントで、
本体はタブ 8 列（学期、学期、曜日時限、科目コード、科目名、担当、摘要、備考）。

## Room スキーマ

Room の生成クラス `…LocalDatabase_Impl` の `createAllTables` から復元した。

| テーブル | 主キー | 主な列 |
| --- | --- | --- |
| `todo_items` | `id` | title, dueAtMillis, isCompleted, priority, notificationTiming, listId, timetableCellId, category, details, repeatPattern, subtasksJson, colorHex, weekday, period, courseName, location, instructor, sortOrder, trashedAtMillis, parentTodoId, createdAtMillis, updatedAtMillis |
| `todo_lists` | `id` | name, systemKey, sortOrder, createdAtMillis |
| `timetable_cells` | `id` | title, weekday, period, location, instructor, colorHex, credits, url, courseType, periodCount, courseCode, note, academicYearId, createdAtMillis, updatedAtMillis |
| `extra_courses` | `id` | title, location, instructor, colorHex, credits, url, courseType, courseCode, note, academicYearId, createdAtMillis, updatedAtMillis |
| `academic_years` | `instanceId` | year, semester, grade |
| `attendance_records` | `id` | timetableCellId, status, dateMillis, recordedAtMillis |
| `cancelled_classes` | `id` | timetableCellId, dateMillis |
| `gpa_entries` | `id` | courseTitle, credits, grade, academicYearId |

インデックスと外部キーは 1 つも定義されていない。`onValidateSchema` の `TableInfo` も
indices / foreignKeys が空であることを示している。

## データ構造の欠陥

1. 休講が 2 系統ある。`attendance_records.status` は PRESENT / ABSENT / LATE / EXCUSED の 4 値しか持たず、
   休講は `cancelled_classes` に別行として保存する。一方 `AttendanceStatistics` は `cancelledCount` を持つため、
   集計側で 2 テーブルを突き合わせる必要がある。片方だけ更新すると休講回数がずれる。
2. 出席記録に一意制約がない。`(timetableCellId, dateMillis)` の UNIQUE インデックスがないので、
   同じ授業・同じ日の記録を複数作れる。集計は重複を除かない。
3. 日付を epoch millis で持つ。`dateMillis` は時刻を含む Long で、端末のタイムゾーンや夏時間で
   同じ日が前日にずれ得る。ウィジェット側は `jstCalendar()` で JST 固定にして回避しているが、
   保存形式そのものが日付を表していない。
4. 外部キーがない。`timetable_cells` を削除しても、その `timetableCellId` を参照する出席記録・休講・TODO が残る。
5. `academic_years` の主キーが `instanceId` で、`(year, semester)` の一意性がない。同じ学期を複数行作れる。
6. `todo_items.subtasksJson` はスキーマを持たない JSON 文字列。`parentTodoId` と合わせて親子関係が二重表現になっている。
7. `timetable_cells.periodCount` に CHECK がない。`period + periodCount - 1` が 7 を超えるコマを作れる。
   `credits` は nullable なのに GPA 側は Int を前提にしている。列挙値は TEXT で CHECK がない。
8. TODO が授業情報（weekday, period, courseName, location, instructor）を重複して持つ。
   時間割を編集しても既存 TODO の値は追随しない。
9. `AttendanceStatistics.attendanceRate` は `(present + late + excused) / totalClasses`。
   遅刻を満員出席として数えており、Svelte 版の 0.5 回換算と一致しない。
10. 同期用の削除マーカーがない。TODO の `trashedAtMillis` はあるが、他テーブルは物理削除のみ。

## UI 側で確認できた不具合

難読化された画面本体は読めないため、読めたウィジェットと通知のコードに限る。

- `WidgetTimetableSupport.currentPeriod` は最初に一致した時限を返す。時限が重なった場合の優先順位がない。
- 同じクラスの `daysBetweenStartOfDays` は「JST の 0 時同士の差 / 86,400,000」で日数を出す。
  JST 固定なので実害は出にくいが、日付計算をタイムゾーンに依存させている。
- `sanitizeMaxPeriod` は 1〜7 に丸めるだけで、範囲外のコマを黙って捨てる。ユーザーには理由が出ない。
- `nextPeriodBoundaryMillis` は次の境界が無いとき「翌日 00:00:01」を返す。境界の定義が 1 秒ずれている。

## 深掘りで追加確認した不具合

通知とウィジェットは難読化されておらず、実際のコードを読めた。

### ウィジェットの再更新がほぼ毎分走る

`WidgetRefreshWorker.Companion.scheduleNext` は次の境界までの待ち時間を計算し、
`delay < ExponentialBackoff.DEFAULT_BACKOFF_MAX_DELAY_MS`（WorkManager の 5 時間）なら
待ち時間を 60,000ms に置き換える。

```java
long delay = nextPeriodBoundaryMillis(0) - System.currentTimeMillis();
if (delay < ExponentialBackoff.DEFAULT_BACKOFF_MAX_DELAY_MS) {
    delay = 60000;
}
```

授業のある時間帯は次の境界まで 5 時間未満なので、ウィジェット更新ワーカーが 1 分ごとに再登録される。
意図は「境界が過去なら 1 分後」のはずで、比較対象が 5 時間なのが誤り。バッテリーと通信を消費する。

### 通知の ID と PendingIntent が String.hashCode() 由来

`AttendanceReminderWorker` は通知 ID に `"attendance_" + cellId` の `hashCode()` を、
`TodoReminderWorker` は `todoId.hashCode()` を使う。PendingIntent の requestCode も同じ。

`String.hashCode()` は 32bit で衝突し得る（"Aa" と "BB" が同じ値になることで知られる）。
別の授業・別の TODO が同じ通知 ID になると、後から出した通知が前の通知を上書きする。
requestCode が衝突すると、通知をタップしたときに別の授業が開く。

### 出席通知の記録日時が「現在時刻」

`NotificationActionReceiver` は出席ボタンの処理で `AttendanceRecord` を既定値付きで生成する。

```java
new AttendanceRecord(null, cellId, status, 0L, 0L, 25, null)
```

既定値は `id = UUID.randomUUID()`、`dateMillis = System.currentTimeMillis()`、
`recordedAtMillis = System.currentTimeMillis()`。つまり「その授業の日」ではなく
「ボタンを押した瞬間」が保存される。時刻部分を含むため、同じ日に手動で入力した記録とは
`dateMillis` が一致しない。一意制約がないことも重なり、同じ日の出席が二重に数えられる余地がある。

### 通知アクションの失敗が握り潰される

`NotificationActionReceiver` のコルーチンは `Throwable` を catch して `pendingResult.finish()` だけ実行する。
DB 書き込みが失敗してもログも通知も出ないため、ユーザーには「押したのに記録されない」と見える。

### ToDo のスヌーズで通知が消えない

スヌーズは `todo_reminder_<id>` の WorkManager ジョブを 1 時間後に置き換えるが、
表示中の通知を `NotificationManager.cancel` していない。通知は画面に残り続ける。

### 通知時刻が固定値で、端末タイムゾーンに従う

通知スケジューラの `computeInitialDelayForWeeklyTrigger` は時限ごとの開始時刻を
9:00 / 10:40 / 13:00 / 14:40 / 16:15 / 17:50 / 19:30 と直書きし、`LocalDateTime.now()` の
システムタイムゾーンで計算する。ウィジェットは JST 固定なので、国内向けの想定と実装が揃っていない。
7 限より後はすべて 19:30 になる。
また、現在時刻が開始時刻と完全に一致すると `isAfter` が false になり、翌週まで送らない。

### シラバス TSV の複数コマが 1 コマに潰れる

`sp4.b` のパーサは曜日時限列（例 `月1, 月2`）から最初の曜日文字と最初の数字だけを取り出す。
同梱 TSV を数えると、2026 年度で 131 行、2025 年度で 132 行が複数コマ表記だった。
「基礎電磁気学および演習」のような 2 コマ科目が 1 コマとして登録され、
時間割の表示と出席の分母がずれる。

同じパーサは `他`（集中講義など）を曜日なしとして 1 件残すが、`period` と `weekday` は null になる。
画面側が null を許容していないと、その科目を選んだときに落ちる。

### 時間割セルの終了時限が検証されない

`WidgetTimetableSupport.normalizeTimetableCells` は曜日と開始時限を 1〜7 で検証するが、
`period + periodCount - 1` の終了時限は検証しない。7 限に 3 コマを置くと 9 限まで占有した扱いになる。
ウィジェット側は `sanitizeMaxPeriod` で 7 に丸めるため、画面と実際のコマ数が食い違う。

### 旧 SharedPreferences から Room への取り込み

`h90.a/b/c` は SharedPreferences の JSON 文字列を読み、前回取り込んだ文字列と違えば
全件を Room に流し込む。判定が文字列の完全一致なので、並び順や空白だけが変わった JSON でも
再取り込みされる。取り込み処理はアプリ起動時に 8 本のコルーチンとして並列に走り、
完了を待たずに画面が表示される。


## 権限・プライバシー

INTERNET のほか、AD_ID / ACCESS_ADSERVICES_* / BILLING / READ_CALENDAR / WRITE_CALENDAR /
POST_NOTIFICATIONS / FOREGROUND_SERVICE を要求する。Firebase と AdMob が入っており、
「UEC の非公式アプリ」としては取得範囲が広い。署名は未検証（REA の指摘どおり、静的解析では真正性を判断できない）。

## 証跡

- `inspect_android_package` evidence: `ev_9d90d3ab255975a9eaf54079eaa8ac9c49136cb29d105785af60faf0c00010b6`
- 親解析: `ev_38f0fcf4af0d068d45796a2af30b5facd6daf190ba3345566a129ff4c0e7be7b`
- スキーマ: `s43.createAllTables` / `s43.onValidateSchema`
- ウィジェット: `WidgetTimetableSupport` の各メソッド
