/**
 * 出席率計算
 * 重要: 休講 (cancelled) は分母から除外する
 */

import type { AttendanceRecord, AttendanceStatus } from './types';

export type AttendanceSummary = {
	rate: number;
	present: number;
	absent: number;
	late: number;
	excused: number;
	cancelled: number;
	/** 休講を除いた分母 (present + absent + late + excused) */
	total: number;
	/** 残り欠席可能回数 */
	remainingAbsences: number;
};

const STATUS_INCLUDED: Record<AttendanceStatus, boolean> = {
	present: true,
	absent: true,
	late: true,
	excused: true,
	cancelled: false // 休講は分母から除外
};

/**
 * 出席記録からサマリを計算
 * @param records 出席記録
 * @param maxAbsences 残り欠席可能回数の基準値 (デフォルト 5)
 */
export function calculateRate(records: AttendanceRecord[], maxAbsences = 5): AttendanceSummary {
	let present = 0;
	let absent = 0;
	let late = 0;
	let excused = 0;
	let cancelled = 0;

	for (const r of records) {
		switch (r.status) {
			case 'present':
				present++;
				break;
			case 'absent':
				absent++;
				break;
			case 'late':
				late++;
				break;
			case 'excused':
				excused++;
				break;
			case 'cancelled':
				cancelled++;
				break;
		}
	}

	// 休講は分母から除外
	const total = present + absent + late + excused;
	const effective = present + late * 0.5 + excused;
	const rate = total === 0 ? 0 : (effective / total) * 100;
	const remainingAbsences = Math.max(0, maxAbsences - absent);

	return { rate, present, absent, late, excused, cancelled, total, remainingAbsences };
}

/**
 * 特定の授業に対して、今日 / 特定日の出席状態を取得
 */
export function getTodayStatus(
	records: AttendanceRecord[],
	courseId: string,
	date: string
): AttendanceStatus | null {
	return records.find((r) => r.courseId === courseId && r.date === date)?.status ?? null;
}

/**
 * 休講を除いた「実授業回数」を取得 (学期内のコマ数の近似)
 */
export function effectiveClassCount(records: AttendanceRecord[]): number {
	return records.filter((r) => STATUS_INCLUDED[r.status]).length;
}

/**
 * 危険な授業 (出席率がしきい値以下) を判定
 */
export function isDangerous(summary: AttendanceSummary, threshold = 80): boolean {
	return summary.total > 0 && summary.rate < threshold;
}
