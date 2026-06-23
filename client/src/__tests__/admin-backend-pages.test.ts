import { createPinia, setActivePinia } from 'pinia';
import { flushPromises, mount } from '@vue/test-utils';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import ImportReviewView from '@/views/ImportReviewView.vue';
import SystemSettingsView from '@/views/SystemSettingsView.vue';
import AdminCollectionReviewView from '@/views/AdminCollectionReviewView.vue';
import importReviewSource from '@/views/ImportReviewView.vue?raw';
import systemSettingsSource from '@/views/SystemSettingsView.vue?raw';
import collectionReviewSource from '@/views/AdminCollectionReviewView.vue?raw';
import { useAuthStore } from '@/stores/auth';
import {
  approveImportItem,
  batchApproveImportItems,
  getImportItem,
  listImportItems,
  rejectImportItem,
  scanArchive,
  type ImportItemResponse,
} from '@/api/importReview';
import { importCoreDataItem, listCoreDataMappings } from '@/api/coreData';
import { fetchSystemSettings } from '@/api/system';
import {
  approveCollectionItem,
  listCollectionItems,
  rejectCollectionItem,
  type CollectionItem,
} from '@/api/profiles';

vi.mock('@/api/importReview', () => ({
  scanArchive: vi.fn(),
  listImportItems: vi.fn(),
  getImportItem: vi.fn(),
  approveImportItem: vi.fn(),
  batchApproveImportItems: vi.fn(),
  rejectImportItem: vi.fn(),
}));

vi.mock('@/api/coreData', () => ({
  importCoreDataItem: vi.fn(),
  listCoreDataMappings: vi.fn(),
}));

vi.mock('@/api/system', () => ({
  fetchSystemSettings: vi.fn(),
}));

vi.mock('@/api/profiles', () => ({
  listCollectionItems: vi.fn(),
  approveCollectionItem: vi.fn(),
  rejectCollectionItem: vi.fn(),
}));

const apiOk = <T,>(data: T) => ({ success: true, data, message: '', timestamp: '' });

const pendingImport: ImportItemResponse = {
  id: 11,
  jobId: 1,
  itemType: 'ANALYSIS',
  status: 'PENDING_REVIEW',
  relativePath: 'analysis/france-brazil.json',
  sha256: 'abc',
  summaryTitle: '法国巴西分析',
  validJson: true,
  validationMessage: 'ok',
};

const approvedImport: ImportItemResponse = {
  id: 12,
  jobId: 1,
  itemType: 'ODDS',
  status: 'APPROVED',
  relativePath: 'odds/france-brazil.json',
  sha256: 'def',
  summaryTitle: '法国巴西赔率',
  validJson: true,
  validationMessage: 'ok',
};

const collectionItem: CollectionItem = {
  id: 21,
  jobId: 2,
  entityType: 'TEAM',
  entityKey: 'france',
  factType: 'STYLE',
  title: '边路速度优势',
  summary: '边路推进效率高',
  sourceName: 'Scout',
  status: 'PENDING_REVIEW',
};

function mountWithRouterStub(component: unknown) {
  return mount(component, {
    global: {
      stubs: {
        RouterLink: {
          props: ['to'],
          template: '<a :href="typeof to === `string` ? to : to.path"><slot /></a>',
        },
      },
    },
  });
}

async function mountAndFlush(component: unknown) {
  const wrapper = mountWithRouterStub(component);
  await flushPromises();
  await flushPromises();
  return wrapper;
}

function setBasicAdmin() {
  useAuthStore().setAdmin({ username: 'operator', displayName: 'Operator', authType: 'BASIC' }, 'secret');
}

