/**
 * src/lib/theme/contrast.ts
 * WCAG 2.1 基準に基づくカラーコントラスト比（Contrast Ratio）計算および
 * ThemePack アクセシビリティ診断ユーティリティ。
 *
 * v2: コントラスト検査マトリックス（contrast-matrix.ts）駆動の包括的診断に対応。
 */

import type { ThemePack } from './types';
import {
	getAllContrastChecks,
	THRESHOLD_VALUES,
	type ContrastCategory,
	type WcagThreshold
} from './contrast-matrix';

/** RGB カラー構造体 */
export interface RgbColor {
	r: number; // 0..255
	g: number; // 0..255
	b: number; // 0..255
	a: number; // 0..1
}

/** コントラスト診断結果 */
export interface ContrastCheckResult {
	label: string;
	fgColor: string;
	bgColor: string;
	ratio: number;
	passAA: boolean; // 4.5:1 以上 (通常テキスト)
	passAALarge: boolean; // 3.0:1 以上 (大型テキスト / UIコンポーネント)
}

/**
 * 任意のカラー文字列 (HEX, RGBA, VAR等) を RGB にパースする
 */
export function parseColor(colorStr: string, defaultAlpha = 1): RgbColor {
	const str = colorStr.trim().toLowerCase();

	// 1) HEX 表記 (#333, #333f, #666666, #666666ff)
	if (str.startsWith('#')) {
		const hex = str.slice(1);
		if (hex.length === 3) {
			const r = parseInt(hex[0] + hex[0], 16);
			const g = parseInt(hex[1] + hex[1], 16);
			const b = parseInt(hex[2] + hex[2], 16);
			return { r, g, b, a: defaultAlpha };
		}
		if (hex.length === 4) {
			const r = parseInt(hex[0] + hex[0], 16);
			const g = parseInt(hex[1] + hex[1], 16);
			const b = parseInt(hex[2] + hex[2], 16);
			const a = parseInt(hex[3] + hex[3], 16) / 255;
			return { r, g, b, a };
		}
		if (hex.length === 6) {
			const r = parseInt(hex.slice(0, 2), 16);
			const g = parseInt(hex.slice(2, 4), 16);
			const b = parseInt(hex.slice(4, 6), 16);
			return { r, g, b, a: defaultAlpha };
		}
		if (hex.length === 8) {
			const r = parseInt(hex.slice(0, 2), 16);
			const g = parseInt(hex.slice(2, 4), 16);
			const b = parseInt(hex.slice(4, 6), 16);
			const a = parseInt(hex.slice(6, 8), 16) / 255;
			return { r, g, b, a };
		}
	}

	// 2) rgba(r, g, b, a) または rgb(r, g, b) 表記
	const rgbMatch = str.match(/^rgba?\(\s*(\d+)\s*,\s*(\d+)\s*,\s*(\d+)(?:\s*,\s*([\d.]+))?\s*\)$/);
	if (rgbMatch) {
		const r = parseInt(rgbMatch[1], 10);
		const g = parseInt(rgbMatch[2], 10);
		const b = parseInt(rgbMatch[3], 10);
		const a = rgbMatch[4] !== undefined ? parseFloat(rgbMatch[4]) : defaultAlpha;
		return { r, g, b, a };
	}

	// フォールバック（パース不可の場合は黒）
	return { r: 0, g: 0, b: 0, a: 1 };
}

/**
 * アルファ合成 (Alpha Blending)
 * 半透明の前景色を背景色の上に重ねた実効カラーを算出
 */
export function blendColors(fg: RgbColor, bg: RgbColor): RgbColor {
	const alpha = fg.a;
	const r = Math.round(fg.r * alpha + bg.r * (1 - alpha));
	const g = Math.round(fg.g * alpha + bg.g * (1 - alpha));
	const b = Math.round(fg.b * alpha + bg.b * (1 - alpha));
	return { r, g, b, a: 1 };
}

