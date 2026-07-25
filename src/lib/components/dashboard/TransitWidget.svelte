<!--
  TransitWidget.svelte
  近くの駅から発車する電車の運行情報と発車予定時刻を秒単位でカウントダウン表示する Bento ウィジェット。
-->
<script lang="ts">
	import { onMount } from 'svelte';
	import { transitStore, settingsStore } from '$lib/stores';
	import { m } from '$lib/paraglide/messages';
	import { Train, RefreshCw, ChevronRight, AlertTriangle } from '@lucide/svelte';
	import BentoCard from '../BentoCard.svelte';
	import IconButton from '../IconButton.svelte';

	const settings = $derived(settingsStore.settings);
	const stationId = $derived(transitStore.stationId);
	const stationName = $derived(transitStore.stationName);
	const departures = $derived(transitStore.departures);
	const loading = $derived(transitStore.loading);
	const error = $derived(transitStore.error);
	const currentElapsedSecs = $derived(transitStore.currentElapsedSecs);

	// 未発車の電車だけをフィルタリングし、最大5件表示
	const upcomingDepartures = $derived(
		departures.filter((d) => d.departureSecs >= currentElapsedSecs).slice(0, 5)
	);

	onMount(() => {
		void transitStore.init();
		// 駅が未設定の場合は現在地の緯度経度から近くの駅を自動検索
		if (!transitStore.stationId && settings.lat && settings.lng) {
			void transitStore.searchNearbyStations(settings.lat, settings.lng);
		}
	});

	function handleRefresh() {
		void transitStore.loadDepartures(true);
	}

	async function selectStation(id: string, name: string, lat: number, lng: number) {
		await transitStore.setStation(id, name, lat, lng);
	}
</script>

