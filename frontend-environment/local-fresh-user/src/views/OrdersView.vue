<template>
  <section class="orders-shell">
    <div class="orders-card">
      <div class="section-header">
        <div>
          <p class="eyebrow">我的訂單</p>
          <h1>訂單與揪團紀錄</h1>
          <p class="section-copy">追蹤付款、門市確認、配送與完成狀態；待付款訂單可直接接續付款流程。</p>
        </div>
        <el-button text type="success" @click="refreshCurrentTab">重新整理</el-button>
      </div>

      <el-tabs v-model="activeTab" class="orders-tabs">
        <el-tab-pane label="一般訂單" name="orders" />
        <el-tab-pane label="我的揪團" name="group-buys" />
      </el-tabs>

      <template v-if="activeTab === 'orders'">
        <el-skeleton v-if="loadingOrders" :rows="10" animated />

        <el-empty v-else-if="!orders.length" description="目前還沒有訂單紀錄">
          <el-button type="success" @click="goHome">回首頁逛逛</el-button>
        </el-empty>

        <template v-else>
          <div class="order-summary-strip" aria-label="訂單摘要">
            <div>
              <span>全部訂單</span>
              <strong>{{ orderSummary.total }}</strong>
            </div>
            <div>
              <span>待付款</span>
              <strong>{{ orderSummary.pendingPayment }}</strong>
            </div>
            <div>
              <span>處理中</span>
              <strong>{{ orderSummary.inProgress }}</strong>
            </div>
            <div>
              <span>已完成</span>
              <strong>{{ orderSummary.completed }}</strong>
            </div>
          </div>

          <div class="order-list">
            <article v-for="order in orders" :key="order.id" class="order-card clickable-card" @click="openOrderDetail(order.id)">
            <div class="order-head">
              <div>
                <div class="order-number">訂單編號 {{ order.number }}</div>
                <div class="order-meta">
                  <span>{{ formatDate(order.orderTime) }}</span>
                  <span>{{ order.consignee }}</span>
                  <span>{{ order.phone }}</span>
                </div>
              </div>
              <div class="order-status-block">
                <el-tag :type="statusType(order.status)" effect="plain">
                  {{ orderStatusText(order.status) }}
                </el-tag>
                <span>{{ paymentText(order) }}</span>
                <strong>NT$ {{ formatPrice(order.amount) }}</strong>
              </div>
            </div>

            <div class="order-tracking" aria-label="訂單進度">
              <span
                v-for="step in orderProgressSteps"
                :key="step.key"
                :class="{ active: statusStepIndex(order.status) >= step.index }"
              >
                {{ step.label }}
              </span>
            </div>

            <div class="order-fulfillment">
              <span>{{ deliveryText(order) }}</span>
              <span>{{ orderPrimaryItem(order) }}</span>
              <span>{{ orderItemCount(order) }} 件商品</span>
            </div>

            <div class="order-address">
              {{ order.consignee }} · {{ order.address }}
            </div>

            <div class="order-items">
              <div v-for="detail in order.orderDetailList" :key="detail.id" class="order-item-row">
                <div>
                  <strong>{{ detail.name }}</strong>
                  <span v-if="detail.productSpec" class="spec">{{ detail.productSpec }}</span>
                </div>
                <div class="order-item-side">
                  <span>x{{ detail.number }}</span>
                  <span>NT$ {{ formatPrice(detail.amount) }}</span>
                </div>
              </div>
            </div>

            <p v-if="order.remark" class="order-remark">備註：{{ order.remark }}</p>

            <div v-if="order.status === 1" class="order-actions">
              <el-button
                type="success"
                :loading="payingOrderNumber === order.number"
                @click.stop="handlePayOrder(order.number)"
              >
                前往付款
              </el-button>
            </div>
          </article>
          </div>
        </template>
      </template>

      <template v-else>
        <el-skeleton v-if="loadingGroupBuys" :rows="8" animated />

        <el-empty v-else-if="!groupBuys.length" description="目前還沒有參與任何揪團">
          <el-button type="success" @click="goHome">回首頁找商品</el-button>
        </el-empty>

        <div v-else class="groupbuy-list">
          <article
            v-for="groupBuy in groupBuys"
            :key="groupBuy.groupNo"
            class="groupbuy-card"
            @click="openGroupBuy(groupBuy.groupNo)"
          >
            <div class="groupbuy-image">
              <img v-if="groupBuy.productImage" :src="groupBuy.productImage" :alt="groupBuy.productName || '揪團商品'" />
              <div v-else class="image-placeholder">暫無圖片</div>
            </div>

            <div class="groupbuy-body">
              <div class="groupbuy-head">
                <div>
                  <div class="order-number">揪團編號 {{ groupBuy.groupNo }}</div>
                  <h2>{{ groupBuy.productName || '揪團商品' }}</h2>
                </div>
                <el-tag :type="groupBuyStatusType(groupBuy.status)" effect="plain">
                  {{ groupBuyStatusText(groupBuy.status) }}
                </el-tag>
              </div>

              <div class="groupbuy-meta">
                <span>數量 {{ groupBuy.quantity || 1 }} 件</span>
                <span>{{ groupBuy.currentCount }}/{{ groupBuy.requiredCount }} 人</span>
                <span>截止 {{ formatDate(groupBuy.expireAt) }}</span>
              </div>

              <div class="participants">
                <span
                  v-for="participant in groupBuy.participants.slice(0, 3)"
                  :key="`${groupBuy.groupNo}-${participant.memberId}`"
                  class="participant-chip"
                >
                  {{ participant.memberName }}
                </span>
              </div>
            </div>
          </article>
        </div>
      </template>
    </div>
  </section>

  <el-dialog v-model="orderDetailVisible" title="訂單詳情" width="640px">
    <el-skeleton v-if="loadingOrderDetail" :rows="8" animated />

    <div v-else-if="selectedOrder" class="order-detail-panel">
      <div class="order-detail-grid">
        <div class="detail-item">
          <span>訂單編號</span>
          <strong>{{ selectedOrder.number }}</strong>
        </div>
        <div class="detail-item">
          <span>狀態</span>
          <strong>{{ orderStatusText(selectedOrder.status) }}</strong>
        </div>
        <div class="detail-item">
          <span>下單時間</span>
          <strong>{{ formatDate(selectedOrder.orderTime) }}</strong>
        </div>
        <div class="detail-item">
          <span>金額</span>
          <strong>NT$ {{ formatPrice(selectedOrder.amount) }}</strong>
        </div>
      </div>

      <div class="detail-timeline" aria-label="訂單進度">
        <span
          v-for="step in orderProgressSteps"
          :key="step.key"
          :class="{ active: statusStepIndex(selectedOrder.status) >= step.index }"
        >
          {{ step.label }}
        </span>
      </div>

      <div class="detail-section">
        <h3>商品列表</h3>
        <div class="detail-list">
          <div
            v-for="detail in selectedOrder.orderDetailList"
            :key="detail.id"
            class="detail-list-row"
          >
            <div>
              <strong>{{ detail.name }}</strong>
              <span v-if="detail.productSpec" class="spec">{{ detail.productSpec }}</span>
            </div>
            <div class="order-item-side">
              <span>x{{ detail.number }}</span>
              <span>NT$ {{ formatPrice(detail.amount) }}</span>
            </div>
          </div>
        </div>
      </div>

      <div class="detail-section">
        <h3>配送資訊</h3>
        <p>{{ selectedOrder.consignee }} {{ selectedOrder.phone }}</p>
        <p>{{ selectedOrder.address }}</p>
      </div>

      <div class="detail-section">
        <h3>備註</h3>
        <p>{{ selectedOrder.remark || '無' }}</p>
      </div>

      <div v-if="selectedOrder.cancelReason" class="detail-section">
        <h3>取消理由</h3>
        <p>{{ selectedOrder.cancelReason }}</p>
      </div>

      <div v-if="selectedOrder.rejectionReason" class="detail-section">
        <h3>拒單理由</h3>
        <p>{{ selectedOrder.rejectionReason }}</p>
      </div>
    </div>
  </el-dialog>
