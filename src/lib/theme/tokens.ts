/**
 * パレット / カラー解決
 * テーマパックの `palette` から ColorEntry を引き、ID / HEX を解決する
 */

import type { ColorEntry, ThemePack, ThemePalette } from './types';

/** 特別な「なし」トークン */
export const NONE_TOKEN = 'none' as const;
export type ColorToken = string; // id, "custom:<hex>", or "none"

/**
 * パレットから ID で ColorEntry を引く
 * @returns 見つからなければ undefined
 */
export function getColorById(palette: ThemePalette, id: string): ColorEntry | undefined {
	// courseColors に存在すればそれを優先（科目色の基本）
	const fromCourse = palette.courseColors.find((c) => c.id === id);
	if (fromCourse) return fromCourse;
	// todoColors
	const fromTodo = palette.todoColors.find((c) => c.id === id);
	if (fromTodo) return fromTodo;
	// eventColors
	const fromEvent = palette.eventColors.find((c) => c.id === id);
	return fromEvent;
}

/**
 * 任意の "kind" (course / todo / event) と ID から ColorEntry を引く
 */
export function getColor(
	pack: ThemePack,
	kind: 'course' | 'todo' | 'event',
	id: string
): ColorEntry | undefined {
	const list =
		kind === 'course'
			? pack.palette.courseColors
			: kind === 'todo'
				? pack.palette.todoColors
				: pack.palette.eventColors;
	return list.find((c) => c.id === id);
}

/**
 * ColorToken (id / "custom:<hex>" / "none") を HEX に解決
 * カスタム HEX を含む "custom:<hex>" 形式にも対応
 */
export function resolveColorHex(pack: ThemePack, token: string | null | undefined): string {
	if (!token || token === NONE_TOKEN) return 'transparent';
	if (token.startsWith('custom:')) return token.slice('custom:'.length);
	if (token.startsWith('index:')) {
		const parts = token.split(':');
		if (parts.length >= 3) {
			const kind = parts[1];
			const idx = parseInt(parts[2], 10);
			const list =
				kind === 'course'
					? pack.palette.courseColors
					: kind === 'todo'
						? pack.palette.todoColors
						: pack.palette.eventColors;
			const found = list[idx];
			return found?.hex ?? 'transparent';
		}
	}
	const found = getColorById(pack.palette, token);
	return found?.hex ?? 'transparent';
}

/**
 * カラー ID を表示用の名前（i18n key）に解決
 */
export function resolveColorName(pack: ThemePack, token: string | null | undefined): string {
	if (!token || token === NONE_TOKEN) return 'color_none';
	if (token.startsWith('custom:')) return 'color_custom';
	if (token.startsWith('index:')) {
		const parts = token.split(':');
		if (parts.length >= 3) {
			const kind = parts[1];
			const idx = parseInt(parts[2], 10);
			const list =
				kind === 'course'
					? pack.palette.courseColors
					: kind === 'todo'
						? pack.palette.todoColors
						: pack.palette.eventColors;
			const found = list[idx];
			return found?.name ?? 'color_none';
		}
	}
	const found = getColorById(pack.palette, token);
	return found?.name ?? 'color_none';
}

/**
 * カスタム HEX かどうかを判定
 */
export function isCustomColor(token: string | null | undefined): boolean {
	return !!token && token.startsWith('custom:');
}
