<!--
  SyllabusSheet.svelte
  Sheet showing course details. If course has syllabusId,
  look up PublicCourse from publicDataStore and show descriptionHtml (sanitized).
-->
<script lang="ts">
	import { m } from '$lib/paraglide/messages';
	import type { Course, DayOfWeek, Period, AttendanceRecord, AttendanceStatus } from '$lib/types';
	import { publicDataStore, themeStore, userDataStore, settingsStore } from '$lib/stores';
	import { resolveColorHex } from '$lib/theme';
	import { sanitize, safeTarget } from '$lib/html-renderer';
	import type { PublicCourse } from '$lib/types/public';
	import Sheet from '$lib/components/Sheet.svelte';
	import Button from '$lib/components/Button.svelte';
	import { ExternalLink, ChevronDown, ChevronUp } from '@lucide/svelte';
	import ProgressRing from '$lib/components/ProgressRing.svelte';
	import AttendanceToggle from '$lib/components/attendance/AttendanceToggle.svelte';
	import AttendanceLog from '$lib/components/attendance/AttendanceLog.svelte';
	import { calculateRate, getTodayStatus, isDangerous } from '$lib/attendance';

	interface Props {
		open: boolean;
		onclose: () => void;
		course: Course | null;
		/** Open the edit sheet for this course */
		onedit?: () => void;
		/** Target date for attendance toggle (YYYY-MM-DD) */
		targetDate?: string;
	}

	let { open = $bindable(false), onclose, course, onedit, targetDate }: Props = $props();

	// Look up PublicCourse by syllabusId
	const publicCourse = $derived.by<PublicCourse | null>(() => {
		if (!course?.syllabusId) return null;
		const data = publicDataStore.data;
		if (!data?.courses) return null;
		return data.courses.find((c) => c.id === course.syllabusId) ?? null;
	});

	// Sanitized HTML for description
	const sanitizedHtml = $derived.by(() => {
		if (!publicCourse?.descriptionHtml) return '';
		return safeTarget(sanitize(publicCourse.descriptionHtml));
	});

	// Resolve course color for the accent bar
	const accentColor = $derived.by(() => {
		if (!course) return 'transparent';
		return resolveColorHex(themeStore.pack, course.color);
	});

	// Day label
	function dayLabel(day: DayOfWeek): string {
		const key = `day_${day}` as keyof typeof m;
		return (m[key] as () => string)();
	}

	// Period label
	function periodLabel(period: Period): string {
		const key = `period_${period}` as keyof typeof m;
		return (m[key] as () => string)();
	}

	// Type label
	function typeLabel(type: 'free' | 'required' | 'elective'): string {
		if (type === 'free') return m.course_free();
		if (type === 'required') return m.course_required();
		return m.course_elective();
	}

	// ----- 出席管理データの準備 -----
	const maxAbsences = $derived(settingsStore.settings.attendanceMaxAbsences);
	const allAttendance = $derived(userDataStore.attendance);
	const courseRecords = $derived(
		course ? allAttendance.filter((r) => r.courseId === course.id) : []
	);
	const summary = $derived(calculateRate(courseRecords, maxAbsences));

	const today = $derived.by(() => {
		const d = new Date();
		const y = d.getFullYear();
		const mo = String(d.getMonth() + 1).padStart(2, '0');
		const da = String(d.getDate()).padStart(2, '0');
		return `${y}-${mo}-${da}`;
	});

	const resolvedTargetDate = $derived(targetDate || today);
	const targetStatus = $derived(
		course ? getTodayStatus(courseRecords, course.id, resolvedTargetDate) : null
	);

	const ringColor = $derived(
		isDangerous(summary) ? '--color-attendance-absent-fg' : '--color-primary-500'
	);

	const stats = $derived([
		{ label: m.attendance_present(), value: summary.present, cssVar: 'present' },
		{ label: m.attendance_absent(), value: summary.absent, cssVar: 'absent' },
		{ label: m.attendance_late(), value: summary.late, cssVar: 'late' },
		{ label: m.attendance_excused(), value: summary.excused, cssVar: 'excused' }
	]);

	let logExpanded = $state(false);
	let showPastForm = $state(false);
	let pastDate = $state('');
	let pastStatus = $state<AttendanceStatus>('present');

	async function handleSelect(status: AttendanceStatus) {
		if (!course) return;
		const record: AttendanceRecord = {
			id: crypto.randomUUID(),
			courseId: course.id,
			date: resolvedTargetDate,
			status,
			createdAt: Date.now()
		};
		await userDataStore.setAttendance(record);
	}

	async function handleClear() {
		if (!course) return;
		await userDataStore.removeAttendance(course.id, resolvedTargetDate);
	}

	async function handleDeleteRecord(record: AttendanceRecord) {
		await userDataStore.removeAttendance(record.courseId, record.date);
	}

	async function handleAddPastRecord() {
		if (!course || !pastDate) return;
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

<Sheet bind:open {onclose} title={m.course_syllabus()}>
	{#if course}
		<div class="space-y-4">
			<!-- Course header with accent color -->
			<div
				class="rounded-lg p-3"
				style="border-left: 4px solid {accentColor}; background-color: var(--color-surface-muted);"
			>
				<h3 class="text-base font-semibold text-[var(--color-nav-active)]">{course.name}</h3>
				{#if course.code}
					<p class="mt-0.5 text-xs text-[var(--color-nav-inactive)]">{course.code}</p>
				{/if}
			</div>

			<!-- Course metadata -->
			<dl class="space-y-2 text-sm">
				{#if course.teacher}
					<div class="flex gap-2">
						<dt class="shrink-0 font-medium text-[var(--color-nav-inactive)]">
							{m.course_teacher()}
						</dt>
						<dd class="text-[var(--color-nav-active)]">{course.teacher}</dd>
					</div>
				{/if}
				{#if course.classroom}
					<div class="flex gap-2">
						<dt class="shrink-0 font-medium text-[var(--color-nav-inactive)]">
							{m.course_classroom()}
						</dt>
						<dd class="text-[var(--color-nav-active)]">{course.classroom}</dd>
					</div>
				{/if}
				<div class="flex gap-2">
					<dt class="shrink-0 font-medium text-[var(--color-nav-inactive)]">{m.course_day()}</dt>
					<dd class="text-[var(--color-nav-active)]">
						{dayLabel(course.day)}
						{periodLabel(course.period)}
					</dd>
				</div>
				<div class="flex gap-2">
					<dt class="shrink-0 font-medium text-[var(--color-nav-inactive)]">{m.course_type()}</dt>
					<dd class="text-[var(--color-nav-active)]">{typeLabel(course.type)}</dd>
				</div>
				<div class="flex gap-2">
					<dt class="shrink-0 font-medium text-[var(--color-nav-inactive)]">
						{m.course_credits()}
					</dt>
					<dd class="text-[var(--color-nav-active)]">{course.credits}</dd>
				</div>
			</dl>

			<!-- 出席管理セクション -->
			<div class="border-t border-[var(--color-surface-border)] pt-4 space-y-4">
				<h4 class="text-sm font-semibold text-[var(--color-nav-active)]">{m.nav_attendance()}</h4>

				<!-- 出席統計とリング -->
				<div class="flex items-center gap-4 bg-[var(--color-surface-muted)] p-3 rounded-lg">
					<ProgressRing value={summary.rate} size={48} strokeWidth={4} color={ringColor}>
						<div class="flex flex-col items-center leading-none">
							<span class="text-xs font-bold text-[var(--color-nav-active)]">
								{summary.rate.toFixed(0)}
							</span>
							<span class="text-[8px] text-[var(--color-nav-inactive)]">%</span>
						</div>
					</ProgressRing>
					<div class="flex-1 grid grid-cols-4 gap-1 text-center">
						{#each stats as stat (stat.cssVar)}
							<div class="flex flex-col">
								<span
									class="text-xs font-bold"
									style="color: var(--color-attendance-{stat.cssVar}-fg);"
								>
									{stat.value}
								</span>
								<span class="text-[9px] text-[var(--color-nav-inactive)]">{stat.label}</span>
							</div>
						{/each}
					</div>
					<div
						class="shrink-0 flex flex-col items-end border-l border-[var(--color-surface-border)] pl-3"
					>
						<span class="text-[9px] text-[var(--color-nav-inactive)]"
							>{m.attendance_remaining()}</span
						>
						<span class="text-xs font-bold text-[var(--color-nav-active)]">
							{summary.remainingAbsences}{m.attendance_count()}
						</span>
					</div>
				</div>

				<!-- 対象日の出席トグル -->
				<div class="space-y-1.5">
					<div class="flex items-center justify-between">
						<span class="text-xs font-medium text-[var(--color-nav-inactive)]">
							{m.attendance_target_date({ date: resolvedTargetDate })}
						</span>
						{#if targetStatus}
							<button
								type="button"
								class="text-xs text-[var(--color-nav-inactive)] underline"
								onclick={handleClear}
							>
								{m.action_undo()}
							</button>
						{/if}
					</div>
					<AttendanceToggle status={targetStatus} onselect={handleSelect} />
				</div>

				<!-- 履歴ログと過去記録追加フォーム -->
				<div class="space-y-3">
					<div class="flex items-center justify-between">
						<button
							type="button"
							class="flex items-center gap-1 text-xs font-medium text-[var(--color-nav-inactive)]"
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
											for="syllabus-past-date"
											class="block text-xs font-medium text-[var(--color-nav-inactive)]">日付</label
										>
										<input
											id="syllabus-past-date"
											type="date"
											bind:value={pastDate}
											class="block w-full rounded-md border border-[var(--color-surface-border)] bg-[var(--color-surface-card)] px-2.5 py-1.5 text-xs text-[var(--color-nav-active)] focus:outline-none"
										/>
									</div>
									<div class="space-y-1">
										<span class="block text-xs font-medium text-[var(--color-nav-inactive)]"
											>状態</span
										>
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

			<!-- Memo -->
			{#if course.memo}
				<div>
					<h4 class="mb-1 text-sm font-medium text-[var(--color-nav-inactive)]">
						{m.course_memo()}
					</h4>
					<p class="text-sm text-[var(--color-nav-active)]">{course.memo}</p>
				</div>
			{/if}

			<!-- URL -->
			{#if course.url}
				<!-- course.url is an external user-provided URL; resolve() is not applicable -->
				<!-- eslint-disable svelte/no-navigation-without-resolve -->
				<a
					href={course.url}
					target="_blank"
					rel="noopener noreferrer"
					class="inline-flex items-center gap-1.5 text-sm text-[var(--color-primary-500)] hover:underline"
				>
					<ExternalLink size={14} />
					{m.course_url()}
				</a>
				<!-- eslint-enable svelte/no-navigation-without-resolve -->
			{/if}

			<!-- Syllabus from public data -->
			{#if publicCourse}
				<div class="border-t border-[var(--color-surface-border)] pt-3">
					<h4 class="mb-2 text-sm font-semibold text-[var(--color-nav-active)]">
						{m.course_syllabus()}
					</h4>
					{#if publicCourse.descriptionHtml}
						<div class="prose prose-sm max-w-none text-[var(--color-nav-active)]">
							<!-- sanitizedHtml is processed via DOMPurify in lib/html-renderer.ts -->
							<!-- eslint-disable-next-line svelte/no-at-html-tags -->
							{@html sanitizedHtml}
						</div>
					{:else}
						<p class="text-sm text-[var(--color-nav-inactive)]">{m.empty_no_data()}</p>
					{/if}

					{#if publicCourse.references?.length}
						<div class="mt-3 space-y-1">
							<h5 class="text-xs font-medium text-[var(--color-nav-inactive)]">References</h5>
							{#each publicCourse.references as ref (ref.url)}
								<!-- eslint-disable svelte/no-navigation-without-resolve -->
								<a
									href={ref.url}
									target="_blank"
									rel="noopener noreferrer"
									class="block text-sm text-[var(--color-primary-500)] hover:underline"
								>
									{ref.title}
								</a>
								<!-- eslint-enable svelte/no-navigation-without-resolve -->
							{/each}
						</div>
					{/if}
				</div>
			{:else if course.syllabusId}
				<div class="border-t border-[var(--color-surface-border)] pt-3">
					<p class="text-sm text-[var(--color-nav-inactive)]">{m.error_load_failed()}</p>
				</div>
			{/if}
		</div>
	{/if}

	{#snippet footer()}
		<div class="flex gap-2">
			{#if onedit}
				<Button variant="secondary" size="md" class="flex-1" onclick={onedit}>
					{m.action_edit()}
				</Button>
			{/if}
			<Button variant="primary" size="md" class="flex-1" onclick={onclose}>
				{m.action_close()}
			</Button>
		</div>
	{/snippet}
</Sheet>
