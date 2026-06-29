import {
	Home,
	BookOpen,
	CheckSquare,
	Calendar,
	BarChart3,
	FileText,
	Calculator,
	Globe,
	SlidersHorizontal,
	Palette,
	Settings
} from '@lucide/svelte';
import * as m from '$lib/paraglide/messages';
import type { Component } from 'svelte';

export type AppFeature = {
	id: string;
	nameKey: string;
	icon: Component<{ size?: number; class?: string }>;
	href: string;
};

export const APP_FEATURES: AppFeature[] = [
	{ id: 'dashboard', nameKey: 'dashboard_title', icon: Home, href: '/' },
	{ id: 'timetable', nameKey: 'nav_timetable', icon: BookOpen, href: '/timetable' },
	{ id: 'todo', nameKey: 'nav_todo', icon: CheckSquare, href: '/todo' },
	{ id: 'calendar', nameKey: 'nav_calendar', icon: Calendar, href: '/calendar' },
	{ id: 'attendance', nameKey: 'nav_attendance', icon: BarChart3, href: '/attendance' },
	{ id: 'pdf', nameKey: 'more_pdf', icon: FileText, href: '/pdf' },
	{ id: 'gpa', nameKey: 'more_gpa', icon: Calculator, href: '/gpa' },
	{ id: 'syllabus', nameKey: 'more_syllabus', icon: Globe, href: '/syllabus' },
	{
		id: 'data-management',
		nameKey: 'more_data_management',
		icon: SlidersHorizontal,
		href: '/data-management'
	},
	{ id: 'theme-editor', nameKey: 'theme_editor_title', icon: Palette, href: '/theme-editor' },
	{ id: 'settings', nameKey: 'settings_title', icon: Settings, href: '/settings' }
];

/**
 * 機能IDから多言語対応の名称を取得する
 */
export function getFeatureName(id: string): string {
	const feature = APP_FEATURES.find((f) => f.id === id);
	if (!feature) return id;

	// Dynamic lookup on Paraglide compiled messages
	const key = feature.nameKey;
	if (key && typeof (m as any)[key] === 'function') {
		return (m as any)[key]();
	}
	return id;
}
