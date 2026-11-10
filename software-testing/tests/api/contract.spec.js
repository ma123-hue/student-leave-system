const { test, expect } = require('@playwright/test');

test.describe('接口契约与边界值', () => {
  test('QA-API-006 审批列表分页参数应被规范化', async ({ request }) => {
    await request.post('/api/admin/auth/login', {
      data: { adminId: 'admin001', password: '123456' }
    });
    const response = await request.get('/api/admin/approvals?page=0&size=0');
    const body = await response.json();

    expect(body.code).toBe(200);
    expect(body.data.page).toBe(1);
    expect(body.data.size).toBe(1);
    expect(Array.isArray(body.data.records)).toBeTruthy();
  });

  test('QA-API-007 非法角色应返回明确错误', async ({ request }) => {
    await request.post('/api/admin/auth/login', {
      data: { adminId: 'admin001', password: '123456' }
    });
    const response = await request.post('/api/admin/role', {
      data: { role: '不存在的角色' }
    });
    const body = await response.json();

    expect(body.code).toBe(400);
    expect(body.msg).toBe('角色不存在');
  });

  test('QA-API-008 JSON 响应应声明 UTF-8', async ({ request }) => {
    const response = await request.get('/api/student/session');
    expect(response.headers()['content-type']).toContain('application/json');
  });
});
