/**
 * ユーザー設定ストア
 */

import { browser } from '$app/environment';
import { DEFAULT_USER_SETTINGS } from '$lib/types';
import type { UserSettings } from '$lib/types';
import { getBuiltinPack, migrateLegacyThemePackId, resolveThemePackId } from '$lib/theme';
import * as remoteStorage from '$lib/remotestorage';
import { setPublicDataCdnBase } from '$lib/publicData';
import { STORAGE_KEY_PREFIX } from '$lib/constants';
import { themeStore } from './theme.svelte';
import { syncWidgetData } from '$lib/widget';

const STORAGE_KEY = STORAGE_KEY_PREFIX + 'user-settings';

function applyBuiltinThemeImmediately(settings: UserSettings) {
	if (!browser) return;
	const targetPackId = resolveThemePackId(settings);
	let pack = getBuiltinPack(targetPackId);
	if (!pack) {
		pack = themeStore.customPacks.find((p) => p.id === targetPackId);
	}
	if (!pack) return;
	// themeStore.pack と同期し、オーバーライドをマージして適用
	themeStore.pack = pack;
	themeStore.setOverrides(settings.themeOverrides);
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
				const migrated = migrateLegacyThemePackId(parsed);
				if (migrated) Object.assign(parsed, migrated);
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
				if (parsed.homeFeatureLayouts) {
					this.settings.homeFeatureLayouts = {
						...DEFAULT_USER_SETTINGS.homeFeatureLayouts,
						...parsed.homeFeatureLayouts
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

		// themeOverrides は部分マージ（undefined が明示された場合はクリア）
		let themeOverrides = this.settings.themeOverrides;
		if (Object.prototype.hasOwnProperty.call(partial, 'themeOverrides')) {
			themeOverrides = partial.themeOverrides
				? { ...this.settings.themeOverrides, ...partial.themeOverrides }
				: undefined;
		}

		this.settings = {
			...this.settings,
			...partial,
			timetablePeriods,
			attendanceNotification,
			themeOverrides,
			updatedAt: Date.now()
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
		// themeOverrides 変更時にテーマストアへ反映
		if (Object.prototype.hasOwnProperty.call(partial, 'themeOverrides')) {
			themeStore.setOverrides(this.settings.themeOverrides);
		}
		void this.syncToRS();
		void syncWidgetData();
	}

	reset() {
		this.settings = { ...DEFAULT_USER_SETTINGS, updatedAt: Date.now() };
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
				const migrated = migrateLegacyThemePackId(remote);
				if (migrated) Object.assign(remote, migrated);

				const remoteUpdatedAt = remote.updatedAt || 0;
				const localUpdatedAt = this.settings.updatedAt || 0;

				if (remoteUpdatedAt > localUpdatedAt) {
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
					if (remote.homeFeatureLayouts) {
						this.settings.homeFeatureLayouts = {
							...DEFAULT_USER_SETTINGS.homeFeatureLayouts,
							...remote.homeFeatureLayouts
						};
					}
					localStorage.setItem(STORAGE_KEY, JSON.stringify(this.settings));
					setPublicDataCdnBase(this.settings.dataCdnUrl || null);
					applyBuiltinThemeImmediately(this.settings);
					void syncWidgetData();
				} else if (remoteUpdatedAt < localUpdatedAt) {
					await this.syncToRS();
				}
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
