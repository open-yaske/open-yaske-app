<script lang="ts">
	import { onMount } from 'svelte';
	import { browser } from '$app/environment';
	import { renderMarkdown } from '$lib/markdown';
	import { settingsStore } from '$lib/stores';
	import { Edit, Eye, Columns } from '@lucide/svelte';
	import katex from 'katex';
	import mermaid from 'mermaid';
	import 'katex/dist/katex.min.css';

	// 状態管理
	let markdownText = $state('');
	let previewEl: HTMLDivElement | null = $state(null);
	let textareaEl: HTMLTextAreaElement | null = $state(null);
	let lineNumbersEl: HTMLDivElement | null = $state(null);
	let activeTab: 'edit' | 'preview' | 'split' = $state('split');
	let mermaidInitialized = $state(false);

	// 行数カウント
	const linesCount = $derived.by(() => {
		// 最低でも1行は表示
		const lines = markdownText.split('\n').length;
		return lines > 0 ? lines : 1;
	});

	// Mermaidの初期化 & メモの初期ロード
	onMount(() => {
		if (browser) {
			const isDark =
				settingsStore.settings.theme === 'dark' ||
				(settingsStore.settings.theme === 'system' &&
					window.matchMedia('(prefers-color-scheme: dark)').matches);

			mermaid.initialize({
				startOnLoad: false,
				theme: isDark ? 'dark' : 'default',
				securityLevel: 'strict',
				fontFamily: 'var(--font-sans, sans-serif)'
			});
			mermaidInitialized = true;

			// settingsStoreからメモを復元
			settingsStore.init();
			markdownText = settingsStore.settings.customMemo || '';
		}
	});

	// スクロール同期
	function handleScroll() {
		if (textareaEl && lineNumbersEl) {
			lineNumbersEl.scrollTop = textareaEl.scrollTop;
		}
	}

	// リモート同期 (remoteStorage)
	let saveTimer: ReturnType<typeof setTimeout>;

	function saveMemo(text: string) {
		settingsStore.update({ customMemo: text });
	}

	function handleInput() {
		// スクロール同期も合わせて実行（行追加等によるスクロール位置調整のため）
		handleScroll();

		// デバウンス保存 (1秒間入力が止まったら remoteStorage に同期)
		clearTimeout(saveTimer);
		saveTimer = setTimeout(() => {
			saveMemo(markdownText);
		}, 1000);
	}

	function handleBlur() {
		clearTimeout(saveTimer);
		saveMemo(markdownText);
	}

	// テキストの前処理: 数式とMermaidブロックを一時的なクラス名付きプレースホルダーに置き換える
	interface MathPlaceholder {
		id: string;
		type: 'inline' | 'block';
		formula: string;
	}

	interface MermaidPlaceholder {
		id: string;
		code: string;
	}

	function preprocess(text: string) {
		const mathPlaceholders: MathPlaceholder[] = [];
		const mermaidPlaceholders: MermaidPlaceholder[] = [];
		let currentText = text;

		// 1. Mermaid コードブロックの置換
		currentText = currentText.replace(/```mermaid\n([\s\S]*?)\n```/g, (_, code) => {
			const index = mermaidPlaceholders.length;
			const id = `mermaid-placeholder-${index}`;
			mermaidPlaceholders.push({ id, code: code.trim() });
			return `<div class="${id} my-4 flex justify-center"></div>`;
		});

		// 2. ブロック数式の置換
		currentText = currentText.replace(/\$\$([\s\S]*?)\$\$/g, (_, formula) => {
			const index = mathPlaceholders.length;
			const id = `math-placeholder-block-${index}`;
			mathPlaceholders.push({ id, type: 'block', formula: formula.trim() });
			return `<div class="${id} my-3 overflow-x-auto py-2"></div>`;
		});

		// 3. インライン数式の置換
		currentText = currentText.replace(/\$([^$\n]+?)\$/g, (_, formula) => {
			const index = mathPlaceholders.length;
			const id = `math-placeholder-inline-${index}`;
			mathPlaceholders.push({ id, type: 'inline', formula: formula.trim() });
			return `<span class="${id}"></span>`;
		});

		return {
			processedText: currentText,
			mathPlaceholders,
			mermaidPlaceholders
		};
	}

	// プレビュー表示用HTMLを状態として派生
	const previewHtml = $derived.by(() => {
		const { processedText } = preprocess(markdownText);
		return renderMarkdown(processedText);
	});

	// プレビューの更新タスク
	$effect(() => {
		// previewHtml のリアクティブ追跡
		const html = previewHtml;
		if (!browser || !previewEl || (mermaid && !mermaidInitialized) || !html) return;

		const { mathPlaceholders, mermaidPlaceholders } = preprocess(markdownText);

		// KaTeX の適用
		mathPlaceholders.forEach((ph) => {
			const el = previewEl?.querySelector(`.${ph.id}`);
			if (el) {
				try {
					katex.render(ph.formula, el as HTMLElement, {
						displayMode: ph.type === 'block',
						throwOnError: false
					});
				} catch (e: unknown) {
					const msg = e instanceof Error ? e.message : String(e);
					el.innerHTML = `<span class="text-xs text-[var(--color-danger-500)] bg-[var(--color-danger-50)] border border-[var(--color-danger-200)] rounded px-1 py-0.5">KaTeX Error: ${msg}</span>`;
				}
			}
		});

		// Mermaid の適用
		mermaidPlaceholders.forEach(async (ph, i) => {
			const el = previewEl?.querySelector(`.${ph.id}`);
			if (el) {
				try {
					const uniqueId = `mermaid-svg-${Date.now()}-${i}`;
					const { svg } = await mermaid.render(uniqueId, ph.code);
					el.innerHTML = svg;
				} catch (e: unknown) {
					const msg = e instanceof Error ? e.message : String(e);
					el.innerHTML = `<pre class="w-full text-xs text-[var(--color-danger-500)] p-3 border border-[var(--color-danger-200)] rounded-lg bg-[var(--color-danger-50)]/50 overflow-x-auto font-mono">Mermaid Syntax Error:\n${msg}</pre>`;
				}
			}
		});
	});
