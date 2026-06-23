<!--
  CalendarWeekView.svelte
  7列の週表示。タイム軸（08:00-20:00）+ 終日イベントエリア + 時間指定イベント。
  イベントは開始/終了時刻で絶対配置される。
-->
<script lang="ts">
	import type { CalendarEvent } from '$lib/types';
	import type { ThemePack } from '$lib/theme';
	import { resolveColorHex } from '$lib/theme';
	import { isHoliday } from '$lib/holidays';
	import { m } from '$lib/paraglide/messages';
	import EventPill from './EventPill.svelte';

	interface Props {
		events: CalendarEvent[];
		weekStartDate: Date;
		weekStart: 'sun' | 'mon';
		pack: ThemePack;
		showHolidays: boolean;
		ondayclick: (date: Date) => void;
		oneventclick: (event: CalendarEvent) => void;
	}

	let {
		events,
		weekStartDate,
		weekStart,
		pack,
		showHolidays = true,
		ondayclick,
		oneventclick
	}: Props = $props();

	const HOUR_START = 8;
	const HOUR_END = 21;
	const HOUR_HEIGHT = 56; // px per hour
	const totalHours = HOUR_END - HOUR_START;

	const today = $derived(new Date());

	const weekLabels = $derived(
		weekStart === 'sun'
			? [m.day_sun(), m.day_mon(), m.day_tue(), m.day_wed(), m.day_thu(), m.day_fri(), m.day_sat()]
			: [m.day_mon(), m.day_tue(), m.day_wed(), m.day_thu(), m.day_fri(), m.day_sat(), m.day_sun()]
	);

	// 週の7日を生成
	const weekDays = $derived.by(() => {
		const days: Date[] = [];
		for (let i = 0; i < 7; i++) {
			days.push(
				new Date(weekStartDate.getFullYear(), weekStartDate.getMonth(), weekStartDate.getDate() + i)
			);
		}
		return days;
	});

	function isSameDay(a: Date, b: Date): boolean {
		return (
			a.getFullYear() === b.getFullYear() &&
			a.getMonth() === b.getMonth() &&
			a.getDate() === b.getDate()
		);
	}

	// 終日イベントを取得
	function getAllDayEventsForDay(date: Date): CalendarEvent[] {
		return events
			.filter((e) => {
				if (!e.allDay) return false;
				const start = new Date(e.startAt);
				const end = new Date(e.endAt);
				const dayStart = new Date(date.getFullYear(), date.getMonth(), date.getDate());
				const dayEnd = new Date(date.getFullYear(), date.getMonth(), date.getDate() + 1);
				return start <= dayEnd && end >= dayStart;
			})
			.sort((a, b) => a.startAt - b.startAt);
	}

	// 時間指定イベントを取得（当日のみ、終日以外）
	function getTimedEventsForDay(date: Date): CalendarEvent[] {
		return events
			.filter((e) => {
				if (e.allDay) return false;
				const start = new Date(e.startAt);
				const end = new Date(e.endAt);
				const dayStart = new Date(date.getFullYear(), date.getMonth(), date.getDate());
				const dayEnd = new Date(date.getFullYear(), date.getMonth(), date.getDate() + 1);
				return start < dayEnd && end > dayStart;
			})
			.sort((a, b) => a.startAt - b.startAt);
	}

	// イベントの時間グリッド上の位置を計算
	function getEventPosition(event: CalendarEvent, dayDate: Date) {
		const dayStart = new Date(dayDate.getFullYear(), dayDate.getMonth(), dayDate.getDate());
		const eventStart = new Date(Math.max(event.startAt, dayStart.getTime()));
		const dayEnd = new Date(dayDate.getFullYear(), dayDate.getMonth(), dayDate.getDate() + 1);
		const eventEnd = new Date(Math.min(event.endAt, dayEnd.getTime()));

		const startHour = eventStart.getHours() + eventStart.getMinutes() / 60;
		const endHour = eventEnd.getHours() + eventEnd.getMinutes() / 60;

		const top = Math.max(0, (startHour - HOUR_START) * HOUR_HEIGHT);
		const height = Math.max(20, (endHour - startHour) * HOUR_HEIGHT);

		return { top, height };
	}

	// 時間ラベルを生成
	const hourLabels = $derived(Array.from({ length: totalHours + 1 }, (_, i) => HOUR_START + i));

	function formatHour(h: number): string {
		return `${h.toString().padStart(2, '0')}:00`;
	}

	type EventDetail = {
		event: CalendarEvent;
		top: number;
		bottom: number;
		height: number;
		col: number;
	};

	function getPositionedEvents(dayDate: Date) {
		const timed = getTimedEventsForDay(dayDate);
		if (timed.length === 0) return [];

		// 1. 各イベントの top, height を計算
		const details: EventDetail[] = timed.map((ev) => {
			const pos = getEventPosition(ev, dayDate);
			return {
				event: ev,
				top: pos.top,
				bottom: pos.top + pos.height,
				height: pos.height,
				col: 0
			};
		});

		// 開始時間順にソート
		details.sort((a, b) => a.top - b.top);

		// 重なり合うグループを分類
		const groups: EventDetail[][] = [];
		for (const detail of details) {
			let placed = false;
			for (const group of groups) {
				const overlaps = group.some((item) => {
					return detail.top < item.bottom && detail.bottom > item.top;
				});
				if (overlaps) {
					group.push(detail);
					placed = true;
					break;
				}
			}
			if (!placed) {
				groups.push([detail]);
			}
		}

		const result: { event: CalendarEvent; style: string; top: number; height: number }[] = [];

		// グループごとにカラムを割り振る
		for (const group of groups) {
			const columns: EventDetail[][] = [];
			for (const item of group) {
				let colIndex = 0;
				while (true) {
					if (!columns[colIndex]) {
						columns[colIndex] = [];
					}
					const hasOverlap = columns[colIndex].some((cItem) => {
						return item.top < cItem.bottom && item.bottom > cItem.top;
					});
					if (!hasOverlap) {
						columns[colIndex].push(item);
						break;
					}
					colIndex++;
				}
				item.col = colIndex;
			}

			const numCols = columns.length;
			for (const item of group) {
				const pctWidth = 100 / numCols;
				const left = pctWidth * item.col;
				// スタイル文字列を作成
				const style = `top: ${item.top}px; height: ${item.height}px; left: calc(${left}% + 2px); width: calc(${pctWidth}% - 4px);`;
				result.push({
					event: item.event,
					style,
					top: item.top,
					height: item.height
				});
			}
		}

		return result;
	}
