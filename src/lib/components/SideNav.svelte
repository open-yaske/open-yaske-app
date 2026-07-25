<!--
  SideNav.svelte
  デスクトップ用 macOS スタイル・フロートサイドナビゲーション。md 以上でのみ表示。
  Apple Design (すりガラス LiquidGlass, 物理触感, ハイライト pill) 準拠。
-->
<script lang="ts">
	import type { Component, Snippet } from 'svelte';
	import { resolve, base } from '$app/paths';
	import { page } from '$app/state';
	import { PanelLeftClose, PanelLeftOpen } from '@lucide/svelte';
	import type { RouteId } from '$app/types';
	import { m } from '$lib/paraglide/messages';
	import { APPLE_PRESS } from '$lib/apple-design';
	import GlassContainer from './GlassContainer.svelte';

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
	class="sticky top-0 hidden h-screen shrink-0 transition-all duration-300 ease-[cubic-bezier(0.16,1,0.3,1)] md:flex z-30 select-none shadow-apple-sm"
	class:w-16={collapsed}
	class:w-64={!collapsed}
	aria-label="Sidebar navigation"
>
	<GlassContainer
		variant="bar"
		glow={true}
		distortion="high"
		elevation="lg"
		class="flex h-full w-full flex-col"
	>
		<div
			class="flex h-16 items-center px-4"
			class:justify-between={!collapsed}
			class:justify-center={collapsed}
		>
			{#if !collapsed}
				<div class="flex items-center gap-3 overflow-hidden">
					<img
						src={logoData || `${base}/favicon.png`}
						alt="Logo"
						class="h-8 w-8 rounded-lg shrink-0 object-cover border border-[var(--color-surface-border)]/50 shadow-apple-sm"
					/>
					<span class="truncate font-bold tracking-tight text-[var(--color-nav-active)] text-base">
						{appName || m.app_name()}
					</span>
				</div>
			{/if}
			<button
				type="button"
				onclick={ontoggle}
				class="flex h-9 w-9 shrink-0 items-center justify-center rounded-full text-[var(--color-nav-inactive)] transition-all duration-150 {APPLE_PRESS.icon} hover:bg-[var(--color-surface-muted)] hover:text-[var(--color-nav-active)]"
				aria-label={collapsed ? 'サイドバーを展開' : 'サイドバーをたたむ'}
			>
				{#if collapsed}
					<PanelLeftOpen size={18} />
				{:else}
					<PanelLeftClose size={18} />
				{/if}
			</button>
		</div>

		<div class="flex-1 overflow-y-auto px-3 py-2">
			<ul class="space-y-1.5">
				{#each items as item (item.href)}
					{@const active = isActive(item.href)}
					<li>
						<a
							href={resolve(item.href)}
							class="group relative flex min-h-[44px] items-center rounded-full py-2.5 text-sm font-medium transition-all duration-150 ease-[cubic-bezier(0.16,1,0.3,1)] {APPLE_PRESS.card}"
							class:justify-center={collapsed}
							class:gap-3={!collapsed}
							class:px-4={!collapsed}
							class:px-2={collapsed}
							class:text-[var(--color-primary-600)]={active}
							class:font-semibold={active}
							class:text-[var(--color-nav-inactive)]={!active}
							class:hover:text-[var(--color-nav-active)]={!active}
							aria-current={active ? 'page' : undefined}
						>
							{#if active}
								<div
									class="absolute inset-0 rounded-full bg-[var(--color-primary-500)]/15 border border-[var(--color-primary-500)]/20 shadow-apple-sm"
								></div>
							{:else}
								<div
									class="absolute inset-0 rounded-full bg-transparent group-hover:bg-[var(--color-surface-muted)]/60 transition-colors"
								></div>
							{/if}
							<div class="relative z-10 flex items-center gap-3">
								<item.icon size={18} />
								<span class:sr-only={collapsed}>{item.label}</span>
							</div>
						</a>
					</li>
				{/each}
			</ul>
		</div>

		{#if footer && !collapsed}
			<div class="shrink-0 border-t border-[var(--color-surface-border)]/50 p-3">
				{@render footer()}
			</div>
		{/if}
	</GlassContainer>
</aside>
