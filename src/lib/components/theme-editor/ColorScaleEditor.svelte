<!--
  ColorScaleEditor.svelte
  10段階カラースケールエディター (50, 100, 200, ..., 900)。
  各段階にネイティブ color input + HEX テキスト入力 + スウォッチプレビュー。
-->
<script lang="ts">
	import type { ColorScale } from '$lib/theme';

	interface Props {
		scale: ColorScale;
		onchange?: (scale: ColorScale) => void;
	}

	let { scale = $bindable(), onchange }: Props = $props();

	const steps: (keyof ColorScale)[] = [50, 100, 200, 300, 400, 500, 600, 700, 800, 900];

	function update(step: keyof ColorScale, value: string) {
		scale = { ...scale, [step]: value };
		onchange?.(scale);
	}

	function handleColorInput(e: Event, step: keyof ColorScale) {
		const target = e.currentTarget as HTMLInputElement;
		update(step, target.value);
	}

	function handleTextBlur(e: FocusEvent, step: keyof ColorScale) {
		const target = e.currentTarget as HTMLInputElement;
		const val = target.value.trim();
		if (/^#[0-9a-fA-F]{6}$/.test(val)) {
			update(step, val.toLowerCase());
		} else {
			// revert to current value
			target.value = scale[step];
		}
	}
</script>

<div class="space-y-3">
	<!-- swatch preview bar -->
	<div class="flex h-8 overflow-hidden rounded-lg border border-[var(--color-surface-border)]">
		{#each steps as step (step)}
			<div class="flex-1" style="background-color: {scale[step]}"></div>
		{/each}
	</div>

	<!-- individual step editors -->
	<div class="grid grid-cols-2 gap-2 sm:grid-cols-5">
		{#each steps as step (step)}
			<div
				class="flex items-center gap-2 rounded-lg border border-[var(--color-surface-border)] bg-[var(--color-surface-card)] px-2 py-1.5"
			>
				<label
					class="relative h-7 w-7 shrink-0 cursor-pointer overflow-hidden rounded-md border border-[var(--color-surface-border)]"
					style="background-color: {scale[step]}"
				>
					<input
						type="color"
						value={scale[step]}
						oninput={(e) => handleColorInput(e, step)}
						class="absolute inset-0 cursor-pointer opacity-0"
						aria-label={String(step)}
					/>
				</label>
				<div class="min-w-0 flex-1">
					<div class="text-[10px] font-medium text-[var(--color-nav-inactive)]">{step}</div>
					<input
						type="text"
						value={scale[step]}
						onblur={(e) => handleTextBlur(e, step)}
						class="w-full bg-transparent text-xs text-[var(--color-nav-active)] focus:outline-none"
						maxlength="7"
					/>
				</div>
			</div>
		{/each}
	</div>
</div>
