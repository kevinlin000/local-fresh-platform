<template>
  <section class="admin-card admin-card-pad">
    <div class="order-stats">
      <div v-for="item in statCards" :key="item.label" class="order-stat">
        <span>{{ item.label }}</span>
        <strong>{{ item.value }}</strong>
      </div>
    </div>

    <div class="table-toolbar">
      <el-input v-model="query.number" clearable placeholder="搜尋訂單編號" @keyup.enter="loadData" />
      <el-select v-model="query.status" clearable placeholder="訂單狀態">
        <el-option v-for="item in statuses" :key="item.value" :label="item.label" :value="item.value" />
      </el-select>
      <el-button type="primary" @click="loadData">查詢</el-button>
    </div>

    <el-table v-loading="loading" :data="rows" stripe>
      <el-table-column prop="number" label="訂單編號" min-width="190" />
      <el-table-column prop="consignee" label="收件人" min-width="110" />
      <el-table-column prop="phone" label="電話" min-width="130" />
      <el-table-column prop="amount" label="金額" width="110" />
      <el-table-column label="狀態" width="130">
        <template #default="{ row }">
          <el-tag>{{ statusText(row.status) }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="orderTime" label="下單時間" min-width="180" />
      <el-table-column label="操作" fixed="right" width="280">
        <template #default="{ row }">
          <el-button link type="primary" @click="openDetail(row)">詳情</el-button>
          <el-button v-if="row.status === 2" link type="success" @click="accept(row)">確認</el-button>
          <el-button v-if="row.status === 2" link type="danger" @click="openReason(row, 'reject')">婉拒</el-button>
          <el-button v-if="[2, 3].includes(row.status)" link type="warning" @click="openReason(row, 'cancel')">取消</el-button>
          <el-button v-if="row.status === 3" link type="primary" @click="delivery(row)">配送</el-button>
          <el-button v-if="row.status === 4" link type="success" @click="complete(row)">完成</el-button>
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
  { label: '待確認', value: statistics.value.toBeConfirmed ?? 0 },
  { label: '已確認', value: statistics.value.confirmed ?? 0 },
  { label: '配送中', value: statistics.value.deliveryInProgress ?? 0 }
])

function statusText(status: number) {
  return statuses.find(item => item.value === status)?.label || '未知'
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
  gap: 12px;
  margin-bottom: 18px;
}

.order-stat {
  padding: 18px;
  border: 1px solid var(--admin-line);
  border-radius: 8px;
  background: #f8faf7;
}

.order-stat span {
  display: block;
  color: var(--admin-muted);
}

.order-stat strong {
  display: block;
  margin-top: 8px;
  font-size: 26px;
}

.detail-table {
  margin-top: 18px;
}
</style>
