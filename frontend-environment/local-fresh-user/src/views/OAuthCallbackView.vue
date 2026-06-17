<template>
  <section class="callback-shell">
    <div class="callback-card">
      <template v-if="loading">
        <div class="spinner" />
        <h1>正在完成 Google 登入</h1>
        <p>請稍候，系統正在驗證你的 Google 身分。</p>
      </template>

      <template v-else>
        <h1>Google 登入失敗</h1>
        <p>{{ errorMessage }}</p>
        <el-button type="primary" size="large" @click="goToLogin">回到登入頁</el-button>
      </template>
    </div>
  </section>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { useMemberStore } from '@/stores/member'
import {
  clearOAuthState,
  clearPostLoginRedirect,
  consumePostLoginRedirect,
  getOAuthState
} from '@/utils/auth'

const router = useRouter()
const memberStore = useMemberStore()

const loading = ref(true)
const errorMessage = ref('')

function fail(message: string) {
  loading.value = false
  errorMessage.value = message
  clearOAuthState()
  clearPostLoginRedirect()
}

async function completeGoogleLogin() {
  const params = new URLSearchParams(window.location.search)
  const code = params.get('code')
  const state = params.get('state')
  const expectedState = getOAuthState()

  if (!code) {
    fail('Google 沒有回傳授權碼，請重新登入。')
    return
  }

  if (!state || !expectedState || state !== expectedState) {
    fail('Google 登入狀態驗證失敗，請重新操作。')
    return
  }

  try {
    const redirectUri = `${window.location.origin}/oauth/callback`
    await memberStore.googleOAuthLogin(code, redirectUri)
    clearOAuthState()
    ElMessage.success(`登入成功：${memberStore.profile.name}`)
    await router.replace(consumePostLoginRedirect() || '/')
  } catch (error) {
    fail(error instanceof Error ? error.message : 'Google 登入失敗')
  }
}

function goToLogin() {
  void router.replace('/login')
}

onMounted(() => {
  void completeGoogleLogin()
})
</script>

<style scoped>
.callback-shell {
  min-height: 100vh;
  display: grid;
  place-items: center;
  padding: 24px;
  background: var(--farm-bg);
}

.callback-card {
  width: min(520px, 100%);
  padding: 36px;
  border: 1px solid var(--farm-line);
  border-radius: 8px;
  text-align: center;
  background: var(--farm-surface);
  box-shadow: var(--farm-shadow);
}

.spinner {
  width: 36px;
  height: 36px;
  margin: 0 auto;
  border: 4px solid rgba(95, 132, 82, 0.18);
  border-top-color: var(--farm-primary);
  border-radius: 50%;
  animation: spin 0.9s linear infinite;
}

h1 {
  margin: 18px 0 12px;
  color: var(--farm-text);
}

p {
  margin: 0 0 24px;
  color: var(--farm-muted);
  line-height: 1.7;
}

@keyframes spin {
  from {
    transform: rotate(0deg);
  }

  to {
    transform: rotate(360deg);
  }
}
</style>
