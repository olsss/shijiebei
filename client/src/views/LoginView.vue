<script setup lang="ts">
import { ref } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { ElMessage } from 'element-plus';
import { login } from '@/api/system';
import { useAuthStore } from '@/stores/auth';

const router = useRouter();
const route = useRoute();
const authStore = useAuthStore();
const username = ref('');
const password = ref('');
const loading = ref(false);

function safeRedirectTarget(value: unknown): string {
  const target = Array.isArray(value) ? value[0] : value;
  if (typeof target !== 'string') {
    return '/';
  }
  const trimmed = target.trim();
  if (!trimmed.startsWith('/') || trimmed.startsWith('//')) {
    return '/';
  }
  if (/^[a-z][a-z0-9+.-]*:/i.test(trimmed)) {
    return '/';
  }
  return trimmed;
}

async function submit() {
  loading.value = true;
  try {
    const response = await login(username.value, password.value);
    authStore.setAdmin(response.data, password.value);
    ElMessage.success('登录成功');
    await router.push(safeRedirectTarget(route.query.redirect));
  } catch {
    ElMessage.error('用户名或密码错误');
  } finally {
    loading.value = false;
  }
}
</script>

<template>
  <main class="login-page">
    <el-card class="login-card" shadow="always">
      <h1>世界杯竞彩个人管理系统</h1>
      <p>单管理员登录入口</p>
      <el-form label-position="top" @submit.prevent="submit">
        <el-form-item label="用户名">
          <el-input v-model="username" autocomplete="username" />
        </el-form-item>
        <el-form-item label="密码">
          <el-input v-model="password" type="password" autocomplete="current-password" show-password />
        </el-form-item>
        <el-button type="primary" :loading="loading" class="login-button" @click="submit">登录</el-button>
      </el-form>
    </el-card>
  </main>
</template>

<style scoped>
.login-page {
  display: grid;
  min-height: 100vh;
  place-items: center;
  background: linear-gradient(135deg, #0f172a, #1d4ed8);
}
.login-card {
  width: 440px;
  border-radius: 20px;
}
.login-card h1 {
  margin: 0 0 8px;
  font-size: 24px;
}
.login-card p {
  margin: 0 0 24px;
  color: #64748b;
}
.login-button {
  width: 100%;
}
</style>
