# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/).

## [Unreleased] - 2026-11-03

### Added

- **スキーマ v2（学校の情報だけを持つ）**:
  - `Term`、`CourseSession`、`SchoolEvent`、`CourseTask`、`CourseGrade`、`GpaScenario`、`ExternalSyncLink` を追加。
  - 科目は通常・時間割外・集中講義・連携大学・ユーザー定義を `CourseKind` / `CourseSource` で区別。
  - 出席は `sessionId` に紐づけ、補講・振替・集中講義も同じ仕組みで扱う。
- **授業回の生成 (`SessionGenerator`)**: 学期の開始日から週次パターンを展開し、休日・部分休講・休講上書きを適用。
- **学校アジェンダ (`AgendaProjector`)**: 授業回・学事・科目課題を日付順に射影。個人の予定は対象外。
- **GPA の科目連携**: `CourseGrade` の状態（未確定・合否・除外・認定）と `GpaScenario` による what-if 試算。
- **日付計算 (`IsoDate`)**: epoch day、曜日、加算をタイムゾーン非依存で実装。
- **DB 設計書 (`docs/database-schema.md`)**: SQLite の DDL、制約、索引、論理削除、移行、主要クエリ。
- **SQLDelight のスキーマとクエリ**:
  - `kmp/shared/src/commonMain/sqldelight/` に学期・科目・授業回・出席・学事・課題・成績・同期・設定・書類のテーブルを定義。
  - 部分ユニーク索引（出席の授業回 / 日付）、形式 CHECK、外部キー CASCADE、単一行設定を含む。
- **`DatabaseRepository`**: モデルと SQLite の相互変換、insert + update による安全な upsert、論理削除。
- **プラットフォーム別ドライバ**: JVM（インメモリ / ファイル）、Android（`AndroidSqliteDriver`）、iOS（`NativeSqliteDriver`）。
- **DB の制約テスト**: 出席の upsert、CASCADE、CHECK、学期の一意性、設定の単一行、同期リンクの一意性を JVM の SQLite で検証。
- **画面向け `CoreApi`**:
  - `state` / `settings` の `StateFlow` と、時間割・学校アジェンダ・出席・科目詳細・GPA・シラバス検索の射影。
  - 学期・科目・授業回・補講・出席・課題・学事・成績・シナリオ・設定・バックアップの変更 API。
  - 入力検証、ID と時刻の補完、休講への出席拒否、参照切れの検出。
  - `AppSettings` を 1 型に集約し、表示曜日・最大時限・時限時刻・出席ポリシー・通知を持つ。
  - シラバスのコード一致・前方一致・日本語 bigram の検索。
- **画面 API のドキュメント (`docs/core-api.md`)**。
- **保存層の抽象化**: `CoreRepository` を追加し、SQLDelight 実装とインメモリ実装を差し替え可能にした。
- **Wasm 用の保存**: `InMemoryRepository` + localStorage の `createWasmCoreApi()`。SQLDelight の非同期ドライバなしで画面 API が動く。
- **iOS のコンパイル確認**: `iosX64` / `iosArm64` / `iosSimulatorArm64` の Kotlin コンパイルを Mac で確認。フレームワークのリンクは Xcode の iOS SDK が必要。
- **Compose Multiplatform のアプリ (`kmp/composeApp`)**:
  - 本家の下部ナビを再現した 5 画面（ホーム / ToDo / 時間割 / カレンダー / さがす）と、右上から開く設定。
  - 幅 720dp 以上で Navigation Rail、未満で Navigation Bar に切り替える。
  - ホームは次の授業・今日のアジェンダ・期限が近い課題・クイック操作。時間割は学期チップ、週グリッド、時間割外科目、競合表示、科目追加。カレンダーは学校予定だけの月グリッド。さがすはシラバス検索。
  - 科目詳細はボトムシートで、授業回・課題・出席率・成績を表示。
  - Noto Sans JP（OFL）を同梱し、全 type scale に適用。Web でも日本語が豆腐にならない。
  - 日本語 / 英語 / 簡体字中国語 / 韓国語の `UiStrings` を追加。設定の言語切り替えが即時反映される。
  - GPA 画面、時間割・カレンダーの表示設定、ToDo の期限別グループ、カレンダーの授業/ToDo/学事フィルタを追加。
  - Noto Sans JP / SC / KR を同梱し、言語に応じて FontFamily を切り替える。
- **本家 UI の全画面抽出 (`docs/original-ui-inventory.md`)**:
  - NavHost、下部ナビ、Home、ToDo、時間割、カレンダー、さがす、GPA、アカウント、オンボーディング、お知らせ、レビュー、PDF、管理、設定のクラスとコンポーネントを整理。
  - 本家の UX 上の問題と、M3E への翻訳方針を対応表にした。
- **コア設計レビュー (`docs/architecture-review.md`)**:
  - 共有モデルと `CoreRepository` は維持し、`CoreApi` を機能別サービスへ分割する方針を整理。
  - 画面は表示状態と入力だけを持ち、業務ルールは共通コアに置く。
- **戻る挙動**:
  - Compose Multiplatform の `ui-backhandler` を導入。Android は predictive back、Web はブラウザ履歴、iOS は画面内の戻る。
  - Web のパス（`/`, `/todo`, `/timetable`, `/calendar`, `/search`, `/settings`, `/gpa`, `/course/<id>`）とディープリンクを追加。
  - Wasm 配布物を SPA フォールバック付きで配る `kmp/tools/serve-wasm.py` を追加。
- **本家の見た目に合わせた UI の作り直し (`docs/visual-spec.md`)**:
  - APK の色定数とストアの実画面から、配色・余白・角丸・部品の寸法を抽出。
  - 独自のデザイントークン (`ui/design/Tokens.kt`) と部品 (`Widgets.kt`, `Navigation.kt`) を追加。
  - 下部ナビを浮遊ピルに、見出しを色付きの帯に、カードを白 + 1dp 線に統一。
  - ホーム・ToDo・時間割・カレンダー・さがす・設定・GPA・科目詳細を新しい部品で作り直した。
  - 時間割は空セルのタップで科目追加、6-7 限のトグル、合計単位数の帯を持つ。
  - カレンダーは日曜始まりのグリッドに最大 3 本の帯と `+n`、表示設定はボトムシート。
  - さがすは年度・学期の絞り込みとシラバスからの追加、空のときは履修中の科目を出す。
  - 科目詳細で今日の出欠を 4 ボタンから記録できるようにした。
  - Web は `index.html` で言語に合ったフォントを先読みし、起動画面を出すようにした。
  - 画面切替に短いフェードとスライドを入れた。
- **時間割の曜日設定**: 設定で隠した曜日が時間割に出ないよう修正。判定は `TimetableDays.resolve` に切り出し、テストを追加。
- **ヘッダー統一（下位画面）**:
  - 「その他」と PDF のヘッダーも `TabHeader`（56dp）に寄せ、全画面で高さを揃えた。
- **ヘッダー統一と細部の修正**:
  - 時間割のヘッダーも `TabHeader`（56dp）に統一。
  - カレンダーの日パネルを `heightIn(max = 480.dp)` にし、予定が少ない日の空白を解消。
  - 時間割セルの `clip` と `border` の角丸を 14dp に揃えた。
  - タブ切り替えの横スライドをやめ、フェードのみに。
- **共通ヘッダーと 2 ペイン**:
  - ホーム・ToDo・カレンダーのヘッダーを `TabHeader`（`statusBarsPadding` + 高さ 56dp + 左右 16dp）に統一。
  - カレンダーの 2 ペイン閾値を 800dp に下げ、900px の窓でも日パネルが出るようにした。
- **UI レビューの反映（2 回目）**:
  - カレンダーの日セルを「色付きの帯＋文字」から「6dp のドット＋件数」に変更（低コントラストと 7.5sp の潰れを解消）。
  - カレンダーの 2 ペイン閾値を 900 → 820dp に。
  - 時間割に `widthIn(max = 1000.dp)` を入れ、教室ピルを内容幅に。
- **UI レビューの反映（1 回目）**:
  - ToDo の白背景をやめ、他画面と同じ地色に統一。
  - ホーム・ToDo・カレンダーの自前ヘッダーに `statusBarsPadding()` を追加。
  - 下部ナビの選択インジケータを `primaryContainer` に変更（1.15:1 → 約 1.5:1）。
  - カレンダーの帯文字が白飛びしないよう `readableOn` のしきい値を 0.45 に。
  - 文言の重複キーを 4 言語で除去（`settings_version` など 7 件）。
- **M3E の仕上げ（時間割とその他）**:
  - 時間割のセルを 14dp 角丸、部屋ピルをスタジアム型に。今日の列は見出しを色付きにした。
  - 「その他」の PDF 行から重複する説明文を削除し、フッターにアプリアイコンを追加。
- **M3E の仕上げ（アプリバーと一覧）**:
  - 「その他」を `LargeTopAppBar` + スクロールで畳む形にし、行を M3 の `ListItem` に統一。
  - PDF 画面を `MediumTopAppBar` に変更。
- **M3E の仕上げ**:
  - シートと大きな面の角丸を 28dp に広げ、`MaterialExpressiveTheme` の motion と合わせた。
  - PDF の書き出し中に M3E の `LoadingIndicator` を表示。
- **PDF（iOS）**:
  - PHPicker で最大 12 枚を選び、UIImage で縮小・JPEG 化して `PdfBuilder` で PDF にまとめる。
  - 目標容量を超えたら品質を下げて作り直す（最大 2 回）。
  - 一時ファイルに書き出し、`UIActivityViewController` で共有。
  - カメラ撮影は iOS では未対応のまま（ボタンは無効表示）。
- **PDF の自己テスト**:
  - `pdf-self-test.html` を追加。画面を使わずに PDF を組み立て、ヘッダ・EOF・ページ数を確認できる。
- **PDF（Android）**:
  - MainActivity に Photo Picker を登録し、選んだ画像を `Bitmap` で縮小・2 値化して `PdfBuilder` で PDF にまとめる。
  - 目標容量を超えたら品質を下げて作り直す（最大 2 回）。
  - FileProvider 経由で共有。`res/xml/file_paths.xml` と provider を追加。
  - 書き出し中は M3E の `LoadingIndicator` を出す。
- **PDF（Web）**:
  - 写真を選んで 1 つの PDF にまとめる画面を追加。「その他」から開く。
  - 目標容量で 5 つのモード（極限 1MB / 軽量 2MB / 標準 5MB / 高画質 10MB / グレースケール 3MB）を用意。
  - 極限 1MB は 1000px + 2 値化 + 40%。文字は読めるが写真は潰れる。OCR の下ごしらえ向け。
  - 無圧縮は再エンコードしない（JPEG はそのまま）。
  - 「撮影して PDF を作る」を追加。Web は capture 属性、Android は TakePicturePreview。
  - 目標を超えたら品質を下げて作り直す。Web は `media.js` で選択・縮小・PDF 組み立て・保存まで行う。
  - PDF の組み立ては共通コードの `PdfBuilder` も用意済み（Android / iOS で使う）。
