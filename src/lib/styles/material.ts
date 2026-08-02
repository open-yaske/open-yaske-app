/**
 * src/lib/styles/material.ts
 * Material Design 3 (Material You) ビジュアルスタイルパック。
 *
 * 特徴:
 * - Elevation-based shadows（レベル0-5）
 * - ステートレイヤー方式のプレスフィードバック（opacity overlay）
 * - MD3 standard / emphasized easing
 * - ソリッドサーフェス（ガラス効果なし）
 */
import type { VisualStylePack } from './types';

export const materialPack: VisualStylePack = {
	id: 'material',
	name: 'Material Design 3',

	// ──  Press feedback — MD3 は状態レイヤー（opacity overlay）が主流だが、
	//     コードベースのScale統一のため、Scale + opacity の複合にする ──
	press: {
		button:
			'active:scale-[0.98] active:brightness-95 transition-all duration-100 ease-[cubic-bezier(0.2,0,0,1)]',
		icon: 'active:scale-95 active:opacity-80 transition-all duration-100 ease-[cubic-bezier(0.2,0,0,1)]',
		card: 'active:scale-[0.99] active:brightness-[0.97] transition-all duration-100 ease-[cubic-bezier(0.2,0,0,1)]',
		tab: 'active:opacity-70 transition-opacity duration-75 ease-[cubic-bezier(0.2,0,0,1)]',
		glass: 'transition-all duration-150 ease-[cubic-bezier(0.2,0,0,1)]',
		toggle: 'active:scale-95 transition-transform duration-75 ease-[cubic-bezier(0.2,0,0,1)]'
	},

	// ──  Materials — MD3 はソリッドサーフェス。Elevation は shadow で表現 ──
	materials: {
		glass:
			'bg-[var(--color-surface-card)] border border-[var(--color-surface-border)]/50 shadow-material-sm',
		glassHeader:
			'bg-[var(--color-surface-page)]/95 border-b border-[var(--color-surface-border)]/30 shadow-material-sm',
		glassModal:
			'bg-[var(--color-surface-card)] border border-[var(--color-surface-border)]/40 shadow-material-xl',
		card: 'bg-[var(--color-surface-card)] border border-[var(--color-surface-border)]/40 rounded-card shadow-material-sm hover:shadow-material-md transition-all duration-200',
		interactiveCard:
			'bg-[var(--color-surface-card)] border border-[var(--color-surface-border)]/40 rounded-card shadow-material-sm hover:shadow-material-md hover:border-[var(--color-primary-500)]/30 active:scale-[0.99] active:brightness-[0.97] transition-all duration-100 cursor-pointer select-none'
	},

	// ──  Easing — MD3 のトークン ──
	easing: {
		default: 'cubic-bezier(0.2, 0, 0, 1)',
		sheet: 'cubic-bezier(0.05, 0.7, 0.1, 1)',
		bounce: 'cubic-bezier(0.34, 1.56, 0.64, 1)',
		decelerate: 'cubic-bezier(0.05, 0.7, 0.1, 1)',
		reverse: 'cubic-bezier(0.4, 0, 1, 1)'
	},

	// ──  Springs — MD3 は spring-based motion を使わないため省略 ──
	//     （easing のみで motion を表現する）

	// ──  Shadow — MD3 Elevation (レベル 1-4) ──
	shadow: {
		sm: '0 1px 3px rgba(0,0,0,0.12), 0 1px 2px rgba(0,0,0,0.08)',
		md: '0 3px 6px rgba(0,0,0,0.14), 0 2px 4px rgba(0,0,0,0.12)',
		lg: '0 6px 12px rgba(0,0,0,0.16), 0 3px 6px rgba(0,0,0,0.14)',
		xl: '0 8px 18px rgba(0,0,0,0.18), 0 4px 8px rgba(0,0,0,0.14)'
	},

	// ──  Typography — MD3 Type Scale ──
	typography: {
		display: 'leading-[1.1] tracking-[-0.02em] font-optical-sizing-auto',
		heading: 'leading-tight tracking-normal',
		body: 'leading-relaxed tracking-[0.01em]',
		label: 'leading-snug tracking-[0.04em]',
		numeric: '[font-variant-numeric:tabular-nums] [font-feature-settings:"tnum"]'
	},

	// ──  will-change ──
	willChange: {
		transform: 'will-change-transform',
		opacity: 'will-change-opacity',
		both: 'will-change-transform, opacity',
		filter: 'will-change-filter'
	},

	// ──  🆕  Layout tokens ──
	layout: {
		cardSurface: 'solid',
		borderRadius: 'lg',
		borders: true,
		glassOverlay: false,
		headingCase: 'normal',
		dense: false
	},

	// ──  🆕  Pre-computed component tokens ──
	tokens: {
		sectionHeader: 'text-xs font-medium tracking-wide text-[var(--color-nav-inactive)]',
		cardHeader: 'text-base font-semibold',
		pageTitle: 'text-xl font-bold tracking-tight',
		navBar:
			'bg-[var(--color-surface-page)]/95 border-b border-[var(--color-surface-border)]/30 shadow-material-sm'
	},

	// MD3 固有の UI レシピ: フル角丸、トーナルボタン、Elevation シャドウ、ソリッドサーフェス
	components: {
		button: {
			shape: 'rounded-full',
			primary:
				'bg-[var(--color-primary-500)] text-white hover:brightness-95 active:brightness-90 shadow-material-sm hover:shadow-material-md',
			secondary:
				'bg-[var(--color-primary-500)]/10 text-[var(--color-primary-600)] hover:bg-[var(--color-primary-500)]/15 active:brightness-95',
			ghost: 'bg-transparent text-[var(--color-primary-600)] hover:bg-[var(--color-primary-500)]/8 active:bg-[var(--color-primary-500)]/12',
			danger:
				'bg-[var(--color-attendance-absent-bg)] text-[var(--color-attendance-absent-fg)] hover:brightness-95 active:brightness-90',
			glass:
				'bg-[var(--color-surface-card)] text-[var(--color-nav-active)] border border-[var(--color-surface-border)]/40 shadow-material-sm'
		},
		input: {
			base: 'block w-full min-h-[44px] rounded-lg border border-[var(--color-surface-border)]/50 bg-[var(--color-surface-card)] px-3.5 py-2.5 text-sm text-[var(--color-nav-active)] placeholder-[var(--color-nav-inactive)] focus:border-[var(--color-primary-500)] focus:outline-none focus:ring-1 focus:ring-[var(--color-primary-500)]/30 disabled:opacity-50'
		},
		card: {
			base: 'rounded-xl border border-[var(--color-surface-border)]/40 bg-[var(--color-surface-card)] shadow-material-sm',
			interactive:
				'rounded-xl border border-[var(--color-surface-border)]/40 bg-[var(--color-surface-card)] shadow-material-sm hover:shadow-material-md hover:border-[var(--color-primary-500)]/30 active:brightness-[0.97] transition-all duration-100 cursor-pointer select-none'
		}
	}
};
