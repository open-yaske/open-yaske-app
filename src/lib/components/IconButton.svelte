<!--
  IconButton.svelte
  アイコン専用ボタン。aria-label 必須。
-->
<script lang="ts">
	import type { Component } from 'svelte';
	import { APPLE_PRESS } from '$lib/apple-design';

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

	const variantClass = $derived(
		{
			ghost:
				'text-[var(--color-nav-inactive)] hover:backdrop-blur-xl hover:bg-[var(--color-surface-card)]/40 hover:shadow-apple-sm hover:text-[var(--color-nav-active)]',
			primary:
				'bg-[var(--color-primary-500)] text-white hover:bg-[var(--color-primary-600)] shadow-apple-sm hover:shadow-apple-md',
			subtle:
				'bg-[var(--color-surface-muted)] backdrop-blur-md text-[var(--color-nav-active)] hover:bg-[var(--color-surface-border)]/80',
			danger:
				'bg-[var(--color-attendance-absent-bg)] text-[var(--color-attendance-absent-fg)] hover:opacity-90 shadow-apple-sm'
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
	class="inline-flex items-center justify-center rounded-full shrink-0 {APPLE_PRESS.icon} select-none disabled:opacity-50 disabled:cursor-not-allowed disabled:pointer-events-none {sizeClass} {variantClass} {activeClass} {klass}"
	aria-label={ariaLabel}
	{disabled}
	{onclick}
>
	<Icon size={iconSize} />
</button>
