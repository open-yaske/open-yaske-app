# UI 監査の検証（2026-11-03）

対象は `docs/ui-audit-2026-11-03.md` の 1〜41。実装は変更していない。ビルドとテストはこの端末で実行し、画面はヘッドレス Chrome で撮った。

## 検証の基準と、最初に伝えるべきこと

検証中、実装セッションは編集を続けていた。15:25 から 16:00 までに `HomeScreen`、`CalendarScreen`、`TasksScreen`、`TaskEditor`、`CourseEditorSheet`（新規）、`App.kt`、`Tokens.kt`、`AppSettings.kt`、`index.html` が変わり、Web の URL はパスからハッシュ（`#/todo`）へ移った。そのため次のように分けて記録する。

- 画面の判定は、15:40:09 に生成された Web 配布物（ソースは 15:35 時点）のスクリーンショットと、その時点のコードを基準にした。
- 16:00:09 に配布物を作り直し、主要 6 画面を撮り直した。ホームの退行、スプラッシュ、設定のアクセントは 16:00 の配布物でも同じだった。
- ビルドは 15:47〜15:52 に 3 回失敗し、15:55 以降は通った。失敗はすべて編集中の状態を拾ったもので、最後は成功している。
- `git` は HEAD `7eaea7e`（メッセージは `2026-11-03`）に未コミット 39 件。直近のコミットに、今回の主要な変更（ホームの作り直し、通知、スプラッシュ、科目編集、アクセント）は入っていない。

## サマリ

| 判定 | 件数 |
| --- | --- |
| 完了 | 25 |
| 部分完了 | 12 |
| 未着手 | 3 |
| 退行 | 1 |
| 合計 | 41 |

ID 単位の内訳:

- 完了: 3, 5, 8, 11, 12, 13, 14, 16, 17, 18, 20, 22, 25, 26, 27, 28, 29, 30, 31, 32, 33, 35, 36, 38, 40
- 部分完了: 1, 2, 4, 9, 10, 15, 19, 21, 23, 34, 39, 41
- 未着手: 7, 24, 37
- 退行: 6

## 退行と部分完了

### 6. ホームのカードがリンクになっていない — 退行

- 根拠: `composeApp/ui/HomeScreen.kt:82-90` は `agenda` から授業だけを `classes` に絞り、`upcoming` は `dueDate > today` で明日以降だけを残す。`AgendaItem.Task` と `AgendaItem.Event` を描く分岐はホームから消えた（`grep "AgendaItem.Task" HomeScreen.kt` は 0 件）。
- 足りない点: 今日ぶんの課題と学事がホームのどこにも出ない。15:40 の配布物でも 16:00 の配布物でも、デモの「レポート提出（今日）」は表示されない。リンク化そのもの（授業カード、今後、学内リンク、検索結果）は進んでいる。

### 1. 起動スプラッシュ — 部分完了

- 根拠: `wasmJsMain/resources/index.html:90-110` はロゴ・地色・不定プログレスを出し、`#boot.done` でノードを消す。Android は `androidMain/res/values/themes.xml`（`Theme.OpenYaske.Splash`）と `MainActivity.kt:19` の `installSplashScreen()`。ただし検出は `root.querySelector('canvas')` のままで、Compose の DOM に `canvas` は無い（実測: `#compose-root > DIV > DIV[shadow]`、`canvas` 0 個）。
- 足りない点: 実測で 9.0 秒、`#boot` が不透明のまま残り、その間は全画面がタップを奪う。8 秒のタイマーだけで消える。ロゴとバーは出るので見た目は直ったが、「読み込みが終わったら消す」が動いていない。

### 2. 通知と PWA — 部分完了

- 根拠: `androidMain/kotlin/org/openyaske/app/notify/ClassReminder.kt` が 15 分ごとの Worker で授業前通知を出し、`MainActivity.kt:34-41` が Android 13 以降の許可を取る。PWA は `wasmJsMain/resources/` に `manifest.webmanifest` も `sw.js` も無い（`index.html` に登録処理なし）。
- 足りない点: 通知は Android だけ。Web と iOS は設定のトグルが何もしないまま。PWA は未着手で、インストールもオフライン再訪もできない。

### 4. ホームの遊び心 — 部分完了

- 根拠: 16:00 の配布物には「今日の時間割」の色カード、3 つの数字タイル、天気枠がある（`HomeScreen.kt:147-189`、`:276-384`）。監査で提案した残り時間リングは 15:25 の作り直しで消えた（`grep "drawArc\|animateFloatAsState" HomeScreen.kt` は 0 件）。
- 足りない点: 画面の主役が「日付 + 天気のデータなし」の面になり、リングがあったときより静かになった。天気は常に「天気のデータなし」で、押せない白い枠だけが残る。

### 9. ホーム横画面の 2 列 — 部分完了

- 根拠: `HomeScreen.kt:108` の `sideBySide = wide && LocalCompactLayout.current`。`LocalCompactLayout` は高さ 480dp 未満だけ（`App.kt:86`）。
- 足りない点: 900×420 では 2 列になる（確認済み）。1280×800 のタブレット横向きは 1 列のままで、右半分が空く。

