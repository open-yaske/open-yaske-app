/**
 * src/lib/styles/win95.ts
 * Windows 95 — クラシック 3D ベベル枠のレトロデスクトップ。
 *
 * 設計原則:
 * - 角ゼロ（rounded-none）— Win95 に角丸は存在しない
 * - 4色ベベル: 白（highlight）→ 銀（face）→ 灰（shadow）→ 黒（dark shadow）
 * - レイズド（浮き）: 上/左=白、下/右=暗色
 * - サンクン（凹み）: 上/左=暗色、下/右=白（入力欄・押下時）
 * - プレス時に枠が反転 + 1px 右下移動（物理的な押し込み）
 * - 影なし（box-shadow: none）— 3D 枠線だけで立体感を表現
 * - MS Sans Serif 8pt 風: 小さめ、ピクセル感、見出しは太字
 * - タイトルバー: アクティブ時はネイビー、非アクティブ時はグレー
 * - transition: none — Win95 にアニメーションは存在しない
 * - dense: true — コンパクトな間隔
 */
import type { VisualStylePack } from './types';

export const win95Pack: VisualStylePack = {
	id: 'win95',
	name: 'Windows 95',
	description: 'クラシック 3D ベベル枠 — レトロデスクトップの原点',
	author: 'OpenYASKE',
	version: 4,

	press: {
		button:
			'active:border-t-[var(--color-surface-border)] active:border-l-[var(--color-surface-border)] active:border-b-white/80 active:border-r-white/80 active:translate-x-px active:translate-y-px transition-none',
		icon: 'active:border-t-[var(--color-surface-border)] active:border-l-[var(--color-surface-border)] active:border-b-white/80 active:border-r-white/80 active:translate-x-px active:translate-y-px transition-none',
		card: 'active:border-t-[var(--color-surface-border)] active:border-l-[var(--color-surface-border)] active:border-b-white/80 active:border-r-white/80 active:translate-x-px active:translate-y-px transition-none',
		tab: 'active:border-t-[var(--color-surface-border)] active:border-l-[var(--color-surface-border)] active:border-b-white/80 active:border-r-white/80 transition-none',
		glass: 'transition-none',
		toggle: 'active:opacity-60 transition-none'
	},

	materials: {
		glass:
			'bg-[var(--color-surface-card)] rounded-none border-2 border-t-white/80 border-l-white/80 border-r-black/40 border-b-black/40',
		glassHeader:
			'bg-[var(--color-primary-800)] text-white rounded-none border-b-2 border-r-2 border-r-black/40 border-b-black/40',
		glassModal:
			'bg-[var(--color-surface-card)] rounded-none border-2 border-t-white/80 border-l-white/80 border-r-black/40 border-b-black/40',
		card: 'bg-[var(--color-surface-card)] rounded-none border-2 border-t-white/80 border-l-white/80 border-r-black/40 border-b-black/40',
		interactiveCard:
			'bg-[var(--color-surface-card)] rounded-none border-2 border-t-white/80 border-l-white/80 border-r-black/40 border-b-black/40 active:border-t-black/40 active:border-l-black/40 active:border-b-white/80 active:border-r-white/80 active:translate-x-px active:translate-y-px transition-none cursor-pointer select-none'
	},

	easing: {
		default: 'step-end',
		sheet: 'step-end',
		bounce: 'step-end',
		decelerate: 'step-end',
		reverse: 'step-end'
	},

	shadow: {
		sm: 'none',
		md: 'none',
		lg: 'none',
		xl: 'none'
	},

	typography: {
		display: 'leading-[1.25] font-bold tracking-normal',
		heading: 'leading-tight font-bold tracking-normal',
		body: 'leading-relaxed font-normal tracking-normal',
		label: 'leading-snug font-normal tracking-normal',
		numeric: '[font-variant-numeric:tabular-nums]'
	},

	willChange: {
		transform: 'will-change-transform',
		opacity: 'will-change-opacity',
		both: 'will-change-transform, opacity',
		filter: 'will-change-filter'
	},

	layout: {
		cardSurface: 'chiseled',
		borderRadius: 'none',
		borders: true,
		glassOverlay: false,
		headingCase: 'normal',
		dense: true
	},

	tokens: {
		sectionHeader:
			'text-[11px] font-bold text-[var(--color-nav-active)] border-b border-[var(--color-surface-border)] pb-0.5',
		cardHeader: 'text-sm font-bold',
		pageTitle: 'text-base font-bold',
		navBar:
			'bg-[var(--color-primary-800)] text-white rounded-none border-b-2 border-r-2 border-r-black/40 border-b-black/40'
	},

	// Win95 固有の UI レシピ: 3D ベベル枠、角ゼロ、影なし
	components: {
		button: {
			shape: 'rounded-none',
			primary:
				'bg-[var(--color-surface-card)] text-[var(--color-nav-active)] border-2 border-t-white/80 border-l-white/80 border-r-black/40 border-b-black/40 active:border-t-black/40 active:border-l-black/40 active:border-b-white/80 active:border-r-white/80 active:translate-x-px active:translate-y-px',
			secondary:
				'bg-[var(--color-surface-card)] text-[var(--color-nav-active)] border-2 border-t-white/80 border-l-white/80 border-r-black/40 border-b-black/40 active:border-t-black/40 active:border-l-black/40 active:border-b-white/80 active:border-r-white/80 active:translate-x-px active:translate-y-px',
			ghost: 'bg-transparent text-[var(--color-nav-active)] hover:bg-[var(--color-surface-muted)]',
			danger:
				'bg-[var(--color-surface-card)] text-[var(--color-attendance-absent-bg)] border-2 border-t-white/80 border-l-white/80 border-r-black/40 border-b-black/40 active:border-t-black/40 active:border-l-black/40 active:border-b-white/80 active:border-r-white/80',
			glass:
				'bg-[var(--color-surface-card)] text-[var(--color-nav-active)] border-2 border-t-white/80 border-l-white/80 border-r-black/40 border-b-black/40'
		},
		input: {
			// サンクン（凹）枠: 上/左が暗色、下/右が白
			base: 'block w-full min-h-[44px] rounded-none border-2 border-t-black/40 border-l-black/40 border-b-white/80 border-r-white/80 bg-[var(--color-surface-card)] px-2 py-1 text-sm text-[var(--color-nav-active)] placeholder-[var(--color-nav-inactive)] focus:outline-none disabled:opacity-50'
		},
		card: {
			base: 'rounded-none border-2 border-t-white/80 border-l-white/80 border-r-black/40 border-b-black/40 bg-[var(--color-surface-card)]',
			interactive:
				'rounded-none border-2 border-t-white/80 border-l-white/80 border-r-black/40 border-b-black/40 bg-[var(--color-surface-card)] active:border-t-black/40 active:border-l-black/40 active:border-b-white/80 active:border-r-white/80 active:translate-x-px active:translate-y-px cursor-pointer select-none'
		},
		sidenav: {
			activeItem:
				'bg-[var(--color-primary-800)] text-white font-bold rounded-none border-2 border-t-black/40 border-l-black/40 border-b-white/80 border-r-white/80',
			inactiveItem: 'text-[var(--color-nav-active)] hover:bg-[var(--color-primary-800)]/10'
		}
	}
};
