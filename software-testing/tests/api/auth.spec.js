const { test, expect } = require('@playwright/test');

test.describe('认证接口', () => {
  test('QA-API-001 未登录查询会话应被拒绝', async ({ request }) => {
    const response = await request.get('/api/student/session');
    const body = await response.json();

    expect(response.ok()).toBeTruthy();
    expect(body.code).toBe(401);
    expect(body.msg).toContain('请先登录');
  });

  test('QA-API-002 空账号密码应返回参数错误', async ({ request }) => {
    const response = await request.post('/api/student/login', {
      data: { studentId: '', password: '' }
    });
    const body = await response.json();

    expect(body.code).toBe(400);
    expect(body.msg).toContain('学号和密码');
  });

  test('QA-API-003 错误密码应返回认证失败', async ({ request }) => {
    const response = await request.post('/api/student/login', {
      data: { studentId: '2024000001', password: 'wrong-password' }
    });
    const body = await response.json();

    expect(body.code).toBe(401);
    expect(body.msg).toBe('学号或密码错误');
  });

  test('QA-API-004 初始账号登录后应进入强制改密流程', async ({ request }) => {
    const response = await request.post('/api/student/login', {
      data: { studentId: '2024000001', password: '123456' }
    });
    const body = await response.json();

    expect(body.code).toBe(403);
    expect(body.msg).toContain('修改密码');
    expect(body.data.student.studentId).toBe('2024000001');
    expect(body.data.student.mustChangePassword).toBe(true);
  });

  test('QA-API-005 登出接口应成功并使会话失效', async ({ request }) => {
    await request.post('/api/student/login', {
      data: { studentId: '2024000001', password: '123456' }
    });

    const logout = await request.post('/api/student/logout');
    expect((await logout.json()).code).toBe(200);

    const session = await request.get('/api/student/session');
    expect((await session.json()).code).toBe(401);
  });
});
