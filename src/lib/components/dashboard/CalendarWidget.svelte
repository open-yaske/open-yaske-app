<script lang="ts">
	import { userDataStore } from '$lib/stores';
	import { m } from '$lib/paraglide/messages';
	import { Calendar, MapPin, Clock, ChevronRight } from '@lucide/svelte';
	import BentoCard from '../BentoCard.svelte';

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

		if (dStart.toDateString() === dEnd.toDateString()) {
			return `${month}/${date} ${formatTime(dStart)} - ${formatTime(dEnd)}`;
		}

		return `${month}/${date} - ${dEnd.getMonth() + 1}/${dEnd.getDate()}`;
	}
</script>

<BentoCard padding="p-4 md:p-5" class="w-full">
	{#snippet header()}
		<div
			class="flex w-full items-center justify-between border-b border-[var(--color-surface-border)]/60 pb-3"
		>
			<div class="flex items-center gap-2.5 font-bold text-[var(--color-nav-active)]">
				<div
					class="flex h-8 w-8 items-center justify-center rounded-xl bg-[var(--color-primary-500)]/15 text-[var(--color-primary-600)]"
				>
					<Calendar size={18} />
				</div>
				<span class="text-sm font-semibold">{m.nav_calendar()}</span>
			</div>
			<a
				href="/calendar"
				class="flex items-center gap-0.5 text-xs font-semibold text-[var(--color-primary-600)] hover:underline"
			>
				<span>{m.action_more()}</span>
				<ChevronRight size={14} />
			</a>
		</div>
	{/snippet}

	{#if upcomingEvents.length === 0}
		<div class="py-8 text-center text-xs text-[var(--color-nav-inactive)]">
			{m.calendar_empty()}
		</div>
	{:else}
		<div class="space-y-3 pt-2">
			{#each upcomingEvents as event (event.id)}
				<div class="flex flex-col gap-1 py-1.5 first:pt-0 last:pb-0">
					<h4 class="font-bold text-xs md:text-sm text-[var(--color-nav-active)] truncate">
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
</BentoCard>
