<template>
  <section class="login-shell">
    <div class="login-visual" :style="{ backgroundImage: visualBackground }">
      <div class="visual-content">
        <p class="visual-eyebrow">菜籃日 · Cailán Day</p>
        <h2 class="visual-headline">
          每一天<br />
          都是菜籃日
        </h2>
        <p class="visual-tagline">3 人成團免運,把產地搬回家</p>
      </div>
    </div>

    <div class="login-panel">
      <div class="login-card">
        <p class="eyebrow">菜籃日 · Cailán Day</p>
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
    </div>
  </section>
</template>

<script setup lang="ts">
import { computed, ref } from 'vue'
import { ElMessage } from 'element-plus'
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
  { label: '測試會員 A', code: 'user_a' },
  { label: '測試會員 B', code: 'user_b' },
  { label: '測試會員 C', code: 'user_c' }
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
  background: #f5f0e6;
}

.login-visual {
  flex: 0 0 60%;
  display: flex;
  align-items: flex-end;
  padding: 80px;
  background-size: cover;
  background-position: center;
  background-repeat: no-repeat;
}

.visual-content {
  max-width: 480px;
}

.visual-eyebrow {
  margin: 0 0 16px;
  color: #4a7c3a;
  font-size: 14px;
  font-weight: 700;
  letter-spacing: 0.15em;
  text-transform: uppercase;
}

.visual-headline {
  margin: 0 0 24px;
  color: #2c2c2c;
  font-family: 'Noto Serif TC', serif;
  font-size: 48px;
  line-height: 1.3;
  font-weight: 700;
}

.visual-tagline {
  margin: 0;
  color: rgba(44, 44, 44, 0.72);
  font-size: 18px;
  line-height: 1.8;
}

.login-panel {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 48px;
  background: #ffffff;
}

.login-card {
  width: 100%;
  max-width: 420px;
  padding: 44px;
  border-radius: 28px;
  background: rgba(255, 255, 255, 0.96);
  box-shadow: 0 24px 56px rgba(74, 124, 58, 0.12);
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

@media (max-width: 768px) {
  .login-shell {
    flex-direction: column;
  }

  .login-visual {
    flex: 0 0 240px;
    padding: 32px;
  }

  .visual-headline {
    font-size: 32px;
  }

  .visual-tagline {
    font-size: 16px;
  }

  .login-panel {
    padding: 32px 24px;
  }

  .login-card {
    padding: 32px 24px;
  }

  .manual-login {
    grid-template-columns: 1fr;
  }
}
</style>
