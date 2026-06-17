<template>
  <section class="detail-shell">
    <el-skeleton v-if="loading" animated :rows="8" />

    <template v-else-if="product">
      <div class="detail-card">
        <div class="media-panel">
          <img v-if="product.image" :src="product.image" :alt="product.productName" />
          <div v-else class="media-placeholder">暫無圖片</div>
        </div>

        <div class="content-panel">
          <p class="eyebrow">商品詳情</p>
          <h1>{{ product.productName }}</h1>
          <p class="price">NT$ {{ formatPrice(product.price) }}</p>
          <div class="detail-meta">
            <span>{{ product.categoryName || '當季鮮選' }}</span>
            <span>可排單配送</span>
            <span>可發起 3 人揪團</span>
          </div>
          <p class="description">{{ product.description || '來自在地產區的當季鮮選，適合日常料理與家庭備菜。' }}</p>

          <div v-if="specOptions.length" class="specs-panel">
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

          <div class="quantity-panel">
            <h2>購買數量</h2>
            <el-input-number v-model="quantity" :min="1" :max="99" />
          </div>

          <div class="actions">
            <el-button type="success" size="large" :loading="submittingCart" @click="handleAddToCart">
              加入購物車
            </el-button>
            <el-button size="large" :loading="groupBuySubmitting" @click="openGroupBuyDialog">
              立即揪團
            </el-button>
          </div>
        </div>
      </div>
    </template>

    <el-empty v-else description="找不到這項商品" />

    <el-dialog v-model="groupBuyDialogVisible" title="發起揪團" width="560px">
      <div class="dialog-body">
        <div class="dialog-summary">
          <div class="summary-image">
            <img v-if="product?.image" :src="product.image" :alt="product.productName" />
            <div v-else class="media-placeholder small">暫無圖片</div>
          </div>
          <div>
            <strong>{{ product?.productName }}</strong>
            <p>3 人成團免運，先建立團再分享給朋友。</p>
            <span>數量 {{ quantity }} 件</span>
          </div>
        </div>

        <div class="dialog-section">
          <div class="section-line">
            <h3>配送地址</h3>
            <el-button text type="success" @click="addressDialogVisible = true">
              新增地址
            </el-button>
          </div>

          <el-alert
            v-if="groupBuyError"
            :title="groupBuyError"
            type="error"
            show-icon
            class="dialog-alert"
            @close="groupBuyError = ''"
          />

          <el-skeleton v-if="addressLoading" :rows="4" animated />

          <el-empty v-else-if="!addressList.length" description="請先新增至少一筆配送地址" />

          <el-radio-group v-else v-model="selectedAddressId" class="address-group">
            <label
              v-for="address in addressList"
              :key="address.id"
              class="address-card"
              :class="{ active: selectedAddressId === address.id }"
            >
              <el-radio :label="address.id">
                <span />
              </el-radio>
              <div class="address-card-body">
                <div class="address-topline">
                  <strong>{{ address.consignee }}</strong>
                  <span>{{ address.phone }}</span>
                </div>
                <p>{{ formatAddress(address) }}</p>
                <div class="address-footer">
                  <span v-if="address.isDefault === 1" class="default-tag">預設地址</span>
                  <el-button
                    v-else
                    text
                    type="success"
                    @click.stop="setAsDefaultAddress(address.id)"
                  >
                    設為預設
                  </el-button>
                </div>
              </div>
            </label>
          </el-radio-group>
        </div>
      </div>

      <template #footer>
        <el-button @click="groupBuyDialogVisible = false">取消</el-button>
        <el-button
          type="success"
          :loading="groupBuySubmitting"
          :disabled="!selectedAddressId"
          @click="handleInitiateGroupBuy"
        >
          建立揪團
        </el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="groupBuyCreatedVisible" title="揪團已建立" width="520px">
      <div class="success-card">
        <el-result
          icon="success"
          title="揪團已建立"
          sub-title="把連結分享給朋友，湊滿 3 人就能成團免運。"
        >
          <template #extra>
            <div class="share-box">
              <p class="share-label">分享連結</p>
              <el-input :model-value="sharedGroupBuyLink" readonly />
            </div>
            <div class="success-actions">
              <el-button @click="copyShareUrl">複製連結</el-button>
              <el-button type="success" @click="goToCreatedGroupBuy">進入揪團頁</el-button>
            </div>
          </template>
        </el-result>
      </div>
    </el-dialog>

    <el-dialog v-model="addressDialogVisible" title="新增配送地址" width="520px">
      <el-form label-position="top" :model="addressForm">
        <el-form-item label="收件人">
          <el-input v-model="addressForm.consignee" placeholder="例如：Kevin Lin" />
        </el-form-item>
        <el-form-item label="手機號碼">
          <el-input v-model="addressForm.phone" placeholder="例如：0912345678" />
        </el-form-item>
        <el-form-item label="城市">
          <el-input v-model="addressForm.cityName" placeholder="例如：台北市" />
        </el-form-item>
        <el-form-item label="行政區">
          <el-input v-model="addressForm.districtName" placeholder="例如：信義區" />
        </el-form-item>
        <el-form-item label="詳細地址">
          <el-input v-model="addressForm.detail" placeholder="例如：市府路 1 號" />
        </el-form-item>
        <el-form-item label="地址標籤">
          <el-input v-model="addressForm.label" placeholder="例如：住家 / 公司" />
        </el-form-item>
        <el-form-item>
          <el-checkbox v-model="addressForm.isDefaultChecked">設成預設地址</el-checkbox>
        </el-form-item>
      </el-form>

      <template #footer>
        <el-button @click="addressDialogVisible = false">取消</el-button>
        <el-button type="success" :loading="savingAddress" @click="createNewAddress">
          儲存地址
        </el-button>
      </template>
    </el-dialog>
  </section>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus/es/components/message/index'
