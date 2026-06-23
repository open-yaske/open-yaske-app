<!--
  ProgressRing.svelte
  SVG 円形プログレスインジケーター。
  stroke-dasharray で円弧を描画し、value (0-100) に応じて進捗を表示。
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
	}

	let {
		value,
		size = 64,
		strokeWidth = 6,
		color = '--color-primary-500',
		trackColor = '--color-surface-border',
		children,
		class: klass = ''
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
>
	<svg width={size} height={size} class="-rotate-90">
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
			style="transition: stroke-dashoffset 0.3s ease;"
		/>
	</svg>
	{#if children}
		<div class="absolute inset-0 flex items-center justify-center">
			{@render children()}
		</div>
	{/if}
</div>
