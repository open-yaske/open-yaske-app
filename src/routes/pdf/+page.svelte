<script lang="ts">
	import { Camera, Image, Lightbulb, CheckCircle2, X } from '@lucide/svelte';
	import PageHeader from '$lib/components/PageHeader.svelte';
	import Container from '$lib/components/Container.svelte';
	import IconButton from '$lib/components/IconButton.svelte';
	import Button from '$lib/components/Button.svelte';
	import BentoCard from '$lib/components/BentoCard.svelte';
	import TextField from '$lib/components/TextField.svelte';
	import { DocumentScanner } from '@capgo/capacitor-document-scanner';
	import { Capacitor } from '@capacitor/core';
	import { m } from '$lib/paraglide/messages';
	import { styleStore } from '$lib/stores';

	let fileInput = $state<HTMLInputElement | null>(null);
	let cameraInput = $state<HTMLInputElement | null>(null);
	let images = $state<{ name: string; url: string }[]>([]);
	let filename = $state('');
	let pageSize = $state<'a4' | 'original'>('a4');
	let error = $state('');

	function handleClose() {
		if (typeof window !== 'undefined') {
			window.history.back();
		}
	}

	function triggerSelectFiles() {
		fileInput?.click();
	}

	async function startScan() {
		if (Capacitor.isNativePlatform()) {
			try {
				const { scannedImages, status } = await DocumentScanner.scanDocument();
				if (status === 'success' && scannedImages && scannedImages.length > 0) {
					error = '';
					if (images.length + scannedImages.length > 30) {
						error = m.pdf_error_max_images();
						return;
					}

					for (const path of scannedImages) {
						try {
							const dataUrl = await convertFileToDataUrl(path);
							images = [...images, { name: `scan_${Date.now()}.jpg`, url: dataUrl }];
						} catch (err) {
							console.error('Failed to convert scanned image to data URL:', err);
							error = m.pdf_error_failed_to_load();
						}
					}
				}
			} catch (e) {
				console.error('Document scanning failed:', e);
				error = m.pdf_error_failed_to_scan();
			}
		} else {
			cameraInput?.click();
		}
	}

	async function convertFileToDataUrl(filePath: string): Promise<string> {
		const webPath = Capacitor.convertFileSrc(filePath);
		const response = await fetch(webPath);
		const blob = await response.blob();
		return new Promise((resolve, reject) => {
			const reader = new FileReader();
			reader.onerror = reject;
			reader.onload = () => {
				resolve(reader.result as string);
			};
			reader.readAsDataURL(blob);
		});
	}

	function handleFileChange(e: Event) {
		const target = e.target as HTMLInputElement;
		if (!target.files) return;

		const files = Array.from(target.files);
		if (images.length + files.length > 30) {
			error = m.pdf_error_max_images();
			return;
		}

		error = '';
		for (const file of files) {
			if (!file.type.startsWith('image/')) {
				error = m.pdf_error_image_only();
				continue;
			}

			const reader = new FileReader();
			reader.onload = (event) => {
				if (event.target?.result) {
					images = [...images, { name: file.name, url: event.target.result as string }];
				}
			};
			reader.readAsDataURL(file);
		}
	}

	function removeImage(index: number) {
		images = images.filter((_, i) => i !== index);
	}

	async function handleExport() {
		if (images.length === 0) {
			error = m.pdf_error_no_images();
			return;
		}

		try {
			const { jsPDF } = await import('jspdf');
			const doc = new jsPDF({
				orientation: 'portrait',
				unit: 'mm',
				format: 'a4'
			});

			images.forEach((img, index) => {
				if (index > 0) {
					doc.addPage();
				}
				doc.addImage(img.url, 'JPEG', 10, 10, 190, 277);
			});

			const dateStr = new Date().toISOString().replace(/[-:T]/g, '').slice(0, 14);
			const outName = filename.trim()
				? `${filename.trim()}_${dateStr}.pdf`
				: `photos_${dateStr}.pdf`;

			doc.save(outName);
		} catch (e) {
			error = m.pdf_error_failed_to_export();
			console.error(e);
		}
	}
