<template>
  <section class="admin-card admin-card-pad">
    <div class="table-toolbar">
      <el-input v-model="query.name" clearable placeholder="搜尋員工姓名" @keyup.enter="loadData" />
      <el-button type="primary" @click="loadData">查詢</el-button>
      <el-button type="success" @click="openCreate">新增員工</el-button>
    </div>

    <el-table v-loading="loading" :data="rows" stripe>
      <el-table-column prop="name" label="姓名" min-width="140" />
      <el-table-column prop="username" label="帳號" min-width="140" />
      <el-table-column prop="phone" label="手機" min-width="140" />
      <el-table-column label="狀態" width="120">
        <template #default="{ row }">
          <el-tag :type="row.status === 1 ? 'success' : 'info'">{{ row.status === 1 ? '啟用' : '停用' }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="220">
        <template #default="{ row }">
          <el-button link type="primary" @click="openEdit(row)">編輯</el-button>
          <el-button link type="primary" @click="toggleStatus(row)">
            {{ row.status === 1 ? '停用' : '啟用' }}
          </el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-pagination
      v-model:current-page="page.page"
      v-model:page-size="page.pageSize"
      class="pager"
      background
      layout="total, prev, pager, next"
      :total="page.total"
      @current-change="loadData"
    />

    <el-dialog v-model="dialogVisible" :title="form.id ? '編輯員工' : '新增員工'" width="520px">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="96px">
        <el-form-item label="帳號" prop="username">
          <el-input v-model="form.username" :disabled="Boolean(form.id)" />
        </el-form-item>
        <el-form-item label="姓名" prop="name">
          <el-input v-model="form.name" />
        </el-form-item>
        <el-form-item label="手機" prop="phone">
          <el-input v-model="form.phone" placeholder="09xxxxxxxx" />
        </el-form-item>
        <el-form-item label="性別" prop="sex">
          <el-select v-model="form.sex" clearable placeholder="請選擇">
            <el-option label="男" value="1" />
            <el-option label="女" value="0" />
          </el-select>
        </el-form-item>
        <el-form-item label="身分證" prop="idNumber">
          <el-input v-model="form.idNumber" />
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
import { ElMessage } from 'element-plus'
import { addEmployee, enableOrDisable, getEmployeeList, updateEmployee } from '@/api/employee'
import { readPage, useLoading, usePage } from './composables'

const rows = ref<any[]>([])
const query = reactive({ name: '' })
const page = usePage()
const { loading, withLoading } = useLoading()
const formRef = ref<FormInstance>()
const dialogVisible = ref(false)
const saving = ref(false)
const form = reactive({
  id: undefined as number | undefined,
  username: '',
  name: '',
  phone: '',
  sex: '',
  idNumber: ''
})

const rules: FormRules = {
  username: [{ required: true, message: '請輸入帳號', trigger: 'blur' }],
  name: [{ required: true, message: '請輸入姓名', trigger: 'blur' }],
  phone: [{ pattern: /^$|^09\d{8}$/, message: '手機格式需為 09xxxxxxxx', trigger: 'blur' }]
}

async function loadData() {
  await withLoading(async () => {
    const response = await getEmployeeList({ name: query.name || undefined, page: page.page, pageSize: page.pageSize })
    const result = readPage(response)
    rows.value = result.records
    page.total = result.total
  })
}

async function toggleStatus(row: any) {
  await enableOrDisable({ id: row.id, status: row.status === 1 ? 0 : 1 })
  ElMessage.success('狀態已更新')
  await loadData()
}

function resetForm() {
  Object.assign(form, { id: undefined, username: '', name: '', phone: '', sex: '', idNumber: '' })
  formRef.value?.clearValidate()
}

function openCreate() {
  resetForm()
  dialogVisible.value = true
}

function openEdit(row: any) {
  Object.assign(form, {
    id: row.id,
    username: row.username,
    name: row.name,
    phone: row.phone || '',
    sex: row.sex || '',
    idNumber: row.idNumber || ''
  })
  dialogVisible.value = true
}

async function submit() {
  await formRef.value?.validate()
  saving.value = true
  try {
    const payload = { ...form }
    if (form.id) {
      await updateEmployee(payload)
    } else {
      await addEmployee(payload)
    }
    ElMessage.success('員工資料已儲存')
    dialogVisible.value = false
    await loadData()
  } finally {
    saving.value = false
  }
}

onMounted(loadData)
</script>
