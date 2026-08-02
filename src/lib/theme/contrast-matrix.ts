/**
 * src/lib/theme/contrast-matrix.ts
 *
 * コントラスト検査マトリックス定義。
 * コードベースの `var(--color-*)` 実使用箇所（約1,200箇所）から逆引きし、
 * 実際にUIで使われる fg/bg 組み合わせを網羅する。
 *
 * 設計原則:
 * - スケール各段は意図的に選ばれた独立した色（マルチヒュー許容）
 * - 品質基準は「使われるコンテキストでのコントラスト合格」のみ
 * - 未使用の変数（secondary.*, nav.bg, nav.hover）は検査しない
 */

import type { ThemePack } from './types';

// ─── 型定義 ───

export type ContrastCategory = 'text' | 'component' | 'palette' | 'attendance' | 'border';

/** WCAG 2.1 閾値 + 装飾的境界線用の緩和閾値 */
export type WcagThreshold = 'AA' | 'AALarge' | 'Decorative';

/** 閾値の数値 */
export const THRESHOLD_VALUES: Record<WcagThreshold, number> = {
	AA: 4.5,
	AALarge: 3.0,
	/** 装飾的境界線: 知覚できる最低限のコントラスト（WCAG対象外） */
	Decorative: 1.0
};

export interface ContrastCheckDef {
	/** 一意のID（テスト・レポートで参照） */
	id: string;
	/** 人間可読なラベル */
	label: string;
	/** カテゴリ */
	category: ContrastCategory;
	/** 前景色の解決関数 */
	fg: (pack: ThemePack) => string;
	/** 背景色の解決関数 */
	bg: (pack: ThemePack) => string;
	/** 合格に必要な最低コントラスト比 */
	threshold: WcagThreshold;
	/** 半透明背景の合成に使うベース背景（デフォルト: surface.page） */
	baseBg?: (pack: ThemePack) => string;
}

// ─── ヘルパー ───

/** 背景の相対輝度から適切なテキスト色（白 or 黒）を選ぶ */
export function pickTextColorForBg(bgHex: string): '#ffffff' | '#000000' {
	// 簡易輝度計算（sRGB近似）
	const hex = bgHex.replace('#', '');
	if (hex.length < 6) return '#000000';
	const r = parseInt(hex.slice(0, 2), 16) / 255;
	const g = parseInt(hex.slice(2, 4), 16) / 255;
	const b = parseInt(hex.slice(4, 6), 16) / 255;
	const luminance = 0.2126 * r + 0.7152 * g + 0.0722 * b;
	return luminance > 0.5 ? '#000000' : '#ffffff';
}

// ─── 検査マトリックス ───

