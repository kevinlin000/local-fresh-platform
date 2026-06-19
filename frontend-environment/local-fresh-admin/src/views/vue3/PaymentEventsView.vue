<template>
  <section class="admin-card admin-card-pad">
    <div class="payment-summary">
      <div v-for="item in summaryCards" :key="item.label" class="payment-summary-card">
        <span>{{ item.label }}</span>
        <strong>{{ item.value }}</strong>
        <small>{{ item.caption }}</small>
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
      <el-button @click="resetQuery">重置</el-button>
    </div>

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
import { getPaymentEventPage } from '@/api/paymentEvent'
import { readPage, useLoading, usePage } from './composables'

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

  return [
    { label: '本頁事件', value: rows.value.length, caption: '目前查詢結果' },
    { label: '成功', value: succeeded, caption: '已入帳或處理完成' },
    { label: '待處理', value: pending, caption: '付款請求已建立' },
    { label: '需追蹤', value: attention, caption: '重複或被拒回呼' }
  ]
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

onMounted(loadData)
</script>

<style scoped>
.payment-summary {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 12px;
  margin-bottom: 18px;
}

.payment-summary-card {
  padding: 16px;
  border: 1px solid var(--admin-line);
  border-radius: 8px;
  background: #f8faf7;
}

.payment-summary-card span,
.payment-summary-card small {
  display: block;
  color: var(--admin-muted);
}

.payment-summary-card strong {
  display: block;
  margin: 8px 0 4px;
  font-size: 26px;
}

.payment-toolbar :deep(.el-date-editor) {
  width: 360px;
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
.reference-cell {
  display: grid;
  gap: 2px;
}

.order-cell strong,
.reference-cell strong {
  font-size: 14px;
}

.order-cell span,
.reference-cell span,
.reference-cell small {
  color: var(--admin-muted);
  font-size: 12px;
}

.reference-cell strong,
.reference-cell span,
.reference-cell small {
  min-width: 0;
  overflow-wrap: anywhere;
}

@media (max-width: 900px) {
  .payment-summary {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .payment-toolbar :deep(.el-date-editor) {
    width: 100%;
  }
}
</style>
