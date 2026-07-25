<!--
  WeatherCard.svelte
  現在の天気と時間別予報を表示する Apple スタイル Bento Card。
-->
<script lang="ts">
	import { m } from '$lib/paraglide/messages';
	import { weatherStore, settingsStore, transitStore, formatDepartureTime } from '$lib/stores';
	import { weatherCodeToLabel, weatherCodeToEmoji } from '$lib/weather';
	import { CloudOff, MapPin, Train } from '@lucide/svelte';
	import EmptyState from './EmptyState.svelte';
	import Emoji from './Emoji.svelte';
	import BentoCard from './BentoCard.svelte';

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
		return `${month}/${date} (${dayOfWeek})`;
	});

	const todayHours = $derived(forecast?.hours ?? []);
	const maxTemp = $derived(
		todayHours.length ? Math.max(...todayHours.map((h) => h.temperature)) : 0
	);
	const minTemp = $derived(
		todayHours.length ? Math.min(...todayHours.map((h) => h.temperature)) : 0
	);
	const currentPrecip = $derived(todayHours.length ? todayHours[0].precipitationProbability : 0);

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

	let currentTime = $state(new Date());

	$effect(() => {
		const interval = setInterval(() => {
			currentTime = new Date();
		}, 1000);
		return () => clearInterval(interval);
	});

	function getTempDisplay(celsius: number): { value: number; unit: string } {
		if (settings.weatherTempUnit === 'fahrenheit') {
			return { value: Math.round(celsius * 1.8 + 32), unit: '°F' };
		}
		return { value: Math.round(celsius), unit: '°C' };
	}

	function formatTime(date: Date | number | null | undefined): string {
		if (!date) return '';
		const d = typeof date === 'number' ? new Date(date) : date;
		const is12h = settings.timeFormat === '12h';
		const showSeconds = settings.weatherTimeFormat === 'second';

		const options: Intl.DateTimeFormatOptions = {
			hour: 'numeric',
			minute: '2-digit',
			hour12: is12h
		};
		if (showSeconds) {
			options.second = '2-digit';
		}

		const loc = settings.locale || 'ja';
		return new Intl.DateTimeFormat(loc, options).format(d);
	}

	$effect(() => {
		const lat = settings.lat;
		const lng = settings.lng;
		if (typeof lat === 'number' && typeof lng === 'number') {
			void weatherStore.load(lat, lng);
		}
		if (settings.weatherShowTransit && settings.transitStationId) {
			void transitStore.loadDepartures();
		}
	});

	const weatherUrl = $derived(
		'https://www.google.com/search?q=' +
			encodeURIComponent((settings.locationName || '調布') + ' 天気')
	);

	// 未発車の電車を WeatherCard 内で最大2件表示
	const cardTransitDepartures = $derived(
		transitStore.departures
			.filter((d) => d.departureSecs >= transitStore.currentElapsedSecs)
			.slice(0, 2)
	);
</script>

<BentoCard
	href={weatherUrl}
	target="_blank"
	rel="noopener noreferrer"
	padding="px-4 py-3.5"
	class="w-full"
