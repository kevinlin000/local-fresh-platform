<template>
  <section class="detail-shell">
    <el-skeleton v-if="loading" animated :rows="8" />

    <template v-else-if="product">
      <div class="detail-card">
        <div class="media-panel">
          <img v-if="product.image" :src="product.image" :alt="product.productName" />
          <div v-else class="media-placeholder">Fresh</div>
        </div>

        <div class="content-panel">
          <p class="eyebrow">商品詳情</p>
          <h1>{{ product.productName }}</h1>
          <p class="price">NT$ {{ formatPrice(product.price) }}</p>
          <p class="description">{{ product.description || '來自在地產區的當季鮮選，適合日常料理與家庭備菜。' }}</p>

          <div class="specs-panel">
            <h2>商品規格</h2>
            <el-radio-group v-model="selectedSpec">
              <el-radio-button
                v-for="spec in specOptions"
                :key="spec.key"
                :label="spec.value"
              >
                {{ spec.label }}
              </el-radio-button>
            </el-radio-group>
          </div>

          <div class="actions">
            <el-button type="success" size="large" :loading="submitting" @click="handleAddToCart">
              加入購物車
            </el-button>
            <el-button size="large" @click="handleGroupBuyStub">
              立即揪團
            </el-button>
          </div>
        </div>
      </div>
    </template>

    <el-empty v-else description="找不到這項商品" />
  </section>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { useRoute, useRouter } from 'vue-router'
import { addToCart } from '@/services/cart'
import { fetchProductDetail, type Product } from '@/services/catalog'

const route = useRoute()
const router = useRouter()

const loading = ref(false)
const submitting = ref(false)
const product = ref<Product | null>(null)
const selectedSpec = ref('')

const productId = computed(() => Number(route.params.id))

const specOptions = computed(() => {
  if (!product.value?.productSpecs?.length) {
    return []
  }
  return product.value.productSpecs.map((spec) => ({
    key: `${spec.name}-${spec.value}`,
    label: spec.name ? `${spec.name}｜${spec.value}` : spec.value,
    value: spec.value
  }))
})

function formatPrice(value: number) {
  return Number(value || 0).toLocaleString('zh-TW')
}

async function loadProduct() {
  loading.value = true
  try {
    product.value = await fetchProductDetail(productId.value)
    selectedSpec.value = specOptions.value[0]?.value || ''
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '商品詳情載入失敗')
    product.value = null
  } finally {
    loading.value = false
  }
}

async function handleAddToCart() {
  if (!product.value) {
    return
  }
  try {
    submitting.value = true
    await addToCart({
      productId: product.value.id,
      productSpec: selectedSpec.value || undefined
    })
    ElMessage.success('已加入購物車')
    await router.push('/cart')
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '加入購物車失敗')
  } finally {
    submitting.value = false
  }
}

function handleGroupBuyStub() {
  ElMessage.info('揪團建立流程會在 batch 4.5 接上')
}

onMounted(() => {
  void loadProduct()
})
</script>

<style scoped>
.detail-shell {
  max-width: 1280px;
  margin: 0 auto;
  padding: 32px 40px 52px;
}

.detail-card {
  display: grid;
  grid-template-columns: 520px 1fr;
  gap: 28px;
  padding: 28px;
  border-radius: 30px;
  background: rgba(255, 255, 255, 0.93);
  box-shadow: 0 24px 60px rgba(61, 111, 39, 0.12);
}

.media-panel {
  overflow: hidden;
  height: 480px;
  border-radius: 24px;
  background: linear-gradient(145deg, #edf6e8 0%, #d9ead1 100%);
}

.media-panel img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.media-placeholder {
  display: grid;
  place-items: center;
  width: 100%;
  height: 100%;
  color: #4d7150;
  font-size: 42px;
  font-weight: 800;
  letter-spacing: 0.08em;
}

.content-panel {
  padding: 12px 8px;
}

.eyebrow {
  margin: 0 0 12px;
  color: #62864e;
  font-size: 13px;
  font-weight: 800;
  letter-spacing: 0.08em;
  text-transform: uppercase;
}

h1 {
  margin: 0;
  color: #25361f;
  font-size: 36px;
}

.price {
  margin: 20px 0 0;
  color: #2f6b1f;
  font-size: 30px;
  font-weight: 800;
}

.description {
  margin: 20px 0 0;
  color: #596757;
  line-height: 1.85;
}

.specs-panel {
  margin-top: 28px;
}

.specs-panel h2 {
  margin: 0 0 14px;
  color: #2b3c24;
  font-size: 18px;
}

.actions {
  display: flex;
  gap: 14px;
  margin-top: 36px;
}
</style>
