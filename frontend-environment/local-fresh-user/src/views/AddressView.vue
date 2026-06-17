<template>
  <section class="page-shell address-shell">
    <div class="address-page-header">
      <div>
        <p class="eyebrow">我的地址</p>
        <h1>管理常用配送地址</h1>
        <p class="address-page-intro">在這裡維護結算與揪團流程會用到的收件資訊。</p>
      </div>
      <el-button type="success" @click="openCreateDialog">新增地址</el-button>
    </div>

    <el-skeleton v-if="loading" :rows="6" animated />

    <el-empty v-else-if="!addressList.length" description="目前還沒有配送地址">
      <el-button type="success" @click="openCreateDialog">新增第一筆地址</el-button>
    </el-empty>

    <div v-else class="address-grid">
      <article v-for="address in addressList" :key="address.id" class="address-card">
        <div class="address-card-header">
          <div>
            <div class="address-topline">
              <h2>{{ address.consignee }}</h2>
              <span>{{ address.phone }}</span>
            </div>
            <div class="address-tags">
              <span v-if="address.isDefault === 1" class="default-tag">預設地址</span>
              <span v-if="address.label" class="label-tag">{{ address.label }}</span>
            </div>
          </div>

          <div class="address-actions">
            <el-button
              v-if="address.isDefault !== 1"
              text
              type="success"
              @click="handleSetDefault(address.id)"
            >
              設為預設
            </el-button>
            <el-button text @click="openEditDialog(address.id)">編輯</el-button>
            <el-button text type="danger" @click="handleDelete(address)">刪除</el-button>
          </div>
        </div>

        <p class="address-text">{{ formatAddress(address) }}</p>
      </article>
    </div>

    <el-dialog
      v-model="dialogVisible"
      :title="dialogMode === 'create' ? '新增配送地址' : '編輯配送地址'"
      width="520px"
      destroy-on-close
      @closed="resetForm"
    >
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
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="success" :loading="submitting" @click="submitAddress">
          {{ dialogMode === 'create' ? '儲存地址' : '更新地址' }}
        </el-button>
      </template>
    </el-dialog>
  </section>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  createAddress,
  deleteAddress,
  fetchAddressList,
  getAddressById,
  setDefaultAddress,
  updateAddress,
  type ShippingAddress
} from '@/services/address'

type DialogMode = 'create' | 'edit'

const loading = ref(false)
const submitting = ref(false)
const dialogVisible = ref(false)
const dialogMode = ref<DialogMode>('create')
const editingAddressId = ref<number | null>(null)
const addressList = ref<ShippingAddress[]>([])

const addressForm = reactive({
  consignee: '',
  phone: '',
  cityName: '',
  districtName: '',
  detail: '',
  label: '',
  isDefaultChecked: true
})

function formatAddress(address: ShippingAddress) {
  return [address.cityName, address.districtName, address.detail].filter(Boolean).join('')
}

function resetForm() {
  editingAddressId.value = null
  addressForm.consignee = ''
  addressForm.phone = ''
  addressForm.cityName = ''
  addressForm.districtName = ''
  addressForm.detail = ''
  addressForm.label = ''
  addressForm.isDefaultChecked = true
}

async function loadAddresses() {
  loading.value = true
  try {
    addressList.value = await fetchAddressList()
  } finally {
    loading.value = false
  }
}

function openCreateDialog() {
  dialogMode.value = 'create'
  resetForm()
  dialogVisible.value = true
}

async function openEditDialog(id: number) {
  try {
    dialogMode.value = 'edit'
    submitting.value = true
    const address = await getAddressById(id)
    editingAddressId.value = address.id
    addressForm.consignee = address.consignee
    addressForm.phone = address.phone
    addressForm.cityName = address.cityName || ''
    addressForm.districtName = address.districtName || ''
    addressForm.detail = address.detail
    addressForm.label = address.label || ''
    addressForm.isDefaultChecked = address.isDefault === 1
    dialogVisible.value = true
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '讀取地址失敗')
  } finally {
    submitting.value = false
  }
}

