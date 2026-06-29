<script lang="ts">
	import { goto } from '$app/navigation';
	import { resolve } from '$app/paths';
	import { page } from '$app/state';
	import { localizeHref } from '$lib/paraglide/runtime';
	import PageHeader from '$lib/components/PageHeader.svelte';
	import Container from '$lib/components/Container.svelte';
	import TextField from '$lib/components/TextField.svelte';
	import Switch from '$lib/components/Switch.svelte';
	import Slider from '$lib/components/Slider.svelte';
	import Button from '$lib/components/Button.svelte';
	import Modal from '$lib/components/Modal.svelte';
	import SettingsSection from '$lib/components/settings/SettingsSection.svelte';
	import { settingsStore, themeStore, uiStore, userDataStore } from '$lib/stores';
	import { BUILTIN_PACKS, getLocalizedText } from '$lib/theme';
	import type { ThemePack } from '$lib/theme';
	import {
		connectRemoteStorage,
		disconnectRemoteStorage,
		getConnectedUserAddress
	} from '$lib/remotestorage';
	import type { Period, SearchProviderId, UserSettings } from '$lib/types';
	import { m } from '$lib/paraglide/messages';
	import {
		Download,
		Upload,
		Trash2,
		Cloud,
		CloudOff,
		Info,
		Link2,
		ChevronUp,
		ChevronDown,
		Plus,
		X,
		Search,
		GripVertical,
		Palette,
		ChevronRight
	} from '@lucide/svelte';
	import { Capacitor } from '@capacitor/core';
	import { APP_FEATURES, getFeatureName } from '$lib/features';

	import { PUBLIC_APP_NAME, SOURCE_URL } from '$lib/constants';

	const appVersion = '2026.06.28';

	// ----- テーマパック名解決 -----
	function getPackName(pack: ThemePack): string {
		return getLocalizedText(pack.name, settingsStore.settings.locale);
	}

	const themePackOptions = $derived([
		...BUILTIN_PACKS.map((p) => ({ value: p.id, label: getPackName(p) })),
		...themeStore.customPacks.map((p) => ({ value: p.id, label: getPackName(p) }))
	]);

	// ----- セグメントコントロール -----
	type SegmentOption = { value: string; label: string };

	function handleSegmentSelect(key: keyof UserSettings, value: string) {
		settingsStore.update({ [key]: value } as Partial<UserSettings>);
		if (key === 'locale') {
			const newHref = localizeHref(page.url.pathname, {
				locale: value as 'ja' | 'en' | 'zh' | 'ko'
			});
			// eslint-disable-next-line svelte/no-navigation-without-resolve
			void goto(newHref);
		}
	}

	// ----- データクリア確認 -----
	let showClearConfirm = $state(false);

	function handleClearData() {
		settingsStore.reset();
		showClearConfirm = false;
		uiStore.toast(m.toast_deleted(), 'success');
	}

	// ----- RemoteStorage -----
	let showConnectModal = $state(false);
	let connectAddress = $state('');

	function handleConnect() {
		showConnectModal = true;
	}

	function submitConnect() {
		if (!connectAddress.trim()) return;
		connectRemoteStorage(connectAddress.trim());
		showConnectModal = false;
		connectAddress = '';
	}

	function handleDisconnect() {
		disconnectRemoteStorage();
	}

	// ----- エクスポート/インポート (stub) -----
	function handleExport() {
		const data = JSON.stringify(settingsStore.settings, null, 2);
		const blob = new Blob([data], { type: 'application/json' });
		const url = URL.createObjectURL(blob);
		const a = document.createElement('a');
		a.href = url;
		a.download = `${PUBLIC_APP_NAME.toLowerCase()}-settings.json`;
		a.click();
		URL.revokeObjectURL(url);
		uiStore.toast(m.toast_saved(), 'success');
	}

	function handleImport(e: Event) {
		const input = e.currentTarget as HTMLInputElement;
		const file = input.files?.[0];
		if (!file) return;
		file.text().then((text) => {
			try {
				const parsed = JSON.parse(text);
				settingsStore.update(parsed);
				uiStore.toast(m.toast_saved(), 'success');
			} catch {
				uiStore.toast(m.toast_error(), 'error');
			}
		});
		input.value = '';
	}

	function handleLogoUpload(e: Event) {
		const input = e.currentTarget as HTMLInputElement;
		const file = input.files?.[0];
		if (!file) return;

		const reader = new FileReader();
		reader.onload = (event) => {
			const img = new Image();
			img.onload = () => {
				const canvas = document.createElement('canvas');
				const ctx = canvas.getContext('2d');
				if (!ctx) return;

				const size = 128;
				canvas.width = size;
				canvas.height = size;

				const minDim = Math.min(img.width, img.height);
				const sx = (img.width - minDim) / 2;
				const sy = (img.height - minDim) / 2;

				ctx.drawImage(img, sx, sy, minDim, minDim, 0, 0, size, size);

				try {
					const compressedData = canvas.toDataURL('image/jpeg', 0.7);
					settingsStore.update({ customLogoData: compressedData });
					uiStore.toast(m.toast_saved(), 'success');
				} catch (error) {
					console.error('Failed to compress image:', error);
					uiStore.toast(m.toast_error(), 'error');
				}
			};
			img.src = event.target?.result as string;
		};
		reader.readAsDataURL(file);
		input.value = '';
	}

	function handleLogoReset() {
		settingsStore.update({ customLogoData: '' });
		uiStore.toast(m.toast_saved(), 'success');
	}

	type SearchItem = {
		category: string;
		label: string;
		sectionId: string;
	};

	// ----- 検索インデックス -----
	const searchItems = $derived<SearchItem[]>([
		{
			category: m.settings_locale(),
			label: m.settings_locale_ja(),
			sectionId: 'settings-language'
		},
		{
			category: m.settings_locale(),
			label: m.settings_locale_en(),
			sectionId: 'settings-language'
		},
		{
			category: m.settings_locale(),
			label: m.settings_locale_zh(),
			sectionId: 'settings-language'
		},
		{
			category: m.settings_locale(),
			label: m.settings_locale_ko(),
			sectionId: 'settings-language'
		},
		{ category: m.settings_theme(), label: m.settings_theme_pack(), sectionId: 'settings-theme' },
		{
			category: m.settings_theme(),
			label: m.settings_theme_mode(),
			sectionId: 'settings-appearance'
		},
		{
			category: m.settings_theme(),
			label: m.settings_theme_light(),
			sectionId: 'settings-appearance'
		},
		{
			category: m.settings_theme(),
			label: m.settings_theme_dark(),
			sectionId: 'settings-appearance'
		},
		{
			category: m.settings_theme(),
			label: m.settings_theme_system(),
			sectionId: 'settings-appearance'
		},
		{
			category: m.settings_theme(),
			label: m.settings_emoji_style(),
			sectionId: 'settings-appearance'
		},
		{
			category: m.settings_theme(),
			label: m.settings_emoji_twemoji(),
			sectionId: 'settings-appearance'
		},
		{
			category: m.settings_theme(),
			label: m.settings_font_family(),
			sectionId: 'settings-appearance'
		},
		{
			category: m.settings_semester(),
			label: m.settings_semester_spring(),
			sectionId: 'settings-semester'
		},
		{
			category: m.settings_semester(),
			label: m.settings_semester_fall(),
			sectionId: 'settings-semester'
		},
		{ category: m.settings_semester(), label: m.settings_year(), sectionId: 'settings-semester' },
		{
			category: m.settings_location(),
			label: m.settings_location(),
			sectionId: 'settings-location'
		},
		{ category: m.settings_home(), label: m.settings_home_weather(), sectionId: 'settings-home' },
		{ category: m.settings_home(), label: m.settings_home_tools(), sectionId: 'settings-home' },
		{ category: m.settings_home(), label: '検索エンジン', sectionId: 'settings-home' },
		{
			category: m.settings_home(),
			label: m.settings_home_external_links(),
			sectionId: 'settings-home'
		},
		{
			category: m.settings_home(),
			label: m.settings_open_links_in_app(),
			sectionId: 'settings-home'
		},
		{
			category: m.settings_timetable(),
			label: m.settings_timetable_day_range(),
			sectionId: 'settings-timetable'
		},
		{
			category: m.settings_timetable(),
			label: m.settings_timetable_period_range(),
			sectionId: 'settings-timetable'
		},
		{
			category: m.settings_timetable(),
			label: m.settings_timetable_ondemand_cols(),
			sectionId: 'settings-timetable'
		},
		{
			category: m.settings_timetable(),
			label: m.settings_timetable_transparent(),
			sectionId: 'settings-timetable'
		},
		{
			category: m.settings_timetable(),
			label: m.settings_timetable_bg_image(),
			sectionId: 'settings-timetable'
		},
		{
			category: m.settings_calendar(),
			label: m.settings_calendar_week_start(),
			sectionId: 'settings-calendar'
		},
		{
			category: m.settings_calendar(),
			label: m.settings_calendar_show_academic(),
			sectionId: 'settings-calendar'
		},
		{
			category: m.settings_calendar(),
			label: m.settings_calendar_show_holidays(),
			sectionId: 'settings-calendar'
		},
		{
			category: m.settings_calendar(),
			label: m.settings_calendar_show_external(),
			sectionId: 'settings-calendar'
		},
		{
			category: m.settings_calendar(),
			label: m.settings_calendar_show_timetable(),
			sectionId: 'settings-calendar'
		},
		{
			category: m.settings_calendar(),
			label: m.settings_calendar_density(),
			sectionId: 'settings-calendar'
		},
		{
			category: m.settings_calendar(),
			label: m.settings_calendar_auto_compact(),
			sectionId: 'settings-calendar'
		},
		{
			category: m.settings_attendance(),
			label: m.settings_attendance_max_absences(),
			sectionId: 'settings-attendance'
		},
		{ category: m.settings_data(), label: m.settings_data_export(), sectionId: 'settings-data' },
		{ category: m.settings_data(), label: m.settings_data_import(), sectionId: 'settings-data' },
		{ category: m.settings_data(), label: m.settings_data_clear(), sectionId: 'settings-data' },
		{
			category: m.settings_account(),
			label: m.settings_account_connect(),
			sectionId: 'settings-account'
		},
		{
			category: m.settings_account(),
			label: m.settings_account_disconnect(),
			sectionId: 'settings-account'
		},
		{
			category: m.settings_about(),
			label: m.settings_about_version(),
			sectionId: 'settings-about'
		},
		{ category: m.settings_about(), label: m.settings_about_source(), sectionId: 'settings-about' }
	]);

	// ----- セグメントコントロール汎用 -----
	const localeOptions: SegmentOption[] = [
		{ value: 'ja', label: m.settings_locale_ja() },
		{ value: 'en', label: m.settings_locale_en() },
		{ value: 'zh', label: m.settings_locale_zh() },
		{ value: 'ko', label: m.settings_locale_ko() }
	];

	const themeModeOptions: SegmentOption[] = [
		{ value: 'light', label: m.settings_theme_light() },
		{ value: 'dark', label: m.settings_theme_dark() },
		{ value: 'system', label: m.settings_theme_system() }
	];

	const semesterOptions: SegmentOption[] = [
		{ value: 'spring', label: m.settings_semester_spring() },
		{ value: 'fall', label: m.settings_semester_fall() },
		{ value: 'other', label: m.category_other() }
	];

	const yearOptions = Array.from({ length: 11 }, (_, i) => {
		const year = String(2020 + i);
		return { value: year, label: year };
	});

	const weekStartOptions: SegmentOption[] = [
		{ value: 'sun', label: m.day_long_sun() },
		{ value: 'mon', label: m.day_long_mon() }
	];

	const timetableDayOptions = [
		{ value: '5', label: '月〜金' },
		{ value: '6', label: '月〜土' },
		{ value: '7', label: '月〜日' }
	];

	const timetablePeriodOptions = [
		{ value: '4', label: '4' },
		{ value: '5', label: '5' },
		{ value: '6', label: '6' },
		{ value: '7', label: '7' }
	];

	const timetableOndemandOptions = [
		{ value: '0', label: '0' },
		{ value: '1', label: '1' },
		{ value: '2', label: '2' },
		{ value: '3', label: '3' }
	];

	const emojiStyleOptions: SegmentOption[] = [
		{ value: 'native', label: m.settings_emoji_native() },
		{ value: 'twemoji', label: m.settings_emoji_twemoji() }
	];

	const fontFamilyOptions = [
		{ value: 'system', label: m.settings_font_system() },
		{ value: 'outfit', label: 'Outfit (Latin)' },
		{ value: 'sans', label: m.settings_font_sans() },
		{ value: 'serif', label: m.settings_font_serif() }
	];

	const searchProviderOptions: { value: SearchProviderId; label: string }[] = [
		{ value: 'google', label: 'Google' },
		{ value: 'bing', label: 'Bing' },
		{ value: 'duckduckgo', label: 'DuckDuckGo' },
		{ value: 'chatgpt', label: 'ChatGPT' },
		{ value: 'claude', label: 'Claude' },
		{ value: 'grok', label: 'Grok' },
		{ value: 'perplexity', label: 'Perplexity' }
	];

	function handleBgImageUpload(e: Event) {
		const input = e.currentTarget as HTMLInputElement;
		const file = input.files?.[0];
		if (!file) return;

		const reader = new FileReader();
		reader.onload = () => {
			if (typeof reader.result === 'string') {
				settingsStore.update({ timetableBgImage: reader.result });
				uiStore.toast('背景画像を適用しました', 'success');
			}
		};
		reader.onerror = () => {
			uiStore.toast('画像の読み込みに失敗しました', 'error');
		};
		reader.readAsDataURL(file);
		input.value = '';
	}

	function handleBgImageClear() {
		settingsStore.update({ timetableBgImage: undefined });
		uiStore.toast('背景画像を削除しました', 'success');
	}

	// ----- 機能配置制御用ヘルパー -----
	function toggleBarFeature(id: string, checked: boolean) {
		const current = [...(settingsStore.settings.barFeatures || [])];
		if (checked) {
			if (current.length >= 5) {
				uiStore.toast('バーに配置できるのは最大5つまでです。', 'error');
				return;
			}
			if (!current.includes(id)) {
				current.push(id);
			}
		} else {
			const index = current.indexOf(id);
			if (index >= 0) {
				current.splice(index, 1);
			}
		}
		settingsStore.update({ barFeatures: current });
	}

	function moveBarFeature(id: string, direction: -1 | 1) {
		const current = [...(settingsStore.settings.barFeatures || [])];
		const index = current.indexOf(id);
		if (index < 0) return;
		const nextIndex = index + direction;
		if (nextIndex < 0 || nextIndex >= current.length) return;

		const temp = current[index];
		current[index] = current[nextIndex];
		current[nextIndex] = temp;

		settingsStore.update({ barFeatures: current });
	}

	function toggleHomeFeature(id: string, checked: boolean) {
		let current = [...(settingsStore.settings.homeFeatures || [])];
		if (checked) {
			if (!current.includes(id)) {
				current.push(id);
			}
		} else {
			current = current.filter((fid) => fid !== id);
		}
		settingsStore.update({ homeFeatures: current });
	}

	function moveHomeFeature(id: string, direction: -1 | 1) {
		const current = [...(settingsStore.settings.homeFeatures || [])];
		const index = current.indexOf(id);
		if (index < 0) return;
		const nextIndex = index + direction;
		if (nextIndex < 0 || nextIndex >= current.length) return;

		const temp = current[index];
		current[index] = current[nextIndex];
		current[nextIndex] = temp;

		settingsStore.update({ homeFeatures: current });
	}

	function toggleHomeLayout(id: string) {
		const currentLayouts = { ...(settingsStore.settings.homeFeatureLayouts || {}) };
		const current = currentLayouts[id] || 'button';
		currentLayouts[id] = current === 'widget' ? 'button' : 'widget';
		settingsStore.update({ homeFeatureLayouts: currentLayouts });
	}

	let tempBarFeatures = $state<string[]>([]);
	let tempHomeFeatures = $state<string[]>([]);

	$effect(() => {
		tempBarFeatures = [...(settingsStore.settings.barFeatures || [])];
	});
	$effect(() => {
		tempHomeFeatures = [...(settingsStore.settings.homeFeatures || [])];
	});

	const unselectedBar = $derived(APP_FEATURES.filter((f) => !tempBarFeatures.includes(f.id)));
	const unselectedHome = $derived(
		APP_FEATURES.filter((f) => f.id !== 'dashboard' && !tempHomeFeatures.includes(f.id))
	);

	let draggingBarIndex = $state<number | null>(null);
	let draggingHomeIndex = $state<number | null>(null);

	function handleBarDragStart(index: number) {
		draggingBarIndex = index;
	}

	function handleBarDragOver(e: DragEvent, index: number) {
		e.preventDefault();
		if (draggingBarIndex === null || draggingBarIndex === index) return;
		const current = [...tempBarFeatures];
		const item = current.splice(draggingBarIndex, 1)[0];
		current.splice(index, 0, item);
		tempBarFeatures = current;
		draggingBarIndex = index;
	}

	function handleBarDragEnd() {
		draggingBarIndex = null;
		settingsStore.update({ barFeatures: tempBarFeatures });
	}

	function handleHomeDragStart(index: number) {
		draggingHomeIndex = index;
	}

	function handleHomeDragOver(e: DragEvent, index: number) {
		e.preventDefault();
		if (draggingHomeIndex === null || draggingHomeIndex === index) return;
		const current = [...tempHomeFeatures];
		const item = current.splice(draggingHomeIndex, 1)[0];
		current.splice(index, 0, item);
		tempHomeFeatures = current;
		draggingHomeIndex = index;
	}

	function handleHomeDragEnd() {
		draggingHomeIndex = null;
		settingsStore.update({ homeFeatures: tempHomeFeatures });
	}

	let searchQuery = $state('');

	const visibleSections = $derived.by(() => {
		const q = searchQuery.trim().toLowerCase();
		if (!q) {
			return {
				'settings-language': true,
				'settings-theme': true,
				'settings-appearance': true,
				'settings-customization': true,
				'settings-semester': true,
				'settings-location': true,
				'settings-home': true,
				'settings-timetable': true,
				'settings-calendar': true,
				'settings-attendance': true,
				'settings-features': true,
				'settings-widget': true,
				'settings-about': true
			};
		}

		const matchedIds = new Set<string>();
		for (const item of searchItems) {
			const fullText = `${item.category} ${item.label}`.toLowerCase();
			if (fullText.includes(q)) {
				matchedIds.add(item.sectionId);
			}
		}

		return {
			'settings-language': matchedIds.has('settings-language'),
			'settings-theme': matchedIds.has('settings-theme'),
			'settings-appearance': matchedIds.has('settings-appearance'),
			'settings-customization': matchedIds.has('settings-customization'),
			'settings-semester': matchedIds.has('settings-semester'),
			'settings-location': matchedIds.has('settings-location'),
			'settings-home': matchedIds.has('settings-home'),
			'settings-timetable': matchedIds.has('settings-timetable'),
			'settings-calendar': matchedIds.has('settings-calendar'),
			'settings-attendance': matchedIds.has('settings-attendance'),
			'settings-features': matchedIds.has('settings-features'),
			'settings-widget': matchedIds.has('settings-widget'),
			'settings-about': matchedIds.has('settings-about')
		};
	});
