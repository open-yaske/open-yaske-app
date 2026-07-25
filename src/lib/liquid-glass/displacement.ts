/**
 * liquid-glass/displacement.ts
 *
 * nikdelvin/liquid-glass の liquidGlass.ts を移植・拡張。
 * SVG feDisplacementMap ベースの屈折エフェクト用ユーティリティ。
 *
 * @see https://github.com/nikdelvin/liquid-glass
 */

export type DisplacementOptions = {
	/** 要素の高さ (px) */
	height: number;
	/** 要素の幅 (px) */
	width: number;
	/** border-radius (px) */
	radius: number;
	/** 屈折深度 (feDisplacementMap の scale 比率基準値) */
	depth: number;
};

export type LiquidGlassFilterOptions = {
	/** フィルターの一意な ID */
	id: string;
	/** displacement map のデータURL */
	displacementMapUrl: string;
	/** 屈折の強度 (0–200, default: 100) */
	strength: number;
	/** クロマティックアベレーション強度 (0–5, default: 0) */
	chromaticAberration: number;
	/** ブラー量 (px, default: 0) */
	blur: number;
	/** 彩度倍率 (default: 1.5) */
	saturate: number;
	/** 輝度倍率 (default: 1.1) */
	brightness: number;
};

/**
 * 要素の形状に合わせた SVG ディスプレイスメントマップをデータ URL で返す。
 * X/Y グラデーションを mix-blend-mode: screen で合成し、角丸領域の屈折を表現する。
 */
export function getDisplacementMap({ height, width, radius, depth }: DisplacementOptions): string {
	const svgStr = `<svg height="${height}" width="${width}" viewBox="0 0 ${width} ${height}" xmlns="http://www.w3.org/2000/svg">
    <style>.mix { mix-blend-mode: screen; }</style>
    <defs>
      <linearGradient id="Y" x1="0" x2="0"
        y1="${Math.ceil((radius / height) * 15)}%"
        y2="${Math.floor(100 - (radius / height) * 15)}%">
          <stop offset="0%" stop-color="#0F0" />
          <stop offset="100%" stop-color="#000" />
      </linearGradient>
      <linearGradient id="X" x1="${Math.ceil((radius / width) * 15)}%" x2="${Math.floor(100 - (radius / width) * 15)}%"
        y1="0" y2="0">
          <stop offset="0%" stop-color="#F00" />
          <stop offset="100%" stop-color="#000" />
      </linearGradient>
      <radialGradient id="TL" cx="0%" cy="0%" r="${Math.ceil(Math.max((radius / width) * 100, (radius / height) * 100))}%">
          <stop offset="0%" stop-color="#000" />
          <stop offset="100%" stop-color="#F00" />
      </radialGradient>
      <radialGradient id="TR" cx="100%" cy="0%" r="${Math.ceil(Math.max((radius / width) * 100, (radius / height) * 100))}%">
          <stop offset="0%" stop-color="#000" />
          <stop offset="100%" stop-color="#0F0" />
      </radialGradient>
      <radialGradient id="BL" cx="0%" cy="100%" r="${Math.ceil(Math.max((radius / width) * 100, (radius / height) * 100))}%">
          <stop offset="0%" stop-color="#000" />
          <stop offset="100%" stop-color="#0F0" />
      </radialGradient>
      <radialGradient id="BR" cx="100%" cy="100%" r="${Math.ceil(Math.max((radius / width) * 100, (radius / height) * 100))}%">
          <stop offset="0%" stop-color="#000" />
          <stop offset="100%" stop-color="#F00" />
      </radialGradient>
    </defs>
    <rect width="${width}" height="${height}" fill="url(#Y)" />
    <rect width="${width}" height="${height}" fill="url(#X)" class="mix" />
    <rect width="${Math.ceil(radius * 1.5)}" height="${Math.ceil(radius * 1.5)}" fill="url(#TL)" />
    <rect width="${Math.ceil(radius * 1.5)}" height="${Math.ceil(radius * 1.5)}" x="${width - Math.ceil(radius * 1.5)}" fill="url(#TR)" />
    <rect width="${Math.ceil(radius * 1.5)}" height="${Math.ceil(radius * 1.5)}" y="${height - Math.ceil(radius * 1.5)}" fill="url(#BL)" />
    <rect width="${Math.ceil(radius * 1.5)}" height="${Math.ceil(radius * 1.5)}" x="${width - Math.ceil(radius * 1.5)}" y="${height - Math.ceil(radius * 1.5)}" fill="url(#BR)" />
  </svg>`;

	return 'data:image/svg+xml;utf8,' + encodeURIComponent(svgStr);
}

/**
 * SVG <filter> 定義の文字列を返す。
 * <defs> 内に埋め込むことで backdrop-filter: url(#id) から参照できる。
 */
