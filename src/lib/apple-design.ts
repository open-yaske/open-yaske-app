/**
 * src/lib/apple-design.ts
 * Apple Design Guidelines (SKILL.md §1-§17) 準拠の中央デザインシステム定義
 *
 * イージング・スプリング・レスポンス・物理フィードバック・マテリアル・シャドウを一元管理。
 */

export const APPLE_EASING = {
	/** Apple 標準スプリング風イージング (応答が早く、着地が滑らか) */
	default: 'cubic-bezier(0.16, 1, 0.3, 1)',
	/** Apple 流動シート / ドロワー用イージング */
	sheet: 'cubic-bezier(0.32, 0.72, 0, 1)',
	/** 物理的勢い・バウンスイージング */
	bounce: 'cubic-bezier(0.34, 1.56, 0.64, 1)',
	/** 高速減速イージング */
	decelerate: 'cubic-bezier(0, 0, 0.2, 1)'
} as const;

export const APPLE_PRESS = {
	/** 標準ボタン押し込みフィードバック */
	button: 'active:scale-[0.97] transition-all duration-150 ease-[cubic-bezier(0.16,1,0.3,1)]',
	/** アイコンボタン押し込みフィードバック */
	icon: 'active:scale-90 transition-all duration-150 ease-[cubic-bezier(0.16,1,0.3,1)]',
	/** カード・リストアイテム押し込みフィードバック */
	card: 'active:scale-[0.985] transition-all duration-200 ease-[cubic-bezier(0.16,1,0.3,1)]',
	/** ナビゲーションタブ・ピル押し込みフィードバック */
	tab: 'active:scale-95 transition-transform duration-150 ease-[cubic-bezier(0.16,1,0.3,1)]'
} as const;

export const APPLE_MATERIALS = {
	/** iOS 18 / macOS Sequoia すりガラス Vibrancy マテリアル */
	glass:
		'backdrop-blur-2xl bg-[var(--color-surface-card)]/80 border border-[var(--color-surface-border)]/60',
	/** ヘッダー/バー用高透過ガラス */
	glassHeader:
		'backdrop-blur-3xl bg-[var(--color-surface-page)]/80 border-b border-[var(--color-surface-border)]/50',
	/** モーダル/シート用深みすりガラス */
	glassModal:
		'backdrop-blur-3xl bg-[var(--color-surface-card)]/90 border border-[var(--color-surface-border)]/80 shadow-apple-xl',
	/** iOS 純正 Bento Tile / コンテンツカード */
	card: 'bg-[var(--color-surface-card)]/80 backdrop-blur-2xl border border-[var(--color-surface-border)]/60 rounded-card shadow-apple-sm hover:shadow-apple-md transition-all duration-200',
	/** インタラクティブカード（ホバー＋プレス反応付き） */
	interactiveCard:
		'bg-[var(--color-surface-card)]/80 backdrop-blur-2xl border border-[var(--color-surface-border)]/60 rounded-card shadow-apple-sm hover:shadow-apple-md hover:border-[var(--color-primary-500)]/40 active:scale-[0.985] transition-all duration-200 cursor-pointer select-none'
} as const;

/**
 * Apple Design §15 — Typography
 * Size-specific tracking (letter-spacing) と leading (line-height) の設定。
 * 大きいテキストはネガティブ tracking、小さいテキストはゼロ付近。
 */
export const APPLE_TYPOGRAPHY = {
	/** 大見出し表示テキスト: tight leading + negative tracking */
	display: 'leading-[1.05] tracking-[-0.02em] font-optical-sizing-auto',
	/** セクション見出し */
	heading: 'leading-tight tracking-tight',
	/** 標準ボディテキスト: comfortable leading */
	body: 'leading-relaxed tracking-normal',
	/** 小さい UI ラベル: slightly positive tracking for legibility */
	label: 'leading-snug tracking-wide',
	/** 数値表示: tabular figures で揃える */
	numeric: '[font-variant-numeric:tabular-nums] [font-feature-settings:"tnum"]'
} as const;

/**
 * Apple Design §9 — Rubber-banding
 * エッジでの弾性抵抗計算。
 * ハードストップではなく、越えるほど抵抗が増す自然な感触を実現。
 *
 * @param overshoot - 境界を越えた距離 (px)
 * @param dimension - コンテナのサイズ (px)
 * @param constant - 抵抗係数 (デフォルト 0.55 = Apple 標準)
 * @returns 適用すべき実際の変位量 (px)
 */
export function rubberBand(overshoot: number, dimension: number, constant = 0.55): number {
	return (overshoot * dimension * constant) / (dimension + constant * Math.abs(overshoot));
}

/**
 * Apple Design §6 — Momentum Projection
 * ジェスチャー速度から「指が向かっている先」を予測。
 * スクロール減速と同じ指数減衰モデル（Apple 公式）。
 *
 * @param initialVelocity - 離れた瞬間の速度 (px/s)
 * @param decelerationRate - 減速係数 (0.998 = 標準スクロール感, 0.99 = スナッピー)
 * @returns 予測される到達距離 (px)
 */
export function projectMomentum(initialVelocity: number, decelerationRate = 0.998): number {
	return ((initialVelocity / 1000) * decelerationRate) / (1 - decelerationRate);
}
