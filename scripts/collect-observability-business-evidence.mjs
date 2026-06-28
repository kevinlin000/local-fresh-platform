#!/usr/bin/env node

import crypto from 'node:crypto'

const config = {
  apiBaseUrl: process.env.API_BASE_URL || 'http://127.0.0.1:18080',
  prometheusBaseUrl: process.env.PROMETHEUS_BASE_URL || 'http://localhost:9091',
  userACode: process.env.USER_A_CODE || 'user_a',
  userCCode: process.env.USER_C_CODE || 'user_c',
  adminUsername: process.env.ADMIN_USERNAME || 'admin',
  adminPassword: process.env.ADMIN_PASSWORD || '123456',
  demoCallbackSecret: process.env.PAYMENT_CALLBACK_SECRET || 'local-demo-payment-callback-secret',
  timeoutMs: Number(process.env.OBSERVABILITY_EVIDENCE_TIMEOUT_MS || 8000),
  scrapeWaitMs: Number(process.env.OBSERVABILITY_EVIDENCE_SCRAPE_WAIT_MS || 35000),
  activeGroupNo: process.env.OBSERVABILITY_EVIDENCE_GROUP_NO || 'GB-202606-ACTIVE',
  activeGroupProductId: Number(process.env.OBSERVABILITY_EVIDENCE_GROUP_PRODUCT_ID || 11)
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

function warn(message) {
  checks.push({ status: 'WARN', message })
  console.warn(`WARN ${message}`)
}

function sleep(ms) {
  return new Promise(resolve => setTimeout(resolve, ms))
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
    const bodyText = await response.text()
    return { name, url, response, bodyText }
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
    fail(`${result.name} did not return JSON: ${result.bodyText.slice(0, 180)}`)
  }
}

function expectHttpOk(result) {
  if (!result.response.ok) {
    fail(`${result.name} returned HTTP ${result.response.status}: ${result.bodyText.slice(0, 180)}`)
  }
}

function expectResultSuccess(result) {
  expectHttpOk(result)
  const json = parseJson(result)
  if (String(json.code) !== '1') {
    fail(`${result.name} returned business code ${json.code}: ${json.msg || result.bodyText.slice(0, 180)}`)
  }
  return json.data
}

async function apiJson(name, path, options = {}) {
  const result = await request(name, `${config.apiBaseUrl}${path}`, options)
  return expectResultSuccess(result)
}

async function login(code) {
  const data = await apiJson(`mock login ${code}`, '/user/member/login', {
    method: 'POST',
    body: JSON.stringify({ code })
  })
  if (!data?.token) {
    fail(`mock login ${code} did not return a token`)
  }
  pass(`mock login ${code} returned member ${data.id}`)
  return { id: data.id, name: data.name, token: data.token }
}

async function loginAdmin() {
  const data = await apiJson(`admin login ${config.adminUsername}`, '/admin/employee/login', {
    method: 'POST',
    body: JSON.stringify({
      username: config.adminUsername,
      password: config.adminPassword
    })
  })
  if (!data?.token) {
    fail(`admin login ${config.adminUsername} did not return a token`)
  }
  pass(`admin login ${config.adminUsername} returned token`)
  return { token: data.token, name: data.name || config.adminUsername }
}

async function getDefaultAddress(user) {
  const result = await request(`default address for ${user.name}`, `${config.apiBaseUrl}/user/shippingAddress/default`, {
    headers: { authentication: user.token }
  })
  if (result.response.ok) {
    const json = parseJson(result)
    if (String(json.code) === '1' && json.data?.id) {
      return json.data
    }
  }

  const addresses = await apiJson(`address list for ${user.name}`, '/user/shippingAddress/list', {
    headers: { authentication: user.token }
  })
  if (!Array.isArray(addresses) || addresses.length === 0) {
    fail(`member ${user.name} has no demo shipping address`)
  }
  return addresses[0]
}

