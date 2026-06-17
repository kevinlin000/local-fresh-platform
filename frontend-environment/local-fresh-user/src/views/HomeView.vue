<template>
  <section class="home-shell">
    <header class="hero">
      <div class="hero-main" :style="{ backgroundImage: heroBackground }">
        <div class="hero-main-content">
          <p class="eyebrow">在地小農直送</p>
          <h1>今天的採買清單，從產地直送開始</h1>
          <p class="hero-copy">
            挑選當季單品、主題直送箱，或從商品頁發起 3 人揪團免運。
          </p>
        </div>
      </div>
      <div class="hero-banner">
        <span>Group Buy</span>
        <strong>3 人成團免運</strong>
        <p>適合鄰居、同事與家庭共同採買，成團後訂單自動轉待確認。</p>
      </div>
    </header>

    <section class="catalog-card">
      <div class="catalog-toolbar">
        <el-segmented v-model="activeTab" :options="tabOptions" />
        <el-input
          v-if="activeTab === 'product'"
          v-model="productSearchDraft"
          class="catalog-search"
          clearable
          placeholder="搜尋蔬果、肉品或商品關鍵字"
        />
      </div>

      <div class="catalog-layout">
        <aside class="category-panel">
          <div class="panel-header">
            <h2>{{ activeTab === 'product' ? '單品分類' : '直送箱分類' }}</h2>
            <span>{{ activeTab === 'product' ? activeCategories.length + 1 : activeCategories.length }} 類</span>
          </div>

          <el-skeleton v-if="categoryLoading" :rows="6" animated />

          <el-empty
            v-else-if="activeTab === 'giftbox' && !activeCategories.length"
            description="目前沒有可用分類"
          />

          <div v-else class="category-list">
            <button
              v-if="activeTab === 'product'"
              class="category-button"
              :class="{ active: activeCategoryId === null }"
              @click="selectCategory(null)"
            >
              <span>全部單品</span>
            </button>
            <button
              v-for="category in activeCategories"
              :key="category.id"
              class="category-button"
              :class="{ active: activeCategoryId === category.id }"
              @click="selectCategory(category.id)"
            >
              <span>{{ category.name }}</span>
            </button>
          </div>
        </aside>

        <div class="catalog-content">
          <div class="content-header">
            <div>
              <p class="eyebrow">{{ activeTab === 'product' ? '鮮選單品' : '主題直送箱' }}</p>
              <h2>{{ activeCategoryName }}</h2>
              <p v-if="activeTab === 'product'" class="result-copy">{{ productResultCopy }}</p>
            </div>
            <el-button text type="success" @click="refreshCurrentTab">重新整理</el-button>
          </div>

          <div v-if="activeTab === 'product'" class="product-controls">
            <el-select v-model="productSort" class="sort-select" placeholder="排序">
              <el-option label="最新上架" value="recommended" />
              <el-option label="價格低到高" value="priceAsc" />
              <el-option label="價格高到低" value="priceDesc" />
              <el-option label="商品名稱 A-Z" value="nameAsc" />
            </el-select>
            <el-input-number
              v-model="productPriceCap"
              :min="0"
              :step="50"
              controls-position="right"
              placeholder="最高價格"
            />
            <el-button @click="resetProductFilters">清除篩選</el-button>
          </div>

          <el-skeleton v-if="itemsLoading" :rows="8" animated />

          <el-empty
            v-else-if="activeTab === 'product' ? !displayedProductItems.length : !giftBoxItems.length"
            :description="activeTab === 'product' ? '找不到符合條件的可販售商品' : '這個分類目前沒有可販售商品'"
          />

          <div v-else-if="activeTab === 'product'" class="product-grid">
            <article
              v-for="product in displayedProductItems"
              :key="product.id"
              class="product-card"
              role="button"
              tabindex="0"
              @click="openProduct(product.id)"
              @keyup.enter="openProduct(product.id)"
            >
              <div class="product-image">
                <img v-if="product.image" :src="product.image" :alt="product.productName" />
                <div v-else class="image-placeholder">暫無圖片</div>
              </div>
              <div class="product-body">
                <div class="product-topline">
                  <h3>{{ product.productName }}</h3>
                  <span class="price">NT$ {{ formatPrice(product.price) }}</span>
                </div>
                <p class="description">{{ product.description || '當季鮮採，適合家常料理。' }}</p>
              </div>
            </article>
          </div>

          <div v-else class="giftbox-grid">
            <article v-for="giftBox in giftBoxItems" :key="giftBox.id" class="giftbox-card">
              <div class="giftbox-image">
                <img v-if="giftBox.image" :src="giftBox.image" :alt="giftBox.boxName" />
                <div v-else class="image-placeholder">暫無圖片</div>
              </div>
              <div class="giftbox-body">
                <div class="product-topline">
                  <h3>{{ giftBox.boxName }}</h3>
                  <span class="price">NT$ {{ formatPrice(giftBox.price) }}</span>
                </div>
                <p class="description">{{ giftBox.description || '精選主題箱，一次帶走多樣食材。' }}</p>
                <div class="giftbox-items">
                  <span class="giftbox-items-label">內含商品</span>
                  <el-skeleton v-if="giftBox.loading" :rows="2" animated />
                  <template v-else>
                    <span
                      v-for="item in giftBox.items.slice(0, 4)"
                      :key="`${giftBox.id}-${item.name}`"
                      class="giftbox-chip"
                    >
                      {{ item.name }} x{{ item.copies }}
                    </span>
                    <span v-if="!giftBox.items.length" class="giftbox-chip muted">內容整理中</span>
                  </template>
                </div>
                <div class="giftbox-actions">
                  <el-button type="success" plain @click="addGiftBoxToCart(giftBox.id)">
                    加入購物車
                  </el-button>
                </div>
              </div>
            </article>
          </div>
        </div>
      </div>
    </section>
  </section>
