import { expect, test } from '@playwright/test';

/*
 * Script Lab (julc engine) on a script page. Needs a Plutus script on the configured store:
 *
 *   SCRIPT_LAB_HASH=<56-hex script hash> PW_CHANNEL=chrome npx playwright test script-lab
 *
 * The julc engine needs WebAssembly GC and exnref, which Playwright's bundled Chromium 128 lacks, so use a current
 * browser channel (PW_CHANNEL=chrome). Without support the test checks the "can't run" message instead.
 */
const hash = process.env.SCRIPT_LAB_HASH;

test.skip(!hash, 'Set SCRIPT_LAB_HASH to a Plutus script hash on the configured store');

const supported = () =>
	WebAssembly.validate(new Uint8Array([0, 97, 115, 109, 1, 0, 0, 0, 1, 5, 1, 95, 1, 120, 0])) &&
	WebAssembly.validate(new Uint8Array([0, 97, 115, 109, 1, 0, 0, 0, 1, 4, 1, 96, 0, 0, 3, 2, 1, 0, 10, 8, 1, 6, 0, 31, 64, 0, 11, 11]));

test('decodes a script into searchable UPLC and verifies its hash', async ({ page }) => {
	await page.goto(`/scripts/${hash}`);
	if (!(await page.evaluate(supported))) {
		await expect(page.getByText('This browser can’t run the julc engine')).toBeVisible();
		return;
	}
	await page.getByRole('button', { name: 'Decode script' }).click();
	const code = page.getByRole('region', { name: 'UPLC' });
	await expect(code).toBeVisible({ timeout: 60_000 });
	await expect(page.getByText('Verified')).toBeVisible();
	await expect(page.getByText(/Builtins used \(\d+\)/)).toBeVisible();
	await expect(code.getByText('(program', { exact: false }).first()).toBeVisible();

	// Search: typing finds matches, Enter moves to the next one.
	await page.getByPlaceholder('Search uplc').fill('builtin');
	await expect(page.getByText(/^1 \/ \d+\+?$/)).toBeVisible();
	await page.keyboard.press('Enter');
	await expect(page.getByText(/^2 \/ \d+\+?$/)).toBeVisible();

	// Only the lines on screen are rendered.
	expect(await code.locator('.sticky').count()).toBeLessThan(200);

	await page.getByRole('tab', { name: 'CBOR' }).click();
	await expect(page).toHaveURL(/view=cbor/);
});
