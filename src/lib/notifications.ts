import { LocalNotifications } from '@capacitor/local-notifications';
import { Capacitor } from '@capacitor/core';
import type { Course, DayOfWeek, UserSettings } from './types';

const dayMap: Record<DayOfWeek, number> = {
	sun: 1,
	mon: 2,
	tue: 3,
	wed: 4,
	thu: 5,
	fri: 6,
	sat: 7
};

async function checkPermission(): Promise<boolean> {
	let status = await LocalNotifications.checkPermissions();
	if (status.display !== 'granted') {
		status = await LocalNotifications.requestPermissions();
	}
	return status.display === 'granted';
}

export async function syncNotifications(courses: Course[], settings: UserSettings): Promise<void> {
	if (!Capacitor.isNativePlatform()) {
		return;
	}

	try {
		// 1) 既存のすべてのローカル通知をキャンセル
		const pending = await LocalNotifications.getPending();
		if (pending.notifications.length > 0) {
			await LocalNotifications.cancel({ notifications: pending.notifications });
		}

		// 通知が無効な場合はクリアして終了
		if (!settings.attendanceNotification.enabled) {
			return;
		}

		// 2) 権限確認
		const hasPermission = await checkPermission();
		if (!hasPermission) {
			console.warn('LocalNotifications permission was not granted.');
			return;
		}

		const notificationsToSchedule = [];
		let idCounter = 1;

		for (const course of courses) {
			const periodTime = settings.timetablePeriods[course.period];
			if (!periodTime) continue;

			const weekday = dayMap[course.day];

			// A) 開始前通知の作成
			if (
				settings.attendanceNotification.trigger === 'start' ||
				settings.attendanceNotification.trigger === 'both'
			) {
				const [h, min] = periodTime.start.split(':').map(Number);
				if (!isNaN(h) && !isNaN(min)) {
					const date = new Date();
					date.setHours(h, min, 0, 0);
					date.setMinutes(date.getMinutes() - settings.attendanceNotification.startOffsetMinutes);

					notificationsToSchedule.push({
						id: idCounter++,
						title: `授業開始前: ${course.name}`,
						body: `まもなく授業が始まります。出席状況を登録しましょう！`,
						schedule: {
							on: {
								weekday,
								hour: date.getHours(),
								minute: date.getMinutes()
							},
							repeats: true,
							allowWhileIdle: true
						},
						extra: {
							courseId: course.id,
							type: 'attendance_start'
						}
					});
				}
			}

			// B) 終了時通知の作成
			if (
				settings.attendanceNotification.trigger === 'end' ||
				settings.attendanceNotification.trigger === 'both'
			) {
				const [h, min] = periodTime.end.split(':').map(Number);
				if (!isNaN(h) && !isNaN(min)) {
					const date = new Date();
					date.setHours(h, min, 0, 0);
					date.setMinutes(date.getMinutes() + settings.attendanceNotification.endOffsetMinutes);

					notificationsToSchedule.push({
						id: idCounter++,
						title: `授業終了時: ${course.name}`,
						body: `授業が終了しました。出席状況を登録しましょう！`,
						schedule: {
							on: {
								weekday,
								hour: date.getHours(),
								minute: date.getMinutes()
							},
							repeats: true,
							allowWhileIdle: true
						},
						extra: {
							courseId: course.id,
							type: 'attendance_end'
						}
					});
				}
			}
		}

		if (notificationsToSchedule.length > 0) {
			await LocalNotifications.schedule({
				notifications: notificationsToSchedule
			});
		}
	} catch (err) {
		console.error('Failed to sync local notifications:', err);
	}
}
