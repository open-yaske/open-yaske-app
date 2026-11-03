# 本家の画面構成と M3E への翻訳

REA と apktool で、Compose の関数名・文字列・シート/ダイアログを抽出した結果。
難読化後も `HomeScreen`、`TodoScreen`、`CalendarScreen` などの関数名は残っている。

## トップレベル

下部ナビは `zn2` の列挙で定義されている。

| 順 | ルート | ラベル | 本家の役割 |
| --- | --- | --- | --- |
| 1 | `home` | ホーム | 今日の授業、天気、便利ツール、学内リンク |
| 2 | `todo` | ToDo | 課題、買い物、リスト、ゴミ箱 |
| 3 | `timetable` | 時間割 | 学期、コマ、時間割外、出席 |
| 4 | `calendar` | カレンダー | 月/リスト、学事、祝日、端末カレンダー |
| 5 | `search` | さがす | サークル、企業、求人、周辺、ニュース |

設定は下部ナビに含まれず、ホームや各画面の導線から開く。

## 画面・コンポーネント一覧

| 機能 | クラス | 主なコンポーネント |
| --- | --- | --- |
| ナビゲーション | `e53`, `k53` | `NexusNavHost`, `NexusBottomTabBar` |
| ホーム | `yt1` | `HomeHeaderBar`, `WeatherCard`, `TodayTimetableHomeCard`, `ToolsSection`, `LinkSection`, `AdventCalendarLinkSection`, `AppGridSection`, `ActionButtonsSection` |
| 今日の時間割 | `x55` | `TodayTimetableHomeCard`、前/次の曜日 |
| ToDo | `f75` | `TodoScreen`, `TodoHeader`, `ListTabStrip`, `TodoRow`, `TodoSectionHeader`, `TodoActionRow`, `TodoFormSheet`, `TrashHeader`, `TextInputDialog` |
| 時間割 | `b55`, `q45`, `i45`, `i35` | `TimetableScreen`, `TimetableMainGrid`, `TimetableDayColumn`, `PeriodTimeColumn`, `SummaryCard`, `TimetableAddCoursesSection`, `ExtraCourseSection`, `PeriodSixSevenToggleHeader`, `TimetableCellCard` |
| 時間割の編集 | `i45` | `CellEditSheetContent`, `AttendanceSheetContent`, `ExtraCourseEditSheetContent`, `AcademicYearPickerContent`, `SemesterSelector`, `WeekdayPeriodPickerRow`, `PeriodPickerRow`, `ColorPickerRow`, `CourseTypeSelector` |
| 科目詳細 | `i35` | `TimetableHeader`, `CourseDetailMetaRow`, `CourseDetailCourseCodeRow`, `CourseDetailActionRow` |
| カレンダー | `v00`, `d00`, `sy`, `lz` | `CalendarScreen`, `MonthGrid`, `MonthListView`, `DayCell`, `SelectedDaySection`, `WeekdayHeader`, `CalendarHeaderRow`, `CalendarActionRow`, `CalendarSettingsBottomSheet`, `DayDetailBottomSheet`, `YearMonthPickerBottomSheet`, `JumpToDateDialog`, `AddEventBottomSheet` |
| シラバス検索 | `bq4` | `SyllabusSearchScreen`, `SearchBar`, `FilterRow`, `FilterMenuChip`, `CourseRow`, `EmptyState` |
| さがす | `g44` | `SearchScreen`, `SearchCategoryBar`, `CirclesTab`, `CompanyTab`, `JobPostingsTab`, `LocalAreaTab`, `NewsTab`, `JobDetailView`, `JobSummaryRow`, `CircleLinkRow` |
| GPA | `nq1` | `GpaCalculatorScreen`, `GpaHeader`, `GpaSummaryCard`, `CreditGroupCard`, `GradeCounterRow`, `StepperButton` |
| アカウント | `q3` | `AccountScreen`, `SignedInBlock`, `SignedOutBlock`, `SyncStatusCard` |
| オンボーディング | `sm4` | `StudentOnboardingScreen`、学科・学年・対象・性別・バイト頻度 |
| プロフィール編集 | `tm4` | `StudentProfileEditScreen`, `ProfileDropdown` |
| お知らせ | `c73` | `NoticeScreen`, `NoticeCard`, `NoticeTopBar`, `UnreadBadge` |
| レビュー | `vd5`, `de5`, `he5`, `ee5`, `hd5` | 詳細、編集、マイレビュー、ガイドライン、利用規約、Coming Soon |
| 写真→PDF | `bj3` | `PhotoToPDFScreen`, `ImageGrid`, `ThumbnailCell`, `AddCell`, `SettingsBar` |
| 管理 | `jn4`, `w63` | `StudentSubmissionsAdminScreen`, `NoticeAdminScreen`, 集計カード |
| 設定 | `ga4` | `SettingsScreen`, `UserHeaderCard`, `FormCard`, `FormToggleRow`, `FormValueRow`, `FormNavigationRow` |
| 静的画面 | `ko3`, `fs4`, `ty0` | プライバシー、利用規約、開発者メニュー |

