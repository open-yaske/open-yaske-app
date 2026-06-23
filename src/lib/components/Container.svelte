<!--
  Container.svelte
  レスポンシブなページ幅コンテナ。
  モバイルはフル幅 (px-4)、タブレットは中央寄せ (max-w-3xl)、デスクトップは max-w-5xl。
-->
<script lang="ts">
	type Size = 'narrow' | 'default' | 'wide' | 'full';

	interface Props {
		size?: Size;
		class?: string;
		children: import('svelte').Snippet;
	}

	let { size = 'default', class: klass = '', children }: Props = $props();

	const sizeClass = $derived(
		{
			narrow: 'max-w-2xl',
			default: 'max-w-3xl md:max-w-5xl',
			wide: 'max-w-6xl',
			full: 'max-w-none'
		}[size]
	);
</script>

<div class="mx-auto w-full px-4 {sizeClass} {klass}">
	{@render children()}
</div>
