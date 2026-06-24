<template>
  <el-config-provider :locale="zhTw">
    <div class="app-shell">
      <header v-if="showNavbar" class="navbar">
        <div class="brand">
          <RouterLink to="/" class="brand-link">
            <span class="brand-mark">菜</span>
            <span>
              <strong>菜籃日</strong>
              <small>Cailán Day</small>
            </span>
          </RouterLink>
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
  </el-config-provider>
</template>

<script setup lang="ts">
import zhTw from 'element-plus/es/locale/lang/zh-tw'
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
  --farm-bg: #f6f5f1;
  --farm-surface: #ffffff;
  --farm-surface-strong: #fbfaf7;
  --farm-line: rgba(31, 42, 36, 0.12);
  --farm-shadow: 0 12px 30px rgba(28, 39, 32, 0.07);
  --farm-primary: #2d6a4f;
  --farm-primary-deep: #1f4b39;
  --farm-primary-soft: #e9f1ec;
  --farm-accent: #b66a3c;
  --farm-accent-soft: #f7ece4;
  --farm-text: #1f2a24;
  --farm-muted: #66746b;
  --farm-radius: 8px;
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
  --el-border-radius-base: 8px;
  --el-border-radius-round: 999px;
  --el-mask-color: rgba(29, 47, 20, 0.45);
}

* {
  box-sizing: border-box;
  letter-spacing: 0;
}

html {
  background: var(--farm-bg);
}

body {
  margin: 0;
  min-width: 0;
  background: var(--farm-bg);
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
  grid-template-columns: minmax(150px, auto) 1fr auto;
  align-items: center;
  gap: 20px;
  padding: 10px clamp(16px, 4vw, 48px);
  background: rgba(255, 255, 255, 0.93);
  backdrop-filter: blur(16px);
  border-bottom: 1px solid var(--farm-line);
  box-shadow: 0 5px 18px rgba(28, 39, 32, 0.045);
}

.brand {
  color: var(--farm-primary-deep);
}

.brand-link {
  display: inline-flex;
  align-items: center;
  gap: 10px;
}

.brand-mark {
  display: grid;
  place-items: center;
  width: 34px;
  height: 34px;
  border-radius: 8px;
  background: #22372d;
  color: #fffaf1;
  font-weight: 800;
}

.brand strong {
  display: block;
  font-size: 17px;
  line-height: 1.1;
}

.brand small {
  display: block;
  margin-top: 2px;
  color: var(--farm-muted);
  font-size: 11px;
  line-height: 1;
}

.nav-links {
  display: flex;
  gap: 4px;
  color: #415148;
  font-weight: 600;
  justify-content: center;
}

.nav-links a {
  padding: 8px 11px;
  border-radius: 7px;
  transition: background-color 0.2s ease, color 0.2s ease;
}

.nav-links a.router-link-active {
  color: var(--farm-primary-deep);
  background: #edf2ee;
}

.member-bar {
  display: flex;
  align-items: center;
  justify-content: flex-end;
  gap: 10px;
}

.member-name {
  max-width: 128px;
  overflow: hidden;
  color: #2f3c34;
  font-weight: 700;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.view-shell.with-navbar {
  padding-top: 0;
}

.home-shell,
.cart-shell,
.orders-shell,
.page-shell {
  width: min(1180px, calc(100% - 32px));
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
  color: var(--farm-accent);
}

.el-empty {
  padding: 32px 20px;
  border: 1px dashed rgba(95, 132, 82, 0.24);
  border-radius: 8px;
  background: #fffdf8;
}

.el-empty__description p {
  color: var(--farm-muted);
}

.el-skeleton {
  padding: 20px;
  border-radius: 8px;
  background: #fffdf8;
}

.el-alert {
  border-radius: 8px;
}

.el-button--success,
.el-button--primary {
  font-weight: 700;
}

.el-drawer,
.el-dialog {
  --el-dialog-border-radius: 8px;
}

@media (max-width: 860px) {
  .navbar {
    grid-template-columns: 1fr auto;
    gap: 12px;
    padding: 10px 14px;
  }

  .brand {
    grid-column: 1;
    grid-row: 1;
  }

  .member-bar {
    grid-column: 2;
    grid-row: 1;
  }

  .nav-links {
    grid-column: 1 / -1;
    grid-row: 2;
    justify-content: flex-start;
    overflow-x: auto;
    padding-bottom: 2px;
    scrollbar-width: none;
  }

  .nav-links::-webkit-scrollbar {
    display: none;
  }

  .nav-links a {
    flex: 0 0 auto;
    padding: 8px 10px;
    font-size: 14px;
  }

  .member-name {
    display: none;
  }

  .home-shell,
  .cart-shell,
  .orders-shell,
  .page-shell {
    width: min(100% - 24px, 1180px);
  }
}

@media (max-width: 640px) {
  .brand small {
    display: none;
  }

  .brand strong {
    font-size: 16px;
  }

  .member-bar .el-button {
    padding-inline: 10px;
  }

  .el-dialog {
    width: calc(100vw - 24px) !important;
  }

  .el-drawer.rtl {
    width: min(100vw, 420px) !important;
  }
}
</style>
