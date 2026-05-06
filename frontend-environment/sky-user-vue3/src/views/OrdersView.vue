<template>
  <section class="orders-shell">
    <div class="orders-card">
      <div class="section-header">
        <div>
          <p class="eyebrow">我的訂單</p>
          <h1>查看最近下單紀錄與揪團進度</h1>
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
                  {{ orderStatusText(order.status) }}
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
              <div v-else class="image-placeholder">團</div>
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
</template>

<script setup lang="ts">
import { onMounted, ref, watch } from 'vue'
import { ElMessage } from 'element-plus'
import { useRoute, useRouter } from 'vue-router'
import { fetchMyGroupBuys, type GroupBuyRecord } from '@/services/groupBuy'
import { fetchOrderHistory, type OrderRecord } from '@/services/order'

const route = useRoute()
const router = useRouter()

const activeTab = ref<'orders' | 'group-buys'>(route.query.tab === 'group-buy' ? 'group-buys' : 'orders')
const loadingOrders = ref(false)
const loadingGroupBuys = ref(false)
const orders = ref<OrderRecord[]>([])
const groupBuys = ref<GroupBuyRecord[]>([])

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
  margin-bottom: 12px;
}

.orders-tabs {
  margin-bottom: 16px;
}

.eyebrow {
  margin: 0 0 8px;
  color: #62864e;
  font-size: 13px;
  font-weight: 800;
  letter-spacing: 0.08em;
  text-transform: uppercase;
}

h1,
h2 {
  margin: 0;
  color: #25361f;
}

.order-list,
.groupbuy-list {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.order-card,
.groupbuy-card {
  padding: 20px;
  border: 1px solid rgba(83, 126, 62, 0.14);
  border-radius: 22px;
  background: #fcfefb;
}

.groupbuy-card {
  display: grid;
  grid-template-columns: 120px 1fr;
  gap: 18px;
  cursor: pointer;
}

.groupbuy-image {
  overflow: hidden;
  height: 120px;
  border-radius: 18px;
  background: linear-gradient(145deg, #edf6e8 0%, #d9ead1 100%);
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
  color: #4d7150;
  font-size: 28px;
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
}

.order-number {
  color: #25361f;
  font-weight: 800;
}

.order-meta,
.groupbuy-meta {
  display: flex;
  gap: 12px;
  margin-top: 8px;
  color: #6a7866;
  font-size: 14px;
  flex-wrap: wrap;
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

.participants {
  display: flex;
  gap: 10px;
  margin-top: 14px;
  flex-wrap: wrap;
}

.participant-chip {
  padding: 6px 10px;
  border-radius: 999px;
  background: #edf6e8;
  color: #456138;
  font-size: 13px;
  font-weight: 700;
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
