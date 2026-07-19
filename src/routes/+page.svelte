<script lang="ts">
	import { resolve } from '$app/paths';
	import { openLink } from '$lib/browser';
	import { m } from '$lib/paraglide/messages';
	import { settingsStore } from '$lib/stores';
	import type { ExternalLink, SearchProviderId } from '$lib/types';
	import PageHeader from '$lib/components/PageHeader.svelte';
	import Container from '$lib/components/Container.svelte';
	import SectionHeader from '$lib/components/SectionHeader.svelte';
	import WeatherCard from '$lib/components/WeatherCard.svelte';
	import IconButton from '$lib/components/IconButton.svelte';
	import Modal from '$lib/components/Modal.svelte';
	import TextField from '$lib/components/TextField.svelte';
	import Button from '$lib/components/Button.svelte';
	import BentoCard from '$lib/components/BentoCard.svelte';
	import SpotlightSearch from '$lib/components/SpotlightSearch.svelte';
	import {
		Bell,
		Laptop,
		GraduationCap,
		Users,
		BookOpen,
		Book,
		Calendar,
		ChevronRight,
		Sun,
		Moon,
		Plus,
		Globe,
		Trash2,
		Pencil,
		ExternalLink as ExternalLinkIcon
	} from '@lucide/svelte';
	import type { Component } from 'svelte';
	import { Capacitor } from '@capacitor/core';
	import TimetableWidget from '$lib/components/dashboard/TimetableWidget.svelte';
	import TodoWidget from '$lib/components/dashboard/TodoWidget.svelte';
	import CalendarWidget from '$lib/components/dashboard/CalendarWidget.svelte';
	import AttendanceWidget from '$lib/components/dashboard/AttendanceWidget.svelte';
	import FeatureLinkCard from '$lib/components/dashboard/FeatureLinkCard.svelte';

	type HomeLink = {
		id: string;
		name: string;
		url: string;
		icon: Component<{ size?: number; class?: string }>;
		forceExternal?: boolean;
		custom?: boolean;
	};

	const settings = $derived(settingsStore.settings);

	const defaultStudyLinks: HomeLink[] = [
		{
			id: 'uec-portal',
			name: 'UECポータル',
			url: 'https://portalweb.uec.ac.jp/Portal/',
			icon: Laptop,
			forceExternal: true
		},
		{
			id: 'campusweb',
			name: '学務情報',
			url: 'https://campusweb.office.uec.ac.jp/campusweb/',
			icon: GraduationCap,
			forceExternal: true
		},
		{
			id: 'classroom',
			name: 'Classroom',
			url: 'https://classroom.google.com/',
			icon: Users,
			forceExternal: true
		},
		{
			id: 'webclass',
			name: 'WebClass',
			url: 'https://webclass.cdel.uec.ac.jp/webclass/',
			icon: BookOpen,
			forceExternal: true
		},
		{
			id: 'jb-moodle',
			name: 'JB Moodle',
			url: 'https://joho.g-edu.uec.ac.jp/moodle3/',
			icon: BookOpen,
			forceExternal: true
		}
	];

	const defaultBottomLinks: HomeLink[] = [
		{
			id: 'youran',
			name: '学修要覧',
			url: 'https://kyoumu.office.uec.ac.jp/youran/youran.html',
			icon: Book,
			forceExternal: true
		},
		{
			id: 'timet',
			name: '時間割',
			url: 'http://kyoumu.office.uec.ac.jp/timet/index.html',
			icon: Calendar,
			forceExternal: true
		}
	];

	let linkModalOpen = $state(false);
	let editingLinkId = $state<string | null>(null);
	let linkName = $state('');
	let linkUrl = $state('');
	let searchQuery = $state('');

	const searchProviders: { id: SearchProviderId; label: string; searchUrl: string }[] = [
		{ id: 'google', label: 'Google', searchUrl: 'https://www.google.com/search?q=' },
		{ id: 'bing', label: 'Bing', searchUrl: 'https://www.bing.com/search?q=' },
		{ id: 'duckduckgo', label: 'DuckDuckGo', searchUrl: 'https://duckduckgo.com/?q=' },
		{ id: 'chatgpt', label: 'ChatGPT', searchUrl: 'https://chatgpt.com/?q=' },
		{ id: 'claude', label: 'Claude', searchUrl: 'https://claude.ai/new?q=' },
		{ id: 'grok', label: 'Grok', searchUrl: 'https://grok.com/?q=' },
		{ id: 'perplexity', label: 'Perplexity', searchUrl: 'https://www.perplexity.ai/?q=' }
	];

	const activeProvider = $derived(
		searchProviders.find((item) => item.id === settings.homeSearchProvider) ?? searchProviders[0]
	);

	const searchPlaceholder = $derived(m.search_with_provider({ provider: activeProvider.label }));

	const customStudyLinks = $derived(
		settings.externalLinks
			.filter((link) => link.category === 'study' && !link.id.startsWith('fixed:'))
			.sort((a, b) => a.order - b.order)
	);

	const fixedOverrides = $derived(
		new Map(
			settings.externalLinks
				.filter((link) => link.category === 'study' && link.id.startsWith('fixed:'))
				.map((link) => [link.id.slice('fixed:'.length), link])
		)
	);

	const fixedStudyLinks = $derived(
		defaultStudyLinks.map((link) => {
			const override = fixedOverrides.get(link.id);
			return override ? { ...link, name: override.name, url: override.url } : link;
		})
	);

	const bottomLinks = $derived(
		defaultBottomLinks.map((link) => {
			const override = fixedOverrides.get(link.id);
			return override ? { ...link, name: override.name, url: override.url } : link;
		})
	);

	const isNativeApp = $derived(Capacitor.isNativePlatform());

	function linkHref(url: string) {
		return url;
	}

	function linkTarget() {
		return '_blank';
	}

	function linkRel() {
		return 'noopener noreferrer';
	}

	function handleLinkClick(e: MouseEvent, url: string, forceExternal = false) {
		if (!forceExternal && isNativeApp && settings.openLinksInApp) {
			e.preventDefault();
			void openLink(url, true);
		}
	}

	async function submitWebSearch(queryToSearch: string) {
		const query = queryToSearch.trim();
		if (!query) return;
		const provider =
			searchProviders.find((item) => item.id === settings.homeSearchProvider) ?? searchProviders[0];
		const url = `${provider.searchUrl}${encodeURIComponent(query)}`;

		const isAIProvider = ['chatgpt', 'claude', 'grok', 'perplexity'].includes(provider.id);

		if (isNativeApp && !isAIProvider && settings.openLinksInApp) {
			void openLink(url, true);
		} else {
			window.open(url, '_blank', 'noopener,noreferrer');
		}
	}

	import { onDestroy } from 'svelte';

	let suggestions = $state<string[]>([]);
	let showSuggestions = $state(false);
	let suggestTimeout: any = null;

	function handleSearchInput(queryVal: string) {
		if (suggestTimeout) clearTimeout(suggestTimeout);

		const query = queryVal.trim();
		if (!query || query.length > 30 || query.includes('\n')) {
			suggestions = [];
			showSuggestions = false;
			return;
		}

		suggestTimeout = setTimeout(async () => {
			try {
				const fetched = await fetchSuggestions(query);
				suggestions = fetched;
				showSuggestions = suggestions.length > 0;
			} catch (err) {
				console.error('Failed to fetch suggestions:', err);
			}
		}, 200);
	}

	function fetchSuggestions(query: string): Promise<string[]> {
		return new Promise((resolve) => {
			if (typeof window === 'undefined') {
				resolve([]);
				return;
			}

			const callbackName = 'googleSuggestCallback_' + Math.random().toString(36).substring(2, 9);
			(window as any)[callbackName] = (data: any) => {
				delete (window as any)[callbackName];
				const scriptElement = document.getElementById(callbackName);
				if (scriptElement) {
					document.body.removeChild(scriptElement);
				}
				if (data && Array.isArray(data[1])) {
					const cleaned = data[1].map((item: any) => {
						if (typeof item === 'string') return item;
						if (Array.isArray(item) && typeof item[0] === 'string') return item[0];
						if (typeof item === 'object' && item !== null) {
							return item.phrase || item.name || String(item);
						}
						return String(item);
					});
					resolve(cleaned);
				} else {
					resolve([]);
				}
			};

			const script = document.createElement('script');
			script.id = callbackName;
			script.src = `https://suggestqueries.google.com/complete/search?client=firefox&hl=ja&q=${encodeURIComponent(query)}&jsonp=${callbackName}`;
			script.onerror = () => {
				delete (window as any)[callbackName];
				const scriptElement = document.getElementById(callbackName);
				if (scriptElement) {
					document.body.removeChild(scriptElement);
				}
				resolve([]);
			};
			document.body.appendChild(script);
		});
	}

	$effect(() => {
		const handleClickOutside = (e: MouseEvent) => {
			const target = e.target as HTMLElement;
			if (!target.closest('.search-container-relative')) {
				showSuggestions = false;
			}
		};
		if (typeof document !== 'undefined') {
			document.addEventListener('click', handleClickOutside);
		}
		return () => {
			if (typeof document !== 'undefined') {
				document.removeEventListener('click', handleClickOutside);
			}
		};
	});

	onDestroy(() => {
		if (suggestTimeout) clearTimeout(suggestTimeout);
	});

	function openAddLink() {
		editingLinkId = null;
		linkName = '';
		linkUrl = '';
		linkModalOpen = true;
	}

	function openEditCustomLink(link: ExternalLink) {
		editingLinkId = link.id;
		linkName = link.name;
		linkUrl = link.url;
		linkModalOpen = true;
	}

	function openEditFixedLink(link: HomeLink) {
		editingLinkId = `fixed:${link.id}`;
		linkName = link.name;
		linkUrl = link.url;
		linkModalOpen = true;
	}

	function normalizeUrl(url: string) {
		const trimmed = url.trim();
		if (!trimmed) return '';
		return trimmed.startsWith('http://') || trimmed.startsWith('https://')
			? trimmed
			: `https://${trimmed}`;
	}

	function saveCustomLink() {
		const name = linkName.trim();
		const url = normalizeUrl(linkUrl);
		if (!name || !url) return;
		const existing = settings.externalLinks;
		if (editingLinkId) {
			const found = existing.some((link) => link.id === editingLinkId);
			const next = found
				? existing.map((link) =>
						link.id === editingLinkId ? { ...link, name, url, icon: 'globe' } : link
					)
				: [
						...existing,
						{
							id: editingLinkId,
							name,
							url,
							icon: 'globe',
							category: 'study' as const,
							order: 0
						}
					];
			settingsStore.update({ externalLinks: next });
		} else {
			const nextOrder = customStudyLinks.reduce((max, link) => Math.max(max, link.order), -1) + 1;
			settingsStore.update({
				externalLinks: [
					...existing,
					{
						id: crypto.randomUUID(),
						name,
						url,
						icon: 'globe',
						category: 'study',
						order: nextOrder
					}
				]
			});
		}
		linkModalOpen = false;
	}

	function deleteCustomLink() {
		if (!editingLinkId) return;
		settingsStore.update({
			externalLinks: settings.externalLinks.filter((link) => link.id !== editingLinkId)
		});
		linkModalOpen = false;
	}

	function handleNotificationClick() {
		alert('新しい通知はありません');
	}

	function toggleTheme() {
		const current = settingsStore.settings.theme;
		let next: 'light' | 'dark' | 'system';
		if (current === 'light') {
			next = 'dark';
		} else if (current === 'dark') {
			next = 'system';
		} else {
			next = 'light';
		}
		settingsStore.update({ theme: next });
	}

	const themeIcon = $derived.by(() => {
		const t = settingsStore.settings.theme;
		if (t === 'light') return Sun;
		if (t === 'dark') return Moon;
		return Laptop;
	});

	const themeAriaLabel = $derived.by(() => {
		const t = settingsStore.settings.theme;
		if (t === 'light') return 'テーマ: ライト';
		if (t === 'dark') return 'テーマ: ダーク';
		return 'テーマ: システム';
	});
