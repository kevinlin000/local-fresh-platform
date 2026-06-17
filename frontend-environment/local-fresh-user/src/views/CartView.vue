<template>
  <section class="cart-shell">
    <div class="cart-layout">
      <div class="cart-main card">
        <div class="section-header">
          <div>
            <p class="eyebrow">購物車</p>
            <h1>整理這一餐想帶走的鮮選商品</h1>
          </div>
          <el-button text type="danger" :disabled="!cartItems.length" @click="handleCleanCart">
            清空購物車
          </el-button>
        </div>

        <el-skeleton v-if="loading" :rows="8" animated />

        <el-empty v-else-if="!cartItems.length" description="購物車目前是空的">
          <el-button type="success" @click="goHome">回首頁選商品</el-button>
        </el-empty>

        <div v-else class="cart-list">
          <article v-for="item in cartItems" :key="item.id" class="cart-item">
            <div class="cart-item-image">
              <img v-if="item.image" :src="item.image" :alt="item.name" />
              <div v-else class="image-placeholder">暫無圖片</div>
            </div>

            <div class="cart-item-body">
              <div class="cart-item-header">
                <div>
                  <h2>{{ item.name }}</h2>
                  <p v-if="item.productSpec" class="spec">{{ item.productSpec }}</p>
                </div>
                <strong>NT$ {{ formatPrice(item.amount) }}</strong>
              </div>

              <div class="cart-item-actions">
                <div class="quantity-box">
                  <el-button circle @click="subItem(item)">-</el-button>
                  <span>{{ item.number }}</span>
                  <el-button circle type="success" plain @click="addItem(item)">+</el-button>
                </div>

                <el-button text type="danger" @click="removeItem(item)">
                  刪除
                </el-button>
              </div>
            </div>
          </article>
        </div>
      </div>

      <aside class="cart-summary card">
        <p class="eyebrow">結算資訊</p>
        <h2>本次合計</h2>

        <div class="summary-row">
          <span>商品小計</span>
          <strong>NT$ {{ formatPrice(subtotal) }}</strong>
        </div>
        <div class="summary-row">
          <span>打包費</span>
          <strong>NT$ 0</strong>
        </div>
        <div class="summary-total">
          <span>應付總額</span>
          <strong>NT$ {{ formatPrice(subtotal) }}</strong>
        </div>

        <el-button
          type="success"
          size="large"
          :disabled="!cartItems.length"
          @click="openCheckout"
        >
          結算下單
        </el-button>
      </aside>
    </div>

    <el-drawer
      v-model="checkoutVisible"
      title="確認訂單"
      size="520px"
      destroy-on-close
    >
      <div class="checkout-panel">
        <div class="checkout-section">
          <div class="section-header">
            <h3>配送地址</h3>
            <div class="address-toolbar">
              <el-button text @click="goAddresses">管理地址</el-button>
              <el-button text type="success" @click="addressDialogVisible = true">
                新增地址
              </el-button>
            </div>
          </div>

          <el-alert
            v-if="checkoutError"
            :title="checkoutError"
            type="error"
            show-icon
            class="checkout-alert"
            @close="checkoutError = ''"
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
              <el-radio :value="address.id">
                <span />
              </el-radio>
              <div class="address-card-body">
                <div class="address-card-header">
                  <div class="address-topline">
                    <strong>{{ address.consignee }}</strong>
                    <span>{{ address.phone }}</span>
                  </div>
                  <div class="address-card-action">
                    <span v-if="address.isDefault === 1" class="default-tag">預設地址</span>
                    <el-button
                      v-else
                      text
                      type="success"
                      size="small"
                      @click.stop="setAsDefault(address.id)"
                    >
                      設為預設
                    </el-button>
                  </div>
                </div>
                <p class="address-detail">{{ formatAddress(address) }}</p>
              </div>
            </label>
          </el-radio-group>
        </div>

        <div class="checkout-section">
          <h3>備註</h3>
          <el-input
            v-model="remark"
            type="textarea"
            :rows="3"
            placeholder="例如：請避開中午時段送達"
          />
        </div>

        <div class="checkout-summary">
          <span>商品 {{ totalItems }} 件</span>
          <strong>NT$ {{ formatPrice(subtotal) }}</strong>
        </div>

        <el-button
          type="success"
          size="large"
          :loading="submittingOrder"
          :disabled="!selectedAddressId || !cartItems.length"
          @click="submitCurrentOrder"
        >
          送出訂單
        </el-button>
      </div>
    </el-drawer>

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
import { ElMessageBox } from 'element-plus/es/components/message-box/index'
import { useRouter } from 'vue-router'
import {
  createAddress,
  fetchAddressList,
  setDefaultAddress,
  type ShippingAddress
} from '@/services/address'
import {
  addToCart,
  cleanCart,
  fetchCartList,
  subFromCart,
  type CartItem
} from '@/services/cart'
import { submitOrder } from '@/services/order'

