<template>
  <section class="dashboard-grid">
    <article v-for="card in cards" :key="card.label" class="admin-card admin-card-pad metric-card">
      <p>{{ card.label }}</p>
      <strong>{{ card.value }}</strong>
      <span>{{ card.caption }}</span>
    </article>

    <article class="admin-card admin-card-pad wide">
      <h2>升級進度</h2>
      <el-steps :active="1" finish-status="success">
        <el-step title="Vue 3 骨架" description="Vite、Pinia、Router 4、Element Plus" />
        <el-step title="功能頁搬遷" description="訂單、商品、分類、員工、報表" />
        <el-step title="部署前驗證" description="E2E、API smoke test、AWS config" />
      </el-steps>
    </article>
  </section>
</template>

<script setup lang="ts">
const cards = [
  { label: '後端版本', value: 'Spring Boot 3.5', caption: '已完成後端升級與測試' },
  { label: '資料庫', value: 'Flyway Ready', caption: 'fresh MySQL 可初始化' },
  { label: '前端', value: 'Vue 3', caption: '管理端升級進行中' }
]
</script>

<style scoped>
.dashboard-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 18px;
}

.metric-card {
  min-height: 150px;
}

.metric-card p,
.metric-card span {
  color: var(--admin-muted);
}

.metric-card strong {
  display: block;
  margin: 16px 0 10px;
  font-size: 30px;
}

.wide {
  grid-column: 1 / -1;
}
</style>
