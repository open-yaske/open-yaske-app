<!--
  CalendarMonthView.svelte
  6行の月グリッド。日付番号、イベントドット/ピル、祝日ハイライト、今日ハイライトを表示。
  日をタップするとイベント追加、イベントをタップすると編集。
-->
<script lang="ts">
	import type { CalendarEvent } from '$lib/types';
	import type { ThemePack } from '$lib/theme';
	import { getJapaneseHolidays } from '$lib/holidays';
	import { m } from '$lib/paraglide/messages';
	import { SvelteMap } from 'svelte/reactivity';
	import EventPill from './EventPill.svelte';

	interface Props {
		events: CalendarEvent[];
		currentDate: Date;
		weekStart: 'sun' | 'mon';
		pack: ThemePack;
		density: 3 | 4;
		showHolidays: boolean;
		ondayclick: (date: Date) => void;
		oneventclick: (event: CalendarEvent) => void;
	}

	let {
		events,
		currentDate,
		weekStart,
		pack,
		density = 4,
		showHolidays = true,
		ondayclick,
		oneventclick
	}: Props = $props();

	const weekLabels = $derived(
		weekStart === 'sun'
			? [m.day_sun(), m.day_mon(), m.day_tue(), m.day_wed(), m.day_thu(), m.day_fri(), m.day_sat()]
			: [m.day_mon(), m.day_tue(), m.day_wed(), m.day_thu(), m.day_fri(), m.day_sat(), m.day_sun()]
	);

	const today = $derived(new Date());

	function isSameDay(a: Date, b: Date): boolean {
		return (
			a.getFullYear() === b.getFullYear() &&
			a.getMonth() === b.getMonth() &&
			a.getDate() === b.getDate()
		);
	}

	function toYMD(d: Date): string {
		const y = d.getFullYear();
		const m = (d.getMonth() + 1).toString().padStart(2, '0');
		const day = d.getDate().toString().padStart(2, '0');
		return `${y}-${m}-${day}`;
	}

	// 祝日マップ（当月分）
	const holidayMap = $derived.by(() => {
		const map = new SvelteMap<string, string>();
		if (!showHolidays) return map;
		const year = currentDate.getFullYear();
		const month = currentDate.getMonth();
		const holidays = getJapaneseHolidays(year);
		for (const h of holidays) {
			const hDate = new Date(h.date + 'T00:00:00');
			if (hDate.getMonth() === month) {
				map.set(toYMD(hDate), h.name);
			}
		}
		return map;
	});

	// 6行×7列のグリッドを生成
	const gridDays = $derived.by(() => {
		const year = currentDate.getFullYear();
		const month = currentDate.getMonth();
		const firstDay = new Date(year, month, 1);
		const startOffset = weekStart === 'sun' ? firstDay.getDay() : (firstDay.getDay() + 6) % 7;
		const gridStart = new Date(year, month, 1 - startOffset);
		const days: Date[] = [];
		for (let i = 0; i < 42; i++) {
			days.push(new Date(gridStart.getFullYear(), gridStart.getMonth(), gridStart.getDate() + i));
		}
		return days;
	});

	// 各日のイベントを取得
	function getEventsForDay(date: Date): CalendarEvent[] {
		return events
			.filter((e) => {
				const start = new Date(e.startAt);
				const end = new Date(e.endAt);
				const dayStart = new Date(date.getFullYear(), date.getMonth(), date.getDate());
				const dayEnd = new Date(date.getFullYear(), date.getMonth(), date.getDate() + 1);
				// 終日イベント or 時間指定イベントが日をまたぐかチェック
				if (e.allDay) {
					return start <= dayEnd && end >= dayStart;
				}
				return start < dayEnd && end > dayStart;
			})
			.sort((a, b) => a.startAt - b.startAt);
	}

	// イベントがキャンセル（休講）扱いか
	function isCancelled(event: CalendarEvent): boolean {
		return event.cancelled === true;
	}

	function handleDayClick(date: Date) {
		ondayclick(date);
	}
</script>

<div class="flex flex-col">
	<!-- 曜日ヘッダー -->
	<div class="grid grid-cols-7 border-b border-[var(--color-surface-border)]">
		{#each weekLabels as label, i (label)}
			<div
				class="py-2 text-center text-xs font-medium
				{(weekStart === 'sun' && i === 0) || (weekStart === 'mon' && i === 6)
					? 'text-[var(--color-attendance-absent-fg)]'
					: (weekStart === 'sun' && i === 6) || (weekStart === 'mon' && i === 5)
						? 'text-[var(--color-primary-500)]'
						: 'text-[var(--color-nav-inactive)]'}"
			>
				{label}
			</div>
		{/each}
	</div>

	<!-- 6行グリッド -->
	<div class="grid grid-cols-7">
		{#each gridDays as day, i (i)}
			{@const dayEvents = getEventsForDay(day)}
			{@const ymd = toYMD(day)}
			{@const holiday = holidayMap.get(ymd)}
			{@const isCurrentMonth = day.getMonth() === currentDate.getMonth()}
			{@const isToday = isSameDay(day, today)}
			{@const isWeekend = day.getDay() === 0 || day.getDay() === 6}
			<button
				type="button"
				class="relative flex min-h-[72px] flex-col items-start gap-0.5 border-b border-r border-[var(--color-surface-border)] p-1 text-left transition-colors hover:bg-[var(--color-surface-muted)] {i %
					7 ===
				6
					? 'border-r-0'
					: ''} {i >= 35 ? 'border-b-0' : ''}"
				onclick={() => handleDayClick(day)}
			>
				<!-- 日付番号 -->
				<div class="flex w-full items-center justify-between">
					<span
						class="flex h-6 w-6 items-center justify-center rounded-full text-xs font-medium
						{isToday
							? 'bg-[var(--color-primary-500)] text-white'
							: holiday
								? 'text-[var(--color-attendance-absent-fg)]'
								: isWeekend && isCurrentMonth
									? day.getDay() === 0
										? 'text-[var(--color-attendance-absent-fg)]'
										: 'text-[var(--color-primary-500)]'
									: isCurrentMonth
										? 'text-[var(--color-nav-active)]'
										: 'text-[var(--color-nav-inactive)]'}"
					>
						{day.getDate()}
					</span>
					{#if holiday}
						<span
							class="truncate text-[8px] leading-tight text-[var(--color-attendance-absent-fg)] max-w-[40px]"
						>
							{holiday}
						</span>
					{/if}
				</div>

				<!-- イベントピル -->
				<div class="flex w-full flex-col gap-0.5 overflow-hidden">
					{#each dayEvents.slice(0, density) as ev (ev.id)}
						<EventPill
							event={ev}
							{pack}
							variant="compact"
							cancelled={isCancelled(ev)}
							onclick={oneventclick}
						/>
					{/each}
					{#if dayEvents.length > density}
						<span class="px-1 text-[9px] text-[var(--color-nav-inactive)]">
							+{dayEvents.length - density}
						</span>
					{/if}
				</div>
			</button>
		{/each}
	</div>
</div>
