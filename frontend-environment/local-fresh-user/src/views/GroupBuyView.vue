<template>
  <section class="page-shell">
    <el-skeleton v-if="loading" :rows="10" animated />

    <div v-else-if="groupBuy" class="card">
      <div class="groupbuy-layout">
        <div class="media-panel">
          <img v-if="groupBuy.productImage" :src="groupBuy.productImage" :alt="groupBuy.productName || '揪團商品'" />
          <div v-else class="image-placeholder">暫無圖片</div>
        </div>

        <div class="content-panel">
          <p class="eyebrow">揪團詳情</p>
          <h1>{{ groupBuy.productName || '揪團商品' }}</h1>
          <div class="status-row">
            <el-tag :type="statusType(groupBuy.status)" size="large" effect="plain">
              {{ statusText(groupBuy.status) }}
            </el-tag>
            <span class="countdown">倒數 {{ countdownText }}</span>
          </div>

          <div v-if="groupBuy.status === 3" class="status-alert danger">
            揪團未成立，訂單已取消。
          </div>
          <div v-else-if="groupBuy.status === 2" class="status-alert success">
            揪團已成團，三位成員的訂單已轉為待確認。
          </div>
          <div v-else-if="groupBuy.status === 4" class="status-alert info">
            揪團已取消，預訂單已同步取消。
          </div>

          <div class="summary-grid">
            <div class="summary-item">
              <span>揪團編號</span>
              <strong>{{ groupBuy.groupNo }}</strong>
            </div>
            <div class="summary-item">
              <span>商品數量</span>
              <strong>{{ groupBuy.quantity || 1 }} 件</strong>
            </div>
            <div class="summary-item">
              <span>目前進度</span>
              <strong>{{ groupBuy.currentCount }}/{{ groupBuy.requiredCount }} 人</strong>
            </div>
            <div class="summary-item">
              <span>還差人數</span>
              <strong>{{ remainingCount }} 人</strong>
            </div>
          </div>

          <div class="actions">
            <template v-if="groupBuy.status === 1 && !joinedByCurrentMember">
              <el-button type="success" size="large" :loading="joining" @click="openJoinDialog">
                加入揪團
              </el-button>
            </template>
            <template v-else-if="groupBuy.status === 1">
              <el-tag type="success" size="large">已加入，等待其他成員</el-tag>
            </template>
            <template v-else>
              <el-button plain @click="goOrders">回我的揪團</el-button>
            </template>

            <el-button @click="copyShareUrl">複製分享連結</el-button>
            <el-button
              v-if="canCancelGroupBuy"
              type="danger"
              plain
              :loading="cancelling"
              @click="handleCancelGroupBuy"
            >
              取消揪團
            </el-button>
          </div>
        </div>
      </div>

      <div class="participants-card">
        <div class="participants-header">
          <div>
            <p class="eyebrow">已加入成員</p>
            <h2>目前有 {{ groupBuy.currentCount }} 位成員加入</h2>
          </div>
          <span class="refresh-note">狀態為揪團中時，每 5 秒自動更新</span>
        </div>

        <div class="participant-list">
          <article
            v-for="participant in groupBuy.participants"
            :key="`${participant.memberId}-${participant.joinedAt}`"
            class="participant-card"
          >
            <strong>{{ participant.memberName }}</strong>
            <span>ID {{ participant.memberId }}</span>
            <time>{{ formatDate(participant.joinedAt) }}</time>
          </article>
        </div>
      </div>
    </div>

    <el-empty v-else description="找不到這個揪團" />

    <el-dialog v-model="joinDialogVisible" title="加入揪團" width="560px">
      <div class="dialog-body">
        <div class="dialog-summary">
          <div class="summary-image">
            <img v-if="groupBuy?.productImage" :src="groupBuy.productImage" :alt="groupBuy.productName || '揪團商品'" />
            <div v-else class="image-placeholder small">暫無圖片</div>
          </div>
          <div>
            <strong>{{ groupBuy?.productName || '揪團商品' }}</strong>
            <p>{{ groupBuy?.currentCount }}/{{ groupBuy?.requiredCount }} 人，還差 {{ remainingCount }} 人成團。</p>
            <span>數量 {{ groupBuy?.quantity || 1 }} 件</span>
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
            v-if="joinError"
            :title="joinError"
            type="error"
            show-icon
            class="dialog-alert"
            @close="joinError = ''"
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
        <el-button @click="joinDialogVisible = false">取消</el-button>
        <el-button
          type="success"
          :loading="joining"
          :disabled="!selectedAddressId"
          @click="handleJoinGroupBuy"
        >
          確認加入
        </el-button>
      </template>
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
import { computed, onBeforeUnmount, onMounted, reactive, ref, watch } from 'vue'
import { ElMessage } from 'element-plus/es/components/message/index'
import { ElMessageBox } from 'element-plus/es/components/message-box/index'
import { useRoute, useRouter } from 'vue-router'
import {
  createAddress,
  fetchAddressList,
  setDefaultAddress,
  type ShippingAddress
} from '@/services/address'
import {
  cancelGroupBuy,
  fetchGroupBuy,
  joinGroupBuy,
  type GroupBuyRecord
} from '@/services/groupBuy'
import { useMemberStore } from '@/stores/member'

