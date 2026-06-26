/**
 * ユーザー設定ストア
 */

import { browser } from '$app/environment';
import { DEFAULT_USER_SETTINGS } from '$lib/types';
import type { UserSettings } from '$lib/types';
import { applyTheme, getBuiltinPack } from '$lib/theme';
import * as remoteStorage from '$lib/remotestorage';
import { setPublicDataCdnBase } from '$lib/publicData';
import { STORAGE_KEY_PREFIX } from '$lib/constants';
import { themeStore } from './theme.svelte';

const STORAGE_KEY = STORAGE_KEY_PREFIX + 'user-settings';
const ACTIVE_THEME_KEY = STORAGE_KEY_PREFIX + 'active-theme';

function resolveThemePackId(settings: UserSettings) {
	const themePackIdLight = settings.themePackIdLight || 'default';
	const themePackIdDark = settings.themePackIdDark || 'dark';

	let targetPackId = themePackIdLight;
	if (settings.theme === 'dark') {
		targetPackId = themePackIdDark;
	} else if (settings.theme === 'system') {
		const osDark =
			typeof window !== 'undefined' && window.matchMedia('(prefers-color-scheme: dark)').matches;
		targetPackId = osDark ? themePackIdDark : themePackIdLight;
	}

	return targetPackId;
}

function applyBuiltinThemeImmediately(settings: UserSettings) {
	if (!browser) return;
	const targetPackId = resolveThemePackId(settings);
	let pack = getBuiltinPack(targetPackId);
	if (!pack) {
		pack = themeStore.customPacks.find((p) => p.id === targetPackId);
	}
	if (!pack) return;
	localStorage.setItem(ACTIVE_THEME_KEY, pack.id);
	applyTheme(pack);
	// themeStore.pack とも同期
	themeStore.pack = pack;
}

class SettingsStore {
	settings = $state<UserSettings>({ ...DEFAULT_USER_SETTINGS });
	initialized = $state(false);

	init() {
		if (!browser) return;
		if (this.initialized) return;
		this.initialized = true;
		const saved = localStorage.getItem(STORAGE_KEY);
		if (saved) {
			try {
				const parsed = JSON.parse(saved) as Partial<UserSettings>;
				// 移行処理：既存の themePackId を themePackIdLight/Dark にマッピング
				if (parsed.themePackId && !parsed.themePackIdLight && !parsed.themePackIdDark) {
					if (parsed.themePackId === 'dark') {
						parsed.themePackIdDark = 'dark';
						parsed.themePackIdLight = 'default';
					} else {
						parsed.themePackIdLight = parsed.themePackId;
						parsed.themePackIdDark = 'dark';
					}
				}
				this.settings = { ...DEFAULT_USER_SETTINGS, ...parsed };
				if (parsed.timetablePeriods) {
					this.settings.timetablePeriods = {
						...DEFAULT_USER_SETTINGS.timetablePeriods,
						...parsed.timetablePeriods
					};
				}
				if (parsed.attendanceNotification) {
					this.settings.attendanceNotification = {
						...DEFAULT_USER_SETTINGS.attendanceNotification,
						...parsed.attendanceNotification
					};
				}
			} catch {
				this.settings = { ...DEFAULT_USER_SETTINGS };
			}
		}
		// 公開データ CDN を settings 値から解決
		setPublicDataCdnBase(this.settings.dataCdnUrl || null);
		applyBuiltinThemeImmediately(this.settings);
		window.addEventListener('rs:connected', () => {
			void this.loadFromRS();
		});

		if (remoteStorage.isConnected()) {
			void this.loadFromRS();
		}
	}

	update(partial: Partial<UserSettings>) {
		const timetablePeriods = partial.timetablePeriods
			? { ...this.settings.timetablePeriods, ...partial.timetablePeriods }
			: this.settings.timetablePeriods;
		const attendanceNotification = partial.attendanceNotification
			? { ...this.settings.attendanceNotification, ...partial.attendanceNotification }
			: this.settings.attendanceNotification;

		this.settings = {
			...this.settings,
			...partial,
			timetablePeriods,
			attendanceNotification
		};
		if (browser) {
			localStorage.setItem(STORAGE_KEY, JSON.stringify(this.settings));
		}
		// dataCdnUrl 変更時に publicData へ反映
		if (Object.prototype.hasOwnProperty.call(partial, 'dataCdnUrl')) {
			setPublicDataCdnBase(partial.dataCdnUrl ?? null);
		}
		if (
			Object.prototype.hasOwnProperty.call(partial, 'theme') ||
			Object.prototype.hasOwnProperty.call(partial, 'themePackId') ||
			Object.prototype.hasOwnProperty.call(partial, 'themePackIdLight') ||
			Object.prototype.hasOwnProperty.call(partial, 'themePackIdDark')
		) {
			applyBuiltinThemeImmediately(this.settings);
		}
		void this.syncToRS();
	}

	reset() {
		this.settings = { ...DEFAULT_USER_SETTINGS };
		if (browser) {
			localStorage.removeItem(STORAGE_KEY);
		}
		setPublicDataCdnBase(null);
		applyBuiltinThemeImmediately(this.settings);
		void this.syncToRS();
	}

	async syncToRS() {
		if (!remoteStorage.isConnected()) return;
		try {
			await remoteStorage.rsSet('settings/data', this.settings);
		} catch (err) {
			console.error('Failed to sync settings to RS', err);
		}
	}

	async loadFromRS() {
		try {
			const remote = await remoteStorage.rsGet<Partial<UserSettings>>('settings/data');
			if (remote) {
				// 移行処理：既存の themePackId を themePackIdLight/Dark にマッピング
				if (remote.themePackId && !remote.themePackIdLight && !remote.themePackIdDark) {
					if (remote.themePackId === 'dark') {
						remote.themePackIdDark = 'dark';
						remote.themePackIdLight = 'default';
					} else {
						remote.themePackIdLight = remote.themePackId;
						remote.themePackIdDark = 'dark';
					}
				}
				this.settings = { ...DEFAULT_USER_SETTINGS, ...remote };
				if (remote.timetablePeriods) {
					this.settings.timetablePeriods = {
						...DEFAULT_USER_SETTINGS.timetablePeriods,
						...remote.timetablePeriods
					};
				}
				if (remote.attendanceNotification) {
					this.settings.attendanceNotification = {
						...DEFAULT_USER_SETTINGS.attendanceNotification,
						...remote.attendanceNotification
					};
				}
				localStorage.setItem(STORAGE_KEY, JSON.stringify(this.settings));
				setPublicDataCdnBase(this.settings.dataCdnUrl || null);
				applyBuiltinThemeImmediately(this.settings);
			} else {
				// リモートに設定データが存在しない場合は、現在のローカル設定をアップロードする（初期同期）
				await this.syncToRS();
			}
		} catch (err) {
			console.error('Failed to load settings from RS', err);
		}
	}
}

export const settingsStore = new SettingsStore();
