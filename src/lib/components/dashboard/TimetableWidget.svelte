<script lang="ts">
	import { userDataStore, settingsStore } from '$lib/stores';
	import { m } from '$lib/paraglide/messages';
	import type { Period } from '$lib/types';
	import { CalendarDays, MapPin, User } from '@lucide/svelte';

	// 今日の曜日を取得
	const days = ['sun', 'mon', 'tue', 'wed', 'thu', 'fri', 'sat'] as const;
	const todayDay = days[new Date().getDay()];

	// 今日が週末（土・日）の場合は、設定の timetableDayRange を見て、表示対象日であればそのまま表示、そうでなければ月曜日のプレビューを表示するか、休みを表示
	const isWeekend = todayDay === 'sun' || todayDay === 'sat';
	const showDay = isWeekend
		? settingsStore.settings.timetableDayRange >= 6
			? todayDay
			: 'mon'
		: todayDay;

	// 今日の授業をフィルタリングして時限順にソート
	const todayCourses = $derived(
		userDataStore.courses.filter((c) => c.day === showDay).sort((a, b) => a.period - b.period)
	);

	function getPeriodTime(p: Period) {
		const time = settingsStore.settings.timetablePeriods[p];
		return time ? `${time.start} - ${time.end}` : '';
	}

	const hasClasses = $derived(todayCourses.length > 0);
	const dayLabel = $derived(
		showDay === 'mon'
			? m.day_long_mon()
			: showDay === 'tue'
				? m.day_long_tue()
				: showDay === 'wed'
					? m.day_long_wed()
					: showDay === 'thu'
						? m.day_long_thu()
						: showDay === 'fri'
							? m.day_long_fri()
							: showDay === 'sat'
								? m.day_long_sat()
								: m.day_long_sun()
	);
</script>

<div
	class="rounded-card border border-[var(--color-surface-border)] bg-[var(--color-surface-card)] p-4"
>
	<div
		class="mb-4 flex items-center justify-between border-b border-[var(--color-surface-border)] pb-2"
	>
		<div class="flex items-center gap-2 font-bold text-[var(--color-nav-active)]">
			<CalendarDays size={18} class="text-[var(--color-primary-500)]" />
			<span>{m.nav_timetable()} ({dayLabel})</span>
		</div>
		{#if isWeekend && settingsStore.settings.timetableDayRange < 6}
			<span
				class="text-[10px] rounded bg-[var(--color-surface-muted)] px-1.5 py-0.5 text-[var(--color-nav-inactive)]"
			>
				月曜の予定を表示
			</span>
		{/if}
	</div>

	{#if !hasClasses}
		<div class="py-6 text-center text-xs text-[var(--color-nav-inactive)]">
			{m.attendance_no_class_today()}
		</div>
	{:else}
		<div class="divide-y divide-[var(--color-surface-border)]">
			{#each todayCourses as course (course.id)}
				<div class="flex items-start gap-4 py-3 first:pt-0 last:pb-0 group">
					<div class="flex flex-col items-center shrink-0 w-12">
						<span class="text-sm font-bold text-[var(--color-primary-600)]">
							{course.period}限
						</span>
						<span class="text-[10px] text-[var(--color-nav-inactive)] mt-0.5 font-mono">
							{getPeriodTime(course.period)}
						</span>
					</div>

					<div class="flex-1 min-w-0">
						<div class="flex items-center gap-2">
							<h4
								class="font-bold text-sm text-[var(--color-nav-active)] truncate group-hover:text-[var(--color-primary-500)] transition-colors"
							>
								{course.name}
							</h4>
							{#if course.type === 'required'}
								<span
									class="text-[9px] font-bold bg-[var(--color-danger-50)] text-[var(--color-danger-600)] rounded px-1 shrink-0"
								>
									{m.course_required()}
								</span>
							{:else}
								<span
									class="text-[9px] font-bold bg-[var(--color-surface-muted)] text-[var(--color-nav-inactive)] rounded px-1 shrink-0"
								>
									{course.type === 'elective' ? m.course_elective() : m.course_free()}
								</span>
							{/if}
						</div>

						<div
							class="flex flex-wrap gap-x-3 gap-y-1 mt-2 text-xs text-[var(--color-nav-inactive)]"
						>
							{#if course.classroom}
								<div class="flex items-center gap-1">
									<MapPin size={12} />
									<span class="truncate">{course.classroom}</span>
								</div>
							{/if}
							{#if course.teacher}
								<div class="flex items-center gap-1">
									<User size={12} />
									<span class="truncate">{course.teacher}</span>
								</div>
							{/if}
						</div>
					</div>
				</div>
			{/each}
		</div>
	{/if}
</div>
