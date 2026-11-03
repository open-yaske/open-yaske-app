# 画面構成

本家の全画面・シート・設定の抽出結果は `docs/original-ui-inventory.md` にある。
配色・余白・部品の仕様は `docs/visual-spec.md` にまとめている。

## 本家から抜いた構成

本家の下部ナビは 5 つだった。

- ホーム
- TODO
- 時間割
- カレンダー
- さがす

設定は下部ナビではなく、ホームや画面右上の導線から開く。
ホームには次の授業、天気、時間割、便利ツール、学内リンク集があった。
設定にはプロフィール、通知、時間割設定、カレンダー設定、表示設定、データ管理、
アプリ情報、レビュー、管理メニューがあった。

本家の Firebase 系（お知らせ、レビュー、求人、提出、落とし物）と広告・課金は、
OpenYASKE のオフライン方針では持たない。

## 新しい構成

下部ナビは本家と同じ 5 つにそろえる。設定は右上の歯車から開く。

| 画面 | 内容 |
| --- | --- |
| ホーム | 次の授業、今日の学校アジェンダ、期限が近い課題、クイック操作 |
| TODO | 科目に紐づく課題。未完了 / 完了 / すべてで絞り込み、追加・編集・削除。検索はタブの下 |
| 時間割 | 学期チップ（追加可）、週グリッド、時間割外科目、競合、単位数と暫定 GPA、科目追加 |
| カレンダー | 月グリッド。学校の授業・学事・課題だけを表示し、日別アジェンダを下に出す |
| さがす | シラバスの科目名・コード・教員検索。年度・学期・曜日・コマ・課程で絞り込み |
| 設定 | 表示（テーマ・言語・アクセント）、成績、時間割、カレンダー、通知、データ、アプリ情報 |
| 科目詳細 | 時間割やアジェンダから開くボトムシート。授業回、課題、出席率、成績 |

## M3E に合わせた点

- 下部ナビは 3〜5 ビュー。5 つなので M3E の Nav Bar の範囲内
- 幅 720dp 以上では Navigation Rail に切り替える
- アプリバーは 2 種だけ。タブ画面は 56dp（横 44dp）の `TabHeader`、下位画面は `AppTopBar`
- 主要操作は `MaterialShapes.Cookie9Sided` の FAB（TODO 追加）と、一覧末尾の追加行
- タイポグラフィは 6 ロール（titleLarge / titleMedium / bodyLarge / bodyMedium / bodySmall / labelSmall）
- 角丸は control 10 / card 16 / sheet 28 / pill 50% の 4 段（`AppShapes`）
- 色は UEC を意識した藍色が既定。アクセント 1 色から primary と地の色相を導出する
- 画面切り替えはフェード、詳細はボトムシート、戻るはシート → 詳細 → タブの 3 段

## フォント

`Noto Sans JP` / `Noto Sans SC` / `Noto Sans KR`（いずれも OFL）を同梱する。
`AppSettings.locale` が `ja` / `zh` / `ko` のとき、それぞれの FontFamily を
MaterialTheme の全 type scale に適用する。英語は日本語フォントのラテン字形を使う。
可変フォントを複数ウェイトで登録すると本文がフォールバックすることがあったため、
1 ウェイトだけ登録し、太字は合成に任せている。

## デザインの置き場所

配色・寸法・部品は `ui/design/` に集約する。

- `Tokens.kt` — ライト / ダークの色、余白、角丸（`Radius` / `AppShapes`）、科目色、アクセント導出
- `Typography.kt` — 数字を等幅にする `tabular()`
- `Widgets.kt` — カード、見出し、チップ、主ボタン、行
- `AppBars.kt` — `TabHeader` と `AppTopBar`
- `DateFields.kt` — 日付・時刻の入力（DatePicker / TimePicker）
- `ModalHost.kt` — シートを閉じる戻る
- `Navigation.kt` — 下部ナビ、レール
- `strings/` — 日本語・英語・中国語・韓国語の文言
- `ui/timetable/TimetableGrid.kt` — 時間割のグリッド
- `ui/calendar/MonthGrid.kt` — 月グリッドと日セル
- `GradePicker.kt` — 成績の 5 段階入力

画面は Material の既定色を直接使わず、`LocalAppColors` から取る。

## 戻る

Android は BackHandler、Web はブラウザ履歴（`popstate`）とパス、iOS は画面内の戻るボタンを使う。
戻るは 3 段で、シート → 詳細 → タブのルート（消費しない）。
履歴はタブ切替だけ `pushState` し、詳細は `replaceState` にする。
詳細は `docs/architecture-review.md` にある。

## i18n

`UiStrings` に日本語、英語、簡体字中国語、韓国語の辞書を持つ。
`AppSettings.locale` を変えると `LocalUiStrings` が切り替わり、その場で再描画する。
キーは `nav_home`、`home_next_class`、`search_hint` など約 230 個。

CMP 1.11 の `stringResource` は実行時の言語切り替えに内部 API が必要なため、
画面の文言は共通コードの辞書に寄せている。フォントとシラバスのみ Compose リソースを使う。
