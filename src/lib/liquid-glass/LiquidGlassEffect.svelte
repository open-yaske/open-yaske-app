<!--
  LiquidGlassEffect.svelte
  nikdelvin/liquid-glass の SVG feDisplacementMap 屈折エフェクトの Svelte 5 実装。

  このコンポーネントは「フィルターレイヤー」として親要素の `absolute inset-0` に配置する。
  親要素は `position: relative; overflow: hidden; isolation: isolate;` が必要。

  仕組み:
  1. ResizeObserver で要素サイズを監視
  2. getDisplacementMap() で要素形状に合わせた SVG マップを生成
  3. getLiquidGlassFilterSvg() でフィルター定義を生成
  4. <body> に SVG <defs> を一度だけ注入（filterId で管理）
  5. このレイヤー自体に backdrop-filter: url(#filterId) を適用

  Safari フォールバック:
  - backdrop-filter: url() 非サポート時は glassmorphism (backdrop-blur) のみ
-->
<script lang="ts">
	import { onMount } from 'svelte';
	import {
		getDisplacementMap,
		getLiquidGlassFilterSvg,
		getComputedRadius
	} from '$lib/liquid-glass/displacement';

	interface Props {
		/** フィルター一意 ID（複数インスタンス共存のため必須） */
		filterId: string;
		/** 屈折強度 (0–200, default: 100) */
		strength?: number;
		/** 屈折深度 (default: 10) */
		depth?: number;
		/** クロマティックアベレーション (0–5, default: 1) */
		chromaticAberration?: number;
		/** ブラー量 px (default: 0) */
		blur?: number;
		/** 彩度倍率 (default: 1.5) */
		saturate?: number;
		/** 輝度倍率 (default: 1.1) */
		brightness?: number;
		/** フォールバック用 backdrop-blur px (default: 16) */
		fallbackBlur?: number;
		/** フィルター無効化 */
		disabled?: boolean;
	}

	let {
		filterId,
		strength = 100,
		depth = 10,
		chromaticAberration = 1,
		blur = 0,
		saturate = 1.5,
		brightness = 1.1,
		fallbackBlur = 16,
		disabled = false
	}: Props = $props();

	let layerEl = $state<HTMLDivElement | null>(null);
	let supportsUrl = $state(false);
	let filterStyle = $state('');
	let defsEl: SVGSVGElement | null = null;

	/** backdrop-filter: url() のサポート検出（一度だけ） */
	function checkSupport(): boolean {
		if (typeof CSS === 'undefined') return false;
		try {
			return CSS.supports('backdrop-filter', 'url(#test)');
		} catch {
			return false;
		}
	}

	/** SVG <defs> を <body> に注入または更新する */
	function injectOrUpdateDefs(svgContent: string) {
		if (typeof document === 'undefined') return;

		const containerId = `lg-defs-${filterId}`;
		let container = document.getElementById(containerId) as SVGSVGElement | null;

		if (!container) {
			const parser = new DOMParser();
			const doc = parser.parseFromString(
				`<svg xmlns="http://www.w3.org/2000/svg" id="${containerId}" width="0" height="0" style="position:absolute;width:0;height:0;overflow:hidden;pointer-events:none">${svgContent}</svg>`,
				'image/svg+xml'
			);
			const newEl = doc.documentElement as unknown as SVGSVGElement;
			document.body.appendChild(newEl);
			defsEl = newEl;
		} else {
			// 既存の filter のみ差し替え
			const parser = new DOMParser();
			const doc = parser.parseFromString(
				`<svg xmlns="http://www.w3.org/2000/svg">${svgContent}</svg>`,
				'image/svg+xml'
			);
			const newFilter = doc.querySelector('filter');
			const oldFilter = container.querySelector(`#${filterId}`);
			if (newFilter && oldFilter) {
				oldFilter.replaceWith(newFilter);
			} else if (newFilter) {
				let defs = container.querySelector('defs');
				if (!defs) {
					defs = document.createElementNS('http://www.w3.org/2000/svg', 'defs');
					container.appendChild(defs);
				}
				defs.appendChild(newFilter);
			}
			defsEl = container;
		}
	}

	/** フィルターを再計算して適用する */
	function applyFilter(el: HTMLDivElement) {
		if (disabled || !supportsUrl) {
			// フォールバック: blur + saturate + brightness で glassmorphism 品質を確保
			const sat = Math.max(1.2, saturate);
			const br = Math.min(1.05, brightness);
			const filterVal = `blur(${fallbackBlur}px) saturate(${sat}) brightness(${br})`;
			filterStyle = `backdrop-filter: ${filterVal}; -webkit-backdrop-filter: ${filterVal};`;
			return;
		}

		const rect = el.getBoundingClientRect();
		const w = Math.round(rect.width);
		const h = Math.round(rect.height);
		if (w === 0 || h === 0) return;

		// 親要素の border-radius を取得
		const parentEl = el.parentElement;
		const radius = parentEl ? getComputedRadius(parentEl) : 0;

		// displacement map を生成
		const dmapUrl = getDisplacementMap({ width: w, height: h, radius, depth });

		// SVG フィルター定義を生成・注入
		const filterSvg = getLiquidGlassFilterSvg({
			id: filterId,
			displacementMapUrl: dmapUrl,
			strength,
			chromaticAberration,
			blur,
			saturate,
			brightness
		});

		const defsContent = `<defs>${filterSvg}</defs>`;
		injectOrUpdateDefs(defsContent);

		filterStyle = `backdrop-filter: url(#${filterId}); -webkit-backdrop-filter: url(#${filterId});`;
	}

	let observer: ResizeObserver | null = null;

	onMount(() => {
		supportsUrl = checkSupport();

		if (layerEl) {
			applyFilter(layerEl);

			observer = new ResizeObserver(() => {
				if (layerEl) applyFilter(layerEl);
			});
			observer.observe(layerEl);
		}

		return () => {
			observer?.disconnect();
			// クリーンアップ: <body> から SVG defs を削除
			if (typeof document !== 'undefined') {
				const el = document.getElementById(`lg-defs-${filterId}`);
				el?.remove();
			}
		};
	});

	// strength/depth 等が動的に変わった場合も再計算
	$effect(() => {
		// 依存値を参照してリアクティブに
		void strength;
		void depth;
		void chromaticAberration;
		void blur;
		void saturate;
		void brightness;
		void disabled;

		if (layerEl && supportsUrl) {
			applyFilter(layerEl);
		} else if (layerEl) {
			filterStyle = `backdrop-filter: blur(${fallbackBlur}px); -webkit-backdrop-filter: blur(${fallbackBlur}px);`;
		}
	});
</script>

<!--
  このレイヤーが親要素の背後コンテンツを参照して歪める。
  z-index: 2 として overlay-bg (z:1) の上、content (z:3) の下に配置。
-->
<div
	bind:this={layerEl}
	class="lg-effect-layer absolute inset-0 z-[2] pointer-events-none"
	style={filterStyle}
	aria-hidden="true"
></div>
