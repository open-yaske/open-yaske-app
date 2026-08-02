<!--
  EmptyState.svelte
  空状態表示。アイコン + メッセージ + オプションのアクションボタン。
  中央寄せ、テーマ CSS 変数を使用。

  Apple Design 準拠:
  - §1 Response: アクションボタンに APPLE_PRESS.button 適用
  - §16 Accessibility: アイコンコンテナに aria-hidden
-->
<script lang="ts">
	import type { Component } from 'svelte';
	import { styleStore } from '$lib/stores';
	import { resolveComponents } from '$lib/styles';
	import GlassContainer from './GlassContainer.svelte';

	interface Props {
		/** lucide-svelte のアイコンコンポーネント */
		icon: Component<{ size?: number; color?: string; 'stroke-width'?: number }>;
		/** 表示メッセージ（i18n key またはプレーン文字列） */
		message: string;
		/** サブメッセージ（任意） */
		submessage?: string;
		/** アクションボタンのラベル */
		actionLabel?: string;
		/** アクションボタン押下時のコールバック */
		onAction?: () => void;
		/** アイコンサイズ */
		iconSize?: number;
		/** 追加クラス */
		class?: string;
	}

	let {
		icon: Icon,
		message,
		submessage,
		actionLabel,
		onAction,
		iconSize = 48,
		class: klass = ''
	}: Props = $props();

	// スタイルパックの components レシピを解決
	const c = $derived(resolveComponents(styleStore.pack));
</script>

<div class="flex flex-col items-center justify-center gap-4 px-6 py-12 text-center {klass}">
	<!-- §12アイコンコンテナ: Liquid Glass すりガラス背景 -->
	<GlassContainer variant="subtle" class="rounded-full p-4">
		<div aria-hidden="true">
			<Icon size={iconSize} color="var(--color-nav-inactive)" stroke-width={1.5} />
		</div>
	</GlassContainer>
	<div class="space-y-1">
		<p class="text-sm font-medium text-[var(--color-nav-active)]">{message}</p>
		{#if submessage}
			<p class="text-xs text-[var(--color-nav-inactive)]">{submessage}</p>
		{/if}
	</div>
	{#if actionLabel && onAction}
		<button
			type="button"
			onclick={onAction}
			class="min-h-[44px] rounded-full px-5 py-2.5 text-sm font-medium {c.button.primary}
			       focus:outline-none focus-visible:ring-2 focus-visible:ring-[var(--color-primary-500)] focus-visible:ring-offset-2
			       {styleStore.pack.press.button}"
		>
			{actionLabel}
		</button>
	{/if}
</div>