### 10. TODO の作り込み — 部分完了

- 根拠: `TaskEditor.kt:147-193` に優先度・繰り返し・メモ、`:200-204` に削除。`TaskService.kt` は完了時に次回分を作る。一方で `TasksScreen.kt:171-213` は期限別グループを 1 つの `item` の中の `Column` にまとめ、`stickyHeader` は 0 件になった。
- 足りない点: サブタスク・リスト・ゴミ箱・並び替えは無い。期限別の見出しがスクロールで固定されず、件数が増えると全行を一度に組み立てる。

### 15. 科目必須・毎週・科目選択 — 部分完了

- 根拠: `CourseTask.repeat` を追加し（`Planning.kt:284`）、`TaskEditor.kt:172-185` で毎日・毎週・毎月を選べる。`TaskService.kt` の `createNextOccurrence` が次回を作る。科目は `TaskEditor.kt:97-113` の `DropdownMenu` で、`:208-212` は科目が無いと保存させない。
- 足りない点: 科目なしの TODO はまだ作れない（`CourseTask.courseId` は非 null のまま）。隔週は無い。科目のドロップダウンは全件を出し、500×900 で 20 科目だと画面の上下にはみ出して選びにくい（`/tmp/opencode/todo-edit-date.png` で確認）。

### 19. 時間割の作り込み — 部分完了

- 根拠: 追加は空きコマから、時間割外は一覧、単位数の帯に GPA（`TimetableScreen.kt:227-246`）。出席・休講・補講の操作は無く、`timetable_issues` は 4 言語の文言だけが残る（`grep "view.issues"` は 0 件）。
- 足りない点: セルを長押ししても出席も休講も編集もできない。競合の警告も出ない。

### 21. 学期の登録・編集 — 部分完了

- 根拠: `TimetableScreen.kt:147-155` のメニューに「学期を追加」、`:487-586` の `TermEditorDialog` が年度・学期・開始・終了を取る。既存学期の編集は無い。
- 足りない点: 追加した学期が選択状態にならず、表示は前の学期のまま。開始・終了の初期値は 4/6〜7/20 固定で、後学期を選んでも日付が変わらない。

### 23. 時間割外の科目 — 部分完了

- 根拠: `ScheduleService.kt:45-48` は「どのセルにも入らなかった科目」を返すよう直った。UI は `TimetableScreen.kt:203-224` で、空のときの見出し・説明・非表示理由の 1 行を消した。
- 足りない点: 15:13 にはあった「時間割外の科目」の見出しと「設定で隠している曜日・時限の科目をここに出しています」が無くなった。科目があるときだけ、文脈なしでカードが並ぶ。

### 34. シラバスの絞り込み — 部分完了

- 根拠: `SearchScreen.kt:179-243` に曜日・コマ・課程を追加し、`SyllabusCatalog.search` がそれらを受ける。学年の条件は無く、年度は 2020〜2026 の固定（`:150`）。
- 足りない点: 「学年」は TSV に列が無いため未実装（監査でも L と書いた）。年度の選択肢が固定で、2027 年以降は選べない。

### 39. 科目モーダル — 部分完了

- 根拠: `CourseEditorSheet.kt` を新設し、メモ・URL・シラバス・色・学期・種別・削除を扱う。`CourseDetailSheet.kt:76-90` は `showEditor` が真なら編集シートに差し替える。ただし `showEditor = true` にする箇所が 1 つも無い（`grep` 0 件、`AppIcons.edit` も未使用）。開くのは URL に `edit=course` を付けたときだけ（`:77`）。
- 足りない点: 利用者が科目を編集・削除する入口が無い。あわせて `#/course/first` を開いてもシートは出ず、デバッグ用の導線もハッシュ移行に追随していない。

### 41. 戻る操作 — 部分完了

- 根拠: `App.kt:168-184` はシート・詳細・タブの 3 段。`PlatformNavigation.wasm.kt:7-14` はハッシュ URL を読み、`popstate` を購読する。16:00 の配布物で `#/todo`、`#/timetable` などの遷移は動く。
- 足りない点: ハッシュ移行が 15:55 に終わったばかりで、ブラウザの戻る（`popstate` → 前のタブへ戻る）と、シートを開いたままの戻るを実機で確認できていない。Android 側の 3 段はコード上そろっている。

## 未着手

- 7（電車の運行状況）: ホームに交通の UI は無い。`AppSettings` に `transitStations` などの項目だけが残り、使われていない。
- 24（時間割の背景画像）: 画像の設定も描画も無い。セルは `tintOf` の不透明な重ねのまま。
- 37（GPA シミュレーション）: 画面は GPA と修得単位と科目一覧のみ。`gpa_title` も「GPA計算機」のまま。

## ビルドとテストの実出力

最後に通った実行（16:00 時点のソース、`/data/open-nexus/kmp`）:

