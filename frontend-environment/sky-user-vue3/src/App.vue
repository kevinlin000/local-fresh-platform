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
  color: #1f2937;
  background: #f4f8f1;
  font-family: "PingFang TC", "Noto Sans TC", sans-serif;
}

* {
  box-sizing: border-box;
}

body {
  margin: 0;
  min-width: 1280px;
  background:
    radial-gradient(circle at top left, rgba(127, 176, 105, 0.18), transparent 32%),
    linear-gradient(180deg, #f7fbf4 0%, #eef6e8 100%);
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
  border-bottom: 1px solid rgba(95, 132, 82, 0.16);
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
</style>
