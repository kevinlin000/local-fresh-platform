#!/usr/bin/env node

import { mkdir } from 'node:fs/promises'
import path from 'node:path'
import { chromium } from '@playwright/test'

const config = {
  userBaseUrl: process.env.USER_BASE_URL || 'http://127.0.0.1:5173',
  adminBaseUrl: process.env.ADMIN_BASE_URL || 'http://127.0.0.1:5174',
  adminUsername: process.env.ADMIN_USERNAME || 'admin',
  adminPassword: process.env.ADMIN_PASSWORD || '123456',
  outputDir: process.env.SCREENSHOT_DIR || 'docs/screenshots'
}

const viewport = { width: 1280, height: 720 }

async function waitForImages(page) {
  await page.waitForLoadState('networkidle')
  await page.evaluate(async () => {
    const images = Array.from(document.images)
    await Promise.all(images.map((image) => {
      if (image.complete) {
        return Promise.resolve()
      }
      return new Promise((resolve) => {
        image.addEventListener('load', resolve, { once: true })
        image.addEventListener('error', resolve, { once: true })
      })
    }))
  })
}

async function screenshot(page, fileName) {
  await waitForImages(page)
  await page.screenshot({
    path: path.join(config.outputDir, fileName),
    fullPage: false
  })
  console.log(`saved ${fileName}`)
}

async function waitForStorefrontProducts(page) {
  await page.getByText('今日市場').waitFor()
  await page.locator('.shelf-button').first().waitFor({ state: 'visible' })
  await page.locator('.product-card').first().waitFor({ state: 'visible' })
  await page.waitForFunction(() => !document.body.innerText.includes('0 / 0 項可購買'))
}

async function scrollProductGridIntoView(page) {
  await page.locator('.product-grid').scrollIntoViewIfNeeded()
  await page.evaluate(() => window.scrollBy(0, -96))
}

async function openUserPage(page, route, heading) {
  await page.goto(`${config.userBaseUrl}${route}`)
  await page.getByRole('heading', { name: heading }).waitFor()
}

async function openAdminPage(page, route, heading) {
  await page.goto(`${config.adminBaseUrl}${route}`)
  await page.getByRole('heading', { name: heading }).waitFor()
}

async function loginMember(page) {
  await page.goto(`${config.userBaseUrl}/login`)
  await page.getByRole('heading', { name: '登入菜籃日' }).waitFor()
  await page.getByRole('button', { name: '會員 A' }).click()
  await page.waitForURL(`${config.userBaseUrl}/`)
}

async function loginAdmin(page) {
  await page.goto(`${config.adminBaseUrl}/login`)
  await page.getByRole('heading', { name: '管理員登入' }).waitFor()
  await page.locator('input[autocomplete="username"]').fill(config.adminUsername)
  await page.locator('input[autocomplete="current-password"]').fill(config.adminPassword)
  await page.getByRole('button', { name: '登入後台' }).click()
  await page.waitForURL(`${config.adminBaseUrl}/dashboard`)
}

async function main() {
  await mkdir(config.outputDir, { recursive: true })

  const browser = await chromium.launch({ headless: true })
  const context = await browser.newContext({
    viewport,
    deviceScaleFactor: 1,
    reducedMotion: 'reduce'
  })

  try {
    const userPage = await context.newPage()
    await loginMember(userPage)
    await waitForStorefrontProducts(userPage)
    await screenshot(userPage, '01-home.png')

    await scrollProductGridIntoView(userPage)
    await screenshot(userPage, '02-product-list.png')

    await openUserPage(userPage, '/product/15', '澎湖花枝 (1隻 約400g)')
    await screenshot(userPage, '03-product-detail.png')

    await userPage.goto(`${config.userBaseUrl}/groupBuy/GB-202606-ACTIVE`)
    await userPage.getByText('揪團詳情').waitFor()
    await screenshot(userPage, '04-group-buy.png')

    await openUserPage(userPage, '/cart', '確認採買清單')
    await screenshot(userPage, '05-cart.png')

    await openUserPage(userPage, '/orders', '訂單與揪團紀錄')
    await screenshot(userPage, '06-orders.png')

    const adminPage = await context.newPage()
    await loginAdmin(adminPage)
    await adminPage.getByRole('heading', { name: '今日營運概覽' }).waitFor()
    await screenshot(adminPage, '07-admin-dashboard.png')

    await openAdminPage(adminPage, '/products', '可售狀態與補貨工作台')
    await screenshot(adminPage, '08-admin-products.png')

    await openAdminPage(adminPage, '/orders', '訂單履約工作台')
    await screenshot(adminPage, '09-admin-orders.png')

    await openAdminPage(adminPage, '/payment-events', '付款事件工作台')
    await screenshot(adminPage, '10-admin-payment-events.png')
  } finally {
    await browser.close()
  }
}

main().catch((error) => {
  console.error(error instanceof Error ? error.message : String(error))
  process.exit(1)
})
