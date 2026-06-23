<!--
  AttendanceLog.svelte
  List of recent attendance records for a course, grouped by date (descending).
  Shows a colored status badge for each record.
-->
<script lang="ts">
	import { m } from '$lib/paraglide/messages';
	import type { AttendanceRecord, AttendanceStatus } from '$lib/types';
	import { SvelteMap } from 'svelte/reactivity';
	import { Trash2 } from '@lucide/svelte';

	interface Props {
		records: AttendanceRecord[];
		/** Max number of records to show */
		limit?: number;
		/** Called when a record is deleted */
		ondelete?: (record: AttendanceRecord) => void;
	}

	let { records, limit = 20, ondelete }: Props = $props();

	const STATUS_LABEL: Record<AttendanceStatus, () => string> = {
		present: m.attendance_present,
		absent: m.attendance_absent,
		late: m.attendance_late,
		excused: m.attendance_excused,
		cancelled: m.attendance_cancelled
	};

	/** Group records by date, sorted descending */
	const grouped = $derived.by(() => {
		const sorted = [...records].sort((a, b) => b.date.localeCompare(a.date));
		const limited = sorted.slice(0, limit);
		const map = new SvelteMap<string, AttendanceRecord[]>();
		for (const r of limited) {
			const list = map.get(r.date) ?? [];
			list.push(r);
			map.set(r.date, list);
		}
		return Array.from(map.entries());
	});

	function formatDate(dateStr: string): string {
		const d = new Date(dateStr + 'T00:00:00');
		const month = d.getMonth() + 1;
		const day = d.getDate();
		return `${month}/${day}`;
	}
</script>

{#if grouped.length === 0}
	<p class="py-4 text-center text-xs text-[var(--color-nav-inactive)]">
		{m.empty_no_attendance()}
	</p>
{:else}
	<div class="space-y-3">
		{#each grouped as [date, items] (date)}
			<div class="space-y-1.5">
				<p class="text-xs font-medium text-[var(--color-nav-inactive)]">
					{formatDate(date)}
				</p>
				{#each items as record (record.id)}
					{@const status = record.status}
					<div class="flex items-center justify-between gap-2 py-0.5">
						<span
							class="inline-flex items-center rounded-full px-2.5 py-0.5 text-xs font-medium"
							style="background-color: var(--color-attendance-{status}-bg); color: var(--color-attendance-{status}-fg);"
						>
							{STATUS_LABEL[status]()}
						</span>
						{#if ondelete}
							<button
								type="button"
								class="rounded p-1 text-[var(--color-nav-inactive)] hover:bg-[var(--color-surface-muted)] hover:text-[var(--color-attendance-absent-fg)] transition-colors"
								aria-label={m.action_delete()}
								onclick={() => {
									if (confirm(m.attendance_delete_confirm())) {
										ondelete(record);
									}
								}}
							>
								<Trash2 size={14} />
							</button>
						{/if}
					</div>
				{/each}
			</div>
		{/each}
	</div>
{/if}
