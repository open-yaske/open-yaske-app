<!--
  InAppBrowser.svelte
  アプリ内ブラウザ。URL を読み込み、ページ内 iframe で表示。
  戻る / 進む / 再読み込み / 閉じる の操作ボタン。
  Capacitor 上で動かしているときは Browser plugin にフォールバック。
-->
<script lang="ts">
	import { onMount } from 'svelte';
	import type { Snippet } from 'svelte';
	import { X, ArrowLeft, ArrowRight, RotateCw, ExternalLink } from '@lucide/svelte';
	import IconButton from '../IconButton.svelte';
	import { Capacitor } from '@capacitor/core';

	interface Props {
		open: boolean;
		url: string;
		onclose: () => void;
		title?: string;
		/** Extra toolbar buttons rendered between URL display and open-external */
		children?: Snippet;
	}

	let { open = $bindable(false), url, onclose, title, children }: Props = $props();

	let history = $state<string[]>([]);
	let currentIndex = $state(0);
	let loading = $state(true);
	let iframe = $state<HTMLIFrameElement | null>(null);
	let lastSeenUrl = $state<string | null>(null);

	const currentUrl = $derived(history[currentIndex] ?? url);
	const canBack = $derived(currentIndex > 0);
	const canForward = $derived(currentIndex < history.length - 1);

	$effect(() => {
		// url prop が変わったら履歴に追加
		if (lastSeenUrl !== url && url) {
			lastSeenUrl = url;
			if (history.length === 0) {
				history = [url];
				currentIndex = 0;
			} else {
				history = [...history.slice(0, currentIndex + 1), url];
				currentIndex = history.length - 1;
			}
		}
	});

	function goBack() {
		if (canBack) {
			currentIndex--;
			loading = true;
		}
	}
	function goForward() {
		if (canForward) {
			currentIndex++;
			loading = true;
		}
	}
	function reload() {
		loading = true;
		if (iframe) {
			iframe.src = currentUrl;
		}
	}
	async function openExternal() {
		if (typeof window !== 'undefined') {
			window.open(currentUrl, '_blank', 'noopener,noreferrer');
		}
	}

	onMount(() => {
		// Capacitor 環境では Browser plugin を使うこともできるが、シンプルさを優先して iframe のみ
		void Capacitor;
	});
</script>

{#if open}
	<div class="fixed inset-0 z-40 flex flex-col bg-[var(--color-surface-page)]">
		<!-- Header -->
		<header
			class="flex shrink-0 items-center gap-2 border-b border-[var(--color-surface-border)] bg-[var(--color-surface-card)] px-2 py-2"
		>
			<IconButton icon={X} ariaLabel="Close" onclick={onclose} />
			<IconButton icon={ArrowLeft} ariaLabel="Back" onclick={goBack} disabled={!canBack} />
			<IconButton
				icon={ArrowRight}
				ariaLabel="Forward"
				onclick={goForward}
				disabled={!canForward}
			/>
			<IconButton icon={RotateCw} ariaLabel="Reload" onclick={reload} />
			<div
				class="min-w-0 flex-1 truncate rounded-full bg-[var(--color-surface-muted)] px-3 py-1.5 text-xs text-[var(--color-nav-active)]"
			>
				{title ?? currentUrl}
			</div>
			{#if children}{@render children()}{/if}
			<IconButton icon={ExternalLink} ariaLabel="Open in external browser" onclick={openExternal} />
		</header>

		<!-- iframe -->
		<div class="relative flex-1 overflow-hidden bg-white">
			{#if loading}
				<div
					class="absolute inset-0 flex items-center justify-center text-sm text-[var(--color-nav-inactive)]"
				>
					Loading...
				</div>
			{/if}
			<iframe
				bind:this={iframe}
				src={currentUrl}
				class="h-full w-full border-0"
				title={title ?? 'In-app browser'}
				sandbox="allow-scripts allow-same-origin allow-forms allow-popups allow-popups-to-escape-sandbox allow-top-navigation-by-user-activation"
				onload={() => (loading = false)}
			></iframe>
		</div>
	</div>
{/if}
