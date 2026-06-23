<!--
  AttendanceCard.svelte
  Card for a single course showing attendance summary, today's toggle, and recent log.
  Uses calculateRate() from attendance.ts (cancelled excluded from denominator).
-->
<script lang="ts">
	import { m } from '$lib/paraglide/messages';
	import { themeStore, userDataStore } from '$lib/stores';
	import { resolveColorHex } from '$lib/theme';
	import { calculateRate, getTodayStatus, isDangerous } from '$lib/attendance';
	import type { Course, AttendanceRecord, AttendanceStatus } from '$lib/types';
	import ProgressRing from '$lib/components/ProgressRing.svelte';
	import AttendanceToggle from './AttendanceToggle.svelte';
	import AttendanceLog from './AttendanceLog.svelte';
	import { ChevronDown, ChevronUp } from '@lucide/svelte';

	interface Props {
		course: Course;
		records: AttendanceRecord[];
		maxAbsences: number;
	}

	let { course, records, maxAbsences }: Props = $props();

	// Records for this course only
	const courseRecords = $derived(records.filter((r) => r.courseId === course.id));

	// Attendance summary
	const summary = $derived(calculateRate(courseRecords, maxAbsences));

	// Today's date as YYYY-MM-DD
	const today = $derived.by(() => {
		const d = new Date();
		const y = d.getFullYear();
		const mo = String(d.getMonth() + 1).padStart(2, '0');
		const da = String(d.getDate()).padStart(2, '0');
		return `${y}-${mo}-${da}`;
	});

	const todayStatus = $derived(getTodayStatus(courseRecords, course.id, today));

	// Course color resolved from theme pack
	const courseColor = $derived(resolveColorHex(themeStore.pack, course.color));

	// ProgressRing color: red if dangerous, primary otherwise
	const ringColor = $derived(
		isDangerous(summary) ? '--color-attendance-absent-fg' : '--color-primary-500'
	);

	// Stats for the 4-count row
	const stats = $derived([
		{ label: m.attendance_present(), value: summary.present, cssVar: 'present' },
		{ label: m.attendance_absent(), value: summary.absent, cssVar: 'absent' },
		{ label: m.attendance_late(), value: summary.late, cssVar: 'late' },
		{ label: m.attendance_excused(), value: summary.excused, cssVar: 'excused' }
	]);

	// Collapsible log
	let logExpanded = $state(false);

	async function handleSelect(status: AttendanceStatus) {
		const record: AttendanceRecord = {
			id: crypto.randomUUID(),
			courseId: course.id,
			date: today,
			status,
			createdAt: Date.now()
		};
		await userDataStore.setAttendance(record);
	}

	async function handleClear() {
		await userDataStore.removeAttendance(course.id, today);
	}

	async function handleDeleteRecord(record: AttendanceRecord) {
		await userDataStore.removeAttendance(record.courseId, record.date);
	}

	let showPastForm = $state(false);
	let pastDate = $state('');
	let pastStatus = $state<AttendanceStatus>('present');

	async function handleAddPastRecord() {
		if (!pastDate) return;
		const record: AttendanceRecord = {
			id: crypto.randomUUID(),
			courseId: course.id,
			date: pastDate,
			status: pastStatus,
			createdAt: Date.now()
		};
		await userDataStore.setAttendance(record);
		showPastForm = false;
		pastDate = '';
	}
</script>

<div
	class="space-y-4 rounded-[var(--radius-card)] border border-[var(--color-surface-border)] bg-[var(--color-surface-card)] p-4 shadow-[var(--shadow-card)]"
