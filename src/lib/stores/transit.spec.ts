import { describe, it, expect, vi, beforeEach } from 'vitest';

const mockFetch = vi.fn();
vi.stubGlobal('fetch', mockFetch);

// settingsStoreのモック
vi.mock('./settings.svelte', () => {
	return {
		settingsStore: {
			settings: {
				transitStationId: '',
				transitStationName: '',
				transitLastLat: 0,
				transitLastLng: 0,
				weatherShowTransit: false,
				lat: 35.6586,
				lng: 139.7454
			},
			update: vi.fn()
		}
	};
});

describe('transitStore', () => {
	beforeEach(() => {
		vi.resetModules();
		mockFetch.mockReset();
	});

	it('initializes with empty defaults', async () => {
		const { transitStore } = await import('./transit.svelte');
		expect(transitStore.stationId).toBe('');
		expect(transitStore.stationName).toBe('');
		expect(transitStore.departures).toEqual([]);
	});

	describe('searchNearbyStations', () => {
		it('calls places/reverse and filters station or stop', async () => {
			mockFetch.mockResolvedValue({
				ok: true,
				json: async () => ({
					places: [
						{
							id: 'tokyo-keio-rail:Chofu',
							endpoint: 'tokyo-keio-rail:Chofu',
							name: '調布',
							kind: 'station',
							source: 'transit',
							lat: 35.6519,
							lon: 139.5446,
							distanceMeters: 100
						},
						{
							id: 'osm:node:280623691',
							endpoint: 'geo:35.6517,139.5448',
							name: '調布駅(OSM)',
							kind: 'station',
							source: 'osm',
							lat: 35.6517,
							lon: 139.5448,
							distanceMeters: 120
						},
						{
							id: 'osm:node:123',
							endpoint: 'geo:35.65,139.54',
							name: 'カフェ',
							kind: 'place',
							source: 'osm',
							lat: 35.6508,
							lon: 139.5441,
							distanceMeters: 50
						}
					]
				})
			});

			const { transitStore } = await import('./transit.svelte');
			await transitStore.searchNearbyStations(35.651, 139.544);

			// check fetch URL
			const firstCallArg = mockFetch.mock.calls[0][0];
			expect(firstCallArg).toContain('/api/v1/places/reverse');
			expect(firstCallArg).toContain('lat=35.651');
			expect(firstCallArg).toContain('lon=139.544');

			// Check that it only keeps 'station' or 'stop'
			expect(transitStore.nearbyStations).toHaveLength(1);
			expect(transitStore.nearbyStations[0].id).toBe('tokyo-keio-rail:Chofu');
			expect(transitStore.nearbyStations[0].name).toBe('調布');
		});
	});

	describe('searchStationsByName', () => {
		it('suggests places based on query', async () => {
			mockFetch.mockResolvedValue({
				ok: true,
				json: async () => ({
					places: [
						{
							id: 'tokyo-keio-rail:Chofu',
							endpoint: 'tokyo-keio-rail:Chofu',
							name: '調布',
							kind: 'station',
							source: 'transit',
							lat: 35.6519,
							lon: 139.5446
						}
					]
				})
			});

			const { transitStore } = await import('./transit.svelte');
			const results = await transitStore.searchStationsByName('調布');

			const firstCallArg = mockFetch.mock.calls[0][0];
			expect(firstCallArg).toContain('/api/v1/places/suggest');
			expect(firstCallArg).toContain('q=%E8%AA%BF%E5%B8%83'); // URI Encoded '調布'

			expect(results).toHaveLength(1);
			expect(results[0].name).toBe('調布');
		});
	});
});
