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

	type Variant = 'primary' | 'secondary' | 'ghost' | 'danger';
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

	const baseClass =
		'inline-flex items-center justify-center gap-2 font-medium rounded-[var(--radius-chip)] transition-colors select-none whitespace-nowrap focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-[var(--color-primary-500)] focus-visible:ring-offset-2 focus-visible:ring-offset-[var(--color-surface-page)] disabled:opacity-50 disabled:cursor-not-allowed disabled:pointer-events-none';

	const sizeClass = $derived(
		{
			sm: 'h-8 px-3 text-sm',
			md: 'h-10 px-4 text-sm',
			lg: 'h-12 px-6 text-base'
		}[size]
	);

	const variantClass = $derived(
		{
			primary:
				'bg-[var(--color-primary-500)] text-white hover:bg-[var(--color-primary-600)] active:bg-[var(--color-primary-700)] shadow-sm',
			secondary:
				'bg-[var(--color-surface-card)] text-[var(--color-nav-active)] border border-[var(--color-surface-border)] hover:bg-[var(--color-surface-muted)]',
			ghost: 'bg-transparent text-[var(--color-nav-active)] hover:bg-[var(--color-surface-muted)]',
			danger:
				'bg-[var(--color-attendance-absent-bg)] text-[var(--color-attendance-absent-fg)] hover:opacity-90 shadow-sm'
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
