/**
 * src/lib/styles/types.ts
 * Visual style pack type definitions.
 *
 * A VisualStylePack defines all non-color visual tokens for a design language:
 * press feedback, materials/glass, shadows, easing, springs, typography,
 * layout tokens, and pre-computed component class strings.
 *
 * Color tokens are handled separately by `src/lib/theme/` (ThemePack).
 */

/**
 * 組み込みスタイルID。
 * カスタム/サードパティスタイルは registry 経由で登録されるため、
 * ここに列挙されていないIDも `string` として扱えるよう
 * 利用側では `(string & {})` Union を推奨。
 */
export type StyleId =
	| 'apple'
	| 'material'
	| 'neumorphism'
	| 'brutalism'
	| 'cyberpunk'
	| 'flat'
	| 'high-contrast'
	| 'win95'
	| 'winxp';

/**
 * スタイル固有のレイアウト設定。
 * コンポーネントはこれらのフラグを読んで出し分ける。
 */
export interface StyleLayout {
	/** カード表面の処理方法 */
	cardSurface: 'glass' | 'solid' | 'neumorphic' | 'brutal' | 'chiseled' | 'gradient';
	/** ベースの角丸 */
	borderRadius: 'none' | 'sm' | 'md' | 'lg' | 'xl' | '2xl';
	/** 枠線を表示するか */
	borders: boolean;
	/** ガラスオーバーレイ（シーン・光彩）を表示するか */
	glassOverlay: boolean;
	/** 大文字強制 */
	headingCase: 'normal' | 'uppercase';
	/** コンパクトな間隔 */
	dense: boolean;
}

/**
 * プリコンピュート済みクラス文字列。
 * コンポーネントはこれを直接 class にセットするだけでスタイルが効く。
 */
export interface StyleTokens {
	/** セクション見出し行 */
	sectionHeader: string;
	/** カード内のヘッダー */
	cardHeader: string;
	/** ページタイトル */
	pageTitle: string;
	/** ナビゲーションバー */
	navBar: string;
}

/**
 * コンポーネント単位の完全な UI レシピ。
 *
 * 各スタイルパックは「デフォルト（Apple 風）と違う部分」だけを `components` に定義する。
 * コンポーネント側は `resolveComponents(pack)` 経由で参照し、
 * 定義されていないキーは中央の `DEFAULT_COMPONENTS` にフォールバックする。
 *
 * これにより、Win95 ならベベル枠のボタン、Neumorphism なら凹凸シャドウのカード、
 * というようにスタイルごとに全く異なる見た目を持たせつつ、
 * 未定義部分は常に安全なデフォルトが保証される。
 */
export interface StyleComponents {
	button: {
		/** 共通ベース（レイアウト・フォーカス・無効化。形状・プレスは含まない） */
		base: string;
		/** 形状（角丸） */
		shape: string;
		sm: string;
		md: string;
		lg: string;
		primary: string;
		secondary: string;
		ghost: string;
		danger: string;
		glass: string;
	};
	input: {
		/** テキスト入力・セレクトの共通ベース */
		base: string;
	};
	card: {
		/** 静的カード */
		base: string;
		/** クリック可能なカード */
		interactive: string;
	};
}

/**
 * StyleComponents の部分オーバーライド型。
 * 各パックは定義したいキーだけ記述すればよい。
 */
export type StyleComponentsOverrides = {
	[K in keyof StyleComponents]?: Partial<StyleComponents[K]>;
};

export interface VisualStylePack {
	/** Unique identifier */
	id: StyleId;
	/** Human-readable name */
	name: string;
	/** スタイルの説明（サードパティ/カスタムスタイル向け） */
	description?: string;
	/** 作者名（サードパティスタイル向け） */
	author?: string;
	/** パック定義のバージョン（将来の互換性管理用） */
	version?: number;

	// ── §1 Response — press feedback (Tailwind class strings) ──
	press: {
		button: string;
		icon: string;
		card: string;
		tab: string;
		/** ガラス/半透明要素用のプレスフィードバック（旧: liquid） */
		glass: string;
		toggle: string;
		[key: string]: string;
	};

	// ── §12 Materials — glass / surface treatments (Tailwind class strings) ──
	materials: {
		glass: string;
		glassHeader: string;
		glassModal: string;
		card: string;
		interactiveCard: string;
		[key: string]: string;
	};

	// ── §4 Easing — CSS cubic-bezier strings ──
	easing: {
		default: string;
		sheet: string;
		bounce: string;
		decelerate: string;
		reverse: string;
		[key: string]: string;
	};

	// ── §4 Springs ──
	/**
	 * Spring 物理パラメータ。
	 * Spring-based motion を使うスタイル（apple / neumorphism / cyberpunk）のみ定義。
	 * MD3 / Brutalism / Flat / Win95 / WinXP 等は easing のみで motion を表現するため省略可。
	 */
	spring?: {
		default: { damping: number; response: number };
		sheet: { damping: number; response: number };
		modal: { damping: number; response: number };
		menu: { damping: number; response: number };
		flick: { damping: number; response: number };
		rotation: { damping: number; response: number };
		pinch: { damping: number; response: number };
		[key: string]: { damping: number; response: number };
	};

	// ── §12 Shadow depth — raw CSS box-shadow strings ──
	shadow: {
		sm: string;
		md: string;
		lg: string;
		xl: string;
		[key: string]: string;
	};

	// ── §15 Typography — Tailwind class strings ──
	typography: {
		display: string;
		heading: string;
		body: string;
		label: string;
		numeric: string;
		[key: string]: string;
	};

	// ── §11 will-change hints ──
	willChange: {
		transform: string;
		opacity: string;
		both: string;
		filter: string;
		[key: string]: string;
	};

	// ── 🆕 スタイル固有のレイアウト設定 ──
	layout: StyleLayout;

	// ── 🆕 プリコンピュート済みトークン文字列 ──
	tokens: StyleTokens;

	// ── 🆕 コンポーネント単位の UI レシピ（部分オーバーライド） ──
	/**
	 * 定義されていないキーは中央の DEFAULT_COMPONENTS にフォールバックする。
	 * 各パックは「デフォルトと違う部分」だけを記述すればよい。
	 */
	components?: StyleComponentsOverrides;
}
