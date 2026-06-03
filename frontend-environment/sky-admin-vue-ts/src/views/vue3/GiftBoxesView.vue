<template>
  <section class="admin-card admin-card-pad">
    <div class="table-toolbar">
      <el-input v-model="query.name" clearable placeholder="搜尋直送箱名稱" @keyup.enter="loadData" />
      <el-button type="primary" @click="loadData">查詢</el-button>
    </div>

    <el-table v-loading="loading" :data="rows" stripe>
      <el-table-column prop="boxName" label="直送箱名稱" min-width="180" />
      <el-table-column prop="categoryName" label="分類" min-width="120" />
      <el-table-column prop="price" label="價格" width="110" />
      <el-table-column label="狀態" width="120">
        <template #default="{ row }">
          <el-tag :type="row.status === 1 ? 'success' : 'info'">{{ row.status === 1 ? '起售' : '停售' }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="160">
        <template #default="{ row }">
          <el-button link type="primary" @click="toggleStatus(row)">
            {{ row.status === 1 ? '停售' : '起售' }}
          </el-button>
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
  </section>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { enableOrDisableSetmeal, getSetmealPage } from '@/api/setMeal'
import { readPage, useLoading, usePage } from './composables'

const rows = ref<any[]>([])
const query = reactive({ name: '' })
const page = usePage()
const { loading, withLoading } = useLoading()

async function loadData() {
  await withLoading(async () => {
    const response = await getSetmealPage({ name: query.name || undefined, page: page.page, pageSize: page.pageSize })
    const result = readPage(response)
    rows.value = result.records
    page.total = result.total
  })
}

async function toggleStatus(row: any) {
  await enableOrDisableSetmeal({ id: row.id, status: row.status === 1 ? 0 : 1 })
  ElMessage.success('狀態已更新')
  await loadData()
}

onMounted(loadData)
</script>
