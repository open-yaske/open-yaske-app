/**
 * 公開データ取得 (CDN → IndexedDB キャッシュ)
 *
 * - stale-while-revalidate
 * - TTL 24h
 * - schemaVersion チェック (将来は 1 から上げても後方互換)
 * - URL は優先度: 1) settings.dataCdnUrl 2) import.meta.env.PUBLIC_DATA_CDN_BASE
 *   3) 既定の jsDelivr
 */

import { get as idbGet, set as idbSet, del as idbDel } from 'idb-keyval';
import { PUBLIC_DATA_SCHEMA_VERSION, type PublicData } from './types/public';

import { STORAGE_KEY_PREFIX, PUBLIC_DATA_CDN_BASE } from './constants';

const DEFAULT_CDN_BASE = PUBLIC_DATA_CDN_BASE;
const CACHE_KEY = STORAGE_KEY_PREFIX + 'public-data';
const TTL_MS = 24 * 60 * 60 * 1000; // 24 hours

type CachedPublic = {
	data: PublicData;
	fetchedAt: number;
};

let inMemory: CachedPublic | null = null;
let inflight: Promise<PublicData> | null = null;

/** ランタイムオーバーライド (Settings 画面から変更可能) */
let runtimeCdnOverride: string | null = null;

/**
 * 使用する CDN ベース URL を解決
 */
function resolveCdnBase(): string {
	if (runtimeCdnOverride && runtimeCdnOverride.trim().length > 0) {
		return runtimeCdnOverride.replace(/\/+$/, '');
	}
	const envBase =
		typeof import.meta !== 'undefined' && import.meta.env
			? (import.meta.env as { PUBLIC_DATA_CDN_BASE?: string }).PUBLIC_DATA_CDN_BASE
			: undefined;
	if (envBase && envBase.trim().length > 0) {
		return envBase.replace(/\/+$/, '');
	}
	return DEFAULT_CDN_BASE;
}

/**
 * ランタイム CDN ベース URL を上書き (Settings 用)
 */
export function setPublicDataCdnBase(url: string | null): void {
	runtimeCdnOverride = url && url.trim().length > 0 ? url : null;
	// 上書き時はキャッシュ無効化
	inMemory = null;
	void idbDel(CACHE_KEY).catch(() => undefined);
}

/**
 * 現在の CDN ベース URL を取得
 */
export function getPublicDataCdnBase(): string {
	return resolveCdnBase();
}

/**
 * IndexedDB から読み出し (TTL 判定)
 */
async function readCache(): Promise<PublicData | null> {
	if (inMemory && Date.now() - inMemory.fetchedAt < TTL_MS) {
		return inMemory.data;
	}
	const cached = (await idbGet(CACHE_KEY)) as CachedPublic | undefined;
	if (!cached) return null;
	if (Date.now() - cached.fetchedAt > TTL_MS) return null;
	if (cached.data.schemaVersion !== PUBLIC_DATA_SCHEMA_VERSION) {
		// 不明なバージョン → フォールバック
		return null;
	}
	inMemory = cached;
	return cached.data;
}

/**
 * IndexedDB に書き出し
 */
async function writeCache(data: PublicData) {
	const wrapped: CachedPublic = { data, fetchedAt: Date.now() };
	inMemory = wrapped;
	await idbSet(CACHE_KEY, wrapped);
}

/**
 * CDN から取得 (タイムアウト 10s)
 */
async function fetchFromCDN(): Promise<PublicData> {
	const url = `${resolveCdnBase()}/public-data.json`;
	const ctl = new AbortController();
	const t = setTimeout(() => ctl.abort(), 10_000);
	try {
		const res = await fetch(url, { signal: ctl.signal });
		if (!res.ok) throw new Error(`HTTP ${res.status}`);
		const data = (await res.json()) as PublicData;
		if (data.schemaVersion !== PUBLIC_DATA_SCHEMA_VERSION) {
			throw new Error(`Unsupported schemaVersion: ${data.schemaVersion}`);
		}
		return data;
	} finally {
		clearTimeout(t);
	}
}

/**
 * 公開データ取得 (stale-while-revalidate)
 * - キャッシュが新しければ即返却、並行して裏で再取得
 * - キャッシュが無ければ CDN を待つ
 */
export async function loadPublicData(): Promise<PublicData> {
	const cached = await readCache();
	if (cached) {
		// stale-while-revalidate: 裏で再取得
		revalidate().catch(() => undefined);
		return cached;
	}
	return revalidate();
}

async function revalidate(): Promise<PublicData> {
	if (inflight) return inflight;
	inflight = fetchFromCDN();
	try {
		const data = await inflight;
		await writeCache(data);
		return data;
	} finally {
		inflight = null;
	}
}

/**
 * キャッシュを削除して再取得
 */
export async function refreshPublicData(): Promise<PublicData> {
	await idbDel(CACHE_KEY);
	inMemory = null;
	return revalidate();
}

/**
 * 公開データから科目を ID で取得
 */
export function findCourse(data: PublicData, id: string) {
	return data.courses.find((c) => c.id === id);
}

/**
 * 公開データからカードを order 順で取得
 */
export function sortedCards(data: PublicData) {
	return [...data.dashboardCards].sort((a, b) => a.order - b.order);
}
