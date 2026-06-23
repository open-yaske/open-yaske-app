# AGENTS.md - AIエージェント向けプロジェクトガイド

## Project Overview

時間割アプリ「open-nexus」。SvelteKit + CapacitorでWeb/モバイルクロスプラットフォーム。  
**コアコンセプト**: Local-First + Offline-First + AIフレンドリーな公開データ。

- **公開データ**: GitHub上のMarkdown（科目情報）→ CI/CDでJSON生成 → CDN配信
- **個人データ**: RemoteStorage.js（ユーザー履修、カレンダー、TODO）
- **モバイル**: Capacitor（Android/iOS） + オフライン対応必須
- **将来像**: BYOK対応エージェントで履修計画提案

## 多言語対応（i18n）

- **対応言語**: 日本語（ベース） / 英語 / 中国語（簡体字） / 韓国語
- **ツール**: Paraglide.js（`@inlang/paraglide-js`）
- **メッセージファイル**: `messages/{locale}.json` — 全キーを全言語で揃えること（**約 220 キー**）
- **ベースロケール**: `ja` — フォールバック言語
- **翻訳追加手順**:
  1. `project.inlang/settings.json` の `locales` に対象言語を追加
  2. `messages/{locale}.json` を作成（既存のキーをすべて翻訳）
  3. `pnpm exec paraglide-js compile` でコード生成
- **Paraglide 生成物**: `src/lib/paraglide/` — 自動生成、直接編集禁止
- **使い方**: `import { m } from '$lib/paraglide/messages'` → `m.hello_world({ name: '...' })`

## コーディング規約（必ず守ること）

- TypeScript厳格モード
- **Svelte 5（Runes）強制** — `$state`, `$derived`, `$props`, `$effect` のみ使用
- **Tailwind v4 + mdsvex対応**
- コンポーネントは `$lib/components/` に整理
- データ関連は `$lib/remotestorage.ts`, `$lib/publicData.ts` にまとめる
- オフライン対応を意識（`navigator.onLine`, キャッシュ優先）

## 🎨 色定義は `src/lib/theme/` に集約（HEX 直書き禁止）

**テーマパックシステム** で UI 色 / 科目色 / TODO 色 / イベント色 / 出席色を統合管理。

```
src/lib/theme/
├── index.ts            # 公開 API
├── types.ts            # ThemePack 型定義
├── defaults/           # 同梱テーマ（default, uec, dango, dark）
├── pack-loader.ts      # JSON ロード + バリデーション
├── tokens.ts           # パレット解決
└── css-bridge.ts       # CSS 変数への適用
```

- **ESLint ルール**: `theme-tokens/no-raw-hex` で HEX 直書きを禁止（`src/lib/theme/` 配下のみ例外）
- コンポーネントは `var(--color-primary-500)` のような CSS 変数経由で参照
- カスタムパックはユーザーが作成可能（Settings > テーマ）

## 📦 データ層

### 公開データ（科目マスター + ダッシュボード + テーマ）

- 一次ソース: `courses/year-id-subject.md`（Frontmatter + 本文） + `dashboard/` 配下の HTML カード
- GitHub で管理 → CI/CD で `courses.json` + `index.json` + `dashboard.json` + `themes.json` に変換 → CDN配信
- アプリ側: CDNからフェッチ → IndexedDB キャッシュ（TTL 24h、stale-while-revalidate）
- AIエージェントがMarkdownを直接読めるように設計
- **拡張性**: `schemaVersion` チェックで未知のフィールドも読み込み可能

### 個人データ（RemoteStorage）

- `/schedule/` : 履修リスト・カスタム時間割
- `/todos/` : TODOリスト
- `/calendar/` : カレンダーイベント
- `/attendance/` : 出席記録（**5 状態**: 出席 / 欠席 / 遅刻 / 公欠 / **休講**）
- `/user/` : 設定・Preferences・テーマパック

## 🆕 出席状態（5 値）

| 値          | 日本語   | 色         | 分母に含む     |
| ----------- | -------- | ---------- | -------------- |
| `present`   | 出席     | 緑         | ✅             |
| `absent`    | 欠席     | 赤         | ✅             |
| `late`      | 遅刻     | オレンジ   | ✅（0.5 換算） |
| `excused`   | 公欠     | 青         | ✅（出席扱い） |
| `cancelled` | **休講** | **グレー** | ❌ **除外**    |

出席率 = `(present + late * 0.5 + excused) / (present + absent + late + excused) * 100`

## 🆕 拡張可能公開データ（DashboardCard）

公開データから「**その他**」画面のカードグリッドを配信:

```ts
type DashboardCard =
	| { type: 'html-internal'; id; title; icon; layout; htmlUrl; order }
	| { type: 'external-url'; id; title; icon; url; openInApp; order }
	| { type: 'webview-popup'; id; title; icon; htmlUrl; externalUrl?; order };
```

- `htmlUrl` は **DOMPurify** でサニタイズしてレンダリング
- `externalUrl` は **アプリ内ブラウザ** または **ボトムポップアップ** で開く

## 重要なファイル・ディレクトリ

- `src/lib/theme/` — **テーマパックシステム**
- `src/lib/types.ts` — 共通型定義
- `src/lib/types/public.ts` — 公開データ型（DashboardCard, PublicCourse）
- `src/lib/remotestorage.ts` — RemoteStorageクライアント
- `src/lib/publicData.ts` — 公開データ取得 + IndexedDB キャッシュ
- `src/lib/attendance.ts` — 出席率計算（休講除外）
- `src/lib/html-renderer.ts` — DOMPurify ラッパー
- `src/lib/components/inapp-browser/` — アプリ内ブラウザ
- `vite.config.ts` — SvelteKit設定・アダプター
- `capacitor.config.ts`

## 🛠️ 開発コマンド

- `pnpm run dev` — 開発サーバー
- `pnpm run check` — 型チェック
- `pnpm run lint` — Lint (ESLint + Prettier)
- `pnpm run test:unit` — Vitest
- `pnpm run test:e2e` — Playwright
- `pnpm exec paraglide-js compile` — i18n メッセージ生成
- `pnpm run storybook` — Storybook
- `pnpm run build` — プロダクションビルド
- `pnpm exec cap sync` — Capacitor 同期
