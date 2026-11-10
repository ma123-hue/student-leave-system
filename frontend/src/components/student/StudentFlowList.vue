<template>
  <div class="flow-list">
    <template v-for="(item, index) in people" :key="`${item.role}-${index}`">
      <div class="flow-item">
        <span class="flow-dot"></span>
        <div class="flow-main">
          <strong>{{ item.role }}</strong>
          <span>{{ item.name }}</span>
        </div>
        <div class="flow-avatar">{{ item.code }}</div>
      </div>
      <div v-if="index < people.length - 1" class="flow-or">
        <span>或签 一人同意即可进入下一节点</span>
      </div>
    </template>
  </div>
</template>

<script setup lang="ts">
defineProps({
  people: {
    type: Array,
    default: () => []
  }
});
</script>
