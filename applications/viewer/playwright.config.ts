import type { PlaywrightTestConfig } from '@playwright/test';

const config: PlaywrightTestConfig = {
	webServer: {
		command: 'npm run build && npm run preview',
		port: 4173,
		reuseExistingServer: !process.env.CI
	},
	testDir: 'tests',
	use: {
		baseURL: 'http://localhost:4173'
	},
	projects: [
		{
			name: 'chromium',
			use: {
				browserName: 'chromium',
				// e.g. PW_CHANNEL=chrome for features the bundled Chromium lacks (the julc engine needs Wasm exnref)
				channel: process.env.PW_CHANNEL || undefined
			}
		}
	]
};

export default config;