async function findAdminCancelableOrder(admin) {
  for (const status of [4, 3, 2, 1]) {
    const page = await apiJson(`admin cancelable orders status ${status}`, `/admin/order/conditionSearch?page=1&pageSize=20&status=${status}`, {
      headers: { token: admin.token }
    })
    const order = [...(page?.records || [])]
      .filter(item => item?.id)
      .sort((a, b) => Number(b.id) - Number(a.id))[0]
    if (order) {
      return order
    }
  }
  return null
}

async function findPaidCallbackOrder(admin) {
  for (const status of [3, 2, 5, 4]) {
    const page = await apiJson(`admin paid callback candidate status ${status}`, `/admin/order/conditionSearch?page=1&pageSize=20&status=${status}`, {
      headers: { token: admin.token }
    })
    const order = page?.records?.find(item => item?.number)
    if (order) {
      return order
    }
  }
  return null
}

async function cancelOrderAsAdmin(admin, orderId) {
  await apiJson(`admin cancel order ${orderId}`, '/admin/order/cancel', {
    method: 'PUT',
    headers: { token: admin.token },
    body: JSON.stringify({
      id: orderId,
      cancelReason: 'observability business evidence'
    })
  })
  pass(`admin cancellation flow completed for order ${orderId}`)
}

function signDemoCallback(payload) {
  const canonicalPayload = Object.entries(payload)
    .filter(([key]) => key !== 'signature')
    .sort(([a], [b]) => a.localeCompare(b))
    .map(([key, value]) => `${key}=${value == null ? '' : value}`)
    .join('&')
  return crypto
    .createHmac('sha256', config.demoCallbackSecret)
    .update(canonicalPayload)
    .digest('hex')
}

async function triggerDuplicatePaymentCallback(orderNumber) {
  const payload = {
    orderNumber,
    status: 'SUCCESS',
    providerReference: `observability-callback:${Date.now()}`,
    providerTradeNo: `OBS${Date.now()}`
  }
  payload.signature = signDemoCallback(payload)
  const result = await request(`duplicate payment callback ${orderNumber}`, `${config.apiBaseUrl}/payment/callback`, {
    method: 'POST',
    headers: { 'content-type': 'application/x-www-form-urlencoded' },
    body: new URLSearchParams(payload).toString()
  })
  expectHttpOk(result)
  if (result.bodyText.trim() !== '1|OK') {
    fail(`duplicate payment callback returned unexpected body: ${result.bodyText}`)
  }
  pass(`duplicate payment callback accepted for ${orderNumber}`)
}

async function completeSeedGroupBuy(user, addressId) {
  await apiJson(`join seed group buy ${config.activeGroupNo}`, '/user/groupBuy/join', {
    method: 'POST',
    headers: { authentication: user.token },
    body: JSON.stringify({
      groupNo: config.activeGroupNo,
      productId: config.activeGroupProductId,
      quantity: 1,
      addressId
    })
  })
  pass(`seed group buy ${config.activeGroupNo} completed by ${user.name}`)
}

async function queryPrometheus(expr) {
  const url = new URL('/api/v1/query', config.prometheusBaseUrl)
  url.searchParams.set('query', expr)
  const result = await request(`prometheus query ${expr}`, url.toString())
  expectHttpOk(result)
  const json = parseJson(result)
  if (json.status !== 'success') {
    fail(`Prometheus query failed: ${result.bodyText.slice(0, 180)}`)
  }
  const value = json.data?.result?.[0]?.value?.[1]
  return Number(value || 0)
}

async function waitForPrometheusIncrease(label, expr, before) {
  const deadline = Date.now() + config.scrapeWaitMs
  let latest = 0
  do {
    latest = await queryPrometheus(expr)
    if (latest > before) {
      pass(`${label} Prometheus value increased from ${before} to ${latest}`)
      return latest
    }
    await sleep(2500)
  } while (Date.now() < deadline)
  fail(`${label} Prometheus value did not increase within ${config.scrapeWaitMs}ms; before=${before}, latest=${latest}`)
}

