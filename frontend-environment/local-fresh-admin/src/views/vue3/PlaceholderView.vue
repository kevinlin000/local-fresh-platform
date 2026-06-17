<template>
  <section class="admin-card admin-card-pad placeholder">
    <p class="eyebrow">Vue 3 migration</p>
    <h2>{{ title }} 功能頁正在搬遷</h2>
    <p>目前已完成 Vue 3 管理端骨架。下一個 checkpoint 會把此頁的 API、表格、表單與互動完整搬到 Element Plus。</p>
  </section>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useRoute } from 'vue-router'

const route = useRoute()
const title = computed(() => String(route.meta.title || '管理'))
</script>

<style scoped>
.placeholder {
  min-height: 360px;
}

.eyebrow {
  color: var(--admin-gold);
  font-weight: 800;
  letter-spacing: 0.14em;
  text-transform: uppercase;
}

h2 {
  margin: 12px 0;
  font-size: 34px;
}

p {
  max-width: 640px;
  color: var(--admin-muted);
  line-height: 1.8;
}
</style>
