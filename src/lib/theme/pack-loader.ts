/**
 * テーマパックのロード + バリデーション
 * JSON から ThemePack を読み込み、構造とバージョンをチェックする
 */

import type { ThemePack, ValidationResult, ThemeValidationError } from './types';
import defaultPack from './defaults/default.json' with { type: 'json' };
import uecPack from './defaults/uec.json' with { type: 'json' };
import dangoPack from './defaults/dango.json' with { type: 'json' };
import darkPack from './defaults/dark.json' with { type: 'json' };

/** HEX カラー正規表現 (#RGB or #RRGGBB) */
const HEX_RE = /^#([0-9a-fA-F]{3}|[0-9a-fA-F]{6})$/;

function err(path: string, message: string): ThemeValidationError {
	return { path, message };
}

function isString(v: unknown): v is string {
	return typeof v === 'string';
}

function isHex(v: unknown): v is string {
	return isString(v) && HEX_RE.test(v);
}

function validateColorEntry(v: unknown, path: string, errors: ThemeValidationError[]): boolean {
	if (typeof v !== 'object' || v === null) {
		errors.push(err(path, 'must be an object'));
		return false;
	}
	const o = v as Record<string, unknown>;
	if (!isString(o.id)) errors.push(err(`${path}.id`, 'must be a string'));
	if (!isString(o.name)) errors.push(err(`${path}.name`, 'must be a string'));
	if (!isHex(o.hex)) errors.push(err(`${path}.hex`, 'must be a valid hex color'));
	return errors.filter((e) => e.path.startsWith(path)).length === 0;
}

function validateAttendance(v: unknown, path: string, errors: ThemeValidationError[]): boolean {
	if (typeof v !== 'object' || v === null) {
		errors.push(err(path, 'must be an object'));
		return false;
	}
	const o = v as Record<string, unknown>;
	const required: (keyof typeof o)[] = ['present', 'absent', 'late', 'excused', 'cancelled'];
	let ok = true;
	for (const key of required) {
		const sub = o[key];
		if (typeof sub !== 'object' || sub === null) {
			errors.push(err(`${path}.${key}`, 'must be an object'));
			ok = false;
			continue;
		}
		const s = sub as Record<string, unknown>;
		if (!isHex(s.fg)) {
			errors.push(err(`${path}.${key}.fg`, 'must be a valid hex color'));
			ok = false;
		}
		if (!isHex(s.bg)) {
			errors.push(err(`${path}.${key}.bg`, 'must be a valid hex color'));
			ok = false;
		}
	}
	return ok;
}

function validateUI(v: unknown, path: string, errors: ThemeValidationError[]): boolean {
	if (typeof v !== 'object' || v === null) {
		errors.push(err(path, 'must be an object'));
		return false;
	}
	const o = v as Record<string, unknown>;

	// primary scale
	const primary = o.primary as Record<string, unknown> | undefined;
	if (typeof primary !== 'object' || primary === null) {
		errors.push(err(`${path}.primary`, 'must be an object'));
		return false;
	}
	const scales = [50, 100, 200, 300, 400, 500, 600, 700, 800, 900];
	for (const s of scales) {
		if (!isHex(primary[String(s)])) {
			errors.push(err(`${path}.primary.${s}`, 'must be a valid hex color'));
		}
	}

	// surface, nav, shadow, radius
	const surface = o.surface as Record<string, unknown> | undefined;
	if (surface) {
		for (const k of ['page', 'card', 'border', 'muted', 'mutedForeground']) {
			if (!isHex(surface[k])) errors.push(err(`${path}.surface.${k}`, 'must be a valid hex color'));
		}
	}

	const nav = o.nav as Record<string, unknown> | undefined;
	if (nav) {
		for (const k of ['bg', 'active', 'inactive', 'hover']) {
			if (!isHex(nav[k])) errors.push(err(`${path}.nav.${k}`, 'must be a valid hex color'));
		}
	}

	const shadow = o.shadow as Record<string, unknown> | undefined;
	if (shadow) {
		for (const k of ['sheet', 'card', 'modal']) {
			if (!isString(shadow[k])) errors.push(err(`${path}.shadow.${k}`, 'must be a string'));
		}
	}

	const radius = o.radius as Record<string, unknown> | undefined;
	if (radius) {
		for (const k of ['pill', 'card', 'chip']) {
			if (!isString(radius[k])) errors.push(err(`${path}.radius.${k}`, 'must be a string'));
		}
	}

	return errors.filter((e) => e.path.startsWith(path)).length === 0;
}

function validatePalette(v: unknown, path: string, errors: ThemeValidationError[]): boolean {
	if (typeof v !== 'object' || v === null) {
		errors.push(err(path, 'must be an object'));
		return false;
	}
	const o = v as Record<string, unknown>;
	const groups: (keyof typeof o)[] = ['courseColors', 'todoColors', 'eventColors'];
	let ok = true;
	for (const g of groups) {
		const arr = o[g];
		if (!Array.isArray(arr)) {
			errors.push(err(`${path}.${g}`, 'must be an array'));
			ok = false;
			continue;
		}
		arr.forEach((entry, i) => {
			validateColorEntry(entry, `${path}.${g}[${i}]`, errors);
		});
	}
	return ok;
}

/**
 * 不明な JSON を ThemePack として検証する
 */
export function validatePack(data: unknown): ValidationResult {
	const errors: ThemeValidationError[] = [];
	if (typeof data !== 'object' || data === null) {
		return { ok: false, errors: [err('', 'must be an object')] };
	}
	const o = data as Record<string, unknown>;

	if (!isString(o.id)) errors.push(err('id', 'must be a string'));
	if (!isString(o.name)) errors.push(err('name', 'must be a string'));
	if (o.version !== 1) errors.push(err('version', 'must be 1'));

	validateUI(o.ui, 'ui', errors);
	validatePalette(o.palette, 'palette', errors);
	validateAttendance(o.attendance, 'attendance', errors);

	if (errors.length > 0) {
		return { ok: false, errors };
	}
	return { ok: true, pack: data as ThemePack };
}

/** 同梱の組み込みパック一覧 */
export const BUILTIN_PACKS: readonly ThemePack[] = [
	defaultPack as ThemePack,
	uecPack as ThemePack,
	dangoPack as ThemePack,
	darkPack as ThemePack
] as const;

/**
 * ID から組み込みパックを取得
 */
export function getBuiltinPack(id: string): ThemePack | undefined {
	return BUILTIN_PACKS.find((p) => p.id === id);
}

/**
 * 任意のパスから JSON を fetch して ThemePack を取得
 */
export async function loadPackFromUrl(url: string): Promise<ValidationResult> {
	try {
		const res = await fetch(url);
		if (!res.ok) {
			return { ok: false, errors: [err('', `HTTP ${res.status}`)] };
		}
		const data = await res.json();
		return validatePack(data);
	} catch (e) {
		return {
			ok: false,
			errors: [err('', e instanceof Error ? e.message : 'unknown error')]
		};
	}
}

/**
 * 文字列 JSON をパースして ThemePack にする
 */
export function parsePack(json: string): ValidationResult {
	try {
		const data = JSON.parse(json);
		return validatePack(data);
	} catch (e) {
		return {
			ok: false,
			errors: [err('', e instanceof Error ? e.message : 'invalid JSON')]
		};
	}
}
