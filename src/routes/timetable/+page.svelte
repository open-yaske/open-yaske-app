<script lang="ts">
	import { m } from '$lib/paraglide/messages';
	import { userDataStore, settingsStore, themeStore } from '$lib/stores';
	import type { Course, DayOfWeek, Period } from '$lib/types';
	import PageHeader from '$lib/components/PageHeader.svelte';
	import Container from '$lib/components/Container.svelte';
	import IconButton from '$lib/components/IconButton.svelte';
	import WeekGrid from '$lib/components/timetable/WeekGrid.svelte';
	import CourseEditSheet from '$lib/components/timetable/CourseEditSheet.svelte';
	import SyllabusSheet from '$lib/components/timetable/SyllabusSheet.svelte';
	import SyllabusPreviewSheet from '$lib/components/timetable/SyllabusPreviewSheet.svelte';
	import { Plus, Share2, GraduationCap, Sun, Moon, Laptop, ChevronDown } from '@lucide/svelte';
	import { resolveColorHex } from '$lib/theme';
	import { page } from '$app/stores';

	let editOpen = $state(false);
	let syllabusOpen = $state(false);
	let syllabusPreviewOpen = $state(false);
	let syllabusPreviewId = $state<string | null>(null);
	let selectedCourse = $state<Course | null>(null);
	let prefillDay = $state<DayOfWeek>('mon');
	let prefillPeriod = $state<Period>(1);

	$effect(() => {
		const editCourseId = $page.url.searchParams.get('editCourseId');
		if (editCourseId) {
			const found = userDataStore.courses.find((c) => c.id === editCourseId);
			if (found) {
				selectedCourse = found;
				editOpen = true;

				// URLからパラメータを削除して履歴を上書き (再起動防止)
				const newUrl = new URL(window.location.href);
				newUrl.searchParams.delete('editCourseId');
				window.history.replaceState({}, '', newUrl.toString());
			}
		}
	});

	const dayRange = $derived(settingsStore.settings.timetableDayRange);
	const periodRange = $derived(settingsStore.settings.timetablePeriodRange);
	const courses = $derived(userDataStore.courses);
	const totalCredits = $derived(courses.reduce((sum, c) => sum + c.credits, 0));

	let currentYear = $state(settingsStore.settings.year);
	let currentSemester = $state(settingsStore.settings.semester);

	$effect(() => {
		if (
			settingsStore.settings.year !== currentYear ||
			settingsStore.settings.semester !== currentSemester
		) {
			settingsStore.update({ year: currentYear, semester: currentSemester });
		}
	});

	function handleCellTap(day: DayOfWeek, period: Period) {
		selectedCourse = null;
		prefillDay = day;
		prefillPeriod = period;
		editOpen = true;
	}

	function handleCourseTap(course: Course) {
		selectedCourse = course;
		syllabusOpen = true;
	}

	function handleAddClick() {
		selectedCourse = null;
		prefillDay = 'mon';
		prefillPeriod = 1;
		editOpen = true;
	}

	function handleEditFromSyllabus() {
		syllabusOpen = false;
		editOpen = true;
	}

	function handleSyllabusViewFromEdit(sid: string) {
		syllabusPreviewId = sid;
		syllabusPreviewOpen = true;
	}

	function wrapCanvasText(
		ctx: CanvasRenderingContext2D,
		text: string,
		x: number,
		y: number,
		maxWidth: number,
		lineHeight: number,
		maxLines: number
	) {
		const words = [...text];
		let line = '';
		let lines = 0;
		for (const word of words) {
			const next = line + word;
			if (ctx.measureText(next).width > maxWidth && line) {
				ctx.fillText(line, x, y + lines * lineHeight);
				line = word;
				lines += 1;
				if (lines >= maxLines) return;
			} else {
				line = next;
			}
		}
		if (line && lines < maxLines) ctx.fillText(line, x, y + lines * lineHeight);
	}

	async function shareTimetableImage() {
		const days: DayOfWeek[] = ['mon', 'tue', 'wed', 'thu', 'fri', 'sat', 'sun'].slice(
			0,
			dayRange
		) as DayOfWeek[];
		const periods = [1, 2, 3, 4, 5, 6, 7].slice(0, periodRange) as Period[];
		const scale = 2;
		const timeCol = 64;
		const cellW = 150;
		const headerH = 54;
		const cellH = 96;
		const width = timeCol + days.length * cellW;
		const height = headerH + periods.length * cellH;
		const canvas = document.createElement('canvas');
		canvas.width = width * scale;
		canvas.height = height * scale;
		const ctx = canvas.getContext('2d');
		if (!ctx) return;
		ctx.scale(scale, scale);
		ctx.fillStyle = 'white';
		ctx.fillRect(0, 0, width, height);
		ctx.strokeStyle = 'rgb(229, 231, 235)';
		ctx.lineWidth = 1;
		ctx.font = '700 18px system-ui, sans-serif';
		ctx.fillStyle = 'rgb(17, 24, 39)';
		days.forEach((day, i) => {
			const key = `day_${day}` as keyof typeof m;
			ctx.fillText((m[key] as () => string)(), timeCol + i * cellW + cellW / 2 - 8, 34);
		});
		periods.forEach((period, row) => {
			const y = headerH + row * cellH;
			ctx.fillStyle = 'rgb(17, 24, 39)';
			ctx.font = '800 20px system-ui, sans-serif';
			ctx.fillText(String(period), 26, y + cellH / 2 + 7);
			ctx.strokeRect(0, y, width, cellH);
		});
		days.forEach((_, col) => {
			ctx.strokeRect(timeCol + col * cellW, 0, cellW, height);
		});
		for (const course of courses) {
			const col = days.indexOf(course.day);
			const row = periods.indexOf(course.period);
			if (col < 0 || row < 0) continue;
			const x = timeCol + col * cellW + 6;
			const y = headerH + row * cellH + 6;
			const h = Math.min(course.span, periods.length - row) * cellH - 12;
			ctx.fillStyle = resolveColorHex(themeStore.pack, course.color);
			ctx.beginPath();
			ctx.roundRect(x, y, cellW - 12, h, 12);
			ctx.fill();
			ctx.fillStyle = themeStore.pack.id === 'dark' ? 'rgb(17, 24, 39)' : 'white';
			ctx.font = '700 14px system-ui, sans-serif';
			wrapCanvasText(
				ctx,
				course.name,
				x + 12,
				y + 24,
				cellW - 36,
				18,
				Math.max(1, Math.floor((h - 22) / 18))
			);
			if (course.classroom) {
				ctx.font = '700 11px system-ui, sans-serif';
				ctx.fillText(course.classroom, x + 12, y + h - 12);
			}
		}
		canvas.toBlob(async (blob) => {
			if (!blob) return;
			const file = new File([blob], `timetable_${currentYear}_${currentSemester}.png`, {
				type: 'image/png'
			});
			if (navigator.canShare?.({ files: [file] })) {
				await navigator.share({ files: [file], title: 'OpenYASKE 時間割' });
				return;
			}
			const url = URL.createObjectURL(blob);
			const a = document.createElement('a');
			a.href = url;
			a.download = file.name;
			a.click();
			URL.revokeObjectURL(url);
		}, 'image/png');
	}

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
	<title>{m.nav_timetable()} | OpenYASKE</title>