</script>

<div
	class="flex flex-col gap-3 rounded-xl border border-[var(--color-surface-border)] bg-[var(--color-surface-card)] p-4 shadow-[var(--shadow-style-sm)]"
>
	<!-- ヘッダーとタブ切り替え -->
	<div
		class="flex flex-wrap items-center justify-between gap-2 border-b border-[var(--color-surface-border)] pb-3"
	>
		<div class="flex items-center gap-2">
			<span
				class="text-xs font-semibold uppercase tracking-wider text-[var(--color-primary-500)] bg-[var(--color-primary-500)]/10 px-2 py-0.5 rounded-full"
			>
				Preview
			</span>
			<h3 class="text-base font-bold text-[var(--color-nav-active)]">マークダウンメモ</h3>
		</div>

		<!-- タブボタン -->
		<div
			class="flex bg-[var(--color-surface-muted)] p-0.5 rounded-lg border border-[var(--color-surface-border)] text-xs"
		>
			<button
				class="flex items-center gap-1.5 px-3 py-1 rounded-md transition-colors font-medium {activeTab ===
				'edit'
					? 'bg-[var(--color-surface-card)] text-[var(--color-nav-active)] shadow-[var(--shadow-style-sm)]'
					: 'text-[var(--color-nav-inactive)] hover:text-[var(--color-nav-active)]'}"
				onclick={() => (activeTab = 'edit')}
			>
				<Edit size={14} />
				編集
			</button>
			<button
				class="flex items-center gap-1.5 px-3 py-1 rounded-md transition-colors font-medium {activeTab ===
				'preview'
					? 'bg-[var(--color-surface-card)] text-[var(--color-nav-active)] shadow-[var(--shadow-style-sm)]'
					: 'text-[var(--color-nav-inactive)] hover:text-[var(--color-nav-active)]'}"
				onclick={() => (activeTab = 'preview')}
			>
				<Eye size={14} />
				プレビュー
			</button>
			<button
				class="hidden md:flex items-center gap-1.5 px-3 py-1 rounded-md transition-colors font-medium {activeTab ===
				'split'
					? 'bg-[var(--color-surface-card)] text-[var(--color-nav-active)] shadow-[var(--shadow-style-sm)]'
					: 'text-[var(--color-nav-inactive)] hover:text-[var(--color-nav-active)]'}"
				onclick={() => (activeTab = 'split')}
			>
				<Columns size={14} />
				分割画面
			</button>
		</div>
	</div>

	<!-- メインコンテンツ領域 -->
	<div class="grid grid-cols-1 md:grid-cols-2 gap-4 h-[480px]">
		<!-- 編集エリア -->
		<div
			class="flex flex-col h-full {activeTab === 'preview'
				? 'hidden'
				: activeTab === 'edit'
					? 'col-span-2'
					: ''}"
		>
			<div
				class="flex border border-[var(--color-surface-border)] rounded-xl overflow-hidden bg-[var(--color-surface-muted)] text-[var(--color-nav-active)] focus-within:ring-2 focus-within:ring-[var(--color-primary-500)] focus-within:border-transparent transition-shadow h-full"
			>
				<!-- 行番号表示エリア -->
				<div
					bind:this={lineNumbersEl}
					class="flex flex-col items-end pr-2 pl-3 py-4 text-xs font-mono text-[var(--color-nav-inactive)] bg-[var(--color-surface-card)]/30 select-none overflow-hidden h-full border-r border-[var(--color-surface-border)]"
					style="line-height: 1.625rem; width: 3rem;"
				>
					<!-- eslint-disable-next-line @typescript-eslint/no-unused-vars -->
					{#each Array(linesCount) as _, i (i)}
						<div class="h-[1.625rem] flex items-center justify-end">{i + 1}</div>
					{/each}
				</div>

				<!-- テキストエリア -->
				<textarea
					bind:this={textareaEl}
					bind:value={markdownText}
					onscroll={handleScroll}
					oninput={handleInput}
					onblur={handleBlur}
					class="flex-1 p-4 bg-transparent font-mono text-sm leading-relaxed resize-none focus:outline-none h-full overflow-y-auto"
					style="line-height: 1.625rem;"
					placeholder="ここにメモを入力..."></textarea>
			</div>
		</div>

		<!-- プレビューエリア -->
		<div
			class="flex flex-col h-full overflow-hidden {activeTab === 'edit'
				? 'hidden'
				: activeTab === 'preview'
					? 'col-span-2'
					: ''}"
		>
			<div
				bind:this={previewEl}
				class="w-full h-full p-4 rounded-xl border border-[var(--color-surface-border)] bg-[var(--color-surface-card)] text-[var(--color-nav-active)] overflow-y-auto prose dark:prose-invert max-w-none text-sm leading-relaxed"
			>
				<!-- eslint-disable-next-line svelte/no-at-html-tags -->
				{@html previewHtml}
			</div>
		</div>
	</div>
