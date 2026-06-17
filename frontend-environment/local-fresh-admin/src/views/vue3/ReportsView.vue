<template>
  <section v-loading="loading" class="report-page">
    <div class="admin-card admin-card-pad report-toolbar">
      <div>
        <p class="eyebrow">報表</p>
        <h2>營運資料統計</h2>
      </div>
      <div class="report-actions">
        <el-date-picker
          v-model="range"
          type="daterange"
          range-separator="至"
          start-placeholder="開始日期"
          end-placeholder="結束日期"
          value-format="YYYY-MM-DD"
        />
        <el-button type="primary" @click="loadReports">查詢</el-button>
        <el-button plain @click="exportReport">匯出 Excel</el-button>
      </div>
    </div>

    <div class="report-metrics">
      <article v-for="item in summaryCards" :key="item.label" class="admin-card admin-card-pad metric-card">
        <p>{{ item.label }}</p>
        <strong>{{ item.value }}</strong>
        <span>{{ item.caption }}</span>
      </article>
    </div>

    <article class="admin-card admin-card-pad">
      <div class="section-title">
        <div>
          <p class="eyebrow">趨勢</p>
          <h2>營業額趨勢</h2>
        </div>
      </div>
      <div class="trend-list">
        <div v-for="item in turnoverSeries" :key="item.date" class="trend-row">
          <span>{{ item.date }}</span>
          <div class="trend-bar">
            <i :style="{ width: `${item.percent}%` }" />
          </div>
          <strong>{{ money(item.value) }}</strong>
        </div>
      </div>
    </article>

    <article class="admin-card admin-card-pad">
      <div class="section-title">
        <div>
          <p class="eyebrow">排行</p>
          <h2>商品銷量排行</h2>
        </div>
      </div>
      <el-table :data="topRows" stripe>
        <el-table-column type="index" label="#" width="64" />
        <el-table-column prop="name" label="商品名稱" />
        <el-table-column prop="count" label="銷量" width="140" />
      </el-table>
    </article>
  </section>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import {
  exportInfor,
  getOrderStatistics,
  getTop,
  getTurnoverStatistics,
  getUserStatistics
} from '@/api'

const loading = ref(false)
const range = ref<[string, string]>(defaultRange())
const turnoverReport = ref<any>({})
const orderReport = ref<any>({})
const userReport = ref<any>({})
const topReport = ref<any>({})

const summaryCards = computed(() => [
  { label: '訂單總數', value: orderReport.value.totalOrderCount ?? 0, caption: '區間內全部訂單' },
  { label: '有效訂單', value: orderReport.value.validOrderCount ?? 0, caption: '已完成訂單' },
  { label: '訂單完成率', value: percent(orderReport.value.orderCompletionRate), caption: '有效訂單 / 全部訂單' },
  { label: '新增會員', value: latest(csvNumbers(userReport.value.newUserList)), caption: '區間最後一天新增' }
])

const turnoverSeries = computed(() => {
  const dates = csv(turnoverReport.value.dateList)
  const values = csvNumbers(turnoverReport.value.turnoverList)
  const max = Math.max(...values, 0)
  return dates.map((date, index) => ({
    date,
    value: values[index] || 0,
    percent: max ? Math.max(4, ((values[index] || 0) / max) * 100) : 0
  }))
})

const topRows = computed(() => {
  const names = csv(topReport.value.nameList)
  const counts = csvNumbers(topReport.value.numberList)
  return names.map((name, index) => ({ name, count: counts[index] || 0 }))
})

function defaultRange(): [string, string] {
  const end = new Date()
  const begin = new Date()
  begin.setDate(end.getDate() - 6)
  return [formatDate(begin), formatDate(end)]
}

function formatDate(date: Date) {
  const year = date.getFullYear()
  const month = String(date.getMonth() + 1).padStart(2, '0')
  const day = String(date.getDate()).padStart(2, '0')
  return `${year}-${month}-${day}`
}

function csv(value: string | undefined) {
  return value ? value.split(',').filter(Boolean) : []
}

function csvNumbers(value: string | undefined) {
  return csv(value).map((item) => Number(item) || 0)
}

function latest(values: number[]) {
  return values.length ? values[values.length - 1] : 0
}

function money(value: number | undefined) {
  return `$${Number(value || 0).toFixed(2)}`
}

function percent(value: number | undefined) {
  return `${(Number(value || 0) * 100).toFixed(2)}%`
}

async function loadReports() {
  const [begin, end] = range.value
  loading.value = true
  try {
    const params = { begin, end }
    const [turnover, orders, users, top] = await Promise.all([
      getTurnoverStatistics(params),
      getOrderStatistics(params),
      getUserStatistics(params),
      getTop(params)
    ])
    turnoverReport.value = turnover.data?.data || {}
    orderReport.value = orders.data?.data || {}
    userReport.value = users.data?.data || {}
    topReport.value = top.data?.data || {}
  } finally {
    loading.value = false
  }
}

async function exportReport() {
  const response = await exportInfor()
  const contentType = String(response.headers['content-type'] || 'application/octet-stream')
  const blob = new Blob([response.data], { type: contentType })
  const url = URL.createObjectURL(blob)
  const link = document.createElement('a')
  link.href = url
  link.download = `business-report-${range.value[0]}-${range.value[1]}.xlsx`
  link.click()
  URL.revokeObjectURL(url)
  ElMessage.success('報表已開始下載')
}

onMounted(loadReports)
</script>

<style scoped>
.report-page {
  display: grid;
  gap: 18px;
}

.report-toolbar,
.report-actions,
.section-title {
  display: flex;
  justify-content: space-between;
  gap: 16px;
  align-items: center;
}

.report-metrics {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 18px;
}

.metric-card p,
.metric-card span {
  color: var(--admin-muted);
}

.metric-card strong {
  display: block;
  margin: 12px 0 8px;
  font-size: 28px;
}

.eyebrow {
  margin: 0 0 8px;
  color: var(--admin-gold);
  font-weight: 800;
  letter-spacing: 0.14em;
  text-transform: uppercase;
}

h2 {
  margin: 0;
}

.trend-list {
  display: grid;
  gap: 12px;
}

.trend-row {
  display: grid;
  grid-template-columns: 110px 1fr 110px;
  gap: 12px;
  align-items: center;
}

.trend-bar {
  height: 12px;
  overflow: hidden;
  border-radius: 999px;
  background: #ece3c8;
}

.trend-bar i {
  display: block;
  height: 100%;
  border-radius: inherit;
  background: linear-gradient(90deg, #355d3d, #f0c85a);
}

@media (max-width: 980px) {
  .report-toolbar,
  .report-actions,
  .section-title {
    align-items: flex-start;
    flex-direction: column;
  }

  .report-metrics {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}
</style>
