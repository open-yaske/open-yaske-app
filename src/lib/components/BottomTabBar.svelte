<!--
  BottomTabBar.svelte
  モバイル用 iOS Floating Glass Dock タブバー。md 以下でのみ表示。
  Apple Design (すりガラス、浮遊カプセル、物理スプリング応答) 採用。
-->
<script lang="ts">
	import type { Component } from 'svelte';
	import { resolve } from '$app/paths';
	import { page } from '$app/state';
	import type { RouteId } from '$app/types';
	import { APPLE_PRESS } from '$lib/apple-design';
	import GlassContainer from './GlassContainer.svelte';

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
		return pathname === href || pathname.startsWith(href + '/');
	}
</script>

<nav
	class="fixed bottom-4 left-1/2 -translate-x-1/2 z-40 w-[calc(100%-2rem)] max-w-md md:hidden select-none"
	aria-label="Main Navigation"
>
	<GlassContainer
		variant="bar"
		class="rounded-full border border-[var(--color-surface-border)]/80 p-1.5 shadow-apple-xl"
	>
		<ul class="flex items-center justify-around">
			{#each tabs as tab (tab.href)}
				{@const active = isActive(tab.href)}
				<li class="flex-1">
					<a
						href={resolve(tab.href)}
						class="relative flex min-h-[44px] flex-col items-center justify-center gap-1 rounded-full py-1 text-[11px] font-semibold transition-all duration-200 ease-[cubic-bezier(0.16,1,0.3,1)] {APPLE_PRESS.tab}"
						class:text-[var(--color-primary-600)]={active}
						class:text-[var(--color-nav-inactive)]={!active}
						aria-current={active ? 'page' : undefined}
					>
						{#if active}
							<div
								class="absolute inset-0 rounded-full bg-[var(--color-primary-500)]/15 backdrop-blur-md transition-all duration-200"
							></div>
						{/if}
						<div class="relative z-10 flex flex-col items-center w-full min-w-0 px-0.5">
							<tab.icon size={20} />
							<span
								class="w-full truncate text-center text-[10px] leading-none mt-1 tracking-tight font-medium"
								>{tab.label}</span
							>
						</div>
					</a>
				</li>
			{/each}
		</ul>
	</GlassContainer>
</nav>
