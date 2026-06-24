<template>
  <section class="admin-card admin-card-pad">
    <div class="ops-header">
      <div>
        <p class="eyebrow">營運隊列</p>
        <h2>訂單履約工作台</h2>
        <span>先處理待確認，再推進配送與完成，避免訂單卡在中間狀態。</span>
      </div>
      <div class="ops-header-actions">
        <el-button @click="clearFilters">清除條件</el-button>
        <el-button type="primary" plain @click="loadData">重新整理</el-button>
      </div>
    </div>

    <div class="order-stats">
      <button
        v-for="item in statCards"
        :key="item.label"
        class="order-stat"
        :class="{ active: query.status === item.status }"
        type="button"
        @click="filterByStatus(item.status)"
      >
        <span>{{ item.label }}</span>
        <strong>{{ item.value }}</strong>
        <small>{{ item.caption }}</small>
      </button>
    </div>

    <div class="table-toolbar">
      <el-input v-model="query.number" clearable placeholder="搜尋訂單編號" @keyup.enter="loadData" />
      <el-select v-model="query.status" clearable placeholder="訂單狀態">
        <el-option v-for="item in statuses" :key="item.value" :label="item.label" :value="item.value" />
      </el-select>
      <el-button type="primary" @click="loadData">查詢</el-button>
    </div>

    <el-alert
      v-if="activeStatusLabel"
      class="filter-alert"
      type="info"
      show-icon
      :closable="false"
      :title="`目前篩選：${activeStatusLabel}。${activeStatusHint}`"
    />

    <el-table v-loading="loading" :data="rows" stripe>
      <el-table-column label="訂單" min-width="180">
        <template #default="{ row }">
          <div class="order-identity">
            <strong>{{ row.number }}</strong>
            <span>{{ orderKind(row) }}</span>
          </div>
        </template>
      </el-table-column>
      <el-table-column prop="consignee" label="收件人" width="100" />
      <el-table-column prop="phone" label="電話" width="118" />
      <el-table-column label="金額" width="100">
        <template #default="{ row }">{{ money(row.amount) }}</template>
      </el-table-column>
      <el-table-column label="狀態" width="110">
        <template #default="{ row }">
          <el-tag :type="statusType(row.status)">{{ statusText(row.status) }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="履約判斷" min-width="170">
        <template #default="{ row }">
          <div class="fulfillment-tags">
            <el-tag
              v-for="item in fulfillmentTags(row)"
              :key="item.label"
              :type="item.type"
              effect="plain"
            >
              {{ item.label }}
            </el-tag>
          </div>
        </template>
      </el-table-column>
      <el-table-column prop="orderTime" label="下單時間" width="160" />
      <el-table-column label="下一步" fixed="right" width="190">
        <template #default="{ row }">
          <div class="row-actions">
            <el-button size="small" @click="openDetail(row)">檢視</el-button>
            <el-button v-if="row.status === 2" size="small" type="primary" @click="accept(row)">確認</el-button>
            <el-button v-else-if="row.status === 3" size="small" type="primary" @click="delivery(row)">配送</el-button>
            <el-button v-else-if="row.status === 4" size="small" type="success" @click="complete(row)">完成</el-button>
            <el-dropdown v-if="hasSecondaryActions(row)" trigger="click">
              <el-button size="small" plain>更多</el-button>
              <template #dropdown>
                <el-dropdown-menu>
                  <el-dropdown-item v-if="row.status === 2" @click="openReason(row, 'reject')">婉拒</el-dropdown-item>
                  <el-dropdown-item v-if="[2, 3].includes(row.status)" @click="openReason(row, 'cancel')">取消訂單</el-dropdown-item>
                </el-dropdown-menu>
              </template>
            </el-dropdown>
          </div>
        </template>
      </el-table-column>
    </el-table>

    <el-pagination
      v-model:current-page="page.page"
      class="pager"
      background
      layout="total, prev, pager, next"
      :total="page.total"
      @current-change="loadData"
    />

    <el-dialog v-model="detailVisible" title="訂單詳情" width="720px">
      <el-descriptions v-if="detail" :column="2" border>
        <el-descriptions-item label="訂單編號">{{ detail.number }}</el-descriptions-item>
        <el-descriptions-item label="狀態">{{ statusText(detail.status) }}</el-descriptions-item>
        <el-descriptions-item label="收件人">{{ detail.consignee }}</el-descriptions-item>
        <el-descriptions-item label="電話">{{ detail.phone }}</el-descriptions-item>
        <el-descriptions-item label="地址" :span="2">{{ detail.address }}</el-descriptions-item>
        <el-descriptions-item label="金額">{{ detail.amount }}</el-descriptions-item>
        <el-descriptions-item label="下單時間">{{ detail.orderTime }}</el-descriptions-item>
        <el-descriptions-item v-if="detail.remark" label="備註" :span="2">{{ detail.remark }}</el-descriptions-item>
      </el-descriptions>
      <el-table v-if="detail?.orderDetailList?.length" :data="detail.orderDetailList" class="detail-table">
        <el-table-column prop="name" label="品項" />
        <el-table-column prop="number" label="數量" width="90" />
        <el-table-column prop="amount" label="金額" width="120" />
      </el-table>
    </el-dialog>

    <el-dialog v-model="reasonVisible" :title="reasonMode === 'reject' ? '婉拒原因' : '取消原因'" width="460px">
      <el-input v-model="reason" type="textarea" :rows="4" placeholder="請輸入原因" />
      <template #footer>
        <el-button @click="reasonVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="submitReason">送出</el-button>
      </template>
    </el-dialog>
  </section>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref, watch } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { useRoute } from 'vue-router'
