<template>
  <main class="login-page">
    <section class="login-hero" :style="{ backgroundImage: heroBackground }" aria-label="菜籃日管理端登入">
      <div class="brand-lockup">
        <span class="brand-mark">菜</span>
        <div>
          <strong>菜籃日管理端</strong>
          <small>Operations Console</small>
        </div>
      </div>
      <div class="hero-copy">
        <p>營運後台</p>
        <h1>把每日菜籃，管理成穩定供應鏈。</h1>
      </div>
      <dl class="ops-strip">
        <div>
          <dt>訂單</dt>
          <dd>履約追蹤</dd>
        </div>
        <div>
          <dt>庫存</dt>
          <dd>異動留痕</dd>
        </div>
        <div>
          <dt>付款</dt>
          <dd>事件可查</dd>
        </div>
      </dl>
    </section>

    <section class="login-panel admin-card">
      <p class="eyebrow">Admin Access</p>
      <h2>管理員登入</h2>
      <p class="panel-copy">展示環境可查看訂單、商品、付款事件與操作紀錄；為避免公開憑證遭濫用，面試展示帳號請洽作者。</p>
      <div class="demo-account" aria-label="展示帳號">
        <strong>面試展示帳號請洽作者</strong>
      </div>
      <el-form ref="formRef" :model="form" :rules="rules" label-position="top" @submit.prevent>
        <el-form-item label="帳號" prop="username">
          <el-input v-model="form.username" autocomplete="username" placeholder="請輸入管理員帳號" />
        </el-form-item>
        <el-form-item label="密碼" prop="password">
          <el-input v-model="form.password" type="password" autocomplete="current-password" placeholder="請輸入密碼" show-password />
        </el-form-item>
        <el-button type="primary" size="large" :loading="loading" @click="submit">登入後台</el-button>
      </el-form>
    </section>
  </main>
</template>

<script setup lang="ts">
import { reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import type { FormInstance, FormRules } from 'element-plus'
import { ElMessage } from 'element-plus'
import { useUserStore } from '@/store'
import loginImage from '@/assets/brand/login.png'

const router = useRouter()
const route = useRoute()
const userStore = useUserStore()
const formRef = ref<FormInstance>()
const loading = ref(false)
const heroBackground = `linear-gradient(180deg, rgba(20, 31, 24, 0.16), rgba(20, 31, 24, 0.68)), url(${loginImage})`

const form = reactive({
  username: '',
  password: ''
})

const rules: FormRules = {
  username: [{ required: true, message: '請輸入帳號', trigger: 'blur' }],
  password: [{ required: true, message: '請輸入密碼', trigger: 'blur' }]
}

async function submit() {
  await formRef.value?.validate()
  loading.value = true
  try {
    await userStore.login(form)
    ElMessage.success('登入成功')
    router.push(String(route.query.redirect || '/dashboard'))
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.login-page {
  min-height: 100vh;
  display: grid;
  grid-template-columns: minmax(0, 1fr) 440px;
  background: var(--admin-bg);
}

.login-hero {
  min-height: 100vh;
  display: flex;
  flex-direction: column;
  justify-content: space-between;
  padding: 56px;
  color: #fffdf8;
  background-size: cover;
  background-position: center;
}

.brand-lockup {
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
  color: var(--admin-green-dark);
  background: #fffdf8;
  font-weight: 800;
}

.brand-lockup strong,
.brand-lockup small {
  display: block;
}

.brand-lockup strong {
  font-size: 20px;
}

.brand-lockup small {
  margin-top: 2px;
  opacity: 0.84;
}

.eyebrow {
  color: var(--admin-green);
  font-weight: 800;
  letter-spacing: 0;
  text-transform: uppercase;
}

h1 {
  max-width: 720px;
  margin: 16px 0 0;
  font-size: 64px;
  line-height: 1.02;
}

.hero-copy {
  max-width: 760px;
}

.hero-copy p {
  margin: 0;
  font-size: 18px;
  font-weight: 800;
}

.ops-strip {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 10px;
  margin: 0;
}

.ops-strip div {
  padding: 14px;
  border: 1px solid rgba(255, 255, 255, 0.28);
  border-radius: 8px;
  background: rgba(16, 24, 20, 0.34);
}

.ops-strip dt {
  font-weight: 800;
}

.ops-strip dd {
  margin: 4px 0 0;
  opacity: 0.82;
}

.login-panel {
  align-self: center;
  width: 100%;
  margin-right: 8vw;
  padding: 34px;
}

.login-panel h2 {
  margin: 8px 0 10px;
}

.panel-copy {
  margin: 0 0 18px;
  color: var(--admin-muted);
  line-height: 1.7;
}

.demo-account {
  display: grid;
  grid-template-columns: 72px 1fr;
  gap: 8px 10px;
  margin-bottom: 22px;
  padding: 12px;
  border: 1px solid rgba(45, 106, 79, 0.16);
  border-radius: 8px;
  background: var(--admin-surface-active);
  color: var(--admin-muted);
  font-size: 13px;
}

.demo-account strong {
  color: var(--admin-ink);
  font-family: ui-monospace, SFMono-Regular, Menlo, Monaco, Consolas, monospace;
}

.login-panel .el-button {
  width: 100%;
  margin-top: 8px;
}

@media (max-width: 900px) {
  .login-page {
    display: block;
  }

  .login-hero {
    min-height: 360px;
    padding: 28px;
  }

  h1 {
    font-size: 40px;
  }

  .ops-strip {
    grid-template-columns: 1fr;
  }

  .login-panel {
    width: auto;
    margin: 20px;
  }
}
</style>
