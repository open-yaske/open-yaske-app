/**
 * ThemePack を CSS 変数に変換し、document.documentElement に適用する
 */

import type { ThemePack } from './types';
import { resolveFontFamily, normalizeLocale, type FontFamilyId } from './font-resolver';
import { parseColor, calculateLuminance } from './contrast';

/**
 * Neumorphism 向けの派生 CSS 変数を生成する。
 *
 * Neumorphism は「背景と同一トーンのサーフェスを凹凸シャドウで浮かび上がらせる」ため、
 * シャドウ色をテーマの表面色（surface.page）から動的に計算する必要がある。
 * - 明るいテーマ: 暗い影は強く、明るいハイライトも強く
 * - 暗いテーマ: 暗い影は強く、明るいハイライトは弱く
 *
 * これによりスタイルパック側に色をハードコードせず、全テーマで Neumorphism が正しく表示される。
 */
function emitStyleDerivedVars(vars: Record<string, string>, pack: ThemePack) {
	const page = pack.ui.surface.page;
	const rgb = parseColor(page);
	const lum = calculateLuminance(rgb); // 0(黒) .. 1(白)

	// 暗い影: 明るいテーマほど強く（0.16）、暗いテーマほど弱く（0.45）
	const darkAlpha = (0.16 + (1 - lum) * 0.3).toFixed(3);
	// 明るいハイライト: 明るいテーマほど強く（0.9）、暗いテーマほど弱く（0.06）
	const lightAlpha = (0.06 + lum * 0.85).toFixed(3);

	const dark = `rgba(0,0,0,${darkAlpha})`;
	const light = `rgba(255,255,255,${lightAlpha})`;

	// Neumorphism: サーフェスは背景と同一色（浮き上がりの前提）
	vars['--neumo-surface'] = page;
	vars['--neumo-shadow-dark'] = dark;
	vars['--neumo-shadow-light'] = light;
	vars['--neumo-sm'] = `4px 4px 8px ${dark}, -4px -4px 8px ${light}`;
	vars['--neumo-md'] = `6px 6px 14px ${dark}, -6px -6px 14px ${light}`;
	vars['--neumo-lg'] = `10px 10px 22px ${dark}, -10px -10px 22px ${light}`;
	vars['--neumo-xl'] = `16px 16px 32px ${dark}, -16px -16px 32px ${light}`;
	// プレス時の凹み（inset）
	vars['--neumo-inset'] = `inset 4px 4px 8px ${dark}, inset -4px -4px 8px ${light}`;
	vars['--neumo-inset-lg'] = `inset 6px 6px 14px ${dark}, inset -6px -6px 14px ${light}`;
}

/**
 * 1 つの ThemePack から CSS カスタムプロパティの宣言を生成
 * 重複キーがある場合は上書き（最後勝ち）
 */
function emitScale(
	vars: Record<string, string>,
	scale:
		| {
				50: string;
				100: string;
				200: string;
				300: string;
				400: string;
				500: string;
				600: string;
				700: string;
				800: string;
				900: string;
		  }
		| undefined,
	prefix: string
) {
	if (!scale) return;
	for (const [k, v] of Object.entries(scale)) {
		vars[`--color-${prefix}-${k}`] = v;
	}
}

export function packToCssVars(pack: ThemePack): Record<string, string> {
	const vars: Record<string, string> = {};

	// 5 つのカラースケール (primary 必須、他 4 つは任意)
	emitScale(vars, pack.ui.primary, 'primary');
	emitScale(vars, pack.ui.secondary, 'secondary');
	emitScale(vars, pack.ui.success, 'success');
	emitScale(vars, pack.ui.warning, 'warning');
	emitScale(vars, pack.ui.danger, 'danger');

	// surface
	vars['--color-surface-page'] = pack.ui.surface.page;
	vars['--color-surface-card'] = pack.ui.surface.card;
	vars['--color-surface-border'] = pack.ui.surface.border;
	vars['--color-surface-muted'] = pack.ui.surface.muted;
	vars['--color-surface-muted-foreground'] = pack.ui.surface.mutedForeground;

	// nav
	vars['--color-nav-bg'] = pack.ui.nav.bg;
	vars['--color-nav-active'] = pack.ui.nav.active;
	vars['--color-nav-inactive'] = pack.ui.nav.inactive;
	vars['--color-nav-hover'] = pack.ui.nav.hover;

	// shadow
	vars['--shadow-sheet'] = pack.ui.shadow.sheet;
	vars['--shadow-card'] = pack.ui.shadow.card;
	vars['--shadow-modal'] = pack.ui.shadow.modal;

	// radius
	vars['--radius-pill'] = pack.ui.radius.pill;
	vars['--radius-card'] = pack.ui.radius.card;
	vars['--radius-chip'] = pack.ui.radius.chip;

	// palette (course)
	pack.palette.courseColors.forEach((c) => {
		vars[`--color-palette-course-${c.id}`] = c.hex;
	});
	pack.palette.todoColors.forEach((c) => {
		vars[`--color-palette-todo-${c.id}`] = c.hex;
	});
	pack.palette.eventColors.forEach((c) => {
		vars[`--color-palette-event-${c.id}`] = c.hex;
	});

	// attendance
	vars['--color-attendance-present-fg'] = pack.attendance.present.fg;
	vars['--color-attendance-present-bg'] = pack.attendance.present.bg;
	vars['--color-attendance-absent-fg'] = pack.attendance.absent.fg;
	vars['--color-attendance-absent-bg'] = pack.attendance.absent.bg;
	vars['--color-attendance-late-fg'] = pack.attendance.late.fg;
	vars['--color-attendance-late-bg'] = pack.attendance.late.bg;
	vars['--color-attendance-excused-fg'] = pack.attendance.excused.fg;
	vars['--color-attendance-excused-bg'] = pack.attendance.excused.bg;
	vars['--color-attendance-cancelled-fg'] = pack.attendance.cancelled.fg;
	vars['--color-attendance-cancelled-bg'] = pack.attendance.cancelled.bg;

	// typography - font family ID (locale-independent)
	const fontFamilyId = (pack.typography?.fontFamily as FontFamilyId) ?? 'system';
	vars['--font-family-id'] = fontFamilyId;

	// スタイル派生変数（Neumorphism 等がテーマの表面色に追従するために使用）
	emitStyleDerivedVars(vars, pack);

	return vars;
}