</template>

<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue'
import { ElMessage } from 'element-plus/es/components/message/index'
import { useRoute, useRouter } from 'vue-router'
import { fetchMyGroupBuys, type GroupBuyRecord } from '@/services/groupBuy'
import { fetchOrderDetail, fetchOrderHistory, payOrder, type OrderRecord } from '@/services/order'
import { isEcpayPaymentResponse, redirectToEcpayCheckout } from '@/services/paymentRedirect'

const route = useRoute()
const router = useRouter()

const activeTab = ref<'orders' | 'group-buys'>(route.query.tab === 'group-buy' ? 'group-buys' : 'orders')
const loadingOrders = ref(false)
const loadingGroupBuys = ref(false)
const orders = ref<OrderRecord[]>([])
const groupBuys = ref<GroupBuyRecord[]>([])
const payingOrderNumber = ref<string | null>(null)
const orderDetailVisible = ref(false)
const loadingOrderDetail = ref(false)
const selectedOrder = ref<OrderRecord | null>(null)

const orderProgressSteps = [
  { key: 'placed', label: '已下單', index: 0 },
  { key: 'paid', label: '付款/確認', index: 1 },
  { key: 'shipping', label: '配送中', index: 2 },
  { key: 'completed', label: '完成', index: 3 }
]

