/**
 * 日本の祝日 (date-holidays ラッパー)
 */

import Holidays from 'date-holidays';
import type { DayOfWeek } from './types';

const hd = new Holidays('JP');

export type Holiday = {
	date: string; // YYYY-MM-DD
	name: string;
	type: string;
};

export function getJapaneseHolidays(year: number): Holiday[] {
	const list = hd.getHolidays(year) as { date: string; name: string; type: string }[];
	return list.map((h) => ({
		date: h.date.slice(0, 10),
		name: h.name,
		type: h.type
	}));
}

export function isHoliday(date: Date): boolean {
	const ymd = date.toISOString().slice(0, 10);
	return getJapaneseHolidays(date.getFullYear()).some((h) => h.date === ymd);
}

export function getDayOfWeek(date: Date): DayOfWeek {
	const map: DayOfWeek[] = ['sun', 'mon', 'tue', 'wed', 'thu', 'fri', 'sat'];
	return map[date.getDay()]!;
}
