<template>
  <section class="orders-shell">
    <div class="orders-card">
      <div class="section-header">
        <div>
          <p class="eyebrow">我的訂單</p>
          <h1>查看最近下單紀錄</h1>
        </div>
        <el-button text type="success" @click="loadOrders">重新整理</el-button>
      </div>

      <el-skeleton v-if="loading" :rows="10" animated />

      <el-empty v-else-if="!orders.length" description="目前還沒有訂單紀錄">
        <el-button type="success" @click="goHome">回首頁逛逛</el-button>
      </el-empty>

      <div v-else class="order-list">
        <article v-for="order in orders" :key="order.id" class="order-card">
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
                {{ statusText(order.status) }}
              </el-tag>
              <strong>NT$ {{ formatPrice(order.amount) }}</strong>
            </div>
          </div>

          <div class="order-address">
            {{ order.address }}
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
        </article>
      </div>
    </div>
  </section>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { useRouter } from 'vue-router'
import { fetchOrderHistory, type OrderRecord } from '@/services/order'

const router = useRouter()
const loading = ref(false)
const orders = ref<OrderRecord[]>([])

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

function statusText(status: number) {
  switch (status) {
    case 1:
      return '待付款'
    case 2:
      return '待接單'
    case 3:
      return '已接單'
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

async function loadOrders() {
  loading.value = true
  try {
    const result = await fetchOrderHistory({
      page: 1,
      pageSize: 20
    })
    orders.value = result.records
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '載入訂單列表失敗')
  } finally {
    loading.value = false
  }
}

function goHome() {
  void router.push('/')
}

onMounted(() => {
  void loadOrders()
})
</script>

<style scoped>
.orders-shell {
  max-width: 1180px;
  margin: 0 auto;
  padding: 32px 40px 52px;
}

.orders-card {
  padding: 28px;
  border-radius: 28px;
  background: rgba(255, 255, 255, 0.93);
  box-shadow: 0 24px 60px rgba(61, 111, 39, 0.12);
}

.section-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 16px;
  margin-bottom: 22px;
}

.eyebrow {
  margin: 0 0 8px;
  color: #62864e;
  font-size: 13px;
  font-weight: 800;
  letter-spacing: 0.08em;
  text-transform: uppercase;
}

h1 {
  margin: 0;
  color: #25361f;
}

.order-list {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.order-card {
  padding: 20px;
  border: 1px solid rgba(83, 126, 62, 0.14);
  border-radius: 22px;
  background: #fcfefb;
}

.order-head,
.order-status-block,
.order-item-row,
.order-item-side {
  display: flex;
  justify-content: space-between;
  gap: 12px;
}

.order-head {
  align-items: flex-start;
}

.order-status-block {
  align-items: center;
}

.order-number {
  color: #25361f;
  font-weight: 800;
}

.order-meta {
  display: flex;
  gap: 12px;
  margin-top: 8px;
  color: #6a7866;
  font-size: 14px;
}

.order-address {
  margin-top: 12px;
  color: #566651;
}

.order-items {
  display: flex;
  flex-direction: column;
  gap: 10px;
  margin-top: 16px;
  padding-top: 14px;
  border-top: 1px dashed rgba(89, 127, 69, 0.18);
}

.order-item-row {
  align-items: center;
}

.spec {
  margin-left: 10px;
  color: #7a8576;
  font-size: 13px;
}

.order-remark {
  margin: 14px 0 0;
  color: #52604d;
}
</style>
