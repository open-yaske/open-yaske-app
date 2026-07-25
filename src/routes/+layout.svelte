<script lang="ts">
	import type { Pathname, RouteId } from '$app/types';
	import { resolve } from '$app/paths';
	import { page } from '$app/state';
	import { locales, localizeHref, setLocale } from '$lib/paraglide/runtime';
	import {
		themeStore,
		settingsStore,
		userDataStore,
		publicDataStore,
		transitStore
	} from '$lib/stores';
	import { initRemoteStorage } from '$lib/remotestorage';
	import { BottomTabBar, SideNav } from '$lib/components';
	import { Home, Calendar, BookOpen, CheckSquare, BarChart3, MoreHorizontal } from '@lucide/svelte';
	import { m } from '$lib/paraglide/messages';
	import { APP_FEATURES, getFeatureName } from '$lib/features';
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

	// 天気の位置情報（settings.lat/lng）の変更を検知して、運行情報の最寄り駅を自動更新
	$effect(() => {
		if (settingsStore.initialized) {
			const lat = settingsStore.settings.lat;
			const lng = settingsStore.settings.lng;
			const lastLat = settingsStore.settings.transitLastLat;
			const lastLng = settingsStore.settings.transitLastLng;

			// 初回設定または位置情報が前回の割り出し位置から0.0001度（約10m）以上動いた場合
			const diff = Math.abs(lat - lastLat) + Math.abs(lng - lastLng);
			if (diff > 0.0001 || !settingsStore.settings.transitStationId) {
				void (async () => {
					try {
						const res = await fetch(
							`https://api.transit.ls8h.com/api/v1/places/reverse?lat=${lat}&lon=${lng}&radiusMeters=500&limit=10`
						);
						if (res.ok) {
							const data = await res.json();
							const stations = (data.places || []).filter(
								(p: any) =>
									(p.kind === 'station' || p.kind === 'stop') &&
									!p.id.startsWith('transit:') &&
									!p.id.startsWith('osm:') &&
									!p.id.startsWith('geocoder:')
							);
							if (stations.length > 0) {
								const nearest = stations[0];
								await transitStore.setStation(nearest.id, nearest.name, lat, lng);
							}
						}
					} catch (err) {
						console.error('Failed to auto-update transit station on location change:', err);
					}
				})();
			}
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
			const fallbackLocale = settingsStore.settings.locale || 'ja';
			const detectedLocale = (locales as readonly string[]).includes(firstSegment)
				? (firstSegment as (typeof locales)[number])
				: fallbackLocale;
			setLocale(detectedLocale, { reload: false });
			currentLocaleSignal = detectedLocale;
		}
	});

	const tabs = $derived.by<NavLink[]>(() => {
		// eslint-disable-next-line @typescript-eslint/no-unused-expressions
		currentLocaleSignal;

		const barFeatures = settingsStore.settings.barFeatures || [
			'dashboard',
			'timetable',
			'todo',
			'calendar',
			'attendance'
		];
		const links: NavLink[] = [];

		for (const fid of barFeatures) {
			const f = APP_FEATURES.find((item) => item.id === fid);
			if (f) {
				links.push({
					href: localizeHref(f.href) as RouteId,
					label: getFeatureName(f.id),
					icon: f.icon as any
				});
			}
		}

		// 末尾に必ず「その他」を固定
		links.push({
			href: localizeHref('/more') as RouteId,
			label: m.nav_more(),
			icon: MoreHorizontal
		});

		return links;
	});

	const navItems = $derived(tabs);

	const fontFamilyStyle = $derived.by(() => {
		const font = settingsStore.settings.fontFamily;
		const locale = currentLocaleSignal;

		// 1) サンセリフ (ゴシック体) および Noto (互換用)
		if (font === 'sans' || font === 'noto') {
			if (locale === 'zh') {
				return "font-family: 'Noto Sans SC', 'PingFang SC', 'Microsoft YaHei', sans-serif;";
			} else if (locale === 'ko') {
				return "font-family: 'Noto Sans KR', 'Malgun Gothic', 'Apple SD Gothic Neo', sans-serif;";
			} else if (locale === 'ja') {
				return "font-family: 'Noto Sans JP', sans-serif;";
			}
			return "font-family: 'Noto Sans', system-ui, -apple-system, sans-serif;";
		}

		// 2) セリフ (明朝体)
		if (font === 'serif') {
			if (locale === 'zh') {
				return "font-family: 'Noto Serif SC', 'Songti SC', 'SimSun', serif;";
			} else if (locale === 'ko') {
				return "font-family: 'Noto Serif KR', 'Batang', 'AppleMyungjo', serif;";
			} else if (locale === 'ja') {
				return "font-family: 'Noto Serif JP', 'BIZ UDMincho', serif;";
			}
			return "font-family: 'Noto Serif', Georgia, 'Times New Roman', serif;";
		}

		// 3) さわらびゴシック (CJKフォールバック含む)
		if (font === 'sawarabi') {
			if (locale === 'zh') {
				return "font-family: 'Sawarabi Gothic', 'Noto Sans SC', 'PingFang SC', sans-serif;";
			} else if (locale === 'ko') {
				return "font-family: 'Sawarabi Gothic', 'Noto Sans KR', 'Malgun Gothic', sans-serif;";
			}
			return "font-family: 'Sawarabi Gothic', 'Noto Sans JP', sans-serif;";
		}

		// 4) Outfit (欧文メイン + 多言語フォールバック)
		if (font === 'outfit') {
			if (locale === 'zh') {
				return "font-family: 'Outfit', 'Noto Sans SC', 'PingFang SC', 'Microsoft YaHei', sans-serif;";
			} else if (locale === 'ko') {
				return "font-family: 'Outfit', 'Noto Sans KR', 'Malgun Gothic', 'Apple SD Gothic Neo', sans-serif;";
			} else if (locale === 'ja') {
				return "font-family: 'Outfit', 'Noto Sans JP', sans-serif;";
			}
			return "font-family: 'Outfit', system-ui, -apple-system, sans-serif;";
		}

		// 5) デフォルト / システムフォント
		if (locale === 'zh') {
			return "font-family: -apple-system, BlinkMacSystemFont, 'SF Pro Text', 'PingFang SC', 'Microsoft YaHei', sans-serif;";
		} else if (locale === 'ko') {
			return "font-family: -apple-system, BlinkMacSystemFont, 'SF Pro Text', 'Apple SD Gothic Neo', 'Malgun Gothic', sans-serif;";
		}
		return "font-family: -apple-system, BlinkMacSystemFont, 'SF Pro Display', 'SF Pro Text', 'Helvetica Neue', sans-serif;";
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
				appName={settingsStore.settings.customAppName}
				logoData={settingsStore.settings.customLogoData}
			/>

			<main class="min-w-0 flex-1 pb-24 md:pb-6">
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
