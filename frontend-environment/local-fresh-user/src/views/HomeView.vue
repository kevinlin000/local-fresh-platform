<template>
  <section class="home-shell">
    <header class="storefront-header">
      <div class="storefront-copy">
        <p class="eyebrow">今日市場</p>
        <h1>把一週餐桌補齊，從當季鮮選開始</h1>
        <p class="header-copy">
          依分類快速挑選蔬果、肉品、海鮮與直送箱；下單後由門市確認配送與備貨。
        </p>
        <div class="header-facts" aria-label="今日市場狀態">
          <span><strong>{{ productItems.length }}</strong> 項商品</span>
          <span>產地直送箱</span>
          <span>台北、新北配送</span>
        </div>
        <div v-if="quickCategoryOptions.length" class="quick-categories" aria-label="快速選擇商品分類">
          <button
            class="quick-category"
            :class="{ active: activeTab === 'product' && activeCategoryId === null }"
            type="button"
            @click="showProductCategory(null)"
          >
            全部
          </button>
          <button
            v-for="category in quickCategoryOptions"
            :key="category.id"
            class="quick-category"
            :class="{ active: activeTab === 'product' && activeCategoryId === category.id }"
            type="button"
            @click="showProductCategory(category.id)"
          >
            {{ category.name }}
          </button>
        </div>
        <div class="header-actions" aria-label="快速採買操作">
          <el-button type="success" @click="scrollToCatalog">開始採買</el-button>
          <el-button plain @click="switchToGiftBoxes">查看直送箱</el-button>
        </div>
        <div v-if="featuredShelves.length" class="market-board" aria-label="本週推薦補貨清單">
          <div class="market-board-header">
            <span>本週補貨清單</span>
            <small>價格、配送與用途一次看</small>
          </div>
          <div class="market-shelves">
            <button
              v-for="item in featuredShelves"
              :key="item.id"
              class="shelf-button"
              type="button"
              @click="openProduct(item.id)"
            >
              <span>{{ item.tag }}</span>
              <strong>{{ item.product.productName }}</strong>
              <small>NT$ {{ formatPrice(item.product.price) }} · {{ productDeliverySignal(item.product) }}</small>
            </button>
          </div>
        </div>
      </div>
      <aside class="fulfillment-panel" :style="{ backgroundImage: heroBackground }">
        <div class="fulfillment-overlay">
          <span class="delivery-state">今日接單中</span>
          <strong>3 人揪團免運</strong>
          <p>從商品頁建立揪團，滿員後自動進入訂單確認。</p>
        </div>
      </aside>
    </header>

    <section ref="catalogSectionRef" class="catalog-card">
      <div class="catalog-toolbar">
        <div class="catalog-switch">
          <el-segmented v-model="activeTab" :options="tabOptions" />
          <span>{{ activeItemSummary }}</span>
        </div>
        <el-input
          v-if="activeTab === 'product'"
          v-model="productSearchDraft"
          class="catalog-search"
          clearable
          aria-label="搜尋商品"
          placeholder="搜尋蔬果、肉品或商品關鍵字"
        />
      </div>

      <div class="catalog-layout">
        <aside class="category-panel">
          <div class="panel-header">
            <h2>分類</h2>
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
              <span>全部商品</span>
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
              <p class="eyebrow">{{ activeTab === 'product' ? '鮮選商品' : '產地直送箱' }}</p>
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
            >
              <button class="product-image product-image-button" type="button" @click="openProduct(product.id)">
                <img v-if="product.image" :src="product.image" :alt="product.productName" />
                <div v-else class="image-placeholder">暫無圖片</div>
              </button>
              <div class="product-body">
                <div class="product-meta">
                  <span>{{ resolveProductCategory(product) }}</span>
                  <span>{{ productDeliverySignal(product) }}</span>
                </div>
                <div class="product-topline">
                  <h3>{{ product.productName }}</h3>
                </div>
                <p class="description">{{ product.description || '當季鮮採，適合家常料理。' }}</p>
                <div class="product-signals" aria-label="商品採買資訊">
                  <span>{{ productUsageHint(product) }}</span>
                  <span>3 人揪團免運</span>
                </div>
                <div class="product-footer">
                  <span class="price">NT$ {{ formatPrice(product.price) }}</span>
                  <div class="product-actions">
                    <el-button text type="success" @click="openProduct(product.id)">查看</el-button>
                    <el-button
                      type="success"
                      :loading="addingProductId === product.id"
                      @click="quickAddProductToCart(product)"
                    >
                      加入購物車
                    </el-button>
                  </div>
                </div>
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
                <div class="product-meta">
                  <span>組合箱</span>
                  <span>家庭備菜</span>
                </div>
                <div class="product-topline">
                  <h3>{{ giftBox.boxName }}</h3>
                </div>
                <p class="description">{{ giftBox.description || '精選主題箱，一次帶走多樣食材。' }}</p>
                <div class="giftbox-value" aria-label="直送箱優惠">
                  <span>{{ giftBoxValueCopy(giftBox) }}</span>
                  <strong v-if="giftBoxSavings(giftBox) > 0">約省 NT$ {{ formatPrice(giftBoxSavings(giftBox)) }}</strong>
                </div>
                <div class="giftbox-items">
                  <span class="giftbox-items-label">內含商品</span>
                  <el-skeleton v-if="giftBox.loading" :rows="2" animated />
                  <template v-else>
                    <span
                      v-for="item in giftBox.items.slice(0, 4)"
                      :key="`${giftBox.id}-${item.productId || item.name}`"
                      class="giftbox-chip"
                    >
                      {{ displayGiftBoxItemName(item) }} x{{ item.copies }}
                    </span>
                    <span v-if="!giftBox.items.length" class="giftbox-chip muted">內容整理中</span>
                  </template>
                </div>
                <div class="giftbox-actions">
                  <span class="price">NT$ {{ formatPrice(giftBox.price) }}</span>
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
import { ElMessage } from 'element-plus/es/components/message/index'
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
const catalogSectionRef = ref<HTMLElement | null>(null)
const productSearchDraft = ref('')
const productSearchTerm = ref('')
const productPriceCap = ref<number | null>(null)
const productSort = ref<'recommended' | 'priceAsc' | 'priceDesc' | 'nameAsc'>('recommended')
const addingProductId = ref<number | null>(null)
let productSearchTimer: ReturnType<typeof setTimeout> | undefined

