<template>
  <section class="admin-card admin-card-pad">
    <div class="table-toolbar">
      <el-input v-model="query.number" clearable placeholder="搜尋訂單號" @keyup.enter="loadData" />
      <el-select v-model="query.status" clearable placeholder="訂單狀態">
        <el-option v-for="item in statuses" :key="item.value" :label="item.label" :value="item.value" />
      </el-select>
      <el-button type="primary" @click="loadData">查詢</el-button>
    </div>

    <el-table v-loading="loading" :data="rows" stripe>
      <el-table-column prop="number" label="訂單號" min-width="190" />
      <el-table-column prop="consignee" label="收件人" min-width="110" />
      <el-table-column prop="phone" label="電話" min-width="130" />
      <el-table-column prop="amount" label="金額" width="110" />
      <el-table-column label="狀態" width="130">
        <template #default="{ row }">
          <el-tag>{{ statusText(row.status) }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="orderTime" label="下單時間" min-width="180" />
    </el-table>

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
import { onMounted, reactive, ref } from 'vue'
import { getOrderDetailPage } from '@/api/order'
import { readPage, useLoading, usePage } from './composables'

const statuses = [
  { label: '待付款', value: 1 },
  { label: '待接單', value: 2 },
  { label: '已接單', value: 3 },
  { label: '派送中', value: 4 },
  { label: '已完成', value: 5 },
  { label: '已取消', value: 6 },
  { label: '退款', value: 7 },
  { label: '團購預訂', value: 8 }
]

const rows = ref<any[]>([])
const query = reactive<{ number: string; status?: number }>({ number: '' })
const page = usePage()
const { loading, withLoading } = useLoading()

function statusText(status: number) {
  return statuses.find(item => item.value === status)?.label || '未知'
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
}

onMounted(loadData)
</script>
