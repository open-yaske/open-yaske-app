import { describe, it, expect, vi } from 'vitest';
import {
	DEFAULT_COMPONENTS,
	resolveComponents,
	isStyleShadowsNone,
	styleShadowClass,
	applyStyleShadows
} from '../components';
import { getAllStyles } from '../registry';

function fakeStyleTarget() {
	const set: [string, string][] = [];
	return {
		set,
		target: {
			style: { setProperty: vi.fn((k: string, v: string) => void set.push([k, v])) }
		} as unknown as { style: { setProperty: (k: string, v: string) => void } }
	};
}

describe('style shadows ownership (敵対的)', () => {
	it('全スタイルが shadow 4段を持つ', () => {
		for (const pack of getAllStyles()) {
			expect(pack.shadow.sm, `${pack.id}.sm`).toBeTruthy();
			expect(pack.shadow.md, `${pack.id}.md`).toBeTruthy();
			expect(pack.shadow.lg, `${pack.id}.lg`).toBeTruthy();
			expect(pack.shadow.xl, `${pack.id}.xl`).toBeTruthy();
		}
	});
	it('影なしスタイルは flat/high-contrast/win95 のみ', () => {
		const noneIds = getAllStyles()
			.filter((p) => isStyleShadowsNone(p))
			.map((p) => p.id)
			.sort();
		expect(noneIds).toEqual(['flat', 'high-contrast', 'win95']);
	});
	it('styleShadowClass は --shadow-style-* に一本化される', () => {
		for (const pack of getAllStyles()) {
			expect(styleShadowClass(pack, 'sm')).toBe('shadow-[var(--shadow-style-sm)]');
			expect(styleShadowClass(pack, 'md')).toBe('shadow-[var(--shadow-style-md)]');
			expect(styleShadowClass(pack, 'lg')).toBe('shadow-[var(--shadow-style-lg)]');
			expect(styleShadowClass(pack, 'xl')).toBe('shadow-[var(--shadow-style-xl)]');
			expect(styleShadowClass(pack, 'none')).toBe('shadow-none');
		}
	});
	it('applyStyleShadows は --shadow-style-* のみ書き、テーマ変数に触らない', () => {
		for (const pack of getAllStyles()) {
			const { target, set } = fakeStyleTarget();
			applyStyleShadows(pack, target);
			const keys = set.map(([k]) => k).sort();
			expect(keys).toEqual([
				'--shadow-style-lg',
				'--shadow-style-md',
				'--shadow-style-sm',
				'--shadow-style-xl'
			]);
		}
	});
});

describe('resolveComponents (敵対的)', () => {
	it('全スタイルで fab/chip/bar/sheet/sidenav が解決できる', () => {
		for (const pack of getAllStyles()) {
			const c = resolveComponents(pack);
			expect(c.fab.base, `${pack.id}.fab`).toBeTruthy();
			expect(c.chip.base, `${pack.id}.chip`).toBeTruthy();
			expect(c.chip.active, `${pack.id}.chip.active`).toBeTruthy();
			expect(c.bar.container, `${pack.id}.bar`).toBeTruthy();
			expect(c.sheet.backdrop, `${pack.id}.sheet`).toBeTruthy();
			expect(c.sheet.panel, `${pack.id}.sheet.panel`).toBeTruthy();
			expect(c.sidenav.activeItem, `${pack.id}.sidenav.active`).toBeTruthy();
			expect(c.sidenav.inactiveItem, `${pack.id}.sidenav.inactive`).toBeTruthy();
		}
	});
	it('sidenav 差分を持つスタイルはデフォルトから逸脱する', () => {
		const withSidenav = getAllStyles().filter((p) => p.components?.sidenav);
		expect(withSidenav.map((p) => p.id).sort()).toEqual([
			'apple',
			'brutalism',
			'cyberpunk',
			'flat',
			'high-contrast',
			'material',
			'neumorphism',
			'win95',
			'winxp'
		]);
		for (const pack of withSidenav) {
			const c = resolveComponents(pack);
			expect(c.sidenav.activeItem).not.toBe(DEFAULT_COMPONENTS.sidenav.activeItem);
		}
	});
	it('未定義キーは DEFAULT_COMPONENTS にフォールバックする', () => {
		for (const pack of getAllStyles()) {
			const c = resolveComponents(pack);
			if (!pack.components?.fab) expect(c.fab.base).toBe(DEFAULT_COMPONENTS.fab.base);
			if (!pack.components?.chip) expect(c.chip.base).toBe(DEFAULT_COMPONENTS.chip.base);
			if (!pack.components?.bar) expect(c.bar.container).toBe(DEFAULT_COMPONENTS.bar.container);
			if (!pack.components?.sheet) expect(c.sheet.backdrop).toBe(DEFAULT_COMPONENTS.sheet.backdrop);
		}
	});
	it('解決済みコンポーネントは旧テーマ影 var(--shadow-card) を含まない', () => {
		for (const pack of getAllStyles()) {
			const c = resolveComponents(pack);
			const all = [
				c.button.primary,
				c.button.secondary,
				c.button.danger,
				c.button.glass,
				c.input.base,
				c.card.base,
				c.card.interactive,
				c.fab.base,
				c.chip.active,
				c.bar.container
			].join('\n');
			expect(all, `${pack.id} に旧影参照`).not.toContain('var(--shadow-card)');
			expect(all, `${pack.id} に旧影参照`).not.toContain('var(--shadow-modal)');
		}
	});
});
