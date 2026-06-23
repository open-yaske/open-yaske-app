<!--
  Emoji.svelte
  設定に応じて、標準のネイティブ絵文字または Twemoji (SVG画像) を切り替えて表示するコンポーネント。
-->
<script lang="ts">
	import { settingsStore } from '$lib/stores';

	interface Props {
		emoji: string;
		class?: string;
	}
	let { emoji, class: className = '' }: Props = $props();

	const isTwemoji = $derived(settingsStore.settings.emojiStyle === 'twemoji');

	// サロゲートペアを考慮して Unicode コードポイントを算出する
	function toCodePoint(unicodeSurrogates: string) {
		const r: string[] = [];
		let p = 0,
			i = 0;
		while (i < unicodeSurrogates.length) {
			const c = unicodeSurrogates.charCodeAt(i++);
			if (p) {
				r.push((0x10000 + ((p - 0xd800) << 10) + (c - 0xdc00)).toString(16));
				p = 0;
			} else if (0xd800 <= c && c <= 0xdbff) {
				p = c;
			} else {
				r.push(c.toString(16));
			}
		}
		// fe0f は Twemoji で不要なバリエーションセレクタなので除外する
		return r.filter((cp) => cp !== 'fe0f').join('-');
	}

	const src = $derived.by(() => {
		try {
			const cp = toCodePoint(emoji);
			return `https://cdn.jsdelivr.net/gh/jdecked/twemoji@14.1.2/assets/svg/${cp}.svg`;
		} catch {
			return '';
		}
	});
</script>

{#if isTwemoji && src}
	<img
		{src}
		alt={emoji}
		class="inline-block h-[1.2em] w-[1.2em] align-middle select-none max-w-none {className}"
	/>
{:else}
	<span class={className}>{emoji}</span>
{/if}
