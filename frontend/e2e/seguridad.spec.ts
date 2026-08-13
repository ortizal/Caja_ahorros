import { expect, test } from '@playwright/test';
import { loginAs } from './helpers';

const USERNAME = `e2e${Date.now().toString().slice(-6)}`;

test.describe.configure({ mode: 'serial' });

test.describe('Módulo de seguridad', () => {
  test('crear usuario, asignar permisos a un rol y consultar auditoría', async ({ page }) => {
    await loginAs(page, 'admin', 'admin123');
    await page.goto('/seguridad');

    // Crear usuario
    await page.getByTestId('input-username').fill(USERNAME);
    await page.getByTestId('input-password').fill('clave123');
    await page.getByTestId('input-nombre-completo').fill('Usuario E2E');
    await page.getByTestId('check-rol-AUDITOR').check();
    await page.getByTestId('btn-guardar-usuario').click();

    await expect(page.getByTestId('seguridad-ok')).toContainText('Usuario guardado');
    await expect(page.getByTestId('usuarios-table')).toContainText(USERNAME);

    // Asignar permisos al rol CONTADOR (asegurar CONTABILIDAD:VER/CREAR)
    await page.getByRole('button', { name: 'Roles y permisos' }).click();
    await page.getByTestId('btn-permisos-CONTADOR').click();
    await expect(page.getByTestId('permisos-editor')).toBeVisible();

    for (const id of ['check-permiso-21', 'check-permiso-22']) {
      const check = page.getByTestId(id);
      if (!(await check.isChecked())) {
        await check.check();
      }
    }
    await page.getByTestId('btn-guardar-permisos').click();
    await expect(page.getByTestId('seguridad-ok')).toContainText('CONTADOR');

    // Auditoría
    await page.getByRole('button', { name: 'Auditoría' }).click();
    await page.getByTestId('btn-consultar-auditoria').click();
    const filaAuditoria = page.getByTestId('auditoria-table').locator('tbody tr').first();
    await expect(page.getByTestId('auditoria-vacio').or(filaAuditoria)).toBeVisible();
  });

  test('auditor solo consulta seguridad', async ({ page }) => {
    await loginAs(page, 'auditor', 'auditor123');
    await page.goto('/seguridad');

    await expect(page).toHaveURL(/\/seguridad/);
    await expect(page.getByTestId('usuarios-table')).toBeVisible();
    await expect(page.getByTestId('usuario-form')).toHaveCount(0);
    await expect(page.getByTestId('btn-nuevo-usuario')).toHaveCount(0);

    await page.getByRole('button', { name: 'Roles y permisos' }).click();
    await expect(page.getByTestId('btn-permisos-CONTADOR')).toHaveCount(0);
  });
});