<BentoCard padding="p-4 md:p-5" class="w-full">
	{#snippet header()}
		<div
			class="flex w-full items-center justify-between border-b border-[var(--color-surface-border)]/60 pb-3"
		>
			<div class="flex items-center gap-2.5 font-bold text-[var(--color-nav-active)] min-w-0">
				<div
					class="flex h-8 w-8 shrink-0 items-center justify-center rounded-xl bg-[var(--color-primary-500)]/15 text-[var(--color-primary-600)]"
				>
					<Train size={18} />
				</div>
				<div class="flex flex-col min-w-0">
					<span class="text-xs font-semibold leading-tight">{m.transit_title()}</span>
					{#if stationName}
						<span class="text-[10px] font-medium text-[var(--color-nav-inactive)] truncate">
							{stationName}
						</span>
					{/if}
				</div>
			</div>

			<div class="flex items-center gap-2 shrink-0">
				{#if stationId}
					<IconButton
						icon={RefreshCw}
						ariaLabel={m.transit_refresh()}
						onclick={handleRefresh}
						variant="subtle"
						size="sm"
						class={loading ? 'animate-spin' : ''}
					/>
				{/if}
				<a
					href="/transit"
					class="flex items-center gap-0.5 text-xs font-semibold text-[var(--color-primary-600)] hover:underline"
				>
					<span>{m.action_more()}</span>
					<ChevronRight size={14} />
				</a>
			</div>
		</div>
	{/snippet}

	{#if !stationId}
		<div class="py-6 text-center">
			<p class="text-xs text-[var(--color-nav-inactive)] mb-3">
				{m.transit_select_station()}
			</p>

			{#if transitStore.loadingNearby}
				<div
					class="flex items-center justify-center gap-2 text-xs text-[var(--color-nav-inactive)] py-2"
				>
					<div
						class="h-4 w-4 animate-spin rounded-full border-2 border-[var(--color-primary-500)] border-t-transparent"
					></div>
					<span>{m.transit_loading_stations()}</span>
				</div>
			{:else if transitStore.nearbyStations.length > 0}
				<div class="flex flex-col gap-1.5 max-w-xs mx-auto pt-1">
					<p class="text-[10px] font-semibold text-[var(--color-nav-inactive)] mb-1">
						近くの駅が見つかりました：
					</p>
					{#each transitStore.nearbyStations.slice(0, 3) as station (station.id)}
						<button
							type="button"
							onclick={() => selectStation(station.id, station.name, station.lat, station.lon)}
							class="w-full px-3 py-2 text-xs font-semibold rounded-xl bg-[var(--color-primary-500)]/10 text-[var(--color-primary-600)] hover:bg-[var(--color-primary-500)]/20 transition active:scale-[0.98] text-left truncate flex justify-between items-center"
						>
							<span class="truncate">{station.name}</span>
							<span class="text-[10px] text-[var(--color-nav-inactive)] font-normal shrink-0">
								{Math.round(station.distanceMeters ?? 0)}m
							</span>
						</button>
					{/each}
				</div>
			{:else}
				<a
					href="/transit"
					class="inline-flex items-center gap-1.5 text-xs font-bold text-[var(--color-primary-600)] bg-[var(--color-primary-500)]/10 px-4 py-2 rounded-xl hover:bg-[var(--color-primary-500)]/20 transition active:scale-95"
				>
					<Train size={14} />
					<span>{m.transit_find_nearby_stations()}</span>
				</a>
			{/if}
		</div>
	{:else if loading && departures.length === 0}
		<div class="py-8 text-center text-xs text-[var(--color-nav-inactive)]">
			<div
				class="inline-block h-5 w-5 animate-spin rounded-full border-2 border-[var(--color-primary-500)] border-t-transparent mb-2"
			></div>
			<p>{m.transit_loading_departures()}</p>
		</div>
	{:else if error && departures.length === 0}
		<div
			class="py-6 text-center text-xs text-[var(--color-danger-600)] flex flex-col items-center gap-2"
		>
			<AlertTriangle size={24} />
			<p>{m.transit_error_loading_departures()}</p>
			<p class="text-[10px] text-[var(--color-nav-inactive)]">{error}</p>
			<button
				type="button"
				onclick={handleRefresh}
				class="mt-1 px-3 py-1.5 rounded-lg bg-[var(--color-surface-muted)] text-[var(--color-nav-active)] font-semibold hover:bg-[var(--color-surface-border)] transition"
			>
				{m.transit_refresh()}
			</button>
		</div>
	{:else if upcomingDepartures.length === 0}
		<div class="py-8 text-center text-xs text-[var(--color-nav-inactive)]">
			{m.transit_no_departures()}
		</div>
	{:else}
		<ul class="divide-y divide-[var(--color-surface-border)]/40 pt-1">
			{#each upcomingDepartures as dep (dep.tripId + dep.departureSecs)}
				{@const remaining = dep.departureSecs - currentElapsedSecs}
				<li class="flex items-center justify-between py-2.5 gap-4">
					<div class="flex items-center gap-2.5 min-w-0 flex-1">
						<!-- 路線色インジケータ -->
						<span
							class="h-2.5 w-2.5 rounded-full shrink-0 border border-black/10"
							style="background-color: #{dep.color || '666666'}"
						></span>
						<div class="flex flex-col min-w-0">
							<span
								class="text-xs md:text-sm font-semibold text-[var(--color-nav-active)] truncate leading-tight"
							>
								{dep.headsign || '---'}
							</span>
							<span
								class="text-[10px] font-medium text-[var(--color-nav-inactive)] mt-0.5 truncate"
							>
								{dep.routeName}{dep.trainType ? ` • ${dep.trainType}` : ''}
							</span>
						</div>
					</div>

					<div class="text-right shrink-0">
						{#if remaining <= 0}
							<span class="text-xs font-bold text-[var(--color-danger-500)] animate-pulse">
								{m.transit_departs_just_now()}
							</span>
						{:else}
							{@const mins = Math.floor(remaining / 60)}
							{@const secs = remaining % 60}
							<span
								class="text-xs md:text-sm font-mono font-bold tracking-tight {remaining < 180
									? 'text-[var(--color-danger-500)] font-extrabold'
									: 'text-[var(--color-nav-active)]'}"
							>
								{#if mins > 0}
									{mins}<span
										class="text-[10px] font-sans font-medium text-[var(--color-nav-inactive)] ml-0.5 mr-1"
										>分</span
									>{secs.toString().padStart(2, '0')}<span
										class="text-[10px] font-sans font-medium text-[var(--color-nav-inactive)] ml-0.5"
										>秒</span
									>
								{:else}
									{secs}<span
										class="text-[10px] font-sans font-medium text-[var(--color-nav-inactive)] ml-0.5"
										>秒</span
									>
								{/if}
							</span>
						{/if}
					</div>
				</li>
			{/each}
		</ul>
	{/if}
</BentoCard>
