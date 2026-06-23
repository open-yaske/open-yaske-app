<!--
  EmptyState.svelte
  空状態表示。アイコン + メッセージ + オプションのアクションボタン。
  中央寄せ、テーマ CSS 変数を使用。
-->
<script lang="ts">
	import type { Component } from 'svelte';

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
</script>

<div class="flex flex-col items-center justify-center gap-4 px-6 py-12 text-center {klass}">
	<div class="flex items-center justify-center rounded-full bg-[var(--color-surface-muted)] p-4">
		<Icon size={iconSize} color="var(--color-nav-inactive)" stroke-width={1.5} />
	</div>
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
			class="rounded-full bg-[var(--color-primary-500)] px-5 py-2 text-sm font-medium text-white shadow-sm transition-colors hover:bg-[var(--color-primary-600)] focus:outline-none focus-visible:ring-2 focus-visible:ring-[var(--color-primary-500)] focus-visible:ring-offset-2"
		>
			{actionLabel}
		</button>
	{/if}
</div>