- **アプリアイコン**:
  - 旧 Web 版のアイコンを potrace でベクター化し、`favicon.svg` と Android の adaptive icon（前景ベクター + 背景色 + monochrome）を追加。
  - Android の manifest に `android:icon` / `roundIcon` を設定し、既定アイコンから差し替えた。
- **ライセンス**:
  - アプリ本体を MIT とした（`LICENSE`）。
  - 「その他」にオープンソースライセンス画面を追加。同梱物の一覧と OFL 1.1 の全文を表示する。
  - シラバスのデータは大学の著作物である旨を明記した。
- **下部ナビの再構成**:
  - 5 番目を「その他」にし、シラバス検索・GPA・設定をそこへ集約。
  - さがすは独立した画面ではなく、その他から開く下位画面にした（戻るで戻る）。
  - PDF は書き出しの土台（`PdfBuilder`）だけ先に入れ、メニューには実装後に出す。
- **テーマとホームの再構成**:
  - テーマにピュアブラック（有機 EL 向け `#000`）を追加。設定は 4 択。
  - ホームの上部をよく使うリンク集に置き換え、下部ナビと同じ行き先のショートカットを削除。
  - 今日のカードに時刻から選ぶ空の絵（太陽 / 月）を入れた。
  - 設定はカードの外（画面上部の 32dp の行）に置き、カードの操作に見えないようにした。
  - アイコンを `AppIcons` に集約（ToDo の絵柄が 2 種類あったのを統一）。行末の「›」を 18dp のアイコンに統一。
  - ToDo の期限グループを見出し固定に、カレンダーの選択日チップを横スクロールにした。
  - フォントのサブセット生成を画面の直書き文字まで含めるよう修正（韓国語の欠けを解消）。
- **見出しと面の整理（unslop）**:
  - ホーム・ToDo・さがすから画面名の見出しを外した。下部ナビが現在地を示すため。
  - 節の見出しを文字だけにし、色付きの帯・アクセントの棒・下線をやめた。
  - 行ごとの角丸カードをやめ、1 つの面にリストを 1 つ置く形に統一。
  - 面の線を外し、余白と 1dp の区切りで分けるようにした。カード角丸は 12dp。
  - 画面ヘッダーを M3 の `TopAppBar` に寄せ、時間割は学期のピルを題にした。
  - 時間割のグリッドを 16dp の余白・2dp の間隔・48dp の時限列に揃えた。
- **M3E への寄せとフォントの 3 段読み**:
  - 下部ナビを端まで届く `NavigationBar` とピル型インジケータに変更（浮遊ピルをやめた）。
  - 成績の 5 段階を `ButtonGroup` + `ToggleButton` に変更。
  - フォントを UI（330KB）→ シラバス込み（820KB）→ フル（9.2MB）の 3 段階で差し替える方式にした。
  - `CoreApi` の 8 サービスを公開し、新しい画面はサービスを直接受け取れるようにした。
- **Material 3 Expressive とフォントの 2 段読み**:
  - `material3:1.11.0-alpha07` を明示し、`MaterialExpressiveTheme` と `MotionScheme.standard()` を導入。
  - 追加ボタンを `MaterialShapes.Cookie9Sided` の形にした。
  - UI 用サブセットフォント（約 330KB）を追加し、フルフォントの取得完了後に差し替える方式にした。
  - `kmp/tools/subset_fonts.py` でサブセットを再生成できる。
- **CoreApi の分割**:
  - `core/api/services/` に Course / Attendance / Task / Grade / Schedule / Syllabus / Settings / Backup を追加。
  - `CoreApi` は 609 行から 259 行になり、公開 API は互換のまま委譲する。
- **成績表記の切り替え**:
  - `GradeCatalog` を追加し、SABCD / 秀優良可不可 / 4.0〜0.0 を同じ 5 段階として相互変換。
  - 設定に「成績の表記」を追加。GPA 画面と科目詳細は選んだ表記で表示・入力する。
  - 入力はどの表記でも受け付ける（「秀」「S」「4.0」は同じ扱い）。
- **学内リンク**:
  - 本家 APK の URL をそのまま使い、授業・学習 / キャンパス / 生活・進路の 3 グループをホームに追加。
  - タップは `LocalUriHandler` で OS のブラウザを開く。
- **広い画面と横画面**:
  - カレンダーは幅 900dp 以上で月グリッドと日パネルを並べる。
  - 高さ 560dp 未満ではヘッダーと日セルを詰める。
  - ホーム・設定・ToDo・さがす・GPA は読みやすい幅で中央に寄せ、ホームのタイルは 3 列 / 6 列を切り替える。
- **構造の整理**:
  - `UiStrings` を言語ごとのファイルに分割。
  - 時間割のグリッドとカレンダーの月グリッドを別ファイルに分離。
  - GPA の集計を `GpaProjector` に移し、`CoreApi.gpa()` は組み立てだけにした。
- **テーマの補正**:
  - Material 3 の surfaceContainer 系の色を明示し、カードが既定の薄紫にならないようにした。
  - 言語チップのラベルを、その言語のフォントで描画するようにした（ハングルの豆腐を解消）。
  - Android は APK、Wasm はブラウザ配布物を生成。Chrome で描画を確認。
  - iOS は `iosArm64` / `iosSimulatorArm64` の Kotlin コンパイルを Mac で確認。Compose 1.11 は Intel シミュレータ `iosX64` 非対応。
- **シラバスデータのパイプライン (`kmp/syllabusTool`)**:
  - 2020〜2026 年の TSV を一次ソースとして同梱。
  - 文字列辞書と列指向の CBOR に変換。1.07MB の TSV を 617KB に圧縮し、11,921 コマを収録。
  - 2026 年は前学期 906、後学期 803 コマ。半角カタカナの学期表記も正規化。
  - マニフェストに件数・バイト数・SHA-256・年度を記録。
  - アプリは同梱 CBOR を起動時に読み、シラバス検索に使う。
- **iOS の CI ワークフロー**: `.github/workflows/ios-core.yml` で macOS ランナーの Xcode を使い、共有コアと Compose アプリのシミュレータ用・実機用フレームワークをリンクする。
- **iOS ビルドと匿名性のメモ (`docs/ios-build.md`)**: Xcode の CLI 導入方法、匿名で続けられる範囲、配布時の制約。
- Gradle の設定キャッシュを有効化。変更なしの `jvmTest` が約 18 秒から約 1 秒になった。
- テストを拡張（JVM 62 件、Wasm 40 件が合格）。

### Changed

