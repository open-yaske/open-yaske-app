/**
 * src/lib/apple-design.ts
 * Apple Design Guidelines (SKILL.md §1-§17) 準拠の中央デザインシステム定義
 *
 * §1  Response — kill latency (即時フィードバック)
 * §2  Direct manipulation — 1:1 tracking
 * §3  Interruptibility — 割り込み可能なアニメーション
 * §4  Behavior over animation — use springs
 * §5  Velocity handoff — ドラッグ→アニメーション速度継承
 * §6  Momentum projection — 指の先を予測してアニメーション
 * §7  Spatial consistency — 対称パス・アンカードオリジン
 * §8  Hint in the direction of the gesture
 * §9  Rubber-banding — ソフトバウンダリ
 * §10 Gesture design details
 * §11 Frame-level smoothness
 * §12 Materials & depth — translucency conveys hierarchy
 * §13 Multimodal feedback
 * §14 Reduced motion & accessibility
 * §15 Typography — optical sizing, tracking, leading
 * §16 Design foundations — eight principles
 * §17 Process
 *
 * Liquid Glass: danilofiumi/liquid-glass-svelte
 */

// ─────────────────────────────────────────────────────────────
// §4  Springs — Apple の物理スプリング定義
// ─────────────────────────────────────────────────────────────

/**
 * Apple のスプリング定義。
 * damping=1.0 がクリティカルダンピング（バウンスなし、滑らかな着地）。
 * damping<1.0 でオーバーシュート（バウンス）。
 *
 * | Interaction              | Damping | Response |
 * |--------------------------|---------|----------|
 * | Move / reposition (PiP)  | 1.0     | 0.4      |
 * | Rotation                 | 0.8     | 0.4      |
 * | Drawer / sheet           | 0.8     | 0.3      |
 * | Default UI spring        | 1.0     | 0.3–0.4  |
 * | Momentum / flick spring  | ~0.8    | 0.3–0.4  |
 */
export const APPLE_SPRING = {
	/** §4 標準 UI スプリング — クリティカルダンピング、バウンスなし */
	default: { damping: 1.0, response: 0.35 },
	/** §4 ドロワー / ボトムシート — 少しバウンス */
	sheet: { damping: 0.8, response: 0.3 },
	/** §4 モーダル表示 — クリティカルダンピング */
	modal: { damping: 1.0, response: 0.3 },
	/** §4 メニュー / ポップオーバー — 軽いバウンス */
	menu: { damping: 0.85, response: 0.3 },
	/** §4 フリック / モメンタム — バウンスあり（勢いを引き継ぐ） */
	flick: { damping: 0.8, response: 0.35 },
	/** §4 回転 — バウンスあり */
	rotation: { damping: 0.8, response: 0.4 },
	/** §4 ピンチ / ズーム */
	pinch: { damping: 0.85, response: 0.25 }
} as const;

/**
 * §4 — Apple スプリングの CSS フォールバック。
 * `motion` ライブラリ (Framer Motion) でなければ CSS で近似する。
 * `cubic-bezier` はスプリングの定性的な近似。
 */
export const APPLE_EASING = {
	/** Apple 標準スプリング風イージング (応答が早く、着地が滑らか) */
	default: 'cubic-bezier(0.16, 1, 0.3, 1)',
	/** Apple 流動シート / ドロワー用イージング */
	sheet: 'cubic-bezier(0.32, 0.72, 0, 1)',
	/** 物理的勢い・バウンスイージング */
	bounce: 'cubic-bezier(0.34, 1.56, 0.64, 1)',
	/** 高速減速イージング */
	decelerate: 'cubic-bezier(0, 0, 0.2, 1)',
	/** §7 逆方向用 — 対称パス（outbound の逆カーブ） */
	reverse: 'cubic-bezier(0.7, 0, 0.84, 0)'
} as const;

// ─────────────────────────────────────────────────────────────
// §1  Response — 即時フィードバック (pointer-down で即座に)
// §10 Gesture design — プレスフィードバック
// ─────────────────────────────────────────────────────────────

export const APPLE_PRESS = {
	/** §1 標準ボタン — pointer-down 即座にスケール */
	button: 'active:scale-[0.97] transition-all duration-150 ease-[cubic-bezier(0.16,1,0.3,1)]',
	/** §1 アイコンボタン — より大きく縮小 */
	icon: 'active:scale-90 transition-all duration-150 ease-[cubic-bezier(0.16,1,0.3,1)]',
	/** §1 カード・リストアイテム — 微小なスケール */
	card: 'active:scale-[0.985] transition-all duration-200 ease-[cubic-bezier(0.16,1,0.3,1)]',
	/** §1 ナビゲーションタブ・ピル — フローティング */
	tab: 'active:scale-95 transition-transform duration-150 ease-[cubic-bezier(0.16,1,0.3,1)]',
	/** §1 Liquid Glass ボタン — 3D 回転フィードバック */
	liquid: 'transition-all duration-200 ease-[cubic-bezier(0.16,1,0.3,1)]',
	/** §1 スイッチ — タップフィードバック */
	toggle: 'active:scale-95 transition-transform duration-100 ease-[cubic-bezier(0.16,1,0.3,1)]'
} as const;

