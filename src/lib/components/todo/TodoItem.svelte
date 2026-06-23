<!--
  TodoItem.svelte
  TODO リストの1行。カスタム円形チェックボックス + タイトル + 期限バッジ + 優先度バッジ + 科目関連付け。
  スワイプで削除、タップで編集。
-->
<script lang="ts">
	import type { Todo, Course } from '$lib/types';
	import { m } from '$lib/paraglide/messages';
	import { themeStore } from '$lib/stores';
	import { resolveColorHex } from '$lib/theme';
	import { Check, Clock, AlertCircle, BookOpen } from '@lucide/svelte';

	interface Props {
		todo: Todo;
		course?: Course;
		ontoggle: (id: string) => void;
		onedit: (todo: Todo) => void;
		ondelete: (id: string) => void;
	}

	let { todo, course, ontoggle, onedit, ondelete }: Props = $props();

	// ----- スワイプ状態 -----
	let translateX = $state(0);
	let startX = 0;
	let startY = 0;
	let dragging = $state(false);
	let horizontal = $state(true);

	const DELETE_THRESHOLD = 80;
	const SNAP_THRESHOLD = 40;

	// ----- 派生値 -----
	const isOverdue = $derived(!!todo.dueAt && !todo.done && todo.dueAt < Date.now());

	const colorHex = $derived(
		resolveColorHex(themeStore.pack, todo.color) || 'var(--color-primary-500)'
	);

	const courseColorHex = $derived(course ? resolveColorHex(themeStore.pack, course.color) : null);

	const priorityConfig = $derived(
		todo.priority === 'high'
			? {
					label: m.todo_priority_high(),
					class: 'bg-[var(--color-attendance-absent-bg)] text-[var(--color-attendance-absent-fg)]'
				}
			: todo.priority === 'mid'
				? {
						label: m.todo_priority_mid(),
						class: 'bg-[var(--color-attendance-late-bg)] text-[var(--color-attendance-late-fg)]'
					}
				: todo.priority === 'low'
					? {
							label: m.todo_priority_low(),
							class: 'bg-[var(--color-surface-muted)] text-[var(--color-surface-muted-foreground)]'
						}
					: null
	);

	const dueLabel = $derived.by(() => {
		if (!todo.dueAt) return null;
		const d = new Date(todo.dueAt);
		const now = new Date();
		const today = new Date(now.getFullYear(), now.getMonth(), now.getDate());
		const due = new Date(d.getFullYear(), d.getMonth(), d.getDate());
		const diffDays = Math.round((due.getTime() - today.getTime()) / 86400000);

		if (diffDays === 0) return m.section_today();
		if (diffDays === 1) return m.section_tomorrow();
		if (diffDays > 0 && diffDays < 7) {
			const dayNames = [
				m.day_sun(),
				m.day_mon(),
				m.day_tue(),
				m.day_wed(),
				m.day_thu(),
				m.day_fri(),
				m.day_sat()
			];
			return dayNames[d.getDay()];
		}
		return new Intl.DateTimeFormat(undefined, { month: 'short', day: 'numeric' }).format(d);
	});

	// ----- ポインターイベント（スワイプ） -----
	function handlePointerDown(e: PointerEvent) {
		if ((e.target as HTMLElement).closest('button, a')) {
			return;
		}
		startX = e.clientX;
		startY = e.clientY;
		dragging = true;
		horizontal = true;
		(e.currentTarget as HTMLElement).setPointerCapture(e.pointerId);
	}

	function handlePointerMove(e: PointerEvent) {
		if (!dragging) return;
		const dx = e.clientX - startX;
		const dy = e.clientY - startY;

		// 縦スクロールの場合はスワイプ無効
		if (horizontal && Math.abs(dy) > Math.abs(dx) && Math.abs(dy) > 10) {
			horizontal = false;
		}
		if (!horizontal) return;

		// 左方向のみ許可（負の translateX）
		translateX = Math.max(-DELETE_THRESHOLD, Math.min(0, dx));
	}

	function handlePointerUp() {
		if (!dragging) return;
		dragging = false;
		if (translateX <= -SNAP_THRESHOLD) {
			translateX = -DELETE_THRESHOLD;
		} else {
			translateX = 0;
		}
	}

	function handleDeleteClick(e: MouseEvent) {
		e.stopPropagation();
		translateX = 0;
		ondelete(todo.id);
	}

	function handleToggleClick(e: MouseEvent) {
		e.stopPropagation();
		ontoggle(todo.id);
	}

	function handleRowClick() {
		if (translateX !== 0) {
			translateX = 0;
			return;
		}
		onedit(todo);
	}

	function handleKeydown(e: KeyboardEvent) {
		if (e.key === 'Enter' || e.key === ' ') {
			e.preventDefault();
			handleRowClick();
		}
	}
