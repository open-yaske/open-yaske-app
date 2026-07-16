/**
 * 日本の祝日 (独自計算による超軽量実装)
 */

import type { DayOfWeek } from './types';

export type Holiday = {
	date: string; // YYYY-MM-DD
	name: string;
	type: string;
};

// 特定の月の第N月曜日を求める関数 (1-indexed)
function getHappyMonday(year: number, month: number, nth: number): number {
	const firstDay = new Date(year, month - 1, 1).getDay();
	const offset = (8 - firstDay) % 7;
	return 1 + offset + (nth - 1) * 7;
}

// 春分の日を求める (簡易推算式: 1980〜2099年対応)
function getSpringEquinox(year: number): number {
	if (year < 1980 || year > 2099) return 20;
	return Math.floor(20.8431 + 0.242194 * (year - 1980) - Math.floor((year - 1980) / 4));
}

// 秋分の日を求める (簡易推算式: 1980〜2099年対応)
function getAutumnEquinox(year: number): number {
	if (year < 1980 || year > 2099) return 23;
	return Math.floor(23.2488 + 0.242194 * (year - 1980) - Math.floor((year - 1980) / 4));
}

export function getJapaneseHolidays(year: number): Holiday[] {
	const holidaysMap: Record<string, string> = {};

	const addHoliday = (month: number, day: number, name: string) => {
		const dateStr = `${year}-${month.toString().padStart(2, '0')}-${day.toString().padStart(2, '0')}`;
		holidaysMap[dateStr] = name;
	};

	// 1. 固定祝日の登録
	addHoliday(1, 1, '元日');
	addHoliday(2, 11, '建国記念の日');

	if (year >= 2020) {
		addHoliday(2, 23, '天皇誕生日');
	} else if (year >= 1989 && year <= 2018) {
		addHoliday(12, 23, '天皇誕生日');
	}

	addHoliday(3, getSpringEquinox(year), '春分の日');
	addHoliday(4, 29, '昭和の日');
	addHoliday(5, 3, '憲法記念日');
	addHoliday(5, 4, 'みどりの日');
	addHoliday(5, 5, 'こどもの日');

	if (year >= 2016) {
		if (year === 2020) {
			addHoliday(8, 10, '山の日');
		} else if (year === 2021) {
			addHoliday(8, 8, '山の日');
		} else {
			addHoliday(8, 11, '山の日');
		}
	}

	addHoliday(11, 3, '文化の日');
	addHoliday(11, 23, '勤労感謝の日');

	// 2. 移動祝日 (ハッピーマンデー等)
	addHoliday(1, getHappyMonday(year, 1, 2), '成人の日');

	if (year === 2020) {
		addHoliday(7, 23, '海の日');
	} else if (year === 2021) {
		addHoliday(7, 22, '海の日');
	} else if (year >= 2003) {
		addHoliday(7, getHappyMonday(year, 7, 3), '海の日');
	} else if (year >= 1996) {
		addHoliday(7, 20, '海の日');
	}

	if (year >= 2003) {
		addHoliday(9, getHappyMonday(year, 9, 3), '敬老の日');
	} else if (year >= 1966) {
		addHoliday(9, 15, '敬老の日');
	}

	addHoliday(9, getAutumnEquinox(year), '秋分の日');

	if (year === 2020) {
		addHoliday(7, 24, 'スポーツの日');
	} else if (year === 2021) {
		addHoliday(7, 23, 'スポーツの日');
	} else if (year >= 2020) {
		addHoliday(10, getHappyMonday(year, 10, 2), 'スポーツの日');
	} else if (year >= 2000) {
		addHoliday(10, getHappyMonday(year, 10, 2), '体育の日');
	} else if (year >= 1966) {
		addHoliday(10, 10, '体育の日');
	}

	// 3. 振替休日
	const originalHolidays = Object.keys(holidaysMap).sort();
	const substituteHolidays: Record<string, string> = {};

	for (const dateStr of originalHolidays) {
		const date = new Date(dateStr);
		if (date.getDay() === 0) {
			let subDate = new Date(date);
			while (true) {
				subDate.setDate(subDate.getDate() + 1);
				const subDateStr = `${subDate.getFullYear()}-${(subDate.getMonth() + 1).toString().padStart(2, '0')}-${subDate.getDate().toString().padStart(2, '0')}`;
				if (!holidaysMap[subDateStr] && !substituteHolidays[subDateStr]) {
					substituteHolidays[subDateStr] = `${holidaysMap[dateStr]} 振替休日`;
					break;
				}
			}
		}
	}
	Object.assign(holidaysMap, substituteHolidays);

	// 4. 国民の休日
	const allHolidays = Object.keys(holidaysMap).sort();
	const nationalHolidays: Record<string, string> = {};

	for (let i = 0; i < allHolidays.length - 1; i++) {
		const d1 = new Date(allHolidays[i]!);
		const d2 = new Date(allHolidays[i + 1]!);
		const diffDays = Math.round((d2.getTime() - d1.getTime()) / (1000 * 60 * 60 * 24));
		if (diffDays === 2) {
			const midDate = new Date(d1);
			midDate.setDate(midDate.getDate() + 1);
			if (midDate.getDay() !== 0) {
				const midDateStr = `${midDate.getFullYear()}-${(midDate.getMonth() + 1).toString().padStart(2, '0')}-${midDate.getDate().toString().padStart(2, '0')}`;
				if (!holidaysMap[midDateStr]) {
					nationalHolidays[midDateStr] = '国民の休日';
				}
			}
		}
	}
	Object.assign(holidaysMap, nationalHolidays);

	return Object.keys(holidaysMap)
		.sort()
		.map((dateStr) => ({
			date: dateStr,
			name: holidaysMap[dateStr]!,
			type: 'public'
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
