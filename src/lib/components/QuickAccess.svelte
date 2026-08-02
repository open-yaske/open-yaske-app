<!--
  QuickAccess.svelte
  ホーム画面のクイックアクセスショートカット。
  横スクロール可能な Liquid Glass アイコンボタンの行。
  時間割 / TODO / カレンダー / 出席 へリンク。

  Apple Design 準拠:
  - §12 Materials: LiquidGlass カード
  - §1 Response: pointer-down 即フィードバック
  - §10 Gesture: 44px 最小タップターゲット
-->
<script lang="ts">
	import { resolve } from '$app/paths';
	import { m } from '$lib/paraglide/messages';
	import { BookOpen, CheckSquare, Calendar, BarChart3 } from '@lucide/svelte';
	import type { Component } from 'svelte';
	import GlassContainer from './GlassContainer.svelte';
	import { styleStore } from '$lib/stores';

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
		<GlassContainer variant="card" glow={true} distortion="medium" class="shrink-0 rounded-2xl">
			<!-- eslint-disable svelte/no-navigation-without-resolve -->
			<a
				href={item.href}
				class="flex flex-col items-center gap-2 px-5 py-4 select-none {styleStore.pack.press.card}"
			>
				<span
					class="flex h-11 w-11 items-center justify-center rounded-full bg-[var(--color-primary-50)] text-[var(--color-primary-600)]"
				>
					<item.icon size={22} />
				</span>
				<span class="text-xs font-medium text-[var(--color-nav-active)]">{item.label}</span>
			</a>
			<!-- eslint-enable svelte/no-navigation-without-resolve -->
		</GlassContainer>
	{/each}
</div>
