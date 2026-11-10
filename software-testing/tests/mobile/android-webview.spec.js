const { test, expect } = require('@playwright/test');

test.describe('Android WebView 兼容性与网络切换', () => {
  test('AND-COMP-001 小屏竖屏无横向溢出', async ({ page }) => {
    await page.setViewportSize({ width: 360, height: 640 });
    await page.goto('/student/login');

    await expect(page.locator('input[name="studentId"]')).toBeVisible();
    await expect(page.getByPlaceholder('初始密码 123456')).toBeVisible();
    await expect(page.getByRole('button', { name: '登录' })).toBeVisible();

    const dimensions = await page.evaluate(() => ({
      viewport: document.documentElement.clientWidth,
      content: document.documentElement.scrollWidth
    }));
    expect(dimensions.content).toBeLessThanOrEqual(dimensions.viewport);
  });

  test('AND-COMP-002 横竖屏切换保留路由和表单数据', async ({ page }) => {
    await page.setViewportSize({ width: 393, height: 851 });
    await page.goto('/student/login');
    await page.locator('input[name="studentId"]').fill('2024000001');

    await page.setViewportSize({ width: 851, height: 393 });
    await expect(page).toHaveURL(/\/student\/login$/);
    await expect(page.locator('input[name="studentId"]')).toHaveValue('2024000001');

    await page.setViewportSize({ width: 393, height: 851 });
    await expect(page.getByRole('button', { name: '登录' })).toBeVisible();
  });

  test('AND-NET-001 断网登录应提示网络异常且恢复后可重试', async ({ page, context }) => {
    await page.goto('/student/login');
    await page.locator('input[name="studentId"]').fill('9999999999');
    await page.getByPlaceholder('初始密码 123456').fill('wrong-password');

    await context.setOffline(true);
    await page.getByRole('button', { name: '登录' }).click();
    await expect(page.getByText('网络连接异常，请检查网络后重试')).toBeVisible();

    await context.setOffline(false);
    await page.getByRole('button', { name: '登录' }).click();
    await expect(page.getByText('学号或密码错误')).toBeVisible();
  });
});
