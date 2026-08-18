import type { Locator, Page } from '@playwright/test';

export async function loginAs(page: Page, username: string, password: string) {
  await page.goto('/login');
  await page.getByTestId('username').fill(username);
  await page.getByTestId('password').fill(password);
  await page.getByTestId('login-submit').click();
  await page.waitForURL(/\/dashboard/);
}

export async function interceptLargePage(page: Page, urlGlob: string): Promise<void> {
  await page.route(urlGlob, async (route) => {
    if (route.request().method() !== 'GET') {
      await route.continue();
      return;
    }
    const url = new URL(route.request().url());
    url.searchParams.set('size', '100');
    await route.continue({ url: url.toString() });
  });
}

export async function asegurarCajaAbierta(
  page: Page,
  username = 'admin',
  password = 'admin123'
): Promise<void> {
  const login = await page.request.post('http://localhost:8080/api/v1/auth/login', {
    data: { username, password }
  });
  const { token } = (await login.json()) as { token: string };
  const headers = { Authorization: `Bearer ${token}` };
  const res = await page.request.get('http://localhost:8080/api/v1/caja/mias', { headers });
  const { content: cajas } = (await res.json()) as { content: { id: number; estado: string; fecha: string }[] };
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

export async function operarConCaja(
  page: Page,
  testIdBtn: string,
  errorTestId: string,
  username = 'admin',
  password = 'admin123',
  abrirMenu?: Locator
): Promise<void> {
  await page.getByTestId(testIdBtn).click();
  const error = page.getByTestId(errorTestId);
  if (await error.waitFor({ state: 'visible', timeout: 1500 }).then(() => true).catch(() => false)) {
    if (/caja/i.test((await error.textContent()) ?? '')) {
      await asegurarCajaAbierta(page, username, password);
      if (abrirMenu) {
        await abrirMenu.click();
      }
      await page.getByTestId(testIdBtn).click();
    }
  }
}
