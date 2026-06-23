<script lang="ts">
	import type { Pathname, RouteId } from '$app/types';
	import { resolve } from '$app/paths';
	import { page } from '$app/state';
	import { locales, localizeHref, setLocale } from '$lib/paraglide/runtime';
	import { themeStore, settingsStore, userDataStore, publicDataStore } from '$lib/stores';
	import { initRemoteStorage } from '$lib/remotestorage';
	import { BottomTabBar, SideNav } from '$lib/components';
	import { Home, Calendar, BookOpen, CheckSquare, BarChart3, MoreHorizontal } from '@lucide/svelte';
	import { m } from '$lib/paraglide/messages';
	import { browser, dev } from '$app/environment';
	import { syncNotifications } from '$lib/notifications';
	import './layout.css';

	interface NavLink {
		href: RouteId;
		label: string;
		icon: typeof Home;
	}

	let { children } = $props();

	let currentLocaleSignal = $state('ja');
	let sideNavCollapsed = $state(false);

	// クライアントサイドでストアを初期化
	$effect(() => {
		initRemoteStorage();
		themeStore.init();
		settingsStore.init();
		void Promise.all([userDataStore.init(), publicDataStore.init()]);

		if (browser && 'serviceWorker' in navigator && !dev) {
			navigator.serviceWorker.register('/service-worker.js').catch((err) => {
				console.error('Service worker registration failed:', err);
			});
		}
	});

	// 授業データ・授業時間設定・通知設定の変更を検知してローカル通知を同期
	$effect(() => {
		if (userDataStore.initialized && settingsStore.initialized) {
			// Svelte 5のリアクティブな依存関係を明示
			const courses = userDataStore.courses;
			const settings = settingsStore.settings;
			void syncNotifications(courses, settings);
		}
	});

	// URLの変更を検知して Paraglide のロケールを同期
	$effect(() => {
		// page.url に依存させる
		// eslint-disable-next-line @typescript-eslint/no-unused-expressions
		page.url;
		if (typeof window !== 'undefined') {
			const pathSegments = window.location.pathname.split('/');
			const firstSegment = pathSegments[1];
			const detectedLocale = (locales as readonly string[]).includes(firstSegment)
				? (firstSegment as (typeof locales)[number])
				: 'ja';
			setLocale(detectedLocale, { reload: false });
			currentLocaleSignal = detectedLocale;
		}
	});

	const tabs = $derived.by<NavLink[]>(() => {
		// eslint-disable-next-line @typescript-eslint/no-unused-expressions
		currentLocaleSignal;
		return [
			{ href: '/', label: m.nav_home(), icon: Home },
			{ href: '/timetable', label: m.nav_timetable(), icon: BookOpen },
			{ href: '/todo', label: m.nav_todo(), icon: CheckSquare },
			{ href: '/calendar', label: m.nav_calendar(), icon: Calendar },
			{ href: '/attendance', label: m.nav_attendance(), icon: BarChart3 },
			{ href: '/more', label: m.nav_more(), icon: MoreHorizontal }
		];
	});

	const navItems = $derived(tabs);

	const fontFamilyStyle = $derived.by(() => {
		const font = settingsStore.settings.fontFamily;
		if (font === 'outfit') {
			return "font-family: 'Outfit', system-ui, -apple-system, sans-serif;";
		} else if (font === 'sawarabi') {
			return "font-family: 'Sawarabi Gothic', sans-serif;";
		} else if (font === 'noto') {
			return "font-family: 'Noto Sans JP', sans-serif;";
		}
		return "font-family: system-ui, -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, Oxygen, Ubuntu, Cantarell, 'Open Sans', 'Helvetica Neue', sans-serif;";
	});

	let osDark = $state(false);

	$effect(() => {
		if (typeof window === 'undefined') return;
		const mediaQuery = window.matchMedia('(prefers-color-scheme: dark)');
		osDark = mediaQuery.matches;

		const handler = (e: MediaQueryListEvent) => {
			osDark = e.matches;
		};
		mediaQuery.addEventListener('change', handler);
		return () => mediaQuery.removeEventListener('change', handler);
	});

	$effect(() => {
		const themeMode = settingsStore.settings.theme; // 'light' | 'dark' | 'system'
		const themePackIdLight = settingsStore.settings.themePackIdLight || 'default';
		const themePackIdDark = settingsStore.settings.themePackIdDark || 'dark';

		const isDark = themeMode === 'dark' ? true : themeMode === 'system' ? osDark : false;

		const targetPackId =
			themeMode === 'dark'
				? themePackIdDark
				: themeMode === 'system'
					? osDark
						? themePackIdDark
						: themePackIdLight
					: themePackIdLight;

		themeStore.setById(targetPackId);

		if (typeof document !== 'undefined') {
			if (isDark) {
				document.documentElement.classList.add('dark');
			} else {
				document.documentElement.classList.remove('dark');
			}
		}
	});
</script>

{#key currentLocaleSignal}
	<div
		class="min-h-screen bg-[var(--color-surface-page)] text-[var(--color-nav-active)]"
		style={fontFamilyStyle +
			(settingsStore.settings.timetableBgImage && settingsStore.settings.bgImageAllPages
				? ` background-image: url(${settingsStore.settings.timetableBgImage}); background-size: cover; background-position: center; background-attachment: fixed;`
				: '')}
	>
		<div class="flex">
			<SideNav
				items={navItems}
				collapsed={sideNavCollapsed}
				ontoggle={() => (sideNavCollapsed = !sideNavCollapsed)}
			/>

			<main class="min-w-0 flex-1 pb-20 md:pb-0">
				{@render children()}
			</main>
		</div>

		<BottomTabBar {tabs} />
	</div>
{/key}

<div style="display:none">
	{#each locales as locale (locale)}
		<a href={resolve(localizeHref(page.url.pathname, { locale }) as Pathname)}>{locale}</a>
	{/each}
</div>
