import { test, expect, type Page } from '@playwright/test';

const CLI_URL = 'http://localhost:10000';

// Helper: wait for wallet SDK to initialize and wallet grid to render
async function waitForWalletReady(page: Page) {
  // Wait for the SDK to inject window.cardano.yacidevkit
  await page.waitForFunction(() => {
    return window['cardano'] && window['cardano']['yacidevkit'];
  }, { timeout: 10000 });

  // Wait for the wallet grid to re-render after the yacidevkit:ready event
  await page.waitForSelector('[data-wallet-id="yacidevkit"]', { timeout: 10000 });
}

test.describe('CIP-30 Wallet Demo - Page Load', () => {
  test('page loads with title and notes', async ({ page }) => {
    await page.goto('/cip30-wallets.html');

    await expect(page.locator('h1')).toContainText('CIP-30 Wallet Connection Demo');
    await expect(page.locator('text=About CIP-30')).toBeVisible();
    await expect(page.locator('text=Yaci DevKit:').first()).toBeVisible();
  });

  test('detects Yaci DevKit wallet when CLI is running', async ({ page }) => {
    await page.goto('/cip30-wallets.html');
    await waitForWalletReady(page);

    // Wait for wallet grid to render
    await page.waitForSelector('[data-wallet-id="yacidevkit"]', { timeout: 10000 });

    const walletBtn = page.locator('[data-wallet-id="yacidevkit"]');
    await expect(walletBtn).toBeVisible();
    await expect(walletBtn.locator('.name')).toContainText('Yaci DevKit');
    await expect(walletBtn.locator('.badge')).toContainText('Local Dev');
  });
});

test.describe('CIP-30 Wallet Demo - Connection', () => {
  test('connects to Yaci DevKit wallet and shows info', async ({ page }) => {
    await page.goto('/cip30-wallets.html');
    await waitForWalletReady(page);

    // Click to connect
    await page.click('[data-wallet-id="yacidevkit"]');

    // Wait for connection - wallet info section should appear
    await expect(page.locator('#wallet-info')).toBeVisible({ timeout: 10000 });

    // Check wallet name
    await expect(page.locator('#wallet-name')).toContainText('Yaci DevKit');

    // Check network shows testnet
    await expect(page.locator('#network-id')).toContainText('Testnet');

    // Check balance is displayed (should have ADA from prefunded accounts)
    await expect(page.locator('#balance')).toContainText('ADA');

    // Check API demo section is visible
    await expect(page.locator('#api-demo')).toBeVisible();

    // Check transfer section is visible
    await expect(page.locator('#transfer-section')).toBeVisible();

    // Check raw section is visible
    await expect(page.locator('#raw-section')).toBeVisible();
  });

  test('button shows connected state after connecting', async ({ page }) => {
    await page.goto('/cip30-wallets.html');
    await waitForWalletReady(page);

    await page.click('[data-wallet-id="yacidevkit"]');
    await expect(page.locator('#wallet-info')).toBeVisible({ timeout: 10000 });

    const walletBtn = page.locator('[data-wallet-id="yacidevkit"]');
    await expect(walletBtn).toHaveClass(/connected/);
    await expect(walletBtn.locator('.status')).toContainText('Connected');
  });

  test('disconnect hides all sections', async ({ page }) => {
    await page.goto('/cip30-wallets.html');
    await waitForWalletReady(page);

    // Connect
    await page.click('[data-wallet-id="yacidevkit"]');
    await expect(page.locator('#wallet-info')).toBeVisible({ timeout: 10000 });

    // Disconnect
    await page.click('button:has-text("Disconnect")');

    // All sections should be hidden
    await expect(page.locator('#wallet-info')).toBeHidden();
    await expect(page.locator('#api-demo')).toBeHidden();
    await expect(page.locator('#transfer-section')).toBeHidden();
    await expect(page.locator('#raw-section')).toBeHidden();
  });
});

test.describe('CIP-30 Wallet Demo - API Functions', () => {
  test.beforeEach(async ({ page }) => {
    await page.goto('/cip30-wallets.html');
    await waitForWalletReady(page);
    await page.click('[data-wallet-id="yacidevkit"]');
    await expect(page.locator('#wallet-info')).toBeVisible({ timeout: 10000 });
  });

  test('Get Addresses shows bech32 addresses', async ({ page }) => {
    await page.click('button:has-text("Get Addresses")');

    const resultBox = page.locator('#api-result');
    await expect(resultBox).toBeVisible({ timeout: 10000 });

    // Should show bech32 addresses (addr_test1...)
    await expect(resultBox).toContainText('addr_test1');
    await expect(resultBox).toContainText('Used Addresses');
    await expect(resultBox).toContainText('Change Address');
  });

  test('Get UTXOs shows UTXO data', async ({ page }) => {
    await page.click('button:has-text("Get UTXOs")');

    const resultBox = page.locator('#api-result');
    await expect(resultBox).toBeVisible({ timeout: 10000 });

    // Should show UTXO count or data
    await expect(resultBox).toContainText(/UTXO/);
  });

  test('Reward Addresses shows stake addresses', async ({ page }) => {
    await page.click('button:has-text("Reward Addresses")');

    const resultBox = page.locator('#api-result');
    await expect(resultBox).toBeVisible({ timeout: 10000 });

    // Should show bech32 stake addresses (stake_test1...)
    await expect(resultBox).toContainText('Reward');
    await expect(resultBox).toContainText('stake_test1');
  });

  test('Sign Data returns signature', async ({ page }) => {
    await page.click('button:has-text("Sign Data")');

    const resultBox = page.locator('#api-result');
    await expect(resultBox).toBeVisible({ timeout: 10000 });

    // Should show successful signing
    await expect(resultBox).toContainText('Signed Successfully');
    await expect(resultBox).toContainText('Hello from CIP-30 demo');
  });
});

