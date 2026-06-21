<template>
  <main class="page-shell">
    <section class="page-content">
      <el-row :gutter="16">
        <el-col :span="24">
          <el-card>
            <h1>数据分析工作台</h1>
            <p>当前阶段已建立 Java 系统骨架。比赛分析仍由 `skill/` 和 JSON 档案负责。</p>
          </el-card>
        </el-col>
      </el-row>

      <el-row :gutter="16" class="module-grid">
        <el-col v-for="item in modules" :key="item.title" :span="6">
          <el-card shadow="hover" class="module-card">
            <RouterLink :to="item.to" class="module-link">
              <h3>{{ item.title }}</h3>
              <p>{{ item.description }}</p>
              <span>进入模块 →</span>
            </RouterLink>
          </el-card>
        </el-col>
      </el-row>
    </section>
  </main>
</template>

<script setup lang="ts">
const modules = [
  { title: 'JSON 审核中心', description: '扫描、校验、批准 JSON 暂存入库', to: '/import-review' },
  { title: '球队画像中心', description: '球队特性、进球时间点和证据链', to: '/' },
  { title: '球员画像中心', description: '状态、伤病、红黄牌和更衣室情况', to: '/' },
  { title: '赛后复盘中心', description: 'ROI、CLV 和五层复盘', to: '/' },
];
</script>

<style scoped>
.module-grid {
  margin-top: 16px;
}
.module-card {
  min-height: 178px;
}
.module-link {
  color: inherit;
  display: block;
  min-height: 126px;
  text-decoration: none;
}
.module-link span {
  color: #2563eb;
  font-weight: 600;
}
</style>
