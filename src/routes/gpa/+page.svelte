<script lang="ts">
	import { resolve } from '$app/paths';
	import PageHeader from '$lib/components/PageHeader.svelte';
	import Container from '$lib/components/Container.svelte';
	import Button from '$lib/components/Button.svelte';
	import TextField from '$lib/components/TextField.svelte';
	import IconButton from '$lib/components/IconButton.svelte';
	import BentoCard from '$lib/components/BentoCard.svelte';
	import { m } from '$lib/paraglide/messages';
	import { Calculator, Plus, Trash2 } from '@lucide/svelte';
	import { calcGpa } from '$lib/gpa';
	import { userDataStore } from '$lib/stores';

	type GradeRow = {
		id: string;
		name: string;
		credits: string;
		point: string;
	};

	const gradeOptions = [
		{ value: '4', label: 'S / 4.0' },
		{ value: '3', label: 'A / 3.0' },
		{ value: '2', label: 'B / 2.0' },
		{ value: '1', label: 'C / 1.0' },
		{ value: '0', label: 'D・F / 0.0' }
	];

	function createRow(): GradeRow {
		return { id: crypto.randomUUID(), name: '', credits: '2', point: '4' };
	}

	function createRowFromCourse(c: { name: string; credits: number }): GradeRow {
		return { id: crypto.randomUUID(), name: c.name, credits: String(c.credits), point: '4' };
	}

	let rows = $state<GradeRow[]>([createRow(), createRow(), createRow()]);

	const totals = $derived(
		calcGpa(
			rows.map((row) => ({ credits: Number(row.credits) || 0, point: Number(row.point) || 0 }))
		)
	);
	const totalCredits = $derived(totals.totalCredits);
	const totalPoints = $derived(totals.totalPoints);
	const gpa = $derived(totals.gpa);

	/** 履修科目を GPA 行に取り込む（重複名は追加しない） */
	function importCourses() {
		const existing = new Set(rows.map((r) => r.name.trim()).filter(Boolean));
		const fresh = userDataStore.courses
			.filter((c) => c.name && !existing.has(c.name))
			.map((c) => createRowFromCourse(c));
		if (fresh.length > 0) rows = [...rows, ...fresh];
	}

	function addRow() {
		rows = [...rows, createRow()];
	}

	function removeRow(id: string) {
		rows = rows.length <= 1 ? rows : rows.filter((row) => row.id !== id);
	}

	function resetRows() {
		rows = [createRow(), createRow(), createRow()];
	}
</script>

<svelte:head><title>GPA | OpenYASKE</title></svelte:head>

<PageHeader title="GPA" />

<Container size="narrow" class="space-y-6 pb-20 py-4">
	<!-- Hero Score Bento Card -->
	<BentoCard padding="p-6 md:p-8" class="w-full relative overflow-hidden">
		<div class="flex flex-col sm:flex-row items-center justify-between gap-6">
			<div class="flex items-center gap-5">
				<div
					class="flex h-16 w-16 items-center justify-center rounded-2xl bg-[var(--color-primary-500)]/15 text-[var(--color-primary-600)] shrink-0 shadow-[var(--shadow-style-sm)] border border-[var(--color-primary-500)]/20"
				>
					<Calculator size={32} />
				</div>
				<div>
					<p class="text-xs font-bold uppercase tracking-wider text-[var(--color-nav-inactive)]">
						計算結果 (合計 {totalCredits} 単位)
					</p>
					<div class="flex items-baseline gap-2">
						<span
							class="text-4xl md:text-5xl font-black leading-tight tracking-tight font-mono text-[var(--color-primary-600)]"
						>
							{gpa.toFixed(2)}
						</span>
						<span class="text-sm font-bold text-[var(--color-nav-inactive)]">/ 4.00</span>
					</div>
				</div>
			</div>

			<div class="w-full sm:w-36 space-y-1.5">
				<div class="flex justify-between text-[11px] font-bold text-[var(--color-nav-inactive)]">
					<span>達成度</span>
					<span>{Math.round((gpa / 4.0) * 100)}%</span>
				</div>
				<div
					class="h-2.5 w-full rounded-full bg-[var(--color-surface-muted)] overflow-hidden p-0.5 border border-[var(--color-surface-border)]/40"
				>
					<div
						class="h-full rounded-full bg-[var(--color-primary-500)] transition-all duration-300"
						style:width="{Math.min(100, (gpa / 4.0) * 100)}%"
					></div>
				</div>
			</div>
		</div>
	</BentoCard>

	<!-- 入力行リスト -->
	<div class="space-y-3">
		{#each rows as row (row.id)}
			<BentoCard padding="p-4 md:p-5" class="w-full border border-[var(--color-surface-border)]/60">
				<div class="mb-3 flex items-center justify-between gap-3">
					<div class="flex-1">
						<TextField
							bind:value={row.name}
							label={m.external_link_name()}
							placeholder="例: 微積分学"
						/>
					</div>
					<div class="pt-5">
						<IconButton
							icon={Trash2}
							ariaLabel={m.action_delete()}
							onclick={() => removeRow(row.id)}
							variant="danger"
							size="sm"
							disabled={rows.length <= 1}
						/>
					</div>
				</div>
				<div class="grid grid-cols-2 gap-3">
					<TextField bind:value={row.credits} type="number" label="単位数" />
					<TextField bind:value={row.point} type="select" label="評価" options={gradeOptions} />
				</div>
			</BentoCard>
		{/each}
	</div>

	<!-- アクションボタン -->
	<div class="grid grid-cols-2 gap-3 pt-2">
		<Button variant="secondary" size="md" onclick={resetRows}>{m.theme_pick_reset()}</Button>
		<Button variant="primary" size="md" onclick={addRow}>
			<Plus size={18} />
			{m.external_link_add()}
		</Button>
	</div>

	{#if userDataStore.courses.length > 0}
		<Button variant="secondary" size="md" class="w-full" onclick={importCourses}>
			履修科目を取り込む ({userDataStore.courses.length}件)
		</Button>
	{/if}

	<Button href={resolve('/')} variant="ghost" size="md" class="w-full">{m.nav_home()}</Button>
</Container>
