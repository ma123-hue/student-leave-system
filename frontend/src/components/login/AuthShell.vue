<template>
  <main class="phone-shell">
    <header class="app-header">
      <div class="header-title">{{ title }}</div>
      <!-- <div class="mini-actions" aria-hidden="true">
        <span></span><span></span><span></span>
        <i></i>
      </div> -->
    </header>

    <section class="view auth-view">
      <slot />
    </section>

    <ToastMessage :message="toast" />
  </main>
</template>

<script setup lang="ts">
import ToastMessage from './ToastMessage.vue';

defineProps({
  title: {
    type: String,
    required: true
  },
  toast: {
    type: String,
    default: ''
  }
});
</script>
