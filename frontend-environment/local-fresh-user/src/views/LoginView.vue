<template>
  <section class="login-shell">
    <div class="login-visual" :style="{ backgroundImage: visualBackground }">
      <div class="visual-content">
        <p class="visual-eyebrow">菜籃日 · Cailán Day</p>
        <h2 class="visual-headline">
          每一天<br />
          都是菜籃日
        </h2>
        <p class="visual-tagline">3 人成團免運，把產地搬回家</p>
      </div>
    </div>

    <div class="login-panel">
      <div class="login-card">
        <p class="eyebrow">菜籃日 · Cailán Day</p>
        <h1>會員登入</h1>
        <p class="description">使用 Google 帳號登入，或使用試用帳號快速體驗購物流程。</p>

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
          <p class="mock-title">試用帳號快速登入</p>
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
    </div>
  </section>
</template>

<script setup lang="ts">
import { computed, ref } from 'vue'
import { ElMessage } from 'element-plus/es/components/message/index'
import { useRoute, useRouter } from 'vue-router'
import { useMemberStore } from '@/stores/member'
import loginImage from '@/assets/brand/login.png'
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
  { label: '試用會員 A', code: 'user_a' },
  { label: '試用會員 B', code: 'user_b' },
  { label: '試用會員 C', code: 'user_c' }
]

const redirectTarget = computed(() => {
  const queryRedirect = route.query.redirect
  return typeof queryRedirect === 'string' && queryRedirect.startsWith('/') ? queryRedirect : '/'
})

const visualBackground = `linear-gradient(rgba(245, 240, 230, 0.35), rgba(245, 240, 230, 0.35)), url(${loginImage})`

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
  display: flex;
  background: #f7f5ef;
}

.login-visual {
  flex: 0 0 56%;
  display: flex;
  align-items: flex-end;
  padding: clamp(32px, 6vw, 72px);
  background-size: cover;
  background-position: center;
  background-repeat: no-repeat;
}

.visual-content {
  max-width: 480px;
  padding: 18px 20px;
  border-radius: 8px;
  background: rgba(255, 253, 248, 0.82);
  backdrop-filter: blur(8px);
}

.visual-eyebrow {
  margin: 0 0 16px;
  color: var(--farm-accent);
  font-size: 14px;
  font-weight: 700;
  letter-spacing: 0;
  text-transform: uppercase;
}

.visual-headline {
  margin: 0 0 24px;
  color: var(--farm-text);
  font-size: 42px;
  line-height: 1.25;
  font-weight: 800;
}

.visual-tagline {
  margin: 0;
  color: var(--farm-muted);
  font-size: 18px;
  line-height: 1.8;
}

.login-panel {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: clamp(24px, 5vw, 48px);
  background: var(--farm-surface);
}

.login-card {
  width: 100%;
  max-width: 420px;
  padding: 32px;
  border: 1px solid var(--farm-line);
  border-radius: 8px;
  background: var(--farm-surface);
  box-shadow: var(--farm-shadow);
}

.eyebrow {
  margin: 0 0 8px;
  color: var(--farm-accent);
  font-size: 12px;
  font-weight: 700;
  letter-spacing: 0;
  text-transform: uppercase;
}

h1 {
  margin: 0;
  color: var(--farm-text);
  font-size: 32px;
}

.description {
  margin: 12px 0 0;
  color: var(--farm-muted);
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
  color: var(--farm-muted);
  text-align: center;
}

.mock-panel {
  padding: 22px;
  border: 1px dashed rgba(47, 111, 78, 0.28);
  border-radius: 8px;
  background: var(--farm-primary-soft);
}

.mock-title {
  margin: 0 0 14px;
  color: var(--farm-primary-deep);
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

@media (max-width: 768px) {
  .login-shell {
    flex-direction: column;
  }

  .login-visual {
    flex: 0 0 220px;
    padding: 20px;
  }

  .visual-headline {
    font-size: 32px;
  }

  .visual-tagline {
    font-size: 16px;
  }

  .login-panel {
    padding: 24px 16px;
  }

  .login-card {
    padding: 24px;
  }

  .manual-login {
    grid-template-columns: 1fr;
  }
}
</style>