import {
  completeOrder,
  deliveryOrder,
  getOrderDetailPage,
  getOrderListBy,
  orderAccept,
  orderCancel,
  orderReject,
  queryOrderDetailById
} from '@/api/order'
import { readPage, useLoading, usePage } from './composables'

const route = useRoute()

const statuses = [
  { label: '待付款', value: 1 },
  { label: '待確認', value: 2 },
  { label: '已確認', value: 3 },
  { label: '配送中', value: 4 },
  { label: '已完成', value: 5 },
  { label: '已取消', value: 6 },
  { label: '退款', value: 7 },
  { label: '團購預訂', value: 8 }
]

const rows = ref<any[]>([])
const query = reactive<{ number: string; status?: number }>({ number: '' })
const page = usePage()
const { loading, withLoading } = useLoading()
const saving = ref(false)
const statistics = ref<any>({})
const detailVisible = ref(false)
const detail = ref<any>(null)
const reasonVisible = ref(false)
const reasonMode = ref<'reject' | 'cancel'>('reject')
const reason = ref('')
const activeOrder = ref<any>(null)

const statCards = computed(() => [
  { label: '待確認', value: statistics.value.toBeConfirmed ?? 0, status: 2, caption: '需要接單或婉拒' },
  { label: '已確認', value: statistics.value.confirmed ?? 0, status: 3, caption: '準備出貨配送' },
  { label: '配送中', value: statistics.value.deliveryInProgress ?? 0, status: 4, caption: '等待完成回報' }
])

function statusText(status: number) {
  return statuses.find(item => item.value === status)?.label || '未知'
}

const activeStatusLabel = computed(() => {
  return query.status ? statusText(query.status) : ''
})

const activeStatusHint = computed(() => {
  const hints: Record<number, string> = {
    2: '請優先確認是否可履約。',
    3: '請安排配送或取消例外訂單。',
    4: '請確認是否已完成送達。',
    5: '可用於核對今日完成量。',
    6: '可用於追蹤取消原因。'
  }
  return query.status ? hints[query.status] || '請依狀態檢查下一步。' : ''
})

function money(value: number | string | undefined) {
  return `$${Number(value || 0).toFixed(2)}`
}

function statusType(status: number) {
  const types: Record<number, 'success' | 'warning' | 'info' | 'danger'> = {
    1: 'warning',
    2: 'danger',
    3: 'warning',
    4: 'warning',
    5: 'success',
    6: 'info',
    7: 'info',
    8: 'warning'
  }
  return types[status] || 'info'
}

function orderKind(row: any) {
  if (row.orderType === 2 || row.type === 2 || row.status === 8) {
    return '揪團預訂'
  }
  return '一般配送'
}

function fulfillmentTags(row: any) {
  const tags: Array<{ label: string; type: 'success' | 'warning' | 'info' | 'danger' }> = []
  if (row.status === 2) {
    tags.push({ label: '優先確認', type: 'danger' })
  } else if (row.status === 3) {
    tags.push({ label: '可安排配送', type: 'warning' })
  } else if (row.status === 4) {
    tags.push({ label: '待完成回報', type: 'warning' })
  } else if (row.status === 5) {
    tags.push({ label: '已履約', type: 'success' })
  } else if (row.status === 6) {
    tags.push({ label: '已結案', type: 'info' })
  } else if (row.status === 8) {
    tags.push({ label: '等候成團', type: 'warning' })
  }

  if (!row.phone || !row.consignee) {
    tags.push({ label: '聯絡資訊不足', type: 'danger' })
  }
  if (!row.address) {
    tags.push({ label: '需核對地址', type: 'warning' })
  }
  if (tags.length === 0) {
    tags.push({ label: '待檢視', type: 'info' })
  }
  return tags
}

function hasSecondaryActions(row: any) {
  return row.status === 2 || row.status === 3
}

function filterByStatus(status: number) {
  query.status = query.status === status ? undefined : status
  page.page = 1
  void loadData()
}

function clearFilters() {
  query.number = ''
  query.status = undefined
  page.page = 1
  void loadData()
}

function getErrorMessage(error: unknown) {
  if (error instanceof Error && error.message) {
    return error.message
  }
  if (typeof error === 'string') {
    return error
  }
  return '操作失敗，請稍後再試'
}

async function confirmAction(message: string, title: string) {
  try {
    await ElMessageBox.confirm(message, title, {
      type: 'warning',
      confirmButtonText: '確定',
      cancelButtonText: '取消'
    })
    return true
  } catch (error) {
    if (error === 'cancel' || error === 'close') {
      return false
    }
    throw error
  }
}