// ─────────────────────────────────────────────────────────────
// §12 Materials & depth — すりガラス / Liquid Glass マテリアル
// ─────────────────────────────────────────────────────────────

export const APPLE_MATERIALS = {
	/** §12 iOS 18 / macOS Sequoia すりガラス Vibrancy マテリアル */
	glass:
		'backdrop-blur-2xl bg-[var(--color-surface-card)]/80 border border-[var(--color-surface-border)]/60',
	/** §12 ヘッダー/バー用高透過ガラス — コンテンツの下をスクロール */
	glassHeader:
		'backdrop-blur-3xl bg-[var(--color-surface-page)]/80 border-b border-[var(--color-surface-border)]/50',
	/** §12 モーダル/シート用深みすりガラス — 親レイヤーをdim */
	glassModal:
		'backdrop-blur-3xl bg-[var(--color-surface-card)]/90 border border-[var(--color-surface-border)]/80 shadow-apple-xl',
	/** §12 iOS 純正 Bento Tile / コンテンツカード */
	card: 'bg-[var(--color-surface-card)]/80 backdrop-blur-2xl border border-[var(--color-surface-border)]/60 rounded-card shadow-apple-sm hover:shadow-apple-md transition-all duration-200',
	/** §12 インタラクティブカード（ホバー＋プレス反応付き） */
	interactiveCard:
		'bg-[var(--color-surface-card)]/80 backdrop-blur-2xl border border-[var(--color-surface-border)]/60 rounded-card shadow-apple-sm hover:shadow-apple-md hover:border-[var(--color-primary-500)]/40 active:scale-[0.985] transition-all duration-200 cursor-pointer select-none',
	/** §12 Liquid Glass フローティング ドック (iOS 26 style) */
	liquidDock:
		'rounded-full border border-white/20 shadow-[0_8px_32px_rgba(0,0,0,0.12),inset_0_1px_0_rgba(255,255,255,0.4)]',
	/** §12 Liquid Glass ナビゲーションバー */
	liquidNav:
		'border border-white/15 shadow-[0_4px_24px_rgba(0,0,0,0.08),inset_0_0.5px_0_rgba(255,255,255,0.3)]',
	/** §12 Liquid Glass シート背景 — 深いレイヤー分離 */
	liquidSheet:
		'border border-white/20 shadow-[0_-4px_32px_rgba(0,0,0,0.15),inset_0_1px_0_rgba(255,255,255,0.5)]'
} as const;

// ─────────────────────────────────────────────────────────────
// §15 Typography — optical sizing, tracking, leading
// ─────────────────────────────────────────────────────────────

export const APPLE_TYPOGRAPHY = {
	/** §15 大見出し表示テキスト: tight leading + negative tracking */
	display: 'leading-[1.05] tracking-[-0.02em] font-optical-sizing-auto',
	/** §15 セクション見出し */
	heading: 'leading-tight tracking-tight',
	/** §15 標準ボディテキスト: comfortable leading */
	body: 'leading-relaxed tracking-normal',
	/** §15 小さい UI ラベル: slightly positive tracking for legibility */
	label: 'leading-snug tracking-wide',
	/** §15 数値表示: tabular figures で揃える */
	numeric: '[font-variant-numeric:tabular-nums] [font-feature-settings:"tnum"]',
	/** §15 Liquid Glass 上のテキスト — 高コントラスト・太めウェイト */
	liquidText: 'font-semibold tracking-tight drop-shadow-[0_1px_2px_rgba(0,0,0,0.1)]'
} as const;

// ─────────────────────────────────────────────────────────────
// §12 Shadow depth — サイズに応じた影の重み
// ─────────────────────────────────────────────────────────────

export const APPLE_SHADOW = {
	/** 小さいエレメント（チップ、バッジ） */
	sm: '0 1px 3px rgba(0,0,0,0.08), 0 1px 2px rgba(0,0,0,0.06)',
	/** 中程度（カード、リストアイテム） */
	md: '0 4px 12px rgba(0,0,0,0.08), 0 2px 4px rgba(0,0,0,0.04)',
	/** 大きい（モーダル、シート） */
	lg: '0 12px 40px rgba(0,0,0,0.12), 0 4px 12px rgba(0,0,0,0.06)',
	/** 非常に大きい（フローティングパネル） */
	xl: '0 24px 64px rgba(0,0,0,0.16), 0 8px 20px rgba(0,0,0,0.08)',
	/** Liquid Glass 用 — 上辺の光 + 下の影 */
	liquid: '0 8px 32px rgba(0,0,0,0.12), inset 0 1px 0 rgba(255,255,255,0.4)',
	/** Liquid Glass ドック — 浮遊感 */
	liquidDock:
		'0 12px 48px rgba(0,0,0,0.16), 0 4px 16px rgba(0,0,0,0.08), inset 0 1px 0 rgba(255,255,255,0.5)'
} as const;

