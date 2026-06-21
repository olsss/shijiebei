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
          <el-card shadow="hover">
            <h3>{{ item.title }}</h3>
            <p>{{ item.description }}</p>
          </el-card>
        </el-col>
      </el-row>
    </section>
  </main>
</template>

<script setup lang="ts">
const modules = [
  { title: 'JSON 审核中心', description: '批准后入库的核心入口' },
  { title: '球队画像中心', description: '球队特性、进球时间点和证据链' },
  { title: '球员画像中心', description: '状态、伤病、红黄牌和更衣室情况' },
  { title: '赛后复盘中心', description: 'ROI、CLV 和五层复盘' },
];
</script>

<style scoped>
.module-grid {
  margin-top: 16px;
}
</style>
