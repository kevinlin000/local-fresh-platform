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
      <el-input v-model="query.name" clearable placeholder="搜尋商品名稱" @keyup.enter="loadData" />
      <el-select v-model="query.status" clearable placeholder="上架狀態">
        <el-option label="上架" :value="1" />
        <el-option label="下架" :value="0" />
      </el-select>
      <el-checkbox v-model="query.lowStock">只看低庫存</el-checkbox>
      <el-button type="primary" @click="loadData">查詢</el-button>
      <el-button type="success" @click="openCreate">新增商品</el-button>
    </div>

    <el-alert
      v-if="query.lowStock"
      class="filter-alert"
      type="warning"
      show-icon
      :closable="false"
      title="目前只顯示低庫存商品，請優先補貨或調整庫存。"
    />

    <el-table v-loading="loading" :data="rows" stripe>
      <el-table-column prop="productName" label="商品名稱" min-width="150" />
      <el-table-column prop="categoryName" label="分類" width="110" />
      <el-table-column prop="price" label="價格" width="90" />
      <el-table-column label="庫存" width="100">
        <template #default="{ row }">
          <span :class="{ 'stock-warning': isLowStock(row) }">{{ row.stock ?? 0 }}</span>
        </template>
      </el-table-column>
      <el-table-column label="狀態" width="100">
        <template #default="{ row }">
          <el-tag :type="row.status === 1 ? 'success' : 'info'">{{ row.status === 1 ? '上架' : '下架' }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="上架品質" min-width="150">
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
      <el-table-column label="操作" width="230">
        <template #default="{ row }">
          <div class="row-actions">
            <el-button link type="primary" @click="openEdit(row)">編輯</el-button>
            <el-button link type="primary" @click="toggleStatus(row)">
              {{ row.status === 1 ? '下架' : '上架' }}
            </el-button>
            <el-button link type="primary" @click="openInventoryAdjust(row)">調整庫存</el-button>
            <el-button link type="primary" @click="openInventoryLogs(row)">庫存紀錄</el-button>
            <el-button link type="danger" @click="remove(row)">刪除</el-button>
          </div>
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

    <el-dialog v-model="dialogVisible" :title="form.id ? '編輯商品' : '新增商品'" width="620px">
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
        <el-form-item label="庫存" prop="stock">
          <el-input-number v-model="form.stock" :min="0" />
        </el-form-item>
        <el-form-item label="低庫存門檻" prop="lowStockThreshold">
          <el-input-number v-model="form.lowStockThreshold" :min="0" />
        </el-form-item>
        <el-form-item label="狀態" prop="status">
          <el-radio-group v-model="form.status">
            <el-radio :value="1">上架</el-radio>
            <el-radio :value="0">下架</el-radio>
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

    <el-dialog v-model="inventoryAdjustVisible" :title="inventoryAdjustTitle" width="460px">
      <el-form ref="inventoryAdjustFormRef" :model="inventoryAdjustForm" :rules="inventoryAdjustRules" label-width="96px">
        <el-form-item label="異動量" prop="changeQuantity">
          <el-input-number v-model="inventoryAdjustForm.changeQuantity" />
          <span class="form-hint">正數代表補貨，負數代表扣減。</span>
        </el-form-item>
        <el-form-item label="原因" prop="reason">
          <el-input
            v-model="inventoryAdjustForm.reason"
            maxlength="120"
            show-word-limit
            type="textarea"
            :rows="3"
            placeholder="例：進貨補貨、盤點耗損、商品報廢"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="inventoryAdjustVisible = false">取消</el-button>
        <el-button type="primary" :loading="inventoryAdjustSaving" @click="submitInventoryAdjust">確認調整</el-button>
      </template>
    </el-dialog>

    <el-drawer
      v-model="inventoryDrawerVisible"
      :title="inventoryDrawerTitle"
      size="720px"
      destroy-on-close
    >
      <el-table v-loading="inventoryLogsLoading" :data="inventoryLogs" stripe>
        <el-table-column prop="createdAt" label="時間" min-width="170">
          <template #default="{ row }">{{ formatDateTime(row.createdAt) }}</template>
        </el-table-column>
        <el-table-column label="異動" width="100">
          <template #default="{ row }">
            <span :class="inventoryChangeClass(row.changeQuantity)">
              {{ formatQuantity(row.changeQuantity) }}
            </span>
          </template>
        </el-table-column>
        <el-table-column label="庫存前後" width="130">
          <template #default="{ row }">{{ row.stockBefore }} → {{ row.stockAfter }}</template>
        </el-table-column>
        <el-table-column label="原因" min-width="150">
          <template #default="{ row }">{{ inventoryReasonLabel(row.reason) }}</template>
        </el-table-column>
        <el-table-column label="說明" min-width="160">
          <template #default="{ row }">{{ row.remark || '-' }}</template>
        </el-table-column>
        <el-table-column label="關聯訂單" width="110">
          <template #default="{ row }">#{{ row.referenceId || '-' }}</template>
        </el-table-column>
        <el-table-column label="操作者" width="130">
          <template #default="{ row }">{{ operatorLabel(row) }}</template>
        </el-table-column>
      </el-table>
      <el-empty v-if="!inventoryLogsLoading && inventoryLogs.length === 0" description="尚無庫存異動紀錄" />
    </el-drawer>
  </section>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref, watch } from 'vue'
