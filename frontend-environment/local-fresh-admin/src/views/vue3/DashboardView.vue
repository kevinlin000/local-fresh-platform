<template>
  <section v-loading="loading" class="dashboard-grid">
    <article class="admin-card admin-card-pad wide command-card">
      <div class="section-title">
        <div>
          <p class="eyebrow">今日</p>
          <h2>營運指揮台</h2>
          <span class="section-copy">先處理會阻塞履約的事項，再檢查付款、庫存與商品可售狀態。</span>
        </div>
        <el-button type="primary" plain @click="loadDashboard">重新整理</el-button>
      </div>

      <div class="command-board">
        <button class="priority-card" :class="primaryPriority.tone" type="button" @click="primaryPriority.onClick">
          <span>目前第一優先</span>
          <strong>{{ primaryPriority.label }}</strong>
          <small>{{ primaryPriority.caption }}</small>
          <b>{{ primaryPriority.actionLabel }}</b>
        </button>

        <div class="priority-queue" aria-label="今日工作隊列">
          <button
            v-for="item in priorityCards"
            :key="item.label"
            class="queue-tile"
            :class="item.tone"
            type="button"
            @click="item.onClick"
          >
            <span>{{ item.label }}</span>
            <strong>{{ item.value }}</strong>
            <small>{{ item.caption }}</small>
          </button>
        </div>
      </div>

      <div class="health-strip" aria-label="今日營運健康度">
        <div v-for="item in healthIndicators" :key="item.label" class="health-item" :class="item.tone">
          <span>{{ item.label }}</span>
          <strong>{{ item.value }}</strong>
          <small>{{ item.caption }}</small>
        </div>
      </div>
    </article>

    <article v-for="card in metricCards" :key="card.label" class="admin-card admin-card-pad metric-card">
      <p>{{ card.label }}</p>
      <strong>{{ card.value }}</strong>
      <span>{{ card.caption }}</span>
    </article>

    <article class="admin-card admin-card-pad wide overview-card">
      <div class="section-title compact-title">
        <div>
          <p class="eyebrow">訂單</p>
          <h2>今日營運概覽</h2>
        </div>
      </div>

      <div class="overview-grid">
        <button v-for="item in orderCards" :key="item.label" class="overview-tile" type="button" @click="goToOrders(item.status)">
          <span>{{ item.label }}</span>
          <strong>{{ item.value }}</strong>
        </button>
      </div>
    </article>

    <article class="admin-card admin-card-pad wide">
      <div class="section-title">
        <div>
          <p class="eyebrow">營運</p>
          <h2>今日優先處理</h2>
        </div>
        <div class="section-actions">
          <el-button @click="goToOrders(2)">待確認</el-button>
          <el-button @click="goToProducts(undefined, true)">低庫存</el-button>
          <el-button @click="goToProducts(0)">下架商品</el-button>
        </div>
      </div>

      <div class="ops-grid">
        <section class="ops-panel urgent">
          <div class="ops-panel-title">
            <div>
              <span>訂單履約</span>
              <strong>{{ pendingOrders.length }} 筆待確認</strong>
            </div>
            <el-tag type="danger" effect="dark">需優先處理</el-tag>
          </div>

          <el-empty v-if="!pendingOrders.length" description="目前沒有待確認訂單" />
          <div v-else class="ops-list">
            <button
              v-for="order in pendingOrders"
              :key="order.id"
              class="ops-row"
              @click="goToOrders(2)"
            >
              <span>
                <strong>{{ order.number }}</strong>
                <small>{{ order.consignee || '未填收件人' }} · {{ order.orderTime || '無下單時間' }}</small>
              </span>
              <b>{{ money(order.amount) }}</b>
            </button>
          </div>
        </section>

        <section class="ops-panel urgent">
          <div class="ops-panel-title">
            <div>
              <span>庫存補貨</span>
              <strong>{{ lowStockProducts.length }} 個低庫存商品</strong>
            </div>
            <el-tag type="danger" effect="plain">影響履約能力</el-tag>
          </div>

          <el-empty v-if="!lowStockProducts.length" description="目前沒有低庫存商品" />
          <div v-else class="ops-list">
            <button
              v-for="product in lowStockProducts.slice(0, 5)"
              :key="product.id"
              class="ops-row"
              @click="goToProducts(undefined, true)"
            >
              <span>
                <strong>{{ product.productName }}</strong>
                <small>{{ product.categoryName || '未分類' }} · 門檻 {{ product.lowStockThreshold ?? 0 }}</small>
              </span>
              <b class="stock-danger">{{ product.stock ?? 0 }} 件</b>
            </button>
          </div>
        </section>

        <section class="ops-panel">
          <div class="ops-panel-title">
            <div>
              <span>商品上架</span>
              <strong>{{ offlineProducts.length }} 個下架商品</strong>
            </div>
            <el-tag type="warning" effect="plain">影響可售品項</el-tag>
          </div>

          <el-empty v-if="!offlineProducts.length" description="目前沒有下架商品" />
          <div v-else class="ops-list">
            <button
              v-for="product in offlineProducts"
              :key="product.id"
              class="ops-row"
              @click="goToProducts(0)"
            >
              <span>
                <strong>{{ product.productName }}</strong>
                <small>{{ product.categoryName || '未分類' }} · {{ product.description ? '描述完整' : '缺描述' }}</small>
              </span>
              <b>{{ money(product.price) }}</b>
            </button>
          </div>
        </section>
      </div>
    </article>

    <article class="admin-card admin-card-pad wide">
      <div class="section-title">
        <div>
          <p class="eyebrow">付款</p>
          <h2>付款與監控</h2>
        </div>
        <div class="section-actions">
          <el-button @click="goToPaymentEvents()">全部事件</el-button>
          <el-button @click="goToPaymentEvents({ result: 'PENDING' })">待處理</el-button>
          <el-button @click="copyPrometheusPath">Prometheus endpoint</el-button>
        </div>
      </div>

      <div class="payment-ops-grid">
        <section class="ops-panel payment-health-panel">
          <div class="payment-health-grid">
            <button
              v-for="item in paymentHealthCards"
              :key="item.label"
              class="payment-health-card"
              type="button"
              @click="item.onClick"
            >
              <span>{{ item.label }}</span>
              <strong>{{ item.value }}</strong>
              <small>{{ item.caption }}</small>
            </button>
          </div>
        </section>

        <section class="ops-panel">
          <div class="ops-panel-title">
            <div>
              <span>最近付款事件</span>
              <strong>{{ recentPaymentEvents.length }} 筆</strong>
            </div>
            <el-tag :type="paymentAttentionCount ? 'warning' : 'success'" effect="plain">
              {{ paymentAttentionCount ? `${paymentAttentionCount} 筆需追蹤` : '狀態正常' }}
            </el-tag>
          </div>

          <el-empty v-if="!recentPaymentEvents.length" description="目前沒有付款事件" />
          <div v-else class="ops-list">
            <button
              v-for="event in recentPaymentEvents"
              :key="event.id || `${event.orderNumber}-${event.createdAt}`"
              class="ops-row"
              @click="goToPaymentEvents({ orderNumber: event.orderNumber })"
            >
              <span>
                <strong>{{ eventLabel(event.eventType) }}</strong>
                <small>{{ event.orderNumber || '無訂單編號' }} · {{ formatDateTime(event.createdAt) }}</small>
              </span>
              <b :class="{ 'stock-danger': ['REJECTED', 'IGNORED'].includes(event.result) }">
                {{ resultLabel(event.result) }}
              </b>
            </button>
          </div>
        </section>
      </div>
    </article>

    <article class="admin-card admin-card-pad half">
      <p class="eyebrow">商品</p>
      <h2>商品狀態</h2>
      <div class="split-stat product-split-stat">
        <div>
          <span>上架</span>
          <strong>{{ productOverview.sold ?? 0 }}</strong>
        </div>
        <div>
          <span>下架</span>
          <strong>{{ productOverview.discontinued ?? 0 }}</strong>
        </div>
        <div>
          <span>低庫存</span>
          <strong>{{ productOverview.lowStock ?? 0 }}</strong>
        </div>
      </div>
    </article>

    <article class="admin-card admin-card-pad half">
      <p class="eyebrow">直送箱</p>
      <h2>直送箱狀態</h2>
      <div class="split-stat">
        <div>
          <span>上架</span>
          <strong>{{ giftBoxOverview.sold ?? 0 }}</strong>
        </div>
        <div>
          <span>下架</span>
          <strong>{{ giftBoxOverview.discontinued ?? 0 }}</strong>
        </div>
      </div>
    </article>
  </section>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { getBusinessData, getLowStockProducts, getOrderData, getOverviewDishes, getSetMealStatistics } from '@/api'
