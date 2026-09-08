<script lang="ts">
	import { publicDataStore, userDataStore, uiStore, themeStore } from '$lib/stores';
	import { m } from '$lib/paraglide/messages';
	import PageHeader from '$lib/components/PageHeader.svelte';
	import Container from '$lib/components/Container.svelte';
	import TextField from '$lib/components/TextField.svelte';
	import Button from '$lib/components/Button.svelte';
	import BentoCard from '$lib/components/BentoCard.svelte';
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
	import { defaultColorId, resolveColorHex } from '$lib/theme';
	import { getLocale } from '$lib/paraglide/runtime';
	import { getLocalizedText } from '$lib/theme/utils';
	import ColorPicker from '$lib/components/ColorPicker.svelte';
	import { styleStore } from '$lib/stores';

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
			return list.slice(0, displayLimit);
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
	let selectedColor = $state('');

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
		selectedColor = defaultColorId(themeStore.pack, 'course');
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

	const dayOptions = [
		{ value: 'all', label: 'すべて' },
		{ value: 'mon', label: '月曜日' },
		{ value: 'tue', label: '火曜日' },
		{ value: 'wed', label: '水曜日' },
		{ value: 'thu', label: '木曜日' },
		{ value: 'fri', label: '金曜日' },
		{ value: 'sat', label: '土曜日' },
		{ value: 'sun', label: '日曜日' }
	];

	const periodOptions = [
		{ value: 'all', label: 'すべて' },
		{ value: '1', label: '1限' },
		{ value: '2', label: '2限' },
		{ value: '3', label: '3限' },
		{ value: '4', label: '4限' },
		{ value: '5', label: '5限' },
		{ value: '6', label: '6限' },
		{ value: '7', label: '7限' }
	];

	const typeOptions = [
		{ value: 'all', label: 'すべて' },
		{ value: 'required', label: m.course_required() },
		{ value: 'elective', label: m.course_elective() },
		{ value: 'free', label: m.course_free() }
	];

	const yearOptions = [
		{ value: 'all', label: 'すべて' },
		{ value: '2026', label: '2026年度' },
		{ value: '2025', label: '2025年度' }
	];
</script>

<svelte:head>
	<title>{m.more_syllabus()} | OpenYASKE</title>
</svelte:head>

<PageHeader title={m.more_syllabus()} />

