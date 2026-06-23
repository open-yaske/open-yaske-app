/**
 * $lib の public API バレル
 */

// 共通型
export type {
	DayOfWeek,
	Period,
	AttendanceStatus,
	ColorToken,
	Course,
	Todo,
	CalendarEvent,
	Assignment,
	AttendanceRecord,
	CustomColor,
	ExternalLink,
	ExternalLinkCategory,
	ThemePackId,
	UserSettings
} from './types';
export {
	DEFAULT_USER_SETTINGS,
	DAY_OF_WEEK_LIST,
	PERIOD_LIST,
	ATTENDANCE_STATUS_LIST
} from './types';
export type {
	PublicData,
	PublicCourse,
	PublicIndex,
	DashboardCard,
	HtmlInternalCard,
	ExternalUrlCard,
	WebviewPopupCard,
	CardLayout
} from './types/public';
export { PUBLIC_DATA_SCHEMA_VERSION } from './types/public';

// データレイヤー
export * from './attendance';
export * from './holidays';
export * from './weather';
export * from './html-renderer';
export * from './markdown';
export * from './publicData';
export * from './pdf';
export * as remoteStorage from './remotestorage';

// テーマ（ColorToken は types と重複するので type のみ）
export type {
	ThemePack,
	ThemeUI,
	ThemePalette,
	ColorEntry,
	ColorScale,
	AttendanceColor,
	AttendanceColors,
	ThemeValidationError,
	ValidationResult
} from './theme';
export {
	NONE_TOKEN,
	getColorById,
	getColor,
	resolveColorHex,
	resolveColorName,
	isCustomColor,
	BUILTIN_PACKS,
	getBuiltinPack,
	validatePack,
	loadPackFromUrl,
	parsePack,
	packToCssVars,
	applyTheme,
	clearTheme
} from './theme';

// ストア
export * from './stores';