## ホームの構成

上から順に次のセクションがある。

- ヘッダー（`HomeHeaderBar`）
- 天気カード
- 今日の時間割カード（`TodayTimetableHomeCard`、前/次の曜日）
- 便利ツール（`ToolsSection`）
  - 写真 → PDF 変換
  - GPA 計算機
  - 落とし物掲示板
  - みんなの授業評価
- 学内リンク（`LinkSection`）をカテゴリ別に折りたたみ
  - 授業関連、学習・授業、キャンパス・施設、進路・キャリア、
    学生生活・サポート、新入生向け、その他資料
- アドベントカレンダー
- アプリグリッド、アクションボタン

リンクには UEC ポータル、学務情報、図書館、ラボガイド、学内マップ、就職支援、
留学、奨学金、学生寮、ITC ポータル、保健管理センター、学生相談室、
学修要覧、授業日数、休業日、学務課 Q&A などが並ぶ。

## ToDo の構成

`TodoScreen` は上部に検索、並び替え、リスト追加、タブストリップを持つ。

- タブ: すべて / 完了 / 未完了 / その他
- セクション: 今日、明日、期限切れ、期限なし、完了、ゴミ箱
- 行: チェック、タイトル、期限、優先度、カテゴリ、授業、通知、サブタスク
- 追加/編集シート（`TodoFormSheet`）
  - タイトル、期限、優先度（低/中/高）、カテゴリ、授業から選択、
    繰り返し、通知、メモ、サブタスク、リスト
  - リスト名の追加・変更は `TextInputDialog`
- ゴミ箱（`TrashHeader`）と完全削除

## 時間割の構成

`TimetableScreen` は学期選択、グリッド、サマリー、追加セクションを持つ。

- ヘッダー: 学期、単位数、時間割共有、全授業の出欠、設定、今日の出欠
- グリッド: 曜日列、時限列、セル、6-7限の表示切替
- サマリー: 合計単位数
- 時間割外科目（`ExtraCourseSection`）
- シラバスから授業を追加（`TimetableAddCoursesSection`）
- 背景画像の位置・拡大（`AdjustableTimetableBackground`）
- セルをタップしたシート
  - 出席（出席/遅刻/欠席/公欠/休講、出席率、危険判定）
  - セル編集（学期、曜日・時限、授業の種類、連続時限数、単位数、色）
  - 時間割外科目の編集
  - シラバス検索
- 科目詳細
  - 出席率、今日の出欠、授業コード、メタ情報
  - シラバスを開く、レビュー、全授業の出欠、編集、共有

## カレンダーの構成

- 月グリッド / リスト表示の切替
- 今日へ移動、日付指定、年月選択
- 曜日ヘッダー
- 日セルに祝日、学事、時間割、ToDo を重ねる
- 選択日のセクション（`SelectedDaySection`）
- 日詳細ボトムシート
- 設定ボトムシート
  - 週の開始（月/日）
  - コンパクト表示
  - 時間割を表示
  - 学事予定を表示
  - ToDo を表示
- 端末カレンダーのソース表示、権限バナー
- 予定追加シート（日時、終日、場所、メモ）

## さがすの構成

`SearchScreen` は「シラバス検索」ではなく、学外情報のカテゴリ検索。

- サークル
- 企業
- 求人
- 周辺店舗
- ニュース

求人は詳細画面、他の求人、協賛募集を持つ。
シラバス検索は時間割から開く `SyllabusSearchScreen` で、学期・曜日・時限・区分で絞り込む。

