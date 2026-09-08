/**
 * GPA 計算の純粋ロジック。+page.svelte の $derived と同一式を切り出したもの。
 * 科目連携（履修データからの初期行生成）の土台にする。
 */

export type GpaInputRow = { credits: number; point: number };

export function calcGpa(rows: GpaInputRow[]): {
	totalCredits: number;
	totalPoints: number;
	gpa: number;
} {
	const totalCredits = rows.reduce((sum, r) => sum + Math.max(0, Number(r.credits) || 0), 0);
	const totalPoints = rows.reduce((sum, r) => {
		const c = Math.max(0, Number(r.credits) || 0);
		const p = Math.max(0, Number(r.point) || 0);
		return sum + c * p;
	}, 0);
	return { totalCredits, totalPoints, gpa: totalCredits === 0 ? 0 : totalPoints / totalCredits };
}
