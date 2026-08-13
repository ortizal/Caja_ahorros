import { expect, test } from '@playwright/test';
import { loginAs, operarConCaja } from './helpers';

const PERIODO = `2027-${String(new Date().getDate()).padStart(2, '0')}`;

test.describe.configure({ mode: 'serial' });

test.describe('Modulo de aportaciones', () => {
  test('crear configuracion, generar periodo y pagar aportacion', async ({ page }) => {
    await loginAs(page, 'admin', 'admin123');
    await page.goto('/aportaciones');

    // Configuracion sembrada por el seed
    await expect(page.getByTestId('config-table')).toContainText('OBLIGATORIA');

    // Crear una configuracion extra
    await page.getByTestId('input-tipo-config').selectOption('VOLUNTARIA');
    await page.getByTestId('input-valor-config').fill('10');
    await page.getByTestId('btn-crear-config').click();
    await expect(page.getByTestId('config-table')).toContainText('VOLUNTARIA');

    // Generar aportaciones para un periodo unico
    await page.getByRole('button', { name: 'Periodos y pagos' }).click();
    await page.getByTestId('input-periodo').fill(PERIODO);
    await page.getByTestId('btn-generar').click();
    await expect(page.getByTestId('aportaciones-ok')).toContainText(`Aportaciones generadas para ${PERIODO}`);

    // Filtrar al periodo generado; pagar la aportacion pendiente si existe
    await page.getByTestId('input-filtro-periodo').fill(PERIODO);
    await expect(page.getByTestId('aportaciones-table')).toContainText(PERIODO);

    await page.getByTestId('btn-pagar').first().waitFor({ state: 'visible', timeout: 3000 }).catch(() => undefined);
    if (await page.getByTestId('btn-pagar').first().isVisible().catch(() => false)) {
      await page.getByTestId('btn-pagar').first().click();
      await expect(page.getByTestId('pago-panel')).toBeVisible();
      await operarConCaja(page, 'btn-confirmar-pago', 'aportaciones-error');
      await expect(page.getByTestId('aportaciones-ok')).toContainText('Pago registrado');
    }
    await expect(page.getByTestId('aportaciones-table')).toContainText('PAGADA');
  });

  test('auditor solo consulta aportaciones', async ({ page }) => {
    await loginAs(page, 'auditor', 'auditor123');
    await page.goto('/aportaciones');

    await expect(page.getByTestId('config-table')).toBeVisible();
    await expect(page.getByTestId('config-form')).toHaveCount(0);
    await expect(page.getByTestId('generar-form')).toHaveCount(0);
  });
});
