/**
 * 公開データ型定義
 * GitHub 上の Markdown → CI/CD で JSON に変換 → CDN で配信される
 */

import type { DayOfWeek, Period } from '../types';

export const PUBLIC_DATA_SCHEMA_VERSION = 1 as const;

// ----- 公開科目 (PublicCourse) -----
export type PublicCourse = {
	id: string;
	code: string;
	name: string;
	teacher?: string;
	classroom?: string;
	credits: number;
	type: 'free' | 'required' | 'elective';
	semester: 'spring' | 'fall' | 'other';
	year: number;
	/** 授業概要 (Markdown/HTML) - DOMPurify でサニタイズしてレンダリング */
	descriptionHtml?: string;
	/** 参考リンク */
	references?: { title: string; url: string }[];
	/** トピック一覧 */
	topics?: string[];
	/** 開講時限 */
	dayPeriods?: { day: DayOfWeek; period: Period }[];
};

// ----- その他画面カード (DashboardCard) -----
export type CardLayout = 'card' | 'banner';

/** アプリ内レンダリング: HTML をフェッチしてサニタイズして表示 */
export type HtmlInternalCard = {
	type: 'html-internal';
	id: string;
	title: string;
	icon: string;
	layout: CardLayout;
	htmlUrl: string;
	order: number;
};

/** 外部 URL: アプリ内ブラウザ or 外部ブラウザで開く */
export type ExternalUrlCard = {
	type: 'external-url';
	id: string;
	title: string;
	icon: string;
	url: string;
	openInApp: boolean;
	order: number;
};

/** ボトムポップアップ: 軽量 HTML を Sheet で表示 */
export type WebviewPopupCard = {
	type: 'webview-popup';
	id: string;
	title: string;
	icon: string;
	htmlUrl: string;
	externalUrl?: string;
	order: number;
};

export type DashboardCard = HtmlInternalCard | ExternalUrlCard | WebviewPopupCard;

// ----- 公開データ全体のスキーマ -----
export type PublicIndex = {
	version: number;
	generatedAt: string;
	count: number;
};

export type PublicData = {
	schemaVersion: typeof PUBLIC_DATA_SCHEMA_VERSION;
	generatedAt: string; // ISO 8601
	index: PublicIndex;
	courses: PublicCourse[];
	dashboardCards: DashboardCard[];
};
