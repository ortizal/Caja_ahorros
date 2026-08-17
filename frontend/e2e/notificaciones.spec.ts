import { expect, test } from '@playwright/test';
import { loginAs } from './helpers';

const API = 'http://localhost:8080/api/v1';
const TS = Date.now().toString().slice(-8);

test.describe.configure({ mode: 'serial' });

test.describe('Notificaciones y alertas', () => {
  test('la campana muestra alertas generadas y permite marcarlas leidas', async ({ page }) => {
    const login = await page.request.post(`${API}/auth/login`, {
      data: { username: 'admin', password: 'admin123' }
    });
    const { token, usuarioId } = (await login.json()) as { token: string; usuarioId: number };
    const headers = { Authorization: `Bearer ${token}` };

    await page.request.post(`${API}/notificaciones/leidas`, { headers });

    const hoy = new Date();
    const anio = hoy.getFullYear();
    const mes = String(hoy.getMonth() + 1).padStart(2, '0');
    const periodo = `${anio}-${mes}`;
    const fecha = `${anio}-${mes}-${String(hoy.getDate()).padStart(2, '0')}`;

    await page.request.post(`${API}/socios`, {
      headers,
      data: {
        codigo: `E2EN${TS}`,
        identificacion: `9999${TS}`,
        nombres: 'E2E',
        apellidos: 'Notificaciones',
        fechaIngreso: fecha,
        usuarioId
      }
    });

    const generadas = await page.request.post(`${API}/aportaciones/generar?periodo=${periodo}`, { headers });
    expect(generadas.ok()).toBeTruthy();

    const generadasAlertas = await page.request.post(`${API}/notificaciones/generar`, { headers });
    expect(generadasAlertas.ok()).toBeTruthy();

    await loginAs(page, 'admin', 'admin123');

    const badge = page.locator('.notif-btn .badge');
    await expect(badge).toBeVisible();
    const count = parseInt((await badge.textContent()) ?? '0', 10);
    expect(count).toBeGreaterThan(0);

    await page.getByTestId('btn-notificaciones').click();
    const panel = page.getByTestId('notif-panel');
    await expect(panel).toBeVisible();
    await expect(panel).toContainText('aportacion del periodo');
    await expect(panel.locator('.notif-item')).not.toHaveCount(0);

    await panel.getByRole('button', { name: 'Marcar todas' }).click();
    await expect(badge).toHaveCount(0);
  });

  test('un usuario sin permiso de administrador no puede generar alertas', async ({ page }) => {
    const login = await page.request.post(`${API}/auth/login`, {
      data: { username: 'cajero', password: 'cajero123' }
    });
    const { token } = (await login.json()) as { token: string };
    const res = await page.request.post(`${API}/notificaciones/generar`, {
      headers: { Authorization: `Bearer ${token}` }
    });
    expect(res.status()).toBe(403);
  });
});
