<template>
  <section class="admin-card admin-card-pad">
    <div class="table-toolbar">
      <el-input v-model="query.name" clearable placeholder="搜尋分類名稱" @keyup.enter="loadData" />
      <el-select v-model="query.type" clearable placeholder="分類類型">
        <el-option label="商品分類" :value="1" />
        <el-option label="直送箱分類" :value="2" />
      </el-select>
      <el-button type="primary" @click="loadData">查詢</el-button>
      <el-button type="success" @click="openCreate">新增分類</el-button>
    </div>

    <el-table v-loading="loading" :data="rows" stripe>
      <el-table-column prop="name" label="分類名稱" min-width="160" />
      <el-table-column label="類型" width="130">
        <template #default="{ row }">{{ row.type === 1 ? '商品' : '直送箱' }}</template>
      </el-table-column>
      <el-table-column prop="sort" label="排序" width="100" />
      <el-table-column label="狀態" width="120">
        <template #default="{ row }">
          <el-tag :type="row.status === 1 ? 'success' : 'info'">{{ row.status === 1 ? '啟用' : '停用' }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="240">
        <template #default="{ row }">
          <el-button link type="primary" @click="openEdit(row)">編輯</el-button>
          <el-button link type="primary" @click="toggleStatus(row)">
            {{ row.status === 1 ? '停用' : '啟用' }}
          </el-button>
          <el-button link type="danger" @click="remove(row)">刪除</el-button>
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

    <el-dialog v-model="dialogVisible" :title="form.id ? '編輯分類' : '新增分類'" width="480px">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="96px">
        <el-form-item label="名稱" prop="name">
          <el-input v-model="form.name" />
        </el-form-item>
        <el-form-item label="類型" prop="type">
          <el-select v-model="form.type" placeholder="請選擇">
            <el-option label="商品分類" :value="1" />
            <el-option label="直送箱分類" :value="2" />
          </el-select>
        </el-form-item>
        <el-form-item label="排序" prop="sort">
          <el-input-number v-model="form.sort" :min="0" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="submit">儲存</el-button>
      </template>
    </el-dialog>
  </section>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import type { FormInstance, FormRules } from 'element-plus'
import { ElMessage, ElMessageBox } from 'element-plus'
import { addCategory, deleCategory, editCategory, enableOrDisableEmployee, getCategoryPage } from '@/api/category'
import { readPage, useLoading, usePage } from './composables'

const rows = ref<any[]>([])
const query = reactive<{ name: string; type?: number }>({ name: '' })
const page = usePage()
const { loading, withLoading } = useLoading()
const formRef = ref<FormInstance>()
const dialogVisible = ref(false)
const saving = ref(false)
const form = reactive({
  id: undefined as number | undefined,
  name: '',
  type: 1,
  sort: 0
})

const rules: FormRules = {
  name: [{ required: true, message: '請輸入分類名稱', trigger: 'blur' }],
  type: [{ required: true, message: '請選擇分類類型', trigger: 'change' }],
  sort: [{ required: true, message: '請輸入排序', trigger: 'blur' }]
}

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

function resetForm() {
  Object.assign(form, { id: undefined, name: '', type: 1, sort: 0 })
  formRef.value?.clearValidate()
}

function openCreate() {
  resetForm()
  dialogVisible.value = true
}

function openEdit(row: any) {
  Object.assign(form, { id: row.id, name: row.name, type: row.type, sort: row.sort ?? 0 })
  dialogVisible.value = true
}

async function submit() {
  await formRef.value?.validate()
  saving.value = true
  try {
    if (form.id) {
      await editCategory({ ...form })
    } else {
      await addCategory({ ...form })
    }
    ElMessage.success('分類已儲存')
    dialogVisible.value = false
    await loadData()
  } finally {
    saving.value = false
  }
}

async function remove(row: any) {
  await ElMessageBox.confirm(`確定刪除分類「${row.name}」？`, '刪除確認', { type: 'warning' })
  await deleCategory(String(row.id))
  ElMessage.success('分類已刪除')
  await loadData()
}

onMounted(loadData)
</script>
