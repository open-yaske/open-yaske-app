# シラバスデータ

## TSV をどう扱うか

TSV は人が差分を見て年度を追加するには便利だが、配布形式としては次の弱点がある。

- 型がない。日付・時限・学期を毎回パースする
- 列名やスキーマの検証がない
- 同じ科目名・教員名・教室が年度ごとに繰り返される
- アプリ起動のたびに全行をパースして索引を作る
- タブ・改行・引用の扱いがデータ側に漏れる

そこで TSV は一次ソースとして残し、アプリに同梱する形式は CBOR に変換する。

## 配布形式

`SyllabusBundle` は文字列辞書と列指向のレコードを持つ。

- 辞書: 科目コード、科目名、教員、教室、学期
- レコード: 年度、各辞書の index、曜日、時限、学期、プログラム種別、重複 suffix
- ID は年度・コード・曜日・時限・教員から復元する。保存しない
- 文字列の繰り返しを辞書に寄せることで、単純な CBOR より大幅に小さい

| 形式 | サイズ | 備考 |
| --- | --- | --- |
| TSV 7 年分 | 1,067,218 B | 人が編集する一次ソース |
| 単純な CBOR（全フィールド） | 2,203,780 B | キーと ID の繰り返しで TSV より大きい |
| 辞書 + 列指向 CBOR | 616,632 B | 現在の配布形式 |

収録は 2020〜2026 年の 11,921 コマ。2026 年は前学期 906、後学期 803。
複数コマ表記（`月1, 月2`）はコマごとに展開している。

## パイプライン

```bash
cd kmp
./gradlew :syllabusTool:run
```

入力は `kmp/data/syllabus/syllabus_YYYY.tsv`、出力は次の 2 つ。

- `composeApp/src/commonMain/composeResources/files/syllabus.cbor`
- `composeApp/src/commonMain/composeResources/files/syllabus-manifest.json`

マニフェストには件数、バイト数、SHA-256、収録年度を書く。アプリは CBOR を読み、
`SyllabusBundleCodec.decode(bytes).toEntries()` で `CoreApi.setSyllabus()` に渡す。

## 検索

`SyllabusCatalog` は読み込み時に科目コード・科目名・教員名を正規化し、文字 bigram を作る。
検索はコード完全一致、前方一致、科目名・教員名の部分一致、bigram の重なりで順位を付ける。
1 万件規模なら起動時の索引構築は許容範囲だが、さらに速くするなら bigram の転置索引を
ビルド時に書き出す拡張ができる。

## データを更新するとき

1. 新しい年度の `syllabus_YYYY.tsv` を `kmp/data/syllabus/` に置く
2. `./gradlew :syllabusTool:run`
3. マニフェストの件数と SHA-256 を確認する
4. `:shared:jvmTest` と `:composeApp:assembleDebug` を回す

出典は UEC の公開シラバス。再配布時は出典と利用条件を確認する。
