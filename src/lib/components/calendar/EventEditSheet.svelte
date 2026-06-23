<!--
  EventEditSheet.svelte
  イベント作成・編集のボトムシート。
  タイトル / 場所 / メモ のテキスト入力、終日スイッチ、開始/終了日時、
  カラーピッカー、カレンダーIDセレクト、保存・削除ボタン。
-->
<script lang="ts">
	import type { CalendarEvent } from '$lib/types';
	import { resolveColorHex, type ThemePack } from '$lib/theme';
	import { m } from '$lib/paraglide/messages';
	import Sheet from '$lib/components/Sheet.svelte';
	import TextField from '$lib/components/TextField.svelte';
	import Switch from '$lib/components/Switch.svelte';
	import ColorPicker from '$lib/components/ColorPicker.svelte';
	import Button from '$lib/components/Button.svelte';
	import { Trash2 } from '@lucide/svelte';

	interface Props {
		open: boolean;
		onclose: () => void;
		/** 編集対象のイベント（undefined なら新規作成） */
		event?: CalendarEvent | null;
		/** 新規作成時のデフォルト日付 */
		defaultDate?: Date;
		pack: ThemePack;
		onsave: (data: Omit<CalendarEvent, 'id' | 'createdAt' | 'updatedAt'>) => void;
		ondelete?: (id: string) => void;
	}

	let {
		open = $bindable(),
		onclose,
		event = null,
		defaultDate,
		pack,
		onsave,
		ondelete
	}: Props = $props();

	// フォーム状態
	let title = $state('');
	let location = $state('');
	let memo = $state('');
	let allDay = $state(false);
	let startDate = $state('');
	let startTime = $state('');
	let endDate = $state('');
	let endTime = $state('');
	let color = $state('blue');
	let calendarId = $state('academic');
	let confirmDelete = $state(false);
	let isEditMode = $state(false);

	const isEditing = $derived(!!event);

	// イベント or デフォルト日付からフォームを初期化
	$effect(() => {
		if (!open) return;
		confirmDelete = false;
		if (event) {
			title = event.title;
			location = event.location ?? '';
			memo = event.memo ?? '';
			allDay = event.allDay;
			color = event.color;
			calendarId = event.calendarId;
			const s = new Date(event.startAt);
			const e = new Date(event.endAt);
			startDate = toDateInput(s);
			startTime = toTimeInput(s);
			endDate = toDateInput(e);
			endTime = toTimeInput(e);
			isEditMode = false;
		} else {
			title = '';
			location = '';
			memo = '';
			allDay = false;
			color = 'blue';
			calendarId = 'academic';
			const base = defaultDate ?? new Date();
			startDate = toDateInput(base);
			startTime = '09:00';
			endDate = toDateInput(base);
			endTime = '10:00';
			isEditMode = true;
		}
	});

	function toDateInput(d: Date): string {
		const y = d.getFullYear();
		const m = (d.getMonth() + 1).toString().padStart(2, '0');
		const day = d.getDate().toString().padStart(2, '0');
		return `${y}-${m}-${day}`;
	}

	function toTimeInput(d: Date): string {
		const h = d.getHours().toString().padStart(2, '0');
		const m = d.getMinutes().toString().padStart(2, '0');
		return `${h}:${m}`;
	}

	function combineDateTime(dateStr: string, timeStr: string): number {
		if (allDay) {
			return new Date(dateStr + 'T00:00:00').getTime();
		}
		return new Date(dateStr + 'T' + timeStr + ':00').getTime();
	}

	function handleSave() {
		if (!title.trim()) return;

		const startAt = combineDateTime(startDate, startTime);
		const endAt = allDay
			? new Date(endDate + 'T23:59:59').getTime()
			: combineDateTime(endDate, endTime);

		onsave({
			title: title.trim(),
			allDay,
			startAt,
			endAt,
			location: location.trim() || undefined,
			memo: memo.trim() || undefined,
			calendarId,
			color
		});
		onclose();
	}

	function handleDelete() {
		if (event && ondelete) {
			ondelete(event.id);
			onclose();
		}
	}

	const calendarOptions = $derived([
		{ value: 'academic', label: m.calendar_academic() },
		{ value: 'holidays', label: m.calendar_holidays() },
		{ value: 'external', label: m.calendar_external() }
	]);

	const eventPalette = $derived(pack.palette.eventColors);
</script>

<Sheet
	bind:open
	{onclose}
	title={isEditing ? (isEditMode ? m.calendar_edit() : '予定の詳細') : m.calendar_add()}