/**
 * フォント関連の CSS 変数を適用（ロケール依存）
 * これはロケール変更時に呼び出す必要がある
 */
export function applyFontVars(
	pack: ThemePack,
	locale: string,
	target: HTMLElement = document.documentElement
) {
	const fontFamilyId = (pack.typography?.fontFamily as FontFamilyId) ?? 'system';
	const normalizedLocale = normalizeLocale(locale);
	const fontFamily = resolveFontFamily(fontFamilyId, normalizedLocale);
	target.style.setProperty('--font-family', fontFamily);
	target.style.setProperty('--font-family-id', fontFamilyId);
}

/**
 * ブラウザ環境に CSS 変数を適用
 * SvelteKit / クライアントサイド専用
 */
export function applyTheme(pack: ThemePack, target: HTMLElement = document.documentElement) {
	const vars = packToCssVars(pack);

	// シャドウ制御: 全ての shadow が "none" の場合は全スタイルのシャドウCSS変数を "none" に
	// これにより、shadow-apple-sm / shadow-material-md 等の全クラスが自動的に無効化される
	const shadows = pack.ui.shadow;
	const allNone = shadows.sheet === 'none' && shadows.card === 'none' && shadows.modal === 'none';
	if (allNone) {
		// 全シャドウスタイルの CSS 変数を "none" に設定
		for (const style of ['apple', 'material', 'liquid', 'neumo', 'brutal', 'cyber']) {
			for (const size of ['sm', 'md', 'lg', 'xl']) {
				vars[`--shadow-${style}-${size}`] = 'none';
			}
		}
		vars['--shadow-sheet'] = 'none';
		vars['--shadow-card'] = 'none';
		vars['--shadow-modal'] = 'none';
	}

	for (const [k, v] of Object.entries(vars)) {
		target.style.setProperty(k, v);
	}
	// データ属性も付与 (CSS の :root[data-theme=dark] などで使用可能)
	target.setAttribute('data-theme', pack.id);

	// シャドウ制御用クラス（CSS ルールの二重の安全策）
	target.classList.toggle('theme-shadows-none', allNone);
	target.setAttribute('data-theme-shadows', allNone ? 'none' : 'all');
}

/**
 * 適用した CSS 変数を削除
 */
export function clearTheme(target: HTMLElement = document.documentElement) {
	// シャドウ制御属性をクリア
	target.removeAttribute('data-theme-shadows');
	const vars = packToCssVars({
		id: '',
		name: '',
		version: 1,
		ui: {
			primary: {
				50: '',
				100: '',
				200: '',
				300: '',
				400: '',
				500: '',
				600: '',
				700: '',
				800: '',
				900: ''
			},
			surface: { page: '', card: '', border: '', muted: '', mutedForeground: '' },
			nav: { bg: '', active: '', inactive: '', hover: '' },
			shadow: { sheet: '', card: '', modal: '' },
			radius: { pill: '', card: '', chip: '' }
		},
		palette: { courseColors: [], todoColors: [], eventColors: [] },
		attendance: {
			present: { fg: '', bg: '' },
			absent: { fg: '', bg: '' },
			late: { fg: '', bg: '' },
			excused: { fg: '', bg: '' },
			cancelled: { fg: '', bg: '' }
		}
	});
	for (const k of Object.keys(vars)) {
		target.style.removeProperty(k);
	}
	target.removeAttribute('data-theme');
	target.classList.remove('theme-shadows-none');
}
