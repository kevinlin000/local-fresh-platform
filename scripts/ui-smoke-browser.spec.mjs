import { expect, test } from '@playwright/test'

const config = {
  userBaseUrl: process.env.USER_BASE_URL || 'http://127.0.0.1:5173',
  adminBaseUrl: process.env.ADMIN_BASE_URL || 'http://127.0.0.1:5174',
  adminUsername: process.env.ADMIN_USERNAME || 'admin',
  adminPassword: process.env.ADMIN_PASSWORD || '123456'
}

test.describe('local browser smoke', () => {
  test('member can log in and reach storefront and orders', async ({ page }) => {
    await page.goto(`${config.userBaseUrl}/login`)
    await expect(page.getByRole('heading', { name: '會員登入' })).toBeVisible()

    await page.getByRole('button', { name: '試用會員 A' }).click()
    await expect(page).toHaveURL(`${config.userBaseUrl}/`)
    await expect(page.getByText('今日市場')).toBeVisible()
    await expect(page.getByRole('heading', { name: '全部商品' })).toBeVisible()

    await page.goto(`${config.userBaseUrl}/orders`)
    await expect(page.getByRole('heading', { name: '訂單與揪團紀錄' })).toBeVisible()
    await expect(page.getByText('一般訂單')).toBeVisible()
  })

  test('admin can log in and reach operational pages', async ({ page }) => {
    await page.goto(`${config.adminBaseUrl}/login`)
    await expect(page.getByRole('heading', { name: '管理員登入' })).toBeVisible()

    await page.locator('input[autocomplete="username"]').fill(config.adminUsername)
    await page.locator('input[autocomplete="current-password"]').fill(config.adminPassword)
    await page.getByRole('button', { name: '登入後台' }).click()

    await expect(page).toHaveURL(`${config.adminBaseUrl}/dashboard`)
    await expect(page.getByRole('heading', { name: '今日營運概覽' })).toBeVisible()
    await expect(page.getByText('今日優先處理')).toBeVisible()

    await page.goto(`${config.adminBaseUrl}/orders`)
    await expect(page.getByPlaceholder('搜尋訂單編號')).toBeVisible()
    await expect(page.getByText('待確認').first()).toBeVisible()

    await page.goto(`${config.adminBaseUrl}/products`)
    await expect(page.getByPlaceholder('搜尋商品名稱')).toBeVisible()
    await expect(page.getByRole('button', { name: '新增商品' })).toBeVisible()
  })
})
