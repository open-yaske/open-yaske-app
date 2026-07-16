<script lang="ts">
	import { userDataStore } from '$lib/stores';
	import { m } from '$lib/paraglide/messages';
	import { Calendar, MapPin, Clock } from '@lucide/svelte';

	// 今日以降のイベントを取得し、日付が早い順にソートして最大3件表示
	const upcomingEvents = $derived.by(() => {
		const today = new Date();
		today.setHours(0, 0, 0, 0);
		const startMs = today.getTime();

		return userDataStore.events
			.filter((e) => e.startAt >= startMs)
			.sort((a, b) => a.startAt - b.startAt)
			.slice(0, 3);
	});

	function formatEventDate(start: number, end: number, allDay: boolean): string {
		const dStart = new Date(start);
		const dEnd = new Date(end);

		const month = dStart.getMonth() + 1;
		const date = dStart.getDate();

		if (allDay) {
			return `${month}/${date} (${m.calendar_all_day()})`;
		}

		const formatTime = (d: Date) => {
			return `${String(d.getHours()).padStart(2, '0')}:${String(d.getMinutes()).padStart(2, '0')}`;
		};

		// 同一日の場合
		if (dStart.toDateString() === dEnd.toDateString()) {
			return `${month}/${date} ${formatTime(dStart)} - ${formatTime(dEnd)}`;
		}

		return `${month}/${date} - ${dEnd.getMonth() + 1}/${dEnd.getDate()}`;
	}
</script>

<div
	class="rounded-card border border-[var(--color-surface-border)] bg-[var(--color-surface-card)] p-4"
>
	<div
		class="mb-4 flex items-center justify-between border-b border-[var(--color-surface-border)] pb-2"
	>
		<div class="flex items-center gap-2 font-bold text-[var(--color-nav-active)]">
			<Calendar size={18} class="text-[var(--color-primary-500)]" />
			<span>{m.nav_calendar()}</span>
		</div>
		<a href="/calendar" class="text-xs text-[var(--color-primary-600)] hover:underline">
			{m.action_more()}
		</a>
	</div>

	{#if upcomingEvents.length === 0}
		<div class="py-6 text-center text-xs text-[var(--color-nav-inactive)]">
			{m.calendar_empty()}
		</div>
	{:else}
		<div class="space-y-3">
			{#each upcomingEvents as event (event.id)}
				<div class="flex flex-col gap-1.5 py-1.5 first:pt-0 last:pb-0">
					<h4 class="font-bold text-sm text-[var(--color-nav-active)] truncate">
						{event.title}
					</h4>

					<div
						class="flex flex-wrap gap-x-3 gap-y-1 text-xs text-[var(--color-nav-inactive)] font-medium"
					>
						<div class="flex items-center gap-1">
							<Clock size={12} />
							<span>{formatEventDate(event.startAt, event.endAt, event.allDay)}</span>
						</div>
						{#if event.location}
							<div class="flex items-center gap-1">
								<MapPin size={12} />
								<span class="truncate">{event.location}</span>
							</div>
						{/if}
					</div>
				</div>
			{/each}
		</div>
	{/if}
</div>
