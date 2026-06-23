<!--
  Calendar +page.svelte
  カレンダー画面。月/週/日の3ビューをトグルタブで切替。
  FAB でイベント追加、イベントタップで編集。
-->
<script lang="ts">
	import { m } from '$lib/paraglide/messages';
	import { settingsStore, userDataStore, themeStore } from '$lib/stores';
	import type { CalendarEvent, Course, DayOfWeek, Period, Todo } from '$lib/types';
	import PageHeader from '$lib/components/PageHeader.svelte';
	import Container from '$lib/components/Container.svelte';
	import IconButton from '$lib/components/IconButton.svelte';
	import EmptyState from '$lib/components/EmptyState.svelte';
	import CalendarMonthView from '$lib/components/calendar/CalendarMonthView.svelte';
	import CalendarWeekView from '$lib/components/calendar/CalendarWeekView.svelte';
	import CalendarDayView from '$lib/components/calendar/CalendarDayView.svelte';
	import EventEditSheet from '$lib/components/calendar/EventEditSheet.svelte';
	import SyllabusSheet from '$lib/components/timetable/SyllabusSheet.svelte';
	import TodoEditSheet from '$lib/components/todo/TodoEditSheet.svelte';
	import Sheet from '$lib/components/Sheet.svelte';
	import Switch from '$lib/components/Switch.svelte';
	import Button from '$lib/components/Button.svelte';
	import { getJapaneseHolidays } from '$lib/holidays';
	import {
		ChevronLeft,
		ChevronRight,
		Plus,
		CalendarDays,
		Sliders,
		Sun,
		Moon,
		Laptop
	} from '@lucide/svelte';

	type ViewMode = 'month' | 'week' | 'day';

	let viewMode = $state<ViewMode>('month');
	let currentDate = $state(new Date());
	let sheetOpen = $state(false);
	let filterOpen = $state(false);
	let editingEvent = $state<CalendarEvent | null>(null);
	let defaultDate = $state<Date | undefined>(undefined);

	// シラバス表示用状態
	let syllabusOpen = $state(false);
	let selectedCourse = $state<Course | null>(null);
	let syllabusTargetDate = $state<string | undefined>(undefined);

	// TODO編集用状態
	let todoEditOpen = $state(false);
	let editingTodo = $state<Todo | null>(null);

	const pack = $derived(themeStore.pack);
	const weekStart = $derived(settingsStore.settings.calendarWeekStart);
	const density = $derived(settingsStore.settings.calendarDensity);
	const showHolidays = $derived(settingsStore.settings.calendarShowHolidays);

	const periodTimes: Record<Period, { start: [number, number]; end: [number, number] }> = {
		1: { start: [8, 40], end: [10, 10] },
		2: { start: [10, 25], end: [11, 55] },
		3: { start: [12, 50], end: [14, 20] },
		4: { start: [14, 35], end: [16, 5] },
		5: { start: [16, 15], end: [17, 45] },
		6: { start: [17, 55], end: [19, 25] },
		7: { start: [19, 35], end: [21, 5] }
	};

	const dayIndex: Record<DayOfWeek, number> = {
		sun: 0,
		mon: 1,
		tue: 2,
		wed: 3,
		thu: 4,
		fri: 5,
		sat: 6
	};

	function dayStart(date: Date): Date {
		return new Date(date.getFullYear(), date.getMonth(), date.getDate());
	}

	function toYMD(date: Date): string {
		return `
			${date.getFullYear()}-${(date.getMonth() + 1).toString().padStart(2, '0')}-${date
				.getDate()
				.toString()
				.padStart(2, '0')}`.trim();
	}

	function visibleRange() {
		if (viewMode === 'week') {
			const start = dayStart(weekStartDate);
			return { start, end: new Date(start.getFullYear(), start.getMonth(), start.getDate() + 7) };
		}
		if (viewMode === 'day') {
			const start = dayStart(currentDate);
			return { start, end: new Date(start.getFullYear(), start.getMonth(), start.getDate() + 1) };
		}
		const firstDay = new Date(currentDate.getFullYear(), currentDate.getMonth(), 1);
		const startOffset = weekStart === 'sun' ? firstDay.getDay() : (firstDay.getDay() + 6) % 7;
		const start = new Date(firstDay.getFullYear(), firstDay.getMonth(), 1 - startOffset);
		return { start, end: new Date(start.getFullYear(), start.getMonth(), start.getDate() + 42) };
	}

	function holidayEvents(start: Date, end: Date): CalendarEvent[] {
		if (!settingsStore.settings.calendarShowHolidays) return [];
		const years = Array.from(
			{ length: end.getFullYear() - start.getFullYear() + 1 },
			(_, i) => start.getFullYear() + i
		);
		return years
			.flatMap((year) => getJapaneseHolidays(year))
			.map((holiday) => {
				const date = new Date(holiday.date + 'T00:00:00');
				const startAt = date.getTime();
				return {
					id: `holiday-${holiday.date}`,
					title: holiday.name,
					allDay: true,
					startAt,
					endAt: startAt + 86_399_999,
					calendarId: 'holidays',
					color: 'red',
					createdAt: startAt,
					updatedAt: startAt
				} satisfies CalendarEvent;
			})
			.filter((event) => event.startAt >= start.getTime() && event.startAt < end.getTime());
	}

	function timetableEvents(courses: Course[], start: Date, end: Date): CalendarEvent[] {
		if (!settingsStore.settings.calendarShowTimetable) return [];
		const events: CalendarEvent[] = [];
		for (const course of courses) {
			const targetDay = dayIndex[course.day];
			for (
				let d = dayStart(start);
				d < end;
				d = new Date(d.getFullYear(), d.getMonth(), d.getDate() + 1)
			) {
				if (d.getDay() !== targetDay) continue;
				const startTime = periodTimes[course.period];
				const endPeriod = Math.min(7, (course.period + course.span - 1) as Period);
				const endTime = periodTimes[endPeriod as Period];
				const startAt = new Date(
					d.getFullYear(),
					d.getMonth(),
					d.getDate(),
					startTime.start[0],
					startTime.start[1]
				).getTime();
				const endAt = new Date(
					d.getFullYear(),
					d.getMonth(),
					d.getDate(),
					endTime.end[0],
					endTime.end[1]
				).getTime();
				events.push({
					id: `timetable-${course.id}-${toYMD(d)}`,
					title: course.name,
					allDay: false,
					startAt,
					endAt,
					location: course.classroom,
					memo: course.memo,
					calendarId: 'timetable',
					color: course.color,
					createdAt: course.createdAt,
					updatedAt: course.updatedAt
				});
			}
		}
		return events;
	}

	function todoEvents(start: Date, end: Date, showTodos: boolean): CalendarEvent[] {
		if (!showTodos) return [];
		return userDataStore.todos
			.filter((t) => t.dueAt && t.dueAt >= start.getTime() && t.dueAt < end.getTime())
			.map((todo) => {
				const due = todo.dueAt!;
				let title = todo.name;
				let color = todo.color;
				if (todo.courseId) {
					const course = userDataStore.courses.find((c) => c.id === todo.courseId);
					if (course) {
						title = `[${course.name}] ${todo.name}`;
						color = course.color;
					}
				} else {
					title = `[TODO] ${todo.name}`;
				}
				return {
					id: `todo-${todo.id}`,
					title,
					allDay: false,
					startAt: due,
					endAt: due + 30 * 60 * 1000,
					location: todo.location,
					memo: todo.memo,
					calendarId: 'todo',
					color,
					cancelled: todo.done,
					createdAt: todo.createdAt,
					updatedAt: todo.updatedAt
				} satisfies CalendarEvent;
			});
	}

	const filteredEvents = $derived.by(() => {
		const range = visibleRange();
		const s = settingsStore.settings;
		const userEvents = userDataStore.events.filter((e) => {
			if (e.calendarId === 'academic' && !s.calendarShowAcademic) return false;
			if (e.calendarId === 'holidays' && !s.calendarShowHolidays) return false;
			if (e.calendarId === 'external' && !s.calendarShowExternal) return false;
			if (e.calendarId === 'todo' && !s.calendarShowTodos) return false;
			return true;
		});
		return [
			...userEvents,
			...holidayEvents(range.start, range.end),
			...timetableEvents(userDataStore.courses, range.start, range.end),
			...todoEvents(range.start, range.end, s.calendarShowTodos ?? true)
		];
	});

	// ヘッダーのタイトル
	const headerTitle = $derived.by(() => {
		const y = currentDate.getFullYear();
		const mo = currentDate.getMonth() + 1;
		if (viewMode === 'month') {
			return `${y}年${mo}月`;
		}
		if (viewMode === 'week') {
			const ws = getWeekStart(currentDate, weekStart);
			const we = new Date(ws.getFullYear(), ws.getMonth(), ws.getDate() + 6);
			return `${ws.getFullYear()}/${ws.getMonth() + 1}/${ws.getDate()} - ${we.getMonth() + 1}/${we.getDate()}`;
		}
		return `${y}/${mo.toString().padStart(2, '0')}/${currentDate.getDate().toString().padStart(2, '0')}`;
	});

	// 週の開始日を計算
	function getWeekStart(date: Date, start: 'sun' | 'mon'): Date {
		const d = new Date(date);
		const day = d.getDay();
		const offset = start === 'sun' ? day : (day + 6) % 7;
		return new Date(d.getFullYear(), d.getMonth(), d.getDate() - offset);
	}

	const weekStartDate = $derived(getWeekStart(currentDate, weekStart));

	// ナビゲーション
	function goPrev() {
		if (viewMode === 'month') {
			currentDate = new Date(currentDate.getFullYear(), currentDate.getMonth() - 1, 1);
		} else if (viewMode === 'week') {
			currentDate = new Date(
				currentDate.getFullYear(),
				currentDate.getMonth(),
				currentDate.getDate() - 7
			);
		} else {
			currentDate = new Date(
				currentDate.getFullYear(),
				currentDate.getMonth(),
				currentDate.getDate() - 1
			);
		}
	}

	function goNext() {
		if (viewMode === 'month') {
			currentDate = new Date(currentDate.getFullYear(), currentDate.getMonth() + 1, 1);
		} else if (viewMode === 'week') {
			currentDate = new Date(
				currentDate.getFullYear(),
				currentDate.getMonth(),
				currentDate.getDate() + 7
			);
		} else {
			currentDate = new Date(
				currentDate.getFullYear(),
				currentDate.getMonth(),
				currentDate.getDate() + 1
			);
		}
	}

	function goToday() {
		currentDate = new Date();
	}

	// イベント操作
	function handleDayClick(date: Date) {
		currentDate = date;
		viewMode = 'day';
	}

	function handleEventClick(event: CalendarEvent) {
		if (event.calendarId === 'holidays') return;

		if (event.calendarId === 'timetable') {
			const parts = event.id.split('-');
			if (parts.length >= 5) {
				const courseId = parts[1];
				const dateStr = parts.slice(2).join('-');
				const course = userDataStore.courses.find((c) => c.id === courseId);
				if (course) {
					selectedCourse = course;
					syllabusTargetDate = dateStr;
					syllabusOpen = true;
				}
			}
			return;
		}

		if (event.calendarId === 'todo') {
			const todoId = event.id.substring(5);
			const todo = userDataStore.todos.find((t) => t.id === todoId);
			if (todo) {
				editingTodo = todo;
				todoEditOpen = true;
			}
			return;
		}

		editingEvent = event;
		defaultDate = undefined;
		sheetOpen = true;
	}

	function handleAddNew() {
		editingEvent = null;
		defaultDate = currentDate;
		sheetOpen = true;
	}

	function handleSave(data: Omit<CalendarEvent, 'id' | 'createdAt' | 'updatedAt'>) {
		if (editingEvent) {
			void userDataStore.updateEvent(editingEvent.id, data);
		} else {
			void userDataStore.addEvent(data);
		}
	}

	function handleDelete(id: string) {
		void userDataStore.removeEvent(id);
	}

	// 月表示でイベントがあるか
	const hasEvents = $derived(filteredEvents.length > 0);

	// ビュー切り替えタブ
	const viewTabs = $derived([
		{ mode: 'day' as const, label: m.calendar_view_day() },
		{ mode: 'week' as const, label: m.calendar_view_week() },
		{ mode: 'month' as const, label: m.calendar_view_month() }
	]);

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

