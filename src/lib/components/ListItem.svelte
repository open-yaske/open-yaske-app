<!--
  ListItem.svelte
  アイコンチップ + タイトル + サブタイトル + 末尾矢印の行アイテム。
  href があれば <a>、onclick があれば <button>、それ以外は div。
  href は内部パスの場合 resolve() でラップする (外部URLはそのまま)。
-->
<script lang="ts">
	import type { Component, Snippet } from 'svelte';
	import type { RouteId } from '$app/types';
	import { resolve } from '$app/paths';
	import { ChevronRight } from '@lucide/svelte';

	interface Props {
		title: string;
		subtitle?: string;
		icon?: Component<{ size?: number; color?: string }>;
		/** アイコン色 (HEX or テーマ色名) */
		iconColor?: string;
		trailing?: Snippet;
		showChevron?: boolean;
		href?: string;
		onclick?: () => void;
		disabled?: boolean;
		class?: string;
	}

	let {
		title,
		subtitle,
		icon: Icon,
		iconColor = 'var(--color-primary-500)',
		trailing,
		showChevron = true,
		href,
		onclick,
		disabled = false,
		class: klass = ''
	}: Props = $props();

	// 内部パスを resolve() でラップ (外部URLはそのまま)
	const resolvedHref = $derived.by(() => {
		if (!href) return undefined;
		if (/^https?:\/\//.test(href)) return href;
		return resolve(href as RouteId);
	});
</script>

{#if resolvedHref && !disabled}
	<!-- eslint-disable svelte/no-navigation-without-resolve -->
	<a
		href={resolvedHref}
		class="flex items-center gap-3 rounded-xl border border-[var(--color-surface-border)] bg-[var(--color-surface-card)] px-4 py-3 transition-colors hover:bg-[var(--color-surface-muted)] {klass}"
	>
		{@render body()}
	</a>
	<!-- eslint-enable svelte/no-navigation-without-resolve -->
{:else if onclick && !disabled}
	<button
		type="button"
		{onclick}
		class="flex w-full items-center gap-3 rounded-xl border border-[var(--color-surface-border)] bg-[var(--color-surface-card)] px-4 py-3 text-left transition-colors hover:bg-[var(--color-surface-muted)] {klass}"
	>
		{@render body()}
	</button>
{:else}
	<div
		class="flex items-center gap-3 rounded-xl border border-[var(--color-surface-border)] bg-[var(--color-surface-card)] px-4 py-3 {klass}"
		class:opacity-50={disabled}
	>
		{@render body()}
	</div>
{/if}

{#snippet body()}
	{#if Icon}
		<div
			class="flex h-9 w-9 shrink-0 items-center justify-center rounded-lg"
			style="background-color: {iconColor}1A; color: {iconColor}"
		>
			<Icon size={18} />
		</div>
	{/if}
	<div class="min-w-0 flex-1">
		<div class="truncate text-sm font-medium text-[var(--color-nav-active)]">{title}</div>
		{#if subtitle}
			<div class="truncate text-xs text-[var(--color-nav-inactive)]">{subtitle}</div>
		{/if}
	</div>
	{#if trailing}
		{@render trailing()}
	{:else if showChevron}
		<ChevronRight size={18} color="var(--color-nav-inactive)" />
	{/if}
{/snippet}