import { getDishPage } from '@/api/dish'
import { getOrderDetailPage } from '@/api/order'
import { getPaymentEventPage, getPendingPaymentRequests } from '@/api/paymentEvent'

const loading = ref(false)
const businessData = ref<any>({})
const orderOverview = ref<any>({})
const productOverview = ref<any>({})
const giftBoxOverview = ref<any>({})
const pendingOrders = ref<any[]>([])
const offlineProducts = ref<any[]>([])
const lowStockProducts = ref<any[]>([])
const recentPaymentEvents = ref<any[]>([])
const pendingPaymentRequests = ref<any[]>([])
const router = useRouter()

const metricCards = computed(() => [
  { label: '今日營業額', value: money(businessData.value.turnover), caption: '已完成訂單金額' },
  { label: '有效訂單', value: businessData.value.validOrderCount ?? 0, caption: '今日完成訂單數' },
  { label: '客單價', value: money(businessData.value.unitPrice), caption: '平均每筆有效訂單' },
  { label: '完成率', value: percent(businessData.value.orderCompletionRate), caption: '有效訂單 / 全部訂單' },
  { label: '新增會員', value: businessData.value.newUsers ?? 0, caption: '今日新增會員' },
  { label: '低庫存', value: productOverview.value.lowStock ?? lowStockProducts.value.length, caption: '需補貨或調整庫存' }
])