</svelte:head>

<div
	class="relative min-h-screen pb-24 md:pb-6"
	style={settingsStore.settings.timetableBgImage && !settingsStore.settings.bgImageAllPages
		? `background-image: url(${settingsStore.settings.timetableBgImage}); background-size: cover; background-position: center; background-attachment: fixed;`
		: ''}
>
	<!-- Custom mockup header with slots -->
	{#snippet titleSnippet()}
		<label class="relative inline-flex min-w-[140px] items-center justify-center">
			<span class="sr-only">{m.settings_semester()}</span>
			<select
				bind:value={currentSemester}
				class="h-9 w-full cursor-pointer appearance-none rounded-chip border border-[var(--color-surface-border)]/60 bg-[var(--color-surface-card)]/90 backdrop-blur-md py-1.5 pl-3.5 pr-8 text-center text-xs font-bold text-[var(--color-nav-active)] outline-none shadow-apple-sm transition-all hover:bg-[var(--color-surface-muted)] focus:ring-2 focus:ring-[var(--color-primary-500)]"
				style="-webkit-appearance: none; -moz-appearance: none; appearance: none;"
			>
				<option value="spring"
					>{currentYear}{m.settings_year()} {m.settings_semester_spring()}</option
				>
				<option value="fall">{currentYear}{m.settings_year()} {m.settings_semester_fall()}</option>
			</select>
			<span
				class="pointer-events-none absolute right-3 top-1/2 flex -translate-y-1/2 text-[var(--color-primary-500)]"
			>
				<ChevronDown size={14} />
			</span>
		</label>
	{/snippet}

	{#snippet trailingSnippet()}
		<div class="flex items-center gap-1">
			<IconButton
				icon={Share2}
				ariaLabel="共有"
				onclick={shareTimetableImage}
				variant="subtle"
				size="sm"
			/>
			<IconButton
				icon={Plus}
				ariaLabel="科目追加"
				onclick={handleAddClick}
				variant="primary"
				size="sm"
			/>
		</div>
	{/snippet}

	<PageHeader {titleSnippet} trailing={trailingSnippet} />

	<Container size="full" class="pt-2 space-y-5">
		<WeekGrid
			{courses}
			{dayRange}
			{periodRange}
			oncelltap={handleCellTap}
			oncoursetap={handleCourseTap}
		/>

		<!-- Mock bottom layout: Add Course dashed button + Total Credits card -->
		<div class="px-4 space-y-4 max-w-5xl mx-auto">
			<!-- Dashed add button -->
			<button
				type="button"
				onclick={handleAddClick}
				class="flex w-full items-center justify-center gap-2 rounded-card border-2 border-dashed border-[var(--color-surface-border)] bg-[var(--color-surface-card)]/80 backdrop-blur-xs py-3.5 text-sm font-semibold text-[var(--color-nav-active)] transition-all hover:bg-[var(--color-surface-muted)]"
			>
				<Plus size={16} />
				授業を追加
			</button>

			<!-- Total credits card -->
			<div
				class="flex items-center justify-between rounded-card bg-[var(--color-primary-50)] text-[var(--color-primary-800)] p-4"
			>
				<div class="flex items-center gap-2.5">
					<span
						class="flex h-10 w-10 items-center justify-center rounded-full bg-[var(--color-primary-100)] text-[var(--color-primary-800)]"
					>
						<GraduationCap size={20} />
					</span>
					<span class="text-sm font-bold">合計の単位数</span>
				</div>
				<span class="text-xl font-black">
					{totalCredits} <span class="text-xs font-normal">単位</span>
				</span>
			</div>
		</div>
	</Container>
</div>

<CourseEditSheet
	bind:open={editOpen}
	onclose={() => (editOpen = false)}
	course={selectedCourse}
	{prefillDay}
	{prefillPeriod}
	onsyllabusview={handleSyllabusViewFromEdit}
/>

<SyllabusSheet
	bind:open={syllabusOpen}
	onclose={() => (syllabusOpen = false)}
	course={selectedCourse}
	onedit={handleEditFromSyllabus}
/>

<SyllabusPreviewSheet
	bind:open={syllabusPreviewOpen}
	onclose={() => (syllabusPreviewOpen = false)}
	syllabusId={syllabusPreviewId}
/>
