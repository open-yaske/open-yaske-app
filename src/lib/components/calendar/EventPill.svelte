<!--
  EventPill.svelte
  カレンダーイベントの小さなピル表示。
  月表示ではドット/ピル、週・日表示では時間付きピルとして使う。
  キャンセル（休講）イベントはグレーで表示。
-->
<script lang="ts">
	import type { CalendarEvent } from '$lib/types';
	import type { ThemePack } from '$lib/theme';
	import { resolveColorHex } from '$lib/theme';

	interface Props {
		event: CalendarEvent;
		pack: ThemePack;
		/** ピルの表示形式 */
		variant?: 'dot' | 'compact' | 'full';
		/** キャンセル扱いでグレー表示にする */
		cancelled?: boolean;
		/** クリックハンドラ */
		onclick?: (event: CalendarEvent) => void;
		class?: string;
	}

	let {
		event,
		pack,
		variant = 'full',
		cancelled = false,
		onclick,
		class: klass = ''
	}: Props = $props();

	const colorHex = $derived(resolveColorHex(pack, event.color));
	const isTransparent = $derived(colorHex === 'transparent');

	function formatTime(ts: number): string {
		const d = new Date(ts);
		const h = d.getHours().toString().padStart(2, '0');
		const m = d.getMinutes().toString().padStart(2, '0');
		return `${h}:${m}`;
	}

	function handleClick(e: MouseEvent) {
		e.stopPropagation();
		onclick?.(event);
	}
</script>

{#if variant === 'dot'}
	<button
		type="button"
		class="h-2 w-2 shrink-0 rounded-full {klass}"
		style="background-color: {cancelled
			? 'var(--color-nav-inactive)'
			: isTransparent
				? 'var(--color-primary-500)'
				: colorHex}"
		aria-label={event.title}
		onclick={handleClick}
	></button>
{:else if variant === 'compact'}
	<button
		type="button"
		class="flex w-full items-center gap-1 overflow-hidden rounded px-1 py-0.5 text-left text-[10px] leading-tight {cancelled
			? 'opacity-50 line-through'
			: ''} {klass}"
		style="background-color: {cancelled
			? 'var(--color-surface-muted)'
			: isTransparent
				? 'var(--color-primary-50)'
				: colorHex + '22'}"
		onclick={handleClick}
	>
		<span
			class="h-1.5 w-1.5 shrink-0 rounded-full"
			style="background-color: {cancelled
				? 'var(--color-nav-inactive)'
				: isTransparent
					? 'var(--color-primary-500)'
					: colorHex}"
		></span>
		<span class="truncate text-[var(--color-nav-active)]">{event.title}</span>
	</button>
{:else}
	<button
		type="button"
		class="flex w-full items-center gap-1.5 overflow-hidden rounded-md px-2 py-1 text-left text-xs leading-tight {cancelled
			? 'opacity-60'
			: ''} {klass}"
		style="background-color: {cancelled
			? 'var(--color-surface-muted)'
			: isTransparent
				? 'var(--color-primary-50)'
				: colorHex + '1a'}; border-left: 3px solid {cancelled
			? 'var(--color-nav-inactive)'
			: isTransparent
				? 'var(--color-primary-500)'
				: colorHex}"
		onclick={handleClick}
	>
		<div class="min-w-0 flex-1">
			<div
				class="truncate font-medium text-[var(--color-nav-active)] {cancelled
					? 'line-through'
					: ''}"
			>
				{event.title}
			</div>
			{#if !event.allDay}
				<div class="truncate text-[10px] text-[var(--color-nav-inactive)]">
					{formatTime(event.startAt)} - {formatTime(event.endAt)}
				</div>
			{:else}
				<div class="truncate text-[10px] text-[var(--color-nav-inactive)]">終日</div>
			{/if}
		</div>
	</button>
{/if}