</script>

<svelte:head>
	<title>{m.nav_home()} | OpenYASKE</title>
</svelte:head>

{#snippet leadingSnippet()}
	<IconButton
		icon={themeIcon}
		ariaLabel={themeAriaLabel}
		onclick={toggleTheme}
		variant="subtle"
		size="sm"
	/>
{/snippet}

{#snippet trailingSnippet()}
	<IconButton
		icon={Bell}
		ariaLabel="通知"
		onclick={handleNotificationClick}
		variant="subtle"
		size="sm"
	/>
{/snippet}

<PageHeader title={m.nav_home()} leading={leadingSnippet} trailing={trailingSnippet} />

<Container class="pb-16 max-w-7xl">
	<div class="space-y-8 py-4">
		<!-- Top Hero Section: Spotlight Search & Weather -->
		<div class="space-y-4">
			<SpotlightSearch
				bind:query={searchQuery}
				bind:showSuggestions
				placeholder={searchPlaceholder}
				{suggestions}
				onsearch={(q) => submitWebSearch(q)}
				oninput={(q) => handleSearchInput(q)}
			/>

			{#if settings.homeShowWeather}
				<WeatherCard />
			{/if}
		</div>

		<!-- Apple Bento Grid: 授業関連 Portal Links -->
		<div class="space-y-4">
			<SectionHeader>{m.section_external_links()}</SectionHeader>

			<!-- Responsive Bento Grid: 2 cols (mobile), 3 cols (sm), 4 cols (md), 6 cols (lg) -->
			<!-- eslint-disable svelte/no-navigation-without-resolve -->
			<div class="grid grid-cols-2 sm:grid-cols-3 md:grid-cols-4 lg:grid-cols-6 gap-3 md:gap-4">
				{#each fixedStudyLinks as link (link.id)}
					<div class="relative group">
						<BentoCard
							href={linkHref(link.url)}
							target={linkTarget()}
							rel={linkRel()}
							onclick={(e) => handleLinkClick(e, link.url, link.forceExternal)}
							padding="p-3.5"
							class="h-full min-h-[96px] text-center flex flex-col items-center justify-center relative"
						>
							<span
								class="mb-2 mx-auto flex h-10 w-10 items-center justify-center rounded-2xl bg-[var(--color-primary-500)]/15 text-[var(--color-primary-600)] transition-transform duration-200 group-hover:scale-105"
							>
								<link.icon size={20} />
							</span>
							<span
								class="line-clamp-1 text-xs font-semibold tracking-tight text-[var(--color-nav-active)] px-1 w-full text-center"
							>
								{link.name}
							</span>
						</BentoCard>
						<button
							type="button"
							onclick={(e) => {
								e.preventDefault();
								e.stopPropagation();
								openEditFixedLink(link);
							}}
							class="absolute right-2 top-2 z-10 flex h-7 w-7 items-center justify-center rounded-full bg-[var(--color-surface-card)] text-[var(--color-nav-inactive)] opacity-0 group-hover:opacity-100 transition-all duration-150 hover:text-[var(--color-nav-active)] hover:scale-105 active:scale-95 shadow-apple-md border border-[var(--color-surface-border)]/60"
							aria-label={m.external_link_name()}
						>
							<Pencil size={13} />
						</button>
					</div>
				{/each}

				{#each customStudyLinks as link (link.id)}
					<div class="relative group">
						<BentoCard
							href={linkHref(link.url)}
							target={linkTarget()}
							rel={linkRel()}
							onclick={(e) => handleLinkClick(e, link.url)}
							padding="p-3.5"
							class="h-full min-h-[96px] text-center flex flex-col items-center justify-center relative"
						>
							<span
								class="mb-2 mx-auto flex h-10 w-10 items-center justify-center rounded-2xl bg-[var(--color-primary-500)]/15 text-[var(--color-primary-600)] transition-transform duration-200 group-hover:scale-105"
							>
								<Globe size={20} />
							</span>
							<span
								class="line-clamp-1 text-xs font-semibold tracking-tight text-[var(--color-nav-active)] px-1 w-full text-center"
							>
								{link.name}
							</span>
						</BentoCard>
						<button
							type="button"
							onclick={(e) => {
								e.preventDefault();
								e.stopPropagation();
								openEditCustomLink(link);
							}}
							class="absolute right-2 top-2 z-10 flex h-7 w-7 items-center justify-center rounded-full bg-[var(--color-surface-card)] text-[var(--color-nav-inactive)] opacity-0 group-hover:opacity-100 transition-all duration-150 hover:text-[var(--color-nav-active)] hover:scale-105 active:scale-95 shadow-apple-md border border-[var(--color-surface-border)]/60"
							aria-label={m.external_link_name()}
						>
							<Pencil size={13} />
						</button>
					</div>
				{/each}

				<BentoCard
					onclick={openAddLink}
					padding="p-3.5"
					class="h-full min-h-[96px] text-center flex flex-col items-center justify-center border-dashed border-[var(--color-surface-border)]/80"
				>
					<span
						class="mb-2 mx-auto flex h-10 w-10 items-center justify-center rounded-2xl bg-[var(--color-surface-muted)] text-[var(--color-nav-inactive)]"
					>
						<Plus size={20} />
					</span>
					<span
						class="line-clamp-1 text-xs font-semibold tracking-tight text-[var(--color-nav-active)] text-center w-full px-1"
					>
						{m.external_link_add()}
					</span>
				</BentoCard>
			</div>
			<!-- eslint-enable svelte/no-navigation-without-resolve -->

			<!-- Wide Banner Cards: 2 Columns on Tablet/Desktop -->
			<!-- eslint-disable svelte/no-navigation-without-resolve -->
			<div class="grid grid-cols-1 sm:grid-cols-2 gap-3 md:gap-4">
				{#each bottomLinks as link (link.id)}
					<div class="relative group">
						<BentoCard
							href={linkHref(link.url)}
							target={linkTarget()}
							rel={linkRel()}
							onclick={(e) => handleLinkClick(e, link.url, link.forceExternal)}
							padding="px-4 py-3.5"
							class="flex-row items-center justify-between"
						>
							<div class="flex items-center gap-3 min-w-0 flex-1 pr-8">
								<div
									class="flex h-10 w-10 shrink-0 items-center justify-center rounded-xl bg-[var(--color-primary-500)]/15 text-[var(--color-primary-600)]"
								>
									<link.icon size={20} />
								</div>
								<span
									class="text-sm font-semibold tracking-tight text-[var(--color-nav-active)] truncate"
								>
									{link.name}
								</span>
							</div>
							<div class="flex items-center gap-1.5 shrink-0">
								<ChevronRight
									size={18}
									class="text-[var(--color-nav-inactive)] group-hover:translate-x-0.5 transition-transform shrink-0"
								/>
							</div>
						</BentoCard>
						<button
							type="button"
							onclick={(e) => {
								e.preventDefault();
								e.stopPropagation();
								openEditFixedLink(link);
							}}
							class="absolute right-3 top-1/2 -translate-y-1/2 z-10 flex h-7 w-7 items-center justify-center rounded-full bg-[var(--color-surface-card)] text-[var(--color-nav-inactive)] opacity-0 group-hover:opacity-100 transition-all duration-150 hover:text-[var(--color-nav-active)] hover:scale-105 active:scale-95 shadow-apple-md border border-[var(--color-surface-border)]/60"
							aria-label={m.external_link_name()}
						>
							<Pencil size={13} />
						</button>
					</div>
				{/each}
			</div>
			<!-- eslint-enable svelte/no-navigation-without-resolve -->
		</div>

		<!-- Bento Feature Grid Area: Dynamic Widgets & Features -->
		<div class="space-y-4">
			<SectionHeader>{m.nav_home()}</SectionHeader>

			<div class="space-y-4">
				{#each settings.homeFeatures as fid (fid)}
					{@const layout = settings.homeFeatureLayouts?.[fid] || 'button'}
					{#if fid === 'timetable' && layout === 'widget'}
						<TimetableWidget />
					{:else if fid === 'todo' && layout === 'widget'}
						<TodoWidget />
					{:else if fid === 'calendar' && layout === 'widget'}
						<CalendarWidget />
					{:else if fid === 'attendance' && layout === 'widget'}
						<AttendanceWidget />
					{:else if fid !== 'dashboard'}
						<FeatureLinkCard id={fid} />
					{/if}
				{/each}
			</div>
		</div>
	</div>
</Container>

<Modal
	open={linkModalOpen}
	onclose={() => (linkModalOpen = false)}
	title={editingLinkId ? m.external_link_name() : m.external_link_add()}
>
	<div class="space-y-4 py-2">
		<TextField bind:value={linkName} label={m.external_link_name()} placeholder="例: 図書館" />
		<TextField
			bind:value={linkUrl}
			type="url"
			label={m.external_link_url()}
			placeholder="https://..."
		/>
	</div>

	{#snippet footer()}
		<div class="flex gap-2">
			{#if editingLinkId}
				<Button
					variant="danger"
					size="md"
					onclick={deleteCustomLink}
					aria-label={m.action_delete()}
				>
					<Trash2 size={16} />
				</Button>
			{/if}
			<Button variant="secondary" size="md" class="flex-1" onclick={() => (linkModalOpen = false)}>
				{m.action_cancel()}
			</Button>
			<Button
				variant="primary"
				size="md"
				class="flex-1"
				onclick={saveCustomLink}
				disabled={!linkName.trim() || !linkUrl.trim()}
			>
				{m.action_save()}
			</Button>
		</div>
	{/snippet}
</Modal>
