<!--
  Modal.svelte
  中央寄せのフルスクリーンモーダル。
  Escape / backdrop click / × ボタン で閉じる。

  Apple Design 準拠:
  - §12 Materials: GlassContainer animateIn で Liquid Glass マテリアライズ
  - §7  Spatial consistency: 同一アニメーションで entrance/exit
  - §14 Reduced motion: GlassContainer が自動で css animation を無効化
  - §16 Accessibility: aria-labelledby, フォーカストラップ
-->
<script lang="ts">
	import type { Snippet } from 'svelte';
	import { X } from '@lucide/svelte';
	import { m } from '$lib/paraglide/messages';
	import IconButton from './IconButton.svelte';
	import GlassContainer from './GlassContainer.svelte';
	import { styleStore } from '$lib/stores';
	import { resolveComponents, resolveRadius } from '$lib/styles';

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

	// Style-aware corner radius + sheet レシピ
	const r = $derived(resolveRadius(styleStore.pack));
	const c = $derived(resolveComponents(styleStore.pack));

	// フォーカストラップ
	let dialogEl = $state<HTMLDivElement | null>(null);
	let previousFocus: HTMLElement | null = null;

	const titleId = `modal-title-${Math.random().toString(36).slice(2, 9)}`;

	$effect(() => {
		if (open) {
			previousFocus = document.activeElement as HTMLElement;
			// animateIn が 300ms → 完了後にフォーカス
			setTimeout(() => {
				if (!dialogEl) return;
				const focusable = dialogEl.querySelectorAll<HTMLElement>(
					'button, [href], input, select, textarea, [tabindex]:not([tabindex="-1"])'
				);
				focusable[0]?.focus();
			}, 320);
		} else {
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

		if (e.key === 'Tab' && dialogEl) {
			const focusable = dialogEl.querySelectorAll<HTMLElement>(
				'button:not([disabled]), [href], input:not([disabled]), select:not([disabled]), textarea:not([disabled]), [tabindex]:not([tabindex="-1"])'
			);
			const fa = Array.from(focusable);
			const first = fa[0];
			const last = fa[fa.length - 1];
			if (e.shiftKey && document.activeElement === first) {
				e.preventDefault();
				last?.focus();
			} else if (!e.shiftKey && document.activeElement === last) {
				e.preventDefault();
				first?.focus();
			}
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
</script>

<svelte:window onkeydown={handleKeydown} />

{#if open}
	<!--
		アニメーションは GlassContainer の animateIn だけに任せる。
		外側の div に transition:fade をかけると 150ms vs 300ms でタイミングがずれ、
		描画の途中ハーフ状態が視認されるバグの原因になっていた。
		代わりに opacity-0 → animation 完了で auto 表示。
	-->
	<div
		class="modal-backdrop fixed inset-0 z-50 flex items-center justify-center p-4 sm:p-6"
		role="dialog"
		aria-modal="true"
		aria-labelledby={title ? titleId : undefined}
		aria-label={title ? undefined : m.modal_close()}
	>
		<!-- backdrop — シンプルな semi-transparent overlay -->
		<div
			class={c.sheet.backdrop}
			onclick={handleBackdrop}
			role="button"
			tabindex="-1"
			aria-label={m.modal_close()}
		></div>

		<!-- §12 Materials: GlassContainer static render — backdrop-filter animation は compositing artifact を起こす -->
		<GlassContainer
			variant="modal"
			elevation="xl"
			class="modal-panel {c.sheet.panel} {r.card}"
			style="max-width: {maxWidth}"
		>
			<div bind:this={dialogEl} class="flex h-full w-full flex-col overflow-hidden">
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
					<footer class="shrink-0 border-t border-[var(--color-surface-border)]/60 px-5 py-3.5">
						{@render footer()}
					</footer>
				{/if}
			</div>
		</GlassContainer>
	</div>
{/if}