</template>

<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import { ElMessage } from 'element-plus'
import { useRouter } from 'vue-router'
import heroImage from '@/assets/brand/hero.png'
import {
  addToCart
} from '@/services/cart'
import {
  fetchCategories,
  fetchGiftBoxProducts,
  fetchGiftBoxesByCategory,
  fetchProducts,
  type Category,
  type GiftBox,
  type Product,
  type ProductItem
} from '@/services/catalog'

type CatalogTab = 'product' | 'giftbox'

interface GiftBoxCard extends GiftBox {
  items: ProductItem[]
  loading: boolean
}

const router = useRouter()

const activeTab = ref<CatalogTab>('product')
const categoryLoading = ref(false)
const itemsLoading = ref(false)

const productCategories = ref<Category[]>([])
const giftBoxCategories = ref<Category[]>([])
const activeCategoryId = ref<number | null>(null)

const productItems = ref<Product[]>([])
const giftBoxItems = ref<GiftBoxCard[]>([])
const productSearchDraft = ref('')
const productSearchTerm = ref('')
const productPriceCap = ref<number | null>(null)
const productSort = ref<'recommended' | 'priceAsc' | 'priceDesc' | 'nameAsc'>('recommended')
let productSearchTimer: ReturnType<typeof setTimeout> | undefined

const tabOptions = [
  { label: '當季單品', value: 'product' },
  { label: '產地直送箱', value: 'giftbox' }
]

const activeCategories = computed(() =>
  activeTab.value === 'product' ? productCategories.value : giftBoxCategories.value
)

const activeCategoryName = computed(() => {
  if (activeTab.value === 'product' && activeCategoryId.value === null) {
    return '全部單品'
  }
  const category = activeCategories.value.find((item) => item.id === activeCategoryId.value)
  return category?.name || '請先選擇分類'
})

