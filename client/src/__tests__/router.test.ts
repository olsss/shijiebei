import { describe, expect, it } from 'vitest';
import { router } from '@/router';

describe('router', () => {
  it('registers login, dashboard, settings, import review, profile, and match routes', () => {
    const paths = router.getRoutes().map((route) => route.path);
    expect(paths).toContain('/login');
    expect(paths).toContain('/');
    expect(paths).toContain('/settings');
    expect(paths).toContain('/import-review');
    expect(paths).toContain('/profiles/teams');
    expect(paths).toContain('/profiles/players');
    expect(paths).toContain('/matches');
    expect(paths).toContain('/odds');
  });
});

