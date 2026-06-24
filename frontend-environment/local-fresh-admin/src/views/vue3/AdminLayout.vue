<template>
  <div class="admin-shell">
    <aside class="admin-sidebar">
      <div class="admin-brand">
        <div class="admin-brand-mark">菜</div>
        <div>
          <div>菜籃日</div>
          <small>Cailán Admin</small>
        </div>
      </div>

      <nav class="admin-nav">
        <RouterLink to="/dashboard">工作台</RouterLink>
        <RouterLink to="/orders">訂單管理</RouterLink>
        <RouterLink to="/products">商品管理</RouterLink>
        <RouterLink to="/operation-logs">操作紀錄</RouterLink>
        <RouterLink to="/payment-events">付款事件</RouterLink>
        <RouterLink to="/gift-boxes">直送箱管理</RouterLink>
        <RouterLink to="/categories">分類管理</RouterLink>
        <RouterLink to="/employees">員工管理</RouterLink>
        <RouterLink to="/reports">資料統計</RouterLink>
      </nav>
    </aside>

    <main class="admin-main">
      <header class="admin-topbar">
        <div>
          <h1>{{ currentTitle }}</h1>
          <p>訂單、商品、庫存與直送箱營運管理</p>
        </div>
        <div class="topbar-actions">
          <span class="environment-pill">Local demo</span>
          <el-button type="primary" plain @click="handleLogout">登出</el-button>
        </div>
      </header>
      <RouterView />
    </main>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useUserStore } from '@/store'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()

const currentTitle = computed(() => String(route.meta.title || '管理後台'))

async function handleLogout() {
  await userStore.logout()
  router.push('/login')
}
</script>

<style scoped>
h1 {
  margin: 0 0 6px;
  font-size: 28px;
  line-height: 1.15;
}

p {
  margin: 0;
  color: var(--admin-muted);
}

small {
  color: var(--admin-muted);
  font-size: 11px;
  letter-spacing: 0;
  text-transform: uppercase;
}

.topbar-actions {
  display: flex;
  gap: 10px;
  align-items: center;
}

.environment-pill {
  padding: 5px 9px;
  border: 1px solid var(--admin-line);
  border-radius: 999px;
  background: #fbfcfa;
  color: var(--admin-muted);
  font-size: 12px;
  font-weight: 800;
}
</style>