const orderCards = computed(() => [
  { label: '待確認', value: orderOverview.value.waitingOrders ?? 0, status: 2 },
  { label: '已確認', value: orderOverview.value.deliveredOrders ?? 0, status: 3 },
  { label: '已完成', value: orderOverview.value.completedOrders ?? 0, status: 5 },
  { label: '已取消', value: orderOverview.value.cancelledOrders ?? 0, status: 6 }
])

const paymentAttentionCount = computed(() => {
  return recentPaymentEvents.value.filter((item) => ['IGNORED', 'REJECTED'].includes(item.result)).length
})
const priorityCards = computed(() => [
  {
    label: '待確認訂單',
    value: pendingOrders.value.length,
    caption: '需要接單或婉拒',
    actionLabel: '前往訂單',
    tone: pendingOrders.value.length ? 'danger' : 'neutral',
    onClick: () => goToOrders(2)
  },
  {
    label: '待對帳付款',
    value: pendingPaymentRequests.value.length,
    caption: '付款請求尚未收到終態回呼',
    actionLabel: '查看付款',
    tone: pendingPaymentRequests.value.length ? 'warning' : 'neutral',
    onClick: () => goToPaymentEvents({ result: 'PENDING' })
  },
  {
    label: '低庫存品項',
    value: lowStockProducts.value.length,
    caption: '可能影響今日履約',
    actionLabel: '檢查庫存',
    tone: lowStockProducts.value.length ? 'danger' : 'neutral',
    onClick: () => goToProducts(undefined, true)
  },
  {
    label: '下架商品',
    value: offlineProducts.value.length,
    caption: '影響會員端可售品項',
    actionLabel: '整理上架',
    tone: offlineProducts.value.length ? 'warning' : 'neutral',
    onClick: () => goToProducts(0)
  }
])
const primaryPriority = computed(() => {
  return priorityCards.value.find((item) => item.value > 0) || {
    label: '營運狀態穩定',
    value: 0,
    caption: '目前沒有阻塞履約的事項，建議檢查商品資料與付款事件。',
    actionLabel: '查看付款監控',
    tone: 'success',
    onClick: () => goToPaymentEvents()
  }
})
const healthIndicators = computed(() => [
  {
    label: '今日完成率',
    value: percent(businessData.value.orderCompletionRate),
    caption: '有效訂單 / 全部訂單',
    tone: Number(businessData.value.orderCompletionRate || 0) >= 0.8 ? 'success' : 'neutral'
  },
  {
    label: '需追蹤付款',
    value: paymentAttentionCount.value,
    caption: '重複或拒絕回呼',
    tone: paymentAttentionCount.value ? 'warning' : 'success'
  },
  {
    label: '商品覆蓋',
    value: `${productOverview.value.sold ?? 0}/${(productOverview.value.sold ?? 0) + (productOverview.value.discontinued ?? 0)}`,
    caption: '上架 / 全部商品',
    tone: (productOverview.value.discontinued ?? 0) ? 'warning' : 'success'
  }
])

