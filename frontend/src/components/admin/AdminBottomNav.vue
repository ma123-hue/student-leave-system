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
      <span class="tab-icon" aria-hidden="true">{{ item.icon }}</span>
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
  { page: 'approval', label: '请假审批', icon: '✓' },
  { page: 'stats', label: '数据统计', icon: '▥' },
  { page: 'role', label: '角色切换', icon: '⇄' },
  { page: 'history', label: '审批记录', icon: '≡' }
];
</script>

<style scoped>
.tab-icon {
  display: grid;
  width: 30px;
  height: 30px;
  border: 0;
  border-radius: 9px;
  place-items: center;
  color: #64748b;
  background: #f1f5f9;
  font-size: 20px;
  font-weight: 700;
  line-height: 1;
}

.bottom-tab.active .tab-icon {
  color: #fff;
  background: #2563eb;
}
</style>
