<template>
  <AuthShell title="管理端登录" :toast="toastMessage">
    <form class="auth-panel" @submit.prevent="handleLogin">
      <h1>管理员登录</h1>
      <p>请使用管理员账号和密码进入管理端</p>

      <label>
        <span>管理员账号</span>
        <input
          v-model.trim="form.adminId"
          name="adminId"
          maxlength="32"
          placeholder="请输入管理员账号"
          autocomplete="username"
        />
      </label>

      <label>
        <span>密码</span>
        <input
          v-model="form.password"
          name="password"
          type="password"
          placeholder="请输入密码"
          autocomplete="current-password"
        />
      </label>

      <button class="submit-button" type="submit" :disabled="loading">
        {{ loading ? '登录中...' : '登录' }}
      </button>
    </form>
  </AuthShell>
</template>

<script setup lang="ts">
import { reactive, ref } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { adminApi } from '../api';
import AuthShell from '../components/login/AuthShell.vue';
import '../styles/student.css';

const route = useRoute();
const router = useRouter();
const loading = ref(false);
const toastMessage = ref('');
const form = reactive({ adminId: '', password: '' });
let toastTimer = 0;

function showToast(message) {
  toastMessage.value = message;
  window.clearTimeout(toastTimer);
  toastTimer = window.setTimeout(() => {
    toastMessage.value = '';
  }, 2200);
}

async function handleLogin() {
  if (!form.adminId) {
    showToast('请输入管理员账号');
    return;
  }
  if (!form.password) {
    showToast('请输入密码');
    return;
  }

  loading.value = true;
  try {
    await adminApi.login({ ...form });
    const redirect = typeof route.query.redirect === 'string' ? route.query.redirect : '/admin';
    await router.replace(redirect);
  } catch (error) {
    showToast(error.message || '登录失败');
  } finally {
    loading.value = false;
  }
}
</script>
