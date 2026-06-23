<!--
  ThemePreview.svelte
  テーマパックのライブプレビュー。packToCssVars で CSS 変数を生成し、
  隔離コンテナ内にサンプル UI (ボタン / カード / テキスト / プログレスリング / 出席バッジ) を表示。
  すべて CSS 変数経由で色を参照 (raw HEX 不使用)。
-->
<script lang="ts">
	import type { ThemePack } from '$lib/theme';
	import { packToCssVars } from '$lib/theme';
	import { m } from '$lib/paraglide/messages';

	interface Props {
		pack: ThemePack;
	}

	let { pack }: Props = $props();

	const cssVars = $derived.by(() => {
		const vars = packToCssVars(pack);
		return Object.entries(vars)
			.map(([k, v]) => `${k}: ${v}`)
			.join('; ');
	});

	const courseColors = $derived(pack.palette.courseColors.slice(0, 5));
	const attendanceStates = [
		{ key: 'present', label: m.attendance_present() },
		{ key: 'absent', label: m.attendance_absent() },
		{ key: 'late', label: m.attendance_late() },
		{ key: 'excused', label: m.attendance_excused() },
		{ key: 'cancelled', label: m.attendance_cancelled() }
	] as const;
</script>

<div
	class="overflow-hidden rounded-[var(--radius-card)] border border-[var(--color-surface-border)] bg-[var(--color-surface-page)] p-4"
	style={cssVars}
>
	<!-- sample header -->
	<div class="mb-4 flex items-center justify-between">
		<h3 class="text-base font-semibold text-[var(--color-nav-active)]">{pack.name || 'Theme'}</h3>
		<span
			class="rounded-[var(--radius-chip)] bg-[var(--color-primary-100)] px-2 py-0.5 text-xs font-medium text-[var(--color-primary-700)]"
		>
			v{pack.version}
		</span>
	</div>

	<!-- sample buttons -->
	<div class="mb-4 flex flex-wrap gap-2">
		<button
			type="button"
			class="rounded-[var(--radius-chip)] bg-[var(--color-primary-500)] px-4 py-2 text-sm font-medium text-white shadow-sm transition-colors hover:bg-[var(--color-primary-600)]"
		>
			Primary Button
		</button>
		<button
			type="button"
			class="rounded-[var(--radius-chip)] border border-[var(--color-surface-border)] bg-[var(--color-surface-card)] px-4 py-2 text-sm font-medium text-[var(--color-nav-active)] transition-colors hover:bg-[var(--color-surface-muted)]"
		>
			Secondary
		</button>
	</div>

	<!-- sample card -->
	<div
		class="mb-4 rounded-[var(--radius-card)] border border-[var(--color-surface-border)] bg-[var(--color-surface-card)] p-3"
		style="box-shadow: var(--shadow-card)"
	>
		<div class="mb-2 flex items-center justify-between">
			<span class="text-sm font-medium text-[var(--color-nav-active)]">Sample Card</span>
			<span class="text-xs text-[var(--color-surface-muted-foreground)]">3 credits</span>
		</div>
		<p class="text-xs text-[var(--color-surface-muted-foreground)]">
			This is a sample card with surface colors and shadow from the theme pack.
		</p>
	</div>

	<!-- progress ring + text -->
	<div class="mb-4 flex items-center gap-4">
		<div class="relative flex h-16 w-16 items-center justify-center">
			<svg width="64" height="64" class="-rotate-90">
				<circle
					cx="32"
					cy="32"
					r="26"
					fill="none"
					stroke="var(--color-surface-border)"
					stroke-width="6"
				/>
				<circle
					cx="32"
					cy="32"
					r="26"
					fill="none"
					stroke="var(--color-primary-500)"
					stroke-width="6"
					stroke-linecap="round"
					stroke-dasharray="163.36"
					stroke-dashoffset="40.84"
					style="transition: stroke-dashoffset 0.3s ease;"
				/>
			</svg>
			<span class="absolute text-sm font-semibold text-[var(--color-nav-active)]">75%</span>
		</div>
		<div class="flex-1">
			<div class="text-sm font-medium text-[var(--color-nav-active)]">Attendance Rate</div>
			<div class="text-xs text-[var(--color-surface-muted-foreground)]">12 / 16 classes</div>
		</div>
	</div>

	<!-- course color chips -->
	<div class="mb-4">
		<div class="mb-1.5 text-xs font-medium text-[var(--color-nav-inactive)]">Course Colors</div>
		<div class="flex flex-wrap gap-1.5">
			{#each courseColors as c (c.id)}
				<span
					class="inline-flex h-6 w-6 items-center justify-center rounded-md text-[10px] font-medium text-white"
					style="background-color: {c.hex}"
				>
					{c.id.charAt(0).toUpperCase()}
				</span>
			{/each}
		</div>
	</div>

	<!-- attendance badges -->
	<div>
		<div class="mb-1.5 text-xs font-medium text-[var(--color-nav-inactive)]">Attendance Badges</div>
		<div class="flex flex-wrap gap-1.5">
			{#each attendanceStates as state (state.key)}
				{@const a = pack.attendance[state.key]}
				<span
					class="inline-flex items-center justify-center rounded-md px-2 py-1 text-xs font-semibold"
					style="background-color: {a.bg}; color: {a.fg}"
				>
					{state.label}
				</span>
			{/each}
		</div>
	</div>
</div>
