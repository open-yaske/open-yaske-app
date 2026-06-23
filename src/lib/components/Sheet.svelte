<!--
  Sheet.svelte
  ボトムシート（モバイル） / サイドシート（デスクトップ）。
  Escape / backdrop click で閉じる。
-->
<script lang="ts">
	import type { Snippet } from 'svelte';
	import { fly } from 'svelte/transition';
	import { X } from '@lucide/svelte';
	import IconButton from './IconButton.svelte';

	interface Props {
		open: boolean;
		onclose: () => void;
		title?: string;
		showCloseButton?: boolean;
		closeOnBackdrop?: boolean;
		/** モバイルは下から、デスクトップは右から */
		position?: 'bottom' | 'right';
		children: Snippet;
		footer?: Snippet;
	}

	let {
		open = $bindable(false),
		onclose,
		title,
		showCloseButton = true,
		closeOnBackdrop = true,
		position = 'bottom',
		children,
		footer
	}: Props = $props();

	function handleKeydown(e: KeyboardEvent) {
		if (e.key === 'Escape' && open) {
			e.preventDefault();
			onclose();
		}
	}

	function handleBackdrop(e: MouseEvent) {
		if (closeOnBackdrop && e.target === e.currentTarget) {
			onclose();
		}
	}

	$effect(() => {
		if (open) {
			document.body.style.overflow = 'hidden';
			return () => {
				document.body.style.overflow = '';
			};
		}
	});
</script>

<svelte:window onkeydown={handleKeydown} />

{#if open}
	<div
		class="fixed inset-0 z-50 flex md:items-stretch"
		class:items-end={position === 'bottom'}
		class:justify-end={position === 'right'}
		role="dialog"
		aria-modal="true"
		aria-label={title}
	>
		<button
			type="button"
			class="absolute inset-0 bg-black/50"
			aria-label="Close"
			onclick={handleBackdrop}
			tabindex="-1"
		></button>

		<div
			class="relative z-10 flex w-full max-h-[90vh] flex-col bg-[var(--color-surface-card)] shadow-[var(--shadow-sheet)] md:max-h-full md:max-w-md md:h-full"
			class:rounded-t-2xl={position === 'bottom'}
			transition:fly={{
				y: position === 'bottom' ? 1000 : 0,
				x: position === 'right' ? 1000 : 0,
				duration: 200
			}}
		>
			{#if position === 'bottom'}
				<div class="flex shrink-0 justify-center pt-2 pb-1">
					<span class="block h-1 w-10 rounded-full bg-[var(--color-surface-border)]"></span>
				</div>
			{/if}
			{#if title || showCloseButton}
				<header
					class="flex shrink-0 items-center justify-between border-b border-[var(--color-surface-border)] px-5 py-3"
				>
					<h2 class="text-base font-semibold text-[var(--color-nav-active)]">{title}</h2>
					{#if showCloseButton}
						<IconButton icon={X} ariaLabel="Close" onclick={onclose} size="sm" />
					{/if}
				</header>
			{/if}
			<div class="flex-1 overflow-y-auto px-5 py-4">
				{@render children()}
			</div>
			{#if footer}
				<footer
					class="shrink-0 border-t border-[var(--color-surface-border)] bg-[var(--color-surface-page)] px-5 py-3"
				>
					{@render footer()}
				</footer>
			{/if}
		</div>
	</div>
{/if}
