<!--
  StatCard.svelte
  コンパクトな統計カード。ラベル + 値 + 単位。
  出席画面やホーム画面で使用。
-->
<script lang="ts">
	interface Props {
		/** ラベル（i18n key または文字列） */
		label: string;
		/** 表示値 */
		value: string | number;
		/** 単位（例: "%" "回"） */
		unit?: string;
		/** 値の色 (CSS 変数名、例: --color-primary-500) */
		valueColor?: string;
		/** 追加クラス */
		class?: string;
	}

	let {
		label,
		value,
		unit,
		valueColor = '--color-nav-active',
		class: klass = ''
	}: Props = $props();
</script>

<div
	class="flex flex-col gap-1 rounded-xl border border-[var(--color-surface-border)] bg-[var(--color-surface-card)] px-4 py-3 shadow-[var(--shadow-card)] {klass}"
>
	<span class="text-xs text-[var(--color-nav-inactive)]">{label}</span>
	<div class="flex items-baseline gap-1">
		<span class="text-2xl font-bold leading-none" style="color: var({valueColor});">
			{value}
		</span>
		{#if unit}
			<span class="text-sm text-[var(--color-nav-inactive)]">{unit}</span>
		{/if}
	</div>
</div>