import type { FormInstance, FormRules } from 'element-plus'
import { ElMessage, ElMessageBox } from 'element-plus'
import { useRoute } from 'vue-router'
import {
  adjustDishInventory,
  addDish,
  deleteDish,
  dishStatusByStatus,
  getCategoryList,
  getDishPage,
  queryDishInventoryLogs,
  queryDishById,
  editDish
} from '@/api/dish'
import { readPage, useLoading, usePage } from './composables'

const route = useRoute()
const rows = ref<any[]>([])
const categories = ref<any[]>([])
const query = reactive<{ name: string; status?: number; lowStock: boolean }>({ name: '', lowStock: false })
const page = usePage()
const { loading, withLoading } = useLoading()
const formRef = ref<FormInstance>()
const dialogVisible = ref(false)
const saving = ref(false)
const inventoryDrawerVisible = ref(false)
const inventoryLogsLoading = ref(false)
const inventoryLogs = ref<any[]>([])
const inventoryDrawerTitle = ref('庫存紀錄')
const inventoryLogProductId = ref<number>()
const inventoryAdjustFormRef = ref<FormInstance>()
const inventoryAdjustVisible = ref(false)
const inventoryAdjustSaving = ref(false)
const inventoryAdjustTitle = ref('調整庫存')
const inventoryAdjustProductId = ref<number>()
const inventoryAdjustForm = reactive({
  changeQuantity: 1,
  reason: ''
})
const form = reactive({
  id: undefined as number | undefined,
  productName: '',
  categoryId: undefined as number | undefined,
  price: 0.01,
  image: '',
  description: '',
  status: 1,
  stock: 100,
  lowStockThreshold: 10
})
const specs = ref<Array<{ name: string; value: string }>>([])

const rules: FormRules = {
  productName: [{ required: true, message: '請輸入商品名稱', trigger: 'blur' }],
  categoryId: [{ required: true, message: '請選擇分類', trigger: 'change' }],
  price: [{ required: true, message: '請輸入價格', trigger: 'blur' }],
  stock: [{ required: true, message: '請輸入庫存', trigger: 'blur' }],
  lowStockThreshold: [{ required: true, message: '請輸入低庫存門檻', trigger: 'blur' }],
  status: [{ required: true, message: '請選擇狀態', trigger: 'change' }]
}

const inventoryAdjustRules: FormRules = {
  changeQuantity: [{ required: true, message: '請輸入庫存異動量', trigger: 'blur' }],
  reason: [{ required: true, message: '請輸入庫存調整原因', trigger: 'blur' }]
}

const summaryCards = computed(() => {
  const onSale = rows.value.filter((item) => item.status === 1).length
  const offSale = rows.value.filter((item) => item.status !== 1).length
  const needsWork = rows.value.filter((item) => productQuality(item).some((quality) => quality.type === 'warning')).length
  const lowStock = rows.value.filter((item) => isLowStock(item)).length

  return [
    { label: '本頁上架', value: onSale, caption: '目前可被會員購買' },
    { label: '低庫存', value: lowStock, caption: '低於警示門檻' },
    { label: '需補資料', value: needsWork, caption: '缺圖或缺描述' }
  ]
})

