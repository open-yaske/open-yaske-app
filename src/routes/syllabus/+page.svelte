<script lang="ts">
	import { publicDataStore, userDataStore, uiStore, themeStore } from '$lib/stores';
	import { m } from '$lib/paraglide/messages';
	import PageHeader from '$lib/components/PageHeader.svelte';
	import Container from '$lib/components/Container.svelte';
	import TextField from '$lib/components/TextField.svelte';
	import Button from '$lib/components/Button.svelte';
	import type { PublicCourse } from '$lib/types/public';
	import type { DayOfWeek, Period } from '$lib/types';
	import { openLink } from '$lib/browser';
	import {
		Search,
		BookOpen,
		User,
		Clock,
		Plus,
		ExternalLink,
		GraduationCap,
		ChevronDown,
		ChevronUp,
		ArrowUp,
		Check
	} from '@lucide/svelte';
	import { goto } from '$app/navigation';
	import Modal from '$lib/components/Modal.svelte';
	import { resolveColorHex } from '$lib/theme';
	import { getLocale } from '$lib/paraglide/runtime';
	import { getLocalizedText } from '$lib/theme/utils';
	import ColorPicker from '$lib/components/ColorPicker.svelte';

	let searchQuery = $state('');
	let displayLimit = $state(50);
	let scrollAnchor = $state<HTMLElement | null>(null);

	let showScrollTop = $state(false);

	function handleScroll() {
		if (typeof window !== 'undefined') {
			showScrollTop = window.scrollY > 400;
		}
	}

	function scrollToTop() {
		if (typeof window !== 'undefined') {
			window.scrollTo({ top: 0, behavior: 'smooth' });
		}
	}

	// フィルター状態
	let showFilter = $state(false);
	let filterDay = $state('all');
	let filterPeriod = $state('all');
	let filterType = $state('all');
	let filterYear = $state('all');

	const publicCourses = $derived(publicDataStore.data?.courses || []);

	const hasActiveFilters = $derived(
		filterDay !== 'all' || filterPeriod !== 'all' || filterType !== 'all' || filterYear !== 'all'
	);

	// 検索キーワードやフィルターが変わったら表示件数をリセット
	$effect(() => {
		const _ = searchQuery;
		const __ = filterDay;
		const ___ = filterPeriod;
		const ____ = filterType;
		const _____ = filterYear;
		displayLimit = 50;
	});

	// スクロール検知用のObserver
	$effect(() => {
		if (!scrollAnchor) return;
		const observer = new IntersectionObserver(
			(entries) => {
				if (entries[0].isIntersecting) {
					displayLimit += 50;
				}
			},
			{ rootMargin: '200px' }
		);
		observer.observe(scrollAnchor);
		return () => observer.disconnect();
	});

	// 検索フィルタリング
	const filteredCourses = $derived.by(() => {
		const q = searchQuery.trim().toLowerCase();

		let list = publicCourses;

		// 絞り込みフィルターの適用
		if (filterDay !== 'all') {
			list = list.filter((c) => c.dayPeriods?.some((dp) => dp.day === filterDay));
		}
		if (filterPeriod !== 'all') {
			const pNum = Number(filterPeriod);
			list = list.filter((c) => c.dayPeriods?.some((dp) => dp.period === pNum));
		}
		if (filterType !== 'all') {
			list = list.filter((c) => c.type === filterType);
		}
		if (filterYear !== 'all') {
			const yNum = Number(filterYear);
			list = list.filter((c) => c.year === yNum);
		}

		// ワード検索の適用
		if (!q) {
			return list.slice(0, displayLimit); // 軽量化
		}
		return list
			.filter(
				(c) =>
					c.name.toLowerCase().includes(q) ||
					(c.teacher && c.teacher.toLowerCase().includes(q)) ||
					c.id.toLowerCase().includes(q)
			)
			.slice(0, displayLimit);
	});

	// シラバス外部リンクを開く
	function handleOpenSyllabus(pc: PublicCourse) {
		const syllabusUrl =
			pc.references?.[0]?.url ||
			`https://kyoumu.office.uec.ac.jp/syllabus/2026/31/31_${pc.code}.html`;
		void openLink(syllabusUrl, true);
	}

	// 曜日時限ラベルのパース
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

	// モーダルと登録ステート
	let showRegisterModal = $state(false);
	let registerTargetCourse = $state<PublicCourse | null>(null);
	let selectedColor = $state('blue');

	// 時間割登録済みヘルパー
	const isRegistered = $derived.by(() => {
		return (cId: string) => userDataStore.courses.some((rc) => rc.syllabusId === cId);
	});

	function handleEditRegistered(cId: string) {
		const rc = userDataStore.courses.find((x) => x.syllabusId === cId);
		if (rc) {
			void goto(`/timetable?editCourseId=${rc.id}`);
		}
	}

	// 登録ボタン押下時: モーダルを開いてカラー選択のデフォルトをロード
	function handleRegisterClick(pc: PublicCourse) {
		registerTargetCourse = pc;
		selectedColor = themeStore.pack.palette.courseColors[0]?.id || 'blue';
		showRegisterModal = true;
	}

	// モーダルでの登録確定
	async function confirmRegister() {
		if (!registerTargetCourse) return;
		const pc = registerTargetCourse;
		try {
			const dp = pc.dayPeriods?.[0] || { day: 'mon' as DayOfWeek, period: 1 as Period };

			await userDataStore.addCourse({
				name: pc.name,
				teacher: pc.teacher || '',
				classroom: pc.classroom || '',
				day: dp.day as DayOfWeek,
				period: dp.period as Period,
				credits: (pc.credits || 2) as any,
				color: selectedColor,
				type: pc.type === 'required' ? 'required' : 'elective',
				span: 1,
				syllabusId: pc.id,
				memo: ''
			});

			uiStore.toast(m.syllabus_added_to_timetable({ name: pc.name }), 'success');
		} catch (err) {
			console.error(err);
			uiStore.toast(m.toast_error(), 'error');
		} finally {
			showRegisterModal = false;
			registerTargetCourse = null;
		}
	}