const heroBackground = `linear-gradient(rgba(245, 240, 230, 0.42), rgba(245, 240, 230, 0.42)), url(${heroImage})`

const displayedProductItems = computed(() => {
  const priceCap = productPriceCap.value
  const products = productItems.value
    .filter((product) => priceCap === null || Number(product.price || 0) <= priceCap)
    .slice()

  if (productSort.value === 'priceAsc') {
    return products.sort((a, b) => Number(a.price || 0) - Number(b.price || 0))
  }
  if (productSort.value === 'priceDesc') {
    return products.sort((a, b) => Number(b.price || 0) - Number(a.price || 0))
  }
  if (productSort.value === 'nameAsc') {
    return products.sort((a, b) => a.productName.localeCompare(b.productName, 'zh-Hant'))
  }
  return products
})

const productResultCopy = computed(() => {
  const keyword = productSearchTerm.value ? `「${productSearchTerm.value}」` : '所有商品'
  return `${keyword}，顯示 ${displayedProductItems.value.length} / ${productItems.value.length} 項可販售商品`
})

function formatPrice(value: number) {
  return Number(value || 0).toLocaleString('zh-TW')
}

function selectCategory(categoryId: number | null) {
  activeCategoryId.value = categoryId
}

async function loadCategories() {
  categoryLoading.value = true
  try {
    const [products, giftBoxes] = await Promise.all([
      fetchCategories(1),
      fetchCategories(2)
    ])
    productCategories.value = products
    giftBoxCategories.value = giftBoxes

    if (activeTab.value === 'product') {
      activeCategoryId.value = activeCategoryId.value && products.some((item) => item.id === activeCategoryId.value)
        ? activeCategoryId.value
        : null
      return
    }

    if (!activeCategoryId.value || !giftBoxes.some((item) => item.id === activeCategoryId.value)) {
      activeCategoryId.value = activeCategories.value[0]?.id ?? null
    }
  } finally {
    categoryLoading.value = false
  }
}

async function loadProducts(categoryId: number | null) {
  itemsLoading.value = true
  try {
    productItems.value = await fetchProducts({
      categoryId,
      productName: productSearchTerm.value
    })
  } finally {
    itemsLoading.value = false
  }
}

async function loadGiftBoxes(categoryId: number) {
  itemsLoading.value = true
  try {
    const list = await fetchGiftBoxesByCategory(categoryId)
    giftBoxItems.value = list.map((item) => ({
      ...item,
      items: [],
      loading: true
    }))

    await Promise.all(
      giftBoxItems.value.map(async (giftBox) => {
        try {
          giftBox.items = await fetchGiftBoxProducts(giftBox.id)
        } catch {
          giftBox.items = []
        } finally {
          giftBox.loading = false
        }
      })
    )
  } finally {
    itemsLoading.value = false
  }
}

async function refreshCurrentTab() {
  if (activeTab.value === 'product') {
    await loadProducts(activeCategoryId.value)
    return
  }

  if (!activeCategoryId.value) {
    return
  }
  await loadGiftBoxes(activeCategoryId.value)
}

function resetProductFilters() {
  productSearchDraft.value = ''
  productSearchTerm.value = ''
  productPriceCap.value = null
  productSort.value = 'recommended'
}

async function addGiftBoxToCart(giftBoxId: number) {
  try {
    await addToCart({ giftBoxId })
    ElMessage.success('已加入購物車')
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '加入購物車失敗')
  }
}

function openProduct(id: number) {
  void router.push(`/product/${id}`)
}

watch(activeTab, () => {
  activeCategoryId.value = activeTab.value === 'product' ? null : activeCategories.value[0]?.id ?? null
})

watch(activeCategoryId, async (categoryId) => {
  if (activeTab.value === 'product') {
    await loadProducts(categoryId)
    return
  }

  if (!categoryId) {
    giftBoxItems.value = []
    return
  }

  await loadGiftBoxes(categoryId)
}, { immediate: false })