import { useRoute, useRouter } from 'vue-router'
import {
  createAddress,
  fetchAddressList,
  setDefaultAddress,
  type ShippingAddress
} from '@/services/address'
import { addToCart } from '@/services/cart'
import { fetchProductDetail, type Product } from '@/services/catalog'
import {
  initiateGroupBuy,
  type GroupBuyRecord
} from '@/services/groupBuy'

const route = useRoute()
const router = useRouter()

const loading = ref(false)
const submittingCart = ref(false)
const groupBuySubmitting = ref(false)
const product = ref<Product | null>(null)
const selectedSpec = ref('')
const quantity = ref(1)

const groupBuyDialogVisible = ref(false)
const groupBuyCreatedVisible = ref(false)
const groupBuyError = ref('')
const createdGroupBuy = ref<GroupBuyRecord | null>(null)

const addressLoading = ref(false)
const savingAddress = ref(false)
const addressDialogVisible = ref(false)
const addressList = ref<ShippingAddress[]>([])
const selectedAddressId = ref<number | null>(null)

const addressForm = reactive({
  consignee: '',
  phone: '',
  cityName: '',
  districtName: '',
  detail: '',
  label: '',
  isDefaultChecked: true
})

const productId = computed(() => Number(route.params.id))
const sharedGroupBuyLink = computed(() => {
  if (!createdGroupBuy.value?.groupNo) {
    return ''
  }
  if (typeof window === 'undefined') {
    return ''
  }
  return `${window.location.origin}/groupBuy/${createdGroupBuy.value.groupNo}`
})

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

function formatAddress(address: ShippingAddress) {
  return [address.cityName, address.districtName, address.detail].filter(Boolean).join('')
}

function resetAddressForm() {
  addressForm.consignee = ''
  addressForm.phone = ''
  addressForm.cityName = ''
  addressForm.districtName = ''
  addressForm.detail = ''
  addressForm.label = ''
  addressForm.isDefaultChecked = true
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

async function loadAddresses() {
  addressLoading.value = true
  try {
    addressList.value = await fetchAddressList()
    const defaultAddress = addressList.value.find((item) => item.isDefault === 1)
    selectedAddressId.value = defaultAddress?.id ?? addressList.value[0]?.id ?? null
  } finally {
    addressLoading.value = false
  }
}

async function handleAddToCart() {
  if (!product.value) {
    return
  }

  try {
    submittingCart.value = true
    for (let count = 0; count < quantity.value; count += 1) {
      await addToCart({
        productId: product.value.id,
        productSpec: selectedSpec.value || undefined
      })
    }
    ElMessage.success('已加入購物車')
    await router.push('/cart')
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '加入購物車失敗')
  } finally {
    submittingCart.value = false
  }
}

