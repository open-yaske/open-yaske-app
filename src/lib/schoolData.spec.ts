import { describe, it, expect, vi, beforeEach } from 'vitest';
import { SchoolDataService } from './schoolData.svelte';

// Environment Mock: Set browser to false to prevent automatic background init in constructor
vi.mock('$app/environment', () => ({
	browser: false
}));

// idb-keyval Mock
const mockIdb = {
	get: vi.fn(),
	set: vi.fn(),
	del: vi.fn()
};
vi.mock('idb-keyval', () => ({
	get: (key: string) => mockIdb.get(key),
	set: (key: string, val: any) => mockIdb.set(key, val),
	del: (key: string) => mockIdb.del(key)
}));

// RemoteStorage Mock
const mockRS = {
	isConnected: vi.fn(() => false),
	rsGet: vi.fn(),
	rsSet: vi.fn(),
	rsDelete: vi.fn()
};
vi.mock('./remotestorage', () => ({
	isConnected: () => mockRS.isConnected(),
	rsGet: (path: string) => mockRS.rsGet(path),
	rsSet: (path: string, val: any) => mockRS.rsSet(path, val),
	rsDelete: (path: string) => mockRS.rsDelete(path)
}));

// settingsStore Mock
vi.mock('./stores', () => ({
	settingsStore: {
		settings: {
			schoolDataSparqlEndpoint: 'https://example.com/sparql'
		}
	}
}));

// fetch Mock
const mockFetch = vi.fn();
vi.stubGlobal('fetch', mockFetch);

