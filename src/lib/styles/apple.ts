/**
 * src/lib/styles/apple.ts
 * Apple Design Guidelines 準拠のビジュアルスタイルパック。
 *
 * このファイルは Apple スタイル単体の定義であり、
 * システム全体のスタイル選択は src/lib/stores/style.svelte.ts で行う。
 */
import type { VisualStylePack } from './types';

export const applePack: VisualStylePack = {
	id: 'apple',
	name: 'Apple',

	// ── §1  Response — 即時フィードバック ──
	press: {
		button:
			'active:scale-[0.97] transition-all duration-150 ease-[cubic-bezier(0.16,1,0.3,1)]',
		icon: 'active:scale-90 transition-all duration-150 ease-[cubic-bezier(0.16,1,0.3,1)]',
		card: 'active:scale-[0.985] transition-all duration-200 ease-[cubic-bezier(0.16,1,0.3,1)]',
		tab: 'active:scale-95 transition-transform duration-150 ease-[cubic-bezier(0.16,1,0.3,1)]',
		glass:
			'transition-all duration-200 ease-[cubic-bezier(0.16,1,0.3,1)]',
		toggle:
			'active:scale-95 transition-transform duration-100 ease-[cubic-bezier(0.16,1,0.3,1)]'
	},

	// ── §12  Materials — すりガラス / サーフェス ──
	materials: {
		glass:
			'backdrop-blur-2xl bg-[var(--color-surface-card)]/80 border border-[var(--color-surface-border)]/60',
		glassHeader:
			'backdrop-blur-3xl bg-[var(--color-surface-page)]/80 border-b border-[var(--color-surface-border)]/50',
		glassModal:
			'backdrop-blur-3xl bg-[var(--color-surface-card)]/90 border border-[var(--color-surface-border)]/80 shadow-apple-xl',
		card:
			'bg-[var(--color-surface-card)]/80 backdrop-blur-2xl border border-[var(--color-surface-border)]/60 rounded-card shadow-apple-sm hover:shadow-apple-md transition-all duration-200',
		interactiveCard:
			'bg-[var(--color-surface-card)]/80 backdrop-blur-2xl border border-[var(--color-surface-border)]/60 rounded-card shadow-apple-sm hover:shadow-apple-md hover:border-[var(--color-primary-500)]/40 active:scale-[0.985] transition-all duration-200 cursor-pointer select-none'
	},

	// ── §4  Easing ──
	easing: {
		default: 'cubic-bezier(0.16, 1, 0.3, 1)',
		sheet: 'cubic-bezier(0.32, 0.72, 0, 1)',
		bounce: 'cubic-bezier(0.34, 1.56, 0.64, 1)',
		decelerate: 'cubic-bezier(0, 0, 0.2, 1)',
		reverse: 'cubic-bezier(0.7, 0, 0.84, 0)'
	},

	// ── §4  Springs ──
	spring: {
		default: { damping: 1.0, response: 0.35 },
		sheet: { damping: 0.8, response: 0.3 },
		modal: { damping: 1.0, response: 0.3 },
		menu: { damping: 0.85, response: 0.3 },
		flick: { damping: 0.6, response: 0.35 },
		rotation: { damping: 0.8, response: 0.4 },
		pinch: { damping: 0.85, response: 0.25 }
	},

	// ── §12  Shadow depth ──
	// layout.css の .shadow-apple-* フォールバック値と一致させる
	shadow: {
		sm: '0 2px 8px rgba(0,0,0,0.04), 0 1px 2px rgba(0,0,0,0.02)',
		md: '0 4px 16px rgba(0,0,0,0.08), 0 2px 4px rgba(0,0,0,0.03)',
		lg: '0 12px 32px rgba(0,0,0,0.12), 0 4px 12px rgba(0,0,0,0.05)',
		xl: '0 24px 48px rgba(0,0,0,0.16), 0 8px 16px rgba(0,0,0,0.08)'
	},

	// ── §15  Typography ──
	typography: {
		display:
			'leading-[1.05] tracking-[-0.02em] font-optical-sizing-auto',
		heading: 'leading-tight tracking-tight',
		body: 'leading-relaxed tracking-normal',
		label: 'leading-snug tracking-wide',
		numeric:
			'[font-variant-numeric:tabular-nums] [font-feature-settings:"tnum"]'
	},

	// ── §11  will-change ──
	willChange: {
		transform: 'will-change-transform',
		opacity: 'will-change-opacity',
		both: 'will-change-transform, opacity',
		filter: 'will-change-filter',
	},

	// ──  🆕  Layout tokens ──
	layout: {
		cardSurface: 'glass',
		borderRadius: 'xl',
		borders: true,
		glassOverlay: true,
		headingCase: 'normal',
		dense: false,
	},

	// ──  🆕  Pre-computed component tokens ──
	tokens: {
		sectionHeader: 'text-xs font-semibold uppercase tracking-widest text-[var(--color-nav-inactive)]',
		cardHeader: 'text-base font-semibold',
		pageTitle: 'text-xl font-bold tracking-tight',
		navBar: 'backdrop-blur-3xl bg-[var(--color-surface-page)]/80 border-b border-[var(--color-surface-border)]/50',
	},

	// Apple 固有の UI レシピ: すりガラス（frosted glass）・チップ角丸・Apple シャドウ
	components: {
		button: {
			shape: 'rounded-chip',
			primary:
				'bg-[var(--color-primary-500)] text-white hover:bg-[var(--color-primary-600)] active:bg-[var(--color-primary-700)] shadow-apple-sm hover:shadow-apple-md',
			secondary:
				'backdrop-blur-xl bg-[var(--color-surface-card)]/70 text-[var(--color-nav-active)] border border-[var(--color-surface-border)]/60 hover:bg-[var(--color-surface-card)]/90 shadow-apple-sm',
			ghost: 'bg-transparent text-[var(--color-nav-active)] hover:bg-[var(--color-surface-muted)]',
			danger:
				'bg-[var(--color-attendance-absent-bg)] text-[var(--color-attendance-absent-fg)] hover:opacity-90 shadow-apple-sm',
			glass:
				'backdrop-blur-2xl bg-[var(--color-surface-card)]/70 border border-[var(--color-surface-border)]/60 text-[var(--color-nav-active)] hover:bg-[var(--color-surface-card)]/90 hover:shadow-apple-md shadow-apple-sm'
		},
		input: {
			base: 'block w-full min-h-[44px] rounded-chip border border-[var(--color-surface-border)]/60 bg-[var(--color-surface-card)]/70 backdrop-blur-2xl px-3.5 py-2.5 text-sm text-[var(--color-nav-active)] placeholder-[var(--color-nav-inactive)] shadow-apple-sm transition-all duration-200 focus:border-[var(--color-primary-500)] focus:outline-none focus:ring-2 focus:ring-[var(--color-primary-500)]/20 disabled:opacity-50'
		},
		card: {
			base: 'rounded-card border border-[var(--color-surface-border)]/60 bg-[var(--color-surface-card)]/80 backdrop-blur-2xl shadow-apple-sm',
			interactive:
				'rounded-card border border-[var(--color-surface-border)]/60 bg-[var(--color-surface-card)]/80 backdrop-blur-2xl shadow-apple-sm hover:shadow-apple-md hover:border-[var(--color-primary-500)]/40 transition-all duration-200 cursor-pointer select-none'
		}
	},
};