/**
 * sRGB チャンネルの相対輝度補正
 */
function channelLuminance(channel: number): number {
	const c = channel / 255;
	return c <= 0.04045 ? c / 12.92 : Math.pow((c + 0.055) / 1.055, 2.4);
}

/**
 * WCAG 2.1 相対輝度 (Relative Luminance $L$) の計算
 */
export function calculateLuminance(color: RgbColor): number {
	const r = channelLuminance(color.r);
	const g = channelLuminance(color.g);
	const b = channelLuminance(color.b);
	return 0.2126 * r + 0.7152 * g + 0.0722 * b;
}

/**
 * 前景色と背景色のコントラスト比 (Contrast Ratio) を計算する (1.0 〜 21.0)
 */
export function calculateContrastRatio(
	fgStr: string,
	bgStr: string,
	baseBgStr = '#ffffff'
): number {
	const baseBg = parseColor(baseBgStr);
	const rawBg = parseColor(bgStr);
	const rawFg = parseColor(fgStr);

	// 背景が半透明の場合はベース背景（通常白か黒）と合成
	const bg = rawBg.a < 1 ? blendColors(rawBg, baseBg) : rawBg;
	// 前景色が半透明の場合は背景色と合成
	const fg = rawFg.a < 1 ? blendColors(rawFg, bg) : rawFg;

	const l1 = calculateLuminance(fg);
	const l2 = calculateLuminance(bg);

	const lighter = Math.max(l1, l2);
	const darker = Math.min(l1, l2);

	const ratio = (lighter + 0.05) / (darker + 0.05);
	return Math.round(ratio * 100) / 100;
}

/**
 * ThemePack 全体のコントラスト比とアクセシビリティを検証
 */
export function diagnoseThemePackContrast(pack: ThemePack): ContrastCheckResult[] {
	const results: ContrastCheckResult[] = [];
	const baseBg = pack.ui.surface.page;

	function check(label: string, fg: string, bg: string) {
		const ratio = calculateContrastRatio(fg, bg, baseBg);
		results.push({
			label,
			fgColor: fg,
			bgColor: bg,
			ratio,
			passAA: ratio >= 4.5,
			passAALarge: ratio >= 3.0
		});
	}

	// 1. Page 背景 vs アクティブ文字 / Muted文字
	check('Page Background vs Active Text', pack.ui.nav.active, pack.ui.surface.page);
	check('Page Background vs Muted Text', pack.ui.surface.mutedForeground, pack.ui.surface.page);

	// 2. Card 背景 vs アクティブ文字 / Muted文字
	check('Card Surface vs Active Text', pack.ui.nav.active, pack.ui.surface.card);
	check('Card Surface vs Muted Text', pack.ui.surface.mutedForeground, pack.ui.surface.card);

	// 3. Status Colors (Danger, Warning, Success) vs Card Surface
	const isDarkBg = calculateLuminance(parseColor(pack.ui.surface.card)) < 0.5;
	if (pack.ui.danger) {
		const dangerColor = isDarkBg
			? pack.ui.danger[400] || pack.ui.danger[500]
			: pack.ui.danger[600] || pack.ui.danger[500];
		check('Card vs Danger Color', dangerColor, pack.ui.surface.card);
	}
	if (pack.ui.warning) {
		const warningColor = isDarkBg
			? pack.ui.warning[400] || pack.ui.warning[500]
			: pack.ui.warning[600] || pack.ui.warning[500];
		check('Card vs Warning Color', warningColor, pack.ui.surface.card);
	}
	if (pack.ui.success) {
		const successColor = isDarkBg
			? pack.ui.success[400] || pack.ui.success[500]
			: pack.ui.success[600] || pack.ui.success[500];
		check('Card vs Success Color', successColor, pack.ui.surface.card);
	}

	// 4. Primary Button (500) vs Card Surface
	check('Primary Button vs Card Surface', pack.ui.primary[500], pack.ui.surface.card);

	// 5. Attendance 5状態の文字 vs 背景
	check('Attendance Present', pack.attendance.present.fg, pack.attendance.present.bg);
	check('Attendance Absent', pack.attendance.absent.fg, pack.attendance.absent.bg);
	check('Attendance Late', pack.attendance.late.fg, pack.attendance.late.bg);
	check('Attendance Excused', pack.attendance.excused.fg, pack.attendance.excused.bg);
	check('Attendance Cancelled', pack.attendance.cancelled.fg, pack.attendance.cancelled.bg);

	return results;
}

