<script lang="ts">
	import { resolve } from '$app/paths';
	import { APP_FEATURES, getFeatureName } from '$lib/features';
	import { ChevronRight } from '@lucide/svelte';
	import BentoCard from '../BentoCard.svelte';

	interface Props {
		id: string;
	}

	let { id }: Props = $props();

	const feature = $derived(APP_FEATURES.find((f) => f.id === id));
	const name = $derived(feature ? getFeatureName(feature.id) : id);

	const descriptions: Record<string, { ja: string; en: string; zh: string; ko: string }> = {
		gpa: {
			ja: '単位数と評価からGPAを計算します',
			en: 'Calculate GPA from credits and grades.',
			zh: '根据学分和成绩计算 GPA。',
			ko: '학점과 성적으로 GPA를 계산합니다.'
		},
		pdf: {
			ja: '複数の写真を1つのPDFにまとめて共有・保存します',
			en: 'Export and combine multiple photos into a single PDF.',
			zh: '将多张照片合并为一个 PDF 导出。',
			ko: '여러 사진을 하나의 PDF로 병합하여 내보냅니다.'
		},
		syllabus: {
			ja: '公開されている科目シラバスを検索・閲覧します',
			en: 'Search and browse course syllabus details.',
			zh: '搜索并浏览公开课程教学大纲。',
			ko: '공개된 과목 강의계획서를 검색하고 열람합니다.'
		},
		'data-browser': {
			ja: '学校の建物、施設、組織などの空間データを閲覧します',
			en: 'Browse campus buildings, facilities, and organizations.',
			zh: '浏览校园建筑物、设施和组织架构。',
			ko: '학교建物、施設、組織等空間データを熱覧します。'
		},
		'data-management': {
			ja: 'RemoteStorage接続、バックアップ、学校データの同期を設定します',
			en: 'Manage remote storage connection, backups, and school data sync.',
			zh: '配置 RemoteStorage 连接、备份及学校数据同步。',
			ko: 'RemoteStorage 연결, 백업, 학교 데이터 동기화를 설정합니다.'
		}
	};

	import { page } from '$app/state';
	const locale = $derived.by<'ja' | 'en' | 'zh' | 'ko'>(() => {
		const path = page.url.pathname;
		if (path.startsWith('/en')) return 'en';
		if (path.startsWith('/zh')) return 'zh';
		if (path.startsWith('/ko')) return 'ko';
		return 'ja';
	});

	const desc = $derived(descriptions[id]?.[locale] || descriptions[id]?.ja || '');
</script>

{#if feature}
	<BentoCard
		href={resolve(feature.href as any)}
		padding="p-4"
		class="flex-row items-center justify-between"
	>
		<div class="flex items-center gap-3.5 min-w-0 flex-1">
			<div
				class="flex h-10 w-10 shrink-0 items-center justify-center rounded-xl bg-[var(--color-primary-500)]/15 text-[var(--color-primary-600)]"
			>
				<feature.icon size={20} />
			</div>
			<div class="flex-1 min-w-0">
				<h4 class="text-sm font-semibold tracking-tight text-[var(--color-nav-active)]">
					{name}
				</h4>
				{#if desc}
					<p class="text-xs text-[var(--color-nav-inactive)] mt-0.5 line-clamp-1">
						{desc}
					</p>
				{/if}
			</div>
		</div>
		<ChevronRight
			size={18}
			class="text-[var(--color-nav-inactive)] shrink-0 ml-2 whitespace-nowrap"
		/>
	</BentoCard>
{/if}
