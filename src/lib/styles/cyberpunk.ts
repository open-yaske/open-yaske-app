/**
 * src/lib/styles/cyberpunk.ts
 * Cyberpunk / Neon — 暗闇に浮かぶネオン発光とグリッチ。
 *
 * 設計原則:
 * - primary 色を使ったネオン発光シャドウ（box-shadow で glow）
 * - 2px solid ボーダー（primary 色に依存、半透明）
 * - 暗いサーフェス + 発光する境界線
 * - プレス時は発光が一瞬強くなる（ネオンのフラッシュ）
 * - ワイドトラッキング + 大文字ラベル（テック感）
 * - グリッチ風イージング（スナップ感のある cubic-bezier）
 * - cardSurface: 'glass' — 半透明 + 発光の組み合わせ
 */
import type { VisualStylePack } from './types';

export const cyberpunkPack: VisualStylePack = {
	id: 'cyberpunk',
	name: 'Cyberpunk',
	description: 'ネオン発光とグリッチ — 暗闇に浮かぶサイバーパンク UI',
	author: 'OpenYASKE',
	version: 4,

	press: {
		button:
			'active:shadow-[0_0_8px_var(--color-primary-400),0_0_20px_var(--color-primary-400),0_0_40px_var(--color-primary-400)] active:scale-[0.96] transition-all duration-100',
		icon: 'active:shadow-[0_0_10px_var(--color-primary-400),0_0_20px_var(--color-primary-400)] active:scale-[0.85] transition-all duration-100',
		card: 'hover:shadow-cyber-md active:shadow-[0_0_12px_var(--color-primary-400),0_0_30px_var(--color-primary-400)] active:scale-[0.98] transition-all duration-150',
		tab: 'active:shadow-[0_0_8px_var(--color-primary-400)] active:opacity-70 transition-all duration-75',
		glass: 'transition-all duration-150',
		toggle: 'active:shadow-[0_0_10px_var(--color-primary-400)] active:scale-90 transition-all duration-75'
	},

	materials: {
		glass: 'bg-[var(--color-surface-card)]/85 backdrop-blur-sm border-2 border-[var(--color-primary-500)]/40 rounded-lg shadow-cyber-sm',
		glassHeader:
			'bg-[var(--color-surface-page)]/90 backdrop-blur-sm border-b-2 border-[var(--color-primary-500)]/30 shadow-cyber-sm',
		glassModal:
			'bg-[var(--color-surface-card)]/90 backdrop-blur-md border-2 border-[var(--color-primary-500)]/50 rounded-xl shadow-cyber-xl',
		card: 'bg-[var(--color-surface-card)]/85 backdrop-blur-sm border-2 border-[var(--color-primary-500)]/25 rounded-lg shadow-cyber-sm hover:shadow-cyber-md hover:border-[var(--color-primary-400)]/50 transition-all duration-200',
		interactiveCard:
			'bg-[var(--color-surface-card)]/85 backdrop-blur-sm border-2 border-[var(--color-primary-500)]/25 rounded-lg shadow-cyber-sm hover:shadow-cyber-lg hover:border-[var(--color-primary-400)]/60 active:shadow-[0_0_12px_var(--color-primary-400),0_0_30px_var(--color-primary-400)] active:scale-[0.98] transition-all duration-150 cursor-pointer select-none'
	},

	easing: {
		default: 'cubic-bezier(0.6, 0.0, 0.4, 1.0)',
		sheet: 'cubic-bezier(0.6, 0.0, 0.4, 1.0)',
		bounce: 'cubic-bezier(0.34, 1.56, 0.64, 1)',
		decelerate: 'cubic-bezier(0.6, 0.0, 0.4, 1.0)',
		reverse: 'cubic-bezier(0.6, 0.0, 0.4, 1.0)'
	},

	spring: {
		default: { damping: 0.85, response: 0.3 },
		sheet: { damping: 0.75, response: 0.25 },
		modal: { damping: 0.85, response: 0.25 },
		menu: { damping: 0.8, response: 0.25 },
		flick: { damping: 0.75, response: 0.3 },
		rotation: { damping: 0.75, response: 0.35 },
		pinch: { damping: 0.8, response: 0.2 }
	},

	shadow: {
		sm: '0 0 6px var(--color-primary-400), 0 0 12px var(--color-primary-400)',
		md: '0 0 10px var(--color-primary-400), 0 0 24px var(--color-primary-400)',
		lg: '0 0 16px var(--color-primary-400), 0 0 36px var(--color-primary-400), 0 0 60px var(--color-primary-400)',
		xl: '0 0 24px var(--color-primary-400), 0 0 48px var(--color-primary-400), 0 0 96px var(--color-primary-400)'
	},

	typography: {
		display: 'leading-[1.05] tracking-[-0.02em] font-bold',
		heading: 'leading-tight tracking-[0.02em] font-semibold',
		body: 'leading-relaxed tracking-[0.01em]',
		label: 'leading-snug tracking-[0.08em] uppercase font-semibold',
		numeric: '[font-variant-numeric:tabular-nums] [font-feature-settings:"tnum"]'
	},

	willChange: {
		transform: 'will-change-transform',
		opacity: 'will-change-opacity',
		both: 'will-change-transform, opacity',
		filter: 'will-change-filter'
	},

	layout: {
		cardSurface: 'glass',
		borderRadius: 'lg',
		borders: true,
		glassOverlay: false,
		headingCase: 'normal',
		dense: false
	},

	tokens: {
		sectionHeader:
			'text-xs font-bold tracking-[0.15em] uppercase text-[var(--color-primary-400)] [text-shadow:0_0_8px_var(--color-primary-400)]',
		cardHeader: 'text-base font-bold tracking-wide',
		pageTitle: 'text-xl font-bold tracking-tight',
		navBar: 'bg-[var(--color-surface-page)]/95 backdrop-blur-sm border-b-2 border-[var(--color-primary-500)]/40 shadow-cyber-md'
	},

	// Cyberpunk 固有の UI レシピ: ネオン発光枠、半透明
	components: {
		button: {
			shape: 'rounded-lg',
			primary:
				'bg-[var(--color-primary-500)]/25 text-[var(--color-primary-300)] border border-[var(--color-primary-400)]/60 shadow-cyber-sm hover:shadow-cyber-md hover:bg-[var(--color-primary-500)]/35 hover:border-[var(--color-primary-400)] active:shadow-[0_0_12px_var(--color-primary-400),0_0_30px_var(--color-primary-400)]',
			secondary:
				'bg-[var(--color-surface-card)]/80 text-[var(--color-nav-active)] border border-[var(--color-primary-500)]/30 shadow-cyber-sm hover:shadow-cyber-md',
			ghost: 'bg-transparent text-[var(--color-primary-400)] hover:bg-[var(--color-primary-500)]/10',
			danger:
				'bg-[var(--color-attendance-absent-bg)]/20 text-[var(--color-attendance-absent-bg)] border border-[var(--color-attendance-absent-bg)]/50',
			glass:
				'backdrop-blur-sm bg-[var(--color-surface-card)]/80 text-[var(--color-nav-active)] border border-[var(--color-primary-500)]/40 shadow-cyber-sm'
		},
		input: {
			base: 'block w-full min-h-[44px] rounded-lg border border-[var(--color-primary-500)]/30 bg-[var(--color-surface-card)]/80 backdrop-blur-sm px-3.5 py-2.5 text-sm text-[var(--color-nav-active)] placeholder-[var(--color-nav-inactive)] shadow-cyber-sm focus:border-[var(--color-primary-400)] focus:shadow-cyber-md focus:outline-none disabled:opacity-50'
		},
		card: {
			base: 'rounded-lg border border-[var(--color-primary-500)]/40 bg-[var(--color-surface-card)]/85 backdrop-blur-sm shadow-cyber-sm',
			interactive:
				'rounded-lg border border-[var(--color-primary-500)]/40 bg-[var(--color-surface-card)]/85 backdrop-blur-sm shadow-cyber-sm hover:shadow-cyber-lg hover:border-[var(--color-primary-400)]/70 transition-all duration-200 cursor-pointer select-none'
		}
	}
};
