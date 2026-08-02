/**
 * src/lib/styles/components.ts
 *
 * コンポーネント単位の UI レシピの「中央デフォルト」とリゾルバー。
 *
 * 設計思想:
 * - DEFAULT_COMPONENTS は Apple 風の安全なデフォルト（全テーマで違和感なく動作）。
 * - 各 VisualStylePack は `components` フィールドに「デフォルトと違う部分」だけを記述。
 * - resolveComponents(pack) がパックのオーバーライドをデフォルトにマージして返す。
 * - コンポーネントは常に resolveComponents を経由するため、
 *   新スタイルが一部のキーしか定義しなくても残りはデフォルトが保証される。
 *
 * 色のハードコード禁止: すべて var(--color-*) 等の CSS 変数経由で参照すること。
 */
import type { VisualStylePack, StyleComponents } from './types';

/** hardcoded な rounded-* を除去し、スタイル対応の角丸クラスを付与する */
const RADIUS_CLASS_RE = /\brounded-(none|sm|md|lg|xl|2xl|3xl|full|card|chip)\b/g;
function withStyleRadius(cls: string, radiusClass: string): string {
	const stripped = cls.replace(RADIUS_CLASS_RE, '').replace(/\s{2,}/g, ' ').trim();
	return `${stripped} ${radiusClass}`;
}

/**
 * 中央デフォルト（Apple 風）。全スタイルのフォールバックとして機能する。
 */
export const DEFAULT_COMPONENTS: StyleComponents = {
	button: {
		// レイアウト・フォーカス・無効化。形状(shape)・プレス(press.button)は別途合成
		base: 'inline-flex items-center justify-center gap-2 font-medium select-none whitespace-nowrap focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-[var(--color-primary-500)] focus-visible:ring-offset-2 focus-visible:ring-offset-[var(--color-surface-page)] disabled:opacity-50 disabled:cursor-not-allowed disabled:pointer-events-none',
		shape: 'rounded-chip',
		sm: 'min-h-[44px] h-9 px-3.5 text-xs font-semibold',
		md: 'min-h-[44px] h-11 px-5 text-sm font-semibold',
		lg: 'min-h-[48px] h-12 px-6 text-base font-semibold',
		primary:
			'bg-[var(--color-primary-500)] text-white hover:bg-[var(--color-primary-600)] active:bg-[var(--color-primary-700)] shadow-[var(--shadow-card)] hover:shadow-[var(--shadow-card)]',
		secondary:
			'bg-[var(--color-surface-card)] text-[var(--color-nav-active)] border border-[var(--color-surface-border)]/80 hover:bg-[var(--color-surface-muted)] shadow-[var(--shadow-card)]',
		ghost: 'bg-transparent text-[var(--color-nav-active)] hover:bg-[var(--color-surface-muted)]',
		danger:
			'bg-[var(--color-attendance-absent-bg)] text-[var(--color-attendance-absent-fg)] hover:opacity-90 shadow-[var(--shadow-card)]',
		glass:
			'backdrop-blur-2xl bg-[var(--color-surface-card)]/70 border border-[var(--color-surface-border)]/60 text-[var(--color-nav-active)] hover:bg-[var(--color-surface-card)]/90 hover:shadow-[var(--shadow-card)] shadow-[var(--shadow-card)]'
	},
	input: {
		base: 'block w-full min-h-[44px] rounded-chip border border-[var(--color-surface-border)]/60 bg-[var(--color-surface-card)]/70 backdrop-blur-2xl px-3.5 py-2.5 text-sm text-[var(--color-nav-active)] placeholder-[var(--color-nav-inactive)] shadow-[var(--shadow-card)] transition-all duration-200 focus:border-[var(--color-primary-500)] focus:outline-none focus:ring-2 focus:ring-[var(--color-primary-500)]/20 disabled:opacity-50'
	},
	card: {
		base: 'rounded-card border border-[var(--color-surface-border)]/60 bg-[var(--color-surface-card)] shadow-[var(--shadow-card)]',
		interactive:
			'rounded-card border border-[var(--color-surface-border)]/60 bg-[var(--color-surface-card)] shadow-[var(--shadow-card)] hover:shadow-[var(--shadow-card)] hover:border-[var(--color-primary-500)]/40 transition-all duration-200 cursor-pointer select-none'
	}
};

/**
 * 特定グループのオーバーライドをデフォルトにマージする。
 */
function mergeGroup<K extends keyof StyleComponents>(
	key: K,
	pack: VisualStylePack
): StyleComponents[K] {
	const overrides = pack.components?.[key];
	if (!overrides) return DEFAULT_COMPONENTS[key];
	return { ...DEFAULT_COMPONENTS[key], ...overrides } as StyleComponents[K];
}

/**
 * スタイルパックの components を中央デフォルトとマージして解決する。
 *
 * コンポーネントは必ずこれを経由すること。これにより:
 * - パックが未定義のキーは常に DEFAULT_COMPONENTS が保証される
 * - 新スタイル追加時に全キーの定義が不要（差分だけ記述）
 *
 * @example
 * ```svelte
 * const c = $derived(resolveComponents(styleStore.pack));
 * <button class="{c.button.base} {c.button.shape} {c.button.md} {c.button.primary} {styleStore.pack.press.button}">
 * ```
 */
export function resolveComponents(pack: VisualStylePack): StyleComponents {
	const r = resolveRadius(pack);
	const button = mergeGroup('button', pack);
	const input = mergeGroup('input', pack);
	const card = mergeGroup('card', pack);
	return {
		button,
		input: { ...input, base: withStyleRadius(input.base, r.input) },
		card: {
			...card,
			base: withStyleRadius(card.base, r.card),
			interactive: withStyleRadius(card.interactive, r.card)
		}
	};
}

export interface StyleRadius {
	card: string;
	chip: string;
	pill: string;
	input: string;
}

/**
 * スタイルパックの layout.borderRadius から具体的な角丸クラスを解決する。
 * これにより win95/brutalism は角ゼロ、neumorphism は大きな角丸、というように
 * スタイルごとに角丸が変化する（従来は未使用だった layout.borderRadius を有効化）。
 */
export function resolveRadius(pack: VisualStylePack): StyleRadius {
	switch (pack.layout.borderRadius) {
		case 'none':
			return { card: 'rounded-none', chip: 'rounded-none', pill: 'rounded-none', input: 'rounded-none' };
		case 'sm':
			return { card: 'rounded-sm', chip: 'rounded-sm', pill: 'rounded-full', input: 'rounded-sm' };
		case 'md':
			return { card: 'rounded-md', chip: 'rounded-md', pill: 'rounded-full', input: 'rounded-md' };
		case 'lg':
			return { card: 'rounded-lg', chip: 'rounded-lg', pill: 'rounded-full', input: 'rounded-lg' };
		case '2xl':
			return { card: 'rounded-2xl', chip: 'rounded-xl', pill: 'rounded-full', input: 'rounded-xl' };
		case 'xl':
		default:
			return { card: 'rounded-card', chip: 'rounded-chip', pill: 'rounded-full', input: 'rounded-chip' };
	}
}
