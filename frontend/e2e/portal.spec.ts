import { expect, test } from '@playwright/test';
import type { Page } from '@playwright/test';

const API = 'http://localhost:8080/api/v1';

async function loginSocio(page: Page) {
  await page.goto('/login');
  await page.getByTestId('username').fill('socio');
  await page.getByTestId('password').fill('socio123');
  await page.getByTestId('login-submit').click();
  await page.waitForURL(/\/portal/);
}

async function apiToken(page: Page, username: string, password: string): Promise<string> {
  const res = await page.request.post(`${API}/auth/login`, { data: { username, password } });
  const { token } = (await res.json()) as { token: string };
  return token;
}

test.describe('Portal del socio', () => {
  test('un socio ingresa al portal de solo lectura', async ({ page }) => {
    await page.goto('/');
    await expect(page).toHaveURL(/\/login/);

    await loginSocio(page);

    await expect(page.getByRole('heading', { name: 'Portal del socio' })).toBeVisible();
    await expect(page).toHaveURL(/\/portal/);
    await expect(page.getByTestId('kpi-saldo-ahorro')).toBeVisible();
    await expect(page.getByTestId('kpi-saldo-credito')).toBeVisible();
    await expect(page.getByTestId('kpi-notificaciones')).toBeVisible();
    await expect(page.getByTestId('seccion-ahorro')).toBeVisible();
    await expect(page.getByTestId('seccion-aportaciones')).toBeVisible();
    await expect(page.getByTestId('seccion-creditos')).toBeVisible();
    await expect(page.getByTestId('seccion-notificaciones')).toBeVisible();
  });

  test('el resumen muestra el codigo del socio vinculado', async ({ page }) => {
    await loginSocio(page);

    await expect(page.getByText('SOC-DEMO-01')).toBeVisible();
  });

  test('el portal de lectura muestra la cuenta de ahorro y las aportaciones del socio', async ({ page }) => {
    const socioToken = await apiToken(page, 'socio', 'socio123');
    const adminToken = await apiToken(page, 'admin', 'admin123');
    const adminHeaders = { Authorization: `Bearer ${adminToken}` };

    const resumen = await page.request.get(`${API}/portal/resumen`, {
      headers: { Authorization: `Bearer ${socioToken}` }
    });
    const r = (await resumen.json()) as { socio: { id: number } };

    const ahorroActual = await page.request.get(`${API}/portal/ahorro`, {
      headers: { Authorization: `Bearer ${socioToken}` }
    });
    const cuentas = (await ahorroActual.json()) as { cuenta: { numeroCuenta: string } }[];
    if (cuentas.length === 0) {
      const productos = await page.request.get(`${API}/productos-ahorro`, { headers: adminHeaders });
      const listaProductos = (await productos.json()) as { id: number }[];
      await page.request.post(`${API}/cuentas-ahorro`, {
        headers: adminHeaders,
        data: { socioId: r.socio.id, productoId: listaProductos[0].id }
      });
    }

    const hoy = new Date();
    const periodo = `${hoy.getFullYear()}-${String(hoy.getMonth() + 1).padStart(2, '0')}`;
    const generar = await page.request.post(`${API}/aportaciones/generar?periodo=${periodo}`, {
      headers: adminHeaders
    });
    expect(generar.ok()).toBeTruthy();

    await loginSocio(page);

    await expect(page.getByTestId('seccion-ahorro')).toContainText('AH-');
    await expect(page.getByTestId('seccion-aportaciones')).toContainText(periodo);
    await expect(page.getByTestId('kpi-total-aportado')).toBeVisible();
  });

  test('un socio no accede a los modulos administrativos', async ({ page }) => {
    await loginSocio(page);

    await page.goto('/socios');
    await expect(page).toHaveURL(/\/dashboard/);
  });

  test('el portal permite cerrar sesion', async ({ page }) => {
    await loginSocio(page);

    await page.getByTestId('portal-logout').click();
    await expect(page).toHaveURL(/\/login/);
  });
});
