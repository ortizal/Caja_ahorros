import type { Page } from '@playwright/test';

export async function loginAs(page: Page, username: string, password: string) {
  await page.goto('/login');
  await page.getByTestId('username').fill(username);
  await page.getByTestId('password').fill(password);
  await page.getByTestId('login-submit').click();
  await page.waitForURL(/\/dashboard/);
}

export async function asegurarCajaAbierta(page: Page): Promise<void> {
  const login = await page.request.post('http://localhost:8080/api/v1/auth/login', {
    data: { username: 'admin', password: 'admin123' }
  });
  const { token } = (await login.json()) as { token: string };
  const headers = { Authorization: `Bearer ${token}` };
  const res = await page.request.get('http://localhost:8080/api/v1/caja/mias', { headers });
  const cajas = (await res.json()) as { id: number; estado: string; fecha: string }[];
  const hoy = new Date();
  const hoyStr = `${hoy.getFullYear()}-${String(hoy.getMonth() + 1).padStart(2, '0')}-${String(hoy.getDate()).padStart(2, '0')}`;
  for (const c of cajas) {
    if (c.estado === 'ABIERTA' && c.fecha !== hoyStr) {
      await page.request.post(`http://localhost:8080/api/v1/caja/${c.id}/cierre`, {
        headers,
        data: { saldoFisico: 0 }
      });
    }
  }
  if (!cajas.some((c) => c.estado === 'ABIERTA' && c.fecha === hoyStr)) {
    await page.request.post('http://localhost:8080/api/v1/caja/apertura', {
      headers,
      data: { saldoInicial: 500 }
    });
  }
}

export async function operarConCaja(page: Page, testIdBtn: string, errorTestId: string): Promise<void> {
  await page.getByTestId(testIdBtn).click();
  const error = page.getByTestId(errorTestId);
  if (await error.waitFor({ state: 'visible', timeout: 1500 }).then(() => true).catch(() => false)) {
    if (/caja/i.test((await error.textContent()) ?? '')) {
      await asegurarCajaAbierta(page);
      await page.getByTestId(testIdBtn).click();
    }
  }
}
