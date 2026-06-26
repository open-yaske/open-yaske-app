/**
 * ユーザーデータの統合ストア。
 * 履修コース / TODO / カレンダーイベント / 出席記録 を保持し、
 * 設定で RemoteStorage が有効なら自動で同期する。
 *
 * 重要: 履修コース ID と公開データ (PublicCourse.id) は独立しており、
 * ユーザーが「履修中」のものは courseId を介して公開データにリンクされる。
 */

import { browser } from '$app/environment';
import * as remoteStorage from '$lib/remotestorage';
import type { Course, Todo, CalendarEvent, AttendanceRecord } from '$lib/types';

import { STORAGE_KEY_PREFIX as GLOBAL_PREFIX } from '$lib/constants';

const STORAGE_KEY_PREFIX = GLOBAL_PREFIX + 'userdata:';

class UserDataStore {
	initialized = $state(false);
	courses = $state<Course[]>([]);
	todos = $state<Todo[]>([]);
	events = $state<CalendarEvent[]>([]);
	attendance = $state<AttendanceRecord[]>([]);
	rsConnected = $state(false);

	async init() {
		if (!browser) return;
		if (this.initialized) return;
		this.initialized = true;

		// 1) ローカルから読み込み
		this.courses = this.loadLocal<Course[]>('courses') ?? [];
		this.todos = this.loadLocal<Todo[]>('todos') ?? [];
		this.events = this.loadLocal<CalendarEvent[]>('events') ?? [];
		this.attendance = this.loadLocal<AttendanceRecord[]>('attendance') ?? [];

		// 2) RemoteStorage 接続状態の追跡
		this.rsConnected = remoteStorage.isConnected();

		window.addEventListener('rs:connected', () => {
			this.rsConnected = true;
			void this.syncFromRemote();
		});
		window.addEventListener('rs:disconnected', () => {
			this.rsConnected = false;
		});

		// リモートの変更を購読
		remoteStorage.rsOnChange((event) => {
			if (event.origin === 'local') return;
			const root = event.path.split('/').find((p) => p !== '') ?? '';
			if (['courses', 'todos', 'events', 'attendance'].includes(root)) {
				void this.syncFromRemote();
			}
		});

		if (this.rsConnected) {
			await this.syncFromRemote();
		}
	}

	private loadLocal<T>(key: string): T | null {
		try {
			const raw = localStorage.getItem(STORAGE_KEY_PREFIX + key);
			return raw ? (JSON.parse(raw) as T) : null;
		} catch {
			return null;
		}
	}

	private saveLocal<T>(key: string, value: T) {
		try {
			localStorage.setItem(STORAGE_KEY_PREFIX + key, JSON.stringify(value));
		} catch (err) {
			console.error('Failed to save user data', key, err);
		}
	}

	private async syncFromRemote() {
		if (!this.rsConnected) return;
		const [courses, todos, events, attendance] = await Promise.all([
			remoteStorage.rsGetAll<Course>('courses'),
			remoteStorage.rsGetAll<Todo>('todos'),
			remoteStorage.rsGetAll<CalendarEvent>('events'),
			remoteStorage.rsGetAll<AttendanceRecord>('attendance')
		]);
		const courseList = Object.values(courses);
		const todoList = Object.values(todos);
		const eventList = Object.values(events);
		const attendanceList = Object.values(attendance);

		// リモートが完全に空で、ローカルにデータが存在する場合、ローカルデータをリモートにアップロードする（初期同期）
		const remoteIsEmpty =
			courseList.length === 0 &&
			todoList.length === 0 &&
			eventList.length === 0 &&
			attendanceList.length === 0;
		const localIsNotEmpty =
			this.courses.length > 0 ||
			this.todos.length > 0 ||
			this.events.length > 0 ||
			this.attendance.length > 0;

		if (remoteIsEmpty && localIsNotEmpty) {
			await Promise.all([
				...this.courses.map((c) => remoteStorage.rsSet(`courses/${c.id}`, c)),
				...this.todos.map((t) => remoteStorage.rsSet(`todos/${t.id}`, t)),
				...this.events.map((e) => remoteStorage.rsSet(`events/${e.id}`, e)),
				...this.attendance.map((r) => remoteStorage.rsSet(`attendance/${r.courseId}/${r.date}`, r))
			]);
			return;
		}

		// リモートにデータがある場合は、ローカルを上書きする
		if (courseList.length) {
			this.courses = courseList;
			this.saveLocal('courses', this.courses);
		}
		if (todoList.length) {
			this.todos = todoList;
			this.saveLocal('todos', this.todos);
		}
		if (eventList.length) {
			this.events = eventList;
			this.saveLocal('events', this.events);
		}
		if (attendanceList.length) {
			this.attendance = attendanceList;
			this.saveLocal('attendance', this.attendance);
		}
	}

	// ----- Courses -----
	async addCourse(c: Omit<Course, 'id' | 'createdAt' | 'updatedAt'>) {
		const id = crypto.randomUUID();
		const now = Date.now();
		const course: Course = { ...c, id, createdAt: now, updatedAt: now };
		this.courses = [...this.courses, course];
		this.saveLocal('courses', this.courses);
		if (this.rsConnected) await remoteStorage.rsSet(`courses/${id}`, course);
	}

	async updateCourse(id: string, patch: Partial<Course>) {
		this.courses = this.courses.map((c) =>
			c.id === id ? { ...c, ...patch, id: c.id, updatedAt: Date.now() } : c
		);
		this.saveLocal('courses', this.courses);
		const updated = this.courses.find((c) => c.id === id);
		if (updated && this.rsConnected) await remoteStorage.rsSet(`courses/${id}`, updated);
	}

