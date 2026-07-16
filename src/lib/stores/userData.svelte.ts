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
import { syncWidgetData } from '$lib/widget';

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
		void syncWidgetData();
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

		// 1) Courses のマージ
		const mergedCourses = [...this.courses];
		let coursesChanged = false;
		for (const remoteCourse of courseList) {
			const localIndex = mergedCourses.findIndex((c) => c.id === remoteCourse.id);
			if (localIndex >= 0) {
				const localCourse = mergedCourses[localIndex];
				if (remoteCourse.updatedAt > localCourse.updatedAt) {
					mergedCourses[localIndex] = remoteCourse;
					coursesChanged = true;
				} else if (localCourse.updatedAt > remoteCourse.updatedAt) {
					void remoteStorage.rsSet(`courses/${localCourse.id}`, localCourse);
				}
			} else {
				mergedCourses.push(remoteCourse);
				coursesChanged = true;
			}
		}
		for (const localCourse of this.courses) {
			if (!courseList.some((c) => c.id === localCourse.id)) {
				void remoteStorage.rsSet(`courses/${localCourse.id}`, localCourse);
			}
		}
		if (coursesChanged || this.courses.length !== mergedCourses.length) {
			this.courses = mergedCourses;
			this.saveLocal('courses', this.courses);
			void syncWidgetData();
		}

		// 2) Todos のマージ
		const mergedTodos = [...this.todos];
		let todosChanged = false;
		for (const remoteTodo of todoList) {
			const localIndex = mergedTodos.findIndex((t) => t.id === remoteTodo.id);
			if (localIndex >= 0) {
				const localTodo = mergedTodos[localIndex];
				if (remoteTodo.updatedAt > localTodo.updatedAt) {
					mergedTodos[localIndex] = remoteTodo;
					todosChanged = true;
				} else if (localTodo.updatedAt > remoteTodo.updatedAt) {
					void remoteStorage.rsSet(`todos/${localTodo.id}`, localTodo);
				}
			} else {
				mergedTodos.push(remoteTodo);
				todosChanged = true;
			}
		}
		for (const localTodo of this.todos) {
			if (!todoList.some((t) => t.id === localTodo.id)) {
				void remoteStorage.rsSet(`todos/${localTodo.id}`, localTodo);
			}
		}
		if (todosChanged || this.todos.length !== mergedTodos.length) {
			this.todos = mergedTodos;
			this.saveLocal('todos', this.todos);
		}

		// 3) Events のマージ
		const mergedEvents = [...this.events];
		let eventsChanged = false;
		for (const remoteEvent of eventList) {
			const localIndex = mergedEvents.findIndex((e) => e.id === remoteEvent.id);
			if (localIndex >= 0) {
				const localEvent = mergedEvents[localIndex];
				if (remoteEvent.updatedAt > localEvent.updatedAt) {
					mergedEvents[localIndex] = remoteEvent;
					eventsChanged = true;
				} else if (localEvent.updatedAt > remoteEvent.updatedAt) {
					void remoteStorage.rsSet(`events/${localEvent.id}`, localEvent);
				}
			} else {
				mergedEvents.push(remoteEvent);
				eventsChanged = true;
			}
		}
		for (const localEvent of this.events) {
			if (!eventList.some((e) => e.id === localEvent.id)) {
				void remoteStorage.rsSet(`events/${localEvent.id}`, localEvent);
			}
		}
		if (eventsChanged || this.events.length !== mergedEvents.length) {
			this.events = mergedEvents;
			this.saveLocal('events', this.events);
		}

		// 4) Attendance のマージ
		const mergedAttendance = [...this.attendance];
		let attendanceChanged = false;
		for (const remoteAttr of attendanceList) {
			const localIndex = mergedAttendance.findIndex(
				(r) => r.courseId === remoteAttr.courseId && r.date === remoteAttr.date
			);
			if (localIndex >= 0) {
				const localAttr = mergedAttendance[localIndex];
				if (remoteAttr.createdAt > localAttr.createdAt) {
					mergedAttendance[localIndex] = remoteAttr;
					attendanceChanged = true;
				} else if (localAttr.createdAt > remoteAttr.createdAt) {
					void remoteStorage.rsSet(`attendance/${localAttr.courseId}/${localAttr.date}`, localAttr);
				}
			} else {
				mergedAttendance.push(remoteAttr);
				attendanceChanged = true;
			}
		}
		for (const localAttr of this.attendance) {
			if (
				!attendanceList.some((r) => r.courseId === localAttr.courseId && r.date === localAttr.date)
			) {
				void remoteStorage.rsSet(`attendance/${localAttr.courseId}/${localAttr.date}`, localAttr);
			}
		}
		if (attendanceChanged || this.attendance.length !== mergedAttendance.length) {
			this.attendance = mergedAttendance;
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
		void syncWidgetData();
	}

	async updateCourse(id: string, patch: Partial<Course>) {
		this.courses = this.courses.map((c) =>
			c.id === id ? { ...c, ...patch, id: c.id, updatedAt: Date.now() } : c
		);
		this.saveLocal('courses', this.courses);
		const updated = this.courses.find((c) => c.id === id);
		if (updated && this.rsConnected) await remoteStorage.rsSet(`courses/${id}`, updated);
		void syncWidgetData();
	}

	async removeCourse(id: string) {
		this.courses = this.courses.filter((c) => c.id !== id);
		this.saveLocal('courses', this.courses);
		if (this.rsConnected) await remoteStorage.rsDelete(`courses/${id}`);
		void syncWidgetData();
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
		void syncWidgetData();
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
