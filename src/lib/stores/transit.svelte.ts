import { browser } from '$app/environment';
import { settingsStore } from './settings.svelte';
import { get as idbGet, set as idbSet } from 'idb-keyval';

export interface Departure {
	routeName: string;
	trainType?: string;
	mode: string;
	color?: string;
	headsign?: string;
	tripId: string;
	stopId: string;
	departureSecs: number;
	headwayBased: boolean;
	headwaySecs?: number;
}

export interface DeparturesData {
	stationId: string;
	date: string;
	timezone: string;
	departures: Departure[];
	fetchedAt: number;
}

export interface TransitPlace {
	id: string;
	endpoint: string;
	name: string;
	kind: 'station' | 'stop' | 'place' | 'address';
	source: string;
	lat: number;
	lon: number;
	distanceMeters?: number;
}

export function formatDepartureTime(departureSecs: number): string {
	let secs = departureSecs;
	if (secs < 0) secs += 86400;
	const hrs = Math.floor(secs / 3600) % 24;
	const mins = Math.floor((secs % 3600) / 60);
	return `${hrs.toString().padStart(2, '0')}:${mins.toString().padStart(2, '0')}`;
}

const cache = new Map<string, DeparturesData>();
let idbLoaded = false;

async function ensureIdbLoaded() {
	if (idbLoaded || !browser) return;
	try {
		const allData = await idbGet<[string, DeparturesData][]>('transit_cache_all');
		if (allData && Array.isArray(allData)) {
			for (const [k, v] of allData) {
				cache.set(k, v);
			}
		}
	} catch (err) {
		console.error('Failed to load transit cache from IndexedDB:', err);
	} finally {
		idbLoaded = true;
	}
}

class TransitStore {
	departures = $state<Departure[]>([]);
	stationId = $state<string>('');
	stationName = $state<string>('');
	lastUpdated = $state<number | null>(null);
	loading = $state(false);
	error = $state<string | null>(null);

	nearbyStations = $state<TransitPlace[]>([]);
	loadingNearby = $state(false);
	nearbyError = $state<string | null>(null);

	// その日の0時0分0秒から現在時刻までの秒数（カウントダウン用基準時間）
	currentElapsedSecs = $state<number>(0);
	serviceDateStr = $state<string>('');
	private intervalId: any = null;

	constructor() {
		if (browser) {
			this.startTimer();
		}
	}

	private startTimer() {
		this.updateElapsed();
		this.intervalId = setInterval(() => {
			this.updateElapsed();
			this.checkAndAutoReload();
		}, 1000);
	}

	private updateElapsed() {
		if (!this.serviceDateStr) return;
		const year = parseInt(this.serviceDateStr.substring(0, 4));
		const month = parseInt(this.serviceDateStr.substring(4, 6)) - 1;
		const day = parseInt(this.serviceDateStr.substring(6, 8));
		const serviceDateStart = new Date(year, month, day, 0, 0, 0);
		const now = new Date();
		this.currentElapsedSecs = Math.floor((now.getTime() - serviceDateStart.getTime()) / 1000);
	}

	private checkAndAutoReload() {
		if (this.departures.length === 0 || this.loading) return;

		// 最も早い（直近の）電車の発車秒数を取得
		// すでに過去になった電車は除外して、次にくる電車の出発時刻を基準にする
		const upcoming = this.departures.filter((d) => d.departureSecs >= this.currentElapsedSecs);

		if (upcoming.length > 0) {
			const nextDepartureSecs = upcoming[0].departureSecs;
			// もし現在の経過秒数が次の発車秒数を超えたら（電車が発車したら）、自動リロードする
			if (this.currentElapsedSecs > nextDepartureSecs + 2) {
				// 2秒の猶予を持たせる
				void this.loadDepartures(true);
			}
		} else {
			// 予定されている電車がもう無い場合も再読み込みしてみる
			// ただし最終更新から1分以上経過している場合のみ（API負荷低減のため）
			if (this.lastUpdated && Date.now() - this.lastUpdated > 60000) {
				void this.loadDepartures(true);
			}
		}
	}

	async init() {
		if (!browser) return;
		await ensureIdbLoaded();
		this.stationId = settingsStore.settings.transitStationId || '';
		this.stationName = settingsStore.settings.transitStationName || '';

		if (this.stationId) {
			await this.loadDepartures();
		}
	}

	async setStation(id: string, name: string, lat?: number, lng?: number) {
		this.stationId = id;
		this.stationName = name;
		this.error = null;

		const updates: any = {
			transitStationId: id,
			transitStationName: name
		};
		if (typeof lat === 'number' && typeof lng === 'number') {
			updates.transitLastLat = lat;
			updates.transitLastLng = lng;
		}

		settingsStore.update(updates);
		await this.loadDepartures(true);
	}

