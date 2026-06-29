<template>
  <section class="admin-card admin-card-pad">
    <div class="audit-summary">
      <div v-for="item in summaryCards" :key="item.label" class="audit-summary-card">
        <span>{{ item.label }}</span>
        <strong>{{ item.value }}</strong>
        <small>{{ item.caption }}</small>
      </div>
    </div>

    <div class="table-toolbar audit-toolbar">
      <el-select v-model="query.action" clearable placeholder="操作類型">
        <el-option v-for="item in actionOptions" :key="item.value" :label="item.label" :value="item.value" />
      </el-select>
      <el-select v-model="query.targetType" clearable placeholder="目標類型">
        <el-option v-for="item in targetOptions" :key="item.value" :label="item.label" :value="item.value" />
      </el-select>
      <el-input v-model="query.targetId" clearable placeholder="目標 ID" @keyup.enter="search" />
      <el-input v-model="query.operatorId" clearable placeholder="操作者 ID" @keyup.enter="search" />
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

    <el-table v-loading="loading" :data="rows" stripe>
      <el-table-column label="操作" width="150">
        <template #default="{ row }">
          <el-tag :type="actionTagType(row.action)" effect="plain">{{ actionLabel(row.action) }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="目標" min-width="150">
        <template #default="{ row }">
          <div class="target-cell">
            <strong>{{ targetLabel(row.targetType) }}</strong>
            <span>#{{ row.targetId }}</span>
          </div>
        </template>
      </el-table-column>
      <el-table-column label="前後值" min-width="150">
        <template #default="{ row }">
          <span>{{ valueText(row.beforeValue) }} → {{ valueText(row.afterValue) }}</span>
        </template>
      </el-table-column>
      <el-table-column label="原因" min-width="180">
        <template #default="{ row }">{{ row.reason || '-' }}</template>
      </el-table-column>
      <el-table-column label="操作者" width="150">
        <template #default="{ row }">{{ operatorLabel(row) }}</template>
      </el-table-column>
      <el-table-column label="時間" min-width="180">
        <template #default="{ row }">{{ formatDateTime(row.createdAt) }}</template>
      </el-table-column>
    </el-table>

    <el-empty v-if="!loading && rows.length === 0" description="尚無符合條件的操作紀錄" />

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
import { getOperationLogPage } from '@/api/operationLog'
import { readPage, useLoading, usePage } from './composables'

const actionOptions = [
  { label: '訂單確認', value: 'ORDER_CONFIRM' },
  { label: '訂單婉拒', value: 'ORDER_REJECT' },
  { label: '訂單取消', value: 'ORDER_CANCEL' },
  { label: '訂單配送', value: 'ORDER_START_DELIVERY' },
  { label: '訂單完成', value: 'ORDER_COMPLETE' },
  { label: '庫存調整', value: 'PRODUCT_INVENTORY_ADJUST' }
]

const inventoryActions = new Set(['PRODUCT_INVENTORY_ADJUST', 'INVENTORY_ADJUST'])

const targetOptions = [
  { label: '訂單', value: 'ORDER' },
  { label: '商品', value: 'PRODUCT' }
]

const rows = ref<any[]>([])
const page = usePage()
const { loading, withLoading } = useLoading()
const query = reactive({
  action: '',
  targetType: '',
  targetId: '',
  operatorId: '',
  timeRange: [] as string[] | null
})

const summaryCards = computed(() => {
  const inventoryAdjustments = rows.value.filter((item) => inventoryActions.has(item.action)).length
  const orderChanges = rows.value.filter((item) => item.targetType === 'ORDER').length
  const operatorCount = new Set(rows.value.map((item) => item.operatorId).filter(Boolean)).size

  return [
    { label: '本頁操作', value: rows.value.length, caption: '目前查詢結果' },
    { label: '訂單異動', value: orderChanges, caption: '確認、婉拒、配送等' },
    { label: '庫存調整', value: inventoryAdjustments, caption: '人工補貨或盤點' },
    { label: '操作者', value: operatorCount, caption: '本頁不同管理員' }
  ]
})

async function loadData() {
  await withLoading(async () => {
    const [beginTime, endTime] = query.timeRange || []
    const response = await getOperationLogPage({
      page: page.page,
      pageSize: page.pageSize,
      action: query.action || undefined,
      targetType: query.targetType || undefined,
      targetId: numericParam(query.targetId),
      operatorId: numericParam(query.operatorId),
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
    action: '',
    targetType: '',
    targetId: '',
    operatorId: '',
    timeRange: []
  })
  page.page = 1
  await loadData()
}

function numericParam(value: string) {
  const trimmed = value.trim()
  if (!trimmed) {
    return undefined
  }
  const numberValue = Number(trimmed)
  return Number.isFinite(numberValue) ? numberValue : undefined
}

function actionLabel(action: string) {
  if (action === 'INVENTORY_ADJUST') {
    return '庫存調整'
  }
  if (action === 'ORDER_DELIVERY') {
    return '訂單配送'
  }
  return actionOptions.find((item) => item.value === action)?.label || action || '-'
}

function targetLabel(targetType: string) {
  return targetOptions.find((item) => item.value === targetType)?.label || targetType || '-'
}

function actionTagType(action: string) {
  if (inventoryActions.has(action)) {
    return 'warning'
  }
  if (action === 'ORDER_REJECT' || action === 'ORDER_CANCEL') {
    return 'danger'
  }
  if (action === 'ORDER_COMPLETE') {
    return 'success'
  }
  return 'primary'
}

function valueText(value?: string) {
  return value || '-'
}

function operatorLabel(row: any) {
  const labels: Record<string, string> = {
    ADMIN: '管理員',
    SYSTEM: '系統'
  }
  const type = labels[row.operatorType] || row.operatorType || '-'
  return row.operatorId ? `${type} #${row.operatorId}` : type
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
.audit-summary {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 12px;
  margin-bottom: 18px;
}

.audit-summary-card {
  padding: 16px;
  border: 1px solid var(--admin-line);
  border-radius: 8px;
  background: #f8faf7;
}

.audit-summary-card span,
.audit-summary-card small {
  display: block;
  color: var(--admin-muted);
}

.audit-summary-card strong {
  display: block;
  margin: 8px 0 4px;
  font-size: 26px;
}

.audit-toolbar :deep(.el-date-editor) {
  width: 360px;
}

.target-cell {
  display: grid;
  gap: 2px;
}

.target-cell strong {
  font-size: 14px;
}

.target-cell span {
  color: var(--admin-muted);
  font-size: 12px;
}

@media (max-width: 900px) {
  .audit-summary {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .audit-toolbar :deep(.el-date-editor) {
    width: 100%;
  }
}
</style>
