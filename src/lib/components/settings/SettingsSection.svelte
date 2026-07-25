<!--
  SettingsSection.svelte
  設定ページのセクションラッパー。
  iOS風のグループ化カード: グレーセクション見出し + 白い角丸カード + アイテムスロット。
-->
<script lang="ts">
	import SectionHeader from '../SectionHeader.svelte';
	import GlassContainer from '../GlassContainer.svelte';
	import type { Snippet } from 'svelte';

	interface Props {
		/** セクション見出しテキスト */
		title: string;
		/** セクションのHTML id（スクロールターゲット用） */
		id?: string;
		/** 補助説明（見出し下に表示） */
		description?: string;
		/** クラス */
		class?: string;
		/** アイテム群 */
		children: Snippet;
	}

	let { title, id, description, class: className = '', children }: Props = $props();
</script>

<section {id} class="scroll-mt-20 {className}">
	<SectionHeader>{title}</SectionHeader>
	{#if description}
		<p class="mb-2 px-1 text-xs text-[var(--color-nav-inactive)]">{description}</p>
	{/if}
	<GlassContainer variant="card" class="overflow-hidden rounded-xl">
		{@render children()}
	</GlassContainer>
</section>
