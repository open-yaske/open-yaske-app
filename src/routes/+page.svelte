<script lang="ts">
	import { resolve } from '$app/paths';
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
	import {
		Bell,
		Laptop,
		GraduationCap,
		Users,
		BookOpen,
		Book,
		Calendar,
		ChevronRight,
		FileText,
		Calculator,
		Sun,
		Moon,
		Plus,
		Globe,
		Trash2,
		Pencil,
		Search
	} from '@lucide/svelte';
	import type { Component } from 'svelte';
	import { Capacitor } from '@capacitor/core';

	type HomeLink = {
		id: string;
		name: string;
		url: string;
		icon: Component<{ size?: number }>;
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

	let linkModalOpen = $state(false);
	let editingLinkId = $state<string | null>(null);
	let linkName = $state('');
	let linkUrl = $state('');
	let searchQuery = $state('');

	const searchProviders: { id: SearchProviderId; label: string; searchUrl: string }[] = [
		{ id: 'google', label: 'Google', searchUrl: 'https://www.google.com/search?q=' },
		{ id: 'bing', label: 'Bing', searchUrl: 'https://www.bing.com/search?q=' },
		{ id: 'duckduckgo', label: 'DuckDuckGo', searchUrl: 'https://duckduckgo.com/?q=' }
	];

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

	const isNativeApp = $derived(Capacitor.isNativePlatform());

	function linkHref(url: string, forceExternal = false) {
		if (forceExternal || !isNativeApp) return url;
		return resolve(`/inapp-browser?url=${encodeURIComponent(url)}`);
	}

	function linkTarget(forceExternal = false) {
		return forceExternal || !isNativeApp ? '_blank' : undefined;
	}

	function linkRel(forceExternal = false) {
		return forceExternal || !isNativeApp ? 'noopener noreferrer' : undefined;
	}

	function submitWebSearch() {
		const query = searchQuery.trim();
		if (!query) return;
		const provider =
			searchProviders.find((item) => item.id === settings.homeSearchProvider) ?? searchProviders[0];
		const url = `${provider.searchUrl}${encodeURIComponent(query)}`;
		if (isNativeApp) {
			window.location.href = resolve(`/inapp-browser?url=${encodeURIComponent(url)}`);
		} else {
			window.open(url, '_blank', 'noopener,noreferrer');
		}
	}

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
	<title>{m.nav_home()} | open-nexus</title>
</svelte:head>

<!-- Mock header: settings left, notification right -->
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

<Container class="pb-10">
	<div class="space-y-6 py-4">
		<!-- Weather -->
		{#if settings.homeShowWeather}
			<WeatherCard />
		{/if}

		<form
			class="flex items-center gap-2 rounded-2xl border border-[var(--color-surface-border)] bg-[var(--color-surface-card)] p-2"
			onsubmit={(e) => {
				e.preventDefault();
				submitWebSearch();
			}}
		>
			<TextField bind:value={searchQuery} placeholder="Web検索" />
			<Button variant="primary" size="md" disabled={!searchQuery.trim()} aria-label="Web検索">
				<Search size={18} />
			</Button>
		</form>

		<!-- 授業関連 -->
		<div class="space-y-3">
			<SectionHeader>授業関連</SectionHeader>

			<!-- Responsive link grid: 3 cols on mobile, 6 cols on desktop -->
			<!-- eslint-disable svelte/no-navigation-without-resolve -->
			<div class="grid grid-cols-3 md:grid-cols-6 gap-3">
				{#each fixedStudyLinks as link (link.id)}
					<div class="relative aspect-square">
						<a
							href={linkHref(link.url, link.forceExternal)}
							target={linkTarget(link.forceExternal)}
							rel={linkRel(link.forceExternal)}
							class="flex h-full flex-col items-center justify-center rounded-2xl border border-[var(--color-surface-border)] bg-[var(--color-surface-card)] p-3 text-center transition-all hover:bg-[var(--color-surface-muted)] active:scale-[0.98]"
						>
							<span
								class="mb-2 flex h-11 w-11 items-center justify-center rounded-full bg-[var(--color-primary-50)] text-[var(--color-primary-800)]"
							>
								<link.icon size={22} />
							</span>
							<span
								class="line-clamp-1 text-[11px] font-bold tracking-tight text-[var(--color-nav-active)]"
							>
								{link.name}
							</span>
						</a>
						<button
							type="button"
							onclick={() => openEditFixedLink(link)}
							class="absolute right-1.5 top-1.5 flex h-7 w-7 items-center justify-center rounded-full bg-[var(--color-surface-card)]/95 text-[var(--color-nav-inactive)] transition-colors hover:text-[var(--color-nav-active)]"
							aria-label="リンクを編集"
						>
							<Pencil size={14} />
						</button>
					</div>
				{/each}
				{#each customStudyLinks as link (link.id)}
					<div class="relative aspect-square">
						<a
							href={linkHref(link.url)}
							target={linkTarget()}
							rel={linkRel()}
							class="flex h-full flex-col items-center justify-center rounded-2xl border border-[var(--color-surface-border)] bg-[var(--color-surface-card)] p-3 text-center transition-all hover:bg-[var(--color-surface-muted)] active:scale-[0.98]"
						>
							<span
								class="mb-2 flex h-11 w-11 items-center justify-center rounded-full bg-[var(--color-primary-50)] text-[var(--color-primary-800)]"
							>
								<Globe size={22} />
							</span>
							<span
								class="line-clamp-1 text-[11px] font-bold tracking-tight text-[var(--color-nav-active)]"
							>
								{link.name}
							</span>
						</a>
						<button
							type="button"
							onclick={() => openEditCustomLink(link)}
							class="absolute right-1.5 top-1.5 flex h-7 w-7 items-center justify-center rounded-full bg-[var(--color-surface-card)]/95 text-[var(--color-nav-inactive)] transition-colors hover:text-[var(--color-nav-active)]"
							aria-label="リンクを編集"
						>
							<Pencil size={14} />
						</button>
					</div>
				{/each}
				<button
					type="button"
					onclick={openAddLink}
					class="flex aspect-square flex-col items-center justify-center rounded-2xl border border-dashed border-[var(--color-surface-border)] bg-[var(--color-surface-card)] p-3 text-center transition-all hover:bg-[var(--color-surface-muted)] active:scale-[0.98]"
				>
					<span
						class="mb-2 flex h-11 w-11 items-center justify-center rounded-full bg-[var(--color-primary-50)] text-[var(--color-primary-800)]"
					>
						<Plus size={22} />
					</span>
					<span class="text-[11px] font-bold tracking-tight text-[var(--color-nav-active)]">
						リンク追加
					</span>
				</button>
			</div>
			<!-- eslint-enable svelte/no-navigation-without-resolve -->

			<!-- Additional 2 wide buttons under grid -->
			<!-- eslint-disable svelte/no-navigation-without-resolve -->
			<div class="grid grid-cols-2 gap-3">
				<!-- 学修要覧 -->
				<a
					href={settings.openLinksInApp
						? resolve(
								`/inapp-browser?url=${encodeURIComponent('https://kyoumu.office.uec.ac.jp/youran/youran.html')}`
							)
						: 'https://kyoumu.office.uec.ac.jp/youran/youran.html'}
					target={settings.openLinksInApp ? undefined : '_blank'}
					rel={settings.openLinksInApp ? undefined : 'noopener noreferrer'}
					class="flex items-center justify-between rounded-xl border border-[var(--color-surface-border)] bg-[var(--color-surface-card)] px-4 py-3.5 transition-all hover:bg-[var(--color-surface-muted)] active:scale-[0.98]"
				>
					<span class="flex items-center gap-2 text-sm font-bold text-[var(--color-nav-active)]">
						<Book size={18} class="text-[var(--color-primary-700)]" />
						学修要覧
					</span>
					<ChevronRight size={16} color="var(--color-nav-inactive)" />
				</a>

				<!-- 時間割 -->
				<a
					href={linkHref('http://kyoumu.office.uec.ac.jp/timet/index.html', true)}
					target="_blank"
					rel="noopener noreferrer"
					class="flex items-center justify-between rounded-xl border border-[var(--color-surface-border)] bg-[var(--color-surface-card)] px-4 py-3.5 transition-all hover:bg-[var(--color-surface-muted)] active:scale-[0.98]"
				>
					<span class="flex items-center gap-2 text-sm font-bold text-[var(--color-nav-active)]">
						<Calendar size={18} class="text-[var(--color-primary-700)]" />
						時間割
					</span>
					<ChevronRight size={16} color="var(--color-nav-inactive)" />
				</a>
			</div>
			<!-- eslint-enable svelte/no-navigation-without-resolve -->
		</div>

		<!-- 便利ツール (Responsive layout: 1 col list on mobile, 3 cols cards on desktop) -->
		<div class="space-y-3">
			<SectionHeader>便利ツール</SectionHeader>
			<div class="grid grid-cols-1 md:grid-cols-3 gap-3">
				<!-- 写真 -> PDF 変換 -->
				<a
					href={resolve('/pdf')}
					class="flex items-center gap-3 rounded-xl border border-[var(--color-surface-border)] bg-[var(--color-surface-card)] p-3 transition-all hover:bg-[var(--color-surface-muted)] active:scale-[0.99] text-left"
				>
					<div
						class="flex h-11 w-11 shrink-0 items-center justify-center rounded-full bg-[var(--color-primary-50)] text-[var(--color-primary-800)]"
					>
						<FileText size={22} />
					</div>
					<div class="flex-1 min-w-0">
						<h4 class="text-sm font-bold text-[var(--color-nav-active)]">写真 → PDF 変換</h4>
						<p class="text-[10px] text-[var(--color-nav-inactive)] truncate mt-0.5">
							複数の写真を 1 つの PDF にまとめて共有
						</p>
					</div>
					<ChevronRight size={16} class="text-[var(--color-nav-inactive)] shrink-0" />
				</a>

				<!-- GPA 計算機 -->
				<a
					href={resolve('/gpa')}
					class="flex items-center gap-3 rounded-xl border border-[var(--color-surface-border)] bg-[var(--color-surface-card)] p-3 text-left transition-all hover:bg-[var(--color-surface-muted)] active:scale-[0.99]"
				>
					<div
						class="flex h-11 w-11 shrink-0 items-center justify-center rounded-full bg-[var(--color-primary-50)] text-[var(--color-primary-800)]"
					>
						<Calculator size={22} />
					</div>
					<div class="min-w-0 flex-1">
						<h4 class="text-sm font-bold text-[var(--color-nav-active)]">GPA 計算機</h4>
						<p class="mt-0.5 truncate text-[10px] text-[var(--color-nav-inactive)]">
							単位数と評価から GPA を計算
						</p>
					</div>
					<ChevronRight size={16} class="shrink-0 text-[var(--color-nav-inactive)]" />
				</a>
			</div>
		</div>
	</div>
</Container>

<Modal
	open={linkModalOpen}
	onclose={() => (linkModalOpen = false)}
	title={editingLinkId ? 'リンクを編集' : 'リンクを追加'}
>
	<div class="space-y-4">
		<TextField bind:value={linkName} label="表示名" placeholder="例: 図書館" />
		<TextField bind:value={linkUrl} type="url" label="URL" placeholder="https://..." />
	</div>

	{#snippet footer()}
		<div class="flex gap-2">
			{#if editingLinkId}
				<Button variant="danger" size="md" onclick={deleteCustomLink} aria-label="削除">
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