async function loadData() {
  await withLoading(async () => {
    const response = await getDishPage({
      name: query.name || undefined,
      status: query.status,
      lowStock: query.lowStock || undefined,
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
  query.lowStock = route.query.lowStock === '1'
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

function isLowStock(row: any) {
  return Number(row.stock ?? 0) <= Number(row.lowStockThreshold ?? 0)
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
    status: 1,
    stock: 100,
    lowStockThreshold: 10
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
    status: data.status,
    stock: data.stock ?? 100,
    lowStockThreshold: data.lowStockThreshold ?? 10
  })
  specs.value = (data.productSpecs || []).map((item: any) => ({
    name: item.name || '',
    value: item.value || ''
  }))
  dialogVisible.value = true
}

async function openInventoryLogs(row: any) {
  inventoryDrawerTitle.value = `${row.productName}｜庫存紀錄`
  inventoryLogProductId.value = row.id
  inventoryDrawerVisible.value = true
  await loadInventoryLogs(row.id)
}

async function loadInventoryLogs(productId: number) {
  inventoryLogsLoading.value = true
  try {
    const response = await queryDishInventoryLogs(productId)
    inventoryLogs.value = response.data?.data || []
  } finally {
    inventoryLogsLoading.value = false
  }
}

function openInventoryAdjust(row: any) {
  inventoryAdjustProductId.value = row.id
  inventoryAdjustTitle.value = `${row.productName}｜調整庫存（目前 ${row.stock ?? 0}）`
  Object.assign(inventoryAdjustForm, {
    changeQuantity: 1,
    reason: ''
  })
  inventoryAdjustFormRef.value?.clearValidate()
  inventoryAdjustVisible.value = true
}

async function submitInventoryAdjust() {
  await inventoryAdjustFormRef.value?.validate()
  if (!inventoryAdjustProductId.value) {
    return
  }
  if (Number(inventoryAdjustForm.changeQuantity) === 0) {
    ElMessage.error('庫存異動量不能為 0')
    return
  }

  inventoryAdjustSaving.value = true
  try {
    await adjustDishInventory(inventoryAdjustProductId.value, { ...inventoryAdjustForm })
    ElMessage.success('庫存已調整')
    inventoryAdjustVisible.value = false
    await loadData()
    if (inventoryDrawerVisible.value && inventoryLogProductId.value === inventoryAdjustProductId.value) {
      await loadInventoryLogs(inventoryAdjustProductId.value)
    }
  } finally {
    inventoryAdjustSaving.value = false
  }
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
    ElMessage.success('商品已儲存')
    dialogVisible.value = false
    await loadData()
  } finally {
    saving.value = false
  }
}

async function remove(row: any) {
  await ElMessageBox.confirm(`確定刪除商品「${row.productName}」？`, '刪除確認', { type: 'warning' })
  await deleteDish(String(row.id))
  ElMessage.success('商品已刪除')
  await loadData()
}

function formatDateTime(value?: string) {
  if (!value) {
    return '-'
  }
  return value.replace('T', ' ').slice(0, 19)
}

function formatQuantity(value: number) {
  return value > 0 ? `+${value}` : String(value)
}

function inventoryChangeClass(value: number) {
  return value > 0 ? 'stock-increase' : 'stock-decrease'
}

function inventoryReasonLabel(reason: string) {
  const labels: Record<string, string> = {
    ORDER_RESERVE: '訂單預留庫存',
    ORDER_CANCEL_RESTORE: '訂單取消回補',
    GROUP_BUY_RESERVE: '揪團預留庫存',
    GROUP_BUY_CANCEL_RESTORE: '揪團取消回補',
    MANUAL_ADJUSTMENT: '人工調整'
  }
  return labels[reason] || reason || '-'
}

function operatorLabel(row: any) {
  const labels: Record<string, string> = {
    MEMBER: '會員',
    ADMIN: '管理員',
    SYSTEM: '系統'
  }
  const type = labels[row.operatorType] || row.operatorType || '-'
  return row.operatorId ? `${type} #${row.operatorId}` : type
}

watch(
  () => route.query.status,
  async () => {
    applyRouteQuery()
    page.page = 1
    await loadData()
  }
)

watch(
  () => route.query.lowStock,
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
  border: 1px solid var(--admin-line);
  border-radius: 8px;
  background: #f8faf7;
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

.row-actions {
  display: flex;
  flex-wrap: wrap;
  gap: 4px 10px;
}

.row-actions :deep(.el-button) {
  margin-left: 0;
}

.stock-warning {
  color: #b45309;
  font-weight: 800;
}

.filter-alert {
  margin-bottom: 14px;
}

.stock-increase {
  color: #15803d;
  font-weight: 800;
}

.stock-decrease {
  color: #b91c1c;
  font-weight: 800;
}

.form-hint {
  margin-left: 10px;
  color: var(--admin-muted);
  font-size: 12px;
}
</style>
