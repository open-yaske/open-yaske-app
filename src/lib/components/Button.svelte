<!--
  Button.svelte
  共通ボタン。primary / secondary / ghost / danger の4バリアント × sm / md / lg の3サイズ。

  - `href` を渡すと <a> として描画し、それ以外は <button> として描画する
  - 色はすべてテーマの CSS 変数経由で参照（raw HEX 禁止）
  - 子要素は Svelte 5 の Snippet `children` を受け取る
-->
<script lang="ts">
	import type { Snippet } from 'svelte';
	import type { HTMLAnchorAttributes, HTMLButtonAttributes } from 'svelte/elements';

	type Variant = 'primary' | 'secondary' | 'ghost' | 'danger' | 'glass';
	type Size = 'sm' | 'md' | 'lg';

	type ClickHandler = (event: MouseEvent) => void;

	interface CommonProps {
		variant?: Variant;
		size?: Size;
		disabled?: boolean;
		class?: string;
		children?: Snippet;
		onclick?: ClickHandler;
	}

	type ButtonProps = CommonProps &
		Omit<HTMLButtonAttributes, 'children' | 'class' | 'onclick'> & {
			href?: undefined;
		};

	type AnchorProps = CommonProps &
		Omit<HTMLAnchorAttributes, 'children' | 'class' | 'onclick'> & {
			href: string;
		};

	type Props = ButtonProps | AnchorProps;

	let {
		variant = 'primary',
		size = 'md',
		disabled = false,
		class: klass = '',
		children,
		href,
		onclick,
		...rest
	}: Props = $props();

	import { styleStore } from '$lib/stores';
	import { resolveComponents } from '$lib/styles';

	// スタイルパックの components レシピを解決（未定義キーは中央デフォルトにフォールバック）
	const c = $derived(resolveComponents(styleStore.pack));

	// base + shape + press を合成（形状とプレスフィードバックはスタイル依存）
	const baseClass = $derived(`${c.button.base} ${c.button.shape} ${styleStore.pack.press.button}`);

	const sizeClass = $derived(
		{
			sm: c.button.sm,
			md: c.button.md,
			lg: c.button.lg
		}[size]
	);

	const variantClass = $derived(
		{
			primary: c.button.primary,
			secondary: c.button.secondary,
			ghost: c.button.ghost,
			danger: c.button.danger,
			glass: c.button.glass
		}[variant]
	);

	const composedClass = $derived(`${baseClass} ${sizeClass} ${variantClass} ${klass}`);
</script>

{#if href}
	<a
		{...rest as HTMLAnchorAttributes}
		{href}
		class={composedClass}
		aria-disabled={disabled || undefined}
		role={disabled ? 'button' : undefined}
		tabindex={disabled ? -1 : undefined}
		{onclick}
	>
		{#if children}{@render children()}{/if}
	</a>
{:else}
	<button
		{...rest as HTMLButtonAttributes}
		type="button"
		class={composedClass}
		{disabled}
		{onclick}
	>
		{#if children}{@render children()}{/if}
	</button>
{/if}
