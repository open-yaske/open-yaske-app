/**
 * 公開データストア
 * - loadPublicData() をラップして Rune ストアとして公開
 * - オフライン状態を追跡 (navigator.onLine)
 */

import { browser } from '$app/environment';
import { loadPublicData, refreshPublicData } from '$lib/publicData';
import type { PublicData } from '$lib/types/public';

class PublicDataStore {
	data = $state<PublicData | null>(null);
	loading = $state(false);
	error = $state<string | null>(null);
	online = $state(true);
	initialized = $state(false);

	init() {
		if (!browser) return;
		if (this.initialized) return;
		this.initialized = true;

		this.online = navigator.onLine;

		window.addEventListener('online', () => {
			this.online = true;
		});
		window.addEventListener('offline', () => {
			this.online = false;
		});

		void this.load();
	}

	async load() {
		this.loading = true;
		this.error = null;
		try {
			this.data = await loadPublicData();
		} catch (err) {
			this.error = err instanceof Error ? err.message : String(err);
		} finally {
			this.loading = false;
		}
	}

	async refresh() {
		this.loading = true;
		this.error = null;
		try {
			this.data = await refreshPublicData();
		} catch (err) {
			this.error = err instanceof Error ? err.message : String(err);
		} finally {
			this.loading = false;
		}
	}
}

export const publicDataStore = new PublicDataStore();
