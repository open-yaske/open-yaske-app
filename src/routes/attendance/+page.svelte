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
	<title>{m.nav_attendance()} | open-nexus</title>
</svelte:head>

{#snippet leadingSnippet()}
	<IconButton
		icon={themeIcon}
		ariaLabel={themeAriaLabel}
		onclick={toggleTheme}
		variant="subtle"
		size="sm"
	/>
{/snippet}

<PageHeader title="出席管理" leading={leadingSnippet} />

<Container class="pb-10">
	{#if courses.length === 0}
		<EmptyState icon={ClipboardCheck} message={m.empty_no_courses()} submessage={m.course_add()} />
	{:else}
		<div class="space-y-5 py-4">
			<!-- Statistics Section -->
			<div class="space-y-2">
				<SectionHeader>統計</SectionHeader>
				<div
					class="rounded-2xl border border-[var(--color-surface-border)] bg-[var(--color-surface-card)] p-5 shadow-sm space-y-4 max-w-5xl mx-auto"
				>
					<div class="grid grid-cols-2 gap-4">
						<div class="space-y-0.5">
							<span class="text-xs font-semibold text-[var(--color-nav-inactive)]">登録授業数</span>
							<div class="text-3xl font-black text-[var(--color-nav-active)]">
								{stats.totalRegistered}
							</div>
						</div>
						<div class="space-y-0.5">
							<span class="text-xs font-semibold text-[var(--color-nav-inactive)]"
								>記録済み授業</span
							>
							<div class="text-3xl font-black text-[var(--color-nav-active)]">
								{stats.recordedCount}
							</div>
						</div>
					</div>
					<div class="border-t border-[var(--color-surface-border)] pt-4 grid grid-cols-2 gap-4">
						<div class="space-y-0.5">
							<span class="text-xs font-semibold text-[var(--color-nav-inactive)]"
								>平均出席率 (記録あり)</span
							>
							<div class="text-3xl font-black text-[var(--color-primary-800)]">
								{stats.averageRate}%
							</div>
						</div>
						<div class="space-y-0.5">
							<span class="text-xs font-semibold text-[var(--color-nav-inactive)]">危険な授業</span>
							<div class="text-3xl font-black text-red-500">{stats.dangerCount}</div>
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
