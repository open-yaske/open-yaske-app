/**
 * src/lib/styles/winxp.ts
 * Windows XP (Luna) — 青いグラデーションと丸みの親しみやすいデスクトップ。
 *
 * 設計原則:
 * - タイトルバー: 鮮やかな青グラデーション + 白文字
 * - ウィンドウ本体: 上から下への緩やかなグラデーション（白 → 薄灰）
 * - 角は小 R（rounded-lg）— Win95 より柔らかいが現代的ではない
 * - ボタン: 緩やかなグラデーション + 押下時にわずかに沈む
 * - 控えめな影（ウィンドウの浮遊感）
 * - Tahoma 8pt 風のタイポグラフィ
 */
import type { VisualStylePack } from './types';

export const winXpPack: VisualStylePack = {
	id: 'winxp',
	name: 'Windows XP',
	description: 'Luna テーマ — 青いグラデーションの親しみやすいデスクトップ',
	author: 'OpenYASKE',
	version: 5,

	press: {
		button:
			'active:brightness-[0.9] active:shadow-[inset_1px_1px_2px_rgba(0,0,0,0.15)] transition-all duration-75',
		icon: 'active:brightness-[0.85] active:scale-95 transition-all duration-75',
		card: 'active:brightness-[0.95] active:shadow-[inset_1px_1px_3px_rgba(0,0,0,0.1)] transition-all duration-100',
		tab: 'active:opacity-60 transition-opacity duration-50',
		glass: 'transition-all duration-100',
		toggle: 'active:brightness-[0.9] transition-all duration-50'
	},

	materials: {
		glass:
			'bg-gradient-to-b from-[var(--color-surface-card)] to-[var(--color-surface-muted)]/60 border border-[var(--color-primary-500)]/30 rounded-lg shadow-[0_1px_4px_rgba(0,0,0,0.12)]',
		glassHeader:
			'bg-gradient-to-r from-[var(--color-primary-700)] via-[var(--color-primary-500)] to-[var(--color-primary-400)] text-white font-bold rounded-t-lg border border-[var(--color-primary-800)]/50 shadow-[0_1px_3px_rgba(0,0,0,0.25)]',
		glassModal:
			'bg-gradient-to-b from-[var(--color-surface-card)] to-[var(--color-surface-muted)]/60 border border-[var(--color-primary-500)]/40 rounded-lg shadow-[0_4px_16px_rgba(0,0,0,0.2)]',
		card: 'bg-gradient-to-b from-[var(--color-surface-card)] to-[var(--color-surface-muted)]/50 border border-[var(--color-primary-500)]/40 rounded-lg shadow-[0_1px_4px_rgba(0,0,0,0.12)] hover:shadow-[0_2px_8px_rgba(0,0,0,0.16)] transition-all duration-150',
		interactiveCard:
			'bg-gradient-to-b from-[var(--color-surface-card)] to-[var(--color-surface-muted)]/50 border border-[var(--color-primary-500)]/40 rounded-lg shadow-[0_1px_4px_rgba(0,0,0,0.12)] hover:border-[var(--color-primary-500)]/60 hover:shadow-[0_2px_8px_rgba(0,0,0,0.16)] active:brightness-[0.95] active:shadow-[inset_1px_1px_3px_rgba(0,0,0,0.12)] transition-all duration-100 cursor-pointer select-none'
	},

	easing: {
		default: 'cubic-bezier(0.25, 0.1, 0.25, 1)',
		sheet: 'cubic-bezier(0.25, 0.1, 0.25, 1)',
		bounce: 'cubic-bezier(0.34, 1.56, 0.64, 1)',
		decelerate: 'ease-out',
		reverse: 'ease-in'
	},

	shadow: {
		sm: '0 1px 4px rgba(0,0,0,0.1)',
		md: '0 2px 8px rgba(0,0,0,0.12)',
		lg: '0 4px 12px rgba(0,0,0,0.15)',
		xl: '0 6px 16px rgba(0,0,0,0.18)'
	},

	typography: {
		display: 'leading-[1.15] tracking-normal font-bold',
		heading: 'leading-tight tracking-[0.01em] font-bold',
		body: 'leading-relaxed tracking-normal',
		label: 'leading-snug tracking-[0.02em] font-semibold',
		numeric: '[font-variant-numeric:tabular-nums]'
	},

	willChange: {
		transform: 'will-change-transform',
		opacity: 'will-change-opacity',
		both: 'will-change-transform, opacity',
		filter: 'will-change-filter'
	},

	layout: {
		cardSurface: 'gradient',
		borderRadius: 'lg',
		borders: true,
		glassOverlay: false,
		headingCase: 'normal',
		dense: false
	},

	tokens: {
		sectionHeader: 'text-xs font-bold tracking-wide text-[var(--color-primary-600)]',
		cardHeader: 'text-sm font-bold',
		pageTitle: 'text-lg font-bold tracking-tight',
		navBar:
			'bg-gradient-to-r from-[var(--color-primary-700)] via-[var(--color-primary-500)] to-[var(--color-primary-400)] text-white font-bold border-t border-[var(--color-primary-800)]/50'
	},

	// WinXP 固有の UI レシピ: グラデーション、青枠、控えめな影
	components: {
		button: {
			shape: 'rounded-lg',
			primary:
				'bg-gradient-to-b from-[var(--color-primary-400)] to-[var(--color-primary-600)] text-white font-bold border border-[var(--color-primary-800)]/60 shadow-[0_1px_3px_rgba(0,0,0,0.15),inset_0_1px_0_rgba(255,255,255,0.3)] hover:from-[var(--color-primary-300)] hover:to-[var(--color-primary-500)] active:from-[var(--color-primary-600)] active:to-[var(--color-primary-700)] active:shadow-[inset_0_2px_3px_rgba(0,0,0,0.2)]',
			secondary:
				'bg-gradient-to-b from-[var(--color-surface-card)] to-[var(--color-surface-muted)]/70 text-[var(--color-nav-active)] border border-[var(--color-surface-border)]/70 shadow-[0_1px_2px_rgba(0,0,0,0.1),inset_0_1px_0_rgba(255,255,255,0.5)] hover:border-[var(--color-primary-500)]/50 hover:shadow-[0_2px_5px_rgba(0,0,0,0.14)] active:shadow-[inset_0_2px_3px_rgba(0,0,0,0.15)]',
			ghost: 'bg-transparent text-[var(--color-nav-active)] hover:bg-[var(--color-surface-muted)]',
			danger:
				'bg-gradient-to-b from-[var(--color-attendance-absent-bg)] to-[var(--color-attendance-absent-bg)]/80 text-[var(--color-attendance-absent-fg)] border border-[var(--color-attendance-absent-bg)]/60 active:brightness-[0.9]',
			glass:
				'bg-gradient-to-b from-[var(--color-surface-card)] to-[var(--color-surface-muted)]/60 text-[var(--color-nav-active)] border border-[var(--color-primary-500)]/30 shadow-[0_1px_4px_rgba(0,0,0,0.12)]'
		},
		input: {
			base: 'block w-full min-h-[44px] rounded-lg border border-[var(--color-surface-border)]/60 bg-[var(--color-surface-card)] px-3 py-2 text-sm text-[var(--color-nav-active)] placeholder-[var(--color-nav-inactive)] shadow-[inset_1px_1px_2px_rgba(0,0,0,0.06)] focus:border-[var(--color-primary-500)] focus:outline-none disabled:opacity-50'
		},
		card: {
			base: 'rounded-lg border border-[var(--color-primary-500)]/40 bg-gradient-to-b from-[var(--color-surface-card)] to-[var(--color-surface-muted)]/50 shadow-[0_1px_4px_rgba(0,0,0,0.12)]',
			interactive:
				'rounded-lg border border-[var(--color-primary-500)]/40 bg-gradient-to-b from-[var(--color-surface-card)] to-[var(--color-surface-muted)]/50 shadow-[0_1px_4px_rgba(0,0,0,0.12)] hover:border-[var(--color-primary-500)]/60 hover:shadow-[0_2px_8px_rgba(0,0,0,0.16)] active:shadow-[inset_0_2px_3px_rgba(0,0,0,0.12)] transition-all duration-100 cursor-pointer select-none'
		},
		sidenav: {
			activeItem:
				'bg-gradient-to-b from-[var(--color-primary-400)] to-[var(--color-primary-600)] text-white font-bold border border-[var(--color-primary-800)]/60 shadow-[0_1px_3px_rgba(0,0,0,0.15),inset_0_1px_0_rgba(255,255,255,0.3)]',
			inactiveItem: 'text-[var(--color-nav-active)] hover:bg-[var(--color-primary-500)]/10'
		}
	}
};
