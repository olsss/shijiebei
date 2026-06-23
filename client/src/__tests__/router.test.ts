import { describe, expect, it } from 'vitest';
import { router } from '@/router';

describe('router', () => {
  it('registers redesigned public, evidence, decision, admin, and compatibility routes', () => {
    const paths = router.getRoutes().map((route) => route.path);
    expect(paths).toContain('/login');
    expect(paths).toContain('/');
    expect(paths).toContain('/workbench');
    expect(paths).toContain('/evidence/matches');
    expect(paths).toContain('/evidence/odds');
    expect(paths).toContain('/evidence/sentiment');
    expect(paths).toContain('/evidence/teams');
    expect(paths).toContain('/evidence/players');
    expect(paths).toContain('/decisions');
    expect(paths).toContain('/admin/import-review');
    expect(paths).toContain('/admin/settings');
    expect(paths).toContain('/matches');
    expect(paths).toContain('/odds');
    expect(paths).toContain('/sentiment');
    expect(paths).toContain('/analysis-review');
    expect(paths).toContain('/prematch-workbench');
  });

  it('marks admin routes as admin-only and redirects old entry points', () => {
    const routes = router.getRoutes();

    expect(routes.find((route) => route.path === '/admin/import-review')?.meta.requiresAdmin).toBe(true);
    expect(routes.find((route) => route.path === '/admin/settings')?.meta.requiresAdmin).toBe(true);
    expect(routes.find((route) => route.path === '/import-review')?.redirect).toBe('/admin/import-review');
    expect(routes.find((route) => route.path === '/settings')?.redirect).toBe('/admin/settings');
    expect(routes.find((route) => route.path === '/matches')?.redirect).toBe('/evidence/matches');
    expect(routes.find((route) => route.path === '/prematch-workbench')?.redirect).toBe('/workbench');
  });
});

