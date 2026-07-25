<!--
  transit/+page.svelte
  運行情報の詳細表示と駅設定画面。
-->
<script lang="ts">
	import { onMount } from 'svelte';
	import { transitStore, settingsStore, formatDepartureTime } from '$lib/stores';
	import { m } from '$lib/paraglide/messages';
	import PageHeader from '$lib/components/PageHeader.svelte';
	import Container from '$lib/components/Container.svelte';
	import BentoCard from '$lib/components/BentoCard.svelte';
	import TextField from '$lib/components/TextField.svelte';
	import Button from '$lib/components/Button.svelte';
	import IconButton from '$lib/components/IconButton.svelte';
	import {
		Train,
		Search,
		RefreshCw,
		MapPin,
		Trash2,
		AlertTriangle,
		ArrowLeft,
		Clock
	} from '@lucide/svelte';

	const settings = $derived(settingsStore.settings);
	const stationId = $derived(transitStore.stationId);
	const stationName = $derived(transitStore.stationName);
	const departures = $derived(transitStore.departures);
	const loading = $derived(transitStore.loading);
	const error = $derived(transitStore.error);
	const currentElapsedSecs = $derived(transitStore.currentElapsedSecs);

	let searchQuery = $state('');
	let searchResults = $state<any[]>([]);
	let searching = $state(false);

	// 未発車の電車をフィルタリング（全件表示）
	const upcomingDepartures = $derived(
		departures.filter((d) => d.departureSecs >= currentElapsedSecs)
	);

	onMount(() => {
		void transitStore.init();
		// 初期ロード時に駅が未設定なら、近くの駅を検索
		if (!transitStore.stationId && settings.lat && settings.lng) {
			void transitStore.searchNearbyStations(settings.lat, settings.lng);
		}
	});

	function handleRefresh() {
		void transitStore.loadDepartures(true);
	}

	async function handleSearch() {
		const q = searchQuery.trim();
		if (!q) return;
		searching = true;
		try {
			searchResults = await transitStore.searchStationsByName(q);
		} catch (err) {
			console.error(err);
		} finally {
			searching = false;
		}
	}

	async function selectStation(id: string, name: string, lat: number, lon: number) {
		await transitStore.setStation(id, name, lat, lon);
		searchQuery = '';
		searchResults = [];
	}

	async function clearStation() {
		await transitStore.clearStation();
		if (settings.lat && settings.lng) {
			void transitStore.searchNearbyStations(settings.lat, settings.lng);
		}
	}

	function handleFindNearby() {
		if (settings.lat && settings.lng) {
			void transitStore.searchNearbyStations(settings.lat, settings.lng);
		}
	}
</script>

<svelte:head>
	<title>{m.nav_transit()} | OpenYASKE</title>
</svelte:head>