</script>

<svelte:head><title>{m.settings_title()} | OpenYASKE</title></svelte:head>

<PageHeader title={m.settings_title()} />

<Container size="narrow" class="space-y-6 py-4">
	<!-- ===== Inline Search ===== -->
	<div class="px-1">
		<TextField bind:value={searchQuery} placeholder={m.action_search()} leadingIcon={Search} />
	</div>

	<!-- ===== Language ===== -->
	<SettingsSection
		id="settings-language"
		title={m.settings_locale()}
		class={visibleSections['settings-language'] ? '' : 'hidden'}
	>
		<div class="px-4 py-3">
			<div class="grid grid-cols-4 gap-2">
				{#each localeOptions as opt (opt.value)}
					<button
						type="button"
						class="rounded-chip border px-2 py-2 text-center text-sm transition-colors {settingsStore
							.settings.locale === opt.value
							? 'border-[var(--color-primary-500)] bg-[var(--color-primary-50)] text-[var(--color-primary-600)]'
							: 'border-[var(--color-surface-border)] text-[var(--color-nav-active)] hover:bg-[var(--color-surface-muted)]'}"
						onclick={() => handleSegmentSelect('locale', opt.value)}
					>
						{opt.label}
					</button>
				{/each}
			</div>
		</div>
	</SettingsSection>

	<!-- ===== Theme Pack ===== -->
	<SettingsSection
		id="settings-theme"
		title={m.settings_theme_pack()}
		class={visibleSections['settings-theme'] ? '' : 'hidden'}
	>
		<div
			class="flex items-center justify-between border-b border-[var(--color-surface-border)] px-4 py-3.5"
		>
			<span class="text-sm font-medium text-[var(--color-nav-active)]">
				{m.settings_theme_light()}
			</span>
			<label class="relative inline-flex items-center">
				<select
					id="settings-theme-pack-light"
					value={settingsStore.settings.themePackIdLight || 'default'}
					onchange={(e) => {
						const target = e.currentTarget as HTMLSelectElement;
						settingsStore.update({ themePackIdLight: target.value });
					}}
					class="h-8 min-w-[7.5rem] cursor-pointer appearance-none rounded-chip border border-[var(--color-surface-border)] bg-[var(--color-surface-card)] py-1 pl-3 pr-8 text-right text-xs font-bold text-[var(--color-nav-active)] outline-none transition-colors hover:bg-[var(--color-surface-muted)] focus:border-[var(--color-primary-500)] focus:ring-2 focus:ring-[var(--color-primary-500)]/25"
					style="-webkit-appearance: none; -moz-appearance: none; appearance: none;"
				>
					{#each themePackOptions as opt}
						<option value={opt.value}>{opt.label}</option>
					{/each}
				</select>
				<span
					class="pointer-events-none absolute right-2.5 top-1/2 flex -translate-y-1/2 text-[var(--color-nav-inactive)]"
				>
					<ChevronDown size={12} />
				</span>
			</label>
		</div>
		<div class="flex items-center justify-between px-4 py-3.5">
			<span class="text-sm font-medium text-[var(--color-nav-active)]">
				{m.settings_theme_dark()}
			</span>
			<label class="relative inline-flex items-center">
				<select
					id="settings-theme-pack-dark"
					value={settingsStore.settings.themePackIdDark || 'dark'}
					onchange={(e) => {
						const target = e.currentTarget as HTMLSelectElement;
						settingsStore.update({ themePackIdDark: target.value });
					}}
					class="h-8 min-w-[7.5rem] cursor-pointer appearance-none rounded-chip border border-[var(--color-surface-border)] bg-[var(--color-surface-card)] py-1 pl-3 pr-8 text-right text-xs font-bold text-[var(--color-nav-active)] outline-none transition-colors hover:bg-[var(--color-surface-muted)] focus:border-[var(--color-primary-500)] focus:ring-2 focus:ring-[var(--color-primary-500)]/25"
					style="-webkit-appearance: none; -moz-appearance: none; appearance: none;"
				>
					{#each themePackOptions as opt}
						<option value={opt.value}>{opt.label}</option>
					{/each}
				</select>
				<span
					class="pointer-events-none absolute right-2.5 top-1/2 flex -translate-y-1/2 text-[var(--color-nav-inactive)]"
				>
					<ChevronDown size={12} />
				</span>
			</label>
		</div>
		<a
			href={resolve(localizeHref('/theme-editor'))}
			class="flex items-center justify-between border-t border-[var(--color-surface-border)] px-4 py-3.5 text-sm text-[var(--color-nav-active)] transition-colors hover:bg-[var(--color-surface-muted)]"
		>
			<span class="flex items-center gap-2 font-medium text-[var(--color-nav-active)]">
				<Palette size={14} class="text-[var(--color-nav-inactive)]" />
				{m.theme_editor_title()}
			</span>
			<ChevronRight size={14} class="text-[var(--color-nav-inactive)]" />
		</a>
	</SettingsSection>

	<!-- ===== Appearance ===== -->
	<SettingsSection
		id="settings-appearance"
		title={m.settings_theme()}
		class={visibleSections['settings-appearance'] ? '' : 'hidden'}
	>
		<!-- テーマ選択 -->
		<div class="border-b border-[var(--color-surface-border)] px-4 py-3.5 space-y-2">
			<div class="text-sm font-medium text-[var(--color-nav-active)]">
				{m.settings_theme_mode()}
			</div>
			<div class="grid grid-cols-3 gap-2">
				{#each themeModeOptions as opt (opt.value)}
					<button
						type="button"
						class="rounded-chip border px-2 py-2 text-center text-sm transition-colors {settingsStore
							.settings.theme === opt.value
							? 'border-[var(--color-primary-500)] bg-[var(--color-primary-50)] text-[var(--color-primary-600)]'
							: 'border-[var(--color-surface-border)] text-[var(--color-nav-active)] hover:bg-[var(--color-surface-muted)]'}"
						onclick={() => handleSegmentSelect('theme', opt.value)}
					>
						{opt.label}
					</button>
				{/each}
			</div>
		</div>

		<!-- 絵文字スタイル -->
		<div class="border-b border-[var(--color-surface-border)] px-4 py-3.5 space-y-2">
			<div class="text-sm font-medium text-[var(--color-nav-active)]">
				{m.settings_emoji_style()}
			</div>
			<div class="grid grid-cols-2 gap-2">
				{#each emojiStyleOptions as opt (opt.value)}
					<button
						type="button"
						class="rounded-chip border px-2 py-2 text-center text-sm transition-colors {settingsStore
							.settings.emojiStyle === opt.value
							? 'border-[var(--color-primary-500)] bg-[var(--color-primary-50)] text-[var(--color-primary-600)]'
							: 'border-[var(--color-surface-border)] text-[var(--color-nav-active)] hover:bg-[var(--color-surface-muted)]'}"
						onclick={() => handleSegmentSelect('emojiStyle', opt.value)}
					>
						{opt.label}
					</button>
				{/each}
			</div>
		</div>

		<!-- フォントファミリー -->
		<div class="flex items-center justify-between px-4 py-3.5">
			<span class="text-sm font-medium text-[var(--color-nav-active)]">
				{m.settings_font_family()}
			</span>
			<label class="relative inline-flex items-center">
				<select
					value={settingsStore.settings.fontFamily}
					onchange={(e) => {
						const target = e.currentTarget as HTMLSelectElement;
						settingsStore.update({ fontFamily: target.value as UserSettings['fontFamily'] });
					}}
					class="h-8 min-w-[7.5rem] cursor-pointer appearance-none rounded-chip border border-[var(--color-surface-border)] bg-[var(--color-surface-card)] py-1 pl-3 pr-8 text-right text-xs font-bold text-[var(--color-nav-active)] outline-none transition-colors hover:bg-[var(--color-surface-muted)] focus:border-[var(--color-primary-500)] focus:ring-2 focus:ring-[var(--color-primary-500)]/25"
					style="-webkit-appearance: none; -moz-appearance: none; appearance: none;"
				>
					{#each fontFamilyOptions as opt}
						<option value={opt.value}>{opt.label}</option>
					{/each}
				</select>
				<span
					class="pointer-events-none absolute right-2.5 top-1/2 flex -translate-y-1/2 text-[var(--color-nav-inactive)]"
				>
					<ChevronDown size={12} />
				</span>
			</label>
		</div>
	</SettingsSection>

	<!-- ===== App Customization ===== -->
	<SettingsSection
		id="settings-customization"
		title={m.settings_customization_title()}
		class={visibleSections['settings-customization'] ? '' : 'hidden'}
	>
		<div class="border-b border-[var(--color-surface-border)] px-4 py-3">
			<TextField
				type="text"
				label={m.settings_custom_app_name()}
				placeholder={m.settings_custom_app_name_placeholder()}
				value={settingsStore.settings.customAppName || ''}
				oninput={(e) => {
					const target = e.currentTarget as HTMLInputElement;
					settingsStore.update({ customAppName: target.value });
				}}
			/>
		</div>

		<div class="px-4 py-3 space-y-3">
			<div class="flex flex-col gap-1">
				<span class="text-sm font-medium text-[var(--color-nav-active)]">
					{m.settings_custom_logo()}
				</span>
				<span class="text-xs text-[var(--color-nav-inactive)]">
					{m.settings_custom_logo_desc()}
				</span>
			</div>

			<div class="flex items-center gap-4">
				<div
					class="relative h-16 w-16 overflow-hidden rounded-md border border-[var(--color-surface-border)] bg-[var(--color-surface-muted)] flex items-center justify-center"
				>
					<img
						src={settingsStore.settings.customLogoData || '/favicon.png'}
						alt="App Logo"
						class="h-full w-full object-cover"
					/>
				</div>
				<div class="flex flex-col gap-2">
					<div class="flex items-center gap-2">
						<label class="cursor-pointer">
							<span
								class="inline-flex items-center gap-1.5 rounded-chip bg-[var(--color-primary-500)] px-3 py-1.5 text-xs font-semibold text-white transition-colors hover:bg-[var(--color-primary-600)]"
							>
								<Upload size={14} />
								{m.settings_custom_logo_upload()}
							</span>
							<input type="file" accept="image/*" class="hidden" onchange={handleLogoUpload} />
						</label>
						{#if settingsStore.settings.customLogoData}
							<button
								type="button"
								onclick={handleLogoReset}
								class="inline-flex items-center gap-1.5 rounded-chip border border-[var(--color-surface-border)] px-3 py-1.5 text-xs font-semibold text-[var(--color-nav-inactive)] transition-colors hover:bg-[var(--color-surface-muted)] hover:text-[var(--color-nav-active)]"
							>
								<Trash2 size={14} />
								{m.settings_custom_logo_reset()}
							</button>
						{/if}
					</div>
				</div>
			</div>
		</div>
	</SettingsSection>

	<!-- ===== Semester ===== -->
	<SettingsSection
		id="settings-semester"
		title={m.settings_semester()}
		class={visibleSections['settings-semester'] ? '' : 'hidden'}
	>
		<div class="border-b border-[var(--color-surface-border)] px-4 py-3.5">
			<div class="grid grid-cols-3 gap-2">
				{#each semesterOptions as opt (opt.value)}
					<button
						type="button"
						class="rounded-chip border px-2 py-2 text-center text-sm transition-colors {settingsStore
							.settings.semester === opt.value
							? 'border-[var(--color-primary-500)] bg-[var(--color-primary-50)] text-[var(--color-primary-600)]'
							: 'border-[var(--color-surface-border)] text-[var(--color-nav-active)] hover:bg-[var(--color-surface-muted)]'}"
						onclick={() => handleSegmentSelect('semester', opt.value)}
					>
						{opt.label}
					</button>
				{/each}
			</div>
		</div>
		<div class="flex items-center justify-between px-4 py-3.5">
			<span class="text-sm font-medium text-[var(--color-nav-active)]">
				{m.settings_year()}
			</span>
			<label class="relative inline-flex items-center">
				<select
					value={String(settingsStore.settings.year)}
					onchange={(e) => {
						const n = Number((e.currentTarget as HTMLSelectElement).value);
						settingsStore.update({ year: n });
					}}
					class="h-8 min-w-[7.5rem] cursor-pointer appearance-none rounded-chip border border-[var(--color-surface-border)] bg-[var(--color-surface-card)] py-1 pl-3 pr-8 text-right text-xs font-bold text-[var(--color-nav-active)] outline-none transition-colors hover:bg-[var(--color-surface-muted)] focus:border-[var(--color-primary-500)] focus:ring-2 focus:ring-[var(--color-primary-500)]/25"
					style="-webkit-appearance: none; -moz-appearance: none; appearance: none;"
				>
					{#each yearOptions as opt}
						<option value={opt.value}>{opt.label}</option>
					{/each}
				</select>
				<span
					class="pointer-events-none absolute right-2.5 top-1/2 flex -translate-y-1/2 text-[var(--color-nav-inactive)]"
				>
					<ChevronDown size={12} />
				</span>
			</label>
		</div>
	</SettingsSection>

	<!-- ===== Location ===== -->
	<SettingsSection
		id="settings-location"
		title={m.settings_location()}
		class={visibleSections['settings-location'] ? '' : 'hidden'}
	>
		<div class="space-y-3.5 px-4 py-3.5">
			<TextField
				type="text"
				label={m.settings_location()}
				value={settingsStore.settings.locationName}
				onchange={(e) => {
					const target = e.currentTarget as HTMLInputElement;
					settingsStore.update({ locationName: target.value });
				}}
			/>
			<div class="grid grid-cols-2 gap-3">
				<TextField
					type="number"
					label="Lat"
					value={String(settingsStore.settings.lat)}
					onchange={(e) => {
						const n = parseFloat((e.currentTarget as HTMLInputElement).value);
						if (!isNaN(n)) settingsStore.update({ lat: n });
					}}
				/>
				<TextField
					type="number"
					label="Lng"
					value={String(settingsStore.settings.lng)}
					onchange={(e) => {
						const n = parseFloat((e.currentTarget as HTMLInputElement).value);
						if (!isNaN(n)) settingsStore.update({ lng: n });
					}}
				/>
			</div>
		</div>
	</SettingsSection>

	<!-- ===== Home ===== -->
	<SettingsSection
		id="settings-home"
		title={m.settings_home()}
		class={visibleSections['settings-home'] ? '' : 'hidden'}
	>
		<div
			class="flex items-center justify-between border-b border-[var(--color-surface-border)] px-4 py-3.5"
		>
			<span class="text-sm font-medium text-[var(--color-nav-active)]"
				>{m.settings_home_weather()}</span
			>
			<Switch
				checked={settingsStore.settings.homeShowWeather}
				onchange={(v) => settingsStore.update({ homeShowWeather: v })}
			/>
		</div>
		<div
			class="flex items-center justify-between border-b border-[var(--color-surface-border)] px-4 py-3.5"
		>
			<span class="text-sm font-medium text-[var(--color-nav-active)]"
				>{m.settings_home_tools()}</span
			>
			<Switch
				checked={settingsStore.settings.homeShowTools}
				onchange={(v) => settingsStore.update({ homeShowTools: v })}
			/>
		</div>
		<div
			class="flex items-center justify-between border-b border-[var(--color-surface-border)] px-4 py-3.5"
		>
			<span class="text-sm font-medium text-[var(--color-nav-active)]"
				>{m.settings_home_external_links()}</span
			>
			<Switch
				checked={settingsStore.settings.homeShowExternalLinks}
				onchange={(v) => settingsStore.update({ homeShowExternalLinks: v })}
			/>
		</div>
		<div
			class="flex items-center justify-between border-b border-[var(--color-surface-border)] px-4 py-3.5"
		>
			<span class="text-sm font-medium text-[var(--color-nav-active)]"> 検索エンジン </span>
			<label class="relative inline-flex items-center">
				<select
					id="settings-home-search-provider"
					value={settingsStore.settings.homeSearchProvider}
					onchange={(e) => {
						const target = e.currentTarget as HTMLSelectElement;
						settingsStore.update({ homeSearchProvider: target.value as SearchProviderId });
					}}
					class="h-8 min-w-[7.5rem] cursor-pointer appearance-none rounded-chip border border-[var(--color-surface-border)] bg-[var(--color-surface-card)] py-1 pl-3 pr-8 text-right text-xs font-bold text-[var(--color-nav-active)] outline-none transition-colors hover:bg-[var(--color-surface-muted)] focus:border-[var(--color-primary-500)] focus:ring-2 focus:ring-[var(--color-primary-500)]/25"
					style="-webkit-appearance: none; -moz-appearance: none; appearance: none;"
				>
					{#each searchProviderOptions as opt}
						<option value={opt.value}>{opt.label}</option>
					{/each}
				</select>
				<span
					class="pointer-events-none absolute right-2.5 top-1/2 flex -translate-y-1/2 text-[var(--color-nav-inactive)]"
				>
					<ChevronDown size={12} />
				</span>
			</label>
		</div>
		<div
			class="flex items-center justify-between border-b border-[var(--color-surface-border)] px-4 py-3.5"
		>
			<span class="text-sm font-medium text-[var(--color-nav-active)]">
				{m.settings_weather_temp_unit()}
			</span>
			<label class="relative inline-flex items-center">
				<select
					id="settings-home-weather-temp-unit"
					value={settingsStore.settings.weatherTempUnit}
					onchange={(e) => {
						const target = e.currentTarget as HTMLSelectElement;
						settingsStore.update({ weatherTempUnit: target.value as 'celsius' | 'fahrenheit' });
					}}
					class="h-8 min-w-[7.5rem] cursor-pointer appearance-none rounded-chip border border-[var(--color-surface-border)] bg-[var(--color-surface-card)] py-1 pl-3 pr-8 text-right text-xs font-bold text-[var(--color-nav-active)] outline-none transition-colors hover:bg-[var(--color-surface-muted)] focus:border-[var(--color-primary-500)] focus:ring-2 focus:ring-[var(--color-primary-500)]/25"
					style="-webkit-appearance: none; -moz-appearance: none; appearance: none;"
				>
					{#each [{ value: 'celsius', label: m.settings_weather_temp_unit_celsius() }, { value: 'fahrenheit', label: m.settings_weather_temp_unit_fahrenheit() }] as opt}
						<option value={opt.value}>{opt.label}</option>
					{/each}
				</select>
				<span
					class="pointer-events-none absolute right-2.5 top-1/2 flex -translate-y-1/2 text-[var(--color-nav-inactive)]"
				>
					<ChevronDown size={12} />
				</span>
			</label>
		</div>
		<div
			class="flex items-center justify-between border-b border-[var(--color-surface-border)] px-4 py-3.5"
		>
			<span class="text-sm font-medium text-[var(--color-nav-active)]">
				{m.settings_weather_time_format()}
			</span>
			<label class="relative inline-flex items-center">
				<select
					id="settings-home-weather-time-format"
					value={settingsStore.settings.weatherTimeFormat}
					onchange={(e) => {
						const target = e.currentTarget as HTMLSelectElement;
						settingsStore.update({ weatherTimeFormat: target.value as 'minute' | 'second' });
					}}
					class="h-8 min-w-[7.5rem] cursor-pointer appearance-none rounded-chip border border-[var(--color-surface-border)] bg-[var(--color-surface-card)] py-1 pl-3 pr-8 text-right text-xs font-bold text-[var(--color-nav-active)] outline-none transition-colors hover:bg-[var(--color-surface-muted)] focus:border-[var(--color-primary-500)] focus:ring-2 focus:ring-[var(--color-primary-500)]/25"
					style="-webkit-appearance: none; -moz-appearance: none; appearance: none;"
				>
					{#each [{ value: 'minute', label: m.settings_weather_time_format_minute() }, { value: 'second', label: m.settings_weather_time_format_second() }] as opt}
						<option value={opt.value}>{opt.label}</option>
					{/each}
				</select>
				<span
					class="pointer-events-none absolute right-2.5 top-1/2 flex -translate-y-1/2 text-[var(--color-nav-inactive)]"
				>
					<ChevronDown size={12} />
				</span>
			</label>
		</div>
		<div
			class="flex items-center justify-between border-b border-[var(--color-surface-border)] px-4 py-3.5"
		>
			<span class="text-sm font-medium text-[var(--color-nav-active)]">
				{m.settings_time_format()}
			</span>
			<label class="relative inline-flex items-center">
				<select
					id="settings-time-format"
					value={settingsStore.settings.timeFormat}
					onchange={(e) => {
						const target = e.currentTarget as HTMLSelectElement;
						settingsStore.update({ timeFormat: target.value as '12h' | '24h' });
					}}
					class="h-8 min-w-[7.5rem] cursor-pointer appearance-none rounded-chip border border-[var(--color-surface-border)] bg-[var(--color-surface-card)] py-1 pl-3 pr-8 text-right text-xs font-bold text-[var(--color-nav-active)] outline-none transition-colors hover:bg-[var(--color-surface-muted)] focus:border-[var(--color-primary-500)] focus:ring-2 focus:ring-[var(--color-primary-500)]/25"
					style="-webkit-appearance: none; -moz-appearance: none; appearance: none;"
				>
					{#each [{ value: '12h', label: m.settings_time_format_12h() }, { value: '24h', label: m.settings_time_format_24h() }] as opt}
						<option value={opt.value}>{opt.label}</option>
					{/each}
				</select>
				<span
					class="pointer-events-none absolute right-2.5 top-1/2 flex -translate-y-1/2 text-[var(--color-nav-inactive)]"
				>
					<ChevronDown size={12} />
				</span>
			</label>
		</div>
		{#if Capacitor.isNativePlatform()}
			<div class="flex items-center justify-between px-4 py-3.5">
				<span class="text-sm font-medium text-[var(--color-nav-active)]"
					>{m.settings_open_links_in_app()}</span
				>
				<Switch
					checked={settingsStore.settings.openLinksInApp}
					onchange={(v) => settingsStore.update({ openLinksInApp: v })}
				/>
			</div>
		{/if}
	</SettingsSection>

	<!-- ===== Timetable ===== -->
	<SettingsSection
		id="settings-timetable"
		title={m.settings_timetable()}
		class={visibleSections['settings-timetable'] ? '' : 'hidden'}
	>
		<div
			class="flex items-center justify-between border-b border-[var(--color-surface-border)] px-4 py-3.5"
		>
			<span class="text-sm font-medium text-[var(--color-nav-active)]">
				{m.settings_timetable_day_range()}
			</span>
			<label class="relative inline-flex items-center">
				<select
					value={String(settingsStore.settings.timetableDayRange)}
					onchange={(e) => {
						const value = Number((e.currentTarget as HTMLSelectElement).value) as 5 | 6 | 7;
						settingsStore.update({ timetableDayRange: value });
					}}
					class="h-8 min-w-[7.5rem] cursor-pointer appearance-none rounded-chip border border-[var(--color-surface-border)] bg-[var(--color-surface-card)] py-1 pl-3 pr-8 text-right text-xs font-bold text-[var(--color-nav-active)] outline-none transition-colors hover:bg-[var(--color-surface-muted)] focus:border-[var(--color-primary-500)] focus:ring-2 focus:ring-[var(--color-primary-500)]/25"
					style="-webkit-appearance: none; -moz-appearance: none; appearance: none;"
				>
					{#each timetableDayOptions as opt}
						<option value={opt.value}>{opt.label}</option>
					{/each}
				</select>
				<span
					class="pointer-events-none absolute right-2.5 top-1/2 flex -translate-y-1/2 text-[var(--color-nav-inactive)]"
				>
					<ChevronDown size={12} />
				</span>
			</label>
		</div>
		<div
			class="flex items-center justify-between border-b border-[var(--color-surface-border)] px-4 py-3.5"
		>
			<span class="text-sm font-medium text-[var(--color-nav-active)]">
				{m.settings_timetable_period_range()}
			</span>
			<label class="relative inline-flex items-center">
				<select
					value={String(settingsStore.settings.timetablePeriodRange)}
					onchange={(e) => {
						const value = Number((e.currentTarget as HTMLSelectElement).value) as 4 | 5 | 6 | 7;
						settingsStore.update({ timetablePeriodRange: value });
					}}
					class="h-8 min-w-[7.5rem] cursor-pointer appearance-none rounded-chip border border-[var(--color-surface-border)] bg-[var(--color-surface-card)] py-1 pl-3 pr-8 text-right text-xs font-bold text-[var(--color-nav-active)] outline-none transition-colors hover:bg-[var(--color-surface-muted)] focus:border-[var(--color-primary-500)] focus:ring-2 focus:ring-[var(--color-primary-500)]/25"
					style="-webkit-appearance: none; -moz-appearance: none; appearance: none;"
				>
					{#each timetablePeriodOptions as opt}
						<option value={opt.value}>{opt.label}</option>
					{/each}
				</select>
				<span
					class="pointer-events-none absolute right-2.5 top-1/2 flex -translate-y-1/2 text-[var(--color-nav-inactive)]"
				>
					<ChevronDown size={12} />
				</span>
			</label>
		</div>
		<div
			class="flex items-center justify-between border-b border-[var(--color-surface-border)] px-4 py-3.5"
		>
			<span class="text-sm font-medium text-[var(--color-nav-active)]">
				{m.settings_timetable_ondemand_cols()}
			</span>
			<label class="relative inline-flex items-center">
				<select
					value={String(settingsStore.settings.timetableOndemandCols)}
					onchange={(e) => {
						const value = Number((e.currentTarget as HTMLSelectElement).value) as 0 | 1 | 2 | 3;
						settingsStore.update({ timetableOndemandCols: value });
					}}
					class="h-8 min-w-[7.5rem] cursor-pointer appearance-none rounded-chip border border-[var(--color-surface-border)] bg-[var(--color-surface-card)] py-1 pl-3 pr-8 text-right text-xs font-bold text-[var(--color-nav-active)] outline-none transition-colors hover:bg-[var(--color-surface-muted)] focus:border-[var(--color-primary-500)] focus:ring-2 focus:ring-[var(--color-primary-500)]/25"
					style="-webkit-appearance: none; -moz-appearance: none; appearance: none;"
				>
					{#each timetableOndemandOptions as opt}
						<option value={opt.value}>{opt.label}</option>
					{/each}
				</select>
				<span
					class="pointer-events-none absolute right-2.5 top-1/2 flex -translate-y-1/2 text-[var(--color-nav-inactive)]"
				>
					<ChevronDown size={12} />
				</span>
			</label>
		</div>
		<div
			class="flex items-center justify-between border-b border-[var(--color-surface-border)] px-4 py-3.5"
		>
			<span class="text-sm font-medium text-[var(--color-nav-active)]">
				{m.settings_timetable_transparent()}
			</span>
			<Switch
				checked={settingsStore.settings.timetableTransparent}
				onchange={(v) => settingsStore.update({ timetableTransparent: v })}
			/>
		</div>
		<div class="flex items-center justify-between px-4 py-3.5">
			<span class="text-sm font-medium text-[var(--color-nav-active)]"
				>{m.settings_bg_image_all_pages()}</span
			>
			<Switch
				checked={settingsStore.settings.bgImageAllPages}
				onchange={(v) => settingsStore.update({ bgImageAllPages: v })}
			/>
		</div>
		<div class="px-4 py-3 space-y-3">
			<span class="block text-sm font-medium text-[var(--color-nav-active)]">
				{m.settings_timetable_bg_image()}
			</span>
			<div class="flex flex-col gap-3">
				{#if settingsStore.settings.timetableBgImage}
					<div
						class="relative w-32 aspect-[3/4] rounded-card overflow-hidden border border-[var(--color-surface-border)] bg-[var(--color-surface-muted)]"
					>
						<img
							src={settingsStore.settings.timetableBgImage}
							alt="背景画像プレビュー"
							class="object-cover w-full h-full"
						/>
					</div>
				{/if}

				<div class="flex gap-2">
					<label
						class="inline-flex h-8 cursor-pointer items-center justify-center gap-2 rounded-chip border border-[var(--color-surface-border)] bg-[var(--color-surface-card)] px-3 text-sm font-medium text-[var(--color-nav-active)] transition-colors hover:bg-[var(--color-surface-muted)]"
					>
						<Upload size={14} />
						画像を選択
						<input type="file" accept="image/*" class="hidden" onchange={handleBgImageUpload} />
					</label>

					{#if settingsStore.settings.timetableBgImage}
						<Button variant="danger" size="sm" onclick={handleBgImageClear}>
							<Trash2 size={14} />
							削除
						</Button>
					{/if}
				</div>

				<TextField
					type="url"
					label="または画像URLを指定"
					placeholder="https://..."
					value={settingsStore.settings.timetableBgImage ?? ''}
					onchange={(e) => {
						const target = e.currentTarget as HTMLInputElement;
						settingsStore.update({ timetableBgImage: target.value || undefined });
					}}
				/>
			</div>
		</div>

		<!-- 授業時間設定 (Timetable Periods) -->
		<div class="border-t border-[var(--color-surface-border)] px-4 py-3 space-y-3">
			<div>
				<span class="block text-sm font-medium text-[var(--color-nav-active)]">
					{m.settings_timetable_periods()}
				</span>
				<span class="text-xs text-[var(--color-surface-muted-foreground)]">
					{m.settings_timetable_periods_desc()}
				</span>
			</div>
			<div class="space-y-2.5">
				{#each [1, 2, 3, 4, 5, 6, 7] as p (p)}
					{@const time = settingsStore.settings.timetablePeriods[p as Period] || {
						start: '00:00',
						end: '00:00'
					}}
					<div class="flex items-center gap-3">
						<span class="text-xs font-semibold w-8">{p}限</span>
						<input
							type="time"
							class="rounded-chip border border-[var(--color-surface-border)] bg-[var(--color-surface-card)] px-2 py-1 text-xs text-[var(--color-nav-active)] focus:outline-none focus:ring-1 focus:ring-[var(--color-primary-500)]"
							value={time.start}
							onchange={(e) => {
								const target = e.currentTarget as HTMLInputElement;
								const updatedPeriods = { ...settingsStore.settings.timetablePeriods };
								updatedPeriods[p as Period] = { ...time, start: target.value };
								settingsStore.update({ timetablePeriods: updatedPeriods });
							}}
						/>
						<span class="text-xs text-[var(--color-surface-muted-foreground)]">〜</span>
						<input
							type="time"
							class="rounded-chip border border-[var(--color-surface-border)] bg-[var(--color-surface-card)] px-2 py-1 text-xs text-[var(--color-nav-active)] focus:outline-none focus:ring-1 focus:ring-[var(--color-primary-500)]"
							value={time.end}
							onchange={(e) => {
								const target = e.currentTarget as HTMLInputElement;
								const updatedPeriods = { ...settingsStore.settings.timetablePeriods };
								updatedPeriods[p as Period] = { ...time, end: target.value };
								settingsStore.update({ timetablePeriods: updatedPeriods });
							}}
						/>
					</div>
				{/each}
			</div>
		</div>
	</SettingsSection>

	<!-- ===== Calendar ===== -->
	<SettingsSection
		id="settings-calendar"
		title={m.settings_calendar()}
		class={visibleSections['settings-calendar'] ? '' : 'hidden'}
	>
		<div class="border-b border-[var(--color-surface-border)] px-4 py-3">
			<div class="mb-2 text-sm font-medium text-[var(--color-nav-active)]">
				{m.settings_calendar_week_start()}
			</div>
			<div class="grid grid-cols-2 gap-2">
				{#each weekStartOptions as opt (opt.value)}
					<button
						type="button"
						class="rounded-chip border px-2 py-2 text-center text-sm transition-colors {settingsStore
							.settings.calendarWeekStart === opt.value
							? 'border-[var(--color-primary-500)] bg-[var(--color-primary-50)] text-[var(--color-primary-600)]'
							: 'border-[var(--color-surface-border)] text-[var(--color-nav-active)] hover:bg-[var(--color-surface-muted)]'}"
						onclick={() => handleSegmentSelect('calendarWeekStart', opt.value)}
					>
						{opt.label}
					</button>
				{/each}
			</div>
		</div>
		<div
			class="flex items-center justify-between border-b border-[var(--color-surface-border)] px-4 py-3"
		>
			<span class="text-sm text-[var(--color-nav-active)]"
				>{m.settings_calendar_show_academic()}</span
			>
			<Switch
				checked={settingsStore.settings.calendarShowAcademic}
				onchange={(v) => settingsStore.update({ calendarShowAcademic: v })}
			/>
		</div>
		<div
			class="flex items-center justify-between border-b border-[var(--color-surface-border)] px-4 py-3"
		>
			<span class="text-sm text-[var(--color-nav-active)]"
				>{m.settings_calendar_show_holidays()}</span
			>
			<Switch
				checked={settingsStore.settings.calendarShowHolidays}
				onchange={(v) => settingsStore.update({ calendarShowHolidays: v })}
			/>
		</div>
		<div
			class="flex items-center justify-between border-b border-[var(--color-surface-border)] px-4 py-3"
		>
			<span class="text-sm text-[var(--color-nav-active)]"
				>{m.settings_calendar_show_external()}</span
			>
			<Switch
				checked={settingsStore.settings.calendarShowExternal}
				onchange={(v) => settingsStore.update({ calendarShowExternal: v })}
			/>
		</div>
		<div
			class="flex items-center justify-between border-b border-[var(--color-surface-border)] px-4 py-3"
		>
			<span class="text-sm text-[var(--color-nav-active)]"
				>{m.settings_calendar_show_timetable()}</span
			>
			<Switch
				checked={settingsStore.settings.calendarShowTimetable}
				onchange={(v) => settingsStore.update({ calendarShowTimetable: v })}
			/>
		</div>
		<div class="border-b border-[var(--color-surface-border)] px-4 py-3">
			<Slider
				label={m.settings_calendar_density()}
				min={3}
				max={4}
				step={1}
				value={settingsStore.settings.calendarDensity}
				onchange={(v) => settingsStore.update({ calendarDensity: v as 3 | 4 })}
			/>
		</div>
		<div class="flex items-center justify-between px-4 py-3">
			<span class="text-sm text-[var(--color-nav-active)]"
				>{m.settings_calendar_auto_compact()}</span
			>
			<Switch
				checked={settingsStore.settings.calendarAutoCompact}
				onchange={(v) => settingsStore.update({ calendarAutoCompact: v })}
			/>
		</div>
	</SettingsSection>

	<!-- ===== Attendance ===== -->
	<SettingsSection
		id="settings-attendance"
		title={m.settings_attendance()}
		class={visibleSections['settings-attendance'] ? '' : 'hidden'}
	>
		<div class="px-4 py-3">
			<Slider
				label={m.settings_attendance_max_absences()}
				min={0}
				max={15}
				step={1}
				value={settingsStore.settings.attendanceMaxAbsences}
				onchange={(v) => settingsStore.update({ attendanceMaxAbsences: v })}
			/>
		</div>

		{#if Capacitor.isNativePlatform()}
			<div class="border-t border-[var(--color-surface-border)] px-4 py-3 space-y-4">
				<div class="flex items-center justify-between">
					<div class="max-w-[75%]">
						<span class="block text-sm font-medium text-[var(--color-nav-active)]">
							{m.settings_attendance_notification()}
						</span>
						<span class="text-xs text-[var(--color-surface-muted-foreground)]">
							{m.settings_attendance_notification_desc()}
						</span>
					</div>
					<Switch
						checked={settingsStore.settings.attendanceNotification.enabled}
						onchange={(v) =>
							settingsStore.update({
								attendanceNotification: {
									...settingsStore.settings.attendanceNotification,
									enabled: v
								}
							})}
					/>
				</div>

				{#if settingsStore.settings.attendanceNotification.enabled}
					<div class="space-y-4 pt-2">
						<TextField
							type="select"
							label={m.settings_attendance_notification_trigger()}
							value={settingsStore.settings.attendanceNotification.trigger}
							options={[
								{ value: 'start', label: m.settings_attendance_notification_trigger_start() },
								{ value: 'end', label: m.settings_attendance_notification_trigger_end() },
								{ value: 'both', label: m.settings_attendance_notification_trigger_both() }
							]}
							onchange={(e) => {
								const val = (e.currentTarget as HTMLSelectElement).value as
									| 'start'
									| 'end'
									| 'both';
								settingsStore.update({
									attendanceNotification: {
										...settingsStore.settings.attendanceNotification,
										trigger: val
									}
								});
							}}
						/>

						{#if settingsStore.settings.attendanceNotification.trigger === 'start' || settingsStore.settings.attendanceNotification.trigger === 'both'}
							<Slider
								label={m.settings_attendance_notification_start_offset()}
								min={0}
								max={30}
								step={5}
								value={settingsStore.settings.attendanceNotification.startOffsetMinutes}
								onchange={(v) =>
									settingsStore.update({
										attendanceNotification: {
											...settingsStore.settings.attendanceNotification,
											startOffsetMinutes: v
										}
									})}
								formatValue={(v: number) =>
									v === 0 ? m.settings_at_end() : m.settings_minutes_before({ minutes: String(v) })}
							/>
						{/if}

						{#if settingsStore.settings.attendanceNotification.trigger === 'end' || settingsStore.settings.attendanceNotification.trigger === 'both'}
							<Slider
								label={m.settings_attendance_notification_end_offset()}
								min={-15}
								max={15}
								step={5}
								value={settingsStore.settings.attendanceNotification.endOffsetMinutes}
								onchange={(v) =>
									settingsStore.update({
										attendanceNotification: {
											...settingsStore.settings.attendanceNotification,
											endOffsetMinutes: v
										}
									})}
								formatValue={(v: number) => {
									if (v === 0) return m.settings_at_end();
									if (v < 0) return m.settings_minutes_before({ minutes: String(Math.abs(v)) });
									return m.settings_minutes_after({ minutes: String(v) });
								}}
							/>
						{/if}
					</div>
				{/if}
			</div>
		{/if}
	</SettingsSection>

	<!-- ===== Modular Feature Configuration ===== -->
	<SettingsSection
		id="settings-features"
		title="アプリの機能配置設定"
		class={visibleSections['settings-features'] ? '' : 'hidden'}
	>
		<!-- バーの配置 -->
		<div class="border-b border-[var(--color-surface-border)] px-4 py-3 space-y-3">
			<h4 class="text-xs font-bold text-[var(--color-nav-active)]">
				{m.settings_features_bar()}
			</h4>

			<!-- 選択済み（順序あり）-->
			{#if tempBarFeatures.length > 0}
				<div class="space-y-1.5">
					<p
						class="text-[10px] font-semibold text-[var(--color-nav-inactive)] uppercase tracking-wide"
					>
						バーの順番（左→右）
					</p>
					{#each tempBarFeatures as id, i (id)}
						{@const f = APP_FEATURES.find((f) => f.id === id)}
						{#if f}
							<!-- svelte-ignore a11y_no_static_element_interactions -->
							<div
								draggable="true"
								ondragstart={() => handleBarDragStart(i)}
								ondragover={(e) => handleBarDragOver(e, i)}
								ondragend={handleBarDragEnd}
								class="flex items-center gap-2 rounded-chip border border-[var(--color-primary-200)] bg-[var(--color-primary-50)] px-3 py-2 cursor-move transition-opacity {draggingBarIndex ===
								i
									? 'opacity-40'
									: ''}"
							>
								<GripVertical size={14} class="text-[var(--color-nav-inactive)] shrink-0" />
								<span
									class="flex h-5 w-5 shrink-0 items-center justify-center rounded-full bg-[var(--color-primary-500)] text-[10px] font-black text-white"
									>{i + 1}</span
								>
								<span class="text-[var(--color-nav-inactive)] shrink-0"><f.icon size={14} /></span>
								<span class="flex-1 text-xs font-bold text-[var(--color-nav-active)] truncate"
									>{getFeatureName(f.id)}</span
								>
								<div class="flex gap-1 shrink-0">
									<button
										type="button"
										onclick={() => moveBarFeature(id, -1)}
										disabled={i === 0}
										class="flex h-6 w-6 items-center justify-center rounded border border-[var(--color-surface-border)] bg-[var(--color-surface-card)] text-[var(--color-nav-inactive)] transition-colors hover:bg-[var(--color-surface-muted)] disabled:opacity-30"
										aria-label="上へ"
									>
										<ChevronUp size={12} />
									</button>
									<button
										type="button"
										onclick={() => moveBarFeature(id, 1)}
										disabled={i === tempBarFeatures.length - 1}
										class="flex h-6 w-6 items-center justify-center rounded border border-[var(--color-surface-border)] bg-[var(--color-surface-card)] text-[var(--color-nav-inactive)] transition-colors hover:bg-[var(--color-surface-muted)] disabled:opacity-30"
										aria-label="下へ"
									>
										<ChevronDown size={12} />
									</button>
									<button
										type="button"
										onclick={() => toggleBarFeature(id, false)}
										class="flex h-6 w-6 items-center justify-center rounded border border-[var(--color-surface-border)] bg-[var(--color-surface-card)] text-[var(--color-nav-inactive)] transition-colors hover:text-[var(--color-danger-500)]"
										aria-label="バーから削除"
									>
										<X size={11} />
									</button>
								</div>
							</div>
						{/if}
					{/each}
				</div>
			{/if}

			<!-- 未選択（追加候補）-->
			{#if unselectedBar.length > 0}
				<div class="space-y-1.5">
					<p
						class="text-[10px] font-semibold text-[var(--color-nav-inactive)] uppercase tracking-wide"
					>
						追加できる機能 {tempBarFeatures.length >= 5 ? '（上限5つ）' : ''}
					</p>
					{#each unselectedBar as f (f.id)}
						<div
							class="flex items-center gap-2 rounded-chip border border-[var(--color-surface-border)] bg-[var(--color-surface-muted)]/40 px-3 py-2"
						>
							<span class="text-[var(--color-nav-inactive)] shrink-0"><f.icon size={14} /></span>
							<span class="flex-1 text-xs font-bold text-[var(--color-nav-inactive)] truncate"
								>{getFeatureName(f.id)}</span
							>
							<button
								type="button"
								onclick={() => toggleBarFeature(f.id, true)}
								disabled={tempBarFeatures.length >= 5}
								class="flex h-6 items-center gap-1 rounded border border-[var(--color-surface-border)] bg-[var(--color-surface-card)] px-2 text-[10px] font-bold text-[var(--color-nav-inactive)] transition-colors hover:border-[var(--color-primary-400)] hover:text-[var(--color-primary-600)] disabled:opacity-30"
							>
								<Plus size={10} /> 追加
							</button>
						</div>
					{/each}
				</div>
			{/if}
		</div>

		<!-- ホームの配置 & レイアウト設定 -->
		<div class="border-b border-[var(--color-surface-border)] px-4 py-3 space-y-3">
			<h4 class="text-xs font-bold text-[var(--color-nav-active)]">
				{m.settings_features_home()}
			</h4>

			<!-- 選択済み（順序あり）-->
			{#if tempHomeFeatures.length > 0}
				<div class="space-y-1.5">
					<p
						class="text-[10px] font-semibold text-[var(--color-nav-inactive)] uppercase tracking-wide"
					>
						ホームの表示順
					</p>
					{#each tempHomeFeatures as id, i (id)}
						{@const f = APP_FEATURES.find((f) => f.id === id && f.id !== 'dashboard')}
						{#if f}
							{@const isWidget = ['timetable', 'todo', 'calendar', 'attendance'].includes(f.id)}
							{@const layout = settingsStore.settings.homeFeatureLayouts?.[f.id] || 'button'}
							<!-- svelte-ignore a11y_no_static_element_interactions -->
							<div
								draggable="true"
								ondragstart={() => handleHomeDragStart(i)}
								ondragover={(e) => handleHomeDragOver(e, i)}
								ondragend={handleHomeDragEnd}
								class="rounded-chip border border-[var(--color-primary-200)] bg-[var(--color-primary-50)] px-3 py-2 space-y-1.5 cursor-move transition-opacity {draggingHomeIndex ===
								i
									? 'opacity-40'
									: ''}"
							>
								<div class="flex items-center gap-2">
									<GripVertical size={14} class="text-[var(--color-nav-inactive)] shrink-0" />
									<span
										class="flex h-5 w-5 shrink-0 items-center justify-center rounded-full bg-[var(--color-primary-500)] text-[10px] font-black text-white"
										>{i + 1}</span
									>
									<span class="text-[var(--color-nav-inactive)] shrink-0"><f.icon size={14} /></span
									>
									<span class="flex-1 text-xs font-bold text-[var(--color-nav-active)] truncate"
										>{getFeatureName(f.id)}</span
									>
									<div class="flex gap-1 shrink-0">
										<button
											type="button"
											onclick={() => moveHomeFeature(id, -1)}
											disabled={i === 0}
											class="flex h-6 w-6 items-center justify-center rounded border border-[var(--color-surface-border)] bg-[var(--color-surface-card)] text-[var(--color-nav-inactive)] transition-colors hover:bg-[var(--color-surface-muted)] disabled:opacity-30"
											aria-label="上へ"
										>
											<ChevronUp size={12} />
										</button>
										<button
											type="button"
											onclick={() => moveHomeFeature(id, 1)}
											disabled={i === tempHomeFeatures.length - 1}
											class="flex h-6 w-6 items-center justify-center rounded border border-[var(--color-surface-border)] bg-[var(--color-surface-card)] text-[var(--color-nav-inactive)] transition-colors hover:bg-[var(--color-surface-muted)] disabled:opacity-30"
											aria-label="下へ"
										>
											<ChevronDown size={12} />
										</button>
										<button
											type="button"
											onclick={() => toggleHomeFeature(id, false)}
											class="flex h-6 w-6 items-center justify-center rounded border border-[var(--color-surface-border)] bg-[var(--color-surface-card)] text-[var(--color-nav-inactive)] transition-colors hover:text-[var(--color-danger-500)]"
											aria-label="ホームから削除"
										>
											<X size={11} />
										</button>
									</div>
								</div>
								{#if isWidget}
									<div class="flex items-center gap-2 pl-7">
										<span class="text-[10px] text-[var(--color-nav-inactive)] font-semibold"
											>表示形式:</span
										>
										<button
											type="button"
											onclick={() => toggleHomeLayout(f.id)}
											class="rounded border border-[var(--color-surface-border)] bg-[var(--color-surface-card)] px-2 py-0.5 text-[10px] font-bold text-[var(--color-primary-700)] hover:bg-[var(--color-surface-muted)] transition-colors"
										>
											{layout === 'widget'
												? m.settings_feature_layout_widget()
												: m.settings_feature_layout_button()}
										</button>
									</div>
								{/if}
							</div>
						{/if}
					{/each}
				</div>
			{/if}

			<!-- 未選択（追加候補）-->
			{#if unselectedHome.length > 0}
				<div class="space-y-1.5">
					<p
						class="text-[10px] font-semibold text-[var(--color-nav-inactive)] uppercase tracking-wide"
					>
						追加できる機能
					</p>
					{#each unselectedHome as f (f.id)}
						<div
							class="flex items-center gap-2 rounded-chip border border-[var(--color-surface-border)] bg-[var(--color-surface-muted)]/40 px-3 py-2"
						>
							<span class="text-[var(--color-nav-inactive)] shrink-0"><f.icon size={14} /></span>
							<span class="flex-1 text-xs font-bold text-[var(--color-nav-inactive)] truncate"
								>{getFeatureName(f.id)}</span
							>
							<button
								type="button"
								onclick={() => toggleHomeFeature(f.id, true)}
								class="flex h-6 items-center gap-1 rounded border border-[var(--color-surface-border)] bg-[var(--color-surface-card)] px-2 text-[10px] font-bold text-[var(--color-nav-inactive)] transition-colors hover:border-[var(--color-primary-400)] hover:text-[var(--color-primary-600)]"
							>
								<Plus size={10} /> 追加
							</button>
						</div>
					{/each}
				</div>
			{/if}
		</div>
	</SettingsSection>

	<!-- ===== Widget Settings ===== -->
	<SettingsSection
		id="settings-widget"
		title={m.settings_widget_theme_title()}
		class={visibleSections['settings-widget'] ? '' : 'hidden'}
	>
		<div
			class="flex items-center justify-between border-b border-[var(--color-surface-border)] px-4 py-3"
		>
			<div class="flex flex-col gap-0.5">
				<span class="text-sm font-semibold text-[var(--color-nav-active)]">
					{m.settings_widget_use_system_theme()}
				</span>
			</div>
			<Switch
				checked={settingsStore.settings.widgetUseSystemTheme}
				onchange={(v) => settingsStore.update({ widgetUseSystemTheme: v })}
			/>
		</div>
		{#if !settingsStore.settings.widgetUseSystemTheme}
			<div class="flex items-center justify-between px-4 py-3">
				<span class="text-sm font-semibold text-[var(--color-nav-active)]">
					{m.settings_widget_theme_color()}
				</span>
				<input
					type="color"
					value={settingsStore.settings.widgetThemeColor}
					oninput={(e) =>
						settingsStore.update({
							widgetThemeColor: (e.currentTarget as HTMLInputElement).value
						})}
					class="h-9 w-14 cursor-pointer rounded border border-[var(--color-surface-border)] bg-transparent p-0.5 focus:outline-none"
				/>
			</div>
		{/if}
	</SettingsSection>

	<!-- ===== About ===== -->
	<SettingsSection
		id="settings-about"
		title={m.settings_about()}
		class={visibleSections['settings-about'] ? '' : 'hidden'}
	>
		<div
			class="flex items-center justify-between border-b border-[var(--color-surface-border)] px-4 py-3"
		>
			<span class="flex items-center gap-2 text-sm text-[var(--color-nav-active)]">
				<Info size={16} class="text-[var(--color-nav-inactive)]" />
				{m.settings_about_version()}
			</span>
			<span class="text-sm text-[var(--color-nav-inactive)]"
				>{m.version({ version: appVersion })}</span
			>
		</div>
		<a
			href={SOURCE_URL}
			target="_blank"
			rel="noopener noreferrer"
			class="flex items-center justify-between px-4 py-3 text-sm text-[var(--color-nav-active)] transition-colors hover:bg-[var(--color-surface-muted)]"
		>
			<span class="flex items-center gap-2">
				<Link2 size={16} class="text-[var(--color-nav-inactive)]" />
				{m.settings_about_source()}
			</span>
		</a>
	</SettingsSection>
</Container>

<!-- ===== Clear Data Confirmation Modal ===== -->
<Modal
	open={showClearConfirm}
	onclose={() => (showClearConfirm = false)}
	title={m.settings_data_clear()}
>
	<div class="space-y-4">
		<p class="text-sm text-[var(--color-nav-active)]">{m.settings_data_clear_confirm()}</p>
		<div class="flex gap-3">
			<Button
				variant="secondary"
				size="sm"
				class="flex-1"
				onclick={() => (showClearConfirm = false)}
			>
				{m.action_cancel()}
			</Button>
			<Button variant="danger" size="sm" class="flex-1" onclick={handleClearData}>
				{m.action_delete()}
			</Button>
		</div>
	</div>
</Modal>

<!-- ===== RemoteStorage Connection Modal ===== -->
<Modal
	open={showConnectModal}
	onclose={() => {
		showConnectModal = false;
		connectAddress = '';
	}}
	title={m.settings_account_connect()}
>
	<form
		onsubmit={(e) => {
			e.preventDefault();
			submitConnect();
		}}
		class="space-y-4"
	>
		<div
			class="rounded-chip border border-[var(--color-surface-border)] bg-[var(--color-surface-muted)]/50 p-3"
		>
			<div class="mb-2 flex items-center gap-2 text-sm font-medium text-[var(--color-nav-active)]">
				<Info size={16} class="text-[var(--color-primary-500)]" />
				{m.settings_account_remote_guide_title()}
			</div>
			<ol class="space-y-1 pl-5 text-xs leading-5 text-[var(--color-nav-inactive)]">
				<li>{m.settings_account_remote_guide_step_1()}</li>
				<li>{m.settings_account_remote_guide_step_2()}</li>
				<li>{m.settings_account_remote_guide_step_3()}</li>
			</ol>
		</div>
		<TextField
			id="rs-address"
			type="text"
			label={m.settings_account_remote_address_label()}
			placeholder={m.settings_account_remote_address_placeholder()}
			hint={m.settings_account_remote_address_hint()}
			bind:value={connectAddress}
			required
		/>
		<div class="flex gap-3">
			<Button
				variant="secondary"
				size="sm"
				class="flex-1"
				onclick={() => {
					showConnectModal = false;
					connectAddress = '';
				}}
			>
				{m.action_cancel()}
			</Button>
			<Button type="submit" variant="primary" size="sm" class="flex-1">
				{m.settings_account_connect()}
			</Button>
		</div>
	</form>
</Modal>
