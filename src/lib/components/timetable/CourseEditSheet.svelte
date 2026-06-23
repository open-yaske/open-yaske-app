<!--
  CourseEditSheet.svelte
  Bottom sheet with form to add/edit/delete a course.
  - TextField: name, teacher, classroom, url, memo
  - Select: day (7), period (7), span (1-3), type (free/required/elective), credits (0-2)
  - ColorPicker from theme palette
  - Save calls userDataStore.addCourse() or updateCourse()
  - Delete button for existing courses
-->
<script lang="ts">
	import { m } from '$lib/paraglide/messages';
	import { DAY_OF_WEEK_LIST, PERIOD_LIST } from '$lib/types';
	import type { Course, CourseType, DayOfWeek, Period } from '$lib/types';
	import { userDataStore, themeStore, settingsStore } from '$lib/stores';
	import Sheet from '$lib/components/Sheet.svelte';
	import TextField from '$lib/components/TextField.svelte';
	import ColorPicker from '$lib/components/ColorPicker.svelte';
	import Button from '$lib/components/Button.svelte';
	import { Trash2 } from '@lucide/svelte';

	interface Props {
		open: boolean;
		onclose: () => void;
		/** Existing course to edit, or null for new course */
		course: Course | null;
		/** Pre-filled day/period for new course from tapping an empty cell */
		prefillDay?: DayOfWeek;
		prefillPeriod?: Period;
	}

	let {
		open = $bindable(false),
		onclose,
		course,
		prefillDay = 'mon',
		prefillPeriod = 1
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

	// Sync form state when sheet opens or course changes
	$effect(() => {
		if (open) {
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