const heroProductNames = [
  '有機高麗菜',
  '雲林溫體豬五花',
  '本土無毒土雞蛋',
  '池上越光米'
]

const heroProductTags = ['今晚青菜', '主菜肉品', '早餐補貨', '主食常備']
const categoryRank: Record<number, number> = {
  1: 1,
  3: 2,
  5: 3,
  6: 4,
  2: 5,
  4: 6,
  7: 7
}

const tabOptions = [
  { label: '當季商品', value: 'product' },
  { label: '產地直送箱', value: 'giftbox' }
]

const activeCategories = computed(() =>
  activeTab.value === 'product' ? productCategories.value : giftBoxCategories.value
)

const quickCategoryOptions = computed(() => productCategories.value.slice(0, 6))

const activeCategoryName = computed(() => {
  if (activeTab.value === 'product' && activeCategoryId.value === null) {
    return '全部商品'
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
  return products.sort((a, b) => recommendedRank(a) - recommendedRank(b))
})

const featuredShelves = computed(() =>
  heroProductNames
    .map((name, index) => {
      const product = productItems.value.find((item) => item.productName.includes(name))
      if (!product) {
        return null
      }
      return {
        id: product.id,
        tag: heroProductTags[index],
        product
      }
    })
    .filter((item): item is { id: number; tag: string; product: Product } => item !== null)
)

const activeItemSummary = computed(() => {
  if (activeTab.value === 'product') {
    return `${displayedProductItems.value.length} / ${productItems.value.length} 項可購買`
  }
  return `${giftBoxItems.value.length} 款直送箱`
})

const productResultCopy = computed(() => {
  const keyword = productSearchTerm.value ? `「${productSearchTerm.value}」` : '所有商品'
  return `${keyword}，顯示 ${displayedProductItems.value.length} / ${productItems.value.length} 項可販售商品`
})

function formatPrice(value: number) {
  return Number(value || 0).toLocaleString('zh-TW')
}

function resolveProductCategory(product: Product) {
  return product.categoryName
    || productCategories.value.find((category) => category.id === product.categoryId)?.name
    || '當季鮮選'
}

function productDeliverySignal(product: Product) {
  const category = resolveProductCategory(product)
  if (category.includes('海鮮') || category.includes('肉')) {
    return '冷鏈排單'
  }
  if (category.includes('蛋') || category.includes('奶')) {
    return '冷藏配送'
  }
  return '明日可配'
}

function productUsageHint(product: Product) {
  const category = resolveProductCategory(product)
  if (category.includes('葉菜')) {
    return '今晚青菜'
  }
  if (category.includes('肉')) {
    return '主菜備料'
  }
  if (category.includes('海鮮')) {
    return '快煮鮮食'
  }
  if (category.includes('蛋') || category.includes('奶')) {
    return '早餐補貨'
  }
  if (category.includes('雜糧')) {
    return '主食常備'
  }
  if (category.includes('調味')) {
    return '料理增香'
  }
  return '家庭備菜'
}

function giftBoxItemCount(giftBox: GiftBoxCard) {
  return giftBox.items.reduce((sum, item) => sum + Number(item.copies || 0), 0)
}

function productPriceByName(name: string) {
  return productItems.value.find((product) => product.productName === name)?.price
}

function productById(productId?: number) {
  return productItems.value.find((product) => Number(product.id) === Number(productId))
}

function displayGiftBoxItemName(item: ProductItem) {
  return item.name || productById(item.productId)?.productName || '未命名商品'
}

function giftBoxItemPrice(item: ProductItem) {
  return item.price || productById(item.productId)?.price || productPriceByName(item.name) || 0
}

function giftBoxMarketTotal(giftBox: GiftBoxCard) {
  return giftBox.items.reduce((sum, item) => {
    const price = giftBoxItemPrice(item)
    return price ? sum + Number(price) * Number(item.copies || 1) : sum
  }, 0)
}

function giftBoxSavings(giftBox: GiftBoxCard) {
  return Math.max(0, giftBoxMarketTotal(giftBox) - Number(giftBox.price || 0))
}

function giftBoxValueCopy(giftBox: GiftBoxCard) {
  const count = giftBoxItemCount(giftBox)
  if (!count) {
    return '內容整理中，適合一次補齊家庭備菜'
  }
  const marketTotal = giftBoxMarketTotal(giftBox)
  if (marketTotal > 0 && giftBoxSavings(giftBox) > 0) {
    return `${count} 件食材，單買約 NT$ ${formatPrice(marketTotal)}`
  }
  return `${count} 件食材，一箱補齊免逐項挑選`
}

function recommendedRank(product: Product) {
  const heroIndex = heroProductNames.findIndex((name) => product.productName.includes(name))
  if (heroIndex >= 0) {
    return heroIndex
  }
  return 100 + (categoryRank[product.categoryId] ?? 9) * 100 + product.id
}

function selectCategory(categoryId: number | null) {
  activeCategoryId.value = categoryId
}

function showProductCategory(categoryId: number | null) {
  activeTab.value = 'product'
  activeCategoryId.value = categoryId
  requestAnimationFrame(() => scrollToCatalog())
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

function scrollToCatalog() {
  catalogSectionRef.value?.scrollIntoView({ behavior: 'smooth', block: 'start' })
}

function switchToGiftBoxes() {
  activeTab.value = 'giftbox'
  requestAnimationFrame(() => scrollToCatalog())
}

async function quickAddProductToCart(product: Product) {
  try {
    addingProductId.value = product.id
    await addToCart({ productId: product.id })
    ElMessage.success(`已加入購物車：${product.productName}`)
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '加入購物車失敗')
  } finally {
    addingProductId.value = null
  }
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

watch(activeTab, async () => {
  const nextCategoryId = activeTab.value === 'product' ? null : activeCategories.value[0]?.id ?? null
  const categoryChanged = activeCategoryId.value !== nextCategoryId
  activeCategoryId.value = nextCategoryId

  if (!categoryChanged) {
    await refreshCurrentTab()
  }
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
    await refreshCurrentTab()
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
  padding: 16px 0 48px;
}

.storefront-header {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 328px;
  gap: 18px;
  align-items: stretch;
  margin: 0 0 18px;
}

.storefront-copy {
  display: flex;
  flex-direction: column;
  justify-content: center;
  min-height: 212px;
  padding: 14px 0 16px;
  border-bottom: 1px solid var(--farm-line);
}

.storefront-copy h1 {
  max-width: 680px;
  margin: 0;
  color: var(--farm-text);
  font-size: 32px;
  line-height: 1.2;
  font-weight: 780;
}

.header-copy {
  max-width: 640px;
  margin: 10px 0 0;
  color: var(--farm-muted);
  font-size: 15px;
  line-height: 1.68;
}

.header-facts {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  margin-top: 16px;
  color: #4f5d55;
  font-size: 13px;
}

.header-facts span {
  display: inline-flex;
  align-items: center;
  min-height: 32px;
  padding: 0 10px;
  border: 1px solid rgba(41, 59, 49, 0.12);
  border-radius: 7px;
  background: rgba(255, 255, 255, 0.72);
}

.header-facts strong {
  margin-right: 4px;
  color: var(--farm-primary-deep);
  font-size: 16px;
}

.quick-categories {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-top: 12px;
}

.quick-category {
  min-height: 30px;
  padding: 0 10px;
  border: 1px solid rgba(41, 59, 49, 0.12);
  border-radius: 7px;
  background: #fffdf8;
  color: #405047;
  font-size: 13px;
  font-weight: 750;
  cursor: pointer;
  transition: border-color 0.18s ease, background-color 0.18s ease, color 0.18s ease;
}

.quick-category:hover,
.quick-category:focus-visible,
.quick-category.active {
  outline: none;
  border-color: rgba(47, 111, 78, 0.28);
  background: #edf2ee;
  color: var(--farm-primary-deep);
}

.header-actions {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  margin-top: 14px;
}

.market-board {
  margin-top: 14px;
  max-width: 760px;
}

.market-board-header {
  display: flex;
  justify-content: space-between;
  gap: 10px;
  align-items: center;
  margin-bottom: 8px;
}

.market-board-header span {
  color: var(--farm-text);
  font-size: 14px;
  font-weight: 820;
}

.market-board-header small {
  color: var(--farm-muted);
  font-size: 12px;
  font-weight: 650;
}

.market-shelves {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 8px;
}

.shelf-button {
  display: flex;
  min-height: 86px;
  flex-direction: column;
  justify-content: flex-start;
  gap: 5px;
  padding: 10px;
  border: 1px solid rgba(41, 59, 49, 0.12);
  border-radius: 8px;
  background: #ffffff;
  color: var(--farm-text);
  text-align: left;
  cursor: pointer;
  transition: border-color 0.18s ease, box-shadow 0.18s ease, transform 0.18s ease;
}

.shelf-button:hover,
.shelf-button:focus-visible {
  outline: none;
  border-color: rgba(47, 111, 78, 0.34);
  box-shadow: 0 8px 18px rgba(28, 39, 32, 0.07);
  transform: translateY(-1px);
}

.shelf-button span {
  color: var(--farm-accent);
  font-size: 12px;
  font-weight: 800;
}

.shelf-button strong {
  color: var(--farm-text);
  font-size: 14px;
  line-height: 1.38;
}

.shelf-button small {
  margin-top: auto;
  color: var(--farm-muted);
  font-size: 12px;
  font-weight: 700;
  line-height: 1.35;
}

.fulfillment-panel {
  display: flex;
  align-items: flex-end;
  min-height: 212px;
  overflow: hidden;
  border: 1px solid rgba(41, 59, 49, 0.12);
  border-radius: 8px;
  background-position: center;
  background-size: cover;
  box-shadow: 0 8px 20px rgba(28, 39, 32, 0.07);
}

.fulfillment-overlay {
  width: 100%;
  padding: 18px;
  color: #fffaf1;
  background: linear-gradient(180deg, rgba(27, 37, 31, 0.08) 0%, rgba(22, 32, 26, 0.78) 44%, rgba(18, 27, 22, 0.92) 100%);
}

.delivery-state {
  display: inline-flex;
  align-items: center;
  gap: 7px;
  font-size: 12px;
  font-weight: 700;
}

.delivery-state::before {
  width: 7px;
  height: 7px;
  border-radius: 999px;
  background: #9be27d;
  content: "";
}

.fulfillment-overlay strong {
  display: block;
  margin-top: 10px;
  font-size: 22px;
  line-height: 1.25;
}

.fulfillment-overlay p {
  margin: 8px 0 0;
  color: rgba(255, 250, 241, 0.84);
  line-height: 1.65;
}

.eyebrow {
  margin: 0 0 8px;
  color: var(--farm-accent);
  font-size: 12px;
  font-weight: 800;
}

.catalog-card {
  padding: 0;
  border: none;
  background: transparent;
  box-shadow: none;
}

.catalog-toolbar {
  display: flex;
  justify-content: space-between;
  gap: 16px;
  align-items: center;
  margin-bottom: 14px;
  padding: 12px;
  border: 1px solid var(--farm-line);
  border-radius: 8px;
  background: rgba(255, 255, 255, 0.9);
  box-shadow: 0 5px 14px rgba(28, 39, 32, 0.045);
}

.catalog-switch {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
  align-items: center;
}

.catalog-switch span {
  color: var(--farm-muted);
  font-size: 13px;
  font-weight: 700;
}

.catalog-switch :deep(.el-segmented) {
  --el-segmented-bg-color: #eef1ec;
  --el-segmented-item-selected-bg-color: #ffffff;
  --el-segmented-item-selected-color: var(--farm-primary-deep);
  --el-border-radius-base: 7px;
}

.catalog-search {
  width: min(360px, 100%);
}

.catalog-layout {
  display: grid;
  grid-template-columns: 188px minmax(0, 1fr);
  gap: 18px;
}

.category-panel {
  padding: 8px 0;
  border: none;
  background: transparent;
  height: fit-content;
  position: sticky;
  top: 88px;
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

.panel-header h2 {
  font-size: 15px;
}

.panel-header span {
  color: var(--farm-muted);
  font-size: 13px;
  font-weight: 700;
}

.category-list {
  display: flex;
  flex-direction: column;
  gap: 6px;
  margin-top: 12px;
}

.category-button {
  padding: 10px 11px;
  border: 1px solid transparent;
  border-radius: 7px;
  background: transparent;
  color: #405047;
  font-size: 14px;
  font-weight: 700;
  text-align: left;
  cursor: pointer;
  transition: border-color 0.18s ease, background-color 0.18s ease, box-shadow 0.18s ease, color 0.18s ease;
}

.category-button:hover,
.category-button.active {
  border-color: rgba(47, 111, 78, 0.18);
  background: #ffffff;
  color: var(--farm-primary-deep);
  box-shadow: 0 7px 18px rgba(28, 39, 32, 0.06);
}

.catalog-content {
  min-width: 0;
  padding: 16px;
  border: 1px solid var(--farm-line);
  border-radius: 8px;
  background: rgba(255, 255, 255, 0.94);
  box-shadow: 0 6px 18px rgba(28, 39, 32, 0.055);
}

.content-header {
  margin-bottom: 12px;
}

.content-header h2 {
  font-size: 22px;
  line-height: 1.25;
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
  padding: 10px;
  border-radius: 7px;
  background: #f7f8f5;
  border: 1px solid var(--farm-line);
}

.sort-select {
  width: 160px;
}

.product-grid,
.giftbox-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(214px, 1fr));
  gap: 14px;
}

.product-card,
.giftbox-card {
  display: flex;
  flex-direction: column;
  overflow: hidden;
  border-radius: 8px;
  background: #ffffff;
  border: 1px solid var(--farm-line);
  box-shadow: none;
}

.product-card {
  transition: border-color 0.18s ease, box-shadow 0.18s ease;
}

.product-card:hover,
.product-card:focus-visible {
  outline: none;
  border-color: rgba(47, 111, 78, 0.36);
  box-shadow: 0 10px 22px rgba(28, 39, 32, 0.075);
}

.product-image,
.giftbox-image {
  height: 156px;
  background: #eef1eb;
}

.product-image-button {
  display: block;
  width: 100%;
  padding: 0;
  border: 0;
  cursor: pointer;
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
  display: flex;
  flex: 1;
  flex-direction: column;
  padding: 12px;
}

.product-meta {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
  margin-bottom: 9px;
  color: #617067;
  font-size: 12px;
  font-weight: 700;
}

.product-meta span {
  display: inline-flex;
  align-items: center;
  min-height: 22px;
  padding: 0 7px;
  border-radius: 6px;
  background: #f2f4ef;
}

.product-topline {
  min-height: 44px;
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
  margin: 8px 0 0;
  color: var(--farm-muted);
  line-height: 1.55;
  font-size: 14px;
}

.product-signals {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 6px;
  margin-top: 10px;
}

.product-signals span {
  min-width: 0;
  padding: 7px 8px;
  border: 1px solid rgba(47, 111, 78, 0.12);
  border-radius: 7px;
  background: #fbfcf8;
  color: #405047;
  font-size: 12px;
  font-weight: 800;
  text-align: center;
}

.product-footer {
  display: flex;
  justify-content: space-between;
  gap: 12px;
  align-items: center;
  margin-top: auto;
  padding-top: 12px;
}

.product-actions {
  display: flex;
  flex-wrap: wrap;
  justify-content: flex-end;
  gap: 6px;
}

.product-actions :deep(.el-button) {
  margin-left: 0;
}

.giftbox-items {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-top: 14px;
}

.giftbox-value {
  display: grid;
  gap: 5px;
  margin-top: 12px;
  padding: 10px;
  border: 1px solid rgba(47, 111, 78, 0.12);
  border-radius: 8px;
  background: #f7faf5;
  color: #405047;
  font-size: 12px;
  font-weight: 760;
  line-height: 1.45;
}

.giftbox-value strong {
  color: var(--farm-primary-deep);
  font-size: 13px;
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
  max-width: 100%;
  padding: 5px 8px;
  border-radius: 7px;
  background: var(--farm-primary-soft);
  color: var(--farm-primary-deep);
  font-size: 12px;
  font-weight: 700;
  line-height: 1.35;
  white-space: normal;
}

.giftbox-chip.muted {
  background: #f1f3ef;
  color: #7c8576;
}

.giftbox-actions {
  display: flex;
  justify-content: space-between;
  gap: 12px;
  align-items: center;
  margin-top: 14px;
}

@media (max-width: 980px) {
  .home-shell {
    padding: 16px 0 36px;
  }

  .storefront-header {
    grid-template-columns: 1fr;
  }

  .storefront-copy {
    min-height: auto;
    padding: 8px 0 18px;
  }

  .catalog-layout {
    grid-template-columns: 1fr;
  }

  .market-shelves {
    grid-template-columns: repeat(2, minmax(0, 1fr));
    max-width: none;
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
  .home-shell {
    padding-top: 10px;
  }

  .storefront-copy h1 {
    font-size: 26px;
  }

  .header-copy {
    font-size: 15px;
    line-height: 1.65;
  }

  .header-facts {
    gap: 8px;
    margin-top: 16px;
  }

  .header-facts span {
    min-height: 30px;
  }

  .header-actions {
    margin-top: 14px;
  }

  .fulfillment-panel {
    min-height: 150px;
  }

  .catalog-card {
    padding: 0;
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

  .market-shelves {
    grid-template-columns: repeat(2, minmax(0, 1fr));
    gap: 8px;
  }

  .shelf-button {
    min-height: 66px;
    padding: 10px;
  }

  .shelf-button strong {
    font-size: 13px;
  }

  .product-grid,
  .giftbox-grid {
    grid-template-columns: 1fr;
  }

  .product-controls :deep(.el-input-number),
  .product-controls .el-button {
    width: 100%;
  }

  .product-footer {
    align-items: flex-start;
  }

  .product-actions {
    width: 100%;
    justify-content: space-between;
  }

  .product-actions :deep(.el-button:not(.is-text)) {
    min-width: 92px;
  }
}
</style>
