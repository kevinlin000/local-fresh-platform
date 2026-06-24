#!/usr/bin/env node

const config = {
  apiBaseUrl: process.env.API_BASE_URL || 'http://127.0.0.1:8080',
  expectedApplication: process.env.EXPECTED_BACKEND_APPLICATION || 'local-fresh-server',
  timeoutMs: Number(process.env.SMOKE_TIMEOUT_MS || 8000)
}

function fail(message) {
  console.error(`FAIL ${message}`)
  console.error('\nExpected Local Fresh backend:')
  console.error(`- API_BASE_URL=${config.apiBaseUrl}`)
  console.error(`- EXPECTED_BACKEND_APPLICATION=${config.expectedApplication}`)
  console.error('\nIf another service is using 8080, start the backend on another port:')
  console.error('  cd backend-environment/local-fresh-backend')
  console.error('  MYSQL_ROOT_PASSWORD=password mvn -f local-fresh-server/pom.xml spring-boot:run -Dspring-boot.run.arguments=--server.port=18080')
  console.error('  API_BASE_URL=http://127.0.0.1:18080 npm run smoke:backend')
  process.exit(1)
}

async function requestJson(name, path) {
  const controller = new AbortController()
  const timeout = setTimeout(() => controller.abort(), config.timeoutMs)
  const url = `${config.apiBaseUrl}${path}`

  try {
    const response = await fetch(url, { signal: controller.signal })
    const bodyText = await response.text()
    if (!response.ok) {
      fail(`${name} returned HTTP ${response.status}: ${bodyText.slice(0, 160)}`)
    }

    try {
      return JSON.parse(bodyText)
    } catch {
      fail(`${name} did not return JSON: ${bodyText.slice(0, 160)}`)
    }
  } catch (error) {
    fail(`${name} request failed: ${error instanceof Error ? error.message : String(error)} (${url})`)
  } finally {
    clearTimeout(timeout)
  }
}

const info = await requestJson('backend identity', '/actuator/info')
const application = info?.deployment?.application

if (application !== config.expectedApplication) {
  fail(
    `backend identity mismatch: expected deployment.application=${config.expectedApplication}, ` +
      `got ${application || 'missing'}`
  )
}

const health = await requestJson('backend health', '/actuator/health')
if (health.status !== 'UP') {
  fail(`backend health is not UP: ${JSON.stringify(health).slice(0, 160)}`)
}

console.log(`PASS backend identity is ${application}`)
console.log('PASS backend health is UP')