</script>

<svelte:head>
	<title>{m.more_syllabus()} | OpenYASKE</title>
</svelte:head>

<PageHeader title={m.more_syllabus()} />

<Container class="pb-10">
	<div class="space-y-4 py-4">
		<!-- 検索とフィルターの固定コンテナ -->
		<div
			class="sticky top-0 z-10 bg-[var(--color-surface-page)] py-3 border-b border-[var(--color-surface-border)] space-y-4 mb-2"
		>
			<!-- 検索バー -->
			<TextField
				bind:value={searchQuery}
				placeholder={m.syllabus_search_placeholder()}
				type="text"
				leadingIcon={Search}
			/>

			<!-- 絞り込み条件（アコーディオン） -->
			<div
				class="border border-[var(--color-surface-border)] rounded-card bg-[var(--color-surface-card)] p-3"
			>
				<button
					type="button"
					class="flex w-full items-center justify-between text-xs font-bold text-[var(--color-nav-active)] hover:text-[var(--color-primary-600)] transition-colors"
					onclick={() => (showFilter = !showFilter)}
				>
					<span class="flex items-center gap-1.5">
						<span>絞り込みフィルター</span>
						{#if hasActiveFilters}
							<span class="h-2 w-2 rounded-full bg-[var(--color-primary-500)]"></span>
						{/if}
					</span>
					{#if showFilter}
						<ChevronUp size={16} />
					{:else}
						<ChevronDown size={16} />
					{/if}
				</button>

				{#if showFilter}
					<div class="grid grid-cols-2 gap-3 pt-3 sm:grid-cols-4">
						<!-- 曜日 -->
						<div class="space-y-1">
							<label for="filter-day" class="text-[10px] font-bold text-[var(--color-nav-inactive)]"
								>曜日</label
							>
							<select
								id="filter-day"
								class="w-full rounded-chip border border-[var(--color-surface-border)] bg-[var(--color-surface-card)] px-3 py-1.5 text-xs font-medium text-[var(--color-nav-active)] focus:outline-none focus:border-[var(--color-primary-500)]"
								bind:value={filterDay}
							>
								<option value="all">すべて</option>
								<option value="mon">月曜日</option>
								<option value="tue">火曜日</option>
								<option value="wed">水曜日</option>
								<option value="thu">木曜日</option>
								<option value="fri">金曜日</option>
								<option value="sat">土曜日</option>
								<option value="sun">日曜日</option>
							</select>
						</div>

						<!-- 時限 -->
						<div class="space-y-1">
							<label
								for="filter-period"
								class="text-[10px] font-bold text-[var(--color-nav-inactive)]">時限</label
							>
							<select
								id="filter-period"
								class="w-full rounded-chip border border-[var(--color-surface-border)] bg-[var(--color-surface-card)] px-3 py-1.5 text-xs font-medium text-[var(--color-nav-active)] focus:outline-none focus:border-[var(--color-primary-500)]"
								bind:value={filterPeriod}
							>
								<option value="all">すべて</option>
								<option value="1">1限</option>
								<option value="2">2限</option>
								<option value="3">3限</option>
								<option value="4">4限</option>
								<option value="5">5限</option>
								<option value="6">6限</option>
								<option value="7">7限</option>
							</select>
						</div>

						<!-- 種別 -->
						<div class="space-y-1">
							<label
								for="filter-type"
								class="text-[10px] font-bold text-[var(--color-nav-inactive)]">種別</label
							>
							<select
								id="filter-type"
								class="w-full rounded-chip border border-[var(--color-surface-border)] bg-[var(--color-surface-card)] px-3 py-1.5 text-xs font-medium text-[var(--color-nav-active)] focus:outline-none focus:border-[var(--color-primary-500)]"
								bind:value={filterType}
							>
								<option value="all">すべて</option>
								<option value="required">必修</option>
								<option value="elective">選択</option>
								<option value="free">自由選択</option>
							</select>
						</div>

						<!-- 開講年度 -->
						<div class="space-y-1">
							<label
								for="filter-year"
								class="text-[10px] font-bold text-[var(--color-nav-inactive)]">年度</label
							>
							<select
								id="filter-year"
								class="w-full rounded-chip border border-[var(--color-surface-border)] bg-[var(--color-surface-card)] px-3 py-1.5 text-xs font-medium text-[var(--color-nav-active)] focus:outline-none focus:border-[var(--color-primary-500)]"
								bind:value={filterYear}
							>
								<option value="all">すべて</option>
								<option value="2025">2025年度</option>
								<option value="2026">2026年度</option>
							</select>
						</div>
					</div>
				{/if}
			</div>
		</div>

		{#if publicCourses.length === 0}
			<div
				class="rounded-card border border-[var(--color-surface-border)] bg-[var(--color-surface-card)] p-8 text-center space-y-2"
			>
				<BookOpen size={40} class="mx-auto text-[var(--color-nav-inactive)]" />
				<div class="text-sm font-bold text-[var(--color-nav-active)]">
					シラバスデータがロードされていません
				</div>
				<p class="text-xs text-[var(--color-nav-inactive)]">
					ネットワークの接続状態を確認するか、しばらく待ってから再度お試しください。
				</p>
			</div>
		{:else if filteredCourses.length === 0}
			<div class="py-12 text-center text-sm text-[var(--color-nav-inactive)]">
				{m.syllabus_no_course()}
			</div>
		{:else}
			<div class="grid grid-cols-1 gap-3 sm:grid-cols-2">
				{#each filteredCourses as c (c.id)}
					<div
						class="flex flex-col justify-between rounded-card border border-[var(--color-surface-border)] bg-[var(--color-surface-card)] p-4 group transition-colors hover:bg-[var(--color-surface-muted)]"
					>
						<button
							type="button"
							class="text-left flex-1 min-w-0"
							onclick={() => handleOpenSyllabus(c)}
						>
							<div class="flex items-center gap-2">
								<h4
									class="font-bold text-sm text-[var(--color-nav-active)] group-hover:text-[var(--color-primary-500)] transition-colors truncate"
								>
									{c.name}
								</h4>
								{#if c.type === 'required'}
									<span
										class="text-[9px] font-bold bg-[var(--color-danger-50)] text-[var(--color-danger-600)] rounded px-1 shrink-0"
									>
										{m.course_required()}
									</span>
								{:else if c.type}
									<span
										class="text-[9px] font-bold bg-[var(--color-surface-muted)] text-[var(--color-nav-inactive)] rounded px-1 shrink-0"
									>
										{c.type === 'elective' ? m.course_elective() : m.course_free()}
									</span>
								{/if}
							</div>

							<p class="text-xs text-[var(--color-nav-inactive)] mt-1 font-mono">
								{c.id}
							</p>

							<div
								class="flex flex-wrap gap-x-4 gap-y-1.5 mt-3 text-xs text-[var(--color-nav-inactive)] font-medium"
							>
								{#if c.teacher}
									<div class="flex items-center gap-1">
										<User size={12} />
										<span>{c.teacher}</span>
									</div>
								{/if}
								<div class="flex items-center gap-1">
									<Clock size={12} />
									<span>{formatDayPeriod(c.dayPeriods || [])}</span>
								</div>
								{#if c.credits}
									<div class="flex items-center gap-1">
										<GraduationCap size={12} />
										<span>{c.credits} {m.course_credits()}</span>
									</div>
								{/if}
							</div>
						</button>

						<div
							class="flex flex-col sm:flex-row items-stretch sm:items-center justify-end gap-2 border-t border-[var(--color-surface-border)] mt-3 pt-3"
						>
							<Button
								variant="secondary"
								size="sm"
								class="w-full sm:w-auto"
								onclick={() => handleOpenSyllabus(c)}
							>
								<ExternalLink size={14} class="mr-1 shrink-0" />
								<span class="truncate">{m.syllabus_detail()}</span>
							</Button>
							{#if isRegistered(c.id)}
								<Button
									variant="secondary"
									size="sm"
									class="w-full sm:w-auto border border-[var(--color-surface-border)] bg-[var(--color-surface-card)] hover:bg-[var(--color-surface-muted)]"
									onclick={() => handleEditRegistered(c.id)}
								>
									<Check size={14} class="mr-1 text-[var(--color-success-500)] shrink-0" />
									<span class="truncate">{m.syllabus_already_registered()}</span>
								</Button>
							{:else}
								<Button
									variant="primary"
									size="sm"
									class="w-full sm:w-auto"
									onclick={() => handleRegisterClick(c)}
								>
									<Plus size={14} class="shrink-0" />
									<span class="truncate">{m.syllabus_add_to_timetable()}</span>
								</Button>
							{/if}
						</div>
					</div>
				{/each}
			</div>

			<!-- スクロール検知用アンカー (さらにデータがある場合のみ表示) -->
			{#if publicCourses.length > displayLimit}
				<div
					bind:this={scrollAnchor}
					class="py-6 text-center text-xs text-[var(--color-nav-inactive)] font-medium"
				>
					スクロールしてさらに読み込む...
				</div>
			{/if}
		{/if}
	</div>
</Container>

<svelte:window onscroll={handleScroll} />

{#if showScrollTop}
	<button
		type="button"
		onclick={scrollToTop}
		class="fixed bottom-24 right-4 z-50 flex h-12 w-12 items-center justify-center rounded-full bg-[var(--color-primary-500)] text-white shadow-lg transition-transform hover:scale-105 active:scale-95 focus:outline-none"
		aria-label="上に戻る"
	>
		<ArrowUp size={20} />
	</button>
{/if}

<Modal
	open={showRegisterModal}
	onclose={() => (showRegisterModal = false)}
	title={m.syllabus_add_to_timetable()}
>
	<div class="space-y-4">
		<div>
			<p class="text-sm font-bold text-[var(--color-nav-active)]">
				{registerTargetCourse?.name}
			</p>
			{#if registerTargetCourse?.teacher}
				<p class="text-xs text-[var(--color-nav-inactive)] mt-0.5">
					{registerTargetCourse.teacher}
				</p>
			{/if}
		</div>

		<div class="space-y-2">
			<ColorPicker
				bind:value={selectedColor}
				palette={themeStore.pack.palette.courseColors}
				kind="course"
				allowCustom={true}
				allowNone={false}
				label={m.syllabus_select_color()}
			/>
		</div>

		<div class="flex gap-2 pt-2">
			<Button
				variant="secondary"
				size="md"
				class="flex-1"
				onclick={() => (showRegisterModal = false)}
			>
				{m.action_cancel()}
			</Button>
			<Button variant="primary" size="md" class="flex-1" onclick={confirmRegister}>
				{m.action_add()}
			</Button>
		</div>
	</div>
</Modal>
