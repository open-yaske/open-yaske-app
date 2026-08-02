<!--
  StatCard.svelte
  コンパクトな統計カード。ラベル + 値 + 単位。
  出席画面やホーム画面で使用。
-->
<script lang="ts">
	import GlassContainer from './GlassContainer.svelte';
	import { styleStore } from '$lib/stores';
	import { resolveRadius } from '$lib/styles';

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

	// Style-aware corner radius
	const r = $derived(resolveRadius(styleStore.pack));
</script>

<GlassContainer
	variant="card"
	glow={true}
	distortion="medium"
	class="flex flex-col justify-between gap-1 {r.card} px-4 py-3.5 {klass}"
>
	<span class="text-xs font-medium text-[var(--color-nav-inactive)]">{label}</span>
	<div class="flex items-baseline gap-1">
		<span
			class="text-2xl md:text-3xl font-bold leading-none font-mono"
			style="color: var({valueColor});"
		>
			{value}
		</span>
		{#if unit}
			<span class="text-xs font-semibold text-[var(--color-nav-inactive)]">{unit}</span>
		{/if}
	</div>
</GlassContainer>
