<!--
  Sheet.svelte
  ボトムシート（モバイル） / サイドシート（デスクトップ）。
  Escape / backdrop click で閉じる。

  Apple Design 準拠:
  - §7  Spatial consistency: bottom → Y軸, right → X軸で統一した進入・退出パス
  - §12 Materials: GlassContainer Liquid Glass すりガラス
  - §14 Reduced motion: animation skip は GlassContainer に任せる
-->
<script lang="ts">
	import type { Snippet } from 'svelte';
	import { fly, fade } from 'svelte/transition';
	import { cubicOut } from 'svelte/easing';
	import { X } from '@lucide/svelte';
	import { m } from '$lib/paraglide/messages';
	import IconButton from './IconButton.svelte';
	import GlassContainer from './GlassContainer.svelte';
	import { styleStore } from '$lib/stores';
	import { resolveComponents } from '$lib/styles';

	interface Props {
		open: boolean;
		onclose: () => void;
		title?: string;
		showCloseButton?: boolean;
		closeOnBackdrop?: boolean;
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

	let prefersReducedMotion = $state(false);
	$effect(() => {
		if (typeof window === 'undefined') return;
		const mq = window.matchMedia('(prefers-reduced-motion: reduce)');
		prefersReducedMotion = mq.matches;
		const h = (e: MediaQueryListEvent) => {
			prefersReducedMotion = e.matches;
		};
		mq.addEventListener('change', h);
		return () => mq.removeEventListener('change', h);
	});

	const titleId = `sheet-title-${Math.random().toString(36).slice(2, 9)}`;

	const c = $derived(resolveComponents(styleStore.pack));

	function handleKeydown(e: KeyboardEvent) {
		if (e.key === 'Escape' && open) {
			e.preventDefault();
			onclose();
		}
	}

	function handleBackdrop(e: MouseEvent) {
		if (closeOnBackdrop && e.target === e.currentTarget) onclose();
	}

	$effect(() => {
		if (open) {
			document.body.style.overflow = 'hidden';
			return () => {
				document.body.style.overflow = '';
			};
		}
	});

	// §7 Spatial consistency: outbound = inbound の逆
	const flyParams = $derived(
		prefersReducedMotion
			? { y: 0, x: 0, duration: 0, opacity: 0 }
			: position === 'bottom'
				? { y: 600, x: 0, duration: 320, easing: cubicOut, opacity: 0.95 }
				: { x: 600, y: 0, duration: 320, easing: cubicOut, opacity: 0.95 }
	);
</script>

<svelte:window onkeydown={handleKeydown} />

{#if open}
	<div
		class="fixed inset-0 z-50 flex md:items-stretch"
		class:items-end={position === 'bottom'}
		class:justify-end={position === 'right'}
		role="dialog"
		aria-modal="true"
		aria-labelledby={title ? titleId : undefined}
		aria-label={title ? undefined : m.sheet_close()}
	>
		<!-- backdrop — fly と同期: fade 150ms delay 0 -->
		<button
			type="button"
			class={c.sheet.backdrop}
			aria-label={m.sheet_close()}
			onclick={handleBackdrop}
			tabindex="-1"
			transition:fade={{ duration: 280 }}
		></button>

		<!-- §7 Spatial consistency: position に応じた fly 方向 -->
		<GlassContainer
			variant="modal"
			elevation="xl"
			class="sheet-panel {c.sheet.panel}
			       md:max-h-full md:max-w-md md:h-full {position === 'bottom'
				? 'rounded-t-[32px]'
				: 'rounded-l-[32px]'}"
		>
			<div class="flex h-full w-full flex-col overflow-hidden" transition:fly={flyParams}>
				<div
					class="absolute inset-x-0 top-0 h-px bg-white/20 rounded-t-[32px] pointer-events-none z-20"
					aria-hidden="true"
				></div>

				{#if position === 'bottom'}
					<div
						class="flex shrink-0 justify-center pt-2.5 pb-1 cursor-grab active:cursor-grabbing"
						aria-hidden="true"
					>
						<span
							class="block h-1.5 w-12 rounded-full bg-[var(--color-surface-border)] hover:bg-[var(--color-nav-inactive)] transition-colors"
						></span>
					</div>
				{/if}
				{#if title || showCloseButton}
					<header
						class="flex shrink-0 items-center justify-between border-b border-[var(--color-surface-border)]/60 px-5 py-3.5"
					>
						<h2
							id={titleId}
							class="text-base font-semibold tracking-tight text-[var(--color-nav-active)]"
						>
							{title || ''}
						</h2>
						{#if showCloseButton}
							<IconButton icon={X} ariaLabel={m.sheet_close()} onclick={onclose} size="sm" />
						{/if}
					</header>
				{/if}
				<div class="flex-1 overflow-y-auto px-5 py-4">
					{@render children()}
				</div>
				{#if footer}
					<footer
						class="shrink-0 border-t border-[var(--color-surface-border)]/60 bg-[var(--color-surface-page)]/80 px-5 py-3.5"
					>
						{@render footer()}
					</footer>
				{/if}
			</div>
		</GlassContainer>
	</div>
{/if}
