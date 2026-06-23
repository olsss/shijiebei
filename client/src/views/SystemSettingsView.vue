<script setup lang="ts">
import { onMounted, ref } from 'vue';
import { fetchSystemSettings, type SystemSettings } from '@/api/system';
import { useAuthStore } from '@/stores/auth';

const authStore = useAuthStore();
const settings = ref<SystemSettings | null>(null);
const loading = ref(false);
const error = ref('');

onMounted(async () => {
  loading.value = true;
  try {
    const response = await fetchSystemSettings(authStore.basicAuthHeader);
    settings.value = response.data;
  } catch {
    error.value = '无法读取系统设置，请确认已经登录并启动后端服务。';
  } finally {
    loading.value = false;
  }
});
</script>

<template>
  <section class="page-shell">
    <section class="page-content">
      <el-card>
        <h1>系统设置</h1>
        <el-alert v-if="error" type="error" :title="error" show-icon />
        <el-skeleton v-else-if="loading" :rows="3" animated />
        <el-descriptions v-else-if="settings" :column="1" border>
          <el-descriptions-item label="JSON 档案路径">{{ settings.archivePath }}</el-descriptions-item>
          <el-descriptions-item label="保护分析体系">{{ settings.analysisSystemProtected ? '是' : '否' }}</el-descriptions-item>
          <el-descriptions-item label="边界说明">{{ settings.boundaryDescription }}</el-descriptions-item>
        </el-descriptions>
        <el-empty v-else description="暂无系统设置" />
      </el-card>
    </section>
  </section>
</template>