watch(productSearchDraft, (value) => {
  if (productSearchTimer) {
    clearTimeout(productSearchTimer)
  }
  productSearchTimer = setTimeout(() => {
    productSearchTerm.value = value.trim()
  }, 300)
})

watch(productSearchTerm, async () => {
  if (activeTab.value === 'product') {
    await loadProducts(activeCategoryId.value)
  }
})

onMounted(async () => {
  try {
    await loadCategories()
    if (activeCategoryId.value) {
      await refreshCurrentTab()
    }
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '載入首頁資料失敗')
  }
})

onBeforeUnmount(() => {
  if (productSearchTimer) {
    clearTimeout(productSearchTimer)
  }
})
</script>

<style scoped>
.home-shell {
  padding: 24px 0 48px;
}

.hero {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 300px;
  gap: 16px;
  align-items: stretch;
  margin: 0 0 18px;
}

.hero-main,
.hero-banner,
.catalog-card {
  border: 1px solid var(--farm-line);
  border-radius: 8px;
  background: var(--farm-surface);
}

.hero-main {
  min-height: 260px;
  padding: 28px 32px;
  display: flex;
  align-items: flex-end;
  background-size: cover;
  background-position: center;
  background-repeat: no-repeat;
}

.hero-main-content {
  max-width: 620px;
  padding: 18px 20px;
  border-radius: 8px;
  background: rgba(255, 253, 248, 0.86);
  backdrop-filter: blur(8px);
}

.hero h1 {
  margin: 0;
  color: var(--farm-text);
  font-size: 34px;
  line-height: 1.2;
  font-weight: 800;
}

.hero-copy {
  margin: 12px 0 0;
  color: var(--farm-muted);
  line-height: 1.7;
}

.eyebrow {
  margin: 0 0 8px;
  color: var(--farm-accent);
  font-size: 12px;
  font-weight: 800;
  letter-spacing: 0;
  text-transform: uppercase;
}

.hero-banner {
  padding: 24px;
  background:
    linear-gradient(180deg, rgba(47, 111, 78, 0.96), rgba(31, 76, 53, 0.98)),
    var(--farm-primary-deep);
  color: #fffdf8;
}

.hero-banner span {
  display: inline-block;
  padding: 5px 9px;
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.18);
  font-size: 12px;
  font-weight: 700;
  letter-spacing: 0;
}

.hero-banner strong {
  display: block;
  margin-top: 16px;
  font-size: 28px;
  line-height: 1.2;
}

.hero-banner p {
  margin: 16px 0 0;
  color: rgba(255, 255, 255, 0.86);
  line-height: 1.7;
}

.catalog-card {
  padding: 20px;
  box-shadow: none;
}

.catalog-toolbar {
  display: flex;
  justify-content: space-between;
  gap: 16px;
  margin-bottom: 20px;
}

.catalog-search {
  width: min(360px, 100%);
}

.catalog-layout {
  display: grid;
  grid-template-columns: 220px minmax(0, 1fr);
  gap: 20px;
}

.category-panel {
  padding: 14px;
  border: 1px solid var(--farm-line);
  border-radius: 8px;
  background: #fbfaf6;
  height: fit-content;
  position: sticky;
  top: 98px;
}

.panel-header,
.content-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 10px;
}

.panel-header h2,
.content-header h2 {
  margin: 0;
  color: var(--farm-text);
}

.panel-header span {
  color: var(--farm-muted);
  font-size: 13px;
  font-weight: 700;
}

.category-list {
  display: flex;
  flex-direction: column;
  gap: 8px;
  margin-top: 14px;
}

.category-button {
  padding: 11px 12px;
  border: 1px solid transparent;
  border-radius: 8px;
  background: white;
  color: #405047;
  font-size: 14px;
  font-weight: 700;
  text-align: left;
  cursor: pointer;
  transition: border-color 0.18s ease, background-color 0.18s ease, color 0.18s ease;
}

