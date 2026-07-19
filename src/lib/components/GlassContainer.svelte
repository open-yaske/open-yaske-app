<!--
  GlassContainer.svelte
  danilofiumi/liquid-glass-svelte (https://github.com/danilofiumi/liquid-glass-svelte)
  準拠の OpenYASKE 共通 Liquid Glass コンポーネント。

  特徴:
  - SVG 液体ノイズ屈折歪み (feTurbulence + feDisplacementMap + saturate)
  - ライト / ダークテーマへの自動連動 (isDark 検出 & contrast 動的切り替え)
  - prefers-reduced-transparency 時はソリッド背景に自動フォールバック
-->
<script lang="ts">
	import type { Snippet } from 'svelte';
	import { themeStore } from '$lib/stores';

	type GlassVariant = 'card' | 'bar' | 'modal' | 'header' | 'subtle' | 'input';
	type GlassContrast = 'auto' | 'light' | 'dark' | 'light-contrast' | 'dark-contrast';

	interface Props {
		/** 用途別プリセット */
		variant?: GlassVariant;
		/** コントラストモード (未指定時はテーマに自動連動) */
		contrast?: GlassContrast;
		/** 追加クラス */
		class?: string;
		/** インラインスタイル */
		style?: string;
		children: Snippet;
		[key: string]: unknown;
	}

	let {
		variant = 'card',
		contrast = 'auto',
		class: klass = '',
		style = '',
		children,
		...rest
	}: Props = $props();

	// prefers-reduced-transparency チェック
	let reducedTransparency = $state(false);
	$effect(() => {
		if (typeof window === 'undefined') return;
		const mq = window.matchMedia('(prefers-reduced-transparency: reduce)');
		reducedTransparency = mq.matches;
		const handler = (e: MediaQueryListEvent) => {
			reducedTransparency = e.matches;
		};
		mq.addEventListener('change', handler);
		return () => mq.removeEventListener('change', handler);
	});

	// ライト / ダークテーマの自動検出 & 連動
	let isDarkTheme = $state(false);
	$effect(() => {
		if (typeof window === 'undefined') return;
		const checkTheme = () => {
			const hasDarkClass = document.documentElement.classList.contains('dark');
			const isDarkPack = themeStore.pack?.id === 'dark' || (themeStore.pack as { isDark?: boolean })?.isDark;
			isDarkTheme = Boolean(hasDarkClass || isDarkPack);
		};

		checkTheme();

		// MutationObserver で documentElement の クラス変更を監視
		const observer = new MutationObserver(checkTheme);
		observer.observe(document.documentElement, { attributes: true, attributeFilter: ['class', 'data-theme'] });

		return () => observer.disconnect();
	});

	const effectiveContrast = $derived.by(() => {
		if (contrast !== 'auto') return contrast;
		return isDarkTheme ? 'dark' : 'light';
	});

	const isDarkStyle = $derived(effectiveContrast === 'dark' || effectiveContrast === 'dark-contrast');

	const variantBlurMap: Record<GlassVariant, string> = {
		card: '16px',
		bar: '24px',
		modal: '32px',
		header: '20px',
		subtle: '10px',
		input: '12px'
	};

	const fallbackStyles: Record<GlassVariant, string> = {
		card: 'bg-[var(--color-surface-card)]',
		bar: 'bg-[var(--color-surface-card)]',
		modal: 'bg-[var(--color-surface-card)]',
		header: 'bg-[var(--color-surface-page)]',
		subtle: 'bg-[var(--color-surface-muted)]',
		input: 'bg-[var(--color-surface-card)]'
	};
</script>

<!-- SVG 液体屈折フィルター定義（グローバル参照用） -->
<svg style="position: absolute; width: 0; height: 0; overflow: hidden; pointer-events: none;" aria-hidden="true">
	<defs>
		<filter id="lg-dist-global" x="-10%" y="-10%" width="120%" height="120%">
			<feTurbulence type="fractalNoise" baseFrequency="0.008 0.008" numOctaves="2" seed="92" result="noise" />
			<feGaussianBlur in="noise" stdDeviation="2" result="blurred" />
			<feDisplacementMap in="SourceGraphic" in2="blurred" scale="120" xChannelSelector="R" yChannelSelector="G" />
		</filter>
	</defs>
</svg>

{#if reducedTransparency}
	<div class="{fallbackStyles[variant]} {klass}" {style} {...rest}>
		{@render children()}
	</div>
{:else}
	<div
		class="liquid-glass-container relative overflow-hidden {isDarkStyle ? 'dark-glassy' : 'light-glassy'} {klass}"
		style="--glass-blur: {variantBlurMap[variant]}; {style}"
		data-contrast={effectiveContrast}
		{...rest}
	>
		<!-- 液体屈折フィルター・すりガラス描画層 -->
		<div class="glass-filter-layer pointer-events-none absolute inset-0 z-0"></div>

		<!-- 明るいエッジの光（Apple / Danilo Fiumi Sheen） -->
		<div class="glass-edge-highlight pointer-events-none absolute inset-x-0 top-0 h-px z-10"></div>

		<!-- コンテンツ領域 -->
		<div class="relative z-10 h-full w-full">
			{@render children()}
		</div>
	</div>
{/if}

<style>
	.liquid-glass-container {
		position: relative;
		isolation: isolate;
		transition: background-color 300ms cubic-bezier(0.25, 1, 0.5, 1), box-shadow 300ms cubic-bezier(0.25, 1, 0.5, 1);
	}

	/* 液体屈折フィルタースタイル (Danilo Fiumi 歪みエフェクト) */
	.glass-filter-layer {
		-webkit-backdrop-filter: blur(var(--glass-blur, 16px));
		backdrop-filter: blur(var(--glass-blur, 16px));
		filter: url(#lg-dist-global) saturate(140%);
	}

	/* Light モードのすりガラス質感 */
	.light-glassy {
		background: color-mix(in srgb, var(--color-surface-card) 75%, transparent);
		box-shadow:
			inset 0 1px 1px rgba(255, 255, 255, 0.6),
			inset 0 -1px 1px rgba(0, 0, 0, 0.05),
			0 4px 20px -2px rgba(0, 0, 0, 0.08);
	}

	.light-glassy .glass-edge-highlight {
		background: linear-gradient(90deg, rgba(255, 255, 255, 0), rgba(255, 255, 255, 0.8) 50%, rgba(255, 255, 255, 0));
	}

	/* Dark モードのすりガラス質感 */
	.dark-glassy {
		background: color-mix(in srgb, var(--color-surface-card) 80%, transparent);
		box-shadow:
			inset 0 1px 1px rgba(255, 255, 255, 0.15),
			inset 0 -1px 1px rgba(0, 0, 0, 0.4),
			0 8px 32px -4px rgba(0, 0, 0, 0.5);
	}

	.dark-glassy .glass-edge-highlight {
		background: linear-gradient(90deg, rgba(255, 255, 255, 0), rgba(255, 255, 255, 0.25) 50%, rgba(255, 255, 255, 0));
	}
</style>
