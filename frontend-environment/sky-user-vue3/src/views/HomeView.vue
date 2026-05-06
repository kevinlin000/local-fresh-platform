<template>
  <section class="home-shell">
    <header class="hero">
      <div>
        <p class="eyebrow">在地小農直送</p>
        <h1>把當季新鮮食材，直接送到你的餐桌</h1>
        <p class="hero-copy">
          先選分類，再把喜歡的單品或直送箱加入購物車，也可以直接發起揪團，和朋友一起湊滿 3 人免運。
        </p>
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
      </div>

      <div class="catalog-layout">
        <aside class="category-panel">
          <div class="panel-header">
            <h2>{{ activeTab === 'product' ? '單品分類' : '直送箱分類' }}</h2>
            <span>{{ activeCategories.length }} 類</span>
          </div>

          <el-skeleton v-if="categoryLoading" :rows="6" animated />

          <el-empty v-else-if="!activeCategories.length" description="目前沒有可用分類" />

          <div v-else class="category-list">
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
            </div>
            <el-button text type="success" @click="refreshCurrentTab">重新整理</el-button>
          </div>

          <el-skeleton v-if="itemsLoading" :rows="8" animated />

          <el-empty
            v-else-if="activeTab === 'product' ? !productItems.length : !giftBoxItems.length"
            description="這個分類目前沒有可販售商品"
          />

          <div v-else-if="activeTab === 'product'" class="product-grid">
            <article
              v-for="product in productItems"
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
import { computed, onMounted, ref, watch } from 'vue'
import { ElMessage } from 'element-plus'
import { useRouter } from 'vue-router'
import {
  addToCart
} from '@/services/cart'
import {
  fetchCategories,
  fetchGiftBoxProducts,
  fetchGiftBoxesByCategory,
  fetchProductsByCategory,
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

const tabOptions = [
  { label: '當季單品', value: 'product' },
  { label: '產地直送箱', value: 'giftbox' }
]

const activeCategories = computed(() =>
  activeTab.value === 'product' ? productCategories.value : giftBoxCategories.value
)

const activeCategoryName = computed(() => {
  const category = activeCategories.value.find((item) => item.id === activeCategoryId.value)
  return category?.name || '請先選擇分類'
})

function formatPrice(value: number) {
  return Number(value || 0).toLocaleString('zh-TW')
}

function selectCategory(categoryId: number) {
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

    if (!activeCategoryId.value || !activeCategories.value.some((item) => item.id === activeCategoryId.value)) {
      activeCategoryId.value = activeCategories.value[0]?.id ?? null
    }
  } finally {
    categoryLoading.value = false
  }
}

async function loadProducts(categoryId: number) {
  itemsLoading.value = true
  try {
    productItems.value = await fetchProductsByCategory(categoryId)
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
  if (!activeCategoryId.value) {
    return
  }
  if (activeTab.value === 'product') {
    await loadProducts(activeCategoryId.value)
    return
  }
  await loadGiftBoxes(activeCategoryId.value)
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
  activeCategoryId.value = activeCategories.value[0]?.id ?? null
})

watch(activeCategoryId, async (categoryId) => {
  if (!categoryId) {
    productItems.value = []
    giftBoxItems.value = []
    return
  }

  if (activeTab.value === 'product') {
    await loadProducts(categoryId)
    return
  }

  await loadGiftBoxes(categoryId)
}, { immediate: false })

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

.hero > div:first-child,
.hero-banner,
.catalog-card {
  border-radius: 28px;
  background: rgba(255, 255, 255, 0.92);
  box-shadow: 0 24px 60px rgba(61, 111, 39, 0.1);
}

.hero > div:first-child {
  padding: 36px 40px;
}

.hero h1 {
  margin: 0;
  color: #21331c;
  font-size: 40px;
  line-height: 1.2;
}

.hero-copy {
  margin: 16px 0 0;
  color: #5a6954;
  line-height: 1.8;
}

.eyebrow {
  margin: 0 0 12px;
  color: #62864e;
  font-size: 13px;
  font-weight: 800;
  letter-spacing: 0.1em;
  text-transform: uppercase;
}

.hero-banner {
  padding: 32px;
  background:
    linear-gradient(160deg, rgba(63, 124, 55, 0.95), rgba(97, 144, 81, 0.9)),
    #4e7f3e;
  color: white;
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
  line-height: 1.7;
}

.catalog-card {
  max-width: 1280px;
  margin: 0 auto;
  padding: 28px;
}

.catalog-toolbar {
  display: flex;
  justify-content: flex-end;
  margin-bottom: 20px;
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
</style>
