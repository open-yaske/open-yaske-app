/**
 * src/lib/styles/index.ts
 *
 * スタイルパックのバレルエクスポート。
 * 利用側は `$lib/styles` から一括で import できる。
 */

// 型定義
export type {
	StyleId,
	VisualStylePack,
	StyleLayout,
	StyleTokens,
	StyleComponents,
	StyleComponentsOverrides
} from './types';

// コンポーネント UI レシピ（中央デフォルト + リゾルバー）
export {
	DEFAULT_COMPONENTS,
	resolveComponents,
	resolveRadius,
	type StyleRadius
} from './components';

// 個別スタイルパック
export { applePack } from './apple';
export { materialPack } from './material';
export { neumorphismPack } from './neumorphism';
export { brutalismPack } from './brutalism';
export { cyberpunkPack } from './cyberpunk';
export { flatPack } from './flat';
export { highContrastPack } from './high-contrast';
export { win95Pack } from './win95';
export { winXpPack } from './winxp';

// レジストリ（動的スタイル登録）
export {
	registerStyle,
	registerStyles,
	getStyle,
	listStyles,
	getAllStyles,
	hasStyle,
	BUILTIN_STYLE_IDS
} from './registry';