</script>

<div class="relative overflow-hidden rounded-xl">
	<!-- 削除ボタン（背景） -->
	<button
		type="button"
		class="absolute inset-y-0 right-0 flex w-[80px] items-center justify-center bg-[var(--color-attendance-absent-bg)] text-[var(--color-attendance-absent-fg)]"
		aria-label={m.todo_delete()}
		onclick={handleDeleteClick}
	>
		<span class="text-sm font-medium">{m.action_delete()}</span>
	</button>

	<!-- メイン行 -->
	<div
		class="relative flex items-center gap-3 border-b border-[var(--color-surface-border)] bg-[var(--color-surface-card)] px-4 py-3 transition-transform"
		style="transform: translateX({translateX}px); {dragging ? 'transition: none;' : ''}"
		onpointerdown={handlePointerDown}
		onpointermove={handlePointerMove}
		onpointerup={handlePointerUp}
		onpointercancel={handlePointerUp}
		onclick={handleRowClick}
		onkeydown={handleKeydown}
		role="button"
		tabindex="0"
	>
		<!-- カスタム円形チェックボックス -->
		<button
			type="button"
			class="flex h-6 w-6 shrink-0 items-center justify-center rounded-full border-2 transition-colors focus:outline-none focus-visible:ring-2 focus-visible:ring-[var(--color-primary-500)] focus-visible:ring-offset-2"
			class:border-[var(--color-primary-500)]={!todo.done}
			class:bg-[var(--color-primary-500)]={todo.done}
			class:border-transparent={todo.done}
			style={todo.done ? '' : `border-color: ${colorHex};`}
			aria-label={todo.done ? m.todo_done() : m.todo_undone()}
			aria-checked={todo.done}
			role="checkbox"
			onclick={handleToggleClick}
		>
			{#if todo.done}
				<Check size={14} color="white" stroke-width={3} />
			{/if}
		</button>

		<!-- コンテンツ -->
		<div class="min-w-0 flex-1">
			<div class="flex items-center gap-2">
				<p
					class="truncate text-sm font-medium"
					class:text-[var(--color-nav-inactive)]={todo.done}
					class:line-through={todo.done}
					class:text-[var(--color-attendance-absent-fg)]={isOverdue}
				>
					{todo.name}
				</p>
			</div>

			<!-- バッジ行 -->
			{#if todo.dueAt || priorityConfig || course}
				<div class="mt-1 flex flex-wrap items-center gap-1.5">
					{#if isOverdue}
						<span
							class="inline-flex items-center gap-1 rounded-full bg-[var(--color-attendance-absent-bg)] px-2 py-0.5 text-xs font-medium text-[var(--color-attendance-absent-fg)]"
						>
							<AlertCircle size={10} stroke-width={2.5} />
							{m.todo_overdue()}
						</span>
					{:else if todo.dueAt}
						<span
							class="inline-flex items-center gap-1 rounded-full bg-[var(--color-surface-muted)] px-2 py-0.5 text-xs text-[var(--color-surface-muted-foreground)]"
						>
							<Clock size={10} stroke-width={2} />
							{dueLabel}
						</span>
					{/if}

					{#if priorityConfig}
						<span
							class="inline-flex items-center rounded-full px-2 py-0.5 text-xs font-medium {priorityConfig.class}"
						>
							{priorityConfig.label}
						</span>
					{/if}

					{#if course}
						<span
							class="inline-flex items-center gap-1 rounded-full px-2 py-0.5 text-xs text-[var(--color-nav-inactive)]"
							style="background-color: {courseColorHex}1A;"
						>
							<BookOpen size={10} stroke-width={2} style="color: {courseColorHex};" />
							<span class="truncate max-w-[100px]">{course.name}</span>
						</span>
					{/if}
				</div>
			{/if}
		</div>
	</div>
</div>
