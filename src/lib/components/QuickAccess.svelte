<!--
  QuickAccess.svelte
  ホーム画面のクイックアクセスショートカット。
  横スクロール可能なアイコンボタンの行。時間割 / TODO / カレンダー / 出席 へリンク。
-->
<script lang="ts">
	import { resolve } from '$app/paths';
	import { m } from '$lib/paraglide/messages';
	import { BookOpen, CheckSquare, Calendar, BarChart3 } from '@lucide/svelte';
	import type { Component } from 'svelte';

	interface Shortcut {
		href: string;
		label: string;
		icon: Component<{ size?: number; color?: string }>;
	}

	const shortcuts = $derived<Shortcut[]>([
		{ href: resolve('/timetable'), label: m.nav_timetable(), icon: BookOpen },
		{ href: resolve('/todo'), label: m.nav_todo(), icon: CheckSquare },
		{ href: resolve('/calendar'), label: m.nav_calendar(), icon: Calendar },
		{ href: resolve('/attendance'), label: m.nav_attendance(), icon: BarChart3 }
	]);
</script>

<div
	class="flex gap-3 overflow-x-auto pb-1 [scrollbar-width:none] [-ms-overflow-style:none] [&::-webkit-scrollbar]:hidden"
>
	{#each shortcuts as item (item.href)}
		<!-- eslint-disable svelte/no-navigation-without-resolve -->
		<a
			href={item.href}
			class="flex shrink-0 flex-col items-center gap-2 rounded-[var(--radius-card)] border border-[var(--color-surface-border)] bg-[var(--color-surface-card)] px-4 py-3 shadow-[var(--shadow-card)] transition-colors hover:bg-[var(--color-surface-muted)]"
		>
			<span
				class="flex h-11 w-11 items-center justify-center rounded-full bg-[var(--color-primary-50)] text-[var(--color-primary-600)]"
			>
				<item.icon size={22} />
			</span>
			<span class="text-xs font-medium text-[var(--color-nav-active)]">{item.label}</span>
		</a>
		<!-- eslint-enable svelte/no-navigation-without-resolve -->
	{/each}
</div>
