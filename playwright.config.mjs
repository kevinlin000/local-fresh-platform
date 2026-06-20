import { defineConfig, devices } from '@playwright/test'

export default defineConfig({
  timeout: 30_000,
  expect: {
    timeout: 8_000
  },
  reporter: [['line']],
  outputDir: 'output/playwright/test-results',
  use: {
    ...devices['Desktop Chrome'],
    headless: true,
    screenshot: 'only-on-failure',
    trace: 'retain-on-failure'
  },
  projects: [
    {
      name: 'chromium'
    }
  ]
})