async function openGroupBuyDialog() {
  groupBuyError.value = ''
  groupBuyDialogVisible.value = true
  await loadAddresses()
}

async function setAsDefaultAddress(id: number) {
  try {
    await setDefaultAddress(id)
    await loadAddresses()
    ElMessage.success('已更新預設地址')
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '更新預設地址失敗')
  }
}

async function createNewAddress() {
  if (!addressForm.consignee || !addressForm.phone || !addressForm.cityName || !addressForm.districtName || !addressForm.detail) {
    ElMessage.warning('請先填完配送地址必要欄位')
    return
  }

  try {
    savingAddress.value = true
    const shouldSetDefault = addressForm.isDefaultChecked
    const addressSnapshot = {
      consignee: addressForm.consignee,
      phone: addressForm.phone,
      cityName: addressForm.cityName,
      districtName: addressForm.districtName,
      detail: addressForm.detail
    }
    await createAddress({
      consignee: addressSnapshot.consignee,
      phone: addressSnapshot.phone,
      cityName: addressSnapshot.cityName,
      districtName: addressSnapshot.districtName,
      detail: addressSnapshot.detail,
      label: addressForm.label || undefined,
      isDefault: shouldSetDefault ? 1 : 0
    })
    await loadAddresses()
    if (shouldSetDefault) {
      const createdAddress = [...addressList.value]
        .filter((item) =>
          item.consignee === addressSnapshot.consignee
          && item.phone === addressSnapshot.phone
          && item.cityName === addressSnapshot.cityName
          && item.districtName === addressSnapshot.districtName
          && item.detail === addressSnapshot.detail
        )
        .sort((a, b) => b.id - a.id)[0]

      if (createdAddress) {
        await setDefaultAddress(createdAddress.id)
        await loadAddresses()
      }
    }
    addressDialogVisible.value = false
    resetAddressForm()
    ElMessage.success('地址已新增')
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '新增地址失敗')
  } finally {
    savingAddress.value = false
  }
}

async function handleInitiateGroupBuy() {
  if (!product.value || !selectedAddressId.value) {
    groupBuyError.value = '請先選擇配送地址'
    return
  }

  try {
    groupBuySubmitting.value = true
    groupBuyError.value = ''
    createdGroupBuy.value = await initiateGroupBuy({
      productId: product.value.id,
      quantity: quantity.value,
      addressId: selectedAddressId.value,
      requiredCount: 3
    })
    groupBuyDialogVisible.value = false
    groupBuyCreatedVisible.value = true
  } catch (error) {
    groupBuyError.value = error instanceof Error ? error.message : '建立揪團失敗'
  } finally {
    groupBuySubmitting.value = false
  }
}

async function copyShareUrl() {
  if (!sharedGroupBuyLink.value) {
    return
  }
  try {
    await navigator.clipboard.writeText(sharedGroupBuyLink.value)
    ElMessage.success('分享連結已複製')
  } catch {
    ElMessage.error('複製失敗，請手動複製連結')
  }
}

async function goToCreatedGroupBuy() {
  if (!createdGroupBuy.value?.groupNo) {
    return
  }
  groupBuyCreatedVisible.value = false
  await router.push(`/groupBuy/${createdGroupBuy.value.groupNo}`)
}

onMounted(() => {
  void loadProduct()
})
</script>

<style scoped>
.detail-shell {
  max-width: 1180px;
  margin: 0 auto;
  padding: 24px 0 48px;
}

.detail-card {
  display: grid;
  grid-template-columns: minmax(320px, 0.9fr) minmax(0, 1fr);
  gap: 24px;
  padding: 18px;
  border: 1px solid var(--farm-line);
  border-radius: 8px;
  background: var(--farm-surface);
  box-shadow: 0 10px 28px rgba(28, 39, 32, 0.06);
}

