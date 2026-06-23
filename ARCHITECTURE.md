# ARCHITECTURE.md

> open-nexus システムアーキテクチャ

最終更新: 2026-06-22

## 全体像

時間割アプリ「open-nexus」。Web + モバイル（Android/iOS）対応の **Local-First** アプリケーション。

### コアコンセプト

1. **Local-First**: データはまずローカルにあり、ネットワークは同期のためだけにある
2. **Offline-First**: オフラインでも全機能が動作する
3. **AI-Friendly**: 公開データは GitHub Markdown で管理し、AI エージェントが直接読める
4. **Theme-able**: ユーザーが複数テーマを切り替えられる

## フロントエンド

- **フレームワーク**: SvelteKit 2 + Svelte 5（Runes 強制）
- **UI**: Tailwind CSS v4 + mdsvex
- **モバイル**: Capacitor 8（`adapter-auto` + WebView）
- **状態管理**: Svelte 5 Runes（`$state`, `$derived`, `$props`, `$effect`）+ RemoteStorage.js

## データ層

### 1. 公開データ（科目マスター + ダッシュボード + テーマ）

**1 次ソース**: GitHub 上の Markdown（人間/AI 編集容易）

```
courses/
  year-id-subject.md      # 科目詳細（Frontmatter + 本文）
dashboard/
  card-id.md              # その他画面のカード定義
themes/
  pack-id.json            # テーマパック
```

**配信**: GitHub Actions → JSON 変換 → Cloudflare（Pages/R2）

- `courses.json` — 科目一覧
- `index.json` — インデックス
- `dashboard.json` — DashboardCard 配列
- `themes.json` — テーマパック配列

**アプリ側**: バージョン確認 → CDN フェッチ → IndexedDB キャッシュ（TTL 24h、stale-while-revalidate）

- スキーマチェック: `schemaVersion` で未知のフィールドを許容
- フォールバック: `static/data/*.json`

### 2. 個人データ（RemoteStorage.js）

ユーザー所有ストレージ:

- `/schedule/` — 履修リスト・カスタム時間割
- `/todos/` — TODO リスト
- `/calendar/` — カレンダーイベント
- `/attendance/` — 出席記録（**5 状態**: 出席 / 欠席 / 遅刻 / 公欠 / **休講**）
- `/user/` — 設定・Preferences・テーマパック・カスタムカラー

ローカル: IndexedDB（RS.js が自動管理）+ Capacitor Preferences（必要時）

### 3. インメモリストア

Svelte 5 Runes ベース:

- `src/lib/stores/theme.svelte.ts` — アクティブテーマ
- `src/lib/stores/settings.svelte.ts` — UserSettings
- `src/lib/stores/weather.svelte.ts` — Open-Meteo キャッシュ
- `src/lib/stores/ui.svelte.ts` — グローバル UI 状態

## テーマパックシステム

`src/lib/theme/` に集約:

```
src/lib/theme/
├── types.ts            # ThemePack 型
├── defaults/           # 4 種類同梱（default, uec, spring, dark）
├── pack-loader.ts      # JSON ロード + バリデーション
├── tokens.ts           # パレット解決
└── css-bridge.ts       # CSS 変数への適用
```

### データ構造

```ts
type ThemePack = {
  id: string
  name: string  // i18n key
  version: 1
  ui: { primary: ColorScale; surface: ...; nav: ...; shadow: ...; radius: ... }
  palette: {
    courseColors: ColorEntry[]   // 7 色 + カスタム
    todoColors: ColorEntry[]     // 6 色
    eventColors: ColorEntry[]    // 10 色
  }
  attendance: {
    present, absent, late, excused, cancelled: { fg, bg }
  }
}
```

### 適用フロー

1. 起動時に `defaults/default.json` を fetch
2. `localStorage` にユーザー選択テーマ ID があればそれをロード
3. `setActiveTheme(id)` で CSS 変数を `<html>` に適用
4. 動的にカスタムパックを追加可能

### ESLint ルール

- `theme-tokens/no-raw-hex` で `style="...#..."` / `bg-[#...]` 禁止
- `src/lib/theme/` 配下のみ例外

## 拡張可能公開データ（DashboardCard）

「その他」画面のカードグリッドを GitHub 経由で配信:

```ts
type DashboardCard =
	| { type: 'html-internal'; id; title; icon; layout; htmlUrl; order }
	| { type: 'external-url'; id; title; icon; url; openInApp; order }
	| { type: 'webview-popup'; id; title; icon; htmlUrl; externalUrl?; order };
```

- `htmlUrl`: HTML をフェッチ → **DOMPurify** でサニタイズ → レンダリング
- `external-url`: アプリ内ブラウザ（`/inapp-browser`）または外部ブラウザで開く
- `webview-popup`: ボトムポップアップ（モバイル）/ 中央モーダル（デスクトップ）

## 出席率計算（休講除外）

`src/lib/attendance.ts`:

```ts
function calculateRate(records: AttendanceRecord[]) {
	const present = records.filter((r) => r.status === 'present').length;
	const absent = records.filter((r) => r.status === 'absent').length;
	const late = records.filter((r) => r.status === 'late').length;
	const excused = records.filter((r) => r.status === 'excused').length;
	const cancelled = records.filter((r) => r.status === 'cancelled').length;

	// 休講は分母から除外
	const total = present + absent + late + excused;
	const effective = present + late * 0.5 + excused;
	const rate = total === 0 ? 0 : (effective / total) * 100;

	return { rate, present, absent, late, excused, cancelled, total };
}
```

## オフライン戦略

- **RemoteStorage**: 自動ローカルキャッシュ + バックグラウンド同期
- **公開データ**: IndexedDB キャッシュ + Service Worker
- **モバイル**: Capacitor Background Runner（将来的強化）

## CI/CD

- **GitHub Actions**: Markdown → JSON 変換 + Cloudflare デプロイ
- **Capacitor**: `pnpm build && pnpm exec cap sync`
- **i18n**: `pnpm exec @inlang/paraglide-js compile`

## テスト

- **Vitest**: ユニットテスト（テーマパック、出席率、DOMPurify、RS クライアント）
- **Playwright**: E2E テスト（テーマ切替、休講登録、その他画面、設定検索など 10 シナリオ）
- **Storybook**: コンポーネントカタログ + ビジュアルリグレッション

## 将来拡張

- **AI エージェント (BYOK)**: 公開 Markdown + 個人データをコンテキストに履修計画生成
- **カレンダー同期**: 現在は RemoteStorage を SSOT（iCal エクスポート対応予定）
- **PWA**: v0.5 で service worker 導入
- **公開データ CDN 実運用**: v1.0 で Cloudflare R2 + Pages

## 技術スタック

- 言語: TypeScript 6 (strict)
- パッケージ: pnpm
- テスト: Vitest + Playwright + Storybook
- その他: Paraglide（i18n）, mdsvex, lucide-svelte, jspdf, DOMPurify, idb-keyval, date-holidays

このドキュメントはアーキテクチャ変更時の参照資料です。開発ルールは [AGENTS.md](AGENTS.md)、利用・セットアップ情報は [README.md](README.md) を参照。
