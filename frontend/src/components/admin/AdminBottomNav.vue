<template>
  <nav v-if="visible" class="bottom-nav">
    <button
      v-for="item in items"
      :key="item.page"
      class="bottom-tab"
      :class="{ active: modelValue === item.page }"
      type="button"
      @click="$emit('update:modelValue', item.page)"
    >
      <span class="tab-icon"></span>
      <em>{{ item.label }}</em>
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

const items = [
  { page: 'approval', label: '请假审批' },
  { page: 'stats', label: '数据统计' },
  { page: 'role', label: '角色切换' },
  { page: 'history', label: '审批记录' }
];
</script>
