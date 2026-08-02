<!--
  BottomTabBar.svelte
  モバイル用ボトムナビゲーションバー。md 以下でのみ表示。
  
  ビジュアルスタイルに応じて表示形態が変化する「ドッキング」方式:
  - glass (Apple, Cyberpunk): 浮遊ガラスピル
  - solid (Material, Flat, Neumorphism): 全幅ソリッドバー
  - chiseled (Win95): 3D レイズドバー
  - gradient (WinXP): グラデーションバー
-->
<script lang="ts">
	import type { Component } from 'svelte';
	import { resolve } from '$app/paths';
	import { page } from '$app/state';
	import type { RouteId } from '$app/types';
	import { styleStore } from '$lib/stores';
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

	// スタイルに応じたコンテナクラス
	const containerClass = $derived.by(() => {
		const surface = styleStore.pack.layout.cardSurface;
		if (surface === 'glass') {
			return 'rounded-full p-1.5 shadow-[var(--shadow-modal)]';
		}
		if (surface === 'chiseled') {
			return 'border-t-2 border-t-white/90 border-b-2 border-b-[var(--color-surface-border)] bg-[var(--color-surface-card)]';
		}
		if (surface === 'gradient') {
			return 'bg-gradient-to-b from-[var(--color-surface-card)] to-[var(--color-surface-muted)]/70 border-t border-[var(--color-primary-500)]/20';
		}
		// solid, neumorphic, brutal
		return 'bg-[var(--color-surface-card)] border-t border-[var(--color-surface-border)]/60';
	});

	// ガラス系スタイルか（歪みエフェクト、floating位置）
	const isGlassStyle = $derived(styleStore.pack.layout.cardSurface === 'glass');

	const navClass = $derived(
		'fixed z-40 md:hidden select-none ' +
		(isGlassStyle
			? 'bottom-4 left-1/2 -translate-x-1/2 w-[calc(100%-2rem)] max-w-md'
			: 'bottom-0 left-0 right-0')
	);
</script>

<nav
	class={navClass}
	aria-label="Main Navigation"
>
	{#if isGlassStyle}
		<!-- 浮遊ガラスピル -->
		<GlassContainer
			variant="bar"
			glow={true}
			distortion="high"
			elevation="xl"
			class={containerClass}
		>
			<ul class="flex items-center justify-around">
				{#each tabs as tab (tab.href)}
					{@const active = isActive(tab.href)}
					<li class="flex-1">
						<a
							href={resolve(tab.href)}
							class="relative flex min-h-[44px] flex-col items-center justify-center gap-1 rounded-full py-1 text-[11px] font-semibold transition-all duration-200 ease-[cubic-bezier(0.16,1,0.3,1)] {styleStore
								.pack.press.tab}"
							class:text-[var(--color-primary-600)]={active}
							class:text-[var(--color-nav-inactive)]={!active}
							aria-current={active ? 'page' : undefined}
						>
							{#if active}
								<div
									class="absolute inset-0 rounded-full bg-[var(--color-primary-500)]/20 backdrop-blur-2xl border border-[var(--color-primary-500)]/30 shadow-[var(--shadow-card)] transition-all duration-200"
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
	{:else}
		<!-- ソリッド/ドッキング型バー -->
		<div class={containerClass}>
			<ul class="flex items-center justify-around py-1">
				{#each tabs as tab (tab.href)}
					{@const active = isActive(tab.href)}
					<li class="flex-1">
						<a
							href={resolve(tab.href)}
							class="relative flex min-h-[44px] flex-col items-center justify-center gap-1 rounded-lg py-1 text-[11px] font-semibold transition-all duration-150 {styleStore
								.pack.press.tab}"
							class:text-[var(--color-primary-600)]={active}
							class:text-[var(--color-nav-inactive)]={!active}
							aria-current={active ? 'page' : undefined}
						>
							{#if active}
								<div
									class="absolute inset-0 rounded-lg bg-[var(--color-primary-500)]/15 transition-all duration-150"
								></div>
							{/if}
							<div class="relative z-10 flex flex-col items-center w-full min-w-0 px-1">
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
		</div>
	{/if}
</nav>
