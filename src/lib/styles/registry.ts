/**
 * src/lib/styles/registry.ts
 *
 * スタイルパックのレジストリ。
 *
 * 組み込み8スタイルは静的に import されるが、サードパティ/カスタムスタイルは
 * このレジストリ経由で動的に登録できる。これにより、新スタイル追加時に
 * style.svelte.ts や index.ts を編集する必要がなくなる。
 *
 * 使い方:
 * ```ts
 * import { registerStyle, getStyle, listStyles } from '$lib/styles/registry';
 * import { myPack } from './my-style';
 *
 * registerStyle(myPack);
 * const pack = getStyle('my-style');
 * ```
 */
import type { StyleId, VisualStylePack } from './types';
import { applePack } from './apple';
import { materialPack } from './material';
import { neumorphismPack } from './neumorphism';
import { brutalismPack } from './brutalism';
import { cyberpunkPack } from './cyberpunk';
import { flatPack } from './flat';
import { highContrastPack } from './high-contrast';
import { win95Pack } from './win95';
import { winXpPack } from './winxp';

/** 全スタイルパックのマップ（組み込み + 動的登録） */
const registry = new Map<string, VisualStylePack>([
	['apple', applePack],
	['material', materialPack],
	['neumorphism', neumorphismPack],
	['brutalism', brutalismPack],
	['cyberpunk', cyberpunkPack],
	['flat', flatPack],
	['high-contrast', highContrastPack],
	['win95', win95Pack],
	['winxp', winXpPack]
]);

/**
 * スタイルパックをレジストリに登録する。
 * 既存IDの場合は上書きされる（カスタムテーマでの差し替えに利用可能）。
 */
export function registerStyle(pack: VisualStylePack): void {
	registry.set(pack.id, pack);
}

/**
 * 複数のスタイルパックを一括登録する。
 */
export function registerStyles(packs: VisualStylePack[]): void {
	for (const pack of packs) {
		registry.set(pack.id, pack);
	}
}

/**
 * ID からスタイルパックを取得する。
 * 未登録の場合は undefined を返す。
 */
export function getStyle(id: StyleId | string): VisualStylePack | undefined {
	return registry.get(id);
}

/**
 * 登録済みの全スタイルIDを列挙する。
 */
export function listStyles(): string[] {
	return Array.from(registry.keys());
}

/**
 * 登録済みの全スタイルパックを配列で取得する。
 */
export function getAllStyles(): VisualStylePack[] {
	return Array.from(registry.values());
}

/**
 * スタイルが登録済みかどうかを判定する。
 */
export function hasStyle(id: StyleId | string): boolean {
	return registry.has(id);
}

/**
 * 組み込みスタイルIDの一覧（静的・不変）。
 */
export const BUILTIN_STYLE_IDS: readonly StyleId[] = [
	'apple',
	'material',
	'neumorphism',
	'brutalism',
	'cyberpunk',
	'flat',
	'high-contrast',
	'win95',
	'winxp'
] as const;