const orderSummary = computed(() => ({
  total: orders.value.length,
  pendingPayment: orders.value.filter((order) => order.status === 1).length,
  inProgress: orders.value.filter((order) => [2, 3, 4, 8].includes(order.status)).length,
  completed: orders.value.filter((order) => order.status === 5).length
}))

function formatPrice(value: number) {
  return Number(value || 0).toLocaleString('zh-TW')
}

function formatDate(value: string) {
  if (!value) {
    return '—'
  }
  return new Date(value).toLocaleString('zh-TW', {
    hour12: false
  })
}

function orderStatusText(status: number) {
  switch (status) {
    case 1:
      return '待付款'
    case 2:
      return '待確認'
    case 3:
      return '已確認'
    case 4:
      return '配送中'
    case 5:
      return '已完成'
    case 6:
      return '已取消'
    case 8:
      return '揪團中'
    default:
      return `狀態 ${status}`
  }
}

function groupBuyStatusText(status: number) {
  switch (status) {
    case 1:
      return '揪團中'
    case 2:
      return '已成團'
    case 3:
      return '已失敗'
    case 4:
      return '已完成'
    default:
      return `狀態 ${status}`
  }
}

function groupBuyStatusType(status: number) {
  switch (status) {
    case 2:
      return 'success'
    case 3:
      return 'danger'
    case 4:
      return 'info'
    default:
      return 'warning'
  }
}

function statusType(status: number) {
  switch (status) {
    case 2:
    case 3:
      return 'success'
    case 4:
      return 'warning'
    case 5:
      return 'info'
    case 6:
      return 'danger'
    default:
      return 'primary'
  }
}

function statusStepIndex(status: number) {
  switch (status) {
    case 1:
      return 0
    case 2:
    case 3:
    case 8:
      return 1
    case 4:
      return 2
    case 5:
      return 3
    default:
      return 0
  }
}

function paymentText(order: OrderRecord) {
  if (order.payStatus === 1) {
    return '已付款'
  }
  if (order.status === 1) {
    return '等待付款'
  }
  if (order.status === 6) {
    return '已取消'
  }
  return '待門市確認'
}

function deliveryText(order: OrderRecord) {
  if (order.status === 4) {
    return '配送中'
  }
  if (order.status === 5) {
    return '配送完成'
  }
  if (order.status === 6) {
    return '訂單已取消'
  }
  return '門市確認配送'
}

function orderItemCount(order: OrderRecord) {
  return order.orderDetailList.reduce((sum, item) => sum + item.number, 0)
}

function orderPrimaryItem(order: OrderRecord) {
  const firstItem = order.orderDetailList[0]
  if (!firstItem) {
    return '商品整理中'
  }
  const extraCount = Math.max(order.orderDetailList.length - 1, 0)
  return extraCount > 0 ? `${firstItem.name} 等 ${order.orderDetailList.length} 項` : firstItem.name
}

async function loadOrders() {
  loadingOrders.value = true
  try {
    const result = await fetchOrderHistory({
      page: 1,
      pageSize: 20
    })
    orders.value = result.records
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '載入訂單列表失敗')
  } finally {
    loadingOrders.value = false
  }
}