>
	{#if !isEditMode}
		<div class="space-y-4 text-sm text-[var(--color-nav-active)]">
			<!-- カラーバーとタイトル -->
			<div
				class="flex items-center gap-3 p-3 rounded-lg bg-[var(--color-surface-muted)]"
				style="border-left: 4px solid {resolveColorHex(pack, color)}"
			>
				<h3 class="text-base font-bold">{title}</h3>
			</div>
			<!-- メタデータ -->
			<dl class="space-y-2">
				<div class="flex gap-2">
					<dt class="shrink-0 font-medium text-[var(--color-nav-inactive)]">日時:</dt>
					<dd>
						{startDate}
						{#if !allDay}{startTime}{/if}
						〜 {endDate}
						{#if !allDay}{endTime}{/if}
						{#if allDay}(終日){/if}
					</dd>
				</div>
				{#if location}
					<div class="flex gap-2">
						<dt class="shrink-0 font-medium text-[var(--color-nav-inactive)]">場所:</dt>
						<dd>{location}</dd>
					</div>
				{/if}
				<div class="flex gap-2">
					<dt class="shrink-0 font-medium text-[var(--color-nav-inactive)]">種類:</dt>
					<dd>
						{calendarId === 'academic'
							? m.calendar_academic()
							: calendarId === 'holidays'
								? m.calendar_holidays()
								: m.calendar_external()}
					</dd>
				</div>
			</dl>
			<!-- メモ -->
			{#if memo}
				<div class="border-t border-[var(--color-surface-border)] pt-3">
					<h4 class="mb-1 font-medium text-[var(--color-nav-inactive)]">{m.course_memo()}</h4>
					<p class="whitespace-pre-wrap">{memo}</p>
				</div>
			{/if}
		</div>
	{:else}
		<form
			class="space-y-5"
			onsubmit={(e) => {
				e.preventDefault();
				handleSave();
			}}
		>
			<!-- タイトル -->
			<TextField
				type="text"
				label={m.calendar_title()}
				bind:value={title}
				placeholder={m.calendar_title()}
				required
			/>

			<!-- 終日スイッチ -->
			<div class="flex items-center justify-between">
				<span class="text-sm font-medium text-[var(--color-nav-active)]"
					>{m.calendar_all_day()}</span
				>
				<Switch bind:checked={allDay} label={m.calendar_all_day()} />
			</div>

			<!-- 開始日時 -->
			<div class="space-y-1.5">
				<span class="block text-sm font-medium text-[var(--color-nav-active)]"
					>{m.calendar_start()}</span
				>
				<div class="flex gap-2">
					<input
						type="date"
						bind:value={startDate}
						aria-label={m.calendar_start()}
						class="block flex-1 rounded-lg border border-[var(--color-surface-border)] bg-[var(--color-surface-card)] px-3 py-2 text-sm text-[var(--color-nav-active)] focus:border-[var(--color-primary-500)] focus:outline-none focus:ring-2 focus:ring-[var(--color-primary-500)]/30"
					/>
					{#if !allDay}
						<input
							type="time"
							bind:value={startTime}
							aria-label={m.calendar_start()}
							class="block w-28 rounded-lg border border-[var(--color-surface-border)] bg-[var(--color-surface-card)] px-3 py-2 text-sm text-[var(--color-nav-active)] focus:border-[var(--color-primary-500)] focus:outline-none focus:ring-2 focus:ring-[var(--color-primary-500)]/30"
						/>
					{/if}
				</div>
			</div>

			<!-- 終了日時 -->
			<div class="space-y-1.5">
				<span class="block text-sm font-medium text-[var(--color-nav-active)]"
					>{m.calendar_end()}</span
				>
				<div class="flex gap-2">
					<input
						type="date"
						bind:value={endDate}
						aria-label={m.calendar_end()}
						class="block flex-1 rounded-lg border border-[var(--color-surface-border)] bg-[var(--color-surface-card)] px-3 py-2 text-sm text-[var(--color-nav-active)] focus:border-[var(--color-primary-500)] focus:outline-none focus:ring-2 focus:ring-[var(--color-primary-500)]/30"
					/>
					{#if !allDay}
						<input
							type="time"
							bind:value={endTime}
							aria-label={m.calendar_end()}
							class="block w-28 rounded-lg border border-[var(--color-surface-border)] bg-[var(--color-surface-card)] px-3 py-2 text-sm text-[var(--color-nav-active)] focus:border-[var(--color-primary-500)] focus:outline-none focus:ring-2 focus:ring-[var(--color-primary-500)]/30"
						/>
					{/if}
				</div>
			</div>

			<!-- 場所 -->
			<TextField
				type="text"
				label={m.calendar_location()}
				bind:value={location}
				placeholder={m.calendar_location()}
			/>

			<!-- メモ -->
			<TextField
				type="textarea"
				label={m.course_memo()}
				bind:value={memo}
				placeholder={m.course_memo()}
				rows={3}
			/>

			<!-- カレンダーID -->
			<TextField
				type="select"
				label={m.nav_calendar()}
				bind:value={calendarId}
				options={calendarOptions}
			/>

			<!-- カラーピッカー -->
			<ColorPicker
				bind:value={color}
				palette={eventPalette}
				label={m.calendar_color()}
				kind="event"
				allowCustom={true}
				allowNone={false}
			/>
		</form>
	{/if}

	{#snippet footer()}
		<div class="flex items-center gap-3 w-full">
			{#if !isEditMode}
				{#if isEditing && ondelete}
					{#if confirmDelete}
						<Button variant="danger" size="md" onclick={handleDelete} class="flex-1">
							{m.dialog_delete_title()}
						</Button>
						<Button
							variant="secondary"
							size="md"
							onclick={() => {
								confirmDelete = false;
							}}
						>
							{m.action_cancel()}
						</Button>
					{:else}
						<Button
							variant="ghost"
							size="md"
							onclick={() => {
								confirmDelete = true;
							}}
							class="text-[var(--color-attendance-absent-fg)] shrink-0"
						>
							<Trash2 size={16} />
							{m.action_delete()}
						</Button>
					{/if}
				{/if}
				<Button variant="secondary" size="md" onclick={() => (isEditMode = true)} class="flex-1">
					{m.action_edit()}
				</Button>
				<Button variant="primary" size="md" onclick={onclose} class="flex-1">
					{m.action_close()}
				</Button>
			{:else}
				<Button
					variant="secondary"
					size="md"
					onclick={isEditing ? () => (isEditMode = false) : onclose}
					class="flex-1"
				>
					{m.action_cancel()}
				</Button>
				<Button
					variant="primary"
					size="md"
					onclick={handleSave}
					disabled={!title.trim()}
					class="flex-1"
				>
					{m.action_save()}
				</Button>
			{/if}
		</div>
	{/snippet}
</Sheet>
