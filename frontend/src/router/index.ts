import { createRouter, createWebHistory } from 'vue-router';
import StudentView from '../views/StudentView.vue';
import AdminView from '../views/AdminView.vue';
import LoginView from '../views/LoginView.vue';
import PasswordChangeView from '../views/PasswordChangeView.vue';
import AdminLoginView from '../views/AdminLoginView.vue';
import { adminApi } from '../api';

const router = createRouter({
  history: createWebHistory(),
  routes: [
    { path: '/', redirect: '/student' },
    { path: '/student/login', component: LoginView },
    { path: '/student/change-password', component: PasswordChangeView },
    { path: '/student', component: StudentView },
    { path: '/admin/login', component: AdminLoginView },
    { path: '/admin', component: AdminView, meta: { requiresAdmin: true } },
    { path: '/:pathMatch(.*)*', redirect: '/student' }
  ]
});

router.beforeEach(async to => {
  if (!to.meta.requiresAdmin) return true;

  try {
    await adminApi.getSession();
    return true;
  } catch {
    return { path: '/admin/login', query: { redirect: to.fullPath } };
  }
});

export default router;