describe('admin backend pages', () => {
  beforeEach(() => {
    setActivePinia(createPinia());
    vi.mocked(listImportItems).mockReset().mockResolvedValue(apiOk([pendingImport, approvedImport]));
    vi.mocked(getImportItem).mockReset().mockResolvedValue(apiOk({ item: pendingImport, rawJson: '{"ok":true}' }));
    vi.mocked(scanArchive).mockReset().mockResolvedValue(apiOk({
      id: 3,
      archivePath: '../skill/archive',
      status: 'DONE',
      totalItems: 2,
      validItems: 2,
      invalidItems: 0,
      message: 'ok',
    }));
    vi.mocked(approveImportItem).mockReset().mockResolvedValue(apiOk(pendingImport));
    vi.mocked(batchApproveImportItems).mockReset().mockResolvedValue(apiOk([pendingImport]));
    vi.mocked(rejectImportItem).mockReset().mockResolvedValue(apiOk(pendingImport));
    vi.mocked(listCoreDataMappings).mockReset().mockResolvedValue(apiOk([{ id: 1, importItemId: 12, targetType: 'ODDS', targetId: 99, mappingStatus: 'IMPORTED', message: 'ok' }]));
    vi.mocked(importCoreDataItem).mockReset().mockResolvedValue(apiOk({ importItemId: 12, status: 'IMPORTED', message: '导入成功', mappings: [] }));
    vi.mocked(fetchSystemSettings).mockReset().mockResolvedValue(apiOk({
      archivePath: '../skill/archive',
      analysisSystemProtected: true,
      boundaryDescription: 'Java 只负责归档与审核',
    }));
    vi.mocked(listCollectionItems).mockReset().mockResolvedValue(apiOk([collectionItem]));
    vi.mocked(approveCollectionItem).mockReset().mockResolvedValue(apiOk({ id: 21, status: 'APPROVED', targetType: 'TEAM', targetId: 7, message: 'ok' }));
    vi.mocked(rejectCollectionItem).mockReset().mockResolvedValue(apiOk({ id: 21, status: 'REJECTED', message: 'ok' }));
  });

  it('keeps ImportReview read/write operations behind Basic admin login', async () => {
    const anonymous = await mountAndFlush(ImportReviewView);
    expect(anonymous.text()).toContain('需要管理员登录');
    expect(listImportItems).not.toHaveBeenCalled();

    setBasicAdmin();
    const wrapper = await mountAndFlush(ImportReviewView);
    expect(listImportItems).toHaveBeenCalledTimes(1);
    expect(wrapper.text()).toContain('JSON 审核中心');
    expect(wrapper.text()).toContain('扫描入审核池');
    expect(wrapper.text()).toContain('批量批准');
    expect(wrapper.text()).toContain('批准入库');
    expect(wrapper.text()).toContain('驳回');
    expect(wrapper.text()).toContain('导入正式库');
    expect(wrapper.text()).toContain('法国巴西分析');
  });

  it('keeps SystemSettings behind Basic admin login', async () => {
    const anonymous = await mountAndFlush(SystemSettingsView);
    expect(anonymous.text()).toContain('需要管理员登录');
    expect(fetchSystemSettings).not.toHaveBeenCalled();

    setBasicAdmin();
    const wrapper = await mountAndFlush(SystemSettingsView);
    expect(fetchSystemSettings).toHaveBeenCalledTimes(1);
    expect(wrapper.text()).toContain('系统设置');
    expect(wrapper.text()).toContain('../skill/archive');
    expect(wrapper.text()).toContain('Java 只负责归档与审核');
  });

  it('adds an admin collection review surface for profile collection approvals', async () => {
    const anonymous = await mountAndFlush(AdminCollectionReviewView);
    expect(anonymous.text()).toContain('需要管理员登录');
    expect(listCollectionItems).not.toHaveBeenCalled();

    setBasicAdmin();
    const wrapper = await mountAndFlush(AdminCollectionReviewView);
    expect(listCollectionItems).toHaveBeenCalledWith(expect.stringMatching(/^Basic /), 'PENDING_REVIEW');
    expect(wrapper.text()).toContain('采集审核中心');
    expect(wrapper.text()).toContain('边路速度优势');
    expect(wrapper.text()).toContain('批准');
    expect(wrapper.text()).toContain('驳回');
  });

  it('declares H5 card layouts for admin backend pages', () => {
    for (const source of [importReviewSource, systemSettingsSource, collectionReviewSource]) {
      expect(source).not.toContain('<el-table');
      expect(source).toContain('@media (max-width: 640px)');
      expect(source).toContain('grid-template-columns: 1fr');
      expect(source).toContain('min-width: 0');
    }
  });
});
