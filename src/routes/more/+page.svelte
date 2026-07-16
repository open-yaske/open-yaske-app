<script lang="ts">
	import { resolve } from '$app/paths';
	import PageHeader from '$lib/components/PageHeader.svelte';
	import Container from '$lib/components/Container.svelte';
	import TextField from '$lib/components/TextField.svelte';
	import SectionHeader from '$lib/components/SectionHeader.svelte';
	import { m } from '$lib/paraglide/messages';
	import { settingsStore, userDataStore } from '$lib/stores';
	import { schoolDataService } from '$lib/schoolData.svelte';
	import { APP_FEATURES, getFeatureName } from '$lib/features';
	import {
		Search,
		ChevronRight,
		BookOpen,
		CheckSquare,
		Calendar,
		MapPin,
		Building2,
		GraduationCap
	} from '@lucide/svelte';
	import type { Component } from 'svelte';

	let searchQuery = $state('');

	const settings = $derived(settingsStore.settings);

	// ナビゲーションバーに固定されていない機能IDリスト
	const unpinnedFeatures = $derived(
		APP_FEATURES.filter((f) => f.id !== 'dashboard' && !(settings.barFeatures || []).includes(f.id))
	);

	// 横断検索結果のアイテム定義
	type SearchResult = {
		id: string;
		type: 'feature' | 'course' | 'todo' | 'event' | 'spatial' | 'org';
		title: string;
		subtitle?: string;
		icon: any;
		href: string;
	};

	const searchResults = $derived.by<SearchResult[]>(() => {
		const q = searchQuery.trim().toLowerCase();
		if (!q) return [];

		const list: SearchResult[] = [];

		// 1) 機能検索
		for (const f of APP_FEATURES) {
			const name = getFeatureName(f.id);
			if (name.toLowerCase().includes(q)) {
				list.push({
					id: `feature-${f.id}`,
					type: 'feature',
					title: name,
					subtitle: 'アプリ機能',
					icon: f.icon,
					href: f.href
				});
			}
		}

		// 2) 履修授業検索
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

		// 3) TODO検索
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

		// 4) カレンダーイベント検索
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

		return list.slice(0, 40); // 最大40件表示
	});
</script>

<svelte:head>
	<title>{m.nav_more()} | OpenYASKE</title>
</svelte:head>

<PageHeader title={m.nav_more()} />

<Container class="pb-10">
	<div class="space-y-6 py-4">
		<!-- 横断検索バー -->
		<TextField
			bind:value={searchQuery}
			placeholder={m.more_search_placeholder()}
			type="text"
			leadingIcon={Search}
		/>

		{#if searchQuery.trim() !== ''}
			<!-- 検索結果エリア -->
			<div class="space-y-3">
				<SectionHeader>検索結果 ({searchResults.length})</SectionHeader>
				{#if searchResults.length === 0}
					<div
						class="py-8 text-center text-xs text-[var(--color-nav-inactive)] border border-dashed border-[var(--color-surface-border)] rounded-card bg-[var(--color-surface-card)]"
					>
						{m.more_search_no_results()}
					</div>
				{:else}
					<div
						class="divide-y divide-[var(--color-surface-border)] rounded-card border border-[var(--color-surface-border)] bg-[var(--color-surface-card)] overflow-hidden"
					>
						{#each searchResults as item (item.id)}
							<a
								href={resolve(item.href as any)}
								class="flex items-center gap-3 p-3 transition-colors hover:bg-[var(--color-surface-muted)]"
							>
								<span
									class="flex h-8 w-8 shrink-0 items-center justify-center rounded-full bg-[var(--color-primary-50)] text-[var(--color-primary-600)]"
								>
									<item.icon size={16} />
								</span>
								<div class="flex-1 min-w-0">
									<div class="font-bold text-xs text-[var(--color-nav-active)] truncate">
										{item.title}
									</div>
									{#if item.subtitle}
										<div class="text-[10px] text-[var(--color-nav-inactive)] truncate mt-0.5">
											{item.subtitle}
										</div>
									{/if}
								</div>
								<ChevronRight size={14} class="text-[var(--color-nav-inactive)] shrink-0" />
							</a>
						{/each}
					</div>
				{/if}
			</div>
		{:else}
			<!-- クイックアクセス (バーに配置されていない機能) -->
			{#if unpinnedFeatures.length > 0}
				<div class="space-y-3">
					<SectionHeader>{m.more_quick_features()}</SectionHeader>
					<div class="grid grid-cols-1 md:grid-cols-2 gap-3">
						{#each unpinnedFeatures as f (f.id)}
							<a
								href={resolve(f.href as any)}
								class="flex items-center gap-3 rounded-card border border-[var(--color-surface-border)] bg-[var(--color-surface-card)] p-3.5 transition-colors hover:bg-[var(--color-surface-muted)] text-left"
							>
								<span
									class="flex h-9 w-9 shrink-0 items-center justify-center rounded-full bg-[var(--color-primary-50)] text-[var(--color-primary-700)]"
								>
									<f.icon size={18} />
								</span>
								<div class="flex-1 min-w-0">
									<h4 class="text-xs font-bold text-[var(--color-nav-active)]">
										{getFeatureName(f.id)}
									</h4>
								</div>
								<ChevronRight size={14} class="text-[var(--color-nav-inactive)] shrink-0" />
							</a>
						{/each}
					</div>
				</div>
			{/if}

			<!-- すべての機能一覧 -->
			<div class="space-y-3">
				<SectionHeader>{m.more_all_features()}</SectionHeader>
				<div
					class="divide-y divide-[var(--color-surface-border)] rounded-card border border-[var(--color-surface-border)] bg-[var(--color-surface-card)] overflow-hidden"
				>
					{#each APP_FEATURES as f (f.id)}
						<a
							href={resolve(f.href as any)}
							class="flex items-center justify-between p-3.5 hover:bg-[var(--color-surface-muted)] transition-colors"
						>
							<div class="flex items-center gap-3 min-w-0">
								<span class="text-[var(--color-primary-600)] shrink-0">
									<f.icon size={18} />
								</span>
								<span class="text-xs font-bold text-[var(--color-nav-active)] truncate">
									{getFeatureName(f.id)}
								</span>
							</div>
							<ChevronRight size={14} class="text-[var(--color-nav-inactive)]" />
						</a>
					{/each}
				</div>
			</div>

			<!-- アプリについて（ライセンス） -->
			<div class="pt-2">
				<a
					href={resolve('/more/about')}
					class="flex items-center justify-between p-3.5 rounded-card border border-[var(--color-surface-border)] bg-[var(--color-surface-card)] hover:bg-[var(--color-surface-muted)] transition-colors"
				>
					<div class="flex items-center gap-3 min-w-0">
						<span class="text-[var(--color-primary-600)] shrink-0">
							<BookOpen size={18} />
						</span>
						<span class="text-xs font-bold text-[var(--color-nav-active)] truncate">
							{m.more_about_app()}
						</span>
					</div>
					<ChevronRight size={14} class="text-[var(--color-nav-inactive)]" />
				</a>
			</div>
		{/if}
	</div>
</Container>
