import { createRouter, createWebHistory } from 'vue-router';
import DashboardView from '@/views/DashboardView.vue';
import ImportReviewView from '@/views/ImportReviewView.vue';
import AnalysisReviewCenterView from '@/views/AnalysisReviewCenterView.vue';
import AdminCollectionReviewView from '@/views/AdminCollectionReviewView.vue';
import LoginView from '@/views/LoginView.vue';
import MatchCenterView from '@/views/MatchCenterView.vue';
import OddsCenterView from '@/views/OddsCenterView.vue';
import PrematchWorkbenchView from '@/views/PrematchWorkbenchView.vue';
import SentimentCenterView from '@/views/SentimentCenterView.vue';
import PlayerProfilesView from '@/views/PlayerProfilesView.vue';
import SystemSettingsView from '@/views/SystemSettingsView.vue';
import TeamProfilesView from '@/views/TeamProfilesView.vue';
import MoreView from '@/views/MoreView.vue';
import { useAuthStore } from '@/stores/auth';

export const router = createRouter({
  history: createWebHistory(),
  routes: [
    { path: '/login', name: 'login', component: LoginView, meta: { standalone: true } },
    { path: '/', name: 'overview', component: DashboardView },
    { path: '/workbench', name: 'prematch-workbench', component: PrematchWorkbenchView },
    { path: '/evidence', redirect: '/evidence/matches' },
    { path: '/evidence/matches', name: 'evidence-matches', component: MatchCenterView },
    { path: '/evidence/odds', name: 'evidence-odds', component: OddsCenterView },
    { path: '/evidence/sentiment', name: 'evidence-sentiment', component: SentimentCenterView },
    { path: '/evidence/teams', name: 'evidence-teams', component: TeamProfilesView },
    { path: '/evidence/players', name: 'evidence-players', component: PlayerProfilesView },
    { path: '/decisions', name: 'decisions', component: AnalysisReviewCenterView },
    { path: '/more', name: 'more', component: MoreView },
    {
      path: '/admin/import-review',
      name: 'admin-import-review',
      component: ImportReviewView,
      meta: { requiresAdmin: true },
    },
    {
      path: '/admin/collection-review',
      name: 'admin-collection-review',
      component: AdminCollectionReviewView,
      meta: { requiresAdmin: true },
    },
    {
      path: '/admin/settings',
      name: 'admin-settings',
      component: SystemSettingsView,
      meta: { requiresAdmin: true },
    },

    { path: '/settings', redirect: '/admin/settings' },
    { path: '/import-review', redirect: '/admin/import-review' },
    { path: '/matches', redirect: '/evidence/matches' },
    { path: '/odds', redirect: '/evidence/odds' },
    { path: '/sentiment', redirect: '/evidence/sentiment' },
    { path: '/analysis-review', redirect: '/decisions' },
    { path: '/prematch-workbench', redirect: '/workbench' },
    { path: '/profiles/teams', redirect: '/evidence/teams' },
    { path: '/profiles/players', redirect: '/evidence/players' },
  ],
});

router.beforeEach((to) => {
  if (!to.meta.requiresAdmin) {
    return true;
  }
  const authStore = useAuthStore();
  if (authStore.canWrite) {
    return true;
  }
  return { path: '/login', query: { redirect: to.fullPath } };
});
