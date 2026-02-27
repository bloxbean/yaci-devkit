import { expect, test } from '@playwright/test';

test('index page has expected heading', async ({ page }) => {
	await page.goto('/');
	const heading = await page.textContent('h1');
	expect(heading).toBe('Blockchain DataViewer');
});

test('navigation is visible', async ({ page }) => {
	await page.goto('/');
	const nav = page.locator('nav');
	await expect(nav).toBeVisible();
});

test('blocks page loads', async ({ page }) => {
	await page.goto('/blocks');
	await expect(page).toHaveURL(/.*blocks/);
});

test('transactions page loads', async ({ page }) => {
	await page.goto('/transactions');
	await expect(page).toHaveURL(/.*transactions/);
});
