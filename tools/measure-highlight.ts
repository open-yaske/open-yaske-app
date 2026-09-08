import { BUILTIN_PACKS } from '../src/lib/theme/pack-loader.ts';
import { calculateContrastRatio, parseColor, blendColors } from '../src/lib/theme/contrast.ts';
function tintBg(card: string, page: string, tintHex: string, alpha: number) {
	const c = parseColor(card);
	const pg = parseColor(page);
	const ce = c.a < 1 ? blendColors(c, pg) : c;
	const t = parseColor(tintHex);
	const bg = blendColors({ r: t.r, g: t.g, b: t.b, a: alpha }, ce);
	return `rgb(${bg.r}, ${bg.g}, ${bg.b})`;
}
for (const p of BUILTIN_PACKS) {
	const bg15 = tintBg(p.ui.surface.card, p.ui.surface.page, p.ui.primary[500], 0.15);
	const bg12 = tintBg(p.ui.surface.card, p.ui.surface.page, p.ui.primary[500], 0.12);
	const bg20 = tintBg(p.ui.surface.card, p.ui.surface.page, p.ui.primary[500], 0.2);
	const r600bg15 = calculateContrastRatio(p.ui.primary[600], bg15, p.ui.surface.page);
	const rNavBg15 = calculateContrastRatio(p.ui.nav.active, bg15, p.ui.surface.page);
	const r700bg12 = calculateContrastRatio(p.ui.primary[700], bg12, p.ui.surface.page);
	const r300bg20 = calculateContrastRatio(p.ui.primary[300], bg20, p.ui.surface.page);
	const rWhite500 = calculateContrastRatio('#ffffff', p.ui.primary[500], p.ui.surface.page);
	const rPageNav = calculateContrastRatio(p.ui.surface.page, p.ui.nav.active, p.ui.surface.page);
	const rWhite800 = calculateContrastRatio('#ffffff', p.ui.primary[800], p.ui.surface.page);
	console.log(
		`${p.id} | 600/bg15=${r600bg15} | nav/bg15=${rNavBg15} | 700/bg12=${r700bg12} | 300/bg20=${r300bg20} | white/500=${rWhite500} | page/navAct=${rPageNav} | white/800=${rWhite800}`
	);
}
