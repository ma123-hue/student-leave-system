<template>
  <main class="app-shell">
    <slot />
  </main>
</template>

<script setup lang="ts">
// 公共页面壳，后续页面拆分时统一承载布局能力。
</script>

<style scoped>
.app-shell {
  min-height: 100vh;
}
</style>
