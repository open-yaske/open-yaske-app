<!--
  PaletteEditor.svelte
  ColorEntry のグリッドエディター。各エントリーに名前 + color input + HEX テキスト。
  追加 / 削除ボタン付き。
-->
<script lang="ts">
	import type { ColorEntry } from '$lib/theme';
	import { m } from '$lib/paraglide/messages';
	import { Plus, Trash2 } from '@lucide/svelte';

	interface Props {
		colors: ColorEntry[];
		onchange?: (colors: ColorEntry[]) => void;
	}

	let { colors = $bindable(), onchange }: Props = $props();

	function updateHex(index: number, hex: string) {
		colors = colors.map((c, i) => (i === index ? { ...c, hex } : c));
		onchange?.(colors);
	}

	function updateName(index: number, name: string) {
		colors = colors.map((c, i) => (i === index ? { ...c, name } : c));
		onchange?.(colors);
	}

	function handleColorInput(e: Event, index: number) {
		const target = e.currentTarget as HTMLInputElement;
		updateHex(index, target.value);
	}

	function handleTextBlur(e: FocusEvent, index: number) {
		const target = e.currentTarget as HTMLInputElement;
		const val = target.value.trim();
		if (/^#[0-9a-fA-F]{6}$/.test(val)) {
			updateHex(index, val.toLowerCase());
		} else {
			target.value = colors[index].hex;
		}
	}

	function addColor() {
		const newId = `custom-${Date.now().toString(36)}`;
		// Neutral default grey for new custom colors. Validated/edited by the user afterwards.
		// eslint-disable-next-line no-raw-hex/no-raw-hex
		const defaultHex = '#6b7280';
		colors = [...colors, { id: newId, name: 'color_custom', hex: defaultHex, isCustom: true }];
		onchange?.(colors);
	}

	function removeColor(index: number) {
		colors = colors.filter((_, i) => i !== index);
		onchange?.(colors);
	}
</script>

<div class="space-y-2">
	<div class="grid grid-cols-1 gap-2 sm:grid-cols-2">
		{#each colors as color, i (color.id)}
			<div
				class="flex items-center gap-2 rounded-lg border border-[var(--color-surface-border)] bg-[var(--color-surface-card)] px-2 py-1.5"
			>
				<label
					class="relative h-8 w-8 shrink-0 cursor-pointer overflow-hidden rounded-md border border-[var(--color-surface-border)]"
					style="background-color: {color.hex}"
				>
					<input
						type="color"
						value={color.hex}
						oninput={(e) => handleColorInput(e, i)}
						class="absolute inset-0 cursor-pointer opacity-0"
						aria-label={color.name}
					/>
				</label>
				<div class="min-w-0 flex-1">
					<input
						type="text"
						value={color.hex}
						onblur={(e) => handleTextBlur(e, i)}
						class="w-full bg-transparent text-xs font-medium text-[var(--color-nav-active)] focus:outline-none"
						maxlength="7"
					/>
					<input
						type="text"
						value={color.name}
						oninput={(e) => updateName(i, (e.currentTarget as HTMLInputElement).value)}
						class="w-full bg-transparent text-[10px] text-[var(--color-nav-inactive)] focus:outline-none"
						placeholder="color_name"
					/>
				</div>
				<button
					type="button"
					class="flex h-7 w-7 shrink-0 items-center justify-center rounded-md text-[var(--color-nav-inactive)] transition-colors hover:bg-[var(--color-surface-muted)] hover:text-[var(--color-attendance-absent-bg)]"
					onclick={() => removeColor(i)}
					aria-label={m.action_delete()}
				>
					<Trash2 size={14} />
				</button>
			</div>
		{/each}
	</div>

	<button
		type="button"
		class="flex w-full items-center justify-center gap-1.5 rounded-lg border border-dashed border-[var(--color-surface-border)] py-2 text-sm text-[var(--color-nav-inactive)] transition-colors hover:bg-[var(--color-surface-muted)] hover:text-[var(--color-nav-active)]"
		onclick={addColor}
	>
		<Plus size={16} />
		{m.theme_editor_add_color()}
	</button>
</div>