>
	<!-- Header: course name + color dot + rate ring -->
	<div class="flex items-center gap-3">
		<span
			class="h-3 w-3 shrink-0 rounded-full"
			style="background-color: {courseColor};"
			aria-hidden="true"
		></span>
		<div class="min-w-0 flex-1">
			<h3 class="truncate text-sm font-semibold text-[var(--color-nav-active)]">
				{course.name}
			</h3>
			{#if course.code}
				<p class="truncate text-xs text-[var(--color-nav-inactive)]">{course.code}</p>
			{/if}
		</div>
		<ProgressRing value={summary.rate} size={56} strokeWidth={5} color={ringColor}>
			<div class="flex flex-col items-center leading-none">
				<span class="text-sm font-bold text-[var(--color-nav-active)]">
					{summary.rate.toFixed(0)}
				</span>
				<span class="text-[10px] text-[var(--color-nav-inactive)]">%</span>
			</div>
		</ProgressRing>
	</div>

	<!-- Stats row: 4 counts -->
	<div class="grid grid-cols-4 gap-2">
		{#each stats as stat (stat.cssVar)}
			<div class="flex flex-col items-center gap-0.5">
				<span
					class="text-lg font-bold leading-none"
					style="color: var(--color-attendance-{stat.cssVar}-fg);"
				>
					{stat.value}
				</span>
				<span class="text-[10px] text-[var(--color-nav-inactive)]">{stat.label}</span>
			</div>
		{/each}
	</div>

	<!-- Remaining absences -->
	<div
		class="flex items-center justify-between rounded-lg bg-[var(--color-surface-muted)] px-3 py-2"
	>
		<span class="text-xs text-[var(--color-surface-muted-foreground)]"
			>{m.attendance_remaining()}</span
		>
		<span
			class="text-sm font-semibold"
			class:text-[var(--color-attendance-absent-fg)]={summary.remainingAbsences <= 1}
			class:text-[var(--color-nav-active)]={summary.remainingAbsences > 1}
		>
			{summary.remainingAbsences}{m.attendance_count()}
		</span>
	</div>

	<!-- Today's attendance toggle -->
	<div class="space-y-1.5">
		<div class="flex items-center justify-between">
			<span class="text-xs font-medium text-[var(--color-nav-inactive)]">
				{m.attendance_today()}
			</span>
			{#if todayStatus}
				<button
					type="button"
					class="text-xs text-[var(--color-nav-inactive)] underline"
					onclick={handleClear}
				>
					{m.action_undo()}
				</button>
			{/if}
		</div>
		<AttendanceToggle status={todayStatus} onselect={handleSelect} />
	</div>

	<!-- Collapsible attendance log -->
	<div class="space-y-3">
		<div class="flex items-center justify-between">
			<button
				type="button"
				class="flex items-center gap-1.5 text-xs font-medium text-[var(--color-nav-inactive)]"
				onclick={() => (logExpanded = !logExpanded)}
			>
				<span>{m.attendance_history()} ({courseRecords.length})</span>
				{#if logExpanded}
					<ChevronUp size={14} />
				{:else}
					<ChevronDown size={14} />
				{/if}
			</button>
			{#if logExpanded}
				<button
					type="button"
					class="text-xs text-[var(--color-primary-500)] font-medium hover:underline"
					onclick={() => (showPastForm = !showPastForm)}
				>
					{showPastForm ? m.action_cancel() : m.attendance_add_past()}
				</button>
			{/if}
		</div>

		{#if logExpanded}
			<div class="border-t border-[var(--color-surface-border)] pt-3 space-y-3">
				{#if showPastForm}
					<div
						class="p-3 rounded-lg border border-[var(--color-surface-border)] bg-[var(--color-surface-muted)] space-y-3"
					>
						<div class="space-y-1">
							<label
								for="past-date-{course.id}"
								class="block text-xs font-medium text-[var(--color-nav-inactive)]"
							>
								日付
							</label>
							<input
								id="past-date-{course.id}"
								type="date"
								bind:value={pastDate}
								class="block w-full rounded-md border border-[var(--color-surface-border)] bg-[var(--color-surface-card)] px-2.5 py-1.5 text-xs text-[var(--color-nav-active)] focus:outline-none"
							/>
						</div>
						<div class="space-y-1">
							<span class="block text-xs font-medium text-[var(--color-nav-inactive)]">状態</span>
							<AttendanceToggle status={pastStatus} onselect={(s) => (pastStatus = s)} />
						</div>
						<div class="flex justify-end gap-2 pt-1">
							<button
								type="button"
								class="rounded px-2.5 py-1 text-xs text-[var(--color-nav-inactive)] hover:bg-[var(--color-surface-border)]"
								onclick={() => (showPastForm = false)}
							>
								{m.action_cancel()}
							</button>
							<button
								type="button"
								disabled={!pastDate}
								class="rounded bg-[var(--color-primary-500)] text-white px-2.5 py-1 text-xs font-medium disabled:opacity-50"
								onclick={handleAddPastRecord}
							>
								{m.action_save()}
							</button>
						</div>
					</div>
				{/if}
				<AttendanceLog records={courseRecords} ondelete={handleDeleteRecord} />
			</div>
		{/if}
	</div>
</div>
