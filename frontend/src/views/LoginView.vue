<template>
  <AuthShell title="学生登录" :toast="toastMessage">
    <LoginForm :loading="loading" @submit="handleLogin" @error="showToast" />
    <section v-if="isDemoMode" class="demo-login-card">
      <strong>求职作品演示</strong>
      <span>学生账号 2024000001 · 密码 123456</span>
      <button type="button" :disabled="loading" @click="enterDemo">一键进入学生端</button>
      <RouterLink to="/admin/login">切换到管理端</RouterLink>
    </section>
  </AuthShell>
</template>

<script setup lang="ts">
import { ref } from 'vue';
import { useRouter } from 'vue-router';
import { studentApi } from '../api';
import AuthShell from '../components/login/AuthShell.vue';
import LoginForm from '../components/login/LoginForm.vue';
import '../styles/student.css';

const router = useRouter();
const isDemoMode = import.meta.env.VITE_DEMO_MODE === 'true';
const loading = ref(false);
const toastMessage = ref('');
let toastTimer = 0;

function showToast(message) {
  toastMessage.value = message;
  window.clearTimeout(toastTimer);
  toastTimer = window.setTimeout(() => {
    toastMessage.value = '';
  }, 2200);
}

async function handleLogin(payload) {
  loading.value = true;

  try {
    const data = await studentApi.login(payload);
    showToast('登录成功');

    if (data.student?.mustChangePassword) {
      await router.replace('/student/change-password');
      return;
    }

    await router.replace('/student');
  } catch (error) {
    showToast(error.message || '登录失败');
  } finally {
    loading.value = false;
  }
}

function enterDemo() {
  handleLogin({ studentId: '2024000001', password: '123456' });
}
</script>

<style scoped>
.demo-login-card {
  display: grid;
  gap: 9px;
  margin-top: 14px;
  padding: 14px;
  border: 1px solid #bfdbfe;
  border-radius: 12px;
  color: #1e3a8a;
  background: #eff6ff;
  text-align: center;
}

.demo-login-card span,
.demo-login-card a {
  font-size: 13px;
}

.demo-login-card button {
  padding: 10px 12px;
  border: 0;
  border-radius: 8px;
  color: #fff;
  background: #2563eb;
  cursor: pointer;
}
</style>