async function loadGroupBuys() {
  loadingGroupBuys.value = true
  try {
    groupBuys.value = await fetchMyGroupBuys()
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '載入揪團列表失敗')
  } finally {
    loadingGroupBuys.value = false
  }
}

async function refreshCurrentTab() {
  if (activeTab.value === 'orders') {
    await loadOrders()
    return
  }
  await loadGroupBuys()
}

async function handlePayOrder(orderNumber: string) {
  payingOrderNumber.value = orderNumber
  try {
    const payment = await payOrder(orderNumber, 1)
    if (isEcpayPaymentResponse(payment)) {
      ElMessage.info('前往付款頁')
      redirectToEcpayCheckout(payment)
      return
    }
    ElMessage.success('付款成功')
    await loadOrders()
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '付款失敗')
  } finally {
    payingOrderNumber.value = null
  }
}

async function openOrderDetail(orderId: number) {
  orderDetailVisible.value = true
  loadingOrderDetail.value = true
  try {
    selectedOrder.value = await fetchOrderDetail(orderId)
  } catch (error) {
    orderDetailVisible.value = false
    ElMessage.error(error instanceof Error ? error.message : '載入訂單詳情失敗')
  } finally {
    loadingOrderDetail.value = false
  }
}

function openGroupBuy(groupNo: string) {
  void router.push(`/groupBuy/${groupNo}`)
}

function goHome() {
  void router.push('/')
}

watch(activeTab, async (tab) => {
  await router.replace({
    path: '/orders',
    query: tab === 'group-buys' ? { tab: 'group-buy' } : {}
  })
  await refreshCurrentTab()
})

onMounted(async () => {
  await refreshCurrentTab()
})
</script>

<style scoped>
.orders-shell {
  max-width: 1180px;
  margin: 0 auto;
  padding: 18px 0 48px;
}

.orders-card {
  padding: 20px;
  border: 1px solid var(--farm-line);
  border-radius: 8px;
  background: var(--farm-surface);
  box-shadow: 0 6px 18px rgba(28, 39, 32, 0.055);
}

.section-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 16px;
  margin-bottom: 12px;
}

.orders-tabs {
  margin-bottom: 16px;
}

.eyebrow {
  margin: 0 0 8px;
  color: var(--farm-accent);
  font-size: 12px;
  font-weight: 800;
}

h1,
h2 {
  margin: 0;
  color: var(--farm-text);
}

.section-copy {
  max-width: 660px;
  margin: 8px 0 0;
  color: var(--farm-muted);
  font-size: 14px;
  line-height: 1.65;
}

.order-summary-strip {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 10px;
  margin-bottom: 14px;
}

.order-summary-strip div {
  min-width: 0;
  padding: 12px;
  border: 1px solid var(--farm-line);
  border-radius: 8px;
  background: #fbfcf8;
}

.order-summary-strip span {
  display: block;
  color: var(--farm-muted);
  font-size: 12px;
  font-weight: 750;
}

.order-summary-strip strong {
  display: block;
  margin-top: 5px;
  color: var(--farm-primary-deep);
  font-size: 22px;
  line-height: 1.1;
}