>
	{#if loading && !weather}
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
		<div class="flex flex-col gap-3">
			<div class="flex items-start justify-between">
				<div class="flex flex-col">
					<div class="flex items-baseline gap-2 flex-wrap">
						<span
							class="text-xl md:text-2xl font-bold tracking-tight text-[var(--color-nav-active)]"
						>
							{todayStr}
						</span>
						<span
							class="text-xl md:text-2xl font-bold tracking-tight text-[var(--color-nav-active)] font-mono"
						>
							{formatTime(currentTime)}
						</span>
					</div>
					<span class="text-[10px] text-[var(--color-nav-inactive)] mt-1">
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
							<span
								class="text-2xl md:text-3xl font-bold leading-none text-[var(--color-nav-active)] font-mono"
							>
								{getTempDisplay(weather.temperature).value}
							</span>
							<span class="text-sm font-medium text-[var(--color-nav-inactive)]">
								{getTempDisplay(weather.temperature).unit}
							</span>
						</div>
						<span class="text-[10px] font-semibold text-[var(--color-nav-inactive)] mt-0.5">
							{weatherLabels[labelKey] ?? weatherLabels.weather_unknown}
						</span>
					</div>
				</div>
			</div>

			<div class="flex items-center justify-between text-xs font-semibold">
				<div class="flex items-center gap-4">
					<span
						class="text-[var(--color-danger-400)] dark:text-[var(--color-danger-400)] text-red-600 font-bold"
						>↑ {getTempDisplay(maxTemp).value}{getTempDisplay(maxTemp).unit}</span
					>
					<span
						class="text-[var(--color-primary-600)] dark:text-[var(--color-primary-400)] font-bold"
						>↓ {getTempDisplay(minTemp).value}{getTempDisplay(minTemp).unit}</span
					>
					<span
						class="inline-flex items-center gap-1 text-[var(--color-primary-600)] dark:text-[var(--color-primary-400)] font-bold"
						><Emoji emoji="💧" /> {currentPrecip}%</span
					>
				</div>
				{#if weatherStore.lastUpdated}
					<span class="text-[10px] font-normal text-[var(--color-nav-inactive)]">
						{m.weather_last_updated({ time: formatTime(weatherStore.lastUpdated) })}
					</span>
				{/if}
			</div>

			{#if forecastPoints.length > 0}
				<div
					class="grid grid-cols-6 gap-1 border-t border-[var(--color-surface-border)]/50 pt-3 mt-1 text-center"
				>
					{#each forecastPoints as pt (pt.label)}
						<div class="flex flex-col items-center">
							<span class="text-[10px] font-medium text-[var(--color-nav-inactive)]"
								>{pt.label}</span
							>
							<span class="text-xl my-1 flex items-center justify-center"
								><Emoji emoji={pt.emoji} /></span
							>
							<span class="text-[10px] font-bold text-[var(--color-primary-600)] font-mono"
								>{pt.prob}%</span
							>
						</div>
					{/each}
				</div>
			{/if}

			{#if settings.weatherShowTransit && settings.transitStationId && cardTransitDepartures.length > 0}
				<a
					href="/transit"
					class="block border-t border-[var(--color-surface-border)]/50 pt-3.5 mt-1 hover:opacity-85 transition active:scale-[0.99] origin-center cursor-pointer"
				>
					<div
						class="flex items-center justify-between text-[10px] font-bold text-[var(--color-nav-inactive)] mb-2"
					>
						<span class="inline-flex items-center gap-1 leading-none">
							<Train size={11} />
							{settings.transitStationName}
							{m.transit_title()}
						</span>
					</div>
					<div class="flex flex-col gap-1.5">
						{#each cardTransitDepartures as dep (dep.tripId + dep.departureSecs)}
							{@const remaining = dep.departureSecs - transitStore.currentElapsedSecs}
							<div class="flex items-center justify-between text-[11px] font-medium">
								<div class="flex items-center gap-2 min-w-0 flex-1">
									<span
										class="h-2 w-2 rounded-full shrink-0 border border-black/5"
										style="background-color: #{dep.color || '666666'}"
									></span>
									<span class="font-semibold text-[var(--color-nav-active)] truncate">
										{dep.headsign}
									</span>
								</div>
								<div class="text-right shrink-0 flex items-center gap-3">
									<span class="text-[11px] font-mono font-bold text-[var(--color-nav-active)]">
										{formatDepartureTime(dep.departureSecs)}
									</span>
									<div
										class="w-12 text-right font-mono font-semibold text-[10px] text-[var(--color-nav-inactive)]"
									>
										{#if remaining <= 0}
											<span class="text-[var(--color-danger-500)] font-bold animate-pulse">
												{m.transit_departs_just_now()}
											</span>
										{:else}
											{@const mins = Math.floor(remaining / 60)}
											{@const secs = remaining % 60}
											<span
												class={remaining < 180 ? 'text-[var(--color-danger-500)] font-bold' : ''}
											>
												{#if mins > 0}
													{mins}:{secs.toString().padStart(2, '0')}
												{:else}
													{secs}秒
												{/if}
											</span>
										{/if}
									</div>
								</div>
							</div>
						{/each}
					</div>
				</a>
			{/if}
		</div>
	{:else if !loading}
		<div class="flex items-center gap-4 py-4">
			<div class="h-12 w-12 animate-pulse rounded-full bg-[var(--color-surface-muted)]"></div>
			<div class="flex-1 space-y-2">
				<div class="h-7 w-24 animate-pulse rounded bg-[var(--color-surface-muted)]"></div>
				<div class="h-4 w-16 animate-pulse rounded bg-[var(--color-surface-muted)]"></div>
			</div>
		</div>
	{/if}
</BentoCard>
