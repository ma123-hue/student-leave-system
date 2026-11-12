<template>
  <aside v-if="isDemoMode" class="demo-banner">
    <div>
      <strong>在线演示</strong>
      <span>使用模拟数据，操作仅保存在当前浏览器</span>
    </div>
    <nav aria-label="演示端切换">
      <RouterLink to="/student">学生端</RouterLink>
      <RouterLink to="/admin">管理端</RouterLink>
      <button type="button" @click="resetDemo">重置数据</button>
    </nav>
  </aside>
  <AppShell>
    <RouterView />
  </AppShell>
</template>

<script setup lang="ts">
import AppShell from './components/AppShell.vue';

const isDemoMode = import.meta.env.VITE_DEMO_MODE === 'true';

function resetDemo() {
  localStorage.removeItem('student-leave-demo-state-v1');
  window.location.reload();
}
</script>

<style scoped>
.demo-banner {
  position: sticky;
  top: 0;
  z-index: 1000;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 24px;
  min-height: 48px;
  padding: 8px 18px;
  color: #eff6ff;
  background: linear-gradient(110deg, #183b70, #2563eb);
  box-shadow: 0 4px 18px rgb(15 23 42 / 16%);
}

.demo-banner div,
.demo-banner nav {
  display: flex;
  align-items: center;
  gap: 10px;
}

.demo-banner strong {
  padding: 3px 8px;
  color: #1e3a8a;
  background: #dbeafe;
  border-radius: 999px;
  font-size: 12px;
}

.demo-banner span {
  font-size: 13px;
}

.demo-banner a,
.demo-banner button {
  padding: 5px 10px;
  border: 1px solid rgb(255 255 255 / 45%);
  border-radius: 7px;
  color: #fff;
  background: rgb(255 255 255 / 10%);
  cursor: pointer;
  font-size: 13px;
}

.demo-banner a.router-link-active {
  color: #1d4ed8;
  background: #fff;
}

@media (max-width: 720px) {
  .demo-banner {
    align-items: stretch;
    flex-direction: column;
    gap: 7px;
  }

  .demo-banner div {
    justify-content: center;
  }

  .demo-banner span {
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
  }

  .demo-banner nav {
    justify-content: center;
  }
}
</style>
