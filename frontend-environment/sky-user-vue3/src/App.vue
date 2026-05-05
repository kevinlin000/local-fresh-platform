<template>
  <RouterView />
</template>

<script setup lang="ts">
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
</style>
