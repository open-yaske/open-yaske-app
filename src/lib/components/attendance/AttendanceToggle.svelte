<!--
  AttendanceToggle.svelte
  5-state horizontal segmented control for attendance marking.
  States: present (green), absent (red), late (orange), excused (blue), cancelled (grey).
  Tap to select; calls onselect with the chosen AttendanceStatus.
-->
<script lang="ts">
	import { m } from '$lib/paraglide/messages';
	import { ATTENDANCE_STATUS_LIST } from '$lib/types';
	import type { AttendanceStatus } from '$lib/types';
	import { Check, X, Clock, ShieldCheck, Ban } from '@lucide/svelte';
	import type { Component } from 'svelte';

	interface Props {
		/** Currently selected status, or null if none */
		status: AttendanceStatus | null;
		/** Called when user taps a segment */
		onselect: (status: AttendanceStatus) => void;
		/** Disable all segments */
		disabled?: boolean;
	}

	let { status, onselect, disabled = false }: Props = $props();

	const STATUS_META: Record<
		AttendanceStatus,
		{ label: () => string; icon: Component<{ size?: number }>; cssVar: string }
	> = {
		present: { label: m.attendance_present, icon: Check, cssVar: 'present' },
		absent: { label: m.attendance_absent, icon: X, cssVar: 'absent' },
		late: { label: m.attendance_late, icon: Clock, cssVar: 'late' },
		excused: { label: m.attendance_excused, icon: ShieldCheck, cssVar: 'excused' },
		cancelled: { label: m.attendance_cancelled, icon: Ban, cssVar: 'cancelled' }
	};
</script>

<div
	class="flex gap-1.5 rounded-[var(--radius-chip)] border border-[var(--color-surface-border)] bg-[var(--color-surface-muted)] p-1.5"
	role="group"
	aria-label={m.attendance_status()}
>
	{#each ATTENDANCE_STATUS_LIST as s (s)}
		{@const meta = STATUS_META[s]}
		{@const Icon = meta.icon}
		{@const isActive = status === s}
		<button
			type="button"
			{disabled}
			class="flex flex-1 flex-col items-center justify-center gap-1 rounded-lg py-2.5 transition-all duration-200 focus:outline-none focus-visible:ring-2 focus-visible:ring-[var(--color-primary-500)]"
			class:bg-transparent={!isActive}
			class:text-[var(--color-nav-inactive)]={!isActive}
			class:opacity-50={disabled}
			style:background-color={isActive ? `var(--color-attendance-${meta.cssVar}-bg)` : undefined}
			style:color={isActive ? `var(--color-attendance-${meta.cssVar}-fg)` : undefined}
			aria-pressed={isActive}
			aria-label={meta.label()}
			onclick={() => onselect(s)}
		>
			<Icon size={18} />
			<span class="text-xs font-medium leading-none">{meta.label()}</span>
		</button>
	{/each}
</div>
