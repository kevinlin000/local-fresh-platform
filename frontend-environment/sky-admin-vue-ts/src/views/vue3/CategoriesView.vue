<template>
  <section class="admin-card admin-card-pad">
    <div class="table-toolbar">
      <el-input v-model="query.name" clearable placeholder="搜尋分類名稱" @keyup.enter="loadData" />
      <el-select v-model="query.type" clearable placeholder="分類類型">
        <el-option label="單品分類" :value="1" />
        <el-option label="直送箱分類" :value="2" />
      </el-select>
      <el-button type="primary" @click="loadData">查詢</el-button>
    </div>

    <el-table v-loading="loading" :data="rows" stripe>
      <el-table-column prop="name" label="分類名稱" min-width="160" />
      <el-table-column label="類型" width="130">
        <template #default="{ row }">{{ row.type === 1 ? '單品' : '直送箱' }}</template>
      </el-table-column>
      <el-table-column prop="sort" label="排序" width="100" />
      <el-table-column label="狀態" width="120">
        <template #default="{ row }">
          <el-tag :type="row.status === 1 ? 'success' : 'info'">{{ row.status === 1 ? '啟用' : '停用' }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="160">
        <template #default="{ row }">
          <el-button link type="primary" @click="toggleStatus(row)">
            {{ row.status === 1 ? '停用' : '啟用' }}
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
import { enableOrDisableEmployee, getCategoryPage } from '@/api/category'
import { readPage, useLoading, usePage } from './composables'

const rows = ref<any[]>([])
const query = reactive<{ name: string; type?: number }>({ name: '' })
const page = usePage()
const { loading, withLoading } = useLoading()

async function loadData() {
  await withLoading(async () => {
    const response = await getCategoryPage({ ...query, page: page.page, pageSize: page.pageSize })
    const result = readPage(response)
    rows.value = result.records
    page.total = result.total
  })
}

async function toggleStatus(row: any) {
  await enableOrDisableEmployee({ id: row.id, status: row.status === 1 ? 0 : 1 })
  ElMessage.success('狀態已更新')
  await loadData()
}

onMounted(loadData)
</script>
