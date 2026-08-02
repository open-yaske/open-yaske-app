<!--
  DashboardCardGrid.svelte
  公開データの DashboardCard をグリッド表示。
  3 バリアント: html-internal / external-url / webview-popup
  - html-internal: HTML をフェッチしてサニタイズしてインライン表示
  - external-url: InAppBrowser または外部ブラウザで開く
  - webview-popup: Sheet にサニタイズした HTML を表示
-->
<script lang="ts">
	import { SvelteSet, SvelteMap } from 'svelte/reactivity';
	import { m } from '$lib/paraglide/messages';
	import { publicDataStore, settingsStore } from '$lib/stores';
	import { sanitize } from '$lib/html-renderer';
	import type {
		DashboardCard,
		HtmlInternalCard,
		ExternalUrlCard,
		WebviewPopupCard
	} from '$lib/types/public';
	import EmptyState from './EmptyState.svelte';
	import Sheet from './Sheet.svelte';
	import GlassContainer from './GlassContainer.svelte';
	import { openLink } from '$lib/browser';
	import { LayoutGrid, ExternalLink, ChevronRight } from '@lucide/svelte';
	import { styleStore } from '$lib/stores';
	import { resolveComponents } from '$lib/styles';

	// --- Card HTML content cache (for html-internal) ---
	const htmlCache = $state<Record<string, string>>({});
	const htmlLoading = $state<Record<string, boolean>>({});

	// --- Sheet (webview-popup) state ---
	let sheetOpen = $state(false);
	let sheetTitle = $state('');
	let sheetHtml = $state('');

	const settings = $derived(settingsStore.settings);
	const cards = $derived(publicDataStore.data?.dashboardCards ?? []);
	const c = $derived(resolveComponents(styleStore.pack));

	// Sort cards: respect hiddenDashboardCards filter, then dashboardCardOrder, then card.order
	const sortedCards = $derived.by<DashboardCard[]>(() => {
		const hidden = new SvelteSet(settings.hiddenDashboardCards);
		const orderMap = new SvelteMap<string, number>();
		settings.dashboardCardOrder.forEach((id, i) => orderMap.set(id, i));

		return cards
			.filter((c) => !hidden.has(c.id))
			.sort((a, b) => {
				const oa = orderMap.get(a.id);
				const ob = orderMap.get(b.id);
				if (oa != null && ob != null) return oa - ob;
				if (oa != null) return -1;
				if (ob != null) return 1;
				return a.order - b.order;
			});
	});

	// Fetch HTML for html-internal cards
	async function fetchHtml(card: HtmlInternalCard) {
		if (htmlCache[card.id] !== undefined) return;
		htmlLoading[card.id] = true;
		try {
			const res = await fetch(card.htmlUrl);
			if (res.ok) {
				htmlCache[card.id] = sanitize(await res.text());
			}
		} catch {
			// silently fail — card shows title only
		} finally {
			htmlLoading[card.id] = false;
		}
	}

	function openExternalUrl(card: ExternalUrlCard) {
		void openLink(card.url, card.openInApp && settings.openLinksInApp);
	}

	async function openWebviewPopup(card: WebviewPopupCard) {
		sheetTitle = card.title;
		sheetOpen = true;
		sheetHtml = '';
		try {
			const res = await fetch(card.htmlUrl);
			if (res.ok) {
				sheetHtml = sanitize(await res.text());
			}
		} catch {
			// show empty sheet
		}
	}
</script>

{#if sortedCards.length === 0}
	<EmptyState icon={LayoutGrid} message={m.empty_no_data()} iconSize={40} class="py-8" />
{:else}
	<div class="grid grid-cols-2 gap-3">
		{#each sortedCards as card (card.id)}
			{#if card.type === 'html-internal'}
				{@const hc = card as HtmlInternalCard}
				{@const isBanner = hc.layout === 'banner'}
				<GlassContainer
					variant="card"
					class="{c.card.base} {isBanner ? 'col-span-2' : ''}"
				>
					<div class="flex flex-col overflow-hidden w-full">
						<div class="flex items-center gap-2 px-4 pt-4">
							<span class="text-lg">{hc.icon}</span>
							<span class="text-sm font-semibold text-[var(--color-nav-active)]">{hc.title}</span>
						</div>
						<div class="px-4 pb-4 pt-2">
							{#if htmlLoading[hc.id]}
								<div class="h-16 animate-pulse rounded-chip bg-[var(--color-surface-muted)]"></div>
							{:else if htmlCache[hc.id]}
								<div class="prose prose-sm max-w-none text-[var(--color-nav-active)]">
									<!-- htmlCache entries are sanitized via DOMPurify in lib/html-renderer.ts -->
									<!-- eslint-disable-next-line svelte/no-at-html-tags -->
									{@html htmlCache[hc.id]}
								</div>
							{:else}
								<div
									role="button"
									tabindex="0"
									onclick={() => fetchHtml(hc)}
									onkeydown={(e) => e.key === 'Enter' && fetchHtml(hc)}
									class="flex min-h-[44px] items-center justify-center gap-1 py-3 text-xs font-medium text-[var(--color-nav-inactive)] cursor-pointer hover:text-[var(--color-nav-active)]"
								>
									{m.action_refresh()}
								</div>
							{/if}
						</div>
					</div>
				</GlassContainer>
			{:else if card.type === 'external-url'}
				{@const ec = card as ExternalUrlCard}
				<GlassContainer
					variant="card"
					class={c.card.interactive}
				>
					<button
						type="button"
						onclick={() => openExternalUrl(ec)}
						class="flex flex-col items-start gap-2.5 p-4 text-left w-full h-full {styleStore.pack
							.press.card} select-none"
					>
						<div class="flex w-full items-center justify-between">
							<span class="text-lg">{ec.icon}</span>
							<ExternalLink size={15} color="var(--color-nav-inactive)" class="shrink-0" />
						</div>
						<span class="text-sm font-semibold text-[var(--color-nav-active)] line-clamp-1"
							>{ec.title}</span
						>
					</button>
				</GlassContainer>
			{:else if card.type === 'webview-popup'}
				{@const wc = card as WebviewPopupCard}
				<GlassContainer
					variant="card"
					class={c.card.interactive}
				>
					<button
						type="button"
						onclick={() => openWebviewPopup(wc)}
						class="flex flex-col items-start gap-2.5 p-4 text-left w-full h-full {styleStore.pack
							.press.card} select-none"
					>
						<div class="flex w-full items-center justify-between">
							<span class="text-lg">{wc.icon}</span>
							<ChevronRight size={16} color="var(--color-nav-inactive)" class="shrink-0" />
						</div>
						<span class="text-sm font-semibold text-[var(--color-nav-active)] line-clamp-1"
							>{wc.title}</span
						>
					</button>
				</GlassContainer>
			{/if}
		{/each}
	</div>
{/if}

<!-- Sheet for webview-popup cards -->
<Sheet bind:open={sheetOpen} onclose={() => (sheetOpen = false)} title={sheetTitle}>
	{#if sheetHtml}
		<div class="prose prose-sm max-w-none text-[var(--color-nav-active)]">
			<!-- sheetHtml is sanitized via DOMPurify in lib/html-renderer.ts -->
			<!-- eslint-disable-next-line svelte/no-at-html-tags -->
			{@html sheetHtml}
		</div>
	{:else}
		<div class="flex items-center justify-center py-8 text-sm text-[var(--color-nav-inactive)]">
			{m.inapp_browser_loading()}
		</div>
	{/if}
</Sheet>
