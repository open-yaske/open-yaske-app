<script lang="ts">
	import { resolve } from '$app/paths';
	import PageHeader from '$lib/components/PageHeader.svelte';
	import Container from '$lib/components/Container.svelte';
	import SectionHeader from '$lib/components/SectionHeader.svelte';
	import BentoCard from '$lib/components/BentoCard.svelte';
	import SpotlightSearch from '$lib/components/SpotlightSearch.svelte';
	import { m } from '$lib/paraglide/messages';
	import { settingsStore, userDataStore } from '$lib/stores';
	import { APP_FEATURES, getFeatureName } from '$lib/features';
	import { ChevronRight, BookOpen, CheckSquare, Calendar } from '@lucide/svelte';

	let searchQuery = $state('');

	const settings = $derived(settingsStore.settings);

	const unpinnedFeatures = $derived(
		APP_FEATURES.filter((f) => f.id !== 'dashboard' && !(settings.barFeatures || []).includes(f.id))
	);

	type SearchResult = {
		id: string;
		type: 'feature' | 'course' | 'todo' | 'event';
		title: string;
		subtitle?: string;
		icon: any;
		href: string;
	};

	const searchResults = $derived.by<SearchResult[]>(() => {
		const q = searchQuery.trim().toLowerCase();
		if (!q) return [];

		const list: SearchResult[] = [];

		for (const f of APP_FEATURES) {
			const name = getFeatureName(f.id);
			if (name.toLowerCase().includes(q)) {
				list.push({
					id: `feature-${f.id}`,
					type: 'feature',
					title: name,
					subtitle: m.nav_more(),
					icon: f.icon,
					href: f.href
				});
			}
		}

		for (const c of userDataStore.courses) {
			if (
				c.name.toLowerCase().includes(q) ||
				(c.classroom && c.classroom.toLowerCase().includes(q)) ||
				(c.teacher && c.teacher.toLowerCase().includes(q))
			) {
				list.push({
					id: `course-${c.id}`,
					type: 'course',
					title: c.name,
					subtitle: `${c.classroom || ''} ${c.teacher || ''} (${c.period}限)`,
					icon: BookOpen,
					href: '/timetable'
				});
			}
		}

		for (const t of userDataStore.todos) {
			if (t.name.toLowerCase().includes(q) || (t.memo && t.memo.toLowerCase().includes(q))) {
				list.push({
					id: `todo-${t.id}`,
					type: 'todo',
					title: t.name,
					subtitle: t.done ? 'TODO (完了済)' : 'TODO (未完了)',
					icon: CheckSquare,
					href: '/todo'
				});
			}
		}

		for (const e of userDataStore.events) {
			if (
				e.title.toLowerCase().includes(q) ||
				(e.location && e.location.toLowerCase().includes(q)) ||
				(e.memo && e.memo.toLowerCase().includes(q))
			) {
				list.push({
					id: `event-${e.id}`,
					type: 'event',
					title: e.title,
					subtitle: e.location || 'カレンダー予定',
					icon: Calendar,
					href: '/calendar'
				});
			}
		}

		return list.slice(0, 40);
	});
</script>

<svelte:head>
	<title>{m.nav_more()} | OpenYASKE</title>
</svelte:head>

<PageHeader title={m.nav_more()} />

