import { createRouter, createWebHistory } from 'vue-router';
import DashboardView from '@/views/DashboardView.vue';
import ImportReviewView from '@/views/ImportReviewView.vue';
import LoginView from '@/views/LoginView.vue';
import PlayerProfilesView from '@/views/PlayerProfilesView.vue';
import SystemSettingsView from '@/views/SystemSettingsView.vue';
import TeamProfilesView from '@/views/TeamProfilesView.vue';

export const router = createRouter({
  history: createWebHistory(),
  routes: [
    { path: '/login', name: 'login', component: LoginView },
    { path: '/', name: 'dashboard', component: DashboardView },
    { path: '/settings', name: 'settings', component: SystemSettingsView },
    { path: '/import-review', name: 'import-review', component: ImportReviewView },
    { path: '/profiles/teams', name: 'team-profiles', component: TeamProfilesView },
    { path: '/profiles/players', name: 'player-profiles', component: PlayerProfilesView },
  ],
});
