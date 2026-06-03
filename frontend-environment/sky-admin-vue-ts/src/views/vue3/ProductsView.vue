<template>
  <section class="admin-card admin-card-pad">
    <div class="product-summary">
      <div v-for="item in summaryCards" :key="item.label" class="summary-card">
        <span>{{ item.label }}</span>
        <strong>{{ item.value }}</strong>
        <small>{{ item.caption }}</small>
      </div>
    </div>

    <div class="table-toolbar">
      <el-input v-model="query.name" clearable placeholder="搜尋單品名稱" @keyup.enter="loadData" />
      <el-select v-model="query.status" clearable placeholder="上架狀態">
        <el-option label="起售" :value="1" />
        <el-option label="停售" :value="0" />
      </el-select>
      <el-button type="primary" @click="loadData">查詢</el-button>
      <el-button type="success" @click="openCreate">新增單品</el-button>
    </div>

    <el-table v-loading="loading" :data="rows" stripe>
      <el-table-column prop="productName" label="單品名稱" min-width="180" />
      <el-table-column prop="categoryName" label="分類" min-width="120" />
      <el-table-column prop="price" label="價格" width="110" />
      <el-table-column label="狀態" width="120">
        <template #default="{ row }">
          <el-tag :type="row.status === 1 ? 'success' : 'info'">{{ row.status === 1 ? '起售' : '停售' }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="上架品質" min-width="220">
        <template #default="{ row }">
          <div class="quality-tags">
            <el-tag
              v-for="item in productQuality(row)"
              :key="item.label"
              :type="item.type"
              effect="plain"
            >
              {{ item.label }}
            </el-tag>
          </div>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="260">
        <template #default="{ row }">
          <el-button link type="primary" @click="openEdit(row)">編輯</el-button>
          <el-button link type="primary" @click="toggleStatus(row)">
            {{ row.status === 1 ? '停售' : '起售' }}
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

    <el-dialog v-model="dialogVisible" :title="form.id ? '編輯單品' : '新增單品'" width="620px">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="96px">
        <el-form-item label="名稱" prop="productName">
          <el-input v-model="form.productName" />
        </el-form-item>
        <el-form-item label="分類" prop="categoryId">
          <el-select v-model="form.categoryId" filterable placeholder="請選擇分類">
            <el-option v-for="item in categories" :key="item.id" :label="item.name" :value="item.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="價格" prop="price">
          <el-input-number v-model="form.price" :min="0.01" :precision="2" />
        </el-form-item>
        <el-form-item label="狀態" prop="status">
          <el-radio-group v-model="form.status">
            <el-radio :value="1">起售</el-radio>
            <el-radio :value="0">停售</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="圖片 URL">
          <el-input v-model="form.image" />
        </el-form-item>
        <el-form-item label="描述">
          <el-input v-model="form.description" type="textarea" :rows="3" />
        </el-form-item>
        <el-form-item label="規格">
          <div class="relation-list">
            <div v-for="(item, index) in specs" :key="index" class="inline-pair">
              <el-input v-model="item.name" placeholder="例：重量" />
              <div class="inline-actions">
                <el-input v-model="item.value" placeholder="例：300g" />
                <el-button link type="danger" @click="removeSpec(index)">移除</el-button>
              </div>
            </div>
            <el-button type="primary" plain @click="addSpec">新增規格</el-button>
          </div>
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
import { computed, onMounted, reactive, ref, watch } from 'vue'
import type { FormInstance, FormRules } from 'element-plus'
import { ElMessage, ElMessageBox } from 'element-plus'
import { useRoute } from 'vue-router'
import {
  addDish,
  deleteDish,
  dishStatusByStatus,
  getCategoryList,
  getDishPage,
  queryDishById,
  editDish
} from '@/api/dish'
import { readPage, useLoading, usePage } from './composables'

const route = useRoute()
const rows = ref<any[]>([])
const categories = ref<any[]>([])
const query = reactive<{ name: string; status?: number }>({ name: '' })
const page = usePage()
const { loading, withLoading } = useLoading()
const formRef = ref<FormInstance>()
const dialogVisible = ref(false)
const saving = ref(false)
const form = reactive({
  id: undefined as number | undefined,
  productName: '',
  categoryId: undefined as number | undefined,
  price: 0.01,
  image: '',
  description: '',
  status: 1
})
const specs = ref<Array<{ name: string; value: string }>>([])

const rules: FormRules = {
  productName: [{ required: true, message: '請輸入單品名稱', trigger: 'blur' }],
  categoryId: [{ required: true, message: '請選擇分類', trigger: 'change' }],
  price: [{ required: true, message: '請輸入價格', trigger: 'blur' }],
  status: [{ required: true, message: '請選擇狀態', trigger: 'change' }]
}

const summaryCards = computed(() => {
  const onSale = rows.value.filter((item) => item.status === 1).length
  const offSale = rows.value.filter((item) => item.status !== 1).length
  const needsWork = rows.value.filter((item) => productQuality(item).some((quality) => quality.type === 'warning')).length

  return [
    { label: '本頁起售', value: onSale, caption: '目前可被會員購買' },
    { label: '本頁停售', value: offSale, caption: '需確認是否補貨或下架' },
    { label: '需補資料', value: needsWork, caption: '缺圖或缺描述' }
  ]
})

async function loadData() {
  await withLoading(async () => {
    const response = await getDishPage({
      name: query.name || undefined,
      status: query.status,
      page: page.page,
      pageSize: page.pageSize
    })
    const result = readPage(response)
    rows.value = result.records
    page.total = result.total
  })
}

function applyRouteQuery() {
  const routeStatus = Number(route.query.status)
  query.status = Number.isFinite(routeStatus) && routeStatus >= 0 ? routeStatus : undefined
}

function productQuality(row: any) {
  const tags: Array<{ label: string; type: 'success' | 'warning' | 'info' }> = []
  if (row.image) {
    tags.push({ label: '有圖片', type: 'success' })
  } else {
    tags.push({ label: '缺圖片', type: 'warning' })
  }
  if (row.description) {
    tags.push({ label: '有描述', type: 'success' })
  } else {
    tags.push({ label: '缺描述', type: 'warning' })
  }
  return tags
}

async function toggleStatus(row: any) {
  await dishStatusByStatus({ id: row.id, status: row.status === 1 ? 0 : 1 })
  ElMessage.success('狀態已更新')
  await loadData()
}

async function loadCategories() {
  const response = await getCategoryList({ type: 1 })
  categories.value = response.data?.data || []
}

function resetForm() {
  Object.assign(form, {
    id: undefined,
    productName: '',
    categoryId: undefined,
    price: 0.01,
    image: '',
    description: '',
    status: 1
  })
  specs.value = []
  formRef.value?.clearValidate()
}

async function openCreate() {
  resetForm()
  await loadCategories()
  dialogVisible.value = true
}

async function openEdit(row: any) {
  await loadCategories()
  const response = await queryDishById(row.id)
  const data = response.data?.data || row
  Object.assign(form, {
    id: data.id,
    productName: data.productName,
    categoryId: data.categoryId,
    price: data.price,
    image: data.image || '',
    description: data.description || '',
    status: data.status
  })
  specs.value = (data.productSpecs || []).map((item: any) => ({
    name: item.name || '',
    value: item.value || ''
  }))
  dialogVisible.value = true
}

function addSpec() {
  specs.value.push({ name: '', value: '' })
}

function removeSpec(index: number) {
  specs.value.splice(index, 1)
}

function productSpecs() {
  return specs.value.filter((item) => item.name || item.value)
}

async function submit() {
  await formRef.value?.validate()
  saving.value = true
  try {
    const payload = { ...form, productSpecs: productSpecs() }
    if (form.id) {
      await editDish(payload)
    } else {
      await addDish(payload)
    }
    ElMessage.success('單品已儲存')
    dialogVisible.value = false
    await loadData()
  } finally {
    saving.value = false
  }
}

async function remove(row: any) {
  await ElMessageBox.confirm(`確定刪除單品「${row.productName}」？`, '刪除確認', { type: 'warning' })
  await deleteDish(String(row.id))
  ElMessage.success('單品已刪除')
  await loadData()
}

watch(
  () => route.query.status,
  async () => {
    applyRouteQuery()
    page.page = 1
    await loadData()
  }
)

onMounted(async () => {
  applyRouteQuery()
  await Promise.all([loadCategories(), loadData()])
})
</script>

<style scoped>
.product-summary {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 12px;
  margin-bottom: 18px;
}

.summary-card {
  padding: 16px;
  border: 1px solid rgba(32, 49, 38, 0.08);
  border-radius: 18px;
  background: #f8f2df;
}

.summary-card span,
.summary-card small {
  display: block;
  color: var(--admin-muted);
}

.summary-card strong {
  display: block;
  margin: 8px 0 4px;
  font-size: 26px;
}

.quality-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
}
</style>
