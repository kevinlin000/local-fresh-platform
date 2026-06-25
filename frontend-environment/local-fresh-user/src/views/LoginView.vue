<template>
  <section class="login-shell">
    <aside class="login-visual" :style="{ backgroundImage: visualBackground }" aria-label="菜籃日會員登入">
      <div class="brand-lockup">
        <span class="brand-mark">菜</span>
        <div>
          <strong>菜籃日</strong>
          <small>Cailán Day</small>
        </div>
      </div>

      <div class="visual-copy">
        <p>台灣在地小農生鮮</p>
        <h1>今天的菜籃，從這裡開始。</h1>
      </div>

      <dl class="service-strip">
        <div>
          <dt>配送</dt>
          <dd>冷藏到府</dd>
        </div>
        <div>
          <dt>揪團</dt>
          <dd>3 人成團免運</dd>
        </div>
        <div>
          <dt>付款</dt>
          <dd>付款狀態可查</dd>
        </div>
      </dl>
    </aside>

    <main class="login-panel">
      <section class="account-panel" aria-labelledby="account-title">
        <p class="eyebrow">會員帳號</p>
        <h2 id="account-title">登入菜籃日</h2>
        <p class="description">使用 Email 管理購物車、常用地址、訂單與揪團紀錄。</p>

        <el-alert
          v-if="errorMessage"
          :title="errorMessage"
          type="error"
          show-icon
          class="status-banner"
          @close="errorMessage = ''"
        />

        <el-tabs v-model="activeTab" class="auth-tabs" stretch>
          <el-tab-pane label="登入" name="login">
            <el-form class="auth-form" label-position="top" @submit.prevent>
              <el-form-item label="Email">
                <el-input
                  v-model="loginForm.email"
                  type="email"
                  size="large"
                  autocomplete="email"
                  placeholder="name@example.com"
                  @keyup.enter="loginWithPassword"
                />
              </el-form-item>
              <el-form-item label="密碼">
                <el-input
                  v-model="loginForm.password"
                  type="password"
                  size="large"
                  show-password
                  autocomplete="current-password"
                  placeholder="請輸入密碼"
                  @keyup.enter="loginWithPassword"
                />
              </el-form-item>
              <el-button
                class="primary-action"
                type="primary"
                size="large"
                :loading="submittingMode === 'login'"
                @click="loginWithPassword"
              >
                登入
              </el-button>
            </el-form>
          </el-tab-pane>

          <el-tab-pane label="註冊" name="register">
            <el-form class="auth-form" label-position="top" @submit.prevent>
              <el-form-item label="姓名">
                <el-input
                  v-model="registerForm.name"
                  size="large"
                  autocomplete="name"
                  placeholder="收件與訂單顯示名稱"
                />
              </el-form-item>
              <el-form-item label="Email">
                <el-input
                  v-model="registerForm.email"
                  type="email"
                  size="large"
                  autocomplete="email"
                  placeholder="name@example.com"
                />
              </el-form-item>
              <el-form-item label="手機">
                <el-input
                  v-model="registerForm.phone"
                  size="large"
                  autocomplete="tel"
                  placeholder="0912345678"
                />
              </el-form-item>
              <el-form-item label="密碼">
                <el-input
                  v-model="registerForm.password"
                  type="password"
                  size="large"
                  show-password
                  autocomplete="new-password"
                  placeholder="至少 8 個字"
                  @keyup.enter="registerWithPassword"
                />
              </el-form-item>
              <el-button
                class="primary-action"
                type="primary"
                size="large"
                :loading="submittingMode === 'register'"
                @click="registerWithPassword"
              >
                建立帳號
              </el-button>
            </el-form>
          </el-tab-pane>
        </el-tabs>

        <div class="secondary-auth">
          <el-button
            class="google-button"
            plain
            size="large"
            :loading="redirecting"
            @click="startGoogleLogin"
          >
            使用 Google 登入
          </el-button>

          <section v-if="isDev" class="demo-login">
            <div class="demo-header">
              <span>快速體驗</span>
              <small>不建立新資料</small>
            </div>
            <div class="quick-actions">
              <el-button
                v-for="preset in presets"
                :key="preset.code"
                plain
                :loading="submittingCode === preset.code"
                @click="loginWithMock(preset.code)"
              >
                {{ preset.label }}
              </el-button>
            </div>
          </section>
        </div>
      </section>
    </main>
  </section>
