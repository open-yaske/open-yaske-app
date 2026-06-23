<!--
  BottomTabBar.svelte
  モバイル用ボトムナビゲーション。md 以下でのみ表示。
-->
<script lang="ts">
	import type { Component } from 'svelte';
	import { resolve } from '$app/paths';
	import { page } from '$app/state';
	import type { RouteId } from '$app/types';

	interface Tab {
		href: RouteId;
		label: string;
		icon: Component<{ size?: number }>;
	}

	interface Props {
		tabs: Tab[];
	}

	let { tabs }: Props = $props();

	function isActive(href: string): boolean {
		const pathname = page.url.pathname;
		// パスの先頭一致でアクティブ判定
		return pathname === href || pathname.startsWith(href + '/');
	}
</script>

<nav
	class="fixed right-0 bottom-0 left-0 z-30 border-t border-[var(--color-surface-border)] bg-[var(--color-nav-bg)] pb-[env(safe-area-inset-bottom)] md:hidden"
	aria-label="Main navigation"
>
	<ul class="flex items-stretch justify-around">
		{#each tabs as tab (tab.href)}
			{@const active = isActive(tab.href)}
			<li class="flex-1">
				<a
					href={resolve(tab.href)}
					class="flex flex-col items-center justify-center gap-0.5 py-2 text-xs transition-colors"
					class:text-[var(--color-nav-active)]={active}
					class:text-[var(--color-nav-inactive)]={!active}
					aria-current={active ? 'page' : undefined}
				>
					<tab.icon size={22} />
					<span>{tab.label}</span>
				</a>
			</li>
		{/each}
	</ul>
</nav>
