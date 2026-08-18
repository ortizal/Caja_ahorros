import { expect, test } from '@playwright/test';
import { interceptLargePage, loginAs, operarConCaja } from './helpers';

const futuro = new Date(Date.now() + 400 * 24 * 60 * 60 * 1000);
const PERIODO = `${futuro.getUTCFullYear()}-${String(futuro.getUTCMonth() + 1).padStart(2, '0')}`;

test.describe.configure({ mode: 'serial' });

test.describe('Modulo de aportaciones', () => {
  test('crear configuracion, generar periodo y pagar aportacion', async ({ page }) => {
    await interceptLargePage(page, '**/api/v1/aportaciones/config*');
    await interceptLargePage(page, '**/api/v1/aportaciones*');
    await loginAs(page, 'admin', 'admin123');
    await page.goto('/aportaciones');

    // Configuracion sembrada por el seed
    await expect(page.getByTestId('config-table')).toContainText('OBLIGATORIA');

    // Crear una configuracion extra desde el formulario ruteado
    await page.getByTestId('btn-nueva-config').click();
    await page.waitForURL(/\/aportaciones\/nuevo/);
    await page.getByTestId('input-tipo-config').selectOption('VOLUNTARIA');
    await page.getByTestId('input-valor-config').fill('10');
    await page.getByTestId('btn-crear-config').click();
    await expect(page.getByTestId('toast-item').filter({ hasText: 'Configuración de aportaciones creada' })).toBeVisible();
    await expect(page).toHaveURL(/\/aportaciones$/);
    await expect(page.getByTestId('config-table')).toContainText('VOLUNTARIA');

    // Generar aportaciones para un periodo unico
    await page.getByRole('button', { name: 'Periodos y pagos' }).click();
    await page.getByTestId('input-periodo').fill(PERIODO);
    await page.getByTestId('btn-generar').click();
    await expect(page.getByTestId('toast-item').filter({ hasText: `Aportaciones generadas para ${PERIODO}` })).toBeVisible();

    // Filtrar al periodo generado; pagar la primera aportacion pendiente si existe
    await page.getByTestId('input-filtro-periodo').fill(PERIODO);
    await expect(page.getByTestId('aportaciones-table')).toContainText(PERIODO);

    const filaPendiente = page.getByTestId('aportaciones-table').locator('tr').filter({ hasText: 'PENDIENTE' }).first();
    if (await filaPendiente.count()) {
      await filaPendiente.getByTestId('btn-acciones-menu').click();
      await filaPendiente.getByTestId('btn-pagar').click();
      await expect(page.getByTestId('pago-panel')).toBeVisible();
      await operarConCaja(page, 'btn-confirmar-pago', 'aportaciones-error');
      await expect(page.getByTestId('toast-item').filter({ hasText: 'Pago registrado' })).toBeVisible();
      await expect(page.getByTestId('aportaciones-table')).toContainText('PAGADA');
    }
  });

  test('auditor solo consulta aportaciones', async ({ page }) => {
    await loginAs(page, 'auditor', 'auditor123');
    await page.goto('/aportaciones');

    await expect(page.getByTestId('config-table')).toBeVisible();
    await expect(page.getByTestId('btn-nueva-config')).toHaveCount(0);
    await expect(page.getByTestId('generar-form')).toHaveCount(0);
  });
});
