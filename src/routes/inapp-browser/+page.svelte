<!--
  /inapp-browser/+page.svelte
  アプリ内ブラウザルート。?url= クエリパラメータを読み込み、
  InAppBrowser コンポーネントでラップして表示。
  URL 不正時はエラー状態 + 再試行ボタンを表示。
  ツールバー: 戻る / 共有 / URL コピー / 外部ブラウザで開く
-->
<script lang="ts">
	import { page } from '$app/state';
	import { goto } from '$app/navigation';
	import { resolve } from '$app/paths';
	import { m } from '$lib/paraglide/messages';
	import InAppBrowser from '$lib/components/inapp-browser/InAppBrowser.svelte';
	import IconButton from '$lib/components/IconButton.svelte';
	import EmptyState from '$lib/components/EmptyState.svelte';
	import Button from '$lib/components/Button.svelte';
	import { Share2, Copy, AlertCircle, ArrowLeft } from '@lucide/svelte';

	// ?url= クエリパラメータから URL を読み取り
	const rawUrl = $derived(page.url.searchParams.get('url') ?? '');
	const isValidUrl = $derived.by(() => {
		try {
			const u = new URL(rawUrl);
			return u.protocol === 'http:' || u.protocol === 'https:';
		} catch {
			return false;
		}
	});

	// 再試行キー (InAppBrowser を再マウントして iframe をリロード)
	let retryKey = $state(0);

	function handleClose() {
		// 履歴があれば戻る、なければ /more へ
		if (window.history.length > 1) {
			window.history.back();
		} else {
			void goto(resolve('/more'));
		}
	}

	function handleRetry() {
		retryKey++;
	}

	async function handleShare() {
		if (!rawUrl) return;
		try {
			if (navigator.share) {
				await navigator.share({ url: rawUrl });
			} else {
				await navigator.clipboard.writeText(rawUrl);
			}
		} catch {
			// ユーザーがキャンセルした場合は何もしない
		}
	}

	async function handleCopyUrl() {
		if (!rawUrl) return;
		try {
			await navigator.clipboard.writeText(rawUrl);
		} catch {
			// クリップボードアクセス失敗時は何もしない
		}
	}
</script>

<svelte:head><title>{m.browser_share()} | open-nexus</title></svelte:head>

{#if !rawUrl || !isValidUrl}
	<!-- URL 不正時のエラー状態 -->
	<div class="flex min-h-screen flex-col items-center justify-center gap-4 px-4">
		<EmptyState
			icon={AlertCircle}
			message={m.inapp_browser_error()}
			submessage={rawUrl ? rawUrl : 'url='}
			actionLabel={m.inapp_browser_retry()}
			onAction={handleRetry}
		/>
		<Button variant="ghost" size="sm" onclick={handleClose}>
			<ArrowLeft size={16} />
			{m.action_back()}
		</Button>
	</div>
{:else}
	<!-- InAppBrowser でラップして表示 (URL 有効時は常に open) -->
	{#key retryKey}
		<InAppBrowser open={true} url={rawUrl} onclose={handleClose}>
			<IconButton icon={Share2} ariaLabel={m.browser_share()} onclick={handleShare} size="sm" />
			<IconButton icon={Copy} ariaLabel={m.browser_copy_url()} onclick={handleCopyUrl} size="sm" />
		</InAppBrowser>
	{/key}
{/if}
