import { m } from '$lib/paraglide/messages';
import type { LocalizedString, ThemePack } from './types';
import type { FontFamilyId } from './font-resolver';
import type { UserSettings } from '$lib/types';
import { calculateLuminance, parseColor } from './contrast';

/**
 * パックがダーク系かを判定する。id 比較ではなく meta.colorScheme を正とし、
 * 未設定のカスタムパックは surface.page の輝度でフォールバックする。
 * WeekGrid / GlassContainer / timetable PNG 等の分岐をここに一本化する。
 */
export function isDarkPack(pack: Pick<ThemePack, 'meta' | 'ui'>): boolean {
	if (pack.meta?.colorScheme === 'dark') return true;
	if (pack.meta?.colorScheme === 'light') return false;
	try {
		return calculateLuminance(parseColor(pack.ui.surface.page)) < 0.4;
	} catch {
		return false;
	}
}

/** ライト/ダークのモードから使うパック ID を解決する（単一の真実）。 */
export function resolveThemePackId(
	settings: {
		theme: 'light' | 'dark' | 'system';
		themePackIdLight?: string;
		themePackIdDark?: string;
	},
	osDark = isOsDark()
): string {
	if (settings.theme === 'dark') return settings.themePackIdDark || 'dark';
	if (settings.theme === 'system' && osDark) return settings.themePackIdDark || 'dark';
	return settings.themePackIdLight || 'default';
}

function isOsDark(): boolean {
	return (
		typeof window !== 'undefined' &&
		typeof window.matchMedia === 'function' &&
		window.matchMedia('(prefers-color-scheme: dark)').matches
	);
}

/** 旧 themePackId 単一指定を Light/Dark の組へ移行する。移行不要なら undefined。 */
export function migrateLegacyThemePackId(settings: {
	themePackId?: string;
	themePackIdLight?: string;
	themePackIdDark?: string;
}): { themePackIdLight: string; themePackIdDark: string } | undefined {
	if (settings.themePackId && !settings.themePackIdLight && !settings.themePackIdDark) {
		return settings.themePackId === 'dark'
			? { themePackIdLight: 'default', themePackIdDark: 'dark' }
			: { themePackIdLight: settings.themePackId, themePackIdDark: 'dark' };
	}
	return undefined;
}

/**
 * 実効フォントを解決する。ユーザー設定 'system' は「パックに従う」の意味。
 * settings.fontFamily は残すが、themeOverrides.fontFamily があればそちらを優先する。
 */
export function resolveEffectiveFontFamily(
	userFont: UserSettings['fontFamily'],
	packFont: FontFamilyId | undefined,
	overrideFont?: FontFamilyId
): FontFamilyId {
	if (overrideFont) return overrideFont;
	if (userFont && userFont !== 'system') return userFont as FontFamilyId;
	return packFont ?? 'system';
}

/**
 * LocalizedString (文字列または言語キーのマップ) を指定のロケールに合わせて解決します。
 * @param text 対象 of 文字列またはオブジェクト
 * @param locale 解決するロケール (ja, en, zh, ko など)
 * @returns 翻訳された文字列
 */
export function getLocalizedText(text: LocalizedString | undefined, locale: string): string {
	if (!text) return '';
	if (typeof text === 'string') {
		// Paraglide のメッセージキー (theme_pack_default など) である場合は解決
		const key = text as keyof typeof m;
		if (key in m && typeof m[key] === 'function') {
			return (m[key] as () => string)();
		}
		return text;
	}
	if (typeof text === 'object' && text !== null) {
		return text[locale] || text['ja'] || Object.values(text)[0] || '';
	}
	return '';
}
