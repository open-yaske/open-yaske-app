<!--
  SectionHeader.svelte
  カテゴリヘッダー。ダーク/ライト両テーマ対応。
-->
<script lang="ts">
	import type { Snippet } from 'svelte';

	interface Props {
		children: Snippet;
		variant?: 'default' | 'light';
	}

	let { children, variant = 'default' }: Props = $props();
</script>

<div
	class="my-3 block w-full rounded-[var(--radius-chip)] px-4 py-2 text-xs font-bold tracking-widest uppercase transition-colors"
	class:bg-[var(--color-primary-500)]={variant === 'default'}
	class:text-white={variant === 'default'}
	class:bg-[var(--color-surface-muted)]={variant === 'light'}
	class:text-[var(--color-nav-active)]={variant === 'light'}
>
	{@render children()}
</div>
