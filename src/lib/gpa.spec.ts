import { describe, it, expect } from 'vitest';
import { calcGpa } from './gpa';

describe('calcGpa', () => {
	it('computes weighted average', () => {
		const r = calcGpa([
			{ credits: 2, point: 4 },
			{ credits: 2, point: 2 }
		]);
		expect(r.totalCredits).toBe(4);
		expect(r.totalPoints).toBe(12);
		expect(r.gpa).toBe(3);
	});
	it('returns 0 when no credits (敵対的: 0除算)', () => {
		expect(calcGpa([]).gpa).toBe(0);
		expect(calcGpa([{ credits: 0, point: 4 }]).gpa).toBe(0);
	});
	it('clamps negatives to 0', () => {
		expect(calcGpa([{ credits: -2, point: 4 }]).totalCredits).toBe(0);
	});
});