- `Course.weekday` / `period` を nullable にし、週次パターンを持たない科目を表現できるようにした。
- `AttendanceRecord` に `sessionId` を追加。
- `UserData` を schemaVersion 2 に上げ、学期・授業回・学事・課題・成績・同期リンクを追加。
- `UserDataCodec` は日付・時刻・状態を検証し、科目を消した孤児の授業回・課題・出席を落とす。
- 端末同期用の墓標 `deletedAtMillis` を科目・授業回・学事・出席に追加。
- **角丸・面・塗りのトークン化（UI 監査 F3）**: `Radius` を control 10 / card 16 / sheet 28 / pill 50% の 4 段に絞り、`AppShapes` を唯一の Shape の出どころにした。`MaterialTheme.shapes` も同じ 4 段に揃え、画面の `RoundedCornerShape(n.dp)` 直値をなくした。`AppCard` は既定で 1dp の線を持つ面にし、科目色の重ねは `tintOf`（明 16% / 暗 24%）だけにした。主ボタンはピルから control の角丸に変更。
- **タイポグラフィのロール化（UI 監査 F2）**: 画面の `fontSize` 直書きを `MaterialTheme.typography` の 6 ロール（titleLarge 20 / titleMedium 16 / bodyLarge 15 / bodyMedium 14 / bodySmall 12 / labelSmall 11）に置き換えた。太字は `SemiBold` までにし、時刻・単位数・GPA・日付は `tabular()` で等幅にした。`design/Typography.kt` を追加。
- **アプリバーと画面骨格の統一（UI 監査 F1）**: アプリバーを `AppTopBar`（下位画面）と `TabHeader`（タブ画面）の 2 種に絞った。`TabHeader` は高さ 56dp（横 44dp）、左 40dp スロット、中央は幅に対して幾何学的に中央、右は 40dp の倍数に揃えた。カレンダーの独自ヘッダー、More・PDF の混在を解消し、未使用の `ScreenHeader` / `AppTopBarContent` を削除した。ホームの設定アイコンは削除し、「その他」と時間割の歯車に集約した（項目 5）。
- **余白・密度・横画面（UI 監査 F5）**: `LocalCompactLayout`（高さ 480dp 未満）を追加し、ヘッダー 44dp・下部余白 72dp・下部ナビ 48dp（ラベル無し）へまとめて切り替えるようにした。画面下の余白は 96dp（ナビ 80 + 16）に統一し、画面ごとの 88/110/150dp を `screenBottomPadding` に寄せた。
- **遷移と戻るの階層（UI 監査 F4）**: 戻るを 3 段（シート → 詳細 → タブのルートでは消費しない）に整理し、履歴はタブ切替だけ `pushState`、詳細は `replaceState` にした。Web は `popstate` を購読して URL から画面を復元する。下位画面の切替はフェードで繋ぎ、シート用の `ModalBackHandler` を追加した。
- **TODO の表記と導線（UI 監査 11・14・17・18）**: 4 言語の表記を `TODO` に統一した。件数は全タブに出す（0 のときは出さない）。検索欄をタブの下へ移し、白面 + 1dp 線 + 44dp にして開いたら自動フォーカスする。一覧の末尾に「TODO を追加」の行を置いた。
- **時間割の読みやすさ（UI 監査 25・26・28・29）**: 左列の時刻とコマ番号を縦中央に揃え、現在コマのドットが高さを変えないようにした。曜日見出しから今日の日付を外し、`clip` と `border` を同じ角丸に揃えた。教室名はセル下端の全幅バーにして中央寄せにした。合計単位数の帯に暫定 GPA と GPA 画面への導線を足した。
- **ホームの導線（UI 監査 5・6・8）**: ホームの設定アイコンを削除し、上部は `OpenYASKE` の見出しだけにした。GPA 行を削除して時間割の単位数バーへ移した。ヒーローの次の授業と今日の課題・学事の行を押せるようにした。
- **カレンダーと PDF（UI 監査 31・36・40）**: 重複していたカレンダーの FAB を削除し、日詳細のボタンを「この日に予定を追加」にした。PDF は説明文を削除して「圧縮」の見出しにし、モードを目標容量の昇順（1MB / 2MB / 3MB / 5MB / 10MB / 元のまま）に並べ替えた。科目シートの内部キー `course_attendance_today` と授業回種別の enum 名を 4 言語の文言に置き換えた。
- **時間割の追加経路と学期（UI 監査 21・22・27）**: 空きコマの中央に薄い＋を置き、押したコマの曜日・時限は固定表示にして二度入力させない。右上の＋を削除し、下部の行を「時間割外の科目を追加」に変えた（`CourseKind.EXTRA` で週次パターンを持たない科目を作る）。学期メニューの末尾に「学期を追加」を置き、年度・前後期/通年・開始終了日から `api.saveTerm` する画面を追加した。時間割セルの科目名に `LineBreak.Paragraph` を適用した。
- **カレンダーの帯と設定導線（UI 監査 30・32）**: 日セルのドットを、予定名が入る高さ 16dp の帯（最大 3 本 + `+n`）に戻し、携帯でも `availableHeight` から行の高さを決めるようにした。歯車は全体設定の「カレンダー」節へ移動し、表示の ON/OFF は日詳細シートから開くようにした。
- **TODO の編集・削除と締切入力（UI 監査 12・16）**: 行タップで `TaskEditorSheet` を開き、題・科目（ドロップダウン）・期限（`DateField` + `TimeField`）を編集できるようにした。行末のメニューから編集と削除ができ、削除は確認ダイアログを挟む。`design/DateFields.kt` を追加。
- **ホームの横画面と遊び心（UI 監査 4・9）**: 横画面（高さ 480dp 未満）では「今日」と「今後」を 2 列に並べた。ヒーローは 1 日の授業の進み具合をリングで示し、授業中は面の色が `primaryContainer` から `primary` に変わる。日付は `headlineLarge` で大きく置いた。
- **シラバス検索の絞り込みと重さ（UI 監査 34・35）**: 曜日・コマ・課程（昼間/夜間/修士/博士）で絞れるようにし、検索は 200ms のデバウンスに変えた。`SyllabusCatalog` に bigram の転置索引を作って候補を絞り、科目コードも索引に入れた。1 文字は科目コードの一致だけを見る。学期は表記ゆれを正規化して比較する。
- **時間割外の科目（UI 監査 23）**: `ScheduleService` の「時間割外」を、どのセルにも入らなかった科目に変えた。設定で隠した曜日・時限の科目もここに出るので、合計単位数だけが増えて見えなくなることがない。グリッド下に節を常設し、追加行と理由の 1 行を添えた。
- **起動画面（UI 監査 1）**: Web の `#boot` にロゴと 2dp の不定プログレスを置き、フェード後にノードごと削除する。Compose 側はシラバスの読み込みが終わるまで、今日のカードと同じ 28dp 角丸のスケルトンを出す。
- **TODO 一覧の面をそろえる（UI 監査 13）**: 期限別のまとまりをそれぞれ `AppCard` に畳み、行の白を面の上に載せた。一覧が画面端まで白く伸びる状態をやめ、区切りはカード内の 1dp の線に統一した。
- **検索結果の行から開く（UI 監査 6）**: シラバスの結果行を押すと `SyllabusSheet` が開き、科目コード・教員・曜日・コマ・教室・学期・課程を確認してから履修に追加できるようにした。
- **Android の起動画面（UI 監査 1）**: `androidx.core:core-splashscreen` を入れ、`Theme.SplashScreen` に地色とランチャーアイコンを設定した。`installSplashScreen()` で最初のフレームまで表示し、スケルトン（Compose 側）へ渡す。
- **授業前のローカル通知（UI 監査 2・判断はローカルのみ）**: `WorkManager` の 15 分間隔 Worker が端末内の DB を見て、開始が近い授業を `NotificationManagerCompat` で通知する。同じ授業回は 1 回だけ、`quietDuringClass` が有効なら授業中は出さない。Android 13 以降は `POST_NOTIFICATIONS` を起動時に求める。サーバも Push も使わない。
- **ホームのダッシュボード化**: 日付と天気（取得元が決まるまで「準備中」の 1 行）、今日の時間割の横スクロール、今日の課題・学事、3 つの数字タイル、学内リンク、便利ツールの順に組み直した。横画面と幅 900dp 以上では「今日」と「今後」を 2 列にする。天気は推測値を出さない。
- **カレンダーの月セレクタ**: ヘッダーを `TabHeader` にし、月のピルから `DatePickerDialog` で日付へジャンプする。前月・翌月の矢印を廃止し、「今日」のテキストボタンと全体設定への歯車を右に置いた。
- **TODO と科目の編集**: TODO は行タップで `TaskEditorSheet`（題・科目・期限・優先度・繰り返し・メモ・削除）。科目は鉛筆から `CourseEditorSheet`（名前・教室・教員・コード・単位数・色・学期・種別・メモ・URL・シラバス・登録解除）。科目選択は検索付きのシートに変えた。
- **繰り返しの保存**: `CourseTask.repeat` を追加し、SQLDelight を schema 2 に上げた（`1.sqm` で `repeat_pattern` を追加）。完了にすると次回分を未完了で作る。
- **ハッシュルーティング**: Web の URL を `#/timetable` 形式にし、`?demo=1` などのクエリはハッシュより先に読む。`popstate` で URL から画面を復元する。
- **PWA**: `manifest.webmanifest`、`sw.js`（キャッシュのみ）、192/512 のアイコンを追加し、`index.html` で登録した。起動画面は `#compose-root` の最初の要素で消すようにして、8 秒待ちをやめた。
- **アクセントの色相化**: `accentHue`（0..359）と `accentStyle`（COLOR / MONOCHROME）に整理し、プリセット 9 色と色相スライダーにした。`onPrimary` はコントラスト比から白黒を選び、地と面の色相もアクセントに寄せる。
- **ホームの情報量を戻す**: 3 つの数字タイル（今日 / 今週 / 合計単位）、今日の課題・学事、今後の課題を加えた。天気と運行は取得元が無いので節ごと出さない。ダミーデータ投入の導線を外した。
- **出席の扱い**: 授業回が無い科目（時間割外）は今日の日付で出席を記録する。警告は「欠席 / (出席 + 欠席 + 遅刻×重み + 公欠×重み) が 1/3 以上」に変え、`absenceWarnRatio`（0.1〜0.5、既定 0.333）で設定できる。科目詳細に「欠席 n / 上限 m」を出す。
- **カレンダーの予定・TODO 編集**: 日パネルと日詳細シートの行、選択日のチップから、TODO 編集シートと予定編集シート（タイトル / 日付 / 時刻 / 終日 / 場所 / メモ / 削除）を開けるようにした。予定に `location` を追加（schema 3）。
- **追加と編集の統一**: 科目は `CourseEditorSheet` に寄せ、上部の「シラバスから探す」で科目名・コード・教員・教室・曜日・時限を埋める。予定は `SchoolEventEditorSheet` で新規も編集も行う。TODO は `TaskEditorSheet` のみ。
- **時間割の背景画像**: Web は media.js と IndexedDB、Android は Photo Picker と filesDir、iOS は Documents に保存する。長辺 1600px・JPEG 80% に縮小し、グリッドの背後に `ContentScale.Crop` とスクリムを敷く。セルは 不透明 / すりガラス（55%） / 半透明（30%） / 透明（枠線のみ）の 4 段で、ヘッダーのメニューと設定の両方から切り替える。空きコマは常に枠線＋薄い＋だけ。
- **シラバスから科目を作る**: 追加シートの「シラバスから探す」に文言を付け、コマから開いたときは曜日・時限で絞る（解除可）。選ぶと科目コード・教員・教室・曜日・時限・URL を保存する。結果行に曜日・時限・教員を出す。
- **TODO の「見る」と「直す」**: ホーム・カレンダー・科目詳細からは `TaskDetailSheet`（完了トグル・期限・優先度・繰り返し・メモ・編集）を開く。科目なしの TODO を作れ、紐づけも外せる（`CourseTask.courseId` を NULL 可にし、schema 4 へ移行）。科目詳細の課題行からも詳細を開く。
- **ホームのリンク集**: 学内リンクと学修リンクを 2 列グリッドで戻した。アイコンは色付きの角丸タイル。
- **データの入出力**: 設定のデータ節に JSON の読み込み（貼り付け・上書き確認）と全データ削除（二段確認）を追加。`BackupService.clearAll` と `CoreApi.clearAllData` を足した。
- **通知**: 既定を「授業開始 10 分前」にし、課題の締切前日 20:00 の知らせを追加。設定で分数（5/10/15/30）と課題リマインダーの ON/OFF を選べる。
- **ハプティクス**: `tapFeedback` / `selectFeedback` を expect/actual で追加し、タブ切替・主要ボタン・FAB・TODO の完了・空きコマ・シートを閉じたときにだけ出した。Web は空、Android は `performHapticFeedback`、iOS は `UIImpactFeedbackGenerator` を使い回す。
- **アクセントカラー（UI 監査 38）**: `AppSettings.accent` を追加し、プリセット 8 色と RGB スライダー（グラデーションのトラック）から 1 色を選べるようにした。`primary` / `primaryContainer` / `on-*` / `sectionBar` に加え、地・面・線の色相もアクセントから導出するので、ライト・ダーク・ピュアブラックのどれでも破綻しない。ホームのヘッダーは左にアプリアイコンとアプリ名、右に設定への歯車を置いた。

## [Unreleased] - 2026-11-02

### Added

- **オフライン運用と QR 転送の設計メモ (`docs/offline-and-qr-transfer.md`)**:
  - 現実的なデータ量で JSON / zstd / 転送専用バイナリのサイズを実測。
  - 1 学期分の時間割 + 出席は 882 B（zstd 292 B）で 1 枚の QR に収まる。
  - 2 年分の全項目でも 2.9 KB まで圧縮でき、L 1 枚または M 2 枚の範囲。
  - `INTERNET` を宣言しない offline flavor の成立条件を整理。
- **Kotlin Multiplatform の共有コア (`kmp/shared`)**:
  - `Course` / `Todo` / `CalendarEvent` / `AttendanceRecord` / `GpaEntry` / `AcademicYear` / `UserData` を Kotlin で定義。
  - 出席率（`AttendanceCalculator`）、GPA（`GpaCalculator`）、時間割の重複・範囲検証（`TimetableValidator`）を実装。
  - バックアップ JSON の読み書き（`UserDataCodec`）。壊れたレコードだけを落とし、重複を統合し、範囲外の値を補正する。
  - シラバス TSV のパーサ（`SyllabusTsvParser`）。APK 版が落としていた「月1, 月2」の複数コマを、コマごとに 1 件へ展開する。
  - JVM / Wasm(Node) / Android の 3 ターゲットでビルドとテストを確認（24 テスト × 2 ターゲット）。
