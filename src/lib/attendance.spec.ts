import { describe, it, expect } from 'vitest';
import { calculateRate, effectiveClassCount, isDangerous, getTodayStatus } from './attendance';
import type { AttendanceRecord } from './types';

function rec(courseId: string, date: string, status: AttendanceRecord['status']): AttendanceRecord {
	return { id: `${courseId}-${date}`, courseId, date, status, createdAt: 0 };
}

describe('attendance.calculateRate', () => {
	it('returns zeros for empty records', () => {
		const s = calculateRate([]);
		expect(s.rate).toBe(0);
		expect(s.total).toBe(0);
		expect(s.cancelled).toBe(0);
	});

	it('counts present, absent, late, excused, cancelled', () => {
		const records = [
			rec('c1', '2026-06-01', 'present'),
			rec('c1', '2026-06-08', 'present'),
			rec('c1', '2026-06-15', 'absent'),
			rec('c1', '2026-06-22', 'late'),
			rec('c1', '2026-06-29', 'excused'),
			rec('c1', '2026-07-06', 'cancelled')
		];
		const s = calculateRate(records, 5);
		expect(s.present).toBe(2);
		expect(s.absent).toBe(1);
		expect(s.late).toBe(1);
		expect(s.excused).toBe(1);
		expect(s.cancelled).toBe(1);
		// total excludes cancelled
		expect(s.total).toBe(5);
		// effective: 2*1 + 1*0.5 + 1*1 = 3.5 (absent=0, cancelled excluded)
		// rate: 3.5/5 = 70
		expect(s.rate).toBe(70);
		// remaining absences: 5 - 1 = 4
		expect(s.remainingAbsences).toBe(4);
	});

	it('excludes cancelled from denominator', () => {
		const records = [
			rec('c1', '2026-06-01', 'present'),
			rec('c1', '2026-06-08', 'present'),
			rec('c1', '2026-06-15', 'cancelled'),
			rec('c1', '2026-06-22', 'cancelled'),
			rec('c1', '2026-06-29', 'cancelled')
		];
		const s = calculateRate(records);
		// 3 cancellations should not lower rate below 100
		expect(s.cancelled).toBe(3);
		expect(s.total).toBe(2); // cancelled excluded
		expect(s.rate).toBe(100);
	});

	it('treats excused as attended (weight 1)', () => {
		const records = [rec('c1', '2026-06-01', 'present'), rec('c1', '2026-06-08', 'excused')];
		const s = calculateRate(records);
		expect(s.rate).toBe(100);
	});

	it('counts late as 0.5', () => {
		const records = [rec('c1', '2026-06-01', 'present'), rec('c1', '2026-06-08', 'late')];
		const s = calculateRate(records);
		expect(s.rate).toBe(75); // 1.5 / 2 = 75
	});

	it('returns 0 remaining when absences exceed max', () => {
		const records = [
			rec('c1', '2026-06-01', 'absent'),
			rec('c1', '2026-06-08', 'absent'),
			rec('c1', '2026-06-15', 'absent'),
			rec('c1', '2026-06-22', 'absent'),
			rec('c1', '2026-06-29', 'absent'),
			rec('c1', '2026-07-06', 'absent')
		];
		const s = calculateRate(records, 5);
		expect(s.remainingAbsences).toBe(0);
	});
});

describe('attendance.effectiveClassCount', () => {
	it('excludes cancelled', () => {
		const records = [
			rec('c1', '2026-06-01', 'present'),
			rec('c1', '2026-06-08', 'cancelled'),
			rec('c1', '2026-06-15', 'absent')
		];
		expect(effectiveClassCount(records)).toBe(2);
	});
});

describe('attendance.isDangerous', () => {
	it('marks low rate as dangerous', () => {
		const records = [
			rec('c1', '2026-06-01', 'present'),
			rec('c1', '2026-06-08', 'present'),
			rec('c1', '2026-06-15', 'present'),
			rec('c1', '2026-06-22', 'absent'),
			rec('c1', '2026-06-29', 'absent'),
			rec('c1', '2026-07-06', 'absent'),
			rec('c1', '2026-07-13', 'absent'),
			rec('c1', '2026-07-20', 'absent'),
			rec('c1', '2026-07-27', 'absent')
		];
		const s = calculateRate(records);
		expect(s.rate).toBeLessThan(50);
		expect(isDangerous(s)).toBe(true);
	});

	it('does not mark empty as dangerous', () => {
		const s = calculateRate([]);
		expect(isDangerous(s)).toBe(false);
	});
});

describe('attendance.getTodayStatus', () => {
	it('returns status for matching record', () => {
		const records = [rec('c1', '2026-06-22', 'present')];
		expect(getTodayStatus(records, 'c1', '2026-06-22')).toBe('present');
	});

	it('returns null when not found', () => {
		expect(getTodayStatus([], 'c1', '2026-06-22')).toBe(null);
	});
});
