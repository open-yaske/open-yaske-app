<!--
  DashboardCardItem.svelte
  公開データの DashboardCard 1 枚を描画。
  3 バリアント: html-internal / external-url / webview-popup
  - html-internal: HTML をフェッチしてサニタイズしてインライン表示
  - external-url: /inapp-browser ルートへリンク or 外部ブラウザ
  - webview-popup: Sheet にサニタイズした HTML を表示
  アイコン文字列を lucide アイコンにマッピング（未対応なら emoji テキストとして描画）。
-->
<script lang="ts">
	import type { Component } from 'svelte';
	import { resolve } from '$app/paths';
	import { m } from '$lib/paraglide/messages';
	import { sanitize } from '$lib/html-renderer';
	import type {
		DashboardCard,
		HtmlInternalCard,
		ExternalUrlCard,
		WebviewPopupCard
	} from '$lib/types/public';
	import Sheet from '../Sheet.svelte';
	import Emoji from '../Emoji.svelte';
	import {
		LayoutGrid,
		ExternalLink,
		ChevronRight,
		BookOpen,
		Calendar,
		CheckSquare,
		BarChart3,
		Info,
		Link2,
		ShieldCheck,
		Cloud,
		Database,
		Palette,
		Languages,
		Settings,
		Home,
		FileText,
		GraduationCap,
		MapPin,
		Megaphone,
		Newspaper,
		Bell,
		Mail,
		Globe,
		Wifi,
		Coffee,
		Heart,
		Star,
		Bookmark,
		Folder,
		Image as ImageIcon,
		Download,
		Upload,
		Search,
		User,
		Users,
		Building,
		Briefcase,
		Wrench,
		Code,
		Terminal,
		Bug,
		Lightbulb,
		Zap,
		Clock,
		AlertCircle,
		HelpCircle,
		RefreshCw,
		Share2,
		Copy,
		Trash2,
		Plus,
		Minus,
		Check,
		X,
		Menu,
		Filter,
		SortAsc,
		Eye,
		EyeOff,
		Lock,
		Unlock,
		Settings2,
		Sliders
	} from '@lucide/svelte';

	interface Props {
		card: DashboardCard;
	}

	let { card }: Props = $props();

	// --- アイコン名 → lucide コンポーネントのマッピング ---
	const iconMap: Record<
		string,
		Component<{ size?: number; color?: string; 'stroke-width'?: number }>
	> = {
		'layout-grid': LayoutGrid,
		'external-link': ExternalLink,
		'chevron-right': ChevronRight,
		'book-open': BookOpen,
		book: BookOpen,
		calendar: Calendar,
		'check-square': CheckSquare,
		check: CheckSquare,
		'bar-chart': BarChart3,
		'bar-chart-3': BarChart3,
		info: Info,
		link: Link2,
		'link-2': Link2,
		shield: ShieldCheck,
		'shield-check': ShieldCheck,
		cloud: Cloud,
		database: Database,
		palette: Palette,
		languages: Languages,
		settings: Settings,
		home: Home,
		'file-text': FileText,
		file: FileText,
		'graduation-cap': GraduationCap,
		'map-pin': MapPin,
		megaphone: Megaphone,
		newspaper: Newspaper,
		bell: Bell,
		mail: Mail,
		globe: Globe,
		wifi: Wifi,
		coffee: Coffee,
		heart: Heart,
		star: Star,
		bookmark: Bookmark,
		folder: Folder,
		image: ImageIcon,
		download: Download,
		upload: Upload,
		search: Search,
		user: User,
		users: Users,
		building: Building,
		briefcase: Briefcase,
		wrench: Wrench,
		tool: Wrench,
		code: Code,
		terminal: Terminal,
		bug: Bug,
		lightbulb: Lightbulb,
		zap: Zap,
		clock: Clock,
		'alert-circle': AlertCircle,
		'help-circle': HelpCircle,
		refresh: RefreshCw,
		'refresh-cw': RefreshCw,
		share: Share2,
		'share-2': Share2,
		copy: Copy,
		trash: Trash2,
		'trash-2': Trash2,
		plus: Plus,
		minus: Minus,
		'check-circle': Check,
		x: X,
		menu: Menu,
		filter: Filter,
		sort: SortAsc,
		'sort-asc': SortAsc,
		eye: Eye,
		'eye-off': EyeOff,
		lock: Lock,
		unlock: Unlock,
		'settings-2': Settings2,
		sliders: Sliders
	};

	// アイコン文字列が lucide 名かどうか判定
	const isLucideIcon = $derived(card.icon in iconMap);
	const ResolvedIcon = $derived(iconMap[card.icon] ?? LayoutGrid);

	// --- html-internal: HTML フェッチ + サニタイズ ---
	let htmlContent = $state<string | null>(null);
	let htmlLoading = $state(false);
	let htmlFetched = $state(false);

	async function fetchHtml(card: HtmlInternalCard) {
		if (htmlFetched) return;
		htmlLoading = true;
		try {
			const res = await fetch(card.htmlUrl);
			if (res.ok) {
				htmlContent = sanitize(await res.text());
			}
		} catch {
			// サイレントフェイル — タイトルのみ表示
		} finally {
			htmlLoading = false;
			htmlFetched = true;
		}
	}

	// --- webview-popup: Sheet 状態 ---
	let sheetOpen = $state(false);
	let sheetHtml = $state('');
	let sheetLoading = $state(false);

	async function openWebviewPopup(card: WebviewPopupCard) {
		sheetOpen = true;
		sheetLoading = true;
		sheetHtml = '';
		try {
			const res = await fetch(card.htmlUrl);
			if (res.ok) {
				sheetHtml = sanitize(await res.text());
			}
		} catch {
			// 空のシートを表示
		} finally {
			sheetLoading = false;
		}
	}

	// --- external-url: href 構築 ---
	const externalHref = $derived.by(() => {
		if (card.type !== 'external-url') return undefined;
		const c = card as ExternalUrlCard;
		if (c.openInApp) {
			return resolve(`/inapp-browser?url=${encodeURIComponent(c.url)}`);
		}
		return c.url;
	});

	const isExternalTarget = $derived(
		card.type === 'external-url' && !(card as ExternalUrlCard).openInApp
	);

	// html-internal の banner 判定
	const isBanner = $derived(
		card.type === 'html-internal' && (card as HtmlInternalCard).layout === 'banner'
	);
