<template>
  <div class="app-shell">
    <header v-if="showNavbar" class="navbar">
      <div class="brand">
        <RouterLink to="/">在地鮮選</RouterLink>
      </div>

      <nav class="nav-links">
        <RouterLink to="/">首頁</RouterLink>
        <RouterLink to="/cart">購物車</RouterLink>
        <RouterLink to="/orders">我的訂單</RouterLink>
        <RouterLink to="/addresses">我的地址</RouterLink>
      </nav>

      <div class="member-bar">
        <span class="member-name">{{ memberDisplayName }}</span>
        <el-button type="success" plain @click="logout">登出</el-button>
      </div>
    </header>

    <main class="view-shell" :class="{ 'with-navbar': showNavbar }">
      <RouterView />
    </main>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useMemberStore } from '@/stores/member'

const route = useRoute()
const router = useRouter()
const memberStore = useMemberStore()

const showNavbar = computed(() => !route.meta.hideChrome && memberStore.isLoggedIn)
const memberDisplayName = computed(() => memberStore.profile.name || '會員')

async function logout() {
  memberStore.clearMember()
  await router.replace('/login')
}
</script>

<style>
:root {
  --farm-bg: #f4f8f1;
  --farm-surface: rgba(255, 255, 255, 0.9);
  --farm-surface-strong: #fcfefb;
  --farm-line: rgba(95, 132, 82, 0.16);
  --farm-shadow: 0 24px 60px rgba(61, 111, 39, 0.12);
  --farm-primary: #4f8a37;
  --farm-primary-deep: #2e5f1f;
  --farm-primary-soft: #edf6e8;
  --farm-text: #1f2937;
  --farm-muted: #5d6c58;
  color: var(--farm-text);
  background: var(--farm-bg);
  font-family: "PingFang TC", "Noto Sans TC", sans-serif;
  color-scheme: light;
  --el-color-primary: var(--farm-primary);
  --el-color-success: var(--farm-primary);
  --el-color-success-light-3: #6ca451;
  --el-color-success-light-5: #8ab670;
  --el-color-success-light-7: #cce0bf;
  --el-color-success-light-8: #ddebdb;
  --el-color-success-light-9: #eef6e8;
  --el-color-success-dark-2: var(--farm-primary-deep);
  --el-border-radius-base: 14px;
  --el-border-radius-round: 999px;
  --el-mask-color: rgba(29, 47, 20, 0.45);
}

* {
  box-sizing: border-box;
}

html {
  background: var(--farm-bg);
}

body {
  margin: 0;
  min-width: 1280px;
  background:
    radial-gradient(circle at top left, rgba(127, 176, 105, 0.18), transparent 32%),
    linear-gradient(180deg, #f7fbf4 0%, #eef6e8 100%);
  color: var(--farm-text);
}

a {
  color: inherit;
  text-decoration: none;
}

#app {
  min-height: 100vh;
}

.navbar {
  position: sticky;
  top: 0;
  z-index: 10;
  display: grid;
  grid-template-columns: 180px 1fr auto;
  align-items: center;
  gap: 24px;
  padding: 18px 48px;
  background: rgba(248, 252, 245, 0.94);
  backdrop-filter: blur(14px);
  border-bottom: 1px solid var(--farm-line);
  box-shadow: 0 10px 28px rgba(65, 103, 48, 0.08);
}

.brand {
  color: #23421c;
  font-size: 24px;
  font-weight: 800;
}

.nav-links {
  display: flex;
  gap: 28px;
  color: #4a6340;
  font-weight: 600;
}

.nav-links a {
  padding: 8px 14px;
  border-radius: 999px;
  transition: background-color 0.2s ease, color 0.2s ease;
}

.nav-links a.router-link-active {
  color: var(--farm-primary-deep);
  background: rgba(79, 138, 55, 0.12);
}

.member-bar {
  display: flex;
  align-items: center;
  gap: 14px;
}

.member-name {
  color: #2e4127;
  font-weight: 700;
}

.view-shell.with-navbar {
  padding-top: 8px;
}

.home-shell,
.cart-shell,
.orders-shell,
.page-shell {
  width: min(1280px, calc(100vw - 64px));
  margin: 0 auto;
}

.card,
.catalog-card,
.orders-card,
.cart-main,
.cart-summary,
.login-card {
  border: 1px solid var(--farm-line);
  box-shadow: var(--farm-shadow);
}

.eyebrow {
  color: #5f8452;
}

.el-empty {
  padding: 32px 20px;
  border: 1px dashed rgba(95, 132, 82, 0.24);
  border-radius: 24px;
  background: linear-gradient(180deg, rgba(250, 252, 247, 0.96) 0%, rgba(241, 248, 235, 0.96) 100%);
}

.el-empty__description p {
  color: var(--farm-muted);
}

.el-skeleton {
  padding: 20px;
  border-radius: 22px;
  background: rgba(248, 252, 245, 0.74);
}

.el-alert {
  border-radius: 16px;
}

.el-button--success,
.el-button--primary {
  font-weight: 700;
}

.el-drawer,
.el-dialog {
  --el-dialog-border-radius: 24px;
}
</style>
