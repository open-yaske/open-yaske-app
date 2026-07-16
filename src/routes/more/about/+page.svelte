<script lang="ts">
	import { settingsStore } from '$lib/stores';
	import { m } from '$lib/paraglide/messages';
	import { sanitize } from '$lib/html-renderer';
	import PageHeader from '$lib/components/PageHeader.svelte';
	import Container from '$lib/components/Container.svelte';
	import SettingsSection from '$lib/components/settings/SettingsSection.svelte';
	import { Shield, Database, BookOpen, ExternalLink, Link2 } from '@lucide/svelte';
	import { openLink } from '$lib/browser';

	const settings = $derived(settingsStore.settings);

	function handleOpenLink(url: string) {
		void openLink(url, true);
	}

	// サードパーティライブラリ定義
	const libraries = [
		{
			name: 'Svelte 5',
			license: 'MIT',
			desc: 'Svelte team. Reactive component compiler.',
			url: 'https://svelte.dev'
		},
		{
			name: 'SvelteKit',
			license: 'MIT',
			desc: 'Application framework for Svelte.',
			url: 'https://kit.svelte.dev'
		},
		{
			name: 'Tailwind CSS',
			license: 'MIT',
			desc: 'A utility-first CSS framework.',
			url: 'https://tailwindcss.com'
		},
		{
			name: 'Capacitor',
			license: 'MIT',
			desc: 'Cross-platform runtime for Android, iOS, and Web.',
			url: 'https://capacitorjs.com'
		},
		{
			name: 'remoteStorage.js',
			license: 'MIT',
			desc: 'Offline-first, user-controlled data synchronization.',
			url: 'https://remotestorage.io'
		},
		{
			name: 'Lucide Svelte',
			license: 'ISC',
			desc: 'Beautiful & consistent icon library.',
			url: 'https://lucide.dev'
		},
		{
			name: '@inlang/paraglide-js',
			license: 'Apache-2.0',
			desc: 'High-performance i18n compiler.',
			url: 'https://inlang.com'
		},
		{
			name: 'DOMPurify',
			license: 'Apache-2.0',
			desc: 'DOM-only, super-fast, HTML-sanitizer.',
			url: 'https://github.com/cure53/DOMPurify'
		},
		{
			name: 'jsPDF',
			license: 'MIT',
			desc: 'Client-side PDF document generation.',
			url: 'https://github.com/parallax/jsPDF'
		},
		{
			name: 'idb-keyval',
			license: 'Apache-2.0',
			desc: 'A super-simple key-value store backed by IndexedDB.',
			url: 'https://github.com/jakearchibald/idb-keyval'
		},
		{
			name: 'marked',
			license: 'MIT',
			desc: 'A markdown parser and compiler built for speed.',
			url: 'https://marked.js.org'
		},
		{
			name: 'Katex',
			license: 'MIT',
			desc: 'Fast math typesetting library for the web.',
			url: 'https://katex.org'
		},
		{
			name: 'Mermaid',
			license: 'MIT',
			desc: 'JavaScript-based diagramming and charting tool.',
			url: 'https://mermaid.js.org'
		},
		{
			name: 'date-holidays',
			license: 'ISC',
			desc: 'Determine state, federal, and religious holidays.',
			url: 'https://github.com/commenthol/date-holidays'
		}
	];
</script>

<svelte:head>
	<title>{m.about_app_title()} | OpenYASKE</title>
</svelte:head>

<PageHeader title={m.about_app_title()} />

<Container class="pb-12">
	<div class="space-y-6 py-4">
		<!-- 概要カード -->
		<div
			class="relative overflow-hidden rounded-card border border-[var(--color-surface-border)] bg-[var(--color-surface-card)] p-6"
		>
			<!-- 装飾的な背景グラデーション -->
			<div
				class="absolute -right-16 -top-16 h-36 w-36 rounded-full bg-gradient-to-br from-[var(--color-primary-100)] to-[var(--color-primary-50)] opacity-40 blur-2xl"
			></div>

			<div class="relative space-y-3">
				<h3 class="text-base font-extrabold text-[var(--color-primary-700)]">OpenYASKE</h3>
				<p class="text-xs text-[var(--color-nav-active)] leading-5">
					{m.about_app_description()}
				</p>
				<div class="flex items-center gap-2 pt-2">
					<button
						type="button"
						onclick={() => handleOpenLink('https://github.com/open-yaske/open-yaske-app')}
						class="flex items-center gap-1.5 rounded-chip bg-[var(--color-surface-muted)] hover:bg-[var(--color-primary-50)] hover:text-[var(--color-primary-600)] border border-[var(--color-surface-border)] px-3 py-1.5 text-[11px] font-bold text-[var(--color-nav-active)] transition-all"
					>
						<Link2 size={13} />
						<span>GitHub Repository</span>
						<ExternalLink size={10} class="opacity-60" />
					</button>
				</div>
			</div>
		</div>

		<SettingsSection title={m.about_app_license()}>
			<div class="p-4 space-y-3">
				<div class="space-y-2 text-xs leading-5 text-[var(--color-nav-inactive)]">
					<p class="font-semibold text-[var(--color-nav-active)]">
						{m.about_app_license_desc()}
					</p>
					<pre
						class="bg-[var(--color-surface-muted)] border border-[var(--color-surface-border)] p-3 rounded-card font-mono text-[10px] text-[var(--color-nav-active)] overflow-x-auto max-h-36 whitespace-pre-wrap leading-normal">
