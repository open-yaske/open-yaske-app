<script lang="ts">
	import PageHeader from '$lib/components/PageHeader.svelte';
	import ListItem from '$lib/components/ListItem.svelte';
	import SectionHeader from '$lib/components/SectionHeader.svelte';
	import Container from '$lib/components/Container.svelte';
	import EmptyState from '$lib/components/EmptyState.svelte';
	import Button from '$lib/components/Button.svelte';
	import Modal from '$lib/components/Modal.svelte';
	import DashboardCardItem from '$lib/components/dashboard/DashboardCardItem.svelte';
	import IconButton from '$lib/components/IconButton.svelte';
	import { m } from '$lib/paraglide/messages';
	import { settingsStore, userDataStore, publicDataStore, uiStore } from '$lib/stores';
	import { getConnectedUserAddress } from '$lib/remotestorage';
	import type { DashboardCard } from '$lib/types/public';
	import { SvelteSet, SvelteMap } from 'svelte/reactivity';
	import {
		Palette,
		Languages,
		Database,
		Download,
		Upload,
		Trash2,
		Link2,
		ShieldCheck,
		Info,
		AlertTriangle,
		Cloud,
		LayoutGrid,
		Loader2,
		Sun,
		Moon,
		Laptop
	} from '@lucide/svelte';

	const appVersion = '0.0.1';
	let showClearConfirm = $state(false);

	function handleExportData() {
		const payload = {
			schemaVersion: 1,
			exportedAt: new Date().toISOString(),
			settings: settingsStore.settings,
			...userDataStore.exportUserData()
		};
		const blob = new Blob([JSON.stringify(payload, null, 2)], { type: 'application/json' });
		const url = URL.createObjectURL(blob);
		const a = document.createElement('a');
		a.href = url;
		a.download = 'open-nexus-data.json';
		a.click();
		URL.revokeObjectURL(url);
		uiStore.toast('データを書き出しました', 'success');
	}

	async function handleImportData(e: Event) {
		const input = e.currentTarget as HTMLInputElement;
		const file = input.files?.[0];
		if (!file) return;
		try {
			const parsed = JSON.parse(await file.text());
			if (parsed.settings && typeof parsed.settings === 'object') {
				settingsStore.update(parsed.settings);
			}
			await userDataStore.replaceUserData({
				courses: parsed.courses,
				todos: parsed.todos,
				events: parsed.events,
				attendance: parsed.attendance
			});
			uiStore.toast('データを読み込みました', 'success');
		} catch {
			uiStore.toast('データの読み込みに失敗しました', 'error');
		} finally {
			input.value = '';
		}
	}

	async function handleClearAllData() {
		settingsStore.reset();
		await userDataStore.clearUserData();
		showClearConfirm = false;
		uiStore.toast('データを削除しました', 'success');
	}

	// 公開データのカードを order 順 + 非表示フィルタ
	const visibleCards = $derived.by<DashboardCard[]>(() => {
		const hidden = new SvelteSet(settingsStore.settings.hiddenDashboardCards);
		const orderMap = new SvelteMap<string, number>();
		settingsStore.settings.dashboardCardOrder.forEach((id, i) => orderMap.set(id, i));

		return (publicDataStore.data?.dashboardCards ?? [])
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

	function toggleTheme() {
		const current = settingsStore.settings.theme;
		let next: 'light' | 'dark' | 'system';
		if (current === 'light') {
			next = 'dark';
		} else if (current === 'dark') {
			next = 'system';
		} else {
			next = 'light';
		}
		settingsStore.update({ theme: next });
	}

	const themeIcon = $derived.by(() => {
		const t = settingsStore.settings.theme;
		if (t === 'light') return Sun;
		if (t === 'dark') return Moon;
		return Laptop;
	});

	const themeAriaLabel = $derived.by(() => {
		const t = settingsStore.settings.theme;
		if (t === 'light') return 'テーマ: ライト';
		if (t === 'dark') return 'テーマ: ダーク';
		return 'テーマ: システム';
	});
</script>

<svelte:head><title>{m.category_other()} | open-nexus</title></svelte:head>

{#snippet leadingSnippet()}
	<IconButton
		icon={themeIcon}
		ariaLabel={themeAriaLabel}
		onclick={toggleTheme}
		variant="subtle"
		size="sm"
	/>
{/snippet}

<PageHeader title={m.category_other()} leading={leadingSnippet} />

<Container>
	<!-- 設定クイックリンク -->
	<SectionHeader>{m.settings_title()}</SectionHeader>
	<ListItem
		href="/settings"
		title={m.settings_title()}
		subtitle={m.settings_locale() + ' / ' + m.settings_theme()}
		icon={Languages}
		iconColor="var(--color-primary-500)"
	/>

	<!-- テーマエディタクイックリンク -->
	<SectionHeader>{m.settings_theme()}</SectionHeader>
	<ListItem
		href="/theme-editor"
		title={m.settings_theme_pack()}
		subtitle={m.theme_pick_color()}
		icon={Palette}
	/>

	<!-- RemoteStorage 接続状態 -->
	<SectionHeader>{m.settings_account()}</SectionHeader>
	{#if !userDataStore.rsConnected}
		<ListItem
			href="/settings#settings-account"
			title={m.settings_account_connect()}
			subtitle={m.settings_account_disconnected()}
			icon={Cloud}
			iconColor="var(--color-primary-500)"
		/>
	{:else}
		<ListItem
			href="/settings#settings-account"
			title={m.settings_account_connected()}
			subtitle={getConnectedUserAddress() || m.settings_account_connected()}
			icon={ShieldCheck}
			iconColor="var(--color-success-500)"
		/>
	{/if}

	<!-- ダッシュボードカードグリッド -->
	<SectionHeader>{m.settings_dashboard()}</SectionHeader>
	{#if publicDataStore.loading && !publicDataStore.data}
		<div
			class="flex items-center justify-center gap-2 py-8 text-sm text-[var(--color-nav-inactive)]"
		>
			<Loader2 size={20} class="animate-spin" />
			{m.inapp_browser_loading()}
		</div>
	{:else if publicDataStore.error && !publicDataStore.data}
		<EmptyState
			icon={AlertTriangle}
			message={m.inapp_browser_error()}
			actionLabel={m.inapp_browser_retry()}
			onAction={() => publicDataStore.load()}
			class="py-8"
		/>
	{:else if visibleCards.length === 0}
		<EmptyState icon={LayoutGrid} message={m.empty_no_data()} class="py-8" />
	{:else}
		<div class="grid grid-cols-1 gap-3 md:grid-cols-2">
			{#each visibleCards as card (card.id)}
				<DashboardCardItem {card} />
			{/each}
		</div>
	{/if}

	<!-- データ管理 -->
	<SectionHeader>{m.settings_data()}</SectionHeader>
	<div
		class="rounded-xl border border-[var(--color-surface-border)] bg-[var(--color-surface-card)] p-4"
	>
		<div class="mb-3 flex items-center gap-3">
			<span
				class="flex h-9 w-9 shrink-0 items-center justify-center rounded-lg"
				style="background-color: var(--color-primary-500)1A; color: var(--color-primary-500)"
			>
				<Database size={18} />
			</span>
			<div class="min-w-0">
				<div class="text-sm font-medium text-[var(--color-nav-active)]">{m.settings_data()}</div>
				<div class="text-xs text-[var(--color-nav-inactive)]">設定・科目・TODO・予定・出席</div>
			</div>
		</div>
		<div class="grid grid-cols-2 gap-2">
			<Button variant="secondary" size="sm" onclick={handleExportData}>
				<Download size={16} />
				{m.settings_data_export()}
			</Button>
			<label
				class="inline-flex h-8 cursor-pointer items-center justify-center gap-2 rounded-[var(--radius-chip)] border border-[var(--color-surface-border)] bg-[var(--color-surface-card)] px-3 text-sm font-medium text-[var(--color-nav-active)] transition-colors hover:bg-[var(--color-surface-muted)]"
			>
				<Upload size={16} />
				{m.settings_data_import()}
				<input type="file" accept="application/json" class="hidden" onchange={handleImportData} />
			</label>
		</div>
		<Button
			variant="danger"
			size="sm"
			class="mt-2 w-full"
			onclick={() => (showClearConfirm = true)}
		>
			<Trash2 size={16} />
			{m.settings_data_clear()}
		</Button>
	</div>

	<!-- アプリについて -->
	<SectionHeader>{m.settings_about()}</SectionHeader>
	<ListItem
		title={m.settings_about_version()}
		subtitle={appVersion}
		icon={Info}
		iconColor="var(--color-primary-500)"
	/>
	<ListItem
		href="https://github.com/"
		title={m.settings_about_source()}
		icon={Link2}
		iconColor="var(--color-primary-500)"
	/>
</Container>

<Modal
	open={showClearConfirm}
	onclose={() => (showClearConfirm = false)}
	title={m.settings_data_clear()}
>
	<div class="space-y-4">
		<p class="text-sm text-[var(--color-nav-active)]">
			設定・科目・TODO・予定・出席をこの端末から削除します。
		</p>
		<div class="flex gap-3">
			<Button
				variant="secondary"
				size="sm"
				class="flex-1"
				onclick={() => (showClearConfirm = false)}
			>
				{m.action_cancel()}
			</Button>
			<Button variant="danger" size="sm" class="flex-1" onclick={handleClearAllData}>
				{m.action_delete()}
			</Button>
		</div>
	</div>
</Modal>
