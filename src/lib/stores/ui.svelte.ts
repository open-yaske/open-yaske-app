/**
 * グローバル UI 状態
 * - 検索バー表示
 * - コマンドパレット
 * - トースト通知
 */

type Toast = {
	id: string;
	message: string;
	kind: 'info' | 'success' | 'error' | 'warning';
	durationMs?: number;
};

class UIStore {
	searchOpen = $state(false);
	paletteOpen = $state(false);
	toasts = $state<Toast[]>([]);

	openSearch() {
		this.searchOpen = true;
	}

	closeSearch() {
		this.searchOpen = false;
	}

	openPalette() {
		this.paletteOpen = true;
	}

	closePalette() {
		this.paletteOpen = false;
	}

	toast(message: string, kind: Toast['kind'] = 'info', durationMs = 3000) {
		const id = Math.random().toString(36).slice(2);
		this.toasts = [...this.toasts, { id, message, kind, durationMs }];
		if (durationMs > 0) {
			setTimeout(() => this.dismiss(id), durationMs);
		}
	}

	dismiss(id: string) {
		this.toasts = this.toasts.filter((t) => t.id !== id);
	}
}

export const uiStore = new UIStore();
