const { test, expect } = require('@playwright/test');

test.describe('管理员认证接口', () => {
  test('QA-API-009 错误管理员密码应登录失败', async ({ request }) => {
    const response = await request.post('/api/admin/auth/login', {
      data: { adminId: 'admin001', password: 'wrong-password' }
    });
    const body = await response.json();
    expect(body.code).toBe(401);
    expect(body.msg).toBe('管理员账号或密码错误');
  });

  test('QA-API-010 管理员登录后可读取会话', async ({ request }) => {
    const login = await request.post('/api/admin/auth/login', {
      data: { adminId: 'admin001', password: '123456' }
    });
    expect((await login.json()).code).toBe(200);

    const session = await request.get('/api/admin/auth/session');
    const body = await session.json();
    expect(body.code).toBe(200);
    expect(body.data.admin.adminId).toBe('admin001');
    expect(body.data.admin.role).toBe('辅导员');
  });

  test('QA-API-011 管理员退出后会话应失效', async ({ request }) => {
    await request.post('/api/admin/auth/login', {
      data: { adminId: 'admin001', password: '123456' }
    });
    expect((await (await request.post('/api/admin/auth/logout')).json()).code).toBe(200);
    expect((await (await request.get('/api/admin/auth/session')).json()).code).toBe(401);
  });
});
