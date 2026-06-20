#!/usr/bin/env node

const config = {
  apiBaseUrl: process.env.API_BASE_URL || 'http://127.0.0.1:8080',
  userBaseUrl: process.env.USER_BASE_URL || 'http://127.0.0.1:5173',
  adminBaseUrl: process.env.ADMIN_BASE_URL || 'http://127.0.0.1:5174',
  userMockCode: process.env.USER_MOCK_CODE || 'user_a',
  adminUsername: process.env.ADMIN_USERNAME || 'admin',
  adminPassword: process.env.ADMIN_PASSWORD || '123456',
  timeoutMs: Number(process.env.SMOKE_TIMEOUT_MS || 8000)
}

const checks = []

function pass(message) {
  checks.push({ status: 'PASS', message })
  console.log(`PASS ${message}`)
}

function fail(message) {
  checks.push({ status: 'FAIL', message })
  throw new Error(message)
}

async function request(name, url, options = {}) {
  const controller = new AbortController()
  const timeout = setTimeout(() => controller.abort(), config.timeoutMs)
  try {
    const response = await fetch(url, {
      ...options,
      signal: controller.signal,
      headers: {
        ...(options.body ? { 'content-type': 'application/json' } : {}),
        ...(options.headers || {})
      }
    })
    const contentType = response.headers.get('content-type') || ''
    const bodyText = await response.text()
    return { name, url, response, contentType, bodyText }
  } catch (error) {
    fail(`${name} request failed: ${error instanceof Error ? error.message : String(error)} (${url})`)
  } finally {
    clearTimeout(timeout)
  }
}

function parseJson(result) {
  try {
    return JSON.parse(result.bodyText)
  } catch {
    fail(`${result.name} did not return JSON: ${result.bodyText.slice(0, 160)}`)
  }
}

function expectHttpOk(result) {
  if (!result.response.ok) {
    fail(`${result.name} returned HTTP ${result.response.status}: ${result.bodyText.slice(0, 160)}`)
  }
}

function expectResultSuccess(result) {
  expectHttpOk(result)
  const json = parseJson(result)
  if (String(json.code) !== '1') {
    fail(`${result.name} returned business code ${json.code}: ${json.msg || result.bodyText.slice(0, 160)}`)
  }
  return json.data
}

async function checkSpaShell(name, baseUrl, path) {
  const result = await request(name, `${baseUrl}${path}`)
  expectHttpOk(result)
  if (!result.contentType.includes('text/html')) {
    fail(`${name} did not return HTML; content-type=${result.contentType}`)
  }
  if (!result.bodyText.includes('id="app"')) {
    fail(`${name} HTML does not look like a Vue app shell`)
  }
  pass(`${name} is serving the Vue app shell`)
}

async function main() {
  console.log('Local UI smoke precheck')
  console.log(`API_BASE_URL=${config.apiBaseUrl}`)
  console.log(`USER_BASE_URL=${config.userBaseUrl}`)
  console.log(`ADMIN_BASE_URL=${config.adminBaseUrl}`)

  const health = await request('backend health', `${config.apiBaseUrl}/actuator/health`)
  expectHttpOk(health)
  const healthJson = parseJson(health)
  if (healthJson.status !== 'UP') {
    fail(`backend health is not UP: ${health.bodyText}`)
  }
  pass('backend health is UP')

  await checkSpaShell('user storefront login page', config.userBaseUrl, '/login')
  await checkSpaShell('admin console login page', config.adminBaseUrl, '/login')

  const userLogin = await request('user mock login', `${config.apiBaseUrl}/user/member/login`, {
    method: 'POST',
    body: JSON.stringify({ code: config.userMockCode })
  })
  const user = expectResultSuccess(userLogin)
  if (!user?.token) {
    fail('user mock login did not return a token')
  }
  pass(`user mock login works for ${user.name || config.userMockCode}`)

  const userHeaders = { authentication: user.token }
  const productList = await request('user product list', `${config.apiBaseUrl}/user/product/list`, {
    headers: userHeaders
  })
  const products = expectResultSuccess(productList)
  if (!Array.isArray(products) || products.length === 0) {
    fail('user product list is empty')
  }
  pass(`user product list has ${products.length} products`)

  const userOrders = await request('user order history', `${config.apiBaseUrl}/user/order/historyOrders?page=1&pageSize=5`, {
    headers: userHeaders
  })
  const orderPage = expectResultSuccess(userOrders)
  const orderCount = Array.isArray(orderPage?.records) ? orderPage.records.length : 0
  pass(`user order history API works (${orderCount} records on first page)`)

  const adminLogin = await request('admin login', `${config.apiBaseUrl}/admin/employee/login`, {
    method: 'POST',
    body: JSON.stringify({
      username: config.adminUsername,
      password: config.adminPassword
    })
  })
  const admin = expectResultSuccess(adminLogin)
  if (!admin?.token) {
    fail('admin login did not return a token')
  }
  pass(`admin login works for ${config.adminUsername}`)

  const adminHeaders = { token: admin.token }
  const businessData = await request('admin business data', `${config.apiBaseUrl}/admin/workspace/businessData`, {
    headers: adminHeaders
  })
  expectResultSuccess(businessData)
  pass('admin business data API works')

  const adminOrders = await request('admin order search', `${config.apiBaseUrl}/admin/order/conditionSearch?page=1&pageSize=5`, {
    headers: adminHeaders
  })
  const adminOrderPage = expectResultSuccess(adminOrders)
  const adminOrderCount = Array.isArray(adminOrderPage?.records) ? adminOrderPage.records.length : 0
  pass(`admin order search API works (${adminOrderCount} records on first page)`)

  console.log(`\nSmoke precheck passed: ${checks.filter(check => check.status === 'PASS').length} checks`)
}

main().catch(error => {
  console.error(`FAIL ${error instanceof Error ? error.message : String(error)}`)
  console.error('\nExpected local services:')
  console.error('- Backend: API_BASE_URL, default http://127.0.0.1:8080')
  console.error('- User storefront: USER_BASE_URL, default http://127.0.0.1:5173')
  console.error('- Admin console: ADMIN_BASE_URL, default http://127.0.0.1:5174')
  process.exit(1)
})
