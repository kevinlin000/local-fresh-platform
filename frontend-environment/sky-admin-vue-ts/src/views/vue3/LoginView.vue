<template>
  <main class="login-page">
    <section class="login-hero">
      <p class="eyebrow">Local Fresh Platform</p>
      <h1>把每日菜籃，管理得像一條穩定供應鏈。</h1>
      <p class="hero-copy">Spring Boot 3 後端、MySQL / Redis、AWS-ready 的在地小農生鮮配送平台。</p>
    </section>

    <section class="login-panel admin-card">
      <h2>管理員登入</h2>
      <el-form ref="formRef" :model="form" :rules="rules" label-position="top" @submit.prevent>
        <el-form-item label="帳號" prop="username">
          <el-input v-model="form.username" autocomplete="username" />
        </el-form-item>
        <el-form-item label="密碼" prop="password">
          <el-input v-model="form.password" type="password" autocomplete="current-password" show-password />
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

const router = useRouter()
const route = useRoute()
const userStore = useUserStore()
const formRef = ref<FormInstance>()
const loading = ref(false)

const form = reactive({
  username: 'admin',
  password: '123456'
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
  grid-template-columns: minmax(0, 1.15fr) 420px;
  gap: 48px;
  align-items: center;
  padding: 64px 8vw;
}

.login-hero {
  max-width: 760px;
}

.eyebrow {
  color: var(--admin-green);
  font-weight: 800;
  letter-spacing: 0.18em;
  text-transform: uppercase;
}

h1 {
  margin: 20px 0;
  font-size: clamp(46px, 6vw, 86px);
  line-height: 0.95;
}

.hero-copy {
  max-width: 560px;
  color: var(--admin-muted);
  font-size: 18px;
  line-height: 1.8;
}

.login-panel {
  padding: 34px;
}

.login-panel h2 {
  margin: 0 0 28px;
}

.login-panel .el-button {
  width: 100%;
  margin-top: 8px;
}
</style>
