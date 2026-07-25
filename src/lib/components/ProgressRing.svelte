<!--
  ProgressRing.svelte
  SVG 円形プログレスインジケーター。
  stroke-dasharray で円弧を描画し、value (0-100) に応じて進捗を表示。

  Apple Design 準拠:
  - §4  Springs: stroke transition を Apple イージングに
  - §14 Reduced motion: transition を無効化
  - §16 Accessibility: aria-valuenow/min/max/label 追加
  - §15 Typography: 数値に tabular-nums 適用
-->
<script lang="ts">
	interface Props {
		/** 進捗値 (0-100) */
		value: number;
		/** SVG のサイズ (px) */
		size?: number;
		/** ストローク幅 (px) */
		strokeWidth?: number;
		/** 進捗部分の色 (CSS 変数名、例: --color-primary-500) */
		color?: string;
		/** 背景トラックの色 (CSS 変数名) */
		trackColor?: string;
		/** 中央に表示するスニペット */
		children?: import('svelte').Snippet;
		/** 追加クラス */
		class?: string;
		/** アクセシビリティ用ラベル */
		ariaLabel?: string;
	}

	let {
		value,
		size = 64,
		strokeWidth = 6,
		color = '--color-primary-500',
		trackColor = '--color-surface-border',
		children,
		class: klass = '',
		ariaLabel
	}: Props = $props();

	// 0-100 にクランプ
	const clampedValue = $derived(Math.max(0, Math.min(100, value)));

	// SVG ジオメトリ計算
	const radius = $derived((size - strokeWidth) / 2);
	const circumference = $derived(2 * Math.PI * radius);
	const dashOffset = $derived(circumference * (1 - clampedValue / 100));
	const center = $derived(size / 2);
</script>

<div
	class="relative inline-flex items-center justify-center {klass}"
	style="width: {size}px; height: {size}px;"
	role="meter"
	aria-valuenow={clampedValue}
	aria-valuemin={0}
	aria-valuemax={100}
	aria-label={ariaLabel}
>
	<!-- §12 背景ガラス円 -->
	<svg width={size} height={size} class="absolute inset-0" aria-hidden="true">
		<defs>
			<filter id="ring-glass-bg">
				<feGaussianBlur in="SourceGraphic" stdDeviation="2" />
			</filter>
		</defs>
		<circle
			cx={center}
			cy={center}
			r={radius + strokeWidth / 2}
			fill="color-mix(in srgb, var({trackColor}) 30%, transparent)"
			class="backdrop-blur-sm"
		/>
	</svg>
	<svg width={size} height={size} class="-rotate-90 relative z-10" aria-hidden="true">
		<circle
			cx={center}
			cy={center}
			r={radius}
			fill="none"
			stroke="var({trackColor})"
			stroke-width={strokeWidth}
		/>
		<circle
			cx={center}
			cy={center}
			r={radius}
			fill="none"
			stroke="var({color})"
			stroke-width={strokeWidth}
			stroke-linecap="round"
			stroke-dasharray={circumference}
			stroke-dashoffset={dashOffset}
			style="transition: stroke-dashoffset 0.3s cubic-bezier(0.16, 1, 0.3, 1);"
		/>
	</svg>
	{#if children}
		<div
			class="absolute inset-0 flex items-center justify-center [font-variant-numeric:tabular-nums]"
		>
			{@render children()}
		</div>
	{/if}
</div>