describe('SchoolDataService', () => {
	let service: SchoolDataService;

	beforeEach(() => {
		vi.clearAllMocks();
		mockFetch.mockReset();
		mockIdb.get.mockReset();
		mockIdb.set.mockReset();
		mockIdb.del.mockReset();
		mockRS.isConnected.mockReturnValue(false);
		mockRS.rsGet.mockReset();
		mockRS.rsSet.mockReset();
		mockRS.rsDelete.mockReset();

		// Create a clean instance for each test.
		// Since browser is mocked as false, no setTimeout automatic init is executed.
		service = new SchoolDataService();
		service.loading = false;
	});

	describe('init', () => {
		it('loads metadata and local cache from IndexedDB', async () => {
			const mockMeta = { lastSyncedAt: 1234567890, courseCount: 1 };
			const mockCourses = [{ id: '123', name: 'Test Course' } as any];

			mockIdb.get.mockImplementation((key: string) => {
				if (key.endsWith('school-metadata')) return Promise.resolve(mockMeta);
				if (key.endsWith('school-courses')) return Promise.resolve(mockCourses);
				return Promise.resolve(null);
			});

			await service.init();

			expect(service.metadata).toEqual(mockMeta);
			expect(service.courseItems).toEqual(mockCourses);
			expect(service.loading).toBe(false);
		});

		it('gracefully handles IndexedDB load errors', async () => {
			mockIdb.get.mockRejectedValue(new Error('IDB Error'));

			await service.init();

			expect(service.metadata).toBeNull();
			expect(service.courseItems).toEqual([]);
		});
	});

	describe('clearCache', () => {
		it('deletes data from local cache and sets state to empty', async () => {
			await service.clearCache();

			expect(mockIdb.del).toHaveBeenCalledTimes(2);
			expect(service.metadata).toBeNull();
			expect(service.courseItems).toEqual([]);
			expect(service.syncStatus).toContain('消去完了');
		});

		it('triggers RemoteStorage delete if connected', async () => {
			mockRS.isConnected.mockReturnValue(true);
			mockRS.rsDelete.mockResolvedValue(undefined);

			await service.clearCache();

			expect(mockRS.rsDelete).toHaveBeenCalledWith('school-data/courses');
			expect(mockRS.rsDelete).toHaveBeenCalledWith('school-data/metadata');
		});

		it('gracefully handles errors and sets syncStatus to failed', async () => {
			mockIdb.del.mockRejectedValue(new Error('IDB delete failed'));

			await expect(service.clearCache()).rejects.toThrow('IDB delete failed');
			expect(service.syncStatus).toContain('失敗');
			expect(service.loading).toBe(false);
		});
	});

	describe('importCustomCourses', () => {
		it('imports custom courses and updates states', async () => {
			const customCourses = [{ id: 'c1', name: 'Custom' } as any];
			mockIdb.set.mockResolvedValue(undefined);

			await service.importCustomCourses(customCourses);

			expect(mockIdb.set).toHaveBeenCalledTimes(2);
			expect(service.courseItems).toEqual(customCourses);
			expect(service.metadata).not.toBeNull();
			expect(service.syncStatus).toContain('完了');
		});

		it('uploads custom courses to RemoteStorage if connected', async () => {
			mockRS.isConnected.mockReturnValue(true);
			mockRS.rsSet.mockResolvedValue(undefined);

			const customCourses = [{ id: 'c1', name: 'Custom' } as any];
			await service.importCustomCourses(customCourses);

			expect(mockRS.rsSet).toHaveBeenCalledTimes(2);
		});
	});

	describe('sync', () => {
		it('restores syllabus data from RemoteStorage if metadata exists on RS', async () => {
			mockRS.isConnected.mockReturnValue(true);
			const mockMeta = { lastSyncedAt: 12345, courseCount: 1 };
			const mockCourses = [{ id: 'rs1', name: 'RS Course' } as any];

			mockRS.rsGet.mockImplementation((path: string) => {
				if (path === 'school-data/metadata') return Promise.resolve(mockMeta);
				if (path === 'school-data/courses') return Promise.resolve(mockCourses);
				return Promise.resolve(null);
			});

			await service.sync();

			expect(service.metadata).toEqual(mockMeta);
			expect(service.courseItems).toEqual(mockCourses);
			expect(service.syncStatus).toContain('復元完了');
		});

		it('fetches syllabus from SPARQL endpoint and parses correctly when RS is empty/disconnected', async () => {
			mockRS.isConnected.mockReturnValue(false);

			const sparqlResult = {
				results: {
					bindings: [
						{
							uri: { value: 'https://uec-atlas.org/lecture/L01' },
							code: { value: 'K001' },
							nameJa: { value: '物理学' },
							credits: { value: '2' },
							semester: { value: '前学期' },
							year: { value: '2026' },
							url: { value: 'https://example.com/phys' },
							period: { value: '月2' },
							teacherName: { value: '佐藤' }
						},
						{
							uri: { value: 'https://uec-atlas.org/lecture/L01' },
							code: { value: 'K001' },
							nameJa: { value: '物理学' },
							credits: { value: '2' },
							semester: { value: '前学期' },
							year: { value: '2026' },
							url: { value: 'https://example.com/phys' },
							period: { value: '月3' },
							teacherName: { value: '鈴木' }
						}
					]
				}
			};

			mockFetch.mockResolvedValue({
				ok: true,
				json: async () => sparqlResult
			});

			await service.sync();

			expect(mockFetch).toHaveBeenCalled();
			expect(service.courseItems).toHaveLength(1);

			const course = service.courseItems[0];
			expect(course.id).toBe('L01');
			expect(course.code).toBe('K001');
			expect(course.name).toBe('物理学');
			expect(course.teacher).toBe('佐藤, 鈴木');
			expect(course.credits).toBe(2);
			expect(course.semester).toBe('spring');
			expect(course.year).toBe(2026);
			expect(course.dayPeriods).toEqual([
				{ day: 'mon', period: 2 },
				{ day: 'mon', period: 3 }
			]);
			expect(course.references).toEqual([{ title: 'シラバス', url: 'https://example.com/phys' }]);

			expect(mockIdb.set).toHaveBeenCalledTimes(2);
			expect(service.syncStatus).toContain('完了');
		});

		it('sets syncStatus to failed if fetch fails', async () => {
			mockRS.isConnected.mockReturnValue(false);
			mockFetch.mockResolvedValue({ ok: false, status: 500 });

			await expect(service.sync()).rejects.toThrow('SPARQL query failed with status 500');
			expect(service.syncStatus).toContain('失敗');
		});
	});
});