	async clearStation() {
		this.stationId = '';
		this.stationName = '';
		this.departures = [];
		this.lastUpdated = null;

		settingsStore.update({
			transitStationId: '',
			transitStationName: '',
			transitLastLat: 0,
			transitLastLng: 0
		});
	}

	async loadDepartures(force = false) {
		if (!browser) return;
		const id = this.stationId || settingsStore.settings.transitStationId;
		if (!id) {
			this.departures = [];
			return;
		}

		await ensureIdbLoaded();

		const cached = cache.get(id);

		// キャッシュ判定：
		// キャッシュがあり、forceでなく、かつキャッシュ内の次の電車の出発時刻を過ぎていない場合
		if (!force && cached) {
			// キャッシュ取得時点の serviceDate に基づいて、現在の経過時間と次の電車を比較
			const year = parseInt(cached.date.substring(0, 4));
			const month = parseInt(cached.date.substring(4, 6)) - 1;
			const day = parseInt(cached.date.substring(6, 8));
			const serviceDateStart = new Date(year, month, day, 0, 0, 0);
			const currentElapsed = Math.floor((Date.now() - serviceDateStart.getTime()) / 1000);

			// 直近의 電車がまだ出発していなければキャッシュをそのまま使う
			const upcoming = cached.departures.filter((d) => d.departureSecs >= currentElapsed);
			if (upcoming.length > 0) {
				this.departures = cached.departures;
				this.serviceDateStr = cached.date;
				this.lastUpdated = cached.fetchedAt;
				this.updateElapsed();
				return;
			}
		}

		this.loading = true;
		this.error = null;

		try {
			const res = await fetch(
				`https://api.transit.ls8h.com/api/v1/stations/${encodeURIComponent(id)}/departures`
			);
			if (!res.ok) {
				throw new Error(`Failed to fetch departures: ${res.status}`);
			}
			const data = (await res.json()) as {
				date: string;
				timezone: string;
				departures: Departure[];
			};

			const fetchedData: DeparturesData = {
				stationId: id,
				date: data.date,
				timezone: data.timezone,
				departures: data.departures || [],
				fetchedAt: Date.now()
			};

			// キャッシュを保存
			cache.set(id, fetchedData);
			await idbSet('transit_cache_all', Array.from(cache.entries()));

			// 状態を更新
			this.departures = fetchedData.departures;
			this.serviceDateStr = fetchedData.date;
			this.lastUpdated = fetchedData.fetchedAt;
			this.updateElapsed();
		} catch (e) {
			this.error = e instanceof Error ? e.message : 'Failed to load departures';
			// エラー時は可能なら古いキャッシュを表示
			if (cached) {
				this.departures = cached.departures;
				this.serviceDateStr = cached.date;
			}
		} finally {
			this.loading = false;
		}
	}

	async searchNearbyStations(lat: number, lng: number) {
		this.loadingNearby = true;
		this.nearbyError = null;
		this.nearbyStations = [];

		try {
			// radiusMeters=500(最大) で周辺スポットを検索
			const res = await fetch(
				`https://api.transit.ls8h.com/api/v1/places/reverse?lat=${lat}&lon=${lng}&radiusMeters=500&limit=10`
			);
			if (!res.ok) {
				throw new Error(`Failed to search nearby places: ${res.status}`);
			}
			const data = (await res.json()) as { places: TransitPlace[] };

			// kind が station または stop で、かつ運行データを持たないランドマーク等（transit:, osm:, geocoder:）を除外
			const filtered = (data.places || []).filter(
				(p) =>
					(p.kind === 'station' || p.kind === 'stop') &&
					!p.id.startsWith('transit:') &&
					!p.id.startsWith('osm:') &&
					!p.id.startsWith('geocoder:')
			);

			this.nearbyStations = filtered;
		} catch (e) {
			this.nearbyError = e instanceof Error ? e.message : 'Failed to search nearby stations';
		} finally {
			this.loadingNearby = false;
		}
	}

	async searchStationsByName(query: string): Promise<TransitPlace[]> {
		if (!query.trim()) return [];
		try {
			const res = await fetch(
				`https://api.transit.ls8h.com/api/v1/places/suggest?q=${encodeURIComponent(query)}`
			);
			if (!res.ok) {
				throw new Error(`Failed to suggest places: ${res.status}`);
			}
			const data = (await res.json()) as { places: TransitPlace[] };
			return (data.places || []).filter(
				(p) =>
					(p.kind === 'station' || p.kind === 'stop') &&
					!p.id.startsWith('transit:') &&
					!p.id.startsWith('osm:') &&
					!p.id.startsWith('geocoder:')
			);
		} catch (e) {
			console.error('Failed to search stations by name:', e);
			return [];
		}
	}
}

export const transitStore = new TransitStore();
