/**
 * src/lib/styles/high-contrast.ts
 * High Contrast — アクセシビリティ重視のビジュアルスタイル。
 *
 * 設計原則:
 * - 影ゼロ（shadow: none）— 深度は太枠線と色コントラストで表現
 * - 太枠線（border-2）— 境界を明確に認識可能
 * - 最小角丸（rounded-sm）— 装飾を排し認知負荷を低減
 * - ガラス効果なし、ブラーなし、半透明なし
 * - プレスは opacity のみ（スケール・シャドウ変化なし）
 * - linear イージング — 即時・等速の遷移（動きで混乱させない）
 * - spring なし — 物理アニメーション不使用
 * - 太字タイポグラフィ — 見出しは font-bold で視認性最大化
 * - CSS 変数参照 — ライト/ダークテーマ両方に自動追従
 */
import type { VisualStylePack } from './types';

export const highContrastPack: VisualStylePack = {
	id: 'high-contrast',
	name: 'High Contrast',
	description: 'アクセシビリティ重視 — 影なし、太枠、高コントラスト',
	author: 'OpenYASKE',
	version: 1,

	press: {
		button: 'active:opacity-70 transition-opacity duration-75',
		icon: 'active:opacity-60 transition-opacity duration-75',
		card: 'active:opacity-70 transition-opacity duration-75',
		tab: 'active:opacity-50 transition-opacity duration-50',
		glass: 'transition-opacity duration-75',
		toggle: 'active:opacity-60 transition-opacity duration-50'
	},

	materials: {
		glass:
			'bg-[var(--color-surface-card)] border-2 border-[var(--color-surface-border)] rounded-sm',
		glassHeader: 'bg-[var(--color-surface-page)] border-b-2 border-[var(--color-surface-border)]',
		glassModal:
			'bg-[var(--color-surface-card)] border-2 border-[var(--color-surface-border)] rounded-sm',
		card: 'bg-[var(--color-surface-card)] border-2 border-[var(--color-surface-border)] rounded-sm',
		interactiveCard:
			'bg-[var(--color-surface-card)] border-2 border-[var(--color-surface-border)] rounded-sm hover:border-[var(--color-nav-active)] active:opacity-70 transition-all duration-75 cursor-pointer select-none'
	},

	easing: {
		default: 'linear',
		sheet: 'linear',
		bounce: 'linear',
		decelerate: 'linear',
		reverse: 'linear'
	},

	// spring なし — high-contrast は物理アニメーション不使用

	shadow: {
		sm: 'none',
		md: 'none',
		lg: 'none',
		xl: 'none'
	},

	typography: {
		display: 'leading-[1.1] tracking-[-0.01em] font-bold',
		heading: 'leading-tight font-bold',
		body: 'leading-relaxed font-normal',
		label: 'leading-snug font-bold',
		numeric: '[font-variant-numeric:tabular-nums]'
	},

	willChange: {
		transform: 'will-change-transform',
		opacity: 'will-change-opacity',
		both: 'will-change-transform, opacity',
		filter: 'will-change-filter'
	},

	layout: {
		cardSurface: 'solid',
		borderRadius: 'sm',
		borders: true,
		glassOverlay: false,
		headingCase: 'normal',
		dense: false
	},

	tokens: {
		sectionHeader:
			'text-xs font-bold tracking-wide text-[var(--color-nav-active)] underline decoration-2 underline-offset-4 decoration-[var(--color-surface-border)]',
		cardHeader: 'text-base font-bold',
		pageTitle: 'text-xl font-bold tracking-tight',
		navBar: 'bg-[var(--color-surface-page)] border-b-2 border-[var(--color-surface-border)]'
	},

	// High Contrast 固有の UI レシピ: 太枠、影ゼロ、最小角丸、高コントラスト
	components: {
		button: {
			shape: 'rounded-sm',
			primary:
				'bg-[var(--color-primary-500)] text-white border-2 border-[var(--color-primary-700)] hover:bg-[var(--color-primary-600)] active:opacity-70',
			secondary:
				'bg-[var(--color-surface-card)] text-[var(--color-nav-active)] border-2 border-[var(--color-surface-border)] hover:border-[var(--color-nav-active)] active:opacity-70',
			ghost:
				'bg-transparent text-[var(--color-nav-active)] border-2 border-transparent hover:border-[var(--color-surface-border)] active:opacity-60',
			danger:
				'bg-[var(--color-attendance-absent-bg)] text-[var(--color-attendance-absent-fg)] border-2 border-[var(--color-attendance-absent-fg)] hover:opacity-90 active:opacity-70',
			glass:
				'bg-[var(--color-surface-card)] text-[var(--color-nav-active)] border-2 border-[var(--color-surface-border)]'
		},
		input: {
			base: 'block w-full min-h-[44px] rounded-sm border-2 border-[var(--color-surface-border)] bg-[var(--color-surface-card)] px-3.5 py-2.5 text-sm font-medium text-[var(--color-nav-active)] placeholder-[var(--color-nav-inactive)] focus:border-[var(--color-primary-500)] focus:outline-none disabled:opacity-50'
		},
		card: {
			base: 'rounded-sm border-2 border-[var(--color-surface-border)] bg-[var(--color-surface-card)]',
			interactive:
				'rounded-sm border-2 border-[var(--color-surface-border)] bg-[var(--color-surface-card)] hover:border-[var(--color-nav-active)] active:opacity-70 transition-all duration-75 cursor-pointer select-none'
		},
		sidenav: {
			activeItem:
				'bg-[var(--color-nav-active)] text-[var(--color-surface-page)] font-bold border-2 border-[var(--color-nav-active)] rounded-sm',
			inactiveItem:
				'text-[var(--color-nav-active)] font-medium border-2 border-transparent hover:border-[var(--color-nav-active)]'
		}
	}
};
