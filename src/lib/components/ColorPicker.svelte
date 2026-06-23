<!--
  ColorPicker.svelte
  カラーピッカー。プリセット (パレット) + カスタム HEX 入力 + "なし" オプション。
  ColorToken (id | "custom:<hex>" | "none") を扱う。
-->
<script lang="ts">
	import type { ColorEntry } from '$lib/theme';
	import { NONE_TOKEN, isCustomColor } from '$lib/theme';
	import { RAINBOW_GRADIENT, DEFAULT_CUSTOM_HEX } from '$lib/theme/picker-gradient';

	interface Props {
		value: string;
		palette: ColorEntry[];
		onchange?: (value: string) => void;
		label?: string;
		/** 選択するパレットの種類 */
		kind?: 'course' | 'todo' | 'event';
		/** カスタム HEX ピッカーを表示する */
		allowCustom?: boolean;
		/** "なし" 選択肢を表示する */
		allowNone?: boolean;
		id?: string;
	}

	let {
		value = $bindable(NONE_TOKEN),
		palette,
		onchange,
		label,
		kind = 'course',
		allowCustom = true,
		allowNone = true,
		id
	}: Props = $props();

	const customHex = $derived(
		isCustomColor(value) ? value.slice('custom:'.length) : DEFAULT_CUSTOM_HEX
	);

	function selectPreset(idx: number) {
		value = `index:${kind}:${idx}`;
		onchange?.(value);
	}

	function selectNone() {
		value = NONE_TOKEN;
		onchange?.(value);
	}

	function selectCustom() {
		value = `custom:${customHex}`;
		onchange?.(value);
	}

	function handleCustomInput(e: Event) {
		const target = e.currentTarget as HTMLInputElement;
		if (/^#[0-9a-fA-F]{6}$/.test(target.value)) {
			value = `custom:${target.value}`;
			onchange?.(value);
		}
	}
</script>

<div class="w-full">
	{#if label}
		<label for={id} class="mb-1.5 block text-sm font-medium text-[var(--color-nav-active)]"
			>{label}</label
		>
	{/if}
	<div class="flex flex-wrap items-center gap-2">
		{#each palette as c, idx (c.id)}
			{@const selected = value === `index:${kind}:${idx}` || value === c.id}
			<button
				type="button"
				class="h-8 w-8 rounded-full border-2 transition-transform hover:scale-110 focus:outline-none focus-visible:ring-2 focus-visible:ring-[var(--color-primary-500)] focus-visible:ring-offset-2"
				class:border-[var(--color-primary-500)]={selected}
				class:border-[var(--color-surface-border)]={!selected}
				style="background-color: {c.hex}"
				aria-label={c.name}
				aria-pressed={selected}
				onclick={() => selectPreset(idx)}
			></button>
		{/each}
		{#if allowNone}
			{@const selected = value === NONE_TOKEN}
			<button
				type="button"
				class="flex h-8 w-8 items-center justify-center rounded-full border-2 border-dashed transition hover:bg-[var(--color-surface-muted)] focus:outline-none focus-visible:ring-2 focus-visible:ring-[var(--color-primary-500)]"
				class:border-[var(--color-primary-500)]={selected}
				class:border-[var(--color-surface-border)]={!selected}
				aria-label="None"
				aria-pressed={selected}
				onclick={selectNone}
			>
				<span class="text-xs text-[var(--color-nav-inactive)]">/</span>
			</button>
		{/if}
		{#if allowCustom}
			<label
				class="relative h-8 w-8 cursor-pointer overflow-hidden rounded-full border-2 transition hover:scale-110 focus-within:ring-2 focus-within:ring-[var(--color-primary-500)] focus-within:ring-offset-2"
				class:border-[var(--color-primary-500)]={isCustomColor(value)}
				class:border-[var(--color-surface-border)]={!isCustomColor(value)}
				style={isCustomColor(value)
					? `background-color: ${customHex}`
					: `background: ${RAINBOW_GRADIENT}`}
				aria-label="Custom color"
			>
				<input
					type="color"
					value={customHex}
					oninput={handleCustomInput}
					onchange={selectCustom}
					class="absolute inset-0 cursor-pointer opacity-0"
				/>
			</label>
		{/if}
	</div>
</div>
