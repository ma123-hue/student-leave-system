<template>
  <nav v-if="visible" class="bottom-nav">
    <button class="tab" :class="{ active: modelValue === 'home' }" type="button" @click="$emit('update:modelValue', 'home')">
      <span class="tab-icon" aria-hidden="true">+</span>
      <em>请假申请</em>
    </button>
    <button class="tab" :class="{ active: modelValue === 'records' }" type="button" @click="$emit('update:modelValue', 'records')">
      <span class="tab-icon" aria-hidden="true">≡</span>
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

<style scoped>
.tab-icon {
  display: grid;
  width: 34px;
  height: 34px;
  border: 0;
  border-radius: 10px;
  place-items: center;
  color: #64748b;
  background: #f1f5f9;
  font-size: 22px;
  font-weight: 700;
  line-height: 1;
}

.tab.active .tab-icon {
  color: #fff;
  background: #2563eb;
}
</style>
