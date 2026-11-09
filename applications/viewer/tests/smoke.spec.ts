import { expect, test, type Page } from '@playwright/test';

/** Wait until the app has hydrated, so keyboard shortcuts are wired up. */
const ready = (page: Page) => page.waitForSelector('html[data-ready]');

/*
 * Smoke tests against a running Yaci Store (PUBLIC_INDEXER_BASE_URL, default http://localhost:8080/api/v1),
 * e.g. a Yaci DevKit devnet started with Yaci Store enabled.
 */

test('dashboard shows the chain tip and live blocks', async ({ page }) => {
	await page.goto('/');
	await expect(page.getByRole('heading', { level: 1 })).toContainText('Chain tip');
	await expect(page.getByText('Live blocks')).toBeVisible();
	await expect(page.locator('a[href^="/blocks/"]').first()).toBeVisible();
});

test('search jumps to a block', async ({ page }) => {
	await page.goto('/');
	await ready(page);
	await page.keyboard.press('/');
	const box = page.getByRole('dialog', { name: 'Search' }).getByRole('textbox');
	await expect(box).toBeFocused();
	await box.fill('1');
	await expect(page.getByRole('dialog').getByText('Block', { exact: true })).toBeVisible();
	await page.keyboard.press('Enter');
	await expect(page).toHaveURL(/\/blocks\/1$/);
	await expect(page.getByRole('heading', { level: 1 })).toHaveText('#1');
});

test('block page steps to the previous block with the arrow key', async ({ page }) => {
	await page.goto('/blocks/2');
	await ready(page);
	await page.keyboard.press('ArrowLeft');
	await expect(page).toHaveURL(/\/blocks\/1$/);
});

test('unknown transaction shows the not-found page', async ({ page }) => {
	const res = await page.goto(`/transactions/${'0'.repeat(64)}`);
	expect(res?.status()).toBe(404);
	await expect(page.getByText("We couldn't find that")).toBeVisible();
});

for (const path of [
	'/blocks',
	'/transactions',
	'/addresses',
	'/assets',
	'/epochs',
	'/pools/search',
	'/rewards',
	'/epoch-stakes',
	'/withdrawals',
	'/adapot/current',
	'/certificates/stakekey-registrations',
	'/governance',
	'/governance/proposals',
	'/governance/dreps',
	'/governance/committee'
]) {
	test(`${path} renders without errors`, async ({ page }) => {
		const errors: string[] = [];
		page.on('pageerror', (e) => errors.push(e.message));
		const res = await page.goto(path);
		expect(res?.status()).toBeLessThan(400);
		await expect(page.getByRole('heading', { level: 1 })).toBeVisible();
		expect(errors).toEqual([]);
	});
}
