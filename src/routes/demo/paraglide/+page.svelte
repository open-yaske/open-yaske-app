<script lang="ts">
	import { setLocale, getLocale, locales as availableLocales } from '$lib/paraglide/runtime';
	import { m } from '$lib/paraglide/messages.js';

	const locales = [
		{ code: 'ja', label: '日本語' },
		{ code: 'en', label: 'English' },
		{ code: 'zh', label: '中文' },
		{ code: 'ko', label: '한국어' }
	] as const;

	const current = $derived(getLocale());
</script>

<svelte:head><title>Paraglide Demo | open-nexus</title></svelte:head>

<div class="space-y-6 p-4">
	<header>
		<h1 class="text-2xl font-bold text-[var(--color-nav-active)]">
			{m.hello_world({ name: 'SvelteKit User' })}
		</h1>
		<p class="mt-1 text-sm text-[var(--color-nav-inactive)]">
			Current locale: <code class="font-mono">{current}</code>
		</p>
	</header>

	<section>
		<h2 class="mb-2 text-sm font-semibold text-[var(--color-nav-active)]">Switch locale</h2>
		<div class="flex flex-wrap gap-2">
			{#each locales as l (l.code)}
				<button
					type="button"
					disabled={!availableLocales.includes(l.code as never)}
					onclick={() => setLocale(l.code as 'ja' | 'en' | 'zh' | 'ko')}
					class="rounded-full border px-3 py-1 text-sm font-medium transition-colors
						{current === l.code
						? 'border-[var(--color-primary-500)] bg-[var(--color-primary-50)] text-[var(--color-primary-700)]'
						: 'border-[var(--color-surface-border)] bg-[var(--color-surface-card)] text-[var(--color-nav-active)] hover:bg-[var(--color-surface-muted)]'}
						disabled:cursor-not-allowed disabled:opacity-50"
				>
					{l.label} ({l.code})
				</button>
			{/each}
		</div>
	</section>

	<section class="space-y-1">
		<h2 class="mb-2 text-sm font-semibold text-[var(--color-nav-active)]">Sample messages</h2>
		<ul class="space-y-1 text-sm">
			<li>{m.nav_home()}</li>
			<li>{m.nav_timetable()}</li>
			<li>{m.nav_calendar()}</li>
			<li>{m.nav_todo()}</li>
			<li>{m.nav_attendance()}</li>
			<li>{m.nav_more()}</li>
		</ul>
	</section>
</div>
