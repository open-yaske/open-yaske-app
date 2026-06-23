<!--
  TodoEditSheet.svelte
  TODO 編集用ボトムシート。
  フィールド: 名前(必須) / メモ / 期限(datetime-local) / 優先度 / 色 / 繰り返し / 科目関連付け
  保存 → addTodo() or updateTodo()、削除ボタン(既存時)
-->
<script lang="ts">
	import type { Todo, RepeatRule, ColorToken } from '$lib/types';
	import { m } from '$lib/paraglide/messages';
	import { userDataStore, themeStore } from '$lib/stores';
	import Sheet from '$lib/components/Sheet.svelte';
	import TextField from '$lib/components/TextField.svelte';
	import ColorPicker from '$lib/components/ColorPicker.svelte';
	import Button from '$lib/components/Button.svelte';
	import { Trash2 } from '@lucide/svelte';

	interface Props {
		open: boolean;
		onclose: () => void;
		/** 編集対象の TODO（null = 新規作成） */
		todo: Todo | null;
	}

	let { open = $bindable(false), onclose, todo }: Props = $props();

	// ----- フォーム状態 -----
	let name = $state('');
	let memo = $state('');
	let dueAtLocal = $state(''); // datetime-local 文字列
	let priority = $state<'low' | 'mid' | 'high'>('mid');
	let color = $state<ColorToken>('blue');
	let repeatFreq = $state<'none' | 'daily' | 'weekly' | 'monthly'>('none');
	let courseId = $state<string>('');

	// ----- 初期化 -----
	$effect(() => {
		if (open) {
			if (todo) {
				name = todo.name;
				memo = todo.memo ?? '';
				dueAtLocal = todo.dueAt ? toLocalDateTime(todo.dueAt) : '';
				priority = todo.priority ?? 'mid';
				color = todo.color;
				repeatFreq = todo.repeat?.freq ?? 'none';
				courseId = todo.courseId ?? '';
			} else {
				resetForm();
			}
		}
	});

	function resetForm() {
		name = '';
		memo = '';
		dueAtLocal = '';
		priority = 'mid';
		color = 'blue';
		repeatFreq = 'none';
		courseId = '';
	}

	// ----- ヘルパー -----
	function toLocalDateTime(ts: number): string {
		const d = new Date(ts);
		const pad = (n: number) => String(n).padStart(2, '0');
		return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())}T${pad(d.getHours())}:${pad(d.getMinutes())}`;
	}

	function fromLocalDateTime(s: string): number | undefined {
		if (!s) return undefined;
		return new Date(s).getTime();
	}

	// ----- 派生値 -----
	const nameError = $derived(name.trim() === '' ? m.todo_name_required() : '');

	const todoPalette = $derived(themeStore.pack.palette.todoColors);

	const courseOptions = $derived([
		{ value: '', label: m.todo_no_course() },
		...userDataStore.courses.map((c) => ({ value: c.id, label: c.name }))
	]);

	const priorityOptions = $derived([
		{ value: 'low', label: m.todo_priority_low() },
		{ value: 'mid', label: m.todo_priority_mid() },
		{ value: 'high', label: m.todo_priority_high() }
	]);

	const repeatOptions = $derived([
		{ value: 'none', label: m.todo_repeat_none() },
		{ value: 'daily', label: m.todo_repeat_daily() },
		{ value: 'weekly', label: m.todo_repeat_weekly() },
		{ value: 'monthly', label: m.todo_repeat_monthly() }
	]);

	// ----- 保存 -----
	function handleSave() {
		if (name.trim() === '') return;

		const dueAt = fromLocalDateTime(dueAtLocal);
		const repeat: RepeatRule | undefined =
			repeatFreq !== 'none' ? { freq: repeatFreq as 'daily' | 'weekly' | 'monthly' } : undefined;

		const payload = {
			name: name.trim(),
			done: todo?.done ?? false,
			courseId: courseId || undefined,
			memo: memo.trim() || undefined,
			color,
			dueAt,
			priority,
			repeat
		};

		if (todo) {
			void userDataStore.updateTodo(todo.id, payload);
		} else {
			void userDataStore.addTodo(payload);
		}

		onclose();
	}

	// ----- 削除 -----
	function handleDelete() {
		if (todo) {
			void userDataStore.removeTodo(todo.id);
		}
		onclose();
	}
</script>

<Sheet bind:open {onclose} title={todo ? m.todo_edit() : m.todo_add()} position="bottom">
	<div class="space-y-4">
		<!-- 名前（必須） -->
		<TextField
			bind:value={name}
			type="text"
			label={m.todo_name()}
			placeholder={m.todo_name()}
			required
			error={nameError}
		/>

		<!-- メモ -->
		<TextField
			bind:value={memo}
			type="textarea"
			label={m.todo_memo()}
			placeholder={m.todo_memo()}
			rows={3}
		/>

		<!-- 期限 -->
		<div class="w-full">
			<label for="todo-due" class="mb-1 block text-sm font-medium text-[var(--color-nav-active)]">
				{m.todo_due()}
			</label>
			<input
				id="todo-due"
				type="datetime-local"
				bind:value={dueAtLocal}
				class="block w-full rounded-lg border border-[var(--color-surface-border)] bg-[var(--color-surface-card)] px-3 py-2 text-sm text-[var(--color-nav-active)] focus:border-[var(--color-primary-500)] focus:outline-none focus:ring-2 focus:ring-[var(--color-primary-500)]/30"
			/>
			{#if !dueAtLocal}
				<p class="mt-1 text-xs text-[var(--color-nav-inactive)]">{m.todo_no_due()}</p>
			{/if}
		</div>

		<!-- 優先度 -->
		<TextField
			bind:value={priority}
			type="select"
			label={m.todo_priority()}
			options={priorityOptions}
		/>

		<!-- 色 -->
		<ColorPicker
			bind:value={color}
			palette={todoPalette}
			label={m.todo_color()}
			kind="todo"
			allowNone={false}
		/>

		<!-- 繰り返し -->
		<TextField
			bind:value={repeatFreq}
			type="select"
			label={m.todo_repeat()}
			options={repeatOptions}
		/>

		<!-- 科目関連付け -->
		<TextField
			bind:value={courseId}
			type="select"
			label={m.todo_course()}
			options={courseOptions}
		/>
	</div>

	{#snippet footer()}
		<div class="flex items-center gap-3">
			{#if todo}
				<Button variant="danger" size="md" onclick={handleDelete} class="shrink-0">
					<Trash2 size={16} />
					{m.action_delete()}
				</Button>
			{/if}
			<Button
				variant="primary"
				size="md"
				onclick={handleSave}
				class="flex-1"
				disabled={name.trim() === ''}
			>
				{m.action_save()}
			</Button>
		</div>
	{/snippet}
</Sheet>
