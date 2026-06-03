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

    <article class="admin-card admin-card-pad half">
      <p class="eyebrow">Products</p>
      <h2>單品狀態</h2>
      <div class="split-stat">
        <div>
          <span>起售</span>
          <strong>{{ productOverview.sold ?? 0 }}</strong>
        </div>
        <div>
          <span>停售</span>
          <strong>{{ productOverview.discontinued ?? 0 }}</strong>
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
import { getBusinessData, getOrderData, getOverviewDishes, getSetMealStatistics } from '@/api'

const loading = ref(false)
const businessData = ref<any>({})
const orderOverview = ref<any>({})
const productOverview = ref<any>({})
const giftBoxOverview = ref<any>({})

const metricCards = computed(() => [
  { label: '今日營業額', value: money(businessData.value.turnover), caption: '已完成訂單金額' },
  { label: '有效訂單', value: businessData.value.validOrderCount ?? 0, caption: '今日完成訂單數' },
  { label: '客單價', value: money(businessData.value.unitPrice), caption: '平均每筆有效訂單' },
  { label: '完成率', value: percent(businessData.value.orderCompletionRate), caption: '有效訂單 / 全部訂單' },
  { label: '新增用戶', value: businessData.value.newUsers ?? 0, caption: '今日新增會員' },
  { label: '全部訂單', value: orderOverview.value.allOrders ?? 0, caption: '平台累計訂單' }
])

const orderCards = computed(() => [
  { label: '待接單', value: orderOverview.value.waitingOrders ?? 0 },
  { label: '待派送', value: orderOverview.value.deliveredOrders ?? 0 },
  { label: '已完成', value: orderOverview.value.completedOrders ?? 0 },
  { label: '已取消', value: orderOverview.value.cancelledOrders ?? 0 }
])

function money(value: number | undefined) {
  return `$${Number(value || 0).toFixed(2)}`
}

function percent(value: number | undefined) {
  return `${(Number(value || 0) * 100).toFixed(2)}%`
}

async function loadDashboard() {
  loading.value = true
  try {
    const [business, orders, products, giftBoxes] = await Promise.all([
      getBusinessData(),
      getOrderData(),
      getOverviewDishes(),
      getSetMealStatistics()
    ])
    businessData.value = business.data?.data || {}
    orderOverview.value = orders.data?.data || {}
    productOverview.value = products.data?.data || {}
    giftBoxOverview.value = giftBoxes.data?.data || {}
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

@media (max-width: 980px) {
  .metric-card,
  .half {
    grid-column: 1 / -1;
  }

  .overview-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}
</style>
