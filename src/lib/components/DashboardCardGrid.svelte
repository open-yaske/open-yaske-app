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
	import InAppBrowser from './inapp-browser/InAppBrowser.svelte';
	import { LayoutGrid, ExternalLink, ChevronRight } from '@lucide/svelte';

	// --- Card HTML content cache (for html-internal) ---
	const htmlCache = $state<Record<string, string>>({});
	const htmlLoading = $state<Record<string, boolean>>({});

	// --- InAppBrowser state ---
	let browserOpen = $state(false);
	let browserUrl = $state('');
	let browserTitle = $state('');

	// --- Sheet (webview-popup) state ---
	let sheetOpen = $state(false);
	let sheetTitle = $state('');
	let sheetHtml = $state('');

	const settings = $derived(settingsStore.settings);
	const cards = $derived(publicDataStore.data?.dashboardCards ?? []);

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
		if (card.openInApp && settings.openLinksInApp) {
			browserUrl = card.url;
			browserTitle = card.title;
			browserOpen = true;
		} else {
			window.open(card.url, '_blank', 'noopener,noreferrer');
		}
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
				{@const c = card as HtmlInternalCard}
				{@const isBanner = c.layout === 'banner'}
				<div
					class="flex flex-col overflow-hidden rounded-[var(--radius-card)] border border-[var(--color-surface-border)] bg-[var(--color-surface-card)] shadow-[var(--shadow-card)]"
					class:col-span-2={isBanner}
				>
					<div class="flex items-center gap-2 px-3 pt-3">
						<span class="text-lg">{c.icon}</span>
						<span class="text-sm font-semibold text-[var(--color-nav-active)]">{c.title}</span>
					</div>
					<div class="px-3 pb-3 pt-2">
						{#if htmlLoading[c.id]}
							<div class="h-16 animate-pulse rounded bg-[var(--color-surface-muted)]"></div>
						{:else if htmlCache[c.id]}
							<div class="prose prose-sm max-w-none text-[var(--color-nav-active)]">
								<!-- htmlCache entries are sanitized via DOMPurify in lib/html-renderer.ts -->
								<!-- eslint-disable-next-line svelte/no-at-html-tags -->
								{@html htmlCache[c.id]}
							</div>
						{:else}
							<div
								role="button"
								tabindex="0"
								onclick={() => fetchHtml(c)}
								onkeydown={(e) => e.key === 'Enter' && fetchHtml(c)}
								class="flex items-center justify-center gap-1 py-4 text-xs text-[var(--color-nav-inactive)]"
							>
								{m.action_refresh()}
							</div>
						{/if}
					</div>
				</div>
			{:else if card.type === 'external-url'}
				{@const c = card as ExternalUrlCard}
				<button
					type="button"
					onclick={() => openExternalUrl(c)}
					class="flex flex-col items-start gap-2 rounded-[var(--radius-card)] border border-[var(--color-surface-border)] bg-[var(--color-surface-card)] px-3 py-3 text-left shadow-[var(--shadow-card)] transition-colors hover:bg-[var(--color-surface-muted)]"
				>
					<div class="flex w-full items-center justify-between">
						<span class="text-lg">{c.icon}</span>
						<ExternalLink size={14} color="var(--color-nav-inactive)" />
					</div>
					<span class="text-sm font-semibold text-[var(--color-nav-active)]">{c.title}</span>
				</button>
			{:else if card.type === 'webview-popup'}
				{@const c = card as WebviewPopupCard}
				<button
					type="button"
					onclick={() => openWebviewPopup(c)}
					class="flex flex-col items-start gap-2 rounded-[var(--radius-card)] border border-[var(--color-surface-border)] bg-[var(--color-surface-card)] px-3 py-3 text-left shadow-[var(--shadow-card)] transition-colors hover:bg-[var(--color-surface-muted)]"
				>
					<div class="flex w-full items-center justify-between">
						<span class="text-lg">{c.icon}</span>
						<ChevronRight size={14} color="var(--color-nav-inactive)" />
					</div>
					<span class="text-sm font-semibold text-[var(--color-nav-active)]">{c.title}</span>
				</button>
			{/if}
		{/each}
	</div>
{/if}

<!-- InAppBrowser for external-url cards -->
<InAppBrowser
	bind:open={browserOpen}
	url={browserUrl}
	onclose={() => (browserOpen = false)}
	title={browserTitle}
/>

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
