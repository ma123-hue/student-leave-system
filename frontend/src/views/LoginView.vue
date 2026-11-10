<template>
  <AuthShell title="学生登录" :toast="toastMessage">
    <LoginForm :loading="loading" @submit="handleLogin" @error="showToast" />
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
</script>
