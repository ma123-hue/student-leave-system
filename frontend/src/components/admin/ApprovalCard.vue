<template>
  <article class="approval-card" @click="$emit('open', row.id)">
    <div class="card-head">
      <strong>{{ row.student_name }}</strong>
      <span>{{ row.student_id }}</span>
    </div>

    <div class="card-lines">
      <p>请假类型：{{ row.leave_type }}</p>
      <p>申请时间：{{ formatDateTime(row.created_at) }}</p>
      <p>开始时间：{{ formatDateTime(row.start_time) }}</p>
      <p :class="{ danger: type === 'overtime' }">结束时间：{{ formatDateTime(row.end_time) }}</p>
      <p>请假理由：{{ row.reason }}</p>
    </div>

    <div class="card-foot">
      <div class="avatar">HAIBO</div>
      <div class="status-text">{{ statusText }}</div>
      <button
        v-if="actionText"
        class="approve-btn"
        type="button"
        @click.stop="$emit('approve', row)"
      >
        {{ actionText }}
      </button>
    </div>
  </article>
</template>

<script setup lang="ts">
import { computed } from 'vue';

const props = defineProps({
  row: {
    type: Object,
    required: true
  },
  type: {
    type: String,
    required: true
  },
  formatDateTime: {
    type: Function,
    required: true
  }
});

defineEmits(['open', 'approve']);

const needsReport = computed(() => {
  const row = props.row;
  return Number(row.step_no || 1) < Number(row.approval_level || row.total_steps || row.step_no || 1);
});

const statusText = computed(() => {
  if (props.row.approval_kind === 'extension') return '续假待审批';
  if (props.type === 'return') return '返校待审批';
  if (props.type === 'overtime') return '学生待申请返校';
  return '请假待审批';
});

const actionText = computed(() => {
  if (props.row.approval_kind === 'extension') return needsReport.value ? '同意续假并上报' : '同意续假';
  if (props.type === 'return') return '同意返校';
  if (props.type === 'leave') return needsReport.value ? '同意并上报' : '同意请假';
  return '';
});
</script>
