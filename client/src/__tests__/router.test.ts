import { describe, expect, it } from 'vitest';
import { router } from '@/router';

describe('router', () => {
  it('registers login, dashboard, and settings routes', () => {
    const paths = router.getRoutes().map((route) => route.path);
    expect(paths).toContain('/login');
    expect(paths).toContain('/');
    expect(paths).toContain('/settings');
  });
});
