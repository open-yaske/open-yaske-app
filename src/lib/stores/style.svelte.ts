/**
 * src/lib/stores/style.svelte.ts
 * ビジュアルスタイルの状態管理。
 *
 * テーマパック（色）と同様に、視覚スタイル（Apple / Material 等）を
 * 切り替えるためのリアクティブストア。
 *
 * スタイルパックの解決は `$lib/styles/registry` を経由する。
 * これにより組み込み8スタイルに加え、registry に動的登録された
 * カスタム/サードパティスタイルも透過的に切り替え可能。
 *
 * 使い方:
 *   import { styleStore } from '$lib/stores/style';
 *   $effect(() => { styleStore.pack.press.button ... });
 */
import { getStyle, listStyles } from '$lib/styles/registry';
import { materialPack } from '$lib/styles/material';
import type { StyleId, VisualStylePack } from '$lib/styles/types';

class StyleStore {
	/** 現在選択中のスタイルID */
	id = $state<StyleId | string>('material');

	/** 現在のスタイルパック（id に応じて自動的に変わる） */
	pack = $derived<VisualStylePack>(getStyle(this.id) ?? materialPack);

	/**
	 * スタイルを切り替える。
	 * 組み込み・動的登録を問わず registry から解決する。
	 */
	set(id: StyleId | string): void {
		if (getStyle(id)) {
			this.id = id;
		} else {
			console.warn(`Unknown visual style: ${id}, falling back to material`);
			this.id = 'material';
		}
	}

	/** 利用可能なスタイルID一覧（組み込み + 動的登録） */
	get availableIds(): string[] {
		return listStyles();
	}
}

export const styleStore = new StyleStore();
