<script lang="ts">
	import { m } from '$lib/paraglide/messages';
	import { userDataStore, settingsStore } from '$lib/stores';
	import PageHeader from '$lib/components/PageHeader.svelte';
	import Container from '$lib/components/Container.svelte';
	import EmptyState from '$lib/components/EmptyState.svelte';
	import AttendanceCard from '$lib/components/attendance/AttendanceCard.svelte';
	import IconButton from '$lib/components/IconButton.svelte';
	import SectionHeader from '$lib/components/SectionHeader.svelte';
	import { ClipboardCheck, Sun, Moon, Laptop } from '@lucide/svelte';

	const courses = $derived(userDataStore.courses);
	const attendanceRecords = $derived(userDataStore.attendance);

	const stats = $derived.by(() => {
		const totalRegistered = courses.length;
		let recordedCount = 0;
		let totalRateSum = 0;
		let rateCount = 0;
		let dangerCount = 0;

		for (const course of courses) {
			const courseRecords = attendanceRecords.filter((r) => r.courseId === course.id);
			if (courseRecords.length > 0) {
				recordedCount++;
				const present = courseRecords.filter((r) => r.status === 'present').length;
				const absent = courseRecords.filter((r) => r.status === 'absent').length;
				const late = courseRecords.filter((r) => r.status === 'late').length;
				const excused = courseRecords.filter((r) => r.status === 'excused').length;

				const denominator = present + absent + late + excused;
				if (denominator > 0) {
					const rate = ((present + late * 0.5 + excused) / denominator) * 100;
					totalRateSum += rate;
					rateCount++;
					// もし残り欠席可能数が2回以下、もしくは出席率が70%未満なら危険
					if (rate < 70) {
						dangerCount++;
					}
				}
			}
		}

		const averageRate = rateCount > 0 ? (totalRateSum / rateCount).toFixed(1) : '0.0';

		return {
			totalRegistered,
			recordedCount,
			averageRate,
			dangerCount
		};
	});

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

<svelte:head>
	<title>{m.nav_attendance()} | OpenYASKE</title>
</svelte:head>

<PageHeader title={m.nav_attendance()} />

<Container class="pb-16 max-w-5xl">
	{#if courses.length === 0}
		<EmptyState icon={ClipboardCheck} message={m.empty_no_courses()} submessage={m.course_add()} />
	{:else}
		<div class="space-y-6 py-4">
			<!-- Statistics Hero Section -->
			<div class="space-y-3">
				<SectionHeader>{m.nav_attendance()}</SectionHeader>
				<div
					class="rounded-3xl border border-[var(--color-surface-border)]/60 bg-[var(--color-surface-card)]/90 backdrop-blur-2xl p-6 shadow-apple-md space-y-5"
				>
					<div class="flex flex-col sm:flex-row items-center justify-between gap-6">
						<!-- 出席率スコア & プログレスバー -->
						<div class="flex-1 space-y-2 w-full">
							<div class="flex items-baseline justify-between">
								<span
									class="text-xs font-extrabold uppercase tracking-wider text-[var(--color-nav-inactive)]"
									>{m.attendance_rate()}</span
								>
								<span class="text-3xl font-black text-[var(--color-primary-600)]"
									>{stats.averageRate}%</span
								>
							</div>
							<div
								class="h-3 w-full rounded-full bg-[var(--color-surface-muted)] overflow-hidden p-0.5 border border-[var(--color-surface-border)]/40"
							>
								<div
									class="h-full rounded-full bg-gradient-to-r from-[var(--color-primary-500)] to-[var(--color-primary-600)] transition-all duration-500"
									style:width="{Math.min(100, Math.max(0, Number(stats.averageRate)))}%"
								></div>
							</div>
						</div>

						<!-- ミニ統計グリッド -->
						<div class="grid grid-cols-3 gap-3 shrink-0 w-full sm:w-auto">
							<div
								class="bg-[var(--color-surface-muted)]/80 p-3 rounded-2xl text-center space-y-0.5 border border-[var(--color-surface-border)]/40"
							>
								<span class="text-[10px] font-bold text-[var(--color-nav-inactive)] block"
									>{m.nav_timetable()}</span
								>
								<span class="text-xl font-black text-[var(--color-nav-active)]"
									>{stats.totalRegistered}</span
								>
							</div>
							<div
								class="bg-[var(--color-surface-muted)]/80 p-3 rounded-2xl text-center space-y-0.5 border border-[var(--color-surface-border)]/40"
							>
								<span class="text-[10px] font-bold text-[var(--color-nav-inactive)] block"
									>{m.attendance_marked()}</span
								>
								<span class="text-xl font-black text-[var(--color-nav-active)]"
									>{stats.recordedCount}</span
								>
							</div>
							<div
								class="bg-[var(--color-attendance-absent-bg)]/60 p-3 rounded-2xl text-center space-y-0.5 border border-[var(--color-attendance-absent-fg)]/20"
							>
								<span class="text-[10px] font-bold text-[var(--color-attendance-absent-fg)] block"
									>{m.attendance_danger()}</span
								>
								<span class="text-xl font-black text-[var(--color-attendance-absent-fg)]"
									>{stats.dangerCount}</span
								>
							</div>
						</div>
					</div>
				</div>
			</div>

			<!-- Course List Section -->
			<div class="space-y-3">
				<SectionHeader>授業一覧</SectionHeader>
				<div class="grid grid-cols-1 md:grid-cols-2 gap-4">
					{#each courses as course (course.id)}
						<AttendanceCard
							{course}
							records={attendanceRecords}
							maxAbsences={settingsStore.settings.attendanceMaxAbsences}
						/>
					{/each}
				</div>
			</div>
		</div>
	{/if}
</Container>
