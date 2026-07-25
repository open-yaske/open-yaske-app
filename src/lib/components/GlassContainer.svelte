<!--
  GlassContainer.svelte
  OpenYASKE 共通 Liquid Glass コンテナ（nikdelvin/liquid-glass 方式）。

  SVG feDisplacementMap による本物の屈折エフェクト（Chromium）。
  Safari / Firefox は自動的に glassmorphism backdrop-blur にフォールバック。

  Props インターフェースは旧実装と後方互換を維持。
-->
<script lang="ts">
	import type { Snippet } from 'svelte';
	import { themeStore } from '$lib/stores';
	import LiquidGlassEffect from '$lib/liquid-glass/LiquidGlassEffect.svelte';

	type V = 'card' | 'bar' | 'modal' | 'header' | 'subtle' | 'input';
	type C = 'auto' | 'light' | 'dark' | 'light-contrast' | 'dark-contrast';
	type E = 'sm' | 'md' | 'lg' | 'xl';

	interface Props {
		variant?: V;
		contrast?: C;
		/** 屈折強度プリセット */
		distortion?: 'none' | 'low' | 'medium' | 'high';
		glow?: boolean;
		elevation?: E;
		animateIn?: boolean;
		/** クロマティックアベレーション (0–5, default: variant により自動) */
		chromaticAberration?: number;
		class?: string;
		style?: string;
		children: Snippet;
		[key: string]: unknown;
	}

	let {
		variant = 'card',
		contrast = 'auto',
		distortion,
		glow,
		elevation = 'md',
		animateIn = false,
		chromaticAberration,
		class: klass = '',
		style = '',
		children,
		...rest
	}: Props = $props();

	// distortion → strength マッピング
	const DISTORTION_STRENGTH: Record<string, number> = {
		none: 0,
		low: 40,
		medium: 80,
		high: 140
	};

	// variant ごとのデフォルト強度
	const VARIANT_STRENGTH: Record<V, number> = {
		card: 60,
		bar: 80,
		modal: 100,
		header: 70,
		subtle: 30,
		input: 40
	};

	// variant ごとのフォールバックブラー（Safari / Firefox 用。十分に強くする）
	const VARIANT_BLUR: Record<V, number> = {
		card: 28,
		bar: 36,
		modal: 48,
		header: 32,
		subtle: 20,
		input: 24
	};

	// variant ごとのデフォルト depth
	const VARIANT_DEPTH: Record<V, number> = {
		card: 10,
		bar: 12,
		modal: 14,
		header: 10,
		subtle: 6,
		input: 8
	};

	// フィルター ID（variant + 乱数でユニーク化）— 初期値のみ使うので const で十分
	const filterId = `lg-${variant}-${Math.random().toString(36).slice(2, 8)}`;

	const strength = $derived(
		distortion != null ? DISTORTION_STRENGTH[distortion] : VARIANT_STRENGTH[variant]
	);
	const cab = $derived(chromaticAberration ?? (variant === 'bar' || variant === 'modal' ? 1.5 : 1));

	const useGlow = $derived(glow ?? false);

	let reduced = $state(false);
	$effect(() => {
		if (typeof window === 'undefined') return;
		const mq = window.matchMedia('(prefers-reduced-transparency: reduce)');
		reduced = mq.matches;
		const h = (e: MediaQueryListEvent) => {
			reduced = e.matches;
		};
		mq.addEventListener('change', h);
		return () => mq.removeEventListener('change', h);
	});

	let noMotion = $state(false);
	$effect(() => {
		if (typeof window === 'undefined') return;
		const mq = window.matchMedia('(prefers-reduced-motion: reduce)');
		noMotion = mq.matches;
		const h = (e: MediaQueryListEvent) => {
			noMotion = e.matches;
		};
		mq.addEventListener('change', h);
		return () => mq.removeEventListener('change', h);
	});

	let dark = $state(false);
	$effect(() => {
		if (typeof window === 'undefined') return;
		const ck = () => {
			dark =
				document.documentElement.classList.contains('dark') ||
				themeStore.pack?.id === 'dark' ||
				!!(themeStore.pack as { isDark?: boolean })?.isDark;
		};
		ck();
		const obs = new MutationObserver(ck);
		obs.observe(document.documentElement, {
			attributes: true,
			attributeFilter: ['class', 'data-theme']
		});
		return () => obs.disconnect();
	});

	const mode = $derived.by(() => {
		if (contrast !== 'auto') return contrast;
		return dark ? 'dark' : 'light';
	});
