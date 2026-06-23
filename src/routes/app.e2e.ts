import { expect, test } from '@playwright/test';

test.describe('open-nexus E2E Suite', () => {
	test('Verify 10 major app scenarios', async ({ page }) => {
		// --- Helper function for responsive navigation ---
		async function navigateTo(tabName: string) {
			const locator = page
				.locator(`aside:visible >> text=${tabName}`)
				.or(page.locator(`nav:visible >> text=${tabName}`))
				.first();
			await expect(locator).toBeVisible();
			await locator.click();
		}

		// --- Scenario 1: Verify Initial Home Page ---
		await page.goto('/');
		await page.waitForLoadState('domcontentloaded');
		await expect(page).toHaveTitle(/ホーム | open-nexus/);

		// Verify navigation tabs are visible (either sidebar or bottom navigation)
		const timetableNav = page
			.locator('aside:visible >> text=時間割')
			.or(page.locator('nav:visible >> text=時間割'))
			.first();
		const todoNav = page
			.locator('aside:visible >> text=TODO')
			.or(page.locator('nav:visible >> text=TODO'))
			.first();
		const calendarNav = page
			.locator('aside:visible >> text=カレンダー')
			.or(page.locator('nav:visible >> text=カレンダー'))
			.first();
		const attendanceNav = page
			.locator('aside:visible >> text=出席')
			.or(page.locator('nav:visible >> text=出席'))
			.first();

		await expect(timetableNav).toBeVisible();
		await expect(todoNav).toBeVisible();
		await expect(calendarNav).toBeVisible();
		await expect(attendanceNav).toBeVisible();

		// --- Scenario 2: Timetable Page - Empty State ---
		await navigateTo('時間割');
		await expect(page).toHaveTitle(/時間割 | open-nexus/);
		await expect(page.locator('text=科目が登録されていません')).toBeVisible();

		// --- Scenario 3: Timetable Page - Add Course ---
		await page.click('button:has-text("科目を追加")');
		await page.fill('label:has-text("科目名") + div input', 'ソフトウェア工学');
		await page.fill('label:has-text("担当") + div input', '山田太郎');
		await page.fill('label:has-text("教室") + div input', '東1号館101');
		await page.selectOption('label:has-text("曜日") + div select', 'mon');
		await page.selectOption('label:has-text("時限") + div select', '1');
		await page.selectOption('label:has-text("区分") + div select', 'required');
		await page.selectOption('label:has-text("単位") + div select', '2');
		await page.click('button:has-text("保存")');

		// Verify added course in WeekGrid
		await expect(page.locator('text=ソフトウェア工学')).toBeVisible();
		await expect(page.locator('text=東1号館101')).toBeVisible();

		// --- Scenario 4: Timetable Page - Syllabus details ---
		await page.click('text=ソフトウェア工学');
		await expect(page.locator('text=シラバス')).toBeVisible();
		await expect(page.locator('text=山田太郎')).toBeVisible();
		await expect(page.locator('text=必修')).toBeVisible();
		await page.click('button:has-text("閉じる")');

		// --- Scenario 5: TODO Page - Add Task ---
		await navigateTo('TODO');
		await expect(page).toHaveTitle(/TODO | open-nexus/);
		// Click FAB (aria-label="TODO を追加")
		await page.click('button[aria-label="TODO を追加"]');
		await page.fill('label:has-text("内容") + div input', 'レポート提出');
		await page.selectOption('label:has-text("優先度") + div select', 'high');
		await page.click('button:has-text("保存")');
		await expect(page.locator('text=レポート提出')).toBeVisible();

		// --- Scenario 6: TODO Page - Complete Task ---
		// Click the circle checkbox next to the todo item
		await page.click('button[role="checkbox"]');
		// The todo should have line-through text style
		await expect(page.locator('p:has-text("レポート提出")')).toHaveClass(/line-through/);

		// --- Scenario 7: Calendar Page - Switch Views ---
		await navigateTo('カレンダー');
		await expect(page).toHaveTitle(/カレンダー | open-nexus/);
		// Switch to week view
		await page.click('button:has-text("週")');
		await expect(page.locator('text=終日')).toBeVisible();
		// Switch to day view
		await page.click('button:has-text("日")');
		// Switch back to month view
		await page.click('button:has-text("月")');

		// --- Scenario 8: Attendance Page - Verify denominator exclusion of cancelled classes ---
		await navigateTo('出席');
		await expect(page).toHaveTitle(/出席 | open-nexus/);
		await expect(page.locator('text=ソフトウェア工学')).toBeVisible();
		// Verify initial attendance rate is 0% (checking with regex due to space representation in HTML)
		await expect(page.locator('text=/0\\s*%/')).toBeVisible();

		// Mark present
		await page.click('button[aria-label="出席"]');
		await expect(page.locator('text=/100\\s*%/')).toBeVisible();

		// Mark absent (overwrites today's status to absent, so rate becomes 0%)
		await page.click('button[aria-label="欠席"]');
		await expect(page.locator('text=/0\\s*%/')).toBeVisible();
		await expect(page.locator('text=4集計')).toBeVisible(); // remaining absences: 5 - 1 = 4. Localization uses "集計" for count.

		// Mark cancelled (overwrites today's status to cancelled, which is excluded from denominator, rate is 0%)
		await page.click('button[aria-label="休講"]');
		await expect(page.locator('text=/0\\s*%/')).toBeVisible();

		// --- Scenario 9: Settings Page - Switch theme pack ---
		await navigateTo('その他');
		await expect(page).toHaveTitle(/その他 | open-nexus/);
		await page.click('a:has-text("設定")');
		await expect(page).toHaveTitle(/設定 | open-nexus/);

		// Change theme pack to "uec"
		await page.selectOption('#settings-theme select', 'uec');

		// --- Scenario 10: Settings Page - Switch locale ---
		// Change language to English
		await page.click('button:has-text("English")');
		// Navigation tabs should be translated (either sidebar or bottom navigation)
		const timetableNavEn = page
			.locator('aside:visible >> text=Timetable')
			.or(page.locator('nav:visible >> text=Timetable'))
			.first();
		const calendarNavEn = page
			.locator('aside:visible >> text=Calendar')
			.or(page.locator('nav:visible >> text=Calendar'))
			.first();
		await expect(timetableNavEn).toBeVisible();
		await expect(calendarNavEn).toBeVisible();

		// Switch back to Japanese
		await page.click('button:has-text("日本語")');
		const timetableNavJa = page
			.locator('aside:visible >> text=時間割')
			.or(page.locator('nav:visible >> text=時間割'))
			.first();
		await expect(timetableNavJa).toBeVisible();
	});
});
