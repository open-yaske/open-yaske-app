/**
 * src/lib/reduced-motion.svelte.ts
 * §14 Reduced motion & accessibility — Svelte 5 リアクティブユーティリティ
 *
 * 3 つの独立したメディアクエリを監視し、UI のフィードバック方式を切り替える:
 * - prefers-reduced-motion: reduce  → slide/spring/parallax を cross-fade に
 * - prefers-reduced-transparency: reduce → すりガラス → ソリッド背景に
 * - prefers-contrast: more → 透過度を下げ、ボーダーを明確に
 */
import { writable } from 'svelte/store';

/**
 * `prefers-reduced-motion: reduce` のリアクティブな値。
 * - `true` の場合: スライド/スプリング/パララクスを避けて opacity cross-fade のみ使用
 * - `false` の場合: 通常のアニメーションを有効化
 */
export const reducedMotion = writable(false);

/**
 * `prefers-reduced-transparency: reduce` のリアクティブな値。
 * - `true` の場合: backdrop-filter を無効化し、ソリッド背景にフォールバック
 * - `false` の場合: すりガラス効果を有効化
 */
export const reducedTransparency = writable(false);

/**
 * `prefers-contrast: more` のリアクティブな値。
 * - `true` の場合: 近乎不透過の背景 + 明確なボーダーを使用
 * - `false` の場合: 通常のコントラスト
 */
export const highContrast = writable(false);

let initialized = false;

/**
 * ブラウザ環境でメディアクエリを初期化する。
 * SSR では何もしない。二重初期化を防止。
 */
export function initReducedMotion(): void {
	if (initialized || typeof window === 'undefined') return;
	initialized = true;

	const motionMq = window.matchMedia('(prefers-reduced-motion: reduce)');
	const transparencyMq = window.matchMedia('(prefers-reduced-transparency: reduce)');
	const contrastMq = window.matchMedia('(prefers-contrast: more)');

	// 初期値をセット
	reducedMotion.set(motionMq.matches);
	reducedTransparency.set(transparencyMq.matches);
	highContrast.set(contrastMq.matches);

	// 変更を監視
	motionMq.addEventListener('change', (e) => reducedMotion.set(e.matches));
	transparencyMq.addEventListener('change', (e) => reducedTransparency.set(e.matches));
	contrastMq.addEventListener('change', (e) => highContrast.set(e.matches));
}

/**
 * §14 — reduced-motion 時に安全なアニメーションパラメータを返す。
 *
 * @param normal - 通常時のパラメータ { duration, y?, x?, opacity? }
 * @returns reduced-motion 時は { duration: 0, opacity: 0 } に変換
 */
export function safeAnimation<T extends { duration: number; opacity?: number }>(normal: T): T {
	if (!reducedMotionCurrent()) {
		return normal;
	}
	return { ...normal, duration: 0, opacity: 0 } as T;
}

/**
 * §14 — reduced-motion の初期値を同期的に取得する（初期化済みの場合のみ）。
 * `$effect` 内や初期値計算で store の `.subscribe` が使えない場合に使用。
 */
function reducedMotionCurrent(): boolean {
	if (typeof window === 'undefined') return false;
	return window.matchMedia('(prefers-reduced-motion: reduce)').matches;
}
