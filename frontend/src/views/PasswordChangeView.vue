<template>
  <AuthShell title="修改密码" :toast="toastMessage">
    <PasswordChangeForm :loading="loading" @submit="handleChangePassword" @error="showToast" />
  </AuthShell>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue';
import { useRouter } from 'vue-router';
import { studentApi } from '../api';
import AuthShell from '../components/login/AuthShell.vue';
import PasswordChangeForm from '../components/login/PasswordChangeForm.vue';
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

async function handleChangePassword(payload) {
  loading.value = true;

  try {
    await studentApi.changePassword(payload);
    showToast('密码修改成功');
    await router.replace('/student');
  } catch (error) {
    showToast(error.message || '密码修改失败');
  } finally {
    loading.value = false;
  }
}

onMounted(async () => {
  try {
    await studentApi.getSession();
  } catch {
    await router.replace('/student/login');
  }
});
</script>
