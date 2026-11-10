<template>
  <div class="bar-list">
    <div v-for="row in rows" :key="row[labelKey]" class="bar-row">
      <span>{{ row[labelKey] }}</span>
      <div class="bar-track">
        <div class="bar-fill" :style="{ width: `${barWidth(row)}%` }"></div>
      </div>
      <em>{{ Number(row[valueKey] || 0) }}人</em>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue';

const props = defineProps({
  rows: {
    type: Array,
    default: () => []
  },
  labelKey: {
    type: String,
    required: true
  },
  valueKey: {
    type: String,
    required: true
  }
});

const maxValue = computed(() => Math.max(...props.rows.map(row => Number(row[props.valueKey] || 0)), 1));

function barWidth(row) {
  return Math.max(6, Math.round((Number(row[props.valueKey] || 0) / maxValue.value) * 100));
}
</script>
