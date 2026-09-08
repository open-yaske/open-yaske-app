/**
 * src/lib/styles/neumorphism.ts
 * Neumorphism (Soft UI) — 背景と同一トーンのサーフェスを凹凸シャドウで浮かび上がらせる。
 *
 * 設計原則:
 * - 光源は左上: 明るいハイライト（左上）+ 暗いシャドウ（右下）の二重影
 * - 背景色と同一トーンのサーフェス（浮き上がって見える）
 * - プレス時に凸→凹（inset shadow）に切り替わる
 * - ボーダーゼロ、角は大きめ（2xl）
 * - タイポグラフィは細め（font-light）で柔らかい印象
 *
 * 重要: シャドウ色・サーフェス色はすべて css-bridge が生成する派生変数
 * （--neumo-surface / --neumo-shadow-dark / --neumo-shadow-light / --neumo-*）を参照する。
 * これにより、明るいテーマでも暗いテーマでも正しい凹凸が自動生成される。
 * パック側に色をハードコードしてはならない。
 */
import type { VisualStylePack } from './types';

export const neumorphismPack: VisualStylePack = {
	id: 'neumorphism',
	name: 'Neumorphism',
	description: 'Soft UI — 凹凸シャドウで表現する柔らかい立体感',
	author: 'OpenYASKE',
	version: 5,

	press: {
		// 凸→凹: inset shadow に切り替え（派生変数でテーマ追従）
		button:
			'active:shadow-[var(--shadow-style-sm)] active:scale-[0.97] transition-all duration-150 ease-[cubic-bezier(0.16,1,0.3,1)]',
		icon: 'active:shadow-[var(--shadow-style-sm)] active:scale-90 transition-all duration-100',
		card: 'hover:shadow-[var(--shadow-style-lg)] active:shadow-[var(--shadow-style-lg)] active:scale-[0.99] transition-all duration-200',
		tab: 'active:shadow-[var(--shadow-style-sm)] transition-all duration-100',
		glass: 'transition-all duration-150',
		toggle: 'active:shadow-[var(--shadow-style-sm)] transition-all duration-100'
	},

	materials: {
		// サーフェスは背景と同一色（--neumo-surface）で浮き上がりを表現
		glass: 'bg-[var(--neumo-surface)] rounded-2xl shadow-[var(--shadow-style-md)] border-0',
		glassHeader: 'bg-[var(--neumo-surface)] rounded-2xl shadow-[var(--shadow-style-sm)] border-0',
		glassModal: 'bg-[var(--neumo-surface)] rounded-3xl shadow-[var(--shadow-style-xl)] border-0',
		card: 'bg-[var(--neumo-surface)] rounded-2xl shadow-[var(--shadow-style-sm)] hover:shadow-[var(--shadow-style-md)] transition-shadow duration-250 border-0',
		interactiveCard:
			'bg-[var(--neumo-surface)] rounded-2xl shadow-[var(--shadow-style-sm)] hover:shadow-[var(--shadow-style-lg)] active:shadow-[var(--shadow-style-lg)] transition-all duration-200 cursor-pointer select-none border-0'
	},

	easing: {
		default: 'cubic-bezier(0.16, 1, 0.3, 1)',
		sheet: 'cubic-bezier(0.32, 0.72, 0, 1)',
		bounce: 'cubic-bezier(0.34, 1.56, 0.64, 1)',
		decelerate: 'cubic-bezier(0, 0, 0.2, 1)',
		reverse: 'cubic-bezier(0.7, 0, 0.84, 0)'
	},

	spring: {
		default: { damping: 1.0, response: 0.45 },
		sheet: { damping: 0.9, response: 0.4 },
		modal: { damping: 1.0, response: 0.4 },
		menu: { damping: 0.9, response: 0.4 },
		flick: { damping: 0.85, response: 0.45 },
		rotation: { damping: 0.85, response: 0.45 },
		pinch: { damping: 0.9, response: 0.35 }
	},

	shadow: {
		// applyStyleShadows で --shadow-style-* に書き出される。値は css-bridge の --neumo-* 派生を使う
		sm: 'var(--neumo-sm)',
		md: 'var(--neumo-md)',
		lg: 'var(--neumo-lg)',
		xl: 'var(--neumo-xl)'
	},

	typography: {
		display: 'leading-[1.2] tracking-[-0.01em] font-extralight',
		heading: 'leading-tight tracking-normal font-light',
		body: 'leading-relaxed tracking-normal font-light',
		label: 'leading-snug tracking-wide font-normal',
		numeric: '[font-variant-numeric:tabular-nums] [font-feature-settings:"tnum"]'
	},

	willChange: {
		transform: 'will-change-transform',
		opacity: 'will-change-opacity',
		both: 'will-change-transform, opacity',
		filter: 'will-change-filter'
	},

	layout: {
		cardSurface: 'neumorphic',
		borderRadius: '2xl',
		borders: false,
		glassOverlay: false,
		headingCase: 'normal',
		dense: false
	},

	tokens: {
		sectionHeader:
			'text-xs font-light tracking-[0.15em] uppercase text-[var(--color-nav-inactive)]',
		cardHeader: 'text-base font-light tracking-wide',
		pageTitle: 'text-2xl font-extralight tracking-tight',
		navBar: 'bg-[var(--neumo-surface)] shadow-[var(--shadow-style-sm)] border-0 rounded-2xl'
	},

	// Neumorphism 固有の UI レシピ: 凹凸シャドウ、ボーダーゼロ、背景同一色
	components: {
		button: {
			shape: 'rounded-2xl',
			primary:
				'bg-[var(--neumo-surface)] text-[var(--color-primary-600)] border-0 shadow-[var(--shadow-style-md)] ring-1 ring-inset ring-white/10 hover:shadow-[var(--shadow-style-lg)] active:shadow-[var(--shadow-style-sm)] active:ring-0',
			secondary:
				'bg-[var(--neumo-surface)] text-[var(--color-nav-active)] border-0 shadow-[var(--shadow-style-sm)] ring-1 ring-inset ring-white/10 hover:shadow-[var(--shadow-style-md)] active:shadow-[var(--shadow-style-sm)] active:ring-0',
			ghost: 'bg-transparent text-[var(--color-nav-active)] hover:bg-[var(--color-surface-muted)]',
			danger:
				'bg-[var(--neumo-surface)] text-[var(--color-attendance-absent-bg)] border-0 shadow-[var(--shadow-style-md)] active:shadow-[var(--shadow-style-sm)]',
			glass:
				'bg-[var(--neumo-surface)] text-[var(--color-nav-active)] border-0 shadow-[var(--shadow-style-md)]'
		},
		input: {
			// 凹んだ入力欄: inset shadow
			base: 'block w-full min-h-[44px] rounded-2xl border-0 bg-[var(--neumo-surface)] px-4 py-2.5 text-sm text-[var(--color-nav-active)] placeholder-[var(--color-nav-inactive)] shadow-[var(--shadow-style-sm)] focus:outline-none focus:shadow-[var(--shadow-style-lg)] focus:ring-2 focus:ring-[var(--color-primary-500)]/20 disabled:opacity-50'
		},
		card: {
			base: 'rounded-2xl border-0 bg-[var(--neumo-surface)] shadow-[var(--shadow-style-sm)] ring-1 ring-inset ring-white/5',
			interactive:
				'rounded-2xl border-0 bg-[var(--neumo-surface)] shadow-[var(--shadow-style-sm)] ring-1 ring-inset ring-white/5 hover:shadow-[var(--shadow-style-lg)] active:shadow-[var(--shadow-style-lg)] active:ring-0 transition-all duration-200 cursor-pointer select-none'
		},
		sidenav: {
			activeItem:
				'bg-[var(--neumo-surface)] text-[var(--color-primary-600)] font-semibold border-0 shadow-[var(--shadow-style-sm)] ring-1 ring-inset ring-white/10',
			inactiveItem:
				'text-[var(--color-nav-inactive)] font-light hover:text-[var(--color-nav-active)]'
		}
	}
};
