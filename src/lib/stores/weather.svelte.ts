/**
 * 天気ストア
 * Open-Meteo API を使い、緯度経度から現在の天気と時間別予報を取得
 * 30 分間キャッシュ
 */

import { browser } from '$app/environment';
import {
	getCurrentWeather,
	getHourlyForecast,
	type CurrentWeather,
	type HourlyForecast
} from '$lib/weather';

interface CachedWeather {
	data: CurrentWeather;
	forecast?: HourlyForecast;
	timestamp: number;
}

const CACHE_TTL_MS = 30 * 60 * 1000; // 30 minutes
// Module-scoped cache; not exposed to reactivity consumers.
// eslint-disable-next-line svelte/prefer-svelte-reactivity
const cache = new Map<string, CachedWeather>();

class WeatherStore {
	current = $state<CurrentWeather | null>(null);
	forecast = $state<HourlyForecast | null>(null);
	loading = $state(false);
	error = $state<string | null>(null);

	async load(lat: number, lng: number, force = false) {
		if (!browser) return;
		const key = `${lat.toFixed(2)},${lng.toFixed(2)}`;
		const cached = cache.get(key);
		if (!force && cached && Date.now() - cached.timestamp < CACHE_TTL_MS) {
			this.current = cached.data;
			this.forecast = cached.forecast ?? null;
			return;
		}
		this.loading = true;
		this.error = null;
		try {
			const data = await getCurrentWeather(lat, lng);
			const forecast = await getHourlyForecast(lat, lng);
			this.current = data;
			this.forecast = forecast;
			cache.set(key, { data, forecast, timestamp: Date.now() });
		} catch (e) {
			this.error = e instanceof Error ? e.message : 'failed';
		} finally {
			this.loading = false;
		}
	}

	clear() {
		cache.clear();
	}
}

export const weatherStore = new WeatherStore();
