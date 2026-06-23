<script lang="ts">
	import { goto } from '$app/navigation';
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
	import SettingsSearch, { type SearchItem } from '$lib/components/settings/SettingsSearch.svelte';
	import { settingsStore, themeStore, uiStore, userDataStore } from '$lib/stores';
	import { BUILTIN_PACKS } from '$lib/theme';
	import type { ThemePack } from '$lib/theme';
	import {
		connectRemoteStorage,
		disconnectRemoteStorage,
		getConnectedUserAddress
	} from '$lib/remotestorage';
	import type { Period, SearchProviderId, UserSettings } from '$lib/types';
	import { m } from '$lib/paraglide/messages';
	import { Search, Download, Upload, Trash2, Cloud, CloudOff, Info, Link2 } from '@lucide/svelte';
	import { Capacitor } from '@capacitor/core';

	const appVersion = '0.0.1';

	// ----- Cmd+K ショートカット -----
	function handleGlobalKeydown(e: KeyboardEvent) {
		if ((e.metaKey || e.ctrlKey) && e.key === 'k') {
			e.preventDefault();
			uiStore.openPalette();
		}
	}

	// ----- テーマパック名解決 -----
	function getPackName(pack: ThemePack): string {
		switch (pack.name) {
			case 'theme_pack_default':
				return m.theme_pack_default();
			case 'theme_pack_uec':
				return m.theme_pack_uec();
			case 'theme_pack_dango':
				return m.theme_pack_dango();
			case 'theme_pack_dark':
				return m.theme_pack_dark();
			case 'theme_pack_custom':
				return m.theme_pack_custom();
			default:
				return pack.name;
		}
	}

	const themePackOptions = $derived([
		...BUILTIN_PACKS.map((p) => ({ value: p.id, label: getPackName(p) })),
		...themeStore.customPacks.map((p) => ({ value: p.id, label: p.name }))
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
		a.download = 'open-nexus-settings.json';
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

	// ----- 検索インデックス -----
	const searchItems: SearchItem[] = [
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
	];

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
		{ value: 'outfit', label: 'Outfit' },
		{ value: 'sawarabi', label: 'Sawarabi Gothic' },
		{ value: 'noto', label: 'Noto Sans JP' }
	];

	const searchProviderOptions: { value: SearchProviderId; label: string }[] = [
		{ value: 'google', label: 'Google' },
		{ value: 'bing', label: 'Bing' },
		{ value: 'duckduckgo', label: 'DuckDuckGo' }
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
</script>

<svelte:window onkeydown={handleGlobalKeydown} />

<svelte:head><title>{m.settings_title()} | open-nexus</title></svelte:head>

<PageHeader
	title={m.settings_title()}
	trailingIcon={Search}
	trailingAriaLabel={m.action_search()}
	ontrailing={() => uiStore.openPalette()}
/>

<Container size="narrow" class="space-y-6 py-4">
	<!-- ===== Language ===== -->
	<SettingsSection id="settings-language" title={m.settings_locale()}>
		<div class="px-4 py-3">
			<div class="grid grid-cols-4 gap-2">
				{#each localeOptions as opt (opt.value)}
					<button
						type="button"
						class="rounded-lg border px-2 py-2 text-center text-sm transition-colors {settingsStore
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
	<SettingsSection id="settings-theme" title={m.settings_theme_pack()}>
		<div class="px-4 py-3 space-y-4">
			<div class="space-y-1">
				<label
					for="settings-theme-pack-light"
					class="text-xs font-semibold text-[var(--color-nav-inactive)]"
				>
					{m.settings_theme_light()}
				</label>
				<TextField
					id="settings-theme-pack-light"
					type="select"
					value={settingsStore.settings.themePackIdLight || 'default'}
					onchange={(e) => {
						const target = e.currentTarget as HTMLSelectElement;
						settingsStore.update({ themePackIdLight: target.value });
					}}
					options={themePackOptions}
				/>
			</div>
			<div class="space-y-1">
				<label
					for="settings-theme-pack-dark"
					class="text-xs font-semibold text-[var(--color-nav-inactive)]"
				>
					{m.settings_theme_dark()}
				</label>
				<TextField
					id="settings-theme-pack-dark"
					type="select"
					value={settingsStore.settings.themePackIdDark || 'dark'}
					onchange={(e) => {
						const target = e.currentTarget as HTMLSelectElement;
						settingsStore.update({ themePackIdDark: target.value });
					}}
					options={themePackOptions}
				/>
			</div>
		</div>
	</SettingsSection>

	<!-- ===== Appearance ===== -->
	<SettingsSection id="settings-appearance" title={m.settings_theme()}>
		<!-- テーマ選択 -->
		<div class="border-b border-[var(--color-surface-border)] px-4 py-3 space-y-2">
			<div class="text-xs font-semibold text-[var(--color-nav-inactive)]">
				{m.settings_theme_mode()}
			</div>
			<div class="grid grid-cols-3 gap-2">
				{#each themeModeOptions as opt (opt.value)}
					<button
						type="button"
						class="rounded-lg border px-2 py-2 text-center text-sm transition-colors {settingsStore
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
		<div class="border-b border-[var(--color-surface-border)] px-4 py-3 space-y-2">
			<div class="text-xs font-semibold text-[var(--color-nav-inactive)]">
				{m.settings_emoji_style()}
			</div>
			<div class="grid grid-cols-2 gap-2">
				{#each emojiStyleOptions as opt (opt.value)}
					<button
						type="button"
						class="rounded-lg border px-2 py-2 text-center text-sm transition-colors {settingsStore
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
		<div class="px-4 py-3 space-y-2">
			<TextField
				type="select"
				label={m.settings_font_family()}
				value={settingsStore.settings.fontFamily}
				onchange={(e) => {
					const target = e.currentTarget as HTMLSelectElement;
					settingsStore.update({ fontFamily: target.value as UserSettings['fontFamily'] });
				}}
				options={fontFamilyOptions}
			/>
		</div>
	</SettingsSection>

	<!-- ===== Semester ===== -->
	<SettingsSection id="settings-semester" title={m.settings_semester()}>
		<div class="border-b border-[var(--color-surface-border)] px-4 py-3">
			<div class="grid grid-cols-3 gap-2">
				{#each semesterOptions as opt (opt.value)}
					<button
						type="button"
						class="rounded-lg border px-2 py-2 text-center text-sm transition-colors {settingsStore
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
		<div class="px-4 py-3">
			<TextField
				type="select"
				label={m.settings_year()}
				value={String(settingsStore.settings.year)}
				options={yearOptions}
				onchange={(e) => {
					const n = Number((e.currentTarget as HTMLSelectElement).value);
					settingsStore.update({ year: n });
				}}
			/>
		</div>
	</SettingsSection>

	<!-- ===== Location ===== -->
	<SettingsSection id="settings-location" title={m.settings_location()}>
		<div class="space-y-3 px-4 py-3">
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
	<SettingsSection id="settings-home" title={m.settings_home()}>
		<div
			class="flex items-center justify-between border-b border-[var(--color-surface-border)] px-4 py-3"
		>
			<span class="text-sm text-[var(--color-nav-active)]">{m.settings_home_weather()}</span>
			<Switch
				checked={settingsStore.settings.homeShowWeather}
				onchange={(v) => settingsStore.update({ homeShowWeather: v })}
			/>
		</div>
		<div
			class="flex items-center justify-between border-b border-[var(--color-surface-border)] px-4 py-3"
		>
			<span class="text-sm text-[var(--color-nav-active)]">{m.settings_home_tools()}</span>
			<Switch
				checked={settingsStore.settings.homeShowTools}
				onchange={(v) => settingsStore.update({ homeShowTools: v })}
			/>
		</div>
		<div
			class="flex items-center justify-between border-b border-[var(--color-surface-border)] px-4 py-3"
		>
			<span class="text-sm text-[var(--color-nav-active)]">{m.settings_home_external_links()}</span>
			<Switch
				checked={settingsStore.settings.homeShowExternalLinks}
				onchange={(v) => settingsStore.update({ homeShowExternalLinks: v })}
			/>
		</div>
		<div class="border-b border-[var(--color-surface-border)] px-4 py-3">
			<label
				for="settings-home-search-provider"
				class="mb-2 block text-sm text-[var(--color-nav-active)]"
			>
				検索エンジン
			</label>
			<select
				id="settings-home-search-provider"
				value={settingsStore.settings.homeSearchProvider}
				onchange={(e) => {
					const target = e.currentTarget as HTMLSelectElement;
					settingsStore.update({ homeSearchProvider: target.value as SearchProviderId });
				}}
				class="w-full rounded-lg border border-[var(--color-surface-border)] bg-[var(--color-surface-card)] px-3 py-2 text-sm text-[var(--color-nav-active)] outline-none focus:border-[var(--color-primary-500)] focus:ring-2 focus:ring-[var(--color-primary-100)]"
			>
				{#each searchProviderOptions as opt (opt.value)}
					<option value={opt.value}>{opt.label}</option>
				{/each}
			</select>
		</div>
		<div class="flex items-center justify-between px-4 py-3">
			<span class="text-sm text-[var(--color-nav-active)]">{m.settings_open_links_in_app()}</span>
			<Switch
				checked={settingsStore.settings.openLinksInApp}
				onchange={(v) => settingsStore.update({ openLinksInApp: v })}
			/>
		</div>
	</SettingsSection>

	<!-- ===== Timetable ===== -->
	<SettingsSection id="settings-timetable" title={m.settings_timetable()}>
		<div class="border-b border-[var(--color-surface-border)] px-4 py-3">
			<TextField
				type="select"
				label={m.settings_timetable_day_range()}
				value={String(settingsStore.settings.timetableDayRange)}
				options={timetableDayOptions}
				onchange={(e) => {
					const value = Number((e.currentTarget as HTMLSelectElement).value) as 5 | 6 | 7;
					settingsStore.update({ timetableDayRange: value });
				}}
			/>
		</div>
		<div class="border-b border-[var(--color-surface-border)] px-4 py-3">
			<TextField
				type="select"
				label={m.settings_timetable_period_range()}
				value={String(settingsStore.settings.timetablePeriodRange)}
				options={timetablePeriodOptions}
				onchange={(e) => {
					const value = Number((e.currentTarget as HTMLSelectElement).value) as 4 | 5 | 6 | 7;
					settingsStore.update({ timetablePeriodRange: value });
				}}
			/>
		</div>
		<div class="border-b border-[var(--color-surface-border)] px-4 py-3">
			<TextField
				type="select"
				label={m.settings_timetable_ondemand_cols()}
				value={String(settingsStore.settings.timetableOndemandCols)}
				options={timetableOndemandOptions}
				onchange={(e) => {
					const value = Number((e.currentTarget as HTMLSelectElement).value) as 0 | 1 | 2 | 3;
					settingsStore.update({ timetableOndemandCols: value });
				}}
			/>
		</div>
		<div
			class="flex items-center justify-between border-b border-[var(--color-surface-border)] px-4 py-3"
		>
			<span class="text-sm text-[var(--color-nav-active)]"
				>{m.settings_timetable_transparent()}</span
			>
			<Switch
				checked={settingsStore.settings.timetableTransparent}
				onchange={(v) => settingsStore.update({ timetableTransparent: v })}
			/>
		</div>
		<div
			class="flex items-center justify-between border-b border-[var(--color-surface-border)] px-4 py-3"
		>
			<span class="text-sm text-[var(--color-nav-active)]">{m.settings_bg_image_all_pages()}</span>
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
						class="relative w-32 aspect-[3/4] rounded-lg overflow-hidden border border-[var(--color-surface-border)] shadow-sm bg-[var(--color-surface-muted)]"
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
						class="inline-flex h-8 cursor-pointer items-center justify-center gap-2 rounded-[var(--radius-chip)] border border-[var(--color-surface-border)] bg-[var(--color-surface-card)] px-3 text-sm font-medium text-[var(--color-nav-active)] transition-colors hover:bg-[var(--color-surface-muted)]"
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
							class="rounded-lg border border-[var(--color-surface-border)] bg-[var(--color-surface-card)] px-2 py-1 text-xs text-[var(--color-nav-active)] focus:outline-none focus:ring-1 focus:ring-[var(--color-primary-500)]"
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
							class="rounded-lg border border-[var(--color-surface-border)] bg-[var(--color-surface-card)] px-2 py-1 text-xs text-[var(--color-nav-active)] focus:outline-none focus:ring-1 focus:ring-[var(--color-primary-500)]"
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
	<SettingsSection id="settings-calendar" title={m.settings_calendar()}>
		<div class="border-b border-[var(--color-surface-border)] px-4 py-3">
			<div class="mb-2 text-sm font-medium text-[var(--color-nav-active)]">
				{m.settings_calendar_week_start()}
			</div>
			<div class="grid grid-cols-2 gap-2">
				{#each weekStartOptions as opt (opt.value)}
					<button
						type="button"
						class="rounded-lg border px-2 py-2 text-center text-sm transition-colors {settingsStore
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
	<SettingsSection id="settings-attendance" title={m.settings_attendance()}>
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

	<!-- ===== Data ===== -->
	<SettingsSection id="settings-data" title={m.settings_data()}>
		<div class="grid grid-cols-2 gap-3 border-b border-[var(--color-surface-border)] px-4 py-3">
			<Button variant="secondary" size="sm" onclick={handleExport}>
				<Download size={16} />
				{m.settings_data_export()}
			</Button>
			<label
				class="inline-flex h-8 cursor-pointer items-center justify-center gap-2 rounded-[var(--radius-chip)] border border-[var(--color-surface-border)] bg-[var(--color-surface-card)] px-3 text-sm font-medium text-[var(--color-nav-active)] transition-colors hover:bg-[var(--color-surface-muted)]"
			>
				<Upload size={16} />
				{m.settings_data_import()}
				<input type="file" accept="application/json" class="hidden" onchange={handleImport} />
			</label>
		</div>
		<div class="px-4 py-3">
			<Button variant="danger" size="sm" class="w-full" onclick={() => (showClearConfirm = true)}>
				<Trash2 size={16} />
				{m.settings_data_clear()}
			</Button>
		</div>
	</SettingsSection>

	<!-- ===== Account ===== -->
	<SettingsSection id="settings-account" title={m.settings_account()}>
		<div class="px-4 py-3">
			{#if userDataStore.rsConnected}
				<div class="mb-3 flex flex-col gap-1 text-sm text-[var(--color-nav-active)]">
					<div class="flex items-center gap-2">
						<Cloud size={16} class="text-[var(--color-success-500)]" />
						{m.settings_account_connected()}
					</div>
					{#if getConnectedUserAddress()}
						<div class="text-xs text-[var(--color-nav-inactive)] pl-6 font-mono">
							{getConnectedUserAddress()}
						</div>
					{/if}
				</div>
				<Button variant="secondary" size="sm" class="w-full" onclick={handleDisconnect}>
					<CloudOff size={16} />
					{m.settings_account_disconnect()}
				</Button>
			{:else}
				<div class="mb-3 flex items-center gap-2 text-sm text-[var(--color-nav-inactive)]">
					<CloudOff size={16} />
					{m.settings_account_disconnected()}
				</div>
				<Button variant="primary" size="sm" class="w-full" onclick={handleConnect}>
					<Cloud size={16} />
					{m.settings_account_connect()}
				</Button>
			{/if}
		</div>
	</SettingsSection>

	<!-- ===== About ===== -->
	<SettingsSection id="settings-about" title={m.settings_about()}>
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
			href="https://github.com/"
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
			class="rounded-lg border border-[var(--color-surface-border)] bg-[var(--color-surface-muted)]/50 p-3"
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

<!-- ===== Settings Search Palette ===== -->
<SettingsSearch items={searchItems} />
