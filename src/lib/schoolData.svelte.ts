import { get as idbGet, set as idbSet, del as idbDel } from 'idb-keyval';
import { STORAGE_KEY_PREFIX } from './constants';
import * as remoteStorage from './remotestorage';
import { browser } from '$app/environment';
import { settingsStore } from './stores';
import type { PublicCourse } from './types/public';
import { PUBLIC_DATA_SCHEMA_VERSION } from './types/public';
import type { DayOfWeek, Period } from './types';
import { m } from '$lib/paraglide/messages';

export type SchoolDataMetadata = {
	lastSyncedAt: number;
	courseCount: number;
};

class SchoolDataService {
	// リアクティブステート (Svelte 5 Runes)
	metadata = $state.raw<SchoolDataMetadata | null>(null);
	loading = $state(false);
	syncStatus = $state('');

	// メモリ上でのデータキャッシュ (イミュータブルな巨大配列はプロキシを避けるため $state.raw にする)
	courseItems = $state.raw<PublicCourse[]>([]);

	private COURSE_KEY = STORAGE_KEY_PREFIX + 'school-courses';
	private METADATA_KEY = STORAGE_KEY_PREFIX + 'school-metadata';

	constructor() {
		if (browser) {
			// UI遷移・スレッドのブロックを回避するため、次のフレームに逃がして非同期初期化
			setTimeout(() => void this.init(), 50);
		}
	}

	async init() {
		this.loading = true;
		try {
			await this.loadMetadata();
			await this.loadLocalCache();
		} catch (e) {
			console.error('Failed to initialize school data service:', e);
		} finally {
			this.loading = false;
		}
	}

	async loadMetadata() {
		const meta = await idbGet<SchoolDataMetadata>(this.METADATA_KEY);
		this.metadata = meta || null;
	}

	async loadLocalCache() {
		try {
			this.courseItems = (await idbGet<PublicCourse[]>(this.COURSE_KEY)) || [];
		} catch (e) {
			console.warn('Failed to load local school data cache:', e);
			this.courseItems = [];
		}
	}

	private async uploadToRS(courses: PublicCourse[], meta: SchoolDataMetadata) {
		try {
			await Promise.all([
				remoteStorage.rsSet('school-data/courses', courses),
				remoteStorage.rsSet('school-data/metadata', meta)
			]);
		} catch (err) {
			console.error('Failed to upload school data to RemoteStorage in background:', err);
		}
	}

	/**
	 * キャッシュの消去
	 */
	async clearCache() {
		this.loading = true;
		this.syncStatus = m.school_data_status_clearing_cache();
		try {
			await idbDel(this.COURSE_KEY);
			await idbDel(this.METADATA_KEY);
			this.metadata = null;
			this.courseItems = [];

			if (remoteStorage.isConnected()) {
				// 削除もバックグラウンドで非同期実行
				void Promise.all([
					remoteStorage.rsDelete('school-data/courses'),
					remoteStorage.rsDelete('school-data/metadata')
				]).catch((err) => {
					console.error('Failed to delete school data from RemoteStorage:', err);
				});
			}
			this.syncStatus = m.school_data_status_clear_cache_complete();
		} catch (err: any) {
			console.error('Failed to clear school data cache:', err);
			this.syncStatus = m.school_data_status_clear_cache_failed({
				error: err.message || String(err)
			});
			throw err;
		} finally {
			this.loading = false;
		}
	}

	/**
	 * SPARQLエンドポイントから科目情報を取得して同期
	 */
	async sync() {
		this.loading = true;
		this.syncStatus = m.school_data_status_initializing();
		try {
			// RemoteStorage に接続されている場合、まずそちらからロードを試みる
			if (remoteStorage.isConnected()) {
				this.syncStatus = m.school_data_status_checking_rs();
				try {
					const remoteMeta = await remoteStorage.rsGet<SchoolDataMetadata>('school-data/metadata');
					if (remoteMeta) {
						this.syncStatus = m.school_data_status_downloading_rs();
						const remoteCourses = await remoteStorage.rsGet<PublicCourse[]>('school-data/courses');

						const courses = remoteCourses || [];

						await idbSet(this.COURSE_KEY, courses);
						await idbSet(this.METADATA_KEY, remoteMeta);

						this.metadata = remoteMeta;
						this.courseItems = courses;
						this.syncStatus = m.school_data_status_restored_rs();
						return;
					}
				} catch (err) {
					console.warn('Failed to fetch from RemoteStorage, falling back to SPARQL:', err);
				}
			}

			// RemoteStorageにデータがない場合、または未接続の場合はSPARQLから取得
			this.syncStatus = m.school_data_status_downloading_sparql();
			const courses = await this.fetchCoursesFromLOD();

			this.syncStatus = m.school_data_status_saving_local();
			await idbSet(this.COURSE_KEY, courses);

			const meta: SchoolDataMetadata = {
				lastSyncedAt: Date.now(),
				courseCount: courses.length
			};
			await idbSet(this.METADATA_KEY, meta);

			this.metadata = meta;
			this.courseItems = courses;

			// RemoteStorage への同期はバックグラウンド（非同期）で実行
			if (remoteStorage.isConnected()) {
				this.syncStatus = m.school_data_status_syncing_rs();
				void this.uploadToRS(courses, meta);
			} else {
				this.syncStatus = m.school_data_status_sync_complete();
			}
		} catch (err: any) {
			console.error('Failed to sync school data:', err);
			this.syncStatus = m.school_data_status_sync_failed({ error: err.message || String(err) });
			throw err;
		} finally {
			this.loading = false;
		}
	}

