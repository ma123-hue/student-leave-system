const { test, expect } = require('@playwright/test');

test.describe('管理端页面', () => {
  test('QA-UI-005 未登录访问管理端应跳转登录页', async ({ page }) => {
    await page.goto('/admin');
    await expect(page).toHaveURL(/\/admin\/login\?redirect=(?:%2F|\/)admin$/);
    await expect(page.getByRole('heading', { name: '管理员登录' })).toBeVisible();
  });

  test('QA-UI-006 管理端导航结构应完整', async ({ page }) => {
    await page.goto('/admin/login');
    await page.getByPlaceholder('请输入管理员账号').fill('admin001');
    await page.getByPlaceholder('请输入密码').fill('123456');
    await page.getByRole('button', { name: '登录' }).click();
    await expect(page).toHaveURL(/\/admin$/);
    await expect(page.getByRole('button', { name: '请假审批', exact: true }).last()).toBeVisible();
    await expect(page.getByRole('button', { name: '数据统计' })).toBeVisible();
    await expect(page.getByRole('button', { name: '角色切换' })).toBeVisible();
    await expect(page.getByRole('button', { name: '审批记录' })).toBeVisible();
  });

  test('QA-UI-007 管理员可以安全退出', async ({ page }) => {
    await page.goto('/admin/login');
    await page.getByPlaceholder('请输入管理员账号').fill('admin001');
    await page.getByPlaceholder('请输入密码').fill('123456');
    await page.getByRole('button', { name: '登录' }).click();
    await page.getByRole('button', { name: '退出' }).click();
    await expect(page).toHaveURL(/\/admin\/login$/);
  });
});