</template>

<script setup lang="ts">
import { computed, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus/es/components/message/index'
import { useRoute, useRouter } from 'vue-router'
import { useMemberStore } from '@/stores/member'
import loginImage from '@/assets/brand/login.png'
import {
  generateOAuthState,
  saveOAuthState,
  savePostLoginRedirect
} from '@/utils/auth'

type AuthMode = 'login' | 'register'

const router = useRouter()
const route = useRoute()
const memberStore = useMemberStore()

const isDev = import.meta.env.DEV || import.meta.env.VITE_ENABLE_MOCK_LOGIN === 'true'
const googleClientId = import.meta.env.VITE_GOOGLE_CLIENT_ID as string | undefined
const activeTab = ref<AuthMode>('login')
const redirecting = ref(false)
const submittingMode = ref<AuthMode | ''>('')
const submittingCode = ref('')
const errorMessage = ref('')

const loginForm = reactive({
  email: '',
  password: ''
})

const registerForm = reactive({
  name: '',
  email: '',
  phone: '',
  password: ''
})

const presets = [
  { label: '會員 A', code: 'user_a' },
  { label: '會員 B', code: 'user_b' },
  { label: '會員 C', code: 'user_c' }
]

const redirectTarget = computed(() => {
  const queryRedirect = route.query.redirect
  return typeof queryRedirect === 'string' && queryRedirect.startsWith('/') ? queryRedirect : '/'
})

const visualBackground = `linear-gradient(180deg, rgba(22, 31, 25, 0.18), rgba(22, 31, 25, 0.62)), url(${loginImage})`

async function loginWithPassword() {
  const email = normalizeEmail(loginForm.email)
  if (!validateEmailPassword(email, loginForm.password)) {
    return
  }

  try {
    errorMessage.value = ''
    submittingMode.value = 'login'
    await memberStore.passwordLogin({
      email,
      password: loginForm.password
    })
    await finishLogin('登入成功')
  } catch (error) {
    errorMessage.value = error instanceof Error ? error.message : '登入失敗'
  } finally {
    submittingMode.value = ''
  }
}

async function registerWithPassword() {
  const email = normalizeEmail(registerForm.email)
  const name = registerForm.name.trim()
  const phone = registerForm.phone.trim()
  if (!name) {
    errorMessage.value = '請輸入姓名'
    return
  }
  if (phone && !/^09\d{8}$/.test(phone)) {
    errorMessage.value = '手機格式需為 09 開頭的 10 碼號碼'
    return
  }
  if (!validateEmailPassword(email, registerForm.password)) {
    return
  }

  try {
    errorMessage.value = ''
    submittingMode.value = 'register'
    await memberStore.register({
      name,
      email,
      phone: phone || undefined,
      password: registerForm.password
    })
    await finishLogin('帳號已建立')
  } catch (error) {
    errorMessage.value = error instanceof Error ? error.message : '註冊失敗'
  } finally {
    submittingMode.value = ''
  }
}

async function loginWithMock(code: string) {
  try {
    errorMessage.value = ''
    submittingCode.value = code
    await memberStore.mockLogin(code)
    await finishLogin('已進入體驗帳號')
  } catch (error) {
    errorMessage.value = error instanceof Error ? error.message : '登入失敗'
  } finally {
    submittingCode.value = ''
  }
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

async function finishLogin(message: string) {
  ElMessage.success(message)
  await router.replace(redirectTarget.value)
}

function validateEmailPassword(email: string, password: string) {
  if (!email || !/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(email)) {
    errorMessage.value = '請輸入正確的 Email'
    return false
  }
  if (!password || password.length < 8) {
    errorMessage.value = '密碼至少需要 8 個字'
    return false
  }
  return true
}

function normalizeEmail(email: string) {
  return email.trim().toLowerCase()
}
</script>

<style scoped>
.login-shell {
  min-height: 100vh;
  display: grid;
  grid-template-columns: minmax(420px, 52%) minmax(420px, 48%);
  background: var(--farm-surface);
}

.login-visual {
  position: relative;
  display: flex;
  flex-direction: column;
  justify-content: space-between;
  padding: clamp(28px, 5vw, 64px);
  background-size: cover;
  background-position: center;
  color: #fffdf8;
}

.brand-lockup,
.footer-brand {
  display: inline-flex;
  align-items: center;
  gap: 12px;
}

.brand-mark {
  width: 42px;
  height: 42px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  border-radius: 8px;
  background: #fffdf8;
  color: var(--farm-primary-deep);
  font-weight: 800;
}

.brand-lockup strong {
  display: block;
  font-size: 20px;
}

.brand-lockup small {
  display: block;
  margin-top: 2px;
  opacity: 0.84;
}

.visual-copy {
  max-width: 560px;
}

.visual-copy p {
  margin: 0 0 16px;
  font-size: 15px;
  font-weight: 700;
}

.visual-copy h1 {
  margin: 0;
  max-width: 9em;
  font-size: clamp(34px, 4.2vw, 52px);
  line-height: 1.12;
  font-weight: 800;
}

.service-strip {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 1px;
  margin: 0;
  overflow: hidden;
  border: 1px solid rgba(255, 253, 248, 0.34);
  border-radius: 8px;
  background: rgba(255, 253, 248, 0.2);
}

.service-strip div {
  padding: 16px;
  background: rgba(22, 31, 25, 0.34);
}

.service-strip dt {
  margin: 0 0 6px;
  font-size: 12px;
  opacity: 0.78;
}

.service-strip dd {
  margin: 0;
  font-weight: 800;
}

.login-panel {
  display: flex;
  align-items: center;
  justify-content: center;
  padding: clamp(24px, 5vw, 72px);
  background:
    linear-gradient(180deg, rgba(247, 245, 239, 0.78), rgba(255, 255, 255, 0.96)),
    var(--farm-surface);
}

.account-panel {
  width: min(100%, 440px);
}

.eyebrow {
  margin: 0 0 8px;
  color: var(--farm-accent);
  font-size: 13px;
  font-weight: 800;
}

h2 {
  margin: 0;
  color: var(--farm-text);
  font-size: 34px;
  line-height: 1.2;
}

.description {
  margin: 12px 0 0;
  color: var(--farm-muted);
  line-height: 1.7;
}

.status-banner {
  margin-top: 22px;
}

.auth-tabs {
  margin-top: 26px;
}

.auth-form {
  padding-top: 10px;
}

.primary-action,
.google-button {
  width: 100%;
  height: 48px;
  font-weight: 800;
}

.secondary-auth {
  margin-top: 20px;
  padding-top: 20px;
  border-top: 1px solid var(--farm-line);
}

.demo-login {
  margin-top: 18px;
  padding: 16px;
  border: 1px solid var(--farm-line);
  border-radius: 8px;
  background: var(--farm-surface-strong);
}

.demo-header {
  display: flex;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 12px;
  color: var(--farm-text);
  font-weight: 800;
}

.demo-header small {
  color: var(--farm-muted);
  font-weight: 600;
}

.quick-actions {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 8px;
}

.quick-actions :deep(.el-button) {
  margin: 0;
}

@media (max-width: 900px) {
  .login-shell {
    grid-template-columns: 1fr;
  }

  .login-visual {
    min-height: 300px;
  }
}

@media (max-width: 560px) {
  .login-visual {
    min-height: 260px;
    padding: 24px 18px;
  }

  .visual-copy h1 {
    font-size: 30px;
  }

  .login-panel {
    padding: 28px 18px 40px;
  }

  h2 {
    font-size: 30px;
  }

  .quick-actions {
    grid-template-columns: 1fr;
  }
}
</style>