- **ドキュメント**: `docs/uec-apk-analysis.md`（APK 解析と通知・ウィジェットの深掘り）、`docs/kmp-migration.md`（移行計画）。
- **unslop スキル**を `~/.agents/skills/unslop` に導入（UI 監査用）。

### Changed

- **Svelte 版を `legacy/svelte` ブランチで凍結**。`master` の本線を KMP に切り替えた。
- `AGENTS.md` のプロジェクト概要を KMP 本線に更新。
- APK 解析ドキュメントと退避ディレクトリを UEC 表記に改名し、現行ドキュメントから旧称を除去した。

### Removed

- ルート直下の解析用 APK（`base.apk`、`uec-universal.apk`、split APK）をリポジトリ外の
  `/data/artifacts/uec-apk-2026-11-02/` へ退避。
- 重複画像 `OpenYASKE.png`、`app-icon.png.bak`、私物スクリーンショット `20260727_134053.jpg` を削除。
  （凍結ブランチには残っている）

## [0.1.0] - 2026-08-02

### Added

- **コントラスト検査基盤 (`src/lib/theme/contrast-matrix.ts`)**:
  - コードベースの実使用箇所から逆引きした全57チェックのコントラスト検査マトリックスを定義。
  - `validateThemePackContrast()` で全テーマの WCAG 2.1 適合を自動検証。新テーマは合格が必須。
- **スタイル対応の角丸システム (`resolveRadius`)**:
  - `src/lib/styles/components.ts` に `resolveRadius(pack)` を追加。`layout.borderRadius`（従来は未使用の死に設定）を実効化。
  - Win95/Brutalism は角ゼロ、Neumorphism は大きな角丸、Apple はピル形状と、スタイルごとに特徴的な角丸が自動適用。
  - `resolveComponents()` が input/card の角丸を自動適用。14コンポーネント超をスタイル対応角丸に移行。
- **ハイコントラストテーマのバンドル**:
  - `src/lib/theme/defaults/high-contrast.json`（ライト）/ `high-contrast-dark.json`（ダーク）を新規追加。
  - ハイコントラストスタイル選択時に対応テーマを自動適用。離脱時は直前のテーマを復元（`prevThemePackIdLight/Dark`）。
- **影の完全制御 (`style-shadows-none`)**:
  - flat / high-contrast / win95 等の影なしスタイルで、アプリ全体の影を強制無効化する機構を追加。
  - DEFAULT_COMPONENTS とコンポーネントのハードコード shadow を CSS 変数（`--shadow-card` 等）に統一。
- **テーマオーバーライド (`themeOverrides`)**:
  - テーマのデフォルトの上に、プライマリカラー・ページ背景色・フォント・角丸を個別カスタマイズ可能。
  - 設定画面に「クイックカスタマイズ」セクションを追加。
- **Neumorphism 派生変数 (`--neumo-*`)**:
  - `css-bridge.ts` がテーマの表面色から凹凸シャドウ色を動的計算。明るい/暗いテーマ両方で正しい凹凸を自動生成。
- **背景画像の可読性確保**:
  - 背景画像有効時に `color-mix` スクリム（ページ色55%）を全面に重ね、ガラス質感を保ちつつテキストの可読性を確保。

### Changed

- **VisualStylePack の品質向上と差別化**:
  - Apple / Material / Flat を明確に差別化（Apple=すりガラス、Material=トーナル+elevation、Flat=影ゼロ+ミュート塗り）。
  - Neumorphism: inner ring ハイライト、focus リング、インセット見出しで一体感を強化（v5）。
  - Brutalism: カラーブロック、hover 色シフト、反転セクションヘッダーを追加（v4）。
  - Cyberpunk: ネオンボタン、text-shadow 発光見出し、グロー強化（v4）。
  - Windows XP: 鮮やかな青グラデーションのタイトルバー/タスクバー、XP ブルーボタンで本物の Luna テーマ風に（v5）。
  - Win95/WinXP: ハードコード色（`bg-white`/`text-black`）を CSS 変数化し、全テーマで 3D ベベルが追従（v4）。
- **設定画面の再編**:
  - 「テーマパック」「外観」「クイックカスタマイズ」の3セクションを「テーマ」「表示」の2セクションに統合。
  - クイックカスタマイズをテーマエディタリンクと画面スタイルの間に配置。
- **トップバーのコントラスト改善**: PageHeader に不透明バッキングを追加し、ガラス透過時でもテキストの可読性を確保。
- **検索UIの統一**: ホーム/More/設定の検索入力がすべてスタイル対応の角丸に追従するよう統一。
- **4テーマのカラーパレットを更新**: iOS 風/大学ブランド/和菓子テーマに洗練。スケール重複（700=800）を修正し、全コントラストチェック合格。

### Fixed

- ハイコントラストスタイルから別のスタイルに戻したとき、以前のテーマが復元されない問題を修正。
- 9コンポーネント14箇所のハードコード `rounded-card` がスタイル切替に追従しない問題を修正。
- SpotlightSearch の検索ボタンが縦長になる問題を修正（36×36 正方形に固定して正円化）。

## [Unreleased] - 2026-07-27

### Added

- **Liquid Glass を削除し、Windows 95 / XP スタイルパックを追加**:
  - Liquid Glass パックを削除（Apple と被るため）。
  - `src/lib/styles/win95.ts`: 3D レイズド/サンクン枠、角ゼロ、MS Sans Serif 風タイポグラフィ。
  - `src/lib/styles/winxp.ts`: XP Luna 風の青いアクセント・グラデーション・Tahoma 調タイポ。
  - 全7スタイル（apple/material/neumorphism/brutalism/cyberpunk/win95/winxp）に整理。
- **設定画面に「画面スタイル」セレクターを追加**:
  - `settingsStore.settings.visualStyle` に永続化。`styleStore` と連動。
  - 設定画面 > 外観にドロップダウンセレクターを配置。
  - i18n: 4言語対応のスタイル名メッセージを追加。

- **Neumorphism / Brutalism / Cyberpunk スタイルパックを追加**:
  - `src/lib/styles/neumorphism.ts`: Soft UI。極端な内外シャドウで凹凸表現。ガラス効果なし・ベタ塗り。
  - `src/lib/styles/brutalism.ts`: ハードオフセットシャドウ・太枠・スクエア角・大文字タイポグラフィ。
  - `src/lib/styles/cyberpunk.ts`: ネオン発光シャドウ（primary色依存）、2px solid ボーダー、光彩フィードバック。
  - `src/routes/layout.css`: 各スタイル用 shadow-\* CSS クラスを追加（shadow-neumo-, shadow-brutal-, shadow-cyber-）。
  - 全6スタイルを `styleStore` に登録。`styleStore.set('cyberpunk')` で即切替可能。

- **Material Design 3 / Liquid Glass スタイルパックを追加**:
  - `src/lib/styles/material.ts`: Material Design 3 準拠。ソリッドサーフェス、Elevation シャドウ、MD3 イージング。
  - `src/lib/styles/liquid-glass.ts`: Liquid Glass。Appleより劇的なガラス・反射・発光、強めの影とバウンス。
  - `src/lib/stores/style.svelte.ts`: 3スタイル（apple / material / liquid-glass）を登録。`styleStore.set('material')` で即切替可能。
  - `src/routes/layout.css`: Material / Liquid Glass 用の shadow-\* CSS クラスを追加。
  - `GlassContainer.svelte`: Material スタイル選択時は SVG distortion をスキップ（ソリッドサーフェス）。

- **ビジュアルスタイルパックシステム (`src/lib/styles/`) を新設**:
  - `src/lib/styles/types.ts`: `VisualStylePack` 型定義 — press/materials/easing/shadow/typography をパック化。
  - `src/lib/styles/apple.ts`: Apple Design Guidelines 準拠の Apple スタイルパックを実装（既存の挙動を完全維持）。
  - `src/lib/stores/style.svelte.ts`: `styleStore` — テーマパックと同様のリアクティブストア。将来 Material / Liquid Glass スタイルを追加可能。
  - `src/lib/apple-design.ts`: 実体を applePack に移行し、後方互換エイリアスとして再構成。
  - `GlassContainer.svelte`: `data-style` 属性を追加。スタイル切り替えに備えた設計に。
- **ダークテーマの改善**: `dark.json` の `surface.page` を `#000000`（ピュア黒）から `#0f172a`（スレート）に変更。`GlassContainer.svelte` のダークモードrgba値も同様にソフト化。

- **Siri Shortcuts / Android App Actions 対応**:
  - iOS: `@bubble-labs/capacitor-plugin-siri-shorts@8.0.1` を追加。4つの機能（時間割/TODO/カレンダー/出席）をSiriに寄贈し、音声で起動可能に。
  - Android: `res/xml/shortcuts.xml` に App Actions（BII: `OPEN_APP_FEATURE`）＋静的ショートカットを定義。`yaske://shortcuts/{feature}` のディープリンクで機能を直接開けるように。
  - 共通: `src/lib/shortcuts.ts` で両プラットフォームの統合リスナーを実装。`@capacitor/app@8.1.1` でディープリンクを受信。
  - i18n: 4言語（ja/en/zh/ko）に対応したSiri呼び出しフレーズと使用許諾説明文を追加。
  - iOS設定: `Info.plist` に `NSUserActivityTypes`、`NSSiriUsageDescription`、`CFBundleURLTypes` を追加。
  - Android設定: `AndroidManifest.xml` に `yaske://shortcuts/*` のVIEWインテントフィルターとショートカットメタデータを追加。`MainActivity.java` で `onNewIntent` をハンドリング。

- **共通エラー画面コンポーネント (`src/routes/+error.svelte`) の追加**:
  - SvelteKitの共通エラーハンドリング機能を利用し、404（Not Found）や503（Service Unavailable）などのエラー発生時にプレーンテキストではなく、美しく統一されたエラー画面を表示するコンポーネントを新規作成しました。
  - Svelte 5 の Runes (`$state`, `$derived`) を使用し、`page.status` および `page.error` に基づく動的なエラー切り替えを行います。
  - Apple Design Guidelines に完全準拠し、すりガラス質感（`APPLE_MATERIALS.card`）、滑らかなイージング（`APPLE_EASING.default`）による登場アニメーション、押し込みフィードバック（`APPLE_PRESS.button`）を備えたボタンを提供します。
  - 前のページに戻る（`history.back()`）およびローカライズされたホームへの遷移ボタンを用意。
  - 開発者やデバッグ向けに、エラーのスタックトレースや詳細メッセージをアコーディオンで展開できるデバッグ表示を実装しました。