const route = useRoute()
const router = useRouter()
const memberStore = useMemberStore()

const loading = ref(false)
const joining = ref(false)
const cancelling = ref(false)
const groupBuy = ref<GroupBuyRecord | null>(null)
const joinDialogVisible = ref(false)
const joinError = ref('')

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

const countdownText = ref('00:00')
let countdownTimer: number | null = null
let pollTimer: number | null = null
let previousStatus: number | null = null

const groupNo = computed(() => String(route.params.groupNo || ''))
const currentMemberId = computed(() => memberStore.profile.id)
const shareUrl = computed(() => {
  if (typeof window === 'undefined' || !groupNo.value) {
    return ''
  }
  return `${window.location.origin}/groupBuy/${groupNo.value}`
})
const joinedByCurrentMember = computed(() =>
  groupBuy.value?.participants.some((participant) => participant.memberId === currentMemberId.value) ?? false
)
const isInitiator = computed(() => groupBuy.value?.initiatorId === currentMemberId.value)
const canCancelGroupBuy = computed(() =>
  groupBuy.value?.status === 1
  && groupBuy.value.participants.length === 1
  && isInitiator.value
)
const remainingCount = computed(() =>
  Math.max((groupBuy.value?.requiredCount || 0) - (groupBuy.value?.currentCount || 0), 0)
)

function formatDate(value: string) {
  return new Date(value).toLocaleString('zh-TW', {
    hour12: false
  })
}

function formatAddress(address: ShippingAddress) {
  return [address.cityName, address.districtName, address.detail].filter(Boolean).join('')
}

function statusText(status: number) {
  switch (status) {
    case 1:
      return '揪團中'
    case 2:
      return '已成團'
    case 3:
      return '已失敗'
    case 4:
      return '已取消'
    default:
      return `狀態 ${status}`
  }
}

