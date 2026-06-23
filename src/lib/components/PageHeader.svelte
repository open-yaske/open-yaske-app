<!--
  PageHeader.svelte
  モバイル用の画面ヘッダー。タイトル中央 + 左/右のアイコンボタン・スロット。
-->
<script lang="ts">
	import type { Component, Snippet } from 'svelte';
	import IconButton from './IconButton.svelte';

	interface Props {
		title?: string;
		subtitle?: string;
		leadingIcon?: Component<{ size?: number }>;
		leadingAriaLabel?: string;
		onleading?: () => void;
		trailingIcon?: Component<{ size?: number }>;
		trailingAriaLabel?: string;
		ontrailing?: () => void;
		leading?: Snippet;
		trailing?: Snippet;
		titleSnippet?: Snippet;
		children?: Snippet;
	}

	let {
		title = '',
		subtitle,
		leadingIcon: Leading,
		leadingAriaLabel,
		onleading,
		trailingIcon: Trailing,
		trailingAriaLabel,
		ontrailing,
		leading,
		trailing,
		titleSnippet,
		children
	}: Props = $props();
</script>

<header class="sticky top-0 z-20 bg-[var(--color-surface-page)] px-4 py-3">
	<div class="grid min-h-9 grid-cols-[5rem_minmax(0,1fr)_5rem] items-center">
		<div class="flex h-9 items-center justify-start gap-1 overflow-visible">
			{#if leading}
				{@render leading()}
			{:else if Leading && onleading && leadingAriaLabel}
				<IconButton
					icon={Leading}
					ariaLabel={leadingAriaLabel}
					onclick={onleading}
					variant="subtle"
					size="sm"
				/>
			{/if}
		</div>

		<div class="flex min-w-0 flex-col items-center justify-center px-2 text-center">
			{#if titleSnippet}
				{@render titleSnippet()}
			{:else}
				<h1 class="truncate text-base font-bold text-[var(--color-nav-active)]">{title}</h1>
				{#if subtitle}
					<p class="truncate text-xs text-[var(--color-nav-inactive)]">{subtitle}</p>
				{/if}
			{/if}
		</div>

		<div class="flex h-9 items-center justify-end gap-1 overflow-visible">
			{#if trailing}
				{@render trailing()}
			{:else if Trailing && ontrailing && trailingAriaLabel}
				<IconButton
					icon={Trailing}
					ariaLabel={trailingAriaLabel}
					onclick={ontrailing}
					variant="subtle"
					size="sm"
				/>
			{/if}
		</div>
	</div>

	{#if children}
		<div class="mt-1 flex min-h-8 items-center justify-center">
			{@render children()}
		</div>
	{/if}
</header>