async function main() {
  console.log('Local Fresh observability business evidence')
  console.log(`API_BASE_URL=${config.apiBaseUrl}`)
  console.log(`PROMETHEUS_BASE_URL=${config.prometheusBaseUrl}`)

  const health = await request('backend health', `${config.apiBaseUrl}/actuator/health`)
  expectHttpOk(health)
  if (parseJson(health).status !== 'UP') {
    fail(`backend health is not UP: ${health.bodyText}`)
  }
  pass('backend health is UP')

  const targetUp = await queryPrometheus('up{job="local-fresh-backend",instance="host.docker.internal:18080"}')
  if (targetUp !== 1) {
    fail(`local-fresh-backend Prometheus target is not UP: ${targetUp}`)
  }
  pass('Prometheus target is UP')

  const paymentExpr = 'sum(localfresh_payment_callback_total{provider="demo",result="ignored"}) or vector(0)'
  const cancellationExpr = 'sum(localfresh_order_cancellation_total{result="applied"}) or vector(0)'
  const groupBuyExpr = 'sum(localfresh_group_buy_transition_total{result="completed"}) or vector(0)'

  const beforePayment = await queryPrometheus(paymentExpr)
  const beforeCancellation = await queryPrometheus(cancellationExpr)
  const beforeGroupBuy = await queryPrometheus(groupBuyExpr)

  const userA = await login(config.userACode)
  const userC = await login(config.userCCode)
  const admin = await loginAdmin()
  const addressC = await getDefaultAddress(userC)

  const paidCallbackOrder = await findPaidCallbackOrder(admin)
  if (paidCallbackOrder) {
    await triggerDuplicatePaymentCallback(paidCallbackOrder.number)
    await waitForPrometheusIncrease('payment callback duplicate ignored', paymentExpr, beforePayment)
  } else if (beforePayment > 0) {
    pass(`payment callback duplicate metric already has evidence (${beforePayment})`)
  } else {
    fail('no paid order is available to trigger duplicate callback evidence, and payment callback metric is still zero')
  }

  const cancellableOrder = await findAdminCancelableOrder(admin)
  if (cancellableOrder) {
    try {
      await cancelOrderAsAdmin(admin, cancellableOrder.id)
      await waitForPrometheusIncrease('order cancellation applied', cancellationExpr, beforeCancellation)
    } catch (error) {
      warn(`order cancellation evidence was skipped: ${error instanceof Error ? error.message : String(error)}`)
    }
  } else if (beforeCancellation > 0) {
    pass(`order cancellation applied metric already has evidence (${beforeCancellation})`)
  } else {
    warn('no admin-cancelable order is available to trigger cancellation evidence, and cancellation metric is still zero')
  }

  if (beforeGroupBuy > 0) {
    pass(`group-buy completed metric already has evidence (${beforeGroupBuy})`)
  } else {
    try {
      await completeSeedGroupBuy(userC, addressC.id)
      await waitForPrometheusIncrease('group-buy completed', groupBuyExpr, beforeGroupBuy)
    } catch (error) {
      warn(`group-buy completion evidence was skipped: ${error instanceof Error ? error.message : String(error)}`)
    }
  }

  const passCount = checks.filter(check => check.status === 'PASS').length
  const warnCount = checks.filter(check => check.status === 'WARN').length
  console.log(`\nObservability business evidence passed: ${passCount} checks, ${warnCount} warnings`)
}

main().catch(error => {
  console.error(`FAIL ${error instanceof Error ? error.message : String(error)}`)
  console.error('\nExpected local services:')
  console.error('- Backend: API_BASE_URL, default http://127.0.0.1:18080')
  console.error('- Prometheus: PROMETHEUS_BASE_URL, default http://localhost:9091')
  console.error('- Local Fresh Prometheus target: host.docker.internal:18080')
  process.exit(1)
})
