<!--
  SettingsSection.svelte
  設定ページのセクションラッパー。
  iOS風のグループ化カード: グレーセクション見出し + 白い角丸カード + アイテムスロット。
-->
<script lang="ts">
	import type { Snippet } from 'svelte';

	interface Props {
		/** セクション見出しテキスト */
		title: string;
		/** セクションのHTML id（スクロールターゲット用） */
		id?: string;
		/** 補助説明（見出し下に表示） */
		description?: string;
		/** アイテム群 */
		children: Snippet;
	}

	let { title, id, description, children }: Props = $props();
</script>

<section {id} class="scroll-mt-20">
	<div
		class="mb-1.5 px-1 text-xs font-medium uppercase tracking-wide text-[var(--color-nav-inactive)]"
	>
		{title}
	</div>
	{#if description}
		<p class="mb-2 px-1 text-xs text-[var(--color-nav-inactive)]">{description}</p>
	{/if}
	<div
		class="overflow-hidden rounded-xl border border-[var(--color-surface-border)] bg-[var(--color-surface-card)]"
	>
		{@render children()}
	</div>
</section>
