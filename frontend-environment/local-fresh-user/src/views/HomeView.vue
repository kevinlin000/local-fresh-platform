<template>
  <section class="home-shell">
    <header class="hero">
      <div class="hero-main" :style="{ backgroundImage: heroBackground }">
        <div class="hero-main-content">
          <p class="eyebrow">在地小農直送</p>
          <h1>把當季新鮮食材，直接送到你的餐桌</h1>
          <p class="hero-copy">
            先選分類，再把喜歡的單品或直送箱加入購物車，也可以直接發起揪團，和朋友一起湊滿 3 人免運。
          </p>
        </div>
      </div>
      <div class="hero-banner">
        <span>揪團中</span>
        <strong>3 人成團免運</strong>
        <p>從商品詳情直接發起揪團，分享連結給朋友一起加入。</p>
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
              @click="openProduct(product.id)"
            >
              <div class="product-image">
                <img v-if="product.image" :src="product.image" :alt="product.productName" />
                <div v-else class="image-placeholder">Fresh</div>
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
                <div v-else class="image-placeholder">Box</div>
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
  padding: 32px 40px 52px;
}

.hero {
  display: grid;
  grid-template-columns: 1.4fr 360px;
  gap: 24px;
  align-items: stretch;
  max-width: 1280px;
  margin: 0 auto 24px;
}

.hero-main,
.hero-banner,
.catalog-card {
  border-radius: 28px;
  background: rgba(255, 255, 255, 0.92);
  box-shadow: 0 24px 60px rgba(61, 111, 39, 0.1);
}

.hero-main {
  padding: 36px 40px;
  min-height: 320px;
  display: flex;
  align-items: flex-end;
  background-size: cover;
  background-position: left center;
  background-repeat: no-repeat;
}

.hero-main-content {
  max-width: 560px;
}

.hero h1 {
  margin: 0;
  color: #21331c;
  font-size: 40px;
  line-height: 1.2;
  font-family: 'Noto Serif TC', serif;
  font-weight: 700;
}

.hero-copy {
  margin: 16px 0 0;
  color: #5a6954;
  line-height: 1.8;
  font-family: 'Noto Sans TC', sans-serif;
}

.eyebrow {
  margin: 0 0 12px;
  color: #62864e;
  font-size: 13px;
  font-weight: 800;
  letter-spacing: 0.1em;
  text-transform: uppercase;
  font-family: 'Noto Sans TC', sans-serif;
}

.hero-banner {
  padding: 32px;
  background:
    linear-gradient(165deg, rgba(74, 124, 58, 0.98), rgba(59, 101, 45, 0.94)),
    #4a7c3a;
  color: white;
  transition: transform 0.18s ease, box-shadow 0.18s ease;
}

.hero-banner:hover {
  transform: scale(1.02);
  box-shadow: 0 28px 64px rgba(53, 88, 41, 0.26);
}

.hero-banner span {
  display: inline-block;
  padding: 6px 12px;
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.18);
  font-size: 12px;
  font-weight: 700;
  letter-spacing: 0.08em;
}

.hero-banner strong {
  display: block;
  margin-top: 18px;
  font-size: 34px;
  line-height: 1.2;
}

.hero-banner p {
  margin: 16px 0 0;
  color: rgba(255, 255, 255, 0.86);
  line-height: 1.85;
  padding-right: 12px;
}

.catalog-card {
  max-width: 1280px;
  margin: 0 auto;
  padding: 28px;
}

.catalog-toolbar {
  display: flex;
  justify-content: space-between;
  gap: 16px;
  margin-bottom: 20px;
}

.catalog-search {
  max-width: 360px;
}

.catalog-layout {
  display: grid;
  grid-template-columns: 260px 1fr;
  gap: 24px;
}

.category-panel {
  padding: 20px;
  border-radius: 24px;
  background: #f5f9f2;
}

.panel-header,
.content-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 12px;
}

.panel-header h2,
.content-header h2 {
  margin: 0;
  color: #23351d;
}

.panel-header span {
  color: #77916d;
  font-size: 13px;
  font-weight: 700;
}

.category-list {
  display: flex;
  flex-direction: column;
  gap: 10px;
  margin-top: 18px;
}

.category-button {
  padding: 14px 16px;
  border: 1px solid rgba(92, 130, 75, 0.12);
  border-radius: 18px;
  background: white;
  color: #41523b;
  font-size: 15px;
  font-weight: 700;
  text-align: left;
  cursor: pointer;
  transition: all 0.18s ease;
}

.category-button:hover,
.category-button.active {
  border-color: rgba(76, 128, 56, 0.4);
  background: linear-gradient(180deg, #fbfef9 0%, #edf7e7 100%);
  color: #28411f;
  transform: translateX(2px);
}

.catalog-content {
  min-width: 0;
}

.content-header {
  margin-bottom: 18px;
}

.result-copy {
  margin: 8px 0 0;
  color: #71806a;
  font-size: 14px;
}

.product-controls {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
  align-items: center;
  margin: -4px 0 18px;
  padding: 14px;
  border-radius: 18px;
  background: #f7faf4;
  border: 1px solid rgba(86, 126, 67, 0.1);
}

.sort-select {
  width: 160px;
}

.product-grid,
.giftbox-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 18px;
}

.product-card,
.giftbox-card {
  overflow: hidden;
  border-radius: 24px;
  background: white;
  border: 1px solid rgba(86, 126, 67, 0.12);
  box-shadow: 0 18px 40px rgba(64, 105, 46, 0.08);
}

.product-card {
  cursor: pointer;
  transition: transform 0.18s ease, box-shadow 0.18s ease;
}

.product-card:hover {
  transform: translateY(-4px);
  box-shadow: 0 24px 44px rgba(64, 105, 46, 0.14);
}

.product-image,
.giftbox-image {
  height: 200px;
  background: linear-gradient(145deg, #eef7e8 0%, #d8ead1 100%);
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
  color: #4f7351;
  font-size: 28px;
  font-weight: 800;
  letter-spacing: 0.06em;
}

.product-body,
.giftbox-body {
  padding: 18px;
}

.product-topline {
  display: flex;
  justify-content: space-between;
  gap: 12px;
  align-items: flex-start;
}

.product-topline h3 {
  margin: 0;
  color: #24361f;
  font-size: 18px;
}

.price {
  color: #2e6b1d;
  font-weight: 800;
  white-space: nowrap;
}

.description {
  min-height: 44px;
  margin: 12px 0 0;
  color: #5a6854;
  line-height: 1.7;
}

.giftbox-items {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-top: 16px;
}

.giftbox-items-label {
  width: 100%;
  color: #6b8461;
  font-size: 12px;
  font-weight: 700;
  letter-spacing: 0.08em;
  text-transform: uppercase;
}

.giftbox-chip {
  padding: 6px 10px;
  border-radius: 999px;
  background: #edf5e8;
  color: #43603b;
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
  margin-top: 18px;
}

@media (max-width: 980px) {
  .home-shell {
    padding: 20px 16px 36px;
  }

  .hero {
    grid-template-columns: 1fr;
  }

  .catalog-layout {
    grid-template-columns: 1fr;
  }

  .category-list {
    display: grid;
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .product-grid,
  .giftbox-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 640px) {
  .hero-main {
    min-height: 260px;
    padding: 28px 24px;
  }

  .hero h1 {
    font-size: 30px;
  }

  .catalog-card {
    padding: 18px;
  }

  .catalog-toolbar,
  .content-header {
    flex-direction: column;
    align-items: stretch;
  }

  .catalog-search,
  .sort-select {
    max-width: none;
    width: 100%;
  }

  .category-list,
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