<svelte:head><title>{m.nav_calendar()} | open-nexus</title></svelte:head>

{#snippet leadingSnippet()}
	<IconButton
		icon={themeIcon}
		ariaLabel={themeAriaLabel}
		onclick={toggleTheme}
		variant="subtle"
		size="sm"
	/>
{/snippet}

{#snippet titleSnippet()}
	<div class="flex items-center justify-center gap-1">
		<h1 class="text-base font-bold text-[var(--color-nav-active)]">{headerTitle}</h1>
	</div>
{/snippet}

{#snippet trailingSnippet()}
	<IconButton
		icon={Sliders}
		ariaLabel="フィルター"
		onclick={() => (filterOpen = true)}
		variant="subtle"
		size="sm"
	/>
{/snippet}

<PageHeader leading={leadingSnippet} {titleSnippet} trailing={trailingSnippet}>
	<div class="flex items-center gap-1 mt-1">
		<IconButton
			icon={ChevronLeft}
			ariaLabel="Previous"
			onclick={goPrev}
			variant="subtle"
			size="sm"
		/>
		<button
			type="button"
			class="rounded-full px-3 py-1 text-xs font-semibold text-[var(--color-primary-700)] hover:bg-[var(--color-surface-muted)]"
			onclick={goToday}
		>
			{m.calendar_today()}
		</button>
		<IconButton icon={ChevronRight} ariaLabel="Next" onclick={goNext} variant="subtle" size="sm" />
	</div>
</PageHeader>

<Container size="full" class="px-0">
	<!-- ビュー切り替えタブ -->
	<div class="flex items-center gap-1 border-b border-[var(--color-surface-border)] px-4 py-2">
		{#each viewTabs as tab (tab.mode)}
			<button
				type="button"
				class="rounded-full px-4 py-1.5 text-sm font-medium transition-colors
				{viewMode === tab.mode
					? 'bg-[var(--color-primary-500)] text-white'
					: 'text-[var(--color-nav-inactive)] hover:bg-[var(--color-surface-muted)]'}"
				onclick={() => {
					viewMode = tab.mode;
				}}
			>
				{tab.label}
			</button>
		{/each}
	</div>

	<!-- アクティブビュー -->
	{#if viewMode === 'month'}
		<CalendarMonthView
			events={filteredEvents}
			{currentDate}
			{weekStart}
			{pack}
			{density}
			{showHolidays}
			ondayclick={handleDayClick}
			oneventclick={handleEventClick}
		/>
	{:else if viewMode === 'week'}
		<CalendarWeekView
			events={filteredEvents}
			{weekStartDate}
			{weekStart}
			{pack}
			{showHolidays}
			ondayclick={handleDayClick}
			oneventclick={handleEventClick}
		/>
	{:else}
		<CalendarDayView
			events={filteredEvents}
			date={currentDate}
			{pack}
			{showHolidays}
			ondayclick={handleDayClick}
			oneventclick={handleEventClick}
		/>
	{/if}

	<!-- 空状態（月表示でイベントが全くない場合） -->
	{#if !hasEvents && viewMode === 'month'}
		<EmptyState
			icon={CalendarDays}
			message={m.calendar_empty()}
			submessage={m.calendar_add()}
			actionLabel={m.action_add()}
			onAction={handleAddNew}
			class="py-12"
		/>
	{/if}
</Container>

<!-- FAB -->
<button
	type="button"
	class="fixed bottom-24 right-4 z-30 flex h-14 w-14 items-center justify-center rounded-full bg-[var(--color-primary-800)] text-white shadow-lg transition-transform hover:scale-105 active:scale-95 dark:bg-[var(--color-primary-500)] dark:text-slate-950 dark:hover:bg-[var(--color-primary-400)] md:bottom-8"
	aria-label={m.calendar_add()}
	onclick={handleAddNew}
>
	<Plus size={24} />
</button>

<Sheet bind:open={filterOpen} onclose={() => (filterOpen = false)} title="カレンダーフィルター">
	<div class="space-y-1">
		<div
			class="flex items-center justify-between border-b border-[var(--color-surface-border)] py-3"
		>
			<span class="text-sm text-[var(--color-nav-active)]"
				>{m.settings_calendar_show_academic()}</span
			>
			<Switch
				checked={settingsStore.settings.calendarShowAcademic}
				onchange={(v) => settingsStore.update({ calendarShowAcademic: v })}
			/>
		</div>
		<div
			class="flex items-center justify-between border-b border-[var(--color-surface-border)] py-3"
		>
			<span class="text-sm text-[var(--color-nav-active)]"
				>{m.settings_calendar_show_holidays()}</span
			>
			<Switch
				checked={settingsStore.settings.calendarShowHolidays}
				onchange={(v) => settingsStore.update({ calendarShowHolidays: v })}
			/>
		</div>
		<div
			class="flex items-center justify-between border-b border-[var(--color-surface-border)] py-3"
		>
			<span class="text-sm text-[var(--color-nav-active)]"
				>{m.settings_calendar_show_external()}</span
			>
			<Switch
				checked={settingsStore.settings.calendarShowExternal}
				onchange={(v) => settingsStore.update({ calendarShowExternal: v })}
			/>
		</div>
		<div
			class="flex items-center justify-between border-b border-[var(--color-surface-border)] py-3"
		>
			<span class="text-sm text-[var(--color-nav-active)]"
				>{m.settings_calendar_show_timetable()}</span
			>
			<Switch
				checked={settingsStore.settings.calendarShowTimetable}
				onchange={(v) => settingsStore.update({ calendarShowTimetable: v })}
			/>
		</div>
		<div class="flex items-center justify-between py-3">
			<span class="text-sm text-[var(--color-nav-active)]">{m.settings_calendar_show_todos()}</span>
			<Switch
				checked={settingsStore.settings.calendarShowTodos}
				onchange={(v) => settingsStore.update({ calendarShowTodos: v })}
			/>
		</div>
	</div>

	{#snippet footer()}
		<Button variant="primary" size="md" class="w-full" onclick={() => (filterOpen = false)}
			>適用</Button
		>
	{/snippet}
</Sheet>

<!-- イベント編集シート -->
<EventEditSheet
	bind:open={sheetOpen}
	onclose={() => {
		sheetOpen = false;
	}}
	event={editingEvent}
	{defaultDate}
	{pack}
	onsave={handleSave}
	ondelete={handleDelete}
/>

<!-- シラバスシート (授業の出席をトグルする用) -->
<SyllabusSheet
	bind:open={syllabusOpen}
	onclose={() => (syllabusOpen = false)}
	course={selectedCourse}
	targetDate={syllabusTargetDate}
/>

<!-- TODO編集シート (TODOのトグル・編集用) -->
<TodoEditSheet bind:open={todoEditOpen} onclose={() => (todoEditOpen = false)} todo={editingTodo} />
