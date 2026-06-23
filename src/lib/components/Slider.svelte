<!--
  Slider.svelte
  数値スライダー。範囲とステップを指定可能。
-->
<script lang="ts">
	interface Props {
		value: number;
		min: number;
		max: number;
		step?: number;
		onchange?: (value: number) => void;
		label?: string;
		showValue?: boolean;
		formatValue?: (v: number) => string;
		disabled?: boolean;
		id?: string;
	}

	let {
		value = $bindable(0),
		min,
		max,
		step = 1,
		onchange,
		label,
		showValue = true,
		formatValue,
		disabled = false,
		id
	}: Props = $props();

	function handleInput(e: Event) {
		const target = e.currentTarget as HTMLInputElement;
		const v = Number(target.value);
		value = v;
		onchange?.(v);
	}

	const displayValue = $derived(formatValue ? formatValue(value) : String(value));
	const percent = $derived(max === min ? 0 : ((value - min) / (max - min)) * 100);
</script>

<div class="w-full">
	{#if label || showValue}
		<div class="mb-1.5 flex items-center justify-between text-sm">
			{#if label}
				<label for={id} class="text-[var(--color-nav-active)]">{label}</label>
			{/if}
			{#if showValue}
				<span class="font-mono text-[var(--color-nav-inactive)]">{displayValue}</span>
			{/if}
		</div>
	{/if}
	<div class="relative h-6">
		<!-- トラック -->
		<div
			class="absolute top-1/2 left-0 h-1.5 w-full -translate-y-1/2 rounded-full bg-[var(--color-surface-muted)]"
		></div>
		<!-- 塗り -->
		<div
			class="absolute top-1/2 h-1.5 -translate-y-1/2 rounded-full bg-[var(--color-primary-500)]"
			style="left: 0; width: {percent}%"
		></div>
		<!-- つまみ -->
		<input
			type="range"
			{min}
			{max}
			{step}
			{value}
			{disabled}
			{id}
			oninput={handleInput}
			class="absolute top-0 left-0 h-6 w-full appearance-none bg-transparent slider-thumb focus:outline-none"
			aria-label={label}
		/>
	</div>
</div>

<style>
	.slider-thumb::-webkit-slider-thumb {
		appearance: none;
		width: 1.25rem;
		height: 1.25rem;
		border-radius: 9999px;
		background: white;
		border: 2px solid var(--color-primary-500);
		cursor: pointer;
		box-shadow: 0 1px 3px rgba(0, 0, 0, 0.2);
	}
	.slider-thumb::-moz-range-thumb {
		width: 1.25rem;
		height: 1.25rem;
		border-radius: 9999px;
		background: white;
		border: 2px solid var(--color-primary-500);
		cursor: pointer;
		box-shadow: 0 1px 3px rgba(0, 0, 0, 0.2);
	}
	.slider-thumb:disabled {
		opacity: 0.5;
		cursor: not-allowed;
	}
</style>