</script>

{#if reduced}
	<!-- prefers-reduced-transparency: ソリッド背景 -->
	<div
		class="lg-root lg-solid {mode === 'dark' || mode === 'dark-contrast'
			? 'lg-dark'
			: 'lg-light'} {klass}"
		style="--lg-elv:{elevation};{style}"
		data-contrast={mode}
		{...rest}
	>
		<div class="relative z-10 h-full w-full">{@render children()}</div>
	</div>
{:else}
	<div
		class="lg-root {mode === 'dark' || mode === 'dark-contrast' ? 'lg-dark' : 'lg-light'}
			{animateIn && !noMotion ? 'lg-in' : ''}
			{klass}"
		style="--lg-elv:{elevation};{style}"
		data-contrast={mode}
		data-variant={variant}
		{...rest}
	>
		<!-- ベースカラーオーバーレイ (z:1) -->
		<div class="lg-overlay-bg absolute inset-0 z-[1] pointer-events-none rounded-[inherit]"></div>

		<!-- feDisplacementMap 屈折レイヤー (z:2) -->
		{#if strength > 0}
			<LiquidGlassEffect
				{filterId}
				{strength}
				depth={VARIANT_DEPTH[variant]}
				chromaticAberration={cab}
				blur={variant === 'bar' ? 3 : 0}
				saturate={1.5}
				brightness={1.05}
				fallbackBlur={VARIANT_BLUR[variant]}
				disabled={reduced}
			/>
		{/if}

		<!-- グロウエフェクト (z:3) -->
		{#if useGlow && !noMotion}
			<div class="lg-glow pointer-events-none absolute inset-0 z-[3]"></div>
		{/if}

		<!-- コンテンツ (z:4) -->
		<div class="relative z-[4] h-full w-full">{@render children()}</div>
	</div>
{/if}

<style>
	@property --lg-a1 {
		syntax: '<angle>';
		inherits: false;
		initial-value: -75deg;
	}
	@property --lg-a2 {
		syntax: '<angle>';
		inherits: false;
		initial-value: -45deg;
	}

	.lg-root {
		position: relative;
		isolation: isolate;
		overflow: hidden;
		transition: box-shadow 0.4s cubic-bezier(0.25, 1, 0.5, 1);
	}

	/* ::before = 内部ハイライト sheen */
	.lg-root::before {
		content: '';
		position: absolute;
		inset: 0;
		border-radius: inherit;
		background: linear-gradient(
			var(--lg-a2),
			transparent 0%,
			rgba(255, 255, 255, 0.18) 22% 28%,
			transparent 50%
		);
		transition: --lg-a2 0.4s cubic-bezier(0.25, 1, 0.5, 1);
		pointer-events: none;
		z-index: 5;
	}

	.lg-root:hover::before {
		--lg-a2: -15deg;
	}

	/* ::after = conic-gradient 輪郭線 */
	.lg-root::after {
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
		transition: --lg-a1 0.5s ease;
		pointer-events: none;
		z-index: 6;
	}

	.lg-root:hover::after {
		--lg-a1: -125deg;
	}

	/* ── LIGHT ── */
	.lg-light {
		-webkit-tap-highlight-color: rgba(0, 0, 0, 0);
	}

	.lg-light .lg-overlay-bg {
		background: linear-gradient(
			-75deg,
			rgba(255, 255, 255, 0.18),
			rgba(255, 255, 255, 0.32),
			rgba(255, 255, 255, 0.18)
		);
	}

	/* feDisplacementMap 非サポート時（Safari/Firefox）は少し強化 */
	@supports not (backdrop-filter: url(#x)) {
		.lg-light .lg-overlay-bg {
			background: linear-gradient(
				-75deg,
				rgba(255, 255, 255, 0.28),
				rgba(255, 255, 255, 0.46),
				rgba(255, 255, 255, 0.28)
			);
		}
	}

	.lg-light {
		box-shadow:
			inset 0 1px 1px rgba(255, 255, 255, 0.6),
			inset 0 -1px 1px rgba(0, 0, 0, 0.04),
			0 4px 20px -2px rgba(0, 0, 0, 0.08);
	}

	.lg-light::before {
		background: linear-gradient(
			var(--lg-a2),
			transparent 0%,
			rgba(255, 255, 255, 0.25) 20% 30%,
			transparent 55%
		);
	}

	.lg-light::after {
		background:
			conic-gradient(
				from var(--lg-a1) at 50% 50%,
				rgba(0, 0, 0, 0.12),
				transparent 5% 40%,
				rgba(0, 0, 0, 0.12) 50%,
				transparent 60% 95%,
				rgba(0, 0, 0, 0.12)
			),
			linear-gradient(180deg, rgba(255, 255, 255, 0.3), rgba(255, 255, 255, 0.2));
	}

	/* ── DARK ── */
	.lg-dark {
		-webkit-tap-highlight-color: rgba(254, 254, 254, 0);
	}

	.lg-dark .lg-overlay-bg {
		background: linear-gradient(
			-75deg,
			rgba(15, 15, 20, 0.3),
			rgba(20, 20, 28, 0.45),
			rgba(15, 15, 20, 0.3)
		);
	}

	/* feDisplacementMap 非サポート時（Safari/Firefox）は少し強化 */
	@supports not (backdrop-filter: url(#x)) {
		.lg-dark .lg-overlay-bg {
			background: linear-gradient(
				-75deg,
				rgba(10, 10, 15, 0.5),
				rgba(15, 15, 22, 0.65),
				rgba(10, 10, 15, 0.5)
			);
		}
	}

	.lg-dark {
		box-shadow:
			inset 0 1px 1px rgba(255, 255, 255, 0.12),
			inset 0 -1px 1px rgba(0, 0, 0, 0.4),
			0 8px 32px -4px rgba(0, 0, 0, 0.5);
	}

	.lg-dark::before {
		background: linear-gradient(
			var(--lg-a2),
			transparent 0%,
			rgba(255, 255, 255, 0.04) 20% 30%,
			transparent 55%
		);
	}

	.lg-dark::after {
		background:
			conic-gradient(
				from var(--lg-a1) at 50% 50%,
				rgba(255, 255, 255, 0.08),
				transparent 5% 40%,
				rgba(255, 255, 255, 0.08) 50%,
				transparent 60% 95%,
				rgba(255, 255, 255, 0.08)
			),
			linear-gradient(180deg, rgba(0, 0, 0, 0.3), rgba(0, 0, 0, 0.2));
	}

	/* ── BAR variant 専用（ボトムナビ等） ── */
	/* ライト: 白い乳白ガラス感を強化。グレーにならないよう pure white ベース */
	.lg-light[data-variant='bar'] .lg-overlay-bg {
		background: linear-gradient(
			-75deg,
			rgba(255, 255, 255, 0.35),
			rgba(255, 255, 255, 0.52),
			rgba(255, 255, 255, 0.35)
		);
	}
	/* ダーク: 深みのある半透明。pure black ベースでグレーにならない */
	.lg-dark[data-variant='bar'] .lg-overlay-bg {
		background: linear-gradient(
			-75deg,
			rgba(8, 8, 12, 0.55),
			rgba(12, 12, 18, 0.68),
			rgba(8, 8, 12, 0.55)
		);
	}
	/* bar は常に強めの box-shadow で浮遊感を強調 */
	.lg-light[data-variant='bar'] {
		box-shadow:
			inset 0 1px 0 rgba(255, 255, 255, 0.9),
			inset 0 -1px 0 rgba(0, 0, 0, 0.06),
			0 8px 32px -4px rgba(0, 0, 0, 0.18),
			0 2px 8px -1px rgba(0, 0, 0, 0.1);
	}
	.lg-dark[data-variant='bar'] {
		box-shadow:
			inset 0 1px 0 rgba(255, 255, 255, 0.18),
			inset 0 -1px 0 rgba(0, 0, 0, 0.5),
			0 8px 32px -4px rgba(0, 0, 0, 0.6),
			0 2px 8px -1px rgba(0, 0, 0, 0.4);
	}

	/* ── Solid (reduced-transparency) ── */
	.lg-solid.lg-light {
		background: linear-gradient(
			-75deg,
			rgba(255, 255, 255, 0.15),
			rgba(255, 255, 255, 0.35),
			rgba(255, 255, 255, 0.15)
		);
		box-shadow:
			inset 0 1px 2px rgba(255, 255, 255, 0.8),
			0 4px 16px rgba(0, 0, 0, 0.1);
	}

	.lg-solid.lg-dark {
		background: linear-gradient(
			-75deg,
			rgba(30, 30, 30, 0.9),
			rgba(40, 40, 40, 0.95),
			rgba(30, 30, 30, 0.9)
		);
		box-shadow:
			inset 0 1px 2px rgba(255, 255, 255, 0.08),
			0 8px 32px rgba(0, 0, 0, 0.5);
	}

	/* ── ELEVATION ── */
	.lg-light[style*='--lg-elv:sm'] {
		box-shadow:
			inset 0 1px 1px rgba(255, 255, 255, 0.6),
			inset 0 -1px 1px rgba(0, 0, 0, 0.04),
			0 2px 8px -1px rgba(0, 0, 0, 0.06);
	}

	.lg-light[style*='--lg-elv:lg'] {
		box-shadow:
			inset 0 1px 1px rgba(255, 255, 255, 0.7),
			inset 0 -1px 1px rgba(0, 0, 0, 0.06),
			0 12px 40px -6px rgba(0, 0, 0, 0.15);
	}

	.lg-light[style*='--lg-elv:xl'] {
		box-shadow:
			inset 0 1px 1px rgba(255, 255, 255, 0.7),
			inset 0 -1px 1px rgba(0, 0, 0, 0.06),
			0 24px 64px -12px rgba(0, 0, 0, 0.22);
	}

	.lg-dark[style*='--lg-elv:sm'] {
		box-shadow:
			inset 0 1px 1px rgba(255, 255, 255, 0.12),
			inset 0 -1px 1px rgba(0, 0, 0, 0.4),
			0 4px 16px -2px rgba(0, 0, 0, 0.3);
	}

	.lg-dark[style*='--lg-elv:lg'] {
		box-shadow:
			inset 0 1px 1px rgba(255, 255, 255, 0.15),
			inset 0 -1px 1px rgba(0, 0, 0, 0.45),
			0 16px 48px -8px rgba(0, 0, 0, 0.6);
	}

	.lg-dark[style*='--lg-elv:xl'] {
		box-shadow:
			inset 0 1px 1px rgba(255, 255, 255, 0.15),
			inset 0 -1px 1px rgba(0, 0, 0, 0.45),
			0 32px 80px -16px rgba(0, 0, 0, 0.7);
	}

	/* ── Glow ── */
	.lg-glow {
		background: radial-gradient(ellipse at 50% 0%, rgba(255, 255, 255, 0.15) 0%, transparent 70%);
		animation: lg-glow-pulse 3s ease-in-out infinite;
	}

	@keyframes lg-glow-pulse {
		0%,
		100% {
			opacity: 0.6;
		}
		50% {
			opacity: 1;
		}
	}

	/* ── Animate In ── */
	.lg-in {
		animation: lg-in 0.3s cubic-bezier(0.16, 1, 0.3, 1) both;
	}

	@keyframes lg-in {
		0% {
			opacity: 0;
		}
		60% {
			opacity: 1;
		}
		to {
			opacity: 1;
		}
	}

	@media (prefers-reduced-motion: reduce) {
		.lg-in,
		.lg-glow,
		.lg-root::before,
		.lg-root::after {
			animation: none !important;
			transition: none !important;
		}

		.lg-glow {
			opacity: 0;
		}
	}
</style>
