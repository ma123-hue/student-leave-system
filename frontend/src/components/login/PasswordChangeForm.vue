<template>
  <form class="auth-panel" @submit.prevent="handleSubmit">
    <h1>修改初始密码</h1>
    <p>首次登录后需要设置自己的新密码</p>

    <label>
      <span>当前密码</span>
      <input
        v-model="form.currentPassword"
        name="currentPassword"
        type="password"
        placeholder="请输入当前密码"
        autocomplete="current-password"
      />
    </label>

    <label>
      <span>新密码</span>
      <input
        v-model="form.newPassword"
        name="newPassword"
        type="password"
        placeholder="6到32位，不能为123456"
        autocomplete="new-password"
      />
    </label>

    <label>
      <span>确认新密码</span>
      <input
        v-model="form.confirmPassword"
        name="confirmPassword"
        type="password"
        placeholder="请再次输入新密码"
        autocomplete="new-password"
      />
    </label>

    <button class="submit-button" type="submit" :disabled="loading">
      {{ loading ? '保存中...' : '保存并进入系统' }}
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
  currentPassword: '',
  newPassword: '',
  confirmPassword: ''
});

function handleSubmit() {
  if (!form.currentPassword) {
    emit('error', '请输入当前密码');
    return;
  }

  if (!form.newPassword) {
    emit('error', '请输入新密码');
    return;
  }

  if (form.newPassword !== form.confirmPassword) {
    emit('error', '两次输入的新密码不一致');
    return;
  }

  if (!props.loading) {
    emit('submit', { ...form });
  }
}
</script>
