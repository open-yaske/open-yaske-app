import { describe, it, expect } from 'vitest';
import { BUILTIN_PACKS } from '../pack-loader';
import {
	calculateContrastRatio,
	diagnoseThemePackContrast,
	validateThemePackContrast
} from '../contrast';
import { CONTRAST_CHECKS, getAllContrastChecks } from '../contrast-matrix';

describe('WCAG 2.1 Theme Contrast Verification', () => {
	it('calculates correct contrast ratio for black and white', () => {
		const ratio = calculateContrastRatio('#000000', '#ffffff');
		expect(ratio).toBe(21);
	});

	it('calculates contrast ratio for identical colors as 1.0', () => {
		const ratio = calculateContrastRatio('#0a84ff', '#0a84ff');
		expect(ratio).toBe(1);
	});

	it('verifies contrast for all built-in theme packs (legacy)', () => {
		for (const pack of BUILTIN_PACKS) {
			const diagnostics = diagnoseThemePackContrast(pack);
			const packName = typeof pack.name === 'string' ? pack.name : pack.name.ja;

			for (const item of diagnostics) {
				expect(
					item.ratio,
					`Theme "${packName}" - ${item.label} (${item.fgColor} on ${item.bgColor}) ratio: ${item.ratio}`
				).toBeGreaterThanOrEqual(3.0);
			}
		}
	});

	it('ensures active text on page background satisfies WCAG AA (>= 4.5:1) for built-in packs', () => {
		for (const pack of BUILTIN_PACKS) {
			const packName = typeof pack.name === 'string' ? pack.name : pack.name.ja;
			const ratio = calculateContrastRatio(pack.ui.nav.active, pack.ui.surface.page);
			expect(
				ratio,
				`Theme "${packName}" page active text contrast ratio: ${ratio}`
			).toBeGreaterThanOrEqual(4.5);
		}
	});
});

describe('Comprehensive Contrast Matrix Verification', () => {
	it('has a well-defined contrast check matrix', () => {
		// 静的チェックが最低30個定義されていること
		expect(CONTRAST_CHECKS.length).toBeGreaterThanOrEqual(30);

		// 全チェックに一意なIDがあること
		const ids = CONTRAST_CHECKS.map((c) => c.id);
		expect(new Set(ids).size).toBe(ids.length);

		// 全チェックに必要なフィールドがあること
		for (const check of CONTRAST_CHECKS) {
			expect(check.id).toBeTruthy();
			expect(check.label).toBeTruthy();
			expect(check.category).toBeTruthy();
			expect(check.fg).toBeTypeOf('function');
			expect(check.bg).toBeTypeOf('function');
			expect(['AA', 'AALarge', 'Decorative']).toContain(check.threshold);
		}
	});

	it('generates palette checks dynamically for each theme', () => {
		for (const pack of BUILTIN_PACKS) {
			const allChecks = getAllContrastChecks(pack);
			// 静的チェック + パレットチェック（course + todo + event）
			const expectedPaletteCount =
				pack.palette.courseColors.length +
				pack.palette.todoColors.length +
				pack.palette.eventColors.length;
			expect(allChecks.length).toBe(CONTRAST_CHECKS.length + expectedPaletteCount);
		}
	});

	it('validates all built-in themes against the full contrast matrix', () => {
		for (const pack of BUILTIN_PACKS) {
			const packName = typeof pack.name === 'string' ? pack.name : pack.name.ja;
			const report = validateThemePackContrast(pack);

			// 不合格がある場合は詳細なエラーメッセージを出力
			if (!report.isValid) {
				const failureDetails = report.failures
					.map(
						(f) =>
							`  ✗ ${f.checkId}: ${f.label}\n` +
							`    ${f.fgColor} on ${f.bgColor} = ${f.ratio}:1 (need ${f.thresholdValue}:1)`
					)
					.join('\n');

				expect(
					report.isValid,
					`Theme "${packName}" failed ${report.failed}/${report.total} contrast checks:\n${failureDetails}`
				).toBe(true);
			}

			expect(report.isValid).toBe(true);
			expect(report.passRate).toBe(1);
		}
	});

	it('ensures text category checks meet WCAG AA (4.5:1)', () => {
		for (const pack of BUILTIN_PACKS) {
			const packName = typeof pack.name === 'string' ? pack.name : pack.name.ja;
			const report = validateThemePackContrast(pack);
			const textChecks = report.results.filter((r) => r.category === 'text');

			for (const check of textChecks) {
				expect(
					check.ratio,
					`Theme "${packName}" - ${check.checkId}: ${check.fgColor} on ${check.bgColor} = ${check.ratio}:1 (need 4.5:1)`
				).toBeGreaterThanOrEqual(4.5);
			}
		}
	});

	it('ensures all checks meet their defined threshold', () => {
		for (const pack of BUILTIN_PACKS) {
			const packName = typeof pack.name === 'string' ? pack.name : pack.name.ja;
			const report = validateThemePackContrast(pack);

			for (const check of report.results) {
				expect(
					check.passed,
					`Theme "${packName}" - ${check.checkId} [${check.category}]: ${check.fgColor} on ${check.bgColor} = ${check.ratio}:1 (need ${check.thresholdValue}:1)`
				).toBe(true);
			}
		}
	});
});
