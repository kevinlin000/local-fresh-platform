<template>
  <section class="login-shell">
    <div class="login-card">
      <p class="eyebrow">在地鮮選</p>
      <h1>會員登入</h1>
      <p class="description">使用 Google 帳號登入，或在開發模式下使用假登入快捷入口。</p>

      <el-alert
        v-if="errorMessage"
        :title="errorMessage"
        type="error"
        show-icon
        class="status-banner"
        @close="errorMessage = ''"
      />

      <el-button
        class="google-button"
        type="success"
        size="large"
        :loading="redirecting"
        @click="startGoogleLogin"
      >
        使用 Google 帳號登入
      </el-button>

      <p class="divider">或</p>

      <section v-if="isDev" class="mock-panel">
        <p class="mock-title">開發模式快捷登入（僅 dev）</p>
        <div class="quick-actions">
          <el-button
            v-for="preset in presets"
            :key="preset.code"
            plain
            type="success"
            :loading="submittingCode === preset.code"
            @click="loginWithMock(preset.code)"
          >
            {{ preset.label }}
          </el-button>
        </div>

        <div class="manual-login">
          <el-input
            v-model="manualCode"
            placeholder="輸入任意測試 code"
            size="large"
            clearable
            @keyup.enter="loginWithManualCode"
          />
          <el-button
            type="primary"
            size="large"
            :loading="submittingCode === manualCode && !!manualCode"
            @click="loginWithManualCode"
          >
            登入
          </el-button>
        </div>
      </section>
    </div>
  </section>
</template>

<script setup lang="ts">
import { computed, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { useRoute, useRouter } from 'vue-router'
import { useMemberStore } from '@/stores/member'
import {
  generateOAuthState,
  saveOAuthState,
  savePostLoginRedirect
} from '@/utils/auth'

const router = useRouter()
const route = useRoute()
const memberStore = useMemberStore()

const isDev = import.meta.env.DEV || import.meta.env.VITE_ENABLE_MOCK_LOGIN === 'true'
const googleClientId = import.meta.env.VITE_GOOGLE_CLIENT_ID as string | undefined
const manualCode = ref('')
const redirecting = ref(false)
const submittingCode = ref('')
const errorMessage = ref('')

const presets = [
  { label: '測試會員 A', code: 'user_a' },
  { label: '測試會員 B', code: 'user_b' },
  { label: '測試會員 C', code: 'user_c' }
]

const redirectTarget = computed(() => {
  const queryRedirect = route.query.redirect
  return typeof queryRedirect === 'string' && queryRedirect.startsWith('/') ? queryRedirect : '/'
})

async function loginWithMock(code: string) {
  if (!code) {
    return
  }

  try {
    errorMessage.value = ''
    submittingCode.value = code
    await memberStore.mockLogin(code)
    ElMessage.success(`登入成功：${memberStore.profile.name}`)
    await router.replace(redirectTarget.value)
  } catch (error) {
    errorMessage.value = error instanceof Error ? error.message : '登入失敗'
  } finally {
    submittingCode.value = ''
  }
}

function loginWithManualCode() {
  const code = manualCode.value.trim()
  if (!code) {
    errorMessage.value = '請先輸入測試 code'
    return
  }
  void loginWithMock(code)
}

function startGoogleLogin() {
  if (!googleClientId) {
    errorMessage.value = '尚未設定 Google Client ID'
    return
  }

  errorMessage.value = ''
  redirecting.value = true

  const state = generateOAuthState()
  const redirectUri = `${window.location.origin}/oauth/callback`
  saveOAuthState(state)
  savePostLoginRedirect(redirectTarget.value)

  const authUrl = new URL('https://accounts.google.com/o/oauth2/v2/auth')
  authUrl.searchParams.set('client_id', googleClientId)
  authUrl.searchParams.set('redirect_uri', redirectUri)
  authUrl.searchParams.set('response_type', 'code')
  authUrl.searchParams.set('scope', 'openid email profile')
  authUrl.searchParams.set('state', state)
  authUrl.searchParams.set('prompt', 'select_account')

  window.location.href = authUrl.toString()
}
</script>

<style scoped>
.login-shell {
  min-height: 100vh;
  display: grid;
  place-items: center;
  padding: 48px;
}

.login-card {
  width: 560px;
  padding: 44px;
  border-radius: 28px;
  background: rgba(255, 255, 255, 0.92);
  box-shadow: 0 28px 70px rgba(61, 111, 39, 0.14);
}

.eyebrow {
  margin: 0 0 8px;
  color: #5f8452;
  font-size: 13px;
  font-weight: 700;
  letter-spacing: 0.08em;
  text-transform: uppercase;
}

h1 {
  margin: 0;
  color: #24321f;
  font-size: 34px;
}

.description {
  margin: 12px 0 0;
  color: #5d6c58;
  line-height: 1.7;
}

.status-banner {
  margin-top: 24px;
}

.google-button {
  width: 100%;
  margin-top: 28px;
  height: 52px;
  font-size: 16px;
  font-weight: 700;
}

.divider {
  margin: 28px 0 20px;
  color: #7a8874;
  text-align: center;
}

.mock-panel {
  padding: 22px;
  border: 1px dashed rgba(95, 132, 82, 0.3);
  border-radius: 20px;
  background: rgba(243, 249, 237, 0.8);
}

.mock-title {
  margin: 0 0 14px;
  color: #36502b;
  font-weight: 700;
}

.quick-actions {
  display: flex;
  gap: 12px;
  flex-wrap: wrap;
}

.manual-login {
  display: grid;
  grid-template-columns: 1fr 120px;
  gap: 12px;
  margin-top: 16px;
}
</style>
