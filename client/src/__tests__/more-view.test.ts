import { mount } from '@vue/test-utils';
import { describe, expect, it } from 'vitest';
import MoreView from '@/views/MoreView.vue';

const routerLinkStub = {
  props: ['to'],
  template: '<a class="router-link-stub" :href="typeof to === `string` ? to : to.path"><slot /></a>',
};

describe('MoreView', () => {
  it('acts as the H5 more gateway before entering admin pages', () => {
    const wrapper = mount(MoreView, {
      global: { stubs: { RouterLink: routerLinkStub } },
    });

    expect(wrapper.find('a[href="/evidence/teams"]').exists()).toBe(true);
    expect(wrapper.find('a[href="/evidence/players"]').exists()).toBe(true);
    expect(wrapper.find('a[href="/admin/import-review"]').exists()).toBe(true);
    expect(wrapper.text()).toContain('Basic');
  });
});
