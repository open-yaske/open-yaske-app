<!--
  LiquidGlassButton.svelte
  §12 Materials & depth — Liquid Glass ボタン (iOS 26 style)
  §1  Response — pointer-down 即座にフィードバック
  §4  Behavior over animation — スプリング

  nikdelvin/liquid-glass の SVG feDisplacementMap 方式で実装。
  liquid-glass-svelte パッケージへの依存なし。
  - ライト/ダークテーマ自動連動
  - prefers-reduced-transparency 時はソリッドボタンにフォールバック
  - 追加コンテンツ（子要素）を Liquid Glass 上に配置可能
-->
<script lang="ts">
	import type { Snippet } from 'svelte';
	import { themeStore } from '$lib/stores';
	import LiquidGlassEffect from '$lib/liquid-glass/LiquidGlassEffect.svelte';

	interface Props {
		/** ボタンテキスト */
		label?: string;
		/** Liquid Glass のハイライト色 */
		accentColor?: string;
		/** コントラストモード (未指定時はテーマに自動連動) */
		contrast?: 'light' | 'dark' | 'light-contrast' | 'dark-contrast' | 'auto';
		/** 角丸 (px) */
		roundness?: number;
		/** 横パディング (rem) */
		paddingX?: number;
		/** 縦パディング (rem) */
		paddingY?: number;
		/** フォントサイズ (rem) */
		fontSize?: number;
		/** フォントウェイト */
		fontWeight?: number;
		/** 屈折強度 (0-200) */
		strength?: number;
		/** クロマティックアベレーション (0-5) */
		chromaticAberration?: number;
		/** 追加クラス */
		class?: string;
		/** フォーカス可能かどうか */
		disabled?: boolean;
		/** クリックハンドラ */
		onclick?: (e: MouseEvent) => void;
		/** 子要素 */
		children?: Snippet;
		[key: string]: unknown;
	}

	let {
		label = '',
		accentColor = 'var(--color-primary-400)',
		contrast = 'auto',
		roundness = 60,
		paddingX = 3,
		paddingY = 0.75,
		fontSize = 1.0,
		fontWeight = 500,
		strength = 90,
		chromaticAberration = 1.5,
		class: klass = '',
		disabled = false,
		onclick,
		children,
		...rest
	}: Props = $props();

	// フィルター ID（インスタンスごとにユニーク）
	const filterId = `lg-btn-${Math.random().toString(36).slice(2, 8)}`;

	// ライト / ダークテーマの自動検出
	let isDarkTheme = $state(false);
	$effect(() => {
		if (typeof window === 'undefined') return;
		const checkTheme = () => {
			const hasDarkClass = document.documentElement.classList.contains('dark');
			const isDarkPack =
				themeStore.pack?.id === 'dark' || (themeStore.pack as { isDark?: boolean })?.isDark;
			isDarkTheme = Boolean(hasDarkClass || isDarkPack);
		};
		checkTheme();
		const observer = new MutationObserver(checkTheme);
		observer.observe(document.documentElement, {
			attributes: true,
			attributeFilter: ['class', 'data-theme']
		});
		return () => observer.disconnect();
	});

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

	const effectiveContrast = $derived.by(() => {
		if (contrast !== 'auto') return contrast;
		return isDarkTheme ? 'dark' : 'light';
	});

	const isDark = $derived(effectiveContrast === 'dark' || effectiveContrast === 'dark-contrast');

	const buttonStyle = $derived(
		[
			`border-radius: ${roundness}px`,
			`padding: ${paddingY}rem ${paddingX}rem`,
			`font-size: ${fontSize}rem`,
			`font-weight: ${fontWeight}`,
			`--btn-accent: ${accentColor}`
		].join('; ')
	);
</script>

