<!--
  SideNav.svelte
  デスクトップ用サイドナビゲーション。md 以上でのみ表示。
  ホバー / アクティブ状態のスタイル切替。
-->
<script lang="ts">
	import type { Component, Snippet } from 'svelte';
	import { resolve } from '$app/paths';
	import { page } from '$app/state';
	import { PanelLeftClose, PanelLeftOpen } from '@lucide/svelte';
	import type { RouteId } from '$app/types';
	import { m } from '$lib/paraglide/messages';

	interface NavItem {
		href: RouteId;
		label: string;
		icon: Component<{ size?: number }>;
	}

	interface Props {
		items: NavItem[];
		footer?: Snippet;
		collapsed?: boolean;
		ontoggle?: () => void;
		appName?: string;
		logoData?: string;
	}

	let { items, footer, collapsed = false, ontoggle, appName, logoData }: Props = $props();

	function isActive(href: string): boolean {
		const pathname = page.url.pathname;
		return pathname === href || pathname.startsWith(href + '/');
	}
</script>

<aside
	class="sticky top-0 hidden h-screen shrink-0 flex-col border-r border-[var(--color-surface-border)] bg-[var(--color-nav-bg)] transition-[width] duration-200 md:flex"
	class:w-16={collapsed}
	class:w-56={!collapsed}
	aria-label="Sidebar navigation"
>
	<div
		class="flex h-14 items-center border-b border-[var(--color-surface-border)] p-3"
		class:justify-between={!collapsed}
		class:justify-center={collapsed}
	>
		{#if !collapsed}
			<div class="flex items-center gap-2 overflow-hidden">
				<img
					src={logoData || resolve('/favicon.png')}
					alt="Logo"
					class="h-8 w-8 rounded-md shrink-0 object-cover"
				/>
				<span class="truncate font-semibold text-[var(--color-nav-active)]">
					{appName || m.app_name()}
				</span>
			</div>
		{/if}
		<button
			type="button"
			onclick={ontoggle}
			class="flex h-9 w-9 shrink-0 items-center justify-center rounded-chip text-[var(--color-nav-inactive)] transition-colors hover:bg-[var(--color-surface-muted)] hover:text-[var(--color-nav-active)]"
			aria-label={collapsed ? 'サイドバーを展開' : 'サイドバーをたたむ'}
		>
			{#if collapsed}
				<PanelLeftOpen size={18} />
			{:else}
				<PanelLeftClose size={18} />
			{/if}
		</button>
	</div>

	<div class="flex-1 overflow-y-auto p-3">
		<ul class="space-y-1">
			{#each items as item (item.href)}
				{@const active = isActive(item.href)}
				<li>
					<a
						href={resolve(item.href)}
						class="flex items-center rounded-chip py-2 text-sm font-medium transition-colors"
						class:justify-center={collapsed}
						class:gap-3={!collapsed}
						class:px-3={!collapsed}
						class:px-2={collapsed}
						class:bg-[var(--color-surface-muted)]={active}
						class:text-[var(--color-nav-active)]={active}
						class:text-[var(--color-nav-inactive)]={!active}
						class:hover:bg-[var(--color-surface-muted)]={!active}
						class:hover:text-[var(--color-nav-active)]={!active}
						aria-current={active ? 'page' : undefined}
					>
						<item.icon size={18} />
						<span class:sr-only={collapsed}>{item.label}</span>
					</a>
				</li>
			{/each}
		</ul>
	</div>
	{#if footer && !collapsed}
		<div class="shrink-0 border-t border-[var(--color-surface-border)] p-3">
			{@render footer()}
		</div>
	{/if}
</aside>