	async removeCourse(id: string) {
		this.courses = this.courses.filter((c) => c.id !== id);
		this.saveLocal('courses', this.courses);
		if (this.rsConnected) await remoteStorage.rsDelete(`courses/${id}`);
	}

	// ----- Todos -----
	async addTodo(t: Omit<Todo, 'id' | 'createdAt' | 'updatedAt'>) {
		const id = crypto.randomUUID();
		const now = Date.now();
		const todo: Todo = { ...t, id, createdAt: now, updatedAt: now };
		this.todos = [...this.todos, todo];
		this.saveLocal('todos', this.todos);
		if (this.rsConnected) await remoteStorage.rsSet(`todos/${id}`, todo);
	}

	async updateTodo(id: string, patch: Partial<Todo>) {
		this.todos = this.todos.map((t) =>
			t.id === id ? { ...t, ...patch, id: t.id, updatedAt: Date.now() } : t
		);
		this.saveLocal('todos', this.todos);
		const updated = this.todos.find((t) => t.id === id);
		if (updated && this.rsConnected) await remoteStorage.rsSet(`todos/${id}`, updated);
	}

	async removeTodo(id: string) {
		this.todos = this.todos.filter((t) => t.id !== id);
		this.saveLocal('todos', this.todos);
		if (this.rsConnected) await remoteStorage.rsDelete(`todos/${id}`);
	}

	// ----- Events -----
	async addEvent(e: Omit<CalendarEvent, 'id' | 'createdAt' | 'updatedAt'>) {
		const id = crypto.randomUUID();
		const now = Date.now();
		const ev: CalendarEvent = { ...e, id, createdAt: now, updatedAt: now };
		this.events = [...this.events, ev];
		this.saveLocal('events', this.events);
		if (this.rsConnected) await remoteStorage.rsSet(`events/${id}`, ev);
	}

	async updateEvent(id: string, patch: Partial<CalendarEvent>) {
		this.events = this.events.map((e) =>
			e.id === id ? { ...e, ...patch, id: e.id, updatedAt: Date.now() } : e
		);
		this.saveLocal('events', this.events);
		const updated = this.events.find((e) => e.id === id);
		if (updated && this.rsConnected) await remoteStorage.rsSet(`events/${id}`, updated);
	}

	async removeEvent(id: string) {
		this.events = this.events.filter((e) => e.id !== id);
		this.saveLocal('events', this.events);
		if (this.rsConnected) await remoteStorage.rsDelete(`events/${id}`);
	}

	// ----- Attendance -----
	async setAttendance(record: AttendanceRecord) {
		const existingIndex = this.attendance.findIndex(
			(r) => r.courseId === record.courseId && r.date === record.date
		);
		if (existingIndex >= 0) {
			this.attendance = this.attendance.map((r, i) => (i === existingIndex ? record : r));
		} else {
			this.attendance = [...this.attendance, record];
		}
		this.saveLocal('attendance', this.attendance);
		if (this.rsConnected)
			await remoteStorage.rsSet(`attendance/${record.courseId}/${record.date}`, record);
	}

	async removeAttendance(courseId: string, date: string) {
		this.attendance = this.attendance.filter((r) => !(r.courseId === courseId && r.date === date));
		this.saveLocal('attendance', this.attendance);
		if (this.rsConnected) await remoteStorage.rsDelete(`attendance/${courseId}/${date}`);
	}

	// ----- Bulk data management -----
	exportUserData() {
		return {
			courses: this.courses,
			todos: this.todos,
			events: this.events,
			attendance: this.attendance
		};
	}

	async replaceUserData(data: {
		courses?: Course[];
		todos?: Todo[];
		events?: CalendarEvent[];
		attendance?: AttendanceRecord[];
	}) {
		const oldCourses = this.courses;
		const oldTodos = this.todos;
		const oldEvents = this.events;
		const oldAttendance = this.attendance;

		this.courses = Array.isArray(data.courses) ? data.courses : [];
		this.todos = Array.isArray(data.todos) ? data.todos : [];
		this.events = Array.isArray(data.events) ? data.events : [];
		this.attendance = Array.isArray(data.attendance) ? data.attendance : [];

		this.saveLocal('courses', this.courses);
		this.saveLocal('todos', this.todos);
		this.saveLocal('events', this.events);
		this.saveLocal('attendance', this.attendance);

		if (!this.rsConnected) return;
		await Promise.all([
			...oldCourses.map((c) => remoteStorage.rsDelete(`courses/${c.id}`)),
			...oldTodos.map((t) => remoteStorage.rsDelete(`todos/${t.id}`)),
			...oldEvents.map((e) => remoteStorage.rsDelete(`events/${e.id}`)),
			...oldAttendance.map((r) => remoteStorage.rsDelete(`attendance/${r.courseId}/${r.date}`))
		]);
		await Promise.all([
			...this.courses.map((c) => remoteStorage.rsSet(`courses/${c.id}`, c)),
			...this.todos.map((t) => remoteStorage.rsSet(`todos/${t.id}`, t)),
			...this.events.map((e) => remoteStorage.rsSet(`events/${e.id}`, e)),
			...this.attendance.map((r) => remoteStorage.rsSet(`attendance/${r.courseId}/${r.date}`, r))
		]);
	}

	async clearUserData() {
		await this.replaceUserData({ courses: [], todos: [], events: [], attendance: [] });
		if (browser) {
			localStorage.removeItem(STORAGE_KEY_PREFIX + 'courses');
			localStorage.removeItem(STORAGE_KEY_PREFIX + 'todos');
			localStorage.removeItem(STORAGE_KEY_PREFIX + 'events');
			localStorage.removeItem(STORAGE_KEY_PREFIX + 'attendance');
		}
	}
}

export const userDataStore = new UserDataStore();