- **運行情報（発車標・カウントダウン）機能の新規実装 (`src/routes/transit/`)**:
  - `https://api.transit.ls8h.com/` を用いた最寄り駅の発車時刻カウントダウン機能を新規作成しました。
  - `src/lib/stores/transit.svelte.ts` を新規作成。位置情報（緯度経度）からの逆ジオコーディング（近くの駅検索）やキーワード検索、ダイヤ情報の取得、IndexedDB キャッシュ（TTL制御）、電車発車時の自動リロード機能をカプセル化した Svelte 5 ストアです。
  - 駅検索時、ダイヤデータを持たない単なる地図上のピンやランドマーク（`transit:` や `osm:`, `geocoder:` プレフィックスで始まるID）を自動的にフィルタリングして除外する仕組みを実装しました。
  - `src/lib/components/dashboard/TransitWidget.svelte` を新規作成。Bento Card スタイルのウィジェットで、設定後は次の電車（最大5件）を秒単位でリアルタイムカウントダウン表示します。
  - `src/routes/transit/+page.svelte` を新規作成。詳細な発車標（最大20件）を表示するとともに、キーワード検索や位置情報検索から駅を設定・解除できる画面です。
  - `src/routes/+layout.svelte` にて、天気の位置情報（`settings.lat`/`lng`）が変更された際に自動的に逆ジオコーディングを行い、新しい最寄り駅（運行データを持つ実在駅）へ自動的に切り替えて設定を更新する連動ロジック（`$effect`）を実装しました。
  - 運行情報ストアの単体テストコード `src/lib/stores/transit.spec.ts` を追加し、逆ジオコーディング、IDフィルタリング、およびキャッシュの挙動をモック環境で検証できるようにしました。

### Changed

- **エラー表示関連の多言語翻訳キーの追加**:
  - `messages/{ja,en,zh,ko}.json` に、エラータイトル、各ステータスコードに応じたエラータイトル and 説明メッセージ、およびボタン用のラベルなどの翻訳キー（計10キー）をすべてのサポート言語に追加しました。
- **天気カードへの運行情報の結合表示と表示改善**:
  - `src/lib/components/WeatherCard.svelte` を修正。設定から「天気カードに運行情報を表示」を有効にした際、天気カードの下部に設定された駅の直近の電車情報（最大2件）を表示する機能を追加しました。
  - 天気カード内の結合表示にて、電車の定刻発車時刻（`HH:MM`）を併記し、残り時間のカウントダウンをスマートな `mm:ss` 形式に変更しました。また、不要な「自動で更新されます」の注記ラベルを削除しました。
- **ユーザー設定項目への運行情報関連オプションとデフォルト値の追加**:
  - `src/lib/types.ts` の `UserSettings` 和 `DEFAULT_USER_SETTINGS` に `transitStationId`, `transitStationName`, `transitLastLat`, `transitLastLng`, `weatherShowTransit` を追加しました。デフォルト設定で、初期値の座標に対応する「調布駅 (`tokyo-keio-rail:Chofu`)」が最初から設定されるよう変更しました。
  - `src/lib/features.ts` の `APP_FEATURES` に `transit` 機能を追加登録し、ナビゲーションバー並び替えやホームレイアウトカスタマイズに自動連動させました。
  - `src/routes/+page.svelte` (ダッシュボード) および `src/routes/settings/+page.svelte` (設定) にて、新機能のウィジェット表示分岐や、トグルスイッチ設定を追加しました。
  - `messages/{ja,en,zh,ko}.json` に、運行情報および天気カード結合設定に関する多言語翻訳キーを追加しました。

- **初期設定のナビゲーションバー（ボトムバー・サイドバー）に「ダッシュボード（ホーム）」を追加**:
  - `src/lib/types.ts` および `src/routes/+layout.svelte` にて、`barFeatures` の初期設定/フォールバック値に `'dashboard'` を追加し、初期状態でダッシュボードがバーに含まれるように修正しました。

- **Liquid Glass エフェクト実装を nikdelvin/liquid-glass 方式に置き換え**:
  - `danilofiumi/liquid-glass-svelte` npm パッケージへの依存を完全に排除し、依存ゼロの自前実装に移行しました。
  - `src/lib/liquid-glass/displacement.ts` を新規作成。SVG `feDisplacementMap` による物理ベースのガラス屈折マップを動的生成するユーティリティです。要素サイズと `border-radius` に合わせてX/Y方向グラデーションを mix-blend-mode: screen で合成し、角丸対応の屈折マップを生成します。
  - `src/lib/liquid-glass/LiquidGlassEffect.svelte` を新規作成。`ResizeObserver` で要素サイズを監視し、SVG `<filter>` 定義を `<body>` に動的注入、`backdrop-filter: url(#id)` でリアルな屈折を適用する Svelte 5 コンポーネントです。Chromium では本物の屈折エフェクト、Safari/Firefox は glassmorphism blur に自動フォールバックします。
  - `GlassContainer.svelte` を新実装に書き換え。Props インターフェース（`variant`, `contrast`, `distortion`, `glow`, `elevation`）は後方互換を維持したまま、`LiquidGlassEffect` を内部で使用します。
  - `LiquidGlassButton.svelte` を新実装に書き換え。`liquid-glass-svelte/GlassedButton.svelte` への import を削除し、自前の Liquid Glass ボタン実装に置き換えました。
  - `src/lib/liquid-glass-svelte.d.ts` の旧型定義をクリア（パッケージ削除に伴い不要）。

- **モバイルアプリアイコンの更新**:
  - Webで現在使用されている最新のアイコン画像 `static/icon-512.png` をルートのモバイルアプリ用アイコンソース `app-icon.png` および `OpenYASKE.png` に適用し、モバイル/Webの各種アセット（Capacitor用およびFavicon類）を再生成・同期しました。
- **PDF・シラバス・データ管理画面のUI刷新 ＆ 検索バーアイコン統一**:
  - **検索バーのアイコン統一**: `SpotlightSearch.svelte` にて、デスクトップ表示時に「検索」の文字列ボタンになっていた部分を、モバイル同様のアイコン（`Search`）のみのスマートな円形ボタン表示に統一しました。
  - **PDF変換画面のUIリニューアル (`src/routes/pdf/+page.svelte`)**: 各種設定や画像追加エリアを `BentoCard` に刷新し、アクションボタンへ `APPLE_PRESS` イージングと最小タップ領域 `min-h-[44px]` を設定。また、ハードコードされていた日本語を Paraglide (`m.xxx()`) メッセージ化し、多言語対応を行いました。
  - **シラバス閲覧画面のUIリニューアル (`src/routes/syllabus/+page.svelte`)**: 固定検索ヘッダーをすりガラス化し、授業カードを `BentoCard` のように美しい境界線、ホバー浮き上がり、`APPLE_PRESS` 反応を持つタイルへと刷新。曜日・時限・ドロップダウンフィルター等のコントロールのスタイルを整理しました。
  - **データ管理画面のUIリニューアル (`src/routes/data-management/+page.svelte`)**: 各種同期ステータスやクレジットバナーに `GlassContainer` や `BentoCard` を適用してすりガラス質感の角丸タイルに刷新し、アクションボタン群に `APPLE_PRESS` 物理プレスフィードバックと最小タップ領域を適用しました。

## [Unreleased] - 2026-07-19

### Added

- **Apple Liquid Glass 共通コンポーネント (`GlassContainer.svelte`) の導入**:
  - `liquid-glass-svelte` (v1.2.0) を導入し、OpenYASKE のプロジェクト規約（テーマ変数経由での色彩管理、HEX直書き排除、`prefers-reduced-transparency` に対する Solid 背景への自動フォールバック）に対応させた共通ガラスラッパーコンポーネントを作成しました。
- **Apple Bento Grid スタイルのコンポーネント (`BentoCard.svelte`) の新規作成**:
  - すりガラス（`backdrop-blur-md`）、ホバー・タップ時押し込みフィードバック（`active:scale-[0.98]`）、可変サイズ指定に対応した汎用タイルコンポーネントを追加し、全画面で使い回せるようにしました。
- **Spotlight 風検索コンポーネント (`SpotlightSearch.svelte`) の新規作成**:
  - macOS / iOS Spotlight の洗練されたデザインと動的サジェスト、即時クリアボタンを備えた独立した検索コンポーネントを作成しました。

### Changed

