<!--
  CourseEditSheet.svelte
  Bottom sheet with form to add/edit/delete a course.
  - TextField: name, teacher, classroom, url, memo
  - Select: day (7), period (7), span (1-3), type (free/required/elective), credits (0-2)
  - ColorPicker from theme palette
  - Save calls userDataStore.addCourse() or updateCourse()
  - Delete button for existing courses
  - Inline syllabus search to auto-fill form fields
-->
<script lang="ts">
	import { m } from '$lib/paraglide/messages';
	import { DAY_OF_WEEK_LIST, PERIOD_LIST } from '$lib/types';
	import type { Course, CourseType, DayOfWeek, Period } from '$lib/types';
	import { userDataStore, themeStore, settingsStore, publicDataStore, styleStore } from '$lib/stores';
	import type { PublicCourse } from '$lib/types/public';
	import Sheet from '$lib/components/Sheet.svelte';
	import TextField from '$lib/components/TextField.svelte';
	import ColorPicker from '$lib/components/ColorPicker.svelte';
	import Button from '$lib/components/Button.svelte';
	import { Trash2, BookOpen, Search, X, Check } from '@lucide/svelte';
	import { resolveRadius } from '$lib/styles';

	interface Props {
		open: boolean;
		onclose: () => void;
		/** Existing course to edit, or null for new course */
		course: Course | null;
		/** Pre-filled day/period for new course from tapping an empty cell */
		prefillDay?: DayOfWeek;
		prefillPeriod?: Period;
		/** Called when user taps "シラバスを見る" to view syllabus of linked course */
		onsyllabusview?: (syllabusId: string) => void;
	}

	let {
		open = $bindable(false),
		onclose,
		course,
		prefillDay = 'mon',
		prefillPeriod = 1,
		onsyllabusview
	}: Props = $props();

	// Form state
	let name = $state('');
	let teacher = $state('');
	let classroom = $state('');
	let url = $state('');
	let memo = $state('');
	let day = $state<DayOfWeek>('mon');
	// TextField select binds to string, so we use string intermediaries
	let periodStr = $state('1');
	let spanStr = $state('1');
	let creditsStr = $state('0');
	let color = $state('blue');
	let type = $state<CourseType>('free');
	let syllabusId = $state<string | undefined>(undefined);

	// Typed accessors derived from string state
	const period = $derived(Number(periodStr) as Period);
	const span = $derived(Number(spanStr) as 1 | 2 | 3);
	const credits = $derived(Number(creditsStr) as 0 | 1 | 2);

	// ----- シラバス検索パネルの状態 -----
	let syllabusSearchOpen = $state(false);
	let syllabusQuery = $state('');
	let linkedPublicCourse = $state<PublicCourse | null>(null);

	const publicCourses = $derived(publicDataStore.data?.courses || []);
	const hasPublicData = $derived(publicCourses.length > 0);

	const syllabusResults = $derived.by<PublicCourse[]>(() => {
		const q = syllabusQuery.trim().toLowerCase();
		if (!q) {
			// 現在選択されている曜日・時限に一致する科目を優先提案
			return publicCourses.filter((c) => {
				if (!c.dayPeriods) return false;
				return c.dayPeriods.some((dp) => dp.day === day && dp.period === period);
			});
		}
		return publicCourses
			.filter(
				(c) =>
					c.name.toLowerCase().includes(q) ||
					(c.teacher && c.teacher.toLowerCase().includes(q)) ||
					c.id.toLowerCase().includes(q)
			)
			.slice(0, 30);
	});

	function formatDayPeriod(dayPeriods: { day: string; period: number }[]) {
		if (!dayPeriods || dayPeriods.length === 0) return 'オンデマンド/集中';
		const days: Record<string, string> = {
			mon: '月',
			tue: '火',
			wed: '水',
			thu: '木',
			fri: '金',
			sat: '土',
			sun: '日'
		};
		return dayPeriods.map((dp) => `${days[dp.day] || dp.day}${dp.period}`).join(', ');
	}

	/** シラバス科目を選んでフォームに自動入力 */
	function applyPublicCourse(pc: PublicCourse) {
		name = pc.name;
		teacher = pc.teacher || '';
		classroom = pc.classroom || '';
		creditsStr = String(pc.credits ?? 2);
		type = pc.type === 'required' ? 'required' : pc.type === 'elective' ? 'elective' : 'free';
		syllabusId = pc.id;
		linkedPublicCourse = pc;

		// 曜日時限もセット (最初の時限のみ)
		const dp = pc.dayPeriods?.[0];
		if (dp) {
			day = dp.day as DayOfWeek;
			periodStr = String(dp.period);
		}

		syllabusSearchOpen = false;
		syllabusQuery = '';
	}

	/** リンク済みシラバスを解除 */
	function unlinkSyllabus() {
		syllabusId = undefined;
		linkedPublicCourse = null;
	}

	// Sync form state when sheet opens or course changes
	$effect(() => {
		if (open) {
			syllabusSearchOpen = false;
			syllabusQuery = '';
			if (course) {
				name = course.name;
				teacher = course.teacher ?? '';
				classroom = course.classroom ?? '';
				url = course.url ?? '';
				memo = course.memo ?? '';
				day = course.day;
				periodStr = String(course.period);
				spanStr = String(course.span);
				color = course.color;
				type = course.type;
				creditsStr = String(course.credits);
				syllabusId = course.syllabusId;
				// シラバスIDがあれば公開データから引っ張る
				linkedPublicCourse = course.syllabusId
					? (publicCourses.find((pc) => pc.id === course.syllabusId) ?? null)
					: null;
			} else {
				name = '';
				teacher = '';
				classroom = '';
				url = '';
				memo = '';
				day = prefillDay;
				periodStr = String(prefillPeriod);
				spanStr = '1';
				color = themeStore.pack.palette.courseColors[0]?.id ?? 'blue';
				type = 'free';
				creditsStr = '0';
				syllabusId = undefined;
				linkedPublicCourse = null;
			}
		}
	});

	const isEditing = $derived(!!course);

	// Day options
	const dayOptions = $derived(
		DAY_OF_WEEK_LIST.map((d) => {
			const key = `day_${d}` as keyof typeof m;
			return { value: d, label: (m[key] as () => string)() };
		})
	);

	// Period options
	const periodOptions = $derived(
		PERIOD_LIST.map((p) => {
			const key = `period_${p}` as keyof typeof m;
			const time = settingsStore.settings.timetablePeriods?.[p];
			const fn = m[key] as () => string;
			const label = time ? `${fn()} (${time.start}~${time.end})` : fn();
			return { value: String(p), label };
		})
	);

	// Span options
	const spanOptions = [
		{ value: '1', label: m.course_span_1() },
		{ value: '2', label: m.course_span_2() },
		{ value: '3', label: m.course_span_3() }
	];

	// Type options
	const typeOptions = [
		{ value: 'free', label: m.course_free() },
		{ value: 'required', label: m.course_required() },
		{ value: 'elective', label: m.course_elective() }
	];

	// Credits options
	const creditsOptions = [
		{ value: '0', label: '0' },
		{ value: '1', label: '1' },
		{ value: '2', label: '2' }
	];

	const coursePalette = $derived(themeStore.pack.palette.courseColors);

	// Style-aware corner radius
	const r = $derived(resolveRadius(styleStore.pack));

	async function handleSave() {
		if (name.trim() === '') return;

		const courseData = {
			name: name.trim(),
			teacher: teacher.trim() || undefined,
			classroom: classroom.trim() || undefined,
			url: url.trim() || undefined,
			memo: memo.trim() || undefined,
			day,
			period,
			span,
			color,
			type,
			credits,
			syllabusId
		};

		if (course) {
			await userDataStore.updateCourse(course.id, courseData);
		} else {
			await userDataStore.addCourse(courseData);
		}
		onclose();
	}

	async function handleDelete() {
		if (!course) return;
		if (confirm(m.course_delete_confirm())) {
			await userDataStore.removeCourse(course.id);
			onclose();
		}
	}
