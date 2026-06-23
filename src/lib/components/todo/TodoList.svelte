<!--
  TodoList.svelte
  フィルタリング + ソート済みの TODO リストを表示。
  フィルタ: all / active / completed
  ソート: due / priority / created
  期限切れアイテムは視覚的に強調。
-->
<script lang="ts">
	import type { Todo, Course } from '$lib/types';
	import { m } from '$lib/paraglide/messages';
	import { userDataStore } from '$lib/stores';
	import TodoItem from './TodoItem.svelte';
	import EmptyState from '$lib/components/EmptyState.svelte';
	import { CheckSquare } from '@lucide/svelte';

	type FilterType = 'all' | 'active' | 'completed';
	type SortType = 'due' | 'priority' | 'created';

	interface Props {
		filter: FilterType;
		sort: SortType;
		searchQuery?: string;
		onedit?: (todo: Todo) => void;
	}

	let { filter, sort, searchQuery = '', onedit }: Props = $props();

	// コース名前解決用マップ
	const courseMap = $derived(new Map<string, Course>(userDataStore.courses.map((c) => [c.id, c])));

	// フィルタリング
	const filteredTodos = $derived.by(() => {
		let list = userDataStore.todos;
		if (filter === 'active') {
			list = list.filter((t) => !t.done);
		} else if (filter === 'completed') {
			list = list.filter((t) => t.done);
		}
		const q = searchQuery.trim().toLowerCase();
		if (q) {
			list = list.filter((t) => {
				const course = courseMap.get(t.courseId ?? '');
				return [t.name, t.memo, t.location, t.assignee, course?.name]
					.filter(Boolean)
					.some((value) => value!.toLowerCase().includes(q));
			});
		}
		return list;
	});

	// ソート
	const sortedTodos = $derived.by(() => {
		const list = [...filteredTodos];
		const priorityWeight: Record<string, number> = { high: 0, mid: 1, low: 2 };

		if (sort === 'due') {
			// 期限なしは最後、期限切れは先頭
			list.sort((a, b) => {
				if (!a.dueAt && !b.dueAt) return b.createdAt - a.createdAt;
				if (!a.dueAt) return 1;
				if (!b.dueAt) return -1;
				return a.dueAt - b.dueAt;
			});
		} else if (sort === 'priority') {
			list.sort((a, b) => {
				const pa = priorityWeight[a.priority ?? 'low'] ?? 2;
				const pb = priorityWeight[b.priority ?? 'low'] ?? 2;
				if (pa !== pb) return pa - pb;
				return b.createdAt - a.createdAt;
			});
		} else {
			// created — 新しい順
			list.sort((a, b) => b.createdAt - a.createdAt);
		}

		return list;
	});

	const isEmpty = $derived(sortedTodos.length === 0);

	// ----- アクション -----
	function handleToggle(id: string) {
		const todo = userDataStore.todos.find((t) => t.id === id);
		if (todo) {
			void userDataStore.updateTodo(id, { done: !todo.done });
		}
	}

	function handleDelete(id: string) {
		void userDataStore.removeTodo(id);
	}

	function handleEdit(todo: Todo) {
		onedit?.(todo);
	}
</script>

{#if isEmpty}
	<EmptyState
		icon={CheckSquare}
		message={m.todo_empty()}
		submessage={m.todo_add()}
		iconSize={56}
		class="py-20"
	/>
{:else}
	<div
		class="overflow-hidden rounded-xl border border-[var(--color-surface-border)] bg-[var(--color-surface-card)]"
	>
		{#each sortedTodos as todo (todo.id)}
			<TodoItem
				{todo}
				course={courseMap.get(todo.courseId ?? '')}
				ontoggle={handleToggle}
				onedit={handleEdit}
				ondelete={handleDelete}
			/>
		{/each}
	</div>
{/if}
