<!--
  SectionHeader.svelte
  カテゴリヘッダー。ダーク/ライト両テーマ対応。
-->
<script lang="ts">
	import type { Snippet } from 'svelte';
	import GlassContainer from './GlassContainer.svelte';

	interface Props {
		children: Snippet;
		variant?: 'default' | 'light';
	}

	let { children, variant = 'default' }: Props = $props();
</script>

{#if variant === 'default'}
	<div
		class="my-3 block w-full rounded-chip px-4 py-2 text-xs font-bold tracking-widest uppercase bg-[var(--color-primary-500)] text-white transition-colors"
	>
		{@render children()}
	</div>
{:else}
	<GlassContainer variant="subtle" class="my-3 w-full rounded-chip px-4 py-2">
		<span class="text-xs font-bold tracking-widest uppercase text-[var(--color-nav-active)]">
			{@render children()}
		</span>
	</GlassContainer>
{/if}
