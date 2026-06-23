/**
 * jsPDF ラッパー: 時間割 / シラバス / TODO を PDF に出力
 */

import { jsPDF } from 'jspdf';
import type { Course, Todo } from './types';
import type { ThemePack } from './theme';

const DAY_LABEL: Record<string, string> = {
	mon: 'Mon',
	tue: 'Tue',
	wed: 'Wed',
	thu: 'Thu',
	fri: 'Fri',
	sat: 'Sat',
	sun: 'Sun'
};

/**
 * 時間割を PDF に出力
 */
export function exportTimetablePdf(courses: Course[], pack: ThemePack, filename = 'timetable.pdf') {
	const doc = new jsPDF({ orientation: 'landscape', unit: 'mm', format: 'a4' });
	const primary = pack.ui.primary[500];
	// Convert #rrggbb to [r, g, b]
	const r = parseInt(primary.slice(1, 3), 16);
	const g = parseInt(primary.slice(3, 5), 16);
	const b = parseInt(primary.slice(5, 7), 16);

	// Title
	doc.setFontSize(18);
	doc.setTextColor(r, g, b);
	doc.text('Timetable', 14, 18);

	// Header row
	doc.setFontSize(10);
	doc.setTextColor(60);
	const days = ['mon', 'tue', 'wed', 'thu', 'fri'];
	const colWidth = 50;
	const rowHeight = 14;
	const startX = 30;
	const startY = 28;

	days.forEach((d, i) => {
		doc.text(DAY_LABEL[d] ?? d, startX + i * colWidth, startY);
	});

	// Grid lines + courses
	for (let p = 1; p <= 7; p++) {
		doc.text(`Period ${p}`, 8, startY + p * rowHeight);
		doc.line(28, startY + p * rowHeight - 4, 280, startY + p * rowHeight - 4);
		days.forEach((d, i) => {
			const c = courses.find((c) => c.day === d && c.period === p);
			if (c) {
				doc.setFontSize(9);
				doc.setTextColor(20);
				doc.text(c.name.slice(0, 20), startX + i * colWidth, startY + p * rowHeight);
				if (c.teacher) {
					doc.setFontSize(7);
					doc.setTextColor(120);
					doc.text(c.teacher.slice(0, 20), startX + i * colWidth, startY + p * rowHeight + 4);
				}
			}
		});
	}

	doc.save(filename);
}

/**
 * TODO リストを PDF に出力
 */
export function exportTodosPdf(todos: Todo[], filename = 'todos.pdf') {
	const doc = new jsPDF({ unit: 'mm', format: 'a4' });
	doc.setFontSize(18);
	doc.text('TODO List', 14, 18);

	doc.setFontSize(10);
	let y = 28;
	todos.forEach((t) => {
		const box = t.done ? '[x]' : '[ ]';
		const line =
			`${box} ${t.name}` + (t.dueAt ? `  (due: ${new Date(t.dueAt).toLocaleDateString()})` : '');
		if (y > 280) {
			doc.addPage();
			y = 18;
		}
		doc.text(line, 14, y);
		y += 6;
	});

	doc.save(filename);
}
