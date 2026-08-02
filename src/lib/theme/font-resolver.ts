/**
 * src/lib/theme/font-resolver.ts
 *
 * フォントファミリー解決ユーティリティ。
 * 設定ID + ロケールから CSS font-family 文字列を生成する。
 *
 * 以前は +layout.svelte の $derived でインライン定義されていたが、
 * テーマの typography.fontFamily 統合に伴い分離・テスト可能にした。
 */

export type FontFamilyId = 'system' | 'outfit' | 'sawarabi' | 'noto' | 'sans' | 'serif';

export type FontLocale = 'ja' | 'en' | 'zh' | 'ko';

/**
 * 利用可能なフォントファミリーID一覧。
 * 設定画面のセレクトボックス等で使用。
 */
export const AVAILABLE_FONT_FAMILIES: FontFamilyId[] = [
	'system',
	'outfit',
	'sawarabi',
	'noto',
	'sans',
	'serif'
];

/**
 * FontFamilyId から表示名を取得する（英語）。
 * i18n が必要な場合は messages/*.json のキー `settings_font_family_${id}` を使用すること。
 */
export function getFontFamilyDisplayName(id: FontFamilyId): string {
	const names: Record<FontFamilyId, string> = {
		system: 'System',
		outfit: 'Outfit',
		sawarabi: 'Sawarabi Gothic',
		noto: 'Noto Sans',
		sans: 'Sans Serif',
		serif: 'Serif'
	};
	return names[id] ?? 'System';
}

/**
 * ロケール文字列を正規化する。
 * 未知のロケールは 'en' にフォールバック。
 */
export function normalizeLocale(locale: string): FontLocale {
	if (locale === 'ja' || locale === 'en' || locale === 'zh' || locale === 'ko') {
		return locale;
	}
	// 'zh-CN', 'zh-TW' → 'zh'
	if (locale.startsWith('zh')) return 'zh';
	if (locale.startsWith('ko')) return 'ko';
	return 'en';
}

/**
 * FontFamilyId + ロケールから CSS font-family 文字列を解決する。
 *
 * @example
 * ```ts
 * resolveFontFamily('system', 'ja')
 * // → "font-family: -apple-system, BlinkMacSystemFont, 'SF Pro Display', ..."
 * resolveFontFamily('outfit', 'ja')
 * // → "font-family: 'Outfit', 'Noto Sans JP', sans-serif;"
 * ```
 */
export function resolveFontFamily(id: FontFamilyId, locale: FontLocale): string {
	switch (id) {
		case 'sans':
		case 'noto':
			return resolveSansFont(locale);

		case 'serif':
			return resolveSerifFont(locale);

		case 'sawarabi':
			return resolveSawarabiFont(locale);

		case 'outfit':
			return resolveOutfitFont(locale);

		case 'system':
		default:
			return resolveSystemFont(locale);
	}
}

function resolveSansFont(locale: FontLocale): string {
	switch (locale) {
		case 'zh':
			return "'Noto Sans SC','PingFang SC','Microsoft YaHei',sans-serif";
		case 'ko':
			return "'Noto Sans KR','Malgun Gothic','Apple SD Gothic Neo',sans-serif";
		case 'ja':
			return "'Noto Sans JP',sans-serif";
		default:
			return "'Noto Sans',system-ui,-apple-system,sans-serif";
	}
}

function resolveSerifFont(locale: FontLocale): string {
	switch (locale) {
		case 'zh':
			return "'Noto Serif SC','Songti SC','SimSun',serif";
		case 'ko':
			return "'Noto Serif KR','Batang','AppleMyungjo',serif";
		case 'ja':
			return "'Noto Serif JP','BIZ UDMincho',serif";
		default:
			return "'Noto Serif',Georgia,'Times New Roman',serif";
	}
}

function resolveSawarabiFont(locale: FontLocale): string {
	// さわらびゴシックは和文特化。中文/韓文は Noto フォールバック
	switch (locale) {
		case 'zh':
			return "'Sawarabi Gothic','Noto Sans SC','PingFang SC',sans-serif";
		case 'ko':
			return "'Sawarabi Gothic','Noto Sans KR','Malgun Gothic',sans-serif";
		default:
			return "'Sawarabi Gothic','Noto Sans JP',sans-serif";
	}
}

function resolveOutfitFont(locale: FontLocale): string {
	// Outfit は欧文書体。CJK は Noto フォールバック
	switch (locale) {
		case 'zh':
			return "'Outfit','Noto Sans SC','PingFang SC','Microsoft YaHei',sans-serif";
		case 'ko':
			return "'Outfit','Noto Sans KR','Malgun Gothic','Apple SD Gothic Neo',sans-serif";
		case 'ja':
			return "'Outfit','Noto Sans JP',sans-serif";
		default:
			return "'Outfit',system-ui,-apple-system,sans-serif";
	}
}

function resolveSystemFont(locale: FontLocale): string {
	switch (locale) {
		case 'zh':
			return "-apple-system,BlinkMacSystemFont,'SF Pro Text','PingFang SC','Microsoft YaHei',sans-serif";
		case 'ko':
			return "-apple-system,BlinkMacSystemFont,'SF Pro Text','Apple SD Gothic Neo','Malgun Gothic',sans-serif";
		default:
			return "-apple-system,BlinkMacSystemFont,'SF Pro Display','SF Pro Text','Helvetica Neue',sans-serif";
	}
}