	/**
	 * カスタムのシラバスデータをインポートして上書きする
	 */
	async importCustomCourses(courses: PublicCourse[]) {
		this.loading = true;
		this.syncStatus = m.school_data_status_importing_custom();
		try {
			await idbSet(this.COURSE_KEY, courses);

			const meta: SchoolDataMetadata = {
				lastSyncedAt: Date.now(),
				courseCount: courses.length
			};
			await idbSet(this.METADATA_KEY, meta);

			this.metadata = meta;
			this.courseItems = courses;

			if (remoteStorage.isConnected()) {
				this.syncStatus = m.school_data_status_syncing_rs();
				void this.uploadToRS(courses, meta);
			} else {
				this.syncStatus = m.school_data_status_import_complete();
			}
		} catch (err: any) {
			console.error('Failed to import custom courses:', err);
			this.syncStatus = m.school_data_status_import_failed({ error: err.message || String(err) });
			throw err;
		} finally {
			this.loading = false;
		}
	}

	private async fetchCoursesFromLOD(): Promise<PublicCourse[]> {
		const query = `
			PREFIX schema: <http://schema.org/>
			PREFIX uao: <https://uec-atlas.org/ontology/>
			PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>

			SELECT DISTINCT ?uri ?code ?nameJa ?credits ?semester ?year ?url ?period ?teacherName WHERE {
				?uri a uao:Lecture .
				OPTIONAL { ?uri uao:timeTableCode ?code . }
				OPTIONAL { ?uri schema:name ?nameJa . FILTER(LANG(?nameJa) = "ja") }
				OPTIONAL { ?uri schema:numberOfCredits ?credits . }
				OPTIONAL { ?uri uao:term ?semester . }
				OPTIONAL { ?uri uao:year ?year . }
				OPTIONAL { ?uri schema:url ?url . }
				OPTIONAL { ?uri uao:period ?period . }
				OPTIONAL {
					?uri schema:instructor ?instructorUri .
					?instructorUri rdfs:label ?teacherName .
					FILTER(LANG(?teacherName) = "ja")
				}
			}
		`;

		type RawLecture = {
			uri: string;
			code?: string;
			nameJa?: string;
			credits?: string;
			semester?: string;
			year?: string;
			url?: string;
			period?: string;
			teacherName?: string;
		};

		const rawList = await this.executeSparql<RawLecture>(query);

		const parseDayPeriod = (periodStr: string): { day: DayOfWeek; period: Period } | null => {
			const dayMap: Record<string, DayOfWeek> = {
				月: 'mon',
				火: 'tue',
				水: 'wed',
				木: 'thu',
				金: 'fri',
				土: 'sat',
				日: 'sun'
			};
			const match = periodStr.match(/^([月火水木金土日])([1-7])$/);
			if (match) {
				const d = dayMap[match[1]];
				const p = Number(match[2]) as Period;
				return { day: d, period: p };
			}
			return null;
		};

		const map: Record<string, PublicCourse> = {};
		for (const item of rawList) {
			const uri = item.uri;
			if (!map[uri]) {
				const semester =
					item.semester === '前学期' ? 'spring' : item.semester === '後学期' ? 'fall' : 'other';
				map[uri] = {
					id: uri.split('/').pop() || uri,
					code: item.code || '',
					name: item.nameJa || '',
					teacher: '',
					credits: item.credits ? Number(item.credits) : 2,
					type: 'free',
					semester,
					year: item.year ? Number(item.year) : new Date().getFullYear(),
					dayPeriods: [],
					references: item.url ? [{ title: 'シラバス', url: item.url }] : []
				} as any;
				(map[uri] as any)._teachers = [];
			}

			const course = map[uri];
			if (item.teacherName && !(course as any)._teachers.includes(item.teacherName)) {
				(course as any)._teachers.push(item.teacherName);
			}
			if (item.period) {
				const parsed = parseDayPeriod(item.period);
				if (parsed) {
					const exists = course.dayPeriods?.some(
						(dp) => dp.day === parsed.day && dp.period === parsed.period
					);
					if (!exists) {
						course.dayPeriods?.push(parsed);
					}
				}
			}
		}

		// 最終マッピング（結合された教員名などを反映）
		return Object.values(map).map((course) => {
			course.teacher = (course as any)._teachers.join(', ');
			delete (course as any)._teachers;
			return course;
		});
	}

	private async executeSparql<T = unknown>(query: string): Promise<T[]> {
		const endpoint =
			settingsStore.settings.schoolDataSparqlEndpoint || 'https://uec-atlas.org/sparql';
		const url = `${endpoint}?query=${encodeURIComponent(query)}&output=json`;

		const res = await fetch(url, {
			headers: {
				Accept: 'application/sparql-results+json'
			}
		});
		if (!res.ok) {
			throw new Error(`SPARQL query failed with status ${res.status}`);
		}
		const data = await res.json();

		const bindings = data.results?.bindings || [];
		// eslint-disable-next-line @typescript-eslint/no-explicit-any
		return bindings.map((b: any) => {
			const obj: Record<string, string> = {};
			for (const key of Object.keys(b)) {
				obj[key] = b[key].value;
			}
			return obj;
		}) as T[];
	}
}

export const schoolDataService = new SchoolDataService();
export { SchoolDataService };
