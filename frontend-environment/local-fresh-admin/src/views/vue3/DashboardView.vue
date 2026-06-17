<template>
  <section v-loading="loading" class="dashboard-grid">
    <article v-for="card in metricCards" :key="card.label" class="admin-card admin-card-pad metric-card">
      <p>{{ card.label }}</p>
      <strong>{{ card.value }}</strong>
      <span>{{ card.caption }}</span>
    </article>

    <article class="admin-card admin-card-pad wide">
      <div class="section-title">
        <div>
          <p class="eyebrow">今日</p>
          <h2>今日營運概覽</h2>
        </div>
        <el-button type="primary" plain @click="loadDashboard">重新整理</el-button>
      </div>

      <div class="overview-grid">
        <div v-for="item in orderCards" :key="item.label" class="overview-tile">
          <span>{{ item.label }}</span>
          <strong>{{ item.value }}</strong>
        </div>
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
import { getBusinessData, getLowStockProducts, getOrderData, getOverviewDishes, getSetMealStatistics } from '@/api'
import { getDishPage } from '@/api/dish'
import { getOrderDetailPage } from '@/api/order'

const loading = ref(false)
const businessData = ref<any>({})
const orderOverview = ref<any>({})
const productOverview = ref<any>({})
const giftBoxOverview = ref<any>({})
const pendingOrders = ref<any[]>([])
const offlineProducts = ref<any[]>([])
const lowStockProducts = ref<any[]>([])
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
  { label: '待確認', value: orderOverview.value.waitingOrders ?? 0 },
  { label: '已確認', value: orderOverview.value.deliveredOrders ?? 0 },
  { label: '已完成', value: orderOverview.value.completedOrders ?? 0 },
  { label: '已取消', value: orderOverview.value.cancelledOrders ?? 0 }
])

function money(value: number | undefined) {
  return `$${Number(value || 0).toFixed(2)}`
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

async function loadDashboard() {
  loading.value = true
  try {
    const [business, orders, products, giftBoxes, pending, offline, lowStock] = await Promise.all([
      getBusinessData(),
      getOrderData(),
      getOverviewDishes(),
      getSetMealStatistics(),
      getOrderDetailPage({ status: 2, page: 1, pageSize: 5 }),
      getDishPage({ status: 0, page: 1, pageSize: 5 }),
      getLowStockProducts()
    ])
    businessData.value = business.data?.data || {}
    orderOverview.value = orders.data?.data || {}
    productOverview.value = products.data?.data || {}
    giftBoxOverview.value = giftBoxes.data?.data || {}
    pendingOrders.value = pending.data?.data?.records || []
    offlineProducts.value = offline.data?.data?.records || []
    lowStockProducts.value = lowStock.data?.data || []
  } finally {
    loading.value = false
  }
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
  min-height: 122px;
  padding: 18px;
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
  margin: 12px 0 8px;
  font-size: 28px;
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
  margin-bottom: 16px;
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
  font-size: 22px;
  line-height: 1.25;
}

.overview-grid,
.split-stat {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 10px;
}

.overview-tile,
.split-stat > div {
  padding: 14px;
  border: 1px solid var(--admin-line);
  border-radius: 7px;
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
  margin-top: 8px;
  font-size: 26px;
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

.ops-panel {
  padding: 16px;
  border: 1px solid var(--admin-line);
  border-radius: 8px;
  background: #ffffff;
}

.ops-panel.urgent {
  border-color: rgba(180, 35, 24, 0.22);
  box-shadow: inset 3px 0 0 rgba(180, 35, 24, 0.72);
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
  padding: 12px;
  border: 1px solid var(--admin-line);
  border-radius: 7px;
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
}
</style>
