<template>
  <form class="auth-panel" @submit.prevent="handleSubmit">
    <h1>学生登录</h1>
    <p>请使用学号和密码进入学生端</p>

    <label>
      <span>学号</span>
      <input
        v-model.trim="form.studentId"
        name="studentId"
        inputmode="numeric"
        maxlength="10"
        placeholder="请输入10位学号"
        autocomplete="username"
      />
    </label>

    <label>
      <span>密码</span>
      <input
        v-model="form.password"
        name="password"
        type="password"
        placeholder="初始密码 123456"
        autocomplete="current-password"
      />
    </label>

    <button class="submit-button" type="submit" :disabled="loading">
      {{ loading ? '登录中...' : '登录' }}
    </button>
  </form>
</template>

<script setup lang="ts">
import { reactive } from 'vue';

const props = defineProps({
  loading: {
    type: Boolean,
    default: false
  }
});

const emit = defineEmits(['submit', 'error']);

const form = reactive({
  studentId: '',
  password: ''
});

function handleSubmit() {
  if (!/^\d{10}$/.test(form.studentId)) {
    emit('error', '请输入10位数字学号');
    return;
  }

  if (!form.password) {
    emit('error', '请输入密码');
    return;
  }

  if (!props.loading) {
    emit('submit', { ...form });
  }
}
</script>