export function getLiquidGlassFilterSvg({
	id,
	displacementMapUrl,
	strength,
	chromaticAberration,
	blur,
	saturate,
	brightness
}: LiquidGlassFilterOptions): string {
	const scale = (strength / 100) * 35;
	const cabScale = chromaticAberration * 2.5;
	const blurStr = blur > 0 ? `<feGaussianBlur stdDeviation="${blur}" result="blurred" />` : '';
	const blurInput = blur > 0 ? 'blurred' : 'displaced';

	if (chromaticAberration > 0) {
		// クロマティックアベレーション: R/G/B チャンネルを個別にシフトして合成
		return `<filter id="${id}" x="-5%" y="-5%" width="110%" height="110%" color-interpolation-filters="sRGB">
      <feImage href="${displacementMapUrl}" result="dmap" x="0" y="0" width="100%" height="100%" preserveAspectRatio="none" />
      <feDisplacementMap in="BackgroundImage" in2="dmap" scale="${scale + cabScale}" xChannelSelector="R" yChannelSelector="G" result="disp_r" />
      <feDisplacementMap in="BackgroundImage" in2="dmap" scale="${scale}" xChannelSelector="R" yChannelSelector="G" result="disp_g" />
      <feDisplacementMap in="BackgroundImage" in2="dmap" scale="${scale - cabScale}" xChannelSelector="R" yChannelSelector="G" result="disp_b" />
      <feColorMatrix in="disp_r" type="matrix" values="1 0 0 0 0  0 0 0 0 0  0 0 0 0 0  0 0 0 1 0" result="only_r" />
      <feColorMatrix in="disp_g" type="matrix" values="0 0 0 0 0  0 1 0 0 0  0 0 0 0 0  0 0 0 1 0" result="only_g" />
      <feColorMatrix in="disp_b" type="matrix" values="0 0 0 0 0  0 0 0 0 0  0 0 1 0 0  0 0 0 1 0" result="only_b" />
      <feBlend in="only_r" in2="only_g" mode="screen" result="rg" />
      <feBlend in="rg" in2="only_b" mode="screen" result="displaced" />
      ${blurStr}
      <feColorMatrix in="${blurInput}" type="saturate" values="${saturate}" result="saturated" />
      <feComponentTransfer in="saturated" result="final">
        <feFuncR type="linear" slope="${brightness}" />
        <feFuncG type="linear" slope="${brightness}" />
        <feFuncB type="linear" slope="${brightness}" />
      </feComponentTransfer>
    </filter>`;
	}

	return `<filter id="${id}" x="-5%" y="-5%" width="110%" height="110%" color-interpolation-filters="sRGB">
    <feImage href="${displacementMapUrl}" result="dmap" x="0" y="0" width="100%" height="100%" preserveAspectRatio="none" />
    <feDisplacementMap in="BackgroundImage" in2="dmap" scale="${scale}" xChannelSelector="R" yChannelSelector="G" result="displaced" />
    ${blurStr}
    <feColorMatrix in="${blurInput}" type="saturate" values="${saturate}" result="saturated" />
    <feComponentTransfer in="saturated" result="final">
      <feFuncR type="linear" slope="${brightness}" />
      <feFuncG type="linear" slope="${brightness}" />
      <feFuncB type="linear" slope="${brightness}" />
    </feComponentTransfer>
  </filter>`;
}

/**
 * <svg><defs>...</defs></svg> 全体の文字列を返す。
 * document.body に一度だけ注入して使い回す。
 */
export function buildSvgDefsString(filters: LiquidGlassFilterOptions[]): string {
	const filterDefs = filters.map((f) => getLiquidGlassFilterSvg(f)).join('\n');
	return `<svg xmlns="http://www.w3.org/2000/svg" width="0" height="0" style="position:absolute;overflow:hidden;pointer-events:none">
  <defs>
    ${filterDefs}
  </defs>
</svg>`;
}

/**
 * backdrop-filter で使う CSS フィルター文字列を返す。
 * ブラウザが backdrop-filter: url() をサポートしていない場合は '' を返す。
 */
export function getBackdropFilterStyle(filterId: string): string {
	if (typeof CSS === 'undefined') return '';
	// Safari と Firefox は backdrop-filter: url() を部分的にしかサポートしない
	// Chromium では完全サポート
	try {
		if (!CSS.supports('backdrop-filter', `url(#${filterId})`)) return '';
	} catch {
		return '';
	}
	return `backdrop-filter: url(#${filterId}); -webkit-backdrop-filter: url(#${filterId});`;
}

/**
 * 要素の computed border-radius を px で返すヘルパー。
 */
export function getComputedRadius(el: Element): number {
	const style = window.getComputedStyle(el);
	const raw = style.borderTopLeftRadius;
	const px = parseFloat(raw);
	return isNaN(px) ? 0 : px;
}
