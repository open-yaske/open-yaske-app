# open-nexus

電気通信大学（UEC）向けの履修管理アプリです。SvelteKit + Capacitor で Web / Android / iOS を 1 つのコードベースから提供し、Local-First / Offline-First を前提にしています。

このプロジェクトは[NEXUS for UEC](https://nexusforuec.team-bookmark.com/)、[uBoard](https://uboard.info)にインスパイアされて開発されています。

[![Status](https://img.shields.io/badge/status-active-green)](#)
[![License](https://img.shields.io/badge/license-MIT-blue)](#)

## Features

- **時間割**: 科目登録、曜日・時限管理、シラバス詳細表示
- **ToDo**: 優先度、期限、科目紐付け、完了管理
- **カレンダー**: 月 / 週 / 日 / Agenda 表示、祝日・履修・ToDo の統合表示
- **出席管理**: 出席 / 欠席 / 遅刻 / 公欠 / 休講の 5 状態に対応。休講は出席率の分母から除外
- **RemoteStorage 同期**: 個人データをユーザーの RemoteStorage に保存
- **公開データ配信**: 科目マスターやダッシュボードカードを CDN + IndexedDB キャッシュで取得(データは未完成)
- **テーマパック**: UI 色、科目色、TODO 色、イベント色、出席色を JSON パックで統合管理
- **多言語対応**: 日本語 / 英語 / 中国語（簡体字） / 韓国語
- **モバイル対応**: Capacitor による Android / iOS ビルド、レスポンシブ UI

## Tech Stack

| Area            | Stack                                   |
| --------------- | --------------------------------------- |
| App             | SvelteKit 2, Svelte 5 Runes, TypeScript |
| Styling         | Tailwind CSS v4, mdsvex                 |
| Mobile          | Capacitor 8                             |
| Data            | RemoteStorage.js, IndexedDB, CDN        |
| i18n            | Paraglide.js                            |
| UI              | lucide-svelte                           |
| Tests           | Vitest, Playwright, Storybook           |
| Package manager | pnpm                                    |

## Getting Started

```bash
pnpm install
pnpm run dev
```

開発サーバーは Vite と mock CDN を同時に起動します。

## Commands

```bash
pnpm run dev          # 開発サーバー
pnpm run check        # Svelte / TypeScript チェック
pnpm run lint         # Prettier + ESLint
pnpm run test:unit    # Vitest
pnpm run test:e2e     # Playwright
pnpm run storybook    # Storybook
pnpm run build        # プロダクションビルド
pnpm exec cap sync    # Capacitor 同期
```

Paraglide の生成が必要な場合:

```bash
pnpm exec paraglide-js compile
```

## Project Structure

```text
open-nexus/
├── AGENTS.md                 # AI エージェント向け開発ガイド
├── ARCHITECTURE.md           # アーキテクチャ概要
├── README.md
├── messages/                 # i18n メッセージ
├── project.inlang/           # Paraglide 設定
├── static/data/              # 公開データ fallback
├── tools/                    # mock CDN などの開発ツール
└── src/
    ├── lib/
    │   ├── components/       # 共通コンポーネント
    │   ├── stores/           # アプリ状態
    │   ├── theme/            # テーマパックシステム
    │   ├── types/            # 共通型 / 公開データ型
    │   ├── attendance.ts     # 出席率計算
    │   ├── html-renderer.ts  # DOMPurify ラッパー
    │   ├── publicData.ts     # 公開データ取得 + キャッシュ
    │   └── remotestorage.ts  # RemoteStorage クライアント
    └── routes/
        ├── +layout.svelte    # ナビゲーション
        ├── +page.svelte      # ホーム
        ├── attendance/
        ├── calendar/
        ├── inapp-browser/
        ├── more/
        ├── settings/
        ├── theme-editor/
        ├── timetable/
        └── todo/
```

## Theme Packs

同梱テーマは `src/lib/theme/defaults/` にあります。

- **default**: 標準テーマ
- **uec**: 電気通信大学カラー
- **dango**: 桜ピンク / 白 / 抹茶グリーンの三色団子テーマ
- **dark**: ダークテーマ

コンポーネントでは HEX を直接書かず、`var(--color-primary-500)` などの CSS 変数を使います。テーマ定義の変更は `src/lib/theme/` に集約します。

## Data Model

公開データと個人データを分けています。

- **公開データ**: 科目マスター、ダッシュボードカード、公開テーマ。CDN から取得し、IndexedDB にキャッシュします。
- **個人データ**: 履修、ToDo、カレンダー、出席、設定。RemoteStorage で同期し、ローカルにも保持します。

出席率は次の式で計算します。

```text
(present + late * 0.5 + excused) / (present + absent + late + excused) * 100
```

`cancelled`（休講）は分母から除外します。

## Documentation

- [AGENTS.md](AGENTS.md) — 開発ルール、i18n、テーマ、データ層の実装ガイド
- [ARCHITECTURE.md](ARCHITECTURE.md) — システム構成とデータフロー

## License

MIT
