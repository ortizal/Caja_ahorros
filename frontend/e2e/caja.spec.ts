import { expect, test } from '@playwright/test';
import { loginAs } from './helpers';

test.describe.configure({ mode: 'serial' });

test.describe('Módulo de caja', () => {
  test('abrir caja, registrar movimiento, arqueo y cierre', async ({ page }) => {
    await loginAs(page, 'admin', 'admin123');
    await page.goto('/caja');

    // Si ya hay una caja abierta, cerrarla para empezar desde cero.
    const cerrar = page.getByTestId('btn-cerrar-caja');
    if (await cerrar.waitFor({ state: 'visible', timeout: 5000 }).then(() => true).catch(() => false)) {
      await cerrar.click();
    }

    // Abrir caja desde el formulario ruteado
    await page.getByTestId('btn-abrir-caja').click();
    await expect(page).toHaveURL(/\/caja\/nuevo/);
    await page.getByTestId('input-saldo-inicial').fill('300');
    await page.getByTestId('btn-confirmar-apertura').click();

    await expect(page).toHaveURL(/\/caja$/);
    await expect(page.getByTestId('toast-item').filter({ hasText: 'abierta' })).toContainText('Caja');
    await expect(page.getByTestId('saldo-actual')).toHaveText('300.00');

    // Registrar una aportación
    await page.getByTestId('input-tipo-movimiento').selectOption('APORTACION');
    await page.getByTestId('input-monto-movimiento').fill('100');
    await page.getByTestId('btn-registrar-movimiento').click();

    await expect(page.getByTestId('saldo-actual')).toHaveText('400.00');
    await expect(page.getByTestId('movimientos-table')).toContainText('APORTACION');

    // Arqueo sin diferencia
    await page.getByTestId('input-saldo-fisico').fill('400');
    await page.getByTestId('btn-arqueo').click();
    await expect(page.getByTestId('arqueo-resultado')).toContainText('0.00');

    // Exportar reporte de caja en Excel y PDF
    const [xlsx] = await Promise.all([
      page.waitForEvent('download'),
      page.getByTestId('btn-exportar-caja-xlsx').click()
    ]);
    expect(xlsx.suggestedFilename()).toBe('caja.xlsx');

    const [pdf] = await Promise.all([
      page.waitForEvent('download'),
      page.getByTestId('btn-exportar-caja-pdf').click()
    ]);
    expect(pdf.suggestedFilename()).toBe('caja.pdf');

    // Cerrar caja
    await page.getByTestId('btn-cerrar-caja').click();
    await expect(page.getByTestId('cajas-table')).toContainText('CERRADA');
    await expect(page.getByTestId('btn-abrir-caja')).toBeVisible();
  });

  test('auditor no puede abrir caja', async ({ page }) => {
    await loginAs(page, 'auditor', 'auditor123');
    await page.goto('/caja');

    await expect(page.getByTestId('btn-abrir-caja')).toHaveCount(0);
  });
});
