<!--
  AttendanceColorEditor.svelte
  5つの出席状態 (present/absent/late/excused/cancelled) の fg/bg カラーエディター。
  各行にラベル + bg color input + fg color input + プレビューバッジ。
-->
<script lang="ts">
	import type { AttendanceColors, AttendanceColor } from '$lib/theme';
	import { m } from '$lib/paraglide/messages';

	type StateKey = keyof AttendanceColors;

	interface Props {
		attendance: AttendanceColors;
		onchange?: (attendance: AttendanceColors) => void;
	}

	let { attendance = $bindable(), onchange }: Props = $props();

	const states: { key: StateKey; label: string }[] = [
		{ key: 'present', label: m.attendance_present() },
		{ key: 'absent', label: m.attendance_absent() },
		{ key: 'late', label: m.attendance_late() },
		{ key: 'excused', label: m.attendance_excused() },
		{ key: 'cancelled', label: m.attendance_cancelled() }
	];

	function updateField(state: StateKey, field: keyof AttendanceColor, value: string) {
		attendance = {
			...attendance,
			[state]: { ...attendance[state], [field]: value }
		};
		onchange?.(attendance);
	}

	function handleColorInput(e: Event, state: StateKey, field: keyof AttendanceColor) {
		const target = e.currentTarget as HTMLInputElement;
		updateField(state, field, target.value);
	}

	function handleTextBlur(e: FocusEvent, state: StateKey, field: keyof AttendanceColor) {
		const target = e.currentTarget as HTMLInputElement;
		const val = target.value.trim();
		if (/^#[0-9a-fA-F]{6}$/.test(val)) {
			updateField(state, field, val.toLowerCase());
		} else {
			target.value = attendance[state][field];
		}
	}
</script>

<div class="space-y-2">
	{#each states as { key, label } (key)}
		{@const a = attendance[key]}
		<div
			class="flex items-center gap-3 rounded-lg border border-[var(--color-surface-border)] bg-[var(--color-surface-card)] px-3 py-2"
		>
			<!-- preview badge -->
			<span
				class="inline-flex h-7 min-w-[3rem] items-center justify-center rounded-md px-2 text-xs font-semibold"
				style="background-color: {a.bg}; color: {a.fg}"
			>
				{label}
			</span>

			<div class="flex flex-1 items-center gap-3">
				<!-- bg editor -->
				<div class="flex items-center gap-1.5">
					<label
						class="relative h-7 w-7 shrink-0 cursor-pointer overflow-hidden rounded-md border border-[var(--color-surface-border)]"
						style="background-color: {a.bg}"
					>
						<input
							type="color"
							value={a.bg}
							oninput={(e) => handleColorInput(e, key, 'bg')}
							class="absolute inset-0 cursor-pointer opacity-0"
							aria-label={`${label} ${m.theme_editor_bg()}`}
						/>
					</label>
					<input
						type="text"
						value={a.bg}
						onblur={(e) => handleTextBlur(e, key, 'bg')}
						class="w-16 bg-transparent text-xs text-[var(--color-nav-active)] focus:outline-none"
						maxlength="7"
					/>
				</div>

				<!-- fg editor -->
				<div class="flex items-center gap-1.5">
					<label
						class="relative h-7 w-7 shrink-0 cursor-pointer overflow-hidden rounded-md border border-[var(--color-surface-border)]"
						style="background-color: {a.fg}"
					>
						<input
							type="color"
							value={a.fg}
							oninput={(e) => handleColorInput(e, key, 'fg')}
							class="absolute inset-0 cursor-pointer opacity-0"
							aria-label={`${label} ${m.theme_editor_fg()}`}
						/>
					</label>
					<input
						type="text"
						value={a.fg}
						onblur={(e) => handleTextBlur(e, key, 'fg')}
						class="w-16 bg-transparent text-xs text-[var(--color-nav-active)] focus:outline-none"
						maxlength="7"
					/>
				</div>
			</div>
		</div>
	{/each}
</div>
