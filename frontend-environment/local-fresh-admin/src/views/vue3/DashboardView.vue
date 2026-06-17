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
          <p class="eyebrow">Today</p>
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
          <p class="eyebrow">Operations</p>
          <h2>今日優先處理</h2>
        </div>
        <div class="section-actions">
          <el-button @click="goToOrders(2)">待確認</el-button>
          <el-button @click="goToProducts(undefined, true)">低庫存</el-button>
          <el-button @click="goToProducts(0)">停售商品</el-button>
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
              <strong>{{ lowStockProducts.length }} 個低庫存單品</strong>
            </div>
            <el-tag type="danger" effect="plain">影響履約能力</el-tag>
          </div>

          <el-empty v-if="!lowStockProducts.length" description="目前沒有低庫存單品" />
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
              <strong>{{ offlineProducts.length }} 個停售單品</strong>
            </div>
            <el-tag type="warning" effect="plain">影響可售品項</el-tag>
          </div>

          <el-empty v-if="!offlineProducts.length" description="目前沒有停售單品" />
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
      <p class="eyebrow">Products</p>
      <h2>單品狀態</h2>
      <div class="split-stat product-split-stat">
        <div>
          <span>起售</span>
          <strong>{{ productOverview.sold ?? 0 }}</strong>
        </div>
        <div>
          <span>停售</span>
          <strong>{{ productOverview.discontinued ?? 0 }}</strong>
        </div>
        <div>
          <span>低庫存</span>
          <strong>{{ productOverview.lowStock ?? 0 }}</strong>
        </div>
      </div>
    </article>

    <article class="admin-card admin-card-pad half">
      <p class="eyebrow">Gift Boxes</p>
      <h2>直送箱狀態</h2>
      <div class="split-stat">
        <div>
          <span>起售</span>
          <strong>{{ giftBoxOverview.sold ?? 0 }}</strong>
        </div>
        <div>
          <span>停售</span>
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
  { label: '新增用戶', value: businessData.value.newUsers ?? 0, caption: '今日新增會員' },
  { label: '低庫存', value: productOverview.value.lowStock ?? lowStockProducts.value.length, caption: '需補貨或調整庫存' }
])

const orderCards = computed(() => [
  { label: '待確認', value: orderOverview.value.waitingOrders ?? 0 },
  { label: '待配送', value: orderOverview.value.deliveredOrders ?? 0 },
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
  gap: 18px;
}

.metric-card {
  grid-column: span 2;
  min-height: 150px;
}

.metric-card p,
.metric-card span {
  color: var(--admin-muted);
}

.metric-card strong {
  display: block;
  margin: 16px 0 10px;
  font-size: 30px;
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
  margin-bottom: 18px;
}

.section-actions {
  display: flex;
  gap: 10px;
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

.overview-grid,
.split-stat {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 14px;
}

.overview-tile,
.split-stat > div {
  padding: 16px;
  border-radius: 18px;
  background: #f7f1df;
}

.overview-tile span,
.split-stat span {
  display: block;
  color: var(--admin-muted);
}

.overview-tile strong,
.split-stat strong {
  display: block;
  margin-top: 10px;
  font-size: 28px;
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
  gap: 16px;
}

.ops-panel {
  padding: 18px;
  border: 1px solid rgba(32, 49, 38, 0.08);
  border-radius: 22px;
  background: #fffaf0;
}

.ops-panel.urgent {
  background: linear-gradient(180deg, #fff8ec 0%, #fff1df 100%);
}

.ops-panel-title,
.ops-row {
  display: flex;
  justify-content: space-between;
  gap: 14px;
  align-items: flex-start;
}

.ops-panel-title {
  margin-bottom: 14px;
}

.ops-panel-title span,
.ops-row small {
  display: block;
  color: var(--admin-muted);
}

.ops-panel-title strong {
  display: block;
  margin-top: 6px;
  font-size: 20px;
}

.ops-list {
  display: grid;
  gap: 10px;
}

.ops-row {
  width: 100%;
  padding: 14px;
  border: 1px solid rgba(32, 49, 38, 0.08);
  border-radius: 16px;
  background: rgba(255, 255, 255, 0.72);
  color: var(--admin-ink);
  text-align: left;
  cursor: pointer;
  transition: border-color 0.18s ease, transform 0.18s ease;
}

.ops-row:hover {
  border-color: rgba(47, 107, 66, 0.35);
  transform: translateY(-1px);
}

.ops-row b {
  color: var(--admin-green);
  white-space: nowrap;
}

.ops-row b.stock-danger {
  color: #b91c1c;
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
