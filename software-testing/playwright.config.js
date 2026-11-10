const { defineConfig, devices } = require('@playwright/test');

const baseURL = process.env.BASE_URL || 'http://127.0.0.1:5173';
const apiBaseURL = process.env.API_BASE_URL || 'http://127.0.0.1:8080';

module.exports = defineConfig({
  testDir: './tests',
  fullyParallel: false,
  timeout: 30_000,
  expect: { timeout: 5_000 },
  retries: process.env.CI ? 1 : 0,
  workers: process.env.CI ? 1 : undefined,
  reporter: [
    ['list'],
    ['html', { outputFolder: 'playwright-report', open: 'never' }]
  ],
  use: {
    baseURL,
    trace: 'retain-on-failure',
    screenshot: 'only-on-failure',
    video: 'retain-on-failure'
  },
  projects: [
    {
      name: 'api',
      testMatch: /api\/.*\.spec\.js/,
      use: { baseURL: apiBaseURL }
    },
    {
      name: 'chromium',
      testMatch: /ui\/.*\.spec\.js/,
      use: { ...devices['Desktop Chrome'] }
    },
    {
      name: 'android-webview',
      testMatch: /mobile\/.*\.spec\.js/,
      use: {
        ...devices['Pixel 5'],
        browserName: 'chromium'
      }
    }
  ],
  webServer: process.env.SKIP_WEBSERVER
    ? undefined
    : [
        {
          command: 'mvn -f ../backend/pom.xml spring-boot:run',
          url: `${apiBaseURL}/api/student/session`,
          reuseExistingServer: true,
          timeout: 120_000
        },
        {
          command: 'npm --prefix ../frontend run dev -- --host 127.0.0.1',
          url: baseURL,
          reuseExistingServer: true,
          timeout: 60_000
        }
      ]
});
