<!--
  SpotlightSearch.svelte
  macOS/iOS Spotlight 風の洗練された検索コンポーネント。
  すりガラス・アクティブレスポンス・動的サジェスト対応。

  Apple Design 準拠:
  - §16 Accessibility: クリアボタン aria-label 修正, ↑/↓ キーボードサジェスト選択
  - §1  Response: 即時フォーカス応答
-->
<script lang="ts">
	import { Search, X } from '@lucide/svelte';
	import { m } from '$lib/paraglide/messages';
	import { resolveRadius } from '$lib/styles';
	import { styleStore } from '$lib/stores';
	import Button from './Button.svelte';
	import GlassContainer from './GlassContainer.svelte';

	interface Props {
		placeholder?: string;
		query?: string;
		suggestions?: string[];
		showSuggestions?: boolean;
		onsearch?: (query: string) => void;
		oninput?: (query: string) => void;
		onselectSuggestion?: (suggestion: string) => void;
		class?: string;
	}

	let {
		placeholder,
		query = $bindable(''),
		suggestions = [],
		showSuggestions = $bindable(false),
		onsearch,
		oninput,
		onselectSuggestion,
		class: klass = ''
	}: Props = $props();

	let inputElement = $state<HTMLInputElement | null>(null);
	let listEl = $state<HTMLUListElement | null>(null);
	let activeIndex = $state(-1);
	const effectivePlaceholder = $derived(placeholder || m.action_search());
	const r = $derived(resolveRadius(styleStore.pack));

	function handleSubmit(e: SubmitEvent) {
		e.preventDefault();
		if (query.trim() && onsearch) {
			onsearch(query.trim());
			showSuggestions = false;
			activeIndex = -1;
		}
	}

	function handleClear() {
		query = '';
		showSuggestions = false;
		activeIndex = -1;
		if (oninput) oninput('');
		if (inputElement) inputElement.focus();
	}

	function selectSuggestion(sug: string) {
		query = sug;
		showSuggestions = false;
		activeIndex = -1;
		if (onselectSuggestion) {
			onselectSuggestion(sug);
		} else if (onsearch) {
			onsearch(sug);
		}
	}

	// ↑/↓ キーボードナビゲーション (Apple Design §16 — wayfinding)
	function handleKeydown(e: KeyboardEvent) {
		if (!showSuggestions || suggestions.length === 0) return;

		if (e.key === 'ArrowDown') {
			e.preventDefault();
			activeIndex = Math.min(activeIndex + 1, suggestions.length - 1);
			focusListItem(activeIndex);
		} else if (e.key === 'ArrowUp') {
			e.preventDefault();
			activeIndex = Math.max(activeIndex - 1, -1);
			if (activeIndex === -1) {
				inputElement?.focus();
			} else {
				focusListItem(activeIndex);
			}
		} else if (e.key === 'Enter' && activeIndex >= 0) {
			e.preventDefault();
			selectSuggestion(suggestions[activeIndex]);
		} else if (e.key === 'Escape') {
			showSuggestions = false;
			activeIndex = -1;
		}
	}

	function focusListItem(index: number) {
		if (!listEl) return;
		const items = listEl.querySelectorAll<HTMLButtonElement>('[role="option"]');
		items[index]?.focus();
	}
</script>

<div class="relative w-full search-container-relative {klass}">
	<GlassContainer
		variant="input"
		glow={true}
		distortion="high"
		elevation="lg"
		class="{r.pill} transition-all duration-200 focus-within:border-[var(--color-primary-500)] focus-within:ring-2 focus-within:ring-[var(--color-primary-500)]/20 hover:shadow-[var(--shadow-card)]"
	>
		<form
			onsubmit={handleSubmit}
			class="group relative flex items-center px-4 py-2.5 w-full"
			role="search"
		>
			<Search
				size={18}
				class="mr-2.5 shrink-0 text-[var(--color-nav-inactive)] group-focus-within:text-[var(--color-primary-500)] transition-colors"
				aria-hidden="true"
			/>

			<input
				bind:this={inputElement}
				bind:value={query}
				type="text"
				placeholder={effectivePlaceholder}
				aria-label={effectivePlaceholder}
				role="combobox"
				aria-autocomplete="list"
				aria-controls={showSuggestions && suggestions.length > 0 ? 'search-suggestions' : undefined}
				aria-expanded={showSuggestions && suggestions.length > 0}
				oninput={() => {
					if (oninput) oninput(query);
				}}
				onfocus={() => {
					if (suggestions.length > 0) showSuggestions = true;
				}}
				onkeydown={handleKeydown}
				class="w-full border-0 bg-transparent text-sm font-medium text-[var(--color-nav-active)] placeholder-[var(--color-nav-inactive)] focus:outline-none focus:ring-0"
			/>

			{#if query}
				<button
					type="button"
					onclick={handleClear}
					class="ml-1.5 flex h-6 w-6 items-center justify-center rounded-full bg-[var(--color-surface-muted)] text-[var(--color-nav-inactive)] hover:text-[var(--color-nav-active)] active:scale-90 transition-all shrink-0"
					aria-label="検索をクリア"
				>
					<X size={14} />
				</button>
			{/if}

			<Button
				variant="primary"
				size="sm"
				type="submit"
				disabled={!query.trim()}
				class="ml-2 !rounded-full !h-9 !w-9 !min-h-0 !p-0 shrink-0 flex items-center justify-center"
				aria-label={m.action_search()}
			>
				<Search size={14} aria-hidden="true" />
			</Button>
		</form>
	</GlassContainer>

	{#if showSuggestions && suggestions.length > 0}
		<GlassContainer
			variant="card"
			class="absolute left-0 right-0 z-50 mt-2.5 overflow-hidden {r.card} border border-[var(--color-surface-border)]/80 shadow-[var(--shadow-modal)] max-h-60 overflow-y-auto origin-top transition-all duration-200 ease-[cubic-bezier(0.16,1,0.3,1)]"
		>
			<ul
				bind:this={listEl}
				id="search-suggestions"
				role="listbox"
				aria-label="検索候補"
				class="py-2 w-full"
			>
				{#each suggestions as sug, i}
					<li role="option" aria-selected={activeIndex === i}>
						<button
							type="button"
							onclick={() => selectSuggestion(sug)}
							onkeydown={(e) => {
								if (e.key === 'ArrowDown') {
									e.preventDefault();
									focusListItem(i + 1);
								}
								if (e.key === 'ArrowUp') {
									e.preventDefault();
									i === 0 ? inputElement?.focus() : focusListItem(i - 1);
								}
								if (e.key === 'Escape') {
									showSuggestions = false;
									inputElement?.focus();
								}
							}}
							role="option"
							aria-selected={activeIndex === i}
							class="flex w-full items-center gap-2.5 px-4 py-2.5 text-left text-sm text-[var(--color-nav-active)] hover:bg-[var(--color-surface-muted)] {styleStore
								.pack.press.card}"
							class:bg-[var(--color-surface-muted)]={activeIndex === i}
						>
							<Search
								size={14}
								class="text-[var(--color-nav-inactive)] shrink-0"
								aria-hidden="true"
							/>
							<span class="truncate">{sug}</span>
						</button>
					</li>
				{/each}
			</ul>
		</GlassContainer>
	{/if}
</div>
