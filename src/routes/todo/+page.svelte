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
	import { settingsStore } from '$lib/stores';

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

{#snippet leadingSnippet()}
	<IconButton
		icon={themeIcon}
		ariaLabel={themeAriaLabel}
		onclick={toggleTheme}
		variant="subtle"
		size="sm"
	/>
{/snippet}

{#snippet trailingSnippet()}
	<IconButton
		icon={Search}
		ariaLabel="検索"
		onclick={() => (searchOpen = !searchOpen)}
		variant="subtle"
		size="sm"
	/>
{/snippet}

<PageHeader title="ToDo" leading={leadingSnippet} trailing={trailingSnippet} />

<Container>
	{#if searchOpen}
		<div class="pt-3">
			<TextField bind:value={searchQuery} label="検索" placeholder="TODO・メモ・場所・科目" />
		</div>
	{/if}

	<!-- フィルタチップ + ソート -->
	<div class="flex items-center gap-2 py-3">
		<!-- フィルタチップ（横スクロール） -->
		<div class="flex flex-1 gap-2 overflow-x-auto scrollbar-hide">
			{#each filterChips as chip (chip.key)}
				<button
					type="button"
					class="shrink-0 rounded-full px-4 py-1.5 text-sm font-medium transition-colors focus:outline-none focus-visible:ring-2 focus-visible:ring-[var(--color-primary-500)]"
					class:bg-[var(--color-primary-500)]={filter === chip.key}
					class:text-white={filter === chip.key}
					class:bg-[var(--color-surface-muted)]={filter !== chip.key}
					class:text-[var(--color-nav-active)]={filter !== chip.key}
					onclick={() => (filter = chip.key)}
				>
					{chip.label}
				</button>
			{/each}
		</div>

		<!-- ソートセレクト -->
		<div class="flex shrink-0 items-center relative gap-1 pr-1">
			<ArrowDownUp size={14} color="var(--color-nav-inactive)" />
			<select
				bind:value={sort}
				class="appearance-none bg-transparent pr-4 text-sm text-[var(--color-nav-active)] focus:outline-none cursor-pointer border-none py-1"
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
			<span
				class="absolute right-0 pointer-events-none text-[8px] text-[var(--color-nav-inactive)]"
			>
				▼
			</span>
		</div>
	</div>

	<!-- TODO リスト -->
	<TodoList {filter} {sort} {searchQuery} onedit={openEditSheet} />
</Container>

<!-- FAB -->
<button
	type="button"
	class="fixed bottom-24 right-4 z-30 flex h-14 w-14 items-center justify-center rounded-full bg-[var(--color-primary-800)] text-white shadow-[var(--shadow-card)] transition-colors hover:bg-[var(--color-primary-900)] dark:bg-[var(--color-primary-500)] dark:text-slate-950 dark:hover:bg-[var(--color-primary-400)] focus:outline-none focus-visible:ring-2 focus-visible:ring-[var(--color-primary-800)] dark:focus-visible:ring-[var(--color-primary-500)] focus-visible:ring-offset-2 md:bottom-6"
	aria-label={m.todo_add()}
	onclick={openAddSheet}
>
	<Plus size={24} stroke-width={2.5} />
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
