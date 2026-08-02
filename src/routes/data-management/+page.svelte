<script lang="ts">
	import { settingsStore, uiStore, userDataStore } from '$lib/stores';
	import { m } from '$lib/paraglide/messages';
	import { schoolDataService } from '$lib/schoolData.svelte';
	import { sanitize } from '$lib/html-renderer';
	import PageHeader from '$lib/components/PageHeader.svelte';
	import Container from '$lib/components/Container.svelte';
	import TextField from '$lib/components/TextField.svelte';
	import Button from '$lib/components/Button.svelte';
	import Modal from '$lib/components/Modal.svelte';
	import SettingsSection from '$lib/components/settings/SettingsSection.svelte';
	import GlassContainer from '$lib/components/GlassContainer.svelte';
	import BentoCard from '$lib/components/BentoCard.svelte';
	import {
		connectRemoteStorage,
		disconnectRemoteStorage,
		getConnectedUserAddress
	} from '$lib/remotestorage';
	import {
		Cloud,
		CloudOff,
		Download,
		Upload,
		Trash2,
		RefreshCw,
		FileCode,
		Database,
		CheckCircle2,
		ChevronDown,
		ChevronUp
	} from '@lucide/svelte';
	import { styleStore } from '$lib/stores';

	const settings = $derived(settingsStore.settings);

	// 詳細設定トグルとバインド用変数
	let showAdvanced = $state(false);
	let localEndpoint = $state(settingsStore.settings.schoolDataSparqlEndpoint);
	let localSourceTitle = $state(settingsStore.settings.schoolDataSourceTitle);
	let localSourceUrl = $state(settingsStore.settings.schoolDataSourceUrl);
	let localLicenseTitle = $state(settingsStore.settings.schoolDataLicenseTitle);
	let localLicenseUrl = $state(settingsStore.settings.schoolDataLicenseUrl);

	$effect(() => {
		localEndpoint = settings.schoolDataSparqlEndpoint;
		localSourceTitle = settings.schoolDataSourceTitle;
		localSourceUrl = settings.schoolDataSourceUrl;
		localLicenseTitle = settings.schoolDataLicenseTitle;
		localLicenseUrl = settings.schoolDataLicenseUrl;
	});

	function updateSchoolSettings() {
		settingsStore.update({
			schoolDataSparqlEndpoint: localEndpoint,
			schoolDataSourceTitle: localSourceTitle,
			schoolDataSourceUrl: localSourceUrl,
			schoolDataLicenseTitle: localLicenseTitle,
			schoolDataLicenseUrl: localLicenseUrl
		});
	}

	// RemoteStorage関連
	let showConnectModal = $state(false);
	let connectAddress = $state('');
	const rsAddress = $derived(getConnectedUserAddress());

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

	// アプリデータインポート・エクスポート
	function handleExportAll() {
		const backupData = {
			settings: settingsStore.settings,
			courses: userDataStore.courses,
			todos: userDataStore.todos,
			events: userDataStore.events,
			attendance: userDataStore.attendance
		};
		const dataStr = JSON.stringify(backupData, null, 2);
		const blob = new Blob([dataStr], { type: 'application/json' });
		const url = URL.createObjectURL(blob);
		const a = document.createElement('a');
		a.href = url;
		a.download = `openyaske-backup-${new Date().toISOString().slice(0, 10)}.json`;
		a.click();
		URL.revokeObjectURL(url);
		uiStore.toast(m.toast_saved(), 'success');
	}

	function handleImportAll(e: Event) {
		const input = e.currentTarget as HTMLInputElement;
		const file = input.files?.[0];
		if (!file) return;

		file.text().then((text) => {
			try {
				const parsed = JSON.parse(text);
				if (parsed.settings) {
					settingsStore.update(parsed.settings);
				}
				if (Array.isArray(parsed.courses)) {
					userDataStore.courses = parsed.courses;
					localStorage.setItem('open-yaske:userdata:courses', JSON.stringify(parsed.courses));
				}
				if (Array.isArray(parsed.todos)) {
					userDataStore.todos = parsed.todos;
					localStorage.setItem('open-yaske:userdata:todos', JSON.stringify(parsed.todos));
				}
				if (Array.isArray(parsed.events)) {
					userDataStore.events = parsed.events;
					localStorage.setItem('open-yaske:userdata:events', JSON.stringify(parsed.events));
				}
				if (Array.isArray(parsed.attendance)) {
					userDataStore.attendance = parsed.attendance;
					localStorage.setItem('open-yaske:userdata:attendance', JSON.stringify(parsed.attendance));
				}

				if (userDataStore.rsConnected) {
					void userDataStore.init();
				}

				uiStore.toast(m.toast_saved(), 'success');
			} catch (err) {
				console.error('Import failed:', err);
				uiStore.toast(m.toast_error(), 'error');
			}
		});
		input.value = '';
	}

	// データ全クリア
	let showClearConfirm = $state(false);

	function handleClearAllData() {
		settingsStore.reset();
		userDataStore.courses = [];
		userDataStore.todos = [];
		userDataStore.events = [];
		userDataStore.attendance = [];
		localStorage.removeItem('open-yaske:userdata:courses');
		localStorage.removeItem('open-yaske:userdata:todos');
		localStorage.removeItem('open-yaske:userdata:events');
		localStorage.removeItem('open-yaske:userdata:attendance');
		void schoolDataService.clearCache();
		showClearConfirm = false;
		uiStore.toast(m.toast_deleted(), 'success');
	}

	// シラバスデータ同期関連
	let syncError = $state<string | null>(null);

	async function handleSyncSchoolData() {
		try {
			syncError = null;
			await schoolDataService.sync();
			uiStore.toast(m.school_data_sync_success(), 'success');
		} catch (e: any) {
			console.error(e);
			syncError = e.message || '同期エラーが発生しました';
			uiStore.toast(m.toast_error(), 'error');
		}
	}

	async function handleClearSchoolDataCache() {
		await schoolDataService.clearCache();
		uiStore.toast(m.toast_deleted(), 'success');
	}

	// カスタムデータ (JSON) インポート/エクスポート
	let customCoursesJSON = $state('');
	let showCustomModal = $state(false);

	function openCustomImport() {
		customCoursesJSON = JSON.stringify(schoolDataService.courseItems, null, 2);
		showCustomModal = true;
	}

	async function saveCustomData() {
		try {
			const courses = JSON.parse(customCoursesJSON);
			if (!Array.isArray(courses)) {
				throw new Error('JSON data must be array formats.');
			}
			await schoolDataService.importCustomCourses(courses);
			showCustomModal = false;
			uiStore.toast(m.toast_saved(), 'success');
		} catch (e: any) {
			alert(`インポートに失敗しました: ${e.message}`);
		}
	}

	function formatDate(timestamp: number) {
		return new Date(timestamp).toLocaleString();
	}
