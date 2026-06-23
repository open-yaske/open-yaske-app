<!--
  Modal.svelte
  中央寄せのフルスクリーンモーダル。
  Escape / backdrop click / × ボタン で閉じる。
-->
<script lang="ts">
	import type { Snippet } from 'svelte';
	import { fade, scale } from 'svelte/transition';
	import { X } from '@lucide/svelte';
	import IconButton from './IconButton.svelte';

	interface Props {
		open: boolean;
		onclose: () => void;
		title?: string;
		showCloseButton?: boolean;
		closeOnBackdrop?: boolean;
		maxWidth?: string;
		children: Snippet;
		footer?: Snippet;
	}

	let {
		open = $bindable(false),
		onclose,
		title,
		showCloseButton = true,
		closeOnBackdrop = true,
		maxWidth = '32rem',
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
		class="fixed inset-0 z-50 flex items-center justify-center p-4"
		role="dialog"
		aria-modal="true"
		aria-label={title}
		transition:fade={{ duration: 150 }}
	>
		<!-- backdrop -->
		<button
			type="button"
			class="absolute inset-0 bg-black/50"
			aria-label="Close"
			onclick={handleBackdrop}
			tabindex="-1"
		></button>

		<!-- dialog -->
		<div
			class="relative z-10 flex max-h-[90vh] w-full flex-col overflow-hidden rounded-2xl bg-[var(--color-surface-card)] shadow-[var(--shadow-modal)]"
			style="max-width: {maxWidth}"
			transition:scale={{ duration: 200, start: 0.95 }}
		>
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
