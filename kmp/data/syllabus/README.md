# シラバスデータ

UEC の公開シラバスを TSV で保持している。アプリに同梱する形式は `syllabusTool` が
CBOR に変換する。TSV は人が差分を確認し、年度を追加するための一次ソースとして残す。

- `syllabus_YYYY.tsv`: 年度ごとのシラバス
- 1 行 8 列。1 行目などの `//` 始まりはコメント
- 列: 連番 / 学期 / 学期 / 曜日時限 / 科目コード / 科目名 / 担当 / 教室

変換:

```bash
cd kmp
./gradlew :syllabusTool:run
```

出力:

- `composeApp/src/commonMain/composeResources/files/syllabus.cbor`
- `composeApp/src/commonMain/composeResources/files/syllabus-manifest.json`

出典は UEC の公開シラバス。再配布時は出典と利用条件を確認すること。
