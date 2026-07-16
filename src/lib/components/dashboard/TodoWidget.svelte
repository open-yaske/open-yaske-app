<script lang="ts">
	import { userDataStore } from '$lib/stores';
	import { m } from '$lib/paraglide/messages';
	import type { Todo } from '$lib/types';
	import { CheckSquare, Square, Calendar, ClipboardList } from '@lucide/svelte';

	// 未完了のTODOを取得し、期限が早い順にソート (期限なしは後回し)
	const activeTodos = $derived(
		userDataStore.todos
			.filter((t) => !t.done)
			.sort((a, b) => {
				if (a.dueAt && b.dueAt) return a.dueAt - b.dueAt;
				if (a.dueAt) return -1;
				if (b.dueAt) return 1;
				return b.createdAt - a.createdAt; // 新しい順
			})
			.slice(0, 3)
	);

	function toggleTodo(todo: Todo) {
		void userDataStore.updateTodo(todo.id, { done: !todo.done });
	}

	function formatDate(timestamp: number | undefined): string {
		if (!timestamp) return '';
		const d = new Date(timestamp);
		return `${d.getMonth() + 1}/${d.getDate()}`;
	}

	function isOverdue(timestamp: number | undefined): boolean {
		if (!timestamp) return false;
		// 今日の始まりより前であれば期限切れ
		const today = new Date();
		today.setHours(0, 0, 0, 0);
		return timestamp < today.getTime();
	}
</script>

<div
	class="rounded-card border border-[var(--color-surface-border)] bg-[var(--color-surface-card)] p-4"
>
	<div
		class="mb-4 flex items-center justify-between border-b border-[var(--color-surface-border)] pb-2"
	>
		<div class="flex items-center gap-2 font-bold text-[var(--color-nav-active)]">
			<ClipboardList size={18} class="text-[var(--color-primary-500)]" />
			<span>{m.nav_todo()}</span>
		</div>
		<a href="/todo" class="text-xs text-[var(--color-primary-600)] hover:underline">
			{m.action_more()}
		</a>
	</div>

	{#if activeTodos.length === 0}
		<div class="py-6 text-center text-xs text-[var(--color-nav-inactive)]">
			{m.todo_empty()}
		</div>
	{:else}
		<ul class="space-y-3">
			{#each activeTodos as todo (todo.id)}
				<li class="flex items-start justify-between gap-3 group">
					<button
						type="button"
						onclick={() => toggleTodo(todo)}
						class="flex items-start gap-2.5 text-left text-sm text-[var(--color-nav-active)] flex-1 min-w-0"
					>
						<span
							class="mt-0.5 text-[var(--color-nav-inactive)] group-hover:text-[var(--color-primary-500)] shrink-0 transition-colors"
						>
							{#if todo.done}
								<CheckSquare size={16} class="text-[var(--color-success-500)]" />
							{:else}
								<Square size={16} />
							{/if}
						</span>
						<div class="flex-1 min-w-0">
							<span class="font-medium truncate block">{todo.name}</span>
							{#if todo.memo}
								<p class="text-[11px] text-[var(--color-nav-inactive)] line-clamp-1 mt-0.5">
									{todo.memo}
								</p>
							{/if}
						</div>
					</button>

					{#if todo.dueAt}
						{@const overdue = isOverdue(todo.dueAt)}
						<span
							class="flex items-center gap-1 text-xs shrink-0 font-medium px-2 py-0.5 rounded-chip {overdue
								? 'bg-[var(--color-danger-50)] text-[var(--color-danger-600)] font-bold'
								: 'bg-[var(--color-surface-muted)] text-[var(--color-nav-inactive)]'}"
						>
							<Calendar size={12} />
							{formatDate(todo.dueAt)}
						</span>
					{/if}
				</li>
			{/each}
		</ul>
	{/if}
</div>
