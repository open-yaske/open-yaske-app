/**
 * テーマアクティブストア
 * ユーザーの選択中テーマを管理し、CSS 変数として <html> に適用する
 */

import { browser } from '$app/environment';
import { applyTheme, BUILTIN_PACKS, getBuiltinPack } from '$lib/theme';
import type { ThemePack } from '$lib/theme';
import * as remoteStorage from '$lib/remotestorage';

import { STORAGE_KEY_PREFIX } from '$lib/constants';

const STORAGE_KEY = STORAGE_KEY_PREFIX + 'active-theme';
const CUSTOM_PACKS_KEY = STORAGE_KEY_PREFIX + 'custom-packs';

class ThemeStore {
	pack = $state<ThemePack>(BUILTIN_PACKS[0]);
	customPacks = $state<ThemePack[]>([]);
	initialized = $state(false);

	init() {
		if (!browser) return;
		if (this.initialized) return;
		this.initialized = true;

		const localCustom = localStorage.getItem(CUSTOM_PACKS_KEY);
		if (localCustom) {
			try {
				this.customPacks = JSON.parse(localCustom) as ThemePack[];
			} catch {
				this.customPacks = [];
			}
		}

		void this.loadCustomPacksFromRS();

		const saved = localStorage.getItem(STORAGE_KEY);
		if (saved) {
			const builtin = getBuiltinPack(saved);
			if (builtin) {
				this.pack = builtin;
			} else {
				const custom = this.customPacks.find((p) => p.id === saved);
				this.pack = custom ?? BUILTIN_PACKS[0];
			}
		}
		applyTheme(this.pack);
	}

	set(pack: ThemePack) {
		// 同じパックの場合は何もしない（$effect の無限ループ防止）
		if (this.pack.id === pack.id) return;
		this.pack = pack;
		if (browser) {
			localStorage.setItem(STORAGE_KEY, pack.id);
			applyTheme(pack);
		}
		void this.syncActiveThemeToRS();
	}

	setById(id: string) {
		const builtin = getBuiltinPack(id);
		if (builtin) {
			this.set(builtin);
			return;
		}
		const custom = this.customPacks.find((p) => p.id === id);
		if (custom) {
			this.set(custom);
		}
	}

	async saveCustomPack(pack: ThemePack) {
		const existingIndex = this.customPacks.findIndex((p) => p.id === pack.id);
		if (existingIndex >= 0) {
			this.customPacks = this.customPacks.map((p, i) => (i === existingIndex ? pack : p));
		} else {
			this.customPacks = [...this.customPacks, pack];
		}
		if (browser) {
			localStorage.setItem(CUSTOM_PACKS_KEY, JSON.stringify(this.customPacks));
			try {
				await remoteStorage.rsSet(`custom-packs/${pack.id}`, pack);
			} catch (err) {
				console.error('Failed to save custom pack to RS', err);
			}
		}
	}

	private async loadCustomPacksFromRS() {
		try {
			const remote = await remoteStorage.rsGetAll<ThemePack>('custom-packs');
			const list = Object.values(remote);
			if (list.length) {
				this.customPacks = list;
				localStorage.setItem(CUSTOM_PACKS_KEY, JSON.stringify(this.customPacks));
			}
		} catch (err) {
			console.error('Failed to load custom packs from RS', err);
		}
	}

	private async syncActiveThemeToRS() {
		if (!browser) return;
		if (!remoteStorage.isConnected()) return;
		try {
			// storeObject はオブジェクト形式が必要なため、文字列はラップして渡す
			await remoteStorage.rsSet('settings/active-theme', { id: this.pack.id });
		} catch (err) {
			console.error('Failed to sync active theme to RS', err);
		}
	}
}

export const themeStore = new ThemeStore();
