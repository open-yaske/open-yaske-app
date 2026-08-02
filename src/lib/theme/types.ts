/**
 * テーマパックの型定義
 * 1 つの ThemePack は UI 色 / 科目色 / TODO 色 / イベント色 / 出席色を統合管理する
 */

export type ColorScale = {
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
};

export type LocalizedString =
	| string
	| {
			ja?: string;
			en?: string;
			zh?: string;
			ko?: string;
			[locale: string]: string | undefined;
	  };

export type ColorEntry = {
	id: string;
	/** i18n key or translation object */
	name: LocalizedString;
	hex: string;
	isCustom?: boolean;
};

export type AttendanceColor = {
	fg: string;
	bg: string;
};

export type AttendanceColors = {
	present: AttendanceColor;
	absent: AttendanceColor;
	late: AttendanceColor;
	excused: AttendanceColor;
	cancelled: AttendanceColor;
};

export type ThemeUI = {
	primary: ColorScale;
	secondary?: ColorScale;
	success?: ColorScale;
	warning?: ColorScale;
	danger?: ColorScale;
	surface: {
		page: string;
		card: string;
		border: string;
		muted: string;
		mutedForeground: string;
	};
	nav: {
		bg: string;
		active: string;
		inactive: string;
		hover: string;
	};
	shadow: {
		sheet: string;
		card: string;
		modal: string;
	};
	radius: {
		pill: string;
		card: string;
		chip: string;
	};
};

export type ThemePalette = {
	courseColors: ColorEntry[];
	todoColors: ColorEntry[];
	eventColors: ColorEntry[];
};

/** テーマのタイポグラフィ設定 */
export type ThemeTypography = {
	/** テーマのデフォルトフォントファミリーID */
	fontFamily?: 'system' | 'outfit' | 'sawarabi' | 'serif' | 'sans' | 'noto';
};

/** テーマのメタ情報 */
export type ThemeMeta = {
	/** カラースキーム（ライト/ダーク） */
	colorScheme?: 'light' | 'dark';
	/** 作者名（サードパティテーマ向け） */
	author?: string;
	/** テーマの説明（ローカライズ対応） */
	description?: LocalizedString;
};

export type ThemePack = {
	id: string;
	/** i18n key or translation object */
	name: LocalizedString;
	description?: LocalizedString;
	version: 1;
	ui: ThemeUI;
	palette: ThemePalette;
	attendance: AttendanceColors;
	builtIn?: boolean;
	/** タイポグラフィ設定（フォントファミリー等） */
	typography?: ThemeTypography;
	/** メタ情報（カラースキーム・作者等） */
	meta?: ThemeMeta;
};

/**
 * テーマパック検証エラー
 */
export type ThemeValidationError = {
	path: string;
	message: string;
};

/**
 * 検証結果
 */
export type ValidationResult =
	| { ok: true; pack: ThemePack }
	| { ok: false; errors: ThemeValidationError[] };