async function loadData() {
  await withLoading(async () => {
    const response = await getOrderDetailPage({
      number: query.number || undefined,
      status: query.status,
      page: page.page,
      pageSize: page.pageSize
    })
    const result = readPage(response)
    rows.value = result.records
    page.total = result.total
  })
  await loadStats()
}

function applyRouteQuery() {
  const routeStatus = Number(route.query.status)
  query.status = Number.isFinite(routeStatus) && routeStatus > 0 ? routeStatus : undefined
}

async function loadStats() {
  const response = await getOrderListBy({})
  statistics.value = response.data?.data || {}
}

async function openDetail(row: any) {
  const response = await queryOrderDetailById({ orderId: row.id })
  detail.value = response.data?.data
  detailVisible.value = true
}

async function accept(row: any) {
  if (!(await confirmAction(`確定將訂單「${row.number}」標記為已確認？`, '訂單確認'))) {
    return
  }
  try {
    await orderAccept({ id: row.id, status: 3 })
    ElMessage.success('已確認')
    await loadData()
  } catch (error) {
    ElMessage.error(getErrorMessage(error))
  }
}

function openReason(row: any, mode: 'reject' | 'cancel') {
  activeOrder.value = row
  reasonMode.value = mode
  reason.value = ''
  reasonVisible.value = true
}

async function submitReason() {
  if (!reason.value.trim()) {
    ElMessage.warning('請輸入原因')
    return
  }
  saving.value = true
  try {
    if (reasonMode.value === 'reject') {
      await orderReject({ id: activeOrder.value.id, rejectionReason: reason.value.trim() })
      ElMessage.success('已婉拒')
    } else {
      await orderCancel({ id: activeOrder.value.id, cancelReason: reason.value.trim() })
      ElMessage.success('訂單已取消')
    }
    reasonVisible.value = false
    await loadData()
  } catch (error) {
    ElMessage.error(getErrorMessage(error))
  } finally {
    saving.value = false
  }
}

async function delivery(row: any) {
  if (!(await confirmAction(`確定開始配送「${row.number}」？`, '配送確認'))) {
    return
  }
  try {
    await deliveryOrder({ id: row.id })
    ElMessage.success('已進入配送')
    await loadData()
  } catch (error) {
    ElMessage.error(getErrorMessage(error))
  }
}

async function complete(row: any) {
  if (!(await confirmAction(`確定完成「${row.number}」？`, '完成確認'))) {
    return
  }
  try {
    await completeOrder({ id: row.id })
    ElMessage.success('訂單已完成')
    await loadData()
  } catch (error) {
    ElMessage.error(getErrorMessage(error))
  }
}

watch(
  () => route.query.status,
  async () => {
    applyRouteQuery()
    page.page = 1
    await loadData()
  }
)

onMounted(async () => {
  applyRouteQuery()
  await loadData()
})
</script>

<style scoped>
.order-stats {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 10px;
  margin-bottom: 14px;
}

.ops-header {
  display: flex;
  justify-content: space-between;
  gap: 18px;
  align-items: flex-start;
  margin-bottom: 14px;
}

.ops-header h2 {
  margin: 0;
  font-size: 22px;
  line-height: 1.25;
}

.ops-header span {
  display: block;
  margin-top: 6px;
  color: var(--admin-muted);
  font-size: 13px;
}

.ops-header-actions {
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

.order-stat {
  width: 100%;
  padding: 14px;
  border: 1px solid var(--admin-line);
  border-radius: 6px;
  background: #f8faf7;
  color: var(--admin-ink);
  text-align: left;
  cursor: pointer;
  transition: border-color 0.18s ease, background-color 0.18s ease, box-shadow 0.18s ease;
}

.order-stat:hover,
.order-stat.active {
  border-color: rgba(45, 106, 79, 0.34);
  background: #f1f6f0;
}

.order-stat.active {
  box-shadow: inset 3px 0 0 var(--admin-green);
}

.order-stat span,
.order-stat small {
  display: block;
  color: var(--admin-muted);
}

.order-stat strong {
  display: block;
  margin: 6px 0 3px;
  font-size: 24px;
}

.filter-alert {
  margin-bottom: 14px;
}

.order-identity {
  display: grid;
  gap: 4px;
}

.order-identity strong {
  font-weight: 800;
}

.order-identity span {
  color: var(--admin-muted);
  font-size: 12px;
}

.fulfillment-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
}

.row-actions {
  display: flex;
  gap: 6px;
  align-items: center;
}

.detail-table {
  margin-top: 18px;
}

@media (max-width: 900px) {
  .ops-header {
    display: grid;
  }

  .ops-header-actions {
    justify-content: flex-start;
  }

  .order-stats {
    grid-template-columns: repeat(3, minmax(0, 1fr));
  }

  .order-stat {
    padding: 12px;
  }

  .order-stat strong {
    font-size: 22px;
  }
}
</style>
