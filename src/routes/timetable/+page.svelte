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
	import { Plus, Share2, GraduationCap, Sun, Moon, Laptop, ChevronDown } from '@lucide/svelte';
	import { resolveColorHex } from '$lib/theme';

	let editOpen = $state(false);
	let syllabusOpen = $state(false);
	let selectedCourse = $state<Course | null>(null);
	let prefillDay = $state<DayOfWeek>('mon');
	let prefillPeriod = $state<Period>(1);

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
				await navigator.share({ files: [file], title: 'open-nexus 時間割' });
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
	<title>{m.nav_timetable()} | open-nexus</title>
</svelte:head>

<div
	class="relative min-h-screen pb-24 md:pb-6"
	style={settingsStore.settings.timetableBgImage && !settingsStore.settings.bgImageAllPages
		? `background-image: url(${settingsStore.settings.timetableBgImage}); background-size: cover; background-position: center; background-attachment: fixed;`
		: ''}
>
	<!-- Custom mockup header with slots -->
	{#snippet leadingSnippet()}
		<div class="flex items-center gap-1">
			<IconButton
				icon={themeIcon}
				ariaLabel={themeAriaLabel}
				onclick={toggleTheme}
				variant="subtle"
				size="sm"
			/>
		</div>
	{/snippet}

	{#snippet titleSnippet()}
		<label class="relative inline-flex min-w-32 items-center justify-center">
			<span class="sr-only">{m.settings_semester()}</span>
			<select
				bind:value={currentSemester}
				class="h-9 w-full cursor-pointer appearance-none rounded-full border border-[var(--color-surface-border)] bg-[var(--color-surface-card)]/95 py-1.5 pl-4 pr-9 text-center text-sm font-bold text-[var(--color-nav-active)] shadow-sm outline-none transition-colors hover:bg-[var(--color-surface-muted)] focus:border-[var(--color-primary-500)] focus:ring-2 focus:ring-[var(--color-primary-500)]/25"
				style="-webkit-appearance: none; -moz-appearance: none; appearance: none;"
			>
				<option value="spring"
					>{currentYear}{m.settings_year()} {m.settings_semester_spring()}</option
				>
				<option value="fall">{currentYear}{m.settings_year()} {m.settings_semester_fall()}</option>
			</select>
			<span
				class="pointer-events-none absolute right-3 top-1/2 flex -translate-y-1/2 text-[var(--color-nav-inactive)]"
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
		</div>
	{/snippet}

	<PageHeader leading={leadingSnippet} {titleSnippet} trailing={trailingSnippet} />

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
				class="flex w-full items-center justify-center gap-2 rounded-xl border-2 border-dashed border-[var(--color-surface-border)] bg-[var(--color-surface-card)]/80 backdrop-blur-xs py-3.5 text-sm font-semibold text-[var(--color-nav-active)] transition-all hover:bg-[var(--color-surface-muted)]"
			>
				<Plus size={16} />
				授業を追加
			</button>

			<!-- Total credits card -->
			<div
				class="flex items-center justify-between rounded-xl bg-[var(--color-primary-50)] text-[var(--color-primary-800)] p-4 shadow-sm"
			>
				<div class="flex items-center gap-2.5">
					<span
						class="flex h-10 w-10 items-center justify-center rounded-full bg-white text-[var(--color-primary-700)] dark:bg-[var(--color-primary-900)] dark:text-[var(--color-primary-300)]"
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
/>

<SyllabusSheet
	bind:open={syllabusOpen}
	onclose={() => (syllabusOpen = false)}
	course={selectedCourse}
	onedit={handleEditFromSyllabus}
/>
