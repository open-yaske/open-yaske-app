<script lang="ts">
	import { userDataStore, settingsStore } from '$lib/stores';
	import { m } from '$lib/paraglide/messages';
	import type { Period } from '$lib/types';
	import { CalendarDays, MapPin, User, ChevronRight } from '@lucide/svelte';
	import BentoCard from '../BentoCard.svelte';

	const days = ['sun', 'mon', 'tue', 'wed', 'thu', 'fri', 'sat'] as const;
	const todayDay = days[new Date().getDay()];

	const isWeekend = todayDay === 'sun' || todayDay === 'sat';
	const showDay = isWeekend
		? settingsStore.settings.timetableDayRange >= 6
			? todayDay
			: 'mon'
		: todayDay;

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

<BentoCard padding="p-4 md:p-5" class="w-full">
	{#snippet header()}
		<div
			class="flex w-full items-center justify-between border-b border-[var(--color-surface-border)]/60 pb-3"
		>
			<div class="flex items-center gap-2.5 font-bold text-[var(--color-nav-active)]">
				<div
					class="flex h-8 w-8 items-center justify-center rounded-xl bg-[var(--color-primary-500)]/15 text-[var(--color-primary-600)]"
				>
					<CalendarDays size={18} />
				</div>
				<span class="text-sm font-semibold">{m.nav_timetable()} ({dayLabel})</span>
			</div>
			<div class="flex items-center gap-2">
				{#if isWeekend && settingsStore.settings.timetableDayRange < 6}
					<span
						class="text-[10px] rounded-full bg-[var(--color-surface-muted)] px-2 py-0.5 font-medium text-[var(--color-nav-inactive)]"
					>
						{m.day_long_mon()}
					</span>
				{/if}
				<a
					href="/timetable"
					class="flex items-center gap-0.5 text-xs font-semibold text-[var(--color-primary-600)] hover:underline"
				>
					<span>{m.syllabus_detail()}</span>
					<ChevronRight size={14} />
				</a>
			</div>
		</div>
	{/snippet}

	{#if !hasClasses}
		<div class="py-8 text-center text-xs text-[var(--color-nav-inactive)]">
			{m.attendance_no_class_today()}
		</div>
	{:else}
		<div class="divide-y divide-[var(--color-surface-border)]/50 pt-2">
			{#each todayCourses as course (course.id)}
				<div class="flex items-start gap-4 py-3.5 first:pt-1 last:pb-1 group">
					<div class="flex flex-col items-center shrink-0 w-12">
						<span class="text-xs font-bold text-[var(--color-primary-600)]">
							{course.period}限
						</span>
						<span class="text-[10px] text-[var(--color-nav-inactive)] mt-0.5 font-mono">
							{getPeriodTime(course.period)}
						</span>
					</div>

					<div class="flex-1 min-w-0">
						<div class="flex items-center gap-2">
							<h4
								class="font-bold text-sm text-[var(--color-nav-active)] truncate group-hover:text-[var(--color-primary-600)] transition-colors"
							>
								{course.name}
							</h4>
							{#if course.type === 'required'}
								<span
									class="text-[9px] font-bold bg-[var(--color-danger-50)] text-[var(--color-danger-600)] rounded-md px-1.5 py-0.5 shrink-0"
								>
									{m.course_required()}
								</span>
							{:else}
								<span
									class="text-[9px] font-bold bg-[var(--color-surface-muted)] text-[var(--color-nav-inactive)] rounded-md px-1.5 py-0.5 shrink-0"
								>
									{course.type === 'elective' ? m.course_elective() : m.course_free()}
								</span>
							{/if}
						</div>

						<div
							class="flex flex-wrap gap-x-3 gap-y-1 mt-1.5 text-xs text-[var(--color-nav-inactive)]"
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
</BentoCard>