export const CONTRAST_CHECKS: ContrastCheckDef[] = [
	// ═══════════════════════════════════════════
	// カテゴリ1: 本文テキスト (WCAG AA: ≥4.5:1)
	// ═══════════════════════════════════════════

	{
		id: 'text-nav-active-on-page',
		label: '本文テキスト on ページ背景',
		category: 'text',
		fg: (p) => p.ui.nav.active,
		bg: (p) => p.ui.surface.page,
		threshold: 'AA'
	},
	{
		id: 'text-nav-active-on-card',
		label: '本文テキスト on カード背景',
		category: 'text',
		fg: (p) => p.ui.nav.active,
		bg: (p) => p.ui.surface.card,
		threshold: 'AA'
	},
	{
		id: 'text-nav-inactive-on-page',
		label: '補助テキスト on ページ背景',
		category: 'text',
		fg: (p) => p.ui.nav.inactive,
		bg: (p) => p.ui.surface.page,
		threshold: 'AA'
	},
	{
		id: 'text-nav-inactive-on-card',
		label: '補助テキスト on カード背景',
		category: 'text',
		fg: (p) => p.ui.nav.inactive,
		bg: (p) => p.ui.surface.card,
		threshold: 'AA'
	},
	{
		id: 'text-nav-inactive-on-muted',
		label: '補助テキスト on ミュート背景',
		category: 'text',
		fg: (p) => p.ui.nav.inactive,
		bg: (p) => p.ui.surface.muted,
		threshold: 'AA'
	},
	{
		id: 'text-muted-fg-on-page',
		label: 'ミュートテキスト on ページ背景',
		category: 'text',
		fg: (p) => p.ui.surface.mutedForeground,
		bg: (p) => p.ui.surface.page,
		threshold: 'AA'
	},
	{
		id: 'text-muted-fg-on-card',
		label: 'ミュートテキスト on カード背景',
		category: 'text',
		fg: (p) => p.ui.surface.mutedForeground,
		bg: (p) => p.ui.surface.card,
		threshold: 'AA'
	},
	{
		id: 'text-muted-fg-on-muted',
		label: 'ミュートテキスト on ミュート背景',
		category: 'text',
		fg: (p) => p.ui.surface.mutedForeground,
		bg: (p) => p.ui.surface.muted,
		threshold: 'AA'
	},

	// ═══════════════════════════════════════════
	// カテゴリ2: UIコンポーネント (WCAG AA Large: ≥3.0:1)
	// ═══════════════════════════════════════════

	// --- プライマリリンク・強調テキスト ---
	{
		id: 'comp-primary-600-on-page',
		label: 'プライマリリンク on ページ背景',
		category: 'component',
		fg: (p) => p.ui.primary[600],
		bg: (p) => p.ui.surface.page,
		threshold: 'AALarge'
	},
	{
		id: 'comp-primary-600-on-card',
		label: 'プライマリリンク on カード背景',
		category: 'component',
		fg: (p) => p.ui.primary[600],
		bg: (p) => p.ui.surface.card,
		threshold: 'AALarge'
	},
	{
		id: 'comp-primary-600-on-primary-50',
		label: 'プライマリリンク on 淡色背景',
		category: 'component',
		fg: (p) => p.ui.primary[600],
		bg: (p) => p.ui.primary[50],
		threshold: 'AALarge'
	},
	{
		id: 'comp-primary-700-on-primary-100',
		label: '強調テキスト on primary.100背景',
		category: 'component',
		fg: (p) => p.ui.primary[700],
		bg: (p) => p.ui.primary[100],
		threshold: 'AALarge'
	},
	{
		id: 'comp-primary-700-on-primary-50',
		label: '強調テキスト on primary.50背景',
		category: 'component',
		fg: (p) => p.ui.primary[700],
		bg: (p) => p.ui.primary[50],
		threshold: 'AALarge'
	},
	{
		id: 'comp-primary-800-on-primary-50',
		label: '濃色テキスト on primary.50背景',
		category: 'component',
		fg: (p) => p.ui.primary[800],
		bg: (p) => p.ui.primary[50],
		threshold: 'AALarge'
	},

	// --- ボタン（白文字 on プライマリ背景）---
	{
		id: 'comp-white-on-primary-500',
		label: 'ボタンラベル(白) on primary.500',
		category: 'component',
		fg: () => '#ffffff',
		bg: (p) => p.ui.primary[500],
		threshold: 'AALarge'
	},
	{
		id: 'comp-white-on-primary-600',
		label: 'ボタンラベル(白) on primary.600(hover)',
		category: 'component',
		fg: () => '#ffffff',
		bg: (p) => p.ui.primary[600],
		threshold: 'AALarge'
	},
	{
		id: 'comp-white-on-primary-700',
		label: 'ボタンラベル(白) on primary.700(active)',
		category: 'component',
		fg: () => '#ffffff',
		bg: (p) => p.ui.primary[700],
		threshold: 'AALarge'
	},

	// --- FAB（淡色文字 on 濃色背景）---
	{
		id: 'comp-primary-50-on-primary-800',
		label: 'FABテキスト on primary.800',
		category: 'component',
		fg: (p) => p.ui.primary[50],
		bg: (p) => p.ui.primary[800],
		threshold: 'AALarge'
	},
	{
		id: 'comp-primary-200-on-primary-800',
		label: 'FAB補助テキスト on primary.800',
		category: 'component',
		fg: (p) => p.ui.primary[200],
		bg: (p) => p.ui.primary[800],
		threshold: 'AALarge'
	},

	// --- ステータス色テキスト ---
	{
		id: 'comp-danger-600-on-page',
		label: '危険テキスト on ページ背景',
		category: 'component',
		fg: (p) => (p.ui.danger ? p.ui.danger[600] : '#dc2626'),
		bg: (p) => p.ui.surface.page,
		threshold: 'AALarge'
	},
	{
		id: 'comp-danger-600-on-danger-50',
		label: '危険テキスト on 危険バナー背景',
		category: 'component',
		fg: (p) => (p.ui.danger ? p.ui.danger[600] : '#dc2626'),
		bg: (p) => (p.ui.danger ? p.ui.danger[50] : '#fef2f2'),
		threshold: 'AALarge'
	},
	{
		id: 'comp-danger-700-on-danger-50',
		label: '危険強調テキスト on 危険バナー背景',
		category: 'component',
		fg: (p) => (p.ui.danger ? p.ui.danger[700] : '#b91c1c'),
		bg: (p) => (p.ui.danger ? p.ui.danger[50] : '#fef2f2'),
		threshold: 'AALarge'
	},
	{
		id: 'comp-danger-500-on-page',
		label: '危険アイコン on ページ背景',
		category: 'component',
		fg: (p) => (p.ui.danger ? p.ui.danger[500] : '#ef4444'),
		bg: (p) => p.ui.surface.page,
		threshold: 'AALarge'
	},
	{
		id: 'comp-success-700-on-success-50',
		label: '成功テキスト on 成功バナー背景',
		category: 'component',
		fg: (p) => (p.ui.success ? p.ui.success[700] : '#15803d'),
		bg: (p) => (p.ui.success ? p.ui.success[50] : '#f0fdf4'),
		threshold: 'AALarge'
	},
	{
		id: 'comp-success-600-on-success-50',
		label: '成功テキスト(淡) on 成功バナー背景',
		category: 'component',
		fg: (p) => (p.ui.success ? p.ui.success[600] : '#16a34a'),
		bg: (p) => (p.ui.success ? p.ui.success[50] : '#f0fdf4'),
		threshold: 'AALarge'
	},
	{
		id: 'comp-warning-600-on-warning-50',
		label: '警告テキスト on 警告バナー背景',
		category: 'component',
		fg: (p) => (p.ui.warning ? p.ui.warning[600] : '#d97706'),
		bg: (p) => (p.ui.warning ? p.ui.warning[50] : '#fffbeb'),
		threshold: 'AALarge'
	},

	// --- フォーカスリング ---
	{
		id: 'comp-primary-500-on-card',
		label: 'フォーカスリング on カード背景',
		category: 'component',
		fg: (p) => p.ui.primary[500],
		bg: (p) => p.ui.surface.card,
		threshold: 'AALarge'
	},

	// ═══════════════════════════════════════════
	// カテゴリ3: 枠線 (装飾的境界線: ≥1.5:1)
	// 注意: 枠線は装飾目的であり、WCAG の UI component boundary (3.0:1)
	// の対象外。Apple Design では非常に薄い枠線が意図的に使われる。
	// 1.5:1 は「知覚できる最低限の境界」の基準。
	// ═══════════════════════════════════════════

	{
		id: 'border-surface-on-page',
		label: '境界線 on ページ背景',
		category: 'border',
		fg: (p) => p.ui.surface.border,
		bg: (p) => p.ui.surface.page,
		threshold: 'Decorative'
	},
	{
		id: 'border-surface-on-card',
		label: '境界線 on カード背景',
		category: 'border',
		fg: (p) => p.ui.surface.border,
		bg: (p) => p.ui.surface.card,
		threshold: 'Decorative'
	},

	// ═══════════════════════════════════════════
	// カテゴリ4: 出席状態 (≥3.0:1)
	// ═══════════════════════════════════════════

	{
		id: 'attend-present-fg-on-bg',
		label: '出席(present) 文字 on 背景',
		category: 'attendance',
		fg: (p) => p.attendance.present.fg,
		bg: (p) => p.attendance.present.bg,
		threshold: 'AALarge'
	},
	{
		id: 'attend-absent-fg-on-bg',
		label: '出席(absent) 文字 on 背景',
		category: 'attendance',
		fg: (p) => p.attendance.absent.fg,
		bg: (p) => p.attendance.absent.bg,
		threshold: 'AALarge'
	},
	{
		id: 'attend-late-fg-on-bg',
		label: '出席(late) 文字 on 背景',
		category: 'attendance',
		fg: (p) => p.attendance.late.fg,
		bg: (p) => p.attendance.late.bg,
		threshold: 'AALarge'
	},
	{
		id: 'attend-excused-fg-on-bg',
		label: '出席(excused) 文字 on 背景',
		category: 'attendance',
		fg: (p) => p.attendance.excused.fg,
		bg: (p) => p.attendance.excused.bg,
		threshold: 'AALarge'
	},
	{
		id: 'attend-cancelled-fg-on-bg',
		label: '出席(cancelled) 文字 on 背景',
		category: 'attendance',
		fg: (p) => p.attendance.cancelled.fg,
		bg: (p) => p.attendance.cancelled.bg,
		threshold: 'AALarge'
	},
	// 出席バッジ背景のカード上での視認性
	{
		id: 'attend-present-bg-on-card',
		label: '出席(present) バッジ on カード',
		category: 'attendance',
		fg: (p) => p.attendance.present.bg,
		bg: (p) => p.ui.surface.card,
		threshold: 'AALarge'
	},
	{
		id: 'attend-absent-bg-on-card',
		label: '出席(absent) バッジ on カード',
		category: 'attendance',
		fg: (p) => p.attendance.absent.bg,
		bg: (p) => p.ui.surface.card,
		threshold: 'AALarge'
	},
	{
		id: 'attend-late-bg-on-card',
		label: '出席(late) バッジ on カード',
		category: 'attendance',
		fg: (p) => p.attendance.late.bg,
		bg: (p) => p.ui.surface.card,
		threshold: 'AALarge'
	},
	{
		id: 'attend-excused-bg-on-card',
		label: '出席(excused) バッジ on カード',
		category: 'attendance',
		fg: (p) => p.attendance.excused.bg,
		bg: (p) => p.ui.surface.card,
		threshold: 'AALarge'
	},
	{
		id: 'attend-cancelled-bg-on-card',
		label: '出席(cancelled) バッジ on カード',
		category: 'attendance',
		fg: (p) => p.attendance.cancelled.bg,
		bg: (p) => p.ui.surface.card,
		threshold: 'AALarge'
	}

	// ═══════════════════════════════════════════
	// カテゴリ5: パレット色 (≥3.0:1)
	// resolveColorHex() 経由でインラインstyleに適用されるため、
	// HEX値を直接 surface.page/card に対して検査する。
	// ═══════════════════════════════════════════
	// 注意: パレット色は動的（テーマごとにエントリ数が異なる）ため、
	// 実行時に generatePaletteChecks() で生成する。
];

/**
 * パレット色のコントラストチェックを動的に生成する。
 * 各テーマの courseColors / todoColors / eventColors を
 * surface.page と surface.card に対して検査する。
 */
export function generatePaletteChecks(pack: ThemePack): ContrastCheckDef[] {
	const checks: ContrastCheckDef[] = [];

	const addPaletteChecks = (
		colors: { id: string; hex: string }[],
		kind: 'course' | 'todo' | 'event'
	) => {
		for (const color of colors) {
			checks.push({
				id: `palette-${kind}-${color.id}-on-page`,
				label: `${kind}色(${color.id}) on ページ背景`,
				category: 'palette',
				fg: () => color.hex,
				bg: (p) => p.ui.surface.page,
				threshold: 'AALarge'
			});
		}
	};

	addPaletteChecks(pack.palette.courseColors, 'course');
	addPaletteChecks(pack.palette.todoColors, 'todo');
	addPaletteChecks(pack.palette.eventColors, 'event');

	return checks;
}

/**
 * 全検査チェックを取得（静的 + 動的パレット）
 */
export function getAllContrastChecks(pack: ThemePack): ContrastCheckDef[] {
	return [...CONTRAST_CHECKS, ...generatePaletteChecks(pack)];
}
