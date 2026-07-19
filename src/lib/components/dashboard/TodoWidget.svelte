<script lang="ts">
	import { userDataStore } from '$lib/stores';
	import { m } from '$lib/paraglide/messages';
	import type { Todo } from '$lib/types';
	import { CheckSquare, Square, Calendar, ClipboardList, ChevronRight } from '@lucide/svelte';
	import BentoCard from '../BentoCard.svelte';

	const activeTodos = $derived(
		userDataStore.todos
			.filter((t) => !t.done)
			.sort((a, b) => {
				if (a.dueAt && b.dueAt) return a.dueAt - b.dueAt;
				if (a.dueAt) return -1;
				if (b.dueAt) return 1;
				return b.createdAt - a.createdAt;
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
		const today = new Date();
		today.setHours(0, 0, 0, 0);
		return timestamp < today.getTime();
	}
</script>

<BentoCard padding="p-4 md:p-5" class="w-full">
	{#snippet header()}
		<div
			class="flex w-full items-center justify-between border-b border-[var(--color-surface-border)]/60 pb-3"
		>
			<div class="flex items-center gap-2.5 font-bold text-[var(--color-nav-active)]">
				<div
					class="flex h-8 w-8 items-center justify-center rounded-xl bg-[var(--color-primary-500)]/15 text-[var(--color-primary-600)]"
				>
					<ClipboardList size={18} />
				</div>
				<span class="text-sm font-semibold">{m.nav_todo()}</span>
			</div>
			<a
				href="/todo"
				class="flex items-center gap-0.5 text-xs font-semibold text-[var(--color-primary-600)] hover:underline"
			>
				<span>{m.action_more()}</span>
				<ChevronRight size={14} />
			</a>
		</div>
	{/snippet}

	{#if activeTodos.length === 0}
		<div class="py-8 text-center text-xs text-[var(--color-nav-inactive)]">
			{m.todo_empty()}
		</div>
	{:else}
		<ul class="space-y-3 pt-2">
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
							<span class="font-medium truncate block text-xs md:text-sm">{todo.name}</span>
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
							class="flex items-center gap-1 text-[11px] shrink-0 font-medium px-2 py-0.5 rounded-full {overdue
								? 'bg-[var(--color-danger-50)] text-[var(--color-danger-600)] font-bold'
								: 'bg-[var(--color-surface-muted)] text-[var(--color-nav-inactive)]'}"
						>
							<Calendar size={11} />
							{formatDate(todo.dueAt)}
						</span>
					{/if}
				</li>
			{/each}
		</ul>
	{/if}
</BentoCard>
