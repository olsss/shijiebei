<template>
  <RouterView v-slot="{ Component }">
    <component :is="Component" v-if="isStandaloneRoute" />
    <AppShell v-else>
      <component :is="Component" />
    </AppShell>
  </RouterView>
</template>

<script setup lang="ts">
import { computed } from 'vue';
import { useRoute } from 'vue-router';
import AppShell from '@/layout/AppShell.vue';

const route = useRoute();
const isStandaloneRoute = computed(() => route.meta.standalone === true);
</script>
