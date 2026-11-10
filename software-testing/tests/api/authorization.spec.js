const { test, expect } = require('@playwright/test');

test.describe('管理端权限边界', () => {
  test('QA-SEC-001 未登录用户不能读取审批列表', async ({ request }) => {
    const response = await request.get('/api/admin/approvals');
    const body = await response.json();
    expect(response.status()).toBe(401);
    expect(body.code).toBe(401);
  });

  test('QA-SEC-002 未登录用户不能读取管理统计', async ({ request }) => {
    const response = await request.get('/api/admin/statistics');
    const body = await response.json();
    expect(response.status()).toBe(401);
    expect(body.code).toBe(401);
  });

  test('QA-SEC-003 未登录用户不能修改管理员角色', async ({ request }) => {
    // 使用非法角色，避免测试对数据库产生写入；身份校验应先于参数校验。
    const response = await request.post('/api/admin/role', {
      data: { role: '__unauthorized_test__' }
    });
    const body = await response.json();
    expect(response.status()).toBe(401);
    expect(body.code).toBe(401);
  });

  test('QA-SEC-004 学生会话不能获得管理员权限', async ({ request }) => {
    await request.post('/api/student/login', {
      data: { studentId: '2024000001', password: '123456' }
    });
    const response = await request.get('/api/admin/approvals');
    const body = await response.json();
    expect(response.status()).toBe(403);
    expect(body.code).toBe(403);
  });

  test('QA-SEC-005 未登录不能通过 studentId 查询他人记录', async ({ request }) => {
    const response = await request.get('/api/leave-records?studentId=2024000001');
    const body = await response.json();
    expect(body.code).toBe(401);
    expect(body.msg).toContain('请先登录');
  });

  test('QA-SEC-006 未登录不能通过请求体代替学生会话', async ({ request }) => {
    const response = await request.post('/api/leaves', {
      data: {
        studentId: '2024000001',
        startTime: '2026-11-01 08:00:00',
        endTime: '2026-11-01 18:00:00',
        reason: 'security-test'
      }
    });
    const body = await response.json();
    expect(body.code).toBe(401);
    expect(body.msg).toContain('请先登录');
  });
});
