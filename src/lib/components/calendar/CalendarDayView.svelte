<!--
  CalendarDayView.svelte
  1日のタイムライン表示。週表示と似ているが1カラム。
  終日イベントエリア + 時間グリッド + 時間指定イベント。
-->
<script lang="ts">
	import type { CalendarEvent } from '$lib/types';
	import type { ThemePack } from '$lib/theme';
	import { resolveColorHex } from '$lib/theme';
	import { isHoliday, getDayOfWeek } from '$lib/holidays';
	import { m } from '$lib/paraglide/messages';
	import EmptyState from '$lib/components/EmptyState.svelte';
	import { CalendarX2 } from '@lucide/svelte';
	import EventPill from './EventPill.svelte';

	interface Props {
		events: CalendarEvent[];
		date: Date;
		pack: ThemePack;
		showHolidays: boolean;
		ondayclick: (date: Date) => void;
		oneventclick: (event: CalendarEvent) => void;
	}

	let { events, date, pack, showHolidays = true, ondayclick, oneventclick }: Props = $props();

	const HOUR_START = 8;
	const HOUR_END = 21;
	const HOUR_HEIGHT = 64;
	const totalHours = HOUR_END - HOUR_START;

	const today = $derived(new Date());

	const dayKey = $derived(getDayOfWeek(date));
	const dayLabel = $derived(m[`day_long_${dayKey}`]());

	const isToday = $derived(
		date.getFullYear() === today.getFullYear() &&
			date.getMonth() === today.getMonth() &&
			date.getDate() === today.getDate()
	);

	const holiday = $derived(showHolidays && isHoliday(date));

	// 終日イベント
	const allDayEvents = $derived(
		events
			.filter((e) => {
				if (!e.allDay) return false;
				const start = new Date(e.startAt);
				const end = new Date(e.endAt);
				const dayStart = new Date(date.getFullYear(), date.getMonth(), date.getDate());
				const dayEnd = new Date(date.getFullYear(), date.getMonth(), date.getDate() + 1);
				return start <= dayEnd && end >= dayStart;
			})
			.sort((a, b) => a.startAt - b.startAt)
	);

	// 時間指定イベント
	const timedEvents = $derived(
		events
			.filter((e) => {
				if (e.allDay) return false;
				const start = new Date(e.startAt);
				const end = new Date(e.endAt);
				const dayStart = new Date(date.getFullYear(), date.getMonth(), date.getDate());
				const dayEnd = new Date(date.getFullYear(), date.getMonth(), date.getDate() + 1);
				return start < dayEnd && end > dayStart;
			})
			.sort((a, b) => a.startAt - b.startAt)
	);

	const hasAnyEvents = $derived(allDayEvents.length > 0 || timedEvents.length > 0);

	function getEventPosition(event: CalendarEvent) {
		const dayStart = new Date(date.getFullYear(), date.getMonth(), date.getDate());
		const dayEnd = new Date(date.getFullYear(), date.getMonth(), date.getDate() + 1);
		const eventStart = new Date(Math.max(event.startAt, dayStart.getTime()));
		const eventEnd = new Date(Math.min(event.endAt, dayEnd.getTime()));

		const startHour = eventStart.getHours() + eventStart.getMinutes() / 60;
		const endHour = eventEnd.getHours() + eventEnd.getMinutes() / 60;

		const top = Math.max(0, (startHour - HOUR_START) * HOUR_HEIGHT);
		const height = Math.max(24, (endHour - startHour) * HOUR_HEIGHT);

		return { top, height };
	}

	const hourLabels = $derived(Array.from({ length: totalHours + 1 }, (_, i) => HOUR_START + i));

	function formatHour(h: number): string {
		return `${h.toString().padStart(2, '0')}:00`;
	}

	function formatTime(ts: number): string {
		const d = new Date(ts);
		return `${d.getHours().toString().padStart(2, '0')}:${d.getMinutes().toString().padStart(2, '0')}`;
	}

	type EventDetail = {
		event: CalendarEvent;
		top: number;
		bottom: number;
		height: number;
		col: number;
	};

	function getPositionedEvents() {
		if (timedEvents.length === 0) return [];

		// 1. 各イベントの top, height を計算
		const details: EventDetail[] = timedEvents.map((ev) => {
			const pos = getEventPosition(ev);
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
				const style = `top: ${item.top}px; height: ${item.height}px; left: calc(${left}% + 4px); width: calc(${pctWidth}% - 8px);`;
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
	<!-- 日付ヘッダー -->
	<div
		class="flex items-center justify-between border-b border-[var(--color-surface-border)] px-4 py-3"
	>
		<button type="button" class="flex flex-col items-start" onclick={() => ondayclick(date)}>
			<span class="text-xs text-[var(--color-nav-inactive)]">{dayLabel}</span>
			<span class="flex items-baseline gap-2">
				<span
					class="text-2xl font-bold
					{isToday
						? 'text-[var(--color-primary-500)]'
						: holiday
							? 'text-[var(--color-attendance-absent-fg)]'
							: 'text-[var(--color-nav-active)]'}"
				>
					{date.getDate()}
				</span>
				<span class="text-sm text-[var(--color-nav-inactive)]">
					{date.getFullYear()}/{(date.getMonth() + 1).toString().padStart(2, '0')}
				</span>
			</span>
			{#if holiday}
				<span class="text-xs text-[var(--color-attendance-absent-fg)]">祝日</span>
			{/if}
		</button>
	</div>

	<!-- 終日イベントエリア -->
	{#if allDayEvents.length > 0}
		<div class="grid grid-cols-[44px_1fr] border-b border-[var(--color-surface-border)]">
			<div
				class="border-r border-[var(--color-surface-border)] flex items-start justify-center pt-1"
			>
				<span class="text-[9px] text-[var(--color-nav-inactive)]">終日</span>
			</div>
			<div class="flex flex-col gap-0.5 p-1">
				{#each allDayEvents as ev (ev.id)}
					<EventPill event={ev} {pack} variant="full" onclick={oneventclick} />
				{/each}
			</div>
		</div>
	{/if}

	{#if !hasAnyEvents}
		<EmptyState
			icon={CalendarX2}
			message={m.calendar_empty()}
			submessage={dayLabel}
			class="py-16"
		/>
	{:else}
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

			<!-- タイムライン -->
			<div class="relative {isToday ? 'bg-[var(--color-primary-50)]/20' : ''}">
				<!-- 時間ライン -->
				{#each hourLabels.slice(0, totalHours) as h (h)}
					<div
						class="border-b border-[var(--color-surface-border)]"
						style="height: {HOUR_HEIGHT}px;"
					></div>
				{/each}

				<!-- 現在時刻ライン -->
				{#if isToday}
					{@const now = new Date()}
					{@const nowHour = now.getHours() + now.getMinutes() / 60}
					{#if nowHour >= HOUR_START && nowHour <= HOUR_END}
						<div
							class="pointer-events-none absolute left-0 right-0 z-20 flex items-center"
							style="top: {(nowHour - HOUR_START) * HOUR_HEIGHT}px;"
						>
							<span class="h-2 w-2 rounded-full bg-[var(--color-attendance-absent-fg)]"></span>
							<div class="h-px flex-1 bg-[var(--color-attendance-absent-fg)]"></div>
						</div>
					{/if}
				{/if}

				<!-- 時間指定イベント -->
				{#each getPositionedEvents() as { event: ev, style, height } (ev.id)}
					{@const colorHex = resolveColorHex(pack, ev.color)}
					{@const isTransparent = colorHex === 'transparent'}
					<button
						type="button"
						class="absolute z-10 overflow-hidden rounded-lg px-2 py-1.5 text-left text-xs leading-tight shadow-sm transition-transform hover:z-20 hover:scale-[1.01]"
						style="{style} background-color: {isTransparent
							? 'var(--color-primary-50)'
							: colorHex + '1a'}; border-left: 4px solid {isTransparent
							? 'var(--color-primary-500)'
							: colorHex};"
						onclick={(e) => {
							e.stopPropagation();
							oneventclick(ev);
						}}
					>
						<div class="truncate font-medium text-[var(--color-nav-active)]">{ev.title}</div>
						{#if height > 36}
							<div class="truncate text-[10px] text-[var(--color-nav-inactive)]">
								{formatTime(ev.startAt)} - {formatTime(ev.endAt)}
							</div>
						{/if}
						{#if ev.location && height > 56}
							<div class="truncate text-[10px] text-[var(--color-nav-inactive)]">
								{ev.location}
							</div>
						{/if}
					</button>
				{/each}
			</div>
		</div>
	{/if}
</div>
