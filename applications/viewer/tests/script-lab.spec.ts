import { expect, test } from '@playwright/test';

/*
 * Script Lab (julc engine). Needs data on the configured store:
 *
 *   SCRIPT_LAB_HASH=<56-hex Plutus script hash> SCRIPT_LAB_TX=<tx hash with Plutus redeemers> \
 *   SCRIPT_LAB_RUN=<purpose/index of one of its runs, default spend/0> PW_CHANNEL=chrome npx playwright test script-lab
 *
 * The julc engine needs WebAssembly GC and exnref, which Playwright's bundled Chromium 128 lacks, so use a current
 * browser channel (PW_CHANNEL=chrome). Without support the test checks the "can't run" message instead.
 */
const hash = process.env.SCRIPT_LAB_HASH;
const txHash = process.env.SCRIPT_LAB_TX;
const run = process.env.SCRIPT_LAB_RUN ?? 'spend/0';

const supported = () =>
	WebAssembly.validate(new Uint8Array([0, 97, 115, 109, 1, 0, 0, 0, 1, 5, 1, 95, 1, 120, 0])) &&
	WebAssembly.validate(new Uint8Array([0, 97, 115, 109, 1, 0, 0, 0, 1, 4, 1, 96, 0, 0, 3, 2, 1, 0, 10, 8, 1, 6, 0, 31, 64, 0, 11, 11]));

test('decodes a script into searchable UPLC and verifies its hash', async ({ page }) => {
	test.skip(!hash, 'Set SCRIPT_LAB_HASH to a Plutus script hash on the configured store');
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

test('re-runs every script of a transaction and compares with the declared budget', async ({ page }) => {
	test.skip(!txHash, 'Set SCRIPT_LAB_TX to a transaction with Plutus redeemers on the configured store');
	await page.goto(`/transactions/${txHash}?tab=scripts`);
	if (!(await page.evaluate(supported))) {
		await expect(page.getByText('This browser can’t run the julc engine')).toBeVisible();
		return;
	}
	const cards = page.locator('article');
	const count = await cards.count();
	await page.getByRole('button', { name: /^Re-run/ }).click();
	await expect(page.getByRole('button', { name: 'Run again' })).toBeVisible({ timeout: 90_000 });

	// Every script got a result, with julc's budget next to the declared one.
	await expect(page.getByText(/julc re-run/i)).toHaveCount(count);
	await expect(page.getByText(/declared$/).first()).toBeVisible();
	await expect(page.getByText(/reproduced exactly|reproduced with|passed|failed|couldn’t run/i).first()).toBeVisible();

	// What the validator saw: the ScriptContext julc built, decoded.
	await page.getByText('What the validator saw (ScriptContext)').first().click();
	await expect(page.getByText(/ScriptContext · /).first()).toBeVisible();
});

test('steps through a script run, with breakpoints and step links', async ({ page }) => {
	test.skip(!txHash, 'Set SCRIPT_LAB_TX (and SCRIPT_LAB_RUN) to a run on the configured store');
	await page.goto(`/transactions/${txHash}/debug/${run}`);
	if (!(await page.evaluate(supported))) {
		await expect(page.getByText(/can’t run the julc engine/)).toBeVisible();
		return;
	}
	const stepButton = page.getByRole('button', { name: 'Step', exact: true });
	await expect(stepButton).toBeVisible({ timeout: 90_000 });
	const position = page.getByText(/^Step [\d,]+ \/ [\d,]+$/);
	await expect(position).toHaveText(/^Step 0 \//);

	await page.keyboard.press('ArrowRight');
	await page.keyboard.press('ArrowRight');
	await expect(position).toHaveText(/^Step 2 \//);
	await expect(page).toHaveURL(/step=2/);

	await page.keyboard.press('End');
	await expect(page.getByText(/The script finished successfully|The script failed|Error/).first()).toBeVisible();

	// Break on the first builtin the script uses, then continue from the start.
	await page.keyboard.press('Home');
	const builtin = page.getByRole('button', { pressed: false }).filter({ hasText: /^[a-z][A-Za-z0-9_]+$/ }).first();
	await builtin.click();
	await page.keyboard.press('c');
	await expect(page.getByText('builtin', { exact: true }).first()).toBeVisible();

	// A step link opens that exact step.
	await page.goto(`/transactions/${txHash}/debug/${run}?step=5`);
	await expect(page.getByText(/^Step 5 \//)).toBeVisible({ timeout: 90_000 });
});
