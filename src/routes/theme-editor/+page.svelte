<script lang="ts">
	import PageHeader from '$lib/components/PageHeader.svelte';
	import Container from '$lib/components/Container.svelte';
	import SectionHeader from '$lib/components/SectionHeader.svelte';
	import Button from '$lib/components/Button.svelte';
	import TextField from '$lib/components/TextField.svelte';
	import ColorScaleEditor from '$lib/components/theme-editor/ColorScaleEditor.svelte';
	import PaletteEditor from '$lib/components/theme-editor/PaletteEditor.svelte';
	import AttendanceColorEditor from '$lib/components/theme-editor/AttendanceColorEditor.svelte';
	import ThemePreview from '$lib/components/theme-editor/ThemePreview.svelte';
	import { m } from '$lib/paraglide/messages';
	import { themeStore } from '$lib/stores';
	import {
		BUILTIN_PACKS,
		validatePack,
		parsePack,
		loadPackFromUrl,
		applyTheme,
		type ThemePack,
		type ColorScale,
		type ColorEntry,
		type AttendanceColors
	} from '$lib/theme';
	import { ArrowLeft, Download, Upload, Save, Link } from '@lucide/svelte';

	// --- state ---
	let editingPack = $state<ThemePack>(structuredClone(BUILTIN_PACKS[0]) as ThemePack);
	let basePackId = $state<string>(BUILTIN_PACKS[0].id);
	let importUrl = $state('');
	let statusMessage = $state('');
	let statusError = $state(false);

	// --- derived ---
	const allPacks = $derived([...BUILTIN_PACKS, ...themeStore.customPacks]);

	// --- helpers ---
	function loadBasePack(id: string) {
		const pack = allPacks.find((p) => p.id === id);
		if (pack) {
			editingPack = structuredClone(pack) as ThemePack;
			basePackId = id;
		}
	}

	function showStatus(msg: string, isError = false) {
		statusMessage = msg;
		statusError = isError;
		setTimeout(() => {
			statusMessage = '';
		}, 3000);
	}

	// --- update handlers ---
	function updatePrimaryScale(scale: ColorScale) {
		editingPack = { ...editingPack, ui: { ...editingPack.ui, primary: scale } };
	}

	function updateSurface(field: keyof ThemePack['ui']['surface'], value: string) {
		editingPack = {
			...editingPack,
			ui: { ...editingPack.ui, surface: { ...editingPack.ui.surface, [field]: value } }
		};
	}

	function updateNav(field: keyof ThemePack['ui']['nav'], value: string) {
		editingPack = {
			...editingPack,
			ui: { ...editingPack.ui, nav: { ...editingPack.ui.nav, [field]: value } }
		};
	}

	function updateShadow(field: keyof ThemePack['ui']['shadow'], value: string) {
		editingPack = {
			...editingPack,
			ui: { ...editingPack.ui, shadow: { ...editingPack.ui.shadow, [field]: value } }
		};
	}

	function updateRadius(field: keyof ThemePack['ui']['radius'], value: string) {
		editingPack = {
			...editingPack,
			ui: { ...editingPack.ui, radius: { ...editingPack.ui.radius, [field]: value } }
		};
	}

	function updateCourseColors(colors: ColorEntry[]) {
		editingPack = {
			...editingPack,
			palette: { ...editingPack.palette, courseColors: colors }
		};
	}

	function updateTodoColors(colors: ColorEntry[]) {
		editingPack = {
			...editingPack,
			palette: { ...editingPack.palette, todoColors: colors }
		};
	}

	function updateEventColors(colors: ColorEntry[]) {
		editingPack = {
			...editingPack,
			palette: { ...editingPack.palette, eventColors: colors }
		};
	}

	function updateAttendance(attendance: AttendanceColors) {
		editingPack = { ...editingPack, attendance };
	}

	function updateName(value: string) {
		editingPack = { ...editingPack, name: value };
	}

	function updateId(value: string) {
		editingPack = { ...editingPack, id: value };
	}

	// --- surface/nav/shadow/radius color input handlers ---
	function handleHexInput<F extends string>(
		e: Event,
		field: F,
		updater: (field: F, value: string) => void
	) {
		const target = e.currentTarget as HTMLInputElement;
		updater(field, target.value);
	}

	function handleHexBlur<F extends string>(
		e: FocusEvent,
		currentValue: string,
		updater: (field: F, value: string) => void,
		field: F
	) {
		const target = e.currentTarget as HTMLInputElement;
		const val = target.value.trim();
		if (/^#[0-9a-fA-F]{6}$/.test(val)) {
			updater(field, val.toLowerCase());
		} else {
			target.value = currentValue;
		}
	}

	// --- export ---
	function exportPack() {
		const json = JSON.stringify(editingPack, null, 2);
		const blob = new Blob([json], { type: 'application/json' });
		const url = URL.createObjectURL(blob);
		const a = document.createElement('a');
		a.href = url;
		a.download = `${editingPack.id || 'theme'}.json`;
		a.click();
		URL.revokeObjectURL(url);
	}

	// --- import from file ---
	function handleImportFile(e: Event) {
		const input = e.currentTarget as HTMLInputElement;
		const file = input.files?.[0];
		if (!file) return;
		const reader = new FileReader();
		reader.onload = () => {
			const result = parsePack(reader.result as string);
			if (result.ok) {
				editingPack = result.pack;
				basePackId = result.pack.id;
				showStatus(m.theme_import_success());
			} else {
				showStatus(m.theme_import_invalid(), true);
			}
		};
		reader.readAsText(file);
		input.value = '';
	}

	// --- import from URL ---
	async function handleImportUrl() {
		if (!importUrl.trim()) return;
		const result = await loadPackFromUrl(importUrl.trim());
		if (result.ok) {
			editingPack = result.pack;
			basePackId = result.pack.id;
			importUrl = '';
			showStatus(m.theme_import_success());
		} else {
			showStatus(m.theme_import_invalid(), true);
		}
	}

	// --- save ---
	async function savePack() {
		// ensure id is set
		if (!editingPack.id.trim()) {
			editingPack = { ...editingPack, id: `custom-${Date.now().toString(36)}` };
		}
		// validate
		const result = validatePack(editingPack);
		if (!result.ok) {
			showStatus(m.theme_import_invalid(), true);
			return;
		}
		try {
			await themeStore.saveCustomPack(editingPack);
			themeStore.set(editingPack);
			showStatus(m.theme_editor_saved());
		} catch {
			showStatus(m.theme_editor_save_failed(), true);
		}
	}

	// --- apply preview live ---
	$effect(() => {
		applyTheme(editingPack);
	});

	// surface fields config
	const surfaceFields: { key: keyof ThemePack['ui']['surface']; label: string }[] = [
		{ key: 'page', label: 'Page' },
		{ key: 'card', label: 'Card' },
		{ key: 'border', label: 'Border' },
		{ key: 'muted', label: 'Muted' },
		{ key: 'mutedForeground', label: 'Muted FG' }
	];

	const navFields: { key: keyof ThemePack['ui']['nav']; label: string }[] = [
		{ key: 'bg', label: 'Background' },
		{ key: 'active', label: 'Active' },
		{ key: 'inactive', label: 'Inactive' },
		{ key: 'hover', label: 'Hover' }
	];

	const shadowFields: { key: keyof ThemePack['ui']['shadow']; label: string }[] = [
		{ key: 'sheet', label: 'Sheet' },
		{ key: 'card', label: 'Card' },
		{ key: 'modal', label: 'Modal' }
	];

	const radiusFields: { key: keyof ThemePack['ui']['radius']; label: string }[] = [
		{ key: 'pill', label: 'Pill' },
		{ key: 'card', label: 'Card' },
		{ key: 'chip', label: 'Chip' }
	];