```
$ ./gradlew :shared:jvmTest :shared:wasmJsNodeTest :composeApp:assembleDebug --console=plain
> Task :composeApp:packageDebug
> Task :composeApp:assembleDebug
BUILD SUCCESSFUL in 19s
93 actionable tasks: 11 executed, 82 up-to-date
```

```
$ ./gradlew :composeApp:wasmJsBrowserDistribution --console=plain
BUILD SUCCESSFUL in 4m 20s
（配布物: composeApp/build/dist/wasmJs/productionExecutable、16:00:09）
```

APK: `composeApp/build/outputs/apk/debug/composeApp-debug.apk`、46,881,407 バイト、15:49:14（15:55 の実行で置き換わっていない場合はこの時刻）。

テスト結果（`shared/build/test-results`、15:47〜15:48 の実行）:

```
jvmTest:        18 suites, tests=70, failures=0, errors=0, skipped=0
wasmJsNodeTest: 15 suites, tests=48, failures=0, errors=0, skipped=0
```

途中で失敗した実行（記録として残す）:

```
15:47  e: App.kt:91 Unresolved reference 'initialWebQuery'
15:48  Execution failed for task ':composeApp:packageDebug'
       java.nio.file.NoSuchFileException: .../mergeProjectDexDebug/1/classes.dex
       （同時に走っていた別のビルドと中間物が衝突。再実行で解消）
15:52  e: Tokens.kt Unresolved reference 'Math'
       e: PlatformNavigation.wasm.kt:8,10,14 js(code) の使い方
       e: App.kt:76 / SettingsScreen.kt:114 Unresolved reference 'accent'
```

いずれも編集中の状態を拾ったもので、15:55 以降は解消している。ただし検証中に 3 回ツリーが壊れたこと自体は、統合の進め方の問題として残る。

## 撮ったスクリーンショット

ヘッドレスは Chrome 156（`/usr/bin/google-chrome --headless=new`）を使った。この端末の Obscura は WebGL2 を持たず、`index.html` の WebGL2 判定でフォールバック画面になるため使えなかった。

16:00 の配布物で撮り直したもの:

- `/tmp/opencode/final2/500x900-home.png`
- `/tmp/opencode/final2/500x900-todo.png`
- `/tmp/opencode/final2/500x900-timetable.png`
- `/tmp/opencode/final2/500x900-calendar.png`
- `/tmp/opencode/final2/500x900-settings.png`
- `/tmp/opencode/final2/500x900-more.png`
- `/tmp/opencode/final2/1280x800-home.png`
- `/tmp/opencode/final2/1280x800-calendar.png`
- `/tmp/opencode/final2/900x420-home-landscape.png`
- `/tmp/opencode/final2/boot-2000.png`（起動 2 秒時点）
- `/tmp/opencode/final2/course-sheet.png`（`#/course/first`。シートは出ない）

15:40 の配布物で撮ったもの（16:00 で撮り直していない画面）:

- `/tmp/opencode/final/500x900-gpa.png`
- `/tmp/opencode/final/500x900-pdf.png`
- `/tmp/opencode/final/500x900-home-scrolled.png`（便利ツールとリンクの下側）
- `/tmp/opencode/final/900x1200-home.png`、`900x1200-timetable.png`、`900x1200-calendar.png`
- `/tmp/opencode/final/1280x800-todo.png`、`1280x800-timetable.png`

操作の確認用:

- `/tmp/opencode/todo-menu.png`（行の ⋯ から編集・削除）
- `/tmp/opencode/todo-datepicker3.png`（期限の日付ダイアログ）
- `/tmp/opencode/todo-edit-date.png`（科目ドロップダウンの画面はみ出し）

## 統合前に直すべき 5 件

1. スプラッシュの 9 秒待ちを直す。`#boot` の検出を `root.querySelector('canvas')` から、Compose のルート要素（`#compose-root` の子）か最初のフレーム通知に変える。実測 9.0 秒、その間は全面がタップを奪う。
2. ホームの「今日」に課題と学事を戻す。`classes` だけでなく `AgendaItem.Task` と `AgendaItem.Event` を出し、今日ぶんがどこにも出ない状態を解消する。退行はここ 1 点。
3. 変更を確定させる。未コミット 39 件のまま、検証中も編集が続き、15:47〜15:52 には 3 回ビルドが壊れた。HEAD `7eaea7e` には今回の中身が入っていない。あわせて `?edit=course`、`?edit=task`、`/course/first` のデバッグ導線を本番コードから外すか、開発ビルドだけに閉じる。
4. 科目編集の入口を作る。`CourseEditorSheet` はあるが `showEditor = true` が無く、URL からしか開けない。詳細シートの右上に編集メニューを置く。
5. 科目選択のドロップダウンに高さの上限と検索を入れる。20 科目で画面外まで伸びる。あわせて設定のアクセントのプリセットが暗く似た色に見えるので、彩度と明度を振って見分けられるようにする（16:00 の設定画面）。
