<template>
  <section class="page-shell">
    <div class="card">
      <p class="eyebrow">Stage 4.2</p>
      <h1>登入頁骨架</h1>
      <p>後續批次會補一鍵假登入、手動輸入 code 與 navbar 狀態同步。</p>
    </div>
  </section>
</template>

<script setup lang="ts">
</script>

<style scoped>
.page-shell {
  min-height: 100vh;
  display: grid;
  place-items: center;
}

.card {
  width: 480px;
  padding: 40px;
  border-radius: 24px;
  background: rgba(255, 255, 255, 0.88);
  box-shadow: 0 24px 60px rgba(61, 111, 39, 0.12);
}

.eyebrow {
  margin: 0 0 8px;
  color: #62864e;
  font-weight: 700;
}

h1 {
  margin: 0 0 12px;
}

p {
  margin: 0;
  line-height: 1.6;
}
</style>
