<!--
  IconButton.svelte
  アイコン専用ボタン。aria-label 必須。
-->
<script lang="ts">
	import type { Component } from 'svelte';
	import { styleStore } from '$lib/stores';
	import { resolveComponents } from '$lib/styles';

	interface Props {
		/** lucide-svelte のアイコンコンポーネント */
		icon: Component<{ size?: number; color?: string; 'stroke-width'?: number }>;
		ariaLabel: string;
		onclick?: (e: MouseEvent) => void;
		variant?: 'ghost' | 'primary' | 'subtle' | 'danger';
		size?: 'sm' | 'md' | 'lg';
		disabled?: boolean;
		active?: boolean;
		class?: string;
	}

	let {
		icon: Icon,
		ariaLabel,
		onclick,
		variant = 'ghost',
		size = 'md',
		disabled = false,
		active = false,
		class: klass = ''
	}: Props = $props();

	const sizeClass = $derived(
		{
			sm: 'w-9 h-9 min-w-[44px] min-h-[44px]',
			md: 'w-11 h-11 min-w-[44px] min-h-[44px]',
			lg: 'w-12 h-12 min-w-[48px] min-h-[48px]'
		}[size]
	);
	const iconSize = $derived({ sm: 18, md: 20, lg: 24 }[size]);

	// スタイルパックの components レシピを解決
	const c = $derived(resolveComponents(styleStore.pack));

	const variantClass = $derived(
		{
			ghost: c.button.ghost,
			primary: c.button.primary,
			subtle: c.button.secondary,
			danger: c.button.danger
		}[variant]
	);

	const activeClass = $derived(
		active
			? 'bg-[var(--color-primary-50)] text-[var(--color-primary-600)] dark:bg-[var(--color-primary-950)]/40'
			: ''
	);
</script>

<button
	type="button"
	class="inline-flex items-center justify-center rounded-full shrink-0 {styleStore.pack.press
		.icon} select-none disabled:opacity-50 disabled:cursor-not-allowed disabled:pointer-events-none {sizeClass} {variantClass} {activeClass} {klass}"
	aria-label={ariaLabel}
	{disabled}
	{onclick}
>
	<Icon size={iconSize} />
</button>
