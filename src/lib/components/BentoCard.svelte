<!--
  BentoCard.svelte
  Apple Design ガイドラインに準拠した汎用 Bento Tile / Card コンポーネント。
  すりガラス効果 (LiquidGlass)、即時応答 (active:scale-[0.98])、レスポンシブ配置に対応。
-->
<script lang="ts">
	import type { Snippet } from 'svelte';
	import type { HTMLAnchorAttributes, HTMLButtonAttributes } from 'svelte/elements';
	import GlassContainer from './GlassContainer.svelte';
	import { APPLE_PRESS } from '$lib/apple-design';

	type ClickHandler = (event: MouseEvent) => void;

	interface CommonProps {
		/** タイトルのオプション */
		title?: string;
		/** アイコンやサブテキスト等のヘッダー用スロット */
		header?: Snippet;
		/** カードのコンテンツスロット */
		children?: Snippet;
		/** フッター領域のスロット */
		footer?: Snippet;
		/** クリックハンドラー */
		onclick?: ClickHandler;
		/** リンク先 */
		href?: string;
		/** カラムサイズ指定（Tailwind class e.g. "col-span-1 md:col-span-2"） */
		colSpan?: string;
		/** 行サイズ指定（Tailwind class e.g. "row-span-1 md:row-span-2"） */
		rowSpan?: string;
		/** パディング指定（デフォルト: "p-4 md:p-5"） */
		padding?: string;
		/** インタラクティブなカードにするか */
		interactive?: boolean;
		/** 追加のカスタムクラス */
		class?: string;
	}

	type CardAsButtonProps = CommonProps &
		Omit<HTMLButtonAttributes, 'children' | 'class' | 'onclick'> & {
			href?: undefined;
		};

	type CardAsAnchorProps = CommonProps &
		Omit<HTMLAnchorAttributes, 'children' | 'class' | 'onclick'> & {
			href: string;
		};

	type Props = CardAsButtonProps | CardAsAnchorProps;

	let {
		title,
		header,
		children,
		footer,
		onclick,
		href,
		colSpan = 'col-span-1',
		rowSpan = 'row-span-1',
		padding = 'p-4 md:p-5',
		interactive = false,
		class: klass = '',
		...rest
	}: Props = $props();

	const isClickable = $derived(Boolean(href || onclick || interactive));
	const isFlexRow = $derived(klass.includes('flex-row'));

	const containerClass = $derived(
		`rounded-card border border-[var(--color-surface-border)]/60 shadow-apple-sm hover:shadow-apple-md ${
			isClickable ? 'hover:border-[var(--color-primary-500)]/40 cursor-pointer select-none' : ''
		} ${colSpan} ${rowSpan} ${klass}`
	);

	const innerClass = $derived(
		`relative flex ${isFlexRow ? '' : 'flex-col justify-between'} h-full w-full overflow-hidden ${padding} ${
			isClickable ? APPLE_PRESS.card : ''
		}`
	);
</script>

<GlassContainer variant="card" class={containerClass}>
	{#if href}
		<a {...rest as HTMLAnchorAttributes} {href} class={innerClass} {onclick}>
			{@render content()}
		</a>
	{:else if onclick || interactive}
		<button {...rest as HTMLButtonAttributes} type="button" class={innerClass} {onclick}>
			{@render content()}
		</button>
	{:else}
		<div class={innerClass}>
			{@render content()}
		</div>
	{/if}
</GlassContainer>

{#snippet content()}
	{#if title || header}
		<div class="mb-3 flex items-center justify-between gap-2 shrink-0 w-full text-left">
			{#if title}
				<h3 class="text-sm font-semibold tracking-tight text-[var(--color-nav-active)]">
					{title}
				</h3>
			{/if}
			{#if header}{@render header()}{/if}
		</div>
	{/if}

	{#if children}
		<div
			class={isFlexRow
				? 'flex items-center justify-between w-full min-w-0'
				: 'flex-1 min-h-0 w-full text-left'}
		>
			{@render children()}
		</div>
	{/if}

	{#if footer}
		<div
			class="mt-3 shrink-0 pt-2 border-t border-[var(--color-surface-border)]/50 w-full text-left"
		>
			{@render footer()}
		</div>
	{/if}
{/snippet}