function statusType(status: number) {
  switch (status) {
    case 2:
      return 'success'
    case 3:
      return 'danger'
    case 4:
      return 'info'
    default:
      return 'warning'
  }
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

function updateCountdown() {
  if (!groupBuy.value?.expireAt) {
    countdownText.value = '00:00'
    return
  }
  const diff = new Date(groupBuy.value.expireAt).getTime() - Date.now()
  if (diff <= 0) {
    countdownText.value = '00:00'
    return
  }
  const totalSeconds = Math.floor(diff / 1000)
  const hours = Math.floor(totalSeconds / 3600)
  const minutes = Math.floor((totalSeconds % 3600) / 60)
  const seconds = totalSeconds % 60
  if (hours >= 24) {
    const days = Math.floor(hours / 24)
    const remainingHours = hours % 24
    countdownText.value = `${days} 天 ${remainingHours} 小時`
    return
  }
  if (hours >= 1) {
    countdownText.value = `${hours} 小時 ${minutes} 分`
    return
  }
  if (minutes >= 1) {
    countdownText.value = `${minutes} 分 ${String(seconds).padStart(2, '0')} 秒`
    return
  }
  countdownText.value = `${seconds} 秒`
}

function clearTimers() {
  if (countdownTimer) {
    window.clearInterval(countdownTimer)
    countdownTimer = null
  }
  if (pollTimer) {
    window.clearInterval(pollTimer)
    pollTimer = null
  }
}

function startCountdown() {
  if (countdownTimer) {
    window.clearInterval(countdownTimer)
  }
  updateCountdown()
  countdownTimer = window.setInterval(updateCountdown, 1000)
}

function stopPollingIfInactive() {
  if (groupBuy.value?.status !== 1 && pollTimer) {
    window.clearInterval(pollTimer)
    pollTimer = null
  }
}

function handleStatusTransition(newStatus: number) {
  if (previousStatus === null || previousStatus === newStatus) {
    previousStatus = newStatus
    return
  }

  if (previousStatus === 1 && newStatus === 2) {
    ElMessage.success('揪團已成團，訂單已更新為待確認')
    window.setTimeout(() => {
      void router.push('/orders?tab=group-buy')
    }, 1200)
  }
  if (previousStatus === 1 && newStatus === 3) {
    ElMessage.warning('揪團未成立，訂單已取消')
  }
  if (previousStatus === 1 && newStatus === 4) {
    ElMessage.info('揪團已取消')
  }
  previousStatus = newStatus
}

async function loadGroupBuy(showError = true) {
  if (!groupNo.value) {
    return
  }
  if (!groupBuy.value) {
    loading.value = true
  }
  try {
    const result = await fetchGroupBuy(groupNo.value)
    groupBuy.value = result
    handleStatusTransition(result.status)
    startCountdown()
    stopPollingIfInactive()
  } catch (error) {
    if (showError) {
      ElMessage.error(error instanceof Error ? error.message : '載入揪團失敗')
    }
    groupBuy.value = null
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

async function openJoinDialog() {
  joinError.value = ''
  joinDialogVisible.value = true
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

async function handleJoinGroupBuy() {
  if (!groupBuy.value || !selectedAddressId.value || !groupBuy.value.productId) {
    joinError.value = '揪團資料不完整，無法加入'
    return
  }

  try {
    joining.value = true
    joinError.value = ''
    groupBuy.value = await joinGroupBuy({
      groupNo: groupBuy.value.groupNo,
      productId: groupBuy.value.productId,
      quantity: groupBuy.value.quantity || 1,
      addressId: selectedAddressId.value
    })
    previousStatus = groupBuy.value.status
    joinDialogVisible.value = false
    startCountdown()
    stopPollingIfInactive()
    ElMessage.success(groupBuy.value.status === 2 ? '加入成功，揪團已成團' : '已加入揪團')
  } catch (error) {
    joinError.value = error instanceof Error ? error.message : '加入揪團失敗'
  } finally {
    joining.value = false
  }
}

async function copyShareUrl() {
  if (!shareUrl.value) {
    return
  }
  try {
    await navigator.clipboard.writeText(shareUrl.value)
    ElMessage.success('分享連結已複製')
  } catch {
    ElMessage.error('複製失敗，請手動複製連結')
  }
}

async function handleCancelGroupBuy() {
  if (!groupBuy.value?.groupNo || !canCancelGroupBuy.value) {
    return
  }

  try {
    await ElMessageBox.confirm(
      '取消後將保留揪團資料，並同步取消目前的預訂單。確定要取消嗎？',
      '取消揪團',
      {
        confirmButtonText: '確認取消',
        cancelButtonText: '回傳',
        type: 'warning'
      }
    )

    cancelling.value = true
    await cancelGroupBuy(groupBuy.value.groupNo)
    ElMessage.success('揪團已取消')
    await router.push('/')
  } catch (error) {
    if (error === 'cancel') {
      return
    }
    ElMessage.error(error instanceof Error ? error.message : '取消揪團失敗')
  } finally {
    cancelling.value = false
  }
}

function goOrders() {
  void router.push('/orders?tab=group-buy')
}

watch(() => groupBuy.value?.status, (status) => {
  if (status === 1) {
    if (!pollTimer) {
      pollTimer = window.setInterval(() => {
        void loadGroupBuy(false)
      }, 5000)
    }
    return
  }
  stopPollingIfInactive()
})

onMounted(async () => {
  await loadGroupBuy()
})

onBeforeUnmount(() => {
  clearTimers()
})
</script>

<style scoped>
.page-shell {
  max-width: 1180px;
  margin: 0 auto;
  padding: 24px 0 48px;
}

.card,
.participants-card {
  border: 1px solid var(--farm-line);
  border-radius: 8px;
  background: var(--farm-surface);
  box-shadow: var(--farm-shadow);
}

.card {
  padding: 20px;
}

.groupbuy-layout {
  display: grid;
  grid-template-columns: 320px minmax(0, 1fr);
  gap: 22px;
}

.media-panel,
.summary-image {
  overflow: hidden;
  border-radius: 8px;
  background: #edf1e9;
}

.media-panel {
  height: 320px;
}

.media-panel img,
.summary-image img {
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
}

.image-placeholder.small {
  font-size: 13px;
}

.content-panel {
  padding-top: 8px;
}

.eyebrow {
  margin: 0 0 10px;
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

.status-row {
  display: flex;
  align-items: center;
  gap: 16px;
  margin-top: 18px;
  flex-wrap: wrap;
}

.countdown {
  color: var(--farm-muted);
  font-weight: 700;
}

.status-alert {
  margin-top: 16px;
  padding: 14px 16px;
  border-radius: 8px;
  font-weight: 700;
}

.status-alert.success {
  background: var(--farm-primary-soft);
  color: var(--farm-primary-deep);
}

.status-alert.danger {
  background: #fff0ee;
  color: #b4412f;
}

.summary-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 12px;
  margin-top: 24px;
}

.summary-item {
  padding: 14px 16px;
  border: 1px solid var(--farm-line);
  border-radius: 8px;
  background: #fbfaf6;
}

.summary-item span {
  display: block;
  color: var(--farm-muted);
  font-size: 13px;
}

.summary-item strong {
  display: block;
  margin-top: 8px;
  color: var(--farm-text);
  font-size: 18px;
}

.actions {
  display: flex;
  gap: 14px;
  align-items: center;
  margin-top: 28px;
  flex-wrap: wrap;
}

.participants-card {
  margin-top: 18px;
  padding: 20px;
}

.participants-header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
}

.refresh-note {
  color: var(--farm-muted);
  font-size: 13px;
}

.participant-list {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 12px;
  margin-top: 18px;
}

.participant-card {
  padding: 14px;
  border-radius: 8px;
  border: 1px solid var(--farm-line);
  background: var(--farm-surface-strong);
}

.participant-card span,
.participant-card time {
  display: block;
  margin-top: 8px;
  color: var(--farm-muted);
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
  width: 88px;
  height: 88px;
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
.address-footer {
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

@media (max-width: 900px) {
  .page-shell {
    padding: 16px 0 36px;
  }

  .groupbuy-layout {
    grid-template-columns: 1fr;
  }

  .media-panel {
    height: auto;
    aspect-ratio: 4 / 3;
  }

  .participant-list {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 560px) {
  .card,
  .participants-card {
    padding: 14px;
  }

  .summary-grid,
  .dialog-summary,
  .address-card {
    grid-template-columns: 1fr;
  }

  .actions .el-button {
    width: 100%;
    margin-left: 0;
  }
}
</style>
