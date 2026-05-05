<template>
  <section class="page-shell">
    <div class="card">
      <p class="eyebrow">訂單列表</p>
      <h1>我的訂單頁骨架</h1>
      <p>後續批次會補一般訂單與揪團訂單區分顯示。</p>
    </div>
  </section>
</template>

<script setup lang="ts">
</script>

<style scoped>
.page-shell {
  padding: 48px;
}

.card {
  max-width: 960px;
  margin: 0 auto;
  padding: 36px;
  border-radius: 24px;
  background: rgba(255, 255, 255, 0.9);
  box-shadow: 0 24px 60px rgba(61, 111, 39, 0.12);
}

.eyebrow {
  margin: 0 0 8px;
  color: #62864e;
  font-weight: 700;
}
</style>
