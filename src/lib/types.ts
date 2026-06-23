/**
 * アプリ全体で使う共通型定義
 */

// ----- 曜日・時限 -----
export type DayOfWeek = 'mon' | 'tue' | 'wed' | 'thu' | 'fri' | 'sat' | 'sun';
export type Period = 1 | 2 | 3 | 4 | 5 | 6 | 7;

export const DAY_OF_WEEK_LIST: readonly DayOfWeek[] = [
	'mon',
	'tue',
	'wed',
	'thu',
	'fri',
	'sat',
	'sun'
] as const;
export const PERIOD_LIST: readonly Period[] = [1, 2, 3, 4, 5, 6, 7] as const;

// ----- 出席状態（5 値）-----
export type AttendanceStatus = 'present' | 'absent' | 'late' | 'excused' | 'cancelled';
export const ATTENDANCE_STATUS_LIST: readonly AttendanceStatus[] = [
	'present',
	'absent',
	'late',
	'excused',
	'cancelled'
] as const;

// ----- カラー ID -----
/** パレット内の color id、または "custom:<hex>" / "none" */
export type ColorToken = string;

// ----- 科目 (Course) -----
export type CourseType = 'free' | 'required' | 'elective';
export type Semester = 'spring' | 'fall' | 'other';

export type Course = {
	id: string;
	code?: string;
	name: string;
	teacher?: string;
	classroom?: string;
	url?: string;
	memo?: string;
	type: CourseType;
	credits: 0 | 1 | 2;
	period: Period;
	day: DayOfWeek;
	span: 1 | 2 | 3;
	color: ColorToken;
	syllabusId?: string;
	createdAt: number;
	updatedAt: number;
};

// ----- TODO -----
export type RepeatRule = {
	freq: 'daily' | 'weekly' | 'monthly';
	count?: number;
	until?: number;
};

export type Todo = {
	id: string;
	name: string;
	done: boolean;
	courseId?: string;
	location?: string;
	assignee?: string;
	memo?: string;
	color: ColorToken;
	dueAt?: number;
	notifyMinutes?: number;
	priority?: 'low' | 'mid' | 'high';
	list?: string;
	repeat?: RepeatRule;
	createdAt: number;
	updatedAt: number;
};

// ----- カレンダーイベント -----
export type CalendarEvent = {
	id: string;
	title: string;
	allDay: boolean;
	startAt: number;
	endAt: number;
	location?: string;
	memo?: string;
	calendarId: string;
	color: ColorToken;
	/** 休講 (カレンダー上でグレー表示・タップ無効化) */
	cancelled?: boolean;
	createdAt: number;
	updatedAt: number;
};

// ----- 課題・テスト -----
export type Assignment = {
	id: string;
	courseId: string;
	kind: 'assignment' | 'test';
	title: string;
	dueAt?: number;
	done: boolean;
	createdAt: number;
	updatedAt: number;
};

// ----- 出席記録 -----
export type AttendanceRecord = {
	id: string;
	courseId: string;
	/** YYYY-MM-DD */
	date: string;
	status: AttendanceStatus;
	createdAt: number;
};

// ----- カスタムカラー -----
export type CustomColor = {
	id: string;
	hex: string;
	name?: string;
	createdAt: number;
};

// ----- 外部リンク -----
export type ExternalLinkCategory = 'study' | 'campus' | 'tools' | 'other';
export type SearchProviderId = 'google' | 'bing' | 'duckduckgo';

export type ExternalLink = {
	id: string;
	name: string;
	url: string;
	icon: string;
	category: ExternalLinkCategory;
	order: number;
};

// ----- ユーザー設定 -----
export type ThemePackId = 'default' | 'uec' | 'dango' | 'dark' | string;

export type PeriodTime = {
	start: string; // "HH:MM"
	end: string; // "HH:MM"
};

export type AttendanceNotificationSettings = {
	enabled: boolean;
	trigger: 'start' | 'end' | 'both';
	startOffsetMinutes: number;
	endOffsetMinutes: number;
};

export type UserSettings = {
	themePackId: ThemePackId;
	themePackIdLight: ThemePackId;
	themePackIdDark: ThemePackId;
	theme: 'light' | 'dark' | 'system';
	locale: 'ja' | 'en' | 'zh' | 'ko';
	semester: Semester;
	year: number;
	lat: number;
	lng: number;
	locationName: string;
	homeShowWeather: boolean;
	homeShowTools: boolean;
	homeShowUecReview: boolean;
	homeShowExternalLinks: boolean;
	openLinksInApp: boolean;
	homeSearchProvider: SearchProviderId;
	timetableDayRange: 5 | 6 | 7;
	timetablePeriodRange: 4 | 5 | 6 | 7;
	timetableOndemandCols: 0 | 1 | 2 | 3;
	timetableTransparent: boolean;
	timetableBgImage?: string;
	timetablePeriods: Record<Period, PeriodTime>;
	calendarWeekStart: 'sun' | 'mon';
	calendarShowAcademic: boolean;
	calendarShowHolidays: boolean;
	calendarShowExternal: boolean;
	calendarShowTimetable: boolean;
	calendarShowTodos: boolean;
	calendarDensity: 3 | 4;
	calendarAutoCompact: boolean;
	attendanceMaxAbsences: number;
	attendanceNotification: AttendanceNotificationSettings;
	externalLinks: ExternalLink[];
	dashboardCardOrder: string[];
	hiddenDashboardCards: string[];
	/** 公開データ CDN ベース URL。空ならビルド時環境変数 / 既定値を使用 */
	dataCdnUrl: string;
	emojiStyle: 'native' | 'twemoji';
	fontFamily: 'system' | 'outfit' | 'sawarabi' | 'noto';
	/** 背景画像をすべての画面（アプリ全体）に適用するか */
	bgImageAllPages: boolean;
};

export const DEFAULT_USER_SETTINGS: UserSettings = {
	themePackId: 'default',
	themePackIdLight: 'default',
	themePackIdDark: 'dark',
	theme: 'system',
	locale: 'ja',
	semester: 'spring',
	year: new Date().getFullYear(),
	lat: 35.6586,
	lng: 139.7454,
	locationName: '調布',
	homeShowWeather: true,
	homeShowTools: true,
	homeShowUecReview: true,
	homeShowExternalLinks: true,
	openLinksInApp: true,
	homeSearchProvider: 'google',
	timetableDayRange: 5,
	timetablePeriodRange: 7,
	timetableOndemandCols: 1,
	timetableTransparent: false,
	timetablePeriods: {
		1: { start: '09:00', end: '10:30' },
		2: { start: '10:40', end: '12:10' },
		3: { start: '13:00', end: '14:30' },
		4: { start: '14:40', end: '16:10' },
		5: { start: '16:15', end: '17:45' },
		6: { start: '17:50', end: '19:20' },
		7: { start: '19:30', end: '21:00' }
	},
	calendarWeekStart: 'mon',
	calendarShowAcademic: true,
	calendarShowHolidays: true,
	calendarShowExternal: true,
	calendarShowTimetable: true,
	calendarShowTodos: true,
	calendarDensity: 4,
	calendarAutoCompact: true,
	attendanceMaxAbsences: 5,
	attendanceNotification: {
		enabled: false,
		trigger: 'both',
		startOffsetMinutes: 5,
		endOffsetMinutes: 0
	},
	externalLinks: [],
	dashboardCardOrder: [],
	hiddenDashboardCards: [],
	dataCdnUrl: '',
	emojiStyle: 'native',
	fontFamily: 'system',
	bgImageAllPages: false
};
