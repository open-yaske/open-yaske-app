import { describe, it, expect, vi, beforeEach } from 'vitest';

// schoolDataService Mock
const mockSchoolDataService = {
	init: vi.fn(),
	sync: vi.fn(),
	courseItems: [] as any[]
};

vi.mock('./schoolData.svelte', () => ({
	schoolDataService: mockSchoolDataService
}));

describe('publicData', () => {
	beforeEach(() => {
		vi.clearAllMocks();
		mockSchoolDataService.courseItems = [];
	});

	describe('loadPublicData', () => {
		it('calls init on schoolDataService and returns formatted PublicData', async () => {
			mockSchoolDataService.init.mockResolvedValue(undefined);
			mockSchoolDataService.courseItems = [{ id: '1', name: 'Course 1' }];

			const { loadPublicData } = await import('./publicData');
			const data = await loadPublicData();

			expect(mockSchoolDataService.init).toHaveBeenCalled();
			expect(data.courses).toEqual(mockSchoolDataService.courseItems);
			expect(data.index.count).toBe(1);
			expect(data.schemaVersion).toBe(1); // PUBLIC_DATA_SCHEMA_VERSION from public.ts
		});
	});

	describe('refreshPublicData', () => {
		it('calls sync on schoolDataService and loads fresh data', async () => {
			mockSchoolDataService.sync.mockResolvedValue(undefined);
			mockSchoolDataService.init.mockResolvedValue(undefined);
			mockSchoolDataService.courseItems = [{ id: '2', name: 'Course 2' }];

			const { refreshPublicData } = await import('./publicData');
			const data = await refreshPublicData();

			expect(mockSchoolDataService.sync).toHaveBeenCalled();
			expect(mockSchoolDataService.init).toHaveBeenCalled();
			expect(data.courses).toEqual([{ id: '2', name: 'Course 2' }]);
		});
	});

	describe('setPublicDataCdnBase', () => {
		it('runs without throwing errors', async () => {
			const { setPublicDataCdnBase } = await import('./publicData');
			expect(() => setPublicDataCdnBase('http://dummy-cdn.com')).not.toThrow();
		});
	});
});