const paymentHealthCards = computed(() => [
  {
    label: '最近事件',
    value: recentPaymentEvents.value.length,
    caption: '付款請求與回呼',
    onClick: () => goToPaymentEvents()
  },
  {
    label: '待對帳',
    value: pendingPaymentRequests.value.length,
    caption: '未收到終態回呼',
    onClick: () => goToPaymentEvents({ result: 'PENDING' })
  },
  {
    label: '需追蹤',
    value: paymentAttentionCount.value,
    caption: '重複或拒絕回呼',
    onClick: () => goToPaymentEvents({ result: 'REJECTED' })
  },
  {
    label: '監控出口',
    value: '/actuator/prometheus',
    caption: 'Prometheus scrape',
    onClick: copyPrometheusPath
  }
])

function money(value: number | undefined) {
  const amount = Number(value || 0)
  return `NT$ ${amount.toLocaleString('zh-TW', { minimumFractionDigits: 0, maximumFractionDigits: 2 })}`
}

function percent(value: number | undefined) {
  return `${(Number(value || 0) * 100).toFixed(2)}%`
}

function goToOrders(status?: number) {
  void router.push({ path: '/orders', query: status ? { status } : {} })
}

function goToProducts(status?: number, lowStock = false) {
  const query: Record<string, number> = {}
  if (status !== undefined) {
    query.status = status
  }
  if (lowStock) {
    query.lowStock = 1
  }
  void router.push({ path: '/products', query })
}

function goToPaymentEvents(query: Record<string, string | undefined> = {}) {
  const nextQuery = Object.fromEntries(
    Object.entries(query).filter(([, value]) => Boolean(value))
  )
  void router.push({ path: '/payment-events', query: nextQuery })
}

function eventLabel(eventType: string) {
  const labels: Record<string, string> = {
    REQUEST_CREATED: '建立付款請求',
    CALLBACK_SUCCEEDED: '付款成功回呼',
    CALLBACK_DUPLICATE: '重複回呼',
    CALLBACK_REJECTED: '拒絕回呼'
  }
  return labels[eventType] || eventType || '未知事件'
}

function resultLabel(result: string) {
  const labels: Record<string, string> = {
    PENDING: '待處理',
    SUCCEEDED: '成功',
    IGNORED: '忽略',
    REJECTED: '拒絕'
  }
  return labels[result] || result || '未知'
}

function formatDateTime(value?: string) {
  if (!value) {
    return '無時間'
  }
  return value.replace('T', ' ').slice(0, 16)
}

async function copyPrometheusPath() {
  await navigator.clipboard?.writeText('/actuator/prometheus')
  ElMessage.success('已複製 /actuator/prometheus')
}

async function loadDashboard() {
  loading.value = true
  try {
    const [business, orders, products, giftBoxes, pending, offline, lowStock, paymentEvents, pendingPayments] = await Promise.allSettled([
      getBusinessData(),
      getOrderData(),
      getOverviewDishes(),
      getSetMealStatistics(),
      getOrderDetailPage({ status: 2, page: 1, pageSize: 5 }),
      getDishPage({ status: 0, page: 1, pageSize: 5 }),
      getLowStockProducts(),
      getPaymentEventPage({ page: 1, pageSize: 5 }),
      getPendingPaymentRequests({ page: 1, pageSize: 5 })
    ])
    businessData.value = readResponseData(business)
    orderOverview.value = readResponseData(orders)
    productOverview.value = readResponseData(products)
    giftBoxOverview.value = readResponseData(giftBoxes)
    pendingOrders.value = readResponseRecords(pending)
    offlineProducts.value = readResponseRecords(offline)
    lowStockProducts.value = readResponseData(lowStock, [])
    recentPaymentEvents.value = readResponseRecords(paymentEvents)
    pendingPaymentRequests.value = readResponseRecords(pendingPayments)
  } finally {
    loading.value = false
  }
}

