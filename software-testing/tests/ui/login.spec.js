const { test, expect } = require('@playwright/test');

test.describe('学生登录页面', () => {
  test.beforeEach(async ({ page }) => {
    await page.goto('/student/login');
  });

  test('QA-UI-001 登录页核心控件展示正常', async ({ page }) => {
    await expect(page.getByRole('heading', { name: '学生登录' })).toBeVisible();
    await expect(page.getByPlaceholder('请输入10位学号')).toBeVisible();
    await expect(page.getByPlaceholder('初始密码 123456')).toBeVisible();
    await expect(page.getByRole('button', { name: '登录' })).toBeEnabled();
  });

  test('QA-UI-002 空表单提交应提示学号格式', async ({ page }) => {
    await page.getByRole('button', { name: '登录' }).click();
    await expect(page.getByText('请输入10位数字学号')).toBeVisible();
  });

  test('QA-UI-003 错误账号密码应显示认证失败', async ({ page }) => {
    await page.getByPlaceholder('请输入10位学号').fill('9999999999');
    await page.getByPlaceholder('初始密码 123456').fill('wrong-password');
    await page.getByRole('button', { name: '登录' }).click();
    await expect(page.getByText('学号或密码错误')).toBeVisible();
  });

  test('QA-UI-004 初始化账号应能从页面登录', async ({ page }) => {
    await page.getByPlaceholder('请输入10位学号').fill('2024000001');
    await page.getByPlaceholder('初始密码 123456').fill('123456');
    await page.getByRole('button', { name: '登录' }).click();
    await expect(page).toHaveURL(/\/student\/change-password$/);
  });
});