</script>

<Sheet bind:open {onclose} title={isEditing ? m.course_edit() : m.course_add()}>
	<div class="space-y-4">
		<!-- ===== シラバス検索パネル ===== -->
		{#if hasPublicData}
			<!-- リンク済みシラバス表示 -->
			{#if linkedPublicCourse && !syllabusSearchOpen}
				<div
					class="flex items-center gap-3 {r.card} border border-[var(--color-primary-200)] bg-[var(--color-primary-50)] px-3 py-2.5"
				>
					<BookOpen size={16} class="shrink-0 text-[var(--color-primary-600)]" />
					<div class="flex-1 min-w-0">
						<p class="text-xs font-semibold text-[var(--color-primary-700)] truncate">
							{linkedPublicCourse.name}
						</p>
						<p class="text-[11px] text-[var(--color-primary-500)]">
							{linkedPublicCourse.teacher || ''}
							{linkedPublicCourse.dayPeriods?.length
								? ' · ' + formatDayPeriod(linkedPublicCourse.dayPeriods)
								: ''}
						</p>
					</div>
					<div class="flex items-center gap-1 shrink-0">
						{#if onsyllabusview}
							<button
								type="button"
								onclick={() => onsyllabusview?.(syllabusId!)}
								class="rounded px-2 py-1 text-[11px] font-medium text-[var(--color-primary-600)] hover:bg-[var(--color-primary-100)] transition-colors"
							>
								詳細を見る
							</button>
						{/if}
						<button
							type="button"
							onclick={unlinkSyllabus}
							class="rounded p-1 text-[var(--color-nav-inactive)] hover:text-[var(--color-nav-active)] transition-colors"
							aria-label="リンクを解除"
						>
							<X size={14} />
						</button>
					</div>
				</div>
			{:else if !syllabusSearchOpen}
				<!-- シラバス検索を開くボタン -->
				<button
					type="button"
					onclick={() => (syllabusSearchOpen = true)}
					class="flex w-full items-center gap-2 {r.card} border border-dashed border-[var(--color-surface-border)] px-3 py-2.5 text-sm text-[var(--color-nav-inactive)] hover:border-[var(--color-primary-400)] hover:text-[var(--color-primary-600)] transition-colors"
				>
					<Search size={15} class="shrink-0" />
					<span>シラバスから科目を検索して自動入力</span>
				</button>
			{/if}

			<!-- 検索パネル（展開時） -->
			{#if syllabusSearchOpen}
				<div
					class="{r.card} border border-[var(--color-primary-300)] bg-[var(--color-surface-card)] overflow-hidden"
				>
					<!-- 検索フィールド (DESIGN 準拠) -->
					<div class="flex items-center gap-2 border-b border-[var(--color-surface-border)] p-3">
						<div class="flex-1">
							<TextField
								bind:value={syllabusQuery}
								placeholder="科目名・担当教員で検索…"
								leadingIcon={Search}
							/>
						</div>
						<button
							type="button"
							onclick={() => {
								syllabusSearchOpen = false;
								syllabusQuery = '';
							}}
							class="shrink-0 p-2 text-[var(--color-nav-inactive)] hover:text-[var(--color-nav-active)] transition-colors"
							aria-label="閉じる"
						>
							<X size={16} />
						</button>
					</div>

					<!-- 結果リスト -->
					<div class="max-h-52 overflow-y-auto">
						{#if syllabusResults.length === 0}
							<div class="px-4 py-5 text-center text-sm text-[var(--color-nav-inactive)]">
								{m.empty_no_results()}
							</div>
						{:else}
							{#each syllabusResults as pc (pc.id)}
								<button
									type="button"
									onclick={() => applyPublicCourse(pc)}
									class="flex w-full items-start gap-3 border-b border-[var(--color-surface-border)] px-3 py-2.5 text-left last:border-b-0 hover:bg-[var(--color-surface-muted)] transition-colors"
								>
									<div class="flex-1 min-w-0">
										<p
											class="text-sm font-medium text-[var(--color-nav-active)] truncate leading-snug"
										>
											{pc.name}
										</p>
										<p class="text-[11px] text-[var(--color-nav-inactive)] mt-0.5">
											{#if pc.teacher}{pc.teacher} ·{/if}
											{pc.dayPeriods?.length ? formatDayPeriod(pc.dayPeriods) : 'オンデマンド'}
											{#if pc.credits}
												· {pc.credits}単位{/if}
										</p>
									</div>
									<Check size={14} class="mt-1 shrink-0 text-[var(--color-primary-500)]" />
								</button>
							{/each}
						{/if}
					</div>
				</div>
			{/if}
		{/if}

		<!-- Course name -->
		<TextField
			bind:value={name}
			label={m.course_name()}
			placeholder={m.course_name()}
			required
			error={name.trim() === '' && open ? m.course_name() : ''}
		/>

		<!-- Teacher -->
		<TextField bind:value={teacher} label={m.course_teacher()} placeholder={m.course_teacher()} />

		<!-- Classroom -->
		<TextField
			bind:value={classroom}
			label={m.course_classroom()}
			placeholder={m.course_classroom()}
		/>

		<!-- Day + Period -->
		<div class="grid grid-cols-2 gap-3">
			<TextField type="select" bind:value={day} label={m.course_day()} options={dayOptions} />
			<TextField
				type="select"
				bind:value={periodStr}
				label={m.course_period()}
				options={periodOptions}
			/>
		</div>

		<!-- Span + Credits -->
		<div class="grid grid-cols-2 gap-3">
			<TextField type="select" bind:value={spanStr} label={m.course_span()} options={spanOptions} />
			<TextField
				type="select"
				bind:value={creditsStr}
				label={m.course_credits()}
				options={creditsOptions}
			/>
		</div>

		<!-- Type -->
		<TextField type="select" bind:value={type} label={m.course_type()} options={typeOptions} />

		<!-- Color -->
		<ColorPicker
			bind:value={color}
			palette={coursePalette}
			label={m.course_color()}
			kind="course"
			allowNone={false}
		/>

		<!-- URL -->
		<TextField bind:value={url} type="url" label={m.course_url()} placeholder="https://" />

		<!-- Memo -->
		<TextField
			bind:value={memo}
			type="textarea"
			label={m.course_memo()}
			placeholder={m.course_memo()}
			rows={3}
		/>
	</div>

	{#snippet footer()}
		<div class="flex items-center gap-2">
			{#if isEditing}
				<Button variant="danger" size="md" onclick={handleDelete} aria-label={m.course_delete()}>
					<Trash2 size={16} />
				</Button>
			{/if}
			<Button variant="secondary" size="md" class="flex-1" onclick={onclose}>
				{m.course_cancel()}
			</Button>
			<Button
				variant="primary"
				size="md"
				class="flex-1"
				onclick={handleSave}
				disabled={name.trim() === ''}
			>
				{m.course_save()}
			</Button>
		</div>
	{/snippet}
</Sheet>