const router = useRouter()

const loading = ref(false)
const cartItems = ref<CartItem[]>([])

const checkoutVisible = ref(false)
const addressDialogVisible = ref(false)
const addressLoading = ref(false)
const savingAddress = ref(false)
const submittingOrder = ref(false)
const checkoutError = ref('')
const selectedAddressId = ref<number | null>(null)
const addressList = ref<ShippingAddress[]>([])
const remark = ref('')

const addressForm = reactive({
  consignee: '',
  phone: '',
  cityName: '',
  districtName: '',
  detail: '',
  label: '',
  isDefaultChecked: true
})

const subtotal = computed(() =>
  cartItems.value.reduce((sum, item) => sum + Number(item.amount) * item.number, 0)
)

const totalItems = computed(() =>
  cartItems.value.reduce((sum, item) => sum + item.number, 0)
)

function formatPrice(value: number) {
  return Number(value || 0).toLocaleString('zh-TW')
}

function buildCartPayload(item: CartItem) {
  if (item.productId) {
    return {
      productId: item.productId,
      productSpec: item.productSpec || undefined
    }
  }
  return {
    giftBoxId: item.giftBoxId || undefined
  }
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

async function loadCart() {
  loading.value = true
  try {
    cartItems.value = await fetchCartList()
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

async function addItem(item: CartItem) {
  try {
    await addToCart(buildCartPayload(item))
    await loadCart()
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '增加數量失敗')
  }
}

async function subItem(item: CartItem) {
  try {
    await subFromCart(buildCartPayload(item))
    await loadCart()
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '減少數量失敗')
  }
}

async function removeItem(item: CartItem) {
  try {
    for (let count = 0; count < item.number; count += 1) {
      await subFromCart(buildCartPayload(item))
    }
    await loadCart()
    ElMessage.success('已從購物車移除')
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '刪除商品失敗')
  }
}

async function handleCleanCart() {
  try {
    await ElMessageBox.confirm('確定要清空購物車嗎？', '提醒', {
      type: 'warning'
    })
    await cleanCart()
    await loadCart()
    ElMessage.success('購物車已清空')
  } catch (error) {
    if (error instanceof Error && error.message) {
      ElMessage.error(error.message)
    }
  }
}

async function openCheckout() {
  checkoutVisible.value = true
  checkoutError.value = ''
  remark.value = ''
  await loadAddresses()
}

async function setAsDefault(id: number) {
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

async function submitCurrentOrder() {
  if (!selectedAddressId.value) {
    checkoutError.value = '請先選擇配送地址'
    return
  }

  try {
    submittingOrder.value = true
    checkoutError.value = ''
    await submitOrder({
      addressBookId: selectedAddressId.value,
      payMethod: 1,
      remark: remark.value || undefined,
      estimatedDeliveryTime: null,
      deliveryStatus: 1,
      tablewareNumber: 0,
      tablewareStatus: 0,
      packAmount: 0,
      amount: subtotal.value
    })
    checkoutVisible.value = false
    await loadCart()
    ElMessage.success('訂單已送出')
    await router.push('/orders')
  } catch (error) {
    checkoutError.value = error instanceof Error ? error.message : '送出訂單失敗'
  } finally {
    submittingOrder.value = false
  }
}

function goHome() {
  void router.push('/')
}

function goAddresses() {
  void router.push('/addresses')
}

onMounted(() => {
  void loadCart()
})
</script>

<style scoped>
.cart-shell {
  max-width: 1180px;
  margin: 0 auto;
  padding: 24px 0 48px;
}

.cart-layout {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 320px;
  gap: 18px;
}

.card {
  border: 1px solid var(--farm-line);
  border-radius: 8px;
  background: var(--farm-surface);
  box-shadow: var(--farm-shadow);
}

.cart-main {
  padding: 20px;
}

.section-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 16px;
  margin-bottom: 20px;
}