test.describe('CIP-30 Wallet Demo - Raw CBOR', () => {
  test.beforeEach(async ({ page }) => {
    await page.goto('/cip30-wallets.html');
    await waitForWalletReady(page);
    await page.click('[data-wallet-id="yacidevkit"]');
    await expect(page.locator('#wallet-info')).toBeVisible({ timeout: 10000 });
  });

  test('Raw Balance shows CBOR hex', async ({ page }) => {
    await page.click('button:has-text("Raw Balance")');

    const resultBox = page.locator('#raw-result');
    await expect(resultBox).toBeVisible({ timeout: 10000 });
    await expect(resultBox).toContainText('CBOR hex');
  });

  test('Raw UTXOs shows hex array', async ({ page }) => {
    await page.click('button:has-text("Raw UTXOs")');

    const resultBox = page.locator('#raw-result');
    await expect(resultBox).toBeVisible({ timeout: 10000 });
    await expect(resultBox).toContainText('CBOR hex array');
  });

  test('Raw Addresses shows hex addresses', async ({ page }) => {
    await page.click('button:has-text("Raw Addresses")');

    const resultBox = page.locator('#raw-result');
    await expect(resultBox).toBeVisible({ timeout: 10000 });
    await expect(resultBox).toContainText('hex');
  });
});

test.describe('CIP-30 Wallet Demo - Transfer (3-Step Flow)', () => {
  test.beforeEach(async ({ page }) => {
    await page.goto('/cip30-wallets.html');
    await waitForWalletReady(page);
    await page.click('[data-wallet-id="yacidevkit"]');
    await expect(page.locator('#wallet-info')).toBeVisible({ timeout: 10000 });
  });

  test('transfer form shows 3-step buttons', async ({ page }) => {
    await expect(page.locator('#transfer-section')).toBeVisible();
    await expect(page.locator('#receiver-address')).toBeVisible();
    await expect(page.locator('#ada-amount')).toBeVisible();

    // 3-step buttons
    await expect(page.locator('#btn-build')).toBeVisible();
    await expect(page.locator('#btn-sign')).toBeVisible();
    await expect(page.locator('#btn-sign')).toBeDisabled();
    await expect(page.locator('#btn-submit')).toBeVisible();
    await expect(page.locator('#btn-submit')).toBeDisabled();
  });

  test('build shows error for empty fields', async ({ page }) => {
    await page.click('#btn-build');

    const resultBox = page.locator('#build-result');
    await expect(resultBox).toBeVisible({ timeout: 5000 });
    await expect(resultBox).toContainText('Error');
  });

  test('3-step server build flow: Build -> Sign -> Submit', async ({ page }) => {
    // Get a receiver address from account 1
    const response = await page.request.get(`${CLI_URL}/api/v1/wallet/accounts/1`);
    const account1 = await response.json();
    const receiverAddress = account1.address;

    // Fill the transfer form
    await page.fill('#receiver-address', receiverAddress);
    await page.fill('#ada-amount', '10');

    // Step 1: Build
    await page.click('#btn-build');
    const buildResult = page.locator('#build-result');
    await expect(buildResult).toBeVisible({ timeout: 15000 });
    await expect(buildResult).toContainText('Transaction Built', { timeout: 15000 });

    // Sign button should now be enabled
    await expect(page.locator('#btn-sign')).toBeEnabled();
    await expect(page.locator('#btn-submit')).toBeDisabled();

    // Step 2: Sign
    await page.click('#btn-sign');
    const signResult = page.locator('#sign-result');
    await expect(signResult).toBeVisible({ timeout: 15000 });
    await expect(signResult).toContainText('Transaction Signed', { timeout: 15000 });

    // Submit button should now be enabled
    await expect(page.locator('#btn-submit')).toBeEnabled();

    // Step 3: Submit
    await page.click('#btn-submit');
    const submitResult = page.locator('#submit-result');
    await expect(submitResult).toBeVisible({ timeout: 15000 });
    await expect(submitResult).toContainText('Transaction Submitted', { timeout: 15000 });
    await expect(submitResult).toContainText(receiverAddress);
    await expect(submitResult).toContainText('10 ADA');

    // After submit, sign and submit buttons should be disabled again
    await expect(page.locator('#btn-sign')).toBeDisabled();
    await expect(page.locator('#btn-submit')).toBeDisabled();
  });
});