MIT License

Copyright (c) 2026 OpenYASKE Contributors

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all
copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
SOFTWARE.</pre>
				</div>
			</div>
		</SettingsSection>

		<!-- データライセンス -->
		<SettingsSection title={m.about_app_data_license()}>
			<div class="p-4 space-y-3">
				<div class="space-y-3 text-xs leading-5 text-[var(--color-nav-inactive)]">
					<p>
						{m.about_app_data_license_desc()}
					</p>

					<div
						class="rounded-card border border-[var(--color-surface-border)] bg-[var(--color-surface-muted)] p-4 space-y-2"
					>
						<div
							class="flex flex-col sm:flex-row justify-between sm:items-center gap-1.5 border-b border-[var(--color-surface-border)] pb-2 mb-1"
						>
							<span class="font-bold text-[var(--color-nav-active)]">現在のデータソース</span>
							<button
								type="button"
								onclick={() => handleOpenLink(settings.schoolDataSourceUrl)}
								class="text-[11px] font-bold text-[var(--color-primary-600)] hover:underline flex items-center gap-1 text-left"
							>
								<span>{settings.schoolDataSourceTitle}</span>
								<ExternalLink size={10} />
							</button>
						</div>
						<div class="flex flex-col sm:flex-row justify-between sm:items-center gap-1.5">
							<span class="font-bold text-[var(--color-nav-active)]">適用ライセンス</span>
							<button
								type="button"
								onclick={() => handleOpenLink(settings.schoolDataLicenseUrl)}
								class="text-[11px] font-bold text-[var(--color-primary-600)] hover:underline flex items-center gap-1 text-left"
							>
								<span>{settings.schoolDataLicenseTitle}</span>
								<ExternalLink size={10} />
							</button>
						</div>
					</div>

					<p class="text-[11px]">
						※データ構造やメタデータの利用規約、商用利用や二次配布に関する詳細情報につきましては、該当するデータソースの公式Webサイト（例:
						<a
							href="https://uec-atlas.org/about/#terms"
							target="_blank"
							rel="noopener noreferrer"
							class="underline text-[var(--color-primary-600)] font-bold">UEC Atlas 利用規約</a
						>）等をご参照ください。
					</p>
				</div>
			</div>
		</SettingsSection>

		<!-- ライブラリライセンス -->
		<SettingsSection title={m.about_app_library_license()}>
			<div class="p-4 space-y-4">
				<p class="text-xs text-[var(--color-nav-inactive)]">
					{m.about_app_library_license_desc()}
				</p>

				<div class="grid grid-cols-1 md:grid-cols-2 gap-3 pt-1">
					{#each libraries as lib}
						<button
							type="button"
							onclick={() => handleOpenLink(lib.url)}
							class="flex flex-col rounded-card border border-[var(--color-surface-border)] bg-[var(--color-surface-muted)] p-3 text-left transition-colors hover:bg-[var(--color-surface-card)] group"
						>
							<div class="flex w-full items-center justify-between">
								<span
									class="font-bold text-xs text-[var(--color-nav-active)] group-hover:text-[var(--color-primary-600)] transition-colors"
								>
									{lib.name}
								</span>
								<span
									class="rounded bg-[var(--color-primary-50)] px-1.5 py-0.5 text-[9px] font-bold text-[var(--color-primary-700)]"
								>
									{lib.license}
								</span>
							</div>
							<p
								class="text-[10px] text-[var(--color-nav-inactive)] mt-1.5 leading-normal truncate w-full"
							>
								{lib.desc}
							</p>
						</button>
					{/each}
				</div>
			</div>
		</SettingsSection>
	</div>
</Container>
