<!--
  SettingsSearch.svelte
  設定項目のインライン検索バー。
  入力するとリアルタイムに結果が候補リストに表示され、クリックで該当セクションへスクロールする。
  モーダルは使わず、ページ先頭に直接埋め込む形式。
-->
<script lang="ts">
	import { Search, X } from '@lucide/svelte';
	import { m } from '$lib/paraglide/messages';

	/** 検索インデックス項目 */
	export type SearchItem = {
		category: string;
		label: string;
		sectionId: string;
	};

	interface Props {
		items: SearchItem[];
	}

	let { items }: Props = $props();

	let query = $state('');
	let selectedIndex = $state(0);
	let inputEl = $state<HTMLInputElement | null>(null);
	let showResults = $state(false);

	// ファジーマッチ
	function fuzzyMatch(text: string, q: string): { matched: boolean; score: number } {
		if (!q) return { matched: true, score: 0 };
		const textLower = text.toLowerCase();
		const qLower = q.toLowerCase();
		let qi = 0;
		let score = 0;
		let lastMatch = -1;

		for (let ti = 0; ti < textLower.length && qi < qLower.length; ti++) {
			if (textLower[ti] === qLower[qi]) {
				score += lastMatch === ti - 1 ? 3 : 1;
				if (ti === 0 || textLower[ti - 1] === ' ' || textLower[ti - 1] === '_') {
					score += 2;
				}
				lastMatch = ti;
				qi++;
			}
		}
		return { matched: qi === qLower.length, score };
	}

	type ScoredItem = SearchItem & { score: number };

	const results = $derived.by<ScoredItem[]>(() => {
		const q = query.trim();
		if (!q) return [];
		const scored: ScoredItem[] = [];
		for (const item of items) {
			const fullText = `${item.category} ${item.label}`;
			const { matched, score } = fuzzyMatch(fullText, q);
			if (matched) {
				scored.push({ ...item, score });
			}
		}
		scored.sort((a, b) => b.score - a.score);
		return scored.slice(0, 8);
	});

	$effect(() => {
		void query;
		selectedIndex = 0;
		showResults = query.trim().length > 0;
	});

	function scrollToSection(sectionId: string) {
		query = '';
		showResults = false;
		queueMicrotask(() => {
			const el = document.getElementById(sectionId);
			el?.scrollIntoView({ behavior: 'smooth', block: 'start' });
		});
	}

	function handleKeydown(e: KeyboardEvent) {
		if (!showResults) return;
		switch (e.key) {
			case 'ArrowDown':
				e.preventDefault();
				if (results.length > 0) {
					selectedIndex = Math.min(selectedIndex + 1, results.length - 1);
				}
				break;
			case 'ArrowUp':
				e.preventDefault();
				selectedIndex = Math.max(selectedIndex - 1, 0);
				break;
			case 'Enter':
				e.preventDefault();
				if (results.length > 0 && results[selectedIndex]) {
					scrollToSection(results[selectedIndex].sectionId);
				}
				break;
			case 'Escape':
				e.preventDefault();
				query = '';
				showResults = false;
				inputEl?.blur();
				break;
		}
	}
</script>

<div class="relative">
	<div
		class="flex items-center gap-2 rounded-chip border border-[var(--color-surface-border)] bg-[var(--color-surface-card)] px-3 py-2 transition-colors focus-within:border-[var(--color-primary-500)] focus-within:ring-2 focus-within:ring-[var(--color-primary-500)]/30"
	>
		<Search size={16} class="shrink-0 text-[var(--color-nav-inactive)]" />
		<input
			bind:this={inputEl}
			bind:value={query}
			type="text"
			placeholder={m.action_search()}
			onkeydown={handleKeydown}
			onfocus={() => {
				if (query.trim()) showResults = true;
			}}
			class="flex-1 bg-transparent text-sm text-[var(--color-nav-active)] placeholder-[var(--color-nav-inactive)] focus:outline-none"
		/>
		{#if query}
			<button
				type="button"
				onclick={() => {
					query = '';
					showResults = false;
					inputEl?.focus();
				}}
				class="shrink-0 text-[var(--color-nav-inactive)] hover:text-[var(--color-nav-active)] transition-colors"
				aria-label="検索をクリア"
			>
				<X size={14} />
			</button>
		{/if}
	</div>

	{#if showResults}
		<!-- 結果ドロップダウン -->
		<!-- svelte-ignore a11y_no_static_element_interactions -->
		<div
			class="absolute left-0 right-0 top-full z-30 mt-1 overflow-hidden rounded-chip border border-[var(--color-surface-border)] bg-[var(--color-surface-card)] shadow-[var(--shadow-style-sm)]"
			onmouseleave={() => (selectedIndex = -1)}
		>
			{#if results.length === 0}
				<div class="px-4 py-6 text-center text-sm text-[var(--color-nav-inactive)]">
					{m.empty_no_results()}
				</div>
			{:else}
				{#each results as result, i (result.sectionId + '-' + result.label)}
					<button
						type="button"
						class="flex w-full items-center gap-3 px-4 py-2.5 text-left transition-colors hover:bg-[var(--color-surface-muted)] {i ===
						selectedIndex
							? 'bg-[var(--color-surface-muted)]'
							: ''}"
						onclick={() => scrollToSection(result.sectionId)}
						onmouseenter={() => (selectedIndex = i)}
					>
						<div class="flex flex-col gap-0.5 min-w-0">
							<span class="text-sm font-medium text-[var(--color-nav-active)] truncate"
								>{result.label}</span
							>
							<span class="text-xs text-[var(--color-nav-inactive)]">{result.category}</span>
						</div>
					</button>
				{/each}
			{/if}
		</div>

		<!-- 外側クリックで閉じる -->
		<!-- svelte-ignore a11y_no_static_element_interactions -->
		<div
			class="fixed inset-0 z-20"
			onclick={() => (showResults = false)}
			onkeydown={() => {}}
			role="none"
		></div>
	{/if}
</div>