{#snippet leadingSnippet()}
	<a
		href="/"
		class="flex h-8 w-8 items-center justify-center rounded-full text-[var(--color-nav-inactive)] hover:bg-[var(--color-surface-muted)] hover:text-[var(--color-nav-active)] transition"
	>
		<ArrowLeft size={20} />
	</a>
{/snippet}

{#snippet trailingSnippet()}
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
{/snippet}

<PageHeader title={m.nav_transit()} leading={leadingSnippet} trailing={trailingSnippet} />

<Container class="pb-16 max-w-4xl">
	<div class="space-y-6 py-4">
		<!-- 駅設定・検索パネル -->
		<BentoCard padding="p-4 md:p-5" class="w-full">
			{#if stationId}
				<div class="flex items-center justify-between gap-4">
					<div class="flex items-center gap-3 min-w-0">
						<div
							class="flex h-10 w-10 shrink-0 items-center justify-center rounded-2xl bg-[var(--color-primary-500)]/15 text-[var(--color-primary-600)]"
						>
							<Train size={20} />
						</div>
						<div class="min-w-0">
							<span class="text-xs font-semibold text-[var(--color-nav-inactive)] block"
								>{m.transit_station()}</span
							>
							<span class="text-base font-bold text-[var(--color-nav-active)] truncate block"
								>{stationName}</span
							>
						</div>
					</div>
					<Button variant="danger" size="sm" onclick={clearStation} class="gap-1.5 font-bold">
						<Trash2 size={14} />
						<span>解除</span>
					</Button>
				</div>
			{:else}
				<div class="space-y-4">
					<div class="flex items-center gap-2">
						<Train size={18} class="text-[var(--color-primary-600)]" />
						<span class="text-sm font-bold text-[var(--color-nav-active)]"
							>{m.transit_select_station()}</span
						>
					</div>

					<!-- キーワード検索 -->
					<div class="flex gap-2">
						<div class="flex-1">
							<TextField
								bind:value={searchQuery}
								placeholder="駅名を入力 (例: 新宿, 調布)"
								onkeydown={(e) => e.key === 'Enter' && handleSearch()}
							/>
						</div>
						<Button
							onclick={handleSearch}
							disabled={searching || !searchQuery.trim()}
							variant="primary"
							class="gap-1 font-bold"
						>
							<Search size={16} />
							<span>検索</span>
						</Button>
					</div>

					<!-- 検索結果表示 -->
					{#if searching}
						<div
							class="flex items-center justify-center gap-2 py-4 text-xs text-[var(--color-nav-inactive)]"
						>
							<div
								class="h-4 w-4 animate-spin rounded-full border-2 border-[var(--color-primary-500)] border-t-transparent"
							></div>
							<span>検索中…</span>
						</div>
					{:else if searchResults.length > 0}
						<div
							class="border border-[var(--color-surface-border)]/60 rounded-2xl overflow-hidden divide-y divide-[var(--color-surface-border)]/40 max-h-60 overflow-y-auto"
						>
							{#each searchResults as result (result.id)}
								<button
									type="button"
									onclick={() => selectStation(result.id, result.name, result.lat, result.lon)}
									class="w-full px-4 py-3 text-sm font-semibold text-left text-[var(--color-nav-active)] hover:bg-[var(--color-surface-muted)]/50 transition flex items-center justify-between"
								>
									<span>{result.name}</span>
									<span class="text-xs text-[var(--color-nav-inactive)] font-medium">設定する</span>
								</button>
							{/each}
						</div>
					{/if}

					<!-- 位置情報から探す（周辺駅） -->
					<div class="border-t border-[var(--color-surface-border)]/50 pt-4">
						<div class="flex items-center justify-between mb-3">
							<span
								class="text-xs font-bold text-[var(--color-nav-inactive)] inline-flex items-center gap-1"
							>
								<MapPin size={12} />
								現在地近くの駅（天気の設定地点）
							</span>
							<Button
								variant="secondary"
								size="sm"
								onclick={handleFindNearby}
								disabled={transitStore.loadingNearby}
								class="font-bold"
							>
								<RefreshCw
									size={11}
									class="mr-1 {transitStore.loadingNearby ? 'animate-spin' : ''}"
								/>
								再検索
							</Button>
						</div>

						{#if transitStore.loadingNearby}
							<div
								class="flex items-center justify-center gap-2 py-4 text-xs text-[var(--color-nav-inactive)]"
							>
								<div
									class="h-4 w-4 animate-spin rounded-full border-2 border-[var(--color-primary-500)] border-t-transparent"
								></div>
								<span>近くの駅を探しています…</span>
							</div>
						{:else if transitStore.nearbyStations.length > 0}
							<div class="grid grid-cols-1 sm:grid-cols-2 gap-2">
								{#each transitStore.nearbyStations as station (station.id)}
									<button
										type="button"
										onclick={() =>
											selectStation(station.id, station.name, station.lat, station.lon)}
										class="px-4 py-2.5 rounded-xl border border-[var(--color-surface-border)]/60 text-left hover:bg-[var(--color-surface-muted)]/50 transition active:scale-[0.98] flex items-center justify-between min-w-0"
									>
										<div class="min-w-0 flex-1 pr-2">
											<span class="text-xs font-bold text-[var(--color-nav-active)] truncate block"
												>{station.name}</span
											>
											<span class="text-[10px] text-[var(--color-nav-inactive)] block mt-0.5">
												徒歩約 {Math.round((station.distanceMeters ?? 0) / 80)} 分 ({Math.round(
													station.distanceMeters ?? 0
												)}m)
											</span>
										</div>
										<span class="text-[10px] font-bold text-[var(--color-primary-600)] shrink-0"
											>設定</span
										>
									</button>
								{/each}
							</div>
						{:else}
							<p class="text-xs text-[var(--color-nav-inactive)] text-center py-2">
								{m.transit_no_stations_found()}
							</p>
						{/if}
					</div>
				</div>
			{/if}
		</BentoCard>

		<!-- ダイヤ情報表示（駅が設定されている場合） -->
		{#if stationId}
			<BentoCard padding="p-4 md:p-5" class="w-full">
				{#if loading && departures.length === 0}
					<div class="py-12 text-center text-xs text-[var(--color-nav-inactive)]">
						<div
							class="inline-block h-6 w-6 animate-spin rounded-full border-2 border-[var(--color-primary-500)] border-t-transparent mb-3"
						></div>
						<p>{m.transit_loading_departures()}</p>
					</div>
				{:else if error && departures.length === 0}
					<div
						class="py-10 text-center text-xs text-[var(--color-danger-600)] flex flex-col items-center gap-2"
					>
						<AlertTriangle size={32} />
						<p class="font-bold">{m.transit_error_loading_departures()}</p>
						<p class="text-[10px] text-[var(--color-nav-inactive)]">{error}</p>
						<Button onclick={handleRefresh} variant="secondary" size="sm" class="mt-2 font-bold">
							<RefreshCw size={12} class="mr-1" />
							{m.transit_refresh()}
						</Button>
					</div>
				{:else if upcomingDepartures.length === 0}
					<div class="py-12 text-center text-xs text-[var(--color-nav-inactive)]">
						{m.transit_no_departures()}
					</div>
				{:else}
					<div class="space-y-4">
						<div
							class="flex items-center justify-between border-b border-[var(--color-surface-border)]/60 pb-3"
						>
							<span
								class="text-xs font-bold text-[var(--color-nav-inactive)] inline-flex items-center gap-1"
							>
								<Clock size={12} />
								発車予定時刻表
							</span>
							{#if transitStore.lastUpdated}
								<span class="text-[9px] text-[var(--color-nav-inactive)]">
									最終更新: {new Date(transitStore.lastUpdated).toLocaleTimeString(
										settings.locale || 'ja',
										{ hour: '2-digit', minute: '2-digit', second: '2-digit' }
									)}
								</span>
							{/if}
						</div>

						<div class="divide-y divide-[var(--color-surface-border)]/40">
							{#each upcomingDepartures as dep (dep.tripId + dep.departureSecs)}
								{@const remaining = dep.departureSecs - currentElapsedSecs}
								<div class="flex items-center justify-between py-3.5 gap-4">
									<div class="flex items-center gap-3 min-w-0 flex-1">
										<!-- 路線色バッジ -->
										<span
											class="h-3 w-3 rounded-full shrink-0 border border-black/10 shadow-sm"
											style="background-color: #{dep.color || '666666'}"
										></span>

										<div class="min-w-0">
											<div class="flex items-baseline gap-2 flex-wrap">
												<span class="text-sm font-bold text-[var(--color-nav-active)] truncate">
													{dep.headsign || '---'}
												</span>
												{#if dep.trainType}
													<span
														class="text-[10px] font-bold px-1.5 py-0.5 rounded bg-[var(--color-surface-muted)] text-[var(--color-nav-inactive)]"
													>
														{dep.trainType}
													</span>
												{/if}
											</div>
											<span
												class="text-xs font-semibold text-[var(--color-nav-inactive)] mt-0.5 block truncate"
											>
												{dep.routeName}
											</span>
										</div>
									</div>

									<div class="text-right shrink-0 flex items-center gap-4">
										<!-- 定刻表示 -->
										<span class="text-sm font-mono font-bold text-[var(--color-nav-active)]">
											{formatDepartureTime(dep.departureSecs)}
										</span>

										<!-- カウントダウン表示 -->
										<div class="w-24 text-right">
											{#if remaining <= 0}
												<span
													class="text-xs font-bold text-[var(--color-danger-500)] animate-pulse"
												>
													{m.transit_departs_just_now()}
												</span>
											{:else}
												{@const mins = Math.floor(remaining / 60)}
												{@const secs = remaining % 60}
												<span
													class="text-sm font-mono font-bold tracking-tight {remaining < 180
														? 'text-[var(--color-danger-500)] font-extrabold'
														: 'text-[var(--color-nav-active)]'}"
												>
													{#if mins > 0}
														{mins}<span
															class="text-[10px] font-sans font-medium text-[var(--color-nav-inactive)] ml-0.5 mr-0.5"
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
									</div>
								</div>
							{/each}
						</div>
					</div>
				{/if}
			</BentoCard>
		{/if}
	</div>
</Container>