function readResponseData(result: PromiseSettledResult<any>, fallback: any = {}) {
  if (result.status !== 'fulfilled') {
    return fallback
  }
  return result.value.data?.data || fallback
}

function readResponseRecords(result: PromiseSettledResult<any>) {
  return readResponseData(result, { records: [] }).records || []
}

onMounted(loadDashboard)
</script>

<style scoped>
.dashboard-grid {
  display: grid;
  grid-template-columns: repeat(6, minmax(0, 1fr));
  gap: 14px;
}

.metric-card {
  grid-column: span 2;
  min-height: 92px;
  padding: 15px 16px;
}

.metric-card p,
.metric-card span {
  color: var(--admin-muted);
  font-size: 13px;
  line-height: 1.45;
}

.metric-card p {
  margin: 0;
  font-weight: 700;
}

.metric-card strong {
  display: block;
  margin: 8px 0 5px;
  font-size: 24px;
  line-height: 1;
}

.wide {
  grid-column: 1 / -1;
}

.half {
  grid-column: span 3;
}

.section-title {
  display: flex;
  justify-content: space-between;
  gap: 16px;
  align-items: flex-start;
  margin-bottom: 14px;
}

.compact-title {
  margin-bottom: 10px;
}

.section-copy {
  display: block;
  margin-top: 6px;
  color: var(--admin-muted);
  font-size: 13px;
}

.section-actions {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
}

.eyebrow {
  margin: 0 0 8px;
  color: var(--admin-green);
  font-weight: 800;
  letter-spacing: 0;
}

h2 {
  margin: 0;
  font-size: 21px;
  line-height: 1.25;
}

.command-card {
  background: #ffffff;
}

.command-board {
  display: grid;
  grid-template-columns: 300px minmax(0, 1fr);
  gap: 12px;
  align-items: stretch;
}

.priority-card,
.queue-tile,
.overview-tile,
.split-stat > div {
  border: 1px solid var(--admin-line);
  border-radius: 6px;
  color: var(--admin-ink);
  text-align: left;
}

.priority-card,
.queue-tile,
.overview-tile {
  position: relative;
  cursor: pointer;
  transition: border-color 0.18s ease, background-color 0.18s ease;
}

.priority-card {
  min-height: 150px;
  padding: 18px;
  background: #ffffff;
}

.priority-card.danger {
  border-color: var(--admin-line-strong);
}

.priority-card.warning {
  border-color: var(--admin-line-strong);
}

.priority-card.success {
  border-color: var(--admin-line-strong);
}

.priority-card::before,
.queue-tile::before,
.health-item::before {
  content: "";
  display: block;
  width: 32px;
  height: 3px;
  margin-bottom: 12px;
  border-radius: 999px;
  background: var(--admin-line-strong);
}

.priority-card.danger::before,
.queue-tile.danger::before {
  background: var(--admin-danger);
}

.priority-card.warning::before,
.queue-tile.warning::before,
.health-item.warning::before {
  background: var(--admin-gold);
}

.priority-card.success::before,
.health-item.success::before {
  background: var(--admin-green);
}

.priority-card span,
.priority-card small,
.priority-card b,
.queue-tile span,
.queue-tile small,
.health-item span,
.health-item small {
  display: block;
  color: var(--admin-muted);
}

.priority-card strong {
  display: block;
  margin: 10px 0 8px;
  font-size: 25px;
  line-height: 1.2;
}

.priority-card b {
  margin-top: 18px;
  color: var(--admin-green-dark);
  font-size: 13px;
}

.priority-queue {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 10px;
}

.queue-tile {
  min-width: 0;
  min-height: 150px;
  padding: 14px;
  background: #ffffff;
}

.queue-tile.danger {
  border-color: var(--admin-line-strong);
}

.queue-tile.warning {
  border-color: var(--admin-line-strong);
}

.queue-tile strong {
  display: block;
  margin: 10px 0 8px;
  font-size: 28px;
  line-height: 1;
}