</script>

<svelte:head>
	<title>{m.more_data_management()} | OpenYASKE</title>
</svelte:head>

<PageHeader title={m.more_data_management()} />

<Container class="pb-16 max-w-4xl">
	<div class="space-y-6 py-4">
		<!-- 1. RemoteStorage セクション -->
		<SettingsSection
			id="data-remotestorage"
			title="RemoteStorage (個人データの同期)"
			description="カレンダー予定、TODO、時間割などのデータを、RemoteStorage サーバーを介して他のデバイスやブラウザと安全に同期・バックアップします。"
		>
			<div class="p-4 space-y-4">
				<div
					class="flex items-center gap-3.5 rounded-2xl bg-[var(--color-surface-muted)]/60 p-3.5 border border-[var(--color-surface-border)]/80 shadow-apple-xs"
				>
					{#if userDataStore.rsConnected}
						<div
							class="flex h-11 w-11 shrink-0 items-center justify-center rounded-2xl bg-[var(--color-success-500)]/15 text-[var(--color-success-600)] shadow-apple-sm"
						>
							<Cloud size={24} />
						</div>
						<div class="flex-1 min-w-0">
							<span class="text-xs font-bold text-[var(--color-success-700)] block">
								{m.settings_account_connected()}
							</span>
							<span
								class="text-[11px] font-mono text-[var(--color-nav-inactive)] truncate block mt-0.5"
							>
								{rsAddress || ''}
							</span>
						</div>
						<Button variant="secondary" size="sm" onclick={handleDisconnect}>
							{m.settings_account_disconnect()}
						</Button>
					{:else}
						<div
							class="flex h-11 w-11 shrink-0 items-center justify-center rounded-2xl bg-[var(--color-surface-muted)] text-[var(--color-nav-inactive)] border border-[var(--color-surface-border)]/50 shadow-apple-sm"
						>
							<CloudOff size={24} />
						</div>
						<div class="flex-1 min-w-0">
							<span class="text-xs font-bold text-[var(--color-nav-inactive)] block pl-1">
								{m.settings_account_disconnected()}
							</span>
						</div>
						<Button variant="primary" size="sm" onclick={handleConnect}>
							{m.settings_account_connect()}
						</Button>
					{/if}
				</div>
			</div>
		</SettingsSection>

		<!-- 2. シラバスデータ同期 (オフライン対応) セクション -->
		<SettingsSection
			id="data-schooldata"
			title="シラバスデータ同期 (オフライン対応)"
			description="大学のシラバス（科目）のマスターデータをローカルにキャッシュします。キャッシュ後は完全にオフラインで検索や時間割等での登録が可能です。"
		>
			<div class="p-4 space-y-4">
				{#if schoolDataService.metadata}
					<div
						class="flex items-center gap-3.5 rounded-2xl bg-[var(--color-success-50)]/80 backdrop-blur-xs p-3.5 text-xs text-[var(--color-success-700)] border border-[var(--color-success-200)]/60 shadow-apple-sm"
					>
						<CheckCircle2 size={18} class="text-[var(--color-success-500)] shrink-0" />
						<div class="flex-1 min-w-0">
							<span class="font-bold">{m.school_data_local_data_active()}</span>
							<span class="block text-[10px] text-[var(--color-success-600)] mt-1 font-mono">
								{m.school_data_last_sync({
									date: formatDate(schoolDataService.metadata.lastSyncedAt)
								})} | 科目: {schoolDataService.metadata.courseCount || 0}件
							</span>
						</div>
					</div>
				{:else}
					<div
						class="flex items-center gap-3.5 rounded-2xl bg-[var(--color-danger-50)]/80 backdrop-blur-xs p-3.5 text-xs text-[var(--color-danger-700)] border border-[var(--color-danger-200)]/60 shadow-apple-sm"
					>
						<Database size={18} class="text-[var(--color-danger-500)] shrink-0" />
						<span class="font-bold">{m.school_data_no_data()}</span>
					</div>
				{/if}

				<!-- クレジット・ライセンス表示 -->
				<div
					class="rounded-2xl bg-[var(--color-surface-muted)]/50 p-4 text-[11px] text-[var(--color-nav-inactive)] border border-[var(--color-surface-border)]/60 leading-5 shadow-apple-xs"
				>
					{@html sanitize(
						m.school_data_credit({
							source: `<a href="${settings.schoolDataSourceUrl}" target="_blank" rel="noopener noreferrer" class="font-bold underline text-[var(--color-primary-600)] hover:text-[var(--color-primary-700)]">${settings.schoolDataSourceTitle}</a>`,
							license: `<a href="${settings.schoolDataLicenseUrl}" target="_blank" rel="noopener noreferrer" class="font-bold underline text-[var(--color-primary-600)] hover:text-[var(--color-primary-700)]">${settings.schoolDataLicenseTitle}</a>`
						})
					)}
				</div>

				{#if syncError}
					<div
						class="rounded-2xl bg-[var(--color-danger-50)]/80 backdrop-blur-xs p-3.5 text-xs text-[var(--color-danger-700)] font-mono border border-[var(--color-danger-200)]/60 shadow-apple-sm"
					>
						{syncError}
					</div>
				{/if}

				{#if schoolDataService.loading && schoolDataService.syncStatus}
					<div
						class="rounded-2xl bg-[var(--color-surface-muted)]/60 p-3.5 text-xs text-[var(--color-nav-active)] border border-[var(--color-surface-border)]/60 flex items-center gap-2.5 shadow-apple-sm"
					>
						<RefreshCw size={14} class="animate-spin text-[var(--color-primary-500)]" />
						<span class="font-semibold">{schoolDataService.syncStatus}</span>
					</div>
				{/if}

				<div class="flex flex-wrap gap-2.5 pt-1">
					<Button
						variant="primary"
						size="md"
						class="flex-1 min-h-[44px]"
						onclick={handleSyncSchoolData}
						disabled={schoolDataService.loading}
					>
						<RefreshCw size={16} class={schoolDataService.loading ? 'animate-spin' : ''} />
						<span
							>{schoolDataService.loading
								? m.school_data_downloading()
								: m.school_data_offline_download()}</span
						>
					</Button>

					<Button
						variant="secondary"
						size="md"
						class="min-h-[44px]"
						onclick={openCustomImport}
						disabled={schoolDataService.loading}
					>
						<FileCode size={16} />
						<span>{m.school_data_edit_custom()}</span>
					</Button>

					{#if schoolDataService.metadata}
						<Button
							variant="danger"
							size="md"
							class="min-h-[44px]"
							onclick={handleClearSchoolDataCache}
							disabled={schoolDataService.loading}
						>
							<Trash2 size={16} />
							<span>{m.school_data_clear_cache()}</span>
						</Button>
					{/if}
				</div>

				<!-- 詳細設定アコーディオン -->
				<div class="border-t border-[var(--color-surface-border)]/50 pt-4 mt-2">
					<button
						type="button"
						class="flex w-full items-center justify-between py-1.5 text-xs font-bold text-[var(--color-nav-active)] hover:text-[var(--color-primary-600)] transition-all cursor-pointer {styleStore
							.pack.press.card}"
						onclick={() => (showAdvanced = !showAdvanced)}
					>
						<span>{m.school_data_advanced_settings()}</span>
						{#if showAdvanced}
							<ChevronUp size={16} class="text-[var(--color-primary-600)]" />
						{:else}
							<ChevronDown size={16} class="text-[var(--color-nav-inactive)]" />
						{/if}
					</button>

					{#if showAdvanced}
						<div class="pt-4 space-y-4 transition-all">
							<TextField
								bind:value={localEndpoint}
								label={m.school_data_sparql_endpoint()}
								placeholder="https://uec-atlas.org/sparql"
								onchange={updateSchoolSettings}
							/>
							<div class="grid grid-cols-1 sm:grid-cols-2 gap-4">
								<TextField
									bind:value={localSourceTitle}
									label={m.school_data_source_title()}
									placeholder="UEC Atlas"
									onchange={updateSchoolSettings}
								/>
								<TextField
									bind:value={localSourceUrl}
									label={m.school_data_source_url()}
									placeholder="https://uec-atlas.org/"
									onchange={updateSchoolSettings}
								/>
							</div>
							<div class="grid grid-cols-1 sm:grid-cols-2 gap-4">
								<TextField
									bind:value={localLicenseTitle}
									label={m.school_data_license_title()}
									placeholder="CC BY-NC-SA 4.0"
									onchange={updateSchoolSettings}
								/>
								<TextField
									bind:value={localLicenseUrl}
									label={m.school_data_license_url()}
									placeholder="https://creativecommons.org/licenses/by-nc-sa/4.0/"
									onchange={updateSchoolSettings}
								/>
							</div>
						</div>
					{/if}
				</div>
			</div>
		</SettingsSection>

		<!-- 3. アプリデータのバックアップ・クリア -->
		<SettingsSection
			id="data-tools"
			title="データ管理ツール"
			description="アプリ内のすべての設定、時間割、TODO、カレンダー、出勤履歴データをJSON形式でバックアップ・復元します。"
		>
			<div class="p-4 space-y-4">
				<div class="flex flex-col gap-3 sm:flex-row">
					<Button
						variant="secondary"
						size="md"
						class="flex-1 min-h-[44px]"
						onclick={handleExportAll}
					>
						<Download size={16} />
						<span>{m.settings_data_export()}</span>
					</Button>

					<div class="relative flex-1 min-h-[44px]">
						<input
							type="file"
							accept=".json"
							onchange={handleImportAll}
							class="absolute inset-0 opacity-0 cursor-pointer w-full h-full z-10"
							aria-label="ファイルをアップロード"
						/>
						<Button variant="secondary" size="md" class="w-full h-full min-h-[44px]">
							<Upload size={16} />
							<span>{m.settings_data_import()}</span>
						</Button>
					</div>

					<Button
						variant="danger"
						size="md"
						class="flex-1 min-h-[44px]"
						onclick={() => (showClearConfirm = true)}
					>
						<Trash2 size={16} />
						<span>{m.settings_data_clear()}</span>
					</Button>
				</div>
			</div>
		</SettingsSection>
	</div>
</Container>

<!-- RemoteStorage 接続モーダル -->
<Modal
	open={showConnectModal}
	onclose={() => (showConnectModal = false)}
	title={m.settings_account_connect()}
>
	<form
		onsubmit={(e) => {
			e.preventDefault();
			submitConnect();
		}}
		class="space-y-4"
	>
		<TextField
			bind:value={connectAddress}
			label={m.settings_account_remote_address_label()}
			placeholder={m.settings_account_remote_address_placeholder()}
			hint={m.settings_account_remote_address_hint()}
			required
		/>
		<div
			class="rounded-2xl bg-[var(--color-surface-muted)]/80 p-4 border border-[var(--color-surface-border)]/60 space-y-2 shadow-apple-xs"
		>
			<h4 class="text-xs font-bold text-[var(--color-nav-active)]">
				{m.settings_account_remote_guide_title()}
			</h4>
			<ol class="list-decimal pl-4 text-[11px] text-[var(--color-nav-inactive)] space-y-1.5">
				<li>{m.settings_account_remote_guide_step_1()}</li>
				<li>{m.settings_account_remote_guide_step_2()}</li>
				<li>{m.settings_account_remote_guide_step_3()}</li>
			</ol>
		</div>
		<div class="flex gap-2.5 pt-2">
			<Button
				variant="secondary"
				size="md"
				class="flex-1"
				onclick={() => (showConnectModal = false)}
			>
				{m.action_cancel()}
			</Button>
			<Button
				variant="primary"
				size="md"
				class="flex-1"
				type="submit"
				disabled={!connectAddress.trim()}
			>
				{m.settings_account_connect()}
			</Button>
		</div>
	</form>
</Modal>

<!-- クリア確認ダイアログ -->
<Modal
	open={showClearConfirm}
	onclose={() => (showClearConfirm = false)}
	title={m.settings_data_clear()}
>
	<div class="space-y-4">
		<p class="text-sm text-[var(--color-nav-active)] leading-relaxed">
			{m.settings_data_clear_confirm()}
		</p>
		<p
			class="text-xs text-[var(--color-danger-600)] font-bold bg-[var(--color-danger-50)]/80 backdrop-blur-xs border border-[var(--color-danger-200)]/60 p-3 rounded-2xl shadow-apple-sm"
		>
			※この操作は取り消せません。設定とすべての時間割、TODO、カレンダー、出席、学校キャッシュデータが完全に消去されます。
		</p>
		<div class="flex gap-2.5 pt-2">
			<Button
				variant="secondary"
				size="md"
				class="flex-1"
				onclick={() => (showClearConfirm = false)}
			>
				{m.action_cancel()}
			</Button>
			<Button variant="danger" size="md" class="flex-1" onclick={handleClearAllData}>
				{m.action_delete()}
			</Button>
		</div>
	</div>
</Modal>

<!-- カスタムデータインポート・エクスポートモーダル -->
<Modal
	open={showCustomModal}
	onclose={() => (showCustomModal = false)}
	title="カスタムシラバスデータの編集 (JSON)"
>
	<div class="space-y-4 max-h-[70vh] overflow-y-auto pr-1">
		<p class="text-xs text-[var(--color-nav-inactive)] leading-relaxed pl-1">
			シラバス科目（Courses）のデータをJSON形式で直接書き換えます。独自の授業情報を取り込むことができます。
		</p>

		<div class="space-y-3.5">
			<TextField
				type="textarea"
				id="custom-courses-json"
				bind:value={customCoursesJSON}
				label="シラバス科目データ (Courses)"
				rows={12}
			/>
		</div>

		<div class="flex gap-2.5 pt-2">
			<Button
				variant="secondary"
				size="md"
				class="flex-1"
				onclick={() => (showCustomModal = false)}
			>
				{m.action_cancel()}
			</Button>
			<Button variant="primary" size="md" class="flex-1" onclick={saveCustomData}>
				{m.action_save()}
			</Button>
		</div>
	</div>
</Modal>
