import { expect, test } from '@playwright/test';
import { loginAs } from './helpers';

test.describe('Autenticación', () => {
  test('login con credenciales correctas muestra el dashboard', async ({ page }) => {
    await page.goto('/');
    await expect(page).toHaveURL(/\/login/);

    await page.getByTestId('username').fill('admin');
    await page.getByTestId('password').fill('admin123');
    await page.getByTestId('login-submit').click();

    await expect(page).toHaveURL(/\/dashboard/);
    await expect(page.getByRole('heading', { name: 'Dashboard' })).toBeVisible();
    await expect(page.getByRole('button', { name: 'Salir' })).toBeVisible();
  });

  test('login con password incorrecta muestra error', async ({ page }) => {
    await page.goto('/login');

    await page.getByTestId('username').fill('admin');
    await page.getByTestId('password').fill('clave-incorrecta');
    await page.getByTestId('login-submit').click();

    await expect(page.getByTestId('login-error')).toBeVisible();
    await expect(page).toHaveURL(/\/login/);
  });

  test('el sidebar se arma según permisos del rol', async ({ page }) => {
    await loginAs(page, 'auditor', 'auditor123');

    await expect(page).toHaveURL(/\/dashboard/);
    // AUDITOR tiene todos los permisos :VER, por lo que ve los 6 módulos en el sidebar
    for (const modulo of ['Dashboard', 'Socios', 'Caja', 'Bancos', 'Contabilidad', 'Seguridad']) {
      await expect(page.getByRole('link', { name: modulo, exact: true })).toBeVisible();
    }
  });

  test('un usuario sin SOCIOS:CREAR no ve el botón de nuevo socio', async ({ page }) => {
    await loginAs(page, 'auditor', 'auditor123');
    await page.goto('/socios');

    await expect(page.getByTestId('socios-table')).toBeVisible();
    await expect(page.getByTestId('btn-nuevo-socio')).toHaveCount(0);
  });

  test('un usuario con SOCIOS:CREAR sí ve el botón de nuevo socio', async ({ page }) => {
    await loginAs(page, 'admin', 'admin123');
    await page.goto('/socios');

    await expect(page.getByTestId('btn-nuevo-socio')).toBeVisible();
  });

  test('logout regresa a login', async ({ page }) => {
    await loginAs(page, 'admin', 'admin123');
    await expect(page).toHaveURL(/\/dashboard/);

    await page.getByRole('button', { name: 'Salir' }).click();
    await expect(page).toHaveURL(/\/login/);
  });
});