.category-button:hover,
.category-button.active {
  border-color: rgba(47, 111, 78, 0.26);
  background: var(--farm-primary-soft);
  color: var(--farm-primary-deep);
}

.catalog-content {
  min-width: 0;
}

.content-header {
  margin-bottom: 14px;
}

.result-copy {
  margin: 8px 0 0;
  color: var(--farm-muted);
  font-size: 14px;
}

.product-controls {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  align-items: center;
  margin: 0 0 16px;
  padding: 12px;
  border-radius: 8px;
  background: #fbfaf6;
  border: 1px solid var(--farm-line);
}

.sort-select {
  width: 160px;
}

.product-grid,
.giftbox-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(210px, 1fr));
  gap: 14px;
}

.product-card,
.giftbox-card {
  overflow: hidden;
  border-radius: 8px;
  background: white;
  border: 1px solid var(--farm-line);
  box-shadow: none;
}

.product-card {
  cursor: pointer;
  transition: border-color 0.18s ease, box-shadow 0.18s ease;
}

.product-card:hover,
.product-card:focus-visible {
  outline: none;
  border-color: rgba(47, 111, 78, 0.36);
  box-shadow: 0 10px 24px rgba(28, 39, 32, 0.08);
}

.product-image,
.giftbox-image {
  height: 178px;
  background: #edf1e9;
}

.product-image img,
.giftbox-image img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.image-placeholder {
  display: grid;
  place-items: center;
  width: 100%;
  height: 100%;
  color: var(--farm-muted);
  font-size: 14px;
  font-weight: 800;
  letter-spacing: 0;
}

.product-body,
.giftbox-body {
  padding: 14px;
}

.product-topline {
  display: flex;
  justify-content: space-between;
  gap: 12px;
  align-items: flex-start;
}

.product-topline h3 {
  margin: 0;
  color: var(--farm-text);
  font-size: 16px;
  line-height: 1.45;
}

.price {
  color: var(--farm-primary-deep);
  font-weight: 800;
  white-space: nowrap;
}

.description {
  min-height: 42px;
  margin: 10px 0 0;
  color: var(--farm-muted);
  line-height: 1.6;
  font-size: 14px;
}

.giftbox-items {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-top: 14px;
}

.giftbox-items-label {
  width: 100%;
  color: var(--farm-muted);
  font-size: 12px;
  font-weight: 700;
  letter-spacing: 0;
  text-transform: uppercase;
}

.giftbox-chip {
  padding: 5px 8px;
  border-radius: 999px;
  background: var(--farm-primary-soft);
  color: var(--farm-primary-deep);
  font-size: 12px;
  font-weight: 700;
}

.giftbox-chip.muted {
  background: #f1f3ef;
  color: #7c8576;
}

.giftbox-actions {
  display: flex;
  justify-content: flex-end;
  margin-top: 14px;
}

@media (max-width: 980px) {
  .home-shell {
    padding: 16px 0 36px;
  }

  .hero {
    grid-template-columns: 1fr;
  }

  .catalog-layout {
    grid-template-columns: 1fr;
  }

  .category-panel {
    position: static;
  }

  .category-list {
    display: grid;
    grid-template-columns: repeat(auto-fit, minmax(130px, 1fr));
  }
}

@media (max-width: 640px) {
  .hero-main {
    min-height: 240px;
    padding: 16px;
  }

  .hero-main-content {
    padding: 14px;
  }

  .hero h1 {
    font-size: 26px;
  }

  .catalog-card {
    padding: 14px;
  }

  .catalog-toolbar,
  .content-header {
    flex-direction: column;
    align-items: stretch;
  }

  .catalog-search,
  .sort-select {
    width: 100%;
  }

  .product-grid,
  .giftbox-grid {
    grid-template-columns: 1fr;
  }

  .product-controls :deep(.el-input-number),
  .product-controls .el-button {
    width: 100%;
  }
}
</style>
