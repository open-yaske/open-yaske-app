/**
 * ThemePack を CSS 変数に変換し、document.documentElement に適用する
 */

import type { ThemePack } from './types';

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

	return vars;
}

/**
 * ブラウザ環境に CSS 変数を適用
 * SvelteKit / クライアントサイド専用
 */
export function applyTheme(pack: ThemePack, target: HTMLElement = document.documentElement) {
	const vars = packToCssVars(pack);
	for (const [k, v] of Object.entries(vars)) {
		target.style.setProperty(k, v);
	}
	// データ属性も付与 (CSS の :root[data-theme=dark] などで使用可能)
	target.setAttribute('data-theme', pack.id);
}

/**
 * 適用した CSS 変数を削除
 */
export function clearTheme(target: HTMLElement = document.documentElement) {
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
}
