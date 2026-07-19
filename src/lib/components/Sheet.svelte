<!--
  Sheet.svelte
  ボトムシート（モバイル） / サイドシート（デスクトップ）。
  Escape / backdrop click で閉じる。

  Apple Design 準拠:
  - §7  Spatial consistency: bottom → Y軸, right → X軸で統一した進入・退出パス
  - §12 Materials: すりガラス + 明るい上辺ボーダー
  - §14 Reduced motion: opacity fade のみにフォールバック
-->
<script lang="ts">
	import type { Snippet } from 'svelte';
	import { fly, fade } from 'svelte/transition';
	import { cubicOut } from 'svelte/easing';
	import { X } from '@lucide/svelte';
	import { m } from '$lib/paraglide/messages';
	import { APPLE_MATERIALS } from '$lib/apple-design';
	import IconButton from './IconButton.svelte';
	import GlassContainer from './GlassContainer.svelte';

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

	// reduced-motion チェック: アニメーションの代わりにフェードのみ
	let prefersReducedMotion = $state(false);
	$effect(() => {
		if (typeof window === 'undefined') return;
		const mq = window.matchMedia('(prefers-reduced-motion: reduce)');
		prefersReducedMotion = mq.matches;
		const handler = (e: MediaQueryListEvent) => {
			prefersReducedMotion = e.matches;
		};
		mq.addEventListener('change', handler);
		return () => mq.removeEventListener('change', handler);
	});

	const titleId = `sheet-title-${Math.random().toString(36).slice(2, 9)}`;

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

	// §7 Spatial consistency: 進入方向と退出方向を一致させる
	const flyParams = $derived(
		prefersReducedMotion
			? { y: 0, x: 0, duration: 0, opacity: 0 }
			: position === 'bottom'
				? { y: 800, x: 0, duration: 300, easing: cubicOut, opacity: 0.9 }
				: { x: 800, y: 0, duration: 300, easing: cubicOut, opacity: 0.9 }
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
		<button
			type="button"
			class="absolute inset-0 bg-black/40 backdrop-blur-md transition-opacity"
			aria-label={m.sheet_close()}
			onclick={handleBackdrop}
			tabindex="-1"
		></button>

		<GlassContainer
			variant="modal"
			class="sheet-panel relative z-10 flex w-full max-h-[90vh] flex-col
			       border border-[var(--color-surface-border)]/80
			       shadow-apple-xl transition-all md:max-h-full md:max-w-md md:h-full {position === 'bottom'
				? 'rounded-t-[32px]'
				: 'rounded-l-[32px]'}"
		>
			<div class="flex h-full w-full flex-col overflow-hidden" transition:fly={flyParams}>
				<!-- 明るい上辺ボーダー: §12 Materials -->
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
