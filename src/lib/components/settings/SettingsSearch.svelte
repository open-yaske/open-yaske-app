<!--
  SettingsSearch.svelte
  設定項目のファジーサーチモーダル（コマンドパレット）。
  Cmd+K (desktop) / ボタンタップ (mobile) で開く。
  矢印キー + Enter でナビゲーション、クリックで該当セクションへスクロール。
-->
<script lang="ts">
	import { fade } from 'svelte/transition';
	import { Search, CornerDownLeft } from '@lucide/svelte';
	import { uiStore } from '$lib/stores';
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

	// ファジーマッチ: クエリの各文字がラベル内に順番に現れるかを判定し、スコアリング
	function fuzzyMatch(text: string, q: string): { matched: boolean; score: number } {
		if (!q) return { matched: true, score: 0 };
		const textLower = text.toLowerCase();
		const qLower = q.toLowerCase();
		let qi = 0;
		let score = 0;
		let lastMatch = -1;

		for (let ti = 0; ti < textLower.length && qi < qLower.length; ti++) {
			if (textLower[ti] === qLower[qi]) {
				// 連続マッチは高スコア
				score += lastMatch === ti - 1 ? 3 : 1;
				// 単語境界マッチはボーナス
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
		if (!q) {
			return items.map((item) => ({ ...item, score: 0 }));
		}
		const scored: ScoredItem[] = [];
		for (const item of items) {
			const fullText = `${item.category} ${item.label}`;
			const { matched, score } = fuzzyMatch(fullText, q);
			if (matched) {
				scored.push({ ...item, score });
			}
		}
		scored.sort((a, b) => b.score - a.score);
		return scored;
	});

	// クエリ変更時に選択をリセット
	$effect(() => {
		void query; // depend on query so this effect re-runs on change
		selectedIndex = 0;
	});

	// パレットオープン時にinputへフォーカス
	$effect(() => {
		if (uiStore.paletteOpen) {
			queueMicrotask(() => {
				inputEl?.focus();
			});
		} else {
			query = '';
			selectedIndex = 0;
		}
	});

	function scrollToSection(sectionId: string) {
		uiStore.closePalette();
		queueMicrotask(() => {
			const el = document.getElementById(sectionId);
			el?.scrollIntoView({ behavior: 'smooth', block: 'start' });
		});
	}

	function handleKeydown(e: KeyboardEvent) {
		if (!uiStore.paletteOpen) return;
		switch (e.key) {
			case 'ArrowDown':
				e.preventDefault();
				selectedIndex = Math.min(selectedIndex + 1, results.length - 1);
				break;
			case 'ArrowUp':
				e.preventDefault();
				selectedIndex = Math.max(selectedIndex - 1, 0);
				break;
			case 'Enter':
				e.preventDefault();
				if (results[selectedIndex]) {
					scrollToSection(results[selectedIndex].sectionId);
				}
				break;
			case 'Escape':
				e.preventDefault();
				uiStore.closePalette();
				break;
		}
	}

	function handleResultClick(index: number) {
		if (results[index]) {
			scrollToSection(results[index].sectionId);
		}
	}
</script>

<svelte:window onkeydown={handleKeydown} />

{#if uiStore.paletteOpen}
	<div
		class="fixed inset-0 z-50 flex items-start justify-center p-4 pt-[10vh]"
		role="dialog"
		aria-modal="true"
		aria-label={m.action_search()}
		transition:fade={{ duration: 150 }}
	>
		<!-- backdrop -->
		<button
			type="button"
			class="absolute inset-0 bg-black/50"
			aria-label={m.modal_close()}
			onclick={() => uiStore.closePalette()}
			tabindex="-1"
		></button>

		<!-- palette -->
		<div
			class="relative z-10 flex max-h-[70vh] w-full flex-col overflow-hidden rounded-2xl bg-[var(--color-surface-card)] shadow-[var(--shadow-modal)]"
			style="max-width: 32rem"
			transition:fade={{ duration: 200 }}
		>
			<!-- search input -->
			<div class="flex items-center gap-3 border-b border-[var(--color-surface-border)] px-4 py-3">
				<Search size={18} class="shrink-0 text-[var(--color-nav-inactive)]" />
				<input
					bind:this={inputEl}
					bind:value={query}
					type="text"
					placeholder={m.action_search()}
					class="flex-1 bg-transparent text-sm text-[var(--color-nav-active)] placeholder-[var(--color-nav-inactive)] focus:outline-none"
				/>
				<kbd
					class="hidden shrink-0 rounded border border-[var(--color-surface-border)] px-1.5 py-0.5 text-xs text-[var(--color-nav-inactive)] sm:block"
				>
					Esc
				</kbd>
			</div>

			<!-- results -->
			<div class="flex-1 overflow-y-auto py-2">
				{#if results.length === 0}
					<div class="px-4 py-8 text-center text-sm text-[var(--color-nav-inactive)]">
						{m.empty_no_results()}
					</div>
				{:else}
					{#each results as result, i (result.sectionId)}
						<button
							type="button"
							class="flex w-full items-center justify-between px-4 py-2.5 text-left transition-colors hover:bg-[var(--color-surface-muted)] {i ===
							selectedIndex
								? 'bg-[var(--color-surface-muted)]'
								: ''}"
							onclick={() => handleResultClick(i)}
							onmouseenter={() => (selectedIndex = i)}
						>
							<div class="flex flex-col gap-0.5">
								<span class="text-sm font-medium text-[var(--color-nav-active)]"
									>{result.label}</span
								>
								<span class="text-xs text-[var(--color-nav-inactive)]">{result.category}</span>
							</div>
							{#if i === selectedIndex}
								<CornerDownLeft size={16} class="shrink-0 text-[var(--color-nav-inactive)]" />
							{/if}
						</button>
					{/each}
				{/if}
			</div>
		</div>
	</div>
{/if}
