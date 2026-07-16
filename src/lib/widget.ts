import { Capacitor, registerPlugin } from '@capacitor/core';
import { settingsStore } from './stores/settings.svelte';
import { userDataStore } from './stores/userData.svelte';

export interface WidgetBridgePluginType {
	setItem(options: { key: string; value: string; group: string }): Promise<{ results: boolean }>;
	getItem(options: { key: string; group: string }): Promise<{ results: string }>;
	removeItem(options: { key: string; group: string }): Promise<{ results: boolean }>;
	setRegisteredWidgets(options: { widgets: string[] }): Promise<{ results: boolean }>;
	reloadAllTimelines(): Promise<{ results: boolean }>;
}

const WidgetBridge = registerPlugin<WidgetBridgePluginType>('WidgetBridgePlugin');

const APP_GROUP = 'group.com.openyaske.app';
const WIDGET_CLASS_NAME = 'com.openyaske.app.TimetableWidgetProvider';

let isRegistered = false;

async function registerAndroidWidget() {
	if (Capacitor.getPlatform() === 'android' && !isRegistered) {
		try {
			await WidgetBridge.setRegisteredWidgets({
				widgets: [WIDGET_CLASS_NAME]
			});
			isRegistered = true;
		} catch (err) {
			console.error('Failed to register Android widget provider', err);
		}
	}
}

export async function syncWidgetData() {
	if (!Capacitor.isNativePlatform()) return;

	try {
		// Androidウィジェットプロバイダーの登録（初回のみ）
		await registerAndroidWidget();

		// 同期するデータの構築
		const coursesJson = JSON.stringify(userDataStore.courses);
		const periodsJson = JSON.stringify(settingsStore.settings.timetablePeriods);
		const useSystemTheme = settingsStore.settings.widgetUseSystemTheme ? 'true' : 'false';
		const themeColor = settingsStore.settings.widgetThemeColor;

		// データの書き込み
		await Promise.all([
			WidgetBridge.setItem({ key: 'courses', value: coursesJson, group: APP_GROUP }),
			WidgetBridge.setItem({ key: 'timetablePeriods', value: periodsJson, group: APP_GROUP }),
			WidgetBridge.setItem({
				key: 'widgetUseSystemTheme',
				value: useSystemTheme,
				group: APP_GROUP
			}),
			WidgetBridge.setItem({ key: 'widgetThemeColor', value: themeColor, group: APP_GROUP })
		]);

		// ウィジェットの再描画をリクエスト
		await WidgetBridge.reloadAllTimelines();
		console.log('Widget data sync completed successfully');
	} catch (err) {
		console.error('Failed to sync widget data', err);
	}
}
