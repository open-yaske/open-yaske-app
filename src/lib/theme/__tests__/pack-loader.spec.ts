import { describe, it, expect } from 'vitest';
import { validatePack, parsePack, BUILTIN_PACKS, getBuiltinPack } from '../pack-loader';

describe('pack-loader', () => {
	it('validates built-in packs', () => {
		for (const pack of BUILTIN_PACKS) {
			const result = validatePack(pack);
			expect(result.ok).toBe(true);
		}
	});

	it('rejects non-object', () => {
		const result = validatePack('hello');
		expect(result.ok).toBe(false);
	});

	it('rejects missing id', () => {
		const result = validatePack({ name: 'x', version: 1, ui: {}, palette: {}, attendance: {} });
		expect(result.ok).toBe(false);
	});

	it('rejects wrong version', () => {
		const result = validatePack({
			id: 'x',
			name: 'x',
			version: 2,
			ui: {},
			palette: {},
			attendance: {}
		});
		expect(result.ok).toBe(false);
	});

	it('rejects invalid hex', () => {
		// simulate by replacing one hex
		const bad = JSON.parse(JSON.stringify(BUILTIN_PACKS[0]));
		bad.ui.primary[500] = 'not-a-hex';
		const r2 = validatePack(bad);
		expect(r2.ok).toBe(false);
	});

	it('parses valid JSON', () => {
		const json = JSON.stringify(BUILTIN_PACKS[0]);
		const result = parsePack(json);
		expect(result.ok).toBe(true);
	});

	it('returns errors for invalid JSON', () => {
		const result = parsePack('not-json');
		expect(result.ok).toBe(false);
	});

	it('getBuiltinPack returns matching pack', () => {
		expect(getBuiltinPack('default')?.id).toBe('default');
		expect(getBuiltinPack('uec')?.id).toBe('uec');
		expect(getBuiltinPack('dango')?.id).toBe('dango');
		expect(getBuiltinPack('dark')?.id).toBe('dark');
	});

	it('getBuiltinPack returns undefined for unknown id', () => {
		expect(getBuiltinPack('unknown')).toBeUndefined();
	});
});