</script>

<svelte:head>
	<title>{m.more_pdf()} | OpenYASKE</title>
</svelte:head>

<div class="min-h-screen bg-[var(--color-surface-page)] pb-16">
	{#snippet leadingSnippet()}
		<IconButton
			icon={X}
			ariaLabel={m.course_cancel()}
			onclick={handleClose}
			variant="subtle"
			size="sm"
		/>
	{/snippet}

	<PageHeader title={m.more_pdf()} leading={leadingSnippet} />

	<Container size="narrow" class="py-4 space-y-6">
		<input
			type="file"
			accept="image/*"
			multiple
			bind:this={fileInput}
			onchange={handleFileChange}
			class="hidden"
		/>

		<input
			type="file"
			accept="image/*"
			capture="environment"
			bind:this={cameraInput}
			onchange={handleFileChange}
			class="hidden"
		/>

		<!-- 画像追加エリア -->
		<BentoCard padding="p-5" class="space-y-5">
			<div class="flex flex-col items-center justify-center py-4 text-center">
				<div
					class="flex h-14 w-14 items-center justify-center rounded-2xl bg-[var(--color-primary-500)]/15 text-[var(--color-primary-600)] mb-4 shadow-[var(--shadow-style-sm)]"
				>
					<Camera size={26} />
				</div>
				<h3 class="text-base font-bold text-[var(--color-nav-active)]">{m.pdf_add_image()}</h3>
				<p class="text-xs text-[var(--color-nav-inactive)] mt-1.5 font-medium">
					{m.pdf_scan_tip()}
				</p>
			</div>

			<div class="grid grid-cols-1 gap-3 sm:grid-cols-2">
				<!-- 書類をスキャン -->
				<button
					type="button"
					onclick={startScan}
					class="flex flex-col items-center justify-center gap-1 rounded-xl bg-[var(--color-primary-800)] py-3 px-4 text-[var(--color-primary-50)] shadow-[var(--shadow-style-sm)] min-h-[52px] {styleStore
						.pack.press.button}"
				>
					<span class="flex items-center gap-1.5 text-sm font-bold">
						<Camera size={18} />
						{m.pdf_scan_button()}
					</span>
					<span class="text-[9px] text-[var(--color-primary-200)]">{m.pdf_scan_desc()}</span>
				</button>

				<!-- 写真を選ぶ -->
				<button
					type="button"
					onclick={triggerSelectFiles}
					class="flex flex-col items-center justify-center gap-1 rounded-xl border border-[var(--color-surface-border)] bg-[var(--color-surface-card)] py-3 px-4 text-[var(--color-nav-active)] shadow-[var(--shadow-style-sm)] min-h-[52px] {styleStore
						.pack.press.button}"
				>
					<span class="flex items-center gap-1.5 text-sm font-bold">
						<Image size={18} class="text-[var(--color-primary-600)]" />
						{m.pdf_select_button()}
					</span>
					<span class="text-[9px] text-[var(--color-nav-inactive)]">{m.pdf_select_desc()}</span>
				</button>
			</div>

			<!-- エラーメッセージ -->
			{#if error}
				<p
					class="text-xs font-semibold text-[var(--color-attendance-absent-fg)] text-center bg-[var(--color-attendance-absent-bg)] rounded-xl py-2 px-3 shadow-[var(--shadow-style-sm)] border border-[var(--color-surface-border)]/50"
				>
					{error}
				</p>
			{/if}

			<!-- アドバイス -->
			<div
				class="rounded-xl bg-[var(--color-surface-muted)]/80 p-4 space-y-2 text-xs text-[var(--color-nav-active)] border border-[var(--color-surface-border)]/40 shadow-[var(--shadow-style-sm)]"
			>
				<p class="font-bold flex items-center gap-1.5 text-[var(--color-primary-600)]">
					<Lightbulb size={14} />
					{m.pdf_advice_title()}
				</p>
				<ul class="space-y-1.5 pl-1 text-[var(--color-nav-inactive)]">
					<li class="flex items-start gap-1.5">
						<CheckCircle2 size={12} class="mt-0.5 text-[var(--color-primary-600)] shrink-0" />
						<span>{m.pdf_advice_1()}</span>
					</li>
					<li class="flex items-start gap-1.5">
						<CheckCircle2 size={12} class="mt-0.5 text-[var(--color-primary-600)] shrink-0" />
						<span>{m.pdf_advice_2()}</span>
					</li>
					<li class="flex items-start gap-1.5">
						<CheckCircle2 size={12} class="mt-0.5 text-[var(--color-primary-600)] shrink-0" />
						<span>{m.pdf_advice_3()}</span>
					</li>
				</ul>
			</div>
		</BentoCard>

		<!-- プレビューエリア -->
		{#if images.length > 0}
			<BentoCard padding="p-4">
				<h4 class="text-xs font-bold text-[var(--color-nav-inactive)] mb-3">
					{m.pdf_preview_title({ count: images.length })}
				</h4>
				<div class="grid grid-cols-3 gap-3 sm:grid-cols-4 md:grid-cols-5">
					{#each images as img, index (img.url)}
						<div
							class="relative aspect-[3/4] rounded-lg border border-[var(--color-surface-border)] overflow-hidden bg-[var(--color-surface-muted)]/50 group"
						>
							<img src={img.url} alt={img.name} class="h-full w-full object-cover" />
							<button
								type="button"
								onclick={() => removeImage(index)}
								class="absolute top-1 right-1 flex h-6 w-6 items-center justify-center rounded-full bg-black/60 text-white hover:bg-black/80 transition-all {styleStore
									.pack.press.icon}"
								aria-label={m.course_delete()}
							>
								<X size={12} />
							</button>
						</div>
					{/each}
				</div>
			</BentoCard>
		{/if}

		<!-- 設定エリア -->
		<BentoCard padding="p-5" class="space-y-4">
			<!-- ファイル名 -->
			<div class="space-y-1.5">
				<TextField
					bind:value={filename}
					id="pdf-filename"
					label={m.pdf_filename_label()}
					placeholder={m.pdf_filename_placeholder()}
				/>
				<p class="text-[10px] text-[var(--color-nav-inactive)] pl-1 leading-normal">
					{m.pdf_filename_desc()}
				</p>
			</div>

			<!-- ページサイズ -->
			<div class="space-y-2">
				<span class="text-xs font-semibold text-[var(--color-nav-inactive)] pl-1"
					>{m.pdf_pagesize_label()}</span
				>
				<div
					class="grid grid-cols-2 p-1 rounded-xl bg-[var(--color-surface-muted)]/80 border border-[var(--color-surface-border)]/50 shadow-[var(--shadow-style-sm)]"
				>
					<button
						type="button"
						onclick={() => (pageSize = 'a4')}
						class="rounded-lg py-2 text-xs font-bold text-center transition-all {styleStore.pack
							.press.tab}"
						class:bg-[var(--color-surface-card)]={pageSize === 'a4'}
						class:text-[var(--color-primary-600)]={pageSize === 'a4'}
						class:shadow-[var(--shadow-style-sm)]={pageSize === 'a4'}
						class:text-[var(--color-nav-inactive)]={pageSize !== 'a4'}
					>
						A4
					</button>
					<button
						type="button"
						onclick={() => (pageSize = 'original')}
						class="rounded-lg py-2 text-xs font-bold text-center transition-all {styleStore.pack
							.press.tab}"
						class:bg-[var(--color-surface-card)]={pageSize === 'original'}
						class:text-[var(--color-primary-600)]={pageSize === 'original'}
						class:shadow-[var(--shadow-style-sm)]={pageSize === 'original'}
						class:text-[var(--color-nav-inactive)]={pageSize !== 'original'}
					>
						{m.pdf_pagesize_original()}
					</button>
				</div>
			</div>
		</BentoCard>

		<!-- PDF生成ボタン -->
		<Button
			onclick={handleExport}
			variant="primary"
			size="lg"
			class="w-full font-bold shadow-[var(--shadow-style-md)]"
			disabled={images.length === 0}
		>
			{m.pdf_submit_button()}
		</Button>
	</Container>
</div>