.priority-card:hover,
.queue-tile:hover,
.overview-tile:hover {
  border-color: rgba(47, 107, 66, 0.35);
  background: #f5f8f3;
}

.health-strip {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 10px;
  margin-top: 12px;
}

.health-item {
  min-width: 0;
  padding: 12px 14px;
  border: 1px solid var(--admin-line);
  border-radius: 6px;
  background: #ffffff;
}

.health-item.warning {
  border-color: var(--admin-line-strong);
}

.health-item.success {
  border-color: var(--admin-line-strong);
}

.health-item strong {
  display: block;
  margin: 6px 0 4px;
  font-size: 18px;
}

.overview-grid,
.split-stat {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 8px;
}

.overview-tile,
.split-stat > div {
  padding: 12px 14px;
  background: #f8faf7;
}

.overview-tile span,
.split-stat span {
  display: block;
  color: var(--admin-muted);
}

.overview-tile strong,
.split-stat strong {
  display: block;
  margin-top: 6px;
  font-size: 23px;
}

.split-stat {
  grid-template-columns: repeat(2, minmax(0, 1fr));
  margin-top: 18px;
}

.product-split-stat {
  grid-template-columns: repeat(3, minmax(0, 1fr));
}

.ops-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 12px;
}

.payment-ops-grid {
  display: grid;
  grid-template-columns: minmax(0, 1fr) minmax(320px, 0.85fr);
  gap: 12px;
  align-items: start;
}

.ops-panel {
  padding: 14px;
  border: 1px solid var(--admin-line);
  border-radius: 6px;
  background: #ffffff;
}

.ops-panel.urgent {
  border-color: var(--admin-line-strong);
  box-shadow: none;
}

.ops-panel.urgent .ops-panel-title {
  padding-left: 10px;
  border-left: 3px solid var(--admin-danger);
}

.ops-panel-title,
.ops-row {
  display: flex;
  justify-content: space-between;
  gap: 14px;
  align-items: flex-start;
}

.ops-panel-title {
  margin-bottom: 12px;
}

.ops-panel-title span,
.ops-row small {
  display: block;
  color: var(--admin-muted);
}

.ops-panel-title strong {
  display: block;
  margin-top: 5px;
  font-size: 18px;
}

.ops-list {
  display: grid;
  gap: 8px;
}

.ops-row {
  width: 100%;
  padding: 10px 12px;
  border: 1px solid var(--admin-line);
  border-radius: 6px;
  background: #fbfcfa;
  color: var(--admin-ink);
  text-align: left;
  cursor: pointer;
  transition: border-color 0.18s ease, background-color 0.18s ease;
}

.ops-row:hover {
  border-color: rgba(47, 107, 66, 0.35);
  background: #f5f8f3;
}

.ops-row b {
  color: var(--admin-green);
  white-space: nowrap;
}

.ops-row b.stock-danger {
  color: var(--admin-danger);
}

.payment-health-panel {
  display: block;
}

.payment-health-grid {
  width: 100%;
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 10px;
}

.payment-health-card {
  min-width: 0;
  min-height: 104px;
  padding: 12px;
  border: 1px solid var(--admin-line);
  border-radius: 6px;
  background: #f8faf7;
  color: var(--admin-ink);
  text-align: left;
  cursor: pointer;
  transition: border-color 0.18s ease, background-color 0.18s ease;
}

.payment-health-card:hover {
  border-color: rgba(47, 107, 66, 0.35);
  background: #f5f8f3;
}

.payment-health-card span,
.payment-health-card small {
  display: block;
  color: var(--admin-muted);
  line-height: 1.4;
}

.payment-health-card strong {
  display: block;
  min-height: 30px;
  margin: 8px 0 6px;
  overflow-wrap: anywhere;
  font-size: 24px;
  line-height: 1.15;
}

.payment-health-card:nth-child(4) strong {
  font-size: 14px;
}

@media (max-width: 980px) {
  .metric-card,
  .half {
    grid-column: 1 / -1;
  }

  .overview-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .ops-grid {
    grid-template-columns: 1fr;
  }

  .command-board,
  .priority-queue,
  .health-strip,
  .payment-ops-grid,
  .payment-health-grid {
    grid-template-columns: 1fr;
  }
}
</style>
