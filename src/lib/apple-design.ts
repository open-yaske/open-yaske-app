/**
 * src/lib/apple-design.ts
 * Apple Design Guidelines — 後方互換エイリアス。
 *
 * 実体は src/lib/styles/apple.ts に移動しました。
 * 新しいコードは `styleStore`（src/lib/stores/style.svelte.ts）経由で
 * 現在のビジュアルスタイルを参照してください。
 *
 * 移行:
 *   Before: import { APPLE_PRESS } from '$lib/apple-design';
 *   After:  import { styleStore } from '$lib/stores';
 *           styleStore.pack.press.button
 */
import { applePack } from '$lib/styles/apple';

// ── §4  Springs ──
export const APPLE_SPRING = applePack.spring;

// ── §4  Easing ──
export const APPLE_EASING = applePack.easing;

// ── §1  Press feedback ──
export const APPLE_PRESS = applePack.press;

// ── §12  Materials ──
export const APPLE_MATERIALS = applePack.materials;

// ── §15  Typography ──
export const APPLE_TYPOGRAPHY = applePack.typography;

// ── §12  Shadow — raw CSS ──
export const APPLE_SHADOW = applePack.shadow;

// ── §11  will-change ──
export const WILL_CHANGE = applePack.willChange;

// ── §14  Reduced motion — アクセシビリティユーティリティ ──

export function prefersReducedMotion(): boolean {
	if (typeof window === 'undefined') return false;
	return window.matchMedia('(prefers-reduced-motion: reduce)').matches;
}

export function prefersReducedTransparency(): boolean {
	if (typeof window === 'undefined') return false;
	return window.matchMedia('(prefers-reduced-transparency: reduce)').matches;
}

export function prefersHighContrast(): boolean {
	if (typeof window === 'undefined') return false;
	return window.matchMedia('(prefers-contrast: more)').matches;
}

export const REDUCED_MOTION_TRANSITION = {
	normalDuration: 200,
	normal: { duration: 300, opacity: 0.9 },
	reduced: { duration: 0, opacity: 0 }
} as const;

// ── §6, §9  Momentum & rubber-banding ──

export function rubberBand(overshoot: number, dimension: number, constant = 0.55): number {
	return (overshoot * dimension * constant) / (dimension + constant * Math.abs(overshoot));
}

export function projectMomentum(initialVelocity: number, decelerationRate = 0.998): number {
	return ((initialVelocity / 1000) * decelerationRate) / (1 - decelerationRate);
}

export function handoffVelocity(gestureVelocity: number, remainingDistance: number): number {
	if (remainingDistance === 0) return 0;
	return gestureVelocity / Math.abs(remainingDistance);
}

export function nearestSnapPoint(projectedEndpoint: number, snapPoints: number[]): number {
	if (snapPoints.length === 0) return projectedEndpoint;
	return snapPoints.reduce((prev, curr) =>
		Math.abs(curr - projectedEndpoint) < Math.abs(prev - projectedEndpoint) ? curr : prev
	);
}

// ── §16  Design principles (ドキュメント用) ──
export const APPLE_PRINCIPLES = {
	purpose: 'Purpose — 意図を持って作り、作らないことを決める',
	agency: 'Agency — ユーザーに選択肢を提供し、 Forgiveness（簡単な元に戻し）で支える',
	responsibility: 'Responsibility — ユーザーの利益のために行動し、プライバシーと安全性を予見',
	familiarity: 'Familiarity — 既知のメタファーを活用し、一貫した振る舞いを保つ',
	flexibility: 'Flexibility — コンテキスト、デバイス、能力の全範囲に適応する',
	simplicity: 'Simplicity — 不要なものを取り除き、核心が輝くようにする',
	craft: 'Craft — 細部への妥協なきこだわりが信頼を構築する',
	delight: 'Delight — 上記7つが正しい結果として生まれる喜び'
} as const;
