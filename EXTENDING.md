# OpenYASKE 拡張・開発ガイド (EXTENDING.md)

本ドキュメントは、開発者が OpenYASKE アプリケーションに新機能を追加したり、既存のデータ構造や設定をカスタマイズ・拡張するためのガイドラインです。

---

## 1. アプリケーション設計原則

本プロジェクトでは以下の技術スタックと設計原則を厳格に適用しています。

- **Svelte 5 (Runes) 強制**: コンポーネントおよびリアクティブな状態管理は、すべて `$state`, `$derived`, `$props`, `$effect` などの Runes のみを使用してください。旧来の `let` 定義による暗黙的なリアクティブや `$:` は使用禁止です。
- **Local-First & Offline-First**: 個人データは RemoteStorage.js を介してローカル (IndexedDB) で管理し、完全オフラインでも動作するように設計します。ネットワーク処理は同期のためのみに用いられます。
- **多言語対応 (i18n)**: 画面上のテキストはすべて Paraglide.js で定義し、ハードコードを排除します。
- **CSS変数によるテーマ適用 (HEX直書き禁止)**: `theme-tokens/no-raw-hex` ESLint ルールにより、テーマパックファイル (`src/lib/theme/**`) を除く全ソースコードでの HEX 色値（`#ffffff` 等）の直書きを禁止しています。色はすべて `var(--color-primary-500)` などの CSS 変数を経由してください。

---

## 2. 新規画面・機能 (Feature) の追加手順

OpenYASKE は「モジュラー機能配置アーキテクチャ」を採用しています。各主要機能は `Feature` として定義され、ユーザー設定に応じてナビゲーションバーやダッシュボードに動的に配置可能です。

### 手順 1: Feature の定義を追加

[src/lib/features.ts](file:///data/open-nexus/src/lib/features.ts) の `FEATURES` 配列に、新規機能の定義を追加します。

```ts
export const FEATURES: Feature[] = [
	// ...既存の機能...
	{
		id: 'my-feature',
		name: 'my_feature_title', // i18nのキー名
		icon: 'Sparkles', // Lucideアイコン名
		route: '/my-feature',
		widgetSupported: true // ホーム画面のミニウィジェットに対応するかどうか
	}
];
```

### 手順 2: ルーティングとページの作成

`src/routes/` 配下に定義した `route` パスに対応するディレクトリとページを作成します。

- `src/routes/my-feature/+page.svelte`

### 手順 3: メニューや設定への表示

必要に応じて、[src/lib/components/SideNav.svelte](file:///data/open-nexus/src/lib/components/SideNav.svelte) や下部バーなどに表示されるアイコン・ナビゲーションの調整を行います。

---

## 3. 多言語メッセージ (i18n) の追加・修正手順

本プロジェクトでは `@inlang/paraglide-js` を使用しています。

### 手順 1: json ファイルにキーを定義

以下の4つのメッセージファイルすべてに、全く同じキーを追加します。

1. [messages/ja.json](file:///data/open-nexus/messages/ja.json) (ベース言語・日本語)
2. [messages/en.json](file:///data/open-nexus/messages/en.json) (英語)
3. [messages/ko.json](file:///data/open-nexus/messages/ko.json) (韓国語)
4. [messages/zh.json](file:///data/open-nexus/messages/zh.json) (中国語 - 簡体字)

> [!IMPORTANT]
> すべての言語ファイルでキーの過不足がないよう完全に一致させてください。

### 手順 2: コンパイルの実行

以下のコマンドを実行し、翻訳定義を型安全な JavaScript コードにビルドします。

```bash
pnpm exec paraglide-js compile
```

### 手順 3: コードからの参照

自動生成された `$lib/paraglide/messages` からインポートして使用します。

```ts
import { m } from '$lib/paraglide/messages';

// プレーンな使用例
console.log(m.my_feature_title());

// パラメータ付きの使用例 (JSON側: "hello": "こんにちは、{name}さん")
console.log(m.hello({ name: 'ユーザー' }));
```

---

## 4. 個人データ (RemoteStorage) スキーマの拡張

個人データストレージのスキーマを変更または新規追加する場合の手順です。

### 手順 1: 型定義の更新

[src/lib/types.ts](file:///data/open-nexus/src/lib/types.ts) に、保存するデータの TypeScript 型を追加・修正します。

### 手順 2: RemoteStorage クライアントでの宣言

[src/lib/remotestorage.ts](file:///data/open-nexus/src/lib/remotestorage.ts) 内で、新規データパスのタイプおよびメソッド（`rsGet`, `rsSet` 等）のラッパーを定義・宣言します。

```ts
// 起動時の declareType への追加
client.declareType('my-data', {
	type: 'object',
	properties: {
		id: { type: 'string' },
		content: { type: 'string' },
		updatedAt: { type: 'number' }
	}
});
```

---

## 5. パフォーマンス向上と軽量化の設計

アプリの起動時間やパッケージ容量を最適に保つため、以下のルールを遵守してください。

- **重いライブラリの動的インポート化**:
  PDF出力 (`jspdf`) や数式描画 (`katex`)、図表描画 (`mermaid`) などの重いモジュールは、ページの初期ロード時に静的インポートせず、ユーザーがその機能を呼び出すタイミングで `await import('module')` を使って動的インポートしてください。
- **祝日データの内製化**:
  祝日計算など、自前で計算可能な軽量ロジックで代替できる場合は、巨大な多国籍データベースを持つ外部パッケージを安易にインストールせず、[src/lib/holidays.ts](file:///data/open-nexus/src/lib/holidays.ts) のような内製計算モジュールを活用してください。

---

## 6. テストの作成と実行

変更を加えたら、必ずテストを作成または更新して品質を担保します。

### 単体テスト (Unit Tests)

ロジックや非同期サービス、状態管理のテストは Vitest で実行します。

- テストファイル名: `src/lib/**/*.spec.ts` または `src/lib/**/*.test.ts`
- 実行コマンド:
  ```bash
  pnpm run test:unit
  ```

### E2Eテスト (End-to-End Tests)

画面遷移や一連のユーザーフロー、UI の統合テストは Playwright で実行します。

- テストファイル名: `src/routes/**/*.e2e.ts`
- 実行コマンド:
  ```bash
  pnpm run test:e2e
  ```

### Storybook (コンポーネントテスト)

UI プリミティブコンポーネントを追加した場合は、Storybook のストーリーを作成します。

- 実行コマンド:
  ```bash
  pnpm run storybook
  ```
