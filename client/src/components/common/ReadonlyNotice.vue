<template>
  <section class="readonly-notice" :class="{ 'readonly-notice--admin': canWrite }" aria-live="polite">
    <div class="readonly-notice__signal" aria-hidden="true"></div>
    <div>
      <strong>{{ title }}</strong>
      <p>{{ message }}</p>
    </div>
  </section>
</template>

<script setup lang="ts">
import { computed } from 'vue';

const props = defineProps<{
  canWrite: boolean;
}>();

const title = computed(() => (props.canWrite ? '管理员模式' : '公开只读'));
const message = computed(() =>
  props.canWrite
    ? '已解锁审核、批准入库和管理操作；敏感数据仍仅在管理员页面展示。'
    : '未登录也可查看脱敏数据；审核、保存、入库和删除等写操作仅管理员可用。',
);
</script>

<style scoped>
.readonly-notice {
  align-items: flex-start;
  background: rgba(147, 197, 253, 0.1);
  border: 1px solid rgba(147, 197, 253, 0.28);
  border-radius: var(--wc-radius-md);
  color: var(--wc-text-muted);
  display: flex;
  gap: 12px;
  padding: 12px 14px;
}

.readonly-notice--admin {
  background: rgba(217, 119, 6, 0.14);
  border-color: rgba(217, 119, 6, 0.36);
}

.readonly-notice__signal {
  background: var(--wc-primary);
  border-radius: 999px;
  box-shadow: 0 0 20px rgba(147, 197, 253, 0.7);
  flex: 0 0 auto;
  height: 10px;
  margin-top: 6px;
  width: 10px;
}

.readonly-notice > div:last-child {
  min-width: 0;
}

.readonly-notice--admin .readonly-notice__signal {
  background: var(--wc-accent);
  box-shadow: 0 0 20px rgba(217, 119, 6, 0.7);
}

.readonly-notice strong {
  color: var(--wc-text);
  display: block;
  font-size: 14px;
  line-height: 1.35;
}

.readonly-notice p {
  font-size: 13px;
  line-height: 1.55;
  margin: 3px 0 0;
  overflow-wrap: anywhere;
}
</style>
