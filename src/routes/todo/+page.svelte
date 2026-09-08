<!--
  todo/+page.svelte
  TODO 画面: PageHeader + フィルタチップ + ソート + TodoList + FAB + TodoEditSheet
-->
<script lang="ts">
	import type { Todo } from '$lib/types';
	import { m } from '$lib/paraglide/messages';
	import PageHeader from '$lib/components/PageHeader.svelte';
	import Container from '$lib/components/Container.svelte';
	import TextField from '$lib/components/TextField.svelte';
	import TodoList from '$lib/components/todo/TodoList.svelte';
	import TodoEditSheet from '$lib/components/todo/TodoEditSheet.svelte';
	import IconButton from '$lib/components/IconButton.svelte';
	import { Plus, ArrowDownUp, Search, Sun, Moon, Laptop } from '@lucide/svelte';
	import { settingsStore, styleStore } from '$lib/stores';
	import { resolveComponents } from '$lib/styles';

	type FilterType = 'all' | 'active' | 'completed';
	type SortType = 'due' | 'priority' | 'created';

	// ----- 状態 -----
	let filter = $state<FilterType>('all');
	let sort = $state<SortType>('due');
	let sheetOpen = $state(false);
	let searchOpen = $state(false);
	let searchQuery = $state('');
	let editingTodo = $state<Todo | null>(null);

	// ----- フィルタチップ -----
	const filterChips = $derived([
		{ key: 'all' as FilterType, label: m.todo_filter_all() },
		{ key: 'active' as FilterType, label: m.todo_undone() },
		{ key: 'completed' as FilterType, label: m.todo_done() }
	]);

	const c = $derived(resolveComponents(styleStore.pack));

	// ----- ソートオプション -----
	const sortOptions = $derived([
		{ value: 'due' as SortType, label: m.todo_sort_due() },
		{ value: 'priority' as SortType, label: m.todo_sort_priority() },
		{ value: 'created' as SortType, label: m.todo_sort_created() }
	]);

	// ----- アクション -----
	function openAddSheet() {
		editingTodo = null;
		sheetOpen = true;
	}

	function openEditSheet(todo: Todo) {
		editingTodo = todo;
		sheetOpen = true;
	}

	function closeSheet() {
		sheetOpen = false;
		editingTodo = null;
	}

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

<svelte:head><title>{m.nav_todo()} | {m.app_name()}</title></svelte:head>

{#snippet trailingSnippet()}
	<div class="flex items-center gap-1">
		<IconButton
			icon={Search}
			ariaLabel="検索"
			onclick={() => (searchOpen = !searchOpen)}
			variant="subtle"
			size="sm"
		/>
		<IconButton
			icon={Plus}
			ariaLabel={m.todo_add()}
			onclick={openAddSheet}
			variant="primary"
			size="sm"
		/>
	</div>
{/snippet}

<PageHeader title="ToDo" trailing={trailingSnippet} />

<Container class="pb-24">
	{#if searchOpen}
		<div class="pt-3 pb-1">
			<TextField bind:value={searchQuery} label="検索" placeholder="TODO・メモ・場所・科目" />
		</div>
	{/if}

	<!-- フィルタピル + ソート -->
	<div class="flex flex-col sm:flex-row items-stretch sm:items-center justify-between gap-3 py-3">
		<!-- 流体セグメントピル -->
		<div
			class="flex items-center p-1 bg-[var(--color-surface-muted)]/90 backdrop-blur-md rounded-chip border border-[var(--color-surface-border)]/40 shadow-[var(--shadow-style-sm)]"
		>
			{#each filterChips as chip (chip.key)}
				<button
					type="button"
					class="{c.chip.base} {filter === chip.key ? c.chip.active : c.chip.inactive}"
					onclick={() => (filter = chip.key)}
				>
					{chip.label}
				</button>
			{/each}
		</div>

		<!-- ソートピル -->
		<div class="flex items-center justify-end">
			<div
				class="inline-flex items-center gap-2 px-3 py-1.5 min-h-[40px] rounded-chip bg-[var(--color-surface-card)] border border-[var(--color-surface-border)]/60 shadow-[var(--shadow-style-sm)] text-xs font-semibold text-[var(--color-nav-active)]"
			>
				<ArrowDownUp size={14} class="text-[var(--color-primary-500)]" />
				<span class="text-[var(--color-nav-inactive)]">{m.todo_sort()}:</span>
				<select
					bind:value={sort}
					class="bg-transparent text-xs font-bold text-[var(--color-nav-active)] focus:outline-none cursor-pointer border-none pr-1 py-0.5"
					aria-label={m.todo_sort()}
				>
					{#each sortOptions as opt (opt.value)}
						<option
							value={opt.value}
							class="bg-[var(--color-surface-card)] text-[var(--color-nav-active)]"
							>{opt.label}</option
						>
					{/each}
				</select>
			</div>
		</div>
	</div>

	<!-- TODO リスト -->
	<TodoList {filter} {sort} {searchQuery} onedit={openEditSheet} />
</Container>

<!-- FAB (Floating Action Button) -->
<button type="button" class={c.fab.base} aria-label={m.todo_add()} onclick={openAddSheet}>
	<Plus size={26} stroke-width={2.5} />
</button>

<!-- 編集シート -->
<TodoEditSheet bind:open={sheetOpen} onclose={closeSheet} todo={editingTodo} />

<style>
	.scrollbar-hide {
		-ms-overflow-style: none;
		scrollbar-width: none;
	}
	.scrollbar-hide::-webkit-scrollbar {
		display: none;
	}
</style>
