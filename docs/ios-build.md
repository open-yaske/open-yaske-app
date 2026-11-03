# iOS ビルドと匿名性

## いまの状態

Linux 機では Kotlin/Native の Apple ターゲットをコンパイルできないため、
LAN の Intel Mac（macOS 15.7.9）に Temurin 21 を入れて検証した。

- `compileKotlinIosX64` / `iosArm64` / `iosSimulatorArm64` は成功
- `linkDebugFrameworkIosX64` は `xcrun` が iOS SDK を見つけられず失敗
- 原因は Xcode 未導入。Command Line Tools だけでは iOS SDK が入らない

## コマンドラインで Xcode を入れる方法

| 方法 | コマンド例 | Apple ID | 備考 |
| --- | --- | --- | --- |
| App Store CLI (`mas`) | `brew install mas` → `mas install 497799835` | 必要 | App Store にサインイン済みの Mac でのみ動く。最新版しか選べない |
| `xcodes` | `brew install xcodesorg/made/xcodes` → `xcodes install 16.4` | 必要 | バージョン指定可。2FA と資格情報の保存に対応。Intel なら 16.4 系を選ぶ |
| 直接ダウンロード | `developer.apple.com/download/all/` から `.xip` | 必要 | 認証 Cookie が必要。無認証の直リンクは 302 で拒否される |
| Command Line Tools | `xcode-select --install` | 不要 | iOS SDK は入らない。Kotlin のコンパイルまでならこれで足りる |
| `softwareupdate` | — | — | Xcode は配布されない |
| macOS CI | GitHub Actions の `macos-15` | 不要 | ランナーに Xcode と iOS SDK が入っている。リンクだけならこれが最短 |

Xcode 導入後の手順は次のとおり。

```bash
sudo xcode-select -s /Applications/Xcode-16.4.app
sudo xcodebuild -license accept
sudo xcodebuild -runFirstLaunch
```

`sudo` とライセンス同意は対話が必要なので、SSH の自動実行だけでは完結しない。

## xtool などの代替について

`xtool` は SwiftPM プロジェクトを iOS アプリにするツールで、Xcode のビルドシステムを使わない。
ただし次の理由で、今回の代替にはならない。

- macOS では前提として Xcode のインストールが必要（iOS SDK とツールチェーンのため）
- Linux / Windows でも `Xcode.xip` を Apple ID でダウンロードし、そこから iOS SDK を作る
- 対象は SwiftPM の Swift アプリで、Kotlin/Native が出力するフレームワークのリンクは行わない
- Kotlin/Native の Apple ターゲットは macOS ホストでしかリンクできない。Linux 上で SDK を用意してもホスト制約は消えない

`theos`、`osxcross`、`cctools-port` も同じで、iOS SDK は Xcode 由来か第三者の抽出物になる。
第三者の Xcode / SDK ミラーはライセンス違反とマルウェア混入の危険があるため使わない。

## 完全匿名で続けられるか

結論は「iOS の配布まで含めると不可能」「開発と CI 検証だけなら Apple ID なしで可能」。
匿名性のレベルを分けると次のようになる。

| レベル | 可能か | 条件 |
| --- | --- | --- |
| Apple ID なしで Kotlin を iOS 向けにコンパイル | 可能 | CLT + JDK のみ。すでに成功している |
| Apple ID なしで iOS フレームワークをリンク | 可能 | macOS CI の Xcode を使う。GitHub アカウントは必要 |
| Apple ID なしでシミュレータ実行 | 可能 | Xcode が入った Mac か CI 上。ローカル Mac には Xcode が必要 |
| Apple ID なしで実機にインストール | 不可 | コード署名に Apple ID の Personal Team が必要。7 日で失効 |
| App Store / TestFlight で配布 | 不可 | Apple Developer Program と法定の身元情報が必要 |
| 完全にアカウントなし | 不可 | Xcode の入手にも CI にも何らかのアカウントが要る |

Apple ID の作成にはメール、氏名、生年月日、2 段階認証用の電話番号が要る。
Developer Program はさらに氏名・住所・規約同意が必要で、EU では trader 情報の表示も求められる。
このため「App Store に匿名で出す」は現実的でない。

## 匿名性を優先する構成

1. Apple ID を作らない。Xcode をローカルに入れない
2. GitHub のアカウントとリポジトリを分ける（組織のアカウントを使わない）
3. GitHub Actions の `macos-15` で `linkDebugFrameworkIosSimulatorArm64` を回す
4. 成果物はシミュレータ用フレームワークだけにし、署名と配布はしない
5. 実機配布が必要になった時点で、Apple ID と Developer Program の話に切り替える

この構成なら Apple には何も登録しない。ただし GitHub（Microsoft）にはコードとビルドログが残る。
「誰にも何も渡さない完全匿名」は、クラウドを使う時点で成立しない。

## 注意

- 第三者が公開している Xcode のミラーや改造版は、Apple のライセンス違反とマルウェア混入のリスクがある。使わない
- Xcode 26 系は Intel Mac での対応が不確実。Intel では 16.4 系を選ぶ
- CI の macOS ランナーは Xcode がプリインストールされているため、Apple ID なしでリンクできる
- 追加した `.github/workflows/ios-core.yml` はシミュレータ用と実機用のフレームワークをリンクし、成果物を保存する
