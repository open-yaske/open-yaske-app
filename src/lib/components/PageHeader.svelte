<!--
  PageHeader.svelte
  モバイル用の画面ヘッダー。タイトル中央 + 左/右のアイコンボタン・スロット。
-->
<script lang="ts">
	import type { Component, Snippet } from 'svelte';
	import IconButton from './IconButton.svelte';
	import GlassContainer from './GlassContainer.svelte';

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

<GlassContainer
	variant="header"
	class="sticky top-0 z-20 border-b border-[var(--color-surface-border)]/40 px-4 py-2.5"
>
	<div class="grid min-h-[44px] grid-cols-[5rem_minmax(0,1fr)_5rem] items-center">
		<div class="flex min-h-[44px] items-center justify-start gap-2 overflow-visible">
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
				<h1 class="truncate text-base font-bold tracking-tight text-[var(--color-nav-active)]">
					{title}
				</h1>
				{#if subtitle}
					<p class="truncate text-xs font-medium text-[var(--color-nav-inactive)]">{subtitle}</p>
				{/if}
			{/if}
		</div>

		<div class="flex min-h-[44px] items-center justify-end gap-2 overflow-visible">
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
		<div class="mt-2 flex min-h-[36px] items-center justify-center">
			{@render children()}
		</div>
	{/if}
</GlassContainer>