</script>

{#if card.type === 'html-internal'}
	{@const c = card as HtmlInternalCard}
	<div
		class="flex flex-col overflow-hidden rounded-[var(--radius-card)] border border-[var(--color-surface-border)] bg-[var(--color-surface-card)] shadow-[var(--shadow-card)]"
		class:col-span-2={isBanner}
	>
		<div class="flex items-center gap-2 px-3 pt-3">
			{#if isLucideIcon}
				<ResolvedIcon size={18} color="var(--color-primary-500)" />
			{:else}
				<span class="text-lg leading-none"><Emoji emoji={c.icon} /></span>
			{/if}
			<span class="text-sm font-semibold text-[var(--color-nav-active)]">{c.title}</span>
		</div>
		<div class="px-3 pb-3 pt-2">
			{#if htmlLoading}
				<div class="h-16 animate-pulse rounded bg-[var(--color-surface-muted)]"></div>
			{:else if htmlContent}
				<div class="prose prose-sm max-w-none text-[var(--color-nav-active)]">
					<!-- htmlContent is sanitized via DOMPurify in lib/html-renderer.ts -->
					<!-- eslint-disable-next-line svelte/no-at-html-tags -->
					{@html htmlContent}
				</div>
			{:else}
				<div
					role="button"
					tabindex="0"
					onclick={() => fetchHtml(c)}
					onkeydown={(e) => e.key === 'Enter' && fetchHtml(c)}
					class="flex items-center justify-center gap-1 py-4 text-xs text-[var(--color-nav-inactive)]"
				>
					{m.action_refresh()}
				</div>
			{/if}
		</div>
	</div>
{:else if card.type === 'external-url'}
	{@const c = card as ExternalUrlCard}
	<!-- eslint-disable svelte/no-navigation-without-resolve -->
	<a
		href={externalHref ?? '#'}
		target={isExternalTarget ? '_blank' : undefined}
		rel={isExternalTarget ? 'noopener noreferrer' : undefined}
		class="flex flex-col items-start gap-2 rounded-[var(--radius-card)] border border-[var(--color-surface-border)] bg-[var(--color-surface-card)] px-3 py-3 text-left shadow-[var(--shadow-card)] transition-colors hover:bg-[var(--color-surface-muted)]"
	>
		<div class="flex w-full items-center justify-between">
			{#if isLucideIcon}
				<ResolvedIcon size={18} color="var(--color-primary-500)" />
			{:else}
				<span class="text-lg leading-none"><Emoji emoji={c.icon} /></span>
			{/if}
			<ExternalLink size={14} color="var(--color-nav-inactive)" />
		</div>
		<span class="text-sm font-semibold text-[var(--color-nav-active)]">{c.title}</span>
	</a>
	<!-- eslint-enable svelte/no-navigation-without-resolve -->
{:else if card.type === 'webview-popup'}
	{@const c = card as WebviewPopupCard}
	<button
		type="button"
		onclick={() => openWebviewPopup(c)}
		class="flex flex-col items-start gap-2 rounded-[var(--radius-card)] border border-[var(--color-surface-border)] bg-[var(--color-surface-card)] px-3 py-3 text-left shadow-[var(--shadow-card)] transition-colors hover:bg-[var(--color-surface-muted)]"
	>
		<div class="flex w-full items-center justify-between">
			{#if isLucideIcon}
				<ResolvedIcon size={18} color="var(--color-primary-500)" />
			{:else}
				<span class="text-lg leading-none"><Emoji emoji={c.icon} /></span>
			{/if}
			<ChevronRight size={14} color="var(--color-nav-inactive)" />
		</div>
		<span class="text-sm font-semibold text-[var(--color-nav-active)]">{c.title}</span>
	</button>
{/if}

<!-- Sheet for webview-popup -->
<Sheet
	bind:open={sheetOpen}
	onclose={() => (sheetOpen = false)}
	title={card.type === 'webview-popup' ? (card as WebviewPopupCard).title : ''}
>
	{#if sheetLoading}
		<div class="flex items-center justify-center py-8 text-sm text-[var(--color-nav-inactive)]">
			{m.inapp_browser_loading()}
		</div>
	{:else if sheetHtml}
		<div class="prose prose-sm max-w-none text-[var(--color-nav-active)]">
			<!-- sheetHtml is sanitized via DOMPurify in lib/html-renderer.ts -->
			<!-- eslint-disable-next-line svelte/no-at-html-tags -->
			{@html sheetHtml}
		</div>
	{:else}
		<div class="flex items-center justify-center py-8 text-sm text-[var(--color-nav-inactive)]">
			{m.inapp_browser_error()}
		</div>
	{/if}
</Sheet>