</script>

<div class="flex flex-col">
	<!-- 曜日ヘッダー -->
	<div class="grid grid-cols-[44px_1fr] border-b border-[var(--color-surface-border)]">
		<div class="border-r border-[var(--color-surface-border)]"></div>
		<div class="grid grid-cols-7">
			{#each weekDays as day, i (i)}
				{@const isToday = isSameDay(day, today)}
				{@const holiday = showHolidays && isHoliday(day)}
				<button
					type="button"
					class="flex flex-col items-center gap-0.5 py-2 transition-colors hover:bg-[var(--color-surface-muted)]"
					onclick={() => ondayclick(day)}
				>
					<span class="text-[10px] font-medium text-[var(--color-nav-inactive)]">
						{weekLabels[i]}
					</span>
					<span
						class="flex h-7 w-7 items-center justify-center rounded-full text-sm font-medium
						{isToday
							? 'bg-[var(--color-primary-500)] text-white'
							: holiday
								? 'text-[var(--color-attendance-absent-fg)]'
								: 'text-[var(--color-nav-active)]'}"
					>
						{day.getDate()}
					</span>
				</button>
			{/each}
		</div>
	</div>

	<!-- 終日イベントエリア -->
	<div class="grid grid-cols-[44px_1fr] border-b border-[var(--color-surface-border)]">
		<div class="border-r border-[var(--color-surface-border)] flex items-start justify-center pt-1">
			<span class="text-[9px] text-[var(--color-nav-inactive)]">終日</span>
		</div>
		<div class="grid grid-cols-7">
			{#each weekDays as day, i (i)}
				<div
					class="min-h-[28px] border-r border-[var(--color-surface-border)] p-0.5 last:border-r-0"
				>
					{#each getAllDayEventsForDay(day) as ev (ev.id)}
						<EventPill event={ev} {pack} variant="compact" onclick={oneventclick} />
					{/each}
				</div>
			{/each}
		</div>
	</div>

	<!-- 時間グリッド + イベント -->
	<div class="relative grid grid-cols-[44px_1fr] overflow-y-auto" style="max-height: 60vh;">
		<!-- 時間軸ラベル -->
		<div class="border-r border-[var(--color-surface-border)]">
			{#each hourLabels as h (h)}
				<div
					class="relative text-right pr-1 text-[10px] text-[var(--color-nav-inactive)]"
					style="height: {HOUR_HEIGHT}px;"
				>
					<span class="absolute -top-1.5 right-1">{formatHour(h)}</span>
				</div>
			{/each}
		</div>

		<!-- 7日分のカラム -->
		<div class="relative grid grid-cols-7">
			{#each weekDays as day, dayIdx (dayIdx)}
				{@const isToday = isSameDay(day, today)}
				<div
					class="relative border-r border-[var(--color-surface-border)] last:border-r-0 {isToday
						? 'bg-[var(--color-primary-50)]/30'
						: ''}"
				>
					<!-- 時間ライン -->
					{#each hourLabels.slice(0, totalHours) as h (h)}
						<div
							class="border-b border-[var(--color-surface-border)]"
							style="height: {HOUR_HEIGHT}px;"
						></div>
					{/each}

					<!-- 時間指定イベント -->
					{#each getPositionedEvents(day) as { event: ev, style, height } (ev.id)}
						{@const colorHex = resolveColorHex(pack, ev.color)}
						{@const isTransparent = colorHex === 'transparent'}
						<button
							type="button"
							class="absolute z-10 overflow-hidden rounded-md px-1.5 py-1 text-left text-[10px] leading-tight shadow-sm transition-transform hover:z-20 hover:scale-[1.02]"
							style="{style} background-color: {isTransparent
								? 'var(--color-primary-50)'
								: colorHex + '1a'}; border-left: 3px solid {isTransparent
								? 'var(--color-primary-500)'
								: colorHex};"
							onclick={(e) => {
								e.stopPropagation();
								oneventclick(ev);
							}}
						>
							<div class="truncate font-medium text-[var(--color-nav-active)]">{ev.title}</div>
							{#if height > 30}
								<div class="truncate text-[9px] text-[var(--color-nav-inactive)]">
									{new Date(ev.startAt).getHours().toString().padStart(2, '0')}:{new Date(
										ev.startAt
									)
										.getMinutes()
										.toString()
										.padStart(2, '0')}
								</div>
							{/if}
						</button>
					{/each}
				</div>
			{/each}
		</div>
	</div>
</div>