// ─────────────────────────────────────────────────────────────
// §14 Reduced Motion — メディアクエリ・ユーティリティ
// ─────────────────────────────────────────────────────────────

/**
 * §14 — `prefers-reduced-motion: reduce` 時に true を返す。
 * SSR 安全（`window` がなければ false を返す）。
 */
export function prefersReducedMotion(): boolean {
	if (typeof window === 'undefined') return false;
	return window.matchMedia('(prefers-reduced-motion: reduce)').matches;
}

/**
 * §14 — `prefers-reduced-transparency: reduce` 時に true を返す。
 * すりガラス → ソリッド背景へのフォールバック判定に使用。
 */
export function prefersReducedTransparency(): boolean {
	if (typeof window === 'undefined') return false;
	return window.matchMedia('(prefers-reduced-transparency: reduce)').matches;
}

/**
 * §14 — `prefers-contrast: more` 時に true を返す。
 */
export function prefersHighContrast(): boolean {
	if (typeof window === 'undefined') return false;
	return window.matchMedia('(prefers-contrast: more)').matches;
}

/**
 * §14 — reduced-motion 時のアニメーションパラメータ。
 * slide/spring/parallax を短い opacity cross-fade に置き換える。
 */
export const REDUCED_MOTION_TRANSITION = {
	/** 通常の transition duration (ms) */
	normalDuration: 200,
	/** 通常の fly/scale パラメータ */
	normal: { duration: 300, opacity: 0.9 },
	/** reduced-motion 時: opacity fade のみ */
	reduced: { duration: 0, opacity: 0 }
} as const;

// ─────────────────────────────────────────────────────────────
// §6  Momentum Projection — Apple 公式
// §9  Rubber-banding — ソフトバウンダリ
// ─────────────────────────────────────────────────────────────

/**
 * §9 Rubber-banding — エッジでの弾性抵抗計算。
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
 * §6 Momentum Projection — ジェスチャー速度から「指が向かっている先」を予測。
 * スクロール減速と同じ指数減衰モデル（Apple 公式）。
 *
 * @param initialVelocity - 離れた瞬間の速度 (px/s)
 * @param decelerationRate - 減速係数 (0.998 = 標準スクロール感, 0.99 = スナッピー)
 * @returns 予測される到達距離 (px)
 */
export function projectMomentum(initialVelocity: number, decelerationRate = 0.998): number {
	return ((initialVelocity / 1000) * decelerationRate) / (1 - decelerationRate);
}

/**
 * §5 Velocity Handoff — ドラッグ速度をスプリングの初期速度に変換。
 *
 * @param gestureVelocity - ポインタの離脱速度 (px/s)
 * @param remainingDistance - 現在位置から目標までの距離 (px)
 * @returns 相対速度 (normalized velocity)
 */
export function handoffVelocity(gestureVelocity: number, remainingDistance: number): number {
	if (remainingDistance === 0) return 0;
	return gestureVelocity / Math.abs(remainingDistance);
}

/**
 * §6 Snap Target — モメンタム投影結果から最も近いスナップポイントを選択。
 *
 * @param projectedEndpoint - 投影された到達点
 * @param snapPoints - スナップポイントの配列 (昇順)
 * @returns 最も近いスナップポイント
 */
export function nearestSnapPoint(projectedEndpoint: number, snapPoints: number[]): number {
	if (snapPoints.length === 0) return projectedEndpoint;
	return snapPoints.reduce((prev, curr) =>
		Math.abs(curr - projectedEndpoint) < Math.abs(prev - projectedEndpoint) ? curr : prev
	);
}

// ─────────────────────────────────────────────────────────────
// §11 Frame-level smoothness — will-change ヒント
// ─────────────────────────────────────────────────────────────

/**
 * §11 — アニメーション対象のプロパティに `will-change` を適用するためのユーティリティ。
 * コンポーザー只中のみヒントし、アニメーション後に削除する。
 */
export const WILL_CHANGE = {
	/** transform アニメーション用 */
	transform: 'will-change-transform',
	/** opacity アニメーション用 */
	opacity: 'will-change-opacity',
	/** transform + opacity 同時 */
	both: 'will-change-transform, opacity',
	/** backdrop-filter アニメーション用 */
	filter: 'will-change-filter'
} as const;

// ─────────────────────────────────────────────────────────────
// §16 Design Foundations — 八原則
// ─────────────────────────────────────────────────────────────

/**
 * §16 — Apple の八原則。UI レビュー時のチェックリスト。
 * コードには直接影響しないが、ドキュメント参照用に。
 */
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
