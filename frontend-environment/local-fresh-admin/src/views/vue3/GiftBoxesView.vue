<template>
  <section class="admin-card admin-card-pad">
    <div class="table-toolbar">
      <el-input v-model="query.name" clearable placeholder="搜尋直送箱名稱" @keyup.enter="loadData" />
      <el-button type="primary" @click="loadData">查詢</el-button>
      <el-button type="success" @click="openCreate">新增直送箱</el-button>
    </div>

    <el-table v-loading="loading" :data="rows" stripe>
      <el-table-column prop="boxName" label="直送箱名稱" min-width="180" />
      <el-table-column prop="categoryName" label="分類" min-width="120" />
      <el-table-column prop="price" label="價格" width="110" />
      <el-table-column label="狀態" width="120">
        <template #default="{ row }">
          <el-tag :type="row.status === 1 ? 'success' : 'info'">{{ row.status === 1 ? '上架' : '下架' }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="260">
        <template #default="{ row }">
          <el-button link type="primary" @click="openEdit(row)">編輯</el-button>
          <el-button link type="primary" @click="toggleStatus(row)">
            {{ row.status === 1 ? '下架' : '上架' }}
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

    <el-dialog v-model="dialogVisible" :title="form.id ? '編輯直送箱' : '新增直送箱'" width="620px">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="108px">
        <el-form-item label="名稱" prop="boxName">
          <el-input v-model="form.boxName" />
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
        <el-form-item label="箱內商品">
          <div class="relation-list">
            <div v-for="item in selectedProducts" :key="item.productId" class="relation-row">
              <span>{{ item.name }}</span>
              <el-input-number v-model="item.copies" :min="1" :max="99" />
              <el-button link type="danger" @click="removeProduct(item.productId)">移除</el-button>
            </div>
            <el-select
              v-model="selectedProductId"
              filterable
              placeholder="新增商品"
              :disabled="products.length === selectedProducts.length"
              @change="addProduct"
            >
              <el-option
                v-for="item in selectableProducts"
                :key="item.id"
                :label="`${item.productName} / $${item.price}`"
                :value="item.id"
              />
            </el-select>
            <p class="helper-text">直送箱至少需要一個已上架商品，否則後端無法建立商品關聯。</p>
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
import { computed, onMounted, reactive, ref } from 'vue'
import type { FormInstance, FormRules } from 'element-plus'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  addSetmeal,
  deleteSetmeal,
  editSetmeal,
  enableOrDisableSetmeal,
  getSetmealPage,
  querySetmealById
} from '@/api/setMeal'
import { getCategoryByType } from '@/api/category'
import { queryDishList } from '@/api/dish'
import { readPage, useLoading, usePage } from './composables'

const rows = ref<any[]>([])
const categories = ref<any[]>([])
const products = ref<any[]>([])
const selectedProducts = ref<Array<{ productId: number; name: string; price: number; copies: number }>>([])
const selectedProductId = ref<number>()
const query = reactive({ name: '' })
const page = usePage()
const { loading, withLoading } = useLoading()
const formRef = ref<FormInstance>()
const dialogVisible = ref(false)
const saving = ref(false)
const form = reactive({
  id: undefined as number | undefined,
  boxName: '',
  categoryId: undefined as number | undefined,
  price: 0.01,
  image: '',
  description: '',
  status: 1
})

const rules: FormRules = {
  boxName: [{ required: true, message: '請輸入直送箱名稱', trigger: 'blur' }],
  categoryId: [{ required: true, message: '請選擇分類', trigger: 'change' }],
  price: [{ required: true, message: '請輸入價格', trigger: 'blur' }],
  status: [{ required: true, message: '請選擇狀態', trigger: 'change' }]
}

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

async function loadCategories() {
  const response = await getCategoryByType({ type: 2 })
  categories.value = response.data?.data || []
}

async function loadProducts() {
  const response = await queryDishList({ status: 1 })
  products.value = response.data?.data || []
}

const selectableProducts = computed(() => {
  const selectedIds = new Set(selectedProducts.value.map((item) => item.productId))
  return products.value.filter((item) => !selectedIds.has(item.id))
})

function resetForm() {
  Object.assign(form, {
    id: undefined,
    boxName: '',
    categoryId: undefined,
    price: 0.01,
    image: '',
    description: '',
    status: 1
  })
  selectedProducts.value = []
  selectedProductId.value = undefined
  formRef.value?.clearValidate()
}

async function openCreate() {
  resetForm()
  await Promise.all([loadCategories(), loadProducts()])
  dialogVisible.value = true
}

async function openEdit(row: any) {
  await Promise.all([loadCategories(), loadProducts()])
  const response = await querySetmealById(row.id)
  const data = response.data?.data || row
  Object.assign(form, {
    id: data.id,
    boxName: data.boxName,
    categoryId: data.categoryId,
    price: data.price,
    image: data.image || '',
    description: data.description || '',
    status: data.status
  })
  selectedProducts.value = (data.giftBoxProducts || []).map((item: any) => ({
    productId: item.productId,
    name: item.name,
    price: Number(item.price),
    copies: item.copies || 1
  }))
  dialogVisible.value = true
}

function addProduct(productId: number) {
  const product = products.value.find((item) => item.id === productId)
  if (!product) {
    return
  }
  selectedProducts.value.push({
    productId: product.id,
    name: product.productName,
    price: Number(product.price),
    copies: 1
  })
  selectedProductId.value = undefined
}

function removeProduct(productId: number) {
  selectedProducts.value = selectedProducts.value.filter((item) => item.productId !== productId)
}

async function submit() {
  await formRef.value?.validate()
  if (!selectedProducts.value.length) {
    ElMessage.warning('請至少加入一個箱內商品')
    return
  }
  saving.value = true
  try {
    const payload = { ...form, giftBoxProducts: selectedProducts.value }
    if (form.id) {
      await editSetmeal(payload)
    } else {
      await addSetmeal(payload)
    }
    ElMessage.success('直送箱已儲存')
    dialogVisible.value = false
    await loadData()
  } finally {
    saving.value = false
  }
}

async function remove(row: any) {
  await ElMessageBox.confirm(`確定刪除直送箱「${row.boxName}」？`, '刪除確認', { type: 'warning' })
  await deleteSetmeal(String(row.id))
  ElMessage.success('直送箱已刪除')
  await loadData()
}

onMounted(async () => {
  await Promise.all([loadCategories(), loadProducts(), loadData()])
})
</script>