.order-list,
.groupbuy-list {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.order-card,
.groupbuy-card {
  padding: 14px;
  border: 1px solid var(--farm-line);
  border-radius: 8px;
  background: #ffffff;
}

.clickable-card {
  cursor: pointer;
  transition: border-color 0.2s ease, box-shadow 0.2s ease, transform 0.2s ease;
}

.clickable-card:hover {
  border-color: rgba(83, 126, 62, 0.28);
  box-shadow: 0 8px 18px rgba(28, 39, 32, 0.07);
}

.groupbuy-card {
  display: grid;
  grid-template-columns: 112px 1fr;
  gap: 14px;
  cursor: pointer;
}

.groupbuy-image {
  overflow: hidden;
  height: 112px;
  border-radius: 8px;
  background: #edf1e9;
}

.groupbuy-image img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.image-placeholder {
  display: grid;
  place-items: center;
  width: 100%;
  height: 100%;
  color: var(--farm-muted);
  font-size: 13px;
  font-weight: 800;
}

.order-head,
.order-status-block,
.order-item-row,
.order-item-side,
.groupbuy-head {
  display: flex;
  justify-content: space-between;
  gap: 12px;
}

.order-head,
.groupbuy-head {
  align-items: flex-start;
}

.order-status-block {
  align-items: center;
  flex-wrap: wrap;
}

.order-number {
  color: var(--farm-text);
  font-weight: 800;
}

.order-meta,
.groupbuy-meta {
  display: flex;
  gap: 12px;
  margin-top: 8px;
  color: var(--farm-muted);
  font-size: 14px;
  flex-wrap: wrap;
}

.order-address {
  margin-top: 10px;
  color: var(--farm-muted);
  font-size: 14px;
  line-height: 1.55;
}

.order-items {
  display: flex;
  flex-direction: column;
  gap: 8px;
  margin-top: 12px;
  padding-top: 12px;
  border-top: 1px dashed var(--farm-line);
}

.order-item-row {
  align-items: center;
}

.participants {
  display: flex;
  gap: 10px;
  margin-top: 14px;
  flex-wrap: wrap;
}

.participant-chip {
  padding: 6px 10px;
  border-radius: 7px;
  background: var(--farm-primary-soft);
  color: var(--farm-primary-deep);
  font-size: 13px;
  font-weight: 700;
}

.spec {
  margin-left: 10px;
  color: var(--farm-muted);
  font-size: 13px;
}

.order-status-block > span,
.order-fulfillment span {
  display: inline-flex;
  align-items: center;
  min-height: 26px;
  padding: 0 8px;
  border-radius: 7px;
  background: #f1f4ef;
  color: #405047;
  font-size: 12px;
  font-weight: 760;
}

.order-status-block strong {
  color: var(--farm-primary-deep);
  font-size: 17px;
}

.order-tracking,
.detail-timeline {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 8px;
  margin-top: 14px;
  padding: 8px;
  border: 1px solid var(--farm-line);
  border-radius: 8px;
  background: #f7f8f5;
}

.order-tracking span,
.detail-timeline span {
  position: relative;
  display: flex;
  justify-content: center;
  align-items: center;
  min-height: 30px;
  border-radius: 7px;
  color: #657267;
  font-size: 12px;
  font-weight: 760;
}

.order-tracking span.active,
.detail-timeline span.active {
  background: #ffffff;
  color: var(--farm-primary-deep);
  box-shadow: 0 4px 12px rgba(28, 39, 32, 0.055);
}

.order-fulfillment {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-top: 10px;
}

.order-remark {
  margin: 14px 0 0;
  color: var(--farm-muted);
}

.order-actions {
  display: flex;
  justify-content: flex-end;
  margin-top: 16px;
}

.order-detail-panel {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.order-detail-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 12px;
}

.detail-timeline {
  margin-top: 0;
}

.detail-item,
.detail-section {
  padding: 16px 18px;
  border-radius: 8px;
  background: #f8faf7;
}

.detail-item span,
.detail-section h3 {
  display: block;
  margin: 0 0 8px;
  color: var(--farm-accent);
  font-size: 13px;
  font-weight: 800;
}

.detail-item strong,
.detail-section p {
  color: var(--farm-text);
}

.detail-section p {
  margin: 0;
  line-height: 1.7;
}

.detail-list {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.detail-list-row {
  display: flex;
  justify-content: space-between;
  gap: 12px;
}

@media (max-width: 760px) {
  .orders-shell {
    padding: 16px 0 36px;
  }

  .orders-card {
    padding: 14px;
  }

  .order-summary-strip {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .order-tracking,
  .detail-timeline {
    grid-template-columns: 1fr;
  }

  .section-header,
  .order-head,
  .groupbuy-head,
  .order-item-row,
  .detail-list-row {
    align-items: flex-start;
    flex-direction: column;
  }

  .order-status-block {
    align-items: flex-start;
  }

  .groupbuy-card {
    grid-template-columns: 88px 1fr;
  }

  .groupbuy-image {
    height: 88px;
  }

  .order-detail-grid {
    grid-template-columns: 1fr;
  }
}
</style>
