import { describe, it, expect, vi } from 'vitest';
import { BUILTIN_PACKS } from '../pack-loader';
import { isThemeShadowsNone, applyTheme, packToCssVars } from '../css-bridge';
import {
	resolveThemePackId,
	migrateLegacyThemePackId,
	resolveEffectiveFontFamily,
	isDarkPack,
	getLocalizedText
} from '../utils';
import { defaultColorId, resolveColorHex } from '../tokens';
import { mergeOverrides } from '$lib/stores/theme.svelte';

function fakeTarget() {
	const set: [string, string][] = [];
	return {
		set,
		target: {
			style: { setProperty: vi.fn((k: string, v: string) => void set.push([k, v])) },
			classList: { toggle: vi.fn() },
			setAttribute: vi.fn(),
			removeAttribute: vi.fn()
		} as unknown as HTMLElement
	};
}

const hc = BUILTIN_PACKS.find((p) => p.id === 'high-contrast')!;
const def = BUILTIN_PACKS.find((p) => p.id === 'default')!;

describe('isThemeShadowsNone', () => {
	it('high-contrast pack has no shadows', () => {
		expect(isThemeShadowsNone(hc)).toBe(true);
	});
	it('default pack has shadows', () => {
		expect(isThemeShadowsNone(def)).toBe(false);
	});
});

describe('applyTheme shadow kill-switch', () => {
	it('toggles theme-shadows-none on for all-none packs', () => {
		const { target } = fakeTarget();
		applyTheme(hc, target);
		expect(target.classList.toggle).toHaveBeenCalledWith('theme-shadows-none', true);
		expect(target.setAttribute).toHaveBeenCalledWith('data-theme-shadows', 'none');
	});
	it('toggles off for normal packs', () => {
		const { target } = fakeTarget();
		applyTheme(def, target);
		expect(target.classList.toggle).toHaveBeenCalledWith('theme-shadows-none', false);
	});
	it('does not set per-style shadow vars (class kill-switch is the single mechanism)', () => {
		const { target, set } = fakeTarget();
		applyTheme(hc, target);
		const keys = set.map(([k]) => k);
		expect(keys).toContain('--shadow-card');
		expect(keys.some((k) => k.startsWith('--shadow-apple-'))).toBe(false);
		expect(keys.some((k) => k.startsWith('--shadow-material-'))).toBe(false);
	});
	it('packToCssVars still exposes the theme own shadows', () => {
		const vars = packToCssVars(def);
		expect(vars['--shadow-card']).toBeTruthy();
	});
});

describe('resolveThemePackId', () => {
	it('dark mode picks dark pack', () => {
		expect(
			resolveThemePackId({ theme: 'dark', themePackIdLight: 'default', themePackIdDark: 'dark' })
		).toBe('dark');
	});
	it('light mode picks light pack', () => {
		expect(
			resolveThemePackId({ theme: 'light', themePackIdLight: 'default', themePackIdDark: 'dark' })
		).toBe('default');
	});
	it('system mode follows osDark flag (no window dependency)', () => {
		const s = { theme: 'system', themePackIdLight: 'default', themePackIdDark: 'dark' } as const;
		expect(resolveThemePackId(s, true)).toBe('dark');
		expect(resolveThemePackId(s, false)).toBe('default');
	});
});

describe('migrateLegacyThemePackId', () => {
	it('maps legacy dark to light=default/dark=dark', () => {
		expect(migrateLegacyThemePackId({ themePackId: 'dark' })).toEqual({
			themePackIdLight: 'default',
			themePackIdDark: 'dark'
		});
	});
	it('maps legacy custom id to light, dark stays dark', () => {
		expect(migrateLegacyThemePackId({ themePackId: 'uec' })).toEqual({
			themePackIdLight: 'uec',
			themePackIdDark: 'dark'
		});
	});
	it('returns undefined when already migrated', () => {
		expect(
			migrateLegacyThemePackId({ themePackIdLight: 'uec', themePackIdDark: 'dark' })
		).toBeUndefined();
	});
});

describe('resolveEffectiveFontFamily', () => {
	it('user choice wins unless system (= follow pack)', () => {
		expect(resolveEffectiveFontFamily('serif', 'sawarabi')).toBe('serif');
		expect(resolveEffectiveFontFamily('system', 'sawarabi')).toBe('sawarabi');
		expect(resolveEffectiveFontFamily('system', undefined)).toBe('system');
	});
});

describe('mergeOverrides', () => {
	it('returns same ref when no overrides, clones otherwise', () => {
		expect(mergeOverrides(def, undefined)).toBe(def);
		const out = mergeOverrides(def, { primaryColor: '#ff0000' });
		expect(out).not.toBe(def);
		expect(out.ui.primary[500]).toBe('#ff0000');
		expect(def.ui.primary[500]).not.toBe('#ff0000');
	});
});

describe('theme name honesty (敵対的)', () => {
	it('全パックのパレット名が実名オブジェクトで解決できる', () => {
		for (const pack of BUILTIN_PACKS) {
			for (const c of [
				...pack.palette.courseColors,
				...pack.palette.todoColors,
				...pack.palette.eventColors
			]) {
				const label = getLocalizedText(c.name, 'ja');
				expect(label, `${pack.id}:${c.id} の名前が解決できない`).toBeTruthy();
				expect(label.startsWith('color_'), `${pack.id}:${c.id} が未定義キー ${label}`).toBe(false);
			}
		}
	});
	it('default の pink は red と別色（名前通り）', () => {
		const pink = def.palette.eventColors.find((c) => c.id === 'pink')!;
		const red = def.palette.eventColors.find((c) => c.id === 'red')!;
		expect(pink.hex.toLowerCase()).not.toBe(red.hex.toLowerCase());
	});
	it('未知色トークンは transparent にならず先頭色に落ちる', () => {
		const dango = BUILTIN_PACKS.find((p) => p.id === 'dango')!;
		// 'blue' は dango に存在しない旧形式トークン
		expect(resolveColorHex(dango, 'blue')).not.toBe('transparent');
		expect(resolveColorHex(dango, 'blue')).toBe(dango.palette.courseColors[0].hex);
	});
	it('defaultColorId は各 kind の先頭 id を返す', () => {
		expect(defaultColorId(def, 'course')).toBe(def.palette.courseColors[0].id);
		expect(defaultColorId(def, 'todo')).toBe(def.palette.todoColors[0].id);
		expect(defaultColorId(def, 'event')).toBe(def.palette.eventColors[0].id);
	});
});

describe('isDarkPack (敵対的)', () => {
	it('dark / high-contrast-dark を dark 判定、他を light 判定', () => {
		const byId = (id: string) => BUILTIN_PACKS.find((p) => p.id === id)!;
		expect(isDarkPack(byId('dark'))).toBe(true);
		expect(isDarkPack(byId('high-contrast-dark'))).toBe(true);
		expect(isDarkPack(byId('default'))).toBe(false);
		expect(isDarkPack(byId('high-contrast'))).toBe(false);
		expect(isDarkPack(byId('dango'))).toBe(false);
		expect(isDarkPack(byId('uec'))).toBe(false);
	});
	it('meta 未設定でも surface 輝度でフォールバックする', () => {
		const darkLike = {
			meta: {},
			ui: { surface: { page: '#000000' } }
		} as never;
		const lightLike = {
			meta: {},
			ui: { surface: { page: '#ffffff' } }
		} as never;
		expect(isDarkPack(darkLike)).toBe(true);
		expect(isDarkPack(lightLike)).toBe(false);
	});
});