</script>

<svelte:head><title>{m.theme_editor_title()} | open-nexus</title></svelte:head>

<PageHeader
	title={m.theme_editor_title()}
	leadingIcon={ArrowLeft}
	leadingAriaLabel={m.action_back()}
	onleading={() => history.back()}
/>

<Container>
	<!-- status message -->
	{#if statusMessage}
		<div
			class="mb-3 rounded-lg px-4 py-2 text-sm font-medium"
			class:bg-[var(--color-primary-100)]={!statusError}
			class:text-[var(--color-primary-700)]={!statusError}
			class:bg-[var(--color-attendance-absent-bg)]={statusError}
			class:text-[var(--color-attendance-absent-fg)]={statusError}
		>
			{statusMessage}
		</div>
	{/if}

	<!-- base theme selector -->
	<SectionHeader variant="light">{m.theme_editor_base()}</SectionHeader>
	<div class="mb-4 flex flex-wrap gap-2">
		{#each allPacks as pack (pack.id)}
			<button
				type="button"
				class="rounded-[var(--radius-chip)] border px-3 py-1.5 text-sm font-medium transition-colors"
				class:border-[var(--color-primary-500)]={basePackId === pack.id}
				class:bg-[var(--color-primary-50)]={basePackId === pack.id}
				class:text-[var(--color-primary-700)]={basePackId === pack.id}
				class:border-[var(--color-surface-border)]={basePackId !== pack.id}
				class:bg-[var(--color-surface-card)]={basePackId !== pack.id}
				class:text-[var(--color-nav-active)]={basePackId !== pack.id}
				onclick={() => loadBasePack(pack.id)}
			>
				{pack.name || pack.id}
			</button>
		{/each}
	</div>

	<!-- pack metadata -->
	<div class="mb-4 grid grid-cols-1 gap-3 sm:grid-cols-2">
		<TextField
			value={editingPack.name}
			label={m.theme_editor_name()}
			placeholder={m.theme_editor_name_placeholder()}
			oninput={(e) => updateName((e.currentTarget as HTMLInputElement).value)}
		/>
		<TextField
			value={editingPack.id}
			label={m.theme_editor_id()}
			placeholder="my-theme"
			oninput={(e) => updateId((e.currentTarget as HTMLInputElement).value)}
		/>
	</div>

	<!-- live preview -->
	<SectionHeader variant="light">{m.theme_editor_preview()}</SectionHeader>
	<div class="mb-4">
		<ThemePreview pack={editingPack} />
	</div>

	<!-- UI Colors: Primary Scale -->
	<SectionHeader>{m.theme_editor_ui_colors()}</SectionHeader>
	<div
		class="mb-4 rounded-xl border border-[var(--color-surface-border)] bg-[var(--color-surface-card)] p-4"
	>
		<h3 class="mb-3 text-sm font-semibold text-[var(--color-nav-active)]">
			{m.theme_editor_primary_scale()}
		</h3>
		<ColorScaleEditor scale={editingPack.ui.primary} onchange={updatePrimaryScale} />
	</div>

	<!-- Surface -->
	<div
		class="mb-4 rounded-xl border border-[var(--color-surface-border)] bg-[var(--color-surface-card)] p-4"
	>
		<h3 class="mb-3 text-sm font-semibold text-[var(--color-nav-active)]">
			{m.theme_editor_surface()}
		</h3>
		<div class="grid grid-cols-2 gap-2 sm:grid-cols-3">
			{#each surfaceFields as { key, label } (key)}
				{@const val = editingPack.ui.surface[key]}
				<div
					class="flex items-center gap-2 rounded-lg border border-[var(--color-surface-border)] px-2 py-1.5"
				>
					<label
						class="relative h-7 w-7 shrink-0 cursor-pointer overflow-hidden rounded-md border border-[var(--color-surface-border)]"
						style="background-color: {val}"
					>
						<input
							type="color"
							value={val}
							oninput={(e) => handleHexInput(e, key, updateSurface)}
							class="absolute inset-0 cursor-pointer opacity-0"
							aria-label={label}
						/>
					</label>
					<div class="min-w-0 flex-1">
						<div class="text-[10px] text-[var(--color-nav-inactive)]">{label}</div>
						<input
							type="text"
							value={val}
							onblur={(e) => handleHexBlur(e, val, updateSurface, key)}
							class="w-full bg-transparent text-xs text-[var(--color-nav-active)] focus:outline-none"
							maxlength="7"
						/>
					</div>
				</div>
			{/each}
		</div>
	</div>

	<!-- Nav -->
	<div
		class="mb-4 rounded-xl border border-[var(--color-surface-border)] bg-[var(--color-surface-card)] p-4"
	>
		<h3 class="mb-3 text-sm font-semibold text-[var(--color-nav-active)]">
			{m.theme_editor_nav()}
		</h3>
		<div class="grid grid-cols-2 gap-2 sm:grid-cols-4">
			{#each navFields as { key, label } (key)}
				{@const val = editingPack.ui.nav[key]}
				<div
					class="flex items-center gap-2 rounded-lg border border-[var(--color-surface-border)] px-2 py-1.5"
				>
					<label
						class="relative h-7 w-7 shrink-0 cursor-pointer overflow-hidden rounded-md border border-[var(--color-surface-border)]"
						style="background-color: {val}"
					>
						<input
							type="color"
							value={val}
							oninput={(e) => handleHexInput(e, key, updateNav)}
							class="absolute inset-0 cursor-pointer opacity-0"
							aria-label={label}
						/>
					</label>
					<div class="min-w-0 flex-1">
						<div class="text-[10px] text-[var(--color-nav-inactive)]">{label}</div>
						<input
							type="text"
							value={val}
							onblur={(e) => handleHexBlur(e, val, updateNav, key)}
							class="w-full bg-transparent text-xs text-[var(--color-nav-active)] focus:outline-none"
							maxlength="7"
						/>
					</div>
				</div>
			{/each}
		</div>
	</div>

	<!-- Shadow -->
	<div
		class="mb-4 rounded-xl border border-[var(--color-surface-border)] bg-[var(--color-surface-card)] p-4"
	>
		<h3 class="mb-3 text-sm font-semibold text-[var(--color-nav-active)]">
			{m.theme_editor_shadow()}
		</h3>
		<div class="space-y-2">
			{#each shadowFields as { key, label } (key)}
				<div class="flex items-center gap-2">
					<span class="w-16 text-xs text-[var(--color-nav-inactive)]">{label}</span>
					<input
						type="text"
						value={editingPack.ui.shadow[key]}
						oninput={(e) => updateShadow(key, (e.currentTarget as HTMLInputElement).value)}
						class="flex-1 rounded-lg border border-[var(--color-surface-border)] bg-[var(--color-surface-page)] px-2 py-1 text-xs text-[var(--color-nav-active)] focus:outline-none focus:ring-1 focus:ring-[var(--color-primary-500)]"
					/>
				</div>
			{/each}
		</div>
	</div>

	<!-- Radius -->
	<div
		class="mb-4 rounded-xl border border-[var(--color-surface-border)] bg-[var(--color-surface-card)] p-4"
	>
		<h3 class="mb-3 text-sm font-semibold text-[var(--color-nav-active)]">
			{m.theme_editor_radius()}
		</h3>
		<div class="space-y-2">
			{#each radiusFields as { key, label } (key)}
				<div class="flex items-center gap-2">
					<span class="w-16 text-xs text-[var(--color-nav-inactive)]">{label}</span>
					<input
						type="text"
						value={editingPack.ui.radius[key]}
						oninput={(e) => updateRadius(key, (e.currentTarget as HTMLInputElement).value)}
						class="flex-1 rounded-lg border border-[var(--color-surface-border)] bg-[var(--color-surface-page)] px-2 py-1 text-xs text-[var(--color-nav-active)] focus:outline-none focus:ring-1 focus:ring-[var(--color-primary-500)]"
					/>
				</div>
			{/each}
		</div>
	</div>

	<!-- Palette -->
	<SectionHeader>{m.theme_editor_palette()}</SectionHeader>
	<div
		class="mb-4 rounded-xl border border-[var(--color-surface-border)] bg-[var(--color-surface-card)] p-4"
	>
		<h3 class="mb-3 text-sm font-semibold text-[var(--color-nav-active)]">
			{m.theme_editor_course_colors()}
		</h3>
		<PaletteEditor colors={editingPack.palette.courseColors} onchange={updateCourseColors} />
	</div>
	<div
		class="mb-4 rounded-xl border border-[var(--color-surface-border)] bg-[var(--color-surface-card)] p-4"
	>
		<h3 class="mb-3 text-sm font-semibold text-[var(--color-nav-active)]">
			{m.theme_editor_todo_colors()}
		</h3>
		<PaletteEditor colors={editingPack.palette.todoColors} onchange={updateTodoColors} />
	</div>
	<div
		class="mb-4 rounded-xl border border-[var(--color-surface-border)] bg-[var(--color-surface-card)] p-4"
	>
		<h3 class="mb-3 text-sm font-semibold text-[var(--color-nav-active)]">
			{m.theme_editor_event_colors()}
		</h3>
		<PaletteEditor colors={editingPack.palette.eventColors} onchange={updateEventColors} />
	</div>

	<!-- Attendance -->
	<SectionHeader>{m.theme_editor_attendance_colors()}</SectionHeader>
	<div
		class="mb-4 rounded-xl border border-[var(--color-surface-border)] bg-[var(--color-surface-card)] p-4"
	>
		<AttendanceColorEditor attendance={editingPack.attendance} onchange={updateAttendance} />
	</div>

	<!-- Actions -->
	<div class="mb-6 flex flex-wrap gap-2">
		<Button variant="primary" onclick={savePack}>
			<Save size={16} />
			{m.theme_editor_save()}
		</Button>
		<Button variant="secondary" onclick={exportPack}>
			<Download size={16} />
			{m.theme_export()}
		</Button>
		<label
			class="inline-flex h-10 cursor-pointer items-center justify-center gap-2 rounded-[var(--radius-chip)] border border-[var(--color-surface-border)] bg-[var(--color-surface-card)] px-4 text-sm font-medium text-[var(--color-nav-active)] transition-colors hover:bg-[var(--color-surface-muted)]"
		>
			<Upload size={16} />
			{m.theme_pick_import()}
			<input type="file" accept="application/json" onchange={handleImportFile} class="hidden" />
		</label>
	</div>

	<!-- Import from URL -->
	<div class="mb-6 flex gap-2">
		<div class="flex-1">
			<TextField
				value={importUrl}
				placeholder="https://..."
				leadingIcon={Link}
				oninput={(e) => (importUrl = (e.currentTarget as HTMLInputElement).value)}
			/>
		</div>
		<Button variant="secondary" onclick={handleImportUrl}>
			{m.theme_editor_import_url()}
		</Button>
	</div>
</Container>
