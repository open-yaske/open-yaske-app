import { describe, it, expect } from 'vitest';
import { BUILTIN_PACKS } from '../pack-loader';
import { calculateContrastRatio, diagnoseThemePackContrast } from '../contrast';

describe('WCAG 2.1 Theme Contrast Verification', () => {
	it('calculates correct contrast ratio for black and white', () => {
		const ratio = calculateContrastRatio('#000000', '#ffffff');
		expect(ratio).toBe(21);
	});

	it('calculates contrast ratio for identical colors as 1.0', () => {
		const ratio = calculateContrastRatio('#0a84ff', '#0a84ff');
		expect(ratio).toBe(1);
	});

	it('verifies contrast for all built-in theme packs', () => {
		for (const pack of BUILTIN_PACKS) {
			const diagnostics = diagnoseThemePackContrast(pack);
			const packName = typeof pack.name === 'string' ? pack.name : pack.name.ja;

			for (const item of diagnostics) {
				// 必須の文字/背景コントラスト比 (大型文字3.0:1 / 通常文字4.5:1)
				// mutedForeground や補助色を含め最低限 3.0:1 以上を全テーマで完全保証
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