- **`liquid-glass-svelte` (https://github.com/danilofiumi/liquid-glass-svelte) 屈折歪み & Light/Dark 自動連動の修復**:
  - Danilo Fiumi 氏の SVG 液体屈折ノイズフィルター (`id="lg-dist-global"`, `feTurbulence` + `feDisplacementMap` + `saturate`) をグローバルに正しく透過配置し、ブラウザ上で本物の液体屈折歪み効果がレンダリングされるよう修正しました。
  - `themeStore` のテーマ状態変更および `document.documentElement` の `.dark` クラスを `MutationObserver` で動的に監視し、Light / Dark モード切り替え時に Liquid Glass のブラー、インセットシャドウ、上辺ハイライト光が自動連動する仕組みを構築しました。
- **アプリ全体での Liquid Glass 一貫適用の拡大**:
  - `PageHeader`, `Modal`, `Sheet`, `SpotlightSearch`, `DashboardCardGrid` 等の主要 UI コンポーネントに `GlassContainer` (variant: header, modal, input, card) を適用し、Tailwind CSS の `backdrop-blur-*` 直書きから統一された Liquid Glass 素材へと全面移行しました。
- **UI コンポーネントの Liquid Glass 化**:
  - `BottomTabBar` (Floating Dock), `SideNav` (デスクトップサイドバー), `BentoCard`, `StatCard`, `ListItem`, `AttendanceCard` などの主要な UI コンポーネントに `GlassContainer` を適用し、本物の液体ガラス効果（エッジ別ブラー・ライティング・ノングレア処理）を導入しました。
- **Apple Design Guidelines (SKILL.md §1-§17) に準拠した全体リファクタリング**:
  - **Reduced Motion & Reduced Transparency 対応 (`layout.css` / `GlassContainer.svelte`)**: `@media (prefers-reduced-motion)` でスライドやスケールアニメーションを無効化・クロスフェードへ置換し、`@media (prefers-reduced-transparency)` で背景を自動でソリッド化するアクセシビリティ対応を実装しました。
  - **物理スワイプ・Rubber-banding 弾性抵抗 (`apple-design.ts` / `TodoItem.svelte`)**: スワイプ削除時のハードストップを廃止し、境界を越えた場合に自然な弾性抵抗が発生する `rubberBand` 物理計算処理を実装しました。
  - **iOS 18 OLED Pure Black & Deep Charcoal ダークテーマへの全面刷新 (`dark.json` / `default.json`)**: 従来のアオくすんだslate系背景から、iOS 18 / macOS Sequoia OLED ピュアブラック (`#000000`) & ディープチャコール (`rgba(28, 28, 30, 0.8)`)、超薄ホワイト極細ボーダー (`rgba(255, 255, 255, 0.12)`), 発色の良い Apple System Blue (`#0a84ff`) に刷新しました。
  - **中央デザインシステムモジュール (`src/lib/apple-design.ts`) の強化**: イージング定数 (`APPLE_EASING`)、物理プレスフィードバック (`APPLE_PRESS`) に加え、`APPLE_TYPOGRAPHY` (size-specific tracking/leading) や `APPLE_MATERIALS` を拡充しました。
  - **アクセシビリティ・セマンティクス及びカラーハードコードの全面修復**:
    - `WeekGrid.svelte`: `div[role=button]` を純粋な `<button>` 要素に変更してセマンティクスを正常化。
    - `SpotlightSearch.svelte`: クリアボタンの誤った `aria-label` を修正し、`role="combobox"` とキーボード上下（↑/↓）サジェスト選択機能を追記。
    - `TextField.svelte` / `WeatherCard.svelte`: ハードコードされた `red-500` / `red-600` 色をすべてテーマ変数 (`var(--color-attendance-absent-fg)`) に統一。
  - **「リンクを追加」タイトルの完全中央揃え (`+page.svelte`)**: 「リンクを追加」テキストに `text-center w-full px-1` を付与し、他のポータルリンクと完全に揃うよう配置を修復しました。
  - **天気カード (`WeatherCard.svelte`) 最高・最低気温のコントラスト大幅向上**: 最高気温・最低気温・降水確率の表示にダーク/ライト両環境で WCAG 2.1 AA 基準 (4.5:1 以上) を満たす視認性カラー (`text-[var(--color-danger-400)]` / `text-[var(--color-primary-400)]`) を適用しました。
  - **背景輝度に応じたスマートコントラスト診断機能 (`contrast.ts`)**: ダーク背景・ライト背景の双方において最適なカラーパレット（400/500/600）を自動判定してテストするアルゴリズムを導入し、テスト信頼性を向上させました。
  - **4大徹底検査の完了**:
    - **揃えと余白**: `items-center` の軸揃えと一貫した間隔。
    - **最大最小サイズ**: モバイル最小タップターゲット 44px (`min-h-[44px]`) を全件保証。
    - **レスポンシブ**: 全ブレークポイントでの Bento Layout 表示最適化。
    - **i18n**: 直書き日本語テキストの完全 Paraglide (`m.xxx()`) 化。

## [Unreleased] - 2026-07-16

### Added

- **時間割ウィジェット（Android/iOSネイティブ）の新規実装**:
  - `capacitor-widget-bridge` プラグインを導入し、Web側と各OSのネイティブウィジェット間で時間割データを同期する仕組みを構築しました。
  - **Web (SvelteKit) 側**: 時間割やウィジェット配色などのデータ・設定変更時に自動同期をトリガーする `src/lib/widget.ts` を新規作成し、`userDataStore` および `settingsStore` に同期フックを追加しました。
  - **Android ネイティブ側**: 今日の時間割（1〜7限）を表示するウィジェット `TimetableWidgetProvider.java`、表示レイアウト `timetable_widget.xml`、メタデータ `timetable_widget_info.xml`、および配色や背景角丸・バッジ用の Drawable / カラーリソースを作成し、`AndroidManifest.xml` にレシーバー登録を行いました。
  - **iOS ネイティブ側**: SwiftUI / WidgetKit を用いて「今日の時間割」を描画する `TimetableWidget.swift` および設定ファイル `TimetableWidgetInfo.plist` をテンプレートとして `ios/App/TimetableWidget/` 配下に配置しました。
- **開発者向け拡張ガイドの新規追加**:
  - `EXTENDING.md` をルートに作成し、新規機能の追加方法、i18n翻訳キーの追加手順、型拡張、テストの記述および実行方法などを体系的にまとめました。
- **データ管理サービスの単体テストの新規作成**:
  - `SchoolDataService` に対する網羅的なテストスイート `src/lib/schoolData.spec.ts` を新規作成し、IndexedDBやRemoteStorage同期、SPARQLデータのパースなどの挙動をモックを用いて網羅しました。
  - 公開データ互換レイヤー `publicData.ts` に対するテストスイート `src/lib/publicData.spec.ts` を作成し、キャッシュ動作とスキーマの一貫性を保証しました。

### Changed

- **ビルド・コンパイル設定の調整**:
  - pnpm v11 での `sharp` や `esbuild` などのネイティブモジュールビルドを許可するため、`.npmrc` に `only-built-dependencies` の記述を追加しました。
  - 開発環境の JDK 17 でもコンパイルができるよう、ルート `build.gradle` において、すべてのサブプロジェクトの Java ソース/ターゲット互換性を Java 17 に強制上書きする `subprojects` 設定を追加しました（Java 21 の CI でも動作します）。
- **日本の祝日計算の内製化と外部依存排除による軽量化**:
  - 巨大な多国籍祝日データベースを持つ `date-holidays` パッケージへの依存を排除し、日本の祝日（振替休日、国民の休日、オリンピック特例を含む）をプログラム計算する超軽量な独自ロジック（`src/lib/holidays.ts`）に置き換えました。これにより `/calendar` ページの初期JSバンドルサイズが大幅に削減されます（1.43MB → 41KB）。
- **PDF生成処理の遅延ロード化による軽量化**:
  - `src/lib/pdf.ts` および `src/routes/pdf/+page.svelte` にて、`jsPDF` を静的インポートから動的インポート (`await import('jspdf')`) へ移行しました。これにより、PDF生成時のみライブラリがロードされ、アプリ起動時および `/pdf` ページの初期ロードサイズが激減します（410KB → 11KB）。
- **データ同期処理におけるエラーハンドリング徹底と安定性向上**:
  - `SchoolDataService` 内の非同期処理（`sync`, `clearCache`, `importCustomCourses`）すべてに `try-catch` を導入し、エラー時にもUIがローディング状態でハングしないようにし、適切なエラー表示ステータスをUIにフィードバックするよう改善しました。
- **データ同期ステータスの多言語化 (i18n)**:
  - 従来日本語でハードコードされていたデータ同期およびキャッシュ消去の進行ステータス（「SPARQLからダウンロード中」など）をすべて Paraglide.js による翻訳キーに移行し、`ja`, `en`, `ko`, `zh` の全言語ファイルに新規キーを反映・コンパイルしました。
- **プロジェクト設定と環境設定の改善**:
  - 旧称 `opennexus` および `nexus` を設定から徹底的に排除し、`docker-compose.yml` 内のコンテナ名等を `openyaske` に統一・修正しました。
  - `.prettierignore` に `/build/`, `/.svelte-kit/`, `/ios/`, `/android/`, `/src/paraglide/`, `/src/lib/paraglide/` などのビルド済み/自動生成アセットを追加し、フォーマッターやリンターの実行速度を大幅に向上させました。
- **AIエージェント向け指示書の整備**:
  - `AGENTS.md` のコーディング規約に、動的インポートによるパフォーマンス維持、try-catchエラーハンドリングの徹底、名称の統一、テスト作成の義務などの項目を追加し、AIエージェントの作業品質を向上させました。

### Fixed

- **CI/CDでのpnpmワークスペースビルドエラーの解消**:
  - `pnpm-workspace.yaml` に `packages` フィールドを追加して root ディレクトリ `.` を指定し、GitHub Actions の `setup-pnpm` アクション実行時に `packages field missing or empty` エラーでビルドが失敗する不具合を修正しました。
  - `package.json` に残っていた非推奨の `pnpm` フィールドを削除し、インストール時の警告ログを排除しました。

### Removed

- **不要な旧ファイルの削除**:
  - 旧称が含まれ、どこからも参照されていなかった不要な `open-nexus-base64.txt` を完全に削除しました。
  - 不要となった `date-holidays` パッケージを依存関係から削除しました。

## [Unreleased] - 2026-06-29

### Added

- **テーマエディターへのアクセス導線の追加**:
  - 設定画面の「テーマパック」設定欄の末尾に、テーマエディター（`/theme-editor`）への直接遷移リンクを追加しました。
  - モジュラー機能配置システムに「テーマエディター（`theme-editor`）」を新規追加し、「その他」画面の機能一覧からアクセス可能にしました。また、ホーム画面のボタンやナビゲーションバーに配置できるようになりました。

### Removed

- **不要となった旧機能定義のクリーンアップ**:
  - 過去に削除された学校データ閲覧機能（`data-browser`）の古い定義がデフォルトのユーザー設定（`homeFeatures`, `homeFeatureLayouts`）に残っていたため、完全に削除しました。

## [Unreleased] - 2026-06-28

### Added

- **シラバス検索の検索バー＆フィルターの固定 (Sticky 化) と「上に戻る」FABの導入**:
  - スクロール時に検索キーワードや条件の変更がスムーズに行えるよう、検索バー and フィルターアコーディオンを画面上部に固定 (`sticky`) しました。
  - 一定スクロールで右下に表示され、クリックするとスムーズに最上部へ戻る「上に戻る」フローティングアクションボタン (FAB) を追加しました。
- **シラバスの「時間割に登録」ボタンとトーストの多言語対応 (i18n)**:
  - `syllabus_add_to_timetable` と `syllabus_added_to_timetable` の翻訳キーを `ja`, `en`, `zh`, `ko` ロケールに追加し、言語切り替えに対応させました。
- **シラバス登録時のカラー選択機能と「登録済み」授業の編集画面遷移連携**:
  - 未登録の科目を時間割に追加する際、アプリ標準の `ColorPicker` コンポーネントを表示して、プリセットパレットからの選択に加え、カスタムHEXカラーによる詳細なセル色設定ができるカラー選択機能を追加しました。
  - すでに登録済みの科目の「登録済み」ボタンをクリックした際に、時間割画面 (`/timetable`) に遷移してその科目の編集ドロワー (`CourseEditSheet`) を自動展開するシームレスな画面連携機能を実装しました。
  - モバイルの縦幅を考慮し、ボタンコンテナを `flex-col sm:flex-row` のレスポンシブな縦並び・横並び自動切り替えに最適化し、文字のはみ出しを防止しました。

### Changed

- **シラバス登録確認モーダル内のテキスト簡略化**:
  - モーダル内の「時間割に登録しますか？」という重複した確認用テキストを削除し、無駄な多言語メッセージキー `syllabus_add_confirm` を廃止しました。
- **シラバス画面のボタン文言修正**:
  - 実際の時間割登録処理に合わせ、ボタンのラベルを「履修登録」から「時間割に登録」に修正しました。
- **各カラースキーマのコントラスト比改善 (WCAG AA基準準拠)**:
  - 各テーマ定義ファイル (`default.json`, `dango.json`, `dark.json`, `uec.json`) の `attendance` (出席ステータス)、`nav.inactive` (非アクティブテキスト)、ブランド/アラートカラー (Success/Warning/Danger) などの配色を、テキストコントラスト比が 4.5:1 以上になるよう再調整・最適化しました。
- **Tailwind `dark:` メディアクエリによる色上書きの完全排除とテーマ変数への統一**:
  - コンポーネントおよびページ内の `dark:bg-xxx` 等による個別のダークモード色書き換え指定を完全に排除し、ライト・ダーク切り替えを含むすべての色彩表現をテーマパックの CSS 変数に委ねる設計に統一しました。
  - カレンダー (`CalendarMonthView.svelte`, `CalendarWeekView.svelte`) の日曜・祝日のテキストに欠席文字色 (白) が誤って指定され白背景と同化していたバグを、テーマ連動の危険色 (`danger.500`) に置き換えて修正しました。
  - GPA計算機 (`gpa/+page.svelte`) や時間割 (`timetable/+page.svelte`) の総単位数カードにおける白固定背景とアイコンがダークモード時に低コントラストで見えなくなっていたバグを、テーマ連動の背景 (`primary.100`) と文字色 (`primary.800`) に統一することで解消しました。
  - フローティングアクションボタン (FAB) や PDF画面のボタンを、ライト/ダークいずれのテーマパックでも自動的に最適なコントラスト比（白文字/黒文字のテーマ追従）が得られるテーマ変数クラス (`primary.800` の背景と `primary.50` のテキスト等) にリファクタリングしました。

- **科目データ（シラバス）の LOD (SPARQL) 統合**:
  - `SchoolDataService` 内に、SPARQL エンドポイントから `uao:Lecture`（講義情報）を一括取得して `PublicCourse` 型にパース・集約する `fetchCoursesFromLOD` メソッドを実装しました。
  - 科目データは IndexedDB および RemoteStorage に保存し、完全オフラインでの検索と時間割へのシラバス追加に対応しました。
  - データ管理画面のカスタム JSON 直接編集モーダルに「シラバス科目データ (Courses)」の編集欄を追加し、科目のマスターデータを直接編集・適用できるようにしました。
- **シラバスの絞り込みフィルターメニュー**:
  - シラバス検索画面 (`/syllabus`) に、曜日・時限・必修/選択種別・開講年度を指定できるアコーディオン式の絞り込み条件メニューを追加しました。ワード検索と併用して検索が可能です。
- **シラバス検索での無限スクロール（自動追加ロード）**:
  - スクリプト下部へのスクロールを検知して表示制限（`displayLimit`）を 50件ずつ自動で拡張する無限スクロール機能を、`/syllabus` に実装しました。スクロール検知にはブラウザ標準の軽量な `IntersectionObserver` を使用しています。

### Changed

- **学校データ（建物・組織）機能の完全撤廃とシラバスへの一本化**:
  - 不要になった建物・組織の学校データに関連する機能（`/data-browser`）および more 画面での全体検索、データ管理画面での同期・カスタム JSON 編集欄を完全に削除し、完全に「シラバスデータのみ」のクリーンな設計に統合しました。
- **機能配置設定（バー・ホーム）が保持されないバグの修正**:
  - `homeFeatureLayouts` がロードされる際、部分的なデータで上書きされて他のウィジェットの配置設定が消滅してしまう不具合を、既定値とディープマージするように修正しました。
  - 設定画面でドラッグ＆ドロップによる並び替えを行った際、移動のたびに超高頻度で LocalStorage 保存と RemoteStorage 同期通信が走り設定データが崩壊する問題を、ドラッグ終了時に初めて保存・同期する設計に改善しました。
- **科目データの大幅軽量化 (`$state.raw` 移行によるオーバーヘッド削減)**:
  - `SchoolDataService` 内で保持される科目の巨大なマスターデータ配列を `$state` から Svelte 5 の `$state.raw`（シャローリアクティブ）に変更しました。これにより、数千件のオブジェクト全てにディープリアクティブプロキシが生成される無駄な CPU / メモリコストを完全に解消し、IndexedDB からのロード時間と動作の不安定さを解消しました。
- **シラバス閲覧の詳細外部リンク遷移**:
  - シラバスの各授業カードの「詳細」ボタン、またはカード本体をクリックした際に詳細ポップアップモーダルを開くのを廃止し、該当する外部シラバス Web サイトへ直接遷移（別タブで開く）するように挙動を変更しました。これに伴い、LODフェッチ時にシラバス URL を `references` に自動でマッピングする処理を `SchoolDataService` に追加しました。
- **学校データの非同期初期化によるUI遷移の改善**:
  - `SchoolDataService` の初期化処理（IndexedDB キャッシュのデシリアライズ）を `setTimeout` で遅延して別フレームで実行するようにし、起動時やページ遷移時に巨大データ読み込みでメインスレッドがブロック（フリーズ）する問題を解消しました。
  - 初期化中も `loading` ステートを反映させ、UIの画面遷移を即座に先行完了させたあとからデータが流し込まれるように改善しました。
- **データ同期セクションの学校＆シラバス一本化**:
  - データ管理画面での「公開データ同期」と「学校データ同期」を「学校＆シラバスデータ同期」に一本化し、地物・組織・科目をまとめてワンクリックで同期できるように整理しました。
  - `publicDataStore` のデータソースを `schoolDataService` の科目（`courseItems`）からリアクティブに導出される `$derived` ストアに変更し、既存のシラバス関連コンポーネントを一切変更せずに安全にデータソースを統合しました。

### Removed

- **不要な外部 CDN フェッチロジックの廃止**:
  - `src/lib/publicData.ts` 内の外部 GitHub CDN からのシラバス JSON フェッチ処理を廃止し、`schoolDataService` のキャッシュを読み取る薄い互換レイヤーに簡略化しました。
  - これに伴い、使用されなくなった unit テストファイル `src/lib/publicData.spec.ts` を削除しました。
- シラバス検索画面 (`/syllabus`) の不要になった詳細モーダルのマークアップを削除しました。

## [Unreleased] - 2026-06-27

### Added

- **公開シラバスデータのJSON直接編集機能**:
  - データ管理画面 (`data-management`) に「公開データ同期 (シラバス & メディアカード)」セクションを追加し、シラバスの JSON データを直接編集・適用できる機能を追加しました。
- **時間割追加時のシラバス候補自動提案**:
  - `CourseEditSheet.svelte` にて、曜日・時限のセルから授業の新規追加シートを開いた際、その時限に開講されるシラバス科目一覧を自動で初期提案する機能を追加しました。
- **学校データ同期の進捗表示**:
  - `schoolDataService` にリアクティブな進捗ステータス (`syncStatus`) を追加し、データ管理画面 (`data-management`) にて同期の各フェーズ（RemoteStorage確認中、SPARQL取得中など）をリアルタイムで表示できるようにしました。
- **「このアプリについて」画面の新設**:
  - `src/routes/more/about/+page.svelte` を新設し、アプリ自身のMITライセンス、学校データのライセンス（設定値に基づく）、およびサードパーティ製オープンソースライブラリ（Svelte 5, Tailwind CSS 等）のライセンス表示に対応しました。
- **データ出典とライセンスのカスタマイズ設定**:
  - `UserSettings` に学校データの SPARQL エンドポイント、データ出典名、出典URL、ライセンス名、ライセンスURLの各設定フィールドを追加しました。
  - データ管理画面 (`data-management`) に「詳細設定」アコーディオンを追加し、上記設定値をユーザーが編集・保存できるようにしました。

### Changed

- **シラバス（公開データ）のキャッシュ永続化**:
  - `src/lib/publicData.ts` にて、TTL (有効期限) によるキャッシュ期限切れ判定を廃止し、自動再検証（`revalidate`）の呼び出しを削除しました。これにより、手動でリフレッシュボタンを押さない限り、CDNへ再問い合わせを一切行わないようにしました。
- **学校データの取得処理の高速化とサーバー負荷削減**:
  - アプリ起動時の RemoteStorage との自動同期を廃止し、起動時はローカルキャッシュ (IndexedDB) を読み込むだけにして無駄な通信を一切行わないようにしました。
  - 手動同期の際、RemoteStorage 上にデータが存在すれば LOD (SPARQL) を叩かずに RemoteStorage から復元するロジックを優先させ、LODサーバーへの負荷を削減しました。
  - SPARQL から新規取得したデータの RemoteStorage への保存は、同期完了を待たずに非同期でバックグラウンド実行するようにし、UIの待機時間を大幅に削減しました。
- **SPARQLフェッチの最適化とエラー堅牢化**:
  - 学校データとシラバスデータの IndexedDB からのキャッシュ読み込み時に例外をキャッチして安全にフォールバックするように修正し、キャッシュ破損による起動クラッシュを防止しました。
  - `schoolDataService` の SPARQL クエリ実行時のタイムアウトを 10秒に延長し、かつフェッチ処理を Promise.all で並列実行するように戻すことで、ハングやフェッチ失敗（コケる問題）を防ぎました。
- **インプットコンポーネントの `TextField` 統一 (デザインガイドライン準拠)**:
  - `CourseEditSheet` の検索インプット、およびデータ管理画面のカスタム JSON 編集テキストエリアなどの生 HTML タグを、デザインガイドライン (DESIGN.md) に準拠した `TextField` コンポーネントに統一しました。
- **学校データ機能の汎用化 (atlasの文言排除)**:
  - 既存の `atlas` という特定のサービス名称をコードベースから一掃し、汎用的な「学校データ (School Data)」へとリファクタリングしました。
  - `src/lib/atlas.svelte.ts` を削除し、`src/lib/schoolData.svelte.ts` に置き換えました。
  - IndexedDB や RemoteStorage のキー名・パスを `school-spatial`, `school-org`, `school-data/` 等に変更しました。
  - 多言語翻訳ファイル (`messages/*.json`) 内の `atlas_` キーを `school_data_` に置換し、関連するUIコンポーネント (`data-management`, `data-browser`, `more`) の変数名やインポートを `schoolData` に修正しました。
- **クレジットの動的表示**:
  - データ管理画面およびデータブラウザ画面の下部に、ユーザーの設定したデータソース名とライセンス情報が安全にサニタイズされた上でリンク表示されるようにしました。

### Added

- **カスタム機能プラグインアーキテクチャの導入**:
  - アプリのすべての機能をカスタム機能プラグインとして定義する `src/lib/features.ts` を追加し、多言語対応の表示名解決関数 `getFeatureName` を実装しました。
- **ダッシュボードウィジェットコンポーネント**:
  - ホーム画面を豊かにする「今日の時間割 (`TimetableWidget.svelte`)」「TODO (`TodoWidget.svelte`)」「カレンダー (`CalendarWidget.svelte`)」「出欠率 (`AttendanceWidget.svelte`)」の4つのミニウィジェット、およびその他機能用の「`FeatureLinkCard.svelte`」を `src/lib/components/dashboard/` に新設しました。
- **データ管理機能 (`/data-management`)**:
  - 設定画面のデータ関連機能を移管・集約し、さらに UEC Atlas とのオフライン同期や、カスタム学校データ (地物・組織) の JSON インポート/エクスポートによる独自差し替え機能を追加しました。
- **学校データ閲覧機能 (`/data-browser`)**:
  - UEC Atlas からキャッシュされた建物や組織データを完全オフラインで検索・詳細表示できる画面を新規作成しました。
- **シラバス検索閲覧機能 (`/syllabus`)**:
  - 公開科目データの検索と授業詳細の確認、およびその場から直接時間割に授業を追加できる履修登録ショートカット機能を持つ画面を新規作成しました。
- **テーマパックの多言語対応**:
  - `src/lib/theme/utils.ts` を追加し、多言語対応 of the display text helper `getLocalizedText` を実装しました。
- **多言語セリフ・サンセリフフォント対応**:
  - Google Fonts から CJK (日・中・韓) およびラテン言語用の明朝体・セリフフォント (`Noto Serif` 系列) とゴシック体・サンセリフフォント (`Noto Sans` 系列) を追加でロードするようにしました。
  - i18n 翻訳ファイル (`messages/*.json`) に `settings_font_sans` と `settings_font_serif` キーを追加しました。

### Changed

- **ナビゲーションとレイアウトの動的化**:
  - `src/routes/+layout.svelte` において、`settingsStore` の `barFeatures` 設定（最大4つの配置制限）に基づいて下部バーおよびサイドバーのタブ表示を動的に生成し、末尾に「その他 (`/more`)」を固定する構成に刷新しました。
- **ホーム画面のカスタマイズ対応**:
  - `src/routes/+page.svelte` を、`settingsStore` の `homeFeatures` の並び順および `homeFeatureLayouts` に基づいて、各機能を「ミニウィジェット」または「ボタンリンクカード」として動的に出し分けるレイアウトに変更しました。
- **設定画面のリファクタリング**:
  - `settings/+page.svelte` 内のデータ/RemoteStorageセクションを削除して `data-management` 画面に統合しました。
  - 新たに「ナビゲーションバーとホームの配置設定（並べ替え ↑/↓ ボタン・トグルスイッチ）」および「ホーム画面ウィジェット設定 (アプリ版用、システムテーマ参照・カスタムカラー)」セクションを追加しました。
- **その他画面の刷新と横断検索**:
  - `more/+page.svelte` にユニバーサル検索（横断検索）を実装し、入力されたキーワードで「機能」「登録授業」「TODO」「予定」「学校データ (建物・組織)」を横断して検索できる強力な機能を提供しました。
  - ナビゲーションバーに固定されていない機能をクイックアクセスとして表示し、全機能へのローンチパッドとしても機能するように整理しました。
- **画面遷移時の言語設定保持**:
  - `src/routes/+layout.svelte` のロケール同期処理を改善し、URL プリフィックスがない場合に `settingsStore` 内のユーザー言語設定にフォールバックさせ、リセットを防ぐようにしました。また、各ナビゲーションリンクを `localizeHref` でラップし、言語設定が維持されるようにしました。
- **データ同期時の衝突解決マージ**:
  - `src/lib/stores/settings.svelte.ts` および `src/lib/stores/userData.svelte.ts` において、RemoteStorage からデータをロードする際に、無条件での上書きを廃止し、最終更新日時（`updatedAt` / `createdAt`）が新しい方を優先してマージする衝突解決ロジックを導入しました。
- **テーマファイルの多言語翻訳の組込み**:
  - 各同梱テーマファイル（`default.json`, `uec.json`, `dango.json`, `dark.json`）の `name`, `description` やカラー定義内の `name` を、日本語・英語・中国語・韓国語の翻訳オブジェクト（`LocalizedString`型）に移行しました。
  - 設定画面やテーマプレビューなどで `getLocalizedText` を適用し、各表示言語に連動してテーマ・カラー名が翻訳されるようにしました。
- **ホーム画面下部リンクのカスタマイズ**:
  - 「学修要覧」と「時間割」リンクを `settings.externalLinks` に統合し、それぞれに編集 Pencil ボタンを追加することで、名前やURLを自由に編集・オーバーライドできるようにしました。
- **フォントのロケール連動型解決**:
  - `src/routes/+layout.svelte` の `fontFamilyStyle` derived 属性において、現在の表示ロケール（日・英・中・韓）に応じて最適なフォントおよびそのフォールバック（例: `Noto Serif` や `Noto Sans` 系列）が CSS に適用されるように修正し、各言語で「サンセリフ (ゴシック体)」と「セリフ (明朝体)」が美しく切り替わるようにしました。

## [Unreleased] - 2026-06-26

### Added

- **アプリの動的カスタマイズ（ブランディング）機能**:
  - 設定（Settings）画面から、ユーザーがアプリの表示名（`customAppName`）を自由に変更できるテキスト入力を追加
  - 設定（Settings）画面から、カスタムロゴ画像（`customLogoData`）をアップロードできる画像アップローダーを追加。画像は Canvas を用いて 128x128 ピクセルにトリミング＆JPEG（画質0.7）圧縮され、Base64 形式で settingsStore に保存される
  - カスタマイズされたアプリ名とロゴをデスクトップ用サイドバー（`SideNav.svelte`）のヘッダーに動的に適用する仕組みを実装
- **OpenYASKEアイコン**: `OpenYASKE.png` をベースにした新しいアプリ・ファビコンアセットの追加（Android/iOS/Web/SVG）

### Changed

- **UserSettingsスキーマと初期値の拡張**: `src/lib/types.ts` 内の `UserSettings` および `DEFAULT_USER_SETTINGS` に `customAppName` と `customLogoData` を追加
- **多言語メッセージの追加**: 各言語（ja, en, zh, ko）のメッセージファイルにアプリカスタマイズ機能に関連するローカライズ文字列を追加し、Paraglide.js によるコンパイルを実行
- **レイアウトのデータ連携**: `src/routes/+layout.svelte` から `SideNav` に動的なカスタムアプリ名とカスタムロゴ画像を Props 経由で渡すように更新
- **プロジェクト名の変更**: プロジェクト名を「open-nexus」から「OpenYASKE」（小文字: `open-yaske`）へ変更し、コードベース、テスト、マニフェスト、およびGit設定（`.git/config`）のすべての出現箇所を更新
- **署名鍵とCIワークフローの更新**: Androidのリリース署名鍵 `open-nexus.jks` を `open-yaske.jks` にリネームし、`.github/workflows/deploy.yml` 内の署名・ビルドアセット参照を更新
- **バージョン表記とソースリンクの更新**: アプリについて（About）に表示されるバージョン情報を日付ベース（`2026.06.26`）に変更し、ソースコードのリンク先 to `https://github.com/open-yaske/open-yaske-app` に更新（`more` ページおよび `settings` ページ）

### Removed

- **古いアイコン**: 旧ロゴアイコン `opennexus.png` の削除

### Fixed

- **カスタムアプリ名入力欄のフリーズ問題**: 設定画面でカスタムアプリ名（`customAppName`）を入力する際、フォールバック値（`|| ''`）の評価によってリネームがクリアされる不具合と、入力イベントハンドラ（`onchange` から `oninput` への変更）を修正しました。

## [Unreleased] - 2026-06-25

### Added

- **カスタム機能エディター** (`CustomFeatureEditor.svelte`): その他画面に Markdown / KaTeX / Mermaid 対応 WYSIWYG 入力プレースホルダーを追加
- **ブラウザユーティリティ** (`src/lib/browser.ts`): Capacitor ネイティブ環境とWebブラウザ環境を統一的に扱うブラウザ起動ユーティリティを追加

### Changed

- **その他画面「カスタム機能」**: ダッシュボードの文言を「カスタム機能」に変更し、カスタム機能エディターを統合
- **角丸（Border Radius）統一**: ボタン・カード・ダイアログ・入力フォームなどすべての UI 要素の角丸を一貫したデザインに統一
  - `Button.svelte`, `Modal.svelte`, `Sheet.svelte`, `TextField.svelte`, `ListItem.svelte`, `SectionHeader.svelte`, `StatCard.svelte`, `WeatherCard.svelte`, `SideNav.svelte` 等を更新
- **アプリ内ブラウザをネイティブ実装へ移行**: JS 実装のアプリ内ブラウザ（In-App Browser）を削除し、Capacitor ネイティブ環境ではネイティブブラウザ、Webブラウザ環境ではデフォルト挙動（別タブ）でリンクを開くよう変更
- **設定画面の検索機能修正** (`SettingsSearch.svelte`): 設定項目がキーワードで正しく検索・フィルタリングされない不具合を修正
- **ホーム画面リンクボタンの高さ統一** (`+page.svelte`): 特定の画面幅でリンクボタンの高さが揃わない不具合を修正
- **検索アライメント修正**: 検索入力欄と検索ボタンの垂直方向のズレを解消
- **i18n メッセージ更新** (`messages/ja.json`, `messages/en.json`, `messages/zh.json`, `messages/ko.json`): 上記変更に伴うキーの追加・修正
- **テーマ更新** (`dango.json`): Dango テーマのカラー調整
- **出席・カレンダー・TODO コンポーネント更新**: UI の改善と角丸統一対応
- **時間割コンポーネント更新** (`WeekGrid.svelte`, `SyllabusSheet.svelte`): UI の改善
- **Android / iOS 設定更新**: `AndroidManifest.xml`, `capacitor.build.gradle`, `Info.plist` 等のネイティブ設定を更新

### Removed

- **JS 製アプリ内ブラウザ** (`InAppBrowser.svelte`, `src/routes/inapp-browser/+page.svelte`): Capacitor ネイティブ実装へ移行に伴い削除
- **設定画面のアプリ内ブラウザ関連設定項目**: 削除されたコンポーネントに対応する設定項目を除去

---

## [0.1.0] - 2026-06-24

### Added

- 初期実装・アセットセットアップ・Android 署名ワークフロー追加
- OGP メタタグ設定
