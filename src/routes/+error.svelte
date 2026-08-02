<script lang="ts">
	import { page } from '$app/state';
	import { m } from '$lib/paraglide/messages';
	import { localizeHref } from '$lib/paraglide/runtime';
	import { resolve } from '$app/paths';
	import type { Pathname } from '$app/types';
	import { AlertCircle, Home, ArrowLeft, ChevronDown, ChevronUp } from '@lucide/svelte';
	import { styleStore } from '$lib/stores';

	// エラーのステータスコードとメッセージをリアクティブに監視
	const status = $derived(page.status);
	const error = $derived(page.error);

	let showDetails = $state(false);

	// ステータスに応じた適切なメッセージとアイコンを返す
	const info = $derived.by(() => {
		if (status === 404) {
			return {
				title: m.error_404_title(),
				message: m.error_404_message(),
				icon: AlertCircle,
				color: 'var(--color-warning-500)'
			};
		}
		if (status === 503) {
			return {
				title: m.error_503_title(),
				message: m.error_503_message(),
				icon: AlertCircle,
				color: 'var(--color-primary-500)'
			};
		}
		return {
			title: m.error_generic_title(),
			message: error?.message || m.error_generic_message(),
			icon: AlertCircle,
			color: 'var(--color-danger-500)'
		};
	});

	function goBack() {
		if (typeof window !== 'undefined') {
			window.history.back();
		}
	}
</script>

<div class="flex min-h-[70vh] flex-col items-center justify-center p-6 text-center">
	<div
		class="w-full max-w-md p-8 {styleStore.pack.materials
			.card} text-[var(--color-nav-active)] transition-all duration-300"
		style="animation: slide-in 0.4s {styleStore.pack.easing.default} forwards;"
	>
		<!-- Icon Container -->
		<div class="mb-6 flex justify-center">
			<div
				class="flex h-16 w-16 items-center justify-center rounded-full bg-[var(--color-surface-muted)]/50"
				style="color: {info.color};"
			>
				<info.icon size={36} />
			</div>
		</div>

		<!-- Status Code -->
		<span
			class="mb-2 block text-5xl font-extrabold tracking-tight opacity-90 {styleStore.pack
				.typography.numeric}"
		>
			{status}
		</span>

		<!-- Error Title -->
		<h1 class="mb-3 text-2xl font-bold tracking-tight {styleStore.pack.typography.heading}">
			{info.title}
		</h1>

		<!-- Error Description -->
		<p class="mb-8 text-sm leading-relaxed opacity-75">
			{info.message}
		</p>

		<!-- Actions -->
		<div class="flex flex-col gap-3 sm:flex-row sm:justify-center">
			<button
				onclick={goBack}
				class="flex items-center justify-center gap-2 rounded-full px-5 py-3 text-sm font-medium border border-[var(--color-surface-border)] hover:bg-[var(--color-surface-muted)]/50 transition-all duration-150 {styleStore
					.pack.press.button}"
			>
				<ArrowLeft size={16} />
				{m.error_go_back()}
			</button>

			<a
				href={resolve(localizeHref('/') as Pathname)}
				class="flex items-center justify-center gap-2 rounded-full bg-[var(--color-primary-500)] text-white px-5 py-3 text-sm font-medium hover:bg-[var(--color-primary-600)] transition-all duration-150 {styleStore
					.pack.press.button}"
			>
				<Home size={16} />
				{m.error_go_home()}
			</a>
		</div>

		<!-- Details Accordion (For debug) -->
		{#if error && error.message}
			<div class="mt-8 border-t border-[var(--color-surface-border)]/50 pt-4 text-left">
				<button
					onclick={() => (showDetails = !showDetails)}
					class="flex w-full items-center justify-between text-xs font-semibold opacity-60 hover:opacity-100 transition-opacity"
				>
					<span>{showDetails ? m.error_hide_details() : m.error_show_details()}</span>
					{#if showDetails}
						<ChevronUp size={14} />
					{:else}
						<ChevronDown size={14} />
					{/if}
				</button>

				{#if showDetails}
					<div
						class="mt-3 max-h-40 overflow-y-auto rounded-lg bg-[var(--color-surface-page)] p-3 text-xs font-mono opacity-80 leading-relaxed break-all"
						style="animation: expand-accordion 0.2s {styleStore.pack.easing.default} forwards;"
					>
						{error.message}
						{#if (error as unknown as Record<string, unknown>).stack}
							<pre class="mt-2 whitespace-pre-wrap opacity-60 text-[10px]">{(
									error as unknown as Record<string, unknown>
								).stack}</pre>
						{/if}
					</div>
				{/if}
			</div>
		{/if}
	</div>
</div>

<style>
	@keyframes slide-in {
		from {
			opacity: 0;
			transform: scale(0.96) translateY(8px);
		}
		to {
			opacity: 1;
			transform: scale(1) translateY(0);
		}
	}

	@keyframes expand-accordion {
		from {
			opacity: 0;
			transform: translateY(-4px);
		}
		to {
			opacity: 1;
			transform: translateY(0);
		}
	}
</style>
