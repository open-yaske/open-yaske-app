<!--
  Modal.svelte
  中央寄せのフルスクリーンモーダル。
  Escape / backdrop click / × ボタン で閉じる。

  Apple Design 準拠:
  - §12 Materials: 正確なすりガラス実装 (backdrop-blur-3xl + 明るい上辺ボーダー)
  - §7  Spatial consistency: scale + fade アニメーション
  - §14 Reduced motion: `prefers-reduced-motion` 時は opacity fade のみ
  - §16 Accessibility: aria-labelledby, フォーカストラップ
-->
<script lang="ts">
	import type { Snippet } from 'svelte';
	import { fade, scale } from 'svelte/transition';
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

	// フォーカストラップ: モーダルが開いたら最初の focusable 要素にフォーカス
	let dialogEl = $state<HTMLDivElement | null>(null);
	let previousFocus: HTMLElement | null = null;

	const titleId = `modal-title-${Math.random().toString(36).slice(2, 9)}`;

	$effect(() => {
		if (open) {
			previousFocus = document.activeElement as HTMLElement;
			// 次のフレームでフォーカスを移動（transition 完了後）
			requestAnimationFrame(() => {
				if (!dialogEl) return;
				const focusable = dialogEl.querySelectorAll<HTMLElement>(
					'button, [href], input, select, textarea, [tabindex]:not([tabindex="-1"])'
				);
				const first = focusable[0];
				if (first) first.focus();
			});
		} else {
			// モーダルが閉じたら元の要素に戻す
			previousFocus?.focus();
			previousFocus = null;
		}
	});

	function handleKeydown(e: KeyboardEvent) {
		if (!open) return;

		if (e.key === 'Escape') {
			e.preventDefault();
			onclose();
			return;
		}

		// フォーカストラップ: Tab / Shift+Tab でダイアログ内を循環
		if (e.key === 'Tab' && dialogEl) {
			const focusable = dialogEl.querySelectorAll<HTMLElement>(
				'button:not([disabled]), [href], input:not([disabled]), select:not([disabled]), textarea:not([disabled]), [tabindex]:not([tabindex="-1"])'
			);
			const focusArray = Array.from(focusable);
			const first = focusArray[0];
			const last = focusArray[focusArray.length - 1];

			if (e.shiftKey) {
				if (document.activeElement === first) {
					e.preventDefault();
					last?.focus();
				}
			} else {
				if (document.activeElement === last) {
					e.preventDefault();
					first?.focus();
				}
			}
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
		class="fixed inset-0 z-50 flex items-center justify-center p-4 sm:p-6"
		role="dialog"
		aria-modal="true"
		aria-labelledby={title ? titleId : undefined}
		aria-label={title ? undefined : m.modal_close()}
		transition:fade={{ duration: 150 }}
	>
		<!-- backdrop -->
		<button
			type="button"
			class="absolute inset-0 bg-black/40 backdrop-blur-md transition-opacity"
			aria-label={m.modal_close()}
			onclick={handleBackdrop}
			tabindex="-1"
		></button>

		<!-- dialog panel -->
		<GlassContainer
			variant="modal"
			class="modal-panel relative z-10 flex max-h-[85vh] w-full flex-col overflow-hidden rounded-card
			       border border-[var(--color-surface-border)]/80
			       shadow-apple-xl transition-all"
			style="max-width: {maxWidth}"
		>
			<div
				bind:this={dialogEl}
				class="flex h-full w-full flex-col overflow-hidden"
				transition:scale={{ duration: 250, start: 0.94, easing: cubicOut }}
			>
				<!-- 明るい上辺ボーダー: Apple §12 Materials — bright top edge = light catching the material -->
				<div
					class="absolute inset-x-0 top-0 h-px bg-white/20 rounded-t-card pointer-events-none z-20"
					aria-hidden="true"
				></div>

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
							<IconButton icon={X} ariaLabel={m.modal_close()} onclick={onclose} size="sm" />
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