## 設定の構成

`SettingsScreen` のセクション。

- ユーザー / アカウント / プロフィール編集
- 通知
- 時間割設定
- カレンダー設定
- ホームに天気予報を表示
- ホームに便利ツールを表示
- リンクをアプリ内で開く
- ナビゲーションバーを自動的に隠す
- 投稿にプロフィールを添付
- マイレビュー
- 利用ガイドライン / 授業レビュー利用規約
- バージョン / 利用規約 / プライバシー
- 公式サイト / X アカウント
- 広告解除（手動）/ 広告非表示を購入
- 提出データ管理 / お知らせ管理
- すべてのデータをリセット

## 本家の UX 上の問題

- ホームが縦に長く、天気・時間割・ツール・リンク・アドベントが同居する
- 設定がホーム、時間割、カレンダー、設定画面に分散している
- ナビゲーションバーを自動で隠すため、現在地が分かりにくい
- ToDo の追加シートに項目が多く、初回の入力が重い
- カレンダーが端末カレンダー、時間割、ToDo、祝日、学事を同時に扱い、密度が高い
- 「さがす」が求人・ニュース依存で、オフラインでは空になる
- アカウントと同期が Firebase 前提で、未設定時の導線が分かりにくい
- 時間割の 6-7 限切替、オンデマンド、背景調整が同じ画面に集中している
- 同じ「設定」でも画面ごとに別のボトムシートを開く

## M3E への翻訳

M3E の Nav Bar は 3〜5 ビュー、幅が広い場合は Nav Rail に切り替える。
本家の 5 つを維持し、設定はトップバーの 1 箇所に集約する。

| 本家 | 新しい構成 | M3E の要素 |
| --- | --- | --- |
| ホームの長い縦積み | 次の授業、今日の時間割、今日のアジェンダ、期限が近い課題、ツール | Large Top App Bar、カード、Filled Tonal Button |
| ToDo の多機能画面 | 未完了/完了/すべてのチップ、期限別セクション、追加はボトムシート | Filter Chip、List、FAB、Bottom Sheet |
| 時間割の詰め込み | 学期チップ、週グリッド、時間割外、競合、追加 FAB | Segmented/Filter Chip、Grid、FAB、Bottom Sheet |
| カレンダーの多重表示 | 月グリッド、授業/ToDo/学事のフィルタ、選択日のアジェンダ | Calendar Grid、Filter Chip、Card |
| さがすの外部情報 | シラバス検索と学内リンクに限定 | Search Bar、Filter Chip、List |
| 分散した設定 | 表示・言語・テーマ、時間割、カレンダー、通知、データ、アプリ情報 | List、Switch、Chip、Section |
| GPA 計算機 | ホームのツールから開く GPA 画面 | Summary Card、List |
| お知らせ/レビュー/求人/管理/アカウント | オフライン方針のため持たない | — |
| 写真→PDF | 後続。まずはデータと画面の骨格を優先 | — |

### 採用する M3E の型

- ナビゲーション: 5 つの Nav Bar、720dp 以上で Nav Rail
- アプリバー: ホームは Large Top App Bar、他は Top App Bar
- 主要操作: Extended FAB（科目追加、ToDo 追加）
- 補助操作: Filter Chip、Segmented Button、Switch
- 詳細: Bottom Sheet
- タイポグラフィ: Material 3 の 15 ロールを Noto Sans JP / SC / KR で使う
- 角丸: 6 / 10 / 16 / 24 / 30dp
- 色: 藍色と緑のトーナル。紫のグラデーションや発光は使わない
- モーション: 画面切り替えとシートに短いイージング。過剰なアニメーションはしない

## 現在の実装との差

実装済み:

- 5 つの下部ナビ、Nav Rail、設定、科目詳細
- ホーム、ToDo、時間割、カレンダー、さがす
- GPA 画面
- 日本語 / 英語 / 中国語 / 韓国語
- Noto Sans JP / SC / KR
- シラバス検索、学校アジェンダ、出席、GPA

未実装:

- 写真→PDF
- 学内リンク集
- 端末カレンダー同期、通知、ウィジェット
- 科目追加のシラバス連携（検索から時間割へ）
- 出席の一括管理画面
- QR 共有
- 本家のレビュー・お知らせ・求人・管理画面（方針として持たない）
