<script lang="ts">
	import { resolve } from '$app/paths';
	import PageHeader from '$lib/components/PageHeader.svelte';
	import Container from '$lib/components/Container.svelte';
	import Button from '$lib/components/Button.svelte';
	import TextField from '$lib/components/TextField.svelte';
	import IconButton from '$lib/components/IconButton.svelte';
	import { Calculator, Plus, Trash2 } from '@lucide/svelte';

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

	let rows = $state<GradeRow[]>([createRow(), createRow(), createRow()]);

	const totalCredits = $derived(
		rows.reduce((sum, row) => sum + Math.max(0, Number(row.credits) || 0), 0)
	);
	const totalPoints = $derived(
		rows.reduce((sum, row) => {
			const credits = Math.max(0, Number(row.credits) || 0);
			const point = Math.max(0, Number(row.point) || 0);
			return sum + credits * point;
		}, 0)
	);
	const gpa = $derived(totalCredits === 0 ? 0 : totalPoints / totalCredits);

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

<svelte:head><title>GPA 計算機 | open-nexus</title></svelte:head>

<PageHeader title="GPA 計算機" />

<Container size="narrow" class="space-y-5 py-4">
	<div
		class="rounded-xl border border-[var(--color-surface-border)] bg-[var(--color-primary-50)] p-5 text-[var(--color-primary-800)] shadow-sm"
	>
		<div class="flex items-center gap-3">
			<span class="flex h-11 w-11 items-center justify-center rounded-full bg-white">
				<Calculator size={22} />
			</span>
			<div>
				<p class="text-xs font-semibold text-[var(--color-primary-700)]">
					合計 {totalCredits} 単位
				</p>
				<p class="text-3xl font-black leading-tight">{gpa.toFixed(2)}</p>
			</div>
		</div>
	</div>

	<div class="space-y-3">
		{#each rows as row (row.id)}
			<div
				class="rounded-xl border border-[var(--color-surface-border)] bg-[var(--color-surface-card)] p-4"
			>
				<div class="mb-3 flex items-center justify-between gap-3">
					<TextField bind:value={row.name} label="科目名" placeholder="任意" />
					<IconButton
						icon={Trash2}
						ariaLabel="削除"
						onclick={() => removeRow(row.id)}
						variant="subtle"
						size="sm"
						disabled={rows.length <= 1}
					/>
				</div>
				<div class="grid grid-cols-2 gap-3">
					<TextField bind:value={row.credits} type="number" label="単位数" />
					<TextField bind:value={row.point} type="select" label="評価" options={gradeOptions} />
				</div>
			</div>
		{/each}
	</div>

	<div class="grid grid-cols-2 gap-3">
		<Button variant="secondary" size="md" onclick={resetRows}>リセット</Button>
		<Button variant="primary" size="md" onclick={addRow}>
			<Plus size={16} />
			行を追加
		</Button>
	</div>

	<Button href={resolve('/')} variant="ghost" size="md" class="w-full">ホームへ戻る</Button>
</Container>
