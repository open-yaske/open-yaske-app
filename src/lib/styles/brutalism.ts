/**
 * src/lib/styles/brutalism.ts
 * Brutalism — 生々しい素材感、ハードオフセット、太枠、大文字。
 *
 * 設計原則:
 * - 角ゼロ（rounded-none）— 一切の装飾を拒否
 * - nav-active 色を使った極太枠線（border-2〜3）
 * - 同色のハードオフセットシャドウ（版下の影、ぼかしゼロ）
 * - 大文字・極太のタイポグラフィ（font-black, uppercase）
 * - プレス時は影が消えて要素が右下に移動（物理的な押し込み）
 * - ダークモードではシャドウが白くなる
 * - 高密度レイアウト（dense: true）
 * - steps(2) イージング — ハードなステップ遷移
 */
import type { VisualStylePack } from './types';

export const brutalismPack: VisualStylePack = {
	id: 'brutalism',
	name: 'Brutalism',
	description: '生コンクリートの美学 — 太枠・ハードオフセット・大文字',
	author: 'OpenYASKE',
	version: 4,

	press: {
		button:
			'active:translate-x-[3px] active:translate-y-[3px] active:shadow-none transition-all duration-50',
		icon: 'active:translate-x-[2px] active:translate-y-[2px] active:shadow-none transition-all duration-50',
		card: 'active:translate-x-[3px] active:translate-y-[3px] active:shadow-none transition-all duration-75',
		tab: 'active:opacity-50 transition-opacity duration-50',
		glass: 'transition-all duration-75',
		toggle: 'active:opacity-50 transition-opacity duration-50'
	},

	materials: {
		glass:
			'bg-[var(--color-surface-card)] rounded-none border-[3px] border-[var(--color-nav-active)] shadow-[var(--shadow-style-md)]',
		glassHeader:
			'bg-[var(--color-nav-active)] text-[var(--color-surface-page)] rounded-none border-b-[3px] border-[var(--color-nav-active)]',
		glassModal:
			'bg-[var(--color-surface-card)] rounded-none border-[3px] border-[var(--color-nav-active)] shadow-[var(--shadow-style-xl)]',
		card: 'bg-[var(--color-surface-card)] rounded-none border-2 border-[var(--color-nav-active)]/80 shadow-[var(--shadow-style-sm)] hover:shadow-[var(--shadow-style-md)] transition-shadow duration-100',
		interactiveCard:
			'bg-[var(--color-surface-card)] rounded-none border-2 border-[var(--color-nav-active)]/80 shadow-[var(--shadow-style-sm)] hover:shadow-[var(--shadow-style-md)] active:translate-x-[3px] active:translate-y-[3px] active:shadow-none transition-all duration-75 cursor-pointer select-none'
	},

	easing: {
		default: 'steps(2, end)',
		sheet: 'steps(2, end)',
		bounce: 'steps(2, end)',
		decelerate: 'steps(2, end)',
		reverse: 'steps(2, end)'
	},

	shadow: {
		sm: '3px 3px 0px rgba(0,0,0,0.9)',
		md: '5px 5px 0px rgba(0,0,0,0.9)',
		lg: '8px 8px 0px rgba(0,0,0,0.9)',
		xl: '12px 12px 0px rgba(0,0,0,0.9)'
	},

	typography: {
		display: 'leading-[1.0] tracking-[-0.03em] uppercase font-black',
		heading: 'leading-tight tracking-normal uppercase font-black',
		body: 'leading-relaxed tracking-normal font-medium',
		label: 'leading-snug tracking-[0.05em] uppercase font-bold',
		numeric: '[font-variant-numeric:tabular-nums] [font-feature-settings:"tnum"]'
	},

	willChange: {
		transform: 'will-change-transform',
		opacity: 'will-change-opacity',
		both: 'will-change-transform, opacity',
		filter: 'will-change-filter'
	},

	layout: {
		cardSurface: 'brutal',
		borderRadius: 'none',
		borders: true,
		glassOverlay: false,
		headingCase: 'uppercase',
		dense: true
	},

	tokens: {
		sectionHeader:
			'text-[11px] font-black uppercase tracking-[0.1em] text-[var(--color-surface-page)] bg-[var(--color-nav-active)] px-2 py-1 inline-block',
		cardHeader: 'text-sm font-black uppercase tracking-wide',
		pageTitle: 'text-xl font-black uppercase tracking-tighter',
		navBar:
			'bg-[var(--color-nav-active)] text-[var(--color-surface-page)] rounded-none border-b-[3px] border-[var(--color-nav-active)]'
	},

	// Brutalism 固有の UI レシピ: 極太枠、角ゼロ、ハードオフセット
	components: {
		button: {
			shape: 'rounded-none',
			primary:
				'bg-[var(--color-nav-active)] text-[var(--color-surface-page)] border-[3px] border-[var(--color-nav-active)] shadow-[var(--shadow-style-md)] hover:bg-[var(--color-primary-500)] hover:border-[var(--color-primary-500)] active:shadow-none active:translate-x-[3px] active:translate-y-[3px]',
			secondary:
				'bg-[var(--color-primary-500)] text-white border-2 border-[var(--color-nav-active)] shadow-[var(--shadow-style-sm)] hover:shadow-[var(--shadow-style-md)] active:shadow-none active:translate-x-[2px] active:translate-y-[2px]',
			ghost:
				'bg-transparent text-[var(--color-nav-active)] border-2 border-transparent hover:border-[var(--color-nav-active)] hover:bg-[var(--color-surface-muted)]',
			danger:
				'bg-[var(--color-attendance-absent-bg)] text-[var(--color-attendance-absent-fg)] border-2 border-[var(--color-nav-active)] shadow-[var(--shadow-style-sm)] hover:shadow-[var(--shadow-style-md)] active:shadow-none active:translate-x-[2px] active:translate-y-[2px]',
			glass:
				'bg-[var(--color-surface-card)] text-[var(--color-nav-active)] border-2 border-[var(--color-nav-active)]'
		},
		input: {
			base: 'block w-full min-h-[44px] rounded-none border-2 border-[var(--color-nav-active)] bg-[var(--color-surface-card)] px-2 py-1 text-sm font-medium text-[var(--color-nav-active)] placeholder-[var(--color-nav-inactive)] focus:outline-none focus:bg-[var(--color-surface-muted)] focus:border-[var(--color-primary-500)] disabled:opacity-50'
		},
		card: {
			base: 'rounded-none border-2 border-[var(--color-nav-active)] bg-[var(--color-surface-card)] shadow-[var(--shadow-style-sm)]',
			interactive:
				'rounded-none border-2 border-[var(--color-nav-active)] bg-[var(--color-surface-card)] shadow-[var(--shadow-style-sm)] hover:shadow-[var(--shadow-style-md)] hover:bg-[var(--color-surface-muted)] active:translate-x-[3px] active:translate-y-[3px] active:shadow-none transition-all duration-75 cursor-pointer select-none'
		},
		sidenav: {
			activeItem:
				'bg-[var(--color-nav-active)] text-[var(--color-surface-page)] font-black uppercase tracking-wide border-[3px] border-[var(--color-nav-active)] shadow-[var(--shadow-style-md)]',
			inactiveItem:
				'text-[var(--color-nav-active)] font-bold uppercase hover:bg-[var(--color-surface-muted)]'
		}
	}
};
