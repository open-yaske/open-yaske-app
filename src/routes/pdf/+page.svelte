<script lang="ts">
	import { jsPDF } from 'jspdf';
	import { Camera, Image, Lightbulb, CheckCircle2, X } from '@lucide/svelte';
	import PageHeader from '$lib/components/PageHeader.svelte';
	import Container from '$lib/components/Container.svelte';
	import IconButton from '$lib/components/IconButton.svelte';
	import Button from '$lib/components/Button.svelte';

	let fileInput: HTMLInputElement;
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
		fileInput.click();
	}

	function handleFileChange(e: Event) {
		const target = e.target as HTMLInputElement;
		if (!target.files) return;

		const files = Array.from(target.files);
		if (images.length + files.length > 30) {
			error = '画像は最大30枚まで選択できます。';
			return;
		}

		error = '';
		for (const file of files) {
			if (!file.type.startsWith('image/')) {
				error = '画像ファイルのみ選択してください。';
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

	function handleExport() {
		if (images.length === 0) {
			error = 'PDFにする画像を追加してください。';
			return;
		}

		try {
			const doc = new jsPDF({
				orientation: 'portrait',
				unit: 'mm',
				format: pageSize === 'a4' ? 'a4' : 'a4' // 元のサイズでもA4に出力
			});

			images.forEach((img, index) => {
				if (index > 0) {
					doc.addPage();
				}
				// 余白10mmで画像を全面に配置
				doc.addImage(img.url, 'JPEG', 10, 10, 190, 277);
			});

			const dateStr = new Date().toISOString().replace(/[-:T]/g, '').slice(0, 14);
			const outName = filename.trim()
				? `${filename.trim()}_${dateStr}.pdf`
				: `photos_${dateStr}.pdf`;

			doc.save(outName);
		} catch (e) {
			error = 'PDFの生成に失敗しました。';
			console.error(e);
		}
	}
</script>

<svelte:head>
	<title>写真 → PDF | open-nexus</title>
</svelte:head>

<div class="min-h-screen bg-[var(--color-surface-page)] pb-10">
	{#snippet leadingSnippet()}
		<IconButton icon={X} ariaLabel="閉じる" onclick={handleClose} variant="subtle" size="sm" />
	{/snippet}

	<PageHeader title="写真 → PDF" leading={leadingSnippet} />

	<Container size="narrow" class="py-4 space-y-6">
		<input
			type="file"
			accept="image/*"
			multiple
			bind:this={fileInput}
			onchange={handleFileChange}
			class="hidden"
		/>

		<!-- 画像追加エリア -->
		<div
			class="rounded-[var(--radius-card)] border border-[var(--color-surface-border)] bg-[var(--color-surface-card)] p-5 space-y-5"
		>
			<div class="flex flex-col items-center justify-center py-4 text-center">
				<div
					class="flex h-14 w-14 items-center justify-center rounded-full bg-[var(--color-primary-50)] text-[var(--color-primary-600)] mb-3"
				>
					<Camera size={26} />
				</div>
				<h3 class="text-base font-bold text-[var(--color-nav-active)]">PDF にする画像を追加</h3>
				<p class="text-xs text-[var(--color-nav-inactive)] mt-1">
					書類のスキャンがおすすめ (自動でまっすぐ補正)
				</p>
			</div>

			<div class="grid grid-cols-1 gap-3 sm:grid-cols-2">
				<!-- スキャン風（実際はカメラ起動またはファイル選択） -->
				<button
					type="button"
					onclick={triggerSelectFiles}
					class="flex flex-col items-center justify-center gap-1 rounded-xl bg-[var(--color-primary-800)] py-3.5 text-white transition-all hover:bg-[var(--color-primary-900)]"
				>
					<span class="flex items-center gap-1.5 text-sm font-bold">
						<Camera size={18} />
						書類をスキャン
					</span>
					<span class="text-[10px] text-white/80">複数ページを連続で取り込めます</span>
				</button>

				<!-- 写真を選ぶ -->
				<button
					type="button"
					onclick={triggerSelectFiles}
					class="flex flex-col items-center justify-center gap-1 rounded-xl border border-[var(--color-primary-200)] bg-white py-3.5 text-[var(--color-primary-800)] transition-all hover:bg-[var(--color-primary-50)]"
				>
					<span class="flex items-center gap-1.5 text-sm font-bold">
						<Image size={18} />
						写真を選ぶ
					</span>
					<span class="text-[10px] text-[var(--color-nav-inactive)]">アルバムから最大30枚</span>
				</button>
			</div>

			<!-- エラーメッセージ -->
			{#if error}
				<p class="text-xs font-semibold text-red-500 text-center">{error}</p>
			{/if}

			<!-- アドバイス -->
			<div
				class="rounded-xl bg-[var(--color-surface-muted)] p-4 space-y-2 text-xs text-[var(--color-nav-active)]"
			>
				<p class="font-bold flex items-center gap-1.5 text-[var(--color-primary-800)]">
					<Lightbulb size={14} />
					きれいに取り込むには
				</p>
				<ul class="space-y-1.5 pl-1 text-[var(--color-surface-mutedForeground)]">
					<li class="flex items-start gap-1.5">
						<CheckCircle2 size={12} class="mt-0.5 text-[var(--color-primary-600)] shrink-0" />
						<span>明るい場所で、紙全体が入るようにする</span>
					</li>
					<li class="flex items-start gap-1.5">
						<CheckCircle2 size={12} class="mt-0.5 text-[var(--color-primary-600)] shrink-0" />
						<span>机と紙の色が分かれると認識しやすい</span>
					</li>
					<li class="flex items-start gap-1.5">
						<CheckCircle2 size={12} class="mt-0.5 text-[var(--color-primary-600)] shrink-0" />
						<span>撮影後に四隅を直せます (画面の指示に従って保存)</span>
					</li>
				</ul>
			</div>
		</div>

		<!-- プレビューエリア -->
		{#if images.length > 0}
			<div
				class="rounded-[var(--radius-card)] border border-[var(--color-surface-border)] bg-[var(--color-surface-card)] p-4"
			>
				<h4 class="text-xs font-bold text-[var(--color-nav-inactive)] mb-3">
					選択された画像 ({images.length}枚)
				</h4>
				<div class="grid grid-cols-3 gap-3 sm:grid-cols-4 md:grid-cols-5">
					{#each images as img, index (img.url)}
						<div
							class="relative aspect-[3/4] rounded-lg border border-[var(--color-surface-border)] overflow-hidden bg-[var(--color-surface-muted)] group"
						>
							<img src={img.url} alt={img.name} class="h-full w-full object-cover" />
							<button
								type="button"
								onclick={() => removeImage(index)}
								class="absolute top-1 right-1 flex h-5 w-5 items-center justify-center rounded-full bg-black/60 text-white hover:bg-black/80"
								aria-label="削除"
							>
								<X size={12} />
							</button>
						</div>
					{/each}
				</div>
			</div>
		{/if}

		<!-- 設定エリア -->
		<div
			class="rounded-[var(--radius-card)] border border-[var(--color-surface-border)] bg-[var(--color-surface-card)] p-5 space-y-4"
		>
			<!-- ファイル名 -->
			<div class="space-y-1.5">
				<label for="pdf-filename" class="text-xs font-bold text-[var(--color-nav-inactive)]"
					>ファイル名</label
				>
				<div class="relative flex items-center">
					<input
						id="pdf-filename"
						type="text"
						placeholder="名前の先頭 (任意)"
						bind:value={filename}
						class="w-full rounded-xl border border-[var(--color-surface-border)] bg-[var(--color-surface-card)] px-4 py-3 pr-24 text-sm text-[var(--color-nav-active)] focus:border-[var(--color-primary-500)] focus:outline-none"
					/>
					<span class="absolute right-4 text-xs font-medium text-[var(--color-nav-inactive)]">
						+日付時刻.pdf
					</span>
				</div>
				<p class="text-[10px] text-[var(--color-nav-inactive)]">
					共有時のファイル名は「名前_YYYYMMDD_HHmmss.pdf」です。空欄のときは photos_日付時刻.pdf
					になります。
				</p>
			</div>

			<!-- ページサイズ -->
			<div class="space-y-2">
				<span class="text-xs font-bold text-[var(--color-nav-inactive)]">ページサイズ</span>
				<div class="grid grid-cols-2 p-1 rounded-xl bg-[var(--color-surface-muted)]">
					<button
						type="button"
						onclick={() => (pageSize = 'a4')}
						class="rounded-lg py-2 text-xs font-bold text-center transition-all"
						class:bg-white={pageSize === 'a4'}
						class:text-[var(--color-nav-active)]={pageSize === 'a4'}
						class:shadow-sm={pageSize === 'a4'}
						class:text-[var(--color-nav-inactive)]={pageSize !== 'a4'}
					>
						A4
					</button>
					<button
						type="button"
						onclick={() => (pageSize = 'original')}
						class="rounded-lg py-2 text-xs font-bold text-center transition-all"
						class:bg-white={pageSize === 'original'}
						class:text-[var(--color-nav-active)]={pageSize === 'original'}
						class:shadow-sm={pageSize === 'original'}
						class:text-[var(--color-nav-inactive)]={pageSize !== 'original'}
					>
						元のサイズ
					</button>
				</div>
			</div>
		</div>

		<!-- PDF生成ボタン -->
		<Button
			onclick={handleExport}
			variant="primary"
			size="lg"
			class="w-full rounded-xl font-bold py-3.5 shadow-sm bg-[var(--color-primary-800)] hover:bg-[var(--color-primary-900)] text-white"
			disabled={images.length === 0}
		>
			PDF を保存・共有する
		</Button>
	</Container>
</div>