<Container class="pb-16">
	<div class="space-y-4 py-4">
		<!-- 検索とフィルターの固定コンテナ (すりガラス化) -->
		<div
			class="sticky top-[53px] z-10 bg-[var(--color-surface-page)]/80 backdrop-blur-xl py-3 border-b border-[var(--color-surface-border)]/40 space-y-4 mb-2 shadow-[var(--shadow-style-sm)]"
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
				class="border border-[var(--color-surface-border)]/80 rounded-card bg-[var(--color-surface-card)]/90 backdrop-blur-xl p-3.5 shadow-[var(--shadow-style-sm)]"
			>
				<button
					type="button"
					class="flex w-full items-center justify-between text-xs font-bold text-[var(--color-nav-active)] hover:text-[var(--color-primary-600)] transition-all cursor-pointer {styleStore
						.pack.press.card}"
					onclick={() => (showFilter = !showFilter)}
				>
					<span class="flex items-center gap-2">
						<span class="font-bold text-sm">絞り込みフィルター</span>
						{#if hasActiveFilters}
							<span
								class="inline-flex items-center px-2 py-0.5 rounded-full text-[10px] font-extrabold bg-[var(--color-primary-500)] text-white shadow-[var(--shadow-style-sm)]"
							>
								アクティブ
							</span>
						{/if}
					</span>
					{#if showFilter}
						<ChevronUp size={18} class="text-[var(--color-primary-500)]" />
					{:else}
						<ChevronDown size={18} class="text-[var(--color-nav-inactive)]" />
					{/if}
				</button>

				{#if showFilter}
					<div class="space-y-3 pt-3 border-t border-[var(--color-surface-border)]/40 mt-2">
						<!-- 曜日マトリクスチップ -->
						<div>
							<span
								class="text-[11px] font-semibold text-[var(--color-nav-inactive)] block mb-1.5 pl-1"
								>曜日</span
							>
							<div class="flex flex-wrap gap-1.5">
								{#each [{ val: 'all', label: 'すべて' }, { val: 'mon', label: '月' }, { val: 'tue', label: '火' }, { val: 'wed', label: '水' }, { val: 'thu', label: '木' }, { val: 'fri', label: '金' }] as d (d.val)}
									<button
										type="button"
										class="min-h-[36px] min-w-[44px] px-3.5 rounded-full text-xs font-semibold transition-all shadow-[var(--shadow-style-sm)] cursor-pointer {styleStore
											.pack.press.tab}"
										class:bg-[var(--color-primary-500)]={filterDay === d.val}
										class:text-white={filterDay === d.val}
										class:bg-[var(--color-surface-muted)]={filterDay !== d.val}
										class:text-[var(--color-nav-active)]={filterDay !== d.val}
										onclick={() => (filterDay = d.val)}
									>
										{d.label}
									</button>
								{/each}
							</div>
						</div>

						<!-- 時限マトリクスチップ -->
						<div>
							<span
								class="text-[11px] font-semibold text-[var(--color-nav-inactive)] block mb-1.5 pl-1"
								>時限</span
							>
							<div class="flex flex-wrap gap-1.5">
								{#each [{ val: 'all', label: 'すべて' }, { val: '1', label: '1限' }, { val: '2', label: '2限' }, { val: '3', label: '3限' }, { val: '4', label: '4限' }, { val: '5', label: '5限' }, { val: '6', label: '6限' }] as p (p.val)}
									<button
										type="button"
										class="min-h-[36px] min-w-[44px] px-3.5 rounded-full text-xs font-semibold transition-all shadow-[var(--shadow-style-sm)] cursor-pointer {styleStore
											.pack.press.tab}"
										class:bg-[var(--color-primary-500)]={filterPeriod === p.val}
										class:text-white={filterPeriod === p.val}
										class:bg-[var(--color-surface-muted)]={filterPeriod !== p.val}
										class:text-[var(--color-nav-active)]={filterPeriod !== p.val}
										onclick={() => (filterPeriod = p.val)}
									>
										{p.label}
									</button>
								{/each}
							</div>
						</div>

						<!-- その他のドロップダウン詳細フィルター -->
						<div class="grid grid-cols-2 gap-3 pt-1">
							<TextField bind:value={filterType} type="select" label="種別" options={typeOptions} />
							<TextField bind:value={filterYear} type="select" label="年度" options={yearOptions} />
						</div>
					</div>
				{/if}
			</div>
		</div>

		{#if publicCourses.length === 0}
			<BentoCard padding="p-8" class="text-center space-y-3">
				<BookOpen size={40} class="mx-auto text-[var(--color-nav-inactive)]" />
				<div class="text-sm font-bold text-[var(--color-nav-active)]">
					シラバスデータがロードされていません
				</div>
				<p class="text-xs text-[var(--color-nav-inactive)] max-w-sm mx-auto leading-relaxed">
					ネットワークの接続状態を確認するか、しばらく待ってから再度お試しください。
				</p>
			</BentoCard>
		{:else if filteredCourses.length === 0}
			<div class="py-12 text-center text-sm text-[var(--color-nav-inactive)] font-semibold">
				{m.syllabus_no_course()}
			</div>
		{:else}
			<div class="grid grid-cols-1 gap-4 sm:grid-cols-2">
				{#each filteredCourses as c (c.id)}
					<BentoCard padding="p-4" class="h-full flex flex-col justify-between group">
						<button
							type="button"
							class="text-left flex-1 min-w-0 cursor-pointer w-full"
							onclick={() => handleOpenSyllabus(c)}
						>
							<div class="flex items-center gap-2">
								<h4
									class="font-bold text-sm text-[var(--color-nav-active)] group-hover:text-[var(--color-primary-600)] transition-colors truncate flex-1"
								>
									{c.name}
								</h4>
								{#if c.type === 'required'}
									<span
										class="text-[9px] font-bold bg-[var(--color-danger-50)] text-[var(--color-danger-600)] rounded-md px-1.5 py-0.5 shrink-0"
									>
										{m.course_required()}
									</span>
								{:else if c.type}
									<span
										class="text-[9px] font-bold bg-[var(--color-surface-muted)] text-[var(--color-nav-inactive)] rounded-md px-1.5 py-0.5 shrink-0"
									>
										{c.type === 'elective' ? m.course_elective() : m.course_free()}
									</span>
								{/if}
							</div>

							<p class="text-xs text-[var(--color-nav-inactive)] mt-1 font-mono pl-0.5">
								{c.id}
							</p>

							<div
								class="flex flex-wrap gap-x-4 gap-y-1.5 mt-3.5 text-xs text-[var(--color-nav-inactive)] font-semibold"
							>
								{#if c.teacher}
									<div class="flex items-center gap-1">
										<User size={12} />
										<span class="truncate">{c.teacher}</span>
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
							class="flex items-center justify-end gap-2 border-t border-[var(--color-surface-border)]/50 mt-4 pt-3 w-full"
						>
							<Button
								variant="secondary"
								size="sm"
								class="flex-1 sm:flex-none"
								onclick={() => handleOpenSyllabus(c)}
							>
								<ExternalLink size={14} class="shrink-0" />
								<span class="truncate">{m.syllabus_detail()}</span>
							</Button>
							{#if isRegistered(c.id)}
								<Button
									variant="secondary"
									size="sm"
									class="flex-1 sm:flex-none border border-[var(--color-surface-border)]/80 bg-[var(--color-surface-card)]"
									onclick={() => handleEditRegistered(c.id)}
								>
									<Check size={14} class="text-[var(--color-success-500)] shrink-0" />
									<span class="truncate">{m.syllabus_already_registered()}</span>
								</Button>
							{:else}
								<Button
									variant="primary"
									size="sm"
									class="flex-1 sm:flex-none"
									onclick={() => handleRegisterClick(c)}
								>
									<Plus size={14} class="shrink-0" />
									<span class="truncate">{m.syllabus_add_to_timetable()}</span>
								</Button>
							{/if}
						</div>
					</BentoCard>
				{/each}
			</div>

			<!-- スクロール検知用アンカー (さらにデータがある場合のみ表示) -->
			{#if publicCourses.length > displayLimit}
				<div
					bind:this={scrollAnchor}
					class="py-8 text-center text-xs text-[var(--color-nav-inactive)] font-semibold"
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
		class="fixed bottom-24 right-4 z-50 flex h-12 w-12 items-center justify-center rounded-full bg-[var(--color-primary-500)] text-white shadow-[var(--shadow-style-lg)] transition-transform hover:scale-105 active:scale-95 focus:outline-none cursor-pointer {styleStore
			.pack.press.icon}"
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