.media-panel {
  overflow: hidden;
  min-height: 396px;
  border-radius: 8px;
  background: #eef1eb;
}

.media-panel img,
.summary-image img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.media-placeholder {
  display: grid;
  place-items: center;
  width: 100%;
  height: 100%;
  color: var(--farm-muted);
  font-size: 15px;
  font-weight: 800;
  letter-spacing: 0;
}

.media-placeholder.small {
  font-size: 13px;
}

.content-panel {
  padding: 10px 6px;
}

.eyebrow {
  margin: 0 0 12px;
  color: var(--farm-accent);
  font-size: 12px;
  font-weight: 800;
}

h1 {
  margin: 0;
  color: var(--farm-text);
  font-size: 32px;
  line-height: 1.25;
}

.price {
  margin: 18px 0 0;
  color: var(--farm-primary-deep);
  font-size: 28px;
  font-weight: 800;
}

.detail-meta {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-top: 14px;
}

.detail-meta span {
  padding: 6px 9px;
  border: 1px solid var(--farm-line);
  border-radius: 7px;
  background: #f8faf7;
  color: #53625a;
  font-size: 13px;
  font-weight: 700;
}

.description {
  margin: 18px 0 0;
  color: var(--farm-muted);
  line-height: 1.75;
}

.specs-panel,
.quantity-panel {
  margin-top: 24px;
}

.specs-panel h2,
.quantity-panel h2,
.dialog-section h3 {
  margin: 0 0 14px;
  color: var(--farm-text);
  font-size: 16px;
}

.actions {
  display: flex;
  gap: 14px;
  margin-top: 32px;
  flex-wrap: wrap;
}

.dialog-body {
  display: flex;
  flex-direction: column;
  gap: 22px;
}

.dialog-summary {
  display: grid;
  grid-template-columns: 88px 1fr;
  gap: 14px;
  padding: 14px;
  border: 1px solid var(--farm-line);
  border-radius: 8px;
  background: #fbfaf6;
}

.dialog-summary p,
.dialog-summary span {
  margin: 8px 0 0;
  color: var(--farm-muted);
}

.summary-image {
  overflow: hidden;
  width: 88px;
  height: 88px;
  border-radius: 8px;
  background: #edf1e9;
}

.section-line {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 14px;
}

.dialog-alert {
  margin-bottom: 14px;
}

.address-group {
  display: flex;
  flex-direction: column;
  gap: 12px;
  width: 100%;
}

.address-card {
  display: grid;
  grid-template-columns: auto 1fr;
  gap: 12px;
  padding: 14px;
  border: 1px solid var(--farm-line);
  border-radius: 8px;
  background: var(--farm-surface-strong);
  cursor: pointer;
}

.address-card.active {
  border-color: rgba(47, 111, 78, 0.38);
  box-shadow: 0 8px 20px rgba(28, 39, 32, 0.08);
}

.address-topline,
.address-footer,
.success-actions {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.address-card-body p {
  margin: 8px 0 0;
  color: var(--farm-muted);
}

.default-tag {
  color: var(--farm-primary-deep);
  font-size: 13px;
  font-weight: 700;
}

.success-card {
  padding: 8px 4px;
}

.share-box {
  width: 100%;
  margin-bottom: 18px;
}

.share-label {
  margin: 0 0 10px;
  color: var(--farm-muted);
  font-weight: 700;
}

@media (max-width: 900px) {
  .detail-shell {
    padding: 16px 0 36px;
  }

  .detail-card {
    grid-template-columns: 1fr;
    padding: 14px;
  }

  .media-panel {
    min-height: 320px;
    aspect-ratio: 4 / 3;
  }

  h1 {
    font-size: 28px;
  }
}

@media (max-width: 560px) {
  .media-panel {
    min-height: 240px;
  }

  .actions .el-button {
    width: 100%;
    margin-left: 0;
  }

  .dialog-summary,
  .address-card {
    grid-template-columns: 1fr;
  }
}
</style>
