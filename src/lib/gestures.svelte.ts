/**
 * src/lib/gestures.svelte.ts
 * §1 Direct manipulation / §5 Velocity handoff / §6 Momentum / §9 Rubber-banding
 * §10 Gesture design details
 *
 * ポインタイベントによる1:1トラッキング、速度計測、モメンタム投影、
 * ラバーバンディングのための Svelte 5 ユーティリティ。
 */

import { rubberBand, projectMomentum, nearestSnapPoint } from '$lib/apple-design';

/**
 * §2 §10 — ポインタ速度の記録。
 * 直近 N フレームの位置・時刻を保持し、release 時の速度を算出する。
 */
export interface VelocitySample {
	x: number;
	y: number;
	t: number;
}

/**
 * §2 §10 — ポインタのトラッキング履歴（直近 5 フレーム）。
 * `recordSample()` で記録、`getVelocity()` でrelease時の速度を取得。
 */
export class VelocityTracker {
	private samples: VelocitySample[] = [];
	private maxSamples = 5;

	/** 最新のサンプルを追加 */
	recordSample(x: number, y: number): void {
		const now = performance.now();
		this.samples.push({ x, y, t: now });
		if (this.samples.length > this.maxSamples) {
			this.samples.shift();
		}
	}

	/**
	 * §5 — 直近サンプルからrelease時の速度 (px/s) を算出。
	 * 最新2点の差分で計算（重み付き平均の簡易版）。
	 */
	getVelocity(): { vx: number; vy: number } {
		if (this.samples.length < 2) return { vx: 0, vy: 0 };
		const first = this.samples[0];
		const last = this.samples[this.samples.length - 1];
		const dt = last.t - first.t;
		if (dt === 0) return { vx: 0, vy: 0 };
		return {
			vx: ((last.x - first.x) / dt) * 1000,
			vy: ((last.y - first.y) / dt) * 1000
		};
	}

	/** 履歴をクリア */
	reset(): void {
		this.samples = [];
	}
}

/**
 * §2 §10 — ポインタトラッキング用のイベントハンドラを生成。
 * `setPointerCapture` でポインタが要素外に出ても追跡を継続する。
 *
 * @param element - トラッキング対象の DOM 要素
 * @param options - コールバック設定
 */
export function createPointerTracker(
	element: HTMLElement,
	options: {
		/** ドラッグ開始時 */
		onDragStart?: (x: number, y: number, grabOffset: { x: number; y: number }) => void;
		/** ドラッグ中の1:1トラッキング */
		onDragMove?: (x: number, y: number, deltaX: number, deltaY: number) => void;
		/** ドラッグ終了時（速度付き） */
		onDragEnd?: (velocity: { vx: number; vy: number }, finalX: number, finalY: number) => void;
		/** キャンセル時（方向が確定しなかった場合） */
		onDragCancel?: () => void;
		/** §10 方向決定のヒステリシス (px) — デフォルト 10px */
		hysteresis?: number;
	}
): () => void {
	const tracker = new VelocityTracker();
	const hysteresis = options.hysteresis ?? 10;

	let isTracking = false;
	let grabOffset = { x: 0, y: 0 };
	let startX = 0;
	let startY = 0;
	let directionDecided = false;

	function handlePointerDown(e: PointerEvent) {
		// 左ボタンのみ
		if (e.button !== 0) return;

		const rect = element.getBoundingClientRect();
		grabOffset = {
			x: e.clientX - rect.left,
			y: e.clientY - rect.top
		};
		startX = e.clientX;
		startY = e.clientY;
		isTracking = true;
		directionDecided = false;
		tracker.reset();
		tracker.recordSample(e.clientX, e.clientY);

		// §2 setPointerCapture: ポインタが要素外に出ていてもトラッキング継続
		element.setPointerCapture(e.pointerId);

		options.onDragStart?.(e.clientX, e.clientY, grabOffset);
	}

	function handlePointerMove(e: PointerEvent) {
		if (!isTracking) return;

		tracker.recordSample(e.clientX, e.clientY);

		const deltaX = e.clientX - startX;
		const deltaY = e.clientY - startY;

		// §10 ヒステリシス: 10px 以上動いたら方向確定
		if (!directionDecided) {
			const dist = Math.sqrt(deltaX * deltaX + deltaY * deltaY);
			if (dist < hysteresis) return;
			directionDecided = true;
		}

		options.onDragMove?.(e.clientX, e.clientY, deltaX, deltaY);
	}

	function handlePointerUp(e: PointerEvent) {
		if (!isTracking) return;
		isTracking = false;
		tracker.recordSample(e.clientX, e.clientY);

		const velocity = tracker.getVelocity();
		options.onDragEnd?.(velocity, e.clientX, e.clientY);
	}

	function handlePointerCancel() {
		if (!isTracking) return;
		isTracking = false;
		options.onDragCancel?.();
	}

	element.addEventListener('pointerdown', handlePointerDown);
	element.addEventListener('pointermove', handlePointerMove);
	element.addEventListener('pointerup', handlePointerUp);
	element.addEventListener('pointercancel', handlePointerCancel);

	// クリーンアップ関数を返す
	return () => {
		element.removeEventListener('pointerdown', handlePointerDown);
		element.removeEventListener('pointermove', handlePointerMove);
		element.removeEventListener('pointerup', handlePointerUp);
		element.removeEventListener('pointercancel', handlePointerCancel);
	};
}

/**
 * §9 — ラバーバンディングの変位を計算。
 * DOM 要素のバウンダリで越えた量を橡皮筋のように戻す。
 *
 * @param delta - 現在のドラッグ変位 (px)
 * @param containerSize - コンテナのサイズ (px)
 * @returns ラバーバンディング適用後の変位 (px)
 */
export function calculateRubberBand(delta: number, containerSize: number): number {
	if (Math.abs(delta) <= 0) return delta;
	// 正方向・負方向を区別して rubberBand を適用
	const sign = delta > 0 ? 1 : -1;
	return sign * rubberBand(Math.abs(delta), containerSize);
}

/**
 * §6 — ドラッグ終了時のターゲット計算。
 * 速度を投影し、最も近いスナップポイントを選択。
 *
 * @param currentPosition - 現在位置 (px)
 * @param velocity - release時の速度 (px/s)
 * @param snapPoints - スナップポイントの配列 (昇順)
 * @param decelerationRate - 減速係数 (デフォルト 0.998)
 * @returns アニメーション先のターゲット位置 (px)
 */
export function calculateSnapTarget(
	currentPosition: number,
	velocity: { vx: number; vy: number },
	snapPoints: number[],
	decelerationRate = 0.998
): number {
	// X 方向の投影を計算（水平スナップが一般的）
	const projectedX = currentPosition + projectMomentum(velocity.vx, decelerationRate);

	// 水平スナップ（通常は左右境界）
	return nearestSnapPoint(snapPoints.length > 0 ? projectedX : currentPosition, snapPoints);
}
