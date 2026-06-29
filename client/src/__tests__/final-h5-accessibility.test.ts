import { describe, expect, it } from 'vitest';
import { router } from '@/router';
import appShellSource from '@/layout/AppShell.vue?raw';
import mobileTabbarSource from '@/layout/MobileTabbar.vue?raw';
import mainCssSource from '@/styles/main.css?raw';
import dashboardSource from '@/views/DashboardView.vue?raw';
import workbenchSource from '@/views/PrematchWorkbenchView.vue?raw';
import matchesSource from '@/views/MatchCenterView.vue?raw';
import oddsSource from '@/views/OddsCenterView.vue?raw';
import sentimentSource from '@/views/SentimentCenterView.vue?raw';
import teamsSource from '@/views/TeamProfilesView.vue?raw';
import playersSource from '@/views/PlayerProfilesView.vue?raw';
import decisionsSource from '@/views/AnalysisReviewCenterView.vue?raw';
import importReviewSource from '@/views/ImportReviewView.vue?raw';
import settingsSource from '@/views/SystemSettingsView.vue?raw';
import collectionReviewSource from '@/views/AdminCollectionReviewView.vue?raw';
import moreSource from '@/views/MoreView.vue?raw';

const responsiveSources = [
  dashboardSource,
  workbenchSource,
  matchesSource,
  oddsSource,
  sentimentSource,
  teamsSource,
  playersSource,
  decisionsSource,
  importReviewSource,
  settingsSource,
  collectionReviewSource,
  moreSource,
];

describe('final H5 and accessibility acceptance', () => {
  it('keeps app shell landmarks, skip link, aria-current, and visible focus styles', () => {
    expect(appShellSource).toContain('skip-link');
    expect(appShellSource).toContain('role="navigation"');
    expect(appShellSource).toContain('aria-label="主导航"');
    expect(appShellSource).toContain('aria-current');
    expect(appShellSource).toContain('<main');
    expect(`${appShellSource}\n${mobileTabbarSource}\n${moreSource}`).toContain(':focus-visible');
    expect(`${appShellSource}\n${mobileTabbarSource}\n${mainCssSource}`).toContain('--wc-focus-ring');
  });

  it('keeps mobile tabbar to the five documented H5 sections', () => {
    expect(mobileTabbarSource).toContain('overview');
    expect(mobileTabbarSource).toContain('workbench');
    expect(mobileTabbarSource).toContain('evidence');
    expect(mobileTabbarSource).toContain('decisions');
    expect(mobileTabbarSource).toContain('more');
    expect(mobileTabbarSource).toContain('aria-label="移动端主导航"');
    expect(mobileTabbarSource).toContain('aria-current');
  });

  it('does not leave a navigation gap at the 768 tablet breakpoint', () => {
    expect(appShellSource).toContain('@media (max-width: 1023px)');
    expect(appShellSource).toMatch(/@media \(max-width: 1023px\)[\s\S]*\.app-shell__sidebar[\s\S]*display:\s*none/);
    expect(mobileTabbarSource).toMatch(/@media \(max-width: 1023px\)[\s\S]*\.mobile-tabbar[\s\S]*display:\s*flex/);
    expect(appShellSource).toMatch(/@media \(max-width: 1023px\)[\s\S]*var\(--mobile-tabbar-height\)/);
  });

  it('ensures all redesigned pages declare small-screen H5 layouts without large fixed min widths', () => {
    for (const source of responsiveSources) {
      expect(source).toContain('@media');
      expect(source).toMatch(/grid-template-columns:\s*(?:1fr|repeat\(2, minmax\(0, 1fr\)\))/);
      expect(source).toContain('min-width: 0');
      expect(source).not.toMatch(/min-width:\s*(?:[4-9]\d{2,}|\d{4,})px/);
    }
    expect(mainCssSource).not.toContain('min-width: 1280px');
  });

  it('keeps all admin routes protected while public routes stay readable', () => {
    const routes = router.getRoutes();
    const protectedPaths = ['/admin/import-review', '/admin/collection-review', '/admin/settings'];
    const publicPaths = ['/', '/workbench', '/evidence/matches', '/evidence/odds', '/evidence/sentiment', '/evidence/teams', '/evidence/players', '/decisions', '/more'];

    for (const path of protectedPaths) {
      expect(routes.find((route) => route.path === path)?.meta.requiresAdmin).toBe(true);
    }
    for (const path of publicPaths) {
      expect(routes.find((route) => route.path === path)?.meta.requiresAdmin).not.toBe(true);
    }
  });
});
