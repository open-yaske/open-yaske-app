/**
 * src/lib/styles/flat.ts
 * Flat Design — 影・グラデーション・装飾を一切排除したミニマル UI。
 *
 * 設計原則:
 * - 影ゼロ（shadow: none）— 深度は色と枠線だけで表現
 * - 枠線は細いが明確（border, 低opacity）
 * - 角は小 R（rounded-md）— 完全スクエアではなく最小限の丸み
 * - ガラス効果なし、グラデーションなし、ブラーなし
 * - プレスは opacity のみ（スケール・シャドウ変化なし）
 * - linear イージング — 即時・等速の遷移
 * - ミニマルだが退屈ではない: 明確な色コントラスト、クリーンなタイポグラフィ
 */
import type { VisualStylePack } from './types';

export const flatPack: VisualStylePack = {
	id: 'flat',
	name: 'Flat',
	description: '影ゼロ・装飾ゼロ — 色と枠線だけのミニマル UI',
	author: 'OpenYASKE',
	version: 3,

	press: {
		button: 'active:opacity-60 transition-opacity duration-75',
		icon: 'active:opacity-50 transition-opacity duration-75',
		card: 'active:opacity-70 transition-opacity duration-75',
		tab: 'active:opacity-40 transition-opacity duration-50',
		glass: 'transition-opacity duration-75',
		toggle: 'active:opacity-50 transition-opacity duration-50'
	},

	materials: {
		glass: 'bg-[var(--color-surface-card)] border border-[var(--color-surface-border)]/40 rounded-md',
		glassHeader: 'bg-[var(--color-surface-page)] border-b border-[var(--color-surface-border)]/30',
		glassModal: 'bg-[var(--color-surface-card)] border border-[var(--color-surface-border)]/40 rounded-md',
		card: 'bg-[var(--color-surface-card)] border border-[var(--color-surface-border)]/30 rounded-md',
		interactiveCard:
			'bg-[var(--color-surface-card)] border border-[var(--color-surface-border)]/30 rounded-md hover:border-[var(--color-surface-border)]/60 active:opacity-70 transition-all duration-75 cursor-pointer select-none'
	},

	easing: {
		default: 'linear',
		sheet: 'linear',
		bounce: 'linear',
		decelerate: 'linear',
		reverse: 'linear'
	},

	shadow: {
		sm: 'none',
		md: 'none',
		lg: 'none',
		xl: 'none'
	},

	typography: {
		display: 'leading-[1.1] tracking-[-0.01em] font-semibold',
		heading: 'leading-tight font-medium',
		body: 'leading-relaxed font-normal',
		label: 'leading-snug font-medium',
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
		borderRadius: 'md',
		borders: true,
		glassOverlay: false,
		headingCase: 'normal',
		dense: false
	},

	tokens: {
		sectionHeader: 'text-xs font-medium tracking-wide text-[var(--color-nav-inactive)]',
		cardHeader: 'text-base font-medium',
		pageTitle: 'text-xl font-semibold tracking-tight',
		navBar: 'bg-[var(--color-surface-page)] border-b border-[var(--color-surface-border)]/30'
	},

	// Flat 固有の UI レシピ: 影ゼロ、細枠、opacity プレス
	components: {
		button: {
			shape: 'rounded-md',
			primary:
				'bg-[var(--color-primary-500)] text-white hover:bg-[var(--color-primary-600)] active:opacity-60',
			secondary:
				'bg-[var(--color-surface-muted)] text-[var(--color-nav-active)] hover:bg-[var(--color-surface-border)]/50 active:opacity-70',
			ghost: 'bg-transparent text-[var(--color-nav-active)] hover:bg-[var(--color-surface-muted)] active:opacity-60',
			danger:
				'bg-[var(--color-attendance-absent-bg)] text-[var(--color-attendance-absent-fg)] hover:opacity-90 active:opacity-60',
			glass:
				'bg-[var(--color-surface-card)] text-[var(--color-nav-active)] border border-[var(--color-surface-border)]/40'
		},
		input: {
			base: 'block w-full min-h-[44px] rounded-md border border-[var(--color-surface-border)]/40 bg-[var(--color-surface-card)] px-3.5 py-2.5 text-sm text-[var(--color-nav-active)] placeholder-[var(--color-nav-inactive)] focus:border-[var(--color-primary-500)] focus:outline-none disabled:opacity-50'
		},
		card: {
			base: 'rounded-md border border-[var(--color-surface-border)]/30 bg-[var(--color-surface-card)]',
			interactive:
				'rounded-md border border-[var(--color-surface-border)]/30 bg-[var(--color-surface-card)] hover:border-[var(--color-surface-border)]/60 active:opacity-70 transition-all duration-75 cursor-pointer select-none'
		}
	}
};
