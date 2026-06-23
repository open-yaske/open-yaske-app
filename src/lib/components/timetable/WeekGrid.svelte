<!--
  WeekGrid.svelte
  時間割の曜日×時限グリッド。
  - 列: settings.timetableDayRange (5=月-金, 6=+土, 7=+日) に基づく曜日
  - 行: settings.timetablePeriodRange (5-7) に基づく時限
  - 空セル: タップ可能。透過設定が有効な場合は半透明。
  - 授業セル: 複数時限にまたがる (span) 授業は CSS Grid の span を使って表示。
-->
<script lang="ts">
	import { m } from '$lib/paraglide/messages';
	import { DAY_OF_WEEK_LIST, PERIOD_LIST } from '$lib/types';
	import type { Course, DayOfWeek, Period } from '$lib/types';
	import { themeStore, settingsStore } from '$lib/stores';
	import { SvelteMap, SvelteSet } from 'svelte/reactivity';
	import { resolveColorHex } from '$lib/theme';

	interface Props {
		courses: Course[];
		dayRange: 5 | 6 | 7;
		periodRange: 4 | 5 | 6 | 7;
		oncelltap: (day: DayOfWeek, period: Period) => void;
		oncoursetap: (course: Course) => void;
	}

	let { courses, dayRange, periodRange, oncelltap, oncoursetap }: Props = $props();

	const days = $derived(DAY_OF_WEEK_LIST.slice(0, dayRange));
	const periods = $derived(PERIOD_LIST.slice(0, periodRange));

	// 曜日ラベル: day_mon -> "月"
	function dayLabel(day: DayOfWeek): string {
		const key = `day_${day}` as keyof typeof m;
		return (m[key] as () => string)();
	}

	// 授業マップと占有済みマッピング
	const cellData = $derived.by(() => {
		const startMap = new SvelteMap<string, Course>();
		const occupiedSet = new SvelteSet<string>();
		for (const course of courses) {
			const startKey = `${course.day}-${course.period}`;
			startMap.set(startKey, course);
			for (let p = course.period + 1; p <= course.period + course.span - 1; p++) {
				occupiedSet.add(`${course.day}-${p}`);
			}
		}
		return { startMap, occupiedSet };
	});

	function cellKey(day: DayOfWeek, period: Period): string {
		return `${day}-${period}`;
	}

	function resolveColor(course: Course): string {
		return resolveColorHex(themeStore.pack, course.color);
	}

	// 列の幅定義（時間ラベル列 + 曜日列）
	const gridTemplateColumns = $derived(
		`minmax(1.75rem, 2rem) repeat(${dayRange}, minmax(5.25rem, 1fr))`
	);
</script>

<div class="overflow-x-auto px-4">
	<div
		class="grid w-max min-w-full gap-1 bg-transparent p-1"
		style="grid-template-columns: {gridTemplateColumns};"
	>
		<!-- ヘッダー行: 左上角 + 曜日名 -->
		<div class="pb-1" style="grid-column: 1; grid-row: 1;"></div>
		{#each days as day, d (day)}
			<div
				class="flex items-end justify-center pb-2 text-center text-xs font-bold text-[var(--color-nav-active)]"
				style="grid-column: {d + 2}; grid-row: 1;"
			>
				{dayLabel(day)}
			</div>
		{/each}

		<!-- ボディ: 各時限の時間ラベル + 各セルの描画 -->
		{#each periods as period, p (period)}
			<!-- 時限・時間ラベル -->
			<div
				class="flex items-center justify-center py-2 text-center"
				style="grid-column: 1; grid-row: {p + 2};"
			>
				<span class="text-xs font-extrabold text-[var(--color-nav-active)]">{period}</span>
			</div>

			{#each days as day, d (day)}
				{@const key = cellKey(day, period)}
				{@const course = cellData.startMap.get(key)}
				{@const occupied = cellData.occupiedSet.has(key)}

				{#if course}
					{@const color = resolveColor(course)}
					{@const isDarkTheme = themeStore.pack.id === 'dark'}
					<!-- 授業カード: 複数時限にまたがる (span) -->
					<div
						class="m-0.5 flex min-h-[64px] cursor-pointer flex-col gap-1 rounded-xl p-2.5 transition-all hover:scale-[0.99] active:scale-[0.97]"
						style="background-color: {color}; grid-column: {d + 2}; grid-row: {p +
							2} / span {course.span};"
						class:text-white={!isDarkTheme}
						class:text-slate-900={isDarkTheme}
						class:shadow-sm={!settingsStore.settings.timetableBgImage}
						class:opacity-95={settingsStore.settings.timetableTransparent}
						role="button"
						tabindex="0"
						onclick={() => oncoursetap(course)}
						onkeydown={(e) => {
							if (e.key === 'Enter' || e.key === ' ') {
								e.preventDefault();
								oncoursetap(course);
							}
						}}
					>
						<p class="line-clamp-3 text-xs font-bold leading-tight">
							{course.name}
						</p>
						{#if course.classroom}
							<span
								class="mt-auto inline-block w-fit rounded-full px-1.5 py-0.5 text-[9px] font-bold {isDarkTheme
									? 'bg-black/10 text-slate-800'
									: 'bg-white/20 text-white'}"
							>
								{course.classroom}
							</span>
						{/if}
					</div>
				{:else if !occupied}
					<!-- 空セル: タップして追加 -->
					<div
						class="m-0.5 flex min-h-[64px] cursor-pointer items-center justify-center rounded-xl border border-dashed transition-all hover:scale-[0.99] active:scale-[0.97] {settingsStore
							.settings.timetableTransparent
							? 'border-[var(--color-surface-muted)] bg-[var(--color-surface-card)]/40 backdrop-blur-xs hover:bg-[var(--color-surface-muted)]/60'
							: 'border-[var(--color-surface-border)] bg-[var(--color-surface-card)] hover:bg-[var(--color-surface-muted)]'}"
						style="grid-column: {d + 2}; grid-row: {p + 2};"
						role="button"
						tabindex="0"
						onclick={() => oncelltap(day, period)}
						onkeydown={(e) => {
							if (e.key === 'Enter' || e.key === ' ') {
								e.preventDefault();
								oncelltap(day, period);
							}
						}}
					>
						<span
							class="text-sm font-medium transition-colors {settingsStore.settings
								.timetableTransparent
								? 'text-[var(--color-nav-inactive)]'
								: 'text-[var(--color-surface-border)]'}">+</span
						>
					</div>
				{/if}
			{/each}
		{/each}
	</div>
</div>