{#if reducedTransparency}
	<!-- §14 prefers-reduced-transparency: ソリッド背景にフォールバック -->
	<button
		class="lg-btn-solid inline-flex items-center justify-center font-semibold transition-all duration-150 ease-[cubic-bezier(0.16,1,0.3,1)] active:scale-[0.97] {klass}"
		style={buttonStyle}
		{disabled}
		{onclick}
		{...rest}
	>
		{#if children}
			{@render children()}
		{:else}
			{label}
		{/if}
	</button>
{:else}
	<button
		class="lg-btn {isDark ? 'lg-btn-dark' : 'lg-btn-light'} {disabled
			? 'opacity-50 pointer-events-none'
			: ''} {klass}"
		style={buttonStyle}
		{disabled}
		{onclick}
		{...rest}
	>
		<!-- ベースカラーオーバーレイ (z:1) -->
		<span class="lg-btn-overlay absolute inset-0 rounded-[inherit] z-[1] pointer-events-none"
		></span>

		<!-- 屈折エフェクトレイヤー (z:2) -->
		<LiquidGlassEffect
			{filterId}
			{strength}
			depth={8}
			{chromaticAberration}
			blur={0}
			saturate={1.2}
			brightness={1.6}
			fallbackBlur={12}
		/>

		<!-- コンテンツ (z:4) -->
		<span class="lg-btn-content relative z-[4] flex items-center justify-center gap-2">
			{#if children}
				{@render children()}
			{:else}
				{label}
			{/if}
		</span>
	</button>
{/if}

<style>
	@property --lg-btn-a1 {
		syntax: '<angle>';
		inherits: false;
		initial-value: -75deg;
	}
	@property --lg-btn-a2 {
		syntax: '<angle>';
		inherits: false;
		initial-value: -45deg;
	}

	/* ── ベースボタン ── */
	.lg-btn {
		position: relative;
		display: inline-flex;
		align-items: center;
		justify-content: center;
		isolation: isolate;
		overflow: hidden;
		cursor: pointer;
		border: none;
		outline: none;
		font-family:
			system-ui,
			-apple-system,
			sans-serif;
		transition:
			transform 0.15s cubic-bezier(0.16, 1, 0.3, 1),
			box-shadow 0.3s cubic-bezier(0.25, 1, 0.5, 1);
		-webkit-tap-highlight-color: transparent;
		will-change: transform;
	}

	.lg-btn:active {
		transform: scale(0.95);
	}

	/* ::before = 内部 sheen */
	.lg-btn::before {
		content: '';
		position: absolute;
		inset: 0;
		border-radius: inherit;
		background: linear-gradient(
			var(--lg-btn-a2),
			transparent 0%,
			rgba(255, 255, 255, 0.3) 25% 35%,
			transparent 60%
		);
		transition: --lg-btn-a2 0.4s cubic-bezier(0.25, 1, 0.5, 1);
		pointer-events: none;
		z-index: 5;
	}

	.lg-btn:hover::before {
		--lg-btn-a2: -15deg;
	}

	/* ::after = 輪郭線 */
	.lg-btn::after {
		content: '';
		position: absolute;
		inset: 0;
		border-radius: inherit;
		padding: 1px;
		mask:
			linear-gradient(#000 0 0) content-box,
			linear-gradient(#000 0 0);
		mask-composite: exclude;
		-webkit-mask-composite: xor;
		transition: --lg-btn-a1 0.5s ease;
		pointer-events: none;
		z-index: 6;
	}

	.lg-btn:hover::after {
		--lg-btn-a1: -125deg;
	}

	/* ── LIGHT ── */
	.lg-btn-light .lg-btn-overlay {
		background: rgba(255, 255, 255, 0.28);
	}

	.lg-btn-light {
		color: rgba(0, 0, 0, 0.85);
		box-shadow:
			0 0 0 1px rgba(255, 255, 255, 0.6),
			inset 0 1px 1px rgba(255, 255, 255, 0.7),
			0 4px 16px -2px rgba(0, 0, 0, 0.12);
	}

	.lg-btn-light::after {
		background:
			conic-gradient(
				from var(--lg-btn-a1) at 50% 50%,
				rgba(0, 0, 0, 0.1),
				transparent 5% 40%,
				rgba(0, 0, 0, 0.1) 50%,
				transparent 60% 95%,
				rgba(0, 0, 0, 0.1)
			),
			linear-gradient(180deg, rgba(255, 255, 255, 0.4), rgba(255, 255, 255, 0.25));
	}

	.lg-btn-light:hover {
		box-shadow:
			0 0 0 1px rgba(255, 255, 255, 0.7),
			inset 0 1px 1px rgba(255, 255, 255, 0.8),
			0 8px 24px -4px rgba(0, 0, 0, 0.18);
	}

	/* ── DARK ── */
	.lg-btn-dark .lg-btn-overlay {
		background: rgba(20, 20, 30, 0.5);
	}

	.lg-btn-dark {
		color: rgba(255, 255, 255, 0.92);
		box-shadow:
			0 0 0 1px rgba(255, 255, 255, 0.15),
			inset 0 1px 1px rgba(255, 255, 255, 0.12),
			0 4px 16px -2px rgba(0, 0, 0, 0.4);
	}

	.lg-btn-dark::after {
		background:
			conic-gradient(
				from var(--lg-btn-a1) at 50% 50%,
				rgba(255, 255, 255, 0.1),
				transparent 5% 40%,
				rgba(255, 255, 255, 0.1) 50%,
				transparent 60% 95%,
				rgba(255, 255, 255, 0.1)
			),
			linear-gradient(180deg, rgba(255, 255, 255, 0.15), rgba(255, 255, 255, 0.08));
	}

	.lg-btn-dark:hover {
		box-shadow:
			0 0 0 1px rgba(255, 255, 255, 0.2),
			inset 0 1px 1px rgba(255, 255, 255, 0.15),
			0 8px 24px -4px rgba(0, 0, 0, 0.5);
	}

	/* ── ソリッドフォールバック ── */
	.lg-btn-solid {
		background: color-mix(in srgb, var(--btn-accent, var(--color-primary-500)) 90%, transparent);
		color: white;
		border: none;
		cursor: pointer;
		font-family:
			system-ui,
			-apple-system,
			sans-serif;
		box-shadow: 0 4px 12px rgba(0, 0, 0, 0.15);
	}

	.lg-btn-solid:hover {
		box-shadow: 0 6px 20px rgba(0, 0, 0, 0.2);
	}

	/* ── reduced-motion ── */
	@media (prefers-reduced-motion: reduce) {
		.lg-btn,
		.lg-btn::before,
		.lg-btn::after {
			animation: none !important;
			transition: none !important;
		}
	}
</style>
