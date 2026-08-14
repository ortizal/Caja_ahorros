import { expect, test } from '@playwright/test';
import { loginAs } from './helpers';

test.describe('Dashboard de indicadores', () => {
  test('admin ve las tarjetas KPI del resumen', async ({ page }) => {
    await loginAs(page, 'admin', 'admin123');
    await page.goto('/dashboard');

    await expect(page.getByTestId('kpi-socios')).toBeVisible();
    await expect(page.getByTestId('kpi-creditos')).toBeVisible();
    await expect(page.getByTestId('kpi-cartera')).toBeVisible();
    await expect(page.getByTestId('kpi-vencida')).toBeVisible();
    await expect(page.getByTestId('kpi-morosidad')).toBeVisible();
    await expect(page.getByTestId('kpi-cajas')).toBeVisible();
    await expect(page.getByTestId('kpi-disponible-caja')).toBeVisible();
    await expect(page.getByTestId('kpi-disponible-bancos')).toBeVisible();
  });

  test('analista de credito no ve indicadores de modulos sin permiso', async ({ page }) => {
    await loginAs(page, 'credito', 'credito123');
    await page.goto('/dashboard');

    await expect(page.getByTestId('kpi-cartera')).toBeVisible();
    await expect(page.getByTestId('kpi-socios')).toBeVisible();
    await expect(page.getByTestId('kpi-disponible-bancos')).toHaveCount(0);
  });
});