// ═══════════════════════════════════════════════════════════
// v2: マトリックス駆動の包括的コントラスト診断
// ═══════════════════════════════════════════════════════════

/** 拡張されたコントラスト診断結果（マトリックス駆動） */
export interface EnhancedContrastResult {
	/** チェック定義の一意ID */
	checkId: string;
	/** 人間可読ラベル */
	label: string;
	/** カテゴリ */
	category: ContrastCategory;
	/** 前景色（解決済み） */
	fgColor: string;
	/** 背景色（解決済み） */
	bgColor: string;
	/** 計算されたコントラスト比 */
	ratio: number;
	/** 適用される閾値 */
	threshold: WcagThreshold;
	/** 閾値の数値 */
	thresholdValue: number;
	/** 合格したか */
	passed: boolean;
	/** 後方互換: AA (4.5:1) 合格 */
	passAA: boolean;
	/** 後方互換: AA Large (3.0:1) 合格 */
	passAALarge: boolean;
}

/** テーマ全体のコントラスト検証レポート */
export interface ContrastReport {
	/** 合格したチェック数 */
	passed: number;
	/** 不合格のチェック数 */
	failed: number;
	/** 総チェック数 */
	total: number;
	/** 全結果 */
	results: EnhancedContrastResult[];
	/** 合格率 (0-1) */
	passRate: number;
	/** 全チェック合格か */
	isValid: boolean;
	/** 不合格結果のみ */
	failures: EnhancedContrastResult[];
}

/**
 * コントラスト検査マトリックスに基づき、ThemePack の全色組み合わせを検証する。
 *
 * 静的チェック（テキスト・コンポーネント・枠線・出席）+
 * 動的チェック（パレット色: テーマごとにエントリ数が異なる）を
 * すべて実行し、包括的なレポートを返す。
 *
 * @example
 * ```ts
 * const report = validateThemePackContrast(defaultPack);
 * if (!report.isValid) {
 *   for (const f of report.failures) {
 *     console.warn(`${f.checkId}: ${f.ratio} < ${f.thresholdValue} (${f.label})`);
 *   }
 * }
 * ```
 */
export function validateThemePackContrast(pack: ThemePack): ContrastReport {
	const checks = getAllContrastChecks(pack);
	const baseBg = pack.ui.surface.page;
	const results: EnhancedContrastResult[] = [];

	for (const check of checks) {
		const fgColor = check.fg(pack);
		const bgColor = check.bg(pack);
		const checkBaseBg = check.baseBg ? check.baseBg(pack) : baseBg;
		const ratio = calculateContrastRatio(fgColor, bgColor, checkBaseBg);
		const thresholdValue = THRESHOLD_VALUES[check.threshold];

		results.push({
			checkId: check.id,
			label: check.label,
			category: check.category,
			fgColor,
			bgColor,
			ratio,
			threshold: check.threshold,
			thresholdValue,
			passed: ratio >= thresholdValue,
			passAA: ratio >= 4.5,
			passAALarge: ratio >= 3.0
		});
	}

	const passed = results.filter((r) => r.passed).length;
	const failures = results.filter((r) => !r.passed);

	return {
		passed,
		failed: failures.length,
		total: results.length,
		results,
		passRate: results.length > 0 ? passed / results.length : 1,
		isValid: failures.length === 0,
		failures
	};
}
