<!--
  WeatherCard.svelte
  現在の天気と時間別予報を表示するカード。
-->
<script lang="ts">
	import { m } from '$lib/paraglide/messages';
	import { weatherStore, settingsStore } from '$lib/stores';
	import { weatherCodeToLabel, weatherCodeToEmoji } from '$lib/weather';
	import { CloudOff, MapPin } from '@lucide/svelte';
	import EmptyState from './EmptyState.svelte';
	import Emoji from './Emoji.svelte';

	const settings = $derived(settingsStore.settings);
	const weather = $derived(weatherStore.current);
	const forecast = $derived(weatherStore.forecast);
	const loading = $derived(weatherStore.loading);
	const error = $derived(weatherStore.error);

	const emoji = $derived(weather ? weatherCodeToEmoji(weather.weatherCode, weather.isDay) : '🌡️');
	const labelKey = $derived(weather ? weatherCodeToLabel(weather.weatherCode) : 'weather_unknown');

	const weatherLabels: Record<string, string> = {
		weather_clear: m.weather_clear(),
		weather_partly_cloudy: m.weather_partly_cloudy(),
		weather_fog: m.weather_fog(),
		weather_drizzle: m.weather_drizzle(),
		weather_rain: m.weather_rain(),
		weather_showers: m.weather_showers(),
		weather_snow: m.weather_snow(),
		weather_snow_showers: m.weather_snow_showers(),
		weather_thunderstorm: m.weather_thunderstorm(),
		weather_unknown: m.weather_unknown()
	};

	const todayStr = $derived.by(() => {
		const d = new Date();
		const month = d.getMonth() + 1;
		const date = d.getDate();
		const dayOfWeek = ['日', '月', '火', '水', '木', '金', '土'][d.getDay()];
		return `${month}/${date} ${dayOfWeek}曜日`;
	});

	const todayHours = $derived(forecast?.hours ?? []);
	const maxTemp = $derived(
		todayHours.length ? Math.round(Math.max(...todayHours.map((h) => h.temperature))) : 0
	);
	const minTemp = $derived(
		todayHours.length ? Math.round(Math.min(...todayHours.map((h) => h.temperature))) : 0
	);
	const currentPrecip = $derived(todayHours.length ? todayHours[0].precipitationProbability : 0);

	// 6 points of forecast (index 0, 3, 6, 9, 12, 15)
	const forecastPoints = $derived.by(() => {
		if (todayHours.length === 0) return [];
		const points = [];
		const indices = [0, 3, 6, 9, 12, 15];
		const nowDay = new Date().getDate();

		for (const idx of indices) {
			const h = todayHours[idx];
			if (!h) break;

			let label = 'Now';
			if (idx > 0) {
				const date = new Date(h.time);
				const hr = date.getHours();
				const isTomorrow = date.getDate() !== nowDay;
				label = (isTomorrow ? '翌' : '') + hr + '時';
			}

			points.push({
				label,
				emoji: weatherCodeToEmoji(h.weatherCode, true),
				prob: h.precipitationProbability
			});
		}
		return points;
	});

	$effect(() => {
		const lat = settingsStore.settings.lat;
		const lng = settingsStore.settings.lng;
		if (typeof lat === 'number' && typeof lng === 'number') {
			void weatherStore.load(lat, lng);
		}
	});
</script>

<div
	class="overflow-hidden rounded-[var(--radius-card)] border border-[var(--color-surface-border)] bg-[var(--color-surface-card)] px-4 py-3.5"
>
	{#if loading && !weather}
		<!-- Loading skeleton -->
		<div class="flex items-center gap-4 py-4">
			<div class="h-12 w-12 animate-pulse rounded-full bg-[var(--color-surface-muted)]"></div>
			<div class="flex-1 space-y-2">
				<div class="h-7 w-24 animate-pulse rounded bg-[var(--color-surface-muted)]"></div>
				<div class="h-4 w-16 animate-pulse rounded bg-[var(--color-surface-muted)]"></div>
			</div>
		</div>
	{:else if error && !weather}
		<EmptyState
			icon={CloudOff}
			message={m.weather_error()}
			submessage={error ?? undefined}
			iconSize={36}
			class="py-6"
		/>
	{:else if weather}
		<!-- Main weather display (Header style from mock) -->
		<div class="flex flex-col gap-3">
			<div class="flex items-start justify-between">
				<div class="flex flex-col">
					<span class="text-2xl font-bold tracking-tight text-[var(--color-nav-active)]">
						{todayStr}
					</span>
					<span class="text-xs text-[var(--color-nav-inactive)] mt-0.5">
						{#if settings.locationName}
							<span class="inline-flex items-center gap-0.5">
								<MapPin size={11} />
								{settings.locationName}
							</span>
						{/if}
					</span>
				</div>
				<div class="flex items-center gap-2">
					<span class="text-4xl leading-none flex items-center"><Emoji {emoji} /></span>
					<div class="flex flex-col items-end">
						<div class="flex items-baseline gap-0.5">
							<span class="text-3xl font-bold leading-none text-[var(--color-nav-active)]">
								{Math.round(weather.temperature)}
							</span>
							<span class="text-sm font-medium text-[var(--color-nav-inactive)]">°C</span>
						</div>
						<span class="text-[10px] font-semibold text-[var(--color-nav-inactive)] mt-0.5">
							{weatherLabels[labelKey] ?? weatherLabels.weather_unknown}
						</span>
					</div>
				</div>
			</div>

			<!-- Extra details aligned horizontally in mock -->
			<div class="flex items-center gap-4 text-xs font-semibold">
				<span class="text-red-500">↑ {maxTemp}°C</span>
				<span class="text-blue-500">↓ {minTemp}°C</span>
				<span class="inline-flex items-center gap-1 text-sky-500"
					><Emoji emoji="💧" /> {currentPrecip}%</span
				>
			</div>

			{#if forecastPoints.length > 0}
				<!-- Hourly Forecast Grid -->
				<div
					class="grid grid-cols-6 gap-1 border-t border-[var(--color-surface-border)] pt-3 mt-1 text-center"
				>
					{#each forecastPoints as pt (pt.label)}
						<div class="flex flex-col items-center">
							<span class="text-[10px] font-medium text-[var(--color-nav-inactive)]"
								>{pt.label}</span
							>
							<span class="text-xl my-1 flex items-center justify-center"
								><Emoji emoji={pt.emoji} /></span
							>
							<span class="text-[10px] font-bold text-sky-500">{pt.prob}%</span>
						</div>
					{/each}
				</div>
			{/if}
		</div>
	{:else if !loading}
		<!-- 天気データ未取得（エラーなし）→ ローディングスケルトンを表示 -->
		<div class="flex items-center gap-4 py-4">
			<div class="h-12 w-12 animate-pulse rounded-full bg-[var(--color-surface-muted)]"></div>
			<div class="flex-1 space-y-2">
				<div class="h-7 w-24 animate-pulse rounded bg-[var(--color-surface-muted)]"></div>
				<div class="h-4 w-16 animate-pulse rounded bg-[var(--color-surface-muted)]"></div>
			</div>
		</div>
	{/if}
</div>