.eyebrow {
  margin: 0 0 8px;
  color: var(--farm-accent);
  font-size: 12px;
  font-weight: 800;
  letter-spacing: 0;
  text-transform: uppercase;
}

h1,
h2,
h3 {
  margin: 0;
  color: var(--farm-text);
}

.cart-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.cart-item {
  display: grid;
  grid-template-columns: 128px 1fr;
  gap: 14px;
  padding: 14px;
  border-radius: 8px;
  border: 1px solid var(--farm-line);
  background: var(--farm-surface-strong);
}

.cart-item-image {
  overflow: hidden;
  height: 112px;
  border-radius: 8px;
  background: #edf1e9;
}

.cart-item-image img {
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
  font-size: 13px;
  font-weight: 800;
}

.cart-item-header,
.cart-item-actions,
.summary-row,
.summary-total,
.checkout-summary,
.address-topline,
.address-toolbar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 12px;
}

.cart-item-header h2 {
  font-size: 18px;
  line-height: 1.4;
}

.spec {
  margin: 8px 0 0;
  color: var(--farm-muted);
}

.cart-item-actions {
  margin-top: 18px;
}

.quantity-box {
  display: flex;
  align-items: center;
  gap: 12px;
}

.quantity-box span {
  min-width: 24px;
  text-align: center;
  font-weight: 700;
}

.cart-summary {
  padding: 20px;
  height: fit-content;
  position: sticky;
  top: 88px;
}

.summary-row {
  margin-top: 16px;
  color: var(--farm-muted);
}

.summary-total {
  margin: 24px 0 28px;
  padding-top: 20px;
  border-top: 1px solid var(--farm-line);
  color: var(--farm-text);
  font-size: 18px;
}

.checkout-panel {
  display: flex;
  flex-direction: column;
  gap: 28px;
}

.checkout-alert {
  margin-top: 12px;
}

.checkout-section {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.address-group {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.address-card {
  display: grid;
  grid-template-columns: 28px 1fr;
  gap: 12px;
  padding: 16px 18px;
  border: 1px solid var(--farm-line);
  border-radius: 8px;
  background: var(--farm-surface-strong);
  cursor: pointer;
  transition: border-color 0.2s ease, background-color 0.2s ease, box-shadow 0.2s ease;
}

.address-card.active {
  border-color: rgba(47, 111, 78, 0.38);
  background: var(--farm-primary-soft);
  box-shadow: 0 0 0 1px rgba(47, 111, 78, 0.08);
}

.address-card :deep(.el-radio__label) {
  display: none;
}

.address-card-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 12px;
}

.address-card-action {
  flex-shrink: 0;
}

.address-topline {
  justify-content: flex-start;
  align-items: baseline;
  flex-wrap: wrap;
}

.address-detail {
  margin: 10px 0 0;
  color: var(--farm-muted);
  line-height: 1.6;
  word-break: break-word;
}

.default-tag {
  padding: 4px 10px;
  border-radius: 999px;
  background: var(--farm-primary-soft);
  color: var(--farm-primary-deep);
  font-size: 12px;
  font-weight: 700;
}

.checkout-summary {
  padding: 18px 20px;
  border-radius: 8px;
  background: #fbfaf6;
  color: var(--farm-primary-deep);
}

@media (max-width: 900px) {
  .cart-shell {
    padding: 16px 0 36px;
  }

  .cart-layout {
    grid-template-columns: 1fr;
  }

  .cart-summary {
    position: static;
    order: -1;
  }
}

@media (max-width: 640px) {
  .cart-main,
  .cart-summary {
    padding: 14px;
  }

  .section-header,
  .cart-item-header,
  .cart-item-actions {
    align-items: flex-start;
    flex-direction: column;
  }

  .cart-item {
    grid-template-columns: 92px 1fr;
  }

  .cart-item-image {
    height: 92px;
  }

  .cart-item-header h2 {
    font-size: 16px;
  }

  .address-card,
  .address-card-header {
    grid-template-columns: 1fr;
  }
}
</style>
