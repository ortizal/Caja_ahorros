import { expect, test } from '@playwright/test';
import { loginAs } from './helpers';

const CEDULA = `17${Date.now().toString().slice(-10)}`;

test.describe.configure({ mode: 'serial' });

test.describe('Módulo de socios', () => {
  test('crear, ver y editar un socio', async ({ page }) => {
    await loginAs(page, 'admin', 'admin123');
    await page.goto('/socios');

    await page.getByTestId('btn-nuevo-socio').click();
    await expect(page).toHaveURL(/\/socios\/nuevo/);

    await page.getByTestId('input-identificacion').fill(CEDULA);
    await page.getByTestId('input-nombres').fill('Test');
    await page.getByTestId('input-apellidos').fill('E2E');
    await page.getByTestId('input-telefono').fill('0987654321');
    await page.getByTestId('input-fecha-ingreso').fill('2026-08-01');
    await page.getByTestId('btn-guardar-socio').click();

    await expect(page).toHaveURL(/\/socios$/);
    await expect(page.getByTestId('toast-item')).toContainText('Socio creado correctamente.');

    const fila = page.getByRole('row').filter({ hasText: CEDULA });
    await fila.getByTestId('btn-acciones-menu').click();
    await fila.getByTestId('btn-ver-socio').click();
    await expect(page.getByTestId('detail-identificacion')).toHaveText(CEDULA);
    await expect(page.getByTestId('detail-estado')).toHaveText('ACTIVO');

    await page.getByTestId('btn-editar-socio').click();
    await expect(page).toHaveURL(/\/socios\/\d+\/editar/);
    await page.getByTestId('input-nombres').fill('Test Modificado');
    await page.getByTestId('btn-guardar-socio').click();

    await expect(page).toHaveURL(/\/socios$/);
    const filaEditada = page.getByRole('row').filter({ hasText: CEDULA });
    await filaEditada.getByTestId('btn-acciones-menu').click();
    await filaEditada.getByTestId('btn-ver-socio').click();
    await expect(page.getByTestId('detail-codigo')).toContainText('Test Modificado');
  });

  test('cambiar estado a SUSPENDIDO', async ({ page }) => {
    await loginAs(page, 'admin', 'admin123');
    await page.goto('/socios');

    const fila = page.getByRole('row').filter({ hasText: CEDULA });
    await fila.getByTestId('btn-acciones-menu').click();
    await fila.getByTestId('btn-ver-socio').click();

    await page.getByTestId('btn-estado-SUSPENDIDO').click();
    await expect(page.getByTestId('detail-estado')).toHaveText('SUSPENDIDO');
  });

  test('auditor no puede editar ni cambiar estado', async ({ page }) => {
    await loginAs(page, 'auditor', 'auditor123');
    await page.goto('/socios');

    const fila = page.getByRole('row').filter({ hasText: CEDULA });
    await fila.getByTestId('btn-acciones-menu').click();
    await fila.getByTestId('btn-ver-socio').click();

    await expect(page.getByTestId('btn-editar-socio')).toHaveCount(0);
    await expect(page.getByTestId('btn-estado-SUSPENDIDO')).toHaveCount(0);
  });

  test('exportar listado de socios en Excel y PDF', async ({ page }) => {
    await loginAs(page, 'admin', 'admin123');
    await page.goto('/socios');

    await expect(page.getByTestId('socios-table')).toBeVisible();

    const [xlsx] = await Promise.all([
      page.waitForEvent('download'),
      page.getByTestId('btn-exportar-socios-xlsx').click()
    ]);
    expect(xlsx.suggestedFilename()).toBe('socios.xlsx');

    const [pdf] = await Promise.all([
      page.waitForEvent('download'),
      page.getByTestId('btn-exportar-socios-pdf').click()
    ]);
    expect(pdf.suggestedFilename()).toBe('socios.pdf');
  });
});
