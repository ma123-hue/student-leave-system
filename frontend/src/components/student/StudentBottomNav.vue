<template>
  <nav v-if="visible" class="bottom-nav">
    <button class="tab" :class="{ active: modelValue === 'home' }" type="button" @click="$emit('update:modelValue', 'home')">
      <span class="tab-icon blue"></span>
      <em>请假申请</em>
    </button>
    <button class="tab" :class="{ active: modelValue === 'records' }" type="button" @click="$emit('update:modelValue', 'records')">
      <span class="tab-icon gray"></span>
      <em>请假记录</em>
    </button>
  </nav>
</template>

<script setup lang="ts">
defineProps({
  modelValue: {
    type: String,
    required: true
  },
  visible: {
    type: Boolean,
    default: true
  }
});

defineEmits(['update:modelValue']);
</script>