async function handleSetDefault(id: number) {
  try {
    await setDefaultAddress(id)
    await loadAddresses()
    ElMessage.success('已更新預設地址')
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '更新預設地址失敗')
  }
}

async function handleDelete(address: ShippingAddress) {
  try {
    await ElMessageBox.confirm(
      `確定刪除 ${address.consignee} 的地址嗎？`,
      '刪除地址',
      { type: 'warning' }
    )
    await deleteAddress(address.id)
    await loadAddresses()
    ElMessage.success('地址已刪除')
  } catch (error) {
    if (error === 'cancel' || error === 'close') {
      return
    }
    ElMessage.error(error instanceof Error ? error.message : '刪除地址失敗')
  }
}

async function submitAddress() {
  if (!addressForm.consignee || !addressForm.phone || !addressForm.cityName || !addressForm.districtName || !addressForm.detail) {
    ElMessage.warning('請先填完配送地址必要欄位')
    return
  }

  try {
    submitting.value = true
    const payload = {
      consignee: addressForm.consignee,
      phone: addressForm.phone,
      cityName: addressForm.cityName,
      districtName: addressForm.districtName,
      detail: addressForm.detail,
      label: addressForm.label || undefined,
      isDefault: addressForm.isDefaultChecked ? 1 : 0
    }

    if (dialogMode.value === 'create') {
      await createAddress(payload)
      ElMessage.success('地址已新增')
    } else if (editingAddressId.value != null) {
      await updateAddress({
        id: editingAddressId.value,
        ...payload
      })
      ElMessage.success('地址已更新')
    }

    dialogVisible.value = false
    resetForm()
    await loadAddresses()
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : dialogMode.value === 'create' ? '新增地址失敗' : '更新地址失敗')
  } finally {
    submitting.value = false
  }
}

onMounted(() => {
  void loadAddresses()
})
</script>

<style scoped>
.address-shell {
  padding: 24px 0 48px;
}

.address-page-header,
.address-card-header,
.address-topline,
.address-actions {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 16px;
}

.address-page-header {
  margin-bottom: 24px;
}

.address-page-intro {
  margin: 10px 0 0;
  color: var(--farm-muted);
}

.address-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 14px;
}

.address-card {
  padding: 18px;
  border-radius: 8px;
  border: 1px solid var(--farm-line);
  background: var(--farm-surface);
  box-shadow: var(--farm-shadow);
}

.address-card-header {
  flex-wrap: wrap;
}

.address-card-header > div:first-child {
  flex: 1 1 260px;
  min-width: 0;
}

.address-topline {
  align-items: baseline;
  flex-wrap: wrap;
}

.address-topline h2 {
  margin: 0;
  color: var(--farm-text);
  font-size: 20px;
}

.address-topline span {
  color: var(--farm-muted);
  font-weight: 600;
  white-space: nowrap;
}

.address-tags {
  display: flex;
  gap: 10px;
  margin-top: 10px;
}

.default-tag,
.label-tag {
  padding: 4px 10px;
  border-radius: 999px;
  font-size: 12px;
  font-weight: 700;
}

.default-tag {
  background: var(--farm-primary-soft);
  color: var(--farm-primary-deep);
}

.label-tag {
  background: var(--farm-accent-soft);
  color: #8b4b2f;
}

.address-actions {
  align-items: center;
  flex-wrap: wrap;
  justify-content: flex-end;
  flex: 0 0 auto;
}

.address-text {
  margin: 18px 0 0;
  color: #405047;
  line-height: 1.7;
  word-break: break-word;
}

@media (max-width: 768px) {
  .address-shell {
    padding: 16px 0 36px;
  }

  .address-page-header {
    flex-direction: column;
    align-items: stretch;
  }

  .address-grid {
    grid-template-columns: 1fr;
  }

  .address-actions {
    width: 100%;
    justify-content: flex-start;
  }
}
</style>
