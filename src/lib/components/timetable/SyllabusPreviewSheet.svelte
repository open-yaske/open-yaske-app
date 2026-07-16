<!--
  SyllabusPreviewSheet.svelte
  PublicCourse のシラバス詳細を表示する軽量シート。
  CourseEditSheet の「詳細を見る」から開かれる、編集フローを中断しない参照用コンポーネント。
  時間割に登録済みかどうかの表示も行う。
-->
<script lang="ts">
	import { m } from '$lib/paraglide/messages';
	import { publicDataStore } from '$lib/stores';
	import { sanitize, safeTarget } from '$lib/html-renderer';
	import type { PublicCourse } from '$lib/types/public';
	import Sheet from '$lib/components/Sheet.svelte';
	import Button from '$lib/components/Button.svelte';
	import { ExternalLink, BookOpen } from '@lucide/svelte';

	interface Props {
		open: boolean;
		onclose: () => void;
		/** 表示するシラバスID（publicDataStore から検索） */
		syllabusId: string | null;
	}

	let { open = $bindable(false), onclose, syllabusId }: Props = $props();

	const publicCourse = $derived.by<PublicCourse | null>(() => {
		if (!syllabusId) return null;
		return publicDataStore.data?.courses?.find((c) => c.id === syllabusId) ?? null;
	});

	const sanitizedHtml = $derived.by(() => {
		if (!publicCourse?.descriptionHtml) return '';
		return safeTarget(sanitize(publicCourse.descriptionHtml));
	});

	function formatDayPeriod(dayPeriods: { day: string; period: number }[]) {
		if (!dayPeriods || dayPeriods.length === 0) return 'オンデマンド/集中';
		const days: Record<string, string> = {
			mon: '月',
			tue: '火',
			wed: '水',
			thu: '木',
			fri: '金',
			sat: '土',
			sun: '日'
		};
		return dayPeriods.map((dp) => `${days[dp.day] || dp.day}${dp.period}`).join(', ');
	}
</script>

<Sheet bind:open {onclose} title={m.course_syllabus()}>
	{#if publicCourse}
		<div class="space-y-4">
			<!-- ヘッダー -->
			<div class="rounded-chip bg-[var(--color-surface-muted)] p-3 flex items-start gap-3">
				<span
					class="mt-0.5 flex h-9 w-9 shrink-0 items-center justify-center rounded-full bg-[var(--color-primary-100)] text-[var(--color-primary-700)]"
				>
					<BookOpen size={18} />
				</span>
				<div class="min-w-0">
					<h3 class="text-base font-semibold text-[var(--color-nav-active)] leading-snug">
						{publicCourse.name}
					</h3>
					{#if publicCourse.id}
						<p class="mt-0.5 text-xs text-[var(--color-nav-inactive)]">{publicCourse.id}</p>
					{/if}
				</div>
			</div>

			<!-- メタデータ -->
			<dl class="grid grid-cols-2 gap-x-4 gap-y-2 text-sm">
				{#if publicCourse.teacher}
					<div class="col-span-2">
						<dt class="text-xs font-medium text-[var(--color-nav-inactive)]">
							{m.course_teacher()}
						</dt>
						<dd class="mt-0.5 text-[var(--color-nav-active)]">{publicCourse.teacher}</dd>
					</div>
				{/if}
				{#if publicCourse.dayPeriods?.length}
					<div>
						<dt class="text-xs font-medium text-[var(--color-nav-inactive)]">{m.course_day()}</dt>
						<dd class="mt-0.5 text-[var(--color-nav-active)]">
							{formatDayPeriod(publicCourse.dayPeriods)}
						</dd>
					</div>
				{/if}
				{#if publicCourse.credits !== undefined}
					<div>
						<dt class="text-xs font-medium text-[var(--color-nav-inactive)]">
							{m.course_credits()}
						</dt>
						<dd class="mt-0.5 text-[var(--color-nav-active)]">{publicCourse.credits} 単位</dd>
					</div>
				{/if}
				{#if publicCourse.classroom}
					<div class="col-span-2">
						<dt class="text-xs font-medium text-[var(--color-nav-inactive)]">
							{m.course_classroom()}
						</dt>
						<dd class="mt-0.5 text-[var(--color-nav-active)]">{publicCourse.classroom}</dd>
					</div>
				{/if}
			</dl>

			<!-- シラバス本文 -->
			{#if sanitizedHtml}
				<div class="border-t border-[var(--color-surface-border)] pt-3">
					<h4 class="mb-2 text-sm font-semibold text-[var(--color-nav-active)]">
						{m.course_syllabus()}
					</h4>
					<div class="prose prose-sm max-w-none text-[var(--color-nav-active)]">
						<!-- sanitizedHtml is processed via DOMPurify in lib/html-renderer.ts -->
						<!-- eslint-disable-next-line svelte/no-at-html-tags -->
						{@html sanitizedHtml}
					</div>
				</div>
			{:else}
				<div class="border-t border-[var(--color-surface-border)] pt-3">
					<p class="text-sm text-[var(--color-nav-inactive)]">{m.empty_no_data()}</p>
				</div>
			{/if}

			<!-- 参考リンク -->
			{#if publicCourse.references?.length}
				<div class="border-t border-[var(--color-surface-border)] pt-3 space-y-1">
					<h5 class="text-xs font-medium text-[var(--color-nav-inactive)]">References</h5>
					{#each publicCourse.references as ref (ref.url)}
						<!-- eslint-disable svelte/no-navigation-without-resolve -->
						<a
							href={ref.url}
							target="_blank"
							rel="noopener noreferrer"
							class="flex items-center gap-1.5 text-sm text-[var(--color-primary-500)] hover:underline"
						>
							<ExternalLink size={12} />
							{ref.title}
						</a>
						<!-- eslint-enable svelte/no-navigation-without-resolve -->
					{/each}
				</div>
			{/if}
		</div>
	{:else}
		<div class="flex flex-col items-center justify-center py-16 gap-3">
			<BookOpen size={36} class="text-[var(--color-nav-inactive)]" />
			<p class="text-sm text-[var(--color-nav-inactive)]">{m.empty_no_data()}</p>
		</div>
	{/if}

	{#snippet footer()}
		<Button variant="primary" size="md" class="w-full" onclick={onclose}>
			{m.action_close()}
		</Button>
	{/snippet}
</Sheet>