<Container class="pb-16 max-w-5xl">
	<div class="space-y-6 py-4">
		<!-- macOS / iOS Spotlight 風横断検索バー -->
		<SpotlightSearch bind:query={searchQuery} placeholder={m.more_search_placeholder()} />

		{#if searchQuery.trim() !== ''}
			<!-- 検索結果エリア -->
			<div class="space-y-3">
				<SectionHeader>検索結果 ({searchResults.length})</SectionHeader>
				{#if searchResults.length === 0}
					<div
						class="py-12 text-center text-xs text-[var(--color-nav-inactive)] border border-dashed border-[var(--color-surface-border)] rounded-card bg-[var(--color-surface-card)]/50"
					>
						{m.more_search_no_results()}
					</div>
				{:else}
					<div class="grid grid-cols-1 sm:grid-cols-2 gap-2.5">
						{#each searchResults as item (item.id)}
							<BentoCard
								href={resolve(item.href as any)}
								padding="p-3.5"
								class="flex-row items-center justify-between"
							>
								<div class="flex items-center gap-3 min-w-0 flex-1">
									<div
										class="flex h-9 w-9 shrink-0 items-center justify-center rounded-xl bg-[var(--color-primary-500)]/15 text-[var(--color-primary-600)]"
									>
										<item.icon size={18} />
									</div>
									<div class="flex-1 min-w-0">
										<div class="font-semibold text-xs text-[var(--color-nav-active)] truncate">
											{item.title}
										</div>
										{#if item.subtitle}
											<div class="text-[10px] text-[var(--color-nav-inactive)] truncate mt-0.5">
												{item.subtitle}
											</div>
										{/if}
									</div>
								</div>
								<ChevronRight size={14} class="text-[var(--color-nav-inactive)] shrink-0 ml-2" />
							</BentoCard>
						{/each}
					</div>
				{/if}
			</div>
		{:else}
			<!-- クイックアクセス (バーに配置されていない機能) -->
			{#if unpinnedFeatures.length > 0}
				<div class="space-y-3">
					<SectionHeader>{m.more_quick_features()}</SectionHeader>
					<div class="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-3">
						{#each unpinnedFeatures as f (f.id)}
							<BentoCard
								href={resolve(f.href as any)}
								padding="p-4"
								class="flex-row items-center justify-between"
							>
								<div class="flex items-center gap-3 min-w-0">
									<div
										class="flex h-9 w-9 shrink-0 items-center justify-center rounded-xl bg-[var(--color-primary-500)]/15 text-[var(--color-primary-600)]"
									>
										<f.icon size={18} />
									</div>
									<h4 class="text-xs font-semibold text-[var(--color-nav-active)] truncate">
										{getFeatureName(f.id)}
									</h4>
								</div>
								<ChevronRight size={16} class="text-[var(--color-nav-inactive)] shrink-0" />
							</BentoCard>
						{/each}
					</div>
				</div>
			{/if}

			<!-- すべての機能一覧 (Bento Grid) -->
			<div class="space-y-3">
				<SectionHeader>{m.more_all_features()}</SectionHeader>
				<div class="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-3">
					{#each APP_FEATURES as f (f.id)}
						<BentoCard
							href={resolve(f.href as any)}
							padding="p-3.5"
							class="flex-row items-center justify-between"
						>
							<div class="flex items-center gap-3 min-w-0">
								<div
									class="flex h-9 w-9 shrink-0 items-center justify-center rounded-xl bg-[var(--color-primary-500)]/15 text-[var(--color-primary-600)]"
								>
									<f.icon size={18} />
								</div>
								<span class="text-xs font-semibold text-[var(--color-nav-active)] truncate">
									{getFeatureName(f.id)}
								</span>
							</div>
							<ChevronRight size={16} class="text-[var(--color-nav-inactive)] shrink-0" />
						</BentoCard>
					{/each}
				</div>
			</div>

			<!-- アプリについて（ライセンス） -->
			<div class="pt-2">
				<BentoCard
					href={resolve('/more/about')}
					padding="p-3.5"
					class="flex-row items-center justify-between"
				>
					<div class="flex items-center gap-3 min-w-0">
						<div
							class="flex h-9 w-9 shrink-0 items-center justify-center rounded-xl bg-[var(--color-primary-500)]/15 text-[var(--color-primary-600)]"
						>
							<BookOpen size={18} />
						</div>
						<span class="text-xs font-semibold text-[var(--color-nav-active)] truncate">
							{m.more_about_app()}
						</span>
					</div>
					<ChevronRight size={16} class="text-[var(--color-nav-inactive)] shrink-0" />
				</BentoCard>
			</div>
		{/if}
	</div>
</Container>
