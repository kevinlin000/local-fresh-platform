<template>
  <section class="admin-card admin-card-pad">
    <div class="payment-header">
      <div>
        <p class="eyebrow">付款對帳</p>
        <h2>付款事件工作台</h2>
        <span>追蹤付款請求、成功回呼、重複 callback、拒絕回呼與冪等鍵，避免訂單付款狀態不一致。</span>
      </div>
      <div class="payment-header-actions">
        <el-button @click="resetQuery">重置</el-button>
        <el-button type="primary" plain @click="loadData">重新整理</el-button>
      </div>
    </div>

    <div class="reconciliation-board" aria-label="付款對帳摘要">
      <div class="reconciliation-primary" :class="paymentPriority.tone">
        <span>目前第一優先</span>
        <strong>{{ paymentPriority.label }}</strong>
        <small>{{ paymentPriority.caption }}</small>
      </div>
      <div class="payment-summary">
        <div v-for="item in summaryCards" :key="item.label" class="payment-summary-card" :class="item.tone">
          <span>{{ item.label }}</span>
          <strong>{{ item.value }}</strong>
          <small>{{ item.caption }}</small>
        </div>
      </div>
    </div>

    <div class="table-toolbar payment-toolbar">
      <el-input v-model="query.orderNumber" clearable placeholder="訂單編號" @keyup.enter="search" />
      <el-input v-model="query.providerTradeNo" clearable placeholder="交易編號" @keyup.enter="search" />
      <el-input v-model="query.idempotencyKey" clearable placeholder="冪等鍵" @keyup.enter="search" />
      <el-select v-model="query.provider" clearable placeholder="付款 Provider">
        <el-option v-for="item in providerOptions" :key="item.value" :label="item.label" :value="item.value" />
      </el-select>
      <el-select v-model="query.eventType" clearable placeholder="事件類型">
        <el-option v-for="item in eventOptions" :key="item.value" :label="item.label" :value="item.value" />
      </el-select>
      <el-select v-model="query.result" clearable placeholder="處理結果">
        <el-option v-for="item in resultOptions" :key="item.value" :label="item.label" :value="item.value" />
      </el-select>
      <el-date-picker
        v-model="query.timeRange"
        type="datetimerange"
        range-separator="至"
        start-placeholder="開始時間"
        end-placeholder="結束時間"
        value-format="YYYY-MM-DD HH:mm:ss"
      />
      <el-button type="primary" @click="search">查詢</el-button>
    </div>

    <el-alert
      v-if="activeFilterLabel"
      class="filter-alert"
      type="info"
      show-icon
      :closable="false"
      :title="`目前篩選：${activeFilterLabel}`"
    />

    <div class="payment-table-wrap">
      <el-table v-loading="loading" :data="rows" stripe>
        <el-table-column label="訂單" min-width="150">
          <template #default="{ row }">
            <div class="order-cell">
              <strong>{{ row.orderNumber || '-' }}</strong>
              <span v-if="row.orderId">#{{ row.orderId }}</span>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="Provider" width="105">
          <template #default="{ row }">
            <el-tag effect="plain">{{ row.provider || '-' }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="事件" min-width="145">
          <template #default="{ row }">
            <el-tag :type="eventTagType(row.eventType)" effect="plain">{{ eventLabel(row.eventType) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="結果" width="100">
          <template #default="{ row }">
            <el-tag :type="resultTagType(row.result)">{{ resultLabel(row.result) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="金額" width="105" align="right">
          <template #default="{ row }">{{ currency(row.amount) }}</template>
        </el-table-column>
        <el-table-column label="對帳判斷" min-width="170">
          <template #default="{ row }">
            <div class="reconciliation-cell" :class="reconciliationTone(row)">
              <strong>{{ reconciliationLabel(row) }}</strong>
              <span>{{ reconciliationHint(row) }}</span>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="Reference" min-width="220">
          <template #default="{ row }">
            <div class="reference-cell">
              <strong>{{ row.providerReference || '-' }}</strong>
              <span v-if="row.providerTradeNo">Trade #{{ row.providerTradeNo }}</span>
              <small v-if="row.idempotencyKey">{{ row.idempotencyKey }}</small>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="時間" min-width="165">
          <template #default="{ row }">{{ formatDateTime(row.createdAt) }}</template>
        </el-table-column>
      </el-table>
    </div>

    <el-empty v-if="!loading && rows.length === 0" description="尚無符合條件的付款事件" />

    <el-pagination
      v-model:current-page="page.page"
      class="pager"
      background
      layout="total, prev, pager, next"
      :total="page.total"
      @current-change="loadData"
    />
  </section>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { useRoute } from 'vue-router'
import { getPaymentEventPage } from '@/api/paymentEvent'
import { readPage, useLoading, usePage } from './composables'

const route = useRoute()

const providerOptions = [
  { label: 'Demo', value: 'DEMO' },
  { label: '綠界 ECPay', value: 'ECPAY' },
  { label: '未知', value: 'UNKNOWN' }
]

const eventOptions = [
  { label: '建立付款請求', value: 'REQUEST_CREATED' },
  { label: '付款成功回呼', value: 'CALLBACK_SUCCEEDED' },
  { label: '重複回呼', value: 'CALLBACK_DUPLICATE' },
  { label: '拒絕回呼', value: 'CALLBACK_REJECTED' }
]

const resultOptions = [
  { label: '待處理', value: 'PENDING' },
  { label: '成功', value: 'SUCCEEDED' },
  { label: '忽略', value: 'IGNORED' },
  { label: '拒絕', value: 'REJECTED' }
]

const rows = ref<any[]>([])
const page = usePage()
const { loading, withLoading } = useLoading()
const query = reactive({
  orderNumber: '',
  providerTradeNo: '',
  idempotencyKey: '',
  provider: '',
  eventType: '',
  result: '',
  timeRange: [] as string[] | null
})

const summaryCards = computed(() => {
  const succeeded = rows.value.filter((item) => item.result === 'SUCCEEDED').length
  const pending = rows.value.filter((item) => item.result === 'PENDING').length
  const attention = rows.value.filter((item) => ['IGNORED', 'REJECTED'].includes(item.result)).length
  const providers = new Set(rows.value.map((item) => item.provider).filter(Boolean)).size

  return [
    { label: '本頁事件', value: rows.value.length, caption: '目前查詢結果', tone: 'neutral' },
    { label: '成功', value: succeeded, caption: '已入帳或處理完成', tone: 'success' },
    { label: '待處理', value: pending, caption: '付款請求已建立', tone: pending ? 'warning' : 'neutral' },
    { label: '需追蹤', value: attention, caption: '重複或被拒回呼', tone: attention ? 'danger' : 'neutral' },
    { label: 'Provider', value: providers, caption: '本頁付款通道數', tone: 'neutral' }
  ]
})
const paymentPriority = computed(() => {
  const rejected = rows.value.filter((item) => item.result === 'REJECTED').length
  const pending = rows.value.filter((item) => item.result === 'PENDING').length
  const ignored = rows.value.filter((item) => item.result === 'IGNORED').length

  if (rejected > 0) {
    return {
      label: `${rejected} 筆拒絕回呼需查`,
      caption: '優先檢查簽章、訂單狀態與 provider payload，避免付款成功但訂單未入帳。',
      tone: 'danger'
    }
  }
  if (pending > 0) {
    return {
      label: `${pending} 筆付款等待終態`,
      caption: '付款請求已建立但尚未收到成功或拒絕回呼，需留意逾時對帳。',
      tone: 'warning'
    }
  }
  if (ignored > 0) {
    return {
      label: `${ignored} 筆冪等忽略事件`,
      caption: '多數為重複 callback 或已處理事件，保留留痕即可。',
      tone: 'neutral'
    }
  }
  return {
    label: '付款事件狀態穩定',
    caption: '目前查詢結果沒有待處理或拒絕回呼，付款鏈路可進入例行巡檢。',
    tone: 'success'
  }
})
const activeFilterLabel = computed(() => {
  const filters = [
    query.orderNumber && `訂單 ${query.orderNumber}`,
    query.providerTradeNo && `交易 ${query.providerTradeNo}`,
    query.idempotencyKey && '冪等鍵',
    query.provider && providerOptions.find((item) => item.value === query.provider)?.label,
    query.eventType && eventLabel(query.eventType),
    query.result && resultLabel(query.result),
    query.timeRange?.length ? '指定時間區間' : ''
  ].filter(Boolean)
  return filters.join('、')
})

async function loadData() {
  await withLoading(async () => {
    const [beginTime, endTime] = query.timeRange || []
    const response = await getPaymentEventPage({
      page: page.page,
      pageSize: page.pageSize,
      orderNumber: query.orderNumber.trim() || undefined,
      providerTradeNo: query.providerTradeNo.trim() || undefined,
      idempotencyKey: query.idempotencyKey.trim() || undefined,
      provider: query.provider || undefined,
      eventType: query.eventType || undefined,
      result: query.result || undefined,
      beginTime,
      endTime
    })
    const result = readPage(response)
    rows.value = result.records
    page.total = result.total
  })
}

async function search() {
  page.page = 1
  await loadData()
}

async function resetQuery() {
  Object.assign(query, {
    orderNumber: '',
    providerTradeNo: '',
    idempotencyKey: '',
    provider: '',
    eventType: '',
    result: '',
    timeRange: []
  })
  page.page = 1
  await loadData()
}

function applyRouteQuery() {
  const routeQuery = route.query
  query.orderNumber = String(routeQuery.orderNumber || '')
  query.providerTradeNo = String(routeQuery.providerTradeNo || '')
  query.idempotencyKey = String(routeQuery.idempotencyKey || '')
  query.provider = String(routeQuery.provider || '')
  query.eventType = String(routeQuery.eventType || '')
  query.result = String(routeQuery.result || '')
}

function eventLabel(eventType: string) {
  return eventOptions.find((item) => item.value === eventType)?.label || eventType || '-'
}

function resultLabel(result: string) {
  return resultOptions.find((item) => item.value === result)?.label || result || '-'
}

function eventTagType(eventType: string) {
  if (eventType === 'CALLBACK_SUCCEEDED') {
    return 'success'
  }
  if (eventType === 'CALLBACK_DUPLICATE') {
    return 'warning'
  }
  if (eventType === 'CALLBACK_REJECTED') {
    return 'danger'
  }
  return 'primary'
}

function resultTagType(result: string) {
  if (result === 'SUCCEEDED') {
    return 'success'
  }
  if (result === 'PENDING') {
    return 'info'
  }
  if (result === 'IGNORED') {
    return 'warning'
  }
  if (result === 'REJECTED') {
    return 'danger'
  }
  return ''
}

function reconciliationLabel(row: any) {
  if (row.result === 'REJECTED') {
    return '回呼被拒絕'
  }
  if (row.result === 'PENDING') {
    return '等待付款終態'
  }
  if (row.eventType === 'CALLBACK_DUPLICATE' || row.result === 'IGNORED') {
    return '已冪等忽略'
  }
  if (row.eventType === 'CALLBACK_SUCCEEDED' || row.result === 'SUCCEEDED') {
    return '已入帳'
  }
  if (row.eventType === 'REQUEST_CREATED') {
    return '付款請求建立'
  }
  return '需人工檢視'
}

function reconciliationHint(row: any) {
  if (row.result === 'REJECTED') {
    return '檢查簽章、金額與訂單狀態'
  }
  if (row.result === 'PENDING') {
    return '等待 provider 回呼或逾時對帳'
  }
  if (row.eventType === 'CALLBACK_DUPLICATE' || row.result === 'IGNORED') {
    return '重複 callback 已留痕'
  }
  if (row.eventType === 'CALLBACK_SUCCEEDED' || row.result === 'SUCCEEDED') {
    return '訂單付款狀態可核對'
  }
  if (row.eventType === 'REQUEST_CREATED') {
    return '後續應收到付款終態'
  }
  return '打開 provider reference 追查'
}

function reconciliationTone(row: any) {
  if (row.result === 'REJECTED') {
    return 'danger'
  }
  if (row.result === 'PENDING') {
    return 'warning'
  }
  if (row.eventType === 'CALLBACK_SUCCEEDED' || row.result === 'SUCCEEDED') {
    return 'success'
  }
  return 'neutral'
}

function currency(value?: number | string) {
  const amount = Number(value)
  if (!Number.isFinite(amount)) {
    return '-'
  }
  return `NT$ ${amount.toLocaleString('zh-TW', { minimumFractionDigits: 0, maximumFractionDigits: 0 })}`
}

function formatDateTime(value?: string) {
  if (!value) {
    return '-'
  }
  return value.replace('T', ' ').slice(0, 19)
}

onMounted(() => {
  applyRouteQuery()
  void loadData()
})
</script>

<style scoped>
.payment-header {
  display: flex;
  justify-content: space-between;
  gap: 18px;
  align-items: flex-start;
  margin-bottom: 16px;
}

.payment-header h2 {
  margin: 0;
  font-size: 22px;
  line-height: 1.25;
}

.payment-header span {
  display: block;
  margin-top: 6px;
  color: var(--admin-muted);
  font-size: 13px;
}

.payment-header-actions {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  justify-content: flex-end;
}

.eyebrow {
  margin: 0 0 6px;
  color: var(--admin-green);
  font-weight: 800;
}

.reconciliation-board {
  display: grid;
  grid-template-columns: 280px minmax(0, 1fr);
  gap: 12px;
  align-items: stretch;
  margin-bottom: 14px;
  padding: 12px;
  border: 1px solid var(--admin-line);
  border-radius: 6px;
  background: #fbfcfa;
}

.reconciliation-primary {
  min-width: 0;
  padding: 14px;
  border: 1px solid var(--admin-line);
  border-radius: 6px;
  background: #ffffff;
}

.reconciliation-primary.danger {
  border-color: rgba(180, 35, 24, 0.24);
  background: #fffafa;
  box-shadow: inset 3px 0 0 rgba(180, 35, 24, 0.72);
}

.reconciliation-primary.warning {
  border-color: rgba(167, 109, 34, 0.25);
  background: #fffaf2;
  box-shadow: inset 3px 0 0 rgba(167, 109, 34, 0.72);
}

.reconciliation-primary.success {
  border-color: rgba(45, 106, 79, 0.2);
  box-shadow: inset 3px 0 0 rgba(45, 106, 79, 0.68);
}

.payment-summary {
  display: grid;
  grid-template-columns: repeat(5, minmax(0, 1fr));
  gap: 10px;
}

.payment-summary-card {
  min-width: 0;
  padding: 13px;
  border: 1px solid var(--admin-line);
  border-radius: 6px;
  background: #ffffff;
}

.payment-summary-card.warning {
  border-color: rgba(167, 109, 34, 0.22);
  background: #fffaf2;
}

.payment-summary-card.danger {
  border-color: rgba(180, 35, 24, 0.22);
  background: #fffafa;
}

.payment-summary-card.success {
  border-color: rgba(45, 106, 79, 0.18);
}

.reconciliation-primary span,
.reconciliation-primary small,
.payment-summary-card span,
.payment-summary-card small {
  display: block;
  color: var(--admin-muted);
  line-height: 1.4;
}

.reconciliation-primary span,
.payment-summary-card span {
  font-size: 12px;
  font-weight: 800;
}

.reconciliation-primary strong {
  display: block;
  margin: 9px 0 7px;
  color: var(--admin-ink);
  font-size: 20px;
  line-height: 1.25;
}

.payment-summary-card strong {
  display: block;
  margin: 7px 0 4px;
  font-size: 22px;
}

.payment-toolbar :deep(.el-date-editor) {
  width: 360px;
}

.filter-alert {
  margin-bottom: 12px;
}

.payment-table-wrap {
  width: 100%;
  max-width: 100%;
  min-width: 0;
  overflow-x: auto;
}

.payment-table-wrap :deep(.el-table) {
  width: 100%;
}

.order-cell,
.reference-cell,
.reconciliation-cell {
  display: grid;
  gap: 3px;
}

.order-cell strong,
.reference-cell strong,
.reconciliation-cell strong {
  font-size: 14px;
}

.order-cell span,
.reference-cell span,
.reference-cell small,
.reconciliation-cell span {
  color: var(--admin-muted);
  font-size: 12px;
  line-height: 1.35;
}

.reference-cell strong,
.reference-cell span,
.reference-cell small {
  min-width: 0;
  overflow-wrap: anywhere;
}

.reconciliation-cell {
  padding-left: 9px;
  border-left: 3px solid var(--admin-line-strong);
}

.reconciliation-cell.danger {
  border-left-color: var(--admin-danger);
}

.reconciliation-cell.warning {
  border-left-color: var(--admin-gold);
}

.reconciliation-cell.success {
  border-left-color: var(--admin-green);
}

@media (max-width: 900px) {
  .payment-header {
    display: grid;
  }

  .payment-header-actions {
    justify-content: flex-start;
  }

  .reconciliation-board {
    grid-template-columns: 1fr;
  }

  .payment-summary {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .payment-toolbar :deep(.el-date-editor) {
    width: 100%;
  }
}
</style>
