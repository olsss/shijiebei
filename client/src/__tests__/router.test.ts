import { describe, expect, it } from 'vitest';
import { router } from '@/router';

describe('router', () => {
  it('registers login, dashboard, settings, and import review routes', () => {
    const paths = router.getRoutes().map((route) => route.path);
    expect(paths).toContain('/login');
    expect(paths).toContain('/');
    expect(paths).toContain('/settings');
    expect(paths).toContain('/import-review');
  });
});
