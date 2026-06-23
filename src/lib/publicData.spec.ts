import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest';
import type { PublicData } from './types/public';

const mockGet = vi.hoisted(() => vi.fn());
const mockSet = vi.hoisted(() => vi.fn());
const mockDel = vi.hoisted(() => vi.fn());

vi.mock('idb-keyval', () => ({
	get: mockGet,
	set: mockSet,
	del: mockDel
}));

const mockFetch = vi.fn();
vi.stubGlobal('fetch', mockFetch);

const BASE_TIME = 1_000_000_000_000;
const TTL_MS = 24 * 60 * 60 * 1000;

function makePublicData(schemaVersion = 1): PublicData {
	return {
		schemaVersion: schemaVersion as 1,
		generatedAt: '2026-06-22T00:00:00Z',
		index: { version: 1, generatedAt: '2026-06-22T00:00:00Z', count: 0 },
		courses: [],
		dashboardCards: []
	};
}

describe('publicData', () => {
	beforeEach(() => {
		vi.resetModules();
		mockGet.mockReset();
		mockSet.mockReset();
		mockDel.mockReset();
		mockFetch.mockReset();
		vi.spyOn(Date, 'now').mockReturnValue(BASE_TIME);
	});

	afterEach(() => {
		vi.restoreAllMocks();
	});

	describe('loadPublicData', () => {
		it('fetches from CDN when no cache exists', async () => {
			mockGet.mockResolvedValue(undefined);
			mockFetch.mockResolvedValue({
				ok: true,
				json: async () => makePublicData()
			});

			const { loadPublicData } = await import('./publicData');
			const data = await loadPublicData();
			expect(data.schemaVersion).toBe(1);
			expect(mockFetch).toHaveBeenCalledTimes(1);
			expect(mockFetch).toHaveBeenCalledWith(
				'https://cdn.jsdelivr.net/gh/ueckoken/open-nexus-data@main/public-data.json',
				expect.objectContaining({ signal: expect.any(AbortSignal) })
			);
		});

		it('returns in-memory cache immediately when valid', async () => {
			mockGet.mockResolvedValue(undefined);
			mockFetch.mockResolvedValue({
				ok: true,
				json: async () => makePublicData()
			});

			const { loadPublicData } = await import('./publicData');
			await loadPublicData(); // populate in-memory cache

			mockFetch.mockClear();
			const data = await loadPublicData();
			expect(data.schemaVersion).toBe(1);
			// SWR triggers background revalidation
			expect(mockFetch).toHaveBeenCalledTimes(1);
		});

		it('returns stale cache immediately while revalidating in background', async () => {
			mockGet.mockResolvedValue(undefined);
			mockFetch.mockResolvedValueOnce({
				ok: true,
				json: async () => makePublicData()
			});

			const { loadPublicData } = await import('./publicData');
			await loadPublicData(); // populate cache

			// Make next fetch hang
			let resolveFetch: (v: unknown) => void;
			const fetchPromise = new Promise<unknown>((resolve) => {
				resolveFetch = resolve;
			});
			mockFetch.mockReturnValueOnce(fetchPromise);

			const dataPromise = loadPublicData();
			const data = await dataPromise;
			expect(data.schemaVersion).toBe(1);
			expect(mockFetch).toHaveBeenCalledTimes(2);

			resolveFetch!({
				ok: true,
				json: async () => makePublicData()
			});
		});

		it('falls back to IndexedDB when in-memory expired', async () => {
			const cached = {
				data: makePublicData(),
				fetchedAt: BASE_TIME - TTL_MS - 1000
			};
			mockGet.mockResolvedValueOnce(cached);
			mockFetch.mockResolvedValueOnce({
				ok: true,
				json: async () => makePublicData()
			});

			const { loadPublicData } = await import('./publicData');
			const data = await loadPublicData();
			expect(data.schemaVersion).toBe(1);
			expect(mockFetch).toHaveBeenCalledTimes(1);
		});

		it('returns null and refetches when IndexedDB TTL expired', async () => {
			const stale = {
				data: makePublicData(),
				fetchedAt: BASE_TIME - TTL_MS - 1000
			};
			mockGet.mockResolvedValueOnce(stale);
			mockFetch.mockResolvedValueOnce({
				ok: true,
				json: async () => makePublicData()
			});

			const { loadPublicData } = await import('./publicData');
			const data = await loadPublicData();
			expect(data.schemaVersion).toBe(1);
			expect(mockFetch).toHaveBeenCalledTimes(1);
		});

		it('returns null and refetches when schema version mismatch in cache', async () => {
			const bad = {
				data: makePublicData(999),
				fetchedAt: BASE_TIME
			};
			mockGet.mockResolvedValueOnce(bad);
			mockFetch.mockResolvedValueOnce({
				ok: true,
				json: async () => makePublicData()
			});

			const { loadPublicData } = await import('./publicData');
			const data = await loadPublicData();
			expect(data.schemaVersion).toBe(1);
			expect(mockFetch).toHaveBeenCalledTimes(1);
		});

		it('falls back to stale cache when CDN fails and cache exists', async () => {
			const cached = {
				data: makePublicData(),
				fetchedAt: BASE_TIME
			};
			mockGet.mockResolvedValueOnce(cached);
			mockFetch.mockRejectedValueOnce(new Error('Network error'));

			const { loadPublicData } = await import('./publicData');
			const data = await loadPublicData();
			expect(data.schemaVersion).toBe(1);
			expect(mockFetch).toHaveBeenCalledTimes(1);
		});

		it('throws when no cache and CDN fails (offline)', async () => {
			mockGet.mockResolvedValue(undefined);
			mockFetch.mockRejectedValue(new Error('Network error'));

			const { loadPublicData } = await import('./publicData');
			await expect(loadPublicData()).rejects.toThrow('Network error');
		});

		it('throws on HTTP error from CDN', async () => {
			mockGet.mockResolvedValue(undefined);
			mockFetch.mockResolvedValue({ ok: false, status: 503 });

			const { loadPublicData } = await import('./publicData');
			await expect(loadPublicData()).rejects.toThrow('HTTP 503');
		});

		it('throws on schema version mismatch from CDN', async () => {
			mockGet.mockResolvedValue(undefined);
			mockFetch.mockResolvedValue({
				ok: true,
				json: async () => makePublicData(2)
			});

			const { loadPublicData } = await import('./publicData');
			await expect(loadPublicData()).rejects.toThrow('Unsupported schemaVersion: 2');
		});

		it('writes fetched data to IndexedDB cache', async () => {
			mockGet.mockResolvedValue(undefined);
			mockFetch.mockResolvedValue({
				ok: true,
				json: async () => makePublicData()
			});

			const { loadPublicData } = await import('./publicData');
			await loadPublicData();
			expect(mockSet).toHaveBeenCalledTimes(1);
			expect(mockSet).toHaveBeenCalledWith(
				'open-nexus:public-data',
				expect.objectContaining({ data: expect.any(Object), fetchedAt: BASE_TIME })
			);
		});
	});

	describe('refreshPublicData', () => {
		it('clears cache and fetches fresh data', async () => {
			mockGet.mockResolvedValue(undefined);
			mockFetch.mockResolvedValue({
				ok: true,
				json: async () => makePublicData()
			});

			const { refreshPublicData } = await import('./publicData');
			const data = await refreshPublicData();
			expect(data.schemaVersion).toBe(1);
			expect(mockDel).toHaveBeenCalledTimes(1);
			expect(mockDel).toHaveBeenCalledWith('open-nexus:public-data');
		});
	});

	describe('findCourse', () => {
		it('finds course by id', async () => {
			const { findCourse } = await import('./publicData');
			const data: PublicData = {
				...makePublicData(),
				courses: [
					{
						id: 'c1',
						code: 'CS101',
						name: 'Intro',
						credits: 2,
						type: 'required',
						semester: 'spring',
						year: 2026
					},
					{
						id: 'c2',
						code: 'CS102',
						name: 'Advanced',
						credits: 2,
						type: 'elective',
						semester: 'fall',
						year: 2026
					}
				]
			};
			expect(findCourse(data, 'c1')?.name).toBe('Intro');
			expect(findCourse(data, 'c999')).toBeUndefined();
		});
	});

	describe('sortedCards', () => {
		it('sorts dashboard cards by order', async () => {
			const { sortedCards } = await import('./publicData');
			const data: PublicData = {
				...makePublicData(),
				dashboardCards: [
					{
						type: 'external-url',
						id: 'b',
						title: 'B',
						icon: 'link',
						url: 'https://b.com',
						openInApp: false,
						order: 2
					},
					{
						type: 'external-url',
						id: 'a',
						title: 'A',
						icon: 'link',
						url: 'https://a.com',
						openInApp: false,
						order: 1
					}
				]
			};
			const cards = sortedCards(data);
			expect(cards[0].id).toBe('a');
			expect(cards[1].id).toBe('b');
		});
	});
});