</div>

<style>
	/* プレビューエリア内のMarkdownコンテンツの調整（Tailwind typographyの代わりの基本設定） */
	:global(.prose h1) {
		font-size: 1.5rem;
		font-weight: 700;
		margin-top: 1rem;
		margin-bottom: 0.5rem;
		border-bottom: 1px solid var(--color-surface-border);
		padding-bottom: 0.25rem;
	}
	:global(.prose h2) {
		font-size: 1.25rem;
		font-weight: 600;
		margin-top: 1.25rem;
		margin-bottom: 0.5rem;
	}
	:global(.prose h3) {
		font-size: 1.1rem;
		font-weight: 600;
		margin-top: 1rem;
		margin-bottom: 0.25rem;
	}
	:global(.prose p) {
		margin-top: 0.5rem;
		margin-bottom: 0.5rem;
	}
	:global(.prose ul) {
		list-style-type: disc;
		padding-left: 1.25rem;
		margin-top: 0.5rem;
		margin-bottom: 0.5rem;
	}
	:global(.prose ol) {
		list-style-type: decimal;
		padding-left: 1.25rem;
		margin-top: 0.5rem;
		margin-bottom: 0.5rem;
	}
	:global(.prose code) {
		font-family: var(--font-mono, monospace);
		background-color: var(--color-surface-muted);
		padding: 0.125rem 0.25rem;
		border-radius: 0.25rem;
		font-size: 0.85em;
	}
	:global(.prose pre) {
		background-color: var(--color-surface-muted);
		padding: 0.75rem;
		border-radius: 0.5rem;
		overflow-x: auto;
		margin-top: 0.75rem;
		margin-bottom: 0.75rem;
		border: 1px solid var(--color-surface-border);
	}
	:global(.prose pre code) {
		background-color: transparent;
		padding: 0;
		border-radius: 0;
		font-size: 0.9em;
	}
	:global(.prose blockquote) {
		border-left: 4px solid var(--color-primary-500);
		padding-left: 1rem;
		color: var(--color-nav-inactive);
		font-style: italic;
		margin: 1rem 0;
	}
	:global(.prose hr) {
		border: 0;
		border-top: 1px solid var(--color-surface-border);
		margin: 1.5rem 0;
	}
	:global(.prose table) {
		width: 100%;
		border-collapse: collapse;
		margin: 1rem 0;
	}
	:global(.prose th),
	:global(.prose td) {
		border: 1px solid var(--color-surface-border);
		padding: 0.5rem 0.75rem;
		text-align: left;
	}
	:global(.prose th) {
		background-color: var(--color-surface-muted);
		font-weight: 600;
	}

	/* Mermaid SVG のレスポンシブ対応 */
	:global(
		.mermaid-placeholder-0 svg,
		.mermaid-placeholder-1 svg,
		.mermaid-placeholder-2 svg,
		.mermaid-placeholder-3 svg,
		.mermaid-placeholder-4 svg
	) {
		max-width: 100% !important;
		height: auto !important;
	}
</style>
