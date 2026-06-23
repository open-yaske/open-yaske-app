<!--
  IconButton.svelte
  アイコン専用ボタン。aria-label 必須。
-->
<script lang="ts">
	import type { Component } from 'svelte';

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

	const sizeClass = $derived({ sm: 'w-8 h-8', md: 'w-10 h-10', lg: 'w-12 h-12' }[size]);
	const iconSize = $derived({ sm: 16, md: 20, lg: 24 }[size]);

	const variantClass = $derived(
		{
			ghost:
				'text-[var(--color-nav-inactive)] hover:bg-[var(--color-surface-muted)] hover:text-[var(--color-nav-active)]',
			primary:
				'bg-[var(--color-primary-500)] text-white hover:bg-[var(--color-primary-600)] shadow-sm',
			subtle:
				'bg-[var(--color-surface-muted)] text-[var(--color-nav-active)] hover:bg-[var(--color-surface-border)]',
			danger: 'bg-red-500 text-white hover:bg-red-600'
		}[variant]
	);

	const activeClass = $derived(
		active ? 'bg-[var(--color-primary-50)] text-[var(--color-primary-600)]' : ''
	);
</script>

<button
	type="button"
	class="inline-flex items-center justify-center rounded-full transition-colors {sizeClass} {variantClass} {activeClass} {klass}"
	aria-label={ariaLabel}
	{disabled}
	{onclick}
>
	<Icon size={iconSize} />
</button>
