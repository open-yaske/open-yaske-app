<script lang="ts">
	import { userDataStore, settingsStore } from '$lib/stores';
	import { calculateRate, isDangerous } from '$lib/attendance';
	import { m } from '$lib/paraglide/messages';
	import ProgressRing from '../ProgressRing.svelte';
	import { BarChart3, AlertTriangle, CheckCircle2 } from '@lucide/svelte';

	const maxAbsences = $derived(settingsStore.settings.attendanceMaxAbsences || 5);

	// 総合出席サマリー
	const overallSummary = $derived(calculateRate(userDataStore.attendance, maxAbsences));

	// 各科目の出席サマリーを計算し、危険（80%未満）または欠席制限が近いものを判定
	type DangerCourse = {
		name: string;
		rate: number;
		absent: number;
		remaining: number;
	};

	const dangerCourses = $derived.by<DangerCourse[]>(() => {
		const courses = userDataStore.courses;
		const attendance = userDataStore.attendance;
		const list: DangerCourse[] = [];

		for (const c of courses) {
			const cRecords = attendance.filter((r) => r.courseId === c.id);
			if (cRecords.length === 0) continue;

			const summary = calculateRate(cRecords, maxAbsences);
			// 欠席が危険水準、または出席率が80%未満
			if (isDangerous(summary, 80) || summary.remainingAbsences <= 1) {
				list.push({
					name: c.name,
					rate: summary.rate,
					absent: summary.absent,
					remaining: summary.remainingAbsences
				});
			}
		}
		return list;
	});
</script>

<div
	class="rounded-card border border-[var(--color-surface-border)] bg-[var(--color-surface-card)] p-4"
>
	<div
		class="mb-4 flex items-center justify-between border-b border-[var(--color-surface-border)] pb-2"
	>
		<div class="flex items-center gap-2 font-bold text-[var(--color-nav-active)]">
			<BarChart3 size={18} class="text-[var(--color-primary-500)]" />
			<span>{m.nav_attendance()}</span>
		</div>
		<a href="/attendance" class="text-xs text-[var(--color-primary-600)] hover:underline">
			{m.action_more()}
		</a>
	</div>

	{#if userDataStore.attendance.length === 0}
		<div class="py-6 text-center text-xs text-[var(--color-nav-inactive)]">
			{m.empty_no_attendance()}
		</div>
	{:else}
		<div class="flex flex-col gap-4 sm:flex-row sm:items-center">
			<!-- 総合グラフ -->
			<div
				class="flex items-center justify-center gap-4 shrink-0 sm:border-r sm:border-[var(--color-surface-border)] sm:pr-6"
			>
				<ProgressRing
					value={overallSummary.rate}
					size={80}
					strokeWidth={8}
					color={overallSummary.rate < 80 ? '--color-danger-500' : '--color-success-500'}
				>
					<span class="text-sm font-bold text-[var(--color-nav-active)] font-mono">
						{Math.round(overallSummary.rate)}%
					</span>
				</ProgressRing>
				<div class="flex flex-col">
					<span class="text-xs font-semibold text-[var(--color-nav-inactive)]">
						{m.attendance_rate()}
					</span>
					<span class="text-[10px] text-[var(--color-nav-inactive)] mt-1">
						{m.attendance_present()}: {overallSummary.present} /
						{m.attendance_absent()}: {overallSummary.absent} /
						{m.attendance_late()}: {overallSummary.late}
					</span>
				</div>
			</div>

			<!-- 危険な授業リストまたは安全メッセージ -->
			<div class="flex-1 min-w-0">
				{#if dangerCourses.length === 0}
					<div
						class="flex items-center gap-2 rounded-card bg-[var(--color-success-50)] p-3 text-xs text-[var(--color-success-700)]"
					>
						<CheckCircle2 size={16} class="shrink-0 text-[var(--color-success-500)]" />
						<span class="font-medium">すべての科目で良好な出席状態を維持しています</span>
					</div>
				{:else}
					<div class="space-y-2">
						<div class="flex items-center gap-1.5 text-xs font-bold text-[var(--color-danger-600)]">
							<AlertTriangle size={14} />
							<span>出席注意の科目 ({dangerCourses.length})</span>
						</div>
						<ul class="space-y-1.5 max-h-24 overflow-y-auto pr-1">
							{#each dangerCourses as dc}
								<li
									class="flex items-center justify-between text-xs border border-[var(--color-surface-border)] bg-[var(--color-surface-muted)] rounded px-2.5 py-1"
								>
									<span
										class="font-bold text-[var(--color-nav-active)] truncate max-w-[120px] sm:max-w-[180px]"
									>
										{dc.name}
									</span>
									<span class="font-mono font-semibold shrink-0 text-[var(--color-danger-600)]">
										{Math.round(dc.rate)}% (欠席 {dc.absent}回 / 残り {dc.remaining}回)
									</span>
								</li>
							{/each}
						</ul>
					</div>
				{/if}
			</div>
		</div>
	{/if}
</div>
